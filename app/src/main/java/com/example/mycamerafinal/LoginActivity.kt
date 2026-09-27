package com.example.mycamerafinal

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.mycamerafinal.data.Roles
import com.example.mycamerafinal.data.AuthManager

/**
 * LOGIN SCREEN — the first screen of the app (the launcher activity).
 *
 * What happens here:
 *  1. If the user logged in before, the saved session is still on the phone,
 *     so we skip this screen and go straight to their dashboard.
 *  2. Otherwise the user types email + password and taps Login.
 *  3. AuthManager checks the email/password against the database
 *     (SQLite on the phone, or the cPanel MySQL server when configured).
 *  4. On success we read the user's ROLE (student / lecturer / admin)
 *     and open the matching dashboard.
 */
class LoginActivity : AppCompatActivity() {

    // Handles login/register/logout and remembers the signed-in user.
    private lateinit var auth: AuthManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = AuthManager(this)

        // Step 1: if a session is still cached on the phone, skip the login screen.
        if (auth.isSignedIn) {
            auth.checkAuthStatus { loggedIn, role -> if (loggedIn) routeTo(role) }
            return
        }

        // Step 2: show the login form (res/layout/activity_login.xml).
        setContentView(R.layout.activity_login)

        // Connect the XML views to Kotlin variables by their android:id.
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)

        btnLogin.setOnClickListener {
            // Read what the user typed. trim() removes accidental spaces.
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString()

            // Simple validation before calling the database.
            if (email.isEmpty() || password.isEmpty()) {
                toast("Please enter email and password")
                return@setOnClickListener
            }

            // Disable the button so the user cannot tap Login twice.
            btnLogin.isEnabled = false
            btnLogin.text = "Signing in…"

            // Step 3: ask the database if this email/password is correct.
            // The answer comes back asynchronously in this callback.
            auth.login(email, password) { success, roleOrMessage ->
                if (success) {
                    toast("Welcome")
                    routeTo(roleOrMessage)          // roleOrMessage = the user's role
                } else {
                    // Login failed — re-enable the button and show why.
                    btnLogin.isEnabled = true
                    btnLogin.text = "Login"
                    toast(roleOrMessage)            // roleOrMessage = the error message
                }
            }
        }

        // "New student? Create an account" opens the register screen.
        findViewById<Button>(R.id.btnGoToRegister).setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    /** Step 4: open the dashboard that matches the user's role. */
    private fun routeTo(role: String) {
        val target = when (role) {
            Roles.ADMIN -> AdminDashboardActivity::class.java
            Roles.LECTURER -> LecturerDashboardActivity::class.java
            else -> StudentDashboardActivity::class.java   // default = student
        }
        startActivity(Intent(this, target))
        finish()   // remove the login screen from the back stack
    }

    /** Small helper to show a short popup message. */
    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
