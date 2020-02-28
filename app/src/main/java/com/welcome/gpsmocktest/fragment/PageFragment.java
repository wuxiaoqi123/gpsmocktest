package com.welcome.gpsmocktest.fragment;

import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.baidu.mapapi.map.offline.MKOLSearchRecord;
import com.baidu.mapapi.map.offline.MKOLUpdateElement;
import com.baidu.mapapi.map.offline.MKOfflineMap;
import com.baidu.mapapi.map.offline.MKOfflineMapListener;
import com.welcome.gpsmocktest.R;
import com.welcome.gpsmocktest.utils.NetConnectUtil;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PageFragment extends BaseFragment implements MKOfflineMapListener {

    private static final String ARG_PAGE = "ARG_PAGE";

    public static PageFragment getInstance(int page) {
        Bundle bundle = new Bundle();
        bundle.putInt(ARG_PAGE, page);
        PageFragment pageFragment = new PageFragment();
        pageFragment.setArguments(bundle);
        return pageFragment;
    }

    private ListView cityListView;
    private SimpleAdapter simpleAdapter;
    private TextView tipText;
    private SearchView citySearchView;

    public static MKOfflineMap offlineMap = null;
    private int mPage;
    private List<Map<String, Object>> allCityList;
    private List<Map<String, Object>> hotCityList;

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
        View view = inflater.inflate(R.layout.fragment_page, container, false);
        tipText = view.findViewById(R.id.tipText);
        citySearchView = view.findViewById(R.id.searchView);
        cityListView = view.findViewById(R.id.city_list_view);
        citySearchView.setIconifiedByDefault(true);
        citySearchView.setFocusable(false);
        citySearchView.requestFocusFromTouch();
        if (citySearchView != null) {
            try {
                Class<?> clazz = citySearchView.getClass();
                Field field = clazz.getDeclaredField("mSearchPlate");
                field.setAccessible(true);
                View backgroundView = (View) field.get(citySearchView);
                if (backgroundView != null) {
                    backgroundView.setBackgroundColor(Color.TRANSPARENT);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (offlineMap == null) {
            offlineMap = new MKOfflineMap();
            offlineMap.init(this);
        }
        allCityList = fetchAllCity();
        hotCityList = fetchAllHotCity();
        simpleAdapter = new SimpleAdapter(
                view.getContext(),
                allCityList,
                R.layout.offline_city_item,
                new String[]{"key_cityname", "key_citysize", "key_cityid"},
                new int[]{R.id.CityNameText, R.id.CitySizeText, R.id.CityIDText});
        cityListView.setAdapter(simpleAdapter);
        setItemClickListener();
        setSearchListener();
        return view;
    }

    @Override
    public void onGetOfflineMapState(int type, int state) {
        switch (type) {
            case MKOfflineMap.TYPE_DOWNLOAD_UPDATE:
                //TODO
                break;
            case MKOfflineMap.TYPE_NEW_OFFLINE:
                break;
            case MKOfflineMap.TYPE_VER_UPDATE:
                break;
            default:
                break;
        }
    }

    private List<Map<String, Object>> fetchAllCity() {
        List<Map<String, Object>> data = new ArrayList<>();
        try {
            List<MKOLSearchRecord> records = offlineMap.getOfflineCityList();
            if (records != null) {
                Map<String, Object> item;
                for (MKOLSearchRecord r : records) {
                    item = new HashMap<>();
                    item.put("key_cityname", r.cityName);
                    item.put("key_citysize", this.formatDataSize(r.dataSize));
                    item.put("key_cityid", r.cityID);
                    data.add(item);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            data.clear();
        }
        return data;
    }

    private List<Map<String, Object>> fetchAllHotCity() {
        List<Map<String, Object>> data = new ArrayList<>();
        try {
            List<MKOLSearchRecord> records = offlineMap.getHotCityList();
            if (records != null) {
                Map<String, Object> item;
                for (MKOLSearchRecord r : records) {
                    item = new HashMap<String, Object>();
                    item.put("key_cityname", r.cityName);
                    item.put("key_citysize", this.formatDataSize(r.dataSize));
                    item.put("key_cityid", r.cityID);
                    data.add(item);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            data.clear();
        }
        return data;
    }

    private String formatDataSize(long size) {
        String ret;
        if (size < (1024 * 1024)) {
            ret = String.format("%dK", size / 1024);
        } else {
            ret = String.format("%.1fM", size / (1024.0 * 1024));
        }
        return ret;
    }

    private void setItemClickListener() {
        cityListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final String cityID = ((TextView) (view.findViewById(R.id.CityIDText))).getText().toString();
                final String cityName = ((TextView) (view.findViewById(R.id.CityNameText))).getText().toString();
                new AlertDialog.Builder(PageFragment.this.getContext())
                        .setTitle("Tips")
                        .setMessage("确定要下载" + cityName + "的离线地图吗?")
                        .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                boolean exist = false;
                                boolean needUpdate = true;
                                for (MKOLUpdateElement e : PageFragment2.localMapList) {
                                    if ((e.cityID == Integer.parseInt(cityID))) {
                                        exist = true;
                                        if (!e.update) {
                                            needUpdate = false;
                                            displayToast("离线地图已存在");
                                        }
                                        break;
                                    }
                                }
                                if (NetConnectUtil.isNetworkAvailable(PageFragment.this.getContext())) {
                                    if (!exist) {
                                        offlineMap.start(Integer.parseInt(cityID));
                                        citySearchView.onActionViewCollapsed();
                                        displayToast("开始下载离线地图");
                                    } else {
                                        if (needUpdate) {
                                            offlineMap.update(Integer.parseInt(cityID));
                                            displayToast("开始更新离线地图");
                                        }
                                    }
                                } else {
                                    displayToast("网络连接不可用，请检查网络设置");
                                }
                            }
                        })
                        .setNegativeButton("取消", null)
                        .show();
            }
        });
    }

    private void setSearchListener() {
        citySearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (TextUtils.isEmpty(newText)) {
                    tipText.setText("全国");
                    simpleAdapter = new SimpleAdapter(
                            PageFragment.this.getContext(),
                            allCityList,
                            R.layout.offline_city_item,
                            new String[]{"key_cityname", "key_citysize", "key_cityid"},
                            new int[]{R.id.CityNameText, R.id.CitySizeText, R.id.CityIDText});
                    cityListView.setAdapter(simpleAdapter);
                } else {
                    List<MKOLSearchRecord> records = offlineMap.searchCity(newText);
                    if (records != null) {
                        if (records.size() > 0) {
                            List<Map<String, Object>> searchRet = new ArrayList<>();
                            tipText.setText("搜索结果");
                            Map<String, Object> item;
                            for (MKOLSearchRecord r : records) {
                                item = new HashMap<>();
                                item.put("key_cityname", r.cityName);
                                item.put("key_citysize", formatDataSize(r.dataSize));
                                item.put("key_cityid", r.cityID);
                                searchRet.add(item);
                            }
                            simpleAdapter = new SimpleAdapter(
                                    PageFragment.this.getContext(),
                                    searchRet,
                                    R.layout.offline_city_item,
                                    new String[]{"key_cityname", "key_citysize", "key_cityid"},
                                    new int[]{R.id.CityNameText, R.id.CitySizeText, R.id.CityIDText});
                            cityListView.setAdapter(simpleAdapter);
                        }
                    } else {
                        tipText.setText("热门城市");
                        displayToast("未搜索到该城市,或该城市不支持离线地图");
                        simpleAdapter = new SimpleAdapter(
                                PageFragment.this.getContext(),
                                hotCityList,
                                R.layout.offline_city_item,
                                new String[]{"key_cityname", "key_citysize", "key_cityid"},// 与下面数组元素要一一对应
                                new int[]{R.id.CityNameText, R.id.CitySizeText, R.id.CityIDText});
                        cityListView.setAdapter(simpleAdapter);
                    }
                }
                return false;
            }
        });
    }
}
