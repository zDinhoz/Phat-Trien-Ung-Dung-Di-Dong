@file:Suppress("SpellCheckingInspection")

package com.hcmute.smartaccess

import android.media.AudioManager
import android.media.ToneGenerator
import android.widget.Toast
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
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SettingsInputAntenna
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds


@Suppress("UNUSED_PARAMETER")
@Composable
fun AdminDashboardScreen(
    language: AppLanguage,
    onLogout: () -> Unit,
    onOpenPendingStudents: () -> Unit,
    onOpenAccessLogs: () -> Unit,
    onOpenDevices: () -> Unit,
    onOpenCredentials: () -> Unit,
    onOpenAttendance: () -> Unit,
    onOpenSettings: () -> Unit
) {

    val firestore =
        FirebaseFirestore.getInstance()

    val context =
        LocalContext.current

    val coroutineScope =
        rememberCoroutineScope()

    val toneGenerator =
        remember {
            ToneGenerator(
                AudioManager.STREAM_NOTIFICATION,
                100
            )
        }


    // =====================================================
    // DASHBOARD STATE
    // =====================================================

    var totalStudents by remember {
        mutableIntStateOf(0)
    }

    var pendingStudents by remember {
        mutableIntStateOf(0)
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

    var devicesOnline by remember {
        mutableIntStateOf(0)
    }

    var latestScanMessage by remember {
        mutableStateOf("")
    }

    var receivedInitialSnapshot by remember {
        mutableStateOf(false)
    }

    var isLoading by remember {
        mutableStateOf(true)
    }

    var errorMessage by remember {
        mutableStateOf("")
    }

    var attendanceOpen by remember {
        mutableStateOf(false)
    }

    var attendanceSubject by remember {
        mutableStateOf("")
    }


    // =====================================================
    // DOOR STATUS
    // =====================================================

    var doorStatus by remember {
        mutableStateOf("closed")
    }

    var doorStatusFromFirestore by remember {
        mutableStateOf(false)
    }

    var simulatedDoorEventId by remember {
        mutableIntStateOf(0)
    }


    DisposableEffect(
        toneGenerator
    ) {

        onDispose {
            toneGenerator.release()
        }
    }


    // =====================================================
    // HELPERS
    // =====================================================

    fun normalizeDoorStatus(
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
                simulatedDoorEventId == eventId
            ) {

                doorStatus =
                    "closed"
            }
        }
    }


    // =====================================================
    // USERS
    // =====================================================

    DisposableEffect(Unit) {

        val listener =
            firestore
                .collection("users")
                .addSnapshotListener {
                        snapshot,
                        error ->

                    if (
                        error != null
                    ) {

                        errorMessage =
                            appText(
                                language,
                                "Unable to load students.",
                                "Không thể tải sinh viên."
                            )

                        isLoading =
                            false

                        return@addSnapshotListener
                    }

                    val documents =
                        snapshot?.documents
                            ?: emptyList()

                    totalStudents =
                        documents.count {

                            it.getString(
                                "role"
                            ) ==
                                    "student"
                        }

                    pendingStudents =
                        documents.count {

                            it.getString(
                                "role"
                            ) ==
                                    "student" &&
                                    it.getString(
                                        "status"
                                    ) ==
                                    "pending"
                        }

                    isLoading =
                        false
                }

        onDispose {
            listener.remove()
        }
    }


    // =====================================================
    // ACCESS LOG
    // =====================================================

    DisposableEffect(Unit) {

        val listener =
            firestore
                .collection(
                    "access_logs"
                )
                .addSnapshotListener {
                        snapshot,
                        error ->

                    if (
                        error != null
                    ) {
                        return@addSnapshotListener
                    }


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


                    val today =
                        snapshot
                            ?.documents
                            ?.filter { document ->

                                document.getString(
                                    "deviceId"
                                ) ==
                                        ClassroomConfig.DEVICE_ID &&
                                        (
                                                document.getTimestamp(
                                                    "timestamp"
                                                )?.seconds
                                                    ?: 0L
                                                ) >=
                                        startOfDay.seconds
                            }
                            ?: emptyList()


                    accessToday =
                        today.size


                    grantedToday =
                        today.count {

                            it.getString(
                                "result"
                            ).equals(
                                "granted",
                                true
                            )
                        }


                    deniedToday =
                        today.count {

                            it.getString(
                                "result"
                            ).equals(
                                "denied",
                                true
                            )
                        }


                    if (
                        !receivedInitialSnapshot
                    ) {

                        receivedInitialSnapshot =
                            true

                        return@addSnapshotListener
                    }


                    snapshot
                        ?.documentChanges
                        ?.filter {

                            it.type ==
                                    DocumentChange
                                        .Type
                                        .ADDED
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


                            val name =
                                document.getString(
                                    "fullName"
                                ) ?: "Student"


                            val method =
                                when (
                                    document.getString(
                                        "authenticationMethod"
                                    )?.lowercase()
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


                            val granted =
                                document.getString(
                                    "result"
                                ).equals(
                                    "granted",
                                    true
                                )


                            val result =
                                if (
                                    granted
                                ) {

                                    appText(
                                        language,
                                        "Access granted",
                                        "Truy cập thành công"
                                    )

                                } else {

                                    appText(
                                        language,
                                        "Access denied",
                                        "Truy cập bị từ chối"
                                    )
                                }


                            latestScanMessage =
                                "$name • $method • $result"


                            if (
                                granted
                            ) {

                                simulateDoorOpen()
                            }


                            coroutineScope.launch {

                                toneGenerator.startTone(
                                    ToneGenerator
                                        .TONE_PROP_BEEP,
                                    120
                                )

                                delay(
                                    180.milliseconds
                                )

                                toneGenerator.startTone(
                                    ToneGenerator
                                        .TONE_PROP_BEEP,
                                    120
                                )
                            }


                            Toast
                                .makeText(
                                    context,
                                    latestScanMessage,
                                    Toast.LENGTH_LONG
                                )
                                .show()
                        }
                }


        onDispose {
            listener.remove()
        }
    }


    // =====================================================
    // DEVICE + DOOR
    // =====================================================

    DisposableEffect(Unit) {

        val listener =
            firestore
                .collection(
                    "devices"
                )
                .document(
                    ClassroomConfig.DEVICE_ID
                )
                .addSnapshotListener {
                        document,
                        _ ->


                    devicesOnline =
                        if (
                            document != null &&
                            document.exists() &&
                            document.getString(
                                "status"
                            ).equals(
                                "online",
                                true
                            )
                        ) {

                            1

                        } else {

                            0
                        }


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
                            normalizeDoorStatus(
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
    // ATTENDANCE
    // =====================================================

    DisposableEffect(Unit) {

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
                        _ ->

                    attendanceOpen =
                        document != null &&
                                document.exists() &&
                                document.getString(
                                    "status"
                                ) ==
                                "open"

                    attendanceSubject =
                        document?.getString(
                            "subjectName"
                        ) ?: ""
                }


        onDispose {
            listener.remove()
        }
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
                    "Logo",
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
                            "Room A102 Management",
                            "Quản lý phòng học A102"
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
                        onOpenSettings
                ) {

                    Icon(
                        imageVector =
                            Icons.Filled.Settings,
                        contentDescription =
                            "Settings"
                    )
                }
            }
        }


        Spacer(
            Modifier.height(
                14.dp
            )
        )


        // =================================================
        // ACCESS SUMMARY
        // =================================================

        AdminAccessSummaryCard(
            language =
                language,
            total =
                accessToday,
            granted =
                grantedToday,
            denied =
                deniedToday,
            latestMessage =
                latestScanMessage
        )


        Spacer(
            Modifier.height(
                20.dp
            )
        )


        Text(
            text =
                appText(
                    language,
                    "Hello, Admin",
                    "Xin chào, Admin"
                ),
            fontSize =
                25.sp,
            fontWeight =
                FontWeight.Bold
        )


        Spacer(
            Modifier.height(
                16.dp
            )
        )


        // =================================================
        // ROOM
        // =================================================

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
                            .surfaceVariant
                )
        ) {

            Row(
                modifier =
                    Modifier.padding(
                        18.dp
                    ),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Box(
                    modifier = Modifier
                        .size(
                            48.dp
                        )
                        .background(
                            MaterialTheme
                                .colorScheme
                                .primary
                                .copy(
                                    alpha =
                                        0.12f
                                ),
                            CircleShape
                        ),
                    contentAlignment =
                        Alignment.Center
                ) {

                    Icon(
                        imageVector =
                            Icons.Filled.School,
                        contentDescription =
                            null,
                        tint =
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
                                "Room A102",
                                "Phòng A102"
                            ),
                        fontWeight =
                            FontWeight.Bold,
                        fontSize =
                            19.sp
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


                AdminStatusChip(
                    text =
                        if (
                            devicesOnline > 0
                        ) {
                            "ONLINE"
                        } else {
                            "OFFLINE"
                        },
                    color =
                        if (
                            devicesOnline > 0
                        ) {

                            Color(
                                0xFF2E7D32
                            )

                        } else {

                            MaterialTheme
                                .colorScheme
                                .error
                        }
                )
            }
        }


        Spacer(
            Modifier.height(
                12.dp
            )
        )


        // =================================================
        // NEW DOOR STATUS
        // =================================================

        AdminDoorStatusCard(
            language =
                language,
            online =
                devicesOnline > 0,
            doorStatus =
                doorStatus,
            realtime =
                doorStatusFromFirestore
        )


        Spacer(
            Modifier.height(
                14.dp
            )
        )


        // =================================================
        // ATTENDANCE
        // =================================================

        if (
            attendanceOpen
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
                            Color(
                                0xFF2E7D32
                            ).copy(
                                alpha =
                                    0.12f
                            )
                    ),
                border =
                    BorderStroke(
                        1.dp,
                        Color(
                            0xFF2E7D32
                        ).copy(
                            alpha =
                                0.25f
                        )
                    )
            ) {

                Row(
                    modifier =
                        Modifier.padding(
                            14.dp
                        ),
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Icon(
                        imageVector =
                            Icons.Filled.CheckCircle,
                        contentDescription =
                            null,
                        tint =
                            Color(
                                0xFF66BB6A
                            )
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
                                    "ATTENDANCE OPEN",
                                    "ĐIỂM DANH ĐANG MỞ"
                                ),
                            color =
                                Color(
                                    0xFF66BB6A
                                ),
                            fontWeight =
                                FontWeight.Bold
                        )

                        Text(
                            text =
                                attendanceSubject,
                            fontSize =
                                12.sp
                        )
                    }
                }
            }


            Spacer(
                Modifier.height(
                    18.dp
                )
            )
        }


        Text(
            text =
                appText(
                    language,
                    "Room A102 Overview",
                    "Tổng quan phòng A102"
                ),
            fontSize =
                19.sp,
            fontWeight =
                FontWeight.Bold
        )


        Spacer(
            Modifier.height(
                12.dp
            )
        )


        if (
            isLoading
        ) {

            CircularProgressIndicator()

            Spacer(
                Modifier.height(
                    12.dp
                )
            )
        }


        if (
            errorMessage.isNotBlank()
        ) {

            Text(
                text =
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


        // =================================================
        // STATS
        // =================================================

        Row(
            modifier =
                Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(
                    12.dp
                )
        ) {

            AdminStatCard(
                modifier =
                    Modifier.weight(1f),
                icon =
                    Icons.Filled.People,
                iconBackground =
                    Color(
                        0xFFE7F0FF
                    ),
                iconColor =
                    Color(
                        0xFF1565C0
                    ),
                value =
                    totalStudents.toString(),
                title =
                    appText(
                        language,
                        "Students",
                        "Sinh viên"
                    ),
                subtitle =
                    appText(
                        language,
                        "Registered",
                        "Đã đăng ký"
                    )
            )


            AdminStatCard(
                modifier =
                    Modifier.weight(1f),
                icon =
                    Icons.Filled.HourglassTop,
                iconBackground =
                    Color(
                        0xFFFFF3E0
                    ),
                iconColor =
                    Color(
                        0xFFEF6C00
                    ),
                value =
                    pendingStudents.toString(),
                title =
                    appText(
                        language,
                        "Pending",
                        "Chờ duyệt"
                    ),
                subtitle =
                    appText(
                        language,
                        "A102 access",
                        "Quyền vào A102"
                    )
            )
        }


        Spacer(
            Modifier.height(
                12.dp
            )
        )


        Row(
            modifier =
                Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(
                    12.dp
                )
        ) {

            AdminStatCard(
                modifier =
                    Modifier.weight(1f),
                icon =
                    Icons
                        .AutoMirrored
                        .Filled
                        .ReceiptLong,
                iconBackground =
                    Color(
                        0xFFE8F5E9
                    ),
                iconColor =
                    Color(
                        0xFF2E7D32
                    ),
                value =
                    accessToday.toString(),
                title =
                    appText(
                        language,
                        "Today's access",
                        "Lượt vào hôm nay"
                    ),
                subtitle =
                    "A102"
            )


            AdminStatCard(
                modifier =
                    Modifier.weight(1f),
                icon =
                    Icons.Filled.Memory,
                iconBackground =
                    Color(
                        0xFFF3E5F5
                    ),
                iconColor =
                    Color(
                        0xFF7B1FA2
                    ),
                value =
                    devicesOnline.toString(),
                title =
                    appText(
                        language,
                        "Door device",
                        "Thiết bị cửa"
                    ),
                subtitle =
                    appText(
                        language,
                        "Online",
                        "Trực tuyến"
                    )
            )
        }


        Spacer(
            Modifier.height(
                24.dp
            )
        )


        // =================================================
        // MANAGEMENT
        // =================================================

        Text(
            text =
                appText(
                    language,
                    "Room A102 Management",
                    "Quản lý phòng A102"
                ),
            fontSize =
                19.sp,
            fontWeight =
                FontWeight.Bold
        )


        Spacer(
            Modifier.height(
                12.dp
            )
        )


        Row(
            modifier =
                Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(
                    12.dp
                )
        ) {

            AdminActionCard(
                modifier =
                    Modifier.weight(1f),
                icon =
                    Icons.Filled.HowToReg,
                iconBackground =
                    Color(
                        0xFFE7F0FF
                    ),
                iconColor =
                    Color(
                        0xFF1565C0
                    ),
                title =
                    appText(
                        language,
                        "Approve students",
                        "Duyệt sinh viên"
                    ),
                subtitle =
                    appText(
                        language,
                        "A102 access",
                        "Quyền vào A102"
                    ),
                onClick =
                    onOpenPendingStudents
            )


            AdminActionCard(
                modifier =
                    Modifier.weight(1f),
                icon =
                    Icons
                        .AutoMirrored
                        .Filled
                        .ReceiptLong,
                iconBackground =
                    Color(
                        0xFFFFF3E0
                    ),
                iconColor =
                    Color(
                        0xFFEF6C00
                    ),
                title =
                    appText(
                        language,
                        "Access history",
                        "Lịch sử ra vào"
                    ),
                subtitle =
                    "A102",
                onClick =
                    onOpenAccessLogs
            )
        }


        Spacer(
            Modifier.height(
                12.dp
            )
        )


        Row(
            modifier =
                Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(
                    12.dp
                )
        ) {

            AdminActionCard(
                modifier =
                    Modifier.weight(1f),
                icon =
                    Icons.Filled
                        .SettingsInputAntenna,
                iconBackground =
                    Color(
                        0xFFE8F5E9
                    ),
                iconColor =
                    Color(
                        0xFF2E7D32
                    ),
                title =
                    appText(
                        language,
                        "A102 door",
                        "Cửa phòng A102"
                    ),
                subtitle =
                    appText(
                        language,
                        "Monitor ESP32",
                        "Giám sát ESP32"
                    ),
                onClick =
                    onOpenDevices
            )


            AdminActionCard(
                modifier =
                    Modifier.weight(1f),
                icon =
                    Icons.Filled.VpnKey,
                iconBackground =
                    Color(
                        0xFFF3E5F5
                    ),
                iconColor =
                    Color(
                        0xFF7B1FA2
                    ),
                title =
                    appText(
                        language,
                        "Credentials",
                        "Xác thực"
                    ),
                subtitle =
                    appText(
                        language,
                        "NFC & fingerprint",
                        "NFC & vân tay"
                    ),
                onClick =
                    onOpenCredentials
            )
        }


        Spacer(
            Modifier.height(
                12.dp
            )
        )


        AdminActionCard(
            modifier =
                Modifier.fillMaxWidth(),
            icon =
                Icons.Filled.CheckCircle,
            iconBackground =
                Color(
                    0xFFE8F5E9
                ),
            iconColor =
                Color(
                    0xFF2E7D32
                ),
            title =
                appText(
                    language,
                    "ATTENDANCE",
                    "ĐIỂM DANH"
                ),
            subtitle =
                appText(
                    language,
                    "Create and manage A102 attendance sessions",
                    "Tạo và quản lý buổi điểm danh A102"
                ),
            onClick =
                onOpenAttendance
        )


        Spacer(
            Modifier.height(
                26.dp
            )
        )


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
                        Color(
                            0xFF2E7D32
                        ).copy(
                            alpha =
                                0.08f
                        )
                ),
            border =
                BorderStroke(
                    1.dp,
                    Color(
                        0xFF2E7D32
                    ).copy(
                        alpha =
                            0.20f
                    )
                )
        ) {

            Row(
                modifier =
                    Modifier.padding(
                        18.dp
                    ),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Icon(
                    imageVector =
                        Icons.Filled.CheckCircle,
                    contentDescription =
                        null,
                    tint =
                        Color(
                            0xFF66BB6A
                        )
                )

                Text(
                    text =
                        appText(
                            language,
                            "Firebase connected",
                            "Firebase đã kết nối"
                        ),
                    modifier =
                        Modifier.padding(
                            start =
                                12.dp
                        ),
                    fontWeight =
                        FontWeight.SemiBold
                )
            }
        }


        Spacer(
            Modifier.height(
                30.dp
            )
        )
    }
}


