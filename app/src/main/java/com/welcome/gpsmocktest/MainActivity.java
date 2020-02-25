package com.welcome.gpsmocktest;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.SimpleAdapter;
import android.widget.TextView;

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
import com.baidu.mapapi.map.MapStatusUpdate;
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
import com.baidu.mapapi.search.sug.OnGetSuggestionResultListener;
import com.baidu.mapapi.search.sug.SuggestionResult;
import com.baidu.mapapi.search.sug.SuggestionSearch;
import com.baidu.mapapi.search.sug.SuggestionSearchOption;
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
                        startDevlopmentSetting();
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
        setHistorySearchClickListener();
        setSugSearchListener();
        randomFix();
        LatLng latLng = getLatestLocation(locHistoryDB, HistoryDBHelper.TABLE_NAME);
        MapStatusUpdate mapStatusUpdate = MapStatusUpdateFactory.newLatLng(latLng);
        mBaiduMap.setMapStatus(mapStatusUpdate);
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
        searchList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String lat = ((TextView) view.findViewById(R.id.poi_latitude)).getText().toString();
                String lng = ((TextView) view.findViewById(R.id.poi_longitude)).getText().toString();
                currentPt = new LatLng(Double.valueOf(lat), Double.valueOf(lng));
                MapStatusUpdate mapStatusUpdate = MapStatusUpdateFactory.newLatLng(currentPt);
                mBaiduMap.setMapStatus(mapStatusUpdate);
                updateMapState();
                transformCoordinate(currentPt);

                //TODO
                ContentValues contentValues = new ContentValues();


                mLinearLayout.setVisibility(View.INVISIBLE);
                searchItem.collapseActionView();
            }
        });
    }

    private void setHistorySearchClickListener() {
        //TODO
    }

    private void setSugSearchListener() {
        mSuggestionSearch = SuggestionSearch.newInstance();
        mSuggestionSearch.setOnGetSuggestionResultListener(new OnGetSuggestionResultListener() {
            @Override
            public void onGetSuggestionResult(SuggestionResult suggestionResult) {
                if (suggestionResult == null || suggestionResult.getAllSuggestions() == null) {
                    displayToast("没有找到检索结果");
                } else {
                    if (isSubmit) {
                        groupLoc.check(R.id.normalloc);
                        MyPoiOverlay poiOverlay = new MyPoiOverlay(mBaiduMap);
                        poiOverlay.setSugData(suggestionResult);
                        mBaiduMap.setOnMarkerClickListener(poiOverlay);
                        poiOverlay.addToMap();
                        poiOverlay.zoomToSpan();
                        mLinearLayout.setVisibility(View.INVISIBLE);
                        searchItem.collapseActionView();
                        isSubmit = false;
                    } else {
                        List<Map<String, Object>> data = new ArrayList<>();
                        int retCount = suggestionResult.getAllSuggestions().size();
                        SuggestionResult.SuggestionInfo suggestionInfo;
                        for (int i = 0; i < retCount; i++) {
                            suggestionInfo = suggestionResult.getAllSuggestions().get(i);
                            if (suggestionInfo.pt == null) {
                                continue;
                            }
                            Map<String, Object> poiItem = new HashMap<>();
                            poiItem.put("key_name", suggestionInfo.key);
                            poiItem.put("key_addr", suggestionInfo.city + " " + suggestionInfo.district);
                            poiItem.put("key_lng", "" + suggestionInfo.pt.longitude);
                            poiItem.put("key_lat", "" + suggestionInfo.pt.latitude);
                            data.add(poiItem);
                        }
                        simpleAdapter = new SimpleAdapter(
                                MainActivity.this,
                                data,
                                R.layout.poi_search_item,
                                new String[]{"key_name", "key_addr", "key_lng", "key_lat"},// 与下面数组元素要一一对应
                                new int[]{R.id.poi_name, R.id.poi_addr, R.id.poi_longitude, R.id.poi_latitude});
                        searchList.setAdapter(simpleAdapter);
                        mLinearLayout.setVisibility(View.VISIBLE);
                    }
                }
            }
        });
    }

    private void randomFix() {
        double ra1 = Math.random() * 2.0 - 1.0;
        double ra2 = Math.random() * 2.0 - 1.0;
        double randLng = 104.07018449827267 + ra1 / 2000.0;
        double randLat = 30.547743718042415 + ra2 / 2000.0;
        currentPt = new LatLng(randLat, randLng);
        transformCoordinate(currentPt);
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
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        searchItem = menu.findItem(R.id.action_search);
        searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        searchView.setIconified(false);
        searchView.onActionViewExpanded();
        searchView.setIconifiedByDefault(true);
        searchView.setSubmitButtonEnabled(true);
        MenuItemCompat.setOnActionExpandListener(searchItem, new MenuItemCompat.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                // 关闭
                menu.setGroupVisible(0, true);
                mLinearLayout.setVisibility(View.INVISIBLE);
                mHistoryLinearLayout.setVisibility(View.INVISIBLE);
                return true;
            }

            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                // 展开
                menu.setGroupVisible(0, false);
                mLinearLayout.setVisibility(View.INVISIBLE);
                List<Map<String, Object>> data = getSearchHistory();
                if (data.size() > 0) {
                    simpleAdapter = new SimpleAdapter(
                            MainActivity.this,
                            data,
                            R.layout.history_search_item,
                            new String[]{"search_key", "search_description", "search_timestamp", "search_isLoc", "search_longitude", "search_latitude"},
                            new int[]{R.id.search_key, R.id.search_description, R.id.search_timestamp, R.id.search_isLoc, R.id.search_longitude, R.id.search_latitude}
                    );
                    historySearchList.setAdapter(simpleAdapter);
                    mHistoryLinearLayout.setVisibility(View.VISIBLE);
                }
                return true;
            }
        });
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                try {
                    isSubmit = true;
                    mSuggestionSearch.requestSuggestion((new SuggestionSearchOption())
                            .keyword(query)
                            .city(mCurrentCity)
                    );
                    ContentValues contentValues = new ContentValues();
                    contentValues.put("SearchKey", query);
                    contentValues.put("Description", "搜索...");
                    contentValues.put("IsLocate", 0);
                    contentValues.put("TimeStamp", System.currentTimeMillis() / 1000);
                    if (insertHistorySearchTable(searchHistoryDB, SearchDBHelper.TABLE_NAME, contentValues)) {
                        Log.d("DATABASE", "insertHistorySearchTable[SearchHistory] success");
                        log.debug("DATABASE: insertHistorySearchTable[SearchHistory] success");
                    } else {
                        Log.e("DATABASE", "insertHistorySearchTable[SearchHistory] error");
                        log.error("DATABASE: insertHistorySearchTable[SearchHistory] error");
                    }
                    mBaiduMap.clear();
                    mLinearLayout.setVisibility(View.INVISIBLE);
                } catch (Exception e) {
                    e.printStackTrace();
                    displayToast("搜索失败，请检查网络连接");
                }
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                mHistoryLinearLayout.setVisibility(View.INVISIBLE);
                if (!TextUtils.isEmpty(newText)) {
                    try {
                        mSuggestionSearch.requestSuggestion(new SuggestionSearchOption()
                                .keyword(newText)
                                .city(mCurrentCity));
                    } catch (Exception e) {
                        e.printStackTrace();
                        displayToast("搜索失败，请检查网络连接");
                    }
                }
                return true;
            }
        });
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_setting:
                startDevlopmentSetting();
                break;
            case R.id.action_resetMap:
                resetMap();
                break;
            case R.id.action_input:
                showLatlngDialog();
                break;
            default:
                break;

        }
        return super.onOptionsItemSelected(item);
    }

    private void startDevlopmentSetting() {
        try {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS);
            startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
            displayToast("无法跳转到开发者选项,请先确保您的设备已处于开发者模式");
        }
    }

    private void resetMap() {
        mBaiduMap.clear();
        MapStatusUpdate mapStatusUpdate = MapStatusUpdateFactory.newLatLng(new LatLng(mCurrentLat, mCurrentLon));
        mBaiduMap.setMapStatus(mapStatusUpdate);
        currentPt = new LatLng(mCurrentLat, mCurrentLon);
        transformCoordinate(currentPt);
    }

    private void showLatlngDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("输入经度和纬度(BD09坐标系)");
        View view = LayoutInflater.from(this).inflate(R.layout.latlng_dialog, null);
        builder.setView(view);
        final EditText dialog_lng = view.findViewById(R.id.dialog_longitude);
        final EditText dialog_lat = view.findViewById(R.id.dialog_latitude);
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String dialog_lng_str, dialog_lat_str;
                try {
                    dialog_lng_str = dialog_lng.getText().toString().trim();
                    dialog_lat_str = dialog_lat.getText().toString().trim();
                    double dialog_lng_double = Double.valueOf(dialog_lng_str);
                    double dialog_lat_double = Double.valueOf(dialog_lat_str);
                    if (dialog_lng_double > 180.0 || dialog_lng_double < -180.0 || dialog_lat_double > 90.0 || dialog_lat_double < -90.0) {
                        displayToast("经纬度超出限制!\n-180.0<经度<180.0\n-90.0<纬度<90.0");
                    } else {
                        currentPt = new LatLng(dialog_lat_double, dialog_lng_double);
                        MapStatusUpdate mapStatusUpdate = MapStatusUpdateFactory.newLatLng(currentPt);
                        mBaiduMap.setMapStatus(mapStatusUpdate);
                        updateMapState();
                        transformCoordinate(currentPt);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    displayToast("获取经纬度出错,请检查输入是否正确");
                }
            }
        });
        builder.setNegativeButton("取消", null);
        builder.show();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull final MenuItem item) {
        drawerLayout.closeDrawer(GravityCompat.START);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                switch (item.getItemId()) {
                    case R.id.nav_map:
                        break;
                    case R.id.nav_history:
                        break;
                    case R.id.nav_localmap:
                        break;
                    case R.id.nav_manage:
                        startActivity(new Intent(Settings.ACTION_SETTINGS));
                        break;
                    case R.id.nav_bug_report:
                        break;
                    case R.id.nav_send:
                        Intent i = new Intent(Intent.ACTION_SEND);
                        // i.setType("text/plain");
                        i.setType("message/rfc822");
                        i.putExtra(Intent.EXTRA_EMAIL, new String[]{"hilavergil@gmail.com"});
                        i.putExtra(Intent.EXTRA_SUBJECT, "SUGGESTION");
                        startActivity(Intent.createChooser(i, "Select email application."));
                        break;
                    default:
                        break;
                }
            }
        }, 100);
        return true;
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
        unregisterReceiver(mockServiceReceiver);
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

    private LatLng getLatestLocation(SQLiteDatabase sqLiteDatabase, String tableName) {
        try {
            Cursor cursor = sqLiteDatabase.query(tableName, null,
                    "ID > ?", new String[]{"0"},
                    null, null, "TimeStamp DESC", "1");
            if (cursor.getCount() == 0) {
                randomFix();
                return MainActivity.currentPt;
            } else {
                cursor.moveToNext();
                String BD09Longitude = cursor.getString(5);
                String BD09Latitude = cursor.getString(6);
                cursor.close();
                return new LatLng(Double.valueOf(BD09Latitude), Double.valueOf(BD09Longitude));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return MainActivity.currentPt;
    }

    private boolean insertHistorySearchTable(SQLiteDatabase sqLiteDatabase, String tableName, ContentValues contentValues) {
        boolean insertRet = true;
        try {
            String searchKey = contentValues.get("SearchKey").toString();
            sqLiteDatabase.delete(tableName, "SearchKey = ?", new String[]{searchKey});
            long insert = sqLiteDatabase.insert(tableName, null, contentValues);
            insertRet = insert != -1;
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "DATABASE insert error");
            log.error(TAG + " DATABASE insert error");
            insertRet = false;
        }
        return insertRet;
    }

    // 获取历史搜索
    private List<Map<String, Object>> getSearchHistory() {
        List<Map<String, Object>> data = new ArrayList<>();
        try {
            Cursor cursor = searchHistoryDB.query(SearchDBHelper.TABLE_NAME, null,
                    "ID > ?", new String[]{"0"},
                    null, null, "TimeStamp DESC", null);
            Map<String, Object> searchHistoryItem;
            while (cursor.moveToNext()) {
//                int id = cursor.getInt(0);
                searchHistoryItem = new HashMap<>();
                searchHistoryItem.put("search_key", cursor.getString(1));
                searchHistoryItem.put("search_description", cursor.getString(2));
                searchHistoryItem.put("search_timestamp", "" + cursor.getInt(3));
                searchHistoryItem.put("search_isLoc", "" + cursor.getInt(4));
                searchHistoryItem.put("search_longitude", "" + cursor.getString(7));
                searchHistoryItem.put("search_latitude", "" + cursor.getString(8));
                data.add(searchHistoryItem);
            }
            cursor.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return data;
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
