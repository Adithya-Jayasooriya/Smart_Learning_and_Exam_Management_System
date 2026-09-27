package com.example.mycamerafinal

/**
 * STUDENT DASHBOARD — the home screen a student sees after login.
 * The layout and button-building logic live in BaseDashboardActivity;
 * this class only lists WHICH buttons a student gets and where they go.
 */
class StudentDashboardActivity : BaseDashboardActivity() {

    override val headerTitle = "Student Dashboard"

    override fun buildMenu() {
        // Each line = one dashboard button: label + the screen it opens.
        menu("📚   View Subjects") { open(SubjectListActivity::class.java) }
        // MODE tells the next screen how to behave (see companion object below).
        menu("📁   Learning Materials") { open(SubjectListActivity::class.java, MODE to MODE_MATERIALS) }
        menu("📝   Upcoming Exams") { open(ExamListActivity::class.java) }
        menu("📷   Scan & Upload Answer Sheet") { open(ExamListActivity::class.java, MODE to MODE_SUBMIT) }
        menu("🏆   My Results") { open(ResultsActivity::class.java) }
        menu("🔔   Notifications") { open(NotificationsActivity::class.java) }
        menu("🔑   Change Password") { open(ChangePasswordActivity::class.java) }
    }

    companion object {
        // "mode" is passed in the Intent so one list screen can serve many purposes.
        const val MODE = "mode"
        const val MODE_MATERIALS = 1   // subject list opens materials on tap
        const val MODE_SUBMIT = 2      // exam list offers scan/upload on tap
    }
}
