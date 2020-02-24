package com.welcome.gpsmocktest.map;

import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.Overlay;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.model.LatLngBounds;

import java.util.ArrayList;
import java.util.List;

public abstract class OverlayManager implements BaiduMap.OnMarkerClickListener, BaiduMap.OnPolylineClickListener {

    protected BaiduMap mBaiduMap;
    protected List<OverlayOptions> mOverlayOptionsList = null;
    protected List<Overlay> mOverlayList = null;

    public OverlayManager(BaiduMap mBaiduMap) {
        this.mBaiduMap = mBaiduMap;
        mOverlayOptionsList = new ArrayList<>();
        mOverlayList = new ArrayList<>();
    }

    public abstract List<OverlayOptions> getOverlayOptions();

    public final void removeFromMap() {
        if (mBaiduMap == null) {
            return;
        }
        for (Overlay marker : mOverlayList) {
            marker.remove();
        }
        mOverlayOptionsList.clear();
        mOverlayList.clear();
    }

    public final void addToMap() {
        if (mBaiduMap == null) {
            return;
        }
        removeFromMap();
        List<OverlayOptions> overlayOptions = getOverlayOptions();
        if (overlayOptions != null) {
            mOverlayOptionsList.addAll(getOverlayOptions());
        }
        for (OverlayOptions option : mOverlayOptionsList) {
            mOverlayList.add(mBaiduMap.addOverlay(option));
        }
    }

    public void zoomToSpan() {
        if (mBaiduMap == null) {
            return;
        }
        if (mOverlayList.size() > 0) {
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            for (Overlay overlay : mOverlayList) {
                if (overlay instanceof Marker) {
                    builder.include(((Marker) overlay).getPosition());
                }
            }
            mBaiduMap.setMapStatus(MapStatusUpdateFactory.newLatLngBounds(builder.build()));
        }
    }
}
