#!/usr/bin/env python3
"""BeagleBone Black TCP server for MobileHandPose ONNX inference.

Receives a square JPEG crop, runs OpenCV DNN on CPU, draws 21 keypoints, and
returns both keypoint JSON and the annotated JPEG to the laptop.
"""

from __future__ import annotations

import argparse
import json
import socket
import struct
import time

import cv2
import numpy as np


PACKET_HEADER = struct.Struct("!II")
MAX_JSON_BYTES = 1_000_000
MAX_IMAGE_BYTES = 8_000_000
IMAGE_SIZE = 192
HEATMAP_SIZE = 48
MEAN = np.asarray((0.485, 0.456, 0.406), dtype=np.float32).reshape(1, 1, 3)
STD = np.asarray((0.229, 0.224, 0.225), dtype=np.float32).reshape(1, 1, 3)
EDGES = [
    (0, 1), (1, 2), (2, 3), (3, 4),
    (0, 5), (5, 6), (6, 7), (7, 8),
    (0, 9), (9, 10), (10, 11), (11, 12),
    (0, 13), (13, 14), (14, 15), (15, 16),
    (0, 17), (17, 18), (18, 19), (19, 20),
]
COLORS = [
    (220, 220, 220),
    (255, 120, 60), (255, 120, 60), (255, 120, 60), (255, 120, 60),
    (60, 220, 255), (60, 220, 255), (60, 220, 255), (60, 220, 255),
    (80, 255, 80), (80, 255, 80), (80, 255, 80), (80, 255, 80),
    (255, 80, 220), (255, 80, 220), (255, 80, 220), (255, 80, 220),
    (80, 120, 255), (80, 120, 255), (80, 120, 255), (80, 120, 255),
]


def recv_exact(sock: socket.socket, size: int) -> bytes:
    chunks = bytearray()
    while len(chunks) < size:
        block = sock.recv(size - len(chunks))
        if not block:
            raise ConnectionError("Client disconnected")
        chunks.extend(block)
    return bytes(chunks)


def receive_packet(sock: socket.socket) -> tuple[dict, bytes]:
    json_size, blob_size = PACKET_HEADER.unpack(recv_exact(sock, PACKET_HEADER.size))
    if json_size > MAX_JSON_BYTES or blob_size > MAX_IMAGE_BYTES:
        raise ValueError(f"Invalid packet sizes: json={json_size}, blob={blob_size}")
    metadata = json.loads(recv_exact(sock, json_size).decode("utf-8"))
    return metadata, recv_exact(sock, blob_size)


def send_packet(sock: socket.socket, metadata: dict, blob: bytes) -> None:
    encoded = json.dumps(metadata, separators=(",", ":")).encode("utf-8")
    if len(encoded) > MAX_JSON_BYTES or len(blob) > MAX_IMAGE_BYTES:
        raise ValueError("Packet is too large")
    sock.sendall(PACKET_HEADER.pack(len(encoded), len(blob)))
    sock.sendall(encoded)
    sock.sendall(blob)


