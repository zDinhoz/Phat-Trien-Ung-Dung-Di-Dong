@file:Suppress("SpellCheckingInspection")

package com.hcmute.smartaccess

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SettingsInputAntenna
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds


data class StudentAccessLog(
    val location: String = "",
    val deviceId: String = "",
    val authenticationMethod: String = "",
    val result: String = "",
    val timestamp: Timestamp? = null
)


data class StudentEnrollmentState(
    val action: String = "",
    val status: String = "",
    val message: String = "",
    val userId: String = "",
    val credentialValue: String = "",
    val fingerprintSlot: Long = 0L,
    val requestedAt: Timestamp? = null
)


data class StudentAccessNotice(
    val success: Boolean,
    val title: String,
    val location: String,
    val method: String,
    val time: String
)


@Composable
fun StudentHomeScreen(
    language: AppLanguage,
    onOpenSettings: () -> Unit
) {

    val firestore =
        FirebaseFirestore.getInstance()

    val currentUser =
        FirebaseAuth.getInstance().currentUser

    val coroutineScope =
        rememberCoroutineScope()

    val toneGenerator =
        remember {
            ToneGenerator(
                AudioManager.STREAM_NOTIFICATION,
                100
            )
        }


    DisposableEffect(toneGenerator) {
        onDispose {
            toneGenerator.release()
        }
    }


    // =====================================================
    // PROFILE
    // =====================================================

    var fullName by remember {
        mutableStateOf("")
    }

    var studentId by remember {
        mutableStateOf("")
    }

    var email by remember {
        mutableStateOf("")
    }

    var accountStatus by remember {
        mutableStateOf("")
    }

    var role by remember {
        mutableStateOf("")
    }


    // =====================================================
    // CREDENTIALS
    // =====================================================

    var nfcUid by remember {
        mutableStateOf("")
    }

    var nfcStatus by remember {
        mutableStateOf("not linked")
    }

    var fingerprintEnrolled by remember {
        mutableStateOf(false)
    }

    var fingerprintSlot by remember {
        mutableLongStateOf(0L)
    }


    // =====================================================
    // DEVICE / DOOR
    // =====================================================

    var deviceOnline by remember {
        mutableStateOf(false)
    }

    var doorStatus by remember {
        mutableStateOf("closed")
    }

    var doorStatusFromFirestore by remember {
        mutableStateOf(false)
    }

    var simulatedDoorEventId by remember {
        mutableIntStateOf(0)
    }


    // =====================================================
    // GENERAL
    // =====================================================

    var isLoading by remember {
        mutableStateOf(true)
    }

    var errorMessage by remember {
        mutableStateOf("")
    }

    var credentialActionNotice by remember {
        mutableStateOf("")
    }

    var deleteTarget by remember {
        mutableStateOf<String?>(null)
    }

    var showAllHistory by remember {
        mutableStateOf(false)
    }


    // =====================================================
    // ENROLLMENT
    // =====================================================

    var requestSubmitting by remember {
        mutableStateOf(false)
    }

    var enrollmentState by remember {
        mutableStateOf(
            StudentEnrollmentState()
        )
    }

    var activeEnrollmentAction by remember {
        mutableStateOf("")
    }


    LaunchedEffect(
        credentialActionNotice
    ) {

        if (
            credentialActionNotice.isNotBlank()
        ) {

            delay(6.seconds)

            credentialActionNotice =
                ""
        }
    }


    val ownEnrollmentRequest =
        enrollmentState.userId ==
                currentUser?.uid


    val requestWaitingOrProcessing =
        enrollmentState.status == "waiting" ||
                enrollmentState.status == "processing"


    val ownRequestBusy =
        ownEnrollmentRequest &&
                requestWaitingOrProcessing


    // =====================================================
    // ACCESS
    // =====================================================

    val accessLogs =
        remember {
            mutableStateListOf<StudentAccessLog>()
        }

    var accessToday by remember {
        mutableIntStateOf(0)
    }

    var grantedToday by remember {
        mutableIntStateOf(0)
    }

    var deniedToday by remember {
        mutableIntStateOf(0)
    }

    var accessNotice by remember {
        mutableStateOf<StudentAccessNotice?>(null)
    }


    // =====================================================
    // ATTENDANCE BACKGROUND
    // =====================================================

    var activeAttendanceSessionId by remember {
        mutableStateOf("")
    }

    var attendanceNotice by remember {
        mutableStateOf("")
    }


    LaunchedEffect(accessNotice) {

        if (
            accessNotice != null
        ) {

            delay(5.seconds)

            accessNotice =
                null
        }
    }


    LaunchedEffect(attendanceNotice) {

        if (
            attendanceNotice.isNotBlank()
        ) {

            delay(5.seconds)

            attendanceNotice =
                ""
        }
    }


    // =====================================================
    // HELPERS
    // =====================================================

    fun timestampAfter(
        value: Timestamp?,
        reference: Timestamp
    ): Boolean {

        if (
            value == null
        ) {
            return false
        }

        return value.seconds > reference.seconds ||
                (
                        value.seconds == reference.seconds &&
                                value.nanoseconds > reference.nanoseconds
                        )
    }


    fun formatTime(
        timestamp: Timestamp?
    ): String {

        if (
            timestamp == null
        ) {
            return "--:--:--"
        }

        return SimpleDateFormat(
            "HH:mm:ss",
            Locale.getDefault()
        ).format(
            timestamp.toDate()
        )
    }


    fun methodText(
        method: String
    ): String {

        return when (
            method.lowercase()
        ) {

            "nfc" ->
                "NFC"

            "fingerprint" ->
                appText(
                    language,
                    "Fingerprint",
                    "Vân tay"
                )

            else ->
                appText(
                    language,
                    "Unknown",
                    "Không xác định"
                )
        }
    }


    fun normalizedDoorStatus(
        value: String
    ): String {

        return when (
            value.trim().lowercase()
        ) {

            "open",
            "opened",
            "unlock",
            "unlocked" ->
                "open"

            "close",
            "closed",
            "lock",
            "locked" ->
                "closed"

            else ->
                "unknown"
        }
    }


    fun simulateDoorOpen() {

        if (
            doorStatusFromFirestore
        ) {
            return
        }

        simulatedDoorEventId +=
            1

        val eventId =
            simulatedDoorEventId

        doorStatus =
            "open"

        coroutineScope.launch {

            delay(3.seconds)

            if (
                !doorStatusFromFirestore &&
                eventId == simulatedDoorEventId
            ) {

                doorStatus =
                    "closed"
            }
        }
    }


    fun playSuccessTone() {

        coroutineScope.launch {

            toneGenerator.startTone(
                ToneGenerator.TONE_PROP_BEEP,
                120
            )

            delay(180.milliseconds)

            toneGenerator.startTone(
                ToneGenerator.TONE_PROP_BEEP,
                120
            )
        }
    }


    fun mapEnrollmentDocument(
        document: DocumentSnapshot
    ): StudentEnrollmentState {

        return StudentEnrollmentState(

            action =
                document.getString(
                    "action"
                ) ?: "",

            status =
                document.getString(
                    "status"
                ) ?: "",

            message =
                document.getString(
                    "message"
                ) ?: "",

            userId =
                document.getString(
                    "userId"
                ) ?: "",

            credentialValue =
                document.getString(
                    "credentialValue"
                ) ?: "",

            fingerprintSlot =
                document.getLong(
                    "fingerprintSlot"
                ) ?: 0L,

            requestedAt =
                document.getTimestamp(
                    "requestedAt"
                )
        )
    }


    // =====================================================
    // PROFILE
    // =====================================================

    DisposableEffect(
        currentUser?.uid,
        language
    ) {

        if (
            currentUser == null
        ) {

            isLoading =
                false

            errorMessage =
                appText(
                    language,
                    "User is not logged in.",
                    "Người dùng chưa đăng nhập."
                )

            onDispose { }

        } else {

            val listener =
                firestore
                    .collection("users")
                    .document(currentUser.uid)
                    .addSnapshotListener {
                            document,
                            error ->

                        if (
                            error != null
                        ) {

                            isLoading =
                                false

                            errorMessage =
                                appText(
                                    language,
                                    "Failed to load student profile.",
                                    "Không thể tải hồ sơ sinh viên."
                                )

                            return@addSnapshotListener
                        }

                        if (
                            document == null ||
                            !document.exists()
                        ) {

                            isLoading =
                                false

                            errorMessage =
                                appText(
                                    language,
                                    "Student profile not found.",
                                    "Không tìm thấy hồ sơ sinh viên."
                                )

                            return@addSnapshotListener
                        }

                        fullName =
                            document.getString(
                                "fullName"
                            ) ?: "Student"

                        studentId =
                            document.getString(
                                "studentId"
                            ) ?: "N/A"

                        email =
                            document.getString(
                                "email"
                            )
                                ?: document.getString(
                                    "e-mail"
                                )
                                        ?: currentUser.email
                                        ?: "N/A"

                        accountStatus =
                            document.getString(
                                "status"
                            ) ?: "unknown"

                        role =
                            document.getString(
                                "role"
                            ) ?: ""

                        isLoading =
                            false
                    }

            onDispose {
                listener.remove()
            }
        }
    }


    // =====================================================
    // CREDENTIALS
    // =====================================================

    DisposableEffect(
        currentUser?.uid
    ) {

        if (
            currentUser == null
        ) {

            onDispose { }

        } else {

            val listener =
                firestore
                    .collection("credentials")
                    .document(currentUser.uid)
                    .addSnapshotListener {
                            document,
                            _ ->

                        if (
                            document != null &&
                            document.exists()
                        ) {

                            nfcUid =
                                document.getString(
                                    "nfcUid"
                                ) ?: ""

                            nfcStatus =
                                document.getString(
                                    "nfcStatus"
                                ) ?: "not linked"

                            fingerprintEnrolled =
                                document.getBoolean(
                                    "fingerprintEnrolled"
                                ) ?: false

                            fingerprintSlot =
                                document.getLong(
                                    "fingerprintSlot"
                                ) ?: 0L

                        } else {

                            nfcUid =
                                ""

                            nfcStatus =
                                "not linked"

                            fingerprintEnrolled =
                                false

                            fingerprintSlot =
                                0L
                        }
                    }

            onDispose {
                listener.remove()
            }
        }
    }


    // =====================================================
    // DEVICE + REAL DOOR STATUS
    // =====================================================

    DisposableEffect(Unit) {

        val listener =
            firestore
                .collection("devices")
                .document(
                    ClassroomConfig.DEVICE_ID
                )
                .addSnapshotListener {
                        document,
                        _ ->

                    deviceOnline =
                        document != null &&
                                document.exists() &&
                                document.getString(
                                    "status"
                                ).equals(
                                    "online",
                                    true
                                )

                    val cloudDoorStatus =
                        document
                            ?.getString(
                                "doorStatus"
                            )
                            ?.trim()

                    if (
                        !cloudDoorStatus.isNullOrBlank()
                    ) {

                        doorStatusFromFirestore =
                            true

                        doorStatus =
                            normalizedDoorStatus(
                                cloudDoorStatus
                            )

                    } else {

                        doorStatusFromFirestore =
                            false
                    }
                }

        onDispose {
            listener.remove()
        }
    }


    // =====================================================
    // ENROLLMENT REQUEST LISTENER
    // =====================================================

    DisposableEffect(
        currentUser?.uid
    ) {

        val uid =
            currentUser?.uid

        if (
            uid == null
        ) {

            enrollmentState =
                StudentEnrollmentState()

            activeEnrollmentAction =
                ""

            requestSubmitting =
                false

            errorMessage =
                ""

            onDispose { }

        } else {

            enrollmentState =
                StudentEnrollmentState()

            activeEnrollmentAction =
                ""

            requestSubmitting =
                false

            errorMessage =
                ""

            credentialActionNotice =
                ""

            val enrollmentReference =
                firestore
                    .collection(
                        "enrollment_requests"
                    )
                    .document(
                        ClassroomConfig.DEVICE_ID
                    )

            val listener =
                enrollmentReference
                    .addSnapshotListener {
                            document,
                            error ->

                        if (
                            error != null
                        ) {

                            requestSubmitting =
                                false

                            return@addSnapshotListener
                        }

                        if (
                            document == null ||
                            !document.exists()
                        ) {

                            enrollmentState =
                                StudentEnrollmentState()

                            activeEnrollmentAction =
                                ""

                            requestSubmitting =
                                false

                            return@addSnapshotListener
                        }

                        val requestUserId =
                            document.getString(
                                "userId"
                            ) ?: ""

                        if (
                            requestUserId !=
                            uid
                        ) {

                            enrollmentState =
                                StudentEnrollmentState()

                            activeEnrollmentAction =
                                ""

                            requestSubmitting =
                                false

                            return@addSnapshotListener
                        }

                        val updatedState =
                            mapEnrollmentDocument(
                                document
                            )

                        when (
                            updatedState.status
                        ) {

                            "waiting",
                            "processing" -> {

                                enrollmentState =
                                    updatedState

                                if (
                                    activeEnrollmentAction.isBlank()
                                ) {

                                    activeEnrollmentAction =
                                        updatedState.action
                                }

                                requestSubmitting =
                                    false
                            }

                            "completed" -> {

                                if (
                                    activeEnrollmentAction ==
                                    updatedState.action
                                ) {

                                    credentialActionNotice =
                                        when (
                                            updatedState.action
                                        ) {

                                            "enroll_nfc" ->
                                                appText(
                                                    language,
                                                    "NFC card registered successfully.",
                                                    "Đăng ký thẻ NFC thành công."
                                                )

                                            "delete_nfc" ->
                                                appText(
                                                    language,
                                                    "NFC card removed successfully.",
                                                    "Xóa thẻ NFC thành công."
                                                )

                                            "enroll_fingerprint" ->
                                                appText(
                                                    language,
                                                    "Fingerprint registered successfully.",
                                                    "Đăng ký vân tay thành công."
                                                )

                                            "delete_fingerprint" ->
                                                appText(
                                                    language,
                                                    "Fingerprint removed successfully.",
                                                    "Xóa vân tay thành công."
                                                )

                                            else ->
                                                appText(
                                                    language,
                                                    "Credential updated successfully.",
                                                    "Cập nhật xác thực thành công."
                                                )
                                        }
                                }

                                errorMessage =
                                    ""

                                enrollmentState =
                                    StudentEnrollmentState()

                                activeEnrollmentAction =
                                    ""

                                requestSubmitting =
                                    false
                            }

                            "failed" -> {

                                errorMessage =
                                    ""

                                enrollmentState =
                                    updatedState

                                activeEnrollmentAction =
                                    ""

                                requestSubmitting =
                                    false
                            }

                            "cancelled" -> {

                                errorMessage =
                                    ""

                                enrollmentState =
                                    StudentEnrollmentState()

                                activeEnrollmentAction =
                                    ""

                                requestSubmitting =
                                    false
                            }

                            else -> {

                                enrollmentState =
                                    updatedState

                                requestSubmitting =
                                    false
                            }
                        }
                    }

            onDispose {
                listener.remove()
            }
        }
    }


    // =====================================================
    // ATTENDANCE RUNTIME
    // =====================================================

    DisposableEffect(
        currentUser?.uid
    ) {

        if (
            currentUser == null
        ) {

            onDispose { }

        } else {

            val listener =
                firestore
                    .collection(
                        "attendance_runtime"
                    )
                    .document(
                        ClassroomConfig.DEVICE_ID
                    )
                    .addSnapshotListener {
                            document,
                            error ->

                        if (
                            error != null ||
                            document == null ||
                            !document.exists()
                        ) {

                            activeAttendanceSessionId =
                                ""

                            return@addSnapshotListener
                        }

                        val runtimeStatus =
                            document.getString(
                                "status"
                            ) ?: ""

                        activeAttendanceSessionId =
                            if (
                                runtimeStatus.equals(
                                    "open",
                                    true
                                )
                            ) {

                                document.getString(
                                    "activeSessionId"
                                ) ?: ""

                            } else {

                                ""
                            }
                    }

            onDispose {
                listener.remove()
            }
        }
    }


    DisposableEffect(
        currentUser?.uid,
        activeAttendanceSessionId,
        language
    ) {

        val uid =
            currentUser?.uid

        if (
            uid == null ||
            activeAttendanceSessionId.isBlank()
        ) {

            onDispose { }

        } else {

            val listenerStartedAt =
                Timestamp.now()

            val listener =
                firestore
                    .collection(
                        "attendance_records"
                    )
                    .whereEqualTo(
                        "userId",
                        uid
                    )
                    .addSnapshotListener {
                            snapshot,
                            error ->

                        if (
                            error != null
                        ) {
                            return@addSnapshotListener
                        }

                        snapshot
                            ?.documentChanges
                            ?.filter {
                                it.type ==
                                        DocumentChange.Type.ADDED
                            }
                            ?.forEach { change ->

                                val document =
                                    change.document

                                if (
                                    document.getString(
                                        "sessionId"
                                    ) !=
                                    activeAttendanceSessionId
                                ) {
                                    return@forEach
                                }

                                val checkInAt =
                                    document.getTimestamp(
                                        "checkInAt"
                                    )

                                if (
                                    !timestampAfter(
                                        checkInAt,
                                        listenerStartedAt
                                    )
                                ) {
                                    return@forEach
                                }

                                attendanceNotice =
                                    if (
                                        document
                                            .getString(
                                                "status"
                                            )
                                            .equals(
                                                "late",
                                                true
                                            )
                                    ) {

                                        appText(
                                            language,
                                            "ATTENDANCE RECORDED • LATE",
                                            "ĐÃ ĐIỂM DANH • ĐI MUỘN"
                                        )

                                    } else {

                                        appText(
                                            language,
                                            "ATTENDANCE RECORDED",
                                            "ĐIỂM DANH THÀNH CÔNG"
                                        )
                                    }
                            }
                    }

            onDispose {
                listener.remove()
            }
        }
    }


    // =====================================================
    // ACCESS LOG
    // =====================================================

    DisposableEffect(
        studentId,
        language
    ) {

        if (
            studentId.isBlank() ||
            studentId == "N/A"
        ) {

            accessLogs.clear()

            accessToday = 0
            grantedToday = 0
            deniedToday = 0

            onDispose { }

        } else {

            val listenerStartedAt =
                Timestamp.now()

            val processedIds =
                mutableSetOf<String>()

            val listener =
                firestore
                    .collection("access_logs")
                    .whereEqualTo(
                        "studentId",
                        studentId
                    )
                    .addSnapshotListener {
                            snapshot,
                            error ->

                        if (
                            error != null
                        ) {

                            errorMessage =
                                appText(
                                    language,
                                    "Failed to load classroom access history.",
                                    "Không thể tải lịch sử ra vào phòng học."
                                )

                            return@addSnapshotListener
                        }

                        val roomDocuments =
                            snapshot
                                ?.documents
                                ?.filter { document ->

                                    document.getString(
                                        "deviceId"
                                    ) ==
                                            ClassroomConfig.DEVICE_ID
                                }
                                ?: emptyList()

                        val calendar =
                            Calendar.getInstance()

                        calendar.set(
                            Calendar.HOUR_OF_DAY,
                            0
                        )

                        calendar.set(
                            Calendar.MINUTE,
                            0
                        )

                        calendar.set(
                            Calendar.SECOND,
                            0
                        )

                        calendar.set(
                            Calendar.MILLISECOND,
                            0
                        )

                        val startOfDay =
                            Timestamp(
                                calendar.time
                            )

                        val todayDocuments =
                            roomDocuments.filter {

                                val timestamp =
                                    it.getTimestamp(
                                        "timestamp"
                                    )

                                timestamp != null &&
                                        timestamp.seconds >=
                                        startOfDay.seconds
                            }

                        accessToday =
                            todayDocuments.size

                        grantedToday =
                            todayDocuments.count {

                                it.getString(
                                    "result"
                                ).equals(
                                    "granted",
                                    true
                                )
                            }

                        deniedToday =
                            todayDocuments.count {

                                it.getString(
                                    "result"
                                ).equals(
                                    "denied",
                                    true
                                )
                            }

                        accessLogs.clear()

                        accessLogs.addAll(

                            roomDocuments
                                .map { document ->

                                    val method =
                                        document.getString(
                                            "authenticationMethod"
                                        )
                                            ?: when {

                                                document.getBoolean(
                                                    "nfcResult"
                                                ) == true ->
                                                    "nfc"

                                                document.getBoolean(
                                                    "fingerprintResult"
                                                ) == true ->
                                                    "fingerprint"

                                                else ->
                                                    "unknown"
                                            }

                                    StudentAccessLog(

                                        location =
                                            document.getString(
                                                "location"
                                            )
                                                ?: classroomName(
                                                    language
                                                ),

                                        deviceId =
                                            document.getString(
                                                "deviceId"
                                            )
                                                ?: ClassroomConfig.DEVICE_ID,

                                        authenticationMethod =
                                            method,

                                        result =
                                            document.getString(
                                                "result"
                                            ) ?: "unknown",

                                        timestamp =
                                            document.getTimestamp(
                                                "timestamp"
                                            )
                                    )
                                }
                                .sortedByDescending {
                                    it.timestamp?.seconds ?: 0L
                                }
                        )

                        snapshot
                            ?.documentChanges
                            ?.filter {
                                it.type ==
                                        DocumentChange.Type.ADDED
                            }
                            ?.forEach { change ->

                                val document =
                                    change.document

                                if (
                                    document.getString(
                                        "deviceId"
                                    ) !=
                                    ClassroomConfig.DEVICE_ID
                                ) {
                                    return@forEach
                                }

                                if (
                                    document.id in
                                    processedIds
                                ) {
                                    return@forEach
                                }

                                processedIds.add(
                                    document.id
                                )

                                val timestamp =
                                    document.getTimestamp(
                                        "timestamp"
                                    )

                                if (
                                    !timestampAfter(
                                        timestamp,
                                        listenerStartedAt
                                    )
                                ) {
                                    return@forEach
                                }

                                val result =
                                    document.getString(
                                        "result"
                                    ) ?: "unknown"

                                val method =
                                    document.getString(
                                        "authenticationMethod"
                                    )
                                        ?: when {

                                            document.getBoolean(
                                                "nfcResult"
                                            ) == true ->
                                                "nfc"

                                            document.getBoolean(
                                                "fingerprintResult"
                                            ) == true ->
                                                "fingerprint"

                                            else ->
                                                "unknown"
                                        }

                                val granted =
                                    result.equals(
                                        "granted",
                                        true
                                    )

                                accessNotice =
                                    StudentAccessNotice(

                                        success =
                                            granted,

                                        title =
                                            if (
                                                granted
                                            ) {

                                                appText(
                                                    language,
                                                    "ACCESS GRANTED",
                                                    "TRUY CẬP THÀNH CÔNG"
                                                )

                                            } else {

                                                appText(
                                                    language,
                                                    "ACCESS DENIED",
                                                    "TRUY CẬP BỊ TỪ CHỐI"
                                                )
                                            },

                                        location =
                                            appText(
                                                language,
                                                "Room A102",
                                                "Phòng A102"
                                            ),

                                        method =
                                            methodText(
                                                method
                                            ),

                                        time =
                                            formatTime(
                                                timestamp
                                            )
                                    )

                                if (
                                    granted
                                ) {

                                    playSuccessTone()

                                    simulateDoorOpen()
                                }
                            }
                    }

            onDispose {
                listener.remove()
            }
        }
    }


    // =====================================================
    // DERIVED STATE
    // =====================================================

    val enrollmentAllowed =
        currentUser != null &&
                role.equals(
                    "student",
                    true
                ) &&
                accountStatus.equals(
                    "active",
                    true
                )


    val accountActive =
        accountStatus.equals(
            "active",
            true
        )


    val nfcLinked =
        nfcStatus.equals(
            "linked",
            true
        ) &&
                nfcUid.isNotBlank()


    val credentialCount =
        (if (nfcLinked) 1 else 0) +
                (if (fingerprintEnrolled) 1 else 0)


    val accessReady =
        accountActive &&
                credentialCount > 0


    val lastAccess =
        accessLogs.firstOrNull()


    val visibleAccessLogs =
        if (
            showAllHistory
        ) {

            accessLogs.toList()

        } else {

            accessLogs.take(5)
        }


    // =====================================================
    // ENROLLMENT REQUEST
    // =====================================================

    fun submitDeviceRequest(
        action: String,
        fingerprintSlotValue: Long = 0L,
        credentialValue: String = ""
    ) {

        val uid =
            currentUser?.uid
                ?: return


        if (
            !enrollmentAllowed
        ) {

            errorMessage =
                appText(
                    language,
                    "Admin approval is required before credential management.",
                    "Bạn cần được Admin cấp quyền trước khi quản lý xác thực."
                )

            return
        }


        if (
            ownRequestBusy ||
            requestSubmitting
        ) {

            errorMessage =
                appText(
                    language,
                    "Another credential request is still being processed.",
                    "Một yêu cầu xác thực khác vẫn đang được xử lý."
                )

            return
        }


        if (
            studentId.isBlank() ||
            studentId == "N/A" ||
            fullName.isBlank()
        ) {

            errorMessage =
                appText(
                    language,
                    "Student profile is not ready.",
                    "Thông tin sinh viên chưa sẵn sàng."
                )

            return
        }


        if (
            action == "delete_nfc" &&
            credentialValue.isBlank()
        ) {

            errorMessage =
                appText(
                    language,
                    "NFC credential is invalid.",
                    "Thông tin thẻ NFC không hợp lệ."
                )

            return
        }


        if (
            action == "delete_fingerprint" &&
            fingerprintSlotValue <= 0L
        ) {

            errorMessage =
                appText(
                    language,
                    "Fingerprint slot is invalid.",
                    "Vị trí vân tay không hợp lệ."
                )

            return
        }


        requestSubmitting =
            true

        activeEnrollmentAction =
            action

        errorMessage =
            ""

        credentialActionNotice =
            ""


        val requestData: Map<String, Any?> =
            hashMapOf(

                "deviceId" to
                        ClassroomConfig.DEVICE_ID,

                "room" to
                        "A102",

                "action" to
                        action,

                "userId" to
                        uid,

                "studentId" to
                        studentId,

                "fullName" to
                        fullName,

                "status" to
                        "waiting",

                "fingerprintSlot" to
                        fingerprintSlotValue,

                "credentialValue" to
                        credentialValue,

                "message" to
                        "",

                "requestedAt" to
                        FieldValue.serverTimestamp(),

                "completedAt" to
                        null
            )


        firestore
            .collection(
                "enrollment_requests"
            )
            .document(
                ClassroomConfig.DEVICE_ID
            )
            .set(
                requestData
            )
            .addOnSuccessListener {

                requestSubmitting =
                    false

                enrollmentState =
                    StudentEnrollmentState(

                        action =
                            action,

                        status =
                            "waiting",

                        userId =
                            uid,

                        credentialValue =
                            credentialValue,

                        fingerprintSlot =
                            fingerprintSlotValue,

                        requestedAt =
                            Timestamp.now()
                    )
            }
            .addOnFailureListener { error ->

                requestSubmitting =
                    false

                activeEnrollmentAction =
                    ""

                enrollmentState =
                    StudentEnrollmentState()

                errorMessage =
                    appText(
                        language,
                        "Unable to send request: ${error.message}",
                        "Không thể gửi yêu cầu: ${error.message}"
                    )
            }
    }


    fun cancelEnrollmentRequest() {

        val uid =
            currentUser?.uid
                ?: return


        if (
            enrollmentState.userId != uid ||
            enrollmentState.status != "waiting"
        ) {

            return
        }


        requestSubmitting =
            true

        errorMessage =
            ""


        val enrollmentReference =
            firestore
                .collection(
                    "enrollment_requests"
                )
                .document(
                    ClassroomConfig.DEVICE_ID
                )


        enrollmentReference
            .update(
                mapOf(

                    "status" to
                            "cancelled",

                    "message" to
                            "Cancelled by student",

                    "completedAt" to
                            FieldValue.serverTimestamp()
                )
            )
            .addOnSuccessListener {

                requestSubmitting =
                    false

                activeEnrollmentAction =
                    ""

                enrollmentState =
                    StudentEnrollmentState()

                errorMessage =
                    ""
            }
            .addOnFailureListener {

                requestSubmitting =
                    false

                errorMessage =
                    ""

                enrollmentReference
                    .get()
                    .addOnSuccessListener { document ->

                        if (
                            !document.exists() ||
                            document.getString(
                                "userId"
                            ) != uid
                        ) {

                            enrollmentState =
                                StudentEnrollmentState()

                            activeEnrollmentAction =
                                ""

                            return@addOnSuccessListener
                        }

                        val latestState =
                            mapEnrollmentDocument(
                                document
                            )

                        enrollmentState =
                            latestState

                        if (
                            latestState.status ==
                            "waiting" ||
                            latestState.status ==
                            "processing"
                        ) {

                            activeEnrollmentAction =
                                latestState.action

                        } else {

                            activeEnrollmentAction =
                                ""
                        }
                    }
            }
    }


    // =====================================================
    // DELETE CONFIRM DIALOG
    // =====================================================

    deleteTarget?.let { target ->

        val fingerprintTarget =
            target == "fingerprint"


        AlertDialog(
            onDismissRequest = {
                deleteTarget = null
            },
            title = {

                Text(
                    text =
                        if (
                            fingerprintTarget
                        ) {

                            appText(
                                language,
                                "Remove fingerprint?",
                                "Xóa vân tay?"
                            )

                        } else {

                            appText(
                                language,
                                "Remove NFC card?",
                                "Xóa thẻ NFC?"
                            )
                        },
                    fontWeight =
                        FontWeight.Bold
                )
            },
            text = {

                Text(
                    text =
                        if (
                            fingerprintTarget
                        ) {

                            appText(
                                language,
                                "The fingerprint will be removed from both the A102 sensor and your account.",
                                "Vân tay sẽ được xóa khỏi cảm biến A102 và tài khoản của bạn."
                            )

                        } else {

                            appText(
                                language,
                                "This NFC card will no longer be allowed to access Room A102.",
                                "Thẻ NFC này sẽ không còn được phép truy cập phòng A102."
                            )
                        }
                )
            },
            confirmButton = {

                TextButton(
                    onClick = {

                        deleteTarget =
                            null

                        if (
                            fingerprintTarget
                        ) {

                            submitDeviceRequest(
                                action =
                                    "delete_fingerprint",

                                fingerprintSlotValue =
                                    fingerprintSlot,

                                credentialValue =
                                    fingerprintSlot.toString()
                            )

                        } else {

                            submitDeviceRequest(
                                action =
                                    "delete_nfc",

                                fingerprintSlotValue =
                                    0L,

                                credentialValue =
                                    nfcUid.trim()
                            )
                        }
                    }
                ) {

                    Text(
                        text =
                            appText(
                                language,
                                "REMOVE",
                                "XÓA"
                            ),
                        color =
                            MaterialTheme
                                .colorScheme
                                .error,
                        fontWeight =
                            FontWeight.Bold
                    )
                }
            },
            dismissButton = {

                TextButton(
                    onClick = {
                        deleteTarget = null
                    }
                ) {

                    Text(
                        text =
                            appText(
                                language,
                                "CANCEL",
                                "HỦY"
                            )
                    )
                }
            }
        )
    }


    // =====================================================
    // UI
    // =====================================================

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                MaterialTheme
                    .colorScheme
                    .background
            )
            .windowInsetsPadding(
                WindowInsets.safeDrawing
            )
            .verticalScroll(
                rememberScrollState()
            )
            .padding(
                horizontal =
                    20.dp,
                vertical =
                    14.dp
            )
    ) {


        // =================================================
        // HEADER
        // =================================================

        Row(
            modifier =
                Modifier.fillMaxWidth(),
            verticalAlignment =
                Alignment.CenterVertically
        ) {


            Image(
                painter =
                    painterResource(
                        id =
                            R.drawable.hcmute_logo
                    ),
                contentDescription =
                    "HCM-UTE Logo",
                modifier =
                    Modifier.size(
                        48.dp
                    )
            )


            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(
                        start =
                            12.dp
                    )
            ) {


                Text(
                    text =
                        "HCM-UTE Smart Access",
                    fontSize =
                        18.sp,
                    fontWeight =
                        FontWeight.Bold,
                    color =
                        MaterialTheme
                            .colorScheme
                            .primary
                )


                Text(
                    text =
                        appText(
                            language,
                            "Student Portal • A102",
                            "Cổng sinh viên • A102"
                        ),
                    fontSize =
                        12.sp,
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )
            }


            Surface(
                shape =
                    CircleShape,
                color =
                    MaterialTheme
                        .colorScheme
                        .surfaceVariant
            ) {

                IconButton(
                    onClick =
                        onOpenSettings,
                    modifier =
                        Modifier.size(
                            48.dp
                        )
                ) {

                    Icon(
                        imageVector =
                            Icons.Filled.Settings,
                        contentDescription =
                            appText(
                                language,
                                "Settings",
                                "Cài đặt"
                            ),
                        tint =
                            MaterialTheme
                                .colorScheme
                                .primary
                    )
                }
            }
        }


        Spacer(
            Modifier.height(
                20.dp
            )
        )


        if (
            isLoading
        ) {

            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.Center
            ) {

                CircularProgressIndicator()
            }

            Spacer(
                Modifier.height(
                    14.dp
                )
            )
        }


        if (
            errorMessage.isNotBlank()
        ) {

            StudentDashboardNoticeCard(
                icon =
                    Icons.Filled.VpnKey,
                title =
                    appText(
                        language,
                        "Action failed",
                        "Thao tác thất bại"
                    ),
                subtitle =
                    errorMessage,
                color =
                    MaterialTheme
                        .colorScheme
                        .error
            )

            Spacer(
                Modifier.height(
                    12.dp
                )
            )
        }


        if (
            credentialActionNotice.isNotBlank()
        ) {

            StudentDashboardNoticeCard(
                icon =
                    Icons.Filled.CheckCircle,
                title =
                    appText(
                        language,
                        "Credential updated",
                        "Đã cập nhật xác thực"
                    ),
                subtitle =
                    credentialActionNotice,
                color =
                    Color(
                        0xFF66BB6A
                    )
            )

            Spacer(
                Modifier.height(
                    12.dp
                )
            )
        }


        accessNotice?.let { notice ->

            StudentAccessNoticeCard(
                notice =
                    notice
            )

            Spacer(
                Modifier.height(
                    12.dp
                )
            )
        }


        if (
            attendanceNotice.isNotBlank()
        ) {

            StudentDashboardNoticeCard(
                icon =
                    Icons.Filled.CheckCircle,
                title =
                    attendanceNotice,
                subtitle =
                    appText(
                        language,
                        "Attendance information has been updated.",
                        "Thông tin điểm danh đã được cập nhật."
                    ),
                color =
                    Color(
                        0xFF66BB6A
                    )
            )

            Spacer(
                Modifier.height(
                    12.dp
                )
            )
        }


        // =================================================
        // PROFILE
        // =================================================

        StudentHeroCard(
            fullName =
                fullName,
            studentId =
                studentId,
            email =
                email,
            accountActive =
                accountActive,
            language =
                language
        )


        Spacer(
            Modifier.height(
                16.dp
            )
        )


        // =================================================
        // SYSTEM STATUS
        // =================================================

        StudentSystemStatusCard(
            language =
                language,
            deviceOnline =
                deviceOnline,
            doorStatus =
                doorStatus,
            accessReady =
                accessReady,
            credentialCount =
                credentialCount,
            doorStatusFromFirestore =
                doorStatusFromFirestore
        )


        Spacer(
            Modifier.height(
                16.dp
            )
        )


        // =================================================
        // LAST ACCESS
        // =================================================

        StudentLastAccessCard(
            language =
                language,
            log =
                lastAccess
        )


        Spacer(
            Modifier.height(
                28.dp
            )
        )


        // =================================================
        // CREDENTIAL MANAGEMENT
        // =================================================

        StudentSectionHeader(
            icon =
                Icons.Filled.VpnKey,
            title =
                appText(
                    language,
                    "Access Methods",
                    "Phương thức xác thực"
                ),
            subtitle =
                appText(
                    language,
                    "Add or remove NFC and fingerprint",
                    "Thêm hoặc xóa NFC và vân tay"
                )
        )


        Spacer(
            Modifier.height(
                12.dp
            )
        )


        if (
            !enrollmentAllowed
        ) {

            StudentDashboardNoticeCard(
                icon =
                    Icons.Filled.VpnKey,
                title =
                    appText(
                        language,
                        "Admin approval required",
                        "Cần Admin cấp quyền"
                    ),
                subtitle =
                    appText(
                        language,
                        "Your account must be active before credential management.",
                        "Tài khoản phải được kích hoạt trước khi quản lý xác thực."
                    ),
                color =
                    Color(
                        0xFFFFB74D
                    )
            )

            Spacer(
                Modifier.height(
                    12.dp
                )
            )
        }


        StudentCredentialManagementCard(
            icon =
                Icons.Filled.CreditCard,
            title =
                appText(
                    language,
                    "NFC Card",
                    "Thẻ NFC"
                ),
            detail =
                if (
                    nfcLinked
                ) {

                    "UID: $nfcUid"

                } else {

                    appText(
                        language,
                        "No NFC card registered",
                        "Chưa đăng ký thẻ NFC"
                    )
                },
            registered =
                nfcLinked,
            color =
                Color(
                    0xFF42A5F5
                ),
            addText =
                appText(
                    language,
                    "ADD NFC CARD",
                    "THÊM THẺ NFC"
                ),
            removeText =
                appText(
                    language,
                    "REMOVE NFC",
                    "XÓA THẺ NFC"
                ),
            enabled =
                enrollmentAllowed &&
                        !ownRequestBusy &&
                        !requestSubmitting,
            onAdd = {

                submitDeviceRequest(
                    action =
                        "enroll_nfc"
                )
            },
            onRemove = {

                deleteTarget =
                    "nfc"
            }
        )


        Spacer(
            Modifier.height(
                12.dp
            )
        )


        StudentCredentialManagementCard(
            icon =
                Icons.Filled.Fingerprint,
            title =
                appText(
                    language,
                    "Fingerprint",
                    "Vân tay"
                ),
            detail =
                if (
                    fingerprintEnrolled
                ) {

                    appText(
                        language,
                        "Sensor slot: $fingerprintSlot",
                        "Vị trí lưu: $fingerprintSlot"
                    )

                } else {

                    appText(
                        language,
                        "No fingerprint registered",
                        "Chưa đăng ký vân tay"
                    )
                },
            registered =
                fingerprintEnrolled,
            color =
                Color(
                    0xFFCE93D8
                ),
            addText =
                appText(
                    language,
                    "ADD FINGERPRINT",
                    "THÊM VÂN TAY"
                ),
            removeText =
                appText(
                    language,
                    "REMOVE FINGERPRINT",
                    "XÓA VÂN TAY"
                ),
            enabled =
                enrollmentAllowed &&
                        !ownRequestBusy &&
                        !requestSubmitting,
            onAdd = {

                submitDeviceRequest(
                    action =
                        "enroll_fingerprint"
                )
            },
            onRemove = {

                deleteTarget =
                    "fingerprint"
            }
        )


        if (
            ownEnrollmentRequest &&
            enrollmentState.status.isNotBlank()
        ) {

            Spacer(
                Modifier.height(
                    12.dp
                )
            )


            StudentEnrollmentMessageCard(
                state =
                    enrollmentState,
                language =
                    language,
                onCancel = {

                    cancelEnrollmentRequest()
                },
                onRetry = {

                    when (
                        enrollmentState.action
                    ) {

                        "enroll_nfc",
                        "enroll_fingerprint" ->

                            submitDeviceRequest(
                                action =
                                    enrollmentState.action
                            )


                        "delete_nfc" ->

                            submitDeviceRequest(
                                action =
                                    "delete_nfc",

                                fingerprintSlotValue =
                                    0L,

                                credentialValue =
                                    enrollmentState
                                        .credentialValue
                                        .ifBlank {
                                            nfcUid
                                        }
                            )


                        "delete_fingerprint" ->

                            submitDeviceRequest(
                                action =
                                    "delete_fingerprint",

                                fingerprintSlotValue =
                                    if (
                                        enrollmentState
                                            .fingerprintSlot >
                                        0
                                    ) {

                                        enrollmentState
                                            .fingerprintSlot

                                    } else {

                                        fingerprintSlot
                                    },

                                credentialValue =
                                    enrollmentState
                                        .credentialValue
                                        .ifBlank {
                                            fingerprintSlot
                                                .toString()
                                        }
                            )
                    }
                }
            )
        }


        Spacer(
            Modifier.height(
                28.dp
            )
        )


        // =================================================
        // ACCESS OVERVIEW
        // =================================================

        StudentSectionHeader(
            icon =
                Icons.Filled.History,
            title =
                appText(
                    language,
                    "Today's Access",
                    "Truy cập hôm nay"
                ),
            subtitle =
                appText(
                    language,
                    "Room A102 access activity",
                    "Hoạt động ra vào phòng A102"
                )
        )


        Spacer(
            Modifier.height(
                12.dp
            )
        )


        StudentAccessSummaryCard(
            language =
                language,
            total =
                accessToday,
            granted =
                grantedToday,
            denied =
                deniedToday
        )


        Spacer(
            Modifier.height(
                26.dp
            )
        )


        // =================================================
        // HISTORY
        // =================================================

        StudentSectionHeader(
            icon =
                Icons.Filled.History,
            title =
                appText(
                    language,
                    "Access History",
                    "Lịch sử ra vào"
                ),
            subtitle =
                if (
                    showAllHistory
                ) {

                    appText(
                        language,
                        "Showing all access records",
                        "Đang hiển thị toàn bộ lịch sử"
                    )

                } else {

                    appText(
                        language,
                        "Latest 5 authentication attempts",
                        "5 lượt xác thực gần nhất"
                    )
                }
        )


        Spacer(
            Modifier.height(
                12.dp
            )
        )


        if (
            visibleAccessLogs.isEmpty()
        ) {

            Card(
                modifier =
                    Modifier.fillMaxWidth(),
                shape =
                    RoundedCornerShape(
                        20.dp
                    )
            ) {

                Text(
                    text =
                        appText(
                            language,
                            "No Room A102 access history yet.",
                            "Chưa có lịch sử ra vào phòng A102."
                        ),
                    modifier =
                        Modifier.padding(
                            18.dp
                        ),
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )
            }

        } else {

            visibleAccessLogs.forEach { log ->

                StudentAccessLogCard(
                    log =
                        log,
                    language =
                        language
                )

                Spacer(
                    Modifier.height(
                        10.dp
                    )
                )
            }


            if (
                accessLogs.size > 5
            ) {

                OutlinedButton(
                    onClick = {
                        showAllHistory =
                            !showAllHistory
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                ) {

                    Text(
                        text =
                            if (
                                showAllHistory
                            ) {

                                appText(
                                    language,
                                    "SHOW LESS",
                                    "THU GỌN"
                                )

                            } else {

                                appText(
                                    language,
                                    "VIEW ALL HISTORY",
                                    "XEM TOÀN BỘ LỊCH SỬ"
                                )
                            }
                    )
                }
            }
        }


        Spacer(
            Modifier.height(
                34.dp
            )
        )
    }
}


