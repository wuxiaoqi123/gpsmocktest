package com.welcome.gpsmocktest.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;

public class MockGpsService extends Service {

    public static final int RunCode = 0x01;
    public static final int StopCode = 0x02;
    public static final String ACTION = "com.welcome.gpsmocktest.service.MockGpsService";

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
