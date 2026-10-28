#!/usr/bin/env python3
"""Capture a hand ROI, send JPEG frames to BBB, and display BBB annotations.

The network worker keeps at most one pending frame. Old frames are dropped, so a
slow BBB reduces FPS instead of building an ever-growing latency queue.
"""

from __future__ import annotations

import argparse
import json
import os
import queue
import socket
import struct
import threading
import time
from dataclasses import dataclass
from pathlib import Path

import cv2
import numpy as np


PACKET_HEADER = struct.Struct("!II")  # JSON length, binary blob length
MAX_JSON_BYTES = 1_000_000
MAX_IMAGE_BYTES = 8_000_000
EDGES = [
    (0, 1), (1, 2), (2, 3), (3, 4),
    (0, 5), (5, 6), (6, 7), (7, 8),
    (0, 9), (9, 10), (10, 11), (11, 12),
    (0, 13), (13, 14), (14, 15), (15, 16),
    (0, 17), (17, 18), (18, 19), (19, 20),
]
HAND_COLORS = {
    "left": (0, 255, 0),
    "right": (255, 180, 0),
    "hand_1": (0, 255, 0),
    "hand_2": (255, 180, 0),
}


def recv_exact(sock: socket.socket, size: int) -> bytes:
    chunks = bytearray()
    while len(chunks) < size:
        block = sock.recv(size - len(chunks))
        if not block:
            raise ConnectionError("BBB closed the connection")
        chunks.extend(block)
    return bytes(chunks)


def send_packet(sock: socket.socket, metadata: dict, blob: bytes) -> None:
    encoded = json.dumps(metadata, separators=(",", ":")).encode("utf-8")
    if len(encoded) > MAX_JSON_BYTES or len(blob) > MAX_IMAGE_BYTES:
        raise ValueError("Packet is too large")
    sock.sendall(PACKET_HEADER.pack(len(encoded), len(blob)))
    sock.sendall(encoded)
    sock.sendall(blob)


def receive_packet(sock: socket.socket) -> tuple[dict, bytes]:
    json_size, blob_size = PACKET_HEADER.unpack(recv_exact(sock, PACKET_HEADER.size))
    if json_size > MAX_JSON_BYTES or blob_size > MAX_IMAGE_BYTES:
        raise ValueError(f"Invalid packet sizes: json={json_size}, blob={blob_size}")
    metadata = json.loads(recv_exact(sock, json_size).decode("utf-8"))
    return metadata, recv_exact(sock, blob_size)


def centered_square(frame: np.ndarray, scale: float) -> tuple[np.ndarray, tuple[int, int, int, int]]:
    height, width = frame.shape[:2]
    side = max(32, int(min(width, height) * scale))
    left = (width - side) // 2
    top = (height - side) // 2
    return frame[top : top + side, left : left + side].copy(), (left, top, side, side)


@dataclass
class DetectedHand:
    hand_id: str
    crop: np.ndarray
    box: tuple[int, int, int, int]


