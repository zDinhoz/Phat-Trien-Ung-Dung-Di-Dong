# AI Hand Pose Estimation on BeagleBone Black

An edge-AI hand pose estimation system developed as part of the **Mobile Application Development** course at Ho Chi Minh City University of Technology and Education (HCMUTE).

The project implements an end-to-end workflow from model training and ONNX export to real-time hand pose inference on a BeagleBone Black and visualization on a laptop client.

---

## Overview

The system estimates **21 hand keypoints** from camera images.

The laptop captures video frames and detects the hand region before sending cropped images to a BeagleBone Black over TCP.

The BeagleBone Black performs AI inference using an ONNX hand pose model with OpenCV DNN and returns:

- 21 normalized hand keypoints
- Inference latency
- Server processing time
- An annotated JPEG image

The laptop then overlays the returned pose landmarks onto the live camera feed.

---

## System Architecture

```text
Laptop Camera
     |
     v
Hand Detection / ROI Extraction
(MediaPipe)
     |
     v
JPEG Hand Crop
     |
     | TCP/IP
     v
+-----------------------------+
|     BeagleBone Black        |
|                             |
|  OpenCV DNN                 |
|       |                     |
|       v                     |
|  ONNX Hand Pose Model       |
|       |                     |
|       v                     |
|  21 Hand Keypoints          |
+-----------------------------+
     |
     | JSON + Annotated JPEG
     v
Laptop Client
     |
     v
Real-time Pose Visualization
```

---

## AI Model

The hand pose model is built with **PyTorch** using a **MobileNetV2-based backbone**.

The network extracts multi-scale image features and produces **21 heatmaps**, one for each hand landmark.

Each heatmap is decoded into a normalized `(x, y)` coordinate representing the predicted location of the corresponding hand keypoint.

### Hand Landmarks

The model predicts:

- Wrist: 1 keypoint
- Thumb: 4 keypoints
- Index finger: 4 keypoints
- Middle finger: 4 keypoints
- Ring finger: 4 keypoints
- Pinky finger: 4 keypoints

Total:

```text
21 hand keypoints
```

---

## Model Pipeline

```text
Training Dataset
      |
      v
PyTorch Training
      |
      v
Best Model Checkpoint (.pth)
      |
      v
ONNX Export
      |
      v
ONNX Validation
      |
      v
OpenCV Compatibility Processing
      |
      v
Deployment to BeagleBone Black
```

The exported ONNX model is validated using ONNX Runtime before deployment.

Additional ONNX processing is included to improve compatibility with older OpenCV versions used on the BeagleBone Black.

---

## Edge AI Deployment

The BeagleBone Black performs the actual hand pose inference.

The server:

1. Opens a TCP server.
2. Receives a JPEG hand crop from the laptop.
3. Preprocesses the image.
4. Runs the ONNX model using OpenCV DNN.
5. Extracts 21 keypoints from the output heatmaps.
6. Draws the predicted hand skeleton.
7. Returns the prediction metadata and annotated image to the laptop.

Inference runs on the **CPU** using the OpenCV DNN backend.

---

## Laptop Client

The laptop application captures frames from a webcam and communicates with the BeagleBone Black.

### Responsibilities

- Capture webcam frames
- Detect hands using MediaPipe
- Extract and resize hand regions
- Send JPEG crops to the BeagleBone Black
- Receive hand keypoints and annotated images
- Display predictions in real time
- Measure round-trip latency and stream FPS

The client can detect up to **two hands** and alternates hand crops when sending frames to the BeagleBone Black.

To avoid increasing latency when the embedded device cannot process frames fast enough, the client keeps only the newest pending frame instead of building a large frame queue.

---

## TCP Communication

The laptop and BeagleBone Black communicate using TCP sockets.

Each packet contains:

```text
+-----------------------+
| JSON Length           |
+-----------------------+
| Binary Blob Length    |
+-----------------------+
| JSON Metadata         |
+-----------------------+
| JPEG Image Data       |
+-----------------------+
```

The laptop sends information such as:

- Frame ID
- Hand ID
- Capture timestamp
- Crop dimensions
- Crop position

The BeagleBone Black returns:

- Frame ID
- Hand ID
- Inference time
- Server processing time
- 21 normalized keypoints
- Annotated JPEG image

---

## Project Structure

