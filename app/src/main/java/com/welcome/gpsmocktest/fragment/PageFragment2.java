package com.welcome.gpsmocktest.fragment;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.baidu.mapapi.map.offline.MKOLUpdateElement;
import com.welcome.gpsmocktest.R;

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

    private static ListView localMapListView;
    private static TextView noOfflineMap;


    public static ArrayList<MKOLUpdateElement> localMapList = null;
    private static LocalMapAdapter localMapAdapter;
    private int mPage;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mPage = getArguments().getInt(ARG_PAGE);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_page_2, container, false);
        localMapListView = view.findViewById(R.id.localmaplist);
        noOfflineMap = view.findViewById(R.id.no_offline_map);
        localMapList = PageFragment.offlineMap.getAllUpdateInfo();
        if (localMapList == null) {
            localMapList = new ArrayList<>();
        }
        localMapAdapter = new LocalMapAdapter();
        localMapListView.setAdapter(localMapAdapter);
        if (localMapList.size() != 0) {
            localMapListView.setVisibility(View.VISIBLE);
            noOfflineMap.setVisibility(View.GONE);
        } else {
            localMapListView.setVisibility(View.GONE);
            noOfflineMap.setVisibility(View.VISIBLE);
        }
        return view;
    }

    public static void updateView() {
        localMapList = PageFragment.offlineMap.getAllUpdateInfo();
        if (localMapList == null) {
            localMapList = new ArrayList<>();
        }
        if (localMapList.size() != 0) {
            localMapListView.setVisibility(View.VISIBLE);
            noOfflineMap.setVisibility(View.GONE);
        } else {
            localMapListView.setVisibility(View.GONE);
            noOfflineMap.setVisibility(View.VISIBLE);
        }
        localMapAdapter.notifyDataSetChanged();
    }

    public class LocalMapAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return localMapList.size();
        }

        @Override
        public MKOLUpdateElement getItem(int position) {
            return localMapList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @SuppressLint("ViewHolder")
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            MKOLUpdateElement e = getItem(position);
            convertView = View.inflate(
                    PageFragment2.this.getContext(),
                    R.layout.local_map_item, null);
            initViewItem(convertView, e);
            return convertView;
        }

        private void initViewItem(View view, final MKOLUpdateElement e) {
            Button remove = view.findViewById(R.id.remove);
            Button control = view.findViewById(R.id.control);
            TextView title = view.findViewById(R.id.title);
            TextView update = view.findViewById(R.id.update);
            TextView ratio = view.findViewById(R.id.ratio);
            ratio.setText(e.ratio + "%");
            title.setText(e.cityName);
            if (e.update) {
                update.setText("可更新");
                control.setVisibility(View.VISIBLE);
            } else {
                update.setText("最新");
            }
            if (e.ratio == 100) {
                remove.setEnabled(true);
            }
            control.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    displayToast("开始更新");
                    PageFragment.offlineMap.update(e.cityID);
                }
            });
            remove.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new AlertDialog.Builder(PageFragment2.this.getContext())
                            .setTitle("Tips")
                            .setMessage("确定要删除" + e.cityName + "的离线地图吗？")
                            .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    PageFragment.offlineMap.remove(e.cityID);
                                    updateView();
                                }
                            })
                            .setNegativeButton("取消", null)
                            .show();
                }
            });
        }
    }
}
