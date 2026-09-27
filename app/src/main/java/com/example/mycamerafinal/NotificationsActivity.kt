package com.example.mycamerafinal

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import com.example.mycamerafinal.data.Roles
import com.example.mycamerafinal.data.Fire
import com.example.mycamerafinal.data.SessionManager

/**
 * NOTIFICATIONS — two kinds of items are shown together:
 *  1. Stored notifications from the database (results published,
 *     "new material added", "new exam" broadcasts, …).
 *  2. LIVE deadline reminders (students only): for every exam the student
 *     has NOT submitted yet, a reminder line is generated on the fly
 *     ("Due today", "OVERDUE", …) — these are calculated, not stored.
 */
class NotificationsActivity : BackArrowActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list)
        val session = SessionManager(this)
        findViewById<TextView>(R.id.tvTitle).text = "Notifications"

        // First get the stored notifications for this user (+ broadcasts).
        Fire.getNotificationsForUser(session.uid) { stored ->
            if (session.role == Roles.STUDENT) {
                // Students also get live deadline reminders:
                Fire.getSubmittedExamIds(session.uid) { submitted ->
                    Fire.getAllExams { exams ->
                        val reminders = exams
                            // Only exams with a deadline that I have NOT submitted.
                            .filter { it.deadline > 0 && it.id !in submitted }
                            // reminder() returns text like "⏰ Due tomorrow" or null.
                            .mapNotNull { it.reminder() }
                        show(reminders + stored)   // reminders on top, then stored ones
                    }
                }
            } else {
                show(stored)   // lecturers/admins: stored notifications only
            }
        }
    }

    /** Put the final list on screen (or show the "empty" message). */
    private fun show(items: List<String>) {
        findViewById<TextView>(R.id.tvEmpty).visibility =
            if (items.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
        findViewById<ListView>(R.id.lvItems).adapter =
            ArrayAdapter(this, R.layout.item_row, items)
    }
}
