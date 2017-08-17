package cn.senssun.ble.sdk.grow;

import android.content.Context;

import java.util.ArrayList;

import cn.senssun.ble.sdk.grow.GrowOnActionMethod.OnConnectState;
import cn.senssun.ble.sdk.grow.GrowOnActionMethod.OnDisplayDATA;
import cn.senssun.ble.sdk.grow.GrowOnActionMethod.OnUserInfoStatus;
import cn.senssun.ble.sdk.util.LOG;

public class BleGrowSDK {

	private static BleGrowSDK mInstance = null;


	public  void InitSDK(Context mContext) {
		BleGrowManager.getInstance().InitSDK(mContext);
	}
	public  void stopSDK(Context mContext){
		BleGrowManager.getInstance().stopSDK(mContext);
	}


	public static synchronized BleGrowSDK getInstance()
	{
		if (mInstance == null) {
			mInstance = new BleGrowSDK();
		}
		return mInstance;
	}
	/***************************************方法代码********************************************/


	public  boolean isConnect(){ //判断连接
		if(BleGrowManager.getInstance().mBleConnectService==null)
			return false;
		else 
			return	BleGrowManager.getInstance().mBleConnectService.ismConnect();
	}

	public boolean isInit(){
		if(BleGrowManager.getInstance().mBleConnectService!=null){
			return true;
		}else{
			return false;		
		}
	}
	//	public  boolean Connect(String address){//执行连接
	//		return mBleConnectService.connect(address);
	//	}

	public  boolean Connect(String address){//执行连接DeviceId
		return BleGrowManager.getInstance().mBleConnectService.connect(address);
	}

	public  void Disconnect(){//执行断开
		BleGrowManager.getInstance().mBleConnectService.disconnect();
		//		BleManager.getInstance().mBleConnectService.close();
	}



	public  void SendAddUser(int serialNum){
		try {
			BleGrowManager.getInstance().mBleConnectService.AddUserBuffer(serialNum);
		} catch (Exception e) {
			LOG.logE("BleSDK", "发送增加用户命令出错");
		}
	}
	public  void SendDeleUser(int serialNum){
		try {
			BleGrowManager.getInstance().mBleConnectService.DeleUserBuffer(serialNum);
		} catch (Exception e) {
			LOG.logE("BleSDK", "发送删除用户命令出错");
		}
	}
	public  void SendSeleUser(int serialNum){
		try {
			BleGrowManager.getInstance().mBleConnectService.SeleUserBuffer(serialNum);
		} catch (Exception e) {
			LOG.logE("BleSDK", "发送选择用户命令出错");
		}
	}
	public  void SendDeepSys(String serialNum){
		try {
			BleGrowManager.getInstance().mBleConnectService.DeepSysBuffer(serialNum);
		} catch (Exception e) {
			LOG.logE("BleSDK", "发送深度同步命令出错");
		}
	}
	public  void SendShallowSys(String serialNum){
		try {
			BleGrowManager.getInstance().mBleConnectService.ShallowSysBuffer(serialNum);
		} catch (Exception e) {
			LOG.logE("BleSDK", "发送浅度同步命令出错");
		}
	}



	ArrayList<OnConnectState> mOnConnectStateList=new ArrayList<>();
	public void setOnConnectState(OnConnectState e){
		mOnConnectStateList.add(e);
	}
	public void RemoveOnConnectState(OnConnectState e){
		mOnConnectStateList.remove(e);
	}
	public void RemoveAllOnConnectState(){
		mOnConnectStateList.clear();
	}

	ArrayList<OnDisplayDATA> mOnDisplayDATAList=new ArrayList<>();
	public void setOnDisplayDATA(OnDisplayDATA e){
		mOnDisplayDATAList.add(e);
	}
	
	public void RemoveOnDisplayDATA(OnDisplayDATA e){
		mOnDisplayDATAList.remove(e);
	}
	public void RemoveAllOnDisplayDATA(){
		mOnDisplayDATAList.clear();
	}

	GrowOnActionMethod.OnInitService mOnInitService=null;
	public void setOnInitService(GrowOnActionMethod.OnInitService e){
		mOnInitService=e;
	}

	ArrayList<OnUserInfoStatus> mOnUserInfoStatusList=new ArrayList<>();
	public void setOnUserInfoStatus(OnUserInfoStatus e){
		mOnUserInfoStatusList.add(e);
	}
	public void RemoveOnUserInfoStatus(OnUserInfoStatus e){
		mOnUserInfoStatusList.remove(e);
	}
	public void RemoveAllOnUserInfoStatus(){
		mOnUserInfoStatusList.clear();
	}



