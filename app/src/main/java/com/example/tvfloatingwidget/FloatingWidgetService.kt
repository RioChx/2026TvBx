package com.example.tvfloatingwidget

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.*
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import android.widget.ImageView
import androidx.core.app.NotificationCompat
import android.animation.ObjectAnimator
import java.util.Calendar

class FloatingWidgetService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var floatingView: View
    private lateinit var params: WindowManager.LayoutParams
    private var isMinimized = false

    // Clock Hands
    private lateinit var hourHand: ImageView
    private lateinit var minuteHand: ImageView
    private lateinit var secondHand: ImageView
    private val clockHandler = Handler(Looper.getMainLooper())
    private val clockRunnable = object : Runnable {
        override fun run() {
            updateClock()
            clockHandler.postDelayed(this, 1000)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Toast.makeText(this, "Widget Service Starting...", Toast.LENGTH_SHORT).show()
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onCreate() {
        super.onCreate()
        
        try {
            startForeground() 

            floatingView = LayoutInflater.from(this).inflate(R.layout.layout_floating_widget, null)

            val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            }

            params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            )
            params.gravity = Gravity.TOP or Gravity.START
            params.x = 100
            params.y = 100

            windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
            windowManager.addView(floatingView, params)

            setupUI()
            setupRGBBorder()
            
            // Start Clock
            clockHandler.post(clockRunnable)
            
            Toast.makeText(this, "Widget Added to Screen", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error: " + e.message, Toast.LENGTH_LONG).show()
            stopSelf()
        }
    }

    private fun updateClock() {
        if (!::hourHand.isInitialized) return
        
        val calendar = Calendar.getInstance()
        val hours = calendar.get(Calendar.HOUR)
        val minutes = calendar.get(Calendar.MINUTE)
        val seconds = calendar.get(Calendar.SECOND)

        val secondDeg = seconds * 6f
        val minuteDeg = minutes * 6f + secondDeg / 60f
        val hourDeg = hours * 30f + minuteDeg / 12f

        secondHand.rotation = secondDeg
        minuteHand.rotation = minuteDeg
        hourHand.rotation = hourDeg
    }

    private fun startForeground() {
        val channelId = "floating_widget_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Floating Widget", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("TV Widget Running")
            .setSmallIcon(R.drawable.ic_launcher)
            .build()
        startForeground(1, notification)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupUI() {
        try {
            val btnClose = floatingView.findViewById<ImageButton>(R.id.btn_close)
            val clockContainer = floatingView.findViewById<View>(R.id.clock_container)
            val buttonsContainer = floatingView.findViewById<LinearLayout>(R.id.buttons_container)
            
            // Initialize Hands
            hourHand = floatingView.findViewById(R.id.hand_hour)
            minuteHand = floatingView.findViewById(R.id.hand_minute)
            secondHand = floatingView.findViewById(R.id.hand_second)

            // --- BUTTON LOGIC ---
            
            val btnHome = floatingView.findViewById<ImageButton>(R.id.btn_home)
            btnHome.setOnClickListener {
                val homeIntent = Intent(Intent.ACTION_MAIN)
                homeIntent.addCategory(Intent.CATEGORY_HOME)
                homeIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(homeIntent)
            }

            val btnVolume = floatingView.findViewById<ImageButton>(R.id.btn_volume)
            btnVolume.setOnClickListener {
                val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
                am.adjustVolume(AudioManager.ADJUST_SAME, AudioManager.FLAG_SHOW_UI)
            }
            
            val btnRecent = floatingView.findViewById<ImageButton>(R.id.btn_recent)
            btnRecent.setOnClickListener {
                 Toast.makeText(this, "Recent Apps", Toast.LENGTH_SHORT).show()
            }

            btnClose.setOnClickListener { stopSelf() }

            // --- DRAG LOGIC ---
            clockContainer.setOnTouchListener(object : View.OnTouchListener {
                private var initialX = 0
                private var initialY = 0
                private var initialTouchX = 0f
                private var initialTouchY = 0f

                override fun onTouch(v: View, event: MotionEvent): Boolean {
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            initialX = params.x
                            initialY = params.y
                            initialTouchX = event.rawX
                            initialTouchY = event.rawY
                            return true
                        }
                        MotionEvent.ACTION_MOVE -> {
                            params.x = initialX + (event.rawX - initialTouchX).toInt()
                            params.y = initialY + (event.rawY - initialTouchY).toInt()
                            windowManager.updateViewLayout(floatingView, params)
                            return true
                        }
                        MotionEvent.ACTION_UP -> {
                            if (Math.abs(event.rawX - initialTouchX) < 10 && Math.abs(event.rawY - initialTouchY) < 10) {
                                 isMinimized = !isMinimized
                                 buttonsContainer.visibility = if (isMinimized) View.GONE else View.VISIBLE
                            }
                            return true
                        }
                    }
                    return false
                }
            })
        } catch (e: Exception) {
             e.printStackTrace()
        }
    }

    private fun setupRGBBorder() {
        val borderView = floatingView.findViewById<View>(R.id.rgb_border)
        val animator = ObjectAnimator.ofFloat(borderView, "rotation", 0f, 360f)
        animator.duration = 4000
        animator.repeatCount = ObjectAnimator.INFINITE
        animator.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        clockHandler.removeCallbacks(clockRunnable)
        if (::floatingView.isInitialized) windowManager.removeView(floatingView)
    }
}