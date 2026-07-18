package com.joel.videodownloader

import android.content.ComponentName
import android.content.Intent
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.service.notification.NotificationListenerService

class MediaNotificationListener : NotificationListenerService() {
    private lateinit var manager: MediaSessionManager
    private val callbacks = mutableMapOf<MediaController, MediaController.Callback>()

    private val sessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { sessions ->
        updateSessions(sessions.orEmpty())
    }

    override fun onListenerConnected() {
        manager = getSystemService(MediaSessionManager::class.java)
        manager.addOnActiveSessionsChangedListener(sessionsListener, ComponentName(this, javaClass))
        updateSessions(manager.getActiveSessions(ComponentName(this, javaClass)))
    }

    override fun onListenerDisconnected() {
        if (::manager.isInitialized) manager.removeOnActiveSessionsChangedListener(sessionsListener)
        callbacks.forEach { (controller, callback) -> controller.unregisterCallback(callback) }
        callbacks.clear()
        setBravePlaying(false)
    }

    private fun updateSessions(sessions: List<MediaController>) {
        callbacks.keys.filter { it !in sessions }.forEach { controller ->
            callbacks.remove(controller)?.let(controller::unregisterCallback)
        }
        sessions.forEach { controller ->
            if (controller !in callbacks) {
                val callback = object : MediaController.Callback() {
                    override fun onPlaybackStateChanged(state: android.media.session.PlaybackState?) = evaluate()
                    override fun onMetadataChanged(metadata: android.media.MediaMetadata?) = evaluate()
                }
                controller.registerCallback(callback)
                callbacks[controller] = callback
            }
        }
        evaluate()
    }

    private fun evaluate() {
        val bravePlaying = callbacks.keys.any { controller ->
            isBrave(controller.packageName) && controller.playbackState?.state == android.media.session.PlaybackState.STATE_PLAYING
        }
        setBravePlaying(bravePlaying)
    }

    private fun isBrave(packageName: String): Boolean = packageName in setOf(
        "com.brave.browser",
        "com.brave.browser_beta",
        "com.brave.browser_nightly"
    )

    private fun setBravePlaying(playing: Boolean) {
        if (!Prefs.overlayEnabled(this)) return
        val intent = Intent(this, OverlayService::class.java).apply {
            action = if (playing) OverlayService.ACTION_SHOW else OverlayService.ACTION_HIDE
        }
        androidx.core.content.ContextCompat.startForegroundService(this, intent)
    }
}
