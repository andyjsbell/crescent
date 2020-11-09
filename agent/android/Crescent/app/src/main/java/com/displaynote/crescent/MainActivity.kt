package com.displaynote.crescent

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.displaynote.crescent.Util.startJob

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val btn = findViewById<Button>(R.id.start_button)
        btn.setOnClickListener { startJob(applicationContext) }
    }
}