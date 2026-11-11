package com.raj.codex.ui
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.raj.codex.data.ProjectStore
import java.io.File
/** Transfer APK: bani hui APK ko share intent se dusre phone ko bhejo (Nearby Share / Bluetooth / USB-OTG copy). Build khud cloud me hota hai, transfer sirf bhejne ka kaam hai. */
class TransferActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(32, 32, 32, 32) }
        val info = TextView(this).apply { text = "Transfer APK\n\n1. APK cloud build (GitHub Actions) se download karke is phone ke Downloads / Codex folder me rakho.\n2. Neeche Share dabao — Nearby Share, Bluetooth ya USB-OTG se dusre phone ko bhejo.\n3. Dusre phone me Unknown apps se install ki permission deni padegi." }
        val shareBtn = Button(this).apply { text = "APK Share / Transfer Karo" }
        val listBtn = Button(this).apply { text = "Mere Projects Dekho" }
        val result = TextView(this)
        layout.addView(info); layout.addView(shareBtn); layout.addView(listBtn); layout.addView(result)
        setContentView(layout)
        listBtn.setOnClickListener { result.text = ProjectStore(this).list().joinToString("\n") { "${it.name} — ${it.lastStatus}" }.ifEmpty { "Abhi koi project nahi hai." } }
        shareBtn.setOnClickListener {
            val apk = File(getExternalFilesDir(null), "codex-app.apk")
            if (!apk.exists()) { Toast.makeText(this, "APK abhi is phone me nahi hai. Pehle cloud build se APK download karo.", Toast.LENGTH_LONG).show(); return }
            val uri = FileProvider.getUriForFile(this, "com.raj.codex.fileprovider", apk)
            val intent = Intent(Intent.ACTION_SEND).apply { type = "application/vnd.android.package-archive"; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            startActivity(Intent.createChooser(intent, "Transfer APK — dusre phone ko bhejo"))
        }
    }
}
