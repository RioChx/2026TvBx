package com.example.textstudiopro

import com.example.textstudiopro.R
import android.content.Intent
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // MASTER SYNC: Explicit R path
        setContentView(com.example.textstudiopro.R.layout.layout_main)
        
        val closeBtn = findViewById<ImageButton>(com.example.textstudiopro.R.id.btn_close_app)
        closeBtn?.setOnClickListener { 
            finish() 
        }
        
        val launchBtn = findViewById<Button>(com.example.textstudiopro.R.id.btn_launch)
        launchBtn?.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                startActivity(intent)
            } else {
                startService(Intent(this, MasterService::class.java))
                Toast.makeText(this, "Text Studio Master Engine Live", Toast.LENGTH_SHORT).show()
            }
        }
    }
}