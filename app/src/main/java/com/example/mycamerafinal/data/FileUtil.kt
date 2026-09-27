package com.example.mycamerafinal.data

import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.pdf.PdfDocument
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.InputStream

/**
 * Copies picked/scanned files into the app's own storage, opens them through a
 * FileProvider, and can download them into the phone's public Downloads folder.
 * This avoids the "permission denied / no longer accessible" problems you get when
 * storing a raw content:// URI from the document picker or scanner.
 */
object FileUtil {

    /**
     * Copy [source] into filesDir/[subdir]. Returns the absolute path of the copy,
     * or null if it could not be read.
     */
    fun importToAppStorage(context: Context, source: Uri, subdir: String, baseName: String): String? {
        return try {
            val dir = File(context.filesDir, subdir).apply { mkdirs() }
            val dest = File(dir, baseName + "_" + System.currentTimeMillis() + guessExtension(context, source))
            context.contentResolver.openInputStream(source)?.use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
            } ?: return null
            dest.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Open a stored file (absolute path) or a URL/URI string in an external viewer.
     * Returns true if a viewer was launched.
     */
    fun open(context: Context, pathOrUri: String): Boolean {
        if (pathOrUri.isBlank()) {
            toast(context, "Nothing to open")
            return false
        }
        return try {
            val file = File(pathOrUri)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (file.exists()) {
                val uri = FileProvider.getUriForFile(
                    context, context.packageName + ".fileprovider", file
                )
                intent.setDataAndType(uri, mimeForName(file.name))
            } else {
                intent.data = Uri.parse(pathOrUri) // a web URL (e.g. a video link)
            }
            context.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            toast(context, "No viewer app found — use \"Download\" to save it to your phone")
            false
        } catch (e: Exception) {
            toast(context, "Cannot open file: ${e.message}")
            false
        }
    }

    /**
     * Save a stored file (or remote URL) into the phone's public Downloads folder so
     * the student can find and open it from their file manager.
     */
    fun downloadToPublic(context: Context, pathOrUri: String, suggestedName: String): Boolean {
        if (pathOrUri.isBlank()) {
            toast(context, "Nothing to download")
            return false
        }
        // Remote URL (Firebase Storage) — let the system DownloadManager fetch it.
        if (pathOrUri.startsWith("http://") || pathOrUri.startsWith("https://")) {
            return try {
                val name = withExtension(suggestedName, ".pdf")
                val request = DownloadManager.Request(Uri.parse(pathOrUri))
                    .setTitle(name)
                    .setDescription("Downloading…")
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, name)
                    .setAllowedOverMetered(true)
                val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                dm.enqueue(request)
                toast(context, "Downloading to Downloads: $name")
                true
            } catch (e: Exception) {
                toast(context, "Download failed: ${e.message}")
                false
            }
        }
        return try {
            val input = openInput(context, pathOrUri) ?: run {
                toast(context, "Could not read the file")
                return false
            }
            val name = withExtension(suggestedName, extOf(pathOrUri))
            val mime = mimeForName(name)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, name)
                    put(MediaStore.Downloads.MIME_TYPE, mime)
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: run { toast(context, "Download failed"); return false }
                resolver.openOutputStream(uri)?.use { out -> input.use { it.copyTo(out) } }
                values.clear()
                values.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            } else {
                @Suppress("DEPRECATION")
                val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!dir.exists()) dir.mkdirs()
                val dest = File(dir, name)
                dest.outputStream().use { out -> input.use { it.copyTo(out) } }
            }
            toast(context, "Saved to Downloads: $name")
            true
        } catch (e: SecurityException) {
            toast(context, "Storage permission needed to download")
            false
        } catch (e: Exception) {
            toast(context, "Download failed: ${e.message}")
            false
        }
    }

    /**
     * Build a multi-page PDF from the given image files (camera scan pages).
     * Uses Android's built-in PdfDocument — no Google Play Services required.
     */
    fun imagesToPdf(images: List<File>, dest: File): Boolean {
        return try {
            dest.parentFile?.mkdirs()
            val pdf = PdfDocument()
            images.forEachIndexed { i, img ->
                val bmp = decodeScaled(img, 1800) ?: return@forEachIndexed
                val info = PdfDocument.PageInfo.Builder(bmp.width, bmp.height, i + 1).create()
                val page = pdf.startPage(info)
                page.canvas.drawBitmap(bmp, 0f, 0f, null)
                pdf.finishPage(page)
                bmp.recycle()
            }
            dest.outputStream().use { pdf.writeTo(it) }
            pdf.close()
            dest.exists() && dest.length() > 0
        } catch (e: Exception) {
            false
        }
    }

    /** Decode an image scaled down (and rotated per EXIF) to keep memory/size sane. */
    private fun decodeScaled(file: File, maxDim: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        var sample = 1
        val largest = maxOf(bounds.outWidth, bounds.outHeight)
        while (largest / sample > maxDim) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        var bmp = BitmapFactory.decodeFile(file.absolutePath, opts) ?: return null

        val degrees = when (
            ExifInterface(file.absolutePath)
                .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        ) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        if (degrees != 0f) {
            val m = Matrix().apply { postRotate(degrees) }
            bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
        }
        return bmp
    }

    // ---------------------------------------------------------------- helpers ---

    private fun openInput(context: Context, pathOrUri: String): InputStream? {
        val file = File(pathOrUri)
        return if (file.exists()) file.inputStream()
        else context.contentResolver.openInputStream(Uri.parse(pathOrUri))
    }

    private fun guessExtension(context: Context, uri: Uri): String {
        val mime = context.contentResolver.getType(uri)
        val fromMime = mime?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) }
        if (!fromMime.isNullOrEmpty()) return ".$fromMime"
        val name = displayName(context, uri)
        val dot = name?.lastIndexOf('.') ?: -1
        return if (name != null && dot >= 0) name.substring(dot) else ""
    }

    private fun displayName(context: Context, uri: Uri): String? = try {
        context.contentResolver.query(uri, null, null, null, null)?.use { c ->
            val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && c.moveToFirst()) c.getString(idx) else null
        }
    } catch (e: Exception) {
        null
    }

    private fun extOf(path: String): String {
        val dot = path.lastIndexOf('.')
        return if (dot >= 0) path.substring(dot) else ""
    }

    private fun withExtension(name: String, ext: String): String {
        val clean = name.replace(Regex("[^A-Za-z0-9._-]"), "_").ifBlank { "file" }
        return if (clean.contains('.')) clean else clean + (ext.ifBlank { ".pdf" })
    }

    private fun mimeForName(name: String): String {
        val ext = name.substringAfterLast('.', "").lowercase()
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "*/*"
    }

    private fun toast(context: Context, msg: String) =
        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
}
