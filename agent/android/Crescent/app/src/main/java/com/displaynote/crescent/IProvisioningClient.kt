package com.displaynote.crescent

interface IProvisioningClient {
    interface ProvisioningCallback {
        fun handler()
    }
    fun onMessage(callback: ProvisioningCallback?)
    fun connect(clientId: String?)
    fun subscribe(topic: String?, qos: Int, callback: ProvisioningCallback? )
    fun publish(topic: String?, payload: Any?, qos: Int)
}