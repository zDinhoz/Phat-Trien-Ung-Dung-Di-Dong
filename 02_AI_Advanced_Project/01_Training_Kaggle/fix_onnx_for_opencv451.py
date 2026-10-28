from pathlib import Path
import sys

TOOLS = Path(r"C:\Users\XuanMinh\Documents\ChatGPT\HHH\report_ai_work\onnx_tools_lib")
sys.path.insert(0, str(TOOLS))

import onnx
import numpy as np
from onnxsim import simplify
from onnx import helper, numpy_helper


source = Path(
    r"C:\Users\XuanMinh\Documents\ChatGPT\HHH\results3_inspect"
    r"\mobilehandpose_large_20261011_164331_392841"
    r"\mobilehandpose_large_fp32.onnx"
)
destination = source.with_name("mobilehandpose_large_opencv451.onnx")

model = onnx.load(source)
onnx.checker.check_model(model)
# The BBB server always performs one-crop inference. A fixed batch dimension
# also lets old OpenCV versions resolve all intermediate tensor shapes.
model.graph.input[0].type.tensor_type.shape.dim[0].dim_param = ""
model.graph.input[0].type.tensor_type.shape.dim[0].dim_value = 1
print("source_opset", [(item.domain, item.version) for item in model.opset_import])
print("source_nodes", len(model.graph.node))
print("empty_optional_inputs", sum(value == "" for node in model.graph.node for value in node.input))

simplified, passed = simplify(model, check_n=1)
if not passed:
    raise RuntimeError("ONNX simplifier numerical check failed")

# OpenCV 4.5.1 tries to resolve empty optional Resize inputs as graph node
# names. Replace those empty names with legal zero-length tensor initializers.
empty_roi_name = "opencv451_empty_roi"
empty_scales_name = "opencv451_empty_scales"
simplified.graph.initializer.extend(
    [
        numpy_helper.from_array(np.asarray([], dtype=np.float32), empty_roi_name),
        numpy_helper.from_array(np.asarray([], dtype=np.float32), empty_scales_name),
    ]
)
for node in simplified.graph.node:
    if node.op_type != "Resize":
        continue
    if len(node.input) > 1 and node.input[1] == "":
        node.input[1] = empty_roi_name
    if len(node.input) > 2 and node.input[2] == "":
        node.input[2] = empty_scales_name

# OpenCV 4.5.1 has no HardSigmoid layer. Expand each HardSigmoid into the
# equivalent Clip(alpha * x + beta, 0, 1), using only well-supported ops.
rewritten_nodes = []
hard_sigmoid_count = 0
for node in simplified.graph.node:
    if node.op_type != "HardSigmoid":
        rewritten_nodes.append(node)
        continue

    hard_sigmoid_count += 1
    attributes = {item.name: helper.get_attribute_value(item) for item in node.attribute}
    alpha = float(attributes.get("alpha", 0.2))
    beta = float(attributes.get("beta", 0.5))
    stem = f"opencv451_hardsigmoid_{hard_sigmoid_count}"
    alpha_name = f"{stem}_alpha"
    beta_name = f"{stem}_beta"
    min_name = f"{stem}_min"
    max_name = f"{stem}_max"
    mul_output = f"{stem}_mul"
    add_output = f"{stem}_add"
    simplified.graph.initializer.extend(
        [
            numpy_helper.from_array(np.asarray(alpha, dtype=np.float32), alpha_name),
            numpy_helper.from_array(np.asarray(beta, dtype=np.float32), beta_name),
            numpy_helper.from_array(np.asarray(0.0, dtype=np.float32), min_name),
            numpy_helper.from_array(np.asarray(1.0, dtype=np.float32), max_name),
        ]
    )
    rewritten_nodes.extend(
        [
            helper.make_node("Mul", [node.input[0], alpha_name], [mul_output], name=f"{stem}_Mul"),
            helper.make_node("Add", [mul_output, beta_name], [add_output], name=f"{stem}_Add"),
            helper.make_node("Clip", [add_output, min_name, max_name], list(node.output), name=f"{stem}_Clip"),
        ]
    )

del simplified.graph.node[:]
simplified.graph.node.extend(rewritten_nodes)
print("rewritten_hard_sigmoid", hard_sigmoid_count)

# OpenCV 4.5.1 Eltwise does not broadcast an SE gate shaped N,C,1,1 over an
# N,C,H,W feature map. Materialize the gate to the feature-map size first.
simplified = onnx.shape_inference.infer_shapes(simplified)


def tensor_shapes(graph):
    result = {}
    values = list(graph.input) + list(graph.value_info) + list(graph.output)
    for value in values:
        tensor_type = value.type.tensor_type
        if not tensor_type.HasField("shape"):
            continue
        dims = []
        for dim in tensor_type.shape.dim:
            dims.append(dim.dim_value if dim.HasField("dim_value") else None)
        result[value.name] = tuple(dims)
    for item in graph.initializer:
        result[item.name] = tuple(item.dims)
    return result


shapes = tensor_shapes(simplified.graph)
broadcast_nodes = []
se_expand_count = 0
for node in simplified.graph.node:
    if node.op_type == "Mul" and len(node.input) == 2:
        left_shape = shapes.get(node.input[0])
        right_shape = shapes.get(node.input[1])
        pair = [(0, left_shape, right_shape), (1, right_shape, left_shape)]
        gate = next(
            (
                (index, gate_shape, feature_shape)
                for index, gate_shape, feature_shape in pair
                if gate_shape
                and feature_shape
                and len(gate_shape) == 4
                and len(feature_shape) == 4
                and gate_shape[:2] == feature_shape[:2]
                and gate_shape[2:] == (1, 1)
                and feature_shape[2] not in (None, 1)
                and feature_shape[3] not in (None, 1)
            ),
            None,
        )
        if gate is not None:
            index, _, feature_shape = gate
            se_expand_count += 1
            stem = f"opencv451_se_expand_{se_expand_count}"
            sizes_name = f"{stem}_sizes"
            expanded_name = f"{stem}_output"
            simplified.graph.initializer.extend(
                [
                    numpy_helper.from_array(
                        np.asarray(feature_shape, dtype=np.int64), sizes_name
                    )
                ]
            )
            broadcast_nodes.append(
                helper.make_node(
                    "Resize",
                    [node.input[index], empty_roi_name, empty_scales_name, sizes_name],
                    [expanded_name],
                    name=f"{stem}_Resize",
                    mode="nearest",
                    coordinate_transformation_mode="asymmetric",
                )
            )
            node.input[index] = expanded_name
    broadcast_nodes.append(node)

del simplified.graph.node[:]
simplified.graph.node.extend(broadcast_nodes)
print("expanded_se_gates", se_expand_count)

onnx.checker.check_model(simplified)
onnx.save(simplified, destination)
print("destination", destination)
print("simplified_nodes", len(simplified.graph.node))
print("empty_optional_inputs", sum(value == "" for node in simplified.graph.node for value in node.input))
