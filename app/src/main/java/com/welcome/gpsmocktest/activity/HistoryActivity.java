package com.welcome.gpsmocktest.activity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.model.LatLng;
import com.welcome.gpsmocktest.R;
import com.welcome.gpsmocktest.db.HistoryDBHelper;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HistoryActivity extends BaseActivity {

    private static final String TAG = "HistoryActivity";

    public static void start(Context context) {
        Intent starter = new Intent(context, HistoryActivity.class);
//        starter.putExtra();
        context.startActivity(starter);
    }

    private TextView noRecordTv;
    private ListView listView;
    private SimpleAdapter simpleAdapter;

    private HistoryDBHelper historyDBHelper;
    private SQLiteDatabase sqLiteDatabase;

    private String bd09Longitude = "104.07018449827267";
    private String bd09Latitude = "30.547743718042415";
    private String wgs84Longitude = "104.06121778639009";
    private String wgs84Latitude = "30.544111926165282";

    @Override
    protected int getLayoutId() {
        return R.layout.activity_history;
    }

    @Override
    public void initView() {
        historyDBHelper = new HistoryDBHelper(getApplicationContext());
        sqLiteDatabase = historyDBHelper.getWritableDatabase();
        noRecordTv = findViewById(R.id.no_record_textview);
        listView = findViewById(R.id.list_view);
        if (recordArchive(sqLiteDatabase, HistoryDBHelper.TABLE_NAME)) {
            Log.d(TAG, "archive success");
        }
        initListView();
        initListener();
    }

    private boolean recordArchive(SQLiteDatabase sqLiteDatabase, String tableName) {
        boolean archiveRet = true;
        final long weekSecond = 7L * 24 * 60 * 60;
        try {
            sqLiteDatabase.delete(tableName, "TimeStamp < ?", new String[]{Long.toString(System.currentTimeMillis() / 1000 - weekSecond)});
        } catch (Exception e) {
            e.printStackTrace();
            archiveRet = false;
        }
        return archiveRet;
    }

    private void initListView() {
        List<Map<String, Object>> allHistoryRecord = fetchAllRecord(sqLiteDatabase, HistoryDBHelper.TABLE_NAME);
        if (allHistoryRecord.size() == 0) {
            listView.setVisibility(View.GONE);
            noRecordTv.setVisibility(View.VISIBLE);
        } else {
            simpleAdapter = new SimpleAdapter(
                    this,
                    allHistoryRecord,
                    R.layout.history_item,
                    new String[]{"key_id", "key_location", "key_time", "key_wgslatlng", "kdy_bdlatlng"},
                    new int[]{R.id.LocationID, R.id.LoctionText, R.id.TimeText, R.id.WGSLatLngText, R.id.BDLatLngText}
            );
            listView.setAdapter(simpleAdapter);
        }
    }

    private List<Map<String, Object>> fetchAllRecord(SQLiteDatabase sqLiteDatabase, String tableName) {
        List<Map<String, Object>> data = new ArrayList<>();
        try {
            Cursor cursor = sqLiteDatabase.query(tableName, null,
                    "ID > ?", new String[]{"0"},
                    null, null, "TimeStamp DESC", null);
            Map<String, Object> item;
            while (cursor.moveToNext()) {
                item = new HashMap<>();
                int ID = cursor.getInt(0);
                String Location = cursor.getString(1);
                String Longitude = cursor.getString(2);
                String Latitude = cursor.getString(3);
                long TimeStamp = cursor.getInt(4);
                String BD09Longitude = cursor.getString(5);
                String BD09Latitude = cursor.getString(6);
                Log.d("TB", ID + "\t" + Location + "\t" + Longitude + "\t" + Latitude + "\t" + TimeStamp + "\t" + BD09Longitude + "\t" + BD09Latitude);

                BigDecimal bigDecimalLongitude = new BigDecimal(Double.valueOf(Longitude));
                BigDecimal bigDecimalLatitude = new BigDecimal(Double.valueOf(Latitude));
                BigDecimal bigDecimalBDLongitude = new BigDecimal(Double.valueOf(BD09Longitude));
                BigDecimal bigDecimalBDLatitude = new BigDecimal(Double.valueOf(BD09Latitude));

                double doubleLongitude = bigDecimalLongitude.setScale(11, BigDecimal.ROUND_HALF_UP).doubleValue();
                double doubleLatitude = bigDecimalLatitude.setScale(11, BigDecimal.ROUND_HALF_UP).doubleValue();

                double doubleBDLongitude = bigDecimalBDLongitude.setScale(11, BigDecimal.ROUND_HALF_UP).doubleValue();
                double doubleBDLatitude = bigDecimalBDLatitude.setScale(11, BigDecimal.ROUND_HALF_UP).doubleValue();

                item.put("key_id", "" + ID);
                item.put("key_location", Location);
                item.put("key_time", timeStamp2Date(Long.toString(TimeStamp), null));
                item.put("key_wgslatlng", "[经度:" + doubleLongitude + " 纬度:" + doubleLatitude + "]");
                item.put("kdy_bdlatlng", "[经度:" + doubleBDLongitude + " 纬度:" + doubleBDLatitude + "]");
                data.add(item);
            }
            cursor.close();
        } catch (Exception e) {
            e.printStackTrace();
            data.clear();
        }
        return data;
    }

    private String timeStamp2Date(String seconds, String format) {
        if (TextUtils.isDigitsOnly(seconds) || seconds.equals("null")) {
            return "";
        }
        if (TextUtils.isEmpty(format)) format = "yyyy-MM-dd HH:mm:ss";
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(format);
        return simpleDateFormat.format(new Date(Long.valueOf(seconds + "000")));
    }

    private void initListener() {
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //bd09坐标
                String bd09LatLng = (String) ((TextView) view.findViewById(R.id.BDLatLngText)).getText();
                bd09LatLng = bd09LatLng.substring(bd09LatLng.indexOf("[") + 1, bd09LatLng.indexOf("]"));
                String latLngStr[] = bd09LatLng.split(" ");
                bd09Longitude = latLngStr[0].substring(latLngStr[0].indexOf(":") + 1);
                bd09Latitude = latLngStr[1].substring(latLngStr[1].indexOf(":") + 1);
                //wgs84坐标
                String wgs84LatLng = (String) ((TextView) view.findViewById(R.id.WGSLatLngText)).getText();
                wgs84LatLng = wgs84LatLng.substring(wgs84LatLng.indexOf("[") + 1, wgs84LatLng.indexOf("]"));
                String latLngStr2[] = wgs84LatLng.split(" ");
                wgs84Longitude = latLngStr2[0].substring(latLngStr2[0].indexOf(":") + 1);
                wgs84Latitude = latLngStr2[1].substring(latLngStr2[1].indexOf(":") + 1);
                if (!setHistoryLocation(bd09Longitude, bd09Latitude, wgs84Longitude, wgs84Latitude)) {
                    displayToast("定位失败,请手动选取定位点");
                }
                finish();
            }
        });
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, final View view, int position, long id) {
                new AlertDialog.Builder(HistoryActivity.this)
                        .setTitle("Warning")//这里是表头的内容
                        .setMessage("确定要删除该项历史记录吗?")//这里是中间显示的具体信息
                        .setPositiveButton("确定",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        String locID = (String) ((TextView) view.findViewById(R.id.LocationID)).getText();
                                        boolean deleteRet = deleteRecord(sqLiteDatabase, HistoryDBHelper.TABLE_NAME, Integer.valueOf(locID));
                                        if (deleteRet) {
                                            displayToast("删除成功!");
                                            initListView();
                                        }
                                    }
                                })
                        .setNegativeButton("取消", null)
                        .show();
                return true;
            }
        });
    }

    private boolean deleteRecord(SQLiteDatabase sqLiteDatabase, String tableName, int ID) {
        boolean deleteRet = true;
        try {
            sqLiteDatabase.delete(tableName,
                    "ID = ?", new String[]{Integer.toString(ID)});
            Log.d("DDDDDD", "delete success");
        } catch (Exception e) {
            Log.e("SQLITE", "delete error");
            deleteRet = false;
            e.printStackTrace();
        }
        return deleteRet;
    }

    public boolean setHistoryLocation(String bd09Longitude, String bd09Latitude, String wgs84Longitude, String wgs84Latitude) {
        boolean ret = true;
        try {
            if (!bd09Longitude.isEmpty() && !bd09Latitude.isEmpty()) {
                MainActivity.currentPt = new LatLng(Double.valueOf(bd09Latitude), Double.valueOf(bd09Longitude));
                MarkerOptions ooA = new MarkerOptions().position(MainActivity.currentPt).icon(MainActivity.bdA);
                MainActivity.mBaiduMap.clear();
                MainActivity.mBaiduMap.addOverlay(ooA);
                MapStatusUpdate mapstatusupdate = MapStatusUpdateFactory.newLatLng(MainActivity.currentPt);
                MainActivity.mBaiduMap.setMapStatus(mapstatusupdate);
                MainActivity.latLngInfo = wgs84Longitude + "&" + wgs84Latitude;
            }
        } catch (Exception e) {
            ret = false;
            Log.e("UNKNOWN", "setHistoryLocation error");
            e.printStackTrace();
        }
        return ret;
    }

    @Override
    protected void onDestroy() {
        sqLiteDatabase.close();
        sqLiteDatabase = null;
        super.onDestroy();
    }
}
