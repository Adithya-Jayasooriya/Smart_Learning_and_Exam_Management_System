package com.example.mycamerafinal

import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.example.mycamerafinal.data.Fire
import com.example.mycamerafinal.data.FileUtil
import com.example.mycamerafinal.data.FsSubject
import com.example.mycamerafinal.data.SessionManager

/**
 * UPLOAD LEARNING MATERIAL (lecturer) — publish a file or link for students.
 *
 * Material types: PDF / PPT / NOTE (a file is picked and uploaded)
 *                 VIDEO           (only a URL is typed, e.g. a YouTube link)
 * After saving, a broadcast notification tells everyone about the new material.
 */
class UploadMaterialActivity : BackArrowActivity() {

    private lateinit var session: SessionManager
    private var subjects: List<FsSubject> = emptyList()
    private var pickedPath: String? = null               // the chosen file (copied locally)

    private val types = arrayOf("PDF", "PPT", "VIDEO", "NOTE")   // dropdown options

    // System file picker; the chosen file is copied into the app's own storage
    // so we keep access to it even after the picker permission expires.
    private val picker =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            if (uri != null) {
                pickedPath = FileUtil.importToAppStorage(this, uri, "tmp", "material")
                findViewById<TextView>(R.id.tvPicked).text =
                    if (pickedPath != null) "File attached ✔" else "Could not read that file"
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_upload_material)
        session = SessionManager(this)

        val spSubject = findViewById<Spinner>(R.id.spSubject)
        val spType = findViewById<Spinner>(R.id.spType)
        val etTitle = findViewById<EditText>(R.id.etTitle)
        val etVideoUrl = findViewById<EditText>(R.id.etVideoUrl)
        val btnSave = findViewById<Button>(R.id.btnSave)

        // Fill both dropdowns: material types (fixed) and subjects (from the DB).
        spType.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, types)
        Fire.getAllSubjects { list ->
            subjects = list
            spSubject.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, list)
        }

        findViewById<Button>(R.id.btnPickFile).setOnClickListener { picker.launch(arrayOf("*/*")) }

        btnSave.setOnClickListener {
            // ---- validation ----
            if (subjects.isEmpty()) { toast("No subjects available yet"); return@setOnClickListener }
            val subject = subjects[spSubject.selectedItemPosition]
            val title = etTitle.text.toString().trim()
            val type = types[spType.selectedItemPosition]
            if (title.isEmpty()) { toast("Enter a title"); return@setOnClickListener }

            btnSave.isEnabled = false
            if (type == "VIDEO") {
                // VIDEO = no file upload, just store the link the lecturer typed.
                val url = etVideoUrl.text.toString().trim()
                if (url.isEmpty()) { toast("Enter a video URL"); btnSave.isEnabled = true; return@setOnClickListener }
                save(subject.id, title, type, url, btnSave)
            } else {
                // PDF/PPT/NOTE = upload the picked file first, then save its URL.
                val path = pickedPath
                if (path == null) { toast("Pick a file first"); btnSave.isEnabled = true; return@setOnClickListener }
                toast("Uploading…")
                Fire.uploadFile(path, "materials", session.uid) { fileUrl ->
                    if (fileUrl == null) { toast("Upload failed — could not store the file"); btnSave.isEnabled = true }
                    else save(subject.id, title, type, fileUrl, btnSave)
                }
            }
        }
    }

    /** INSERT the material row, then notify everyone about it. */
    private fun save(subjectId: String, title: String, type: String, url: String, btn: Button) {
        Fire.addMaterial(subjectId, title, type, url, session.uid) { ok ->
            if (ok) {
                Fire.addNotification("ALL", "New material", "'$title' was added")
                toast("Material uploaded")
                finish()
            } else {
                toast("Failed to save material")
                btn.isEnabled = true
            }
        }
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
