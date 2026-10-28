package com.hcmute.smartaccess

import android.util.Patterns
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore


@Composable
fun RegisterScreen(
    language: AppLanguage,
    onBackToLogin: () -> Unit,
    onRegisterSuccess: () -> Unit
) {

    val auth =
        FirebaseAuth.getInstance()

    val firestore =
        FirebaseFirestore.getInstance()


    var fullName by remember {
        mutableStateOf("")
    }

    var studentId by remember {
        mutableStateOf("")
    }

    var email by remember {
        mutableStateOf("")
    }

    var password by remember {
        mutableStateOf("")
    }

    var confirmPassword by remember {
        mutableStateOf("")
    }

    var passwordVisible by remember {
        mutableStateOf(false)
    }

    var confirmPasswordVisible by remember {
        mutableStateOf(false)
    }

    var isLoading by remember {
        mutableStateOf(false)
    }

    var errorMessage by remember {
        mutableStateOf("")
    }


    // =====================================================
    // PASSWORD RULES
    // =====================================================

    val hasMinimumLength =
        password.length >= 8

    val hasUppercase =
        password.any {
            it.isUpperCase()
        }

    val hasLowercase =
        password.any {
            it.isLowerCase()
        }

    val hasNumber =
        password.any {
            it.isDigit()
        }

    val hasSpecialCharacter =
        password.any {
            !it.isLetterOrDigit() &&
                    !it.isWhitespace()
        }

    val strongPassword =
        hasMinimumLength &&
                hasUppercase &&
                hasLowercase &&
                hasNumber &&
                hasSpecialCharacter

    val passwordsMatch =
        password.isNotBlank() &&
                confirmPassword.isNotBlank() &&
                password == confirmPassword


    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                MaterialTheme
                    .colorScheme
                    .background
            )
            .verticalScroll(
                rememberScrollState()
            )
            .padding(
                horizontal = 28.dp,
                vertical = 24.dp
            ),

        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {

        Spacer(
            modifier =
                Modifier.height(
                    12.dp
                )
        )


        Image(
            painter =
                painterResource(
                    id = R.drawable.hcmute_logo
                ),

            contentDescription =
                "HCM-UTE Logo",

            modifier =
                Modifier.size(
                    90.dp
                )
        )


        Spacer(
            modifier =
                Modifier.height(
                    14.dp
                )
        )


        Text(
            text =
                appText(
                    language,
                    "Create Student Account",
                    "Tạo tài khoản sinh viên"
                ),

            fontSize =
                23.sp,

            fontWeight =
                FontWeight.Bold
        )


        Spacer(
            modifier =
                Modifier.height(
                    6.dp
                )
        )


        Text(
            text =
                appText(
                    language,
                    "The account must be approved by Admin before accessing room A102.",
                    "Tài khoản cần được Admin duyệt trước khi được phép vào phòng A102."
                ),

            fontSize =
                13.sp,

            color =
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant
        )


        Spacer(
            modifier =
                Modifier.height(
                    24.dp
                )
        )


        // =====================================================
        // FULL NAME
        // =====================================================

        OutlinedTextField(
            value =
                fullName,

            onValueChange = {
                fullName = it
            },

            label = {
                Text(
                    appText(
                        language,
                        "Full name",
                        "Họ và tên"
                    )
                )
            },

            singleLine =
                true,

            enabled =
                !isLoading,

            modifier =
                Modifier.fillMaxWidth()
        )


        Spacer(
            modifier =
                Modifier.height(
                    12.dp
                )
        )


        // =====================================================
        // STUDENT ID
        // =====================================================

        OutlinedTextField(
            value =
                studentId,

            onValueChange = { value ->

                studentId =
                    value.filter {
                        it.isDigit()
                    }
            },

            label = {
                Text(
                    appText(
                        language,
                        "Student ID",
                        "Mã số sinh viên"
                    )
                )
            },

            singleLine =
                true,

            enabled =
                !isLoading,

            keyboardOptions =
                KeyboardOptions(
                    keyboardType =
                        KeyboardType.Number
                ),

            modifier =
                Modifier.fillMaxWidth()
        )


        Spacer(
            modifier =
                Modifier.height(
                    12.dp
                )
        )


        // =====================================================
        // EMAIL
        // =====================================================

        OutlinedTextField(
            value =
                email,

            onValueChange = {
                email = it
            },

            label = {
                Text("Email")
            },

            singleLine =
                true,

            enabled =
                !isLoading,

            keyboardOptions =
                KeyboardOptions(
                    keyboardType =
                        KeyboardType.Email
                ),

            modifier =
                Modifier.fillMaxWidth()
        )


        Spacer(
            modifier =
                Modifier.height(
                    12.dp
                )
        )


        // =====================================================
        // PASSWORD
        // =====================================================

        OutlinedTextField(
            value =
                password,

            onValueChange = {
                password = it
            },

            label = {
                Text(
                    appText(
                        language,
                        "Password",
                        "Mật khẩu"
                    )
                )
            },

            visualTransformation =
                if (passwordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },

            trailingIcon = {

                IconButton(
                    onClick = {
                        passwordVisible =
                            !passwordVisible
                    }
                ) {

                    Icon(
                        imageVector =
                            if (passwordVisible) {
                                Icons.Filled.VisibilityOff
                            } else {
                                Icons.Filled.Visibility
                            },

                        contentDescription =
                            null
                    )
                }
            },

            singleLine =
                true,

            enabled =
                !isLoading,

            keyboardOptions =
                KeyboardOptions(
                    keyboardType =
                        KeyboardType.Password
                ),

            modifier =
                Modifier.fillMaxWidth()
        )


        Spacer(
            modifier =
                Modifier.height(
                    10.dp
                )
        )


        // =====================================================
        // PASSWORD REQUIREMENTS
        // =====================================================

        Column(
            modifier =
                Modifier.fillMaxWidth()
        ) {

            Text(
                text =
                    appText(
                        language,
                        "Password requirements:",
                        "Yêu cầu mật khẩu:"
                    ),

                fontSize =
                    13.sp,

                fontWeight =
                    FontWeight.SemiBold
            )


            Spacer(
                modifier =
                    Modifier.height(
                        6.dp
                    )
            )


            PasswordRequirementRow(
                fulfilled =
                    hasMinimumLength,

                text =
                    appText(
                        language,
                        "At least 8 characters",
                        "Ít nhất 8 ký tự"
                    )
            )


            PasswordRequirementRow(
                fulfilled =
                    hasUppercase,

                text =
                    appText(
                        language,
                        "At least 1 uppercase letter",
                        "Có ít nhất 1 chữ hoa"
                    )
            )


            PasswordRequirementRow(
                fulfilled =
                    hasLowercase,

                text =
                    appText(
                        language,
                        "At least 1 lowercase letter",
                        "Có ít nhất 1 chữ thường"
                    )
            )


            PasswordRequirementRow(
                fulfilled =
                    hasNumber,

                text =
                    appText(
                        language,
                        "At least 1 number",
                        "Có ít nhất 1 chữ số"
                    )
            )


            PasswordRequirementRow(
                fulfilled =
                    hasSpecialCharacter,

                text =
                    appText(
                        language,
                        "At least 1 special character",
                        "Có ít nhất 1 ký tự đặc biệt"
                    )
            )
        }


        Spacer(
            modifier =
                Modifier.height(
                    12.dp
                )
        )


        // =====================================================
        // CONFIRM PASSWORD
        // =====================================================

        OutlinedTextField(
            value =
                confirmPassword,

            onValueChange = {
                confirmPassword = it
            },

            label = {
                Text(
                    appText(
                        language,
                        "Confirm password",
                        "Xác nhận mật khẩu"
                    )
                )
            },

            visualTransformation =
                if (confirmPasswordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },

            trailingIcon = {

                IconButton(
                    onClick = {
                        confirmPasswordVisible =
                            !confirmPasswordVisible
                    }
                ) {

                    Icon(
                        imageVector =
                            if (confirmPasswordVisible) {
                                Icons.Filled.VisibilityOff
                            } else {
                                Icons.Filled.Visibility
                            },

                        contentDescription =
                            null
                    )
                }
            },

            singleLine =
                true,

            enabled =
                !isLoading,

            isError =
                confirmPassword.isNotEmpty() &&
                        !passwordsMatch,

            keyboardOptions =
                KeyboardOptions(
                    keyboardType =
                        KeyboardType.Password
                ),

            modifier =
                Modifier.fillMaxWidth()
        )


        if (
            confirmPassword.isNotEmpty() &&
            !passwordsMatch
        ) {

            Text(
                text =
                    appText(
                        language,
                        "Passwords do not match.",
                        "Mật khẩu xác nhận không khớp."
                    ),

                fontSize =
                    12.sp,

                color =
                    MaterialTheme
                        .colorScheme
                        .error,

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            top = 4.dp
                        )
            )
        }


        Spacer(
            modifier =
                Modifier.height(
                    12.dp
                )
        )


        // =====================================================
        // ERROR
        // =====================================================

        if (
            errorMessage.isNotBlank()
        ) {

            Text(
                text =
                    errorMessage,

                color =
                    MaterialTheme
                        .colorScheme
                        .error,

                fontSize =
                    13.sp,

                modifier =
                    Modifier.fillMaxWidth()
            )


            Spacer(
                modifier =
                    Modifier.height(
                        12.dp
                    )
            )
        }


        // =====================================================
        // REGISTER
        // =====================================================

        if (isLoading) {

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(
                            52.dp
                        ),

                horizontalArrangement =
                    Arrangement.Center,

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                CircularProgressIndicator(
                    modifier =
                        Modifier.size(
                            22.dp
                        ),

                    strokeWidth =
                        2.5.dp
                )


                Text(
                    text =
                        appText(
                            language,
                            "Creating account...",
                            "Đang tạo tài khoản..."
                        ),

                    modifier =
                        Modifier.padding(
                            start = 12.dp
                        )
                )
            }

        } else {

            Button(
                onClick = {

                    errorMessage =
                        ""


                    val cleanFullName =
                        fullName.trim()

                    val cleanStudentId =
                        studentId.trim()

                    val cleanEmail =
                        email
                            .trim()
                            .lowercase()


                    if (
                        cleanFullName.isBlank() ||
                        cleanStudentId.isBlank() ||
                        cleanEmail.isBlank()
                    ) {

                        errorMessage =
                            appText(
                                language,
                                "Please complete all information.",
                                "Vui lòng nhập đầy đủ thông tin."
                            )

                        return@Button
                    }


                    if (
                        !Patterns.EMAIL_ADDRESS
                            .matcher(cleanEmail)
                            .matches()
                    ) {

                        errorMessage =
                            appText(
                                language,
                                "Invalid email address.",
                                "Địa chỉ email không hợp lệ."
                            )

                        return@Button
                    }


                    if (!strongPassword) {

                        errorMessage =
                            appText(
                                language,
                                "Password does not meet the security requirements.",
                                "Mật khẩu chưa đáp ứng yêu cầu bảo mật."
                            )

                        return@Button
                    }


                    if (!passwordsMatch) {

                        errorMessage =
                            appText(
                                language,
                                "Passwords do not match.",
                                "Mật khẩu xác nhận không khớp."
                            )

                        return@Button
                    }


                    isLoading =
                        true


                    auth
                        .createUserWithEmailAndPassword(
                            cleanEmail,
                            password
                        )
                        .addOnSuccessListener {

                            val firebaseUser =
                                auth.currentUser


                            if (firebaseUser == null) {

                                isLoading =
                                    false

                                errorMessage =
                                    appText(
                                        language,
                                        "Unable to create Firebase user.",
                                        "Không thể tạo người dùng Firebase."
                                    )

                                return@addOnSuccessListener
                            }


                            val userDocument =
                                hashMapOf<String, Any>(
                                    "uid" to
                                            firebaseUser.uid,

                                    "fullName" to
                                            cleanFullName,

                                    "studentId" to
                                            cleanStudentId,

                                    "email" to
                                            cleanEmail,

                                    "role" to
                                            "student",

                                    "status" to
                                            "pending",

                                    "createdAt" to
                                            FieldValue
                                                .serverTimestamp(),

                                    "updatedAt" to
                                            FieldValue
                                                .serverTimestamp()
                                )


                            firestore
                                .collection("users")
                                .document(
                                    firebaseUser.uid
                                )
                                .set(
                                    userDocument
                                )
                                .addOnSuccessListener {

                                    isLoading =
                                        false

                                    // Giữ user đăng nhập để PendingApprovalScreen
                                    // có thể theo dõi tài khoản hiện tại.
                                    onRegisterSuccess()
                                }
                                .addOnFailureListener { error ->

                                    // Firebase Auth vừa được tạo nhưng
                                    // Firestore profile thất bại -> rollback.
                                    firebaseUser
                                        .delete()
                                        .addOnCompleteListener {

                                            auth.signOut()

                                            isLoading =
                                                false

                                            errorMessage =
                                                appText(
                                                    language,
                                                    "Unable to save student profile: ${error.message}",
                                                    "Không thể lưu hồ sơ sinh viên: ${error.message}"
                                                )
                                        }
                                }
                        }
                        .addOnFailureListener { error ->

                            isLoading =
                                false

                            errorMessage =
                                appText(
                                    language,
                                    "Account creation failed: ${error.message}",
                                    "Tạo tài khoản thất bại: ${error.message}"
                                )
                        }
                },

                enabled =
                    strongPassword &&
                            passwordsMatch,

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(
                            52.dp
                        )
            ) {

                Text(
                    text =
                        appText(
                            language,
                            "CREATE ACCOUNT",
                            "TẠO TÀI KHOẢN"
                        ),

                    fontWeight =
                        FontWeight.Bold
                )
            }
        }


        Spacer(
            modifier =
                Modifier.height(
                    10.dp
                )
        )


        TextButton(
            onClick =
                onBackToLogin,

            enabled =
                !isLoading
        ) {

            Text(
                text =
                    appText(
                        language,
                        "Back to login",
                        "Quay lại đăng nhập"
                    )
            )
        }


        Spacer(
            modifier =
                Modifier.height(
                    20.dp
                )
        )
    }
}


@Composable
fun PasswordRequirementRow(
    fulfilled: Boolean,
    text: String
) {

    Row(
        modifier =
            Modifier.padding(
                vertical = 2.dp
            ),

        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Icon(
            imageVector =
                if (fulfilled) {
                    Icons.Filled.CheckCircle
                } else {
                    Icons.Filled.Cancel
                },

            contentDescription =
                null,

            tint =
                if (fulfilled) {
                    Color(0xFF2E7D32)
                } else {
                    MaterialTheme
                        .colorScheme
                        .error
                },

            modifier =
                Modifier.size(
                    17.dp
                )
        )


        Text(
            text =
                text,

            fontSize =
                12.sp,

            color =
                if (fulfilled) {
                    Color(0xFF2E7D32)
                } else {
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
                },

            modifier =
                Modifier.padding(
                    start = 7.dp
                )
        )
    }
}