package com.example.mycamerafinal.data

import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/* ============================== data models ==============================
 * These small "data classes" are the app's in-memory picture of one database
 * row each (user, subject, material, exam, submission, result). The screens
 * work with these objects instead of raw JSON. The "Fs" prefix and the name
 * "Fire" are historical (an early version used Firebase) — today everything
 * is served by SQLite or the cPanel MySQL API.
 * ========================================================================== */

data class FsUser(
    val id: String, val fullName: String, val email: String,
    val role: String, val phone: String, val active: Boolean
) {
    override fun toString() = "$fullName\n$email  •  ${if (active) "Active" else "Inactive"}"
}

data class FsSubject(
    val id: String, val code: String, val name: String,
    val description: String, val lecturerId: String
) {
    override fun toString() = "$code - $name"
}

data class FsMaterial(
    val id: String, val subjectId: String, val title: String,
    val type: String, val fileUrl: String, val uploadedBy: String
) {
    override fun toString() = "[$type] $title"
}

data class FsExam(
    val id: String, val subjectId: String, val title: String, val description: String,
    val startAt: Long, val deadline: Long, val paperUrl: String,
    val durationMinutes: Int, val totalMarks: Int, val createdBy: String,
    val published: Boolean, val subjectCode: String = "", val subjectName: String = ""
) {
    val hasPaper get() = paperUrl.isNotBlank()
    val examDateStr get() = Fire.fmt(startAt)
    val deadlineStr get() = Fire.fmt(deadline)

    fun isOverdue() = deadline in 1 until System.currentTimeMillis()

    /** True once the exam start time is reached (or no start time was set). */
    fun hasStarted() = startAt <= 0 || System.currentTimeMillis() >= startAt

    /** Student may submit an answer sheet only inside the [start, deadline] window. */
    fun isSubmissionOpen() = hasStarted() && !isOverdue()

    fun statusLabel(): String {
        if (deadline <= 0) return "No deadline"
        val diff = deadline - System.currentTimeMillis()
        if (diff < 0) return "Overdue"
        val days = TimeUnit.MILLISECONDS.toDays(diff)
        return when (days) { 0L -> "Due today"; 1L -> "Due tomorrow"; else -> "Due in $days days" }
    }

    fun reminder(withinDays: Long = 7): String? {
        if (deadline <= 0) return null
        val diff = deadline - System.currentTimeMillis()
        val days = TimeUnit.MILLISECONDS.toDays(diff)
        return when {
            diff < 0 -> "⛔ OVERDUE — '$title' ($subjectName) answer sheet was due $deadlineStr"
            days <= withinDays -> "⏰ Reminder — '$title' ($subjectName) ${statusLabel().lowercase()} ($deadlineStr)"
            else -> null
        }
    }

    override fun toString(): String {
        val sb = StringBuilder("$title\n$subjectName  •  Exam: $examDateStr")
        if (deadline > 0) sb.append("\n⏰ Submit by: $deadlineStr")
        if (hasPaper) sb.append("   📄 paper attached")
        return sb.toString()
    }
}

data class FsSubmission(
    val id: String, val examId: String, val studentId: String,
    val studentName: String, val examTitle: String,
    val answerSheetUrls: List<String>, val status: String, val marks: Long?
) {
    val fileUrl get() = answerSheetUrls.firstOrNull() ?: ""
    val marksLabel get() = if (marks == null) "Not graded" else "$marks"
}

data class FsResult(
    val id: String, val studentId: String, val subjectId: String, val examId: String,
    val examTitle: String, val marks: Long, val totalMarks: Long, val grade: String
)

/* ============================== data layer ============================== */

/**
 * DATA ACCESS OBJECT — every screen reads/writes the database ONLY through
 * the methods of this object (getAllSubjects, addExam, gradeSubmission, …).
 * Each method builds the right request and hands it to [Api], which serves
 * it from SQLite (offline) or the cPanel PHP/MySQL API (online).
 * All calls are asynchronous: the answer arrives later in a callback.
 */
object Fire {

    const val ROLE_STUDENT = "student"
    const val ROLE_LECTURER = "lecturer"
    const val ROLE_ADMIN = "admin"

    // Dates travel to/from the database as text ("2026-07-20 09:00:00") but the
    // app works with milliseconds — these two formats convert between the two.
    private val sqlOut = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
    private val inFormats = listOf(
        "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm", "yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd"
    )

