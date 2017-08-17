package cn.senssun.ble.sdk.food;

import cn.senssun.ble.sdk.BluetoothBuffer;
import cn.senssun.ble.sdk.entity.FoodMeasure;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;

public class BleFoodSDK {
	public 	 BleFoodConnectService mBleFoodConnectService;
	public   ServiceConnection mServiceConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName componentName, IBinder service) {
			mBleFoodConnectService=((BleFoodConnectService.LocalBinder) service).getService();//BluetoothLeService mBluetoothLeService =
			
			if(mOnInitService!=null){
				mOnInitService.OnInitService();
			}
		}
		@Override
		public void onServiceDisconnected(ComponentName componentName) {
		}
	};

	public  void InitSDK(Context mContext) {
		Intent gattServiceIntent = new Intent(mContext, BleFoodConnectService.class);
		mContext.bindService(gattServiceIntent, mServiceConnection, Context.BIND_AUTO_CREATE);

		mContext.registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());//注册你的广播接收
	}

	public  void stopSDK(Context mContext) {
		mContext.unbindService(mServiceConnection);
	}

	public  boolean isConnect(){
		return	mBleFoodConnectService.ismConnect();
	}

	public  boolean Connect(String address){
		return mBleFoodConnectService.connect(address);
	}
	
	public  void Disconnect(){
		mBleFoodConnectService.disconnect();
	}

	private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {    //广播接收
		@Override
		public void onReceive(Context context, Intent intent) {
			final String action = intent.getAction();

			if (BluetoothBuffer.ACTION_Display_DATA.equals(action)) {
				String data=intent.getStringExtra(BluetoothBuffer.Display_DATA);
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
			}else if (BluetoothBuffer.ACTION_Measure_DATA.equals(action)) {
				FoodMeasure foodMeasure=(FoodMeasure) intent.getSerializableExtra(BluetoothBuffer.Measure_DATA);
				if(mOnMeasure!=null){
					mOnMeasure.OnMeasure(foodMeasure);
				}
			}
		}
	};

	private  IntentFilter makeGattUpdateIntentFilter() {
		final IntentFilter intentFilter = new IntentFilter();

		intentFilter.addAction(BluetoothBuffer.ACTION_Display_DATA);
		intentFilter.addAction(BluetoothBuffer.ACTION_Measure_DATA);

		return intentFilter;
	}
	/***************************************自定义搜索连接代码********************************************/
	public interface OnConnectState{
		void OnConnectState(boolean State);
	}

	OnConnectState mOnConnectState=null;
	public void setOnConnectState(OnConnectState e){
		mOnConnectState=e;
	}

	public interface OnMeasure{
		void OnMeasure(FoodMeasure foodMeasure);
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
