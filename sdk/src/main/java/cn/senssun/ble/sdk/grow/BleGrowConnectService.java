package cn.senssun.ble.sdk.grow;


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
import java.util.List;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import cn.senssun.ble.sdk.BluetoothBuffer;
import cn.senssun.ble.sdk.entity.GrowMeasure;
import cn.senssun.ble.sdk.util.LOG;

/**
 * Service for managing connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 */
public class BleGrowConnectService extends Service {
	public final static String ACTION_GATT_DISCONNECTED = "cn.senssun.ble.sdk.BleGrowConnectService.ACTION_GATT_DISCONNECTED";
	public final static String ACTION_GATT_CONNECTED ="cn.senssun.ble.sdk.BleGrowConnectService.ACTION_GATT_CONNECTED";
	public final static String ACTION_GATT_SERVICES_DISCOVERED = "cn.senssun.ble.sdk.BleGrowConnectService.ACTION_GATT_SERVICES_DISCOVERED";
	public final static String ACTION_DATA_NOTIFY = "cn.senssun.ble.sdk.BleGrowConnectService.ACTION_DATA_NOTIFY";
	public final static String ACTION_DATA_WRITE = "cn.senssun.ble.sdk.BleGrowConnectService.ACTION_DATA_WRITE";
	public final static String ACTION_DATA_READ = "cn.senssun.ble.sdk.BleGrowConnectService.ACTION_DATA_READ";
	
	private final static String TAG = "BleGrowConnectService";

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

	private List<String> mSendDataList=new ArrayList();
	private List<String> mSendDataList2=new ArrayList();

