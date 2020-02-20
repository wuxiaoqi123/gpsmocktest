package com.welcome.gpsmocktest.activity;

import android.content.Context;
import android.location.Criteria;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

import androidx.annotation.LayoutRes;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public abstract class BaseActivity extends AppCompatActivity {

    public static final String TAG = "GpsMockTest";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getLayoutId());
        initView();
    }

    @LayoutRes
    protected abstract int getLayoutId();

    public abstract void initView();

    public void displayToast(String str) {
        Toast toast = Toast.makeText(this, str, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.TOP, 0, 220);
        toast.show();
    }

    //判断GPS是否打开
    public boolean isGpsOpened() {
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    //模拟位置权限开启
    public boolean isAllowMockLocation() {
        boolean canMockPosition;
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP_MR1) {
            canMockPosition = Settings.Secure.getInt(getContentResolver(), Settings.Secure.ALLOW_MOCK_LOCATION, 0) != 0;
        } else {
            try {
                LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                String providerStr = LocationManager.GPS_PROVIDER;
                LocationProvider provider = locationManager.getProvider(providerStr);
                try {
                    locationManager.removeTestProvider(providerStr);
                    Log.d(TAG, "try to move test provider");
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.d(TAG, "try to move test provider");
                }
                if (provider != null) {
                    try {
                        locationManager.addTestProvider(
                                provider.getName(),
                                provider.requiresNetwork(),
                                provider.requiresSatellite(),
                                provider.requiresCell(),
                                provider.hasMonetaryCost(),
                                provider.supportsAltitude(),
                                provider.supportsSpeed(),
                                provider.supportsBearing(),
                                provider.getPowerRequirement(),
                                provider.getAccuracy()
                        );
                        canMockPosition = true;
                    } catch (Exception e) {
                        Log.e(TAG, "add origin gps test provider error");
                        canMockPosition = false;
                        e.printStackTrace();
                    }
                } else {
                    try {
                        locationManager.addTestProvider(
                                providerStr,
                                true,
                                true,
                                false,
                                false,
                                true,
                                true,
                                true,
                                Criteria.POWER_HIGH,
                                Criteria.ACCURACY_FINE
                        );
                        canMockPosition = true;
                    } catch (Exception e) {
                        Log.e(TAG, "add gps test provider error");
                        canMockPosition = false;
                        e.printStackTrace();
                    }
                }
                if (canMockPosition) {
                    locationManager.setTestProviderEnabled(providerStr, true);
                    locationManager.setTestProviderStatus(providerStr, LocationProvider.AVAILABLE, null, System.currentTimeMillis());
                    locationManager.setTestProviderEnabled(providerStr, false);
                    locationManager.removeTestProvider(providerStr);
                }
            } catch (Exception e) {
                canMockPosition = false;
                e.printStackTrace();
            }
        }
        return canMockPosition;
    }
}
