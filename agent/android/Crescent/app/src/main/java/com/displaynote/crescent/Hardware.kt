package com.displaynote.crescent

import android.content.Context
import android.os.Build
import android.util.Log
import com.google.gson.Gson
import com.hht.support.display.SourceManager
import java.util.*

object Hardware {
    private const val VERSION_PREFIX = "V"
    private const val TAG = "Hardware"
    private var sourceManager : SourceManager? = null

    fun init(context: Context) {
        Log.d(TAG, "Starting Hardware")

        if (isNewline) {
            Log.i(TAG, "Hardware is Newline")
            sourceManager = SourceManager()
            sourceManager?.registerSourceBroadcast(context, object : SourceManager.OnSourceCallback {
                override fun onSourceChanged(p0: Int, p1: Boolean) {
                    IoTSystem.publish(StateData("sourceChanged", Gson().toJson(object {
                        val input: Int = p0
                        val value: Boolean = p1
                    })))
                }

                override fun onX10DBoxChanged(p0: Int) {
                    IoTSystem.publish(StateData("x10DBoxChanged", Gson().toJson(object {
                        val input: Int = p0
                    })))
                }

                override fun onSourceDone() {
                    IoTSystem.publish(StateData("sourceDone", ""))
                }
            })
        }
    }

    var firmwareVersion: String = ""
        get() {
            var firmware = ""
            if (isNewline) {
                try {

                    firmware = SourceManager().firmwareVersion
                } catch (e: Exception) {
                    Log.e(TAG, "Exception on firmware version ${e.message}")
                }
            } else {
                firmware = Objects.toString(Build.VERSION.INCREMENTAL, "")
                firmware = if (firmware.toUpperCase(Locale.ROOT).startsWith(VERSION_PREFIX)) {
                    firmware.substring(VERSION_PREFIX.length)
                } else firmware
            }
            return firmware
        }
        private set

    private val isNewline: Boolean = "HHT".equals(Build.MANUFACTURER, ignoreCase = true)

}