package com.alisekutty.client

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import org.json.JSONObject
import java.io.File
import java.util.zip.ZipInputStream

class MainActivity : Activity() {
    private lateinit var web: WebView
    private lateinit var status: TextView
    private val pickZip = 4102
    private val extensionsDir by lazy { File(filesDir, "extensions").apply { mkdirs() } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CookieManager.getInstance().setAcceptCookie(true)
        buildScreen()
        web.loadUrl("https://web.whatsapp.com/")
    }

    private fun buildScreen() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(8, 8, 8))
        }
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(14, 8, 14, 8)
            setBackgroundColor(Color.BLACK)
        }
        val title = TextView(this).apply {
            text = "Alise×Kutty"
            textSize = 19f
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
        }
        val ext = Button(this).apply {
            text = "Extensions"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.rgb(48, 48, 48))
            setOnClickListener { showExtensions() }
        }
        bar.addView(title)
        bar.addView(ext)
        status = TextView(this).apply {
            text = "Unofficial WhatsApp Web client • Extensions are local CSS themes"
            textSize = 11f
            setTextColor(Color.LTGRAY)
            setPadding(12, 5, 12, 5)
            setBackgroundColor(Color.rgb(24, 24, 24))
        }
        web = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.databaseEnabled = true
            settings.loadsImagesAutomatically = true
            settings.userAgentString = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
            webChromeClient = WebChromeClient()
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    if (url?.startsWith("https://web.whatsapp.com") == true) applyEnabledStyles()
                }
                override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                    return !url.startsWith("https://web.whatsapp.com") && !url.startsWith("https://www.whatsapp.com")
                }
            }
        }
        root.addView(bar)
        root.addView(status)
        root.addView(web, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
    }

    private fun showExtensions() {
        val dirs = extensionsDir.listFiles()?.filter { it.isDirectory } ?: emptyList()
        val labels = dirs.map { dir ->
            val meta = runCatching { JSONObject(File(dir, "manifest.json").readText()) }.getOrNull()
            val name = meta?.optString("name", dir.name) ?: dir.name
            val enabled = File(dir, "enabled.flag").exists()
            "$name  ${if (enabled) "[ON]" else "[OFF]"}"
        }.toMutableList()
        labels.add("＋ Import extension ZIP")
        labels.add("About / safety")
        AlertDialog.Builder(this)
            .setTitle("Alise×Kutty Extensions")
            .setItems(labels.toTypedArray()) { _, which ->
                when {
                    which == dirs.size -> openZipPicker()
                    which == dirs.size + 1 -> AlertDialog.Builder(this)
                        .setTitle("Extension safety")
                        .setMessage("Only import CSS theme ZIPs from sources you trust. This client deliberately does not run extension JavaScript or read your messages. WhatsApp login and data are handled by WhatsApp Web.")
                        .setPositiveButton("OK", null).show()
                    else -> {
                        val flag = File(dirs[which], "enabled.flag")
                        if (flag.exists()) flag.delete() else flag.writeText("enabled")
                        showExtensions()
                        applyEnabledStyles()
                    }
                }
            }.setNegativeButton("Close", null).show()
    }

    private fun openZipPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/zip"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream"))
        }
        startActivityForResult(intent, pickZip)
    }

    @Deprecated("Deprecated by Android, retained for broad device compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == pickZip && resultCode == Activity.RESULT_OK) {
            val uri = data?.data ?: return
            try {
                importCssZip(uri)
                Toast.makeText(this, "Extension imported. Enable it in Extensions.", Toast.LENGTH_LONG).show()
                showExtensions()
            } catch (e: Exception) {
                Toast.makeText(this, "Import failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun importCssZip(uri: Uri) {
        val id = "ext_${System.currentTimeMillis()}"
        val target = File(extensionsDir, id).apply { mkdirs() }
        var manifestFound = false
        var cssFound = false
        contentResolver.openInputStream(uri).use { raw ->
            requireNotNull(raw) { "Cannot open ZIP" }
            ZipInputStream(raw).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    require(!entry.isDirectory) { "Folders are not supported in extension ZIPs" }
                    val name = entry.name.substringAfterLast('/')
                    require(name == "manifest.json" || name == "style.css") { "Only manifest.json and style.css are allowed" }
                    val out = File(target, name)
                    val bytes = zip.readBytes()
                    require(bytes.size <= 512 * 1024) { "Extension file is too large" }
                    out.writeBytes(bytes)
                    if (name == "manifest.json") manifestFound = true
                    if (name == "style.css") cssFound = true
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        }
        require(manifestFound && cssFound) { "ZIP must contain manifest.json and style.css" }
        val manifest = JSONObject(File(target, "manifest.json").readText())
        require(manifest.optString("entry") == "style.css") { "manifest entry must be style.css" }
        if (manifest.optString("name").isBlank()) manifest.put("name", id)
        File(target, "manifest.json").writeText(manifest.toString(2))
        File(target, "enabled.flag").writeText("enabled")
    }

    private fun applyEnabledStyles() {
        val css = extensionsDir.listFiles()?.filter { it.isDirectory && File(it, "enabled.flag").exists() }
            ?.mapNotNull { dir -> runCatching { File(dir, "style.css").readText() }.getOrNull() }
            ?.joinToString("\n") ?: ""
        val encoded = android.util.Base64.encodeToString(css.toByteArray(Charsets.UTF_8), android.util.Base64.NO_WRAP)
        val script = "(function(){var c=atob('$encoded');var s=document.getElementById('alisekutty-extension-css');if(!s){s=document.createElement('style');s.id='alisekutty-extension-css';document.head.appendChild(s);}s.textContent=c;})();"
        web.evaluateJavascript(script, null)
    }

    override fun onBackPressed() {
        if (::web.isInitialized && web.canGoBack()) web.goBack() else super.onBackPressed()
    }

    override fun onDestroy() {
        if (::web.isInitialized) web.destroy()
        super.onDestroy()
    }
}
