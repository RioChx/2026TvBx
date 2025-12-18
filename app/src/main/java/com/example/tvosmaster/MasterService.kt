package com.example.tvosmaster

import android.app.*
import android.content.*
import android.graphics.*
import android.media.AudioManager
import android.os.*
import android.view.*
import android.widget.*
import androidx.core.app.NotificationCompat
import com.example.tvosmaster.R

class MasterService : Service() {
    private lateinit var wm: WindowManager
    private var dock: View? = null
    
    override fun onBind(i: Intent?): IBinder? = null
    
    override fun onCreate() {
        super.onCreate()
        val channelId = "tv_master_production_v5"
        
        if (Build.VERSION.SDK_INT >= 26) {
            val ch = NotificationChannel(channelId, "TV HUD Master", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(ch)
        }
        
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("TV Master HUD Online")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
            
        startForeground(1, notification)
        
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val inflater = LayoutInflater.from(this)
        dock = inflater.inflate(R.layout.layout_dock, null)
        
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= 26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        params.x = 60
        params.y = 60
        
        wm.addView(dock, params)
        
        // Setup Button Interactivity
        dock?.findViewById<ImageButton>(R.id.btn_home)?.setOnClickListener {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
        }
        
        dock?.findViewById<ImageButton>(R.id.btn_recent)?.setOnClickListener {
            Toast.makeText(this, "Master Engine: Fetching Recents", Toast.LENGTH_SHORT).show()
        }
        
        dock?.findViewById<ImageButton>(R.id.btn_volume)?.setOnClickListener {
            val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_SAME, AudioManager.FLAG_SHOW_UI)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        dock?.let { wm.removeView(it) }
    }
}