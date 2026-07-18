package com.joel.videodownloader

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textfield.TextInputEditText

class MainActivity : AppCompatActivity() {
    private lateinit var urlInput: TextInputEditText
    private lateinit var overlaySwitch: MaterialSwitch
    private lateinit var statusText: TextView

    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { refreshStatus() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        urlInput = findViewById(R.id.urlInput)
        overlaySwitch = findViewById(R.id.overlaySwitch)
        statusText = findViewById(R.id.statusText)

        consumeSharedIntent(intent)

        findViewById<android.view.View>(R.id.pasteButton).setOnClickListener {
            val url = UrlTools.clipboardUrl(this)
            if (url == null) Toast.makeText(this, "Clipboard does not contain a web link", Toast.LENGTH_SHORT).show()
            else urlInput.setText(url)
        }

        findViewById<android.view.View>(R.id.openDownloadButton).setOnClickListener {
            val url = urlInput.text?.toString()?.trim().orEmpty()
            if (!UrlTools.isWebUrl(url)) {
                Toast.makeText(this, "Enter a valid https link", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            Prefs.setLastUrl(this, url)
            DownloadSheetActivity.open(this, url)
        }

        findViewById<android.view.View>(R.id.overlayPermissionButton).setOnClickListener {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
        }

        findViewById<android.view.View>(R.id.notificationAccessButton).setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }

        overlaySwitch.isChecked = Prefs.overlayEnabled(this)
        overlaySwitch.setOnCheckedChangeListener { _, enabled ->
            Prefs.setOverlayEnabled(this, enabled)
            if (enabled) {
                if (!Settings.canDrawOverlays(this)) {
                    overlaySwitch.isChecked = false
                    Toast.makeText(this, "Allow display over other apps first", Toast.LENGTH_LONG).show()
                    startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
                }
            } else {
                stopService(Intent(this, OverlayService::class.java))
            }
            refreshStatus()
        }

        if (Build.VERSION.SDK_INT >= 33) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consumeSharedIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
    }

    private fun consumeSharedIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND) {
            val shared = intent.getStringExtra(Intent.EXTRA_TEXT)?.trim().orEmpty()
            if (UrlTools.isWebUrl(shared)) {
                Prefs.setLastUrl(this, shared)
                DownloadSheetActivity.open(this, shared)
            }
        }
    }

    private fun refreshStatus() {
        val overlay = Settings.canDrawOverlays(this)
        val listener = NotificationAccess.isEnabled(this)
        val engine = when {
            EngineState.ready -> "ready"
            EngineState.error != null -> "error: ${EngineState.error}"
            else -> "initializing"
        }
        statusText.text = "Overlay permission: ${yesNo(overlay)}\nNotification access: ${yesNo(listener)}\nDownloader engine: $engine\n\nTest: enable both permissions, turn on the switch, then play a video in Brave."
    }

    private fun yesNo(value: Boolean) = if (value) "granted" else "not granted"
}
