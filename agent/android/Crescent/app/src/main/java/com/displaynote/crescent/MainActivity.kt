package com.displaynote.crescent

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.displaynote.crescent.Util.startJob
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val btn = findViewById<Button>(R.id.start_button)

        try {
            IoTSystem.init(applicationContext)
            IoTSystem.subscribe {
                Log.d(TAG, "payload received: $it")
                try {
                    val data = Gson().fromJson(it, MessageData::class.java)
                    when (data?.name) {
                        "message" -> {
                            Log.i(TAG, data.value)
                        }
                        "alert" -> {
                            Log.i(TAG, data.value)
                            // TODO Not sure why this doesn't work
                            runOnUiThread { Toast.makeText(this@MainActivity, data.value, Toast.LENGTH_LONG) }
                        }
                    }
                } catch (e: JsonSyntaxException) {
                    Log.e(TAG, "Syntax error in JSON")
                }
            }
        } catch (re: RuntimeException) {
            Log.e(TAG, "Runtime exception: ${re.message}")
        }

        startJob(applicationContext)

        btn.setOnClickListener {
            // Say hello on click
            IoTSystem.publish(MessageData("message", "hello"))
        }

        IoTSystem.publish(MessageData("power", "awake"))
    }

    companion object {
        const val TAG: String = "MainActivity"
    }
}