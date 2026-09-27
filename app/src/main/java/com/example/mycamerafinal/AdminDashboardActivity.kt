package com.example.mycamerafinal

import com.example.mycamerafinal.data.Roles

/**
 * ADMIN DASHBOARD — the home screen the administrator sees after login.
 * The admin manages people (students / lecturers), subjects and reports.
 */
class AdminDashboardActivity : BaseDashboardActivity() {

    override val headerTitle = "Admin Dashboard"

    override fun buildMenu() {
        // ROLE_KEY chooses which user list ManageUsersActivity shows (0/1 below).
        menu("🎓   Manage Students") { open(ManageUsersActivity::class.java, ROLE_KEY to 0) }
        menu("👨‍🏫   Manage Lecturers") { open(ManageUsersActivity::class.java, ROLE_KEY to 1) }
        // MODE_MANAGE turns the subject list into edit/delete mode.
        menu("📚   Manage Subjects") { open(SubjectListActivity::class.java, StudentDashboardActivity.MODE to MODE_MANAGE) }
        menu("➕   Add Subject") { open(AddSubjectActivity::class.java) }
        menu("➕   Add Lecturer") { open(AddLecturerActivity::class.java) }
        menu("📊   System Reports") { open(ReportsActivity::class.java) }
        menu("🔑   Change Password") { open(ChangePasswordActivity::class.java) }
    }

    companion object {
        const val ROLE_KEY = "roleKey"          // 0 = students, 1 = lecturers
        const val MODE_MANAGE = 4               // subject list allows edit/delete
        val ROLES = arrayOf(Roles.STUDENT, Roles.LECTURER)   // index 0 / 1 above
    }
}
