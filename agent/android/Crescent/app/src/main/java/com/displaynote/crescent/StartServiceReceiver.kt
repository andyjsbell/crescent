package com.displaynote.crescent

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.displaynote.crescent.Util.startJob

class StartServiceReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        startJob(context)
    }
}