// =========================================================
// HERO
// =========================================================

@Composable
private fun StudentHeroCard(
    fullName: String,
    studentId: String,
    email: String,
    accountActive: Boolean,
    language: AppLanguage
) {

    Card(
        modifier =
            Modifier.fillMaxWidth(),
        shape =
            RoundedCornerShape(
                24.dp
            ),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    MaterialTheme
                        .colorScheme
                        .surfaceVariant
            ),
        border =
            BorderStroke(
                1.dp,
                MaterialTheme
                    .colorScheme
                    .primary
                    .copy(
                        alpha =
                            0.22f
                    )
            )
    ) {

        Column(
            modifier =
                Modifier.padding(
                    20.dp
                )
        ) {


            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {


                Box(
                    modifier = Modifier
                        .size(
                            58.dp
                        )
                        .clip(
                            CircleShape
                        )
                        .background(
                            MaterialTheme
                                .colorScheme
                                .primary
                                .copy(
                                    alpha =
                                        0.15f
                                )
                        ),
                    contentAlignment =
                        Alignment.Center
                ) {

                    Text(
                        text =
                            fullName
                                .trim()
                                .firstOrNull()
                                ?.uppercaseChar()
                                ?.toString()
                                ?: "S",
                        fontSize =
                            24.sp,
                        fontWeight =
                            FontWeight.Bold,
                        color =
                            MaterialTheme
                                .colorScheme
                                .primary
                    )
                }


                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(
                            start =
                                14.dp
                        )
                ) {


                    Text(
                        text =
                            appText(
                                language,
                                "Welcome back",
                                "Xin chào"
                            ),
                        fontSize =
                            12.sp,
                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant
                    )


                    Text(
                        text =
                            fullName.ifBlank {

                                appText(
                                    language,
                                    "Student",
                                    "Sinh viên"
                                )
                            },
                        fontSize =
                            21.sp,
                        fontWeight =
                            FontWeight.Bold
                    )


                    Text(
                        text =
                            appText(
                                language,
                                "Student ID: $studentId",
                                "MSSV: $studentId"
                            ),
                        fontSize =
                            12.sp,
                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant
                    )
                }


                StudentStatusPill(
                    text =
                        if (
                            accountActive
                        ) {

                            appText(
                                language,
                                "ALLOWED",
                                "ĐƯỢC PHÉP"
                            )

                        } else {

                            appText(
                                language,
                                "PENDING",
                                "CHỜ DUYỆT"
                            )
                        },
                    color =
                        if (
                            accountActive
                        ) {

                            Color(
                                0xFF66BB6A
                            )

                        } else {

                            Color(
                                0xFFFFB74D
                            )
                        }
                )
            }


            Spacer(
                Modifier.height(
                    18.dp
                )
            )


            Surface(
                modifier =
                    Modifier.fillMaxWidth(),
                shape =
                    RoundedCornerShape(
                        18.dp
                    ),
                color =
                    MaterialTheme
                        .colorScheme
                        .surface
            ) {

                Row(
                    modifier =
                        Modifier.padding(
                            16.dp
                        ),
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    StudentIconBadge(
                        icon =
                            Icons.Filled.School,
                        background =
                            MaterialTheme
                                .colorScheme
                                .primary
                                .copy(
                                    alpha =
                                        0.12f
                                ),
                        color =
                            MaterialTheme
                                .colorScheme
                                .primary,
                        size =
                            44.dp
                    )


                    Column(
                        modifier =
                            Modifier.padding(
                                start =
                                    12.dp
                            )
                    ) {

                        Text(
                            text =
                                appText(
                                    language,
                                    "Current Classroom",
                                    "Phòng học hiện tại"
                                ),
                            fontSize =
                                11.sp,
                            color =
                                MaterialTheme
                                    .colorScheme
                                    .onSurfaceVariant
                        )

                        Text(
                            text =
                                classroomName(
                                    language
                                ),
                            fontSize =
                                18.sp,
                            fontWeight =
                                FontWeight.Bold
                        )

                        Text(
                            text =
                                ClassroomConfig.DEVICE_ID,
                            fontSize =
                                11.sp,
                            color =
                                MaterialTheme
                                    .colorScheme
                                    .onSurfaceVariant
                        )
                    }
                }
            }


            Spacer(
                Modifier.height(
                    12.dp
                )
            )


            Surface(
                modifier =
                    Modifier.fillMaxWidth(),
                shape =
                    RoundedCornerShape(
                        16.dp
                    ),
                color =
                    MaterialTheme
                        .colorScheme
                        .surface
            ) {

                Column(
                    modifier =
                        Modifier.padding(
                            14.dp
                        )
                ) {

                    Text(
                        text =
                            "EMAIL",
                        fontSize =
                            10.sp,
                        fontWeight =
                            FontWeight.Bold,
                        color =
                            MaterialTheme
                                .colorScheme
                                .primary
                    )

                    Spacer(
                        Modifier.height(
                            4.dp
                        )
                    )

                    Text(
                        text =
                            email,
                        fontSize =
                            13.sp,
                        fontWeight =
                            FontWeight.Medium
                    )
                }
            }
        }
    }
}


