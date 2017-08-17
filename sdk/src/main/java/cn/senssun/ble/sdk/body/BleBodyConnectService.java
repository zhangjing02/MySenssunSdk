package cn.senssun.ble.sdk.body;


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
import android.os.IBinder;

import java.util.List;
import java.util.UUID;

import cn.senssun.ble.sdk.BluetoothBuffer;
import cn.senssun.ble.sdk.body.alarm.BleBodyAlarmSDK.OnInitService;
import cn.senssun.ble.sdk.entity.BodyMeasure;
import cn.senssun.ble.sdk.util.LOG;

/**
 * Service for managing connection and data communication with a GATT server hosted on a given Bluetooth LE device.
 */
public class BleBodyConnectService extends Service {
	public final static String ACTION_GATT_DISCONNECTED = "cn.senssun.ble.sdk.BleBodyConnectService.ACTION_GATT_DISCONNECTED";
	public final static String ACTION_GATT_CONNECTED ="cn.senssun.ble.sdk.BleBodyConnectService.ACTION_GATT_CONNECTED";
	public final static String ACTION_GATT_SERVICES_DISCOVERED = "cn.senssun.ble.sdk.BleBodyConnectService.ACTION_GATT_SERVICES_DISCOVERED";
	public final static String ACTION_DATA_READ = "cn.senssun.ble.sdk.BleBodyConnectService.ACTION_DATA_READ";
	public final static String ACTION_DATA_NOTIFY = "cn.senssun.ble.sdk.BleBodyConnectService.ACTION_DATA_NOTIFY";
	
	private final static String TAG = "BleBodyConnectService";

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



	//	private boolean mSynchronizeDate=false;
	//	private boolean mSynchronizeTime=false;
	//	public boolean mDataCommun=false;
	//	public boolean mBodyTest=false;
	//	public boolean mSetAlarm=false;
	public boolean misSend=false;


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
					intentAction =ACTION_GATT_CONNECTED;
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
			BleBodyConnectService getService() {
				return BleBodyConnectService.this;
			}
		}
		@Override
		public IBinder onBind(Intent intent) {
			registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());

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
					misSend=false;
				} else if (ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
					displayGattServices(getSupportedGattServices());

				}else if(ACTION_DATA_NOTIFY.equals(action)){
				misSend=true;
				String data=intent.getStringExtra(BluetoothBuffer.EXTRA_DATA);
				
				if(data==null){return;}
				
				LOG.logD(TAG, data);
				String[] strdata=data.split("-");
				//发送命令回复
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
				}
			}
			if (gattService.getUuid().toString().equals("0000ffb0-0000-1000-8000-00805f9b34fb")) {
				List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
				for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
					if (gattCharacteristic.getUuid().toString().trim().equals("0000ffb2-0000-1000-8000-00805f9b34fb")){
						setCharacteristicNotification(gattCharacteristic, true);
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



	/***************************************    自定义搜索连接结束代码          *******************************************/
	@Override
	public boolean onUnbind(Intent intent) {
		// After using a given device, you should make sure that BluetoothGatt.close() is called
		// such that resources are cleaned up properly.  In this particular example, close() is
		// invoked when the UI is disconnected from the Service.
		close();
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
