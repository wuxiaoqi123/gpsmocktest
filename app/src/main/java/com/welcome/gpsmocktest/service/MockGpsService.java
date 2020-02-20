package com.welcome.gpsmocktest.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.Process;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.baidu.mapapi.model.LatLng;
import com.welcome.gpsmocktest.R;
import com.welcome.gpsmocktest.view.FloatWindow;

import org.apache.log4j.Logger;

import java.util.List;
import java.util.UUID;

public class MockGpsService extends Service {

    public static final int RunCode = 0x01;
    public static final int StopCode = 0x02;
    public static final String ACTION = "com.welcome.gpsmocktest.service.MockGpsService";

    private static String TAG = "MockGpsService";

    private LocationManager locationManager;
    private HandlerThread handlerThread;
    private Handler handler;

    private boolean isStop = true;

    private LatLng latLngInfo = new LatLng(30.547743718042415, 104.07018449827267);

    private FloatWindow floatWindow;
    private boolean isFloatWindowStart = false;

    private static Logger log = Logger.getLogger(MockGpsService.class);

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");
        log.debug(TAG + ": onCreate");
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        getProviders();
        rmNetworkTestProvider();
        rmGPSTestProvider();
        setNetworkTestProvider();
        setGPSTestProvider();
        handlerThread = new HandlerThread(getUUID(), Process.THREAD_PRIORITY_FOREGROUND);
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                try {
                    Thread.sleep(128);
                    if (!isStop) {
                        setTestProviderLocation();
                        setGPSLocation();
                        sendEmptyMessage(0);
                        sendBroadcastReceiver(false);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.d(TAG, "handleMessage error");
                    log.debug(TAG + ": handleMessage error");
                    Thread.currentThread().interrupt();
                }
            }
        };
        handler.sendEmptyMessage(0);
    }

    private void getProviders() {
        List<String> providerList = locationManager.getProviders(true);
        for (String str : providerList) {
            Log.d(TAG, str);
            log.debug("active provider: " + str);
        }
    }

    private void rmNetworkTestProvider() {
        try {
            String providerStr = LocationManager.NETWORK_PROVIDER;
            if (locationManager.isProviderEnabled(providerStr)) {
                Log.d(TAG, "now remove NetworkProvider");
                log.debug(TAG + ": now remove NetworkProvider");
                locationManager.removeTestProvider(providerStr);
            } else {
                Log.d(TAG, "NetworkProvider is not enabled");
                log.debug(TAG + ": NetworkProvider is not enabled");
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG, "rmNetworkProvider error");
            log.debug(TAG + ": rmNetworkProvider error");
        }
    }

    private void rmGPSTestProvider() {
        try {
            String providerStr = LocationManager.GPS_PROVIDER;
            if (locationManager.isProviderEnabled(providerStr)) {
                Log.d(TAG, "now remove GPSProvider");
                log.debug(TAG + ": now remove GPSProvider");
                locationManager.removeTestProvider(providerStr);
            } else {
                Log.d(TAG, "GPSProvider is not enable");
                log.debug(TAG + ": GPSProvider is not enable");
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG, "remove GPSProvider error");
            log.debug(TAG + ": remove GPSProvider error");
        }
    }

    private void setNetworkTestProvider() {
        String providerStr = LocationManager.NETWORK_PROVIDER;
        try {
            locationManager.addTestProvider(providerStr,
                    false,
                    false,
                    false,
                    false,
                    false,
                    false,
                    false,
                    1,
                    Criteria.ACCURACY_FINE
            );
            Log.d(TAG, "addTestProvider[NETWORK_PROVIDER] success");
            log.debug(TAG + ": addTestProvider[NETWORK_PROVIDER] success");
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG, "addTestProvider[NETWORK_PROVIDER] error");
            log.debug(TAG + ": addTestProvider[NETWORK_PROVIDER] error");
        }
        if (!locationManager.isProviderEnabled(providerStr)) {
            try {
                locationManager.setTestProviderEnabled(providerStr, true);
            } catch (Exception e) {
                e.printStackTrace();
                Log.d(TAG, "setTestProviderEnabled[NETWORK_PROVIDER] error");
                log.debug(TAG + ": setTestProviderEnabled[NETWORK_PROVIDER] error");
            }
        }
    }

    private void setGPSTestProvider() {
        try {
            locationManager.addTestProvider(LocationManager.GPS_PROVIDER,
                    false,
                    true,
                    true,
                    false,
                    true,
                    true,
                    true,
                    0,
                    5);
            Log.d(TAG, "addTestProvider[GPS_PROVIDER] success");
            log.debug(TAG + ": addTestProvider[GPS_PROVIDER] success");
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG, "addTestProvider[GPS_PROVIDER] error");
            log.debug(TAG + ": addTestProvider[GPS_PROVIDER] error");
        }
    }

    private String getUUID() {
        return UUID.randomUUID().toString();
    }

    private void sendBroadcastReceiver(boolean stop) {
        Intent intent = new Intent();
        intent.putExtra("statusCode", stop ? StopCode : RunCode);
        intent.setAction(ACTION);
        sendBroadcast(intent);
    }

    private void setTestProviderLocation() {
        Log.d(TAG, "setNetworkLocation: " + latLngInfo.toString());
        log.debug(TAG + ": setNetworkLocation: " + latLngInfo.toString());
        String providerStr = LocationManager.NETWORK_PROVIDER;
        try {
            locationManager.setTestProviderLocation(providerStr, generateLocation(latLngInfo));
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG, "setNetworkLocation error");
            log.debug(TAG + ": setNetworkLocation error");
        }
    }

    private void setGPSLocation() {
        Log.d(TAG, "setGPSLocation: " + latLngInfo.toString());
        log.debug(TAG + ": setGPSLocation: " + latLngInfo.toString());
        try {
            locationManager.setTestProviderLocation(LocationManager.GPS_PROVIDER, generateLocation(latLngInfo));
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG, "setGPSLocation error");
            log.debug(TAG + ": setGPSLocation error");
        }
    }

    private Location generateLocation(LatLng latLng) {
        Location loc = new Location("gps");
        loc.setAccuracy(2.0f);
        loc.setAltitude(55.0D);
        loc.setBearing(1.0f);
        Bundle bundle = new Bundle();
        bundle.putInt("satellites", 7);
        loc.setExtras(bundle);
        loc.setLatitude(latLng.latitude);
        loc.setLongitude(latLng.longitude);
        loc.setTime(System.currentTimeMillis());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            loc.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
        }
        return loc;
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");
        log.debug(TAG + ": onStartCommand");
        String channelId = "channel_01";
        String name = "channel_name";
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel mChannel = new NotificationChannel(channelId, name, NotificationManager.IMPORTANCE_LOW);
            Log.i(TAG, mChannel.toString());
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(mChannel);
            }
            notification = new Notification.Builder(this, channelId)
                    .setChannelId(channelId)
                    .setContentTitle("位置模拟服务已启动")
                    .setContentText("MockLocation service is running")
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .build();
        } else {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                    .setContentTitle("位置模拟服务已启动")
                    .setContentText("MockLocation service is running")
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setOngoing(true);
            notification = builder.build();
        }
        startForeground(1, notification);

        latLngInfo = intent.getParcelableExtra("key");
        if (latLngInfo != null) {
            Log.d(TAG, "DataFromMain is " + latLngInfo.toString());
            log.debug(TAG + ": DataFromMain is " + latLngInfo.toString());
        }
        isStop = false;
        if (!isFloatWindowStart) {
            floatWindow = new FloatWindow(this);
            floatWindow.showFloatWindow();
            isFloatWindowStart = true;
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        log.debug(TAG + ": onDestroy");
        isStop = true;
        floatWindow.hideFloatWindow();
        isFloatWindowStart = false;
        handler.removeMessages(0);
        handlerThread.quit();
        rmNetworkTestProvider();
        rmGPSTestProvider();
        stopForeground(true);
        sendBroadcastReceiver(true);
        super.onDestroy();
    }
}
