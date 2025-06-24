package com.example.clnain;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.clnain.smartfactory.tools.DatabaseHelper;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ChartActivity extends AppCompatActivity {

    private static final String TAG = "ChartActivity";
    private String dataType;
    private DatabaseHelper databaseHelper;
    private LineChart lineChart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chart);

        Intent intent = getIntent();
        dataType = intent.getStringExtra("type");

        if (dataType == null || dataType.isEmpty()) {
            Log.e(TAG, "Data type not provided in Intent for ChartActivity.");
            Toast.makeText(this, "无法加载图表：数据类型未知", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(dataType + " - 历史数据");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        Log.d(TAG, "ChartActivity started for type: " + dataType);
        databaseHelper = new DatabaseHelper(this);
        lineChart = findViewById(R.id.lineChart);

        setupChartStyle();
        loadChartData();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private void setupChartStyle() {
        Description description = new Description();
        description.setText(dataType + "历史数据折线图");
        description.setTextSize(12f);
        lineChart.setDescription(description);
        lineChart.setDrawGridBackground(false);
        lineChart.setBackgroundColor(Color.WHITE);
        lineChart.setNoDataText("当前类型暂无历史数据可供显示");
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(true);
        xAxis.setLabelCount(6, true);
        lineChart.getAxisRight().setEnabled(false);
        lineChart.getAxisLeft().setDrawGridLines(true);
        Legend legend = lineChart.getLegend();
        legend.setForm(Legend.LegendForm.LINE);
        legend.setTextSize(11f);
        legend.setTextColor(Color.BLACK);
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        legend.setDrawInside(false);
        lineChart.setDragEnabled(true);
        lineChart.setScaleEnabled(true);
        lineChart.setPinchZoom(true);
    }

    private void loadChartData() {
        LineData lineData = getLineDataFromDb();
        if (lineData != null && lineData.getDataSetCount() > 0 && lineData.getEntryCount() > 0) {
            lineChart.setData(lineData);
            lineChart.animateX(1000);
        } else {
            lineChart.clear();
            Log.w(TAG, "No data or empty dataset to display for type: " + dataType);
        }
        lineChart.invalidate();
    }

    private LineData getLineDataFromDb() {
        ArrayList<Entry> sensorDataEntries = new ArrayList<>();
        populateChartEntriesFromUserDb(sensorDataEntries, 20);

        if (sensorDataEntries.isEmpty()) {
            return null;
        }
        LineDataSet dataSet = new LineDataSet(sensorDataEntries, dataType);
        dataSet.setColor(Color.BLUE);
        dataSet.setCircleColor(Color.BLUE);
        dataSet.setLineWidth(2f);
        dataSet.setCircleRadius(3.5f);
        dataSet.setDrawCircleHole(false);
        dataSet.setValueTextSize(10f);
        dataSet.setValueTextColor(Color.DKGRAY);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(Color.argb(100, 30, 144, 255));
        return new LineData(dataSet);
    }

    private void populateChartEntriesFromUserDb(List<Entry> entries, int maxCount) {
        // ★★★ 调用你的 DatabaseHelper 的 search 方法，它需要 Context ★★★
        List<Float> rawDataList = databaseHelper.search(this, dataType); // `this` (ChartActivity) is a Context
        Log.d(TAG, "Data fetched from DB for type '" + dataType + "': " + rawDataList.size() + " items.");
        entries.clear();
        if (rawDataList.isEmpty()) {
            return;
        }
        List<Float> dataToPlot = new ArrayList<>(rawDataList);
        // 你的数据库触发器限制20条，且按ID降序。图表通常希望X轴旧->新。
        // 所以我们反转获取到的（最新的在前）列表，然后取需要的条数。
        Collections.reverse(dataToPlot); // 现在是 ID 升序 (旧 -> 新)

        int limit = Math.min(dataToPlot.size(), maxCount);
        List<Entry> finalEntries = new ArrayList<>(); // 创建新列表以避免并发修改

        for (int i = 0; i < limit; i++) {
            finalEntries.add(new Entry(i, dataToPlot.get(i)));
        }
        entries.addAll(finalEntries); // 添加到传入的列表
        Log.d(TAG, "Populated chart entries: " + entries.size() + " items.");
    }
}