//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package cn.senssun.ble.sdk;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothAdapter.LeScanCallback;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import cn.senssun.ble.sdk.util.ScannerServiceParser;

public class BleScan {
    private String TAG = "BleScan";
    public static final String EXTRAS_DEVICE = "DEVICE_ADDRESS";
    private Handler mHandler;
    private BluetoothAdapter mBtAdapter = null;
    private BluetoothManager mBluetoothManager = null;
    private LeScanCallback mLeScanCallback;
    private BluetoothLeScanner bluetoothLeScanner;
    private ScanCallback mScanCallback;
    private boolean mScanning = false;
    private BleScan.MyHandler myHandler;
    private Context mContext;
    @SuppressLint({"NewApi"})
    Runnable scanStartForHighVersionRunnable = new Runnable() {
        public void run() {
            if(BleScan.this.mScanning) {
                BleScan.this.mScanning = false;
                if(BleScan.this.mBtAdapter.isEnabled()) {
                    BleScan.this.bluetoothLeScanner.stopScan(BleScan.this.mScanCallback);
                }

                if(BleScan.this.mOnScanStatus != null) {
                    BleScan.this.mOnScanStatus.OnStatus(0);
                }
            }

        }
    };
    Runnable scanStartForLowVersionRunnable = new Runnable() {
        public void run() {
            if(BleScan.this.mScanning) {
                BleScan.this.mScanning = false;
                BleScan.this.mBtAdapter.stopLeScan(BleScan.this.mLeScanCallback);
                if(BleScan.this.mOnScanStatus != null) {
                    BleScan.this.mOnScanStatus.OnStatus(0);
                }
            }

        }
    };
    BleScan.OnScanListening mOnScanListening = null;
    BleScan.OnScanStatus mOnScanStatus = null;

    public BleScan() {
    }

