package com.hcmute.smartaccess

object ClassroomConfig {

    const val ROOM_CODE = "A102"

    const val DEVICE_ID = "ESP32_A102"

    const val DEVICE_NAME = "A102 Classroom Door Controller"
}

fun classroomName(
    language: AppLanguage
): String {

    return appText(
        language,
        "Room ${ClassroomConfig.ROOM_CODE}",
        "Phòng ${ClassroomConfig.ROOM_CODE}"
    )
}