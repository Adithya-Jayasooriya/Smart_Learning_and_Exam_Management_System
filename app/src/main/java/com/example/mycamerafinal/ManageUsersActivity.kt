package com.example.mycamerafinal

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.example.mycamerafinal.data.Roles
import com.example.mycamerafinal.data.Fire
import com.example.mycamerafinal.data.FsUser

/**
 * MANAGE USERS (admin only) — lists either all STUDENTS or all LECTURERS,
 * depending on which dashboard button was tapped (ROLE_KEY = 0 or 1).
 *
 * Tapping a user shows Activate / Deactivate. A deactivated user still
 * exists in the database but can no longer log in (checked at login).
 */
class ManageUsersActivity : BackArrowActivity() {

    private lateinit var listView: ListView
    private lateinit var roleLower: String            // "student" or "lecturer"
    private var users: List<FsUser> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list)
        listView = findViewById(R.id.lvItems)

        // 0 = students, 1 = lecturers (sent by AdminDashboardActivity).
        val roleKey = intent.getIntExtra(AdminDashboardActivity.ROLE_KEY, 0)
        roleLower = AdminDashboardActivity.ROLES[roleKey]
        findViewById<TextView>(R.id.tvTitle).text =
            if (roleLower == Roles.STUDENT) "Students" else "Lecturers"

        listView.setOnItemClickListener { _, _, pos, _ -> showDialog(users[pos]) }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    /** Load all users of this role and show "name / email / Active|Inactive" rows. */
    private fun refresh() {
        Fire.getUsersByRole(roleLower.uppercase()) { list ->
            users = list
            val rows = list.map { it.toString() }
            findViewById<TextView>(R.id.tvEmpty).visibility =
                if (list.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
            listView.adapter = ArrayAdapter(this, R.layout.item_row, rows)
        }
    }

    /** Show the opposite action of the user's current state, then flip it. */
    private fun showDialog(user: FsUser) {
        val action = if (user.active) "Deactivate" else "Activate"
        AlertDialog.Builder(this)
            .setTitle(user.fullName)
            .setMessage(user.email)
            .setPositiveButton(action) { _, _ ->
                // !user.active flips true -> false / false -> true in the database.
                Fire.setUserActive(user.id, !user.active) { refresh() }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
