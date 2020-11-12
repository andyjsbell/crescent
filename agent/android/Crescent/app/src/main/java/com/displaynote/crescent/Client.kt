package com.displaynote.crescent

import android.util.Log
import software.amazon.awssdk.crt.CRT
import software.amazon.awssdk.crt.io.ClientBootstrap
import software.amazon.awssdk.crt.io.EventLoopGroup
import software.amazon.awssdk.crt.io.HostResolver
import software.amazon.awssdk.crt.mqtt.MqttClientConnection
import software.amazon.awssdk.crt.mqtt.MqttClientConnectionEvents
import software.amazon.awssdk.crt.mqtt.MqttMessage
import software.amazon.awssdk.crt.mqtt.QualityOfService
import software.amazon.awssdk.iot.AwsIotMqttConnectionBuilder
import software.amazon.awssdk.iot.iotshadow.IotShadowClient
import software.amazon.awssdk.iot.iotshadow.model.*
import java.io.UnsupportedEncodingException
import java.nio.charset.Charset
import java.util.concurrent.CompletableFuture

data class ClientSettings(val clientId: String,
                          val cert: String,
                          val privateKey: String,
                          val rootCert: String,
                          val endpoint: String)


class Client(private val settings: ClientSettings, private val ip: String) {

    private lateinit var gotResponse: CompletableFuture<Any>
    private val eventGroup = EventLoopGroup(1)
    private val resolver = HostResolver(eventGroup)
    private val clientBootstrap = ClientBootstrap(eventGroup, resolver)
    private var builder: AwsIotMqttConnectionBuilder = AwsIotMqttConnectionBuilder.newMtlsBuilder(
            settings.cert,
            settings.privateKey
    )
    private var connection: MqttClientConnection
    private var shadow : IotShadowClient
    private var thingName : String
    private var localValue : HashMap<String, Any>? = null
    private var defaultValue : HashMap<String, Any>

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
        val clientId = "${Companion.precursor}${settings.clientId}"
        builder.withCertificateAuthority(settings.rootCert)
        builder.withBootstrap(clientBootstrap)
                .withConnectionEventCallbacks(callbacks)
                .withClientId(clientId)
                .withEndpoint(settings.endpoint)
                .withCleanSession(true)

        connection = builder.build()
        shadow = IotShadowClient(connection);

        defaultValue = hashMapOf(
                Model to android.os.Build.MODEL,
                Location to ip,
                Firmware to Hardware.firmwareVersion
        )

