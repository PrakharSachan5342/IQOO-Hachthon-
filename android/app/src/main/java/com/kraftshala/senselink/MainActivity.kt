package com.kraftshala.senselink

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.webkit.WebViewAssetLoader
import java.io.IOException

/**
 * SENSE-LINK — native Kotlin + Jetpack Compose shell hosting the fully on-device
 * edge-AI web pipeline (MediaPipe hand tracking + context composer + speech I/O).
 *
 * The web app and ALL of its dependencies (MediaPipe WASM, the hand-landmark model
 * and the fonts) are bundled in assets/ and served over a secure virtual https origin
 * via [WebViewAssetLoader], so the app runs with zero network — true airplane mode.
 */
class MainActivity : ComponentActivity() {

    private var webView: WebView? = null
    private var llmBridge: LlmBridge? = null

    /** A WebView getUserMedia request that is waiting on an OS permission grant. */
    private var pendingWebRequest: PermissionRequest? = null

    fun registerLlm(bridge: LlmBridge) { llmBridge = bridge }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        // The OS dialog has been answered — resolve any WebView request that was
        // parked waiting for it.
        val req = pendingWebRequest
        pendingWebRequest = null
        if (req != null) {
            val grantable = req.resources.filter { res ->
                osPermissionsFor(res).all { isGranted(it) }
            }.toTypedArray()
            if (grantable.isNotEmpty()) req.grant(grantable) else req.deny()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Keep the screen awake for the live demo.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Ask for camera + mic up front so they're ready by the onboarding toggles.
        val upfront = arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
            .filter { !isGranted(it) }
            .toTypedArray()
        if (upfront.isNotEmpty()) permissionLauncher.launch(upfront)

        WebView.setWebContentsDebuggingEnabled(true)

        setContent { SenseLinkApp(onWebViewCreated = { webView = it }) }
    }

    /**
     * FIX for "Microphone permission denied" on real devices: the WebView fires this
     * when the page calls getUserMedia. Blindly calling request.grant() does NOT grant
     * the underlying OS permission, so if RECORD_AUDIO / CAMERA isn't actually held the
     * capture is denied downstream (which is exactly what happened with the mic toggle).
     * Here we grant only when the matching OS permission is present, and otherwise
     * request it on demand and grant once the user approves.
     */
    fun handleWebPermissionRequest(request: PermissionRequest) {
        runOnUiThread {
            val requiredOs = request.resources.flatMap { osPermissionsFor(it) }.distinct()
            val missing = requiredOs.filter { !isGranted(it) }
            if (missing.isEmpty()) {
                request.grant(request.resources)
            } else {
                pendingWebRequest = request
                permissionLauncher.launch(missing.toTypedArray())
            }
        }
    }

    private fun osPermissionsFor(resource: String): List<String> = when (resource) {
        PermissionRequest.RESOURCE_VIDEO_CAPTURE -> listOf(Manifest.permission.CAMERA)
        PermissionRequest.RESOURCE_AUDIO_CAPTURE -> listOf(Manifest.permission.RECORD_AUDIO)
        else -> emptyList()
    }

    private fun isGranted(p: String): Boolean =
        ContextCompat.checkSelfPermission(this, p) == PackageManager.PERMISSION_GRANTED

    override fun onResume() {
        super.onResume()
        webView?.onResume()
    }

    override fun onPause() {
        webView?.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        llmBridge?.close()
        llmBridge = null
        webView?.destroy()
        webView = null
        super.onDestroy()
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun SenseLinkApp(onWebViewCreated: (WebView) -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                // Serve bundled assets over https://appassets.androidplatform.net so that
                // ES-module imports, WebAssembly streaming and getUserMedia (secure-context)
                // all work from local files.
                val assetLoader = WebViewAssetLoader.Builder()
                    .addPathHandler("/assets/", MimeAwareAssetsHandler(ctx))
                    .build()
                val host = ctx as? MainActivity

                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    setBackgroundColor(Color.parseColor("#0E0E10"))

                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        mediaPlaybackRequiresUserGesture = false
                        allowFileAccess = false
                        allowContentAccess = false
                    }

                    webViewClient = object : WebViewClient() {
                        override fun shouldInterceptRequest(
                            view: WebView,
                            request: WebResourceRequest
                        ): WebResourceResponse? =
                            assetLoader.shouldInterceptRequest(request.url)
                    }

                    webChromeClient = object : WebChromeClient() {
                        override fun onPermissionRequest(request: PermissionRequest) {
                            if (host != null) host.handleWebPermissionRequest(request)
                            else request.grant(request.resources)
                        }
                    }

                    // Expose the on-device Gemma LLM to the web app (degrades to rule-based).
                    if (host != null) {
                        val bridge = LlmBridge(host, this)
                        addJavascriptInterface(bridge, "AndroidLLM")
                        host.registerLlm(bridge)
                        bridge.initIfPresent()
                    }

                    loadUrl("https://appassets.androidplatform.net/assets/www/index.html")
                    onWebViewCreated(this)
                }
            }
        )
    }
}

/**
 * Serves files from assets/ with the correct Content-Type. The default asset handler
 * mislabels `.mjs`/`.wasm`, which breaks ES-module imports and WebAssembly — and would
 * silently break the whole MediaPipe pipeline.
 */
private class MimeAwareAssetsHandler(context: Context) : WebViewAssetLoader.PathHandler {

    private val assets = context.assets

    override fun handle(path: String): WebResourceResponse? {
        val assetPath = path.trimStart('/')
        return try {
            val stream = assets.open(assetPath)
            val mime = mimeFor(assetPath)
            val encoding = if (
                mime.startsWith("text/") ||
                mime.endsWith("javascript") ||
                mime.endsWith("json") ||
                mime == "image/svg+xml"
            ) "utf-8" else null
            WebResourceResponse(mime, encoding, stream).apply {
                responseHeaders = mapOf(
                    "Access-Control-Allow-Origin" to "*",
                    "Cache-Control" to "no-cache"
                )
            }
        } catch (e: IOException) {
            null
        }
    }

    private fun mimeFor(p: String): String = when {
        p.endsWith(".js") || p.endsWith(".mjs") -> "text/javascript"
        p.endsWith(".wasm") -> "application/wasm"
        p.endsWith(".html") || p.endsWith(".htm") -> "text/html"
        p.endsWith(".css") -> "text/css"
        p.endsWith(".json") -> "application/json"
        p.endsWith(".woff2") -> "font/woff2"
        p.endsWith(".woff") -> "font/woff"
        p.endsWith(".ttf") -> "font/ttf"
        p.endsWith(".svg") -> "image/svg+xml"
        p.endsWith(".png") -> "image/png"
        p.endsWith(".jpg") || p.endsWith(".jpeg") -> "image/jpeg"
        p.endsWith(".task") || p.endsWith(".bin") || p.endsWith(".data") -> "application/octet-stream"
        else -> "application/octet-stream"
    }
}
