package com.example.mycamerafinal

import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.example.mycamerafinal.data.Api

/**
 * ADD LECTURER (admin only) — creates a LECTURER account.
 * Students register themselves, but lecturer accounts can only be created
 * here, by the administrator. The account is saved with role = "lecturer",
 * so after login this person is routed to the Lecturer Dashboard.
 */
class AddLecturerActivity : BackArrowActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_lecturer)

        val etName = findViewById<EditText>(R.id.etName)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnSave = findViewById<Button>(R.id.btnSave)

        btnSave.setOnClickListener {
            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val pass = etPassword.text.toString()
            when {
                // Validate before saving.
                name.isEmpty() || email.isEmpty() || pass.isEmpty() -> toast("Fill in all fields")
                !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> toast("Enter a valid email")
                pass.length < 4 -> toast("Password must be at least 4 characters")
                else -> {
                    btnSave.isEnabled = false
                    // users.php?action=add — INSERT a new user with role "lecturer".
                    Api.post("users.php?action=add", mapOf(
                        "name" to name, "email" to email, "password" to pass, "role" to "lecturer"
                    )) { ok, _, msg ->
                        if (ok) {
                            toast("Lecturer account created")
                            finish()
                        } else {
                            btnSave.isEnabled = true
                            toast(msg.ifBlank { "Could not create account" })   // e.g. duplicate email
                        }
                    }
                }
            }
        }
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
