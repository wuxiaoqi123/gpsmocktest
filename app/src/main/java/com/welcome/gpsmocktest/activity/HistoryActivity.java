package com.welcome.gpsmocktest.activity;

import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.welcome.gpsmocktest.R;
import com.welcome.gpsmocktest.db.HistoryDBHelper;

import java.util.ArrayList;
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
        //TODO
        return data;
    }

    private void initListener() {

    }
}
