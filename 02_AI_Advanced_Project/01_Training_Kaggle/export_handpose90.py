from __future__ import annotations

import importlib.util
import json
from pathlib import Path
import sys
import time


ROOT = Path(r"C:\Users\XuanMinh\Documents\ChatGPT\HHH")
TOOLS = ROOT / "report_ai_work" / "export_tools"
RUN = ROOT / "results90_inspect" / "handpose_accuracy_v3_20261011_145437_630011"
MODEL_SOURCE = ROOT / "results90_inspect" / "handpose_accuracy.py"
CHECKPOINT = RUN / "handpose_accuracy_best.pth"
OUTPUT = RUN / "handpose_accuracy_epoch84_opset11.onnx"
REPORT = RUN / "onnx_export_validation.json"

sys.path.insert(0, str(TOOLS))

import numpy as np
import onnx
import onnxruntime as ort
import torch


def load_model_module():
    spec = importlib.util.spec_from_file_location("handpose_accuracy_export", MODEL_SOURCE)
    if spec is None or spec.loader is None:
        raise RuntimeError(f"Cannot import model source: {MODEL_SOURCE}")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


module = load_model_module()
checkpoint = torch.load(CHECKPOINT, map_location="cpu", weights_only=True)
if checkpoint.get("architecture") != "HandPoseAccuracy":
    raise RuntimeError(f"Unexpected architecture: {checkpoint.get('architecture')}")

model = module.HandPoseAccuracy(pretrained=False).cpu().eval()
model.load_state_dict(checkpoint["model_state"], strict=True)

torch.manual_seed(42)
sample = torch.randn(1, 3, module.IMAGE_SIZE, module.IMAGE_SIZE, dtype=torch.float32)
with torch.inference_mode():
    pytorch_output = model(sample).numpy()

started = time.perf_counter()
torch.onnx.export(
    model,
    sample,
    OUTPUT,
    export_params=True,
    opset_version=11,
    do_constant_folding=True,
    input_names=["images"],
    output_names=["heatmaps"],
    dynamo=False,
)
export_seconds = time.perf_counter() - started

onnx_model = onnx.load(OUTPUT)
onnx.checker.check_model(onnx_model)

session = ort.InferenceSession(str(OUTPUT), providers=["CPUExecutionProvider"])
ort_output = session.run(["heatmaps"], {"images": sample.numpy()})[0]

report = {
    "checkpoint": str(CHECKPOINT),
    "onnx": str(OUTPUT),
    "architecture": checkpoint["architecture"],
    "best_epoch": int(checkpoint["epoch"]),
    "best_validation_pck": float(checkpoint["best_val_pck"]),
    "best_validation_epe_px": float(checkpoint["best_val_epe_px"]),
    "input_shape": list(sample.shape),
    "output_shape": list(ort_output.shape),
    "opset": 11,
    "parameters": int(sum(parameter.numel() for parameter in model.parameters())),
    "export_seconds": export_seconds,
    "onnxruntime_max_abs_diff": float(np.max(np.abs(pytorch_output - ort_output))),
    "onnxruntime_mean_abs_diff": float(np.mean(np.abs(pytorch_output - ort_output))),
    "onnx_checker_passed": True,
}
REPORT.write_text(json.dumps(report, indent=2, ensure_ascii=False), encoding="utf-8")
print(json.dumps(report, indent=2, ensure_ascii=False))
