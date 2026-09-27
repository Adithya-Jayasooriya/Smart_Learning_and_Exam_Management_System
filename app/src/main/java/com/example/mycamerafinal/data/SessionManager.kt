package com.example.mycamerafinal.data

import android.content.Context

/**
 * SESSION MANAGER — remembers WHO is logged in, using SharedPreferences
 * (a small key-value file Android keeps for the app, private to this app).
 *
 * Because the session survives closing the app, the user goes straight to
 * their dashboard next time instead of logging in again. Logout simply
 * clears this file.
 */
class SessionManager(context: Context) {

    // "session" is the file name; MODE_PRIVATE = only this app can read it.
    private val prefs = context.getSharedPreferences("session", Context.MODE_PRIVATE)

    /** Called after a successful login — store the user's basic details. */
    fun save(uid: String, name: String, email: String, role: String) {
        prefs.edit()
            .putBoolean("loggedIn", true)
            .putString("uid", uid)               // database id of the user
            .putString("name", name)
            .putString("email", email)
            .putString("role", role.lowercase()) // "student" / "lecturer" / "admin"
            .apply()                             // apply() saves in the background
    }

    // Read the stored values anywhere in the app (dashboards, submissions, …).
    val isLoggedIn: Boolean get() = prefs.getBoolean("loggedIn", false)
    val uid: String get() = prefs.getString("uid", "") ?: ""
    val name: String get() = prefs.getString("name", "") ?: ""
    val email: String get() = prefs.getString("email", "") ?: ""
    val role: String get() = prefs.getString("role", "") ?: ""

    /** Logout — wipe everything so the app returns to the login screen. */
    fun logout() = prefs.edit().clear().apply()
}
