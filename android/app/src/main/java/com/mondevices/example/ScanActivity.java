package com.mondevices.example;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.logging.Logger;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnItemClick;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;

public class ScanActivity extends AppCompatActivity {
    public static final String BLE_MAC = "mBleMac";
    private static final String TAG = ScanActivity.class.getSimpleName();
    private final Logger log = Logger.getLogger(TAG);

    @BindView(R.id.ble_scan_button)
    protected FloatingActionButton mBleScanButton;
    @BindView(R.id.ble_list_view)
    protected ListView mBleDevicesListView;
    @BindView(R.id.textView)
    protected TextView mTitleTextView;
    @BindView(R.id.scan_progress)
    protected ProgressBar mScanProgress;

    private ArrayAdapter<String> mBleDeviceAdapter;
    private ArrayList<String> mDeviceList;
    private BleScanHelper mBleScanHelper;
    private Subscription mBleScanSubstcription;

    @OnItemClick(R.id.ble_list_view)
    protected void onItemClick(AdapterView<?> parent, int position) {
        final String bleMac = mBleDeviceAdapter.getItem(position);
        mBleScanSubstcription.unsubscribe();
        log.info("pos: " + position + " device: " + bleMac);

        Intent intent = new Intent(this, ConnectActivity.class);

        Log.d(TAG, "bleMac = " + bleMac);
        intent.putExtra(BLE_MAC, bleMac);
        startActivity(intent);
    }

    @OnClick(R.id.ble_scan_button)
    protected void onClickScanButton(View v) {
        runBleScan();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);
        ButterKnife.bind(this);
        log.info("onCreate ()");

        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "BLE doesnt supported", Toast.LENGTH_SHORT).show();
            finish();
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth doesnt supported", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

    }

    @Override
    protected void onResume() {
        super.onResume();

        // BLE
        mDeviceList = new ArrayList<String>();
        mBleDeviceAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, mDeviceList);
        mBleDevicesListView.setAdapter(mBleDeviceAdapter);

        /*if (!mBleMac.isEmpty()) {
            log.info("onResume() has cached addr for a button. addr: " + mBleMac);
        }*/

        if (checkPermissions()) {
            runBleScan();
        }
    }

    private boolean checkPermissions() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    1);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case 1: {
                if (checkPermissions()) {
                    runBleScan();
                }
            }
        }
    }

    @Override
    protected void onPause() {

        if (mBleScanSubstcription != null) {
            mBleScanSubstcription.unsubscribe();
        }
        mBleScanHelper.stopScanForBle();

        log.info("onPause ()");
        super.onPause();
    }


    private void runBleScan() {

        mBleScanButton.setVisibility(View.INVISIBLE);
        mScanProgress.setVisibility(View.VISIBLE);

        mBleScanHelper = new BleScanHelper(this);

        mDeviceList.clear();
        mBleDeviceAdapter.notifyDataSetChanged();

        //mBleScanButton.setEnabled(false);

        mBleScanSubstcription = mBleScanHelper.startScanForBle()
                .observeOn(AndroidSchedulers.mainThread())
                .doOnUnsubscribe(() -> mBleScanHelper.stopScanForBle())
                .subscribe(new Subscriber<String>() {
                    @Override
                    public void onCompleted() {
                        //mBleScanButton.setText("Scanned");
                        //mBleScanButton.setTextColor(Color.BLACK);
                        mBleScanButton.setVisibility(View.VISIBLE);
                        mScanProgress.setVisibility(View.INVISIBLE);
                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(String device) {
                        log.info("mBleScanSubstcription.onNext() device:" + device);
                        if (!mDeviceList.contains(device)) {
                            mBleDeviceAdapter.add(device);
                        }
                    }
                });
    }

}
