package com.displaynote.crescent

import android.util.EventLog
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

    private val eventGroup = EventLoopGroup(1)
    private val resolver = HostResolver(eventGroup)
    private val clientBootstrap = ClientBootstrap(eventGroup, resolver)
    private var builder: AwsIotMqttConnectionBuilder
    private var connection: MqttClientConnection

    private val callbacks: MqttClientConnectionEvents = object : MqttClientConnectionEvents {
        override fun onConnectionInterrupted(errorCode: Int) {
            if (errorCode != 0) {
                Log.e(Companion.TAG, "Connection interrupted: " + errorCode + ": " + CRT.awsErrorString(errorCode))
            }
        }

        override fun onConnectionResumed(sessionPresent: Boolean) {
            Log.d(Companion.TAG, "Connection resumed: " + if (sessionPresent) "existing session" else "clean session")
        }
    }

    init {
        builder = AwsIotMqttConnectionBuilder.newMtlsBuilder(
                settings.cert,
                settings.privateKey
        )
        builder.withCertificateAuthority(settings.rootCert)
        builder.withBootstrap(clientBootstrap)
                .withConnectionEventCallbacks(callbacks)
                .withClientId("${Companion.precursor}${settings.clientId}")
                .withEndpoint(settings.endpoint)
                .withCleanSession(false)

        connection = builder.build()
    }

    fun connect() {
        // Connect client
        val connected: CompletableFuture<Boolean> = connection.connect()
        try {
            val sessionPresent = connected.get()
            Log.d(Companion.TAG, "Connected to " + (if (!sessionPresent) "new" else "existing") + " session!")
        } catch (ex: Exception) {
            throw RuntimeException("Exception occurred during connect", ex)
        }
    }

    fun subscribe(topic: String, callback: (String) -> Unit) {
        val sub: CompletableFuture<Int> = connection.subscribe(topic, QualityOfService.AT_LEAST_ONCE)
        { message: MqttMessage ->
            try {
                val payload = String(message.payload, Charset.forName("UTF-8"))
                Log.d(Companion.TAG, "TOPIC: ${message.topic} MESSAGE: $payload")
                callback(payload)
            } catch (ex: UnsupportedEncodingException) {
                Log.e(Companion.TAG, "Unable to decode payload: " + ex.message)
            }
        }
        sub.get()
    }

    fun publish(topic: String, message: String) {
        val published: CompletableFuture<Int> = connection.publish(MqttMessage(
                topic,
                message.toByteArray()
        ), QualityOfService.AT_LEAST_ONCE, false)

        published.get()
    }

    fun disconnect() {
        val disconnected = connection.disconnect()
        disconnected.get()
    }

    companion object {
        private const val TAG: String = "Client"
        private const val precursor = "LFD_"
    }
}