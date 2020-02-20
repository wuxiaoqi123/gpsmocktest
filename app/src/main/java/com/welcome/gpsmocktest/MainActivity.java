package com.welcome.gpsmocktest;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.NetworkUtil;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.model.LatLng;
import com.google.android.material.navigation.NavigationView;
import com.welcome.gpsmocktest.activity.BaseActivity;
import com.welcome.gpsmocktest.db.HistoryDBHelper;
import com.welcome.gpsmocktest.db.SearchDBHelper;
import com.welcome.gpsmocktest.log4j.LogUtil;
import com.welcome.gpsmocktest.service.MockGpsService;

import org.apache.log4j.Logger;

public class MainActivity extends BaseActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static final int ACTION_LOCATION_SOURCE_SETTINGS_CODE = 1;
    private static Logger log = Logger.getLogger(MainActivity.class);

    private HistoryDBHelper historyDBHelper;
    private SQLiteDatabase locHistoryDB;
    private SearchDBHelper searchDBHelper;
    private SQLiteDatabase searchHistoryDB;

    private MockServiceReceiver mockServiceReceiver;
    private boolean isServiceRun = false;

    private MapView mMapView;
    private BaiduMap mBaiduMap = null;
    private boolean isNetworkConnected = true;
    private boolean isGPSOpen = false;
    private boolean isFirstLoc = true;

    private LocationClient mLocClient = null;
    private MyLocationListener myListener = new MyLocationListener();
    private MyLocationConfiguration.LocationMode mCurrentMode;
    private BitmapDescriptor mCurrentMarker;
    private SensorManager mSensorManager;
    private double lastX = 0.0;
    private int mCurrentDirection = 0;
    private double mCurrentLat = 0.0;
    private double mCurrentLon = 0.0;
    private float mCurrentAccracy;
    private String mCurrentCity = "";
    private String mCurrentAddr;
    private MyLocationData locData;


    @Override
    protected int getLayoutId() {
        return R.layout.activity_main;
    }

    @Override
    public void initView() {
        LogUtil.configLog();
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        initDB();

        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this,
                drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        registerMockReceiver();
        initBaiduMap();
        initListener();
    }

    private void initDB() {
        historyDBHelper = new HistoryDBHelper(getApplicationContext());
        searchDBHelper = new SearchDBHelper(getApplicationContext());
        locHistoryDB = historyDBHelper.getWritableDatabase();
        searchHistoryDB = searchDBHelper.getWritableDatabase();
    }

    private void registerMockReceiver() {
        mockServiceReceiver = new MockServiceReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(MockGpsService.ACTION);
        registerReceiver(mockServiceReceiver, filter);
    }

    private void initBaiduMap() {
        if (!NetworkUtil.isNetworkAvailable(this)) {
            displayToast("网络连接不可用，请检查网络连接设置");
            isNetworkConnected = false;
        }
        if (!(isGPSOpen = isGpsOpened())) {
            displayToast("GPS定位未开启，请先打开GPS定位服务");
        }

        mMapView = findViewById(R.id.mapView);
        mBaiduMap = mMapView.getMap();
        mBaiduMap.setMyLocationEnabled(true);
        if (!isGPSOpen) {
            showGpsDialog();
        } else {
            openLocateLayer();
        }
    }

    private void initListener() {

    }

    private void showGpsDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Tips")
                .setMessage("是否开启GPS定位服务?")
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivityForResult(intent, ACTION_LOCATION_SOURCE_SETTINGS_CODE);
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void openLocateLayer() {
        mLocClient = new LocationClient(this);
        mLocClient.registerLocationListener(myListener);
        LocationClientOption option = new LocationClientOption();
        option.setIsNeedAddress(true);
        option.setOpenGps(true);
        option.setCoorType("bd09ll");
        option.setScanSpan(1000);
        mLocClient.setLocOption(option);
        mLocClient.start();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ACTION_LOCATION_SOURCE_SETTINGS_CODE) {
            if (isGPSOpen = isGpsOpened()) {
                openLocateLayer();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        return false;
    }

    public class MockServiceReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            int statusCode;
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                statusCode = bundle.getInt("statusCode");
                log.debug("DEBUG: BroadcastReceiver statusCode: " + statusCode);
                if (statusCode == MockGpsService.RunCode) {
                    isServiceRun = true;
                } else if (statusCode == MockGpsService.StopCode) {
                    isServiceRun = false;
                }
            }
        }
    }

    public class MyLocationListener implements BDLocationListener {

        @Override
        public void onReceiveLocation(BDLocation bdLocation) {
            if (bdLocation == null || mMapView == null) {
                return;
            }
            mCurrentAddr = bdLocation.getAddrStr();
            mCurrentCity = bdLocation.getCity();
            mCurrentLat = bdLocation.getLatitude();
            mCurrentLon = bdLocation.getLongitude();
            mCurrentAccracy = bdLocation.getRadius();
            locData = new MyLocationData.Builder()
                    .accuracy(bdLocation.getRadius())
                    .direction(mCurrentDirection)
                    .latitude(bdLocation.getLatitude())
                    .longitude(bdLocation.getLongitude()).build();
            mBaiduMap.setMyLocationData(locData);
            if (isFirstLoc) {
                isFirstLoc = false;
                LatLng ll = new LatLng(bdLocation.getLatitude(),
                        bdLocation.getLongitude());
                MapStatus.Builder builder = new MapStatus.Builder();
                builder.target(ll).zoom(18.0f);
                mBaiduMap.animateMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()));
            }
        }
    }
}
