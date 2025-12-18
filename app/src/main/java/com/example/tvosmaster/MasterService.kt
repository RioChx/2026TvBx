package com.example.tvosmaster

import com.example.tvosmaster.R
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
        val channelId = "master_v35_service"
        
        if (Build.VERSION.SDK_INT >= 26) {
            val ch = NotificationChannel(channelId, "System Master", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(ch)
        }
        
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("TV OS Master Live")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
            
        startForeground(1, notification)
        
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        
        // Setup Dock Overlay using explicit R paths
        dock = LayoutInflater.from(this).inflate(com.example.tvosmaster.R.layout.layout_dock, null)
        val dockParams = createOverlayParams(40, 40)
        dock?.findViewById<ImageButton>(com.example.tvosmaster.R.id.btn_close_dock)?.setOnClickListener { 
            stopSelf() 
        }
        wm.addView(dock, dockParams)
        setupDraggable(dock!!, dockParams)

        // Setup Text Studio Overlay using explicit R paths
        textStudio = LayoutInflater.from(this).inflate(com.example.tvosmaster.R.layout.layout_text_studio, null)
        val tsParams = createOverlayParams(40, 300)
        val marqueeText = textStudio?.findViewById<TextView>(com.example.tvosmaster.R.id.tv_marquee)
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