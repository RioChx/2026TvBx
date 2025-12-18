package com.example.textstudiopro
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
        
        val btnStart = findViewById<Button>(R.id.btn_start_service)
        val statusText = findViewById<TextView>(R.id.txt_status)

        btnStart.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                startActivity(intent)
            } else {
                startService(Intent(this, TextStudioService::class.java))
                Toast.makeText(this, "Studio Started", Toast.LENGTH_SHORT).show()
                statusText.text = "Status: Running"
            }
        }
    }
}