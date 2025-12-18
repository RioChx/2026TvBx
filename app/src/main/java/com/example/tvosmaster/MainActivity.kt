package com.example.tvosmaster

import com.example.tvosmaster.R
import android.content.Intent
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // GLOBAL SYNC: Explicit R path
        setContentView(com.example.tvosmaster.R.layout.layout_main)
        
        val closeBtn = findViewById<ImageButton>(com.example.tvosmaster.R.id.btn_close_app)
        closeBtn?.setOnClickListener { 
            finish() 
        }
        
        val launchBtn = findViewById<Button>(com.example.tvosmaster.R.id.btn_launch)
        launchBtn?.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                startActivity(intent)
            } else {
                startService(Intent(this, MasterService::class.java))
                Toast.makeText(this, "TV Master Engine Initialized", Toast.LENGTH_SHORT).show()
            }
        }
    }
}