class HandAutoCropper:
    """Use MediaPipe to locate hands; BBB remains the pose estimator."""

    def __init__(
        self,
        model_path: Path,
        min_detection_confidence: float,
        margin: float,
        smoothing: float,
        max_hands: int,
    ) -> None:
        # MediaPipe imports matplotlib; keep its cache inside the writable project.
        cache_dir = Path(__file__).resolve().parent / ".mplconfig"
        cache_dir.mkdir(parents=True, exist_ok=True)
        os.environ.setdefault("MPLCONFIGDIR", str(cache_dir))

        import mediapipe as mp

        self.mp = mp
        options = mp.tasks.vision.HandLandmarkerOptions(
            base_options=mp.tasks.BaseOptions(model_asset_path=str(model_path)),
            running_mode=mp.tasks.vision.RunningMode.VIDEO,
            num_hands=max_hands,
            min_hand_detection_confidence=min_detection_confidence,
            min_hand_presence_confidence=min_detection_confidence,
            min_tracking_confidence=0.5,
        )
        self.detector = mp.tasks.vision.HandLandmarker.create_from_options(options)
        self.margin = margin
        self.smoothing = smoothing
        self.smoothed_boxes: dict[str, np.ndarray] = {}
        self.last_timestamp_ms = 0

    def close(self) -> None:
        self.detector.close()

    def detect(self, frame: np.ndarray) -> list[DetectedHand]:
        rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
        mp_image = self.mp.Image(image_format=self.mp.ImageFormat.SRGB, data=rgb)
        timestamp_ms = max(int(time.monotonic() * 1000), self.last_timestamp_ms + 1)
        self.last_timestamp_ms = timestamp_ms
        result = self.detector.detect_for_video(mp_image, timestamp_ms)
        if not result.hand_landmarks:
            self.smoothed_boxes.clear()
            return []

        height, width = frame.shape[:2]
        raw_hands = []
        for index, landmarks in enumerate(result.hand_landmarks):
            xs = np.asarray([point.x * width for point in landmarks], dtype=np.float32)
            ys = np.asarray([point.y * height for point in landmarks], dtype=np.float32)
            center_x = float((xs.min() + xs.max()) * 0.5)
            center_y = float((ys.min() + ys.max()) * 0.5)
            side = float(max(xs.max() - xs.min(), ys.max() - ys.min()))
            side = max(64.0, side * (1.0 + 2.0 * self.margin))
            side = min(side, float(min(width, height)))
            label = f"hand_{index + 1}"
            if index < len(result.handedness) and result.handedness[index]:
                category = result.handedness[index][0]
                category_name = getattr(category, "category_name", None)
                if category_name:
                    label = str(category_name).lower()
            raw_hands.append((label, center_x, center_y, side))

        # Handedness normally gives stable left/right IDs. Fall back to x-order
        # if MediaPipe returns duplicate labels for the same frame.
        labels = [item[0] for item in raw_hands]
        if len(set(labels)) != len(labels):
            raw_hands.sort(key=lambda item: item[1])
            raw_hands = [
                (f"hand_{index + 1}", center_x, center_y, side)
                for index, (_, center_x, center_y, side) in enumerate(raw_hands)
            ]

        active_ids = {item[0] for item in raw_hands}
        self.smoothed_boxes = {
            hand_id: box
            for hand_id, box in self.smoothed_boxes.items()
            if hand_id in active_ids
        }
        detected: list[DetectedHand] = []
        for hand_id, center_x, center_y, side in raw_hands:
            new_box = np.asarray((center_x, center_y, side), dtype=np.float32)
            previous = self.smoothed_boxes.get(hand_id)
            if previous is None:
                smoothed = new_box
            else:
                alpha = self.smoothing
                smoothed = (1.0 - alpha) * previous + alpha * new_box
            self.smoothed_boxes[hand_id] = smoothed

            center_x, center_y, side = map(float, smoothed)
            side_i = max(32, min(int(round(side)), min(width, height)))
            left = int(round(center_x - side_i * 0.5))
            top = int(round(center_y - side_i * 0.5))
            left = max(0, min(left, width - side_i))
            top = max(0, min(top, height - side_i))
            crop = frame[top : top + side_i, left : left + side_i].copy()
            detected.append(DetectedHand(hand_id, crop, (left, top, side_i, side_i)))
        return sorted(detected, key=lambda hand: hand.hand_id)


@dataclass
class SharedResult:
    image: np.ndarray | None = None
    metadata: dict | None = None
    hands: dict[str, dict] | None = None
    error: str | None = None
    updated_at: float = 0.0
    snapshot_saved: bool = False


