package com.hcmute.smartaccess

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.content.edit
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.hcmute.smartaccess.ui.theme.HCMUTESmartAccessTheme


class MainActivity : ComponentActivity() {

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(
            savedInstanceState
        )


        val preferences =
            getSharedPreferences(
                "smart_access_settings",
                MODE_PRIVATE
            )


        setContent {

            // =================================================
            // FIREBASE
            // =================================================

            val auth =
                FirebaseAuth.getInstance()

            val firestore =
                FirebaseFirestore.getInstance()


            // =================================================
            // THEME
            // =================================================

            var themeMode by remember {

                mutableStateOf(
                    preferences.getString(
                        "theme_mode",
                        "system"
                    ) ?: "system"
                )
            }


            val systemDark =
                isSystemInDarkTheme()


            val useDarkTheme =
                when (
                    themeMode
                ) {

                    "light" ->
                        false

                    "dark" ->
                        true

                    else ->
                        systemDark
                }


            // =================================================
            // LANGUAGE
            // =================================================

            var language by remember {

                mutableStateOf(
                    languageFromCode(
                        preferences.getString(
                            "language",
                            "en"
                        ) ?: "en"
                    )
                )
            }


            // =================================================
            // REMEMBER LOGIN
            // =================================================

            var rememberLogin by remember {

                mutableStateOf(
                    preferences.getBoolean(
                        "remember_login",
                        false
                    )
                )
            }


            // =================================================
            // NAVIGATION
            // =================================================
            //
            // FIX:
            // Luôn bắt đầu bằng authCheck.
            // Không tự ép về login khi Activity được tạo lại.
            //
            // FirebaseAuth sẽ quyết định user còn session hay không.
            // =================================================

            var currentScreen by rememberSaveable {

                mutableStateOf(
                    "authCheck"
                )
            }


            val saveableStateHolder =
                rememberSaveableStateHolder()


            // =================================================
            // HELPERS
            // =================================================

            fun updateRememberLogin(
                value: Boolean
            ) {

                rememberLogin =
                    value


                preferences.edit {

                    putBoolean(
                        "remember_login",
                        value
                    )
                }
            }


            fun logoutToLogin() {

                /*
                 * Chỉ hàm này mới signOut Firebase.
                 *
                 * Hàm này chỉ được gọi khi:
                 * - User bấm Logout
                 * - Account không còn hợp lệ
                 */
                auth.signOut()


                updateRememberLogin(
                    false
                )


                currentScreen =
                    "login"
            }


            // =================================================
            // RESTORE FIREBASE SESSION
            // =================================================
            //
            // Firebase Auth trên Android tự giữ currentUser.
            //
            // Vì vậy:
            //
            // currentUser != null
            //      ↓
            // đọc role/status
            //      ↓
            // vào thẳng dashboard
            //
            // KHÔNG signOut chỉ vì remember_login == false.
            // =================================================

            LaunchedEffect(Unit) {

                val firebaseUser =
                    auth.currentUser


                // =============================================
                // CHƯA CÓ FIREBASE SESSION
                // =============================================

                if (
                    firebaseUser == null
                ) {

                    currentScreen =
                        "login"

                    return@LaunchedEffect
                }


                // =============================================
                // ĐÃ CÓ FIREBASE SESSION
                // =============================================

                firestore
                    .collection(
                        "users"
                    )
                    .document(
                        firebaseUser.uid
                    )
                    .get()
                    .addOnSuccessListener {
                            document ->


                        // =====================================
                        // PROFILE KHÔNG TỒN TẠI
                        // =====================================

                        if (
                            !document.exists()
                        ) {

                            logoutToLogin()

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


                        // =====================================
                        // ROUTE THEO ROLE + STATUS
                        // =====================================

                        currentScreen =
                            when {

                                role ==
                                        "admin" &&
                                        status ==
                                        "active" -> {

                                    "admin"
                                }


                                role ==
                                        "student" &&
                                        status ==
                                        "active" -> {

                                    "student"
                                }


                                role ==
                                        "student" &&
                                        status ==
                                        "pending" -> {

                                    "pending"
                                }


                                else -> {

                                    logoutToLogin()

                                    "login"
                                }
                            }
                    }
                    .addOnFailureListener {

                        /*
                         * QUAN TRỌNG:
                         *
                         * Nếu mạng / Firestore lỗi tạm thời,
                         * KHÔNG signOut Firebase.
                         *
                         * Session vẫn được giữ.
                         *
                         * Khi mở app lần sau Firebase vẫn
                         * còn currentUser để kiểm tra lại.
                         */
                        currentScreen =
                            "login"
                    }
            }


            // =================================================
            // THEME
            // =================================================

            HCMUTESmartAccessTheme(
                darkTheme =
                    useDarkTheme
            ) {

                Surface(
                    modifier =
                        Modifier.fillMaxSize(),

                    color =
                        MaterialTheme
                            .colorScheme
                            .background
                ) {

                    // =================================================
                    // BACK BUTTON
                    // =================================================

                    val backEnabled =
                        currentScreen in
                                setOf(
                                    "register",
                                    "pending",
                                    "studentSettings",
                                    "settings",
                                    "pendingStudents",
                                    "accessLogs",
                                    "devices",
                                    "credentials",
                                    "attendance"
                                )


                    BackHandler(
                        enabled =
                            backEnabled
                    ) {

                        when (
                            currentScreen
                        ) {

                            "register" -> {

                                currentScreen =
                                    "login"
                            }


                            "pending" -> {

                                logoutToLogin()
                            }


                            "studentSettings" -> {

                                currentScreen =
                                    "student"
                            }


                            "settings",
                            "pendingStudents",
                            "accessLogs",
                            "devices",
                            "credentials",
                            "attendance" -> {

                                currentScreen =
                                    "admin"
                            }
                        }
                    }


                    // =================================================
                    // SCREEN STATE HOLDER
                    // =================================================

                    saveableStateHolder
                        .SaveableStateProvider(
                            key =
                                currentScreen
                        ) {

                            when (
                                currentScreen
                            ) {


                                // =====================================
                                // AUTH CHECK
                                // =====================================

                                "authCheck" -> {

                                    Box(
                                        modifier =
                                            Modifier.fillMaxSize(),

                                        contentAlignment =
                                            Alignment.Center
                                    ) {

                                        CircularProgressIndicator()
                                    }
                                }


                                // =====================================
                                // LOGIN
                                // =====================================

                                "login" -> {

                                    LoginScreen(
                                        language =
                                            language,

                                        rememberLogin =
                                            rememberLogin,

                                        onRememberLoginChange = {
                                                value ->

                                            updateRememberLogin(
                                                value
                                            )
                                        },

                                        onCreateAccount = {

                                            currentScreen =
                                                "register"
                                        },

                                        onStudentLogin = {

                                            currentScreen =
                                                "student"
                                        },

                                        onAdminLogin = {

                                            currentScreen =
                                                "admin"
                                        },

                                        onPendingAccount = {

                                            currentScreen =
                                                "pending"
                                        }
                                    )
                                }


                                // =====================================
                                // REGISTER
                                // =====================================

                                "register" -> {

                                    RegisterScreen(
                                        language =
                                            language,

                                        onBackToLogin = {

                                            currentScreen =
                                                "login"
                                        },

                                        onRegisterSuccess = {

                                            /*
                                             * Tài khoản mới vẫn chưa
                                             * bật Remember Login mặc định.
                                             *
                                             * Nhưng Firebase session
                                             * vẫn không bị tự signOut nữa.
                                             */
                                            updateRememberLogin(
                                                false
                                            )


                                            currentScreen =
                                                "pending"
                                        }
                                    )
                                }


                                // =====================================
                                // PENDING
                                // =====================================

                                "pending" -> {

                                    PendingApprovalScreen(
                                        language =
                                            language,

                                        onBackToLogin = {

                                            logoutToLogin()
                                        }
                                    )
                                }


                                // =====================================
                                // STUDENT
                                // =====================================

                                "student" -> {

                                    StudentHomeScreen(
                                        language =
                                            language,

                                        onOpenSettings = {

                                            currentScreen =
                                                "studentSettings"
                                        }
                                    )
                                }


                                // =====================================
                                // STUDENT SETTINGS
                                // =====================================

                                "studentSettings" -> {

                                    SettingsScreen(
                                        themeMode =
                                            themeMode,

                                        language =
                                            language,

                                        onThemeModeChange = {
                                                newTheme ->


                                            themeMode =
                                                newTheme


                                            preferences.edit {

                                                putString(
                                                    "theme_mode",
                                                    newTheme
                                                )
                                            }
                                        },

                                        onLanguageChange = {
                                                newLanguage ->


                                            language =
                                                newLanguage


                                            preferences.edit {

                                                putString(
                                                    "language",
                                                    languageCode(
                                                        newLanguage
                                                    )
                                                )
                                            }
                                        },

                                        onBack = {

                                            currentScreen =
                                                "student"
                                        },

                                        onRefresh = {

                                            currentScreen =
                                                "student"
                                        },

                                        onLogout = {

                                            logoutToLogin()
                                        }
                                    )
                                }


                                // =====================================
                                // ADMIN
                                // =====================================

                                "admin" -> {

                                    AdminDashboardScreen(
                                        language =
                                            language,

                                        onLogout = {

                                            logoutToLogin()
                                        },

                                        onOpenPendingStudents = {

                                            currentScreen =
                                                "pendingStudents"
                                        },

                                        onOpenAccessLogs = {

                                            currentScreen =
                                                "accessLogs"
                                        },

                                        onOpenDevices = {

                                            currentScreen =
                                                "devices"
                                        },

                                        onOpenCredentials = {

                                            currentScreen =
                                                "credentials"
                                        },

                                        onOpenAttendance = {

                                            currentScreen =
                                                "attendance"
                                        },

                                        onOpenSettings = {

                                            currentScreen =
                                                "settings"
                                        }
                                    )
                                }


                                // =====================================
                                // PENDING STUDENTS
                                // =====================================

                                "pendingStudents" -> {

                                    PendingStudentsScreen(
                                        language =
                                            language,

                                        onBack = {

                                            currentScreen =
                                                "admin"
                                        }
                                    )
                                }


                                // =====================================
                                // ACCESS LOGS
                                // =====================================

                                "accessLogs" -> {

                                    AccessLogsScreen(
                                        language =
                                            language,

                                        onBack = {

                                            currentScreen =
                                                "admin"
                                        }
                                    )
                                }


                                // =====================================
                                // DEVICES
                                // =====================================

                                "devices" -> {

                                    DeviceManagementScreen(
                                        language =
                                            language,

                                        onBack = {

                                            currentScreen =
                                                "admin"
                                        }
                                    )
                                }


                                // =====================================
                                // CREDENTIALS
                                // =====================================

                                "credentials" -> {

                                    CredentialManagementScreen(
                                        language =
                                            language,

                                        onBack = {

                                            currentScreen =
                                                "admin"
                                        }
                                    )
                                }


                                // =====================================
                                // ATTENDANCE
                                // =====================================

                                "attendance" -> {

                                    AttendanceManagementScreen(
                                        language =
                                            language,

                                        onBack = {

                                            currentScreen =
                                                "admin"
                                        }
                                    )
                                }


                                // =====================================
                                // ADMIN SETTINGS
                                // =====================================

                                "settings" -> {

                                    SettingsScreen(
                                        themeMode =
                                            themeMode,

                                        language =
                                            language,

                                        onThemeModeChange = {
                                                newTheme ->


                                            themeMode =
                                                newTheme


                                            preferences.edit {

                                                putString(
                                                    "theme_mode",
                                                    newTheme
                                                )
                                            }
                                        },

                                        onLanguageChange = {
                                                newLanguage ->


                                            language =
                                                newLanguage


                                            preferences.edit {

                                                putString(
                                                    "language",
                                                    languageCode(
                                                        newLanguage
                                                    )
                                                )
                                            }
                                        },

                                        onBack = {

                                            currentScreen =
                                                "admin"
                                        },

                                        onRefresh = {

                                            currentScreen =
                                                "admin"
                                        },

                                        onLogout = {

                                            logoutToLogin()
                                        }
                                    )
                                }


                                // =====================================
                                // FALLBACK
                                // =====================================

                                else -> {

                                    /*
                                     * Nếu route lạ:
                                     * không signOut Firebase.
                                     *
                                     * Chỉ đưa UI về Login.
                                     */
                                    LoginScreen(
                                        language =
                                            language,

                                        rememberLogin =
                                            rememberLogin,

                                        onRememberLoginChange = {
                                                value ->

                                            updateRememberLogin(
                                                value
                                            )
                                        },

                                        onCreateAccount = {

                                            currentScreen =
                                                "register"
                                        },

                                        onStudentLogin = {

                                            currentScreen =
                                                "student"
                                        },

                                        onAdminLogin = {

                                            currentScreen =
                                                "admin"
                                        },

                                        onPendingAccount = {

                                            currentScreen =
                                                "pending"
                                        }
                                    )
                                }
                            }
                        }
                }
            }
        }
    }
}