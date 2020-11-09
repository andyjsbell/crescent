package com.displaynote.crescent

data class CertSettings(val secureCert: String, val rootCert: String, val claimCert: String, val claimKey: String, val endpoint: String, val template: String) {
}