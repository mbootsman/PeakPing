package nl.marcel.peakping

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlin.math.abs

class FloatingWindowService : Service() {

    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null
    private lateinit var layoutParams: WindowManager.LayoutParams
    private lateinit var elevationText: TextView
    private lateinit var unitText: TextView

    private val prefs by lazy { getSharedPreferences("peakping_prefs", Context.MODE_PRIVATE) }

    /** Collects the shared [LocationRepository] state; cancelled in onDestroy. */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /** True while this service holds a [LocationRepository] reference. */
    private var holdsLocation = false

    private fun render(state: GpsState) {
        if (!state.locked) return  // keep "--" until the first fix
        val unit = UnitSystem.valueOf(
            prefs.getString("unit_system", UnitSystem.METRIC.name) ?: UnitSystem.METRIC.name
        )
        elevationText.text = if (unit == UnitSystem.METRIC) state.elevation.toInt().toString()
                             else (state.elevation * 3.28084).toInt().toString()
        unitText.text = if (unit == UnitSystem.METRIC) " m" else " ft"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        LocationRepository.init(this)
        startForegroundNotification()
        createOverlayView()
        // Same source (GPS + baro fusion, interval throttling) as the main screen,
        // so the overlay always shows the same value at the same moment.
        holdsLocation = LocationRepository.acquire()
        LocationRepository.gpsState.onEach(::render).launchIn(scope)
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        overlayView?.let { windowManager.removeView(it) }
        scope.cancel()
        if (holdsLocation) {
            LocationRepository.release()
            holdsLocation = false
        }
    }

    private fun startForegroundNotification() {
        val channelId = "peakping_overlay"
        val nm = getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(channelId) == null) {
            nm.createNotificationChannel(
                NotificationChannel(channelId, "Floating Window", NotificationManager.IMPORTANCE_LOW)
            )
        }

        val openPi = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_IMMUTABLE
        )
        val stopPi = PendingIntent.getService(
            this, 1,
            Intent(this, FloatingWindowService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("PeakPing")
            .setContentText("Floating elevation window is active")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(openPi)
            .addAction(0, "Close", stopPi)
            .setOngoing(true)
            .setSilent(true)
            .build()

        startForeground(NOTIF_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
    }

    private fun createOverlayView() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val density = resources.displayMetrics.density
        fun dp(v: Int) = (v * density).toInt()

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            background = GradientDrawable().apply {
                // ~82% opaque dark navy matching the app's DarkColors.bg
                setColor(Color.argb(209, 8, 14, 20))
                cornerRadius = dp(18).toFloat()
            }
            setPadding(dp(28), dp(16), dp(20), dp(16))
            gravity = Gravity.CENTER_VERTICAL
        }

        elevationText = TextView(this).apply {
            text = "--"
            textSize = 20f
            setTextColor(Color.WHITE)
            typeface = android.graphics.Typeface.MONOSPACE
        }

        unitText = TextView(this).apply {
            text = " m"
            textSize = 14f
            setTextColor(Color.WHITE)
            typeface = android.graphics.Typeface.MONOSPACE
        }

        val closeView = TextView(this).apply {
            text = "  ✕"
            textSize = 15f
            setTextColor(Color.argb(160, 255, 255, 255))
        }

        container.addView(elevationText)
        container.addView(unitText)
        container.addView(closeView)

        layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dp(16)
            y = dp(80)
        }

        setupDragAndTap(container, closeView)
        windowManager.addView(container, layoutParams)
        overlayView = container
    }

    private fun setupDragAndTap(container: LinearLayout, closeView: TextView) {
        var initialX = 0
        var initialY = 0
        var touchX = 0f
        var touchY = 0f
        var isDragging = false

        container.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = layoutParams.x
                    initialY = layoutParams.y
                    touchX = event.rawX
                    touchY = event.rawY
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - touchX
                    val dy = event.rawY - touchY
                    if (!isDragging && (abs(dx) > 8f || abs(dy) > 8f)) isDragging = true
                    if (isDragging) {
                        layoutParams.x = (initialX + dx).toInt()
                        layoutParams.y = (initialY + dy).toInt()
                        windowManager.updateViewLayout(container, layoutParams)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isDragging) {
                        // Right ~40 dp = close button area
                        val closeThreshold = v.width - closeView.width
                        if (event.x >= closeThreshold) {
                            stopSelf()
                        } else {
                            startActivity(
                                Intent(this, MainActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                                }
                            )
                        }
                    }
                    true
                }
                else -> false
            }
        }
    }

    companion object {
        var isRunning = false
            private set

        private const val NOTIF_ID = 1001
        const val ACTION_STOP = "nl.marcel.peakping.STOP_FLOATING"

        fun start(context: Context) =
            context.startForegroundService(Intent(context, FloatingWindowService::class.java))

        fun stop(context: Context) =
            context.stopService(Intent(context, FloatingWindowService::class.java))
    }
}
