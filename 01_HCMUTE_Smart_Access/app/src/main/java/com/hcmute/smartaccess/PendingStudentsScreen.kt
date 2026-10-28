package com.hcmute.smartaccess

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions


data class PendingStudentItem(
    val uid: String = "",
    val fullName: String = "",
    val studentId: String = "",
    val email: String = ""
)


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PendingStudentsScreen(
    language: AppLanguage,
    onBack: () -> Unit
) {

    val firestore =
        FirebaseFirestore.getInstance()


    val students =
        remember {
            mutableStateListOf<PendingStudentItem>()
        }


    var isLoading by remember {
        mutableStateOf(true)
    }


    var errorMessage by remember {
        mutableStateOf("")
    }


    var approvingUid by remember {
        mutableStateOf("")
    }


    // =====================================================
    // LOAD PENDING STUDENTS
    // =====================================================

    DisposableEffect(language) {

        val listener =
            firestore
                .collection("users")
                .whereEqualTo(
                    "status",
                    "pending"
                )
                .addSnapshotListener { snapshot, error ->

                    if (error != null) {

                        isLoading = false

                        errorMessage =
                            appText(
                                language,
                                "Failed to load A102 pending students.",
                                "Không thể tải sinh viên chờ duyệt phòng A102."
                            )

                        return@addSnapshotListener
                    }


                    students.clear()


                    snapshot
                        ?.documents
                        ?.filter {

                            it.getString(
                                "role"
                            ).equals(
                                "student",
                                true
                            )
                        }
                        ?.forEach { document ->

                            students.add(
                                PendingStudentItem(
                                    uid =
                                        document.id,

                                    fullName =
                                        document.getString(
                                            "fullName"
                                        ) ?: "Student",

                                    studentId =
                                        document.getString(
                                            "studentId"
                                        ) ?: "N/A",

                                    email =
                                        document.getString(
                                            "email"
                                        )
                                            ?: document.getString(
                                                "e-mail"
                                            )
                                            ?: "N/A"
                                )
                            )
                        }


                    isLoading = false
                }


        onDispose {
            listener.remove()
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
                                    "A102 Access Approval",
                                    "Duyệt quyền vào phòng A102"
                                ),

                            fontWeight =
                                FontWeight.Bold
                        )


                        Text(
                            text =
                                appText(
                                    language,
                                    "Approve student classroom access",
                                    "Phê duyệt quyền ra vào cho sinh viên"
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
                    horizontal =
                        20.dp,

                    vertical =
                        18.dp
                )
        ) {


            // =================================================
            // SUMMARY CARD
            // =================================================

            Card(
                modifier =
                    Modifier.fillMaxWidth(),

                shape =
                    RoundedCornerShape(
                        20.dp
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
                                46.dp
                            )
                            .clip(
                                CircleShape
                            )
                            .background(
                                Color(
                                    0xFFFFF3E0
                                )
                            ),

                        contentAlignment =
                            Alignment.Center
                    ) {


                        Icon(
                            imageVector =
                                Icons.Filled.HowToReg,

                            contentDescription =
                                null,

                            tint =
                                Color(
                                    0xFFEF6C00
                                )
                        )
                    }


                    Column(
                        modifier =
                            Modifier.padding(
                                start = 14.dp
                            )
                    ) {


                        Text(
                            text =
                                students.size.toString(),

                            fontSize =
                                25.sp,

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
                                    "Waiting for A102 access approval",
                                    "Đang chờ cấp quyền vào A102"
                                ),

                            fontSize =
                                13.sp,

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
                    20.dp
                )
            )


            // =================================================
            // LOADING
            // =================================================

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


            // =================================================
            // ERROR
            // =================================================

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


            // =================================================
            // EMPTY STATE
            // =================================================

            if (
                !isLoading &&
                students.isEmpty()
            ) {

                Card(
                    modifier =
                        Modifier.fillMaxWidth()
                ) {

                    Text(
                        text =
                            appText(
                                language,
                                "No students are waiting for A102 access approval.",
                                "Không có sinh viên đang chờ duyệt quyền vào A102."
                            ),

                        modifier =
                            Modifier.padding(
                                20.dp
                            )
                    )
                }
            }


            // =================================================
            // PENDING STUDENT LIST
            // =================================================

            students.forEach { student ->

                A102PendingStudentCard(

                    student =
                        student,

                    language =
                        language,

                    isApproving =
                        approvingUid ==
                                student.uid,

                    onApprove = {

                        approvingUid =
                            student.uid

                        errorMessage =
                            ""


                        val userRef =
                            firestore
                                .collection(
                                    "users"
                                )
                                .document(
                                    student.uid
                                )


                        val credentialRef =
                            firestore
                                .collection(
                                    "credentials"
                                )
                                .document(
                                    student.uid
                                )


                        val credentialData =
                            hashMapOf<String, Any>(
                                "userId" to
                                        student.uid,

                                "studentId" to
                                        student.studentId,

                                "nfcUid" to
                                        "",

                                "nfcStatus" to
                                        "not linked",

                                "fingerprintEnrolled" to
                                        false,

                                "fingerprintSlot" to
                                        0L,

                                "updatedAt" to
                                        FieldValue.serverTimestamp()
                            )


                        val batch =
                            firestore.batch()


                        batch.update(
                            userRef,
                            "status",
                            "active"
                        )


                        batch.set(
                            credentialRef,
                            credentialData,
                            SetOptions.merge()
                        )


                        batch
                            .commit()
                            .addOnSuccessListener {

                                approvingUid =
                                    ""
                            }
                            .addOnFailureListener { error ->

                                approvingUid =
                                    ""

                                errorMessage =
                                    appText(
                                        language,
                                        "Approval failed: ${error.message}",
                                        "Phê duyệt thất bại: ${error.message}"
                                    )
                            }
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
// PENDING STUDENT CARD
// =========================================================

@Composable
fun A102PendingStudentCard(
    student: PendingStudentItem,
    language: AppLanguage,
    isApproving: Boolean,
    onApprove: () -> Unit
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


            Row(
                verticalAlignment =
                    Alignment.CenterVertically
            ) {


                Box(
                    modifier = Modifier
                        .size(
                            44.dp
                        )
                        .clip(
                            CircleShape
                        )
                        .background(
                            Color(
                                0xFFE7F0FF
                            )
                        ),

                    contentAlignment =
                        Alignment.Center
                ) {

                    Icon(
                        imageVector =
                            Icons.Filled.Person,

                        contentDescription =
                            null,

                        tint =
                            Color(
                                0xFF1565C0
                            )
                    )
                }


                Column(
                    modifier =
                        Modifier.padding(
                            start = 13.dp
                        )
                ) {


                    Text(
                        text =
                            student.fullName,

                        fontWeight =
                            FontWeight.SemiBold,

                        fontSize =
                            16.sp
                    )


                    Text(
                        text =
                            appText(
                                language,
                                "Waiting for A102 access",
                                "Chờ quyền vào phòng A102"
                            ),

                        fontSize =
                            12.sp,

                        color =
                            Color(
                                0xFFEF6C00
                            )
                    )
                }
            }


            Spacer(
                Modifier.height(
                    18.dp
                )
            )


            Text(
                text =
                    appText(
                        language,
                        "Student ID: ${student.studentId}",
                        "Mã số sinh viên: ${student.studentId}"
                    )
            )


            Spacer(
                Modifier.height(
                    5.dp
                )
            )


            Text(
                text =
                    student.email
            )


            Spacer(
                Modifier.height(
                    18.dp
                )
            )


            Button(
                onClick =
                    onApprove,

                enabled =
                    !isApproving,

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(
                            48.dp
                        ),

                colors =
                    ButtonDefaults
                        .buttonColors(
                            containerColor =
                                Color(
                                    0xFF2E7D32
                                ),

                            contentColor =
                                Color.White
                        )
            ) {


                if (isApproving) {

                    CircularProgressIndicator(
                        modifier =
                            Modifier.size(
                                20.dp
                            ),

                        strokeWidth =
                            2.dp,

                        color =
                            Color.White
                    )

                } else {

                    Icon(
                        imageVector =
                            Icons.Filled.HowToReg,

                        contentDescription =
                            null
                    )


                    Text(
                        text =
                            appText(
                                language,
                                "APPROVE A102 ACCESS",
                                "CẤP QUYỀN VÀO A102"
                            ),

                        modifier =
                            Modifier.padding(
                                start = 8.dp
                            ),

                        fontWeight =
                            FontWeight.SemiBold
                    )
                }
            }
        }
    }
}