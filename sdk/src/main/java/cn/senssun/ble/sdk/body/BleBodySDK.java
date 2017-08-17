package cn.senssun.ble.sdk.body;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import cn.senssun.ble.sdk.body.BleBodyConnectService.OnDisplayDATA;
import cn.senssun.ble.sdk.body.BleBodyConnectService.OnMeasureDATA;
import cn.senssun.ble.sdk.entity.BodyMeasure;

public class BleBodySDK {
	public 	 BleBodyConnectService mBleConnectService;
	public   ServiceConnection mServiceConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName componentName, IBinder service) {
			mBleConnectService=((BleBodyConnectService.LocalBinder) service).getService();//BluetoothLeService mBluetoothLeService =
			if(mBleConnectService.initialize()){
				if(mOnInitService!=null){
					mOnInitService.OnInitService();
				}
			}
			
			mBleConnectService.setOnDisplayDATA(new OnDisplayDATA() {
				@Override
				public void OnDATA(String data) {
					String[] strdata=data.split("-");
					if(strdata[1].equals("status")){
						if(strdata[2].equals("disconnect")){
							if(mOnConnectState!=null){
								mOnConnectState.OnConnectState(false);
							}
						}else if(strdata[2].equals("connect")){
							if(mOnConnectState!=null){
								mOnConnectState.OnConnectState(true);
							}
						}
					}
				}
			});
			mBleConnectService.setOnMeasureDATA(new OnMeasureDATA() {

				@Override
				public void OnMeasureDATA(BodyMeasure bodyMeasure) {
					if(mOnMeasure!=null){
						mOnMeasure.OnMeasure(bodyMeasure);
					}
				}
			});
		}
		@Override
		public void onServiceDisconnected(ComponentName componentName) {
		}
	};

	public  void InitSDK(Context mContext) {
		Intent gattServiceIntent = new Intent(mContext, BleBodyConnectService.class);
		mContext.bindService(gattServiceIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
	}

	public  void stopSDK(Context mContext){
		mContext.unbindService(mServiceConnection);
	}

	public  boolean isConnect(){
		try {
			if(mBleConnectService==null)
				return false;
			else 
				return	mBleConnectService.ismConnect();
		} catch (Exception e) {
			return false;
		}
	
	}

	public  boolean Connect(String address){
		return mBleConnectService.connect(address);
	}

	public  void Disconnect(){
		mBleConnectService.disconnect();
	}

	/***************************************自定义搜索连接代码********************************************/

	public boolean ConnectStatus(){
		try {
			return mBleConnectService.ismConnect();
		} catch (Exception e) {
			return false;
		}
	}

	public interface OnConnectState{
		void OnConnectState(boolean State);
	}

	OnConnectState mOnConnectState=null;
	public void setOnConnectState(OnConnectState e){
		mOnConnectState=e;
	}

	public interface OnMeasure{
		void OnMeasure(BodyMeasure bodyMeasure);
	}

	OnMeasure mOnMeasure=null;
	public void setOnMeasure(OnMeasure e){
		mOnMeasure=e;
	}

	public interface OnInitService{
		void OnInitService();
	}

	OnInitService mOnInitService=null;
	public void setOnInitService(OnInitService e){
		mOnInitService=e;
	}

	
}
