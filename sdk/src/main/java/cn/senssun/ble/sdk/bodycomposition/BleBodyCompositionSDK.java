package cn.senssun.ble.sdk.bodycomposition;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import cn.senssun.ble.sdk.bodycomposition.BleBodyCompositionConnectService.OnDisplayDATA;
import cn.senssun.ble.sdk.bodycomposition.BleBodyCompositionConnectService.OnMeasureDATA;
import cn.senssun.ble.sdk.util.LOG;

public class BleBodyCompositionSDK {
	public 	 BleBodyCompositionConnectService mBleConnectService;
	public   ServiceConnection mServiceConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName componentName, IBinder service) {
			mBleConnectService=((BleBodyCompositionConnectService.LocalBinder) service).getService();//BluetoothLeService mBluetoothLeService =

			mBleConnectService.setOnDisplayDATA(new OnDisplayDATA() {
				@Override
				public void OnDisplayDATA(String data) {
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
						}else if(strdata[2].equals("bodyTest")){
							if(mOnSendBodyTestListener!=null){
								mOnSendBodyTestListener.OnListener();
							}
						}
					}
				}
			});
			mBleConnectService.setOnMeasureDATA(new OnMeasureDATA() {

				@Override
				public void OnDATA(String data) {
					if(mOnMeasure!=null){
						mOnMeasure.OnMeasure(data);
					}
				}
			});
			
			if(mOnInitService!=null){
				mOnInitService.OnInitService();
			}


		}
		@Override
		public void onServiceDisconnected(ComponentName componentName) {
		}
	};

	public  void InitSDK(Context mContext) {
		Intent gattServiceIntent = new Intent(mContext, BleBodyCompositionConnectService.class);
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


	public boolean ConnectStatus(){
		try {
			return mBleConnectService.ismConnect();
		} catch (Exception e) {
			return false;
		}
	}
	/**
	 * @param height 身高
	 * @param age 年龄
	 * @param sex 用户性别 （1：男 0：女）
	 * @param serialNum 用户序号 1-12
	 */
	public  void SendTestBodyInfo(int height,int age,int sex,int serialNum){
//		//往蓝牙模块写入数据
		try {
			mBleConnectService.mBodyTest=false;
			mBleConnectService.BodyCompositionTestBuffer(height,age,sex);	
		} catch (Exception e) {
			LOG.logE("BleSDK", "发送脂肪测试命令出错");
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
		void OnMeasure(String data);
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


	public interface OnSendBodyTestListener{
		void OnListener();
	}

	OnSendBodyTestListener mOnSendBodyTestListener=null;
	public void setOnSendBodyTestListener(OnSendBodyTestListener e){
		mOnSendBodyTestListener=e;
	}

}