        // Classic thing
        thingName = clientId
    }

    private fun onGetShadowAccepted(response: GetShadowResponse) {
        Log.d(TAG, "Received initial shadow state")
        if (response.state != null && localValue == null) {
            gotResponse.complete(null)
            if (response.state.delta != null) {
                val value = response.state.delta.toString()
                Log.d(TAG, "Shadow delta value: $value")
            }
            if (response.state.reported != null) {
                val value = response.state.reported.toString()
                Log.d(TAG, "Shadow reported value: $value")
                // Initialize local value to match the reported shadow value
                localValue = response.state.reported
            }
        } else {
            Log.d(TAG, "Shadow document has no value setting default...")
            changeShadowValue(defaultValue)
        }
    }

    private fun onGetShadowRejected(response: ErrorResponse) {
        if (response.code == 404) {
            Log.d(TAG, "Shadow document has no value setting default...")
            changeShadowValue(defaultValue)
            return
        }
        gotResponse.complete(null)
        Log.e(TAG, "GetShadow request was rejected: code: $response.code.toString() message: $response.message")
    }

    private fun onShadowDeltaUpdated(response: ShadowDeltaUpdatedEvent) {
        Log.d(TAG, "Shadow delta updated")
//        if (response.state != null && response.state.containsKey(SHADOW_PROPERTY)) {
//            val value = response.state[SHADOW_PROPERTY].toString()
//            println("  Delta wants to change value to '$value'. Changing local value...")
//            changeShadowValue(value)
//        } else {
//            Log.d(TAG, "  Delta did not report a change in $SHADOW_PROPERTY")
//        }
    }

    private fun onUpdateShadowAccepted(response: UpdateShadowResponse) {
        val value = response.state.reported.toString()
        Log.d(TAG, "Shadow updated, value is $value")
        gotResponse.complete(null)
    }

    private fun onUpdateShadowRejected(response: ErrorResponse) {
        Log.e(TAG, "Shadow update was rejected: code: $response.code.toString() message: $response.message")
    }

    fun changeShadowValue(map: HashMap<String, Any>) {
        for ((key, value) in map) {
            changeShadowValue(key, value.toString())
        }
    }

    fun changeShadowValue(key: String, value: String): CompletableFuture<Void?>? {
        if (localValue?.get(key) === value) {
            Log.d(TAG, "Local value for $key is already $value")
            val result = CompletableFuture<Void?>()
            result.complete(null)
            return result
        }
        Log.d(TAG, "Changed local value for $key to $value")
        localValue?.put(key, value)
        Log.d(TAG, "Updating shadow value for $key to $value")

        // build a request to let the service know our current value and desired value, and that we only want
        // to update if the version matches the version we know about
        val request = UpdateShadowRequest()
        request.thingName = thingName
        request.state = ShadowState()
        request.state.reported = hashMapOf(key to value)
        request.state.desired = hashMapOf(key to value)

        // Publish the request
        return shadow.PublishUpdateShadow(request, QualityOfService.AT_LEAST_ONCE).thenRun {
            Log.e(TAG, "Update request published")
        }.exceptionally { ex: Throwable ->
            Log.e(TAG, "Update request failed: $ex.message")
            null
        }
    }

    private fun subscribeToShadowEvents() {
        Log.d(TAG, "Subscribing to shadow delta events...")
        val requestShadowDeltaUpdated = ShadowDeltaUpdatedSubscriptionRequest()
        requestShadowDeltaUpdated.thingName = thingName
        val subscribedToDeltas = shadow.SubscribeToShadowDeltaUpdatedEvents(
                requestShadowDeltaUpdated,
                QualityOfService.AT_LEAST_ONCE,
                ::onShadowDeltaUpdated)
        subscribedToDeltas.get()

        Log.d(TAG, "Subscribing to update respones...")
        val requestUpdateShadow = UpdateShadowSubscriptionRequest()
        requestUpdateShadow.thingName = thingName
        val subscribedToUpdateAccepted = shadow.SubscribeToUpdateShadowAccepted(
                requestUpdateShadow,
                QualityOfService.AT_LEAST_ONCE,
                ::onUpdateShadowAccepted)
        val subscribedToUpdateRejected = shadow.SubscribeToUpdateShadowRejected(
                requestUpdateShadow,
                QualityOfService.AT_LEAST_ONCE,
                ::onUpdateShadowRejected)
        subscribedToUpdateAccepted.get()
        subscribedToUpdateRejected.get()

        Log.d(TAG, "Subscribing to get responses...")
        val requestGetShadow = GetShadowSubscriptionRequest()
        requestGetShadow.thingName = thingName
        val subscribedToGetShadowAccepted = shadow.SubscribeToGetShadowAccepted(
                requestGetShadow,
                QualityOfService.AT_LEAST_ONCE,
                ::onGetShadowAccepted)
        val subscribedToGetShadowRejected = shadow.SubscribeToGetShadowRejected(
                requestGetShadow,
                QualityOfService.AT_LEAST_ONCE,
                ::onGetShadowRejected)
        subscribedToGetShadowAccepted.get()
        subscribedToGetShadowRejected.get()

        gotResponse = CompletableFuture<Any>()
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

        shadow()
    }

    private fun shadow() {

        subscribeToShadowEvents()

        Log.d(TAG, "Requesting current shadow state...")
        val getShadowRequest = GetShadowRequest()
        getShadowRequest.thingName = thingName
        val publishedGetShadow = shadow.PublishGetShadow(
                getShadowRequest,
                QualityOfService.AT_LEAST_ONCE)
        publishedGetShadow.get()
        gotResponse.get()

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
        public const val Model = "model"
        public const val Location = "location"
        public const val Firmware = "firmware"
    }
}