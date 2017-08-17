package cn.senssun.ble.sdk.fat;


import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import cn.senssun.ble.sdk.BluetoothBuffer;
import cn.senssun.ble.sdk.entity.FatMeasure;
import cn.senssun.ble.sdk.entity.FatMeasure.DataTypeEnum;
import cn.senssun.ble.sdk.util.LOG;

/**
 * Service for managing connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 */
public class BleConnectService extends Service {
	public final static String ACTION_GATT_DISCONNECTED = "cn.senssun.ble.sdk.BleEQIConnectService.ACTION_GATT_DISCONNECTED";
	public final static String ACTION_GATT_CONNECTED ="cn.senssun.ble.sdk.BleEQIConnectService.ACTION_GATT_CONNECTED";
	public final static String ACTION_GATT_SERVICES_DISCOVERED = "cn.senssun.ble.sdk.BleEQIConnectService.ACTION_GATT_SERVICES_DISCOVERED";
	public final static String ACTION_DATA_NOTIFY = "cn.senssun.ble.sdk.BleEQIConnectService.ACTION_DATA_NOTIFY";
	public final static String ACTION_DATA_READ = "cn.senssun.ble.sdk.BleEQIConnectService.ACTION_DATA_READ";
	
	private final static String TAG = "BleConnectService";

	private BluetoothManager mBluetoothManager;
	private BluetoothAdapter mBluetoothAdapter;
	private String mBluetoothDeviceAddress;
	private BluetoothGatt mBluetoothGatt;
	private int mConnectionState = STATE_DISCONNECTED;

	private static final int STATE_DISCONNECTED = 0;
	private static final int STATE_CONNECTING = 1;
	private static final int STATE_CONNECTED = 2;


	private Handler mSendDataHandler;
	private Handler mSendDataHandler2;

	private List<byte[]> mSendDataList=new ArrayList<byte[]>();
	private List<byte[]> mSendDataList2=new ArrayList<byte[]>();

	private boolean connectIng=false;
	public boolean misSend=false;

	private int indexNumE1,indexNumE2;
	private FatMeasure fatMeasure=new FatMeasure();//发出对象
	private HashMap<Integer, FatMeasure> SendHisOBjectList=new HashMap<Integer, FatMeasure>();
	private BluetoothGattCharacteristic mWriteCharacteristic; //写出GATT,char

