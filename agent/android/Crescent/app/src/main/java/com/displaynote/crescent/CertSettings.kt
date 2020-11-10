package com.displaynote.crescent

import java.io.File

data class CertSettings(val secureCert: String?, val rootCert: String?, val claimCert: String?, val claimKey: String?, val endpoint: String?, val template: String?, val certPath: File?) {
}