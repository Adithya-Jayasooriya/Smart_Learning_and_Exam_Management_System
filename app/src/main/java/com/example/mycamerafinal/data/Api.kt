package com.example.mycamerafinal.data

import android.os.Handler
import android.os.Looper
import com.android.volley.DefaultRetryPolicy
import com.android.volley.NetworkResponse
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.HttpHeaderParser
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors

/**
 * Data client. Every endpoint returns { success:bool, message:string, data:... }
 * and callbacks run on the main thread.
 *
 * Two modes, chosen automatically:
 *  - [BASE] still the placeholder → requests are served by the on-device
 *    SQLite database ([LocalDb]); the app works fully offline.
 *  - [BASE] set to a real server  → requests go over HTTP (Volley) to the
 *    PHP/MySQL REST API.
 *
 * IMPORTANT FIX: Volley caches HTTP responses by URL only (it does NOT
 * factor in the POST body). Since every login goes to the SAME URL
 * ("auth.php?action=login") no matter which email/password was sent,
 * Volley could silently return a CACHED response from an earlier login
 * (e.g. the very first lecturer test account) instead of hitting the
 * server again — making every later login look like it returns the same
 * role. We fix this by explicitly disabling caching on every request
 * ([Request.setShouldCache]) and by clearing the queue's cache on
 * startup, so login (and every other call) always goes to the network.
 */
object Api {

    /**
     * Optional: set this to where you uploaded the server/api folder on your
     * cPanel hosting to use the remote MySQL database instead of the built-in
     * SQLite one. Must end with a slash, e.g. "https://yourdomain.com/api/".
     * Full setup instructions: server/README.md in this project.
     */
    var BASE = "https://yourdomain.com/api/"

    /** True while no server URL is configured — the local SQLite DB serves all requests. */
    val usingLocalDb: Boolean get() = BASE.contains("YOUR-DOMAIN-HERE")

    private val queue by lazy {
        Volley.newRequestQueue(App.context).also {
            // Wipe any previously cached responses the first time the queue
            // is created (e.g. right after app start). Belt-and-braces on
            // top of setShouldCache(false) below.
            it.cache.clear()
        }
    }
    private val local by lazy { LocalDb(App.context) }                  // the on-device SQLite DB
    private val io = Executors.newSingleThreadExecutor()                // worker thread for SQLite
    private val main = Handler(Looper.getMainLooper())                  // to reply on the UI thread

    fun get(path: String, onResult: (ok: Boolean, data: Any?, msg: String) -> Unit) =
        send(Request.Method.GET, path, null, onResult)

    fun post(path: String, body: Map<String, Any?>, onResult: (ok: Boolean, data: Any?, msg: String) -> Unit) {
        val json = JSONObject()
        for ((k, v) in body) json.put(k, v ?: JSONObject.NULL)
        send(Request.Method.POST, path, json, onResult)
    }

    /**
     * The heart of the data layer: every read/write in the app comes through
     * here, and this method decides WHERE the request goes.
     */
    private fun send(
        method: Int, path: String, body: JSONObject?,
        onResult: (Boolean, Any?, String) -> Unit
    ) {
        // OFFLINE MODE: serve the request from the phone's SQLite database.
        if (usingLocalDb) {
            io.execute {
                // Run the SQL on a background thread (never block the UI thread).
                val result = try {
                    local.handle(path, body)
                } catch (e: Exception) {
                    Triple(false, null, "Database error: ${e.message}")
                }
                // Deliver the answer back on the main thread, like Volley would.
                main.post { onResult(result.first, result.second, result.third) }
            }
            return
        }
        // ONLINE MODE: send the request as JSON over HTTP with Volley.
        val req = object : JsonObjectRequest(method, BASE + path, body,
            { resp ->
                // The PHP API always answers { success, message, data }.
                val ok = resp.optBoolean("success", false)
                val data = if (resp.isNull("data")) null else resp.opt("data")
                onResult(ok, data, resp.optString("message"))
            },
            { err -> onResult(false, null, parseError(err)) }   // network / server error
        ) {}
        req.retryPolicy = DefaultRetryPolicy(60000, 1, 1f)      // 20 s timeout, 1 retry
        req.setShouldCache(false)                               // NEVER cache: same URL, different bodies (login, etc.)
        queue.add(req)                                          // Volley sends it in background
    }

