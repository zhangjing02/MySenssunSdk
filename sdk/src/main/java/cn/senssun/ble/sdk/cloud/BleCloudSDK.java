//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package cn.senssun.ble.sdk.cloud;

import android.content.Context;

import java.util.ArrayList;

import cn.senssun.ble.sdk.util.LOG;


public class BleCloudSDK {
    private static BleCloudSDK mInstance = null;
    ArrayList<CloudOnActionMethod.OnConnectState> mOnConnectStateList = new ArrayList();
    CloudOnActionMethod.OnDisplayDATA mOnDisplayDATA = null;
    CloudOnActionMethod.OnInitService mOnInitService = null;
    ArrayList<CloudOnActionMethod.OnUserInfoStatus> mOnUserInfoStatusList = new ArrayList();
    CloudOnActionMethod.OnAllUsers mOnAllUsers = null;

    public BleCloudSDK() {
    }

    public void InitSDK(Context mContext) {
        BleCloudManager.getInstance().InitSDK(mContext);
    }

    public void stopSDK(Context mContext) {
        BleCloudManager.getInstance().stopSDK(mContext);
    }

    public static synchronized BleCloudSDK getInstance() {
        if(mInstance == null) {
            mInstance = new BleCloudSDK();
        }

        return mInstance;
    }

    public boolean isConnect() {
        return BleCloudManager.getInstance().mBleConnectService == null?false:BleCloudManager.getInstance().mBleConnectService.ismConnect();
    }

    public boolean isInit() {
        return BleCloudManager.getInstance().mBleConnectService != null;
    }

    public boolean ConnectDeviceId(String DeviceId) {
        return BleCloudManager.getInstance().mBleConnectService.connectDeviceId(DeviceId);
    }

    public boolean Connect(String Mac) {
        return BleCloudManager.getInstance().mBleConnectService.connect(Mac);
    }

    public void Disconnect() {
        BleCloudManager.getInstance().mBleConnectService.disconnect();
    }

    public void AddUserInfo(String pin, int sex, int height, int age, int acvitivty, int unit, int weightKG) {
        try {
            BleCloudManager.getInstance().mBleConnectService.AddUserInfo(pin, sex, height, age, acvitivty, unit, weightKG);
        } catch (Exception var9) {
            LOG.logE("BleSDK", "发送脂肪测试命令出错");
        }

    }

    public void DelUserInfo(String pin) {
        try {
            BleCloudManager.getInstance().mBleConnectService.DelUserInfo(pin);
        } catch (Exception var3) {
            LOG.logE("BleSDK", "发送同步历史命令出错");
        }

    }

    public void SendDataCommun(String pin) {
        try {
            BleCloudManager.getInstance().mBleConnectService.DataCommunBuffer(pin);
        } catch (Exception var3) {
            LOG.logE("BleSDK", "发送同步历史命令出错");
        }

    }

    public void SendDataCommunAll(String pin) {
        try {
            BleCloudManager.getInstance().mBleConnectService.DataCommunAllBuffer(pin);
        } catch (Exception var3) {
            LOG.logE("BleSDK", "发送同步历史命令出错");
        }

    }

    public void QueryAllUserInfo() {
        try {
            BleCloudManager.getInstance().mBleConnectService.QueryUserInfoBuffer();
        } catch (Exception var2) {
            LOG.logE("BleSDK", "发送同步历史命令出错");
        }

    }

    public void ResetBuffer() {
        try {
            BleCloudManager.getInstance().mBleConnectService.ResetBuffer();
        } catch (Exception var2) {
            LOG.logE("BleSDK", "发送同步历史命令出错");
        }

    }

    public void setOnConnectState(CloudOnActionMethod.OnConnectState e) {
        this.mOnConnectStateList.add(e);
    }

    public void RemoveOnConnectState(CloudOnActionMethod.OnConnectState e) {
        this.mOnConnectStateList.remove(e);
    }

    public void RemoveAllOnConnectState() {
        this.mOnConnectStateList.clear();
    }

    public void setOnDisplayDATA(CloudOnActionMethod.OnDisplayDATA e) {
        this.mOnDisplayDATA = e;
    }

    public void setOnInitService(CloudOnActionMethod.OnInitService e) {
        this.mOnInitService = e;
    }

    public void setOnUserInfoStatus(CloudOnActionMethod.OnUserInfoStatus e) {
        this.mOnUserInfoStatusList.add(e);
    }

    public void RemoveOnUserInfoStatus(CloudOnActionMethod.OnUserInfoStatus e) {
        this.mOnUserInfoStatusList.remove(e);
    }

    public void RemoveAllOnUserInfoStatus() {
        this.mOnUserInfoStatusList.clear();
    }

    public void setOnAllUsers(CloudOnActionMethod.OnAllUsers e) {
        this.mOnAllUsers = e;
    }
}
