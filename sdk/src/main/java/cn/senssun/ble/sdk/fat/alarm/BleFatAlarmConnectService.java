package cn.senssun.ble.sdk.fat.alarm;



import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import cn.senssun.ble.sdk.BluetoothBuffer;
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
import android.util.Log;

/**
 * Service for managing connection and data communication with a GATT server hosted on a given Bluetooth LE device.
 */
public class BleFatAlarmConnectService extends Service {
	public final static String ACTION_GATT_DISCONNECTED = "cn.senssun.ble.sdk.BleFatAlarmConnectService.ACTION_GATT_DISCONNECTED";
	public final static String ACTION_GATT_CONNECTED ="cn.senssun.ble.sdk.BleFatAlarmConnectService.ACTION_GATT_CONNECTED";
	public final static String ACTION_GATT_SERVICES_DISCOVERED = "cn.senssun.ble.sdk.BleFatAlarmConnectService.ACTION_GATT_SERVICES_DISCOVERED";
	public final static String ACTION_DATA_NOTIFY = "cn.senssun.ble.sdk.BleFatAlarmConnectService.ACTION_DATA_NOTIFY";
	public final static String ACTION_DATA_READ = "cn.senssun.ble.sdk.BleFatAlarmConnectService.ACTION_DATA_READ";
	
	private final static String TAG = "BleFatAlarmConnectService";

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
	private HashMap<Integer, FatDataCommun> SendHisOBjectList=new HashMap<Integer, FatDataCommun>();
	private BluetoothGattCharacteristic mWriteCharacteristic; //写出GATT,char

