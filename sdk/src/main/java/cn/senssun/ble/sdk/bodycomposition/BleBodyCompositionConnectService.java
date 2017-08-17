package cn.senssun.ble.sdk.bodycomposition;


import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import cn.senssun.ble.sdk.BluetoothBuffer;
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
public class BleBodyCompositionConnectService extends Service {
	public final static String ACTION_GATT_DISCONNECTED = "cn.senssun.ble.sdk.BleBodyCompositionConnectService.ACTION_GATT_DISCONNECTED";
	public final static String ACTION_GATT_CONNECTED ="cn.senssun.ble.sdk.BleBodyCompositionConnectService.ACTION_GATT_CONNECTED";
	public final static String ACTION_GATT_SERVICES_DISCOVERED = "cn.senssun.ble.sdk.BleBodyCompositionConnectService.ACTION_GATT_SERVICES_DISCOVERED";
	public final static String ACTION_DATA_NOTIFY = "cn.senssun.ble.sdk.BleBodyCompositionConnectService.ACTION_DATA_NOTIFY";
	public final static String ACTION_DATA_READ = "cn.senssun.ble.sdk.BleBodyCompositionConnectService.ACTION_DATA_READ";
	public final static String ACTION_DATA_WRITE = "cn.senssun.ble.sdk.BleBodyCompositionConnectService.ACTION_DATA_WRITE";
	
	static final String TAG = "BleBodyCompositionConnectService";
	static final String TAG1 = "DIS";

	private Handler mFatBodyHandler;

	// BLE
	private boolean mConnect=false;

	private BluetoothManager mBluetoothManager = null;
	private BluetoothAdapter mBtAdapter = null; //蓝牙适配器
	private BluetoothGatt mBluetoothGatt = null;
	private static BleBodyCompositionConnectService mThis = null;
	private volatile boolean mBusy = false; // Write/read pending response
	private BluetoothGattCharacteristic mWriteCharacteristic; //写出GATT,char
	public boolean mBodyTest =false;

	/**
	 * GATT client callbacks
	 */
	private BluetoothGattCallback mGattCallbacks = new BluetoothGattCallback() {

		@Override
		public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
			if (mBluetoothGatt == null) {
				LOG.logE(TAG, "mBluetoothGatt not created!");
				return;
			}

			BluetoothDevice device = gatt.getDevice();
			String address = device.getAddress();
			LOG.logD(TAG, "onConnectionStateChange (" + address + ") " + newState + " status: " + status);
			try {
				switch (newState) {
				case BluetoothProfile.STATE_CONNECTED:
					if(status==133){
						disconnect();
						close();
						broadcastUpdate(ACTION_GATT_DISCONNECTED, address, status);
					}else{
						broadcastUpdate(ACTION_GATT_CONNECTED, address, status);
						mBluetoothGatt.discoverServices();
					}
					break;
				case BluetoothProfile.STATE_DISCONNECTED:
					disconnect();
					close();
					broadcastUpdate(ACTION_GATT_DISCONNECTED, address, status);
					break;
				default:
					LOG.logE(TAG, "New state not processed: " + newState);
					break;
				}
			} catch (NullPointerException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void onServicesDiscovered(BluetoothGatt gatt, int status) {
			BluetoothDevice device = gatt.getDevice();
			broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED,device.getAddress(),status);
		}

		@Override
		public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
			broadcastUpdate(ACTION_DATA_NOTIFY,characteristic,BluetoothGatt.GATT_SUCCESS);
		}

