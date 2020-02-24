package com.welcome.gpsmocktest;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.SimpleAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.core.view.MenuItemCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.NetworkUtil;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapPoi;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.search.core.PoiInfo;
import com.baidu.mapapi.search.core.SearchResult;
import com.baidu.mapapi.search.poi.OnGetPoiSearchResultListener;
import com.baidu.mapapi.search.poi.PoiDetailResult;
import com.baidu.mapapi.search.poi.PoiDetailSearchOption;
import com.baidu.mapapi.search.poi.PoiDetailSearchResult;
import com.baidu.mapapi.search.poi.PoiIndoorResult;
import com.baidu.mapapi.search.poi.PoiResult;
import com.baidu.mapapi.search.poi.PoiSearch;
import com.baidu.mapapi.search.sug.SuggestionResult;
import com.baidu.mapapi.search.sug.SuggestionSearch;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.welcome.gpsmocktest.activity.BaseActivity;
import com.welcome.gpsmocktest.db.HistoryDBHelper;
import com.welcome.gpsmocktest.db.SearchDBHelper;
import com.welcome.gpsmocktest.log4j.LogUtil;
import com.welcome.gpsmocktest.map.PoiOverlay;
import com.welcome.gpsmocktest.service.MockGpsService;

import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends BaseActivity implements NavigationView.OnNavigationItemSelectedListener, SensorEventListener {

    private static final int ACTION_LOCATION_SOURCE_SETTINGS_CODE = 1;
    private static Logger log = Logger.getLogger(MainActivity.class);

    private HistoryDBHelper historyDBHelper;
    private SQLiteDatabase locHistoryDB;
    private SearchDBHelper searchDBHelper;
    private SQLiteDatabase searchHistoryDB;

    private MockServiceReceiver mockServiceReceiver;
    private boolean isServiceRun = false;
    private boolean isMockServiceStart = false;

    private MapView mMapView;
    private BaiduMap mBaiduMap = null;
    private boolean isNetworkConnected = true;
    private boolean isGPSOpen = false;
    private boolean isFirstLoc = true;
    private boolean isMockLocOpen = false;

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

    private DrawerLayout drawerLayout;
    private PoiSearch poiSearch;
    private SearchView searchView;
    private ListView searchList;
    private ListView historySearchList;
    private SimpleAdapter simpleAdapter;
    private LinearLayout mLinearLayout;
    private LinearLayout mHistoryLinearLayout;
    private MenuItem searchItem;
    private boolean isSubmit;
    private SuggestionSearch mSuggestionSearch;

    private RadioGroup groupLoc, groupMap;
    private FloatingActionButton fab, fabStop;

    public static LatLng currentPt = new LatLng(30.547743718042415, 104.07018449827267);
    public static BitmapDescriptor bdA = BitmapDescriptorFactory.fromResource(R.mipmap.icon_gcoding);

    @Override
    protected int getLayoutId() {
        return R.layout.activity_main;
    }

    @Override
    public void initView() {
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mCurrentMode = MyLocationConfiguration.LocationMode.NORMAL;
        LogUtil.configLog();
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        initDB();

        drawerLayout = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this,
                drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        searchView = findViewById(R.id.action_search);
        searchList = findViewById(R.id.search_list_view);
        mLinearLayout = findViewById(R.id.search_linear);
        historySearchList = findViewById(R.id.search_history_list_view);
        mHistoryLinearLayout = findViewById(R.id.search_history_linear);
        groupLoc = findViewById(R.id.RadioGroupLocType);
        groupMap = findViewById(R.id.RadioGroup);

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
        mBaiduMap.setOnMapTouchListener(new BaiduMap.OnMapTouchListener() {
            @Override
            public void onTouch(MotionEvent motionEvent) {

            }
        });
        mBaiduMap.setOnMapClickListener(new BaiduMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                currentPt = latLng;
                transformCoordinate(currentPt);
                updateMapState();
            }

            @Override
            public boolean onMapPoiClick(MapPoi mapPoi) {
                currentPt = mapPoi.getPosition();
                transformCoordinate(currentPt);
                updateMapState();
                return false;
            }
        });
        mBaiduMap.setOnMapLongClickListener(new BaiduMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {
                currentPt = latLng;
                transformCoordinate(currentPt);
                updateMapState();
            }
        });
        mBaiduMap.setOnMapDoubleClickListener(new BaiduMap.OnMapDoubleClickListener() {
            @Override
            public void onMapDoubleClick(LatLng latLng) {
                currentPt = latLng;
                transformCoordinate(currentPt);
                updateMapState();
            }
        });
        mBaiduMap.setOnMapStatusChangeListener(new BaiduMap.OnMapStatusChangeListener() {
            @Override
            public void onMapStatusChangeStart(MapStatus mapStatus) {

            }

            @Override
            public void onMapStatusChangeStart(MapStatus mapStatus, int i) {

            }

            @Override
            public void onMapStatusChange(MapStatus mapStatus) {

            }

            @Override
            public void onMapStatusChangeFinish(MapStatus mapStatus) {

            }
        });
        mBaiduMap.setMyLocationEnabled(true);
        if (!isGPSOpen) {
            showGpsDialog();
        } else {
            openLocateLayer();
        }
        isMockLocOpen = isAllowMockLocation();
        if (!isMockLocOpen) {
            showOpenMockDialog();
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(getApplicationContext())) {
                showFloatWindowDialog();
            }
        }
    }

    private void transformCoordinate(LatLng currentPt) {
        //TODO
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

    private void showOpenMockDialog() {
        new AlertDialog.Builder(this)
                .setTitle("启用位置模拟")
                .setMessage("请在\"开发者选项→选择模拟位置信息应用\"中进行设置")
                .setPositiveButton("设置", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            Intent intent = new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS);
                            startActivity(intent);
                        } catch (Exception e) {
                            e.printStackTrace();
                            displayToast("无法跳转到开发者选项,请先确保您的设备已处于开发者模式");
                        }
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void showFloatWindowDialog() {
        new AlertDialog.Builder(this)
                .setTitle("启用悬浮窗")
                .setMessage("为了模拟定位的稳定性，建议开启\"显示悬浮窗\"选项")
                .setPositiveButton("设置", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
                            startActivity(intent);
                        } catch (Exception e) {
                            e.printStackTrace();
                            displayToast("无法跳转到设置界面，请在权限管理中开启该应用的悬浮窗");
                        }
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void initListener() {
        setFabListener();
        groupLoc.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.normalloc) {
                    mCurrentMode = MyLocationConfiguration.LocationMode.NORMAL;
                    mBaiduMap.setMyLocationConfiguration(new MyLocationConfiguration(mCurrentMode,
                            true, mCurrentMarker));
                    MapStatus.Builder builder1 = new MapStatus.Builder();
                    builder1.overlook(0);
                    mBaiduMap.animateMapStatus(MapStatusUpdateFactory.newMapStatus(builder1.build()));
                } else if (checkedId == R.id.trackloc) {
                    mCurrentMode = MyLocationConfiguration.LocationMode.FOLLOWING;
                    mBaiduMap.setMyLocationConfiguration(new MyLocationConfiguration(mCurrentMode,
                            true, mCurrentMarker));
                    MapStatus.Builder builder = new MapStatus.Builder();
                    builder.overlook(0);
                    mBaiduMap.animateMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()));
                } else if (checkedId == R.id.compassloc) {
                    mCurrentMode = MyLocationConfiguration.LocationMode.COMPASS;
                    mBaiduMap.setMyLocationConfiguration(new MyLocationConfiguration(mCurrentMode,
                            true, mCurrentMarker));
                }
            }
        });
        groupMap.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.normal) {
                    mBaiduMap.setMapType(BaiduMap.MAP_TYPE_NORMAL);
                } else if (checkedId == R.id.statellite) {
                    mBaiduMap.setMapType(BaiduMap.MAP_TYPE_SATELLITE);
                }
            }
        });

        initPoiSearchResultListener();
        setSearchRetClickListener();
    }

    private void initPoiSearchResultListener() {
        poiSearch = PoiSearch.newInstance();
        poiSearch.setOnGetPoiSearchResultListener(new OnGetPoiSearchResultListener() {
            @Override
            public void onGetPoiResult(PoiResult poiResult) {
                if (poiResult == null || poiResult.error == SearchResult.ERRORNO.RESULT_NOT_FOUND) {
                    displayToast("没有找到检索结果");
                    return;
                }
                if (poiResult.error == SearchResult.ERRORNO.NO_ERROR) {
                    if (isSubmit) {
                        MyPoiOverlay poiOverlay = new MyPoiOverlay(mBaiduMap);
                        poiOverlay.setData(poiResult);
                        mBaiduMap.setOnMarkerClickListener(poiOverlay);
                        poiOverlay.addToMap();
                        poiOverlay.zoomToSpan();
                        mLinearLayout.setVisibility(View.INVISIBLE);
                        searchItem.collapseActionView();
                        isSubmit = false;
                    } else {
                        List<Map<String, Object>> data = new ArrayList<>();
                        int retCnt = poiResult.getAllPoi().size();
                        PoiInfo poiInfo;
                        for (int i = 0; i < retCnt; i++) {
                            poiInfo = poiResult.getAllPoi().get(i);
                            Map<String, Object> testitem = new HashMap<>();
                            testitem.put("key_name", poiInfo.name);
                            testitem.put("key_addr", poiInfo.address);
                            testitem.put("key_lng", poiInfo.location.longitude);
                            testitem.put("key_lat", poiInfo.location.latitude);
                            data.add(testitem);
                        }
                        simpleAdapter = new SimpleAdapter(
                                MainActivity.this,
                                data,
                                R.layout.poi_search_item,
                                new String[]{"key_name", "key_addr", "key_lng", "key_lat"},
                                new int[]{R.id.poi_name, R.id.poi_addr, R.id.poi_longitude, R.id.poi_latitude}
                        );
                        searchList.setAdapter(simpleAdapter);
                        mLinearLayout.setVisibility(View.VISIBLE);
                    }
                }
            }

            @Override
            public void onGetPoiDetailResult(PoiDetailResult poiDetailResult) {
                displayToast(poiDetailResult.name);
            }

            @Override
            public void onGetPoiDetailResult(PoiDetailSearchResult poiDetailSearchResult) {

            }

            @Override
            public void onGetPoiIndoorResult(PoiIndoorResult poiIndoorResult) {

            }
        });
    }

    private void setSearchRetClickListener() {

    }

    private void setFabListener() {
        fab = findViewById(R.id.fab);
        fabStop = findViewById(R.id.fabStop);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isGPSOpen) {
                    showGpsDialog();
                } else {
                    if (!(isMockLocOpen = isAllowMockLocation())) {
                        showOpenMockDialog();
                    } else {
                        if (!isMockServiceStart && !isServiceRun) {
                            Log.d(TAG, "current pt is " + currentPt.longitude + " " + currentPt.latitude);
                            log.debug("current pt is " + currentPt.longitude + " " + currentPt.latitude);
                            updateMapState();
                            startMockService();
                            updatePositionInfo();
                            isMockServiceStart = true;
                            Snackbar.make(v, "位置模拟已开启", Snackbar.LENGTH_LONG)
                                    .setAction("Action", null).show();
                            fab.hide();
                            fabStop.show();
                            groupLoc.check(R.id.trackloc);
                        } else {
                            Snackbar.make(v, "位置模拟已在运行", Snackbar.LENGTH_LONG)
                                    .setAction("Action", null).show();
                            fab.hide();
                            fabStop.show();
                            isMockServiceStart = true;
                        }
                    }
                }
            }
        });
        fabStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, MockGpsService.class);
                stopService(intent);
                Snackbar.make(v, "位置模拟服务终止", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                isMockServiceStart = false;
                fab.show();
                fabStop.hide();
                mLocClient.stop();
                mLocClient.start();
                groupLoc.check(R.id.normalloc);
            }
        });
    }

    private void startMockService() {
        Intent intent = new Intent(this, MockGpsService.class);
        intent.putExtra("key", currentPt);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
            Log.d(TAG, "startForegroundService: MOCK_GPS");
            log.debug("startForegroundService: MOCK_GPS");
        } else {
            startService(intent);
            Log.d(TAG, "startService: MOCK_GPS");
            log.debug("startService: MOCK_GPS");
        }
    }

    private void updatePositionInfo() {
        //TODO
    }

    private void updateMapState() {
        Log.d(TAG, "updateMapState");
        log.debug("updateMapState");
        if (currentPt != null) {
            MarkerOptions markerOptions = new MarkerOptions()
                    .position(currentPt).icon(bdA);
            mBaiduMap.clear();
            mBaiduMap.addOverlay(markerOptions);
        }
    }

    public void setTraffic(View view) {
        mBaiduMap.setTrafficEnabled(((CheckBox) view).isChecked());
    }

    public void setBaiduHeatMap(View view) {
        mBaiduMap.setBaiduHeatMapEnabled(((CheckBox) view).isChecked());
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
        searchItem = menu.findItem(R.id.action_search);
        searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        return true;
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        return false;
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onResume() {
        mMapView.onResume();
        super.onResume();
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),
                SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        double x = event.values[SensorManager.DATA_X];
        if (Math.abs(x - lastX) > 1.0) {
            mCurrentDirection = (int) x;
            locData = new MyLocationData.Builder()
                    .accuracy(mCurrentAccracy)
                    .direction(mCurrentDirection)
                    .latitude(mCurrentLat)
                    .longitude(mCurrentLon)
                    .build();
            mBaiduMap.setMyLocationData(locData);
        }
        lastX = x;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    protected void onPause() {
        mMapView.onPause();
        super.onPause();
    }

    @Override
    protected void onStop() {
        mSensorManager.unregisterListener(this);
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if (isMockServiceStart) {
            stopService(new Intent(this, MockGpsService.class));
            isMockServiceStart = false;
        }
        mLocClient.stop();
        mBaiduMap.setMyLocationEnabled(false);
        mMapView.onDestroy();
        mMapView = null;
        poiSearch.destroy();
        mSuggestionSearch.destroy();
        locHistoryDB.close();
        locHistoryDB = null;
        searchHistoryDB.close();
        searchHistoryDB = null;
        super.onDestroy();
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

    public class MyPoiOverlay extends PoiOverlay {

        public MyPoiOverlay(BaiduMap map) {
            super(map);
        }

        @Override
        public boolean onPoiClick(int i) {
            PoiResult poiResult = getPoiResult();
            if (poiResult != null && poiResult.getAllPoi() != null) {
                PoiInfo poiInfo = poiResult.getAllPoi().get(i);
                currentPt = poiInfo.location;
                transformCoordinate(currentPt);
                poiSearch.searchPoiDetail(new PoiDetailSearchOption()
                        .poiUid(poiInfo.uid));
            }
            SuggestionResult suggestionResult = getSugResult();
            if (suggestionResult != null && suggestionResult.getAllSuggestions() != null) {
                SuggestionResult.SuggestionInfo suggestionInfo = suggestionResult.getAllSuggestions().get(i);
                currentPt = suggestionInfo.pt;
                transformCoordinate(currentPt);
                poiSearch.searchPoiDetail(new PoiDetailSearchOption()
                        .poiUid(suggestionInfo.uid));
            }
            return true;
        }
    }
}
