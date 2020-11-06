package com.displaynote.crescent;

public interface IPermanentClient extends IProvisioningClient {
    public void updateClientCredentials(String permanentCertificate, String permanentCertificateKey);
}
