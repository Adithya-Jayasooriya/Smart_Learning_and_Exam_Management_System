package com.example.mycamerafinal

import android.os.Bundle
import android.text.InputType
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.mycamerafinal.data.Fire
import com.example.mycamerafinal.data.FsExam
import com.example.mycamerafinal.data.FsSubmission
import com.example.mycamerafinal.data.FileUtil

/**
 * SUBMISSIONS (lecturer) — every student's answer sheet for ONE exam.
 *
 * For each submission the lecturer can:
 *  - view the scanned answer sheet PDF,
 *  - download it to the phone,
 *  - enter/update the marks (0–100).
 * Saving marks sets the status to GRADED and instantly publishes the
 * result + a notification to that student.
 */
class SubmissionsActivity : BackArrowActivity() {

    private lateinit var listView: ListView
    private var examId = ""
    private var exam: FsExam? = null                      // loaded for its total marks
    private var submissions: List<FsSubmission> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list)
        listView = findViewById(R.id.lvItems)
        examId = intent.getStringExtra("examId") ?: ""    // which exam to show

        findViewById<TextView>(R.id.tvTitle).text =
            "Submissions: ${intent.getStringExtra("examTitle") ?: ""}"

        // Load the exam itself too — the grade dialog needs its total marks.
        Fire.getExam(examId) { exam = it }
        listView.setOnItemClickListener { _, _, pos, _ -> showSubmission(submissions[pos]) }
    }

    // Reload the list whenever the screen appears (e.g. after grading).
    override fun onResume() {
        super.onResume()
        Fire.getSubmissionsByExam(examId) { list ->
            submissions = list
            // Each row: student name + submission status + marks (or "Not graded").
            val rows = list.map { "${it.studentName}\nStatus: ${it.status}   •   Marks: ${it.marksLabel}" }
            findViewById<TextView>(R.id.tvEmpty).visibility =
                if (list.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
            listView.adapter = ArrayAdapter(this, R.layout.item_row, rows)
        }
    }

    /** Tapped one student's submission: choose view / download / grade. */
    private fun showSubmission(s: FsSubmission) {
        AlertDialog.Builder(this)
            .setTitle(s.studentName)
            .setItems(
                arrayOf("📄  View answer sheet", "⬇  Download answer sheet", "✅  Enter / update marks")
            ) { _, which ->
                when (which) {
                    0 -> FileUtil.open(this, s.fileUrl)          // open the PDF
                    1 -> FileUtil.downloadToPublic(this, s.fileUrl, "answer_${s.studentName}_${s.examTitle}")
                    else -> gradeDialog(s)                        // enter marks
                }
            }
            .show()
    }

    /** Popup with a number field for the marks; Save = publish the result. */
    private fun gradeDialog(s: FsSubmission) {
        val current = exam
        if (current == null) {
            // The exam details have not arrived yet (very fast taps) — just wait.
            Toast.makeText(this, "Still loading exam…", Toast.LENGTH_SHORT).show()
            return
        }
        // Build the input field in code: numbers only, hint shows the maximum.
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            hint = "Marks out of ${current.totalMarks}"
            if (s.marks != null) setText(s.marks.toString())   // pre-fill when re-grading
        }
        AlertDialog.Builder(this)
            .setTitle("Grade ${s.studentName}")
            .setView(input)
            .setPositiveButton("Save & publish") { _, _ ->
                val marks = input.text.toString().toIntOrNull()
                // Validation: must be a number between 0 and the exam's total.
                if (marks == null || marks < 0 || marks > current.totalMarks) {
                    Toast.makeText(this, "Enter 0 - ${current.totalMarks}", Toast.LENGTH_SHORT).show()
                } else {
                    // UPDATE the submission (status → graded) and notify the student.
                    Fire.gradeSubmission(s, current, marks) { ok ->
                        Toast.makeText(
                            this, if (ok) "Saved & published" else "Failed", Toast.LENGTH_SHORT
                        ).show()
                        onResume()   // refresh the list to show the new marks
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
