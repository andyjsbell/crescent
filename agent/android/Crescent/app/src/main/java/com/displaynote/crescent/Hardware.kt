package com.displaynote.crescent

import android.content.Context
import android.os.Build
import android.util.Log
import com.google.gson.Gson
import com.hht.support.broadcast.IUsbView
import com.hht.support.broadcast.UsbStateBroadcastReceiver
import com.hht.support.display.SourceManager
import com.hht.support.model.*
import com.hht.support.panel.OEMPanel
import com.hht.support.utils.AndroidUtils
import java.util.*

object Hardware {
    private const val VERSION_PREFIX = "V"
    private const val TAG = "Hardware"
    private var sourceManager : SourceManager? = null
    private var usbStateReceiver : UsbStateBroadcastReceiver? = null

    fun init(context: Context) {
        Log.d(TAG, "Starting Hardware")

        if (isNewline) {
            OEMPanel.init(context);
            Log.i(TAG, "Hardware is Newline")
            sourceManager = SourceManager()
            sourceManager?.registerSourceBroadcast(context, object : SourceManager.OnSourceCallback {
                override fun onSourceChanged(p0: Int, p1: Boolean) {
                    val stateData = IoTSystem.stateData
                    val signal = Signal()
                    signal.OPS = sourceManager?.isSourceWithSignal(SourceOPS())
                    signal.Front = sourceManager?.isSourceWithSignal(SourceFront())
                    signal.HDMI1 = sourceManager?.isSourceWithSignal(SourceHDMI1())
                    signal.HDMI2 = sourceManager?.isSourceWithSignal(SourceHDMI2())
                    signal.HDMI3 = sourceManager?.isSourceWithSignal(SourceHDMI3())
                    signal.DP = sourceManager?.isSourceWithSignal(SourceDP())
                    signal.VGA = sourceManager?.isSourceWithSignal(SourceVGA())
                    stateData.signal = signal
                    IoTSystem.publish(stateData)
                }

                override fun onX10DBoxChanged(p0: Int) {
                    IoTSystem.publish(MessageData("x10DBoxChanged", Gson().toJson(object {
                        val input: Int = p0
                    })))
                }

                override fun onSourceDone() {
                    IoTSystem.publish(MessageData("sourceDone", ""))
                }
            })

            usbStateReceiver = UsbStateBroadcastReceiver(object : IUsbView {
                override fun attachUsb(p0: String?) {
                    val stateData = IoTSystem.stateData
                    stateData.usb = AndroidUtils.isUSBAllocation(context)
                    IoTSystem.publish(stateData)
                    IoTSystem.publish(MessageData("attachUsb", p0.toString()))
                }

                override fun detachedUsb(p0: String?) {
                    val stateData = IoTSystem.stateData
                    stateData.usb = AndroidUtils.isUSBAllocation(context)
                    IoTSystem.publish(stateData)
                    IoTSystem.publish(MessageData("detachUsb", p0.toString()))
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