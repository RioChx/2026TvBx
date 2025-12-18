package com.example.tvosmaster

import android.app.*
import android.content.*
import android.graphics.*
import android.os.*
import android.view.*
import android.widget.*
import androidx.core.app.NotificationCompat
import com.example.tvosmaster.R

class TextStudioService : Service() {
    private lateinit var wm: WindowManager
    private var ribbon: View? = null
    
    override fun onBind(i: Intent?): IBinder? = null
    
    override fun onCreate() {
        super.onCreate()
        val channelId = "tv_text_studio"
        
        if (Build.VERSION.SDK_INT >= 26) {
            val ch = NotificationChannel(channelId, "Text Studio", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(ch)
        }
        
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Text Studio Active")
            .setSmallIcon(android.R.drawable.ic_menu_edit)
            .build()
            
        startForeground(2, notification)
        
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        
        val inflater = LayoutInflater.from(this)
        ribbon = inflater.inflate(R.layout.layout_text_studio, null)
        
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= 26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.BOTTOM
        params.y = 100
        
        wm.addView(ribbon, params)
        
        val textView = ribbon?.findViewById<TextView>(R.id.tv_marquee)
        textView?.isSelected = true // Required for Marquee to scroll
    }

    override fun onDestroy() {
        super.onDestroy()
        ribbon?.let { wm.removeView(it) }
    }
}