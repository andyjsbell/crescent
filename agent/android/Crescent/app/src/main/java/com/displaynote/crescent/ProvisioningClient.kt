package com.displaynote.crescent

import android.R.id.message
import android.app.Application
import android.util.Log
import com.google.gson.Gson
import software.amazon.awssdk.crt.CRT
import software.amazon.awssdk.crt.CrtRuntimeException
import software.amazon.awssdk.crt.io.ClientBootstrap
import software.amazon.awssdk.crt.io.EventLoopGroup
import software.amazon.awssdk.crt.io.HostResolver
import software.amazon.awssdk.crt.mqtt.MqttClientConnectionEvents
import software.amazon.awssdk.crt.mqtt.MqttMessage
import software.amazon.awssdk.crt.mqtt.QualityOfService
import software.amazon.awssdk.iot.AwsIotMqttConnectionBuilder
import java.io.File
import java.io.UnsupportedEncodingException
import java.nio.charset.Charset
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException


open class ProvisioningClient(private val settings: CertSettings, private val clientId: String) {

    val TAG : String = "ProvisioningClient"

    init {
        initializeClient()
    }

    private fun initializeClient() {

        Log.d(TAG, "initializeClient")
        val callbacks: MqttClientConnectionEvents = object : MqttClientConnectionEvents {
            override fun onConnectionInterrupted(errorCode: Int) {
                if (errorCode != 0) {
                    Log.e(TAG, "Connection interrupted: " + errorCode + ": " + CRT.awsErrorString(errorCode))
                }
            }

            override fun onConnectionResumed(sessionPresent: Boolean) {
                Log.d(TAG, "Connection resumed: " + if (sessionPresent) "existing session" else "clean session")
            }

        }

        var completed = false
        var ownershipToken : String? = null

        try {
            EventLoopGroup(1).use { eventLoopGroup ->
                HostResolver(eventLoopGroup).use { resolver ->
                    ClientBootstrap(eventLoopGroup, resolver).use { clientBootstrap ->
                        AwsIotMqttConnectionBuilder.newMtlsBuilder(settings.claimCert, settings.claimKey).use { builder ->
                            builder.withCertificateAuthority(settings.rootCert)
                            builder.withBootstrap(clientBootstrap)
                                    .withConnectionEventCallbacks(callbacks)
                                    .withClientId(clientId)
                                    .withEndpoint(settings.endpoint)
                                    .withCleanSession(true)

                            builder.build().use { connection ->

                                // Connect client
                                val connected: CompletableFuture<Boolean> = connection.connect()
                                try {
                                    val sessionPresent: Boolean = connected.get()
                                    Log.d(TAG, "Connected to " + (if (!sessionPresent) "new" else "existing") + " session!")
                                } catch (ex: Exception) {
                                    throw RuntimeException("Exception occurred during connect", ex)
                                }

                                // Subscribe to pertinent IoTCore topics that would emit errors
                                val provisionTemplateRejectedTopic = "\$aws/provisioning-templates/${settings.template}/provision/json/rejected"
                                val createCertRejectedTopic = "\$aws/certificates/create/json/rejected"
                                val provisionTemplateAcceptedTopic = "\$aws/provisioning-templates/${settings.template}/provision/json/accepted"
                                val createCertAcceptedTopic = "\$aws/certificates/create/json/accepted"

                                val topics = arrayOf(provisionTemplateRejectedTopic, createCertRejectedTopic, provisionTemplateAcceptedTopic, createCertAcceptedTopic)
                                for (topic in topics) {
                                    val sub: CompletableFuture<Int> = connection.subscribe(topic, QualityOfService.AT_LEAST_ONCE) { message: MqttMessage ->
                                        try {
                                            val payload = String(message.payload, Charset.forName("UTF-8"))
                                            Log.d(TAG,"TOPIC: ${message.topic} MESSAGE: $payload")

                                            when {
                                                payload.contains("certificateId") -> {
                                                    Log.d(TAG, "certificateId found, now creating cert and key files")
                                                    var map: Map<String, Any> = HashMap()
                                                    map = Gson().fromJson(payload, map.javaClass)
                                                    val certificateId = map["certificateId"].toString()
                                                    val keyRoot = certificateId.substring(0, 10)
                                                    Log.d(TAG, "using root name $keyRoot")
                                                    val certName = "${keyRoot}-certificate.pem.crt"
                                                    val certPem = map["certificatePem"]
                                                    File(settings.certPath, certName).printWriter().use { out ->
                                                        out.write(certPem.toString())
                                                    }
                                                    Log.d(TAG, "Written cert")

                                                    val keyName = "${keyRoot}-private.pem.key"
                                                    val privateKey = map["privateKey"]
                                                    File(settings.certPath, keyName).printWriter().use { out ->
                                                        out.write(privateKey.toString())
                                                    }
                                                    Log.d(TAG, "Written key")
                                                    ownershipToken = map["certificateOwnershipToken"].toString()
                                                    Log.d(TAG, "Store ownershipToken to publish and register thing: $ownershipToken")
                                                }
                                                payload.contains("deviceConfiguration") -> {
                                                    // validate certs
                                                    Log.d(TAG, "deviceConfiguration located")
                                                }
                                                payload.contains("service_response") -> {
                                                    Log.d(TAG, "service_response located")
                                                }
                                            }
                                        } catch (ex: UnsupportedEncodingException) {
                                            Log.e(TAG,"Unable to decode payload: " + ex.message)
                                        }
                                    }
                                    sub.get()
                                }

                                val topic = "\$aws/certificates/create/json"
                                val published: CompletableFuture<Int> = connection.publish(MqttMessage(topic, "{}".toByteArray()), QualityOfService.AT_LEAST_ONCE, false)
                                published.get()
                                Log.d(TAG, "Published to $topic")

                                while (!completed) {
                                    Thread.sleep(100)
                                    if (ownershipToken != null) {
                                        // register the device
                                        val registerTemplate = "{\"certificateOwnershipToken\":\"${ownershipToken}\", \"parameters\":{\"SerialNumber\":\"${clientId}\"}}"
                                        ownershipToken = null
                                        Log.d(TAG, "Registering thing: $registerTemplate")
                                        val templateTopic = "\$aws/provisioning-templates/${settings.template}/provision/json"
                                        Log.d(TAG, "template topic: $templateTopic")
                                        val pubTemplate: CompletableFuture<Int> = connection.publish(MqttMessage(templateTopic, registerTemplate.toByteArray()), QualityOfService.AT_LEAST_ONCE, false)
                                        pubTemplate.get()
                                        Log.d(TAG, "Registered thing!")
                                    }
                                }
                                Log.d(TAG, "Disconnecting")

                                val disconnected: CompletableFuture<Void> = connection.disconnect()
                                disconnected.get()
                            }
                        }
                    }
                }
            }
        } catch (ex: CrtRuntimeException) {
            Log.e(TAG,"Exception encountered: $ex")
        } catch (ex: InterruptedException) {
            Log.e(TAG,"Exception encountered: $ex")
        } catch (ex: ExecutionException) {
            Log.e(TAG, "Exception encountered: $ex")
        }
    }
}