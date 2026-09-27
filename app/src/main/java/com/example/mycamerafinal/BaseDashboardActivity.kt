package com.example.mycamerafinal

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.mycamerafinal.data.SessionManager

/**
 * BASE DASHBOARD — the shared parent class of all three dashboards
 * (StudentDashboardActivity, LecturerDashboardActivity, AdminDashboardActivity).
 *
 * Instead of writing the same screen three times, the three dashboards
 * INHERIT from this class and only provide two things:
 *  - headerTitle : the text at the top (e.g. "Student Dashboard")
 *  - buildMenu() : which buttons appear, and what each button opens
 *
 * This class then builds the screen: it shows the header, the signed-in
 * user's name and role, creates every menu button in code (no XML needed
 * per button), and always adds a Logout button at the bottom.
 */
abstract class BaseDashboardActivity : AppCompatActivity() {

    protected lateinit var session: SessionManager   // who is logged in
    private lateinit var container: LinearLayout     // the vertical list that holds the buttons

    /** Each dashboard supplies its own title, e.g. "Admin Dashboard". */
    abstract val headerTitle: String

    /** Each dashboard adds its own menu entries here by calling [menu]. */
    abstract fun buildMenu()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        session = SessionManager(this)
        setContentView(R.layout.activity_dashboard)   // shared layout for all dashboards

        // Header: the dashboard name + "user name • role" under it.
        findViewById<TextView>(R.id.tvHeader).text = headerTitle
        findViewById<TextView>(R.id.tvSub).text = "${session.name}  •  ${session.role}"
        container = findViewById(R.id.llButtons)

        buildMenu()               // the child class adds its buttons
        menu("Logout") { logout() }   // every dashboard ends with Logout
    }

    /**
     * Creates ONE big menu button in code and adds it to the screen.
     * The button style (blue background, rounded corners, white text)
     * comes from the app-wide AppButton style in res/values/themes.xml.
     */
    protected fun menu(label: String, onClick: () -> Unit) {
        // density converts "design pixels" (dp) to real screen pixels,
        // so the button is the same physical size on every phone.
        val d = resources.displayMetrics.density
        val btn = Button(this).apply {
            text = label
            textSize = 15f
            gravity = Gravity.START or Gravity.CENTER_VERTICAL   // text starts at the left
            setPadding((22 * d).toInt(), 0, (22 * d).toInt(), 0)
            minHeight = (58 * d).toInt()                          // finger-friendly height
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,              // full width
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = (12 * d).toInt() }              // gap between buttons
            setOnClickListener { onClick() }                      // what happens on tap
        }
        container.addView(btn)   // put the finished button into the vertical list
    }

    /** Opens another screen, optionally passing small values (e.g. a mode number). */
    protected fun open(target: Class<*>, vararg extras: Pair<String, Int>) {
        val intent = Intent(this, target)
        extras.forEach { intent.putExtra(it.first, it.second) }
        startActivity(intent)
    }

    /** Clears the saved session and returns to the login screen. */
    private fun logout() {
        com.example.mycamerafinal.data.AuthManager(this).signout()
        val intent = Intent(this, LoginActivity::class.java)
        // CLEAR_TASK removes all previous screens so "back" cannot re-enter the app.
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
