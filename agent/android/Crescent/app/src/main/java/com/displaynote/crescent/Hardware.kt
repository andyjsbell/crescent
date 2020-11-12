package com.displaynote.crescent

import android.os.Build
import android.util.Log
import com.hht.support.display.SourceManager
import java.util.*

object Hardware {
    private const val VERSION_PREFIX = "V"
    private const val TAG = "Hardware"

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

    var isNewline: Boolean = false
        get() = "HHT".equals(Build.MANUFACTURER, ignoreCase = true)
        private set

}