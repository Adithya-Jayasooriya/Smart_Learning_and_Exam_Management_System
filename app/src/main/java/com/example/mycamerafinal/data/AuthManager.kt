package com.example.mycamerafinal.data

import android.content.Context
import org.json.JSONObject

/**
 * AUTH MANAGER — login, sign-up and logout.
 *
 * It talks to the data layer through [Api] (which decides automatically:
 * SQLite on the phone, or auth.php on the cPanel server when configured)
 * and stores the result in [SessionManager] so the user stays signed in.
 */
class AuthManager(context: Context) {

    private val session = SessionManager(context)

    /** True when a session is already saved on the phone. */
    val isSignedIn: Boolean get() = session.isLoggedIn

    /** No server round-trip needed — the session is already persisted locally. */
    fun checkAuthStatus(onResult: (loggedIn: Boolean, role: String) -> Unit) {
        if (session.isLoggedIn) onResult(true, session.role) else onResult(false, "")
    }

    /**
     * LOGIN: send email+password to the database.
     *  - success → the database returns the user's id, name, email and role;
     *    we save them as the session and give the ROLE back to the caller
     *    (LoginActivity uses it to open the right dashboard).
     *  - failure → we give the error MESSAGE back instead
     *    ("Incorrect password", "Account deactivated", …).
     */
    fun login(email: String, password: String, onResult: (Boolean, String) -> Unit) {
        Api.post("auth.php?action=login", mapOf("email" to email, "password" to password)) { ok, data, msg ->
            if (ok && data is JSONObject) {
                val role = data.optString("role", "student").lowercase()
                session.save(
                    data.optString("id"), data.optString("name"),
                    data.optString("email"), role
                )
                onResult(true, role)
            } else {
                onResult(false, msg.ifBlank { "Login failed" })
            }
        }
    }

    /** SIGN UP: create a new STUDENT account (the database checks for duplicate emails). */
    fun signup(name: String, email: String, password: String, onResult: (Boolean, String) -> Unit) {
        Api.post("auth.php?action=register",
            mapOf("name" to name, "email" to email, "password" to password)) { ok, _, msg ->
            onResult(ok, if (ok) "" else msg.ifBlank { "Sign up failed" })
        }
    }

    /** LOGOUT: clear the saved session. */
    fun signout() = session.logout()
}
