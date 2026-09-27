package com.example.mycamerafinal

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.example.mycamerafinal.data.Api
import com.example.mycamerafinal.data.SessionManager

/**
 * CHANGE PASSWORD — any signed-in user can set a new password here.
 * The new password is sent to the data layer (auth.php?action=change_password
 * on the server, or the SQLite users table in offline mode) and replaces
 * the old one for the CURRENT user (identified by session.uid).
 */
class ChangePasswordActivity : BackArrowActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_change_password)
        val session = SessionManager(this)

        val etNew = findViewById<EditText>(R.id.etNewPassword)
        val etConfirm = findViewById<EditText>(R.id.etConfirmPassword)
        val btnSave = findViewById<Button>(R.id.btnSave)

        btnSave.setOnClickListener {
            val pass = etNew.text.toString()
            val confirm = etConfirm.text.toString()
            when {
                // Basic validation before touching the database.
                pass.length < 4 -> toast("Password must be at least 4 characters")
                pass != confirm -> toast("Passwords do not match")
                else -> {
                    btnSave.isEnabled = false   // prevent double taps
                    // UPDATE the password of the logged-in user (session.uid).
                    Api.post("auth.php?action=change_password",
                        mapOf("user_id" to session.uid, "new_password" to pass)) { ok, _, msg ->
                        if (ok) {
                            toast("Password updated")
                            finish()             // go back to the dashboard
                        } else {
                            btnSave.isEnabled = true
                            toast(msg.ifBlank { "Could not update password" })
                        }
                    }
                }
            }
        }
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
