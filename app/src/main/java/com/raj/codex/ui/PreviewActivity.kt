package com.raj.codex.ui
import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import com.raj.codex.data.ProjectStore
import java.io.File
class PreviewActivity : AppCompatActivity() {
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val wv = WebView(this); setContentView(wv)
        wv.settings.javaScriptEnabled = true
        wv.webViewClient = WebViewClient()
        val id = intent.getStringExtra("projectId") ?: ""
        val f = File(ProjectStore(this).dir(id), "index.html")
        if (f.exists()) wv.loadUrl("file://" + f.absolutePath) else wv.loadData("<h1>Abhi koi app nahi bani</h1>", "text/html", "utf-8")
    }
}
