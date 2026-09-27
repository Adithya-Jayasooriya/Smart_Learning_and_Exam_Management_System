package com.example.mycamerafinal

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.mycamerafinal.data.Roles
import com.example.mycamerafinal.data.Fire
import com.example.mycamerafinal.data.FsSubject
import com.example.mycamerafinal.data.SessionManager

/**
 * SUBJECT LIST — one screen, two behaviours chosen by the "mode" extra:
 *  - normal mode : tapping a subject opens its learning materials
 *  - manage mode : (admin) tapping a subject offers Edit / Delete
 * A lecturer only sees their own subjects; everyone else sees all subjects.
 */
class SubjectListActivity : BackArrowActivity() {

    private lateinit var session: SessionManager
    private lateinit var listView: ListView
    private var subjects: List<FsSubject> = emptyList()   // the rows currently shown
    private var manage = false                            // true = admin edit/delete mode

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list)   // shared list layout (title + ListView)
        session = SessionManager(this)
        listView = findViewById(R.id.lvItems)

        // Read the mode passed by the dashboard button that opened this screen.
        val mode = intent.getIntExtra(StudentDashboardActivity.MODE, 0)
        manage = mode == AdminDashboardActivity.MODE_MANAGE
        findViewById<TextView>(R.id.tvTitle).text = if (manage) "Manage Subjects" else "Subjects"

        // What happens when the user taps one row of the list.
        listView.setOnItemClickListener { _, _, pos, _ ->
            val subject = subjects[pos]
            if (manage) showManageDialog(subject)   // admin: Edit / Delete
            else startActivity(                      // others: open the materials screen
                Intent(this, MaterialListActivity::class.java)
                    .putExtra("subjectId", subject.id)
                    .putExtra("subjectName", subject.toString())
            )
        }
    }

    // onResume runs every time the screen becomes visible again,
    // so the list refreshes automatically after adding/editing a subject.
    override fun onResume() {
        super.onResume()
        refresh()
    }

    /** Load the subjects from the database and show them in the ListView. */
    private fun refresh() {
        val handler = { list: List<FsSubject> ->
            subjects = list
            // Show the "empty" message only when there are no rows.
            findViewById<TextView>(R.id.tvEmpty).visibility =
                if (list.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
            // ArrayAdapter turns each FsSubject into one text row (item_row.xml).
            listView.adapter = ArrayAdapter(this, R.layout.item_row, list)
        }
        if (session.role == Roles.LECTURER && !manage) {
            Fire.getSubjectsByLecturer(session.uid, handler)   // only my subjects
        } else {
            Fire.getAllSubjects(handler)                        // all subjects
        }
    }

    /** Admin tapped a subject: ask whether to Edit or Delete it. */
    private fun showManageDialog(subject: FsSubject) {
        AlertDialog.Builder(this)
            .setTitle(subject.toString())
            .setItems(arrayOf("Edit", "Delete")) { _, which ->
                if (which == 0) editDialog(subject)
                else Fire.deleteSubject(subject.id) {          // DELETE in the database
                    Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show()
                    refresh()
                }
            }
            .show()
    }

    /** Small popup form with the current code/name; Save = UPDATE in the database. */
    private fun editDialog(subject: FsSubject) {
        val view = layoutInflater.inflate(R.layout.dialog_edit_subject, null)
        val etCode = view.findViewById<EditText>(R.id.etCode)
        val etName = view.findViewById<EditText>(R.id.etName)
        etCode.setText(subject.code)     // pre-fill with the current values
        etName.setText(subject.name)
        AlertDialog.Builder(this)
            .setTitle("Edit Subject")
            .setView(view)
            .setPositiveButton("Save") { _, _ ->
                Fire.updateSubject(subject.id, etCode.text.toString(), etName.text.toString()) { refresh() }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