class BbbWorker(threading.Thread):
    def __init__(
        self,
        host: str,
        port: int,
        request_queue: queue.Queue,
        shared: SharedResult,
        lock: threading.Lock,
        stop_event: threading.Event,
        jpeg_quality: int,
        snapshot_dir: Path | None,
        snapshot_not_before: float,
    ) -> None:
        super().__init__(daemon=True)
        self.host = host
        self.port = port
        self.request_queue = request_queue
        self.shared = shared
        self.lock = lock
        self.stop_event = stop_event
        self.jpeg_quality = jpeg_quality
        self.snapshot_dir = snapshot_dir
        self.snapshot_saved = False
        self.snapshot_not_before = snapshot_not_before
        self.last_response_time = None
        self.response_fps = 0.0

    def set_error(self, message: str) -> None:
        with self.lock:
            self.shared.error = message
            self.shared.updated_at = time.monotonic()

    def run(self) -> None:
        sock = None
        while not self.stop_event.is_set():
            try:
                frame_id, hand_id, crop, crop_box, capture_time_ns = self.request_queue.get(timeout=0.2)
            except queue.Empty:
                continue

            try:
                if sock is None:
                    sock = socket.create_connection((self.host, self.port), timeout=5.0)
                    sock.settimeout(20.0)

                ok, encoded = cv2.imencode(
                    ".jpg", crop, [cv2.IMWRITE_JPEG_QUALITY, self.jpeg_quality]
                )
                if not ok:
                    raise RuntimeError("Could not JPEG-encode camera crop")

                sent_at = time.perf_counter()
                send_packet(
                    sock,
                    {
                        "frame_id": frame_id,
                        "hand_id": hand_id,
                        "capture_time_ns": capture_time_ns,
                        "crop_width": int(crop.shape[1]),
                        "crop_height": int(crop.shape[0]),
                        "crop_box": list(crop_box),
                    },
                    encoded.tobytes(),
                )
                metadata, jpeg = receive_packet(sock)
                received_at = time.perf_counter()
                metadata["round_trip_ms"] = (received_at - sent_at) * 1000.0
                if self.last_response_time is not None:
                    instant_fps = 1.0 / max(received_at - self.last_response_time, 1e-6)
                    self.response_fps = (
                        instant_fps
                        if self.response_fps == 0.0
                        else 0.85 * self.response_fps + 0.15 * instant_fps
                    )
                self.last_response_time = received_at
                metadata["stream_fps"] = self.response_fps
                metadata.setdefault("hand_id", hand_id)
                metadata.setdefault("crop_box", list(crop_box))
                metadata["received_monotonic"] = time.monotonic()
                image = cv2.imdecode(np.frombuffer(jpeg, np.uint8), cv2.IMREAD_COLOR)
                if image is None:
                    raise RuntimeError("BBB returned an invalid JPEG")

                if (
                    self.snapshot_dir is not None
                    and not self.snapshot_saved
                    and time.monotonic() >= self.snapshot_not_before
                ):
                    self.snapshot_dir.mkdir(parents=True, exist_ok=True)
                    camera_path = self.snapshot_dir / "camera_crop_sent.jpg"
                    returned_path = self.snapshot_dir / "bbb_returned.jpg"
                    metadata_path = self.snapshot_dir / "bbb_returned_metadata.json"
                    if not cv2.imwrite(str(camera_path), crop):
                        raise RuntimeError(f"Could not save {camera_path}")
                    if not cv2.imwrite(str(returned_path), image):
                        raise RuntimeError(f"Could not save {returned_path}")
                    metadata_path.write_text(
                        json.dumps(metadata, indent=2), encoding="utf-8"
                    )
                    self.snapshot_saved = True

                with self.lock:
                    self.shared.image = image
                    self.shared.metadata = metadata
                    if self.shared.hands is None:
                        self.shared.hands = {}
                    self.shared.hands[hand_id] = dict(metadata)
                    self.shared.error = None
                    self.shared.updated_at = time.monotonic()
                    self.shared.snapshot_saved = self.snapshot_saved
            except Exception as exc:
                self.set_error(f"{type(exc).__name__}: {exc}")
                if sock is not None:
                    try:
                        sock.close()
                    except OSError:
                        pass
                    sock = None
                time.sleep(0.5)

        if sock is not None:
            try:
                sock.close()
            except OSError:
                pass


def replace_latest(target_queue: queue.Queue, item) -> None:
    try:
        target_queue.put_nowait(item)
        return
    except queue.Full:
        pass

    # Drop the stale frame and keep only the newest crop.  This prevents a
    # slow BBB from building an increasingly delayed video queue.
    try:
        target_queue.get_nowait()
    except queue.Empty:
        pass
    try:
        target_queue.put_nowait(item)
    except queue.Full:
        pass


