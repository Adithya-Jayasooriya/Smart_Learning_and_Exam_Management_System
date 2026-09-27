package com.example.mycamerafinal.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * On-device SQLite implementation of the same REST API the PHP/MySQL backend
 * exposes (auth.php, users.php, subjects.php, materials.php, exams.php,
 * submissions.php, notifications.php). [Api] routes every request here while
 * no server URL is configured, so the whole app works offline out of the box.
 *
 * Seeded demo accounts (email / password):
 *   admin@slems.lk    / admin123     – Admin
 *   lecturer@slems.lk / lecturer123  – Lecturer
 *   student@slems.lk  / student123   – Student
 */
class LocalDb(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    private val appFilesDir: File = context.filesDir

    companion object {
        private const val DB_NAME = "smart_learning.db"
        private const val DB_VERSION = 1
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE users(
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                email TEXT NOT NULL UNIQUE COLLATE NOCASE,
                password TEXT NOT NULL,
                role TEXT NOT NULL,
                phone TEXT NOT NULL DEFAULT '',
                active INTEGER NOT NULL DEFAULT 1)"""
        )
        db.execSQL(
            """CREATE TABLE subjects(
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                code TEXT NOT NULL,
                name TEXT NOT NULL,
                description TEXT NOT NULL DEFAULT '',
                lecturer_id INTEGER)"""
        )
        db.execSQL(
            """CREATE TABLE materials(
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                subject_id INTEGER NOT NULL,
                title TEXT NOT NULL,
                type TEXT NOT NULL,
                file_url TEXT NOT NULL DEFAULT '',
                uploaded_by INTEGER,
                created_at TEXT NOT NULL DEFAULT (datetime('now','localtime')))"""
        )
        db.execSQL(
            """CREATE TABLE exams(
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                subject_id INTEGER NOT NULL,
                title TEXT NOT NULL,
                description TEXT NOT NULL DEFAULT '',
                exam_date TEXT NOT NULL DEFAULT '',
                deadline TEXT NOT NULL DEFAULT '',
                paper_url TEXT NOT NULL DEFAULT '',
                duration_minutes INTEGER NOT NULL DEFAULT 60,
                created_by INTEGER,
                created_at TEXT NOT NULL DEFAULT (datetime('now','localtime')))"""
        )
        db.execSQL(
            """CREATE TABLE submissions(
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                exam_id INTEGER NOT NULL,
                student_id INTEGER NOT NULL,
                file_url TEXT NOT NULL DEFAULT '',
                status TEXT NOT NULL DEFAULT 'submitted',
                marks INTEGER,
                graded_by INTEGER,
                created_at TEXT NOT NULL DEFAULT (datetime('now','localtime')),
                UNIQUE(exam_id, student_id))"""
        )
        db.execSQL(
            """CREATE TABLE notifications(
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER NOT NULL DEFAULT 0,
                title TEXT NOT NULL,
                message TEXT NOT NULL DEFAULT '',
                created_at TEXT NOT NULL DEFAULT (datetime('now','localtime')))"""
        )
        seed(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        for (t in listOf("notifications", "submissions", "exams", "materials", "subjects", "users"))
            db.execSQL("DROP TABLE IF EXISTS $t")
        onCreate(db)
    }

    private fun seed(db: SQLiteDatabase) {
        fun user(name: String, email: String, pass: String, role: String) =
            db.insert("users", null, ContentValues().apply {
                put("name", name); put("email", email); put("password", pass)
                put("role", role); put("active", 1)
            })
        user("System Admin", "admin@slems.lk", "admin123", Roles.ADMIN)
        val lecturerId = user("Demo Lecturer", "lecturer@slems.lk", "lecturer123", Roles.LECTURER)
        user("Demo Student", "student@slems.lk", "student123", Roles.STUDENT)

        fun subject(code: String, name: String) =
            db.insert("subjects", null, ContentValues().apply {
                put("code", code); put("name", name)
                put("description", ""); put("lecturer_id", lecturerId)
            })
        subject("IT101", "Introduction to Programming")
        subject("IT202", "Mobile Application Development")
    }

    // ------------------------------------------------------------ dispatch ---

    /**
     * Serve one request. [pathAndQuery] is what the app passes to [Api]
     * (e.g. "subjects.php?action=add"); [body] is the JSON the app POSTs.
     * Returns (success, data, message) — the same triple the PHP API returns.
     */
    fun handle(pathAndQuery: String, body: JSONObject?): Triple<Boolean, Any?, String> {
        val endpoint = pathAndQuery.substringBefore('?').substringAfterLast('/')
        val params = parseQuery(pathAndQuery.substringAfter('?', ""))
        val action = params["action"] ?: ""
        val b = body ?: JSONObject()
        return when (endpoint) {
            "auth.php" -> auth(action, b)
            "users.php" -> users(action, params, b)
            "subjects.php" -> subjects(action, params, b)
            "materials.php" -> materials(action, params, b)
            "exams.php" -> exams(action, params, b)
            "submissions.php" -> submissions(action, params, b)
            "notifications.php" -> notifications(action, params, b)
            else -> Triple(false, null, "Unknown endpoint: $endpoint")
        }
    }

    /** Local replacement for upload.php: keep the file in app storage, return its path as the URL. */
    fun saveUpload(fileName: String, bytes: ByteArray, folder: String): JSONObject {
        val safeName = fileName.replace(Regex("[^A-Za-z0-9._-]"), "_").ifBlank { "file" }
        val dir = File(File(appFilesDir, "uploads"), folder).apply { mkdirs() }
        val dest = File(dir, "${System.currentTimeMillis()}_$safeName")
        dest.writeBytes(bytes)
        return JSONObject().put("url", dest.absolutePath)
    }

    // ---------------------------------------------------------------- auth ---

    private fun auth(action: String, b: JSONObject): Triple<Boolean, Any?, String> = when (action) {
        "login" -> {
            val email = b.optString("email").trim()
            val pass = b.optString("password")
            readableDatabase.rawQuery(
                "SELECT id, name, email, role, password, active FROM users WHERE email = ?", arrayOf(email)
            ).use { c ->
                when {
                    !c.moveToFirst() -> Triple(false, null, "No account found for this email")
                    c.getString(c.getColumnIndexOrThrow("password")) != pass ->
                        Triple(false, null, "Incorrect password")
                    c.getInt(c.getColumnIndexOrThrow("active")) != 1 ->
                        Triple(false, null, "This account has been deactivated")
                    else -> Triple(true, JSONObject().apply {
                        put("id", c.getLong(c.getColumnIndexOrThrow("id")))
                        put("name", c.getString(c.getColumnIndexOrThrow("name")))
                        put("email", c.getString(c.getColumnIndexOrThrow("email")))
                        put("role", c.getString(c.getColumnIndexOrThrow("role")))
                    }, "Welcome")
                }
            }
        }
        "register" -> insertUser(b.optString("name").trim(), b.optString("email").trim(),
            b.optString("password"), Roles.STUDENT)
        "change_password" -> {
            val n = writableDatabase.update("users",
                ContentValues().apply { put("password", b.optString("new_password")) },
                "id = ?", arrayOf(b.optString("user_id")))
            if (n > 0) Triple(true, null, "Password updated") else Triple(false, null, "User not found")
        }
        else -> Triple(false, null, "Unknown auth action: $action")
    }

    private fun insertUser(name: String, email: String, pass: String, role: String): Triple<Boolean, Any?, String> {
        if (name.isBlank() || email.isBlank() || pass.isBlank())
            return Triple(false, null, "Name, email and password are required")
        readableDatabase.rawQuery("SELECT id FROM users WHERE email = ?", arrayOf(email)).use { c ->
            if (c.moveToFirst()) return Triple(false, null, "This email is already registered")
        }
        val id = writableDatabase.insert("users", null, ContentValues().apply {
            put("name", name); put("email", email); put("password", pass)
            put("role", role.lowercase()); put("active", 1)
        })
        return if (id > 0) Triple(true, JSONObject().put("id", id), "Account created")
        else Triple(false, null, "Could not create the account")
    }

    // --------------------------------------------------------------- users ---

    private fun users(action: String, params: Map<String, String>, b: JSONObject): Triple<Boolean, Any?, String> =
        when (action) {
            "" -> query(
                "SELECT id, name, email, role, phone, active FROM users WHERE role = ? ORDER BY name",
                arrayOf(params["role"].orEmpty().lowercase())
            )
            "add" -> insertUser(b.optString("name").trim(), b.optString("email").trim(),
                b.optString("password"), b.optString("role", Roles.STUDENT))
            "set_active" -> {
                val n = writableDatabase.update("users",
                    ContentValues().apply { put("active", b.optInt("active", 1)) },
                    "id = ?", arrayOf(b.optString("id")))
                if (n > 0) Triple(true, null, "Updated") else Triple(false, null, "User not found")
            }
            else -> Triple(false, null, "Unknown users action: $action")
        }

    // ------------------------------------------------------------ subjects ---

    private fun subjects(action: String, params: Map<String, String>, b: JSONObject): Triple<Boolean, Any?, String> =
        when (action) {
            "" -> {
                val lecturerId = params["lecturer_id"]
                if (lecturerId.isNullOrBlank())
                    query("SELECT * FROM subjects ORDER BY code", emptyArray())
                else
                    query("SELECT * FROM subjects WHERE lecturer_id = ? ORDER BY code", arrayOf(lecturerId))
            }
            "add" -> {
                val code = b.optString("code").trim()
                val name = b.optString("name").trim()
                if (code.isBlank() || name.isBlank()) Triple(false, null, "Code and name are required")
                else {
                    val id = writableDatabase.insert("subjects", null, ContentValues().apply {
                        put("code", code); put("name", name)
                        put("description", b.optString("description"))
                        if (b.has("lecturer_id")) put("lecturer_id", b.optString("lecturer_id"))
                    })
                    if (id > 0) Triple(true, JSONObject().put("id", id), "Subject added")
                    else Triple(false, null, "Could not add the subject")
                }
            }
            "update" -> {
                val n = writableDatabase.update("subjects", ContentValues().apply {
                    put("code", b.optString("code").trim())
                    put("name", b.optString("name").trim())
                }, "id = ?", arrayOf(b.optString("id")))
                if (n > 0) Triple(true, null, "Updated") else Triple(false, null, "Subject not found")
            }
            "delete" -> {
                val id = b.optString("id")
                writableDatabase.delete("materials", "subject_id = ?", arrayOf(id))
                writableDatabase.delete("exams", "subject_id = ?", arrayOf(id))
                val n = writableDatabase.delete("subjects", "id = ?", arrayOf(id))
                if (n > 0) Triple(true, null, "Deleted") else Triple(false, null, "Subject not found")
            }
            else -> Triple(false, null, "Unknown subjects action: $action")
        }

    // ----------------------------------------------------------- materials ---

    private fun materials(action: String, params: Map<String, String>, b: JSONObject): Triple<Boolean, Any?, String> =
        when (action) {
            "" -> {
                val subjectId = params["subject_id"]
                if (subjectId.isNullOrBlank())
                    query("SELECT * FROM materials ORDER BY id DESC", emptyArray())
                else
                    query("SELECT * FROM materials WHERE subject_id = ? ORDER BY id DESC", arrayOf(subjectId))
            }
            "add" -> {
                val id = writableDatabase.insert("materials", null, ContentValues().apply {
                    put("subject_id", b.optString("subject_id"))
                    put("title", b.optString("title"))
                    put("type", b.optString("type"))
                    put("file_url", b.optString("file_url"))
                    put("uploaded_by", b.optString("uploaded_by"))
                })
                if (id > 0) Triple(true, JSONObject().put("id", id), "Material added")
                else Triple(false, null, "Could not save the material")
            }
            "delete" -> {
                val n = writableDatabase.delete("materials", "id = ?", arrayOf(b.optString("id")))
                if (n > 0) Triple(true, null, "Deleted") else Triple(false, null, "Material not found")
            }
            else -> Triple(false, null, "Unknown materials action: $action")
        }

    // --------------------------------------------------------------- exams ---

    private fun exams(action: String, params: Map<String, String>, b: JSONObject): Triple<Boolean, Any?, String> =
        when (action) {
            "" -> query(
                """SELECT e.*, s.code AS subject_code, s.name AS subject_name
                   FROM exams e LEFT JOIN subjects s ON s.id = e.subject_id
                   ORDER BY e.exam_date""", emptyArray()
            )
            "add" -> {
                val id = writableDatabase.insert("exams", null, ContentValues().apply {
                    put("subject_id", b.optString("subject_id"))
                    put("title", b.optString("title"))
                    put("description", b.optString("description"))
                    put("exam_date", b.optString("exam_date"))
                    put("deadline", b.optString("deadline"))
                    put("paper_url", b.optString("paper_url"))
                    put("duration_minutes", b.optInt("duration_minutes", 60))
                    put("created_by", b.optString("created_by"))
                })
                if (id > 0) Triple(true, JSONObject().put("id", id), "Exam created")
                else Triple(false, null, "Could not create the exam")
            }
            else -> Triple(false, null, "Unknown exams action: $action")
        }

    // --------------------------------------------------------- submissions ---

    private fun submissions(action: String, params: Map<String, String>, b: JSONObject): Triple<Boolean, Any?, String> =
        when (action) {
            "" -> {
                val base = """SELECT sub.*, u.name AS student_name, e.title AS exam_title
                              FROM submissions sub
                              LEFT JOIN users u ON u.id = sub.student_id
                              LEFT JOIN exams e ON e.id = sub.exam_id"""
                when {
                    !params["student_id"].isNullOrBlank() ->
                        query("$base WHERE sub.student_id = ? ORDER BY sub.id DESC", arrayOf(params["student_id"]!!))
                    !params["exam_id"].isNullOrBlank() ->
                        query("$base WHERE sub.exam_id = ? ORDER BY u.name", arrayOf(params["exam_id"]!!))
                    else -> query("$base ORDER BY sub.id DESC", emptyArray())
                }
            }
            "add" -> {
                val examId = b.optString("exam_id")
                val studentId = b.optString("student_id")
                val windowError = if (examId.isBlank()) null else examWindowError(examId)
                if (examId.isBlank() || studentId.isBlank())
                    Triple(false, null, "Missing exam or student id")
                else if (windowError != null)
                    Triple(false, null, windowError)
                else {
                    // Re-submitting replaces the previous answer sheet and resets the grade.
                    val values = ContentValues().apply {
                        put("file_url", b.optString("file_url"))
                        put("status", "submitted")
                        putNull("marks")
                        putNull("graded_by")
                    }
                    val updated = writableDatabase.update(
                        "submissions", values, "exam_id = ? AND student_id = ?", arrayOf(examId, studentId)
                    )
                    if (updated > 0) Triple(true, null, "Submission updated")
                    else {
                        values.put("exam_id", examId)
                        values.put("student_id", studentId)
                        val id = writableDatabase.insert("submissions", null, values)
                        if (id > 0) Triple(true, JSONObject().put("id", id), "Submitted")
                        else Triple(false, null, "Could not record the submission")
                    }
                }
            }
            "grade" -> {
                val n = writableDatabase.update("submissions", ContentValues().apply {
                    put("marks", b.optInt("marks"))
                    put("status", "graded")
                    put("graded_by", b.optString("graded_by"))
                }, "id = ?", arrayOf(b.optString("id")))
                if (n > 0) Triple(true, null, "Graded") else Triple(false, null, "Submission not found")
            }
            else -> Triple(false, null, "Unknown submissions action: $action")
        }

    // ------------------------------------------------------- notifications ---

    private fun notifications(action: String, params: Map<String, String>, b: JSONObject): Triple<Boolean, Any?, String> =
        when (action) {
            // user_id = 0 rows are broadcasts every user should see.
            "" -> query(
                "SELECT * FROM notifications WHERE user_id = ? OR user_id = 0 ORDER BY id DESC",
                arrayOf(params["user_id"].orEmpty().ifBlank { "0" })
            )
            "add" -> {
                val id = writableDatabase.insert("notifications", null, ContentValues().apply {
                    put("user_id", b.optString("user_id").ifBlank { "0" })
                    put("title", b.optString("title"))
                    put("message", b.optString("message"))
                })
                if (id > 0) Triple(true, null, "Added") else Triple(false, null, "Could not add the notification")
            }
            else -> Triple(false, null, "Unknown notifications action: $action")
        }

    // ------------------------------------------------------------- helpers ---

    /**
     * null if the exam is open for submission right now, otherwise the reason it
     * is closed. Dates are stored as "yyyy-MM-dd HH:mm:ss", so a plain string
     * comparison is also a correct chronological comparison.
     */
    private fun examWindowError(examId: String): String? {
        readableDatabase.rawQuery(
            "SELECT exam_date, deadline FROM exams WHERE id = ?", arrayOf(examId)
        ).use { c ->
            if (!c.moveToFirst()) return null
            val examDate = c.getString(0) ?: ""
            val deadline = c.getString(1) ?: ""
            val now = sqlNow()
            if (examDate.isNotBlank() && now < examDate) return "The exam has not started yet"
            if (deadline.isNotBlank() && now > deadline) return "The submission deadline has passed"
        }
        return null
    }

    private fun sqlNow(): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())

    private fun query(sql: String, args: Array<String>): Triple<Boolean, Any?, String> =
        readableDatabase.rawQuery(sql, args).use { c -> Triple(true, cursorToJson(c), "") }

    private fun cursorToJson(c: Cursor): JSONArray {
        val arr = JSONArray()
        while (c.moveToNext()) {
            val o = JSONObject()
            for (i in 0 until c.columnCount) {
                val name = c.getColumnName(i)
                when (c.getType(i)) {
                    Cursor.FIELD_TYPE_NULL -> o.put(name, JSONObject.NULL)
                    Cursor.FIELD_TYPE_INTEGER -> o.put(name, c.getLong(i))
                    Cursor.FIELD_TYPE_FLOAT -> o.put(name, c.getDouble(i))
                    else -> o.put(name, c.getString(i))
                }
            }
            arr.put(o)
        }
        return arr
    }

    private fun parseQuery(query: String): Map<String, String> =
        query.split('&').filter { it.contains('=') }
            .associate { it.substringBefore('=') to it.substringAfter('=') }
}
