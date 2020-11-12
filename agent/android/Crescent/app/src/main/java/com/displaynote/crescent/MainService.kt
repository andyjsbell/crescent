package com.displaynote.crescent

import android.app.job.JobParameters
import android.app.job.JobService
import android.database.ContentObserver
import android.provider.Settings
import android.util.Log

class MainService : JobService() {
    override fun onStartJob(jobParameters: JobParameters): Boolean {
        Log.d(TAG, "onStartJob called")
        IoTSystem.publish(StateData("job", "start"))
        contentResolver
                .registerContentObserver(
                        Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS), false,
                        object : ContentObserver(null) {
                            override fun onChange(selfChange: Boolean) {
                                super.onChange(selfChange)
                                val b = Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, -1)
                                val data = StateData("brightness", b.toString())
                                IoTSystem.publish(data)
                            }
                        })
        // returning false means the work has been done, return true if the job is being run asynchronously
        return true
    }

    override fun onStopJob(jobParameters: JobParameters): Boolean {
        // if the job is prematurely cancelled, do cleanup work here
        Log.d(TAG, "onStopJob called")
        IoTSystem.publish(StateData("job", "stop"))
        // return true to restart the job
        return false
    }

    companion object {
        private const val TAG = "MainService"
    }
}