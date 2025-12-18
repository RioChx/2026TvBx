package com.example.tvosmaster
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
    private lateinit var dock: View

    override fun onBind(i: Intent?): IBinder? = null
    override fun onCreate() {
        super.onCreate()
        val channelId = "master_v35"
        if (Build.VERSION.SDK_INT >= 26) {
            val ch = NotificationChannel(channelId, "Master", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }
        startForeground(1, NotificationCompat.Builder(this, channelId).setContentTitle("TV OS Master Live").setSmallIcon(android.R.drawable.ic_menu_edit).build())
        
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        dock = LayoutInflater.from(this).inflate(R.layout.layout_dock, null)
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= 26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        params.x = 100; params.y = 100
        
        dock.findViewById<ImageButton>(R.id.btn_close_dock).setOnClickListener { stopSelf() }
        
        wm.addView(dock, params)
        dock.setOnTouchListener { v, e ->
            if (e.action == MotionEvent.ACTION_MOVE) {
                params.x = (e.rawX - v.width/2).toInt()
                params.y = (e.rawY - v.height/2).toInt()
                wm.updateViewLayout(dock, params)
            }
            true
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::dock.isInitialized) wm.removeView(dock)
    }
}