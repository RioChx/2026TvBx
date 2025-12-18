package com.example.tvfloatingwidget
import android.annotation.SuppressLint
import android.app.*
import android.content.*
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.media.AudioManager
import android.os.*
import android.view.*
import android.view.animation.LinearInterpolator
import android.widget.*
import androidx.core.app.NotificationCompat
import android.animation.*
import java.util.Calendar
import java.util.concurrent.Executors

class FloatingWidgetService : Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var floatingView: View
    private lateinit var params: WindowManager.LayoutParams
    private lateinit var audioManager: AudioManager
    private lateinit var hourHand: ImageView
    private lateinit var minuteHand: ImageView
    private lateinit var secondHand: ImageView
    private var borderAnimator: ObjectAnimator? = null
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
        val notification = NotificationCompat.Builder(this, channelId).setContentTitle("TV Dock is Active").setSmallIcon(android.R.drawable.ic_dialog_info).build()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(1, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(1, notification)
        }
    }

    @SuppressLint("ClickableViewAccessibility", "SetTextI18n")
    private fun setupFloatingDock() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        floatingView = LayoutInflater.from(this).inflate(R.layout.layout_floating_widget, null)
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE
        params = WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT, type, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT)
        params.gravity = Gravity.TOP or Gravity.START
        params.x = 100; params.y = 100
        windowManager.addView(floatingView, params)
        hourHand = floatingView.findViewById(R.id.hand_hour)
        minuteHand = floatingView.findViewById(R.id.hand_minute)
        secondHand = floatingView.findViewById(R.id.hand_second)
        val clockContainer = floatingView.findViewById<FrameLayout>(R.id.clock_container)
        val density = resources.displayMetrics.density
        for (i in 1..12) {
            val tv = TextView(this).apply { text = i.toString(); setTextColor(Color.WHITE); textSize = 10f; typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.CENTER }
            val lp = FrameLayout.LayoutParams((20 * density).toInt(), (20 * density).toInt(), Gravity.CENTER)
            val angle = Math.toRadians((i * 30 - 90).toDouble())
            val r = 38 * density
            tv.translationX = (r * Math.cos(angle)).toFloat()
            tv.translationY = (r * Math.sin(angle)).toFloat()
            clockContainer.addView(tv, lp)
        }
        floatingView.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0; private var initialY = 0; private var initialTouchX = 0f; private var initialTouchY = 0f
            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> { initialX = params.x; initialY = params.y; initialTouchX = event.rawX; initialTouchY = event.rawY; return true }
                    MotionEvent.ACTION_MOVE -> { params.x = initialX + (event.rawX - initialTouchX).toInt(); params.y = initialY + (event.rawY - initialTouchY).toInt(); try { windowManager.updateViewLayout(floatingView, params) } catch (e: Exception) {}; return true }
                }
                return false
            }
        })
        val border = floatingView.findViewById<View>(R.id.rgb_border)
        val colors = intArrayOf(Color.RED, Color.YELLOW, Color.GREEN, Color.CYAN, Color.BLUE, Color.MAGENTA, Color.RED)
        border.background = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, colors).apply { gradientType = GradientDrawable.SWEEP_GRADIENT; shape = GradientDrawable.OVAL }
        borderAnimator = ObjectAnimator.ofFloat(border, "rotation", 0f, 360f).apply { duration = 6000; interpolator = LinearInterpolator(); repeatCount = ValueAnimator.INFINITE; start() }
    }

    private fun updateClock() {
        val cal = Calendar.getInstance()
        secondHand.rotation = cal.get(Calendar.SECOND) * 6f
        minuteHand.rotation = cal.get(Calendar.MINUTE) * 6f
        hourHand.rotation = (cal.get(Calendar.HOUR) % 12) * 30f + (cal.get(Calendar.MINUTE) * 0.5f)
    }

    override fun onDestroy() { super.onDestroy(); clockHandler.removeCallbacks(clockRunnable); borderAnimator?.cancel(); if (::floatingView.isInitialized) windowManager.removeView(floatingView) }
}