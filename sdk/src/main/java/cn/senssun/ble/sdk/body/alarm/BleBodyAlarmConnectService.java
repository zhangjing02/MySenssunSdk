package cn.senssun.ble.sdk.body.alarm;


import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import cn.senssun.ble.sdk.BluetoothBuffer;
import cn.senssun.ble.sdk.body.alarm.BleBodyAlarmSDK.OnInitService;
import cn.senssun.ble.sdk.entity.BodyMeasure;
import cn.senssun.ble.sdk.entity.FatDataCommun;
import cn.senssun.ble.sdk.util.LOG;

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

/**
 * Service for managing connection and data communication with a GATT server hosted on a given Bluetooth LE device.
 */
public class BleBodyAlarmConnectService extends Service {
	public final static String ACTION_GATT_DISCONNECTED = "cn.senssun.ble.sdk.BleBodyAlarmConnectService.ACTION_GATT_DISCONNECTED";
	public final static String ACTION_GATT_CONNECTED ="cn.senssun.ble.sdk.BleBodyAlarmConnectService.ACTION_GATT_CONNECTED";
	public final static String ACTION_GATT_SERVICES_DISCOVERED = "cn.senssun.ble.sdk.BleBodyAlarmConnectService.ACTION_GATT_SERVICES_DISCOVERED";
	public final static String ACTION_DATA_READ = "cn.senssun.ble.sdk.BleBodyAlarmConnectService.ACTION_DATA_READ";
	public final static String ACTION_DATA_NOTIFY = "cn.senssun.ble.sdk.BleBodyAlarmConnectService.ACTION_DATA_NOTIFY";
	
	private final static String TAG = "BleBodyAlarmConnectService";

	private BluetoothManager mBluetoothManager;
	private BluetoothAdapter mBluetoothAdapter;
	private String mBluetoothDeviceAddress;
	private BluetoothGatt mBluetoothGatt;
	private int mConnectionState = STATE_DISCONNECTED;

	private static final int STATE_DISCONNECTED = 0;
	private static final int STATE_CONNECTING = 1;
	private static final int STATE_CONNECTED = 2;

	//	private Handler mSynchronizeDateHandler;
	//	private Handler mSynchronizeTimeHandler;
	//	private Handler mDataCommunHandler;
	//	private Handler mBodyTestHandler;
	//	private Handler mSetAlarmHandler;

	private Handler mSendDataHandler;

	private List<byte[]> mSendDataList=new ArrayList<byte[]>();

	//	private boolean mSynchronizeDate=false;
	//	private boolean mSynchronizeTime=false;
	//	public boolean mDataCommun=false;
	//	public boolean mBodyTest=false;
	//	public boolean mSetAlarm=false;
	public boolean misSend=false;

	private BluetoothGattCharacteristic mWriteCharacteristic; //写出GATT,char

