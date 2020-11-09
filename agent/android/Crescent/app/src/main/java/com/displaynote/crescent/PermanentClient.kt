package com.displaynote.crescent

class PermanentClient(settings: CertSettings) : ProvisioningClient(settings), IPermanentClient {
    override fun updateClientCredentials(permanentCertificate: String?, permanentCertificateKey: String?) {}
}