// =========================================================
// SYSTEM STATUS
// =========================================================

@Composable
private fun StudentSystemStatusCard(
    language: AppLanguage,
    deviceOnline: Boolean,
    doorStatus: String,
    accessReady: Boolean,
    credentialCount: Int,
    doorStatusFromFirestore: Boolean
) {

    val doorOpen =
        doorStatus == "open"

    val doorUnknown =
        doorStatus == "unknown"


    Card(
        modifier =
            Modifier.fillMaxWidth(),
        shape =
            RoundedCornerShape(
                22.dp
            ),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    MaterialTheme
                        .colorScheme
                        .surface
            ),
        border =
            BorderStroke(
                1.dp,
                MaterialTheme
                    .colorScheme
                    .outline
                    .copy(
                        alpha =
                            0.14f
                    )
            )
    ) {

        Column(
            modifier =
                Modifier.padding(
                    18.dp
                )
        ) {

            Row(
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                StudentIconBadge(
                    icon =
                        Icons.Filled.SettingsInputAntenna,
                    background =
                        MaterialTheme
                            .colorScheme
                            .primary
                            .copy(
                                alpha =
                                    0.12f
                            ),
                    color =
                        MaterialTheme
                            .colorScheme
                            .primary,
                    size =
                        46.dp
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(
                            start =
                                12.dp
                        )
                ) {

                    Text(
                        text =
                            appText(
                                language,
                                "A102 System Status",
                                "Trạng thái hệ thống A102"
                            ),
                        fontWeight =
                            FontWeight.Bold,
                        fontSize =
                            17.sp
                    )

                    Text(
                        text =
                            if (
                                doorStatusFromFirestore
                            ) {

                                appText(
                                    language,
                                    "Realtime device status",
                                    "Trạng thái trực tiếp từ thiết bị"
                                )

                            } else {

                                appText(
                                    language,
                                    "Door status follows access events",
                                    "Trạng thái cửa theo sự kiện truy cập"
                                )
                            },
                        fontSize =
                            11.sp,
                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant
                    )
                }
            }


            Spacer(
                Modifier.height(
                    16.dp
                )
            )


            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(
                        10.dp
                    )
            ) {

                StudentSmallStatusCard(
                    modifier =
                        Modifier.weight(1f),
                    title =
                        appText(
                            language,
                            "DEVICE",
                            "THIẾT BỊ"
                        ),
                    value =
                        if (
                            deviceOnline
                        ) {
                            "ONLINE"
                        } else {
                            "OFFLINE"
                        },
                    color =
                        if (
                            deviceOnline
                        ) {

                            Color(
                                0xFF66BB6A
                            )

                        } else {

                            MaterialTheme
                                .colorScheme
                                .error
                        }
                )


                StudentSmallStatusCard(
                    modifier =
                        Modifier.weight(1f),
                    title =
                        appText(
                            language,
                            "DOOR",
                            "CỬA"
                        ),
                    value =
                        when {

                            doorOpen ->
                                "OPEN"

                            doorUnknown ->
                                "UNKNOWN"

                            else ->
                                "CLOSED"
                        },
                    color =
                        when {

                            doorOpen ->
                                Color(
                                    0xFFFFB74D
                                )

                            doorUnknown ->
                                MaterialTheme
                                    .colorScheme
                                    .onSurfaceVariant

                            else ->
                                Color(
                                    0xFF66BB6A
                                )
                        }
                )


                StudentSmallStatusCard(
                    modifier =
                        Modifier.weight(1f),
                    title =
                        appText(
                            language,
                            "ACCESS",
                            "TRUY CẬP"
                        ),
                    value =
                        if (
                            accessReady
                        ) {
                            "$credentialCount/2"
                        } else {
                            "SETUP"
                        },
                    color =
                        if (
                            accessReady
                        ) {

                            MaterialTheme
                                .colorScheme
                                .primary

                        } else {

                            Color(
                                0xFFFFB74D
                            )
                        }
                )
            }
        }
    }
}