class MobileHandPoseOnnx:
    def __init__(
        self,
        model_path: str,
        threads: int,
        image_size: int = IMAGE_SIZE,
        heatmap_size: int = HEATMAP_SIZE,
    ) -> None:
        self.image_size = image_size
        self.heatmap_size = heatmap_size
        cv2.setNumThreads(max(1, threads))
        self.net = cv2.dnn.readNetFromONNX(model_path)
        self.net.setPreferableBackend(cv2.dnn.DNN_BACKEND_OPENCV)
        self.net.setPreferableTarget(cv2.dnn.DNN_TARGET_CPU)

        # Fail early if the installed OpenCV cannot execute an exported layer.
        warmup = np.zeros((self.image_size, self.image_size, 3), np.uint8)
        self.infer(warmup)

    def preprocess(self, bgr: np.ndarray) -> np.ndarray:
        resized = cv2.resize(
            bgr,
            (self.image_size, self.image_size),
            interpolation=cv2.INTER_LINEAR,
        )
        rgb = cv2.cvtColor(resized, cv2.COLOR_BGR2RGB).astype(np.float32) / 255.0
        rgb = (rgb - MEAN) / STD
        return np.transpose(rgb, (2, 0, 1))[None].astype(np.float32, copy=False)

    def infer(self, bgr: np.ndarray) -> tuple[np.ndarray, float]:
        blob = self.preprocess(bgr)
        started = time.perf_counter()
        self.net.setInput(blob)
        output = self.net.forward()
        inference_ms = (time.perf_counter() - started) * 1000.0
        heatmaps = np.asarray(output, dtype=np.float32)
        if heatmaps.ndim == 4:
            heatmaps = heatmaps[0]
        expected_shape = (21, self.heatmap_size, self.heatmap_size)
        if heatmaps.shape != expected_shape:
            raise RuntimeError(
                f"Expected ONNX output {expected_shape}, got {heatmaps.shape}"
            )

        indices = heatmaps.reshape(21, -1).argmax(axis=1)
        x = (indices % self.heatmap_size + 0.5) / self.heatmap_size
        y = (indices // self.heatmap_size + 0.5) / self.heatmap_size
        return np.stack((x, y), axis=1).astype(np.float32), inference_ms


class MockHandPose:
    """Deterministic skeleton used only to test camera and TCP before training."""

    def __init__(self) -> None:
        self.points = np.asarray(
            [
                [0.50, 0.88],
                [0.39, 0.72], [0.31, 0.61], [0.24, 0.50], [0.18, 0.39],
                [0.43, 0.64], [0.41, 0.48], [0.40, 0.32], [0.39, 0.17],
                [0.51, 0.62], [0.51, 0.43], [0.51, 0.26], [0.51, 0.10],
                [0.59, 0.65], [0.61, 0.48], [0.62, 0.33], [0.63, 0.20],
                [0.66, 0.70], [0.70, 0.57], [0.73, 0.45], [0.76, 0.35],
            ],
            dtype=np.float32,
        )

    def infer(self, bgr: np.ndarray) -> tuple[np.ndarray, float]:
        return self.points.copy(), 0.0


def annotate(image: np.ndarray, normalized_xy: np.ndarray) -> np.ndarray:
    output = image.copy()
    height, width = output.shape[:2]
    points = normalized_xy * np.asarray([width, height], np.float32)
    points = np.rint(points).astype(np.int32)

    for source, target in EDGES:
        color = COLORS[target]
        cv2.line(
            output,
            tuple(points[source]),
            tuple(points[target]),
            color,
            2,
            cv2.LINE_AA,
        )
    for index, point in enumerate(points):
        cv2.circle(output, tuple(point), 3 if index else 4, COLORS[index], -1, cv2.LINE_AA)
    return output


def serve_client(
    connection: socket.socket,
    address,
    model: MobileHandPoseOnnx,
    jpeg_quality: int,
) -> None:
    connection.settimeout(30.0)
    print("Client connected:", address, flush=True)
    while True:
        request, jpeg = receive_packet(connection)
        frame = cv2.imdecode(np.frombuffer(jpeg, np.uint8), cv2.IMREAD_COLOR)
        if frame is None:
            raise ValueError("Client sent an invalid JPEG")

        server_started = time.perf_counter()
        normalized_xy, inference_ms = model.infer(frame)
        annotated = annotate(frame, normalized_xy)
        ok, encoded = cv2.imencode(
            ".jpg", annotated, [cv2.IMWRITE_JPEG_QUALITY, jpeg_quality]
        )
        if not ok:
            raise RuntimeError("Could not encode response JPEG")

        response = {
            "frame_id": request.get("frame_id"),
            "hand_id": request.get("hand_id", "hand_1"),
            "capture_time_ns": request.get("capture_time_ns"),
            "crop_box": request.get("crop_box"),
            "inference_ms": inference_ms,
            "server_total_ms": (time.perf_counter() - server_started) * 1000.0,
            "keypoints_normalized": normalized_xy.tolist(),
        }
        send_packet(connection, response, encoded.tobytes())


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--model", help="Path to mobilehandpose_large_fp32.onnx")
    parser.add_argument(
        "--mock",
        action="store_true",
        help="Test camera/TCP using a fixed skeleton; does not load an AI model",
    )
    parser.add_argument("--host", default="0.0.0.0")
    parser.add_argument("--port", type=int, default=5000)
    parser.add_argument("--threads", type=int, default=1)
    parser.add_argument("--jpeg-quality", type=int, default=82)
    parser.add_argument("--image-size", type=int, default=IMAGE_SIZE)
    parser.add_argument("--heatmap-size", type=int, default=HEATMAP_SIZE)
    args = parser.parse_args()
    if not args.mock and not args.model:
        parser.error("--model is required unless --mock is used")
    return args


def main() -> None:
    args = parse_args()
    if args.mock:
        model = MockHandPose()
        print("MOCK mode: testing camera and TCP without AI inference", flush=True)
    else:
        print("Loading model:", args.model, flush=True)
        model = MobileHandPoseOnnx(
            args.model,
            args.threads,
            image_size=args.image_size,
            heatmap_size=args.heatmap_size,
        )
        print("Model warm-up passed", flush=True)

    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as server:
        server.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        server.bind((args.host, args.port))
        server.listen(1)
        print(f"Listening on {args.host}:{args.port}", flush=True)
        while True:
            connection, address = server.accept()
            with connection:
                try:
                    serve_client(connection, address, model, args.jpeg_quality)
                except (ConnectionError, TimeoutError, socket.timeout) as exc:
                    print("Client disconnected:", exc, flush=True)
                except Exception as exc:
                    print(f"Client error: {type(exc).__name__}: {exc}", flush=True)


if __name__ == "__main__":
    main()
