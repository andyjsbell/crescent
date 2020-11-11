package com.displaynote.crescent

import android.accessibilityservice.AccessibilityService
import android.os.Build
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import java.util.*

class CustomAccessibilityService : AccessibilityService() {

    private val TAG = MainService::class.java.canonicalName

    //Configure the Accessibility Service
    override fun onServiceConnected() {
        Log.d(TAG, "onServiceConnected")
    }

    //Respond to AccessibilityEvents
    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        Log.d(TAG, "onAccessibilityEvent")

        val payload = "{" +
                "\"message\":" +
                "\"packageName\"," +
                "\"value\":" +
                "\"${event.packageName.toString()}\"," +
                "\"time\":" +
                "\"${event.eventTime}\"" +
                "}"

        IoTSystem.get()?.publish("topic", payload)
    }

    override fun onInterrupt() {
        //Interrupt the Accessibility service
        //Stop
    }
}