	private boolean connectIng=false;
	public boolean misSend=false;

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
//					connectIng=false;
					disconnect();
//					intentAction = BluetoothBuffer.ACTION_GATT_DISCONNECTED;
//					mConnectionState = STATE_DISCONNECTED;
//					LOG.logI(TAG, "Disconnected from GATT server.");
//					broadcastUpdate(intentAction);
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
		BleGrowConnectService getService() {
			return BleGrowConnectService.this;
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
			} else if (ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
				displayGattServices(getSupportedGattServices());
				SynchronizeDateBuffer();//同步日期

			}else if(ACTION_DATA_NOTIFY.equals(action)){
				misSend=true;
				String data=intent.getStringExtra(BluetoothBuffer.EXTRA_DATA);

				if(data==null){return;}

				LOG.logD(TAG, data);
				String[] strdata=data.split("-");

				if (strdata[1].equals("50")){
					if(mSendDataList.size()!=0){
						String outBuffer=mSendDataList.get(0);

						if (outBuffer.split("-")[1].trim().equals("50")){
							if(mOnDisplayDATA!=null){
								mOnDisplayDATA.OnDATA("result-status-50-"+strdata[2]);
							}
						}
						
						List<String> delbuffer=new ArrayList<>();
						for(String buffer:mSendDataList){
							if (outBuffer.split("-")[1].trim().equals("50")){
								delbuffer.add(buffer);
							}
						}
						for(String buffer:delbuffer){
							mSendDataList.remove(buffer);
						}
					}
				}
				if (strdata[1].equals("51")){
					if(mSendDataList.size()!=0){
						String outBuffer=mSendDataList.get(0);

						if (outBuffer.split("-")[1].trim().equals("51")){
							if(mOnDisplayDATA!=null){
								mOnDisplayDATA.OnDATA("result-status-51");
							}
						}
						
						List<String> delbuffer=new ArrayList<>();
						for(String buffer:mSendDataList){
							if (outBuffer.split("-")[1].trim().equals("51")){
								delbuffer.add(buffer);
							}
						}
						for(String buffer:delbuffer){
							mSendDataList.remove(buffer);
						}
					}
				}
				if (strdata[1].equals("52")){
					if(mSendDataList.size()!=0){
						String outBuffer=mSendDataList.get(0);

						if (outBuffer.split("-")[1].trim().equals("52")){
							if(mOnDisplayDATA!=null){
								mOnDisplayDATA.OnDATA("result-status-52");
							}
						}
						
						List<String> delbuffer=new ArrayList<>();
						for(String buffer:mSendDataList){
							if (outBuffer.split("-")[1].trim().equals("52")){
								delbuffer.add(buffer);
							}
						}
						for(String buffer:delbuffer){
							mSendDataList.remove(buffer);
						}
					}
				}
				if (strdata[1].equals("60")){
					if(mSendDataList2.size()!=0){
						String outBuffer=mSendDataList2.get(0);

						if (outBuffer.split("-")[1].trim().equals("60")){
							if(mOnDisplayDATA!=null){
								mOnDisplayDATA.OnDATA("result-status-60");
							}
						}
						
						List<String> delbuffer=new ArrayList<>();
						for(String buffer:mSendDataList2){
							if (outBuffer.split("-")[1].trim().equals("60")){
								delbuffer.add(buffer);
							}
						}
						for(String buffer:delbuffer){
							mSendDataList2.remove(buffer);
						}
					}
				}
				if (strdata[1].equals("61")){
					if(mSendDataList.size()>0){
						String outBuffer=mSendDataList.get(0);

						if (outBuffer.split("-")[1].trim().equals("61")&&strdata[2].equals(outBuffer.split("-")[2].trim())){
							mSendDataList.remove(outBuffer);
							if(mOnDisplayDATA!=null){
								mOnDisplayDATA.OnDATA("result-status-61");
							}
						}
						
//						List<byte[]> delbuffer=new ArrayList<byte[]>();
//						for(byte[] buffer:mSendDataList){
//							if (String.format("%02X ", buffer[1]).trim().equals("61")){
//								delbuffer.add(buffer);
//							}
//						}
//						for(byte[] buffer:delbuffer){
//							mSendDataList.remove(buffer);
//						}
					}
				}
				if (strdata[1].equals("6A")){
					if(mSendDataList.size()>0){
						String outBuffer=mSendDataList.get(0);

						if (outBuffer.split("-")[1].trim().equals("6A")&&strdata[2].equals(outBuffer.split("-")[2].trim())){
							mSendDataList.remove(outBuffer);
							if(mOnDisplayDATA!=null){
								mOnDisplayDATA.OnDATA("result-status-6A");
							}
						}
						
//						List<byte[]> delbuffer=new ArrayList<byte[]>();
//						for(byte[] buffer:mSendDataList){
//							if (String.format("%02X ", buffer[1]).trim().equals("6A")){
//								delbuffer.add(buffer);
//							}
//						}
//						for(byte[] buffer:delbuffer){
//							mSendDataList.remove(buffer);
//						}
					}
				}
				if (strdata[1].equals("A5")){

					GrowMeasure growMeasure=new GrowMeasure();//发出对象

					String tmpNum= strdata[2]+strdata[3];
					int WeightNum=Integer.valueOf(tmpNum,16);
					growMeasure.setWeightKg(WeightNum);

					tmpNum= strdata[4]+strdata[5];
					int lbWeightNum=Integer.valueOf(tmpNum,16);
					growMeasure.setWeightLb(lbWeightNum);

					tmpNum= strdata[6]+strdata[7];
					int cmHeightNum=Integer.valueOf(tmpNum,16);
					growMeasure.setHeightCm(cmHeightNum);
					
					growMeasure.setSerimal(Integer.valueOf(strdata[9],16));

					if (strdata[10].equals("A0")){
						growMeasure.setIfStable(false);
					}else{
						growMeasure.setIfStable(true);
					}
					
					
					
					if ("01-02-03-04-05-06-07-08-09".contains(strdata[8])){
						if("02-04".contains(strdata[8])){
							if("02".equals(strdata[8]))growMeasure.setSymbol(true); else growMeasure.setSymbol(false);
							growMeasure.setUnitType(GrowMeasure.UnitTypeEnum.LbInchUnit);
						}else if("06-08".contains(strdata[8])){
							if("06".equals(strdata[8]))growMeasure.setSymbol(true); else growMeasure.setSymbol(false);
							growMeasure.setUnitType(GrowMeasure.UnitTypeEnum.OzInchUnit);
						}else if("07-09".contains(strdata[8])){
							if("07".equals(strdata[8]))growMeasure.setSymbol(true); else growMeasure.setSymbol(false);
							growMeasure.setUnitType(GrowMeasure.UnitTypeEnum.LbOzInchUnit);
						}else{
							if("01".equals(strdata[8]))growMeasure.setSymbol(true); else growMeasure.setSymbol(false);
							growMeasure.setUnitType(GrowMeasure.UnitTypeEnum.KgCmUnit);
						}
					}

					growMeasure.setDataType(GrowMeasure.DataTypeEnum.DataTypeWeigh);
					if(mOnMeasureDATA!=null){
						mOnMeasureDATA.OnDATA(growMeasure);
					}
				}else if (strdata[1].equals("75")&&strdata[0].equals("FF")){
					GrowMeasure growMeasure=new GrowMeasure();//发出对象

					String tmpNum= strdata[2];
					int year_Num=Integer.valueOf(tmpNum,16)+2000;

					tmpNum= strdata[3]+strdata[4];
					int day_Num=Integer.valueOf(tmpNum,16);

					Calendar cal = Calendar.getInstance();
					cal.set(Calendar.YEAR, year_Num);
					cal.set(Calendar.DAY_OF_YEAR, day_Num);
					cal.set(Calendar.HOUR_OF_DAY, 23);
					cal.set(Calendar.MINUTE, 59);
					cal.set(Calendar.SECOND, 59);
					growMeasure.setHistoryDate(cal.getTime());

					tmpNum= strdata[5]+strdata[6]; //重量kg 高位 低位
					growMeasure.setHistoryWeightKg(Integer.valueOf(tmpNum,16));

					tmpNum=strdata[7]+strdata[8];//身高
					growMeasure.setHistoryHeightCm(Integer.valueOf(tmpNum,16));


					tmpNum= strdata[9];
					growMeasure.setHistoryUserSerimal(Integer.valueOf(tmpNum,16));

					tmpNum= strdata[10];
					growMeasure.setHistoryDataSerimal(Integer.valueOf(tmpNum,16));

					tmpNum= strdata[11];
					growMeasure.setHistoryDataAmount(Integer.valueOf(tmpNum,16));

					growMeasure.setDataType(GrowMeasure.DataTypeEnum.DataTypeHistory);
					if(mOnMeasureDATA!=null){
						mOnMeasureDATA.OnDATA(growMeasure);
					}
				}
			}
		}
	};


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
				String outBuffer=mSendDataList.get(0);
				String[] outBuf=outBuffer.split("-");
				byte[] out=new byte[outBuf.length];
				for(int i=0;i<outBuf.length;i++){
					out[i]=(byte)Integer.parseInt(outBuf[i], 16); 
				}
