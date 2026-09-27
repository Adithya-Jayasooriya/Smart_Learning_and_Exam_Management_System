package com.example.mycamerafinal

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.example.mycamerafinal.data.Roles
import com.example.mycamerafinal.data.Fire
import com.example.mycamerafinal.data.SessionManager

/**
 * ADD SUBJECT (admin or lecturer) — a small form with subject code + name.
 * If a LECTURER creates the subject it is automatically assigned to them;
 * if the ADMIN creates it, it stays unassigned (lecturer_id empty).
 */
class AddSubjectActivity : BackArrowActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_subject)
        val session = SessionManager(this)

        val etCode = findViewById<EditText>(R.id.etCode)
        val etName = findViewById<EditText>(R.id.etName)
        val btnSave = findViewById<Button>(R.id.btnSave)

        btnSave.setOnClickListener {
            val code = etCode.text.toString().trim()
            val name = etName.text.toString().trim()
            if (code.isEmpty() || name.isEmpty()) {
                toast("Enter code and name")
                return@setOnClickListener
            }
            // A lecturer adding a subject owns it; admin leaves it unassigned.
            val lecturerId = if (session.role == Roles.LECTURER) session.uid else ""
            btnSave.isEnabled = false
            // INSERT the subject row into the database.
            Fire.addSubject(code, name, lecturerId) { ok ->
                toast(if (ok) "Subject added" else "Failed to add subject")
                if (ok) finish() else btnSave.isEnabled = true
            }
        }
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
