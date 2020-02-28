package com.welcome.gpsmocktest.fragment;

import android.os.Bundle;

import com.baidu.mapapi.map.offline.MKOLUpdateElement;

import java.util.ArrayList;

public class PageFragment2 extends BaseFragment {

    private static final String ARG_PAGE = "ARG_PAGE";

    public static PageFragment2 getInstance(int page) {
        Bundle bundle = new Bundle();
        bundle.putInt(ARG_PAGE, page);
        PageFragment2 pageFragment2 = new PageFragment2();
        pageFragment2.setArguments(bundle);
        return pageFragment2;
    }

    public static ArrayList<MKOLUpdateElement> localMapList = null;
}
