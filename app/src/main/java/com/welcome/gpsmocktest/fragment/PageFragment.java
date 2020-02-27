package com.welcome.gpsmocktest.fragment;

import android.os.Bundle;

public class PageFragment extends BaseFragment {

    private static final String ARG_PAGE = "ARG_PAGE";

    public static PageFragment getInstance(int page) {
        Bundle bundle = new Bundle();
        bundle.putInt(ARG_PAGE, page);
        PageFragment pageFragment = new PageFragment();
        pageFragment.setArguments(bundle);
        return pageFragment;
    }
}