// =========================================================
// DOOR STATUS
// =========================================================

@Composable
private fun AdminDoorStatusCard(
    language: AppLanguage,
    online: Boolean,
    doorStatus: String,
    realtime: Boolean
) {

    val open =
        doorStatus == "open"

    val unknown =
        doorStatus == "unknown"


    val doorColor =
        when {

            open ->
                Color(
                    0xFFFF9800
                )

            unknown ->
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant

            else ->
                Color(
                    0xFF2E7D32
                )
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
                    doorColor.copy(
                        alpha =
                            0.08f
                    )
            ),
        border =
            BorderStroke(
                1.dp,
                doorColor.copy(
                    alpha =
                        0.24f
                )
            )
    ) {

        Row(
            modifier =
                Modifier.padding(
                    18.dp
                ),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .size(
                        50.dp
                    )
                    .background(
                        doorColor.copy(
                            alpha =
                                0.14f
                        ),
                        CircleShape
                    ),
                contentAlignment =
                    Alignment.Center
            ) {

                Icon(
                    imageVector =
                        Icons.Filled.VpnKey,
                    contentDescription =
                        null,
                    tint =
                        doorColor
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
                            "Door Status",
                            "Trạng thái cửa"
                        ),
                    fontSize =
                        16.sp,
                    fontWeight =
                        FontWeight.Bold
                )


                Text(
                    text =
                        if (
                            realtime
                        ) {

                            appText(
                                language,
                                "Realtime status from ESP32",
                                "Trạng thái trực tiếp từ ESP32"
                            )

                        } else {

                            appText(
                                language,
                                "Status follows successful access events",
                                "Trạng thái theo lượt truy cập thành công"
                            )
                        },
                    fontSize =
                        11.sp,
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )


                Text(
                    text =
                        if (
                            online
                        ) {

                            appText(
                                language,
                                "Device online",
                                "Thiết bị trực tuyến"
                            )

                        } else {

                            appText(
                                language,
                                "Device offline",
                                "Thiết bị ngoại tuyến"
                            )
                        },
                    fontSize =
                        11.sp,
                    color =
                        if (
                            online
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
            }


            AdminStatusChip(
                text =
                    when {

                        open ->
                            "OPEN"

                        unknown ->
                            "UNKNOWN"

                        else ->
                            "CLOSED"
                    },
                color =
                    doorColor
            )
        }
    }
}


