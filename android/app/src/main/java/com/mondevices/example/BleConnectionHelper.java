package com.mondevices.example;

import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;

import java.util.UUID;
import java.util.logging.Logger;

import rx.Observable;
import rx.functions.Action1;

public class BleConnectionHelper {
    private static final Logger log = Logger.getLogger(BleConnectionHelper.class.getSimpleName());

    private static final int ACCELEROMETER_SERVICE = 0xAA10;
    private static final int ACCELEROMETER_CONFIG = 0xAA12;
    private static final int ACCELEROMETER_DATA14 = 0xAA16;
    private static final String UUID_PATTERN_STRING = "0000%04x-0000-1000-8000-00805f9b34fb";
    private static final int DESCRIPTOR = 0x2902;
    private static final byte[] VALUE_ENABLE_ACCELEROMETER = {0x03};

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;
    private static final int STATE_ENABLE_ACC = 3;
    private static final int STATE_SUBSCRIBE_TO_DATA = 3;
    private int mConnectionState = STATE_DISCONNECTED;
    private BluetoothManager mBluetoothManager;
    private BluetoothGatt mBluetoothGatt;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothDevice device;
    private Application application;
    private Action1<XYZ> mXYZAction1;
    private boolean mBleState = false;

    public BleConnectionHelper(Application application) {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        this.application = application;
        mBluetoothManager = (BluetoothManager) application.getSystemService(Context.BLUETOOTH_SERVICE);
        if (mBluetoothManager == null) {
            throw new IllegalStateException("XXYY Unable to initialize BluetoothManager.");
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            throw new IllegalStateException("XXYY Unable to obtain a BluetoothAdapter");
        }

        log.info("BleConnectionHelper() successfully initialized");
    }

    private static UUID getUuid(int shortUuid) {
        return UUID.fromString(String.format(UUID_PATTERN_STRING, shortUuid));
    }

    private void runBleConnection(Action1<XYZ> onValueReceived) {
        mXYZAction1 = onValueReceived;

        final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
            @Override
            public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                log.info("onDescriptorRead()");
            }