@Composable
private fun StudentSmallStatusCard(
    modifier: Modifier,
    title: String,
    value: String,
    color: Color
) {

    Surface(
        modifier =
            modifier,
        shape =
            RoundedCornerShape(
                15.dp
            ),
        color =
            color.copy(
                alpha =
                    0.08f
            ),
        border =
            BorderStroke(
                1.dp,
                color.copy(
                    alpha =
                        0.18f
                )
            )
    ) {

        Column(
            modifier =
                Modifier.padding(
                    vertical =
                        13.dp,
                    horizontal =
                        6.dp
                ),
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Text(
                text =
                    title,
                fontSize =
                    9.sp,
                fontWeight =
                    FontWeight.Bold,
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
            )

            Spacer(
                Modifier.height(
                    4.dp
                )
            )

            Text(
                text =
                    value,
                fontSize =
                    11.sp,
                fontWeight =
                    FontWeight.Bold,
                color =
                    color
            )
        }
    }
}


// =========================================================
// LAST ACCESS
// =========================================================

@Composable
private fun StudentLastAccessCard(
    language: AppLanguage,
    log: StudentAccessLog?
) {

    val granted =
        log?.result.equals(
            "granted",
            true
        )

    val accent =
        if (
            log == null
        ) {

            MaterialTheme
                .colorScheme
                .onSurfaceVariant

        } else if (
            granted
        ) {

            Color(
                0xFF66BB6A
            )

        } else {

            MaterialTheme
                .colorScheme
                .error
        }


    Card(
        modifier =
            Modifier.fillMaxWidth(),
        shape =
            RoundedCornerShape(
                20.dp
            ),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    accent.copy(
                        alpha =
                            0.07f
                    )
            ),
        border =
            BorderStroke(
                1.dp,
                accent.copy(
                    alpha =
                        0.20f
                )
            )
    ) {

        Row(
            modifier =
                Modifier.padding(
                    16.dp
                ),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            StudentIconBadge(
                icon =
                    Icons.Filled.History,
                background =
                    accent.copy(
                        alpha =
                            0.12f
                    ),
                color =
                    accent,
                size =
                    44.dp
            )


            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(
                        start =
                            12.dp
                    )
            ) {

                Text(
                    text =
                        appText(
                            language,
                            "Last Access",
                            "Lần truy cập gần nhất"
                        ),
                    fontWeight =
                        FontWeight.Bold
                )


                if (
                    log == null
                ) {

                    Text(
                        text =
                            appText(
                                language,
                                "No access record yet.",
                                "Chưa có lượt truy cập."
                            ),
                        fontSize =
                            12.sp,
                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant
                    )

                } else {

                    val method =
                        when (
                            log.authenticationMethod
                                .lowercase()
                        ) {

                            "nfc" ->
                                "NFC"

                            "fingerprint" ->
                                appText(
                                    language,
                                    "Fingerprint",
                                    "Vân tay"
                                )

                            else ->
                                "-"
                        }


                    val time =
                        log.timestamp?.let {

                            SimpleDateFormat(
                                "dd/MM/yyyy • HH:mm:ss",
                                Locale.getDefault()
                            ).format(
                                it.toDate()
                            )

                        } ?: "--"


                    Text(
                        text =
                            "$method • $time",
                        fontSize =
                            12.sp,
                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant
                    )
                }
            }


            if (
                log != null
            ) {

                StudentStatusPill(
                    text =
                        if (
                            granted
                        ) {

                            appText(
                                language,
                                "GRANTED",
                                "CHO PHÉP"
                            )

                        } else {

                            appText(
                                language,
                                "DENIED",
                                "TỪ CHỐI"
                            )
                        },
                    color =
                        accent
                )
            }
        }
    }
}


