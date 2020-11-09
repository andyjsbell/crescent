package com.displaynote.crescent

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

object Util {
    @JvmStatic
    fun startJob(context: Context) {
        val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
        val jobInfo = JobInfo.Builder(11, ComponentName(context, MainService::class.java)) // only add if network access is required
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .build()
        jobScheduler.schedule(jobInfo)
    }

    fun readTextFileFromAssets(context: Context, filename: String?): String? {
        var reader: BufferedReader? = null
        try {
            reader = BufferedReader(
                    InputStreamReader(context.assets.open(filename!!), "UTF-8"))
            val sb = StringBuilder()
            // do reading, usually loop until end of file reading
            var mLine: String?
            while (reader.readLine().also { mLine = it } != null) {
                sb.append(mLine).append("\n")
            }
            return sb.toString()
        } catch (e: IOException) {
            //log the exception
        } finally {
            if (reader != null) {
                try {
                    reader.close()
                } catch (e: IOException) {
                    //log the exception
                }
            }
        }

        return null
    }
}