package com.displaynote.crescent

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileNotFoundException
import java.util.*
import com.google.gson.Gson

data class AccessibilityData(val name: String, val time: Long, val type: Int) {}
data class StateData(val name: String, val value: String) {}
data class MessageData(val name: String, val value: String) {}

object IoTSystem {
    private var iot : IoT? = null
    const val TAG: String = "IoTSystem"

    fun init(context: Context) {
        Log.d(TAG, "Starting IoTSystem")
        if (iot == null) {
            val certPath = context.filesDir
            val endpoint = context.getString(R.string.endpoint)
            val provisioningTemplate = context.getString(R.string.provisioningTemplate)
            iot = IoT(context, certPath, endpoint, provisioningTemplate)
        }
    }

    fun publish(data: AccessibilityData) {
        iot?.publish("topic/device/accessibility", Gson().toJson(data))
    }

    fun publish(data: StateData) {
        iot?.publish("topic/device/state", Gson().toJson(data))
    }

    fun publish(data: MessageData) {
        iot?.publish("topic/device/messages", Gson().toJson(data))
    }

    fun subscribe(callback: (String) -> Unit) {
        iot?.subscribe("topic/device/messages", callback)
    }
}

internal class IoT(
        private val context: Context,
        private val certPath: File?,
        private val endpoint: String?,
        private val provisioningTemplate: String?
) {
    private lateinit var rootCert : String
    private var certName : String? = null
    private var keyName : String? = null
    private lateinit var claimCert : String
    private lateinit var claimKey : String
    private var client : Client? = null

    init {
        Log.d(TAG, "Initialising IoT subsystem")
        loadCerts()
        provisionDevice()
        val cert = File(certPath, certName.toString()).readText()
        val key = File(certPath, keyName.toString()).readText()
        val clientSettings = ClientSettings(getClientId(certPath), cert, key, rootCert, endpoint.toString())
        val ipAddress = GetPublicIP().execute().get()
        client = Client(clientSettings, ipAddress)
        client?.connect()

        updateShadow()
        Log.d(TAG, "Connected to IoT")
    }

    private fun updateShadow() {
        Log.d(TAG, "update shadow values")
        client?.changeShadowValue(Client.Location, GetPublicIP().execute().get())
        client?.changeShadowValue(Client.Firmware, Hardware.firmwareVersion)
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
        if (certName.isNullOrEmpty() || keyName.isNullOrEmpty()) {
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