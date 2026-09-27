package com.example.mycamerafinal

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import com.example.mycamerafinal.data.Fire

/**
 * SYSTEM REPORTS (admin) — a live summary of how much data the system holds:
 * number of students, lecturers, subjects, materials and exams.
 *
 * The five requests are NESTED (one inside the other) because each call is
 * asynchronous — we can only build the final list after ALL counts arrived.
 */
class ReportsActivity : BackArrowActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list)
        findViewById<TextView>(R.id.tvTitle).text = "System Reports"

        // Count users, subjects, materials and exams one after another.
        Fire.getUsersByRole(Fire.ROLE_STUDENT) { students ->
            Fire.getUsersByRole(Fire.ROLE_LECTURER) { lecturers ->
                Fire.getAllSubjects { subjects ->
                    Fire.getAllMaterials { materials ->
                        Fire.getAllExams { exams ->
                            // .size = how many rows each query returned.
                            val rows = listOf(
                                "Total Students: ${students.size}",
                                "Total Lecturers: ${lecturers.size}",
                                "Total Subjects: ${subjects.size}",
                                "Total Learning Materials: ${materials.size}",
                                "Total Exams: ${exams.size}"
                            )
                            findViewById<ListView>(R.id.lvItems).adapter =
                                ArrayAdapter(this, R.layout.item_row, rows)
                        }
                    }
                }
            }
        }
    }
}
