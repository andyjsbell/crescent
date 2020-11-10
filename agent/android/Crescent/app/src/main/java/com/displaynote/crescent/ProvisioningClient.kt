package com.displaynote.crescent

import android.R.id.message
import android.app.Application
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


open class ProvisioningClient(private val settings: CertSettings, private val clientId: String, private val serialNumber: String) {

    init {
        initializeClient()
    }

    private fun initializeClient() {

        val callbacks: MqttClientConnectionEvents = object : MqttClientConnectionEvents {
            override fun onConnectionInterrupted(errorCode: Int) {
                if (errorCode != 0) {
                    println("Connection interrupted: " + errorCode + ": " + CRT.awsErrorString(errorCode))
                }
            }

            override fun onConnectionResumed(sessionPresent: Boolean) {
                println("Connection resumed: " + if (sessionPresent) "existing session" else "clean session")
            }

        }

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
                                    println("Connected to " + (if (!sessionPresent) "new" else "existing") + " session!")
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
                                            println("TOPIC: ${message.topic} MESSAGE: $payload")

                                            when {
                                                payload.contains("certificateId") -> {
                                                    var map: Map<String, Any> = HashMap()
                                                    map = Gson().fromJson(payload, map.javaClass)
                                                    val certificateId = map["certificateId"].toString()
                                                    val keyRoot = certificateId.substring(0, 10)
                                                    val certName = "${keyRoot}-certificate.pem.crt"
                                                    val certPem = map["certificatePem"]
                                                    File(settings.certPath, certName).printWriter().use { out ->
                                                        out.write(certPem.toString())
                                                    }
                                                    val keyName = "${keyRoot}-private.pem.key"
                                                    val privateKey = map["privateKey"]
                                                    File(settings.certPath, keyName).printWriter().use { out ->
                                                        out.write(privateKey.toString())
                                                    }

                                                    val ownershipToken = map["certificateOwnershipToken"].toString()

                                                    // register the device
                                                    val registerTemplate = "{\"certificateOwnershipToken\": ${ownershipToken}, \"parameters\": {\"SerialNumber\": ${serialNumber}}}"
                                                    val published: CompletableFuture<Int> = connection.publish(MqttMessage("\$aws/provisioning-templates/${settings.template}/provision/json", registerTemplate.toByteArray()), QualityOfService.AT_LEAST_ONCE, false)
                                                    published.get()

                                                }
                                                payload.contains("deviceConfiguration") -> {

                                                }
                                                payload.contains("service_response") -> {

                                                }
                                            }


                                        } catch (ex: UnsupportedEncodingException) {
                                            println("Unable to decode payload: " + ex.message)
                                        }
                                    }
                                    sub.get()
                                }

                                val topic = "\$aws/certificates/create/json"
                                val published: CompletableFuture<Int> = connection.publish(MqttMessage(topic, "{}".toByteArray()), QualityOfService.AT_LEAST_ONCE, false)
                                published.get()
//
//                                while (true) {
//
//                                }

                                Thread.sleep(5000);
                                val disconnected: CompletableFuture<Void> = connection.disconnect()
                                disconnected.get()
                            }
                        }
                    }
                }
            }
        } catch (ex: CrtRuntimeException) {
            println("Exception encountered: $ex")
        } catch (ex: InterruptedException) {
            println("Exception encountered: $ex")
        } catch (ex: ExecutionException) {
            println("Exception encountered: " + ex.toString())
        }
    }
}