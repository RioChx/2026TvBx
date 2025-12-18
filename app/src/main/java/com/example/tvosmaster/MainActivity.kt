package com.example.tvosmaster
import com.example.tvosmaster.R
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(com.example.tvosmaster.R.layout.layout_main)
        findViewById<Button>(com.example.tvosmaster.R.id.btn_launch)?.setOnClickListener {
            startService(Intent(this, MasterService::class.java))
        }
    }
}