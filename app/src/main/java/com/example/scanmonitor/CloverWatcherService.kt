package com.example.scanmonitor

import android.accessibilityservice.AccessibilityService
import android.media.MediaPlayer
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast

class CloverWatcherService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val text = event?.source?.text?.toString()
            ?: event?.contentDescription?.toString()
            ?: return

        // These are phrases Clover shows when an item isn't found
        val triggerPhrases = listOf("not found", "item not found", "no item", "cancel")

        if (triggerPhrases.any { text.contains(it, ignoreCase = true) }) {
            playAlert()
        }
    }

    private fun playAlert() {
        val player = MediaPlayer.create(this, R.raw.alert)
        player?.apply {
            setOnCompletionListener { mp -> mp.release() }
            start()
        }
        Toast.makeText(this, "Item not found! Please cancel and rescan.", Toast.LENGTH_LONG).show()
    }

    override fun onInterrupt() {}
}