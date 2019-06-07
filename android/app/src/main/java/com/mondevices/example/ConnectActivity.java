package com.mondevices.example;

import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;

import butterknife.BindView;
import butterknife.ButterKnife;
import rx.Subscription;

public class ConnectActivity extends AppCompatActivity implements
        OnChartValueSelectedListener {

    private static final String TAG = ConnectActivity.class.getSimpleName();
    private static final float MAX_LIMIT_Y = 2f;
    private static final float MIN_LIMIT_Y = -2f;
    private static final float NUMBER_OF_DISPLAYED_POINTS = 100;
    private static final float LINE_WIDTH = 1f;
    private static final int X_AXIS_INDEX = 0;
    private static final int Y_AXIS_INDEX = 1;
    private static final int Z_AXIS_INDEX = 2;

    @BindView(R.id.realTimeChart)
    protected LineChart mChart;
    @BindView(R.id.connection_progress)
    protected ProgressBar mConnectionProgress;

    // experiment
    private BleConnectionHelper mBleConnectionHelper;
    private Subscription mConnectSubscription;

    private String mBleMac;
    private int mCount = 0;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "onCreate");
        setContentView(R.layout.activity_connect);
        ButterKnife.bind(this);

        mBleMac = getIntent().getStringExtra(ScanActivity.BLE_MAC);

        Log.d(TAG, "mBleMac = " + mBleMac);


    }

    @Override
    protected void onPause() {

        if (mConnectSubscription != null) {
            mConnectSubscription.unsubscribe();
        }
        if (mBleConnectionHelper != null) {
            mBleConnectionHelper.disconnect(false);
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();

        connectToBle(mBleMac);
        setupChart();
    }

    @Override
    public void onValueSelected(Entry e, Highlight h) {
        Log.i("Entry selected", e.toString());
    }

    @Override
    public void onNothingSelected() {
        Log.i("Nothing selected", "Nothing selected.");
    }


    private void setupChart() {
        mChart.setOnChartValueSelectedListener(this);

        // enable description text
        mChart.getDescription().setEnabled(true);

        // enable touch gestures
        mChart.setTouchEnabled(false);

        // enable scaling and dragging
        mChart.setDragEnabled(false);
        mChart.setScaleEnabled(false);
        mChart.setDrawGridBackground(true);

        // if disabled, scaling can be done on x- and y-axis separately
        mChart.setPinchZoom(true);

        // set an alternative background color
        mChart.setBackgroundColor(Color.LTGRAY);

        LineData data = new LineData();
        //data.setValueTextColor(Color.WHITE);

        // add empty data
        mChart.setData(data);

        // get the legend (only possible after setting data)
        Legend l = mChart.getLegend();

        // modify the legend ...
        l.setForm(Legend.LegendForm.LINE);
        //l.setTypeface(mTfLight);
        l.setTextColor(Color.GRAY);

        XAxis xl = mChart.getXAxis();
        //xl.setTypeface(mTfLight);
        xl.setTextColor(Color.GRAY);
        xl.setDrawGridLines(true);
        xl.setAvoidFirstLastClipping(true);
        xl.setEnabled(true);

        YAxis leftAxis = mChart.getAxisLeft();
        //leftAxis.setTypeface(mTfLight);
        leftAxis.setTextColor(Color.GRAY);
        leftAxis.setAxisMaximum(MAX_LIMIT_Y);
        leftAxis.setAxisMinimum(MIN_LIMIT_Y);
        leftAxis.setDrawGridLines(true);

        YAxis rightAxis = mChart.getAxisRight();
        rightAxis.setEnabled(false);
    }

    private void addChartEntry(BleConnectionHelper.XYZ xyz) {

        mConnectionProgress.setVisibility(View.INVISIBLE);

        LineData data = mChart.getData();

        if (data != null) {

            ILineDataSet setX = data.getDataSetByIndex(X_AXIS_INDEX);
            ILineDataSet setY = data.getDataSetByIndex(Y_AXIS_INDEX);
            ILineDataSet setZ = data.getDataSetByIndex(Z_AXIS_INDEX);
            // set.addChartEntry(...); // can be called as well

            if (setX == null) {
                setX = createChartSet("X", Color.RED);
                data.addDataSet(setX);
            }
            if (setY == null) {
                setY = createChartSet("Y", Color.GREEN);
                data.addDataSet(setY);
            }
            if (setZ == null) {
                setZ = createChartSet("Z", Color.BLUE);
                data.addDataSet(setZ);
            }

            Entry entryX = new Entry(setX.getEntryCount(), xyz.getX());
            Entry entryY = new Entry(setY.getEntryCount(), xyz.getY());
            Entry entryZ = new Entry(setZ.getEntryCount(), xyz.getZ());

            data.addEntry(entryX, X_AXIS_INDEX);
            data.addEntry(entryY, Y_AXIS_INDEX);
            data.addEntry(entryZ, Z_AXIS_INDEX);
            data.notifyDataChanged();

            mChart.notifyDataSetChanged();

            mChart.setVisibleXRangeMaximum(NUMBER_OF_DISPLAYED_POINTS);

            mCount = data.getEntryCount();

            mChart.moveViewToX(mCount);
        }
    }

    private LineDataSet createChartSet(String name, int color) {

        LineDataSet set = new LineDataSet(null, name);
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setColor(color);
        set.setCircleColor(Color.TRANSPARENT);
        set.setLineWidth(LINE_WIDTH);
        set.setDrawCircleHole(false);
        set.setDrawValues(false);
        return set;
    }

    public void
    connectToBle(String mac) {

        mConnectionProgress.setVisibility(View.VISIBLE);

        mBleConnectionHelper = new BleConnectionHelper(this.getApplication());

        try {
            mBleConnectionHelper.connect(mac, this::addChartEntry);
        } catch (IllegalStateException ex) {
            Toast.makeText(this, ex.toString(), Toast.LENGTH_SHORT).show();
            finish();
        }
    }

}
