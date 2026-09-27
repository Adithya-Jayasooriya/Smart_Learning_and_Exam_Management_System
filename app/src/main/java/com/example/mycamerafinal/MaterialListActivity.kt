package com.example.mycamerafinal

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.example.mycamerafinal.data.Roles
import com.example.mycamerafinal.data.Fire
import com.example.mycamerafinal.data.FsMaterial
import com.example.mycamerafinal.data.FileUtil
import com.example.mycamerafinal.data.SessionManager

/**
 * MATERIAL LIST — shows the learning materials of ONE subject.
 * (The subject id/name arrive in the Intent from SubjectListActivity.)
 *
 *  - Tap a material      : opens it (PDF viewer, browser for video links, …)
 *  - Long-press (lecturer): asks to delete the material
 */
class MaterialListActivity : BackArrowActivity() {

    private lateinit var session: SessionManager
    private lateinit var listView: ListView
    private var subjectId = ""
    private var materials: List<FsMaterial> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list)
        session = SessionManager(this)
        listView = findViewById(R.id.lvItems)

        // Which subject's materials to show — passed from the previous screen.
        subjectId = intent.getStringExtra("subjectId") ?: ""
        findViewById<TextView>(R.id.tvTitle).text =
            intent.getStringExtra("subjectName") ?: "Learning Materials"

        // Tap = open the file (FileUtil decides how: local file or web link).
        listView.setOnItemClickListener { _, _, pos, _ -> FileUtil.open(this, materials[pos].fileUrl) }

        // Long-press = delete, but only lecturers are allowed to.
        listView.setOnItemLongClickListener { _, _, pos, _ ->
            if (session.role == Roles.LECTURER) confirmDelete(materials[pos])
            true
        }
    }

    // Refresh the list every time the screen appears.
    override fun onResume() {
        super.onResume()
        Fire.getMaterialsBySubject(subjectId) { list ->
            materials = list
            findViewById<TextView>(R.id.tvEmpty).visibility =
                if (list.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
            listView.adapter = ArrayAdapter(this, R.layout.item_row, list)
        }
    }

    /** Ask before deleting — prevents accidental long-press deletions. */
    private fun confirmDelete(material: FsMaterial) {
        AlertDialog.Builder(this)
            .setTitle("Delete material")
            .setMessage(material.title)
            .setPositiveButton("Delete") { _, _ -> Fire.deleteMaterial(material.id) { onResume() } }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
