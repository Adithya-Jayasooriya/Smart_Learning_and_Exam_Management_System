package com.example.mycamerafinal

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.example.mycamerafinal.data.Fire
import com.example.mycamerafinal.data.FileUtil
import com.example.mycamerafinal.data.FsSubject
import com.example.mycamerafinal.data.SessionManager
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * CREATE EXAM (lecturer) — schedule a new exam.
 *
 * The lecturer:
 *  1. picks the SUBJECT from a dropdown (Spinner),
 *  2. types the exam TITLE,
 *  3. picks the EXAM DATE & TIME and the SUBMISSION DEADLINE
 *     using native date + time picker dialogs,
 *  4. optionally attaches the EXAM PAPER (a PDF or image),
 *  5. taps Create Exam — the exam is saved and every user gets a
 *     broadcast notification about it.
 *
 * Validation: title and both dates are required, and the deadline
 * must be AFTER the exam date.
 */
class CreateExamActivity : BackArrowActivity() {

    private lateinit var session: SessionManager
    private var subjects: List<FsSubject> = emptyList()  // dropdown content
    private var paperLocalPath: String = ""              // copied paper file (if attached)
    private var startAt: Long = 0                        // exam date+time in milliseconds
    private var deadline: Long = 0                       // deadline in milliseconds

    // Opens the system file picker; the chosen paper is copied into app storage.
    private val pickPaper =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            if (uri != null) {
                val path = FileUtil.importToAppStorage(this, uri, "tmp", "paper")
                paperLocalPath = path ?: ""
                findViewById<TextView>(R.id.tvPaper).text =
                    if (path != null) "Paper attached ✔" else "Could not read that file"
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_exam)
        session = SessionManager(this)

        val spSubject = findViewById<Spinner>(R.id.spSubject)
        val etTitle = findViewById<EditText>(R.id.etTitle)
        val etDate = findViewById<EditText>(R.id.etDate)
        val etDeadline = findViewById<EditText>(R.id.etDeadline)
        val btnSave = findViewById<Button>(R.id.btnSave)

        // Fill the subject dropdown from the database.
        Fire.getAllSubjects { list ->
            subjects = list
            spSubject.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, list)
        }

        // Turn the two date fields into "tap to pick date & time" fields.
        setupDateTimeField(etDate, { startAt }) { startAt = it; etDate.setText(format(it)) }
        setupDateTimeField(etDeadline, { deadline }) { deadline = it; etDeadline.setText(format(it)) }

        findViewById<Button>(R.id.btnPickPaper).setOnClickListener {
            pickPaper.launch(arrayOf("application/pdf", "image/*"))   // allowed file types
        }

        btnSave.setOnClickListener {
            // ---- validation ----
            if (subjects.isEmpty()) { toast("No subjects available — add one first"); return@setOnClickListener }
            val subject = subjects[spSubject.selectedItemPosition]
            val title = etTitle.text.toString().trim()
            if (title.isEmpty() || startAt <= 0) { toast("Enter title and exam date"); return@setOnClickListener }
            if (deadline <= 0) { toast("Set a submission deadline"); return@setOnClickListener }
            if (deadline <= startAt) { toast("Deadline must be after the exam date"); return@setOnClickListener }

            // ---- save ----
            btnSave.isEnabled = false
            if (paperLocalPath.isNotEmpty()) {
                // A paper was attached: upload it first, then save the exam with its URL.
                toast("Uploading paper…")
                Fire.uploadFile(paperLocalPath, "examPapers", session.uid) { url ->
                    saveExam(subject.id, title, url ?: "", btnSave)
                }
            } else {
                saveExam(subject.id, title, "", btnSave)   // no paper attached
            }
        }
    }

    /** INSERT the exam into the database, then broadcast a notification to everyone. */
    private fun saveExam(subjectId: String, title: String, paperUrl: String, btn: Button) {
        Fire.addExam(subjectId, title, startAt, deadline, paperUrl, session.uid) { ok ->
            if (ok) {
                // "ALL" = broadcast notification (user_id 0 in the database).
                Fire.addNotification("ALL", "New exam",
                    "'$title' — submit answer sheets by ${format(deadline)}")
                toast("Exam created")
                finish()
            } else {
                toast("Failed to create exam")
                btn.isEnabled = true
            }
        }
    }

    /**
     * Makes an EditText open a DatePickerDialog, then a TimePickerDialog.
     * PickerDialogTheme (res/values/themes.xml) gives the dialogs visible
     * blue OK/Cancel buttons. The picker starts from the previously picked
     * value so re-editing is easy.
     */
    private fun setupDateTimeField(field: EditText, current: () -> Long, onPicked: (Long) -> Unit) {
        field.isFocusable = false    // don't show the keyboard —
        field.isClickable = true     // — a tap opens the picker instead
        field.setOnClickListener {
            val base = Calendar.getInstance().apply { if (current() > 0) timeInMillis = current() }
            DatePickerDialog(this, R.style.PickerDialogTheme, { _, y, m, d ->
                // Date chosen — now ask for the time of day.
                TimePickerDialog(this, R.style.PickerDialogTheme, { _, h, min ->
                    val c = Calendar.getInstance().apply {
                        set(y, m, d, h, min, 0)          // combine date + time
                        set(Calendar.MILLISECOND, 0)
                    }
                    onPicked(c.timeInMillis)             // give the result back as millis
                }, base.get(Calendar.HOUR_OF_DAY), base.get(Calendar.MINUTE), true).show()
            }, base.get(Calendar.YEAR), base.get(Calendar.MONTH), base.get(Calendar.DAY_OF_MONTH)).show()
        }
    }

    /** Millis → readable text like "2026-07-20 09:00" for the form fields. */
    private fun format(millis: Long) =
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(millis)

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
