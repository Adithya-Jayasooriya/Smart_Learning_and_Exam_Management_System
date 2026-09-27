package com.example.mycamerafinal.data

/**
 * The three user roles of the system, written once here so every screen
 * compares against the SAME lowercase strings (typos become impossible).
 * The role decides which dashboard opens after login and what a user may do.
 */
object Roles {
    const val STUDENT = "student"
    const val LECTURER = "lecturer"
    const val ADMIN = "admin"
}
