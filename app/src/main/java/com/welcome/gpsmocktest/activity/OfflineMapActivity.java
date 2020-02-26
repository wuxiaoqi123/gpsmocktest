package com.welcome.gpsmocktest.activity;

import android.content.Context;
import android.content.Intent;

public class OfflineMapActivity extends BaseActivity {

    public static void start(Context context) {
        Intent starter = new Intent(context, OfflineMapActivity.class);
//        starter.putExtra();
        context.startActivity(starter);
    }

    @Override
    protected int getLayoutId() {
        return 0;
    }

    @Override
    public void initView() {

    }
}
