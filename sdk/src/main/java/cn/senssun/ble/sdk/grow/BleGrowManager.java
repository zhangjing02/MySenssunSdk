package cn.senssun.ble.sdk.grow;

import cn.senssun.ble.sdk.BluetoothBuffer;
import cn.senssun.ble.sdk.entity.GrowMeasure;
import cn.senssun.ble.sdk.grow.BleGrowConnectService.OnDisplayDATA;
import cn.senssun.ble.sdk.grow.BleGrowConnectService.OnMeasureDATA;
import cn.senssun.ble.sdk.grow.GrowOnActionMethod.OnConnectState;
import cn.senssun.ble.sdk.grow.GrowOnActionMethod.OnUserInfoStatus;


import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

public class BleGrowManager {
	private static BleGrowManager mInstance = null;
	public 	 BleGrowConnectService mBleConnectService;

	public static synchronized BleGrowManager getInstance()
	{
		if (mInstance == null) {
			mInstance = new BleGrowManager();
		}
		return mInstance;
	}
	public  void InitSDK(Context mContext) {
		Intent gattServiceIntent = new Intent(mContext, BleGrowConnectService.class);
		mContext.bindService(gattServiceIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
	}

	public  void stopSDK(Context mContext){
		mContext.unbindService(mServiceConnection);
	}

	public   ServiceConnection mServiceConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName componentName, IBinder service) {
			mBleConnectService=((BleGrowConnectService.LocalBinder) service).getService();//BluetoothLeService mBluetoothLeService =
			if(mBleConnectService.initialize()){
				if(BleGrowSDK.getInstance().mOnInitService!=null){
					BleGrowSDK.getInstance().mOnInitService.OnInit();
				}
				Intent intent = new Intent(BluetoothBuffer.ACTION_INIT);
				mBleConnectService.sendBroadcast(intent);
			}


			mBleConnectService.setOnDisplayDATA(new OnDisplayDATA() {
				@Override
				public void OnDATA(String data) {
					String[] strdata=data.split("-");
					if(strdata[1].equals("status")){
						if(strdata[2].equals("disconnect")){
							if(BleGrowSDK.getInstance().mOnConnectStateList.size()>0){
								for(OnConnectState onConnectState:BleGrowSDK.getInstance().mOnConnectStateList){
									onConnectState.OnState(false);
								}
							}
						}else if(strdata[2].equals("connect")){
							if(BleGrowSDK.getInstance().mOnConnectStateList.size()>0){
								for(OnConnectState onConnectState:BleGrowSDK.getInstance().mOnConnectStateList){
									onConnectState.OnState(true);
								}
							}
						}else if (strdata[2].equals("50")){
							if(BleGrowSDK.getInstance().mOnUserInfoStatusList.size()>0){
								for(OnUserInfoStatus onUserInfoStatus:BleGrowSDK.getInstance().mOnUserInfoStatusList){
									onUserInfoStatus.OnListener(0);
								}
							}

							//											if(mOnAddUser!=null){
							//												mOnAddUser.OnAddUserCallBack();
							//											}
						}else	if (strdata[2].equals("51")){
							if(BleGrowSDK.getInstance().mOnUserInfoStatusList.size()>0){
								for(OnUserInfoStatus onUserInfoStatus:BleGrowSDK.getInstance().mOnUserInfoStatusList){
									onUserInfoStatus.OnListener(1);
								}
							}
							//											if(mOnDeleUserSuccess!=null){
							//												mOnDeleUserSuccess.OnCallBack();
							//											}
						}else	if (strdata[2].equals("52")){
							if(BleGrowSDK.getInstance().mOnUserInfoStatusList.size()>0){
								for(OnUserInfoStatus onUserInfoStatus:BleGrowSDK.getInstance().mOnUserInfoStatusList){
									onUserInfoStatus.OnListener(2);
								}
							}
							//											if(mOnSeleUserSuccess!=null){
							//												mOnSeleUserSuccess.OnCallBack();
							//											}
						}else	if (strdata[2].equals("6A")){
							if(BleGrowSDK.getInstance().mOnUserInfoStatusList.size()>0){
								for(OnUserInfoStatus onUserInfoStatus:BleGrowSDK.getInstance().mOnUserInfoStatusList){
									onUserInfoStatus.OnListener(3);
								}
							}
							//											if(mOnSysData!=null){
							//												mOnSysData.OnCallBack();
							//											}
						}else	if (strdata[2].equals("61")){
							if(BleGrowSDK.getInstance().mOnUserInfoStatusList.size()>0){
								for(OnUserInfoStatus onUserInfoStatus:BleGrowSDK.getInstance().mOnUserInfoStatusList){
									onUserInfoStatus.OnListener(4);
								}
							}
							//											if(mOnSeleUserSuccess!=null){
							//												mOnSeleUserSuccess.OnCallBack();
							//											}
						}
					}
				}
			});

			mBleConnectService.setOnMeasureDATA(new OnMeasureDATA() {
				@Override
				public void OnDATA(GrowMeasure growMeasure) {
					if(BleGrowSDK.getInstance().mOnDisplayDATAList.size()>0){
						for(GrowOnActionMethod.OnDisplayDATA onDisplayDATA:BleGrowSDK.getInstance().mOnDisplayDATAList){
							onDisplayDATA.OnDATA(growMeasure);
						}
					}
					Intent intent = new Intent(BluetoothBuffer.ACTION_DATA);
					intent.putExtra(BluetoothBuffer.EXTRA_DATA, growMeasure);
					mBleConnectService.sendBroadcast(intent);
				}
			});

		}
		@Override
		public void onServiceDisconnected(ComponentName componentName) {
		}
	};
}