```text
02_AI_Advanced_Project/
│
├── 00_Bao_cao/
│   └── Báo cáo AI.pdf
│
├── 01_Training_Kaggle/
│   ├── train_handpose_kaggle.ipynb
│   ├── handpose_accuracy.py
│   ├── export_handpose90.py
│   └── fix_onnx_for_opencv451.py
│
├── 02_BBB_Server/
│   ├── bbb_pose_server.py
│   ├── eth0.network
│   └── handpose-bbb.service
│
├── 03_Laptop_Client/
│   ├── laptop_stream_client.py
│   ├── requirements-laptop.txt
│   └── CHAY_LAPTOP.cmd
│
└── README.md
```

---

## Folder Description

### `00_Bao_cao`

Contains the project report and detailed documentation.

### `01_Training_Kaggle`

Contains the AI model training and deployment preparation code.

| File | Description |
|---|---|
| `train_handpose_kaggle.ipynb` | Model training notebook |
| `handpose_accuracy.py` | PyTorch hand pose network definition |
| `export_handpose90.py` | Exports the trained PyTorch checkpoint to ONNX |
| `fix_onnx_for_opencv451.py` | Processes the ONNX graph for compatibility with older OpenCV versions |

### `02_BBB_Server`

Contains the software and configuration required on the BeagleBone Black.

| File | Description |
|---|---|
| `bbb_pose_server.py` | TCP server and ONNX inference application |
| `eth0.network` | Ethernet network configuration |
| `handpose-bbb.service` | systemd service for automatically starting the AI server |

### `03_Laptop_Client`

Contains the laptop-side real-time client.

| File | Description |
|---|---|
| `laptop_stream_client.py` | Camera capture, hand detection, TCP communication and visualization |
| `requirements-laptop.txt` | Required Python packages |
| `CHAY_LAPTOP.cmd` | Windows launcher script |

---

## Technologies

### AI / Machine Learning

- Python
- PyTorch
- TorchVision
- MobileNetV2
- ONNX
- ONNX Runtime

### Computer Vision

- OpenCV
- MediaPipe

### Embedded / Edge AI

- BeagleBone Black
- Linux
- OpenCV DNN
- systemd

### Networking

- TCP/IP
- Python Socket Programming
- Ethernet

---

## Laptop Requirements

Install the required Python packages:

```bash
pip install -r requirements-laptop.txt
```

The current client dependencies include:

```text
NumPy
OpenCV Contrib
MediaPipe
```

A compatible MediaPipe Hand Landmarker model must also be available to the laptop client when automatic hand detection is enabled.

---

## Running the Laptop Client

From the `03_Laptop_Client` directory:

```bash
python laptop_stream_client.py --host <BBB_IP_ADDRESS> --port 5000
```

For example:

```bash
python laptop_stream_client.py --host 192.168.137.2 --port 5000
```

Press:

```text
Q
```

or:

```text
Esc
```

to stop the application.

---

## Running the BeagleBone Black Server

The server application can be started with:

```bash
python3 bbb_pose_server.py \
  --model <MODEL_PATH> \
  --host 0.0.0.0 \
  --port 5000 \
  --threads 1 \
  --image-size 128 \
  --heatmap-size 64
```

The repository also includes a `systemd` service configuration that can be used to automatically start the server when the BeagleBone Black boots.

---

## Network Configuration

The laptop client and BeagleBone Black communicate over a TCP/IP connection.

The network address is configurable depending on the deployment environment. The laptop client should be configured to connect to the IP address assigned to the BeagleBone Black.

Example:

```bash
python laptop_stream_client.py --host <BBB_IP_ADDRESS> --port 5000
```

The server listens for incoming client connections on the configured TCP port.

```text
Laptop Client
      |
      | TCP/IP
      v
BeagleBone Black
      |
      v
AI Inference Server
```

Network parameters may be adjusted according to the Ethernet or local network configuration used during deployment.

## Key Engineering Features

- End-to-end AI training and edge deployment pipeline
- MobileNetV2-based hand pose estimation
- 21-keypoint heatmap regression
- ONNX model export and validation
- Compatibility processing for embedded OpenCV
- CPU inference on BeagleBone Black
- TCP-based client-server communication
- Real-time webcam streaming
- MediaPipe-based automatic hand cropping
- Multi-hand support
- Frame dropping to reduce real-time latency
- Inference and round-trip latency measurement
- Linux `systemd` deployment

---

## Course Information

- **Course:** Mobile Application Development
- **University:** Ho Chi Minh City University of Technology and Education (HCMUTE)

---

## Author

**Nhat Dinh Ho**

GitHub: [zDinhoz](https://github.com/zDinhoz)
