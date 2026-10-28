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
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.text.SimpleDateFormat
import java.util.Locale


data class A102Credential(
    val userId: String = "",
    val fullName: String = "",
    val studentId: String = "",
    val nfcUid: String = "",
    val nfcStatus: String = "not linked",
    val fingerprintEnrolled: Boolean = false,
    val fingerprintSlot: Long = 0L,
    val updatedAt: Timestamp? = null
)


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CredentialManagementScreen(
    language: AppLanguage,
    onBack: () -> Unit
) {

    val firestore =
        FirebaseFirestore.getInstance()


    val credentials =
        remember {
            mutableStateListOf<A102Credential>()
        }


    var isLoading by remember {
        mutableStateOf(true)
    }


    var errorMessage by remember {
        mutableStateOf("")
    }


    var busyUserId by remember {
        mutableStateOf("")
    }


    var deleteFingerprintUserId by remember {
        mutableStateOf("")
    }


    var deleteFingerprintStatus by remember {
        mutableStateOf("")
    }


    // =====================================================
    // LOAD CREDENTIALS
    // =====================================================

    DisposableEffect(language) {

        val listener =
            firestore
                .collection("credentials")
                .addSnapshotListener { snapshot, error ->

                    if (error != null) {

                        isLoading = false

                        errorMessage =
                            appText(
                                language,
                                "Unable to load A102 credentials.",
                                "Không thể tải thông tin xác thực phòng A102."
                            )

                        return@addSnapshotListener
                    }


                    val newItems =
                        snapshot
                            ?.documents
                            ?.map { document ->

                                A102Credential(

                                    userId =
                                        document.getString("userId")
                                            ?: document.id,

                                    fullName =
                                        appText(
                                            language,
                                            "Student",
                                            "Sinh viên"
                                        ),

                                    studentId =
                                        document.getString("studentId")
                                            ?: "N/A",

                                    nfcUid =
                                        document.getString("nfcUid")
                                            ?: "",

                                    nfcStatus =
                                        document.getString("nfcStatus")
                                            ?: "not linked",

                                    fingerprintEnrolled =
                                        document.getBoolean(
                                            "fingerprintEnrolled"
                                        ) ?: false,

                                    fingerprintSlot =
                                        document.getLong(
                                            "fingerprintSlot"
                                        ) ?: 0L,

                                    updatedAt =
                                        document.getTimestamp(
                                            "updatedAt"
                                        )
                                )
                            }
                            ?: emptyList()


                    credentials.clear()
                    credentials.addAll(newItems)


                    newItems.forEach { item ->

                        firestore
                            .collection("users")
                            .document(item.userId)
                            .get()
                            .addOnSuccessListener { userDocument ->

                                val index =
                                    credentials.indexOfFirst {
                                        it.userId == item.userId
                                    }


                                if (index >= 0) {

                                    credentials[index] =
                                        credentials[index].copy(

                                            fullName =
                                                userDocument.getString(
                                                    "fullName"
                                                )
                                                    ?: credentials[index]
                                                        .fullName,

                                            studentId =
                                                userDocument.getString(
                                                    "studentId"
                                                )
                                                    ?: credentials[index]
                                                        .studentId
                                        )
                                }
                            }
                    }


                    isLoading = false
                }


        onDispose {
            listener.remove()
        }
    }


    // =====================================================
    // WATCH DELETE FINGERPRINT REQUEST
    // =====================================================

    DisposableEffect(Unit) {

        val listener =
            firestore
                .collection("enrollment_requests")
                .document(ClassroomConfig.DEVICE_ID)
                .addSnapshotListener { document, _ ->

                    if (
                        document == null ||
                        !document.exists()
                    ) {

                        deleteFingerprintUserId = ""
                        deleteFingerprintStatus = ""

                        return@addSnapshotListener
                    }


                    val action =
                        document.getString("action")
                            ?: ""


                    val status =
                        document.getString("status")
                            ?: ""


                    if (action == "delete_fingerprint") {

                        deleteFingerprintUserId =
                            document.getString("userId")
                                ?: ""

                        deleteFingerprintStatus =
                            status

                    } else {

                        deleteFingerprintUserId = ""
                        deleteFingerprintStatus = ""
                    }
                }


        onDispose {
            listener.remove()
        }
    }


    // =====================================================
    // REVOKE NFC
    // =====================================================

    fun revokeNfc(
        item: A102Credential
    ) {

        if (item.nfcUid.isBlank()) {
            return
        }


        busyUserId =
            item.userId

        errorMessage =
            ""


        val credentialRef =
            firestore
                .collection("credentials")
                .document(item.userId)


        val lookupRef =
            firestore
                .collection("nfc_lookup")
                .document(
                    item.nfcUid
                        .uppercase()
                )


        val batch =
            firestore.batch()


        batch.delete(
            lookupRef
        )


        batch.set(
            credentialRef,

            mapOf(
                "nfcUid" to "",
                "nfcStatus" to "not linked",
                "updatedAt" to
                        FieldValue.serverTimestamp()
            ),

            SetOptions.merge()
        )


        batch
            .commit()
            .addOnSuccessListener {

                busyUserId =
                    ""
            }
            .addOnFailureListener { error ->

                busyUserId =
                    ""

                errorMessage =
                    appText(
                        language,
                        "Unable to revoke NFC: ${error.message}",
                        "Không thể thu hồi NFC: ${error.message}"
                    )
            }
    }


    // =====================================================
    // REQUEST ESP32 TO DELETE FINGERPRINT
    // =====================================================

    fun requestFingerprintDelete(
        item: A102Credential
    ) {

        if (
            !item.fingerprintEnrolled ||
            item.fingerprintSlot <= 0L
        ) {
            return
        }


        busyUserId =
            item.userId

        errorMessage =
            ""


        val requestRef =
            firestore
                .collection("enrollment_requests")
                .document(
                    ClassroomConfig.DEVICE_ID
                )


        firestore
            .runTransaction { transaction ->

                val current =
                    transaction.get(
                        requestRef
                    )


                val currentStatus =
                    current.getString(
                        "status"
                    ) ?: ""


                if (
                    current.exists() &&
                    (
                            currentStatus == "waiting" ||
                                    currentStatus == "processing"
                            )
                ) {

                    throw IllegalStateException(
                        appText(
                            language,
                            "A102 device is processing another request.",
                            "Thiết bị A102 đang xử lý một yêu cầu khác."
                        )
                    )
                }


                val requestData =
                    hashMapOf<String, Any?>(
                        "deviceId" to
                                ClassroomConfig.DEVICE_ID,

                        "room" to
                                "A102",

                        "action" to
                                "delete_fingerprint",

                        "userId" to
                                item.userId,

                        "studentId" to
                                item.studentId,

                        "fullName" to
                                item.fullName,

                        "status" to
                                "waiting",

                        "fingerprintSlot" to
                                item.fingerprintSlot,

                        "credentialValue" to
                                item.fingerprintSlot
                                    .toString(),

                        "message" to
                                "",

                        "requestedAt" to
                                FieldValue.serverTimestamp(),

                        "completedAt" to
                                null
                    )


                transaction.set(
                    requestRef,
                    requestData,
                    SetOptions.merge()
                )
            }
            .addOnSuccessListener {

                busyUserId =
                    ""
            }
            .addOnFailureListener { error ->

                busyUserId =
                    ""

                errorMessage =
                    error.message
                        ?: appText(
                            language,
                            "Unable to request fingerprint deletion.",
                            "Không thể gửi yêu cầu xóa vân tay."
                        )
            }
    }


    // =====================================================
    // UI
    // =====================================================

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
                                    "A102 Access Credentials",
                                    "Xác thực ra vào phòng A102"
                                ),

                            fontWeight =
                                FontWeight.Bold
                        )


                        Text(
                            text =
                                appText(
                                    language,
                                    "View, revoke NFC and remove fingerprints",
                                    "Xem, thu hồi NFC và xóa vân tay"
                                ),

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
                                Icons
                                    .AutoMirrored
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


            if (isLoading) {

                CircularProgressIndicator()

                Spacer(
                    Modifier.height(
                        16.dp
                    )
                )
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


                Spacer(
                    Modifier.height(
                        16.dp
                    )
                )
            }


            if (
                !isLoading &&
                credentials.isEmpty()
            ) {

                Card(
                    modifier =
                        Modifier.fillMaxWidth()
                ) {

                    Text(
                        text =
                            appText(
                                language,
                                "No A102 credentials found.",
                                "Chưa có dữ liệu xác thực cho phòng A102."
                            ),

                        modifier =
                            Modifier.padding(
                                20.dp
                            )
                    )
                }
            }


            credentials.forEach { item ->

                val deletingFingerprint =
                    deleteFingerprintUserId ==
                            item.userId &&
                            (
                                    deleteFingerprintStatus == "waiting" ||
                                            deleteFingerprintStatus == "processing"
                                    )


                A102CredentialCard(

                    credential =
                        item,

                    language =
                        language,

                    busy =
                        busyUserId ==
                                item.userId,

                    deletingFingerprint =
                        deletingFingerprint,

                    onRevokeNfc = {

                        revokeNfc(
                            item
                        )
                    },

                    onDeleteFingerprint = {

                        requestFingerprintDelete(
                            item
                        )
                    }
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


// =========================================================
// CREDENTIAL CARD
// =========================================================

@Composable
fun A102CredentialCard(
    credential: A102Credential,
    language: AppLanguage,
    busy: Boolean,
    deletingFingerprint: Boolean,
    onRevokeNfc: () -> Unit,
    onDeleteFingerprint: () -> Unit
) {

    val nfcLinked =
        credential
            .nfcStatus
            .equals(
                "linked",
                true
            ) &&
                credential.nfcUid.isNotBlank()


    val updated =
        credential
            .updatedAt
            ?.let {

                SimpleDateFormat(
                    "dd/MM/yyyy HH:mm:ss",
                    Locale.getDefault()
                )
                    .format(
                        it.toDate()
                    )

            }
            ?: appText(
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
                    18.dp
                )
        ) {


            Text(
                text =
                    credential.fullName,

                fontSize =
                    17.sp,

                fontWeight =
                    FontWeight.SemiBold
            )


            Text(
                text =
                    appText(
                        language,
                        "Student ID: ${credential.studentId}",
                        "Mã số sinh viên: ${credential.studentId}"
                    ),

                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
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
                        12.dp
                    )
            ) {


                A102CredentialStatus(

                    modifier =
                        Modifier.weight(
                            1f
                        ),

                    icon =
                        Icons.Filled.CreditCard,

                    title =
                        "NFC",

                    status =
                        if (nfcLinked) {

                            appText(
                                language,
                                "LINKED",
                                "ĐÃ LIÊN KẾT"
                            )

                        } else {

                            appText(
                                language,
                                "NOT LINKED",
                                "CHƯA LIÊN KẾT"
                            )
                        },

                    detail =
                        if (nfcLinked) {

                            credential.nfcUid

                        } else {

                            appText(
                                language,
                                "Not assigned",
                                "Chưa gán"
                            )
                        },

                    active =
                        nfcLinked
                )


                A102CredentialStatus(

                    modifier =
                        Modifier.weight(
                            1f
                        ),

                    icon =
                        Icons.Filled.Fingerprint,

                    title =
                        appText(
                            language,
                            "FINGERPRINT",
                            "VÂN TAY"
                        ),

                    status =
                        if (
                            credential
                                .fingerprintEnrolled
                        ) {

                            appText(
                                language,
                                "ENROLLED",
                                "ĐÃ ĐĂNG KÝ"
                            )

                        } else {

                            appText(
                                language,
                                "NOT ENROLLED",
                                "CHƯA ĐĂNG KÝ"
                            )
                        },

                    detail =
                        if (
                            credential
                                .fingerprintEnrolled &&
                            credential
                                .fingerprintSlot > 0L
                        ) {

                            appText(
                                language,
                                "Slot: ${credential.fingerprintSlot}",
                                "Vị trí: ${credential.fingerprintSlot}"
                            )

                        } else {

                            appText(
                                language,
                                "No fingerprint",
                                "Chưa có vân tay"
                            )
                        },

                    active =
                        credential
                            .fingerprintEnrolled
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
                        "Authentication: NFC OR fingerprint",
                        "Xác thực: NFC HOẶC vân tay"
                    ),

                fontSize =
                    12.sp,

                color =
                    MaterialTheme
                        .colorScheme
                        .primary
            )


            Spacer(
                Modifier.height(
                    12.dp
                )
            )


            if (nfcLinked) {

                OutlinedButton(

                    onClick =
                        onRevokeNfc,

                    enabled =
                        !busy &&
                                !deletingFingerprint,

                    modifier =
                        Modifier.fillMaxWidth()
                ) {

                    Text(
                        text =
                            appText(
                                language,
                                "REVOKE NFC",
                                "THU HỒI NFC"
                            ),

                        fontWeight =
                            FontWeight.SemiBold
                    )
                }


                Spacer(
                    Modifier.height(
                        8.dp
                    )
                )
            }


            if (
                credential
                    .fingerprintEnrolled
            ) {

                Button(

                    onClick =
                        onDeleteFingerprint,

                    enabled =
                        !busy &&
                                !deletingFingerprint,

                    modifier =
                        Modifier.fillMaxWidth(),

                    colors =
                        ButtonDefaults
                            .buttonColors(

                                containerColor =
                                    MaterialTheme
                                        .colorScheme
                                        .error,

                                contentColor =
                                    MaterialTheme
                                        .colorScheme
                                        .onError
                            )
                ) {


                    if (
                        busy ||
                        deletingFingerprint
                    ) {

                        CircularProgressIndicator(

                            modifier =
                                Modifier.height(
                                    20.dp
                                ),

                            strokeWidth =
                                2.dp,

                            color =
                                MaterialTheme
                                    .colorScheme
                                    .onError
                        )

                    } else {

                        Text(
                            text =
                                appText(
                                    language,
                                    "DELETE FINGERPRINT",
                                    "XÓA VÂN TAY"
                                ),

                            fontWeight =
                                FontWeight.SemiBold
                        )
                    }
                }


                if (
                    deletingFingerprint
                ) {

                    Spacer(
                        Modifier.height(
                            8.dp
                        )
                    )


                    Text(
                        text =
                            appText(
                                language,
                                "Waiting for ESP32 A102 to delete the sensor template...",
                                "Đang chờ ESP32 A102 xóa mẫu vân tay trong cảm biến..."
                            ),

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
                    10.dp
                )
            )


            Text(
                text =
                    appText(
                        language,
                        "Updated: $updated",
                        "Cập nhật: $updated"
                    ),

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
// STATUS CARD
// =========================================================

@Composable
fun A102CredentialStatus(
    modifier: Modifier,
    icon: ImageVector,
    title: String,
    status: String,
    detail: String,
    active: Boolean
) {

    Card(

        modifier =
            modifier,

        shape =
            RoundedCornerShape(
                16.dp
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
            modifier =
                Modifier.padding(
                    14.dp
                )
        ) {


            Icon(
                imageVector =
                    icon,

                contentDescription =
                    null,

                tint =
                    MaterialTheme
                        .colorScheme
                        .primary
            )


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
                    status,

                fontSize =
                    10.sp,

                fontWeight =
                    FontWeight.Bold,

                color =
                    if (active) {

                        Color(
                            0xFF2E7D32
                        )

                    } else {

                        MaterialTheme
                            .colorScheme
                            .error
                    }
            )


            Text(
                text =
                    detail,

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