// =========================================================
// SECTION HEADER
// =========================================================

@Composable
private fun StudentSectionHeader(
    icon: ImageVector,
    title: String,
    subtitle: String
) {

    Row(
        verticalAlignment =
            Alignment.CenterVertically
    ) {

        StudentIconBadge(
            icon =
                icon,
            background =
                MaterialTheme
                    .colorScheme
                    .primary
                    .copy(
                        alpha =
                            0.12f
                    ),
            color =
                MaterialTheme
                    .colorScheme
                    .primary,
            size =
                42.dp
        )


        Column(
            modifier =
                Modifier.padding(
                    start =
                        12.dp
                )
        ) {

            Text(
                text =
                    title,
                fontSize =
                    19.sp,
                fontWeight =
                    FontWeight.Bold
            )

            Text(
                text =
                    subtitle,
                fontSize =
                    12.sp,
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
            )
        }
    }
}


// =========================================================
// CREDENTIAL MANAGEMENT
// =========================================================

@Composable
private fun StudentCredentialManagementCard(
    icon: ImageVector,
    title: String,
    detail: String,
    registered: Boolean,
    color: Color,
    addText: String,
    removeText: String,
    enabled: Boolean,
    onAdd: () -> Unit,
    onRemove: () -> Unit
) {

    val statusColor =
        if (
            registered
        ) {

            Color(
                0xFF66BB6A
            )

        } else {

            Color(
                0xFFFFB74D
            )
        }


    Card(
        modifier =
            Modifier.fillMaxWidth(),
        shape =
            RoundedCornerShape(
                22.dp
            ),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    MaterialTheme
                        .colorScheme
                        .surfaceVariant
            ),
        border =
            BorderStroke(
                1.dp,
                color.copy(
                    alpha =
                        0.18f
                )
            )
    ) {

        Column(
            modifier =
                Modifier.padding(
                    18.dp
                )
        ) {

            Row(
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                StudentIconBadge(
                    icon =
                        icon,
                    background =
                        color.copy(
                            alpha =
                                0.13f
                        ),
                    color =
                        color,
                    size =
                        52.dp
                )


                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(
                            start =
                                14.dp
                        )
                ) {

                    Text(
                        text =
                            title,
                        fontSize =
                            17.sp,
                        fontWeight =
                            FontWeight.Bold
                    )

                    Spacer(
                        Modifier.height(
                            3.dp
                        )
                    )

                    Text(
                        text =
                            detail,
                        fontSize =
                            12.sp,
                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant
                    )
                }


                StudentStatusPill(
                    text =
                        if (
                            registered
                        ) {

                            "ACTIVE"

                        } else {

                            "EMPTY"
                        },
                    color =
                        statusColor
                )
            }


            Spacer(
                Modifier.height(
                    16.dp
                )
            )


            if (
                registered
            ) {

                OutlinedButton(
                    onClick =
                        onRemove,
                    enabled =
                        enabled,
                    modifier =
                        Modifier.fillMaxWidth()
                ) {

                    Text(
                        text =
                            removeText,
                        color =
                            if (
                                enabled
                            ) {

                                MaterialTheme
                                    .colorScheme
                                    .error

                            } else {

                                MaterialTheme
                                    .colorScheme
                                    .onSurfaceVariant
                            },
                        fontWeight =
                            FontWeight.Bold
                    )
                }

            } else {

                Button(
                    onClick =
                        onAdd,
                    enabled =
                        enabled,
                    modifier =
                        Modifier.fillMaxWidth()
                ) {

                    Text(
                        text =
                            addText,
                        fontWeight =
                            FontWeight.Bold
                    )
                }
            }
        }
    }
}


