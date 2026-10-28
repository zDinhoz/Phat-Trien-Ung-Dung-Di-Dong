package com.hcmute.smartaccess

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale


data class AccessLog(
    val fullName: String = "",
    val studentId: String = "",
    val deviceId: String = "",
    val location: String = "",
    val nfcResult: Boolean = false,
    val fingerprintResult: Boolean = false,
    val result: String = "",
    val timestamp: Timestamp? = null
)


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccessLogsScreen(
    language: AppLanguage,
    onBack: () -> Unit
) {

    val firestore =
        FirebaseFirestore.getInstance()


    val logs =
        remember {
            mutableStateListOf<AccessLog>()
        }


    var isLoading by remember {
        mutableStateOf(true)
    }

    var errorMessage by remember {
        mutableStateOf("")
    }


    DisposableEffect(language) {

        val listener =
            firestore
                .collection("access_logs")
                .addSnapshotListener { snapshot, error ->

                    if (error != null) {

                        isLoading =
                            false

                        errorMessage =
                            appText(
                                language,
                                "Failed to load A102 access logs.",
                                "Không thể tải lịch sử ra vào phòng A102."
                            )

                        return@addSnapshotListener
                    }


                    logs.clear()


                    val result =
                        snapshot
                            ?.documents
                            ?.filter {

                                it.getString(
                                    "deviceId"
                                ) ==
                                        ClassroomConfig.DEVICE_ID
                            }
                            ?.map { document ->

                                AccessLog(

                                    fullName =
                                        document.getString(
                                            "fullName"
                                        ) ?: "Student",

                                    studentId =
                                        document.getString(
                                            "studentId"
                                        ) ?: "N/A",

                                    deviceId =
                                        document.getString(
                                            "deviceId"
                                        )
                                            ?: ClassroomConfig.DEVICE_ID,

                                    location =
                                        document.getString(
                                            "location"
                                        )
                                            ?: ClassroomConfig.ROOM_CODE,

                                    nfcResult =
                                        document.getBoolean(
                                            "nfcResult"
                                        ) ?: false,

                                    fingerprintResult =
                                        document.getBoolean(
                                            "fingerprintResult"
                                        ) ?: false,

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
                            ?.sortedByDescending {

                                it.timestamp?.seconds
                                    ?: 0L
                            }


                    if (result != null) {

                        logs.addAll(
                            result
                        )
                    }


                    isLoading =
                        false
                }


        onDispose {
            listener.remove()
        }
    }


    Scaffold(

        containerColor =
            MaterialTheme
                .colorScheme
                .background,

        topBar = {

            TopAppBar(

                title = {

                    Column {

                        Text(
                            text =
                                appText(
                                    language,
                                    "A102 Access Logs",
                                    "Lịch sử ra vào phòng A102"
                                ),

                            fontWeight =
                                FontWeight.Bold
                        )


                        Text(
                            text =
                                ClassroomConfig.DEVICE_ID,

                            fontSize =
                                12.sp,

                            color =
                                MaterialTheme
                                    .colorScheme
                                    .onSurfaceVariant
                        )
                    }
                },

                navigationIcon = {

                    IconButton(
                        onClick =
                            onBack
                    ) {

                        Icon(
                            imageVector =
                                Icons.AutoMirrored
                                    .Filled
                                    .ArrowBack,

                            contentDescription =
                                appText(
                                    language,
                                    "Back",
                                    "Quay lại"
                                )
                        )
                    }
                }
            )
        }

    ) { innerPadding ->


        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    MaterialTheme
                        .colorScheme
                        .background
                )
                .padding(
                    innerPadding
                )
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(
                    20.dp
                )
        ) {


            Text(
                text =
                    appText(
                        language,
                        "${logs.size} A102 access events",
                        "${logs.size} lượt ra vào phòng A102"
                    ),

                fontWeight =
                    FontWeight.SemiBold,

                color =
                    MaterialTheme
                        .colorScheme
                        .onBackground
            )


            Spacer(
                Modifier.height(
                    16.dp
                )
            )


            if (isLoading) {

                Row(
                    modifier =
                        Modifier.fillMaxWidth(),

                    horizontalArrangement =
                        Arrangement.Center
                ) {

                    CircularProgressIndicator()
                }
            }


            if (
                errorMessage.isNotEmpty()
            ) {

                Text(
                    text =
                        errorMessage,

                    color =
                        MaterialTheme
                            .colorScheme
                            .error
                )
            }


            if (
                !isLoading &&
                logs.isEmpty()
            ) {

                Card(
                    modifier =
                        Modifier.fillMaxWidth()
                ) {

                    Text(
                        text =
                            appText(
                                language,
                                "No A102 access logs found.",
                                "Chưa có lịch sử ra vào phòng A102."
                            ),

                        modifier =
                            Modifier.padding(
                                20.dp
                            )
                    )
                }
            }


            logs.forEach { log ->

                A102AccessLogCard(
                    log =
                        log,

                    language =
                        language
                )


                Spacer(
                    Modifier.height(
                        12.dp
                    )
                )
            }
        }
    }
}


@Composable
fun A102AccessLogCard(
    log: AccessLog,
    language: AppLanguage
) {

    val granted =
        log.result.equals(
            "granted",
            true
        )


    val time =
        log.timestamp?.let {

            SimpleDateFormat(
                "dd/MM/yyyy HH:mm:ss",
                Locale.getDefault()
            ).format(
                it.toDate()
            )

        } ?: appText(
            language,
            "No timestamp",
            "Không có thời gian"
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


            Row(
                modifier =
                    Modifier.fillMaxWidth()
            ) {


                Text(
                    text =
                        log.fullName,

                    modifier =
                        Modifier.weight(
                            1f
                        ),

                    fontSize =
                        17.sp,

                    fontWeight =
                        FontWeight.SemiBold
                )


                Text(
                    text =
                        if (granted) {

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
                        if (granted) {

                            Color(
                                0xFF2E7D32
                            )

                        } else {

                            MaterialTheme
                                .colorScheme
                                .error
                        },

                    fontWeight =
                        FontWeight.Bold,

                    fontSize =
                        11.sp
                )
            }


            Spacer(
                Modifier.height(
                    12.dp
                )
            )


            Text(
                text =
                    appText(
                        language,
                        "Student ID: ${log.studentId}",
                        "Mã số sinh viên: ${log.studentId}"
                    )
            )


            Text(
                text =
                    appText(
                        language,
                        "Classroom: A102",
                        "Phòng học: A102"
                    )
            )


            Text(
                text =
                    appText(
                        language,
                        "Door Controller: ${log.deviceId}",
                        "Bộ điều khiển cửa: ${log.deviceId}"
                    )
            )


            Spacer(
                Modifier.height(
                    10.dp
                )
            )


            Text(
                text =
                    "NFC: " +
                            if (log.nfcResult) {

                                appText(
                                    language,
                                    "Verified",
                                    "Đã xác thực"
                                )

                            } else {

                                appText(
                                    language,
                                    "Failed",
                                    "Thất bại"
                                )
                            }
            )


            Text(
                text =
                    appText(
                        language,
                        "Fingerprint: ",
                        "Vân tay: "
                    ) +
                            if (
                                log.fingerprintResult
                            ) {

                                appText(
                                    language,
                                    "Verified",
                                    "Đã xác thực"
                                )

                            } else {

                                appText(
                                    language,
                                    "Failed",
                                    "Thất bại"
                                )
                            }
            )


            Spacer(
                Modifier.height(
                    10.dp
                )
            )


            Text(
                text =
                    time,

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