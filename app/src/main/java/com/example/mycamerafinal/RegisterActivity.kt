package com.example.mycamerafinal

import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.example.mycamerafinal.data.AuthManager

/**
 * REGISTER SCREEN — lets a NEW STUDENT create their own account.
 * (Lecturer accounts are created by the admin, not here.)
 *
 * What happens here:
 *  1. The student fills in name, email, password and confirm-password.
 *  2. We validate the input (all fields filled, valid email format,
 *     password at least 6 characters, both passwords match).
 *  3. AuthManager saves the new account in the database with role "student".
 *  4. On success we return to the login screen so they can sign in.
 */
class RegisterActivity : BackArrowActivity() {

    private lateinit var auth: AuthManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)   // res/layout/activity_register.xml
        auth = AuthManager(this)

        // Connect the XML input fields to Kotlin variables.
        val etName = findViewById<EditText>(R.id.etName)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val etConfirm = findViewById<EditText>(R.id.etConfirmPassword)
        val btnRegister = findViewById<Button>(R.id.btnRegister)

        btnRegister.setOnClickListener {
            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString()
            val confirm = etConfirm.text.toString()

            // Validation: check each rule one by one; stop at the first problem.
            when {
                name.isEmpty() || email.isEmpty() || password.isEmpty() ->
                    toast("Please fill in all fields")
                // Patterns.EMAIL_ADDRESS is Android's built-in email format checker.
                !Patterns.EMAIL_ADDRESS.matcher(email).matches() ->
                    toast("Enter a valid email address")
                password.length < 6 ->
                    toast("Password must be at least 6 characters")
                password != confirm ->
                    toast("Passwords do not match")
                else -> {
                    // All rules passed — create the account.
                    btnRegister.isEnabled = false          // prevent double taps
                    btnRegister.text = "Creating account…"
                    auth.signup(name, email, password) { success, message ->
                        if (success) {
                            toast("Account created. Please log in.")
                            finish()                        // go back to the login screen
                        } else {
                            // e.g. "This email is already registered"
                            btnRegister.isEnabled = true
                            btnRegister.text = "Register"
                            toast(message)
                        }
                    }
                }
            }
        }

        // "Already have an account?" — just close this screen.
        findViewById<Button>(R.id.btnGoToLogin).setOnClickListener { finish() }
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