// =========================================================
// ENROLLMENT STATUS
// =========================================================

@Composable
private fun StudentEnrollmentMessageCard(
    state: StudentEnrollmentState,
    language: AppLanguage,
    onCancel: () -> Unit,
    onRetry: () -> Unit
) {

    val message =
        when {

            state.status == "waiting" ->

                when (
                    state.action
                ) {

                    "delete_fingerprint" ->
                        appText(
                            language,
                            "Waiting for A102 to remove fingerprint...",
                            "Đang chờ A102 xóa vân tay..."
                        )

                    else ->
                        appText(
                            language,
                            "Waiting for ESP32 A102...",
                            "Đang chờ thiết bị A102..."
                        )
                }


            state.status == "processing" ->

                when (
                    state.action
                ) {

                    "enroll_nfc" ->
                        appText(
                            language,
                            "Please scan your NFC card at A102.",
                            "Hãy quét thẻ NFC tại thiết bị A102."
                        )

                    "enroll_fingerprint" ->
                        appText(
                            language,
                            "Please place your finger on the sensor.",
                            "Hãy đặt ngón tay lên cảm biến."
                        )

                    "delete_fingerprint" ->
                        appText(
                            language,
                            "Removing fingerprint from A102...",
                            "Đang xóa vân tay khỏi A102..."
                        )

                    else ->
                        state.message
                }


            state.status == "completed" ->

                appText(
                    language,
                    "Credential update successful.",
                    "Cập nhật xác thực thành công."
                )


            state.status == "failed" ->

                appText(
                    language,
                    "Action failed: ${state.message}",
                    "Thao tác thất bại: ${state.message}"
                )


            state.status == "cancelled" ->

                appText(
                    language,
                    "Request cancelled.",
                    "Yêu cầu đã được hủy."
                )


            else ->
                ""
        }


    val accent =
        when {

            state.status == "completed" ->
                Color(
                    0xFF66BB6A
                )

            state.status == "failed" ->
                MaterialTheme
                    .colorScheme
                    .error

            else ->
                MaterialTheme
                    .colorScheme
                    .primary
        }


    Card(
        modifier =
            Modifier.fillMaxWidth(),
        shape =
            RoundedCornerShape(
                18.dp
            ),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    accent.copy(
                        alpha =
                            0.09f
                    )
            ),
        border =
            BorderStroke(
                1.dp,
                accent.copy(
                    alpha =
                        0.24f
                )
            )
    ) {

        Column(
            modifier =
                Modifier.padding(
                    16.dp
                )
        ) {

            Row(
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                if (
                    state.status == "waiting" ||
                    state.status == "processing"
                ) {

                    CircularProgressIndicator(
                        modifier =
                            Modifier.size(
                                22.dp
                            ),
                        strokeWidth =
                            2.dp,
                        color =
                            accent
                    )

                } else {

                    Icon(
                        imageVector =
                            Icons.Filled.CheckCircle,
                        contentDescription =
                            null,
                        tint =
                            accent
                    )
                }


                Text(
                    text =
                        message,
                    modifier = Modifier
                        .weight(1f)
                        .padding(
                            start =
                                12.dp
                        ),
                    fontWeight =
                        FontWeight.SemiBold
                )
            }


            if (
                state.status == "waiting"
            ) {

                Spacer(
                    Modifier.height(
                        12.dp
                    )
                )

                OutlinedButton(
                    onClick =
                        onCancel,
                    modifier =
                        Modifier.fillMaxWidth()
                ) {

                    Text(
                        text =
                            appText(
                                language,
                                "CANCEL REQUEST",
                                "HỦY YÊU CẦU"
                            )
                    )
                }
            }


            if (
                state.status == "failed" ||
                state.status == "cancelled"
            ) {

                Spacer(
                    Modifier.height(
                        12.dp
                    )
                )

                Button(
                    onClick =
                        onRetry,
                    modifier =
                        Modifier.fillMaxWidth()
                ) {

                    Text(
                        text =
                            appText(
                                language,
                                "TRY AGAIN",
                                "THỬ LẠI"
                            ),
                        fontWeight =
                            FontWeight.Bold
                    )
                }
            }
        }
    }
}


