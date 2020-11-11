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
    val TAG: String = "MainActivity"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val btn = findViewById<Button>(R.id.start_button)
        btn.setOnClickListener {
            startJob(applicationContext)
            val certPath = applicationContext.filesDir
            val idFile = getString(R.string.idFile)
            val rootCert = Util.readTextFileFromAssets(
                    applicationContext,
                    "AmazonRootCA1.pem"
            )
            val endpoint = getString(R.string.endpoint)
            var clientId = UUID.randomUUID().toString()
            try {
                clientId = File(certPath, idFile).readText()
            } catch (e: FileNotFoundException) {
                Log.e(TAG, "id file not found will proceed to create one")
                File(certPath, idFile).printWriter().use { out ->
                    out.write(clientId)
                }
            }
            // Check if we need to provision or use device cert and key
            var certName: String? = null
            certPath.walkTopDown().filter { file ->
                file.isFile && file.extension == "crt"
            }.forEach { certName = it.name }

            var keyName: String? = null
            certPath.walkTopDown().filter { file ->
                file.isFile && file.extension == "key"
            }.forEach { keyName = it.name }

            val claimCert = Util.readTextFileFromAssets(
                    applicationContext,
                    getString(R.string.claimCert)
            )
            val claimKey = Util.readTextFileFromAssets(
                    applicationContext,
                    getString(R.string.claimKey)
            )
            val provisioningTemplate = getString(R.string.provisioningTemplate)
            val settings = ProvisionSettings(
                    clientId,
                    claimCert!!,
                    claimKey!!,
                    rootCert!!,
                    endpoint,
                    provisioningTemplate,
                    certPath
            )

            if (certName.isNullOrEmpty() || keyName.isNullOrEmpty()) {
                val provisionClient = ProvisioningClient(settings)
                provisionClient.provision()
                certName = provisionClient.certName
                keyName = provisionClient.keyName
            }

            if (certName!!.isNotEmpty() && keyName!!.isNotEmpty()) {
                val cert = File(settings.certPath, certName!!).readText()
                val key = File(settings.certPath, keyName!!).readText()
                val clientSettings = ClientSettings(clientId, cert, key, rootCert, endpoint)
                val client = Client(clientSettings)
                client.subscribe("topic") { payload ->
                    Log.d(TAG, "payload received: $payload")
                }
                client.publish("topic", "{\"message\":\"hello\"}")
            }
        }
    }
}