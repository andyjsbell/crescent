package com.displaynote.crescent

import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.displaynote.crescent.Util.startJob
import java.io.File
import java.io.FileNotFoundException
import java.util.*

class MainActivity : AppCompatActivity() {

    private fun onSubscribeReceived(payload: String) {
        Log.d(Companion.TAG, "payload received: $payload")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val btn = findViewById<Button>(R.id.start_button)

        val certPath = applicationContext.filesDir
        val endpoint = getString(R.string.endpoint)
        val provisioningTemplate = getString(R.string.provisioningTemplate)

        IoTSystem.init(applicationContext, certPath, endpoint, provisioningTemplate)
        IoTSystem.subscribe(::onSubscribeReceived)

        btn.setOnClickListener {
            // Say hello on click
            IoTSystem.publish(StateData("message", "hello"))
        }

        startJob(applicationContext)
    }

    companion object {
        const val TAG: String = "MainActivity"
    }
}