def draw_hand_pose(
    frame: np.ndarray,
    box: tuple[int, int, int, int],
    normalized_xy,
    color: tuple[int, int, int],
) -> None:
    points = np.asarray(normalized_xy, dtype=np.float32)
    if points.shape != (21, 2) or not np.isfinite(points).all():
        return
    left, top, width, height = box
    points = points * np.asarray((width, height), np.float32)
    points += np.asarray((left, top), np.float32)
    pixels = np.rint(points).astype(np.int32)
    for source, target in EDGES:
        cv2.line(frame, tuple(pixels[source]), tuple(pixels[target]), color, 2, cv2.LINE_AA)
    for index, point in enumerate(pixels):
        cv2.circle(frame, tuple(point), 4 if index == 0 else 3, color, -1, cv2.LINE_AA)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--host", default="192.168.2.2", help="BBB Ethernet IP")
    parser.add_argument("--port", type=int, default=5000)
    parser.add_argument("--camera", type=int, default=0)
    parser.add_argument("--max-fps", type=float, default=15.0)
    parser.add_argument("--roi-scale", type=float, default=0.72)
    parser.add_argument(
        "--detector-model",
        type=Path,
        default=Path(__file__).resolve().parent / "hand_landmarker.task",
        help="MediaPipe Hand Landmarker model used only for automatic cropping",
    )
    parser.add_argument(
        "--no-auto-hand",
        action="store_true",
        help="Disable hand detection and use the old fixed center crop",
    )
    parser.add_argument("--hand-confidence", type=float, default=0.55)
    parser.add_argument("--max-hands", type=int, default=2, choices=(1, 2))
    parser.add_argument("--hand-margin", type=float, default=0.30)
    parser.add_argument("--box-smoothing", type=float, default=0.35)
    parser.add_argument("--send-size", type=int, default=256)
    parser.add_argument("--jpeg-quality", type=int, default=82)
    parser.add_argument("--camera-width", type=int, default=640)
    parser.add_argument("--camera-height", type=int, default=480)
    parser.add_argument(
        "--snapshot-dir",
        type=Path,
        help="Save the first transmitted crop, BBB response, and metadata here",
    )
    parser.add_argument(
        "--exit-after-snapshot",
        action="store_true",
        help="Close automatically after the first response snapshot is saved",
    )
    parser.add_argument(
        "--snapshot-delay",
        type=float,
        default=0.0,
        help="Seconds to wait before saving the response snapshot",
    )
    args = parser.parse_args()
    if not 0.2 <= args.roi_scale <= 1.0:
        parser.error("--roi-scale must be in [0.2, 1.0]")
    if args.max_fps <= 0:
        parser.error("--max-fps must be positive")
    if not 0.0 < args.hand_confidence <= 1.0:
        parser.error("--hand-confidence must be in (0, 1]")
    if not 0.0 < args.box_smoothing <= 1.0:
        parser.error("--box-smoothing must be in (0, 1]")
    if args.hand_margin < 0.0:
        parser.error("--hand-margin must be non-negative")
    return args