	private HashMap<Integer, FatDataCommun> SendHisOBjectList=new HashMap<Integer, FatDataCommun>();
	// Implements callback methods for GATT events that the app cares about.  For example,
		// connection change and services discovered.
	private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
		@Override
		public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
			String intentAction;
			if (newState == BluetoothProfile.STATE_CONNECTED) {
				if(status==133){
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
			BleBodyAlarmConnectService getService() {
				return BleBodyAlarmConnectService.this;
			}
		}
		@Override
		public IBinder onBind(Intent intent) {
			registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());

			mSendDataHandler=new Handler();
			mSendData();
			return binder;
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
					misSend=false;
				} else if (ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
					displayGattServices(getSupportedGattServices());
					SynchronizeDateBuffer();//同步日期
					SynchronizeTimeBuffer();//同步时间

				}else if(ACTION_DATA_NOTIFY.equals(action)){
				misSend=true;
				String data=intent.getStringExtra(BluetoothBuffer.EXTRA_DATA);
				
				if(data==null){return;}
				
				LOG.logD(TAG, data);
				String[] strdata=data.split("-");
				//发送命令回复
				if(strdata[1].equals("AA")){
					if (strdata[2].equals("30")){
						if(mSendDataList.size()!=0){
							byte[] outBuffer=mSendDataList.get(0);

							if (String.format("%02X ", outBuffer[1]).trim().equals("30")){
								mSendDataList.remove(outBuffer);
								
								if(mOnDisplayDATA!=null){
									mOnDisplayDATA.OnDATA("result-status-data");
								}
							}
						}
					}
					if (strdata[2].equals("31")){
						if(mSendDataList.size()!=0){
							byte[] outBuffer=mSendDataList.get(0);

							if (String.format("%02X ", outBuffer[1]).trim().equals("31")){
								mSendDataList.remove(outBuffer);
								
								if(mOnDisplayDATA!=null){
									mOnDisplayDATA.OnDATA("result-status-time");
								}
							}
						}
					}
					
					if (strdata[2].equals("32")){
						if(mSendDataList.size()!=0){
							byte[] outBuffer=mSendDataList.get(0);

							if (String.format("%02X ", outBuffer[1]).trim().equals("32")&&String.format("%02X ", outBuffer[6]).trim().equals(strdata[3])){
								mSendDataList.remove(outBuffer);
								
								if(mOnDisplayDATA!=null){
									mOnDisplayDATA.OnDATA("result-status-setAlarm");
								}
							}
						}
						
					}
				}
				//数据获取
				if ("AA-A0".contains(strdata[6])&&strdata[1].equals("AA")&&strdata[0].equals("FF")){
					String tmpNum= strdata[2]+strdata[3];
					int WeightNum=Integer.valueOf(tmpNum,16);
					
					 tmpNum= strdata[4]+strdata[5];
					 int lbWeightNum=Integer.valueOf(tmpNum,16);
					
					 BodyMeasure bodyMeasure=new BodyMeasure();
					 if (strdata[6].equals("A0")){
						 bodyMeasure.setIfStable(false);
					 }else{
						 bodyMeasure.setIfStable(true);
					 }
					bodyMeasure.setWeightKg(WeightNum);
					bodyMeasure.setWeightLb(lbWeightNum);
					 
						if(mOnMeasureDATA!=null){
							mOnMeasureDATA.OnMeasureDATA(bodyMeasure);
						}
				}
			}
		}
	};
	

	private void displayGattServices(List<BluetoothGattService> gattServices) {
		if (gattServices == null) return;
		//遍历 GATT 服务可用。
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
	Runnable mSendRunnable=	new Runnable() {
		@Override
		public void run() {
			if(mSendDataList.size()!=0&&misSend&&mWriteCharacteristic!=null&&mConnectionState==STATE_CONNECTED){
				byte[] outBuffer=mSendDataList.get(0);
				mWriteCharacteristic.setValue(outBuffer);
				writeCharacteristic(mWriteCharacteristic);
			}
			mSendData(); //不断发送
		}
	};

	private void mSendData(){
		mSendDataHandler.postDelayed(mSendRunnable, 200);
	} 

	public void SynchronizeDateBuffer() { 
		Calendar cal = Calendar.getInstance();

		int intByte2=Integer.valueOf(String.valueOf(cal.get(Calendar.YEAR)).substring(2)); //年
		String byte2=Long.toHexString(intByte2);
		byte[] outBuffer=BluetoothBuffer.SynchronizeDateBuffer;
		outBuffer[2]=(byte)Integer.parseInt(byte2, 16);

		int intByte3and4=cal.get(Calendar.DAY_OF_YEAR); //第几天
		String byte3and4=Long.toHexString(intByte3and4);
		byte3and4=byte3and4.length()==1?"000"+byte3and4:
			byte3and4.length()==2?"00"+byte3and4:
				byte3and4.length()==3?"0"+byte3and4:byte3and4;

		String byte3=byte3and4.substring(0,2);
		outBuffer[3]=(byte)Integer.parseInt(byte3, 16);
		int intByte3=Integer.valueOf(byte3,16);//Integer.valueOf(byte3.substring(0,1))*16+Integer.valueOf(byte3.substring(1,2));

		String byte4=byte3and4.substring(2,4);
		outBuffer[4]=(byte)Integer.parseInt(byte4, 16);
		int intByte4=Integer.valueOf(byte4,16);//Integer.valueOf(byte4.substring(0,1))*16+Integer.valueOf(byte4.substring(1,2));

		int intbyte7=48+intByte2+intByte3+intByte4;
		String byte7=Long.toHexString(intbyte7);
		byte7=byte7.substring(byte7.length()-2,byte7.length());
		outBuffer[7]=(byte)Integer.parseInt(byte7, 16);

		mSendDataList.add(outBuffer);
	}

	public void SynchronizeTimeBuffer() { 
		Calendar cal = Calendar.getInstance();

		int intByte2=cal.get(Calendar.HOUR_OF_DAY); //时
		String 	byte2=Long.toHexString(intByte2);
		byte[] outBuffer=BluetoothBuffer.SynchronizeTimeBuffer;
		outBuffer[2]=(byte)Integer.parseInt(byte2, 16);

		int intByte3=cal.get(Calendar.MINUTE);  //分
		String byte3=Long.toHexString(intByte3);
		outBuffer[3]=(byte)Integer.parseInt(byte3, 16);

		int intByte4=cal.get(Calendar.SECOND); //秒
		String byte4=Long.toHexString(intByte4);
		outBuffer[4]=(byte)Integer.parseInt(byte4, 16);

		int intbyte7=49+intByte2+intByte3+intByte4;
		String byte7=Long.toHexString(intbyte7);
		byte7=byte7.substring(byte7.length()-2,byte7.length());
		outBuffer[7]=(byte)Integer.parseInt(byte7, 16);

		mSendDataList.add(outBuffer);
	}
	
	public void SendAlarmBuffer(final boolean sun,final boolean mon,final boolean tue,final boolean wed,final boolean thu,final boolean fri,final boolean sat,final int hour,final int minutes,final int alarmId,final int music){
		byte[] outBuffer=new byte[]{(byte)0xA5,0x32,0x00,0x00,0x00,0x00,0x00,0x00};
		int byte5 = 0x00;
		if (sun) {
			byte5 = byte5 | (1 << 0);
		} else {
			byte5 = byte5 | (0 << 0);
		}
		//set bit 1 to 0|1
		if (mon) {
			byte5 = byte5 | (1 << 1);
		} else {
			byte5 = byte5 | (0 << 1);
		}
		if (tue) {
			byte5 = byte5 | (1 << 2);
		} else {
			byte5 = byte5 | (0 << 2);
		}
		//set bit 3 to 0|1
		if (wed) {
			byte5 = byte5 | (1 << 3);
		} else {
			byte5 = byte5 | (0 << 3);
		}
		//set bit 4 to 0|1
		if (thu) {
			byte5 = byte5 | (1 << 4);
		} else {
			byte5 = byte5 | (0 << 4);
		}
		//set bit 5 to 0|1
		if (fri) {
			byte5 = byte5 | (1 << 5);
		} else {
			byte5 = byte5 | (0 << 5);
		}
		if (sat) {
			byte5 = byte5 | (1 << 6);
		} else {
			byte5 = byte5 | (0 << 6);
		}

		outBuffer[2]=(byte)Integer.parseInt(Long.toHexString(hour), 16);
		outBuffer[3]=(byte)Integer.parseInt(Long.toHexString(minutes), 16);
		outBuffer[4]=(byte)Integer.parseInt(Long.toHexString(music), 16);
		outBuffer[5]=(byte) byte5;
		outBuffer[6]=(byte) Integer.parseInt(Long.toHexString(alarmId), 16);
		outBuffer[7]=(byte) (outBuffer[0]+outBuffer[1]+outBuffer[2]+outBuffer[3]+outBuffer[4]+outBuffer[5]+outBuffer[6]);

		mSendDataList.add(outBuffer);
	}

	/***************************************    自定义搜索连接结束代码          *******************************************/
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
	
	private final IBinder binder = new LocalBinder();

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


	
	public interface OnDisplayDATAService{
		void OnInitService();
	}
	
	OnInitService mOnInitService=null;
	public void setOnInitService(OnInitService e){
		mOnInitService=e;
	}

	public interface OnDisplayDATA{
		void OnDATA(String data);
	}
	
	OnDisplayDATA mOnDisplayDATA=null;
	public void setOnDisplayDATA(OnDisplayDATA e){
		mOnDisplayDATA=e;
	}
	
	public interface OnMeasureDATA{
		void OnMeasureDATA(BodyMeasure bodyMeasure);
	}
	
	OnMeasureDATA mOnMeasureDATA=null;
	public void setOnMeasureDATA(OnMeasureDATA e){
		mOnMeasureDATA=e;
	}
}
