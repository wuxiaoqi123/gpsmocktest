package com.welcome.gpsmocktest.app;

import android.app.Application;
import android.content.Context;
import android.os.Vibrator;

import com.baidu.mapapi.SDKInitializer;

public class LocationApplication extends Application {

    public Vibrator mVibrator;

    @Override
    public void onCreate() {
        super.onCreate();
        mVibrator = (Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
        SDKInitializer.initialize(this);
    }
}
