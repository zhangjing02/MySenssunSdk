//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package cn.senssun.ble.sdk.cloud;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import cn.senssun.ble.sdk.BluetoothBuffer;
import cn.senssun.ble.sdk.entity.SysUserInfo;
import cn.senssun.ble.sdk.util.LOG;


public class BleCloudConnectService extends Service {
    private static final String TAG = "BleConnectService";
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGatt mBluetoothGatt;
    private int mConnectionState = 0;
    public final UUID SERVICE_UUID = UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb");
    public final UUID CHARACTERISTIC_WRITE_UUID = UUID.fromString("0000fff2-0000-1000-8000-00805f9b34fb");
    public final UUID CHARACTERISTIC_NOTIFY_UUID = UUID.fromString("0000fff1-0000-1000-8000-00805f9b34fb");
    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;
    private Handler mSendDataHandler;
    private Handler mHandler;
    private List<String> mSendDataList = new ArrayList();
    private boolean broastStop = false;
    private ArrayList<SysUserInfo> userInfos = new ArrayList();
    private BluetoothGattCharacteristic mWriteCharacteristic;
    private StringBuffer stringBuffer = new StringBuffer();
    private String mBluetoothDeviceAddress;
    private boolean misSend;
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            LOG.logE("BleConnectService", "status: " + status + " newState: " + newState);
            String intentAction;
            if(newState == 2) {
                if(status != 133 && status != 129) {
                    intentAction = "cn.senssun.ble.sdk.ACTION_GATT_CONNECTED";
                    BleCloudConnectService.this.mConnectionState = 2;
                    BleCloudConnectService.this.broadcastUpdate(intentAction);
                    LOG.logI("BleConnectService", "Connected to GATT server.");
                    LOG.logI("BleConnectService", "Attempting to start service discovery:" + BleCloudConnectService.this.mBluetoothGatt.discoverServices());
                } else {
                    BleCloudConnectService.this.disconnect();
                }
            } else if(newState == 0) {
                intentAction = "cn.senssun.ble.sdk.ACTION_GATT_DISCONNECTED";
                BleCloudConnectService.this.mConnectionState = 0;
                LOG.logI("BleConnectService", "Disconnected from GATT server.");
                BleCloudConnectService.this.broadcastUpdate(intentAction);
            }

        }

        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if(status != 0 || !BleCloudConnectService.this.displayGattServices(BleCloudConnectService.this.getSupportedGattServices())) {
                BleCloudConnectService.this.disconnect();
            }

        }

        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if(status == 0) {
                BleCloudConnectService.this.broadcastUpdate("cn.senssun.ble.sdk.ACTION_DATA_READ", characteristic);
            }

        }

        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            BleCloudConnectService.this.broadcastUpdate("cn.senssun.ble.sdk.ACTION_DATA_NOTIFY", characteristic);
        }
    };
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch(action.hashCode()) {
                case 77575096:
                    if(action.equals("cn.senssun.ble.sdk.ACTION_DATA_NOTIFY")) {
                        BleCloudConnectService.this.misSend = true;
                        String data = intent.getStringExtra("cn.senssun.ble.sdk.EXTRA_DATA");
                        String[] strdata = data.split("-");
                        String var7;
                        switch((var7 = strdata[5]).hashCode()) {
                            case 1539:
                                if(var7.equals("03")) {
                                    String var8;
                                    ArrayList delbuffer;
                                    ArrayList userInfo;
                                    String buffer;
                                    Iterator out;
                                    String[] out1;
                                    String delbuffer1;
                                    String userInfo2;
                                    Iterator buffer1;
                                    String[] out2;
                                    label189:
                                    switch((var8 = strdata[6]).hashCode()) {
                                        case 1785:
                                            if(!var8.equals("81")) {
                                                break;
                                            }

                                            String var9;
                                            switch((var9 = strdata[7]).hashCode()) {
                                                case 1536:
                                                    if(var9.equals("00") && BleCloudConnectService.this.mOnServiceDisplayStatus != null) {
                                                        BleCloudConnectService.this.mOnServiceDisplayStatus.OnStatus("result-status-UserInfoNew");
                                                    }
                                                    break;
                                                case 1537:
                                                    if(var9.equals("01") && BleCloudConnectService.this.mOnServiceDisplayStatus != null) {
                                                        BleCloudConnectService.this.mOnServiceDisplayStatus.OnStatus("result-status-UserInfoEdit");
                                                    }
                                                    break;
                                                case 1538:
                                                    if(var9.equals("02") && BleCloudConnectService.this.mOnServiceDisplayStatus != null) {
                                                        BleCloudConnectService.this.mOnServiceDisplayStatus.OnStatus("result-status-UserInfoDel");
                                                    }
                                                    break;
                                                case 1539:
                                                    if(var9.equals("03") && BleCloudConnectService.this.mOnServiceDisplayStatus != null) {
                                                        BleCloudConnectService.this.mOnServiceDisplayStatus.OnStatus("result-status-UserInfoFull");
                                                    }
                                            }

                                            if(BleCloudConnectService.this.mSendDataList.size() == 0) {
                                                break;
                                            }

                                            if(strdata[7].equals("02")) {
                                                Iterator userInfo3 = BleCloudConnectService.this.mSendDataList.iterator();

                                                while(userInfo3.hasNext()) {
                                                    delbuffer1 = (String)userInfo3.next();
                                                    LOG.logI("BleConnectService", "里面有1：" + delbuffer1);
                                                }

                                                delbuffer1 = (String)BleCloudConnectService.this.mSendDataList.get(0);
                                                String[] userInfo4 = delbuffer1.split("-");
                                                if(userInfo4[6].equals("01")) {
                                                    BleCloudConnectService.this.mSendDataList.remove(delbuffer1);
                                                }
                                                break;
                                            }

                                            delbuffer = new ArrayList();
                                            buffer1 = BleCloudConnectService.this.mSendDataList.iterator();

                                            while(buffer1.hasNext()) {
                                                userInfo2 = (String)buffer1.next();
                                                out2 = userInfo2.split("-");
                                                if(out2[6].equals("01")) {
                                                    delbuffer.add(userInfo2);
                                                }
                                            }

                                            buffer1 = delbuffer.iterator();

                                            while(buffer1.hasNext()) {
                                                userInfo2 = (String)buffer1.next();
                                                BleCloudConnectService.this.mSendDataList.remove(userInfo2);
                                            }
                                            break;
                                        case 1786:
                                            if(!var8.equals("82")) {
                                                break;
                                            }

                                            switch((delbuffer1 = strdata[7]).hashCode()) {
                                                case 1536:
                                                    if(delbuffer1.equals("00")) {
                                                        break label189;
                                                    }
                                                default:
                                                    if(BleCloudConnectService.this.mSendDataList.size() == 0) {
                                                        break label189;
                                                    }

                                                    userInfo = new ArrayList();
                                                    out = BleCloudConnectService.this.mSendDataList.iterator();
                                            }

                                            while(out.hasNext()) {
                                                buffer = (String)out.next();
                                                out1 = buffer.split("-");
                                                if(out1[6].equals("02")) {
                                                    userInfo.add(buffer);
                                                }
                                            }

                                            out = userInfo.iterator();

                                            while(true) {
                                                if(!out.hasNext()) {
                                                    break label189;
                                                }

                                                buffer = (String)out.next();
                                                BleCloudConnectService.this.mSendDataList.remove(buffer);
                                            }
                                        case 1787:
                                            if(!var8.equals("83") || BleCloudConnectService.this.mSendDataList.size() == 0) {
                                                break;
                                            }

                                            delbuffer = new ArrayList();
                                            buffer1 = BleCloudConnectService.this.mSendDataList.iterator();

                                            while(buffer1.hasNext()) {
                                                userInfo2 = (String)buffer1.next();
                                                out2 = userInfo2.split("-");
                                                if(out2[6].equals("02")) {
                                                    delbuffer.add(userInfo2);
                                                }
                                            }

                                            buffer1 = delbuffer.iterator();

                                            while(true) {
                                                if(!buffer1.hasNext()) {
                                                    break label189;
                                                }

                                                userInfo2 = (String)buffer1.next();
                                                BleCloudConnectService.this.mSendDataList.remove(userInfo2);
                                            }
                                        case 1791:
                                            if(var8.equals("87")) {
                                                if(BleCloudConnectService.this.mSendDataList.size() != 0) {
                                                    userInfo = new ArrayList();
                                                    out = BleCloudConnectService.this.mSendDataList.iterator();

                                                    while(out.hasNext()) {
                                                        buffer = (String)out.next();
                                                        out1 = buffer.split("-");
                                                        if(out1[6].equals("07")) {
                                                            userInfo.add(buffer);
                                                        }
                                                    }

                                                    out = userInfo.iterator();

                                                    while(out.hasNext()) {
                                                        buffer = (String)out.next();
                                                        BleCloudConnectService.this.mSendDataList.remove(buffer);
                                                    }
                                                }

                                                if(!(strdata[9] + strdata[10]).equals("0000")) {
                                                    SysUserInfo userInfo1 = new SysUserInfo();
                                                    userInfo1.setSerialNum(Integer.valueOf(strdata[8], 16).intValue());
                                                    userInfo1.setPIN(strdata[9] + strdata[10]);
                                                    userInfo1.setSex(Integer.valueOf(strdata[11], 16).intValue());
                                                    userInfo1.setHeight(Integer.valueOf(strdata[12], 16).intValue());
                                                    userInfo1.setAge(Integer.valueOf(strdata[13], 16).intValue());
                                                    userInfo1.setActivity(Integer.valueOf(strdata[14], 16).intValue());
                                                    userInfo1.setWeight(Integer.valueOf(strdata[15], 16).intValue());
                                                    if(!BleCloudConnectService.this.userInfos.contains(userInfo1)) {
                                                        BleCloudConnectService.this.userInfos.add(userInfo1);
                                                    }
                                                }

                                                if(Integer.valueOf(strdata[7], 16).intValue() == Integer.valueOf(strdata[8], 16).intValue() + 1 && BleCloudConnectService.this.mOnAllUserInfoDATA != null) {
                                                    if(Integer.valueOf(strdata[7], 16).intValue() == BleCloudConnectService.this.userInfos.size()) {
                                                        BleCloudConnectService.this.mOnAllUserInfoDATA.OnUsers(BleCloudConnectService.this.userInfos, true);
                                                    } else {
                                                        BleCloudConnectService.this.mOnAllUserInfoDATA.OnUsers(BleCloudConnectService.this.userInfos, false);
                                                    }
                                                }
                                            }
                                    }
                                }
                        }

                        if(BleCloudConnectService.this.mOnServiceDisplayDATA != null) {
                            BleCloudConnectService.this.mOnServiceDisplayDATA.OnDATA(data);
                        }
                    }
                    break;
                case 861991242:
                    if(action.equals("cn.senssun.ble.sdk.ACTION_GATT_CONNECTED")) {
                        BleCloudConnectService.this.mHandler.removeCallbacks(BleCloudConnectService.this.mConnectingRunnable);
                        if(BleCloudConnectService.this.mOnServiceDisplayStatus != null) {
                            BleCloudConnectService.this.mOnServiceDisplayStatus.OnStatus("result-status-connect");
                        }
                    }
                    break;
                case 966197306:
                    if(action.equals("cn.senssun.ble.sdk.ACTION_GATT_DISCONNECTED")) {
                        BleCloudConnectService.this.close();
                        BleCloudConnectService.this.misSend = false;
                        BleCloudConnectService.this.mWriteCharacteristic = null;
                        BleCloudConnectService.this.mSendDataList.clear();
                        BleCloudConnectService.this.mHandler.removeCallbacks(BleCloudConnectService.this.mConnectingRunnable);
                        if(BleCloudConnectService.this.mOnServiceDisplayStatus != null) {
                            BleCloudConnectService.this.mOnServiceDisplayStatus.OnStatus("result-status-disconnect");
                        }
                    }
            }

        }
    };
    Runnable mConnectingRunnable = new Runnable() {
        public void run() {
            LOG.logI("BleConnectService", "超时判断(不执行)" + BleCloudConnectService.this.mConnectionState);
        }
    };
    private final IBinder mBinder = new BleCloudConnectService.LocalBinder();
    BleCloudConnectService.OnServiceDisplayStatus mOnServiceDisplayStatus = null;
    BleCloudConnectService.OnServiceDisplayDATA mOnServiceDisplayDATA = null;
    BleCloudConnectService.OnAllUserInfoDATA mOnAllUserInfoDATA = null;

    public BleCloudConnectService() {
    }

    void broadcastUpdate(String action) {
        Intent intent = new Intent(action);
        this.sendBroadcast(intent);
    }

    private void broadcastUpdate(String action, BluetoothGattCharacteristic characteristic) {
        byte[] data = characteristic.getValue();
        if(data != null) {
            StringBuffer sb = new StringBuffer(data.length);
            byte[] var8 = data;
            int var7 = data.length;

            for(int var6 = 0; var6 < var7; ++var6) {
                byte byteChar = var8[var6];
                String ms = String.format("%02X ", new Object[]{Byte.valueOf(byteChar)}).trim();
                this.stringBuffer.append(ms);
                sb.append(ms);
            }
        }

    }

    public IBinder onBind(Intent intent) {
        this.registerReceiver(this.mGattUpdateReceiver, makeGattUpdateIntentFilter());
        this.mSendDataHandler = new Handler();
        this.mHandler = new Handler();
        this.mSendData();
        this.mSendBroastData();
        return this.mBinder;
    }

    private boolean displayGattServices(List<BluetoothGattService> gattServices) {
        if(gattServices == null) {
            return false;
        } else {
            Iterator var3 = gattServices.iterator();

            while(true) {
                BluetoothGattService gattService;
                do {
                    if(!var3.hasNext()) {
                        if(this.mWriteCharacteristic != null) {
                            return true;
                        }

                        return false;
                    }

                    gattService = (BluetoothGattService)var3.next();
                } while(!gattService.getUuid().toString().equals(this.SERVICE_UUID.toString().trim()));

                List gattCharacteristics = gattService.getCharacteristics();
                Iterator var6 = gattCharacteristics.iterator();

                while(var6.hasNext()) {
                    BluetoothGattCharacteristic gattCharacteristic = (BluetoothGattCharacteristic)var6.next();
                    if(gattCharacteristic.getUuid().toString().trim().equals(this.CHARACTERISTIC_NOTIFY_UUID.toString().trim())) {
                        this.setCharacteristicNotification(gattCharacteristic, true);
                    }

                    if(gattCharacteristic.getUuid().toString().trim().equals(this.CHARACTERISTIC_WRITE_UUID.toString().trim())) {
                        this.mWriteCharacteristic = gattCharacteristic;
                    }
                }
            }
        }
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("cn.senssun.ble.sdk.ACTION_GATT_CONNECTED");
        intentFilter.addAction("cn.senssun.ble.sdk.ACTION_GATT_DISCONNECTED");
        intentFilter.addAction("cn.senssun.ble.sdk.ACTION_DATA_NOTIFY");
        return intentFilter;
    }

    private void mSendBroastData() {
        (new Thread() {
            public void run() {
                while(!BleCloudConnectService.this.broastStop) {
                    if(BleCloudConnectService.this.stringBuffer.length() >= 10) {
                        String e = BleCloudConnectService.this.stringBuffer.substring(0, 8);
                        if(e.equalsIgnoreCase("100000c5")) {
                            String lengthStr = BleCloudConnectService.this.stringBuffer.substring(8, 10);
                            int length = Integer.valueOf(lengthStr, 16).intValue() * 2;
                            if(BleCloudConnectService.this.stringBuffer.length() >= length) {
                                int verify = 0;

                                String strTmp;
                                for(int data = 8; data < length - 2; data += 2) {
                                    strTmp = BleCloudConnectService.this.stringBuffer.substring(data, data + 2);
                                    verify += Integer.parseInt(strTmp, 16);
                                }

                                if((byte)verify == (byte)Integer.parseInt(BleCloudConnectService.this.stringBuffer.substring(length - 2, length), 16)) {
                                    String data1 = BleCloudConnectService.this.stringBuffer.substring(0, length);
                                    BleCloudConnectService.this.stringBuffer.delete(0, length);
                                    strTmp = "";

                                    for(int intent = 0; intent < data1.length(); intent += 2) {
                                        strTmp = strTmp + data1.substring(intent, intent + 2) + "-";
                                    }

                                    Intent intent1 = new Intent("cn.senssun.ble.sdk.ACTION_DATA_NOTIFY");
                                    intent1.putExtra("cn.senssun.ble.sdk.EXTRA_DATA", strTmp);
                                    BleCloudConnectService.this.sendBroadcast(intent1);
                                } else {
                                    BleCloudConnectService.this.stringBuffer.delete(0, length);
                                }
                            }
                        } else {
                            while(BleCloudConnectService.this.stringBuffer.length() >= 8 && !BleCloudConnectService.this.stringBuffer.substring(0, 8).toLowerCase().equals("100000c5")) {
                                BleCloudConnectService.this.stringBuffer.delete(0, 1);
                            }
                        }
                    } else {
                        for(int e1 = 0; e1 < BleCloudConnectService.this.stringBuffer.length() && BleCloudConnectService.this.stringBuffer.length() > 2 && BleCloudConnectService.this.stringBuffer.substring(0, 2).equals("00"); e1 += 2) {
                            BleCloudConnectService.this.stringBuffer.delete(0, 2);
                        }
                    }

                    try {
                        Thread.sleep(40L);
                    } catch (InterruptedException var8) {
                        var8.printStackTrace();
                    }
                }

            }
        }).start();
    }

    private void mSendData() {
        (new Thread() {
            public void run() {
                while(!BleCloudConnectService.this.broastStop) {
                    LOG.logI("BleConnectService", "mSendDataList.size()：" + BleCloudConnectService.this.mSendDataList.size() + " mWriteCharacteristic:" + BleCloudConnectService.this.mWriteCharacteristic + " mConnectionState:" + BleCloudConnectService.this.mConnectionState);
                    if(BleCloudConnectService.this.mSendDataList.size() != 0 && BleCloudConnectService.this.mWriteCharacteristic != null && BleCloudConnectService.this.mConnectionState == 2) {
                        String e = (String)BleCloudConnectService.this.mSendDataList.get(0);
                        String[] outBuf = e.split("-");
                        byte[] out = new byte[outBuf.length];

                        for(int i = 0; i < outBuf.length; ++i) {
                            out[i] = (byte)Integer.parseInt(outBuf[i], 16);
                        }

                        BleCloudConnectService.this.mWriteCharacteristic.setValue(out);
                        BleCloudConnectService.this.writeCharacteristic(BleCloudConnectService.this.mWriteCharacteristic);
                    }

                    try {
                        Thread.sleep(500L);
                    } catch (InterruptedException var5) {
                        var5.printStackTrace();
                    }
                }

            }
        }).start();
    }

    public void DataCommunAllBuffer(String pin) {
        byte[] outBuffer = BluetoothBuffer.SysUserHealthAllBuffer;
        String verify = pin.substring(0, 2);
        String stringBuilder = pin.substring(pin.length() - 2, pin.length());
        outBuffer[7] = (byte)Integer.parseInt(verify, 16);
        outBuffer[8] = (byte)Integer.parseInt(stringBuilder, 16);
        byte var10 = 0;

        for(int var11 = 4; var11 <= 9; ++var11) {
            var10 += outBuffer[var11];
        }

        outBuffer[10] = var10;
        StringBuilder var12 = new StringBuilder(outBuffer.length);
        byte[] var8 = outBuffer;
        int var7 = outBuffer.length;

        for(int var6 = 0; var6 < var7; ++var6) {
            byte byteChar = var8[var6];
            String ms = String.format("%02X ", new Object[]{Byte.valueOf(byteChar)}).trim();
            var12.append(ms + "-");
        }

        LOG.logI("BleConnectService", "增加发送命令：" + var12.toString());
        this.mSendDataList.add(var12.toString());
    }

    public void DataCommunBuffer(String pin) {
        byte[] outBuffer = BluetoothBuffer.SysUserHealthBuffer;
        String verify = pin.substring(0, 2);
        String stringBuilder = pin.substring(pin.length() - 2, pin.length());
        outBuffer[7] = (byte)Integer.parseInt(verify, 16);
        outBuffer[8] = (byte)Integer.parseInt(stringBuilder, 16);
        byte var10 = 0;

        for(int var11 = 4; var11 <= 9; ++var11) {
            var10 += outBuffer[var11];
        }

        outBuffer[10] = var10;
        StringBuilder var12 = new StringBuilder(outBuffer.length);
        byte[] var8 = outBuffer;
        int var7 = outBuffer.length;

        for(int var6 = 0; var6 < var7; ++var6) {
            byte byteChar = var8[var6];
            String ms = String.format("%02X ", new Object[]{Byte.valueOf(byteChar)}).trim();
            var12.append(ms + "-");
        }

        LOG.logI("BleConnectService", "增加发送命令：" + var12.toString());
        this.mSendDataList.add(var12.toString());
    }

    public void QueryUserInfoBuffer() {
        this.userInfos.clear();
        byte[] outBuffer = BluetoothBuffer.QueryUserInfoBuffer;
        StringBuilder stringBuilder = new StringBuilder(outBuffer.length);
        byte[] var6 = outBuffer;
        int var5 = outBuffer.length;

        for(int var4 = 0; var4 < var5; ++var4) {
            byte byteChar = var6[var4];
            String ms = String.format("%02X ", new Object[]{Byte.valueOf(byteChar)}).trim();
            stringBuilder.append(ms + "-");
        }

        LOG.logI("BleConnectService", "增加发送命令：" + stringBuilder.toString());
        this.mSendDataList.add(stringBuilder.toString());
    }

    public void ResetBuffer() {
        byte[] outBuffer = BluetoothBuffer.ResetBuffer;
        this.mWriteCharacteristic.setValue(outBuffer);
        this.writeCharacteristic(this.mWriteCharacteristic);
    }

    public void AddUserInfo(String pin, int sex, int height, int age, int activity, int unit, int weightKG) {
        byte[] outBuffer = BluetoothBuffer.AddUserInfoBuffer;
        String byte12 = pin.substring(0, 2);
        String byte13 = pin.substring(pin.length() - 2, pin.length());
        outBuffer[9] = (byte)Integer.parseInt(byte12, 16);
        outBuffer[10] = (byte)Integer.parseInt(byte13, 16);
        outBuffer[11] = (byte)(sex == 0?0:1);
        byte12 = Long.toHexString((long)height);
        outBuffer[12] = (byte)Integer.parseInt(byte12, 16);
        byte13 = Long.toHexString((long)age);
        outBuffer[13] = (byte)Integer.parseInt(byte13, 16);
        String byte14 = Long.toHexString((long)activity);
        outBuffer[14] = (byte)Integer.parseInt(byte14, 16);
        String byte15 = Long.toHexString((long)unit);
        outBuffer[15] = (byte)Integer.parseInt(byte15, 16);
        String verify = Long.toHexString((long)weightKG);

        int stringBuilder;
        for(stringBuilder = 4; stringBuilder >= verify.length(); --stringBuilder) {
            verify = "0" + verify;
        }

        String var21 = verify.substring(0, 2);
        String byteChar = verify.substring(verify.length() - 2, verify.length());
        outBuffer[16] = (byte)Integer.parseInt(var21, 16);
        outBuffer[17] = (byte)Integer.parseInt(byteChar, 16);
        byte var20 = 0;

        for(stringBuilder = 4; stringBuilder <= 17; ++stringBuilder) {
            var20 += outBuffer[stringBuilder];
        }

        outBuffer[18] = var20;
        StringBuilder var23 = new StringBuilder(outBuffer.length);
        byte[] var18 = outBuffer;
        int var17 = outBuffer.length;

        for(int var16 = 0; var16 < var17; ++var16) {
            byte var22 = var18[var16];
            String ms = String.format("%02X ", new Object[]{Byte.valueOf(var22)}).trim();
            var23.append(ms + "-");
        }

        LOG.logI("BleConnectService", "增加发送命令：" + var23.toString());
        this.mSendDataList.add(var23.toString());
    }

    public void DelUserInfo(String pin) {
        byte[] outBuffer = BluetoothBuffer.DelUserInfoBuffer;
        String verify = pin.substring(0, 2);
        String stringBuilder = pin.substring(pin.length() - 2, pin.length());
        outBuffer[9] = (byte)Integer.parseInt(verify, 16);
        outBuffer[10] = (byte)Integer.parseInt(stringBuilder, 16);
        byte var10 = 0;

        for(int var11 = 4; var11 <= 17; ++var11) {
            var10 += outBuffer[var11];
        }

        outBuffer[18] = var10;
        StringBuilder var12 = new StringBuilder(outBuffer.length);
        byte[] var8 = outBuffer;
        int var7 = outBuffer.length;

        for(int var6 = 0; var6 < var7; ++var6) {
            byte byteChar = var8[var6];
            String ms = String.format("%02X ", new Object[]{Byte.valueOf(byteChar)}).trim();
            var12.append(ms + "-");
        }

        LOG.logI("BleConnectService", "增加发送命令：" + var12.toString());
        this.mSendDataList.add(var12.toString());
    }

    public boolean onUnbind(Intent intent) {
        this.disconnect();
        this.close();
        this.broastStop = true;
        this.unregisterReceiver(this.mGattUpdateReceiver);
        return super.onUnbind(intent);
    }

    public boolean initialize() {
        if(this.mBluetoothManager == null) {
            this.mBluetoothManager = (BluetoothManager)this.getSystemService("bluetooth");
            if(this.mBluetoothManager == null) {
                LOG.logE("BleConnectService", "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        this.mBluetoothAdapter = this.mBluetoothManager.getAdapter();
        if(this.mBluetoothAdapter == null) {
            LOG.logE("BleConnectService", "Unable to obtain a BluetoothAdapter.");
            return false;
        } else {
            return true;
        }
    }

    public synchronized boolean connectDeviceId(String DeviceId) {
        LOG.logI("BleConnectService", "进入连接" + DeviceId);
        if(this.mConnectionState != 0) {
            return false;
        } else if(DeviceId == null) {
            return false;
        } else {
            LOG.logW("BleConnectService", "DeviceId:" + DeviceId);
            String address = DeviceId.substring(DeviceId.length() - 12, DeviceId.length() - 10) + ":" + DeviceId.substring(DeviceId.length() - 10, DeviceId.length() - 8) + ":" + DeviceId.substring(DeviceId.length() - 8, DeviceId.length() - 6) + ":" + DeviceId.substring(DeviceId.length() - 6, DeviceId.length() - 4) + ":" + DeviceId.substring(DeviceId.length() - 4, DeviceId.length() - 2) + ":" + DeviceId.substring(DeviceId.length() - 2, DeviceId.length());
            if(this.mBluetoothAdapter != null && address != null) {
                if(this.mBluetoothDeviceAddress != null && address.equals(this.mBluetoothDeviceAddress) && this.mBluetoothGatt != null) {
                    LOG.logI("BleConnectService", "Trying to use an existing mBluetoothGatt for connection.");
                    if(this.mBluetoothGatt.connect()) {
                        this.mConnectionState = 1;
                        return true;
                    } else {
                        return false;
                    }
                } else {
                    BluetoothDevice device = this.mBluetoothAdapter.getRemoteDevice(address);
                    if(device == null) {
                        LOG.logW("BleConnectService", "Device not found.  Unable to connect.");
                        return false;
                    } else {
                        this.mBluetoothGatt = device.connectGatt(this, false, this.mGattCallback);
                        LOG.logI("BleConnectService", "Trying to create a new connection.");
                        this.mConnectionState = 1;
                        this.mBluetoothDeviceAddress = address;
                        this.mHandler.postDelayed(this.mConnectingRunnable, 10000L);
                        return true;
                    }
                }
            } else {
                LOG.logW("BleConnectService", "BluetoothAdapter not initialized or unspecified address.");
                return false;
            }
        }
    }

    public synchronized boolean connect(String address) {
        if(this.mConnectionState != 0) {
            return false;
        } else if(address == null) {
            return false;
        } else if(this.mBluetoothAdapter != null && address != null) {
            if(this.mBluetoothDeviceAddress != null && address.equals(this.mBluetoothDeviceAddress) && this.mBluetoothGatt != null) {
                LOG.logI("BleConnectService", "Trying to use an existing mBluetoothGatt for connection.");
                if(this.mBluetoothGatt.connect()) {
                    this.mConnectionState = 1;
                    return true;
                } else {
                    return false;
                }
            } else {
                BluetoothDevice device = this.mBluetoothAdapter.getRemoteDevice(address);
                if(device == null) {
                    LOG.logW("BleConnectService", "Device not found.  Unable to connect.");
                    return false;
                } else {
                    this.mBluetoothGatt = device.connectGatt(this, false, this.mGattCallback);
                    LOG.logI("BleConnectService", "Trying to create a new connection.");
                    this.mConnectionState = 1;
                    this.mBluetoothDeviceAddress = address;
                    this.mHandler.postDelayed(this.mConnectingRunnable, 10000L);
                    return true;
                }
            }
        } else {
            LOG.logW("BleConnectService", "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }
    }

    public void disconnect() {
        if(this.mBluetoothAdapter != null && this.mBluetoothGatt != null) {
            this.mBluetoothGatt.disconnect();
        } else {
            LOG.logW("BleConnectService", "BluetoothAdapter not initialized");
        }
    }

    public void close() {
        LOG.logI("BleConnectService", "执行close mBluetoothGatt");
        if(this.mBluetoothGatt != null) {
            this.mBluetoothGatt.close();
            this.mBluetoothGatt = null;
        }
    }

    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if(this.mBluetoothAdapter != null && this.mBluetoothGatt != null) {
            this.mBluetoothGatt.readCharacteristic(characteristic);
        } else {
            LOG.logW("BleConnectService", "BluetoothAdapter not initialized");
        }
    }

    public boolean setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enabled) {
        if(this.mBluetoothAdapter != null && this.mBluetoothGatt != null) {
            this.mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
            if(descriptor == null) {
                return false;
            } else {
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
                return this.mBluetoothGatt.writeDescriptor(descriptor);
            }
        } else {
            LOG.logW("BleConnectService", "BluetoothAdapter not initialized");
            return false;
        }
    }

    public boolean writeCharacteristic(BluetoothGattCharacteristic characteristic) {
        return this.mBluetoothGatt == null?false:this.mBluetoothGatt.writeCharacteristic(characteristic);
    }

    public boolean ismConnect() {
        return this.mConnectionState == 2;
    }

    public List<BluetoothGattService> getSupportedGattServices() {
        return this.mBluetoothGatt == null?null:this.mBluetoothGatt.getServices();
    }

    public void setOnServiceDisplayStatus(BleCloudConnectService.OnServiceDisplayStatus e) {
        this.mOnServiceDisplayStatus = e;
    }

    public void setOnServiceDisplayDATA(BleCloudConnectService.OnServiceDisplayDATA e) {
        this.mOnServiceDisplayDATA = e;
    }

    public void setOnAllUserInfoDATA(BleCloudConnectService.OnAllUserInfoDATA e) {
        this.mOnAllUserInfoDATA = e;
    }

    public class LocalBinder extends Binder {
        public LocalBinder() {
        }

        BleCloudConnectService getService() {
            return BleCloudConnectService.this;
        }
    }

    public interface OnAllUserInfoDATA {
        void OnUsers(ArrayList<SysUserInfo> var1, boolean var2);
    }

    public interface OnServiceDisplayDATA {
        void OnDATA(String var1);
    }

    public interface OnServiceDisplayStatus {
        void OnStatus(String var1);
    }
}
