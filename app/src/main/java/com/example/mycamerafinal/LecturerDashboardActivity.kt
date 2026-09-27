package com.example.mycamerafinal

/**
 * LECTURER DASHBOARD — the home screen a lecturer sees after login.
 * Only lists the lecturer's buttons; the screen itself is built by
 * BaseDashboardActivity.
 */
class LecturerDashboardActivity : BaseDashboardActivity() {

    override val headerTitle = "Lecturer Dashboard"

    override fun buildMenu() {
        menu("📚   My Subjects") { open(SubjectListActivity::class.java) }
        menu("⬆️   Upload Learning Material") { open(UploadMaterialActivity::class.java) }
        menu("📝   Create Exam") { open(CreateExamActivity::class.java) }
        // MODE_GRADE makes the exam list open the submissions screen on tap.
        menu("✅   Review Submissions & Grade") { open(ExamListActivity::class.java, StudentDashboardActivity.MODE to MODE_GRADE) }
        menu("🔔   Notifications") { open(NotificationsActivity::class.java) }
        menu("🔑   Change Password") { open(ChangePasswordActivity::class.java) }
    }

    companion object {
        const val MODE_GRADE = 3   // exam list behaves as "select an exam to grade"
    }
}
