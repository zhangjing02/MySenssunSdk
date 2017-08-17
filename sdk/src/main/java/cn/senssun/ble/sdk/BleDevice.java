//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package cn.senssun.ble.sdk;

import android.bluetooth.BluetoothDevice;
import android.os.Parcel;
import android.os.Parcelable;

public class BleDevice implements Parcelable {
    private BluetoothDevice bluetoothDevice;
    private String deviceName;
    private int deviceType;
    private byte[] broadCast;
    private String manuData;
    private int Rssi;
    public static final int NullScale = -1;
    public static final int BodyBroadScale = 1;
    public static final int FatScale = 2;
    public static final int FoodScale = 3;
    public static final int GrowthScale = 4;
    public static final int FatAlarmScale = 5;
    public static final int EightBodyScale = 6;
    public static final int BodyScale = 7;
    public static final int BodyAlarmScale = 8;
    public static final int Eqi99Scale = 9;
    public static final int Eqi912Scale = 10;
    public static final int SportScale = 11;
    public static final int CloudScale = 12;
    public static final int WiFiFatScale = 13;
    public static final Creator<BleDevice> CREATOR = new Creator() {
        public BleDevice createFromParcel(Parcel in) {
            BluetoothDevice bluetoothDevice = (BluetoothDevice)in.readParcelable(BluetoothDevice.class.getClassLoader());
            BleDevice face = new BleDevice();
            face.bluetoothDevice = bluetoothDevice;
            face.manuData = in.readString();
            face.deviceName = in.readString();
            face.deviceType = in.readInt();
            face.Rssi = in.readInt();
            int broadCastLength = in.readInt();
            byte[] bytes = new byte[broadCastLength];
            in.readByteArray(bytes);
            face.broadCast = bytes;
            return face;
        }

        public BleDevice[] newArray(int size) {
            return new BleDevice[size];
        }
    };

    public BleDevice() {
    }

    public BluetoothDevice getBluetoothDevice() {
        return this.bluetoothDevice;
    }

    public void setBluetoothDevice(BluetoothDevice bluetoothDevice) {
        this.bluetoothDevice = bluetoothDevice;
    }

    public byte[] getBroadCast() {
        return this.broadCast;
    }

    public void setBroadCast(byte[] broadCast) {
        this.broadCast = broadCast;
    }

    public void setManuData(String manuData) {
        this.manuData = manuData;
    }

    public String getManuData() {
        return this.manuData;
    }

    public int getRssi() {
        return this.Rssi;
    }

    public void setRssi(int rssi) {
        this.Rssi = rssi;
    }

    public String getDeviceName() {
        return this.deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public int getDeviceType() {
        return this.deviceType;
    }

    public void setDeviceType(int deviceType) {
        this.deviceType = deviceType;
    }

    public boolean equals(Object obj) {
        if(obj instanceof BleDevice) {
            BleDevice u = (BleDevice)obj;
            return this.bluetoothDevice.getAddress().equals(u.getBluetoothDevice().getAddress());
        } else {
            return super.equals(obj);
        }
    }

    public String toString() {
        return this.getBluetoothDevice().getAddress();
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeParcelable(this.bluetoothDevice, flags);
        out.writeString(this.manuData);
        out.writeString(this.deviceName);
        out.writeInt(this.deviceType);
        out.writeInt(this.Rssi);
        out.writeInt(this.broadCast.length);
        out.writeByteArray(this.broadCast);
    }
}