//				StringBuilder stringBuilder = new StringBuilder(outBuffer.length);
//				for(byte byteChar : outBuffer){
//					String ms=String.format("%02X ", byteChar).trim();
//					stringBuilder.append(ms+"-");
//				}
				LOG.logD(TAG, "发送1："+outBuffer);

				mWriteCharacteristic.setValue(out);
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
				String outBuffer=mSendDataList2.get(0);
				String[] outBuf=outBuffer.split("-");
				byte[] out=new byte[outBuf.length];
				for(int i=0;i<outBuf.length;i++){
					out[i]=(byte)Integer.parseInt(outBuf[i], 16); 
				}
				
//				StringBuilder stringBuilder = new StringBuilder(outBuffer.length);
//				for(byte byteChar : outBuffer){
//					String ms=String.format("%02X ", byteChar).trim();
//					stringBuilder.append(ms+"-");
//				}
				LOG.logD(TAG, "发送2："+outBuffer);

				mWriteCharacteristic.setValue(out);
				writeCharacteristic(mWriteCharacteristic);
			}
			mSendData2(); //不断发送
		}
	};

	public void SynchronizeDateBuffer() { 
		Calendar cal = Calendar.getInstance();
		byte[] outBuffer = BluetoothBuffer.GrowSynchronizeDateBuffer;

		int intByte2=Integer.valueOf(String.valueOf(cal.get(Calendar.YEAR)).substring(2));
		String byte2=Long.toHexString(intByte2);
		outBuffer[2]=(byte)Integer.parseInt(byte2, 16);

		int intByte3and4=cal.get(Calendar.DAY_OF_YEAR);
		String byte3and4=Long.toHexString(intByte3and4);
		byte3and4=byte3and4.length()==1?"000"+byte3and4:
			byte3and4.length()==2?"00"+byte3and4:
				byte3and4.length()==3?"0"+byte3and4:byte3and4;

		String byte3=byte3and4.substring(0, 2);
		outBuffer[3]=(byte)Integer.parseInt(byte3, 16);
		int intByte3=Integer.valueOf(byte3,16);

		String byte4=byte3and4.substring(2, 4);
		outBuffer[4]=(byte)Integer.parseInt(byte4, 16);
		int intByte4=Integer.valueOf(byte4,16);

		int intByte5=cal.get(Calendar.HOUR_OF_DAY);
		String 	byte5=Long.toHexString(intByte5);
		outBuffer[5]=(byte)Integer.parseInt(byte5, 16);

		int intByte6=cal.get(Calendar.MINUTE);
		String byte6=Long.toHexString(intByte6);
		outBuffer[6]=(byte)Integer.parseInt(byte6, 16);

		int intByte7=cal.get(Calendar.SECOND);
		String byte7=Long.toHexString(intByte7);
		outBuffer[7]=(byte)Integer.parseInt(byte7, 16);

		int intbyte8=96+intByte2+intByte3+intByte4+intByte5+intByte6+intByte7;
		String byte8=Long.toHexString(intbyte8);
		byte8=byte8.substring(byte8.length()-2,byte8.length());
		//				byte7="6E";
		outBuffer[8]=(byte)Integer.parseInt(byte8, 16);

		StringBuilder stringBuilder = new StringBuilder(outBuffer.length);
		for(byte byteChar : outBuffer){
			String ms=String.format("%02X ", byteChar).trim();
			stringBuilder.append(ms+"-");
		}
		mSendDataList2.add(stringBuilder.toString());
	}

	public void AddUserBuffer(final int serialNum) { 
		String	byte2=Long.toHexString(serialNum);
		byte[] outBuffer=BluetoothBuffer.GrowAddUserBuffer;

		outBuffer[2]=(byte)Integer.parseInt(byte2, 16);
		outBuffer[8]=(byte) (outBuffer[1]+outBuffer[2]);

		StringBuilder stringBuilder = new StringBuilder(outBuffer.length);
		for(byte byteChar : outBuffer){
			String ms=String.format("%02X ", byteChar).trim();
			stringBuilder.append(ms+"-");
		}
		mSendDataList.add(stringBuilder.toString());
	}

	public void SeleUserBuffer(final int serialNum) { 
		String	byte2=Long.toHexString(serialNum);
		byte[] outBuffer=BluetoothBuffer.GrowSeleUserBuffer;

		outBuffer[2]=(byte)Integer.parseInt(byte2, 16);
		outBuffer[8]=(byte) (outBuffer[1]+outBuffer[2]);

		StringBuilder stringBuilder = new StringBuilder(outBuffer.length);
		for(byte byteChar : outBuffer){
			String ms=String.format("%02X ", byteChar).trim();
			stringBuilder.append(ms+"-");
		}
		mSendDataList.add(stringBuilder.toString());
	}
	public void DeleUserBuffer(final int serialNum) { 
		String	byte2=Long.toHexString(serialNum);
		byte[] outBuffer=BluetoothBuffer.GrowDeleUserBuffer;

		outBuffer[2]=(byte)Integer.parseInt(byte2, 16);
		outBuffer[8]=(byte) (outBuffer[1]+outBuffer[2]);

		StringBuilder stringBuilder = new StringBuilder(outBuffer.length);
		for(byte byteChar : outBuffer){
			String ms=String.format("%02X ", byteChar).trim();
			stringBuilder.append(ms+"-");
		}
		mSendDataList.add(stringBuilder.toString());
	}
	public void ShallowSysBuffer(String serialNum) { 
//		String	byte2=Long.toHexString(serialNum);
		
		byte[] outBuffer=BluetoothBuffer.GrowDataShallowBuffer;

		outBuffer[2]=(byte)Integer.parseInt(serialNum, 16);
		outBuffer[8]=(byte) (outBuffer[1]+outBuffer[2]);

		StringBuilder stringBuilder = new StringBuilder(outBuffer.length);
		for(byte byteChar : outBuffer){
			String ms=String.format("%02X ", byteChar).trim();
			stringBuilder.append(ms+"-");
		}
		mSendDataList.add(stringBuilder.toString());
	}

	public void DeepSysBuffer(String serialNum) { 
//		String	byte2=Long.toHexString(serialNum);
		byte[] outBuffer=BluetoothBuffer.GrowDataDeepBuffer;

		outBuffer[2]=(byte)Integer.parseInt(serialNum, 16);
		outBuffer[8]=(byte) (outBuffer[1]+outBuffer[2]);

		StringBuilder stringBuilder = new StringBuilder(outBuffer.length);
		for(byte byteChar : outBuffer){
			String ms=String.format("%02X ", byteChar).trim();
			stringBuilder.append(ms+"-");
		}
		mSendDataList.add(stringBuilder.toString());
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
		if(mConnectionState!=STATE_DISCONNECTED)return false;

		if (mBluetoothAdapter == null || address == null) {
			LOG.logW(TAG, "BluetoothAdapter not initialized or unspecified address.");
			return false;
		}

		// Previously connected device.  Try to reconnect.
		if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
				&& mBluetoothGatt != null) {
			LOG.logI(TAG, "Trying to use an existing mBluetoothGatt for connection.");
			if (mBluetoothGatt.connect()) {
				mConnectionState = STATE_CONNECTING;
				return true;
			} else {
				return false;
			}
		}
		//		else{
		//			close();
		//		}

		final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
		if (device == null) {
			LOG.logW(TAG, "Device not found.  Unable to connect.");
			return false;
		}
		// We want to directly connect to the device, so we are setting the autoConnect
		// parameter to false.
		mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
		LOG.logI(TAG, "Trying to create a new connection.");
		mConnectionState = STATE_CONNECTING;
		mBluetoothDeviceAddress = address;
		
