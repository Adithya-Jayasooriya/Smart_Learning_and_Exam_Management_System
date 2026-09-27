package com.example.mycamerafinal

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

/**
 * BASE CLASS FOR EVERY INNER SCREEN — shows a small back arrow (←) at the
 * left of the action bar, like every professional Android app.
 *
 * How it works:
 *  - setDisplayHomeAsUpEnabled(true) asks the action bar to draw the arrow.
 *  - When the arrow is tapped, Android calls onSupportNavigateUp();
 *    we simply finish() this screen, which returns to the previous one.
 *
 * All inner pages (lists, forms, scan screen, …) extend this class instead
 * of AppCompatActivity. The three dashboards and the login screen do NOT
 * extend it, because they are top-level screens with nothing to go back to.
 */
abstract class BackArrowActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)   // show the ← arrow
    }

    /** Called when the ← arrow is tapped: close this screen (= go back). */
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}