		@Override
		public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
			broadcastUpdate(ACTION_DATA_READ,characteristic,status);
		}

		@Override
		public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
			broadcastUpdate(ACTION_DATA_WRITE,characteristic,status);
		}

		@Override
		public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
			mBusy = false;
			LOG.logI(TAG, "onDescriptorRead");
		}

		@Override
		public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
			mBusy = false;
			LOG.logI(TAG, "onDescriptorWrite");
		}
	};

	private void broadcastUpdate(final String action, final String address, final int status) {
		final Intent intent = new Intent(action);
		intent.putExtra(BluetoothBuffer.EXTRA_ADDRESS, address);
		intent.putExtra(BluetoothBuffer.EXTRA_STATUS, status);
		sendBroadcast(intent);
		mBusy = false;
	}

	private void broadcastUpdate(final String action, final BluetoothGattCharacteristic characteristic, final int status) {
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
		mBusy = false;
	}

	private void broadcastDisplay(final String action,final String data) {
		final Intent intent = new Intent(action);
		intent.putExtra(BluetoothBuffer.Display_DATA, data);
		sendBroadcast(intent);
		mBusy = false;
	}

	private boolean checkGatt() {
		if (mBtAdapter == null) {
			LOG.logW(TAG, "BluetoothAdapter not initialized");
			return false;
		}
		if (mBluetoothGatt == null) {
			//			LOG.logW(TAG, "BluetoothGatt not initialized");
			return false;
		}

		if (mBusy) {
			LOG.logW(TAG, "LeService busy");
			return false;
		}
		return true;
	}

	/**
	 * Manage the BLE service
	 */
	public class LocalBinder extends Binder {
		public BleBodyCompositionConnectService getService() {
			return BleBodyCompositionConnectService.this;
		}
	}
	@Override
	public IBinder onBind(Intent intent) {
		mBluetoothManager= (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
		mBtAdapter = mBluetoothManager.getAdapter();
		registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
		mFatBodyHandler=new Handler();

		return binder;
	}


	private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {    //广播接收
		@Override
		public void onReceive(Context context, Intent intent) {
			final String action = intent.getAction();
			if (ACTION_GATT_CONNECTED.equals(action)) {
				mConnect=true;
				if(mOnDisplayDATA!=null){
					mOnDisplayDATA.OnDisplayDATA("result-status-connect");
				}
			}else if (ACTION_GATT_DISCONNECTED.equals(action)) {
				mConnect=false;
				if(mOnDisplayDATA!=null){
					mOnDisplayDATA.OnDisplayDATA("result-status-disconnect");
				}
			} else if (ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
				displayGattServices(getSupportedGattServices());

			}else if(ACTION_DATA_NOTIFY.equals(action)){

				String data=intent.getStringExtra(BluetoothBuffer.EXTRA_DATA);

				if(data==null)return;

				LOG.logD(TAG1, data);
				String[] strdata=data.split("-");
				
				if (strdata[2].equals("10")){
					if(!mBodyTest){
						if(mOnDisplayDATA!=null){
							mOnDisplayDATA.OnDisplayDATA("result-status-bodyTest");
						}	
					}
					mBodyTest=true;
				}
				
				if (strdata[0].equals("FF")&&strdata[1].equals("A5")  &&("10-1A").contains(strdata[6])) {
					String tmpNum = strdata[2] + strdata[3];
					if("10".equals(strdata[6])){
						if(mOnMeasureDATA!=null){
							mOnMeasureDATA.OnDATA("result-data-1-0-"+new BigDecimal(Integer.valueOf(tmpNum, 16)/10f).setScale(1, BigDecimal.ROUND_HALF_UP));
						}
					}else if("1A".equals(strdata[6])){
						if(mOnMeasureDATA!=null){
							mOnMeasureDATA.OnDATA("result-data-1-1-"+new BigDecimal(Integer.valueOf(tmpNum, 16)/10f).setScale(1, BigDecimal.ROUND_HALF_UP));
						}
					}
					
					 tmpNum = strdata[4] + strdata[5];
					 if("10".equals(strdata[6])){
						 if(mOnMeasureDATA!=null){
								mOnMeasureDATA.OnDATA("result-data-2-0-"+new BigDecimal(Integer.valueOf(tmpNum, 16)/10f).setScale(1, BigDecimal.ROUND_HALF_UP));
							}
						}else if("1A".equals(strdata[6])){
							if(mOnMeasureDATA!=null){
								mOnMeasureDATA.OnDATA("result-data-2-1-"+new BigDecimal(Integer.valueOf(tmpNum, 16)/10f).setScale(1, BigDecimal.ROUND_HALF_UP));
							}
						}
				}

				//脂肪 水分
				if (strdata[6].equals("B0")) {//
					String tmpNum = strdata[2] + strdata[3];
					if(mOnMeasureDATA!=null){
						mOnMeasureDATA.OnDATA("result-data-3-"+new BigDecimal(Integer.valueOf(tmpNum)/10f).setScale(1, BigDecimal.ROUND_HALF_UP));
					}
				}
				
				//内脏脂肪 细胞总水
				if (strdata[6].equals("B1")) {//
					String tmpNum = strdata[2] + strdata[3];
					if(mOnMeasureDATA!=null){
						mOnMeasureDATA.OnDATA("result-data-4-"+new BigDecimal(Integer.valueOf(tmpNum)/10f).setScale(1, BigDecimal.ROUND_HALF_UP));
					}

					tmpNum = strdata[4] + strdata[5];
					if(mOnMeasureDATA!=null){
						mOnMeasureDATA.OnDATA("result-data-5-"+new BigDecimal(Integer.valueOf(tmpNum)/10f).setScale(1, BigDecimal.ROUND_HALF_UP));
					}
				}

				
				if (strdata[6].equals("B2")) {//
					String tmpNum = strdata[2] + strdata[3];
					if(mOnMeasureDATA!=null){
						mOnMeasureDATA.OnDATA("result-data-6-"+new BigDecimal(Integer.valueOf(tmpNum)/10f).setScale(1, BigDecimal.ROUND_HALF_UP));
					}

					tmpNum = strdata[4] + strdata[5];
					if(mOnMeasureDATA!=null){
						mOnMeasureDATA.OnDATA("result-data-7-"+new BigDecimal(Integer.valueOf(tmpNum)/10f).setScale(1, BigDecimal.ROUND_HALF_UP));
					}
				}

				if (strdata[6].equals("B3")) {
					String tmpNum = strdata[3];
					if(mOnMeasureDATA!=null){
						mOnMeasureDATA.OnDATA("result-data-8-"+Integer.valueOf(tmpNum));
					}

					tmpNum = strdata[5];
					if(mOnMeasureDATA!=null){
						mOnMeasureDATA.OnDATA("result-data-9-"+Integer.valueOf(tmpNum));
					}
				}
				
				if (strdata[6].equals("C0")) {//肌肉百分比 肌肉重 骨骼重
					String tmpNum = strdata[2] + strdata[3];
					if(mOnMeasureDATA!=null){
						mOnMeasureDATA.OnDATA("result-data-10-"+new BigDecimal(Integer.valueOf(tmpNum)/10f).setScale(1, BigDecimal.ROUND_HALF_UP));
					}

					tmpNum = strdata[5] + strdata[4];
					if(mOnMeasureDATA!=null){
						mOnMeasureDATA.OnDATA("result-data-11-"+new BigDecimal(Integer.valueOf(tmpNum)/10f).setScale(1, BigDecimal.ROUND_HALF_UP));
					}
				}

				//基础代谢率
				if (strdata[6].equals("D0")) {
					String tmpNum = strdata[2] + strdata[3];
					if(mOnMeasureDATA!=null){
						mOnMeasureDATA.OnDATA("result-data-12-"+Integer.valueOf(tmpNum));
					}

					tmpNum = strdata[4] + strdata[5];
					if(mOnMeasureDATA!=null){
						mOnMeasureDATA.OnDATA("result-data-13-"+new BigDecimal(Integer.valueOf(tmpNum)/10f).setScale(1, BigDecimal.ROUND_HALF_UP));
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
	public void BodyCompositionTestBuffer(final int height,final int age,final int sex) { 
				mFatBodyHandler.postDelayed(new Runnable() {
					@Override
					public void run() {
						if(!mBodyTest){
							BodyCompositionTestBuffer(height, age, sex); //不断发送
						}
					}
				}, 100);
		if(mWriteCharacteristic!=null){

			byte[] outBuffer= BluetoothBuffer.BodyCompositionSendBodyBuffer;

			int serialNum=1;

			int intByte2 = (sex == 0 ? 0 : 8) * 16 + serialNum;
			String byte2 = Long.toHexString(intByte2);
			outBuffer[2] = (byte) Integer.parseInt(byte2, 16);

			int intByte3 = age;
			String byte3 = Long.toHexString(intByte3).toUpperCase();
			outBuffer[3] = (byte) Integer.parseInt(byte3, 16);

			int intByte4 = height;
			String byte4 = Long.toHexString(intByte4);
			outBuffer[4] = (byte) Integer.parseInt(byte4, 16);

			String HeightTmp = String.valueOf(new BigDecimal(intByte4 / 0.254).setScale(0, BigDecimal.ROUND_HALF_UP));
			String Height = HeightTmp.substring(0, HeightTmp.length() - 1) + ((Integer.valueOf(HeightTmp.substring(HeightTmp.length() - 1)) >= 5 && Integer.valueOf(HeightTmp.substring(HeightTmp.length() - 1)) <= 9) ? "5" : "0");

			int intbyte5and6 = Integer.valueOf(Height);
			String byte5and6 = Long.toHexString(intbyte5and6);
			byte5and6 = byte5and6.length() == 2 ? "00" + byte5and6 :
				byte5and6.length() == 3 ? "0" + byte5and6 : byte5and6;

			String byte5 = byte5and6.substring(0, 2);
			outBuffer[5] = (byte) Integer.parseInt(byte5, 16);
			int intByte5 = Integer.valueOf(byte5, 16);

			String byte6 = byte5and6.substring(2, 4);
			outBuffer[6] = (byte) Integer.parseInt(byte6, 16);
			int intByte6 = Integer.valueOf(byte6, 16);
			////////////////////////////////////////////////////////

			int intbyte7 = 16 + intByte2 + intByte3 + intByte4 + intByte5 + intByte6;
			String byte7 = Long.toHexString(intbyte7).toUpperCase();
			byte7 = byte7.substring(byte7.length() - 2, byte7.length());
			outBuffer[7] = (byte) Integer.parseInt(byte7, 16);

			mWriteCharacteristic.setValue(outBuffer);
			writeCharacteristic(mWriteCharacteristic);
		}
	}
	/***************************************    自定义搜索连接结束代码          *******************************************/
	@Override
	public boolean onUnbind(Intent intent) {
		unregisterReceiver(mGattUpdateReceiver);
		close();
		return super.onUnbind(intent);
	}

	private final IBinder binder = new LocalBinder();

	/**
	 * Initializes a reference to the local Bluetooth adapter.
	 * 
	 * @return Return true if the initialization is successful.
	 */
	public boolean initialize() {

		LOG.logD(TAG, "initialize");
		// For API level 18 and above, get a reference to BluetoothAdapter through
		// BluetoothManager.
		mThis = this;
		if (mBluetoothManager == null) {
			mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
			if (mBluetoothManager == null) {
				LOG.logE(TAG, "Unable to initialize BluetoothManager.");
				return false;
			}
		}

		mBtAdapter = mBluetoothManager.getAdapter();
		if (mBtAdapter == null) {
			LOG.logE(TAG, "Unable to obtain a BluetoothAdapter.");
			return false;
		}
		return true;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		LOG.logI(TAG, "Received start id " + startId + ": " + intent);
		// We want this service to continue running until it is explicitly
		// stopped, so return sticky.
		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		LOG.logD(TAG, "onDestroy() called");
		if (mBluetoothGatt != null) {
			mBluetoothGatt.close();
			mBluetoothGatt = null;
		}
	}

	//
	// GATT API
	//
	/**
	 * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported asynchronously through the
	 * {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)} callback.
	 * 
	 * @param characteristic
	 *          The characteristic to read from.
	 */
	public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
		if (!checkGatt())
			return;
		mBusy = true;
		mBluetoothGatt.readCharacteristic(characteristic);
	}

	public boolean writeCharacteristic(BluetoothGattCharacteristic characteristic) {
		if (!checkGatt())
			return false;

		mBusy = true;
		return mBluetoothGatt.writeCharacteristic(characteristic);
	}

	/**
	 * Retrieves a list of supported GATT services on the connected device. This should be invoked only after {@code BluetoothGatt#discoverServices()} completes
	 * successfully.
	 * 
	 * @return A {@code List} of supported services.
	 */
	public List<BluetoothGattService> getSupportedGattServices() {
		if (mBluetoothGatt == null)
			return null;

		return mBluetoothGatt.getServices();
	}

	/**
	 * Enables or disables notification on a give characteristic.
	 * 
	 * @param characteristic
	 *          Characteristic to act on.
	 * @param enabled
	 *          If true, enable notification. False otherwise.
	 */
	public boolean setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enable) {
		if (!checkGatt())
			return false;

		if (!mBluetoothGatt.setCharacteristicNotification(characteristic, enable)) {
			LOG.logW(TAG, "setCharacteristicNotification failed");
			return false;
		}

		BluetoothGattDescriptor clientConfig = characteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
		if (clientConfig == null)
			return false;

		if (enable) {
			LOG.logI(TAG, "enable notification");
			clientConfig.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
		} else {
			LOG.logI(TAG, "disable notification");
			clientConfig.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
		}

		mBusy = true;
		return mBluetoothGatt.writeDescriptor(clientConfig);
	}

	public boolean isNotificationEnabled(BluetoothGattCharacteristic characteristic) {
		if (!checkGatt())
			return false;

		BluetoothGattDescriptor clientConfig = characteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
		if (clientConfig == null)
			return false;

		return clientConfig.getValue() == BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE;
	}

	/**
	 * Connects to the GATT server hosted on the Bluetooth LE device.
	 * 
	 * @param address
	 *          The device address of the destination device.
	 * 
	 * @return Return true if the connection is initiated successfully. The connection result is reported asynchronously through the
	 *         {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)} callback.
	 */
	public boolean connect(final String address) {
		if (mBtAdapter == null || address == null) {
			LOG.logW(TAG, "BluetoothAdapter not initialized or unspecified address.");
			return false;
		}

		final BluetoothDevice device = mBtAdapter.getRemoteDevice(address);
		int connectionState = mBluetoothManager.getConnectionState(device, BluetoothProfile.GATT);

		if (connectionState == BluetoothProfile.STATE_DISCONNECTED) {

			if (mBluetoothGatt!=null){
				disconnect();
				close();
			}

			if (device == null) {
				LOG.logW(TAG, "Device not found.  Unable to connect.");
				return false;
			}
			// We want to directly connect to the device, so we are setting the
			// autoConnect parameter to false.
			LOG.logD(TAG, "Create a new GATT connection.");
			mBluetoothGatt = device.connectGatt(this, false, mGattCallbacks);

		} else {
			LOG.logW(TAG, "Attempt to connect in state: " + connectionState);
			return false;
		}
		return true;
	}

	public void WriteChar(byte[] byBuffer ){
		mWriteCharacteristic.setValue(byBuffer);
		writeCharacteristic(mWriteCharacteristic);
	}

	/**
	 * Disconnects an existing connection or cancel a pending connection. The disconnection result is reported asynchronously through the
	 * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)} callback.
	 */
	public void disconnect() {
		if (mBtAdapter == null || mBluetoothGatt == null) {
			LOG.logW(TAG, "BluetoothAdapter not initialized");
			return;
		}
		mBluetoothGatt.disconnect();
	}

	/**
	 * After using a given BLE device, the app must call this method to ensure resources are released properly.
	 */
	public void close() {
		if (mBluetoothGatt != null) {
			LOG.logI(TAG, "close");
			mBluetoothGatt.close();
			mBluetoothGatt = null;
		}
	}

	//
	// Utility functions
	//
	public static BluetoothGatt getBtGatt() {
		return mThis.mBluetoothGatt;
	}

	public static BluetoothManager getBtManager() {
		return mThis.mBluetoothManager;
	}

	public static BleBodyCompositionConnectService getInstance() {
		return mThis;
	}

	public boolean ismConnect() {
		return mConnect;
	}



	public interface OnDisplayDATA{
		void OnDisplayDATA(String data);
	}

	OnDisplayDATA mOnDisplayDATA=null;
	public void setOnDisplayDATA(OnDisplayDATA e){
		mOnDisplayDATA=e;
	}

	public interface OnMeasureDATA{
		void OnDATA(String data);
	}

	OnMeasureDATA mOnMeasureDATA=null;
	public void setOnMeasureDATA(OnMeasureDATA e){
		mOnMeasureDATA=e;
	}
}
