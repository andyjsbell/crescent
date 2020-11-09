package com.displaynote.crescent

interface IPermanentClient : IProvisioningClient {
    fun updateClientCredentials(permanentCertificate: String?, permanentCertificateKey: String?)
}