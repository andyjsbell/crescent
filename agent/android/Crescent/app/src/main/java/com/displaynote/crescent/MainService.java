package com.displaynote.crescent;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.database.ContentObserver;
import android.provider.Settings;
import android.util.Log;

public class MainService extends JobService {
    private static final String TAG = MainService.class.getCanonicalName();

    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        Log.d(TAG, "onStartJob called");

        getContentResolver()
                .registerContentObserver(
                        Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS), false,
                        new ContentObserver(null) {
                            @Override
                            public void onChange(boolean selfChange) {
                                super.onChange(selfChange);
                                int b = Settings.System.getInt(getContentResolver(), android.provider.Settings.System.SCREEN_BRIGHTNESS,-1);
                                Log.d(TAG,"brightness: " + b);
                            }
                        });
        // returning false means the work has been done, return true if the job is being run asynchronously
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        // if the job is prematurely cancelled, do cleanup work here
        Log.d(TAG, "onStopJob called");

        // return true to restart the job
        return false;
    }
}
