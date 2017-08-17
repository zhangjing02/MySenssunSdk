package cn.senssun.ble.sdk.fat.alarm;

import java.math.BigDecimal;

import cn.senssun.ble.sdk.entity.FatDataCommun;
import cn.senssun.ble.sdk.fat.alarm.BleFatAlarmConnectService.OnCommunDATA;
import cn.senssun.ble.sdk.fat.alarm.BleFatAlarmConnectService.OnDisplayDATA;
import cn.senssun.ble.sdk.fat.alarm.BleFatAlarmConnectService.OnMeasureDATA;
import cn.senssun.ble.sdk.util.LOG;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

public class BleFatAlarmSDK {
	public 	 BleFatAlarmConnectService mBleConnectService;
	public   ServiceConnection mServiceConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName componentName, IBinder service) {
			mBleConnectService=((BleFatAlarmConnectService.LocalBinder) service).getService();//BluetoothLeService mBluetoothLeService =
			LOG.logE("BleFatAlarmSDK","初始化 mBleConnectService 成功");
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
						}else if(strdata[2].equals("dataCommun")){
							if(mOnSendDataCommunListener!=null){
								mOnSendDataCommunListener.OnListener();
							}
						}else if(strdata[2].equals("dataCommunFinish")){
							if(mOnSendDataCommunFinish!=null){
								mOnSendDataCommunFinish.OnListener();
							}
						}else if(strdata[2].equals("bodyTest")){
							LOG.logE("BleFatAlarmSDK", "bodyTest:0");
							if(mOnSendBodyTestListener!=null){
								mOnSendBodyTestListener.OnListener(0);
							}
						}else if(strdata[2].equals("bodyTestError")){
							LOG.logE("BleFatAlarmSDK", "bodyTest:-1");
							if(mOnSendBodyTestListener!=null){
								mOnSendBodyTestListener.OnListener(-1);
							}
						}else if(strdata[2].equals("bodyTestError2")){
							LOG.logE("BleFatAlarmSDK", "bodyTest:-2");
							if(mOnSendBodyTestListener!=null){
								mOnSendBodyTestListener.OnListener(-2);
							}
						}else if(strdata[2].equals("setAlarm")){
							if(mOnSendAlarmListener!=null){
								mOnSendAlarmListener.OnListener();
							}
						}else if(strdata[2].equals("clearData")){
							if(mOnClearDataListener!=null){
								mOnClearDataListener.OnListener();
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
			
			mBleConnectService.setOnCommunDATA(new OnCommunDATA() {
				@Override
				public void OnDATA(FatDataCommun fatDataCommun) {
					if(mOnDataCommun!=null){
						mOnDataCommun.OnData(fatDataCommun);
					}
					
				}
			});
			

		}
		@Override
		public void onServiceDisconnected(ComponentName componentName) {
		}
	};

	public  void InitSDK(Context mContext) {
		Intent gattServiceIntent = new Intent(mContext, BleFatAlarmConnectService.class);
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
		LOG.logE("BleFatAlarmSDK","mBleConnectService:" + mBleConnectService +" address:"+address );
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
	public  void SendTestFatInfo(int height,int age,int sex){
		LOG.logE("BleFatAlarmSDK"," height:" +height+ " age:"+age +" sex:"+sex );
		//往蓝牙模块写入数据
		try {
			mBleConnectService.FatTestBuffer(height,age,sex,0);	
		} catch (Exception e) {
			LOG.logE("BleSDK", "发送脂肪测试命令出错");
		}
	}
	
	/**
	 * @param serialNum 浅同步
	 */
	public  void SendDataCommun(){
		//往蓝牙模块写入数据
		try {
			mBleConnectService.DataCommunBuffer();
		} catch (Exception e) {
			LOG.logE("BleSDK", "发送同步历史命令出错");
		}
	}
	
	/**
	 * @param serialNum 深同步
	 */
	public  void SendAllDataCommun(){
		//往蓝牙模块写入数据
		try {
			mBleConnectService.AllDataCommunBuffer();
		} catch (Exception e) {
			LOG.logE("BleSDK", "发送同步历史命令出错");
		}
	}
	
	/**
	 * @param clearData 清除历史
	 */
	public  void SendClearData(){
		//往蓝牙模块写入数据
		try {
			mBleConnectService.SendClearData();
		} catch (Exception e) {
			LOG.logE("BleSDK", "发送同步历史命令出错");
		}
	}

	/**
	 * @param bmr 基础代谢
	 * @param sportMode 运动模式
	 * @param sex 性别
	 */
	public  int CalculateAMR(int bmr,int sportMode,int sex){
		
		 switch (sportMode){
         case 2:
             return sex == 0 ?
            		 new BigDecimal(bmr * 1.35).setScale(0, BigDecimal.ROUND_HALF_UP).intValue() :
                     new BigDecimal(bmr * 1.4).setScale(0, BigDecimal.ROUND_HALF_UP).intValue();
         case 3:
             return sex == 0 ?
                     new BigDecimal(bmr * 1.45).setScale(0, BigDecimal.ROUND_HALF_UP).intValue() :
                     new BigDecimal(bmr * 1.5).setScale(0, BigDecimal.ROUND_HALF_UP).intValue();
         case 4:
        	 return sex == 0 ?
                     new BigDecimal(bmr * 1.7).setScale(0, BigDecimal.ROUND_HALF_UP).intValue() :
                     new BigDecimal(bmr * 1.85).setScale(0, BigDecimal.ROUND_HALF_UP).intValue();
         case 5:
        	 return sex == 0 ?
                     new BigDecimal(bmr * 2).setScale(0, BigDecimal.ROUND_HALF_UP).intValue() :
                     new BigDecimal(bmr * 2.1).setScale(0, BigDecimal.ROUND_HALF_UP).intValue();
         case 0:
         case 1:
         default:
        	 return new BigDecimal(bmr*1.15).setScale(0,BigDecimal.ROUND_HALF_UP).intValue();
		 }
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
	
	public interface OnDataCommun{
		void OnData(FatDataCommun fatDataCommun);
	}

	OnDataCommun mOnDataCommun=null;
	public void setOnDataCommun(OnDataCommun e){
		mOnDataCommun=e;
	}
	
	public interface OnInitService{
		void OnInitService();
	}

	OnInitService mOnInitService=null;
	public void setOnInitService(OnInitService e){
		mOnInitService=e;
	}

	public interface OnSendBodyTestListener{
		void OnListener(int isSucType);
	}

	OnSendBodyTestListener mOnSendBodyTestListener=null;
	public void setOnSendBodyTestListener(OnSendBodyTestListener e){
		mOnSendBodyTestListener=e;
	}
	public interface OnSendAlarmListener{
		void OnListener();
	}

	OnSendAlarmListener mOnSendAlarmListener=null;
	public void setOnSendAlarmListener(OnSendAlarmListener e){
		mOnSendAlarmListener=e;
	}

	public interface OnSendDataCommunListener{
		void OnListener();
	}

	OnSendDataCommunListener mOnSendDataCommunListener=null;
	public void setOnSendDataCommunListener(OnSendDataCommunListener e){
		mOnSendDataCommunListener=e;
	}
	
	public interface OnDataCommunListenerFinish{
		void OnListener();
	}

	OnDataCommunListenerFinish mOnSendDataCommunFinish=null;
	public void setOnDataCommunListenerFinish(OnDataCommunListenerFinish e){
		mOnSendDataCommunFinish=e;
	}
	
	public interface OnClearDataListener{
		void OnListener();
	}

	OnClearDataListener mOnClearDataListener=null;
	public void setOnClearDataListener(OnClearDataListener e){
		mOnClearDataListener=e;
	}
}
