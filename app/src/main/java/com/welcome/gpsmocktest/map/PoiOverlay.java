package com.welcome.gpsmocktest.map;

import android.os.Bundle;

import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.map.Polyline;
import com.baidu.mapapi.search.poi.PoiResult;
import com.baidu.mapapi.search.sug.SuggestionResult;
import com.welcome.gpsmocktest.R;

import java.util.ArrayList;
import java.util.List;

public class PoiOverlay extends OverlayManager {

    private static final int MAX_POI_SIZE = 10;

    private PoiResult mPoiResult = null;
    private SuggestionResult mSuggestionResult = null;

    public PoiOverlay(BaiduMap mBaiduMap) {
        super(mBaiduMap);
    }

    public void setData(PoiResult poiResult) {
        this.mPoiResult = poiResult;
    }

    public void setSugData(SuggestionResult sugResult) {
        this.mSuggestionResult = sugResult;
    }

    @Override
    public List<OverlayOptions> getOverlayOptions() {
        if ((mPoiResult == null || mPoiResult.getAllPoi() == null) &&
                (mSuggestionResult == null || mSuggestionResult.getAllSuggestions() == null)
        ) {
            return null;
        }
        List<OverlayOptions> markerList = new ArrayList<>();
        int curInd = 0;
        if (!(mPoiResult == null || mPoiResult.getAllPoi() == null)) {
            for (int i = 0; i < mPoiResult.getAllPoi().size(); i++) {
                if (mPoiResult.getAllPoi().get(i).location == null) {
                    continue;
                }
                Bundle bundle = new Bundle();
                bundle.putInt("index", curInd++);
                markerList.add(new MarkerOptions()
                        .icon(BitmapDescriptorFactory.fromAssetWithDpi("ic_location_on_black_36dp.png"))
                        .extraInfo(bundle)
                        .position(mPoiResult.getAllPoi().get(i).location));
            }
        }
        if (!(mSuggestionResult == null || mSuggestionResult.getAllSuggestions() == null)) {
            for (int j = 0; j < mSuggestionResult.getAllSuggestions().size(); j++) {
                if (mSuggestionResult.getAllSuggestions().get(j).pt == null) {
                    continue;
                }
                Bundle bundle = new Bundle();
                bundle.putInt("index", curInd++);
                markerList.add(new MarkerOptions()
                        .icon(BitmapDescriptorFactory.fromAssetWithDpi("ic_location_on_black_36dp.png"))
                        .extraInfo(bundle)
                        .position(mSuggestionResult.getAllSuggestions().get(j).pt));
            }
        }
        return markerList;
    }


    public PoiResult getPoiResult() {
        return mPoiResult;
    }

    public SuggestionResult getSugResult() {
        return mSuggestionResult;
    }

    public boolean onPoiClick(int i) {
        return false;
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        if (!mOverlayList.contains(marker)) {
            return false;
        }
        if (marker.getExtraInfo() != null) {
            mBaiduMap.clear();
            addToMap();
            mBaiduMap.addOverlay(new MarkerOptions().position(marker.getPosition())
                    .icon(BitmapDescriptorFactory.fromResource(R.mipmap.icon_gcoding)));
            return onPoiClick(marker.getExtraInfo().getInt("index"));
        }
        return false;
    }

    @Override
    public boolean onPolylineClick(Polyline polyline) {
        return false;
    }
}
