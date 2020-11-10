package com.displaynote.crescent

import android.app.Application
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.displaynote.crescent.Util.startJob
import java.util.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val btn = findViewById<Button>(R.id.start_button)
        btn.setOnClickListener {
            startJob(applicationContext)
            val rootCert = Util.readTextFileFromAssets(applicationContext, "AmazonRootCA1.pem")
            val claimCert = Util.readTextFileFromAssets(applicationContext, "bb36ce9517-certificate.pem.crt")
            val claimKey = Util.readTextFileFromAssets(applicationContext, "bb36ce9517-private.pem.key")
            val endpoint = getString(R.string.endpoint)
            val provisioningTemplate = getString(R.string.provisioningTemplate)
            val certPath = applicationContext.filesDir
            val settings = CertSettings("", rootCert, claimCert, claimKey, endpoint, provisioningTemplate, certPath)
            val clientId = UUID.randomUUID().toString()
            ProvisioningClient(settings, clientId)
        }
    }
}