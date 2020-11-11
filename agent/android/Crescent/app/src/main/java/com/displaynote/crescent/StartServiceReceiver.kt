package com.displaynote.crescent

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.displaynote.crescent.Util.startJob

class StartServiceReceiver : BroadcastReceiver() {
    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    override fun onReceive(context: Context, intent: Intent) {
        IoTSystem.init(context)
        startJob(context)
    }
}