package com.hcmute.smartaccess

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    themeMode: String,
    language: AppLanguage,
    onThemeModeChange: (String) -> Unit,
    onLanguageChange: (AppLanguage) -> Unit,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onLogout: () -> Unit
) {

    Scaffold(

        topBar = {

            TopAppBar(

                title = {

                    Text(
                        text = appText(
                            language,
                            "Settings",
                            "Cài đặt"
                        ),
                        fontWeight =
                            FontWeight.Bold
                    )
                },

                navigationIcon = {

                    TextButton(
                        onClick = onBack
                    ) {

                        Text(
                            text = "←",
                            fontSize = 26.sp,
                            color =
                                MaterialTheme
                                    .colorScheme
                                    .primary
                        )
                    }
                }
            )
        }

    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(20.dp)
        ) {

            // =========================
            // APPEARANCE
            // =========================

            Text(
                text = appText(
                    language,
                    "Appearance",
                    "Giao diện"
                ),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color =
                    MaterialTheme
                        .colorScheme
                        .primary
            )

            Spacer(
                modifier =
                    Modifier.height(12.dp)
            )

            Card(
                modifier =
                    Modifier.fillMaxWidth()
            ) {

                Column {

                    ThemeOption(
                        symbol = "◐",

                        title = appText(
                            language,
                            "System default",
                            "Theo hệ thống"
                        ),

                        description = appText(
                            language,
                            "Follow device appearance",
                            "Sử dụng giao diện của thiết bị"
                        ),

                        selected =
                            themeMode == "system",

                        onClick = {

                            onThemeModeChange(
                                "system"
                            )
                        }
                    )

                    HorizontalDivider()

                    ThemeOption(
                        symbol = "☀",

                        title = appText(
                            language,
                            "Light",
                            "Sáng"
                        ),

                        description = appText(
                            language,
                            "Always use light theme",
                            "Luôn sử dụng giao diện sáng"
                        ),

                        selected =
                            themeMode == "light",

                        onClick = {

                            onThemeModeChange(
                                "light"
                            )
                        }
                    )

                    HorizontalDivider()

                    ThemeOption(
                        symbol = "☾",

                        title = appText(
                            language,
                            "Dark",
                            "Tối"
                        ),

                        description = appText(
                            language,
                            "Always use dark theme",
                            "Luôn sử dụng giao diện tối"
                        ),

                        selected =
                            themeMode == "dark",

                        onClick = {

                            onThemeModeChange(
                                "dark"
                            )
                        }
                    )
                }
            }


            Spacer(
                modifier =
                    Modifier.height(28.dp)
            )


            // =========================
            // LANGUAGE
            // =========================

            Text(
                text = appText(
                    language,
                    "Language",
                    "Ngôn ngữ"
                ),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color =
                    MaterialTheme
                        .colorScheme
                        .primary
            )

            Spacer(
                modifier =
                    Modifier.height(12.dp)
            )

            Card(
                modifier =
                    Modifier.fillMaxWidth()
            ) {

                Column {

                    LanguageOption(
                        symbol = "EN",
                        title = "English",
                        selected =
                            language ==
                                    AppLanguage.ENGLISH,
                        onClick = {

                            onLanguageChange(
                                AppLanguage.ENGLISH
                            )
                        }
                    )

                    HorizontalDivider()

                    LanguageOption(
                        symbol = "VI",
                        title = "Tiếng Việt",
                        selected =
                            language ==
                                    AppLanguage.VIETNAMESE,
                        onClick = {

                            onLanguageChange(
                                AppLanguage.VIETNAMESE
                            )
                        }
                    )
                }
            }


            Spacer(
                modifier =
                    Modifier.height(28.dp)
            )


            // =========================
            // SYSTEM
            // =========================

            Text(
                text = appText(
                    language,
                    "System",
                    "Hệ thống"
                ),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color =
                    MaterialTheme
                        .colorScheme
                        .primary
            )

            Spacer(
                modifier =
                    Modifier.height(12.dp)
            )

            Card(
                modifier =
                    Modifier.fillMaxWidth()
            ) {

                Column {

                    SettingsAction(
                        symbol = "↻",

                        title = appText(
                            language,
                            "Refresh dashboard data",
                            "Làm mới dữ liệu"
                        ),

                        onClick =
                            onRefresh
                    )

                    HorizontalDivider()

                    SettingsAction(
                        symbol = "ⓘ",

                        title = appText(
                            language,
                            "About application",
                            "Thông tin ứng dụng"
                        ),

                        subtitle =
                            "HCM-UTE Smart Access • Version 1.0",

                        onClick = { }
                    )
                }
            }


            Spacer(
                modifier =
                    Modifier.height(28.dp)
            )


            Button(
                onClick = onLogout,
                modifier =
                    Modifier.fillMaxWidth()
            ) {

                Text(
                    text = appText(
                        language,
                        "LOGOUT",
                        "ĐĂNG XUẤT"
                    )
                )
            }


            Spacer(
                modifier =
                    Modifier.height(30.dp)
            )
        }
    }
}


@Composable
private fun ThemeOption(
    symbol: String,
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {

    TextButton(
        onClick = onClick,
        modifier =
            Modifier.fillMaxWidth()
    ) {

        Row(
            modifier =
                Modifier.fillMaxWidth(),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Text(
                text = symbol,
                fontSize = 24.sp,
                color =
                    MaterialTheme
                        .colorScheme
                        .primary
            )

            Spacer(
                modifier =
                    Modifier.width(14.dp)
            )

            Column(
                modifier =
                    Modifier.weight(1f)
            ) {

                Text(
                    text = title,
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurface,
                    fontWeight =
                        FontWeight.Medium
                )

                Spacer(
                    modifier =
                        Modifier.height(2.dp)
                )

                Text(
                    text = description,
                    fontSize = 12.sp,
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )
            }

            RadioButton(
                selected = selected,
                onClick = onClick
            )
        }
    }
}


@Composable
private fun LanguageOption(
    symbol: String,
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {

    TextButton(
        onClick = onClick,
        modifier =
            Modifier.fillMaxWidth()
    ) {

        Row(
            modifier =
                Modifier.fillMaxWidth(),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Text(
                text = symbol,
                fontSize = 15.sp,
                fontWeight =
                    FontWeight.Bold,
                color =
                    MaterialTheme
                        .colorScheme
                        .primary
            )

            Spacer(
                modifier =
                    Modifier.width(16.dp)
            )

            Text(
                text = title,
                modifier =
                    Modifier.weight(1f),
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurface,
                fontWeight =
                    FontWeight.Medium
            )

            RadioButton(
                selected = selected,
                onClick = onClick
            )
        }
    }
}


@Composable
private fun SettingsAction(
    symbol: String,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {

    TextButton(
        onClick = onClick,
        modifier =
            Modifier.fillMaxWidth()
    ) {

        Row(
            modifier =
                Modifier.fillMaxWidth(),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Text(
                text = symbol,
                fontSize = 24.sp,
                color =
                    MaterialTheme
                        .colorScheme
                        .primary
            )

            Spacer(
                modifier =
                    Modifier.width(14.dp)
            )

            Column {

                Text(
                    text = title,
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurface,
                    fontWeight =
                        FontWeight.Medium
                )

                if (subtitle != null) {

                    Spacer(
                        modifier =
                            Modifier.height(2.dp)
                    )

                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant
                    )
                }
            }
        }
    }
}