    /** Multipart file upload (to upload.php). */
    fun upload(
        path: String, fileName: String, bytes: ByteArray, fields: Map<String, String>,
        onResult: (Boolean, Any?, String) -> Unit
    ) {
        if (usingLocalDb) {
            io.execute {
                val result = try {
                    local.saveUpload(fileName, bytes, fields["folder"] ?: "files")
                } catch (e: Exception) {
                    null
                }
                main.post {
                    if (result != null) onResult(true, result, "")
                    else onResult(false, null, "Could not save the file")
                }
            }
            return
        }
        val req = MultipartRequest(
            BASE + path, fileName, bytes, fields,
            { resp ->
                val ok = resp.optBoolean("success", false)
                val data = if (resp.isNull("data")) null else resp.opt("data")
                onResult(ok, data, resp.optString("message"))
            },
            { msg -> onResult(false, null, msg) }
        )
        req.retryPolicy = DefaultRetryPolicy(30000, 1, 1f)
        req.setShouldCache(false)   // uploads must never be served from cache either
        queue.add(req)
    }

    private fun parseError(err: VolleyError): String {
        val data = err.networkResponse?.data ?: return err.localizedMessage ?: "Cannot reach the server"
        return try {
            JSONObject(String(data, Charsets.UTF_8)).optString("message", "Request failed")
        } catch (e: Exception) {
            "Request failed (${err.networkResponse?.statusCode})"
        }
    }
}

/**
 * A custom Volley request that posts ONE file plus text fields as
 * multipart/form-data — the same format an HTML <form> with a file input
 * uses, which is what upload.php on the server expects. Volley has no
 * built-in multipart support, so getBody() builds the raw bytes by hand:
 * each field and the file are separated by a unique "boundary" line.
 */
class MultipartRequest(
    url: String,
    private val fileName: String,
    private val fileBytes: ByteArray,
    private val fields: Map<String, String>,
    private val onOk: (JSONObject) -> Unit,
    private val onErr: (String) -> Unit,
) : Request<JSONObject>(Method.POST, url, Response.ErrorListener { onErr(it.localizedMessage ?: "Upload failed") }) {

    private val boundary = "----slems" + System.currentTimeMillis()
    private val nl = "\r\n"

    override fun getBodyContentType() = "multipart/form-data; boundary=$boundary"

    override fun getBody(): ByteArray {
        val bos = ByteArrayOutputStream()
        for ((k, v) in fields) {
            bos.write(("--$boundary$nl").toByteArray())
            bos.write(("Content-Disposition: form-data; name=\"$k\"$nl$nl").toByteArray())
            bos.write(("$v$nl").toByteArray())
        }
        bos.write(("--$boundary$nl").toByteArray())
        bos.write(("Content-Disposition: form-data; name=\"file\"; filename=\"$fileName\"$nl").toByteArray())
        bos.write(("Content-Type: application/octet-stream$nl$nl").toByteArray())
        bos.write(fileBytes)
        bos.write(nl.toByteArray())
        bos.write(("--$boundary--$nl").toByteArray())
        return bos.toByteArray()
    }

    override fun parseNetworkResponse(response: NetworkResponse): Response<JSONObject> = try {
        Response.success(
            JSONObject(String(response.data, Charsets.UTF_8)),
            HttpHeaderParser.parseCacheHeaders(response)
        )
    } catch (e: Exception) {
        Response.error(VolleyError(e))
    }

    override fun deliverResponse(response: JSONObject) = onOk(response)
}