package com.hcmute.smartaccess

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Memory
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale


data class A102Device(
    val id: String = "",
    val name: String = "",
    val location: String = "",
    val status: String = "",
    val nfcStatus: String = "",
    val fingerprintStatus: String = "",
    val lastSeen: Timestamp? = null
)


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceManagementScreen(
    language: AppLanguage,
    onBack: () -> Unit
) {

    val firestore =
        FirebaseFirestore.getInstance()


    var device by remember {
        mutableStateOf<A102Device?>(
            null
        )
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
                .collection("devices")
                .document(
                    ClassroomConfig.DEVICE_ID
                )
                .addSnapshotListener { document, error ->

                    if (error != null) {

                        isLoading =
                            false

                        errorMessage =
                            appText(
                                language,
                                "Unable to load A102 door controller.",
                                "Không thể tải bộ điều khiển cửa phòng A102."
                            )

                        return@addSnapshotListener
                    }


                    if (
                        document != null &&
                        document.exists()
                    ) {

                        device =
                            A102Device(

                                id =
                                    document.id,

                                name =
                                    document.getString(
                                        "name"
                                    )
                                        ?: ClassroomConfig.DEVICE_NAME,

                                location =
                                    document.getString(
                                        "location"
                                    )
                                        ?: ClassroomConfig.ROOM_CODE,

                                status =
                                    document.getString(
                                        "status"
                                    ) ?: "offline",

                                nfcStatus =
                                    document.getString(
                                        "nfcReaderStatus"
                                    ) ?: "unknown",

                                fingerprintStatus =
                                    document.getString(
                                        "fingerprintSensorStatus"
                                    ) ?: "unknown",

                                lastSeen =
                                    document.getTimestamp(
                                        "lastSeen"
                                    )
                            )

                    } else {

                        device =
                            null
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
                                    "A102 Door Controller",
                                    "Thiết bị cửa phòng A102"
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
                .padding(
                    20.dp
                )
        ) {


            if (isLoading) {

                CircularProgressIndicator()
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
                device == null
            ) {

                Card(
                    modifier =
                        Modifier.fillMaxWidth()
                ) {

                    Text(
                        text =
                            appText(
                                language,
                                "A102 door controller has not been connected yet.",
                                "Thiết bị cửa A102 chưa được kết nối."
                            ),

                        modifier =
                            Modifier.padding(
                                20.dp
                            )
                    )
                }
            }


            device?.let { item ->

                A102DeviceCard(
                    device =
                        item,

                    language =
                        language
                )
            }
        }
    }
}


@Composable
fun A102DeviceCard(
    device: A102Device,
    language: AppLanguage
) {

    val online =
        device.status.equals(
            "online",
            true
        )


    val nfcOnline =
        device.nfcStatus.equals(
            "online",
            true
        )


    val fingerprintOnline =
        device.fingerprintStatus.equals(
            "online",
            true
        )


    val lastSeen =
        device.lastSeen?.let {

            SimpleDateFormat(
                "dd/MM/yyyy HH:mm:ss",
                Locale.getDefault()
            ).format(
                it.toDate()
            )

        } ?: appText(
            language,
            "No data",
            "Chưa có dữ liệu"
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
                    20.dp
                )
        ) {


            Row(
                verticalAlignment =
                    Alignment.CenterVertically
            ) {


                Icon(
                    imageVector =
                        Icons.Filled.Memory,

                    contentDescription =
                        null,

                    tint =
                        MaterialTheme
                            .colorScheme
                            .primary
                )


                Text(
                    text =
                        device.name,

                    modifier =
                        Modifier.padding(
                            start = 10.dp
                        ),

                    fontSize =
                        18.sp,

                    fontWeight =
                        FontWeight.Bold
                )
            }


            Spacer(
                Modifier.height(
                    16.dp
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
                        "Device ID: ${device.id}",
                        "Mã thiết bị: ${device.id}"
                    )
            )


            Spacer(
                Modifier.height(
                    14.dp
                )
            )


            Text(
                text =
                    if (online) {

                        appText(
                            language,
                            "DEVICE ONLINE",
                            "THIẾT BỊ TRỰC TUYẾN"
                        )

                    } else {

                        appText(
                            language,
                            "DEVICE OFFLINE",
                            "THIẾT BỊ NGOẠI TUYẾN"
                        )
                    },

                color =
                    if (online) {

                        Color(
                            0xFF2E7D32
                        )

                    } else {

                        MaterialTheme
                            .colorScheme
                            .error
                    },

                fontWeight =
                    FontWeight.Bold
            )


            Spacer(
                Modifier.height(
                    18.dp
                )
            )


            A102HardwareStatusRow(
                icon =
                    Icons.Filled.CreditCard,

                label =
                    "NFC Reader",

                online =
                    nfcOnline,

                language =
                    language
            )


            Spacer(
                Modifier.height(
                    10.dp
                )
            )


            A102HardwareStatusRow(
                icon =
                    Icons.Filled.Fingerprint,

                label =
                    appText(
                        language,
                        "Fingerprint Sensor",
                        "Cảm biến vân tay"
                    ),

                online =
                    fingerprintOnline,

                language =
                    language
            )


            Spacer(
                Modifier.height(
                    18.dp
                )
            )


            Text(
                text =
                    appText(
                        language,
                        "Last seen: $lastSeen",
                        "Hoạt động gần nhất: $lastSeen"
                    ),

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
fun A102HardwareStatusRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    online: Boolean,
    language: AppLanguage
) {

    Row(
        modifier =
            Modifier.fillMaxWidth(),

        verticalAlignment =
            Alignment.CenterVertically
    ) {


        Icon(
            imageVector =
                icon,

            contentDescription =
                null,

            tint =
                if (online) {

                    Color(
                        0xFF2E7D32
                    )

                } else {

                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
                }
        )


        Text(
            text =
                label,

            modifier =
                Modifier
                    .weight(
                        1f
                    )
                    .padding(
                        start = 10.dp
                    )
        )


        Text(
            text =
                if (online) {

                    appText(
                        language,
                        "ONLINE",
                        "TRỰC TUYẾN"
                    )

                } else {

                    appText(
                        language,
                        "OFFLINE",
                        "NGOẠI TUYẾN"
                    )
                },

            fontSize =
                11.sp,

            fontWeight =
                FontWeight.Bold
        )
    }
}