// =========================================================
// NOTICES
// =========================================================

@Composable
private fun StudentAccessNoticeCard(
    notice: StudentAccessNotice
) {

    val accent =
        if (
            notice.success
        ) {

            Color(
                0xFF66BB6A
            )

        } else {

            MaterialTheme
                .colorScheme
                .error
        }


    Card(
        modifier =
            Modifier.fillMaxWidth(),
        shape =
            RoundedCornerShape(
                20.dp
            ),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    accent.copy(
                        alpha =
                            0.10f
                    )
            ),
        border =
            BorderStroke(
                1.dp,
                accent.copy(
                    alpha =
                        0.28f
                )
            )
    ) {

        Row(
            modifier =
                Modifier.padding(
                    16.dp
                ),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            StudentIconBadge(
                icon =
                    Icons.Filled.CheckCircle,
                background =
                    accent.copy(
                        alpha =
                            0.14f
                    ),
                color =
                    accent,
                size =
                    46.dp
            )


            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(
                        start =
                            12.dp
                    )
            ) {

                Text(
                    text =
                        notice.title,
                    fontWeight =
                        FontWeight.Bold,
                    color =
                        accent
                )

                Text(
                    text =
                        "${notice.location} • ${notice.method}",
                    fontSize =
                        12.sp
                )

                Text(
                    text =
                        notice.time,
                    fontSize =
                        11.sp,
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )
            }
        }
    }
}