	private int ReceiveFat;
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
				connectIng=false;
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
		BleFatAlarmConnectService getService() {
			return BleFatAlarmConnectService.this;
		}
	}
	@Override
	public IBinder onBind(Intent intent) {
		registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());

		mSendDataHandler=new Handler();
		mSendDataHandler2=new Handler();
		mSendData();
		mSendData2();
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
				mSendDataList2.clear();
				misSend=false;
				mWriteCharacteristic=null;
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
				if(strdata[1].equals("A5")){
					
					if (strdata[6].equals("BE")){
						if(mOnDisplayDATA!=null){
							mOnDisplayDATA.OnDATA("result-status-bodyTestError2");
						}
					}
					
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
//								mSendDataList.remove(outBuffer);
								
								if(mOnDisplayDATA!=null){
									mOnDisplayDATA.OnDATA("result-status-time");
								}
							}
							
							List<byte[]> delbuffer=new ArrayList<byte[]>();
							for(byte[] buffer:mSendDataList){
								if (String.format("%02X ", buffer[1]).trim().equals("31")){
									delbuffer.add(buffer);
								}
							}
							for(byte[] buffer:delbuffer){
								mSendDataList.remove(buffer);
							}
						}
					}

					if (strdata[2].equals("32")){
						if(mSendDataList2.size()!=0){
							byte[] outBuffer=mSendDataList2.get(0);

							if (String.format("%02X ", outBuffer[1]).trim().equals("32")&&String.format("%02X ", outBuffer[6]).trim().equals(strdata[3])){
//								mSendDataList2.remove(outBuffer);
								
								if(mOnDisplayDATA!=null){
									mOnDisplayDATA.OnDATA("result-status-setAlarm");
								}
							}
							
							List<byte[]> delbuffer=new ArrayList<byte[]>();
							for(byte[] buffer:mSendDataList2){
								if (String.format("%02X ", buffer[1]).trim().equals("32")&&String.format("%02X ", buffer[6]).trim().equals(strdata[3])){
									delbuffer.add(buffer);
								}
							}
							for(byte[] buffer:delbuffer){
									mSendDataList2.remove(buffer);
							}
						}
						
					}

					if (strdata[2].equals("21")){
						if(mSendDataList2.size()!=0){
							byte[] outBuffer=mSendDataList2.get(0);

							if (String.format("%02X ", outBuffer[1]).trim().equals("21")){
//								mSendDataList2.remove(outBuffer);
								
								if(mOnDisplayDATA!=null){
									mOnDisplayDATA.OnDATA("result-status-dataCommun");
								}
							}
							
							List<byte[]> delbuffer=new ArrayList<byte[]>();
							for(byte[] buffer:mSendDataList2){
								if (String.format("%02X ", buffer[1]).trim().equals("21")){
									delbuffer.add(buffer);
								}
							}
							for(byte[] buffer:delbuffer){
									mSendDataList2.remove(buffer);
							}
						}
					}
					
					if (strdata[2].equals("5A")){
						if(mSendDataList2.size()!=0){
							byte[] outBuffer=mSendDataList2.get(0);

							if (String.format("%02X ", outBuffer[1]).trim().equals("5A")){
//								mSendDataList2.remove(outBuffer);
								
								if(mOnDisplayDATA!=null){
									mOnDisplayDATA.OnDATA("result-status-clearData");
								}
							}
							
							List<byte[]> delbuffer=new ArrayList<byte[]>();
							for(byte[] buffer:mSendDataList2){
								if (String.format("%02X ", buffer[1]).trim().equals("5A")){
									delbuffer.add(buffer);
								}
							}
							for(byte[] buffer:delbuffer){
									mSendDataList2.remove(buffer);
							}
						}
					}
					
					if (strdata[2].equals("5A")){
						if(mSendDataList2.size()!=0){
							byte[] outBuffer=mSendDataList2.get(0);

							if (String.format("%02X ", outBuffer[1]).trim().equals("5A")){
//								mSendDataList2.remove(outBuffer);
								
								if(mOnDisplayDATA!=null){
									mOnDisplayDATA.OnDATA("result-status-clearData");
								}
							}
							
							List<byte[]> delbuffer=new ArrayList<byte[]>();
							for(byte[] buffer:mSendDataList2){
								if (String.format("%02X ", buffer[1]).trim().equals("5A")){
									delbuffer.add(buffer);
								}
							}
							for(byte[] buffer:delbuffer){
									mSendDataList2.remove(buffer);
							}
						}
					}

					if (strdata[6].equals("F0")){
						List<Map.Entry<Integer, FatDataCommun>> infoIds =
							    new ArrayList<Map.Entry<Integer, FatDataCommun>>(SendHisOBjectList.entrySet());
						
						Collections.sort(infoIds, new Comparator<Map.Entry<Integer, FatDataCommun>>() {   
						    @Override
							public int compare(Map.Entry<Integer, FatDataCommun> o1, Map.Entry<Integer, FatDataCommun> o2) {      
						    	return o1.getKey().compareTo(o2.getKey());
						    }
						}); 
						
						for (int i = 0; i < infoIds.size(); i++) {
							FatDataCommun fatDataCommun = infoIds.get(i).getValue();
							fatDataCommun.setSubcutaneousFat(fatDataCommun.getFat()-fatDataCommun.getVisceralFat());
							if(fatDataCommun.getWeightKg()!=-1&&fatDataCommun.getYear()!=-1&&mOnCommunDATA!=null){
								mOnCommunDATA.OnDATA(fatDataCommun);
							}
						}
						SendHisOBjectList.clear();

						if(mOnDisplayDATA!=null){
							mOnDisplayDATA.OnDATA("result-status-dataCommunFinish");
						}
					}

					if (strdata[2].equals("10")){
						if(mSendDataList2.size()!=0){
							byte[] outBuffer=mSendDataList2.get(0);
							
							if (String.format("%02X ", outBuffer[1]).trim().equals("10")){
//								mSendDataList2.remove(outBuffer);
								if(mOnDisplayDATA!=null){
									mOnDisplayDATA.OnDATA("result-status-bodyTest");
								}	
							}
							List<byte[]> delbuffer=new ArrayList<byte[]>();
							for(byte[] buffer:mSendDataList2){
								if (String.format("%02X ", buffer[1]).trim().equals("10")){
									delbuffer.add(buffer);
								}
							}
							for(byte[] buffer:delbuffer){
									mSendDataList2.remove(buffer);
							}
							
						}
					}
				}
				//数据获取
				if(strdata[0].equals("FF")&&strdata[1].equals("A5")){
					if (("AA-A0").contains(strdata[6])) {
						String tmpNum = strdata[2] + strdata[3];
						if("A0".equals(strdata[6])){
							if(mOnMeasureDATA!=null){
								mOnMeasureDATA.OnDATA("result-data-1-0-"+new BigDecimal(Integer.valueOf(tmpNum, 16)/10f).setScale(1, BigDecimal.ROUND_HALF_UP));
							}
						}else if("AA".equals(strdata[6])){
							if(mOnMeasureDATA!=null){
								mOnMeasureDATA.OnDATA("result-data-1-1-"+new BigDecimal(Integer.valueOf(tmpNum, 16)/10f).setScale(1, BigDecimal.ROUND_HALF_UP));
							}
						}

						tmpNum = strdata[4] + strdata[5];
						if("A0".equals(strdata[6])){
							if(mOnMeasureDATA!=null){
								mOnMeasureDATA.OnDATA("result-data-2-0-"+new BigDecimal(Integer.valueOf(tmpNum, 16)/10f).setScale(1, BigDecimal.ROUND_HALF_UP));
							}
						}else if("AA".equals(strdata[6])){
							if(mOnMeasureDATA!=null){
								mOnMeasureDATA.OnDATA("result-data-2-1-"+new BigDecimal(Integer.valueOf(tmpNum, 16)/10f).setScale(1, BigDecimal.ROUND_HALF_UP));
							}
						}
					}

					//脂肪 水分
					if (strdata[6].equals("B0")) {
						String tmpNum = strdata[2] + strdata[3];
						if(Integer.valueOf(tmpNum, 16)==0) {
							if(mOnDisplayDATA!=null){
								mOnDisplayDATA.OnDATA("result-status-bodyTestError");
							}
							return;
						}
						ReceiveFat=Integer.valueOf(tmpNum, 16);
						if(mOnMeasureDATA!=null){
							mOnMeasureDATA.OnDATA("result-data-3-"+new BigDecimal(Integer.valueOf(tmpNum, 16)/10f).setScale(1, BigDecimal.ROUND_HALF_UP));
						}

						tmpNum = strdata[4] + strdata[5];
						if(mOnMeasureDATA!=null){
							mOnMeasureDATA.OnDATA("result-data-4-"+new BigDecimal(Integer.valueOf(tmpNum, 16)/10f).setScale(1, BigDecimal.ROUND_HALF_UP));
						}
					}

					//内脏脂肪和细胞总水重
					if (strdata[6].equals("B1")) {
						String tmpNum = strdata[2] + strdata[3];
						if(Integer.valueOf(tmpNum, 16)==0)return;
						if(mOnMeasureDATA!=null){
							mOnMeasureDATA.OnDATA("result-data-5-"+new BigDecimal(Integer.valueOf(tmpNum, 16)/10f).setScale(1, BigDecimal.ROUND_HALF_UP));
						}
						if(mOnMeasureDATA!=null){
							int subcutaneousFat=ReceiveFat-Integer.valueOf(tmpNum, 16);
							String dd=(subcutaneousFat==0?"0":new BigDecimal(subcutaneousFat/10f).setScale(1, BigDecimal.ROUND_HALF_UP).toString());
							mOnMeasureDATA.OnDATA("result-data-13-"+dd);
						}
					}
					//瘦体重和蛋白质数据
					if (strdata[6].equals("B2")) {
						String tmpNum = strdata[4] + strdata[5];
						if(Integer.valueOf(tmpNum, 16)==0)return;
						if(mOnMeasureDATA!=null){
							mOnMeasureDATA.OnDATA("result-data-6-"+new BigDecimal(Integer.valueOf(tmpNum, 16)/10f).setScale(1, BigDecimal.ROUND_HALF_UP));
						}
					}

					//身体年龄和健康得分
					if (strdata[6].equals("B3")) {
						String tmpNum = strdata[3];
						if(Integer.valueOf(tmpNum, 16)==0) return;
						if(mOnMeasureDATA!=null){
							mOnMeasureDATA.OnDATA("result-data-7-"+Integer.valueOf(tmpNum, 16));
						}

						tmpNum = strdata[5];
						if(Integer.valueOf(tmpNum, 16)==0) return;
						if(mOnMeasureDATA!=null){
							mOnMeasureDATA.OnDATA("result-data-8-"+Integer.valueOf(tmpNum, 16));
						}
					}

					//肌肉百分比 肌肉重 骨骼重
					if (strdata[6].equals("C0")) {//
						String tmpNum = strdata[2] + strdata[3];
						if(Integer.valueOf(tmpNum, 16)==0) return;
						if(mOnMeasureDATA!=null){
							mOnMeasureDATA.OnDATA("result-data-9-"+new BigDecimal(Integer.valueOf(tmpNum, 16)/10f).setScale(1, BigDecimal.ROUND_HALF_UP));
						}

						tmpNum = strdata[5] + strdata[4];
						if(Integer.valueOf(tmpNum, 16)==0) return;
						if(mOnMeasureDATA!=null){
							mOnMeasureDATA.OnDATA("result-data-10-"+new BigDecimal(Integer.valueOf(tmpNum, 16)/10f).setScale(1, BigDecimal.ROUND_HALF_UP));
						}
					}
					//基础代谢率卡路里 BMI
					if (strdata[6].equals("D0")) {
						String tmpNum = strdata[2] + strdata[3];
						if(Integer.valueOf(tmpNum, 16)==0) return;
						if(mOnMeasureDATA!=null){
							mOnMeasureDATA.OnDATA("result-data-11-"+Integer.valueOf(tmpNum, 16));
						}

						tmpNum = strdata[4] + strdata[5];
						if(Integer.valueOf(tmpNum, 16)==0) return;
						if(mOnMeasureDATA!=null){
							mOnMeasureDATA.OnDATA("result-data-12-"+new BigDecimal(Integer.valueOf(tmpNum, 16)/10f).setScale(1, BigDecimal.ROUND_HALF_UP));
						}
					}
					//					//用户资料1
					//					if (strdata[6].equals("E0")) {
					//						String tmpNum=strdata[2]; 
					//						if(!tmpNum.equals("FF")&&Integer.valueOf(strdata[4], 16)!=0&&mOnCommunDATA!=null){
					//							mOnCommunDATA.OnDATA("result-data-13-"+Integer.valueOf(tmpNum, 16));
					//						}
					//
					//						tmpNum=strdata[4]; 
					//						if(!tmpNum.equals("FF")&&Integer.valueOf(strdata[4], 16)!=0&&mOnCommunDATA!=null){
					//							mOnCommunDATA.OnDATA("result-data-14-"+Integer.valueOf(tmpNum, 16));
					//						}
					//					}
					//					
					//					//用户资料2
					//					if (strdata[6].equals("E1")) {
					//						String tmpNum=strdata[2]; 
					//						if(!tmpNum.equals("FF")&&Integer.valueOf(tmpNum, 16)!=0&&mOnCommunDATA!=null){
					//							mOnCommunDATA.OnDATA("result-data-15-"+Integer.valueOf(tmpNum, 16));
					//						}
					//					}
					//					
					//					//数据的测量日期
					//					if (strdata[6].equals("E2")){
					//						String tmpNum= strdata[2];
					//						int year_Num=Integer.valueOf(tmpNum,16)+2000;
					//
					//						tmpNum= strdata[3]+strdata[4];
					//						int day_Num=Integer.valueOf(tmpNum,16);
					//
					//						Calendar cal = Calendar.getInstance();
					//						cal.set(Calendar.YEAR, year_Num);
					//						cal.set(Calendar.DAY_OF_YEAR, day_Num);
					//						//					cal.set(Calendar.DAY_OF_YEAR, day_Num);
					//						cal.set(Calendar.MONTH, 0);
					//						cal.set(Calendar.MINUTE, 0);
					//						cal.set(Calendar.SECOND, 0);
					//
					//						if(!tmpNum.equals("FFFF")&&mOnCommunDATA!=null){
					//							mOnCommunDATA.OnDATA("result-data-16-"+Integer.valueOf(strdata[1], 16)+"-"
					//									+String.valueOf(year_Num)+"-"+(cal.get(Calendar.MONTH) + 1)+"-"+cal.get(Calendar.DAY_OF_MONTH));
					//						}
					//					}
					//
					//					//数据的测量时间
					//					if (strdata[6].equals("E3")){
					//						String tmpNum= strdata[2];
					//
					//						if(!tmpNum.equals("FF")&&mOnCommunDATA!=null){
					//							mOnCommunDATA.OnDATA("result-data-17-"+Integer.valueOf(strdata[1], 16)+"-"
					//									+Integer.valueOf(strdata[2], 16)+"-"+Integer.valueOf(strdata[3], 16)+"-"+Integer.valueOf(strdata[4], 16));
					//						}
					//					}
				}else{ //历史
					if (SendHisOBjectList.get(Integer.valueOf(strdata[1], 16))==null){
						FatDataCommun fatDataComm=new FatDataCommun();
						fatDataComm.setSerial(Integer.valueOf(strdata[1], 16));
						SendHisOBjectList.put(Integer.valueOf(strdata[1], 16), fatDataComm);
					}

					if (("AA-A0").contains(strdata[6])) {
						String tmpNum = strdata[2] + strdata[3];
						if(!tmpNum.equals("FFFF")){
							SendHisOBjectList.get(Integer.valueOf(strdata[1], 16)).setWeightKg(new BigDecimal(Integer.valueOf(tmpNum, 16)/10f).setScale(1, BigDecimal.ROUND_HALF_UP).floatValue());
							if("A0".equals(strdata[6])){
								SendHisOBjectList.get(Integer.valueOf(strdata[1], 16)).setIfStable(false);
							}else if("AA".equals(strdata[6])){
								SendHisOBjectList.get(Integer.valueOf(strdata[1], 16)).setIfStable(true);
							}

							tmpNum = strdata[4] + strdata[5];
							SendHisOBjectList.get(Integer.valueOf(strdata[1], 16)).setWeightLb(new BigDecimal(Integer.valueOf(tmpNum, 16)/10f).setScale(1, BigDecimal.ROUND_HALF_UP).floatValue());
							if("A0".equals(strdata[6])){
								SendHisOBjectList.get(Integer.valueOf(strdata[1], 16)).setIfStable(false);
							}else if("AA".equals(strdata[6])){
								SendHisOBjectList.get(Integer.valueOf(strdata[1], 16)).setIfStable(true);
							}
						}
					}

					//脂肪 水分
					if (strdata[6].equals("B0")) {
						String tmpNum = strdata[2] + strdata[3];
						if(Integer.valueOf(tmpNum, 16)<=1000&&Integer.valueOf(tmpNum, 16)!=0){
							SendHisOBjectList.get(Integer.valueOf(strdata[1], 16)).setFat(new BigDecimal(Integer.valueOf(tmpNum, 16)/10f).setScale(1, BigDecimal.ROUND_HALF_UP).floatValue());
						}

						tmpNum = strdata[4] + strdata[5];
						if(Integer.valueOf(tmpNum, 16)<=1000&&Integer.valueOf(tmpNum, 16)!=0){
							SendHisOBjectList.get(Integer.valueOf(strdata[1], 16)).setHydration(new BigDecimal(Integer.valueOf(tmpNum, 16)/10f).setScale(1, BigDecimal.ROUND_HALF_UP).floatValue());
						}
					}

					//内脏脂肪和细胞总水重
					if (strdata[6].equals("B1")) {//
						String tmpNum = strdata[2] + strdata[3];
						if(Integer.valueOf(tmpNum, 16)<=1000&&Integer.valueOf(tmpNum, 16)!=0){
							SendHisOBjectList.get(Integer.valueOf(strdata[1], 16)).setVisceralFat(new BigDecimal(Integer.valueOf(tmpNum, 16)/10f).setScale(1, BigDecimal.ROUND_HALF_UP).floatValue());
						}
					}

					//瘦体重和蛋白质数据
					if (strdata[6].equals("B2")) {
						String tmpNum = strdata[4] + strdata[5];
						if(Integer.valueOf(tmpNum, 16)<=1000&&Integer.valueOf(tmpNum, 16)!=0){
							SendHisOBjectList.get(Integer.valueOf(strdata[1], 16)).setProtein(new BigDecimal(Integer.valueOf(tmpNum, 16)/10f).setScale(1, BigDecimal.ROUND_HALF_UP).floatValue());
						}
					}

					//身体年龄和健康得分
					if (strdata[6].equals("B3")) {
						String tmpNum = strdata[3];
						if(Integer.valueOf(tmpNum, 16)<=1000&&Integer.valueOf(tmpNum, 16)!=0){
							SendHisOBjectList.get(Integer.valueOf(strdata[1], 16)).setBodyAge(Integer.valueOf(tmpNum, 16));
						}

						tmpNum = strdata[5];
						if(Integer.valueOf(tmpNum, 16)<=1000&&Integer.valueOf(tmpNum, 16)!=0){
							SendHisOBjectList.get(Integer.valueOf(strdata[1], 16)).setHealthGrade(Integer.valueOf(tmpNum, 16));
						}
					}

					//肌肉百分比 肌肉重 骨骼重
					if (strdata[6].equals("C0")) {//
						String tmpNum = strdata[2] + strdata[3];
						if(Integer.valueOf(tmpNum, 16)<=1000&&Integer.valueOf(tmpNum, 16)!=0){
							SendHisOBjectList.get(Integer.valueOf(strdata[1], 16)).setMuscle(new BigDecimal(Integer.valueOf(tmpNum, 16)/10f).setScale(1, BigDecimal.ROUND_HALF_UP).floatValue());
						}

						tmpNum = strdata[5] + strdata[4];
						if(Integer.valueOf(tmpNum, 16)<=1000&&Integer.valueOf(tmpNum, 16)!=0){
							SendHisOBjectList.get(Integer.valueOf(strdata[1], 16)).setBone(new BigDecimal(Integer.valueOf(tmpNum, 16)/10f).setScale(1, BigDecimal.ROUND_HALF_UP).floatValue());
						}
					}

					//基础代谢率卡路里 BMI
					if (strdata[6].equals("D0")) {
						String tmpNum = strdata[2] + strdata[3];
						if(!tmpNum.equals("FFFF")&&Integer.valueOf(tmpNum, 16)!=0){
							SendHisOBjectList.get(Integer.valueOf(strdata[1], 16)).setKcal(Integer.valueOf(tmpNum, 16));
						}

						tmpNum = strdata[4] + strdata[5];
						if(!tmpNum.equals("FFFF")&&Integer.valueOf(tmpNum, 16)!=0){
							SendHisOBjectList.get(Integer.valueOf(strdata[1], 16)).setBMI(new BigDecimal(Integer.valueOf(tmpNum, 16)/10f).setScale(1, BigDecimal.ROUND_HALF_UP).floatValue());
						}
					}
					//用户资料1
					if (strdata[6].equals("E0")) {
						String tmpNum=strdata[2]; 
						if(!tmpNum.equals("FF")&&Integer.valueOf(strdata[4], 16)!=0){
							SendHisOBjectList.get(Integer.valueOf(strdata[1], 16)).setSex(Integer.valueOf(tmpNum, 16));
						}

						tmpNum=strdata[4]; 
						if(!tmpNum.equals("FF")&&Integer.valueOf(strdata[4], 16)!=0){
							SendHisOBjectList.get(Integer.valueOf(strdata[1], 16)).setAge(Integer.valueOf(tmpNum, 16));
						}
					}

					//用户资料2
					if (strdata[6].equals("E1")) {
						String tmpNum=strdata[2]; 
						if(!tmpNum.equals("FF")&&Integer.valueOf(tmpNum, 16)!=0){
							SendHisOBjectList.get(Integer.valueOf(strdata[1], 16)).setHeight(Integer.valueOf(tmpNum, 16));
						}
					}

					//数据的测量日期
					if (strdata[6].equals("E2")){
						String tmpNum= strdata[2];
						int year_Num=Integer.valueOf(tmpNum,16)+2000;

						tmpNum= strdata[3]+strdata[4];
						int day_Num=Integer.valueOf(tmpNum,16);

						Calendar cal = Calendar.getInstance();
						cal.set(Calendar.YEAR, year_Num);
						cal.set(Calendar.DAY_OF_YEAR, day_Num);
						//					cal.set(Calendar.DAY_OF_YEAR, day_Num);
						cal.set(Calendar.MONTH, 0);
						cal.set(Calendar.MINUTE, 0);
						cal.set(Calendar.SECOND, 0);

						if(!tmpNum.equals("FFFF")){
							SendHisOBjectList.get(Integer.valueOf(strdata[1], 16)).setYear(year_Num);
							SendHisOBjectList.get(Integer.valueOf(strdata[1], 16)).setMonth(cal.get(Calendar.MONTH) + 1);
							SendHisOBjectList.get(Integer.valueOf(strdata[1], 16)).setDay(cal.get(Calendar.DAY_OF_MONTH));
						}
					}

					//数据的测量时间
					if (strdata[6].equals("E3")){
						String tmpNum= strdata[2]+strdata[3];

						if(!tmpNum.equals("FFFF")){
							SendHisOBjectList.get(Integer.valueOf(strdata[1], 16)).setHour(Integer.valueOf(strdata[2], 16));
							SendHisOBjectList.get(Integer.valueOf(strdata[1], 16)).setMinutes(Integer.valueOf(strdata[3], 16));
							SendHisOBjectList.get(Integer.valueOf(strdata[1], 16)).setSeconds(Integer.valueOf(strdata[4], 16));
						}
					}
				}
			}
		}
	};





	private void displayGattServices(List<BluetoothGattService> gattServices) {
		Log.e("gattServices","gattServices"+gattServices);
		if (gattServices == null) return;
//		遍历 GATT 服务可用
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
	private void mSendData(){
		mSendDataHandler.postDelayed(mSendRunnable, 500);
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
		mSendDataHandler2.postDelayed(mSendRunnable2, 1000);
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

	public void DataCommunBuffer() { 
		byte[] outBuffer=BluetoothBuffer.DataCommun2Buffer;

		mSendDataList2.add(outBuffer);
	}
	public void AllDataCommunBuffer() { 
		byte[] outBuffer=BluetoothBuffer.DataCommun3Buffer;

		mSendDataList2.add(outBuffer);
	}
	public void SendClearData() { 
		byte[] outBuffer=BluetoothBuffer.ClearDataBuffer;

		mSendDataList2.add(outBuffer);
	}

	public void FatTestBuffer(final int height,final int age,final int sex,final int serialNum) { 
		byte[] outBuffer = new byte[8];
		String byte0=Long.toHexString(165);
		outBuffer[0]=(byte)Integer.parseInt(byte0, 16);

		int intByte1=16; //脂肪测试模式 10
		String byte1=Long.toHexString(intByte1);
		outBuffer[1]=(byte)Integer.parseInt(byte1, 16);

		int intByte2=(sex==0?0:8)*16+serialNum;
		String byte2=Long.toHexString(intByte2);
		outBuffer[2]=(byte)Integer.parseInt(byte2, 16);

		int intByte3=age;
		String byte3=Long.toHexString(intByte3);
		outBuffer[3]=(byte)Integer.parseInt(byte3, 16);

		int intByte4=height;
		String byte4=Long.toHexString(intByte4);
		outBuffer[4]=(byte)Integer.parseInt(byte4, 16);

		int intbyte7=intByte1+intByte2+intByte3+intByte4+0+0;
		String byte7=Long.toHexString(intbyte7);
		byte7=byte7.substring(byte7.length()-2,byte7.length());

		outBuffer[7]=(byte)Integer.parseInt(byte7, 16);

		mSendDataList2.add(outBuffer);

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

		mSendDataList2.add(outBuffer);
	}

	/***************************************    自定义搜索连接结束代码          *******************************************/
	@Override
	public boolean onUnbind(Intent intent) {
		// After using a given device, you should make sure that BluetoothGatt.close() is called
		// such that resources are cleaned up properly.  In this particular example, close() is
		// invoked when the UI is disconnected from the Service.
		close();
		mSendDataHandler.removeCallbacks(mSendRunnable);
		mSendDataHandler2.removeCallbacks(mSendRunnable2);
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

	public interface OnCommunDATA{
		void OnDATA(FatDataCommun fatDataCommun);
	}

	OnCommunDATA mOnCommunDATA=null;
	public void setOnCommunDATA(OnCommunDATA e){
		mOnCommunDATA=e;
	}

	public interface OnMeasureDATA{
		void OnDATA(String data);
	}

	OnMeasureDATA mOnMeasureDATA=null;
	public void setOnMeasureDATA(OnMeasureDATA e){
		mOnMeasureDATA=e;
	}
}
