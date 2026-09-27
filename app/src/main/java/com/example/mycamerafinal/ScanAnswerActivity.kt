package com.example.mycamerafinal

import android.app.Application
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.example.mycamerafinal.data.Fire
import com.example.mycamerafinal.data.FileUtil
import com.example.mycamerafinal.data.SessionManager
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult

/**
 * SCAN / UPLOAD ANSWER SHEET (student) — submit the answer for ONE exam.
 *
 * Two ways to submit:
 *  1. SCAN — opens Google's ML Kit document scanner (the camera screen is
 *     provided by Google Play services, so the app itself needs NO camera
 *     permission). It auto-detects page edges, crops, lets the student add
 *     up to 10 pages, and returns everything as ONE PDF file.
 *  2. UPLOAD — pick an existing PDF/image with the system file picker.
 *
 * Either way, the file is copied into app storage, uploaded through the
 * data layer, and a "submissions" row is saved for this exam + student.
 */
class ScanAnswerActivity : BackArrowActivity() {

    private lateinit var session: SessionManager
    private var examId = ""
    private var examTitle = ""

    // Receives the result when the Google scanner closes.
    private val scannerLauncher =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            val scan = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
            val pdfUri = scan?.pdf?.uri
            if(pdfUri==null)
            {
                Toast.makeText(this,"This is null", Toast.LENGTH_SHORT).show()
            }
            // one PDF containing all scanned pages
            if (result.resultCode == RESULT_OK && pdfUri != null) {
                submitExistingFile(pdfUri)         // treat it like any picked file
            }
        }

    // Receives the file the student picked with "Upload a PDF / image".
    private val filePicker =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) submitExistingFile(uri)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan)
        session = SessionManager(this)
        // Which exam this submission belongs to (sent by ExamListActivity).
        examId = intent.getStringExtra("examId") ?: ""
        examTitle = intent.getStringExtra("examTitle") ?: "exam"

        findViewById<TextView>(R.id.tvExam).text = examTitle
        findViewById<Button>(R.id.btnScan).setOnClickListener { startGoogleScan() }
        findViewById<Button>(R.id.btnUpload).setOnClickListener { filePicker.launch(arrayOf("*/*")) }
    }

    /** Configure and launch the Google ML Kit document scanner. */
    private fun startGoogleScan() {
        val options = GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(true)                                  // also allow gallery photos
            .setPageLimit(10)                                               // up to 10 pages per sheet
            .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_PDF)  // give us one PDF back
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)    // full UI: crop, filters…
            .build()

        GmsDocumentScanning.getClient(options)
            .getStartScanIntent(this)
            .addOnSuccessListener { intentSender ->
                // Play services gave us the scanner screen — launch it.
                scannerLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
            }
            .addOnFailureListener {
                // Phone without Google Play services — fall back to file upload.
                toast("Google scanner is not available on this phone — use \"Upload a PDF / image\"")
            }
    }

    /** Copy the file into app storage, then upload + record it. */
    private fun submitExistingFile(uri: Uri) {
        val path = FileUtil.importToAppStorage(this, uri, "answers", "answer")
        if (path == null) { toast("Could not read the file"); return }
        uploadAndRecord(path)
    }

    /**
     * Step 1: upload the PDF (to app storage in offline mode, or to
     *         upload.php on the cPanel server when online).
     * Step 2: save a submissions row linking exam + student + file URL.
     */
    private fun uploadAndRecord(localPath: String) {
        toast("Uploading…")
        Fire.uploadFile(localPath, "answerSheets", session.uid) { url ->
            if (url == null) {
                toast("Upload failed — could not store the answer sheet")
                return@uploadFile
            }
            Fire.addSubmission(examId, session.uid, session.name, examTitle, url) { ok ->
                if (ok) {
                    Toast.makeText(this, "Answer sheet submitted ✔", Toast.LENGTH_LONG).show()
                    finish()   // back to the exam list
                } else {
                    // e.g. the database rejected it because the deadline passed.
                    toast("Failed to record submission")
                }
            }
        }
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
