package com.example.mycamerafinal

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import com.example.mycamerafinal.data.Fire
import com.example.mycamerafinal.data.SessionManager

/**
 * MY RESULTS (student) — shows only this student's GRADED submissions.
 * Each row shows the exam title, marks out of 100 and the letter grade
 * (A/B/C/S/F, calculated in Fire.gradeFor from the percentage).
 */
class ResultsActivity : BackArrowActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list)
        val session = SessionManager(this)

        findViewById<TextView>(R.id.tvTitle).text = "My Results"
        val empty = findViewById<TextView>(R.id.tvEmpty)
        empty.text = "No results published yet."

        // Ask the database for this student's graded submissions only.
        // session.uid = the id of the student who is logged in right now.
        Fire.getResultsForStudent(session.uid) { results ->
            val rows = results.map { "${it.examTitle}\nMarks: ${it.marks}/${it.totalMarks}   (${it.grade})" }
            empty.visibility = if (rows.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
            findViewById<ListView>(R.id.lvItems).adapter = ArrayAdapter(this, R.layout.item_row, rows)
        }
    }
}