	//	/***************************************方法代码********************************************/
	//	public 	 BleGrowConnectService mBleGrowConnectService;
	//	public   ServiceConnection mServiceConnection = new ServiceConnection() {
	//		@Override
	//		public void onServiceConnected(ComponentName componentName, IBinder service) {
	//			mBleGrowConnectService=((BleGrowConnectService.LocalBinder) service).getService();//BluetoothLeService mBluetoothLeService =
	//			
	//			if(mBleGrowConnectService.initialize()){
	//				if(mOnInitService!=null){
	//					mOnInitService.OnCallBack();
	//				}
	//			}
	//			
	//			mBleGrowConnectService.setOnDisplayDATA(new OnDisplayDATA() {
	//				@Override
	//				public void OnDATA(String data) {
	//					String[] strdata=data.split("-");
	//					if(strdata[1].equals("status")){
	//						if(strdata[2].equals("disconnect")){
	//							if(mOnConnectState!=null){
	//								mOnConnectState.OnState(false);
	//							}
	//						}else if(strdata[2].equals("connect")){
	//							if(mOnConnectState!=null){
	//								mOnConnectState.OnState(true);
	//							}
	//						}else if (strdata[2].equals("50")){
	//							if(mOnAddUser!=null){
	//								mOnAddUser.OnAddUserCallBack();
	//							}
	//						}else	if (strdata[2].equals("51")){
	//							if(mOnDeleUserSuccess!=null){
	//								mOnDeleUserSuccess.OnCallBack();
	//							}
	//						}else	if (strdata[2].equals("52")){
	//							if(mOnSeleUserSuccess!=null){
	//								mOnSeleUserSuccess.OnCallBack();
	//							}
	//						}else	if (strdata[2].equals("6A")){
	//							if(mOnSysData!=null){
	//								mOnSysData.OnCallBack();
	//							}
	//						}else	if (strdata[2].equals("61")){
	//							if(mOnSeleUserSuccess!=null){
	//								mOnSeleUserSuccess.OnCallBack();
	//							}
	//						}
	//					}
	//				}
	//			});
	//			
	//			mBleGrowConnectService.setOnMeasureDATA(new OnMeasureDATA() {
	//				@Override
	//				public void OnDATA(GrowMeasure growMeasure) {
	//					if(mOnMeasure!=null){
	//						mOnMeasure.OnData(growMeasure);
	//					}
	//				}
	//			});
	//			
	//		}
	//		@Override
	//		public void onServiceDisconnected(ComponentName componentName) {
	//		}
	//	};
	//
	//	public  void InitSDK(Context mContext) {
	//		Intent gattServiceIntent = new Intent(mContext, BleGrowConnectService.class);
	//		mContext.bindService(gattServiceIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
	//	}
	//
	//	public  void stopSDK(Context mContext){
	//		mContext.unbindService(mServiceConnection);
	//	}
	//
	//	public  boolean isConnect(){
	//		return	mBleGrowConnectService.ismConnect();
	//	}
	//
	//	public  boolean Connect(String address){
	//		return mBleGrowConnectService.connect(address);
	//	}
	//	
	//	public  void Disconnect(){
	//		mBleGrowConnectService.disconnect();
	//	}
	//
	//	private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {    //广播接收
	//		@Override
	//		public void onReceive(Context context, Intent intent) {
	//			final String action = intent.getAction();
	//
	//			if (BluetoothBuffer.ACTION_Display_DATA.equals(action)) {
	//				String data=intent.getStringExtra(BluetoothBuffer.Display_DATA);
	//				String[] strdata=data.split("-");
	//				
	//			}else if (BluetoothBuffer.ACTION_Measure_DATA.equals(action)) {
	//				GrowMeasure growMeasure=(GrowMeasure) intent.getSerializableExtra(BluetoothBuffer.Measure_DATA);
	//				if(mOnMeasure!=null){
	//					mOnMeasure.OnData(growMeasure);
	//				}
	//			}else if(BluetoothBuffer.ACTION_DATA_NOTIFY.equals(action)){
	//				String data=intent.getStringExtra(BluetoothBuffer.EXTRA_DATA);
	//				if(data==null){return;}
	//				String[] strdata=data.split("-");
	//				
	//			}
	//		}
	//	};
	//
	//	/***************************************自定义搜索连接代码********************************************/

	//	
	//	public interface OnConnectState{
	//		void OnState(boolean State);
	//	}
	//
	//	OnConnectState mOnConnectState=null;
	//	public void setOnConnectState(OnConnectState e){
	//		mOnConnectState=e;
	//	}
	//
	//	public interface OnMeasure{
	//		void OnData(GrowMeasure growMeasure);
	//	}
	//
	//	OnMeasure mOnMeasure=null;
	//	public void setOnMeasure(OnMeasure e){
	//		mOnMeasure=e;
	//	}
	//	
	//	public interface OnInitService{
	//		void OnCallBack();
	//	}
	//	
	//	OnInitService mOnInitService=null;
	//	public void setOnInitService(OnInitService e){
	//		mOnInitService=e;
	//	}
	//
	//	public interface OnAddUser{
	//		void OnAddUserCallBack();
	//	}
	//
	//	OnAddUser mOnAddUser=null;
	//	public void setOnAddUser(OnAddUser e){
	//		mOnAddUser=e;
	//	}
	//	
	//	public interface OnDeleUserSuccess{
	//		void OnCallBack();
	//	}
	//
	//	OnDeleUserSuccess mOnDeleUserSuccess=null;
	//	public void setOnDeleUserSuccess(OnDeleUserSuccess e){
	//		mOnDeleUserSuccess=e;
	//	}
	//	
	//	public interface OnSeleUserSuccess{
	//		void OnCallBack();
	//	}
	//
	//	OnSeleUserSuccess mOnSeleUserSuccess=null;
	//	public void setOnSeleUserSuccess(OnSeleUserSuccess e){
	//		mOnSeleUserSuccess=e;
	//	}
	//	
	//	public interface OnSysData{
	//		void OnCallBack();
	//	}
	//
	//	OnSysData mOnSysData=null;
	//	public void setOnSysData(OnSysData e){
	//		mOnSysData=e;
	//	}
}
