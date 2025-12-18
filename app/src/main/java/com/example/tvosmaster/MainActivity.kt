package com.example.tvosmaster

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.tvosmaster.R

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_main)
        
        findViewById<Button>(R.id.btn_launch)?.setOnClickListener {
            // Launch the artistic master HUD system
            startService(Intent(this, MasterService::class.java))
        }
    }
}