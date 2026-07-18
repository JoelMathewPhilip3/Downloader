package com.joel.videodownloader

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Patterns

object UrlTools {
    fun clipboardUrl(context: Context): String? {
        val clipboard = context.getSystemService(ClipboardManager::class.java)
        val text = clipboard.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString()?.trim()
        return text?.takeIf(::isWebUrl)
    }

    fun isWebUrl(value: String): Boolean =
        value.startsWith("https://", true) && Patterns.WEB_URL.matcher(value).matches()

    fun copy(context: Context, value: String) {
        context.getSystemService(ClipboardManager::class.java)
            .setPrimaryClip(ClipData.newPlainText("video link", value))
    }
}
