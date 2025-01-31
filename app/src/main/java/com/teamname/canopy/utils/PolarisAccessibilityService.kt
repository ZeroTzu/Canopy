package com.teamname.canopy.utils

import android.accessibilityservice.AccessibilityService
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class PolarisAccessibilityService : AccessibilityService() {

    private lateinit var textToSpeech: TextToSpeech
    private var isTTSInitialised = false
    override fun onServiceConnected() {
        textToSpeech = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                Log.d("PolarisAccessibilityService", "Initialization Success")
                isTTSInitialised=true
            } else {
                Log.d("PolarisAccessibilityService", "Initialization Failed")
            }
        }
        super.onServiceConnected()
    }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        Log.i("PolarisAccessibilityService","onAccessibilityEvent")
        if (event != null && event.eventType == AccessibilityEvent.TYPE_VIEW_CLICKED) {
            val text = event.text.toString()
            if (text.isNotEmpty()) {
                Log.i("TextToSpeech",text)
                textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
            }
        }
    }
    override fun onInterrupt() {
        Log.i("PolarisAccessibilityService","onInterrupt")
        if (::textToSpeech.isInitialized) {
            textToSpeech.stop()
        }    }

    override fun onDestroy() {
        Log.i("PolarisAccessibilityService","onDestroy")
        if (::textToSpeech.isInitialized) {
            textToSpeech.shutdown()
        }
        super.onDestroy()
    }
}