// =========================================================
// STATUS CHIP
// =========================================================

@Composable
private fun AdminStatusChip(
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
                    0.12f
            ),
        border =
            BorderStroke(
                1.dp,
                color.copy(
                    alpha =
                        0.26f
                )
            )
    ) {

        Text(
            text =
                text,
            modifier =
                Modifier.padding(
                    horizontal =
                        10.dp,
                    vertical =
                        6.dp
                ),
            color =
                color,
            fontSize =
                10.sp,
            fontWeight =
                FontWeight.Bold
        )
    }
}


// =========================================================
// STAT CARD
// =========================================================

@Composable
fun AdminStatCard(
    modifier: Modifier,
    icon: ImageVector,
    iconBackground: Color,
    iconColor: Color,
    value: String,
    title: String,
    subtitle: String
) {

    Card(
        modifier =
            modifier,
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
            )
    ) {

        Column(
            modifier =
                Modifier.padding(
                    16.dp
                )
        ) {

            Box(
                modifier = Modifier
                    .size(
                        44.dp
                    )
                    .background(
                        iconBackground,
                        CircleShape
                    ),
                contentAlignment =
                    Alignment.Center
            ) {

                Icon(
                    imageVector =
                        icon,
                    contentDescription =
                        null,
                    tint =
                        iconColor
                )
            }


            Spacer(
                Modifier.height(
                    10.dp
                )
            )


            Text(
                text =
                    value,
                fontSize =
                    24.sp,
                fontWeight =
                    FontWeight.Bold
            )


            Text(
                text =
                    title,
                fontWeight =
                    FontWeight.SemiBold
            )


            Text(
                text =
                    subtitle,
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


// =========================================================
// ACTION CARD
// =========================================================

@Composable
fun AdminActionCard(
    modifier: Modifier,
    icon: ImageVector,
    iconBackground: Color,
    iconColor: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {

    Card(
        modifier =
            modifier,
        onClick =
            onClick,
        shape =
            RoundedCornerShape(
                20.dp
            ),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    MaterialTheme
                        .colorScheme
                        .surfaceVariant
            )
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    16.dp
                ),
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Box(
                modifier = Modifier
                    .size(
                        44.dp
                    )
                    .background(
                        iconBackground,
                        CircleShape
                    ),
                contentAlignment =
                    Alignment.Center
            ) {

                Icon(
                    imageVector =
                        icon,
                    contentDescription =
                        null,
                    tint =
                        iconColor
                )
            }


            Spacer(
                Modifier.height(
                    10.dp
                )
            )


            Text(
                text =
                    title,
                fontWeight =
                    FontWeight.SemiBold
            )


            Text(
                text =
                    subtitle,
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


// =========================================================
// ACCESS SUMMARY
// =========================================================

@Composable
fun AdminAccessSummaryCard(
    language: AppLanguage,
    total: Int,
    granted: Int,
    denied: Int,
    latestMessage: String
) {

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
                        "Tổng quan truy cập hôm nay"
                    ),
                fontWeight =
                    FontWeight.Bold
            )


            Spacer(
                Modifier.height(
                    14.dp
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

                AdminAccessMetric(
                    modifier =
                        Modifier.weight(1f),
                    value =
                        total,
                    label =
                        appText(
                            language,
                            "Total",
                            "Tổng"
                        ),
                    color =
                        MaterialTheme
                            .colorScheme
                            .primary
                )


                AdminAccessMetric(
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
                            0xFF2E7D32
                        )
                )


                AdminAccessMetric(
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


            Spacer(
                Modifier.height(
                    14.dp
                )
            )


            Text(
                text =
                    appText(
                        language,
                        "Latest scan",
                        "Lượt quét mới nhất"
                    ),
                fontWeight =
                    FontWeight.SemiBold
            )


            Text(
                text =
                    latestMessage.ifBlank {

                        appText(
                            language,
                            "Waiting for a new scan...",
                            "Đang chờ lượt quét mới..."
                        )
                    },
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


@Composable
private fun AdminAccessMetric(
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
                14.dp
            ),
        color =
            color.copy(
                alpha =
                    0.08f
            )
    ) {

        Column(
            modifier =
                Modifier.padding(
                    vertical =
                        12.dp
                ),
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Text(
                text =
                    value.toString(),
                fontSize =
                    21.sp,
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