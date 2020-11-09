package com.displaynote.crescent

import software.amazon.awssdk.crt.mqtt.MqttClient

open class ProvisioningClient(settings: CertSettings) : IProvisioningClient {
    private val _settings: CertSettings
    private val _mqtt: MqttClient? = null
    private fun initializeClient(iotEndpoint: String, certificatePath: String, rootCertificate: String,
                                 certificate: String, certificateKey: String) {

//        _mqtt = new MqttClient(new ClientBootstrap())

        //        var caCert = X509Certificate.CreateFromCertFile(Path.Join(certificatePath, rootCertificate));
//        var clientCert = _certificateLoader.LoadX509Certificate(certificatePath, certificate, certificateKey);
//
//        MqttClient = new MqttClient(iotEndpoint, 8883, true, caCert, clientCert, MqttSslProtocols.TLSv1_2);
//
//        MqttClient.MqttMsgPublished += ClientOnMqttMsgPublished;
//        MqttClient.MqttMsgSubscribed += ClientOnMqttMsgSubscribed;
//        MqttClient.MqttMsgUnsubscribed += ClientOnMqttMsgUnsubscribed;
//        MqttClient.MqttMsgPublishReceived += ClientOnMqttMsgPublishReceived;
    }

    override fun onMessage(callback: IProvisioningClient.ProvisioningCallback?) {
        TODO("Not yet implemented")
    }

    override fun connect(clientId: String?) {
        TODO("Not yet implemented")
    }

    override fun subscribe(topic: String?, qos: Int, callback: IProvisioningClient.ProvisioningCallback?) {
        TODO("Not yet implemented")
    }

    override fun publish(topic: String?, payload: Any?, qos: Int) {
        TODO("Not yet implemented")
    }

    init {
        _settings = settings
    }
}