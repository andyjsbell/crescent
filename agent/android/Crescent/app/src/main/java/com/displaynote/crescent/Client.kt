package com.displaynote.crescent

import android.util.Log
import software.amazon.awssdk.crt.CRT
import software.amazon.awssdk.crt.CrtRuntimeException
import software.amazon.awssdk.crt.io.ClientBootstrap
import software.amazon.awssdk.crt.io.EventLoopGroup
import software.amazon.awssdk.crt.io.HostResolver
import software.amazon.awssdk.crt.mqtt.MqttClientConnection
import software.amazon.awssdk.crt.mqtt.MqttClientConnectionEvents
import software.amazon.awssdk.crt.mqtt.MqttMessage
import software.amazon.awssdk.crt.mqtt.QualityOfService
import software.amazon.awssdk.iot.AwsIotMqttConnectionBuilder
import java.io.UnsupportedEncodingException
import java.nio.charset.Charset
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException

data class ClientSettings(val clientId: String,
                          val cert: String,
                          val privateKey: String,
                          val rootCert: String,
                          val endpoint: String)

class Client(private val settings: ClientSettings) {

    val TAG: String = "Client"
    private var _connection: MqttClientConnection? = null

    fun disconnect() {
        val disconnected: CompletableFuture<Void> = _connection!!.disconnect()
        disconnected.get()
    }

    fun connect() : Boolean {
        var sessionPresent = false
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

        try {
            EventLoopGroup(1).use { eventLoopGroup ->
                HostResolver(eventLoopGroup).use { resolver ->
                    ClientBootstrap(eventLoopGroup, resolver).use { clientBootstrap ->
                        AwsIotMqttConnectionBuilder.newMtlsBuilder(
                                settings.cert,
                                settings.privateKey
                        ).use { builder ->
                            builder.withCertificateAuthority(settings.rootCert)
                            builder.withBootstrap(clientBootstrap)
                                    .withConnectionEventCallbacks(callbacks)
                                    .withClientId("LFD_${settings.clientId}")
                                    .withEndpoint(settings.endpoint)
                                    .withCleanSession(true)

                            builder.build().use { connection ->
                                _connection = connection
                                // Connect client
                                val connected: CompletableFuture<Boolean> = connection.connect()
                                try {
                                    sessionPresent = connected.get()
                                    Log.d(TAG, "Connected to " + (if (!sessionPresent) "new" else "existing") + " session!")
                                } catch (ex: Exception) {
                                    throw RuntimeException("Exception occurred during connect", ex)
                                }
                            }
                        }
                    }
                }
            }
        } catch (ex: CrtRuntimeException) {
            Log.e(TAG, "Exception encountered: $ex")
        } catch (ex: InterruptedException) {
            Log.e(TAG, "Exception encountered: $ex")
        } catch (ex: ExecutionException) {
            Log.e(TAG, "Exception encountered: $ex")
        }

        return sessionPresent
    }

    fun subscribe(topic: String, callback: (String) -> Unit): Int? {
        val sub: CompletableFuture<Int> = _connection!!.subscribe(topic, QualityOfService.AT_LEAST_ONCE)
        {
            message: MqttMessage ->
            try {
                val payload = String(message.payload, Charset.forName("UTF-8"))
                Log.d(TAG, "TOPIC: ${message.topic} MESSAGE: $payload")
                callback(payload)
            } catch (ex: UnsupportedEncodingException) {
                Log.e(TAG, "Unable to decode payload: " + ex.message)
            }
        }
        return sub.get()
    }

    fun publish(topic: String, message: String): Int? {
        val published: CompletableFuture<Int> = _connection!!.publish(MqttMessage(
                topic,
                message.toByteArray()
        ), QualityOfService.AT_LEAST_ONCE, false)
        return published.get()
    }
}