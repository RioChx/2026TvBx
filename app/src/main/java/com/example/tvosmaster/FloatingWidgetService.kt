package com.example.tvosmaster

import android.app.*
import android.content.*
import android.graphics.*
import android.os.*
import android.view.*
import android.widget.*
import androidx.core.app.NotificationCompat
import com.example.tvosmaster.R

class FloatingWidgetService : Service() {
    private lateinit var wm: WindowManager
    private var widget: View? = null
    
    override fun onBind(i: Intent?): IBinder? = null
    
    override fun onCreate() {
        super.onCreate()
        val channelId = "tv_floating_widget"
        
        if (Build.VERSION.SDK_INT >= 26) {
            val ch = NotificationChannel(channelId, "Floating Widget", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(ch)
        }
        
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Floating Clock Active")
            .setSmallIcon(android.R.drawable.ic_menu_today)
            .build()
            
        startForeground(3, notification)
        
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val inflater = LayoutInflater.from(this)
        widget = inflater.inflate(R.layout.layout_floating_widget, null)
        
        val params = WindowManager.LayoutParams(
            200, 200,
            if (Build.VERSION.SDK_INT >= 26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.CENTER
        wm.addView(widget, params)
    }

    override fun onDestroy() {
        super.onDestroy()
        widget?.let { wm.removeView(it) }
    }
}