//		if (connectIng)return false;
//		connectIng=true;
//		if (mBluetoothAdapter == null || address == null) {
//			LOG.logW(TAG, "BluetoothAdapter not initialized or unspecified address.");
//			return false;
//		}
//
//		// Previously connected device.  Try to reconnect.
//		if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
//				&& mBluetoothGatt != null) {
//			LOG.logD(TAG, "Trying to use an existing mBluetoothGatt for connection.");
//			if (mBluetoothGatt.connect()) {
//				mConnectionState = STATE_CONNECTING;
//				return true;
//			} else {
//				return false;
//			}
//		}
//
//		final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
//		if (device == null) {
//			LOG.logW(TAG, "Device not found.  Unable to connect.");
//			return false;
//		}
//		// We want to directly connect to the device, so we are setting the autoConnect
//		// parameter to false.
//		mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
//		LOG.logD(TAG, "Trying to create a new connection.");
//		mBluetoothDeviceAddress = address;
//		mConnectionState = STATE_CONNECTING;
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
	public interface OnMeasureDATA{
		void OnDATA(GrowMeasure growMeasure);
	}

	OnDisplayDATA mOnDisplayDATA=null;
	public void setOnDisplayDATA(OnDisplayDATA e){
		mOnDisplayDATA=e;
	}

	OnMeasureDATA mOnMeasureDATA=null;
	public void setOnMeasureDATA(OnMeasureDATA e){
		mOnMeasureDATA=e;
	}
}
