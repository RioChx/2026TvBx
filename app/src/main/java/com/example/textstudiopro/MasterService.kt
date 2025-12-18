package com.example.textstudiopro

import com.example.textstudiopro.R
import android.annotation.SuppressLint
import android.app.*
import android.content.*
import android.graphics.*
import android.os.*
import android.view.*
import android.widget.*
import androidx.core.app.NotificationCompat

class MasterService : Service() {
    private lateinit var wm: WindowManager
    private var dock: View? = null
    private var textStudio: View? = null

    override fun onBind(i: Intent?): IBinder? = null
    
    @SuppressLint("InflateParams", "ClickableViewAccessibility")
    override fun onCreate() {
        super.onCreate()
        val channelId = "tv_box_master_service_v35"
        
        if (Build.VERSION.SDK_INT >= 26) {
            val ch = NotificationChannel(channelId, "System HUD", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(ch)
        }
        
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("TV Master Hub Live")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
            
        startForeground(1, notification)
        
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        
        // FORCED IDENTITY SYNC: Absolute R reference
        dock = LayoutInflater.from(this).inflate(com.example.textstudiopro.R.layout.layout_dock, null)
        val dockParams = createOverlayParams(50, 50)
        dock?.findViewById<ImageButton>(com.example.textstudiopro.R.id.btn_close_dock)?.setOnClickListener { 
            stopSelf() 
        }
        wm.addView(dock, dockParams)
        setupDraggable(dock!!, dockParams)

        textStudio = LayoutInflater.from(this).inflate(com.example.textstudiopro.R.layout.layout_text_studio, null)
        val tsParams = createOverlayParams(50, 250)
        val marqueeText = textStudio?.findViewById<TextView>(com.example.textstudiopro.R.id.tv_marquee)
        marqueeText?.isSelected = true
        
        wm.addView(textStudio, tsParams)
        setupDraggable(textStudio!!, tsParams)
    }

    private fun createOverlayParams(x: Int, y: Int): WindowManager.LayoutParams {
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT, 
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= 26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        params.x = x
        params.y = y
        return params
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupDraggable(view: View, params: WindowManager.LayoutParams) {
        view.setOnTouchListener { v, e ->
            when (e.action) {
                MotionEvent.ACTION_MOVE -> {
                    params.x = (e.rawX - v.width / 2).toInt()
                    params.y = (e.rawY - v.height / 2).toInt()
                    wm.updateViewLayout(v, params)
                }
            }
            true
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        dock?.let { if (it.isAttachedToWindow) wm.removeView(it) }
        textStudio?.let { if (it.isAttachedToWindow) wm.removeView(it) }
    }
}