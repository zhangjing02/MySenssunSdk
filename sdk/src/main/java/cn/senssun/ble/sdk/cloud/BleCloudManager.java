//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package cn.senssun.ble.sdk.cloud;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import java.util.ArrayList;
import java.util.Iterator;

import cn.senssun.ble.sdk.cloud.BleCloudConnectService.LocalBinder;
import cn.senssun.ble.sdk.cloud.BleCloudConnectService.OnAllUserInfoDATA;
import cn.senssun.ble.sdk.cloud.BleCloudConnectService.OnServiceDisplayDATA;
import cn.senssun.ble.sdk.cloud.BleCloudConnectService.OnServiceDisplayStatus;
import cn.senssun.ble.sdk.entity.SysUserInfo;

public class BleCloudManager {
    private static BleCloudManager mInstance = null;
    public BleCloudConnectService mBleConnectService;
    public ServiceConnection mServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            BleCloudManager.this.mBleConnectService = ((LocalBinder)service).getService();
            if(BleCloudManager.this.mBleConnectService.initialize()) {
                if(BleCloudSDK.getInstance().mOnInitService != null) {
                    BleCloudSDK.getInstance().mOnInitService.OnInit();
                }

                Intent intent = new Intent("cn.senssun.ble.sdk.ACTION_INIT");
                BleCloudManager.this.mBleConnectService.sendBroadcast(intent);
            }

            BleCloudManager.this.mBleConnectService.setOnServiceDisplayStatus(new OnServiceDisplayStatus() {
                public void OnStatus(String status) {
                    String[] strdata = status.split("-");
                    if(strdata[1].equals("status")) {
                        String var3;
                        CloudOnActionMethod.OnConnectState onUserInfoStatus;
                        Iterator var5;
                        CloudOnActionMethod.OnUserInfoStatus onUserInfoStatus1;
                        switch((var3 = strdata[2]).hashCode()) {
                            case -983578094:
                                if(var3.equals("UserInfoDel") && BleCloudSDK.getInstance().mOnUserInfoStatusList.size() > 0) {
                                    var5 = BleCloudSDK.getInstance().mOnUserInfoStatusList.iterator();

                                    while(var5.hasNext()) {
                                        onUserInfoStatus1 = (CloudOnActionMethod.OnUserInfoStatus)var5.next();
                                        onUserInfoStatus1.OnListener(2);
                                    }
                                }
                                break;
                            case -983568473:
                                if(var3.equals("UserInfoNew") && BleCloudSDK.getInstance().mOnUserInfoStatusList.size() > 0) {
                                    var5 = BleCloudSDK.getInstance().mOnUserInfoStatusList.iterator();

                                    while(var5.hasNext()) {
                                        onUserInfoStatus1 = (CloudOnActionMethod.OnUserInfoStatus)var5.next();
                                        onUserInfoStatus1.OnListener(0);
                                    }
                                }
                                break;
                            case -426120989:
                                if(var3.equals("UserInfoEdit") && BleCloudSDK.getInstance().mOnUserInfoStatusList.size() > 0) {
                                    var5 = BleCloudSDK.getInstance().mOnUserInfoStatusList.iterator();

                                    while(var5.hasNext()) {
                                        onUserInfoStatus1 = (CloudOnActionMethod.OnUserInfoStatus)var5.next();
                                        onUserInfoStatus1.OnListener(1);
                                    }
                                }
                                break;
                            case -426074776:
                                if(var3.equals("UserInfoFull") && BleCloudSDK.getInstance().mOnUserInfoStatusList.size() > 0) {
                                    var5 = BleCloudSDK.getInstance().mOnUserInfoStatusList.iterator();

                                    while(var5.hasNext()) {
                                        onUserInfoStatus1 = (CloudOnActionMethod.OnUserInfoStatus)var5.next();
                                        onUserInfoStatus1.OnListener(3);
                                    }
                                }
                                break;
                            case 530405532:
                                if(var3.equals("disconnect") && BleCloudSDK.getInstance().mOnConnectStateList.size() > 0) {
                                    var5 = BleCloudSDK.getInstance().mOnConnectStateList.iterator();

                                    while(var5.hasNext()) {
                                        onUserInfoStatus = (CloudOnActionMethod.OnConnectState)var5.next();
                                        onUserInfoStatus.OnState(false);
                                    }
                                }
                                break;
                            case 951351530:
                                if(var3.equals("connect") && BleCloudSDK.getInstance().mOnConnectStateList.size() > 0) {
                                    var5 = BleCloudSDK.getInstance().mOnConnectStateList.iterator();

                                    while(var5.hasNext()) {
                                        onUserInfoStatus = (CloudOnActionMethod.OnConnectState)var5.next();
                                        onUserInfoStatus.OnState(true);
                                    }
                                }
                        }
                    }

                }
            });
            BleCloudManager.this.mBleConnectService.setOnServiceDisplayDATA(new OnServiceDisplayDATA() {
                public void OnDATA(String data) {
                    if(BleCloudSDK.getInstance().mOnDisplayDATA != null) {
                        BleCloudSDK.getInstance().mOnDisplayDATA.OnDATA(data);
                    }

                    Intent intent = new Intent("cn.senssun.ble.sdk.ACTION_DATA");
                    intent.putExtra("cn.senssun.ble.sdk.EXTRA_DATA", data);
                    BleCloudManager.this.mBleConnectService.sendBroadcast(intent);
                }
            });
            BleCloudManager.this.mBleConnectService.setOnAllUserInfoDATA(new OnAllUserInfoDATA() {
                public void OnUsers(ArrayList<SysUserInfo> users, boolean isfull) {
                    if(BleCloudSDK.getInstance().mOnAllUsers != null) {
                        BleCloudSDK.getInstance().mOnAllUsers.OnShow(users, isfull);
                    }

                    Intent intent = new Intent("cn.senssun.ble.sdk.ACTION_USERS_DATA");
                    intent.putParcelableArrayListExtra("cn.senssun.ble.sdk.EXTRA_DATA", users);
                    intent.putExtra("cn.senssun.ble.sdk.EXTRA_STATUS", isfull);
                    BleCloudManager.this.mBleConnectService.sendBroadcast(intent);
                }
            });
        }

        public void onServiceDisconnected(ComponentName componentName) {
        }
    };

    public BleCloudManager() {
    }

    public static synchronized BleCloudManager getInstance() {
        if(mInstance == null) {
            mInstance = new BleCloudManager();
        }

        return mInstance;
    }

    public void InitSDK(Context mContext) {
        Intent gattServiceIntent = new Intent(mContext, BleCloudConnectService.class);
        mContext.bindService(gattServiceIntent, this.mServiceConnection, 1);
    }

    public void stopSDK(Context mContext) {
        mContext.unbindService(this.mServiceConnection);
    }
}
