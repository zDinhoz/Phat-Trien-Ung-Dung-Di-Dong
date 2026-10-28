# HCM-UTE Smart Access

HCM-UTE Smart Access is a smart classroom access management system for **Room A102**, using **NFC cards or fingerprint authentication**.

The system integrates:

- Android Application
- Firebase Authentication
- Cloud Firestore
- ESP32
- RC522 NFC Reader
- Fingerprint Sensor
- Servo Door Controller

The Android application is developed using **Kotlin + Jetpack Compose**.

---

## System Architecture

```text
          ┌─────────────────────────┐
          │   Android Application   │
          │ Kotlin + Jetpack Compose│
          └────────────┬────────────┘
                       │
                       ▼
          ┌─────────────────────────┐
          │ Firebase Authentication │
          │    Cloud Firestore      │
          └────────────┬────────────┘
                       │
                       ▼
          ┌─────────────────────────┐
          │       ESP32_A102        │
          │    Access Controller    │
          └───────────┬─────────────┘
                      │
              ┌───────┴───────┐
              │               │
              ▼               ▼
       ┌────────────┐   ┌──────────────┐
       │ RC522 NFC  │   │ Fingerprint  │
       │   Reader   │   │    Sensor    │
       └──────┬─────┘   └──────┬───────┘
              │                │
              └───────┬────────┘
                      │
                      ▼
               ┌────────────┐
               │Servo / Door│
               └────────────┘
```

The Android application does not directly control the door.  
NFC and fingerprint authentication are processed by **ESP32_A102**.

---

## Main Features

### Student

- Account registration and login
- Wait for Admin approval
- NFC enrollment
- Fingerprint enrollment
- NFC / fingerprint removal
- View credential status
- View device and door status
- View access history
- View attendance information
- English / Vietnamese support
- Light / Dark theme

### Admin

- Approve Student accounts
- View Admin Dashboard
- Monitor access logs
- Monitor ESP32 and sensors
- Manage Student credentials
- Revoke NFC credentials
- Delete fingerprint credentials
- Create and manage attendance sessions
- View attendance records

---

## Authentication

The system supports:

```text
NFC OR Fingerprint
```

NFC and fingerprint can be used independently.

Fingerprint templates are stored directly in the fingerprint sensor. Firestore only stores the enrollment status and fingerprint slot.

---

## Firebase Collections

```text
users/
credentials/
devices/
enrollment_requests/
nfc_lookup/
fingerprint_lookup/
access_logs/
attendance_sessions/
attendance_runtime/
attendance_records/
```

The application uses `SnapshotListener` to synchronize Firestore data with the Jetpack Compose UI in near real time.

---

## Android Architecture

The current application uses:

```text
Single Activity
      │
      ▼
MainActivity
      │
      ▼
currentScreen
      │
      ├── Login
      ├── Register
      ├── Pending Approval
      ├── Student Dashboard
      ├── Admin Dashboard
      ├── Access Logs
      ├── Device Management
      ├── Credential Management
      ├── Attendance Management
      └── Settings
```

The current version uses **Stateful Jetpack Compose Screens** with direct Firebase access.

MVVM + Repository is planned as a future improvement.

---

## Source Code Structure

```text
app/src/main/
│
├── AndroidManifest.xml
│
├── java/com/hcmute/smartaccess/
│   ├── MainActivity.kt
│   ├── LoginScreen.kt
│   ├── RegisterScreen.kt
│   ├── PendingApprovalScreen.kt
│   ├── StudentHomeScreen.kt
│   ├── AdminDashboardScreen.kt
│   ├── PendingStudentsScreen.kt
│   ├── AccessLogsScreen.kt
│   ├── DeviceManagementScreen.kt
│   ├── CredentialManagementScreen.kt
│   ├── AttendanceManagementScreen.kt
│   ├── SettingsScreen.kt
│   ├── AppLanguage.kt
│   ├── ClassroomConfig.kt
│   └── ui/theme/
│
└── res/
```

---

## Access Flow

```text
User
  │
  ▼
NFC / Fingerprint
  │
  ▼
ESP32_A102
  │
  ▼
Credential Validation
  │
  ├── Valid
  │     ├── ACCESS GRANTED
  │     ├── Open Servo
  │     └── Write Access Log
  │
  └── Invalid
        ├── ACCESS DENIED
        └── Write Access Log
```

When access is granted, the servo opens and automatically closes after approximately 3 seconds.

---

## Attendance

Admin can create and manage attendance sessions.

When a session is active and a Student successfully authenticates, ESP32 can create an attendance record with:

```text
present
late
```

Duplicate attendance records are prevented using the session and student identifiers.

---

## Video Demo

### Student Account Registration

https://youtu.be/G3_Ac4GDy2A

### Admin Approval of Student Account

https://youtu.be/rPZDO_zdbOs

### NFC and Fingerprint Enrollment

https://youtu.be/lbneo9DQlTE

### NFC and Fingerprint Removal

https://youtu.be/264U0LPDeXI

### Admin Attendance Session Creation

https://youtu.be/vrL58Vwm7Xg

---

## Project Report

The complete project report is available here:

[View HCM-UTE Smart Access Project Report](docs/Report_Final_Di_động.pdf)

---

## Run the Project

Clone the repository:

```bash
git clone https://github.com/zDinhoz/HCMUTE_Smart_Access.git
```

Open the project using Android Studio.

Then:

1. Sync Gradle.
2. Check Firebase configuration.
3. Connect an Android device or start an Emulator.
4. Run the `app` module.

Firebase configuration:

```text
app/google-services.json
```

---

## Build APK

In Android Studio:

```text
Build
→ Build Bundle(s) / APK(s)
→ Build APK(s)
```

Debug APK:

```text
app/build/outputs/apk/debug/app-debug.apk
```

---

## Current Limitations

- Currently designed mainly for Room A102.
- Enrollment uses a single request document for ESP32_A102.
- Multiple concurrent enrollment requests are not fully supported.
- The Android application has not yet been refactored to MVVM / Repository.
- No dedicated trusted backend is currently used.
- Device heartbeat and `lastSeen` monitoring are not fully implemented.
- Large-scale load and latency testing has not yet been performed.

---

## Project Information

**Project:** HCM-UTE Smart Access  
**Course:** Mobile Application Development  
**Platform:** Android  
**Language:** Kotlin  
**UI:** Jetpack Compose  
**Backend:** Firebase  
**Embedded Controller:** ESP32  
**Authentication:** NFC / Fingerprint  
**Target Room:** A102