            @Override
            public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    log.info("onDescriptorWrite() GATT_SUCCESS: " +
                            " service: " + descriptor.getCharacteristic().getService().getUuid() +
                            " characteristic: " + descriptor.getCharacteristic().getUuid());
                } else {
                    log.warning("onDescriptorWrite received: " + status);
                }
            }

            @Override
            public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                final UUID uuid = characteristic.getUuid();

                if (status == BluetoothGatt.GATT_SUCCESS) {
                    log.info("onCharacteristicWrite GATT_SUCCESS:" +
                            " service: " + characteristic.getService().getUuid() +
                            " characteristic: " + uuid);

                    if (mConnectionState == STATE_ENABLE_ACC) {
                        subscribeForData();
                    }
                } else {
                    log.warning("onCharacteristicWrite received: " + status);
                }
            }

            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    mConnectionState = STATE_CONNECTED;
                    log.info("Connected to GATT server.");
                    // Attempts to discover services after successful connection.
                    log.info("Attempting to start service discovery:" + mBluetoothGatt.discoverServices());

                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    mConnectionState = STATE_DISCONNECTED;
                    close();
                    reconnect(this);
                    log.info("Disconnected from GATT server.");
                }
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    log.info("onServicesDiscovered success");
                    collect();
                } else {
                    reconnect(this);
                    log.warning("onServicesDiscovered received: " + status);
                }
            }

            @Override
            public void onCharacteristicRead(BluetoothGatt gatt,
                                             BluetoothGattCharacteristic characteristic,
                                             int status) {
                if (status == BluetoothGatt.GATT_SUCCESS) {

                    log.info("onCharacteristicRead() characteristic: " + characteristic.toString());
                } else {
                    log.info("onCharacteristicRead() status: " + status + " characteristic: " + characteristic.toString());
                }
            }

            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt,
                                                BluetoothGattCharacteristic characteristic) {
                final UUID uuid = characteristic.getUuid();

                if (uuid.equals(getUuid(ACCELEROMETER_DATA14))) {
                    final long now = System.currentTimeMillis();

                    final byte[] xyz = characteristic.getValue();

                    float x = ((((short) ((xyz[0] & 0xff) << 8)) | (short) (xyz[1] & 0xff)) >> 2) / 4096.0f;
                    if (xyz[0] > 0x7f)
                        x = -(~((((short) ((xyz[0] & 0xff) << 8)) | (short) (xyz[1] & 0xff)) >> 2) + 1) / 4096.0f;

                    float y = ((((short) ((xyz[2] & 0xff) << 8)) | (short) (xyz[3] & 0xff)) >> 2) / 4096.0f;
                    if (xyz[2] > 0x7f)
                        y = -(~((((short) ((xyz[2] & 0xff) << 8)) | (short) (xyz[3] & 0xff)) >> 2) + 1) / 4096.0f;

                    float z = ((((short) ((xyz[4] & 0xff) << 8)) | (short) (xyz[5] & 0xff)) >> 2) / 4096.0f;
                    if (xyz[4] > 0x7f)
                        z = -(~((((short) ((xyz[4] & 0xff) << 8)) | (short) (xyz[5] & 0xff)) >> 2) + 1) / 4096.0f;

                    log.info("onCharacteristicChanged() ts:" + now + " x: " + x + " y:" + y + " z:" + z);
                    final String bytes = String.format("%02X%02X%02X%02X%02X%02X", xyz[5], xyz[4], xyz[3], xyz[2], xyz[1], xyz[0]);
                    log.info("onCharacteristicChanged() bytes: " + bytes);

                    Observable.just(new XYZ(x, y, z)).subscribe(mXYZAction1);

                } else {
                    log.info("onCharacteristicChanged() characteristic: " + characteristic.getUuid());
                }

            }
        };

        /////////////////
        // code is here

        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        mBleState = true;
        mBluetoothGatt = device.connectGatt(application, false, gattCallback);
        log.info("Trying to create a new connection.");

    }

    public void connect(final String address, Action1<XYZ> onValueReceived) throws IllegalStateException {
        if (mBluetoothAdapter == null || address == null) {
            throw new IllegalArgumentException("BluetoothAdapter not initialized or unspecified address.");
        }

        mConnectionState = STATE_CONNECTING;

        device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            throw new IllegalStateException("Device not found. Unable to connect.");
        }

        runBleConnection(onValueReceived);
    }

    private void reconnect(BluetoothGattCallback gattCallback) {
        if (mBleState) {
            disconnect(mBleState);
            close();
            mConnectionState = STATE_CONNECTING;
            mBluetoothGatt = device.connectGatt(application, false, gattCallback);
        }
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect(boolean bleState) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            log.warning("BluetoothAdapter not initialized");
            return;
        }
        mBleState = bleState;
        mBluetoothGatt.disconnect();
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    private void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
        log.info("close() e.");
    }

    private void collect() {
        log.info("collect()");

        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            log.severe("BluetoothAdapter not initialized");
            return;
        }

        final BluetoothGattService accelerometer = mBluetoothGatt.getService(getUuid(ACCELEROMETER_SERVICE));
        final BluetoothGattCharacteristic characteristic = accelerometer.getCharacteristic(getUuid(ACCELEROMETER_CONFIG));
        characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
        characteristic.setValue(VALUE_ENABLE_ACCELEROMETER);
        boolean res = mBluetoothGatt.writeCharacteristic(characteristic);

        mConnectionState = STATE_ENABLE_ACC;

        log.info("collect() e. res = " + res);
    }

    private void subscribeForData() {
        log.info("subscribeForData()");

        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            log.severe("BluetoothAdapter not initialized");
            return;
        }

        final BluetoothGattService accelerometer = mBluetoothGatt.getService(getUuid(ACCELEROMETER_SERVICE));
        final BluetoothGattCharacteristic data = accelerometer.getCharacteristic(getUuid(ACCELEROMETER_DATA14));
        final UUID descriptorUUID = getUuid(DESCRIPTOR);
        log.info("subscribeForData() descriptor UUID: " + descriptorUUID.toString());
        final BluetoothGattDescriptor descriptor = data.getDescriptor(descriptorUUID);
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);

        mBluetoothGatt.setCharacteristicNotification(data, true);
        log.info("subscribeForData() descriptor: " + descriptor.toString());
        boolean res = mBluetoothGatt.writeDescriptor(descriptor);

        mConnectionState = STATE_SUBSCRIBE_TO_DATA;

        log.info("subscribeForData() e. res = " + res);
    }

    class XYZ {
        private float x;
        private float y;
        private float z;

        public XYZ(float x, float y, float z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public float getX() {
            return x;
        }

        public float getY() {
            return y;
        }

        public float getZ() {
            return z;
        }
    }
}
