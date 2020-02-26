package com.welcome.gpsmocktest.view;

import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import com.welcome.gpsmocktest.activity.MainActivity;
import com.welcome.gpsmocktest.R;

import java.lang.reflect.Field;

public class FloatWindow implements View.OnTouchListener {

    private Context mContext;
    private WindowManager.LayoutParams mWindowParams;
    private WindowManager mWindowManager;

    private View mFloatLayout;
    private float mInViewX;
    private float mInViewY;
    private float mDownInScreenX;
    private float mDownInScreenY;
    private float mInScreenX;
    private float mInScreenY;

    private long firstClickTime = 0;

    public FloatWindow(Context context) {
        this.mContext = context;
        initFloatWindow();
    }

    private void initFloatWindow() {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        if (inflater == null) return;
        mFloatLayout = inflater.inflate(R.layout.float_button, null);
        mFloatLayout.setOnTouchListener(this);

        mWindowParams = new WindowManager.LayoutParams();
        mWindowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        if (Build.VERSION.SDK_INT >= 26) {
            mWindowParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            mWindowParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        }
        mWindowParams.format = PixelFormat.RGBA_8888;
        mWindowParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        mWindowParams.gravity = Gravity.START | Gravity.TOP;
        mWindowParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        mWindowParams.height = 108;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        return floatLayoutTouch(event);
    }

    private boolean floatLayoutTouch(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                clicks(500);
                mInViewX = event.getX();
                mInViewY = event.getY();
                mDownInScreenX = event.getRawX();
                mDownInScreenY = event.getRawY() - getSysBarHeight(mContext);
                mInScreenX = event.getRawX();
                mInScreenY = event.getRawY() - getSysBarHeight(mContext);
                break;
            case MotionEvent.ACTION_MOVE:
                mInScreenX = event.getRawX();
                mInScreenY = event.getRawY() - getSysBarHeight(mContext);
                mWindowParams.x = (int) (mInScreenX - mInViewX);
                mWindowParams.y = (int) (mInScreenY - mInViewY);
                mWindowManager.updateViewLayout(mFloatLayout, mWindowParams);
                break;
            case MotionEvent.ACTION_UP:

                break;
            default:
                break;
        }
        return true;
    }

    private void clicks(int intervalTime) {
        if (firstClickTime > 0) {
            if (System.currentTimeMillis() - firstClickTime < intervalTime) {
                firstClickTime = 0;
                Intent intent = new Intent(mContext, MainActivity.class);
                intent.addCategory(Intent.CATEGORY_LAUNCHER);
                intent.setAction(Intent.ACTION_MAIN);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                mContext.startActivity(intent);
            }
        }
        firstClickTime = System.currentTimeMillis();
    }

    public void showFloatWindow() {
        if (mFloatLayout.getParent() != null) {
            DisplayMetrics metrics = new DisplayMetrics();
            mWindowManager.getDefaultDisplay().getMetrics(metrics);
            mWindowParams.x = metrics.widthPixels;
            mWindowParams.y = metrics.heightPixels / 3 * 2 - getSysBarHeight(mContext);
            mWindowManager.addView(mFloatLayout, mWindowParams);
        }
    }

    public void hideFloatWindow() {
        if (mFloatLayout.getParent() != null) {
            mWindowManager.removeView(mFloatLayout);
        }
    }

    private static int getSysBarHeight(Context context) {
        Class<?> c;
        Object obj;
        Field field;
        int x;
        int sbar = 0;
        try {
            c = Class.forName("com.android.internal.R$dimen");
            obj = c.newInstance();
            field = c.getField("status_bar_height");
            field.setAccessible(true);
            x = Integer.parseInt(field.get(obj).toString());
            sbar = context.getResources().getDimensionPixelSize(x);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sbar;
    }
}
