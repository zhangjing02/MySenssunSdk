package cn.senssun.ble.sdk.body.alarm;

import cn.senssun.ble.sdk.body.alarm.BleBodyAlarmConnectService.OnDisplayDATA;
import cn.senssun.ble.sdk.body.alarm.BleBodyAlarmConnectService.OnMeasureDATA;
import cn.senssun.ble.sdk.entity.BodyMeasure;
import cn.senssun.ble.sdk.util.LOG;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

public class BleBodyAlarmSDK {
	public 	 BleBodyAlarmConnectService mBleConnectService;
	public   ServiceConnection mServiceConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName componentName, IBinder service) {
			mBleConnectService=((BleBodyAlarmConnectService.LocalBinder) service).getService();//BluetoothLeService mBluetoothLeService =
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
						}else if(strdata[2].equals("setAlarm")){
							if(mOnSendAlarmListener!=null){
								mOnSendAlarmListener.OnListener();
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
			if(mOnInitService!=null){
				mOnInitService.OnInitService();
			}
		}
		@Override
		public void onServiceDisconnected(ComponentName componentName) {
		}
	};

	public  void InitSDK(Context mContext) {
		Intent gattServiceIntent = new Intent(mContext, BleBodyAlarmConnectService.class);
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
	
	/**
	 * @param sun 星期天
	 * @param mon 星期一
	 * @param tue 星期二
	 * @param wed 星期三
	 * @param thu 星期四
	 * @param fri 星期五
	 * @param sat 星期六
	 * @param hour 小时
	 * @param minutes 分钟
	 * @param music 音乐 1-15
	 * @param music 闹钟id 0-2
	 */
	public  void SendAlarm(boolean sun,boolean mon,boolean tue,boolean wed,boolean thu,boolean fri,boolean sat,int hour,int minutes,int alarmId,int music){
		//往蓝牙模块写入数据
		try {
			mBleConnectService.SendAlarmBuffer(sun,mon,tue,wed,thu,fri,sat,hour,minutes,alarmId,music);
		} catch (Exception e) {
			LOG.logE("BleSDK", "发送闹钟命令出错");
		}
	}

	
	public interface OnSendAlarmListener{
		void OnListener();
	}

	OnSendAlarmListener mOnSendAlarmListener=null;
	public void setOnSendAlarmListener(OnSendAlarmListener e){
		mOnSendAlarmListener=e;
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
