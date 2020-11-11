package com.displaynote.crescent

import android.content.Context
import android.hardware.usb.UsbEndpoint
import android.util.Log
import java.io.File
import java.io.FileNotFoundException
import java.util.*

object IoTSystem {
    private var iot : IoT? = null

    fun init(context: Context, certPath: File?, endpoint: String?, provisioningTemplate: String?) : IoT? {
        if (iot == null) {
            iot = IoT(context, certPath, endpoint, provisioningTemplate)
        }
        return iot
    }

    fun get() : IoT? {
        return iot
    }
}

class IoT(
        private val context: Context,
        private val certPath: File?,
        private val endpoint: String?,
        private val provisioningTemplate: String?
) {
    private lateinit var rootCert : String
    private lateinit var certName : String
    private lateinit var keyName : String
    private lateinit var claimCert : String
    private lateinit var claimKey : String
    private var client : Client? = null

    init {
        Log.d(MainActivity.TAG, "Initialising IoT subsystem")
        loadCerts()
        provisionDevice()
        if (certName.isNotEmpty() && keyName.isNotEmpty()) {
            val cert = File(certPath, certName).readText()
            val key = File(certPath, keyName).readText()
            val clientSettings = ClientSettings(getClientId(certPath), cert, key, rootCert, endpoint.toString())
            client = Client(clientSettings)
            client?.connect()
            Log.d(MainActivity.TAG, "Connected to IoT")
        }
    }

    private fun loadCerts() {
        val applicationContext = context.applicationContext
        rootCert = Util.readTextFileFromAssets(
                context,
                "AmazonRootCA1.pem").toString()

        certPath?.walkTopDown()?.filter { file ->
            file.isFile && file.extension == "crt"
        }?.forEach { certName = it.name }

        certPath?.walkTopDown()?.filter { file ->
            file.isFile && file.extension == "key"
        }?.forEach { keyName = it.name }

        claimCert = Util.readTextFileFromAssets(
                applicationContext,
                context.getString(R.string.claimCert)
        ).toString()

        claimKey = Util.readTextFileFromAssets(
                applicationContext,
                context.getString(R.string.claimKey)
        ).toString()
    }

    private fun provisionDevice() {
        if (certName.isEmpty() || keyName.isEmpty()) {
            val settings = certPath?.let {
                ProvisionSettings(
                        getClientId(certPath),
                        claimCert,
                        claimKey,
                        rootCert,
                        endpoint.toString(),
                        provisioningTemplate.toString(),
                        it
                )
            }
            val provisionClient = settings?.let { ProvisioningClient(it) }
            provisionClient?.provision()
            certName = provisionClient?.certName.toString()
            keyName = provisionClient?.keyName.toString()
        }
    }

    private fun getClientId(certPath: File?): String {
        val idFile = context.getString(R.string.idFile)
        var clientId = UUID.randomUUID().toString()
        try {
            clientId = File(certPath, idFile).readText()
        } catch (e: FileNotFoundException) {
            Log.e(MainActivity.TAG, "id file not found will proceed to create one")
            File(certPath, idFile).printWriter().use { out ->
                out.write(clientId)
            }
        }
        return clientId
    }

    fun subscribe(topic: String, callback: (String) -> Unit) {
        Log.d(TAG, "Subscribing to topic $topic")
        client?.subscribe(topic, callback)
    }

    fun publish(topic: String, message: String) {
        Log.d(TAG, "Publishing to $topic message: $message")
        client?.publish(topic, message)
    }

    companion object {
        const val TAG: String = "IoT"
    }
}