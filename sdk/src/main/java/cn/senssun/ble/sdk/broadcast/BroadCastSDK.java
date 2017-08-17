package cn.senssun.ble.sdk.broadcast;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import cn.senssun.ble.sdk.entity.BodyMeasure;
import cn.senssun.ble.sdk.fat.BleConnectService;

public class BroadCastSDK {
	private MyHandler handler;
	private  Handler mHandler;
	private  BluetoothAdapter mBtAdapter = null; //蓝牙适配器
	private  boolean mScanning=true;//判断搜索状态
	private  BluetoothManager mBluetoothManager = null;
	public 	 BleConnectService mBleConnectService;
	private String mDeviceAddress;

	/***************************************自定义搜索连接代码********************************************/

	public  void initSDK(Context mContext,String mDeviceAddress) {
		this.mDeviceAddress=mDeviceAddress;
		mBluetoothManager= (BluetoothManager)mContext.getSystemService(Context.BLUETOOTH_SERVICE);
		mBtAdapter = mBluetoothManager.getAdapter();
		mHandler = new Handler();
		handler=new MyHandler();
	}

	public  void ScanLeStartDevice(final int SCAN_PERIOD) { //搜索BLE设备
		// Stops scanning after a pre-defined scan period. 一个预定义的扫描周期后停止扫描。
		mHandler.postDelayed(new Runnable() {
			@Override
			public void run() {
				if(mScanning){
					mBtAdapter.stopLeScan(mLeScanCallback);
					ScanLeStartDevice(SCAN_PERIOD);
				}
			}
		}, SCAN_PERIOD);
		mScanning = true;
		mBtAdapter.startLeScan(mLeScanCallback);
	}

	public  void scanLeStopDevice() { //搜索BLE设备
		mScanning = false;
		mBtAdapter.stopLeScan(mLeScanCallback);
	}
	
	// Device scan callback. 设备扫描回调。
	private  BluetoothAdapter.LeScanCallback mLeScanCallback =
			new BluetoothAdapter.LeScanCallback() {
		@Override
		public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					if (device.getName()==null)return;
					if(device.getAddress().trim().equals(mDeviceAddress)){
						Message m = new Message();
						Bundle b=new Bundle();
						b.putByteArray("scanRecord", scanRecord);
						m.setData(b);
						handler.sendMessage(m);
					}
				}
			}).start();
		}
	};

	public interface OnMeasure{
		void OnData(BodyMeasure bodyMeasure);
	}

	OnMeasure mOnMeasure=null;
	public void setOnMeasure(OnMeasure e){
		mOnMeasure=e;
	}

	
	class MyHandler extends  Handler{
		@Override
		public void handleMessage(Message msg) {
			Bundle b=msg.getData();
			 byte[] scanRecord=	b.getByteArray("scanRecord");
			String kgNum=String.format("%02X ", scanRecord[13]).trim()+String.format("%02X ", scanRecord[14]).trim();
			String lbNum=String.format("%02X ", scanRecord[15]).trim()+String.format("%02X ", scanRecord[16]).trim();
			
			int WeightNum=Integer.valueOf(kgNum,16);
			int lbWeightNum=Integer.valueOf(lbNum,16);

			BodyMeasure bodyMeasure=new BodyMeasure();
			if (String.format("%02X ", scanRecord[17]).trim().equals("A0")){
				bodyMeasure.setIfStable(false);
			}else{
				bodyMeasure.setIfStable(true);
			}
			bodyMeasure.setWeightKg(WeightNum);
			bodyMeasure.setWeightLb(lbWeightNum);

			if(mOnMeasure!=null){
				mOnMeasure.OnData(bodyMeasure);
			}
		}
	}
	
}
