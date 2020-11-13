package com.displaynote.crescent

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class CustomAccessibilityService : AccessibilityService() {

    //Configure the Accessibility Service
    override fun onServiceConnected() {
        Log.d(TAG, "onServiceConnected")
        val data = StateData("accessibility", "connected")
        IoTSystem.publish(data)
    }

    //Respond to AccessibilityEvents
    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        Log.d(TAG, "onAccessibilityEvent")
        val data = AccessibilityData(
                event.packageName.toString(),
                event.eventTime,
                event.eventType
        )

        IoTSystem.publish(data)
    }

    override fun onInterrupt() {
        //Interrupt the Accessibility service
        //Stop
        val data = StateData("accessibility", "interrupt")
        IoTSystem.publish(data)
    }

    companion object {
        private const val TAG: String = "MainService"
    }
}