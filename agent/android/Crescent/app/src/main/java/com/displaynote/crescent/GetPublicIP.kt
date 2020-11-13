package com.displaynote.crescent

import android.os.AsyncTask
import android.util.Log
import java.io.IOException
import java.net.URL
import java.util.*

class GetPublicIP : AsyncTask<String?, String?, String>() {

    override fun doInBackground(vararg p0: String?): String {
        var publicIP = ""
        try {
            val s = Scanner(
                    URL(
                            "https://api.ipify.org")
                            .openStream(), "UTF-8")
                    .useDelimiter("\\A")
            publicIP = s.next()
            Log.d(TAG, "My current IP address is $publicIP")
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return publicIP
    }

    companion object {
        private const val TAG = "GetPublicIP"
    }
}