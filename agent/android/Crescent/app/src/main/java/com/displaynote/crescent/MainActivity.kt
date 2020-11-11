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

    private lateinit var certPath : File
    private lateinit var endpoint : String
    private lateinit var provisioningTemplate : String

    private lateinit var rootCert : String
    private lateinit var certName : String
    private lateinit var keyName : String
    private lateinit var claimCert : String
    private lateinit var claimKey : String
    private var client : Client? = null

    private fun loadCerts() {
        rootCert = Util.readTextFileFromAssets(
                applicationContext,
                "AmazonRootCA1.pem").toString()

        certPath.walkTopDown().filter { file ->
            file.isFile && file.extension == "crt"
        }.forEach { certName = it.name }

        certPath.walkTopDown().filter { file ->
            file.isFile && file.extension == "key"
        }.forEach { keyName = it.name }

        claimCert = Util.readTextFileFromAssets(
                applicationContext,
                getString(R.string.claimCert)
        ).toString()

        claimKey = Util.readTextFileFromAssets(
                applicationContext,
                getString(R.string.claimKey)
        ).toString()
    }

    private fun provisionDevice() {
        if (certName.isEmpty() || keyName.isEmpty()) {
            val settings = ProvisionSettings(
                    getClientId(certPath),
                    claimCert,
                    claimKey,
                    rootCert,
                    endpoint,
                    provisioningTemplate,
                    certPath
            )
            val provisionClient = ProvisioningClient(settings)
            provisionClient.provision()
            certName = provisionClient.certName.toString()
            keyName = provisionClient.keyName.toString()
        }
    }

    private fun initIot() {
        Log.d(TAG, "Initialising IoT subsystem")
        certPath = applicationContext.filesDir
        endpoint = getString(R.string.endpoint)
        provisioningTemplate = getString(R.string.provisioningTemplate)
        loadCerts()
        provisionDevice()
        if (certName.isNotEmpty() && keyName.isNotEmpty()) {
            val cert = File(certPath, certName).readText()
            val key = File(certPath, keyName).readText()
            val clientSettings = ClientSettings(getClientId(certPath), cert, key, rootCert, endpoint)
            client = Client(clientSettings)
            client?.connect()
            client?.subscribe("topic", ::onSubscribeReceived)
            Log.d(TAG, "Connected and subscribed to IoT")
        }
    }
    private fun onSubscribeReceived(payload: String) {
        Log.d(Companion.TAG, "payload received: $payload")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val btn = findViewById<Button>(R.id.start_button)

        startJob(applicationContext)
        initIot()

        btn.setOnClickListener {
            client?.publish("topic", "{\"message\":\"hello\"}")
        }
    }

    private fun getClientId(certPath: File?): String {
        val idFile = getString(R.string.idFile)
        var clientId = UUID.randomUUID().toString()
        try {
            clientId = File(certPath, idFile).readText()
        } catch (e: FileNotFoundException) {
            Log.e(TAG, "id file not found will proceed to create one")
            File(certPath, idFile).printWriter().use { out ->
                out.write(clientId)
            }
        }
        return clientId
    }

    companion object {
        const val TAG: String = "MainActivity"
    }
}