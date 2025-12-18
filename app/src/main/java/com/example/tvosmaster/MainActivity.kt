package com.example.tvosmaster
import android.content.Intent
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_main)
        findViewById<ImageButton>(R.id.btn_close_app).setOnClickListener { finish() }
        findViewById<Button>(R.id.btn_launch).setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            } else {
                startService(Intent(this, MasterService::class.java))
                Toast.makeText(this, "Master Engine v3.5 Initialized", Toast.LENGTH_SHORT).show()
            }
        }
    }
}