    fun fmt(millis: Long): String =
        if (millis <= 0) "—" else SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(millis))

    private fun toSql(millis: Long): String = sqlOut.format(Date(millis))

    private fun parseSql(value: String): Long {
        if (value.isBlank()) return 0
        for (f in inFormats) {
            try {
                return SimpleDateFormat(f, Locale.US).parse(value)?.time ?: continue
            } catch (_: Exception) { /* try next */ }
        }
        return 0
    }

    // ------------------------------------------------------------- helpers ---

    private fun rows(data: Any?): List<JSONObject> {
        val a = data as? JSONArray ?: return emptyList()
        return (0 until a.length()).map { a.getJSONObject(it) }
    }

    private fun str(o: JSONObject, key: String): String =
        if (o.isNull(key)) "" else o.optString(key, "")

    private fun toUser(o: JSONObject) =
        FsUser(str(o, "id"), str(o, "name"), str(o, "email"), str(o, "role"),
            str(o, "phone"), o.optInt("active", 1) == 1)

    private fun toSubject(o: JSONObject) =
        FsSubject(str(o, "id"), str(o, "code"), str(o, "name"),
            str(o, "description"), str(o, "lecturer_id"))

    private fun toMaterial(o: JSONObject) =
        FsMaterial(str(o, "id"), str(o, "subject_id"), str(o, "title"),
            str(o, "type"), str(o, "file_url"), str(o, "uploaded_by"))

    private fun toExam(o: JSONObject) =
        FsExam(str(o, "id"), str(o, "subject_id"), str(o, "title"), str(o, "description"),
            parseSql(str(o, "exam_date")), parseSql(str(o, "deadline")), str(o, "paper_url"),
            o.optInt("duration_minutes", 60), 100, str(o, "created_by"), true,
            str(o, "subject_code"), str(o, "subject_name").ifBlank { "Unknown subject" })

    private fun toSubmission(o: JSONObject) =
        FsSubmission(str(o, "id"), str(o, "exam_id"), str(o, "student_id"),
            str(o, "student_name").ifBlank { "Student" }, str(o, "exam_title").ifBlank { "Exam" },
            listOf(str(o, "file_url")), str(o, "status").uppercase(),
            if (o.isNull("marks")) null else o.optLong("marks"))

    private fun toResult(o: JSONObject): FsResult {
        val marks = if (o.isNull("marks")) 0L else o.optLong("marks")
        return FsResult(str(o, "id"), str(o, "student_id"), "", str(o, "exam_id"),
            str(o, "exam_title").ifBlank { "Exam" }, marks, 100, gradeFor(marks.toInt(), 100))
    }

    // --------------------------------------------------------------- users ---

    fun getUsersByRole(role: String, onResult: (List<FsUser>) -> Unit) {
        Api.get("users.php?role=${role.lowercase()}") { ok, data, _ ->
            onResult(if (ok) rows(data).map { toUser(it) } else emptyList())
        }
    }

    fun setUserActive(uid: String, active: Boolean, onDone: () -> Unit = {}) {
        Api.post("users.php?action=set_active", mapOf("id" to uid, "active" to if (active) 1 else 0)) { _, _, _ -> onDone() }
    }

    // ----------------------------------------------------------- subjects ---

    fun getAllSubjects(onResult: (List<FsSubject>) -> Unit) {
        Api.get("subjects.php") { ok, data, _ ->
            onResult(if (ok) rows(data).map { toSubject(it) } else emptyList())
        }
    }

    fun getSubjectsByLecturer(uid: String, onResult: (List<FsSubject>) -> Unit) {
        Api.get("subjects.php?lecturer_id=$uid") { ok, data, _ ->
            onResult(if (ok) rows(data).map { toSubject(it) } else emptyList())
        }
    }

    fun addSubject(code: String, name: String, lecturerId: String, onDone: (Boolean) -> Unit) {
        val body = mutableMapOf<String, Any?>("code" to code, "name" to name, "description" to "")
        if (lecturerId.isNotBlank()) body["lecturer_id"] = lecturerId
        Api.post("subjects.php?action=add", body) { ok, _, _ -> onDone(ok) }
    }

    fun updateSubject(id: String, code: String, name: String, onDone: () -> Unit = {}) {
        Api.post("subjects.php?action=update",
            mapOf("id" to id, "code" to code, "name" to name, "description" to "")) { _, _, _ -> onDone() }
    }

    fun deleteSubject(id: String, onDone: () -> Unit = {}) {
        Api.post("subjects.php?action=delete", mapOf("id" to id)) { _, _, _ -> onDone() }
    }

    // ---------------------------------------------------------- materials ---

    fun getMaterialsBySubject(subjectId: String, onResult: (List<FsMaterial>) -> Unit) {
        Api.get("materials.php?subject_id=$subjectId") { ok, data, _ ->
            onResult(if (ok) rows(data).map { toMaterial(it) } else emptyList())
        }
    }

    fun getAllMaterials(onResult: (List<FsMaterial>) -> Unit) {
        Api.get("materials.php") { ok, data, _ ->
            onResult(if (ok) rows(data).map { toMaterial(it) } else emptyList())
        }
    }

    fun addMaterial(subjectId: String, title: String, type: String, fileUrl: String,
                    uploadedBy: String, onDone: (Boolean) -> Unit) {
        Api.post("materials.php?action=add", mapOf(
            "subject_id" to subjectId, "title" to title, "type" to type,
            "file_url" to fileUrl, "uploaded_by" to uploadedBy
        )) { ok, _, _ -> onDone(ok) }
    }

    fun deleteMaterial(id: String, onDone: () -> Unit = {}) {
        Api.post("materials.php?action=delete", mapOf("id" to id)) { _, _, _ -> onDone() }
    }

    // -------------------------------------------------------------- exams ---

    fun getAllExams(onResult: (List<FsExam>) -> Unit) {
        Api.get("exams.php") { ok, data, _ ->
            onResult(if (ok) rows(data).map { toExam(it) } else emptyList())
        }
    }

    fun getExamsByLecturer(uid: String, onResult: (List<FsExam>) -> Unit) {
        getAllExams { list -> onResult(list.filter { it.createdBy == uid }) }
    }

    fun getExam(id: String, onResult: (FsExam?) -> Unit) {
        getAllExams { list -> onResult(list.find { it.id == id }) }
    }

    fun addExam(subjectId: String, title: String, startAt: Long, deadline: Long,
                paperUrl: String, createdBy: String, onDone: (Boolean) -> Unit) {
        Api.post("exams.php?action=add", mapOf(
            "subject_id" to subjectId, "title" to title, "description" to "",
            "exam_date" to toSql(startAt), "deadline" to toSql(deadline),
            "paper_url" to paperUrl, "duration_minutes" to 60, "created_by" to createdBy
        )) { ok, _, _ -> onDone(ok) }
    }

    // -------------------------------------------------------- submissions ---

    fun hasSubmitted(examId: String, studentId: String, onResult: (Boolean) -> Unit) {
        Api.get("submissions.php?student_id=$studentId") { ok, data, _ ->
            onResult(ok && rows(data).any { str(it, "exam_id") == examId })
        }
    }

    fun getSubmittedExamIds(uid: String, onResult: (Set<String>) -> Unit) {
        Api.get("submissions.php?student_id=$uid") { ok, data, _ ->
            onResult(if (ok) rows(data).map { str(it, "exam_id") }.toSet() else emptySet())
        }
    }

    fun addSubmission(examId: String, studentId: String, studentName: String, examTitle: String,
                      fileUrl: String, onDone: (Boolean) -> Unit) {
        Api.post("submissions.php?action=add",
            mapOf("exam_id" to examId, "student_id" to studentId, "file_url" to fileUrl)) { ok, _, _ -> onDone(ok) }
    }

    fun getSubmissionsByExam(examId: String, onResult: (List<FsSubmission>) -> Unit) {
        Api.get("submissions.php?exam_id=$examId") { ok, data, _ ->
            onResult(if (ok) rows(data).map { toSubmission(it) } else emptyList())
        }
    }

    fun gradeSubmission(sub: FsSubmission, exam: FsExam, marks: Int, onDone: (Boolean) -> Unit) {
        val gradedBy = SessionManager(App.context).uid
        Api.post("submissions.php?action=grade",
            mapOf("id" to sub.id, "marks" to marks, "graded_by" to gradedBy)) { ok, _, _ ->
            if (ok) addNotification(sub.studentId, "Result published",
                "Your result for '${exam.title}' is $marks/${exam.totalMarks}")
            onDone(ok)
        }
    }

    // ------------------------------------------------------------ results ---

    fun getResultsForStudent(uid: String, onResult: (List<FsResult>) -> Unit) {
        Api.get("submissions.php?student_id=$uid") { ok, data, _ ->
            onResult(if (ok) rows(data).filter { str(it, "status") == "graded" }.map { toResult(it) } else emptyList())
        }
    }

    /**
     * Convert marks into a letter grade using the standard Sri Lankan scale:
     * 75+ = A, 65–74 = B, 55–64 = C, 40–54 = S (simple pass), below 40 = F.
     */
    fun gradeFor(marks: Int, total: Int): String {
        if (total <= 0) return "-"
        val pct = marks * 100.0 / total          // percentage of the total marks
        return when {
            pct >= 75 -> "A"; pct >= 65 -> "B"; pct >= 55 -> "C"; pct >= 40 -> "S"; else -> "F"
        }
    }

    // ------------------------------------------------------ notifications ---

    fun addNotification(userId: String, title: String, message: String) {
        val uid = if (userId == "ALL" || userId.isBlank()) "0" else userId
        Api.post("notifications.php?action=add",
            mapOf("user_id" to uid, "title" to title, "message" to message)) { _, _, _ -> }
    }

    fun getNotificationsForUser(uid: String, onResult: (List<String>) -> Unit) {
        Api.get("notifications.php?user_id=$uid") { ok, data, _ ->
            onResult(if (ok) rows(data).map { str(it, "title") + "\n" + str(it, "message") } else emptyList())
        }
    }

    // -------------------------------------------------------------- files ---

    /** Upload a local file to the server (upload.php); returns the public URL (or null). */
    fun uploadFile(localPath: String, folder: String, owner: String, onDone: (String?) -> Unit) {
        val file = File(localPath)
        if (!file.exists()) { onDone(null); return }
        val bytes = try { file.readBytes() } catch (e: Exception) { onDone(null); return }
        Api.upload("upload.php", file.name, bytes, mapOf("folder" to folder, "owner" to owner)) { ok, data, _ ->
            onDone(if (ok && data is JSONObject) data.optString("url").ifBlank { null } else null)
        }
    }
}