    @SuppressLint({"NewApi"})
    public void Create(Context mContext) {
        this.mContext = mContext;
        this.mBluetoothManager = (BluetoothManager)mContext.getSystemService("bluetooth");
        this.mBtAdapter = this.mBluetoothManager.getAdapter();
        this.mHandler = new Handler();
        this.myHandler = new BleScan.MyHandler();
        if(VERSION.SDK_INT < 21) {
            this.mLeScanCallback = new LeScanCallback() {
                public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
                    (new Thread(new Runnable() {
                        public void run() {
                            Message m = new Message();
                            Bundle data = new Bundle();
                            data.putByteArray("ScanRecord", scanRecord);
                            data.putParcelable("BluetoothDevice", device);
                            data.putInt("Rssi", rssi);
                            m.setData(data);
                            BleScan.this.myHandler.sendMessage(m);
                        }
                    })).start();
                }
            };
        } else {
            this.mScanCallback = new ScanCallback() {
                public void onScanResult(int callbackType, ScanResult result) {
                    Message m = new Message();
                    Bundle data = new Bundle();
                    data.putByteArray("ScanRecord", result.getScanRecord().getBytes());
                    data.putParcelable("BluetoothDevice", result.getDevice());
                    data.putInt("Rssi", result.getRssi());
                    m.setData(data);
                    BleScan.this.myHandler.sendMessage(m);
                }

                public void onBatchScanResults(List<ScanResult> results) {
                    System.out.println("BLE// onBatchScanResults");
                    Iterator var3 = results.iterator();

                    while(var3.hasNext()) {
                        ScanResult sr = (ScanResult)var3.next();
                        Log.i("ScanResult - Results", sr.toString());
                    }

                }

                public void onScanFailed(int errorCode) {
                    if(BleScan.this.mOnScanStatus != null) {
                        BleScan.this.mOnScanStatus.OnStatus(-1);
                    }

                    System.out.println("BLE// onScanFailed");
                    Log.e("Scan Failed", "Error Code: " + errorCode);
                }
            };
        }

    }

    @SuppressLint({"NewApi"})
    public void ScanLeStartDevice(int SCAN_PERIOD) {
        if(this.mBtAdapter.isEnabled()) {
            if(VERSION.SDK_INT < 21) {
                this.ScanLeStartDeviceForLowVersion(SCAN_PERIOD);
            } else {
                this.ScanLeStartDeviceForHighVersion(SCAN_PERIOD);
            }

        }
    }

    @SuppressLint({"NewApi"})
    public void scanLeStopDevice() {
        this.mScanning = false;
        if(this.mBtAdapter.isEnabled()) {
            if(VERSION.SDK_INT < 21) {
                this.mHandler.removeCallbacks(this.scanStartForLowVersionRunnable);
                this.mBtAdapter.stopLeScan(this.mLeScanCallback);
            } else {
                this.mHandler.removeCallbacks(this.scanStartForHighVersionRunnable);
                this.bluetoothLeScanner = this.mBtAdapter.getBluetoothLeScanner();
                this.bluetoothLeScanner.stopScan(this.mScanCallback);
            }

        }
    }

    @SuppressLint({"NewApi"})
    private void ScanLeStartDeviceForHighVersion(int SCAN_PERIOD) {
        this.bluetoothLeScanner = this.mBtAdapter.getBluetoothLeScanner();
        this.mHandler.postDelayed(this.scanStartForHighVersionRunnable, (long)SCAN_PERIOD);
        if(!this.mScanning && this.mBtAdapter.isEnabled()) {
            this.mScanning = true;
            this.bluetoothLeScanner.startScan(this.mScanCallback);
        }

    }

    private void ScanLeStartDeviceForLowVersion(int SCAN_PERIOD) {
        this.mHandler.postDelayed(this.scanStartForLowVersionRunnable, (long)SCAN_PERIOD);
        if(!this.mScanning) {
            this.mScanning = true;
            this.mBtAdapter.startLeScan(this.mLeScanCallback);
        }

    }

    public void setOnScanListening(BleScan.OnScanListening e) {
        this.mOnScanListening = e;
    }

    public void setOnScanStatus(BleScan.OnScanStatus e) {
        this.mOnScanStatus = e;
    }

    class MyHandler extends Handler {
        MyHandler() {
        }

        public void handleMessage(Message msg) {
            Bundle data = msg.getData();
            int rssi = data.getInt("Rssi");
            byte[] scanRecord = data.getByteArray("ScanRecord");
            BluetoothDevice device = (BluetoothDevice)data.getParcelable("BluetoothDevice");
            if(device.getName() != null) {
                String Type_1 = "ifit scale-senssun body";
                String Type_2 = "ble to uart_2-senssun fat-jointown";
                String Type_3 = "senssun food";
                String Type_4 = "senssun growth-aguscales";
                String Type_5 = "senssun fat_a";
                String Type_6 = "senssun fat pro";
                String Type_8 = "senssun body_a";
                String Type_9_1 = "eqi-99";
                String Type_9_2 = "eqi-912";
                String Type_10 = "moving-id101-id107";
                String Type_11 = "senssun cloud";
                String Type_12= "if_b2a";
                BleDevice bleDevice = null;
                bleDevice = new BleDevice();
                bleDevice.setBluetoothDevice(device);
                bleDevice.setRssi(rssi);
                bleDevice.setBroadCast(scanRecord);
                byte[] manuByte = ScannerServiceParser.decodeManufacturer(scanRecord);
                StringBuffer manuBuffer = new StringBuffer();
                if(manuByte != null) {
                    byte[] var23 = manuByte;
                    int var22 = manuByte.length;

                    for(int var21 = 0; var21 < var22; ++var21) {
                        byte deviceName = var23[var21];
                        String ms = String.format("%02X ", new Object[]{Byte.valueOf(deviceName)}).trim();
                        manuBuffer.append(ms);
                    }
                }

                bleDevice.setManuData(manuBuffer.toString());
                String var25 = device.getName().toLowerCase(Locale.ENGLISH);
                if(Type_1.contains(var25)) {
                    if(scanRecord[11] == -1 && scanRecord[12] == -86) {
                        bleDevice.setDeviceType(1);
                    } else {
                        bleDevice.setDeviceType(7);
                    }
                } else if(Type_2.contains(var25)) {
                    bleDevice.setDeviceType(2);
                } else if(var25.contains(Type_3)) {
                    bleDevice.setDeviceType(3);
                } else if(!var25.contains(Type_4.split("-")[0]) && !var25.contains(Type_4.split("-")[1])) {
                    if(var25.contains(Type_5)) {
                        bleDevice.setDeviceType(5);
                    } else if(var25.contains(Type_6)) {
                        bleDevice.setDeviceType(6);
                    } else if(var25.contains(Type_8)) {
                        bleDevice.setDeviceType(8);
                    } else if(var25.contains(Type_9_1)) {
                        bleDevice.setDeviceType(9);
                    } else if(var25.contains(Type_9_2)) {
                        bleDevice.setDeviceType(10);
                    } else if(!var25.contains(Type_10.split("-")[0]) && !var25.contains(Type_10.split("-")[1]) && !var25.contains(Type_10.split("-")[2])) {
                        if(var25.contains(Type_11)) {
                            bleDevice.setDeviceType(12);
                        }else if (var25.contains(Type_12)){
                            bleDevice.setDeviceType(13);
                        } else {
                            Log.e(BleScan.this.TAG, "device.getName()" + device.getName().trim());
                            return;
                        }
                    } else {
                        bleDevice.setDeviceType(11);
                    }
                } else {
                    bleDevice.setDeviceType(4);
                }

                if(bleDevice != null && BleScan.this.mOnScanListening != null) {
                    BleScan.this.mOnScanListening.OnListening(bleDevice);
                }

            }
        }
    }

    public interface OnScanListening {
        void OnListening(BleDevice var1);
    }

    public interface OnScanStatus {
        void OnStatus(int var1);
    }
}
