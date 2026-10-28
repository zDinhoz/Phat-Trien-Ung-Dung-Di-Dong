package com.hcmute.smartaccess

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PendingApprovalScreen(
    language: AppLanguage,
    onBackToLogin: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                MaterialTheme.colorScheme.background
            )
            .padding(32.dp),

        verticalArrangement =
            Arrangement.Center,

        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {

        Text(
            text = "⏳",
            fontSize = 52.sp
        )

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        Text(
            text = appText(
                language,
                "Account Pending",
                "Tài khoản đang chờ duyệt"
            ),
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold,
            color =
                MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(
            modifier = Modifier.height(14.dp)
        )

        Text(
            text = appText(
                language,
                "Your account has been created successfully.",
                "Tài khoản của bạn đã được tạo thành công."
            ),
            color =
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(
            modifier = Modifier.height(6.dp)
        )

        Text(
            text = appText(
                language,
                "Please wait for Lecturer/Admin approval.",
                "Vui lòng chờ Giảng viên/Quản trị viên phê duyệt."
            ),
            color =
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(
            modifier = Modifier.height(28.dp)
        )

        Button(
            onClick = onBackToLogin,
            modifier = Modifier.fillMaxWidth()
        ) {

            Text(
                text = appText(
                    language,
                    "BACK TO LOGIN",
                    "QUAY LẠI ĐĂNG NHẬP"
                )
            )
        }
    }
}