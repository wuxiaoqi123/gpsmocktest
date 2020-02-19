package com.welcome.gpsmocktest;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.welcome.gpsmocktest.activity.BaseActivity;
import com.welcome.gpsmocktest.db.HistoryDBHelper;
import com.welcome.gpsmocktest.db.SearchDBHelper;
import com.welcome.gpsmocktest.log4j.LogUtil;
import com.welcome.gpsmocktest.service.MockGpsService;

import org.apache.log4j.Logger;

public class MainActivity extends BaseActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static Logger log = Logger.getLogger(MainActivity.class);

    private HistoryDBHelper historyDBHelper;
    private SQLiteDatabase locHistoryDB;
    private SearchDBHelper searchDBHelper;
    private SQLiteDatabase searchHistoryDB;

    private MockServiceReceiver mockServiceReceiver;
    private boolean isServiceRun = false;

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

    private void initListener() {

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
}
