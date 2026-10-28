package com.hcmute.smartaccess

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


// =========================================================
// TIME MASK
// Người dùng nhập: 1730
// Hiển thị:       17:30
// =========================================================

private class AttendanceTimeVisualTransformation :
    VisualTransformation {

    override fun filter(
        text: AnnotatedString
    ): TransformedText {

        val raw =
            text.text
                .filter {
                    it.isDigit()
                }
                .take(4)

        val hour =
            raw
                .take(2)
                .padEnd(
                    2,
                    '_'
                )

        val minute =
            raw
                .drop(2)
                .take(2)
                .padEnd(
                    2,
                    '_'
                )

        val formatted =
            "$hour:$minute"

        val rawLength =
            raw.length

        val mapping =
            object : OffsetMapping {

                override fun originalToTransformed(
                    offset: Int
                ): Int {

                    val safeOffset =
                        offset.coerceIn(
                            0,
                            rawLength
                        )

                    return when (
                        safeOffset
                    ) {

                        0 -> 0
                        1 -> 1
                        2 -> 3
                        3 -> 4
                        else -> 5
                    }
                }


                override fun transformedToOriginal(
                    offset: Int
                ): Int {

                    val candidate =
                        when {

                            offset <= 0 ->
                                0

                            offset == 1 ->
                                1

                            offset <= 3 ->
                                2

                            offset == 4 ->
                                3

                            else ->
                                4
                        }

                    return candidate
                        .coerceAtMost(
                            rawLength
                        )
                }
            }

        return TransformedText(
            AnnotatedString(
                formatted
            ),
            mapping
        )
    }
}


// =========================================================
// MODELS
// =========================================================

data class AdminAttendanceSession(
    val id: String = "",
    val room: String = "A102",
    val subjectName: String = "",
    val date: String = "",
    val startTime: Timestamp? = null,
    val endTime: Timestamp? = null,
    val checkInOpenAt: Timestamp? = null,
    val checkInCloseAt: Timestamp? = null,
    val status: String = "draft"
)


data class AdminAttendanceStudent(
    val userId: String = "",
    val studentId: String = "",
    val fullName: String = ""
)


data class AdminAttendanceRecord(
    val sessionId: String = "",
    val userId: String = "",
    val studentId: String = "",
    val fullName: String = "",
    val checkInAt: Timestamp? = null,
    val checkInMethod: String = "",
    val status: String = ""
)


