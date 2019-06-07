package com.mondevices.example;

import android.app.Activity;
import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;

import java.util.logging.Logger;

// sample
// https://github.com/googlesamples/android-BluetoothLeGatt

public class BleScanHelper {
    private static final Logger log = Logger.getLogger(BleScanHelper.class.getSimpleName());
    private static final int REQUEST_ENABLE_BT = 1;
    public static int BLE_SCAN_TIMEOUT_MS = 20000;
    private Application application;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothManager bluetoothManager;
    private BluetoothAdapter.LeScanCallback mLeScanCallback;

    public BleScanHelper(Activity activity) {

        application = activity.getApplication();
        bluetoothManager = (BluetoothManager) application.getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        // BLE Stuff
        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!bluetoothAdapter.isEnabled()) {
            if (!bluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                activity.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }
    }

    public rx.Observable<String> startScanForBle() {
        final long stopTime_ms = System.currentTimeMillis() + BLE_SCAN_TIMEOUT_MS;

        return rx.Observable.create(subscriber -> {
            mLeScanCallback = (device, rssi, scanRecord) -> {
                final String devName = device.getName();
                if (devName != null && devName.toLowerCase().contains("monbaby")) {
                    log.info("Device: " + device.getName() + " addr: " + device.toString());
                    subscriber.onNext(device.toString());
                }

                // FIXME: need redesign, could stuck if no any BLE around
                // but practically it's impossible (need to implement timeout Observer)
                if (System.currentTimeMillis() > stopTime_ms) {
                    subscriber.onCompleted();
                }
            };

            log.info("startScanForBle() START scanning");
            bluetoothAdapter.startLeScan(mLeScanCallback);
        });
    }

    public void stopScanForBle() {
        log.info("stopScanForBle() STOP scanning");
        bluetoothAdapter.stopLeScan(mLeScanCallback);
    }
}
