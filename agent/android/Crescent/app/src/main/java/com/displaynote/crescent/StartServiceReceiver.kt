package com.displaynote.crescent

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.displaynote.crescent.Util.startJob

class StartServiceReceiver : BroadcastReceiver() {
    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "onReceive")
        IoTSystem.init(context)
        Hardware.init(context)
        startJob(context)
    }

    companion object {
        const val TAG = "StartServiceReceiver"
    }
}