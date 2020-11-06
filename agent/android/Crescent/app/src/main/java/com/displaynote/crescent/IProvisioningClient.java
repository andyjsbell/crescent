package com.displaynote.crescent;

public interface IProvisioningClient {
    void onMessage(IProvisioningCallback callback);
    void connect(String clientId);
    void subscribe(String topic, int qos, IProvisioningCallback callback);
    void publish(String topic, Object payload, int qos);
}
