package com.hcmute.smartaccess

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore


@Composable
fun LoginScreen(
    language: AppLanguage,
    rememberLogin: Boolean,
    onRememberLoginChange: (Boolean) -> Unit,
    onCreateAccount: () -> Unit,
    onStudentLogin: () -> Unit,
    onAdminLogin: () -> Unit,
    onPendingAccount: () -> Unit
) {

    var email by remember {
        mutableStateOf("")
    }

    var password by remember {
        mutableStateOf("")
    }

    var passwordVisible by remember {
        mutableStateOf(false)
    }

    var isLoading by remember {
        mutableStateOf(false)
    }

    var errorMessage by remember {
        mutableStateOf("")
    }


    val auth =
        FirebaseAuth.getInstance()

    val firestore =
        FirebaseFirestore.getInstance()


    // =====================================================
    // LOGIN SCREEN
    // =====================================================

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                MaterialTheme
                    .colorScheme
                    .background
            )
            .padding(
                horizontal = 32.dp
            ),

        verticalArrangement =
            Arrangement.Center,

        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {

        // =================================================
        // LOGO
        // =================================================

        Image(
            painter =
                painterResource(
                    id = R.drawable.hcmute_logo
                ),

            contentDescription =
                "HCM-UTE Logo",

            modifier =
                Modifier.size(
                    110.dp
                )
        )


        Spacer(
            modifier =
                Modifier.height(
                    16.dp
                )
        )


        // =================================================
        // APP NAME
        // =================================================

        Text(
            text =
                "HCM-UTE Smart Access",

            fontSize =
                24.sp,

            fontWeight =
                FontWeight.Bold,

            color =
                MaterialTheme
                    .colorScheme
                    .onBackground
        )


        Spacer(
            modifier =
                Modifier.height(
                    8.dp
                )
        )


        Text(
            text =
                appText(
                    language,
                    "Login to your account",
                    "Đăng nhập vào tài khoản"
                ),

            fontSize =
                15.sp,

            color =
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant
        )


        Spacer(
            modifier =
                Modifier.height(
                    32.dp
                )
        )


        // =================================================
        // EMAIL
        // =================================================

        OutlinedTextField(
            value =
                email,

            onValueChange = {
                email = it

                if (
                    errorMessage.isNotBlank()
                ) {
                    errorMessage = ""
                }
            },

            label = {
                Text(
                    "Email"
                )
            },

            modifier =
                Modifier.fillMaxWidth(),

            singleLine =
                true,

            enabled =
                !isLoading,

            keyboardOptions =
                KeyboardOptions(
                    keyboardType =
                        KeyboardType.Email
                ),

            colors =
                OutlinedTextFieldDefaults.colors(

                    focusedTextColor =
                        MaterialTheme
                            .colorScheme
                            .onSurface,

                    unfocusedTextColor =
                        MaterialTheme
                            .colorScheme
                            .onSurface,

                    cursorColor =
                        MaterialTheme
                            .colorScheme
                            .primary,

                    focusedBorderColor =
                        MaterialTheme
                            .colorScheme
                            .primary,

                    unfocusedBorderColor =
                        MaterialTheme
                            .colorScheme
                            .outline
                )
        )


        Spacer(
            modifier =
                Modifier.height(
                    16.dp
                )
        )


        // =================================================
        // PASSWORD
        // =================================================

        OutlinedTextField(
            value =
                password,

            onValueChange = {
                password = it

                if (
                    errorMessage.isNotBlank()
                ) {
                    errorMessage = ""
                }
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

            modifier =
                Modifier.fillMaxWidth(),

            visualTransformation =
                if (
                    passwordVisible
                ) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },

            trailingIcon = {

                IconButton(
                    onClick = {

                        passwordVisible =
                            !passwordVisible
                    },

                    enabled =
                        !isLoading
                ) {

                    Icon(
                        imageVector =
                            if (
                                passwordVisible
                            ) {
                                Icons.Filled.VisibilityOff
                            } else {
                                Icons.Filled.Visibility
                            },

                        contentDescription =
                            if (
                                passwordVisible
                            ) {

                                appText(
                                    language,
                                    "Hide password",
                                    "Ẩn mật khẩu"
                                )

                            } else {

                                appText(
                                    language,
                                    "Show password",
                                    "Hiện mật khẩu"
                                )
                            }
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

            colors =
                OutlinedTextFieldDefaults.colors(

                    focusedTextColor =
                        MaterialTheme
                            .colorScheme
                            .onSurface,

                    unfocusedTextColor =
                        MaterialTheme
                            .colorScheme
                            .onSurface,

                    cursorColor =
                        MaterialTheme
                            .colorScheme
                            .primary,

                    focusedBorderColor =
                        MaterialTheme
                            .colorScheme
                            .primary,

                    unfocusedBorderColor =
                        MaterialTheme
                            .colorScheme
                            .outline
                )
        )


        Spacer(
            modifier =
                Modifier.height(
                    8.dp
                )
        )


        // =================================================
        // REMEMBER LOGIN
        // =================================================
        //
        // FirebaseAuth vẫn giữ session cho đến khi Logout.
        //
        // Checkbox này vẫn được giữ để lưu preference
        // hiện tại của người dùng và tương thích với
        // MainActivity / UI cũ.
        // =================================================

        Row(
            modifier =
                Modifier.fillMaxWidth(),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Checkbox(
                checked =
                    rememberLogin,

                onCheckedChange = { checked ->

                    if (
                        !isLoading
                    ) {
                        onRememberLoginChange(
                            checked
                        )
                    }
                },

                enabled =
                    !isLoading
            )


            Text(
                text =
                    appText(
                        language,
                        "Remember me",
                        "Ghi nhớ đăng nhập"
                    ),

                fontSize =
                    14.sp,

                color =
                    MaterialTheme
                        .colorScheme
                        .onSurface
            )
        }


        Spacer(
            modifier =
                Modifier.height(
                    8.dp
                )
        )


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


        // =================================================
        // LOGIN PROGRESS
        // =================================================

        if (
            isLoading
        ) {

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
                            "Logging in...",
                            "Đang đăng nhập..."
                        ),

                    modifier =
                        Modifier.padding(
                            start = 12.dp
                        ),

                    fontSize =
                        15.sp,

                    fontWeight =
                        FontWeight.SemiBold,

                    color =
                        MaterialTheme
                            .colorScheme
                            .primary
                )
            }

        } else {

            // =================================================
            // LOGIN BUTTON
            // =================================================

            Button(
                onClick = {

                    errorMessage =
                        ""


                    val cleanEmail =
                        email
                            .trim()
                            .filterNot {
                                it.isWhitespace()
                            }


                    if (
                        cleanEmail.isBlank() ||
                        password.isBlank()
                    ) {

                        errorMessage =
                            appText(
                                language,
                                "Please enter your email and password.",
                                "Vui lòng nhập email và mật khẩu."
                            )

                        return@Button
                    }


                    isLoading =
                        true


                    // =========================================
                    // FIREBASE AUTH
                    // =========================================

                    auth
                        .signInWithEmailAndPassword(
                            cleanEmail,
                            password
                        )
                        .addOnSuccessListener {

                            val user =
                                auth.currentUser


                            if (
                                user == null
                            ) {

                                isLoading =
                                    false

                                errorMessage =
                                    appText(
                                        language,
                                        "Unable to load user.",
                                        "Không thể tải thông tin người dùng."
                                    )

                                return@addOnSuccessListener
                            }


                            // =================================
                            // LOAD USER PROFILE
                            // =================================

                            firestore
                                .collection(
                                    "users"
                                )
                                .document(
                                    user.uid
                                )
                                .get()
                                .addOnSuccessListener { document ->

                                    isLoading =
                                        false


                                    // =========================
                                    // PROFILE NOT FOUND
                                    // =========================

                                    if (
                                        !document.exists()
                                    ) {

                                        /*
                                         * Account Firebase tồn tại
                                         * nhưng không có profile Firestore.
                                         *
                                         * Đây là account không hợp lệ
                                         * với hệ thống Smart Access,
                                         * nên mới thực hiện signOut.
                                         */
                                        auth.signOut()


                                        onRememberLoginChange(
                                            false
                                        )


                                        errorMessage =
                                            appText(
                                                language,
                                                "User profile was not found.",
                                                "Không tìm thấy hồ sơ người dùng."
                                            )


                                        return@addOnSuccessListener
                                    }


                                    val role =
                                        document.getString(
                                            "role"
                                        )


                                    val status =
                                        document.getString(
                                            "status"
                                        )


                                    // =================================
                                    // PENDING STUDENT
                                    // =================================

                                    if (
                                        role == "student" &&
                                        status == "pending"
                                    ) {

                                        /*
                                         * KHÔNG signOut.
                                         *
                                         * PendingApprovalScreen cần
                                         * Firebase currentUser để theo
                                         * dõi trạng thái được Admin duyệt.
                                         */
                                        onPendingAccount()


                                        return@addOnSuccessListener
                                    }


                                    // =================================
                                    // INVALID / DISABLED STATUS
                                    // =================================

                                    if (
                                        status != "active"
                                    ) {

                                        /*
                                         * Account bị disable hoặc có
                                         * trạng thái không hợp lệ.
                                         *
                                         * Trường hợp này mới xóa session.
                                         */
                                        auth.signOut()


                                        onRememberLoginChange(
                                            false
                                        )


                                        errorMessage =
                                            appText(
                                                language,
                                                "Your account is not active.",
                                                "Tài khoản của bạn chưa hoạt động."
                                            )


                                        return@addOnSuccessListener
                                    }


                                    // =================================
                                    // ACTIVE ACCOUNT
                                    // =================================

                                    when (
                                        role
                                    ) {

                                        "student" -> {

                                            /*
                                             * KHÔNG gọi signOut.
                                             *
                                             * Firebase Auth sẽ giữ
                                             * currentUser sau khi app
                                             * đóng và mở lại.
                                             */
                                            errorMessage =
                                                ""


                                            onStudentLogin()
                                        }


                                        "admin" -> {

                                            errorMessage =
                                                ""


                                            onAdminLogin()
                                        }


                                        else -> {

                                            /*
                                             * Role không hợp lệ:
                                             * account không được phép
                                             * sử dụng hệ thống.
                                             */
                                            auth.signOut()


                                            onRememberLoginChange(
                                                false
                                            )


                                            errorMessage =
                                                appText(
                                                    language,
                                                    "Invalid user role.",
                                                    "Vai trò người dùng không hợp lệ."
                                                )
                                        }
                                    }
                                }
                                .addOnFailureListener { error ->

                                    isLoading =
                                        false


                                    /*
                                     * FIX QUAN TRỌNG:
                                     *
                                     * CODE CŨ:
                                     *
                                     * auth.signOut()
                                     * onRememberLoginChange(false)
                                     *
                                     * => Chỉ cần mạng yếu hoặc Firestore
                                     * lỗi tạm thời là session Firebase
                                     * bị xóa.
                                     *
                                     * CODE MỚI:
                                     *
                                     * KHÔNG signOut.
                                     *
                                     * Firebase currentUser vẫn được giữ.
                                     */
                                    errorMessage =
                                        appText(
                                            language,
                                            "Unable to load account information: ${error.message}",
                                            "Không thể tải thông tin tài khoản: ${error.message}"
                                        )
                                }
                        }
                        .addOnFailureListener { error ->

                            isLoading =
                                false


                            /*
                             * signInWithEmailAndPassword thất bại
                             * thì Firebase chưa tạo session mới,
                             * nên cũng không cần signOut().
                             */
                            errorMessage =
                                appText(
                                    language,
                                    "Login failed: ${error.message}",
                                    "Đăng nhập thất bại: ${error.message}"
                                )
                        }
                },

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(
                            52.dp
                        ),

                colors =
                    ButtonDefaults
                        .buttonColors(

                            containerColor =
                                MaterialTheme
                                    .colorScheme
                                    .primary,

                            contentColor =
                                MaterialTheme
                                    .colorScheme
                                    .onPrimary
                        )
            ) {

                Text(
                    text =
                        appText(
                            language,
                            "LOGIN",
                            "ĐĂNG NHẬP"
                        ),

                    fontWeight =
                        FontWeight.SemiBold
                )
            }


            Spacer(
                modifier =
                    Modifier.height(
                        14.dp
                    )
            )


            // =================================================
            // CREATE ACCOUNT
            // =================================================

            TextButton(
                onClick =
                    onCreateAccount
            ) {

                Text(
                    text =
                        appText(
                            language,
                            "Create Account",
                            "Tạo tài khoản"
                        )
                )
            }
        }
    }
}