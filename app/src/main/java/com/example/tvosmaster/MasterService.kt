package com.example.tvosmaster

import android.app.*
import android.content.*
import android.graphics.*
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
        val channelId = "tv_master_dock"
        
        if (Build.VERSION.SDK_INT >= 26) {
            val ch = NotificationChannel(channelId, "Dock HUD", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(ch)
        }
        
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Master Dock Active")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
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
        wm.addView(dock, params)
    }

    override fun onDestroy() {
        super.onDestroy()
        dock?.let { wm.removeView(it) }
    }
}