def main() -> None:
    args = parse_args()
    camera = cv2.VideoCapture(args.camera)
    camera.set(cv2.CAP_PROP_FRAME_WIDTH, args.camera_width)
    camera.set(cv2.CAP_PROP_FRAME_HEIGHT, args.camera_height)
    camera.set(cv2.CAP_PROP_BUFFERSIZE, 1)
    if not camera.isOpened():
        raise RuntimeError(f"Could not open camera {args.camera}")

    cropper = None
    if not args.no_auto_hand:
        if not args.detector_model.is_file():
            raise FileNotFoundError(
                f"Missing MediaPipe model: {args.detector_model}. "
                "Place hand_landmarker.task beside this script."
            )
        cropper = HandAutoCropper(
            args.detector_model,
            args.hand_confidence,
            args.hand_margin,
            args.box_smoothing,
            args.max_hands,
        )

    request_queue: queue.Queue = queue.Queue(maxsize=1)
    shared = SharedResult(hands={})
    lock = threading.Lock()
    stop_event = threading.Event()
    snapshot_not_before = time.monotonic() + max(0.0, args.snapshot_delay)
    worker = BbbWorker(
        args.host,
        args.port,
        request_queue,
        shared,
        lock,
        stop_event,
        args.jpeg_quality,
        args.snapshot_dir,
        snapshot_not_before,
    )
    worker.start()

    frame_id = 0
    round_robin_index = 0
    next_send = 0.0
    send_period = 1.0 / args.max_fps
    print(f"Sending to {args.host}:{args.port}; press Q or Esc to quit")

    try:
        while True:
            ok, frame = camera.read()
            if not ok:
                raise RuntimeError("Camera read failed")

            if cropper is not None:
                detected_hands = cropper.detect(frame)
            else:
                crop, box = centered_square(frame, args.roi_scale)
                detected_hands = [DetectedHand("hand_1", crop, box)]

            now = time.monotonic()
            if detected_hands and now >= next_send:
                selected = detected_hands[round_robin_index % len(detected_hands)]
                round_robin_index += 1
                resized_crop = cv2.resize(
                    selected.crop,
                    (args.send_size, args.send_size),
                    interpolation=cv2.INTER_AREA,
                )
                frame_id += 1
                replace_latest(
                    request_queue,
                    (frame_id, selected.hand_id, resized_crop, selected.box, time.time_ns()),
                )
                next_send = now + send_period

            with lock:
                result_image = None if shared.image is None else shared.image.copy()
                result_meta = None if shared.metadata is None else dict(shared.metadata)
                hand_results = {
                    hand_id: dict(metadata)
                    for hand_id, metadata in (shared.hands or {}).items()
                }
                error = shared.error
                snapshot_saved = shared.snapshot_saved

            display = frame.copy()
            if detected_hands:
                for detected_hand in detected_hands:
                    left, top, side, _ = detected_hand.box
                    color = HAND_COLORS.get(detected_hand.hand_id, (0, 255, 255))
                    cv2.rectangle(display, (left, top), (left + side, top + side), color, 2)
                    cv2.putText(
                        display,
                        detected_hand.hand_id,
                        (left, max(18, top - 7)),
                        cv2.FONT_HERSHEY_SIMPLEX,
                        0.55,
                        color,
                        2,
                        cv2.LINE_AA,
                    )
                    hand_meta = hand_results.get(detected_hand.hand_id)
                    if hand_meta is not None:
                        result_age = now - float(hand_meta.get("received_monotonic", 0.0))
                        if result_age <= 2.0:
                            draw_hand_pose(
                                display,
                                detected_hand.box,
                                hand_meta.get("keypoints_normalized"),
                                color,
                            )
                status_text = f"{len(detected_hands)} hand(s) detected - alternating crops to BBB"
                status_color = (0, 255, 0)
            else:
                status_text = "No hand - nothing sent to BBB"
                status_color = (0, 0, 255)
            cv2.putText(
                display,
                status_text,
                (12, 28),
                cv2.FONT_HERSHEY_SIMPLEX,
                0.65,
                status_color,
                2,
                cv2.LINE_AA,
            )
            if args.snapshot_dir is not None and not snapshot_saved:
                remaining = max(0.0, snapshot_not_before - time.monotonic())
                cv2.putText(
                    display,
                    f"Snapshot in {remaining:.1f}s",
                    (12, 56),
                    cv2.FONT_HERSHEY_SIMPLEX,
                    0.65,
                    (0, 255, 255),
                    2,
                    cv2.LINE_AA,
                )
            if result_image is not None and result_meta is not None:
                inference_ms = float(result_meta.get("inference_ms", 0.0))
                round_trip_ms = float(result_meta.get("round_trip_ms", 0.0))
                stream_fps = float(result_meta.get("stream_fps", 0.0))
                cv2.putText(
                    result_image,
                    f"BBB {inference_ms:.0f}ms | RTT {round_trip_ms:.0f}ms | {stream_fps:.1f} FPS",
                    (7, result_image.shape[0] - 10),
                    cv2.FONT_HERSHEY_SIMPLEX,
                    0.48,
                    (0, 255, 255),
                    1,
                    cv2.LINE_AA,
                )
                cv2.imshow("BBB returned pose", result_image)

            if error:
                cv2.putText(
                    display,
                    "BBB: " + error[:75],
                    (12, display.shape[0] - 16),
                    cv2.FONT_HERSHEY_SIMPLEX,
                    0.48,
                    (0, 0, 255),
                    1,
                    cv2.LINE_AA,
                )

            cv2.imshow("Laptop camera", display)
            key = cv2.waitKey(1) & 0xFF
            if key in (27, ord("q"), ord("Q")):
                break
            if args.exit_after_snapshot and snapshot_saved:
                break
    finally:
        stop_event.set()
        worker.join(timeout=2.0)
        if cropper is not None:
            cropper.close()
        camera.release()
        cv2.destroyAllWindows()


if __name__ == "__main__":
    main()
