package com.example.tvfloatingwidget

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.graphics.Typeface
import android.graphics.Shader
import android.graphics.LinearGradient
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.*
import android.view.animation.LinearInterpolator
import android.widget.*
import androidx.core.app.NotificationCompat
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import java.util.Calendar
import java.util.concurrent.Executors

class FloatingWidgetService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var floatingView: View
    private lateinit var params: WindowManager.LayoutParams
    private lateinit var audioManager: AudioManager
    
    // UI References
    private lateinit var volumeBar: ProgressBar
    private lateinit var settingsPanelContainer: FrameLayout
    private lateinit var buttonsContainer: LinearLayout
    private lateinit var dividerView: View
    private lateinit var pillContainer: View
    private lateinit var bannerTextView: TextView
    private lateinit var toggleTextSwitch: Switch
    private lateinit var textInput: EditText
    private lateinit var spinnerFont: Spinner
    private lateinit var spinnerEffect: Spinner
    
    // State
    private var isSettingsOpen = false
    private var isMinimized = false
    private var isTextVisible = false
    private var pulseAnimator: ObjectAnimator? = null

    // Colors
    private var currentRed = 0
    private var currentGreen = 229 
    private var currentBlue = 255

    // Hands
    private lateinit var hourHand: ImageView
    private lateinit var minuteHand: ImageView
    private lateinit var secondHand: ImageView
    private var borderAnimator: ObjectAnimator? = null 

    // Clocks
    private val clockHandler = Handler(Looper.getMainLooper())
    private val clockRunnable = object : Runnable {
        override fun run() {
            updateClock()
            clockHandler.postDelayed(this, 1000)
        }
    }
    
    private val backgroundExecutor = Executors.newSingleThreadExecutor()
    private val volumeHideHandler = Handler(Looper.getMainLooper())
    private val volumeHideRunnable = Runnable { volumeBar.visibility = View.GONE }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        try {
            startForeground() 

            audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
            
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
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT
            )
            params.gravity = Gravity.TOP or Gravity.START
            params.x = 100
            params.y = 100

            windowManager.addView(floatingView, params)
            setupUI()
            setupRGBBorder()
            clockHandler.post(clockRunnable)
            
        } catch (e: Exception) {
            e.printStackTrace()
            stopSelf()
        }
    }
    
    private fun updateClock() {
        if (!::hourHand.isInitialized) return
        val calendar = Calendar.getInstance()
        val hours = calendar.get(Calendar.HOUR)
        val minutes = calendar.get(Calendar.MINUTE)
        val seconds = calendar.get(Calendar.SECOND)
        secondHand.rotation = seconds * 6f
        minuteHand.rotation = minutes * 6f + (seconds * 6f / 60f)
        hourHand.rotation = hours * 30f + (minutes * 6f / 12f)
    }

    private fun startForeground() {
        val channelId = "floating_widget_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Floating Widget", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), 
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_IMMUTABLE else 0
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("TV Widget Running")
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentIntent(pendingIntent)
            .build()
        
        try {
            if (Build.VERSION.SDK_INT >= 34) {
                 startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } else {
                 startForeground(1, notification)
            }
        } catch(e: Exception) {
             // Fallback
             startForeground(1, notification)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupUI() {
        try {
            // ... (Previous bindings)
            val btnClose = floatingView.findViewById<ImageButton>(R.id.btn_close)
            val btnSettings = floatingView.findViewById<ImageButton>(R.id.btn_settings)
            val btnMove = floatingView.findViewById<ImageButton>(R.id.btn_move)
            val clockContainer = floatingView.findViewById<FrameLayout>(R.id.clock_container)

            buttonsContainer = floatingView.findViewById(R.id.buttons_container)
            volumeBar = floatingView.findViewById(R.id.volume_bar)
            settingsPanelContainer = floatingView.findViewById(R.id.settings_panel_container)
            pillContainer = floatingView.findViewById(R.id.pill_container)
            dividerView = floatingView.findViewById(R.id.divider_view)
            bannerTextView = floatingView.findViewById(R.id.banner_text)
            
            // Settings controls
            val seekOpacity = floatingView.findViewById<SeekBar>(R.id.seek_opacity)
            val seekScale = floatingView.findViewById<SeekBar>(R.id.seek_scale)
            val seekSpeed = floatingView.findViewById<SeekBar>(R.id.seek_speed)
            val seekRed = floatingView.findViewById<SeekBar>(R.id.seek_red)
            val seekGreen = floatingView.findViewById<SeekBar>(R.id.seek_green)
            val seekBlue = floatingView.findViewById<SeekBar>(R.id.seek_blue)
            
            toggleTextSwitch = floatingView.findViewById(R.id.switch_show_text)
            textInput = floatingView.findViewById(R.id.input_banner_text)
            spinnerFont = floatingView.findViewById(R.id.spinner_font)
            spinnerEffect = floatingView.findViewById(R.id.spinner_effect)

            hourHand = floatingView.findViewById(R.id.hand_hour)
            minuteHand = floatingView.findViewById(R.id.hand_minute)
            secondHand = floatingView.findViewById(R.id.hand_second)
            
            // Clock Face Generation
            val density = resources.displayMetrics.density
            val radius = 46 * density 
            for (i in 0 until 60) {
                val tick = View(this)
                val isHour = i % 5 == 0
                val w = if (isHour) 2 * density else 1 * density
                val h = if (isHour) 4 * density else 2 * density
                val color = if (isHour) 0xFFFFD700.toInt() else 0xB3FFD700.toInt() 
                tick.setBackgroundColor(color)
                val lp = FrameLayout.LayoutParams(w.toInt(), h.toInt())
                lp.gravity = Gravity.CENTER
                tick.layoutParams = lp
                tick.rotation = i * 6f
                tick.translationY = -radius 
                clockContainer.addView(tick, 0)
            }

            // --- SPINNERS SETUP (Font/Effect) ---
            val fontAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, arrayOf("Modern", "Digital/LED", "Retro", "Code"))
            spinnerFont.adapter = fontAdapter
            spinnerFont.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                    when (pos) {
                        0 -> bannerTextView.typeface = Typeface.SANS_SERIF
                        1 -> bannerTextView.typeface = Typeface.MONOSPACE
                        2 -> bannerTextView.typeface = Typeface.SERIF
                        3 -> bannerTextView.typeface = Typeface.MONOSPACE
                    }
                    if(pos == 1) { // LED simulation
                        bannerTextView.setTypeface(Typeface.MONOSPACE, Typeface.BOLD)
                    }
                }
                override fun onNothingSelected(p: AdapterView<*>?) {}
            }

            val effectAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, arrayOf("Marquee", "Pulse", "Rainbow"))
            spinnerEffect.adapter = effectAdapter
            spinnerEffect.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                     // Reset effects
                     pulseAnimator?.cancel()
                     bannerTextView.alpha = 1f
                     bannerTextView.paint.shader = null
                     
                     when (pos) {
                         0 -> {} // Marquee is default behaviour of TextView
                         1 -> { // Pulse
                             pulseAnimator = ObjectAnimator.ofFloat(bannerTextView, "alpha", 1f, 0.3f, 1f).apply {
                                 duration = 1500
                                 repeatCount = ValueAnimator.INFINITE
                                 start()
                             }
                         }
                         2 -> { // Rainbow
                            val width = bannerTextView.paint.measureText(bannerTextView.text.toString())
                            if (width > 0) {
                                val shader = LinearGradient(0f, 0f, width, 0f, 
                                    intArrayOf(Color.RED, Color.YELLOW, Color.GREEN, Color.BLUE, Color.MAGENTA, Color.RED),
                                    null, Shader.TileMode.CLAMP)
                                bannerTextView.paint.shader = shader
                            }
                         }
                     }
                }
                override fun onNothingSelected(p: AdapterView<*>?) {}
            }
            
            // --- Text Banner Logic ---
            toggleTextSwitch.setOnCheckedChangeListener { _, isChecked ->
                isTextVisible = isChecked
                bannerTextView.visibility = if (isChecked) View.VISIBLE else View.GONE
                if (isChecked) bannerTextView.isSelected = true // Trigger marquee
            }
            
            textInput.setOnKeyListener { _, _, event ->
                if (event.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_ENTER) {
                     bannerTextView.text = textInput.text.toString()
                     return@setOnKeyListener true
                }
                false
            }
            // Also update on focus lost
            textInput.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) bannerTextView.text = textInput.text.toString()
            }

            // --- Button Listeners ---
            floatingView.findViewById<ImageButton>(R.id.btn_home).setOnClickListener {
                val homeIntent = Intent(Intent.ACTION_MAIN)
                homeIntent.addCategory(Intent.CATEGORY_HOME)
                homeIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(homeIntent)
            }
            floatingView.findViewById<ImageButton>(R.id.btn_volume).setOnClickListener {
                audioManager.adjustVolume(AudioManager.ADJUST_SAME, AudioManager.FLAG_SHOW_UI)
            }
            floatingView.findViewById<ImageButton>(R.id.btn_recent).setOnClickListener {
                backgroundExecutor.execute {
                    try { Runtime.getRuntime().exec("input keyevent 187") } catch (e: Exception) {}
                }
            }
            
            btnClose.setOnClickListener { stopSelf() }
            
            btnSettings.setOnClickListener {
                isSettingsOpen = !isSettingsOpen
                // When opening settings, we must make window focusable to allow text input
                if (isSettingsOpen) {
                    params.flags = params.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
                } else {
                    params.flags = params.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                }
                windowManager.updateViewLayout(floatingView, params)
                settingsPanelContainer.visibility = if (isSettingsOpen) View.VISIBLE else View.GONE
            }
            
            // --- Sliders ---
            seekOpacity.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, p: Int, b: Boolean) {
                    params.alpha = (p + 20) / 100f 
                    try { windowManager.updateViewLayout(floatingView, params) } catch(e:Exception){}
                }
                override fun onStartTrackingTouch(s: SeekBar?) {}
                override fun onStopTrackingTouch(s: SeekBar?) {}
            })
            
            seekScale.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, p: Int, b: Boolean) {
                    val scale = 0.5f + (p / 100f) 
                    floatingView.scaleX = scale
                    floatingView.scaleY = scale
                }
                override fun onStartTrackingTouch(s: SeekBar?) {}
                override fun onStopTrackingTouch(s: SeekBar?) {}
            })
            
            seekSpeed.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                 override fun onProgressChanged(s: SeekBar?, p: Int, b: Boolean) {
                     val duration = (11000 - (p * 1000)).toLong() 
                     if (borderAnimator != null) borderAnimator?.duration = duration
                 }
                 override fun onStartTrackingTouch(s: SeekBar?) {}
                 override fun onStopTrackingTouch(s: SeekBar?) {}
            })

            val colorListener = object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, p: Int, b: Boolean) {
                    if (s == seekRed) currentRed = p
                    if (s == seekGreen) currentGreen = p
                    if (s == seekBlue) currentBlue = p
                    val color = Color.rgb(currentRed, currentGreen, currentBlue)
                    dividerView.setBackgroundColor(color)
                    volumeBar.progressTintList = ColorStateList.valueOf(color)
                    bannerTextView.setTextColor(color)
                }
                override fun onStartTrackingTouch(s: SeekBar?) {}
                override fun onStopTrackingTouch(s: SeekBar?) {}
            }
            seekRed.setOnSeekBarChangeListener(colorListener)
            seekGreen.setOnSeekBarChangeListener(colorListener)
            seekBlue.setOnSeekBarChangeListener(colorListener)

            clockContainer.setOnClickListener { toggleMinimize() }

            // --- DRAG LISTENER ---
            val touchListener = object : View.OnTouchListener {
                private var initialX = 0
                private var initialY = 0
                private var initialTouchX = 0f
                private var initialTouchY = 0f
                private var isDragging = false
                private var lastUpdateX = 0
                private var lastUpdateY = 0

                override fun onTouch(v: View, event: MotionEvent): Boolean {
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            initialX = params.x
                            initialY = params.y
                            initialTouchX = event.rawX
                            initialTouchY = event.rawY
                            isDragging = false
                            lastUpdateX = initialX
                            lastUpdateY = initialY
                            return true
                        }
                        MotionEvent.ACTION_MOVE -> {
                            val dx = (event.rawX - initialTouchX).toInt()
                            val dy = (event.rawY - initialTouchY).toInt()
                            if (!isDragging && (Math.abs(dx) > 15 || Math.abs(dy) > 15)) isDragging = true
                            if (isDragging) {
                                params.x = initialX + dx
                                params.y = initialY + dy
                                // Throttle updates
                                if (Math.abs(params.x - lastUpdateX) > 2 || Math.abs(params.y - lastUpdateY) > 2) {
                                    try {
                                        windowManager.updateViewLayout(floatingView, params)
                                        lastUpdateX = params.x
                                        lastUpdateY = params.y
                                    } catch (e: Exception) {}
                                }
                            }
                            return true
                        }
                        MotionEvent.ACTION_UP -> {
                            if (!isDragging) v.performClick()
                            return true
                        }
                    }
                    return false
                }
            }
            pillContainer.setOnTouchListener(touchListener)
            btnMove.setOnTouchListener(touchListener)
            
            // --- DETECT CLICK OUTSIDE TO CLOSE SETTINGS ---
            floatingView.setOnTouchListener { _, event ->
                 if (event.action == MotionEvent.ACTION_OUTSIDE) {
                     if (isSettingsOpen) {
                         isSettingsOpen = false
                         // Revert focusable flag so touches pass through outside
                         params.flags = params.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                         windowManager.updateViewLayout(floatingView, params)
                         settingsPanelContainer.visibility = View.GONE
                         return@setOnTouchListener true
                     }
                 }
                 false
            }

        } catch (e: Exception) { e.printStackTrace() }
    }
    
    // ... (rest of service methods same as before) ...
    
    private fun showVolumeFeedback(increasing: Boolean) {
        volumeHideHandler.removeCallbacks(volumeHideRunnable)
        volumeBar.visibility = View.VISIBLE
        volumeHideHandler.postDelayed(volumeHideRunnable, 2000)
    }

    private fun toggleMinimize() {
        isMinimized = !isMinimized
        val overlayControls = floatingView.findViewById<LinearLayout>(R.id.overlay_controls)
        if (isMinimized) {
            dividerView.visibility = View.GONE
            buttonsContainer.visibility = View.GONE
            if (isSettingsOpen) {
                isSettingsOpen = false
                params.flags = params.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                windowManager.updateViewLayout(floatingView, params)
                settingsPanelContainer.visibility = View.GONE
            }
            volumeBar.visibility = View.GONE
            overlayControls?.visibility = View.GONE
            bannerTextView.visibility = View.GONE 
        } else {
            dividerView.visibility = View.VISIBLE
            buttonsContainer.visibility = View.VISIBLE
            overlayControls?.visibility = View.VISIBLE
            if (isTextVisible) bannerTextView.visibility = View.VISIBLE
        }
    }

    private fun setupRGBBorder() {
        val borderView = floatingView.findViewById<View>(R.id.rgb_border)
        val colors = intArrayOf(Color.RED, Color.YELLOW, Color.GREEN, Color.CYAN, Color.BLUE, Color.MAGENTA, Color.RED)
        val gradient = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, colors)
        gradient.gradientType = GradientDrawable.SWEEP_GRADIENT
        gradient.shape = GradientDrawable.OVAL
        borderView.background = gradient

        borderAnimator = ObjectAnimator.ofFloat(borderView, "rotation", 0f, 360f).apply {
            duration = 6000 
            interpolator = LinearInterpolator() 
            repeatCount = ObjectAnimator.INFINITE
            start()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        clockHandler.removeCallbacks(clockRunnable)
        borderAnimator?.cancel()
        pulseAnimator?.cancel()
        backgroundExecutor.shutdown()
        if (::floatingView.isInitialized) windowManager.removeView(floatingView)
    }
}