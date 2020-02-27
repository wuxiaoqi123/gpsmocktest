package com.welcome.gpsmocktest.adapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import com.welcome.gpsmocktest.fragment.PageFragment;
import com.welcome.gpsmocktest.fragment.PageFragment2;

public class SimpleFragmentPagerAdapter extends FragmentPagerAdapter {

    private String[] tabTitles = new String[]{"城市列表", "下载管理"};

    public SimpleFragmentPagerAdapter(@NonNull FragmentManager fm, int behavior) {
        super(fm, behavior);
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0:
                return PageFragment.getInstance(position);
            case 1:
                return PageFragment2.getInstance(position);
            default:
                return null;
        }
    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        return tabTitles[position % tabTitles.length];
    }

    @Override
    public int getCount() {
        return tabTitles.length;
    }
}