// =========================================================
// SCREEN
// =========================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceManagementScreen(
    language: AppLanguage,
    onBack: () -> Unit
) {

    val firestore =
        FirebaseFirestore.getInstance()

    val adminUid =
        FirebaseAuth
            .getInstance()
            .currentUser
            ?.uid
            ?: ""


    // =====================================================
    // LIST DATA
    // =====================================================

    val sessions =
        remember {
            mutableStateListOf<AdminAttendanceSession>()
        }

    val students =
        remember {
            mutableStateListOf<AdminAttendanceStudent>()
        }

    val records =
        remember {
            mutableStateListOf<AdminAttendanceRecord>()
        }


    var selectedSessionId by remember {
        mutableStateOf("")
    }

    var errorMessage by remember {
        mutableStateOf("")
    }


    // =====================================================
    // DIALOG
    // =====================================================

    var showSessionDialog by remember {
        mutableStateOf(false)
    }

    var editingSession by remember {
        mutableStateOf<AdminAttendanceSession?>(null)
    }

    var sessionPendingDelete by remember {
        mutableStateOf<AdminAttendanceSession?>(null)
    }

    var isSaving by remember {
        mutableStateOf(false)
    }

    var isDeleting by remember {
        mutableStateOf(false)
    }

    var formErrorMessage by remember {
        mutableStateOf("")
    }


    // =====================================================
    // FORM
    // =====================================================

    var subjectName by remember {
        mutableStateOf("")
    }

    var sessionDate by remember {
        mutableStateOf(
            SimpleDateFormat(
                "dd/MM/yyyy",
                Locale.getDefault()
            ).format(
                Date()
            )
        )
    }


    /*
     * Các giá trị giờ bên dưới KHÔNG lưu dấu :
     *
     * 0700 -> UI hiển thị 07:00
     * 1730 -> UI hiển thị 17:30
     */

    var startTimeText by remember {
        mutableStateOf("0700")
    }

    var endTimeText by remember {
        mutableStateOf("0930")
    }

    var openAtText by remember {
        mutableStateOf("0650")
    }

    var closeAtText by remember {
        mutableStateOf("0715")
    }


    val timeTransformation =
        remember {
            AttendanceTimeVisualTransformation()
        }


    // =====================================================
    // HELPERS
    // =====================================================

    fun sanitizeTimeInput(
        value: String
    ): String {

        return value
            .filter {
                it.isDigit()
            }
            .take(4)
    }


    fun rawTimeToFormatted(
        raw: String
    ): String? {

        val digits =
            raw.filter {
                it.isDigit()
            }

        if (
            digits.length !=
            4
        ) {

            return null
        }

        val hour =
            digits
                .substring(
                    0,
                    2
                )
                .toIntOrNull()
                ?: return null

        val minute =
            digits
                .substring(
                    2,
                    4
                )
                .toIntOrNull()
                ?: return null


        if (
            hour !in
            0..23
        ) {

            return null
        }


        if (
            minute !in
            0..59
        ) {

            return null
        }


        return String.format(
            Locale.US,
            "%02d:%02d",
            hour,
            minute
        )
    }


    fun timestampToRawTime(
        timestamp: Timestamp?
    ): String {

        if (
            timestamp ==
            null
        ) {

            return ""
        }

        return SimpleDateFormat(
            "HHmm",
            Locale.getDefault()
        ).format(
            timestamp.toDate()
        )
    }


    fun timestampToDisplayTime(
        timestamp: Timestamp?
    ): String {

        if (
            timestamp ==
            null
        ) {

            return "--:--"
        }

        return SimpleDateFormat(
            "HH:mm",
            Locale.getDefault()
        ).format(
            timestamp.toDate()
        )
    }


    fun timestampToFormDate(
        timestamp: Timestamp?
    ): String {

        if (
            timestamp ==
            null
        ) {

            return SimpleDateFormat(
                "dd/MM/yyyy",
                Locale.getDefault()
            ).format(
                Date()
            )
        }

        return SimpleDateFormat(
            "dd/MM/yyyy",
            Locale.getDefault()
        ).format(
            timestamp.toDate()
        )
    }


    fun parseDateTime(
        date: String,
        rawTime: String
    ): Timestamp? {

        val formattedTime =
            rawTimeToFormatted(
                rawTime
            )
                ?: return null

        return try {

            val formatter =
                SimpleDateFormat(
                    "dd/MM/yyyy HH:mm",
                    Locale.getDefault()
                )

            formatter.isLenient =
                false

            val parsed =
                formatter.parse(
                    "$date $formattedTime"
                )
                    ?: return null

            Timestamp(
                parsed
            )

        } catch (_: Exception) {

            null
        }
    }


    fun resetForm() {

        subjectName =
            ""

        sessionDate =
            SimpleDateFormat(
                "dd/MM/yyyy",
                Locale.getDefault()
            ).format(
                Date()
            )

        startTimeText =
            "0700"

        endTimeText =
            "0930"

        openAtText =
            "0650"

        closeAtText =
            "0715"

        formErrorMessage =
            ""
    }


    fun openCreateDialog() {

        editingSession =
            null

        resetForm()

        showSessionDialog =
            true
    }


    fun openEditDialog(
        session: AdminAttendanceSession
    ) {

        editingSession =
            session

        subjectName =
            session.subjectName

        sessionDate =
            timestampToFormDate(
                session.startTime
            )

        startTimeText =
            timestampToRawTime(
                session.startTime
            )

        endTimeText =
            timestampToRawTime(
                session.endTime
            )

        openAtText =
            timestampToRawTime(
                session.checkInOpenAt
            )

        closeAtText =
            timestampToRawTime(
                session.checkInCloseAt
            )

        formErrorMessage =
            ""

        showSessionDialog =
            true
    }


    fun dismissSessionDialog() {

        if (
            isSaving
        ) {

            return
        }

        showSessionDialog =
            false

        editingSession =
            null

        formErrorMessage =
            ""
    }


    // =====================================================
    // SESSION LISTENER
    // =====================================================

    DisposableEffect(Unit) {

        val listener =
            firestore
                .collection(
                    "attendance_sessions"
                )
                .addSnapshotListener { snapshot, error ->

                    if (
                        error !=
                        null
                    ) {

                        errorMessage =
                            error.message
                                ?: "Attendance error"

                        return@addSnapshotListener
                    }


                    val items =
                        snapshot
                            ?.documents
                            ?.map { document ->

                                AdminAttendanceSession(

                                    id =
                                        document.id,

                                    room =
                                        document.getString(
                                            "room"
                                        )
                                            ?: "A102",

                                    subjectName =
                                        document.getString(
                                            "subjectName"
                                        )
                                            ?: "",

                                    date =
                                        document.getString(
                                            "date"
                                        )
                                            ?: "",

                                    startTime =
                                        document.getTimestamp(
                                            "startTime"
                                        ),

                                    endTime =
                                        document.getTimestamp(
                                            "endTime"
                                        ),

                                    checkInOpenAt =
                                        document.getTimestamp(
                                            "checkInOpenAt"
                                        ),

                                    checkInCloseAt =
                                        document.getTimestamp(
                                            "checkInCloseAt"
                                        ),

                                    status =
                                        document.getString(
                                            "status"
                                        )
                                            ?: "draft"
                                )
                            }
                            ?.sortedByDescending {

                                it.startTime
                                    ?.seconds
                                    ?: 0L
                            }
                            ?: emptyList()


                    sessions.clear()

                    sessions.addAll(
                        items
                    )


                    if (
                        selectedSessionId.isBlank() &&
                        items.isNotEmpty()
                    ) {

                        selectedSessionId =
                            items.first().id
                    }


                    if (
                        selectedSessionId.isNotBlank() &&
                        items.none {
                            it.id ==
                                    selectedSessionId
                        }
                    ) {

                        selectedSessionId =
                            items
                                .firstOrNull()
                                ?.id
                                ?: ""
                    }
                }


        onDispose {

            listener.remove()
        }
    }


    // =====================================================
    // STUDENT LISTENER
    // =====================================================

    DisposableEffect(Unit) {

        val listener =
            firestore
                .collection(
                    "users"
                )
                .addSnapshotListener { snapshot, _ ->

                    students.clear()


                    snapshot
                        ?.documents
                        ?.filter { document ->

                            document.getString(
                                "role"
                            ) ==
                                    "student" &&

                                    document.getString(
                                        "status"
                                    ) ==
                                    "active"
                        }
                        ?.forEach { document ->

                            students.add(

                                AdminAttendanceStudent(

                                    userId =
                                        document.id,

                                    studentId =
                                        document.getString(
                                            "studentId"
                                        )
                                            ?: "",

                                    fullName =
                                        document.getString(
                                            "fullName"
                                        )
                                            ?: "Student"
                                )
                            )
                        }
                }


        onDispose {

            listener.remove()
        }
    }


    // =====================================================
    // RECORD LISTENER
    // =====================================================

    DisposableEffect(Unit) {

        val listener =
            firestore
                .collection(
                    "attendance_records"
                )
                .addSnapshotListener { snapshot, _ ->

                    records.clear()


                    snapshot
                        ?.documents
                        ?.forEach { document ->

                            records.add(

                                AdminAttendanceRecord(

                                    sessionId =
                                        document.getString(
                                            "sessionId"
                                        )
                                            ?: "",

                                    userId =
                                        document.getString(
                                            "userId"
                                        )
                                            ?: "",

                                    studentId =
                                        document.getString(
                                            "studentId"
                                        )
                                            ?: "",

                                    fullName =
                                        document.getString(
                                            "fullName"
                                        )
                                            ?: "",

                                    checkInAt =
                                        document.getTimestamp(
                                            "checkInAt"
                                        ),

                                    checkInMethod =
                                        document.getString(
                                            "checkInMethod"
                                        )
                                            ?: "",

                                    status =
                                        document.getString(
                                            "status"
                                        )
                                            ?: ""
                                )
                            )
                        }
                }


        onDispose {

            listener.remove()
        }
    }


    // =====================================================
    // SAVE SESSION
    // =====================================================

    fun saveSession() {

        if (
            isSaving
        ) {

            return
        }


        if (
            subjectName.isBlank()
        ) {

            formErrorMessage =
                "Vui lòng nhập tên môn học."

            return
        }


        if (
            startTimeText.length !=
            4
        ) {

            formErrorMessage =
                "Vui lòng nhập đầy đủ giờ bắt đầu theo định dạng HH:mm."

            return
        }


        if (
            endTimeText.length !=
            4
        ) {

            formErrorMessage =
                "Vui lòng nhập đầy đủ giờ kết thúc theo định dạng HH:mm."

            return
        }


        if (
            openAtText.length !=
            4
        ) {

            formErrorMessage =
                "Vui lòng nhập đầy đủ giờ mở check-in theo định dạng HH:mm."

            return
        }


        if (
            closeAtText.length !=
            4
        ) {

            formErrorMessage =
                "Vui lòng nhập đầy đủ giờ đóng check-in theo định dạng HH:mm."

            return
        }


        val start =
            parseDateTime(
                sessionDate.trim(),
                startTimeText
            )

        val end =
            parseDateTime(
                sessionDate.trim(),
                endTimeText
            )

        val openAt =
            parseDateTime(
                sessionDate.trim(),
                openAtText
            )

        val closeAt =
            parseDateTime(
                sessionDate.trim(),
                closeAtText
            )


        if (
            start == null ||
            end == null ||
            openAt == null ||
            closeAt == null
        ) {

            formErrorMessage =
                "Ngày hoặc giờ không hợp lệ. Giờ phải nằm trong 00:00 - 23:59."

            return
        }


        if (
            start.seconds >=
            end.seconds
        ) {

            formErrorMessage =
                "Giờ kết thúc phải sau giờ bắt đầu."

            return
        }


        if (
            openAt.seconds >
            closeAt.seconds
        ) {

            formErrorMessage =
                "Giờ đóng check-in phải sau giờ mở check-in."

            return
        }


        if (
            closeAt.seconds >
            end.seconds
        ) {

            formErrorMessage =
                "Giờ đóng check-in không được sau giờ kết thúc."

            return
        }


        formErrorMessage =
            ""

        isSaving =
            true


        val storedDate =
            SimpleDateFormat(
                "yyyy-MM-dd",
                Locale.US
            ).format(
                start.toDate()
            )


        val editing =
            editingSession


        // =================================================
        // CREATE
        // =================================================

        if (
            editing ==
            null
        ) {

            val documentId =
                "A102_" +
                        SimpleDateFormat(
                            "yyyyMMdd_HHmmss",
                            Locale.US
                        ).format(
                            Date()
                        )


            val data =
                hashMapOf(

                    "room" to
                            "A102",

                    "subjectName" to
                            subjectName.trim(),

                    "date" to
                            storedDate,

                    "startTime" to
                            start,

                    "endTime" to
                            end,

                    "checkInOpenAt" to
                            openAt,

                    "checkInCloseAt" to
                            closeAt,

                    "status" to
                            "draft",

                    "createdBy" to
                            adminUid,

                    "createdAt" to
                            FieldValue.serverTimestamp(),

                    "updatedAt" to
                            FieldValue.serverTimestamp()
                )


            firestore
                .collection(
                    "attendance_sessions"
                )
                .document(
                    documentId
                )
                .set(
                    data
                )
                .addOnSuccessListener {

                    selectedSessionId =
                        documentId

                    isSaving =
                        false

                    showSessionDialog =
                        false

                    editingSession =
                        null

                    resetForm()

                    errorMessage =
                        ""
                }
                .addOnFailureListener { error ->

                    isSaving =
                        false

                    formErrorMessage =
                        error.message
                            ?: "Không thể tạo buổi điểm danh."
                }

            return
        }


        // =================================================
        // EDIT
        // =================================================

        val sessionRef =
            firestore
                .collection(
                    "attendance_sessions"
                )
                .document(
                    editing.id
                )


        val updateData =
            hashMapOf(

                "room" to
                        "A102",

                "subjectName" to
                        subjectName.trim(),

                "date" to
                        storedDate,

                "startTime" to
                        start,

                "endTime" to
                        end,

                "checkInOpenAt" to
                        openAt,

                "checkInCloseAt" to
                        closeAt,

                "updatedAt" to
                        FieldValue.serverTimestamp()
            )


        if (
            editing.status ==
            "open"
        ) {

            val runtimeRef =
                firestore
                    .collection(
                        "attendance_runtime"
                    )
                    .document(
                        ClassroomConfig.DEVICE_ID
                    )


            val runtimeData =
                hashMapOf(

                    "deviceId" to
                            ClassroomConfig.DEVICE_ID,

                    "room" to
                            "A102",

                    "activeSessionId" to
                            editing.id,

                    "subjectName" to
                            subjectName.trim(),

                    "status" to
                            "open",

                    "startTime" to
                            start,

                    "endTime" to
                            end,

                    "checkInOpenAt" to
                            openAt,

                    "checkInCloseAt" to
                            closeAt,

                    "updatedAt" to
                            FieldValue.serverTimestamp()
                )


            val batch =
                firestore.batch()


            batch.set(
                sessionRef,
                updateData,
                SetOptions.merge()
            )


            batch.set(
                runtimeRef,
                runtimeData,
                SetOptions.merge()
            )


            batch
                .commit()
                .addOnSuccessListener {

                    isSaving =
                        false

                    showSessionDialog =
                        false

                    editingSession =
                        null

                    formErrorMessage =
                        ""

                    errorMessage =
                        ""
                }
                .addOnFailureListener { error ->

                    isSaving =
                        false

                    formErrorMessage =
                        error.message
                            ?: "Không thể cập nhật buổi điểm danh."
                }

        } else {

            sessionRef
                .set(
                    updateData,
                    SetOptions.merge()
                )
                .addOnSuccessListener {

                    isSaving =
                        false

                    showSessionDialog =
                        false

                    editingSession =
                        null

                    formErrorMessage =
                        ""

                    errorMessage =
                        ""
                }
                .addOnFailureListener { error ->

                    isSaving =
                        false

                    formErrorMessage =
                        error.message
                            ?: "Không thể cập nhật buổi điểm danh."
                }
        }
    }


    // =====================================================
    // OPEN SESSION
    // =====================================================

    fun openSession(
        session: AdminAttendanceSession
    ) {

        val anotherOpen =
            sessions.any {

                it.status ==
                        "open" &&
                        it.id !=
                        session.id
            }


        if (
            anotherOpen
        ) {

            errorMessage =
                "A102 đang có một buổi điểm danh khác đang mở."

            return
        }


        val sessionRef =
            firestore
                .collection(
                    "attendance_sessions"
                )
                .document(
                    session.id
                )


        val runtimeRef =
            firestore
                .collection(
                    "attendance_runtime"
                )
                .document(
                    ClassroomConfig.DEVICE_ID
                )


        val runtime =
            hashMapOf<String, Any?>(

                "deviceId" to
                        ClassroomConfig.DEVICE_ID,

                "room" to
                        "A102",

                "activeSessionId" to
                        session.id,

                "subjectName" to
                        session.subjectName,

                "status" to
                        "open",

                "startTime" to
                        session.startTime,

                "endTime" to
                        session.endTime,

                "checkInOpenAt" to
                        session.checkInOpenAt,

                "checkInCloseAt" to
                        session.checkInCloseAt,

                "updatedAt" to
                        FieldValue.serverTimestamp()
            )


        val batch =
            firestore.batch()


        batch.update(

            sessionRef,

            mapOf(

                "status" to
                        "open",

                "updatedAt" to
                        FieldValue.serverTimestamp()
            )
        )


        batch.set(
            runtimeRef,
            runtime,
            SetOptions.merge()
        )


        batch
            .commit()
            .addOnSuccessListener {

                errorMessage =
                    ""
            }
            .addOnFailureListener { error ->

                errorMessage =
                    error.message
                        ?: "Không thể mở điểm danh."
            }
    }


    // =====================================================
    // CLOSE SESSION
    // =====================================================

    fun closeSession(
        session: AdminAttendanceSession
    ) {

        val sessionRef =
            firestore
                .collection(
                    "attendance_sessions"
                )
                .document(
                    session.id
                )


        val runtimeRef =
            firestore
                .collection(
                    "attendance_runtime"
                )
                .document(
                    ClassroomConfig.DEVICE_ID
                )


        val batch =
            firestore.batch()


        batch.update(

            sessionRef,

            mapOf(

                "status" to
                        "closed",

                "updatedAt" to
                        FieldValue.serverTimestamp()
            )
        )


        batch.set(

            runtimeRef,

            mapOf(

                "deviceId" to
                        ClassroomConfig.DEVICE_ID,

                "room" to
                        "A102",

                "activeSessionId" to
                        "",

                "subjectName" to
                        "",

                "status" to
                        "closed",

                "updatedAt" to
                        FieldValue.serverTimestamp()
            ),

            SetOptions.merge()
        )


        batch
            .commit()
            .addOnSuccessListener {

                errorMessage =
                    ""
            }
            .addOnFailureListener { error ->

                errorMessage =
                    error.message
                        ?: "Không thể đóng điểm danh."
            }
    }


    // =====================================================
    // DELETE SESSION
    // =====================================================

    fun deleteSession(
        session: AdminAttendanceSession
    ) {

        if (
            isDeleting
        ) {

            return
        }


        isDeleting =
            true


        firestore
            .collection(
                "attendance_records"
            )
            .whereEqualTo(
                "sessionId",
                session.id
            )
            .get()
            .addOnSuccessListener { recordSnapshot ->


                val batch =
                    firestore.batch()


                val sessionRef =
                    firestore
                        .collection(
                            "attendance_sessions"
                        )
                        .document(
                            session.id
                        )


                batch.delete(
                    sessionRef
                )


                recordSnapshot
                    .documents
                    .forEach { document ->

                        batch.delete(
                            document.reference
                        )
                    }


                if (
                    session.status ==
                    "open"
                ) {

                    val runtimeRef =
                        firestore
                            .collection(
                                "attendance_runtime"
                            )
                            .document(
                                ClassroomConfig.DEVICE_ID
                            )


                    batch.set(

                        runtimeRef,

                        mapOf(

                            "deviceId" to
                                    ClassroomConfig.DEVICE_ID,

                            "room" to
                                    "A102",

                            "activeSessionId" to
                                    "",

                            "subjectName" to
                                    "",

                            "status" to
                                    "closed",

                            "updatedAt" to
                                    FieldValue.serverTimestamp()
                        ),

                        SetOptions.merge()
                    )
                }


                batch
                    .commit()
                    .addOnSuccessListener {

                        isDeleting =
                            false

                        sessionPendingDelete =
                            null


                        if (
                            selectedSessionId ==
                            session.id
                        ) {

                            selectedSessionId =
                                ""
                        }


                        errorMessage =
                            ""
                    }
                    .addOnFailureListener { error ->

                        isDeleting =
                            false

                        errorMessage =
                            error.message
                                ?: "Không thể xóa buổi điểm danh."
                    }
            }
            .addOnFailureListener { error ->

                isDeleting =
                    false

                errorMessage =
                    error.message
                        ?: "Không thể tải dữ liệu để xóa."
            }
    }


    // =====================================================
    // SELECTED SESSION
    // =====================================================

    val selectedSession =
        sessions.firstOrNull {

            it.id ==
                    selectedSessionId
        }


    val selectedRecords =
        records.filter {

            it.sessionId ==
                    selectedSessionId
        }


    val presentCount =
        selectedRecords.count {

            it.status ==
                    "present"
        }


    val lateCount =
        selectedRecords.count {

            it.status ==
                    "late"
        }


    val checkedStudentIds =
        selectedRecords
            .map {

                it.studentId
            }
            .toSet()


    val absentCount =
        students.count {

            !checkedStudentIds.contains(
                it.studentId
            )
        }


    // =====================================================
    // UI
    // =====================================================

    Scaffold(

        topBar = {

            TopAppBar(

                title = {

                    Text(

                        text =
                            appText(
                                language,
                                "A102 Attendance Management",
                                "QUẢN LÝ ĐIỂM DANH A102"
                            ),

                        fontWeight =
                            FontWeight.Bold
                    )
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
                                "Back"
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


            Button(

                onClick = {

                    openCreateDialog()
                },

                modifier =
                    Modifier.fillMaxWidth()
            ) {

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
                            " CREATE ATTENDANCE SESSION",
                            " TẠO BUỔI ĐIỂM DANH"
                        )
                )
            }


            if (
                errorMessage.isNotBlank()
            ) {

                Spacer(
                    Modifier.height(
                        12.dp
                    )
                )


                Text(

                    text =
                        errorMessage,

                    color =
                        MaterialTheme
                            .colorScheme
                            .error
                )
            }


            Spacer(
                Modifier.height(
                    20.dp
                )
            )


            Text(

                text =
                    appText(
                        language,
                        "Attendance Sessions",
                        "Các buổi điểm danh"
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
                sessions.isEmpty()
            ) {

                Card(

                    modifier =
                        Modifier.fillMaxWidth()
                ) {

                    Text(

                        text =
                            "Chưa có buổi điểm danh.",

                        modifier =
                            Modifier.padding(
                                16.dp
                            )
                    )
                }
            }


            sessions.forEach { session ->


                val selected =
                    session.id ==
                            selectedSessionId


                Card(

                    onClick = {

                        selectedSessionId =
                            session.id
                    },

                    modifier =
                        Modifier.fillMaxWidth(),

                    shape =
                        RoundedCornerShape(
                            18.dp
                        ),

                    colors =
                        CardDefaults.cardColors(

                            containerColor =
                                if (
                                    selected
                                ) {

                                    MaterialTheme
                                        .colorScheme
                                        .primaryContainer

                                } else {

                                    MaterialTheme
                                        .colorScheme
                                        .surface
                                }
                        )
                ) {

                    Column(

                        modifier =
                            Modifier.padding(
                                16.dp
                            )
                    ) {


                        Text(

                            text =
                                session.subjectName,

                            fontSize =
                                17.sp,

                            fontWeight =
                                FontWeight.Bold
                        )


                        Text(

                            text =
                                "${session.date} • A102"
                        )


                        Text(

                            text =
                                "Giờ học: ${
                                    timestampToDisplayTime(
                                        session.startTime
                                    )
                                } - ${
                                    timestampToDisplayTime(
                                        session.endTime
                                    )
                                }"
                        )


                        Text(

                            text =
                                "Check-in: ${
                                    timestampToDisplayTime(
                                        session.checkInOpenAt
                                    )
                                } - ${
                                    timestampToDisplayTime(
                                        session.checkInCloseAt
                                    )
                                }"
                        )


                        Spacer(
                            Modifier.height(
                                8.dp
                            )
                        )


                        Text(

                            text =
                                when (
                                    session.status
                                ) {

                                    "open" ->
                                        "ĐANG MỞ"

                                    "closed" ->
                                        "ĐÃ ĐÓNG"

                                    else ->
                                        "CHƯA MỞ"
                                },

                            fontWeight =
                                FontWeight.Bold,

                            color =
                                when (
                                    session.status
                                ) {

                                    "open" ->
                                        Color(
                                            0xFF2E7D32
                                        )

                                    "closed" ->
                                        MaterialTheme
                                            .colorScheme
                                            .onSurfaceVariant

                                    else ->
                                        Color(
                                            0xFFEF6C00
                                        )
                                }
                        )


                        Spacer(
                            Modifier.height(
                                10.dp
                            )
                        )


                        if (
                            session.status ==
                            "draft"
                        ) {

                            Button(

                                onClick = {

                                    openSession(
                                        session
                                    )
                                },

                                modifier =
                                    Modifier.fillMaxWidth()
                            ) {

                                Text(
                                    "MỞ ĐIỂM DANH"
                                )
                            }


                            Spacer(
                                Modifier.height(
                                    8.dp
                                )
                            )
                        }


                        if (
                            session.status ==
                            "open"
                        ) {

                            OutlinedButton(

                                onClick = {

                                    closeSession(
                                        session
                                    )
                                },

                                modifier =
                                    Modifier.fillMaxWidth()
                            ) {

                                Text(
                                    "ĐÓNG ĐIỂM DANH"
                                )
                            }


                            Spacer(
                                Modifier.height(
                                    8.dp
                                )
                            )
                        }


                        Row(

                            modifier =
                                Modifier.fillMaxWidth(),

                            horizontalArrangement =
                                Arrangement.spacedBy(
                                    8.dp
                                )
                        ) {


                            OutlinedButton(

                                onClick = {

                                    openEditDialog(
                                        session
                                    )
                                },

                                modifier =
                                    Modifier.weight(
                                        1f
                                    )
                            ) {

                                Text(
                                    "SỬA"
                                )
                            }


                            OutlinedButton(

                                onClick = {

                                    sessionPendingDelete =
                                        session
                                },

                                modifier =
                                    Modifier.weight(
                                        1f
                                    )
                            ) {

                                Text(

                                    text =
                                        "XÓA",

                                    color =
                                        MaterialTheme
                                            .colorScheme
                                            .error
                                )
                            }
                        }
                    }
                }


                Spacer(
                    Modifier.height(
                        10.dp
                    )
                )
            }


            if (
                selectedSession !=
                null
            ) {

                Spacer(
                    Modifier.height(
                        16.dp
                    )
                )


                Text(

                    text =
                        "Thống kê: ${selectedSession.subjectName}",

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
                        Arrangement.SpaceBetween
                ) {


                    AttendanceStat(
                        students.size,
                        "Tổng"
                    )


                    AttendanceStat(
                        presentCount,
                        "Đã điểm danh"
                    )


                    AttendanceStat(
                        lateCount,
                        "Đi muộn"
                    )


                    AttendanceStat(
                        absentCount,
                        "Chưa"
                    )
                }


                Spacer(
                    Modifier.height(
                        18.dp
                    )
                )


                students.forEach { student ->


                    val record =
                        selectedRecords
                            .firstOrNull {

                                it.studentId ==
                                        student.studentId
                            }


                    AttendanceStudentRow(
                        student,
                        record
                    )


                    Spacer(
                        Modifier.height(
                            8.dp
                        )
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


    // =====================================================
    // CREATE / EDIT DIALOG
    // =====================================================

    if (
        showSessionDialog
    ) {

        val dialogScrollState =
            rememberScrollState()


        AlertDialog(

            modifier =
                Modifier.imePadding(),

            onDismissRequest = {

                dismissSessionDialog()
            },

            title = {

                Text(

                    if (
                        editingSession ==
                        null
                    ) {

                        "TẠO BUỔI ĐIỂM DANH"

                    } else {

                        "SỬA BUỔI ĐIỂM DANH"
                    }
                )
            },

            text = {

                Column(

                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(
                                max = 400.dp
                            )
                            .verticalScroll(
                                dialogScrollState
                            )
                            .padding(
                                bottom = 8.dp
                            )
                ) {


                    OutlinedTextField(

                        value =
                            subjectName,

                        onValueChange = {

                            subjectName =
                                it

                            formErrorMessage =
                                ""
                        },

                        label = {

                            Text(
                                "Tên môn"
                            )
                        },

                        singleLine =
                            true,

                        modifier =
                            Modifier.fillMaxWidth()
                    )


                    Spacer(
                        Modifier.height(
                            10.dp
                        )
                    )


                    OutlinedTextField(

                        value =
                            sessionDate,

                        onValueChange = {

                            sessionDate =
                                it

                            formErrorMessage =
                                ""
                        },

                        label = {

                            Text(
                                "Ngày (dd/MM/yyyy)"
                            )
                        },

                        singleLine =
                            true,

                        modifier =
                            Modifier.fillMaxWidth()
                    )


                    Spacer(
                        Modifier.height(
                            10.dp
                        )
                    )


                    // =================================================
                    // START TIME
                    // =================================================

                    OutlinedTextField(

                        value =
                            startTimeText,

                        onValueChange = {

                            startTimeText =
                                sanitizeTimeInput(
                                    it
                                )

                            formErrorMessage =
                                ""
                        },

                        label = {

                            Text(
                                "Giờ bắt đầu (HH:mm)"
                            )
                        },

                        singleLine =
                            true,

                        keyboardOptions =
                            KeyboardOptions(
                                keyboardType =
                                    KeyboardType.Number
                            ),

                        visualTransformation =
                            timeTransformation,

                        modifier =
                            Modifier.fillMaxWidth()
                    )


                    Spacer(
                        Modifier.height(
                            10.dp
                        )
                    )


                    // =================================================
                    // END TIME
                    // =================================================

                    OutlinedTextField(

                        value =
                            endTimeText,

                        onValueChange = {

                            endTimeText =
                                sanitizeTimeInput(
                                    it
                                )

                            formErrorMessage =
                                ""
                        },

                        label = {

                            Text(
                                "Giờ kết thúc (HH:mm)"
                            )
                        },

                        singleLine =
                            true,

                        keyboardOptions =
                            KeyboardOptions(
                                keyboardType =
                                    KeyboardType.Number
                            ),

                        visualTransformation =
                            timeTransformation,

                        modifier =
                            Modifier.fillMaxWidth()
                    )


                    Spacer(
                        Modifier.height(
                            10.dp
                        )
                    )


                    // =================================================
                    // CHECK-IN OPEN
                    // =================================================

                    OutlinedTextField(

                        value =
                            openAtText,

                        onValueChange = {

                            openAtText =
                                sanitizeTimeInput(
                                    it
                                )

                            formErrorMessage =
                                ""
                        },

                        label = {

                            Text(
                                "Mở check-in (HH:mm)"
                            )
                        },

                        singleLine =
                            true,

                        keyboardOptions =
                            KeyboardOptions(
                                keyboardType =
                                    KeyboardType.Number
                            ),

                        visualTransformation =
                            timeTransformation,

                        modifier =
                            Modifier.fillMaxWidth()
                    )


                    Spacer(
                        Modifier.height(
                            10.dp
                        )
                    )


                    // =================================================
                    // CHECK-IN CLOSE
                    // =================================================

                    OutlinedTextField(

                        value =
                            closeAtText,

                        onValueChange = {

                            closeAtText =
                                sanitizeTimeInput(
                                    it
                                )

                            formErrorMessage =
                                ""
                        },

                        label = {

                            Text(
                                "Đóng check-in (HH:mm)"
                            )
                        },

                        singleLine =
                            true,

                        keyboardOptions =
                            KeyboardOptions(
                                keyboardType =
                                    KeyboardType.Number
                            ),

                        visualTransformation =
                            timeTransformation,

                        modifier =
                            Modifier.fillMaxWidth()
                    )


                    if (
                        formErrorMessage.isNotBlank()
                    ) {

                        Spacer(
                            Modifier.height(
                                12.dp
                            )
                        )


                        Text(

                            text =
                                formErrorMessage,

                            color =
                                MaterialTheme
                                    .colorScheme
                                    .error,

                            fontSize =
                                13.sp
                        )
                    }


                    Spacer(
                        Modifier.height(
                            16.dp
                        )
                    )
                }
            },

            confirmButton = {

                Button(

                    onClick = {

                        saveSession()
                    },

                    enabled =
                        !isSaving
                ) {

                    Text(

                        when {

                            isSaving ->
                                "ĐANG LƯU..."

                            editingSession !=
                                    null ->
                                "LƯU THAY ĐỔI"

                            else ->
                                "TẠO BUỔI"
                        }
                    )
                }
            },

            dismissButton = {

                TextButton(

                    onClick = {

                        dismissSessionDialog()
                    },

                    enabled =
                        !isSaving
                ) {

                    Text(
                        "HỦY"
                    )
                }
            }
        )
    }


    // =====================================================
    // DELETE DIALOG
    // =====================================================

    val deletingSession =
        sessionPendingDelete


    if (
        deletingSession !=
        null
    ) {

        AlertDialog(

            onDismissRequest = {

                if (
                    !isDeleting
                ) {

                    sessionPendingDelete =
                        null
                }
            },

            title = {

                Text(
                    "XÓA BUỔI ĐIỂM DANH"
                )
            },

            text = {

                Column {


                    Text(
                        "Bạn có chắc muốn xóa buổi điểm danh:"
                    )


                    Spacer(
                        Modifier.height(
                            8.dp
                        )
                    )


                    Text(

                        text =
                            deletingSession.subjectName,

                        fontWeight =
                            FontWeight.Bold
                    )


                    Text(
                        "${deletingSession.date} • A102"
                    )


                    Spacer(
                        Modifier.height(
                            10.dp
                        )
                    )


                    Text(

                        text =
                            "Các bản ghi điểm danh của buổi này cũng sẽ bị xóa.",

                        color =
                            MaterialTheme
                                .colorScheme
                                .error,

                        fontSize =
                            13.sp
                    )
                }
            },

            confirmButton = {

                Button(

                    onClick = {

                        deleteSession(
                            deletingSession
                        )
                    },

                    enabled =
                        !isDeleting
                ) {

                    Text(

                        if (
                            isDeleting
                        ) {

                            "ĐANG XÓA..."

                        } else {

                            "XÓA"
                        }
                    )
                }
            },

            dismissButton = {

                TextButton(

                    onClick = {

                        sessionPendingDelete =
                            null
                    },

                    enabled =
                        !isDeleting
                ) {

                    Text(
                        "HỦY"
                    )
                }
            }
        )
    }
}


// =========================================================
// STAT
// =========================================================

@Composable
fun AttendanceStat(
    value: Int,
    title: String
) {

    Column {


        Text(

            text =
                value.toString(),

            fontSize =
                22.sp,

            fontWeight =
                FontWeight.Bold,

            color =
                MaterialTheme
                    .colorScheme
                    .primary
        )


        Text(

            text =
                title,

            fontSize =
                10.sp
        )
    }
}


// =========================================================
// STUDENT ROW
// =========================================================

@Composable
fun AttendanceStudentRow(
    student: AdminAttendanceStudent,
    record: AdminAttendanceRecord?
) {

    Card(

        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(
                16.dp
            )
    ) {

        Column(

            modifier =
                Modifier.padding(
                    14.dp
                )
        ) {


            Text(

                text =
                    student.fullName,

                fontWeight =
                    FontWeight.Bold
            )


            Text(

                text =
                    student.studentId,

                fontSize =
                    12.sp
            )


            Spacer(
                Modifier.height(
                    6.dp
                )
            )


            when {

                record ==
                        null -> {

                    Text(

                        text =
                            "CHƯA ĐIỂM DANH",

                        color =
                            MaterialTheme
                                .colorScheme
                                .error,

                        fontWeight =
                            FontWeight.Bold
                    )
                }


                record.status ==
                        "late" -> {

                    Text(

                        text =
                            "ĐI MUỘN",

                        color =
                            Color(
                                0xFFEF6C00
                            ),

                        fontWeight =
                            FontWeight.Bold
                    )
                }


                else -> {

                    Text(

                        text =
                            "ĐÃ ĐIỂM DANH",

                        color =
                            Color(
                                0xFF2E7D32
                            ),

                        fontWeight =
                            FontWeight.Bold
                    )
                }
            }


            if (
                record !=
                null
            ) {

                val time =
                    record
                        .checkInAt
                        ?.let {

                            SimpleDateFormat(
                                "HH:mm",
                                Locale.getDefault()
                            ).format(
                                it.toDate()
                            )
                        }
                        ?: "--:--"


                Text(

                    text =
                        "$time • ${
                            if (
                                record.checkInMethod ==
                                "nfc"
                            ) {

                                "NFC"

                            } else {

                                "VÂN TAY"
                            }
                        }",

                    fontSize =
                        12.sp
                )
            }
        }
    }
}