@Composable
private fun StudentDashboardNoticeCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    color: Color
) {

    Card(
        modifier =
            Modifier.fillMaxWidth(),
        shape =
            RoundedCornerShape(
                18.dp
            ),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    color.copy(
                        alpha =
                            0.09f
                    )
            ),
        border =
            BorderStroke(
                1.dp,
                color.copy(
                    alpha =
                        0.24f
                )
            )
    ) {

        Row(
            modifier =
                Modifier.padding(
                    16.dp
                ),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            StudentIconBadge(
                icon =
                    icon,
                background =
                    color.copy(
                        alpha =
                            0.13f
                    ),
                color =
                    color,
                size =
                    42.dp
            )


            Column(
                modifier =
                    Modifier.padding(
                        start =
                            12.dp
                    )
            ) {

                Text(
                    text =
                        title,
                    fontWeight =
                        FontWeight.Bold,
                    color =
                        color
                )

                Text(
                    text =
                        subtitle,
                    fontSize =
                        12.sp,
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )
            }
        }
    }
}


// =========================================================
// ACCESS SUMMARY
// =========================================================

@Composable
private fun StudentAccessSummaryCard(
    language: AppLanguage,
    total: Int,
    granted: Int,
    denied: Int
) {

    Card(
        modifier =
            Modifier.fillMaxWidth(),
        shape =
            RoundedCornerShape(
                22.dp
            ),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    MaterialTheme
                        .colorScheme
                        .surface
            ),
        border =
            BorderStroke(
                1.dp,
                MaterialTheme
                    .colorScheme
                    .outline
                    .copy(
                        alpha =
                            0.14f
                    )
            )
    ) {

        Column(
            modifier =
                Modifier.padding(
                    18.dp
                )
        ) {

            Text(
                text =
                    appText(
                        language,
                        "Today's Access Overview",
                        "Tổng quan ra vào hôm nay"
                    ),
                fontSize =
                    16.sp,
                fontWeight =
                    FontWeight.Bold
            )


            Spacer(
                Modifier.height(
                    16.dp
                )
            )


            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(
                        10.dp
                    )
            ) {

                StudentMetricCard(
                    modifier =
                        Modifier.weight(1f),
                    value =
                        total,
                    label =
                        appText(
                            language,
                            "Scans",
                            "Lượt quét"
                        ),
                    color =
                        MaterialTheme
                            .colorScheme
                            .primary
                )


                StudentMetricCard(
                    modifier =
                        Modifier.weight(1f),
                    value =
                        granted,
                    label =
                        appText(
                            language,
                            "Granted",
                            "Thành công"
                        ),
                    color =
                        Color(
                            0xFF66BB6A
                        )
                )


                StudentMetricCard(
                    modifier =
                        Modifier.weight(1f),
                    value =
                        denied,
                    label =
                        appText(
                            language,
                            "Denied",
                            "Từ chối"
                        ),
                    color =
                        MaterialTheme
                            .colorScheme
                            .error
                )
            }
        }
    }
}


@Composable
private fun StudentMetricCard(
    modifier: Modifier,
    value: Int,
    label: String,
    color: Color
) {

    Surface(
        modifier =
            modifier,
        shape =
            RoundedCornerShape(
                16.dp
            ),
        color =
            color.copy(
                alpha =
                    0.09f
            ),
        border =
            BorderStroke(
                1.dp,
                color.copy(
                    alpha =
                        0.18f
                )
            )
    ) {

        Column(
            modifier =
                Modifier.padding(
                    vertical =
                        14.dp,
                    horizontal =
                        5.dp
                ),
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Text(
                text =
                    value.toString(),
                fontSize =
                    24.sp,
                fontWeight =
                    FontWeight.Bold,
                color =
                    color
            )

            Text(
                text =
                    label,
                fontSize =
                    10.sp,
                fontWeight =
                    FontWeight.SemiBold
            )
        }
    }
}


// =========================================================
// ACCESS HISTORY CARD
// =========================================================

@Composable
private fun StudentAccessLogCard(
    log: StudentAccessLog,
    language: AppLanguage
) {

    val granted =
        log.result.equals(
            "granted",
            true
        )

    val authentication =
        when (
            log.authenticationMethod
                .lowercase()
        ) {

            "nfc" ->
                "NFC"

            "fingerprint" ->
                appText(
                    language,
                    "Fingerprint",
                    "Vân tay"
                )

            else ->
                appText(
                    language,
                    "Unknown",
                    "Không xác định"
                )
        }


    val time =
        log.timestamp?.let { timestamp ->

            SimpleDateFormat(
                "dd/MM/yyyy • HH:mm:ss",
                Locale.getDefault()
            ).format(
                timestamp.toDate()
            )

        } ?: "--"


    val accent =
        if (
            granted
        ) {

            Color(
                0xFF66BB6A
            )

        } else {

            MaterialTheme
                .colorScheme
                .error
        }


    val methodIcon =
        if (
            log.authenticationMethod
                .equals(
                    "fingerprint",
                    true
                )
        ) {

            Icons.Filled.Fingerprint

        } else {

            Icons.Filled.CreditCard
        }


    Card(
        modifier =
            Modifier.fillMaxWidth(),
        shape =
            RoundedCornerShape(
                20.dp
            ),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    MaterialTheme
                        .colorScheme
                        .surface
            ),
        border =
            BorderStroke(
                1.dp,
                MaterialTheme
                    .colorScheme
                    .outline
                    .copy(
                        alpha =
                            0.12f
                    )
            )
    ) {

        Row(
            modifier =
                Modifier.padding(
                    16.dp
                ),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            StudentIconBadge(
                icon =
                    methodIcon,
                background =
                    accent.copy(
                        alpha =
                            0.10f
                    ),
                color =
                    accent,
                size =
                    46.dp
            )


            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(
                        start =
                            12.dp
                    )
            ) {

                Row(
                    modifier =
                        Modifier.fillMaxWidth(),
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Text(
                        text =
                            log.location,
                        modifier =
                            Modifier.weight(1f),
                        fontWeight =
                            FontWeight.Bold
                    )


                    StudentStatusPill(
                        text =
                            if (
                                granted
                            ) {

                                appText(
                                    language,
                                    "GRANTED",
                                    "CHO PHÉP"
                                )

                            } else {

                                appText(
                                    language,
                                    "DENIED",
                                    "TỪ CHỐI"
                                )
                            },
                        color =
                            accent
                    )
                }


                Spacer(
                    Modifier.height(
                        6.dp
                    )
                )


                Text(
                    text =
                        appText(
                            language,
                            "Authentication: $authentication",
                            "Xác thực: $authentication"
                        ),
                    fontSize =
                        12.sp,
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )


                Text(
                    text =
                        time,
                    fontSize =
                        11.sp,
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )
            }
        }
    }
}


// =========================================================
// SMALL COMPONENTS
// =========================================================

@Composable
private fun StudentStatusPill(
    text: String,
    color: Color
) {

    Surface(
        shape =
            RoundedCornerShape(
                50.dp
            ),
        color =
            color.copy(
                alpha =
                    0.13f
            ),
        border =
            BorderStroke(
                1.dp,
                color.copy(
                    alpha =
                        0.30f
                )
            )
    ) {

        Text(
            text =
                text,
            modifier =
                Modifier.padding(
                    horizontal =
                        9.dp,
                    vertical =
                        6.dp
                ),
            fontSize =
                9.sp,
            fontWeight =
                FontWeight.Bold,
            color =
                color
        )
    }
}


@Composable
private fun StudentIconBadge(
    icon: ImageVector,
    background: Color,
    color: Color,
    size: Dp = 46.dp
) {

    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(background),
        contentAlignment =
            Alignment.Center
    ) {

        Icon(
            imageVector =
                icon,
            contentDescription =
                null,
            tint =
                color,
            modifier =
                Modifier.size(
                    if (
                        size <= 40.dp
                    ) {
                        20.dp
                    } else {
                        24.dp
                    }
                )
        )
    }
}