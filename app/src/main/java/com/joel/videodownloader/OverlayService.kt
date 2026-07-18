package com.joel.videodownloader

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.core.app.NotificationCompat

class OverlayService : Service() {
    private lateinit var windowManager: WindowManager
    private var button: View? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WindowManager::class.java)
        createChannel()
        startForeground(NOTIFICATION_ID, NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle("Brave download button")
            .setContentText("Visible while Brave is playing media")
            .setOngoing(true)
            .build())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SHOW -> showButton()
            ACTION_HIDE -> hideButton(stop = true)
        }
        return START_NOT_STICKY
    }

    private fun showButton() {
        if (button != null || !Settings.canDrawOverlays(this)) return

        val view = TextView(this).apply {
            text = "↓"
            textSize = 30f
            gravity = Gravity.CENTER
            setTextColor(android.graphics.Color.WHITE)
            background = getDrawable(R.drawable.bg_overlay_button)
            elevation = 12f
            contentDescription = "Download current Brave video"
        }

        val params = WindowManager.LayoutParams(
            dp(58), dp(58),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.END
            x = dp(18)
            y = dp(90)
        }

        var downX = 0f
        var downY = 0f
        var startX = 0
        var startY = 0
        var moved = false
        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX; downY = event.rawY
                    startX = params.x; startY = params.y
                    moved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - downX).toInt()
                    val dy = (event.rawY - downY).toInt()
                    if (kotlin.math.abs(dx) > 8 || kotlin.math.abs(dy) > 8) moved = true
                    params.x = (startX - dx).coerceAtLeast(0)
                    params.y = (startY - dy).coerceAtLeast(0)
                    windowManager.updateViewLayout(view, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved) openSheet()
                    true
                }
                else -> false
            }
        }

        button = view
        windowManager.addView(view, params)
    }

    private fun openSheet() {
        val url = UrlTools.clipboardUrl(this) ?: Prefs.lastUrl(this)
        startActivity(Intent(this, DownloadSheetActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(DownloadSheetActivity.EXTRA_URL, url)
        })
    }

    private fun hideButton(stop: Boolean) {
        button?.let { runCatching { windowManager.removeView(it) } }
        button = null
        if (stop) stopSelf()
    }

    override fun onDestroy() {
        hideButton(stop = false)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CHANNEL, "Overlay", NotificationManager.IMPORTANCE_LOW)
            )
        }
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    companion object {
        const val ACTION_SHOW = "com.joel.videodownloader.SHOW"
        const val ACTION_HIDE = "com.joel.videodownloader.HIDE"
        private const val CHANNEL = "overlay"
        private const val NOTIFICATION_ID = 71
    }
}
