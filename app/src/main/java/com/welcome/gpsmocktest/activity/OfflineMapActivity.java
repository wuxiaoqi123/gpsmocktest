package com.welcome.gpsmocktest.activity;

import android.content.Context;
import android.content.Intent;

import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;
import com.welcome.gpsmocktest.R;
import com.welcome.gpsmocktest.adapter.SimpleFragmentPagerAdapter;

public class OfflineMapActivity extends BaseActivity {

    public static void start(Context context) {
        Intent starter = new Intent(context, OfflineMapActivity.class);
//        starter.putExtra();
        context.startActivity(starter);
    }

    private TabLayout tabLayout;
    private ViewPager viewPager;

    private SimpleFragmentPagerAdapter pagerAdapter;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_offline_map;
    }

    @Override
    public void initView() {
        tabLayout = findViewById(R.id.sliding_tabs);
        viewPager = findViewById(R.id.viewpager);
        pagerAdapter = new SimpleFragmentPagerAdapter(getSupportFragmentManager(), 0);
        viewPager.setAdapter(pagerAdapter);
        tabLayout.setupWithViewPager(viewPager);
        tabLayout.setTabMode(TabLayout.MODE_FIXED);
    }
}
