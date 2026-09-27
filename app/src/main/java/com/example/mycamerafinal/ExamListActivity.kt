package com.example.mycamerafinal

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.mycamerafinal.data.Fire
import com.example.mycamerafinal.data.FsExam
import com.example.mycamerafinal.data.FileUtil
import com.example.mycamerafinal.data.Roles
import com.example.mycamerafinal.data.SessionManager

/**
 * EXAM LIST — one screen, three behaviours chosen by the "mode" extra:
 *  - default      (student "Upcoming Exams") : tap to open/download the exam paper
 *  - MODE_SUBMIT  (student "Scan & Upload")  : tap to scan & submit an answer sheet
 *  - MODE_GRADE   (lecturer)                 : tap to review that exam's submissions
 *
 * TIME RULES enforced here (and again inside the database layer):
 *  - a student can only see the exam paper AFTER the exam start time
 *  - a student can only submit BETWEEN the start time and the deadline
 */
class ExamListActivity : BackArrowActivity() {

    private lateinit var session: SessionManager
    private lateinit var listView: ListView
    private var exams: List<FsExam> = emptyList()
    private var mode = 0

    /** Only students are time-restricted; a lecturer/admin may preview any paper. */
    private val isStudent get() = session.role == Roles.STUDENT

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list)
        session = SessionManager(this)
        listView = findViewById(R.id.lvItems)
        mode = intent.getIntExtra(StudentDashboardActivity.MODE, 0)

        // The title tells the user what tapping a row will do in this mode.
        findViewById<TextView>(R.id.tvTitle).text = when (mode) {
            StudentDashboardActivity.MODE_SUBMIT -> "Select an exam to submit"
            LecturerDashboardActivity.MODE_GRADE -> "Select an exam to grade"
            else -> "Upcoming Exams"
        }

        listView.setOnItemClickListener { _, _, pos, _ ->
            val exam = exams[pos]
            when (mode) {
                // Student submit mode: first check if they already submitted,
                // because that changes which options we offer.
                StudentDashboardActivity.MODE_SUBMIT ->
                    Fire.hasSubmitted(exam.id, session.uid) { submitted -> showSubmitOptions(exam, submitted) }
                // Lecturer grade mode: open the submissions screen for this exam.
                LecturerDashboardActivity.MODE_GRADE ->
                    startActivity(
                        Intent(this, SubmissionsActivity::class.java)
                            .putExtra("examId", exam.id)
                            .putExtra("examTitle", exam.title)
                    )
                // Default mode: just view/download the paper.
                else -> showPaperOptions(exam)
            }
        }
    }

    // Refresh the exams whenever the screen becomes visible.
    override fun onResume() {
        super.onResume()
        val handler = { list: List<FsExam> ->
            exams = list
            findViewById<TextView>(R.id.tvEmpty).visibility =
                if (list.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
            listView.adapter = ArrayAdapter(this, R.layout.item_row, list)
        }
        // A lecturer grading sees only exams THEY created; everyone else sees all.
        if (mode == LecturerDashboardActivity.MODE_GRADE)
            Fire.getExamsByLecturer(session.uid, handler) else Fire.getAllExams(handler)
    }

    /** Default mode: offer to open/download the paper — with the time lock for students. */
    private fun showPaperOptions(exam: FsExam) {
        if (!exam.hasPaper) {
            Toast.makeText(this, "No exam paper attached to '${exam.title}'", Toast.LENGTH_SHORT).show()
            return
        }
        // TIME RULE: students cannot see the paper before the exam start time.
        if (isStudent && !exam.hasStarted()) {
            AlertDialog.Builder(this)
                .setTitle(exam.title)
                .setMessage("🔒 The exam paper will be available when the exam starts:\n${exam.examDateStr}")
                .setPositiveButton("OK", null)
                .show()
            return
        }
        AlertDialog.Builder(this)
            .setTitle("${exam.title}\n${exam.subjectName} • Exam: ${exam.examDateStr}")
            .setItems(arrayOf("📄  Open exam paper", "⬇  Download exam paper")) { _, which ->
                if (which == 0) openPaper(exam) else downloadPaper(exam)
            }
            .setNegativeButton("Close", null)
            .show()
    }

    /**
     * Submit mode: build the action list according to the exam's time window
     * and whether the student already submitted. Blocked actions still appear
     * (greyed by meaning, not colour) so the student understands WHY.
     */
    private fun showSubmitOptions(exam: FsExam, submitted: Boolean) {
        val started = exam.hasStarted()   // has the exam start time passed?
        val overdue = exam.isOverdue()    // has the submission deadline passed?
        val actions = mutableListOf<String>()

        // Exam paper: revealed only once the exam has started.
        if (exam.hasPaper) {
            if (started) {
                actions.add("📄  Open exam paper")
                actions.add("⬇  Download exam paper")
            } else {
                actions.add("🔒  Paper opens at exam time")
            }
        }

        // Answer sheet: allowed only inside the [start, deadline] window.
        when {
            !started -> actions.add("⏳  Submission opens at exam time")
            overdue && submitted -> actions.add("✔  Submitted — deadline passed")   // no late re-submit
            overdue -> actions.add("⛔  Deadline passed")                            // no late submit
            submitted -> actions.add("✔  Submitted — re-scan / re-upload")           // replace before deadline
            else -> actions.add("📷  Scan / upload answer sheet")                    // normal submit
        }

        AlertDialog.Builder(this)
            .setTitle("${exam.title}\n${exam.subjectName} • ${exam.statusLabel()} (${exam.deadlineStr})")
            .setItems(actions.toTypedArray()) { _, which ->
                when (actions[which]) {
                    "📄  Open exam paper" -> openPaper(exam)
                    "⬇  Download exam paper" -> downloadPaper(exam)
                    // Blocked options only explain the rule; they never open the scanner.
                    "🔒  Paper opens at exam time" ->
                        Toast.makeText(this, "The exam paper opens at ${exam.examDateStr}", Toast.LENGTH_LONG).show()
                    "⏳  Submission opens at exam time" ->
                        Toast.makeText(this, "You can submit after the exam starts (${exam.examDateStr})", Toast.LENGTH_LONG).show()
                    "⛔  Deadline passed", "✔  Submitted — deadline passed" ->
                        Toast.makeText(this, "The submission deadline has passed — no changes allowed", Toast.LENGTH_LONG).show()
                    // Allowed: open the scan/upload screen for this exam.
                    else -> startActivity(
                        Intent(this, ScanAnswerActivity::class.java)
                            .putExtra("examId", exam.id)
                            .putExtra("examTitle", exam.title)
                    )
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /** Open the paper in a PDF viewer (local file) or browser (URL). */
    private fun openPaper(exam: FsExam) = FileUtil.open(this, exam.paperUrl)

    /** Save the paper into the phone's public Downloads folder. */
    private fun downloadPaper(exam: FsExam) {
        val base = exam.subjectCode.ifBlank { "exam" } + "_" + exam.title
        FileUtil.downloadToPublic(this, exam.paperUrl, base)
    }
}
