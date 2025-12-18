package com.example.tvfloatingwidget
import android.annotation.SuppressLint
import android.app.*
import android.content.*
import android.graphics.*
import android.os.*
import android.view.*
import android.widget.*
import androidx.core.app.NotificationCompat
import java.util.Calendar

class FloatingWidgetService : Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var floatingView: View
    private lateinit var params: WindowManager.LayoutParams
    private var isMinimized = false
    private lateinit var hourHand: ImageView
    private lateinit var minuteHand: ImageView
    private lateinit var secondHand: ImageView
    private val clockHandler = Handler(Looper.getMainLooper())
    private val clockRunnable = object : Runnable {
        override fun run() { updateClock(); clockHandler.postDelayed(this, 1000) }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForeground()
        setupFloatingDock()
        clockHandler.post(clockRunnable)
    }

    private fun startForeground() {
        val channelId = "floating_dock"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Dock Service", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("TV Dock Active")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(1, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else startForeground(1, notification)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupFloatingDock() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        floatingView = LayoutInflater.from(this).inflate(R.layout.layout_floating_widget, null)
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE
        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        params.x = 100; params.y = 100
        windowManager.addView(floatingView, params)

        hourHand = floatingView.findViewById(R.id.hand_hour)
        minuteHand = floatingView.findViewById(R.id.hand_minute)
        secondHand = floatingView.findViewById(R.id.hand_second)
        
        val controlsRibbon = floatingView.findViewById<View>(R.id.controls_ribbon)

        val gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                isMinimized = !isMinimized
                controlsRibbon.visibility = if (isMinimized) View.GONE else View.VISIBLE
                return true
            }
        })

        floatingView.setOnTouchListener { v, event ->
            gestureDetector.onTouchEvent(event)
            when (event.action) {
                MotionEvent.ACTION_DOWN -> true
                MotionEvent.ACTION_MOVE -> {
                    params.x = (event.rawX - v.width / 2).toInt()
                    params.y = (event.rawY - v.height / 2).toInt()
                    windowManager.updateViewLayout(floatingView, params)
                    true
                }
                else -> false
            }
        }
    }

    private fun updateClock() {
        val cal = Calendar.getInstance()
        secondHand.rotation = cal.get(Calendar.SECOND) * 6f
        minuteHand.rotation = cal.get(Calendar.MINUTE) * 6f
        hourHand.rotation = (cal.get(Calendar.HOUR) % 12) * 30f + (cal.get(Calendar.MINUTE) * 0.5f)
    }

    override fun onDestroy() { 
        super.onDestroy()
        clockHandler.removeCallbacks(clockRunnable)
        if (::floatingView.isInitialized) windowManager.removeView(floatingView) 
    }
}