	private Lock lock = new ReentrantLock();  
	/*人体秤*/
	// Implements callback methods for GATT events that the app cares about.  For example,
	// connection change and services discovered.
	private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
		@Override
		public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
			String intentAction;
			if (newState == BluetoothProfile.STATE_CONNECTED) {
				if(status==133){
					connectIng=false;
					intentAction = ACTION_GATT_DISCONNECTED;
					mConnectionState = STATE_DISCONNECTED;
					LOG.logI(TAG, "Disconnected from GATT server.");
					broadcastUpdate(intentAction);
				}else{
					intentAction = ACTION_GATT_CONNECTED;
					mConnectionState = STATE_CONNECTED;
					broadcastUpdate(intentAction);
					LOG.logI(TAG, "Connected to GATT server.");
					// Attempts to discover services after successful connection.
					LOG.logI(TAG, "Attempting to start service discovery:" +
							mBluetoothGatt.discoverServices());
				}
			} else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
				connectIng=false;
				intentAction = ACTION_GATT_DISCONNECTED;
				mConnectionState = STATE_DISCONNECTED;
				LOG.logI(TAG, "Disconnected from GATT server.");
				broadcastUpdate(intentAction);
			}
		}

		@Override
		public void onServicesDiscovered(BluetoothGatt gatt, int status) {
			if (status == BluetoothGatt.GATT_SUCCESS) {
				broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
			} else {
				LOG.logW(TAG, "onServicesDiscovered received: " + status);

				String intentAction;
				intentAction = ACTION_GATT_DISCONNECTED;
				mConnectionState = STATE_DISCONNECTED;
				LOG.logI(TAG, "Disconnected from GATT server.");
				broadcastUpdate(intentAction);
			}
		}

		@Override
		public void onCharacteristicRead(BluetoothGatt gatt,
				BluetoothGattCharacteristic characteristic,
				int status) {
			if (status == BluetoothGatt.GATT_SUCCESS) {
				broadcastUpdate(ACTION_DATA_READ, characteristic);
			}
		}

		@Override
		public void onCharacteristicChanged(BluetoothGatt gatt,
				BluetoothGattCharacteristic characteristic) {
			broadcastUpdate(ACTION_DATA_NOTIFY, characteristic);
		}
	};

	private void broadcastUpdate(final String action) {
		final Intent intent = new Intent(action);
		sendBroadcast(intent);
	}

	private void broadcastUpdate(final String action,
			final BluetoothGattCharacteristic characteristic) {
		final Intent intent = new Intent(action);

		final byte[] data = characteristic.getValue();
		if (data != null&& data.length >=8 ) {
			StringBuilder stringBuilder = new StringBuilder(data.length);
			for(byte byteChar : data){
				String ms=String.format("%02X ", byteChar).trim();
				stringBuilder.append(ms+"-");
			}
			intent.putExtra(BluetoothBuffer.EXTRA_DATA, stringBuilder.toString());
		}

		sendBroadcast(intent);
	}

	public class LocalBinder extends Binder {
		BleConnectService getService() {
			return BleConnectService.this;
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());

		mSendDataHandler=new Handler();
		mSendDataHandler2=new Handler();

		mSendData();
		mSendData2();
		return mBinder;
	}


	private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {    //广播接收
		@Override
		public void onReceive(Context context, Intent intent) {
			final String action = intent.getAction();
			if (ACTION_GATT_CONNECTED.equals(action)) {
				if(mOnDisplayDATA!=null){
					mOnDisplayDATA.OnDATA("result-status-connect");
				}
			}else if (ACTION_GATT_DISCONNECTED.equals(action)) {
				close();
				if(mOnDisplayDATA!=null){
					mOnDisplayDATA.OnDATA("result-status-disconnect");
				}
				mSendDataList.clear();
				mSendDataList2.clear();

				misSend=false;
				fatMeasure.setDataType(DataTypeEnum.DataTypeNone);
			} else if (ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
				displayGattServices(getSupportedGattServices());
				SynchronizeDateBuffer();//同步日期
				SynchronizeTimeBuffer();//同步时间

			}else if(ACTION_DATA_NOTIFY.equals(action)){
				misSend=true;
				String data=intent.getStringExtra(BluetoothBuffer.EXTRA_DATA);

				if(data==null){return;}

				LOG.logD(TAG, data);
				String[] str=data.split("-");

				//				String[] strdata=new String[str.length-2];
				//				if (str[0].equals("F5")&&str[1].equals("5F")){
				//					System.arraycopy(str, 2, strdata, 0, strdata.length);
				//				}else{
				//					return;
				//				}

				String[] strdata=new String[str.length-2];
				if (str[0].equals("FF")){
					System.arraycopy(str, 1, strdata, 0, strdata.length);
				}else{
					return;
				}

				if(strdata[0].equals("A5")){
					if(mSendDataList.size()>0){
						byte[] outBuffer=mSendDataList.get(0);
						switch (strdata[1]) {
						case "30":
							if (String.format("%02X ", outBuffer[1]).trim().equals("30")){
								mSendDataList.remove(outBuffer);
								if(mOnDisplayDATA!=null) mOnDisplayDATA.OnDATA("result-status-data");
							}

							break;
						case "31":
							if (String.format("%02X ", outBuffer[1]).trim().equals("31")){
								mSendDataList.remove(outBuffer);
								if(mOnDisplayDATA!=null) mOnDisplayDATA.OnDATA("result-status-time");
							}
							break;
						case "10":
							if (String.format("%02X ", outBuffer[1]).trim().equals("10")){
								mSendDataList.remove(outBuffer);
								if(mOnDisplayDATA!=null) mOnDisplayDATA.OnDATA("result-status-fatTest");
							}
							break;
						}
					}
					if(mSendDataList2.size()>0){
						byte[] outBuffer=mSendDataList2.get(0);
						switch (strdata[1]) {
						case "20":
							if (String.format("%02X ", outBuffer[1]).trim().equals("20")){
								mSendDataList2.remove(outBuffer);
								fatMeasure.setDataType(DataTypeEnum.DataTypeHistory);
								if(mOnDisplayDATA!=null)mOnDisplayDATA.OnDATA("result-status-dataCommun");
							}
							break;
						}
					}

					switch (strdata[5]) {
					case "BE":
						if(mOnDisplayDATA!=null) mOnDisplayDATA.OnDATA("result-status-fatTestError");
						break;
					case "AA":
					case "A0":
						int WeightNum=Integer.valueOf(strdata[1]+strdata[2],16);
						int lbWeightNum=Integer.valueOf( strdata[3]+strdata[4],16);

						if (strdata[5].equals("A0")){
							fatMeasure.setIfStable(false);
						}else{
							fatMeasure.setIfStable(true);
						}
						if (fatMeasure.getDataType().getValue()==-1){
							fatMeasure.setDataType(DataTypeEnum.DataTypeWeigh);
						}

						switch (fatMeasure.getDataType().getValue()) {
						case 0:
							fatMeasure.setWeightKg(WeightNum);
							fatMeasure.setWeightLb(lbWeightNum);
							break;
						case 1:
							fatMeasure.setWeightKg(WeightNum);
							fatMeasure.setWeightLb(lbWeightNum);
							break;
						case 2:
							fatMeasure.setHistoryWeightKg(WeightNum);
							fatMeasure.setHistoryWeightLb(lbWeightNum);
							break;
						default:
							return;
						}
						fatObjectSacle();
						break;
					case "B0"://脂肪、水份
						int fat_Num=Integer.valueOf(strdata[1]+strdata[2],16);

						int hydration_Num=Integer.valueOf(strdata[3]+strdata[4],16);

						if (fatMeasure.getDataType().getValue()==0) fatMeasure.setDataType(DataTypeEnum.DatatypeTestFat);

						switch (fatMeasure.getDataType().getValue()) {
						case 1:
							fatMeasure.setFat(fat_Num);
							fatMeasure.setHydration(hydration_Num);
							break;
						case 2:
							fatMeasure.setHistoryFat(fat_Num);
							fatMeasure.setHistoryHydration(hydration_Num);
							break;
						default:
							return;
						}
						break;
					case "C0"://肌肉、骨骼
						int  muscle_Num=Integer.valueOf(strdata[1]+strdata[2],16);
						int bone_Num=Integer.valueOf(strdata[4]+strdata[3],16);

						switch (fatMeasure.getDataType().getValue()) {
						case 1:
							fatMeasure.setMuscle(muscle_Num);
							fatMeasure.setBone(bone_Num);
							break;
						case 2:
							fatMeasure.setHistoryMuscle(muscle_Num);
							fatMeasure.setHistoryBone(bone_Num);
							break;
						default:
							return;
						}
						break;
					case "D0"://计算卡路里
						int kcal_Num=Integer.valueOf(strdata[1]+strdata[2],16);

						switch (fatMeasure.getDataType().getValue()) {
						case 1:
							fatMeasure.setKcal(kcal_Num);
							break;
						case 2:
							fatMeasure.setHistoryKcal(kcal_Num);
							break;
						default:
							return;
						}
						break;
					case "E0":
						int current_Num=Integer.valueOf(strdata[4],16);//当前数据组数
						fatMeasure.setNumber(current_Num);
						break;
					case "E1":
						int E1_num=Integer.valueOf(strdata[4],16);
						indexNumE1=E1_num;
						break;
					case "E2":
						int year_Num=Integer.valueOf(strdata[1],16)+2000;
						int day_Num=Integer.valueOf(strdata[2]+strdata[3],16);

						Calendar cal = Calendar.getInstance();
						cal.set(Calendar.YEAR, year_Num);
						cal.set(Calendar.DAY_OF_YEAR, day_Num);
						//					cal.set(Calendar.DAY_OF_YEAR, day_Num);
						cal.set(Calendar.MONTH, 0);
						cal.set(Calendar.MINUTE, 0);
						cal.set(Calendar.SECOND, 0);

						String sign_date=cal.get(Calendar.YEAR)+"-"+String.format("%02d", cal.get(Calendar.MONTH) + 1)+"-"+String.format("%02d", cal.get(Calendar.DAY_OF_MONTH));
						fatMeasure.setHistoryDate(sign_date);

						fatObjectSacle();

						int E2_num=Integer.valueOf(strdata[4],16);
						indexNumE2=E2_num;
						break;
					case "F0":
						int current_Num2=Integer.valueOf(strdata[1],16);//当前数据组数
						fatMeasure.setNumber(current_Num2);

						int  total_Num=Integer.valueOf(strdata[2],16);//当前数据总数组
						if(current_Num2==total_Num){
							fatSendHisListSacle();

							if(mOnDisplayDATA!=null)mOnDisplayDATA.OnDATA("result-status-dataCommunEnd");
							fatMeasure.setDataType(DataTypeEnum.DataTypeNone);
						}
						break;
					}
				}


				if (fatMeasure.getHistoryDate()!=null
						&&fatMeasure.getHistoryWeightKg()>0
						&&fatMeasure.getHistoryFat()>0
						&&fatMeasure.getHistoryHydration()>0
						&&fatMeasure.getHistoryBone()>0
						&&fatMeasure.getHistoryMuscle()>0
						&&fatMeasure.getHistoryKcal()>0){

					if(indexNumE1==indexNumE2){
						fatSendHisObjectSacle();
						fatMeasure.empty();//保存成功
						indexNumE1=-1;
						indexNumE2=-2;
					}else{
						fatMeasure.empty();//记录失败
					}
				}else{
					if(indexNumE1==indexNumE2){
						fatMeasure.empty();//记录失败
						indexNumE1=-1;
						indexNumE2=-2;
					}
				}
			}
		}
	};

	private void fatObjectSacle(){
		switch (fatMeasure.getDataType().getValue()) {
		case -1:
			fatMeasure.setDataType(DataTypeEnum.DatatypeTestFat);return;
		case 0:
			if(fatMeasure.getWeightKg()==-1)	return;
			if(fatMeasure.getWeightLb()==-1)	return;
			if(mOnMeasureDATA!=null){
				mOnMeasureDATA.OnDATA(fatMeasure);
			}
			fatMeasure.empty();
			break;
		case 1:
			if(fatMeasure.getWeightKg()==-1)	return;
			if(fatMeasure.getWeightLb()==-1)	return;
			FatMeasure  tmpbodyMesure=new FatMeasure();
			tmpbodyMesure.setWeightKg(fatMeasure.getWeightKg());
			tmpbodyMesure.setWeightLb(fatMeasure.getWeightLb());
			tmpbodyMesure.setIfStable(fatMeasure.isIfStable());

			if(fatMeasure.getFat()==-1)	return;
			if(fatMeasure.getHydration()==-1)  return;
			if(fatMeasure.getMuscle()==-1)  return;
			if(fatMeasure.getBone()==-1)  return;
			if(fatMeasure.getKcal()==-1)  return;

			if(mOnMeasureDATA!=null){
				mOnMeasureDATA.OnDATA(fatMeasure);
			}
			fatMeasure.empty();
			break;
		}
	}

	private void fatSendHisObjectSacle(){
		switch (fatMeasure.getDataType().getValue()) {
		case 2:
			if(fatMeasure.getNumber()==-1)	break;
			if(fatMeasure.getHistoryDate()==null)	break;
			if(fatMeasure.getHistoryWeightKg()==-1)	break;
			if(fatMeasure.getHistoryWeightLb()==-1)	break;
			if(fatMeasure.getHistoryFat()==-1)  break;
			if(fatMeasure.getHistoryHydration()==-1)  break;
			if(fatMeasure.getHistoryMuscle()==-1)  break;
			if(fatMeasure.getHistoryBone()==-1)  break;
			if(fatMeasure.getHistoryKcal()==-1)  break;
			if (SendHisOBjectList.get(fatMeasure.getNumber())==null){

				FatMeasure tmp=new FatMeasure(fatMeasure);
				SendHisOBjectList.put(fatMeasure.getNumber(), tmp);
			}
			fatMeasure.empty();
			break;
		}
	}

	private void fatSendHisListSacle(){
		Iterator<Integer> itor = SendHisOBjectList.keySet().iterator();
		while(itor.hasNext())
		{
			Integer key = itor.next();
			FatMeasure fatMeasure3 = SendHisOBjectList.get(key);
			if(mOnMeasureDATA!=null)mOnMeasureDATA.OnDATA(fatMeasure3);
		}
		SendHisOBjectList.clear();
	}

	private void displayGattServices(List<BluetoothGattService> gattServices) {
		if (gattServices == null){	String intentAction;
		intentAction = ACTION_GATT_DISCONNECTED;
		mConnectionState = STATE_DISCONNECTED;
		LOG.logI(TAG, "Disconnected from GATT server.");
		broadcastUpdate(intentAction);return;}
		//遍历 GATT 服务可用。
		try {
			lock.lock();  
			for (BluetoothGattService gattService : gattServices) {

				if (gattService.getUuid().toString().equals("0000fff0-0000-1000-8000-00805f9b34fb")) {
					List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
					for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
						if (gattCharacteristic.getUuid().toString().trim().equals("0000fff1-0000-1000-8000-00805f9b34fb")){
							setCharacteristicNotification(gattCharacteristic, true);
						}
						if (gattCharacteristic.getUuid().toString().trim().equals("0000fff2-0000-1000-8000-00805f9b34fb")){
							mWriteCharacteristic=gattCharacteristic;
						}
					}
				}
				if (gattService.getUuid().toString().equals("0000ffb0-0000-1000-8000-00805f9b34fb")) {
					List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
					for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
						if (gattCharacteristic.getUuid().toString().trim().equals("0000ffb2-0000-1000-8000-00805f9b34fb")){
							setCharacteristicNotification(gattCharacteristic, true);
						}
						if (gattCharacteristic.getUuid().toString().trim().equals("0000ffb2-0000-1000-8000-00805f9b34fb")){
							mWriteCharacteristic=gattCharacteristic; 
						}
					}
				}
			}
		} catch (Exception e) {
			String intentAction;
			intentAction = ACTION_GATT_DISCONNECTED;
			mConnectionState = STATE_DISCONNECTED;
			LOG.logI(TAG, "Disconnected from GATT server.");
			broadcastUpdate(intentAction);

			return;
		}finally{
			lock.unlock();  
		}
	}
	private static IntentFilter makeGattUpdateIntentFilter() {
		final IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(ACTION_GATT_CONNECTED);
		intentFilter.addAction(ACTION_GATT_DISCONNECTED);
		intentFilter.addAction(ACTION_GATT_SERVICES_DISCOVERED);
		intentFilter.addAction(ACTION_DATA_NOTIFY);
		return intentFilter;
	}

	/***************************************自定义发送连接代码***/
	private void mSendData(){
		mSendDataHandler.postDelayed(mSendRunnable, 50);
	} 
	Runnable mSendRunnable=	new Runnable() {
		@Override
		public void run() {
			if(mSendDataList.size()!=0&&misSend&&mWriteCharacteristic!=null&&mConnectionState==STATE_CONNECTED){
				byte[] outBuffer=mSendDataList.get(0);
				StringBuilder stringBuilder = new StringBuilder(outBuffer.length);
				for(byte byteChar : outBuffer){
					String ms=String.format("%02X ", byteChar).trim();
					stringBuilder.append(ms+"-");
				}
				LOG.logD(TAG, "发送1："+stringBuilder.toString());

				mWriteCharacteristic.setValue(outBuffer);
				writeCharacteristic(mWriteCharacteristic);
			}
			mSendData(); //不断发送
		}
	};

	private void mSendData2(){
		mSendDataHandler2.postDelayed(mSendRunnable2, 50);
	} 
	Runnable mSendRunnable2=	new Runnable() {
		@Override
		public void run() {
			if(mSendDataList2.size()!=0&&misSend&&mWriteCharacteristic!=null&&mConnectionState==STATE_CONNECTED){
				byte[] outBuffer=mSendDataList2.get(0);
				StringBuilder stringBuilder = new StringBuilder(outBuffer.length);
				for(byte byteChar : outBuffer){
					String ms=String.format("%02X ", byteChar).trim();
					stringBuilder.append(ms+"-");
				}
				LOG.logD(TAG, "发送2："+stringBuilder.toString());

				mWriteCharacteristic.setValue(outBuffer);
				writeCharacteristic(mWriteCharacteristic);
			}
			mSendData2(); //不断发送
		}
	};

	public void SynchronizeDateBuffer() { 
		Calendar cal = Calendar.getInstance();

		byte[] outBuffer=BluetoothBuffer.SynchronizeDateBuffer;
		outBuffer[2]=(byte)Integer.parseInt(Long.toHexString(Integer.valueOf(String.valueOf(cal.get(Calendar.YEAR)).substring(2))), 16); //年

		String DayLength=Long.toHexString(cal.get(Calendar.DAY_OF_YEAR));
		DayLength=DayLength.length()==1?"000"+DayLength:
			DayLength.length()==2?"00"+DayLength:
				DayLength.length()==3?"0"+DayLength:DayLength;
		
		outBuffer[3]=(byte)Integer.parseInt(DayLength.substring(0,2), 16);
		outBuffer[4]=(byte)Integer.parseInt(DayLength.substring(2,4), 16);

		byte verify = 0;
		for(int i=1;i<outBuffer.length-2;i++){
			verify=(byte) (verify+outBuffer[i]);
		}
		outBuffer[outBuffer.length-2]=verify;
		
		mSendDataList.add(outBuffer);
	}

	public void SynchronizeTimeBuffer() { 
		Calendar cal = Calendar.getInstance();
		byte[] outBuffer=BluetoothBuffer.SynchronizeTimeBuffer;

		outBuffer[2]=(byte)Integer.parseInt(Long.toHexString(cal.get(Calendar.HOUR_OF_DAY)), 16);//时
		outBuffer[3]=(byte)Integer.parseInt(Long.toHexString(cal.get(Calendar.MINUTE)), 16);//分
		outBuffer[4]=(byte)Integer.parseInt(Long.toHexString(cal.get(Calendar.SECOND)), 16);//秒

		byte verify = 0;
		for(int i=1;i<outBuffer.length-2;i++){
			verify=(byte) (verify+outBuffer[i]);
		}
		outBuffer[outBuffer.length-2]=verify;

		mSendDataList.add(outBuffer);
	}

	public void DataCommunBuffer(final int serialNum) { 
		fatMeasure.setUserID(serialNum);

		byte[] outBuffer=BluetoothBuffer.DataCommunBuffer;
		outBuffer[2]=(byte)Integer.parseInt(Long.toHexString(serialNum), 16);

		byte verify = 0;
		for(int i=1;i<outBuffer.length-2;i++){
			verify=(byte) (verify+outBuffer[i]);
		}
		outBuffer[outBuffer.length-2]=verify;

		mSendDataList2.add(outBuffer);
	}

	public void DataCommunBufferTest(final int serialNum) { 
		fatMeasure.setUserID(serialNum);

		byte[] outBuffer=BluetoothBuffer.DataCommunBuffer;
		outBuffer[2]=(byte)Integer.parseInt(Long.toHexString(serialNum), 16);

		byte verify = 0;
		for(int i=1;i<outBuffer.length-2;i++){
			verify=(byte) (verify+outBuffer[i]);
		}
		outBuffer[outBuffer.length-2]=verify;
		
		mWriteCharacteristic.setValue(outBuffer);
		writeCharacteristic(mWriteCharacteristic);
	}

	public void FatTestBuffer(final int height,final int age,final int sex,final int serialNum) { 
		fatMeasure.setUserID(serialNum);

		byte[] outBuffer =BluetoothBuffer.TestFatBuffer;
		int intByte2=(sex==0?0:8)*16+serialNum;
		outBuffer[2]=(byte)Integer.parseInt(Long.toHexString(intByte2), 16);
		outBuffer[3]=(byte)Integer.parseInt(Long.toHexString(age), 16);
		outBuffer[4]=(byte)Integer.parseInt(Long.toHexString(height), 16);
		
		byte verify = 0;
		for(int i=1;i<outBuffer.length-2;i++){
			verify=(byte) (verify+outBuffer[i]);
		}
		outBuffer[outBuffer.length-2]=verify;
		
		mSendDataList.add(outBuffer);

	}
	@Override
	public boolean onUnbind(Intent intent) {
		// After using a given device, you should make sure that BluetoothGatt.close() is called
		// such that resources are cleaned up properly.  In this particular example, close() is
		// invoked when the UI is disconnected from the Service.
		close();
		mSendDataHandler.removeCallbacks(mSendRunnable);
		unregisterReceiver(mGattUpdateReceiver);
		return super.onUnbind(intent);
	}

	private final IBinder mBinder = new LocalBinder();

	/**
	 * Initializes a reference to the local Bluetooth adapter.
	 *
	 * @return Return true if the initialization is successful.
	 */
	public boolean initialize() {
		// For API level 18 and above, get a reference to BluetoothAdapter through
		// BluetoothManager.
		if (mBluetoothManager == null) {
			mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
			if (mBluetoothManager == null) {
				LOG.logE(TAG, "Unable to initialize BluetoothManager.");
				return false;
			}
		}

		mBluetoothAdapter = mBluetoothManager.getAdapter();
		if (mBluetoothAdapter == null) {
			LOG.logE(TAG, "Unable to obtain a BluetoothAdapter.");
			return false;
		}

		return true;
	}

	/**
	 * Connects to the GATT server hosted on the Bluetooth LE device.
	 *
	 * @param address The device address of the destination device.
	 *
	 * @return Return true if the connection is initiated successfully. The connection result
	 *         is reported asynchronously through the
	 *         {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
	 *         callback.
	 */
	public boolean connect(final String address) {
		if (connectIng)return false;
		connectIng=true;
		if (mBluetoothAdapter == null || address == null) {
			LOG.logW(TAG, "BluetoothAdapter not initialized or unspecified address.");
			return false;
		}

		// Previously connected device.  Try to reconnect.
		if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
				&& mBluetoothGatt != null) {
			LOG.logD(TAG, "Trying to use an existing mBluetoothGatt for connection.");
			if (mBluetoothGatt.connect()) {
				mConnectionState = STATE_CONNECTING;
				return true;
			} else {
				return false;
			}
		}

		final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
		if (device == null) {
			LOG.logW(TAG, "Device not found.  Unable to connect.");
			return false;
		}
		// We want to directly connect to the device, so we are setting the autoConnect
		// parameter to false.
		mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
		LOG.logD(TAG, "Trying to create a new connection.");
		mBluetoothDeviceAddress = address;
		mConnectionState = STATE_CONNECTING;
		return true;
	}


	/**
	 * Disconnects an existing connection or cancel a pending connection. The disconnection result
	 * is reported asynchronously through the
	 * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
	 * callback.
	 */
	public void disconnect() {
		if (mBluetoothAdapter == null || mBluetoothGatt == null) {
			LOG.logW(TAG, "BluetoothAdapter not initialized");
			return;
		}
		mBluetoothGatt.disconnect();
	}

	/**
	 * After using a given BLE device, the app must call this method to ensure resources are
	 * released properly.
	 */
	public void close() {
		if (mBluetoothGatt == null) {
			return;
		}
		mBluetoothGatt.close();
		mBluetoothGatt = null;
	}

	/**
	 * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
	 * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
	 * callback.
	 *
	 * @param characteristic The characteristic to read from.
	 */
	public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
		if (mBluetoothAdapter == null || mBluetoothGatt == null) {
			LOG.logW(TAG, "BluetoothAdapter not initialized");
			return;
		}
		mBluetoothGatt.readCharacteristic(characteristic);
	}

	/**
	 * Enables or disables notification on a give characteristic.
	 *
	 * @param characteristic Characteristic to act on.
	 * @param enabled If true, enable notification.  False otherwise.
	 */
	public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
			boolean enabled) {
		if (mBluetoothAdapter == null || mBluetoothGatt == null) {
			LOG.logW(TAG, "BluetoothAdapter not initialized");
			return;
		}
		mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);

		BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
		if(descriptor==null)return;

		descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
		mBluetoothGatt.writeDescriptor(descriptor);
	}

	public boolean writeCharacteristic(BluetoothGattCharacteristic characteristic) {
		if(mBluetoothGatt==null)return false;
		return mBluetoothGatt.writeCharacteristic(characteristic);
	}

	public boolean ismConnect() {
		if (mConnectionState==STATE_CONNECTED)
			return true;
		else return false;
	}


	/**
	 * Retrieves a list of supported GATT services on the connected device. This should be
	 * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
	 *
	 * @return A {@code List} of supported services.
	 */
	public List<BluetoothGattService> getSupportedGattServices() {
		if (mBluetoothGatt == null) return null;

		return mBluetoothGatt.getServices();
	}

	public interface OnDisplayDATA{
		void OnDATA(String data);
	}

	OnDisplayDATA mOnDisplayDATA=null;
	public void setOnDisplayDATA(OnDisplayDATA e){
		mOnDisplayDATA=e;
	}

	public interface OnMeasureDATA{
		void OnDATA(FatMeasure fatMeasure);
	}

	OnMeasureDATA mOnMeasureDATA=null;
	public void setOnMeasureDATA(OnMeasureDATA e){
		mOnMeasureDATA=e;
	}
}
