package com.majun.phonedetection;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.TrafficStats;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

public class MainActivity extends Activity implements OnItemSelectedListener {
	public final int INSTALL_SOFTWARE_STATUS = 0;
	public final int PHONE_STATUS = 1;
	public final int SMS_STATUS = 2;
	public final int APN_VPDN_STATUS = 3;
	public final int NETWORK_RECORD = 4;
	public final int PERIPHERAL_STATUS = 5;
	public final int ISROOT=6;
	public final int USB_DEVICE=7;
	private PackageManager manager;
	private TextView textView;
	private StringBuilder buffer;
	private Spinner spinner, packageSpinner;
	private String selectPackage;
	private String[] packageStrings;
	private int plugged;
	private final static int kSystemRootStateUnknow=-1;
	private final static int kSystemRootStateDisable=0;
	private final static int kSystemRootStateEnable=1;
	private static int systemRootState=kSystemRootStateUnknow; 
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		manager = getPackageManager();
		textView = (TextView) findViewById(R.id.textview);
		spinner = (Spinner) findViewById(R.id.spinner);
		packageSpinner = (Spinner) findViewById(R.id.package_spinner);

		buffer = new StringBuilder();
		spinner.setOnItemSelectedListener(this);
		List<PackageInfo> infos = manager.getInstalledPackages(0);
		packageStrings = new String[infos.size()];
		for (int i = 0; i < infos.size(); i++) {
			packageStrings[i] = infos.get(i).packageName;
		}
		ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, packageStrings);
		packageSpinner.setAdapter(arrayAdapter);
		packageSpinner.setOnItemSelectedListener(this);
		selectPackage = packageStrings[0];
		textView.setText(buffer);
		IntentFilter mIntentFilter = new IntentFilter(); 
		mIntentFilter.addAction(Intent.ACTION_BATTERY_CHANGED); 
		registerReceiver(BtStatusReceiver, mIntentFilter);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2,
			long arg3) {
		// TODO Auto-generated method stub
		switch (arg0.getId()) {
		case R.id.spinner:
			System.out.println("arg2:" + arg2);
			switch (arg2) {
			// 安装软件状态监测
			case INSTALL_SOFTWARE_STATUS:
				buffer.delete(0, buffer.length());
				getInstallSoftwareStatus();
				break;
				// 电话功能监测
			case PHONE_STATUS:
				buffer.delete(0, buffer.length());
				getPhoneState();
				break;
				// 短信功能监测
			case SMS_STATUS:
				buffer.delete(0, buffer.length());
				boolean isSMSUsed = getSMSState();
				if (isSMSUsed) {
					textView.setText("短信功能进程开启");
				} else {
					textView.setText("短信功能进程开启");
				}
				break;
			case APN_VPDN_STATUS:
				buffer.delete(0, buffer.length());
				getAPNVPDNState();
				break;
			case NETWORK_RECORD:
				buffer.delete(0, buffer.length());
				ContentResolver contentResolver = getContentResolver();
				getRecords(contentResolver);
				break;
			case PERIPHERAL_STATUS:
				buffer.delete(0, buffer.length());
				boolean isWIFIEnable = WiFiEnable(this);
				boolean isWIFIopen = WiFiActive(this);
				buffer.append("wifi是否可用：" + isWIFIEnable + "\r\n");
				buffer.append("wifi是否开启：" + isWIFIopen + "\r\n");
				boolean isBluetoothEnable = BluetoothEnable(this);
				boolean isBluetoothOpen = BluetoothOpen(this);
				buffer.append("Bluetooth是否可用：" + isBluetoothEnable + "\r\n");
				buffer.append("Bluetooth是否开启：" + isBluetoothOpen + "\r\n");
				buffer.append("红外功能是否可用：" + false + "\r\n");
				buffer.append("红外功能是否开启：" + false + "\r\n");
				textView.setText(buffer);
				break;
			case ISROOT:
				buffer.delete(0, buffer.length());
				boolean isRoot=isSu();
				buffer.append("是否Root：" +isRoot + "\r\n");
				textView.setText(buffer);
				break;
			case USB_DEVICE:
				buffer.delete(0, buffer.length());
				String acString = ""; 
				switch (plugged) { 
				case 0:
					acString = "没有连接设备"; 
					break; 
				case BatteryManager.BATTERY_PLUGGED_AC: 
					acString = "连接的设备为电源"; 
					break; 
				case BatteryManager.BATTERY_PLUGGED_USB: 
					acString = "连接的设备为usb"; 
					break; 
				} 
				buffer.append(acString+ "\r\n");
				textView.setText(buffer);
			default:
				break;
			}
			break;
		case R.id.package_spinner:
			selectPackage = packageStrings[arg2];
			break;
		default:
			break;
		}

	}

	@Override
	public void onNothingSelected(AdapterView<?> arg0) {
		// TODO Auto-generated method stub
	}

	private void getPhoneState() {
		// 获取硬件支持类型
		System.out.println("phone_state");
		boolean telephoneSupport = manager
				.hasSystemFeature(PackageManager.FEATURE_TELEPHONY);// 检测电话硬件是否可用。
		boolean gsmSupport = manager
				.hasSystemFeature(PackageManager.FEATURE_TELEPHONY_GSM);
		boolean cdmaSupport = manager
				.hasSystemFeature(PackageManager.FEATURE_TELEPHONY_CDMA);
		buffer.append("硬件支持：" + telephoneSupport + "\r\n");
		buffer.append("gsm支持：" + gsmSupport + "\r\n");
		buffer.append("cdma支持：" + cdmaSupport + "\r\n");

		// 获取电话类型
		TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		int phoneType = telephonyManager.getPhoneType();
		switch (phoneType) {
		case TelephonyManager.PHONE_TYPE_GSM:
			buffer.append("电话类型：GSM" + "\r\n");
			break;
		case TelephonyManager.PHONE_TYPE_CDMA:
			buffer.append("电话类型：CDMA" + "\r\n");
			break;
		case TelephonyManager.PHONE_TYPE_SIP:
			buffer.append("电话类型：SIP" + "\r\n");
			break;
		case TelephonyManager.PHONE_TYPE_NONE:
			buffer.append("电话类型：none" + "\r\n");
			break;
		default:
			break;
		}

		// 获取SIM卡信息
		int aimState = telephonyManager.getSimState();
		switch (aimState) {
		case TelephonyManager.SIM_STATE_READY:
			String simCountry = telephonyManager.getSimCountryIso();// 获得iso国家号
			String simOperatorCode = telephonyManager.getSimOperator();// 获得SIM运营商代码
			String simOperatorName = telephonyManager.getSimOperatorName();// 获得运营商名称
			String simSerial = telephonyManager.getSimSerialNumber();// SIM序列号
			buffer.append("ISO国家号：" + simCountry + "\r\n");
			buffer.append("SIM运营商代码：" + simOperatorCode + "\r\n");
			buffer.append("运营商名称：" + simOperatorName + "\r\n");
			buffer.append("SIM序列号：" + simSerial + "\r\n");
			break;

		default:
			buffer.append("手机SIM没有处于就绪状态" + "\r\n");
			break;
		}

		// 获取数据连接状态
		int dataState = telephonyManager.getDataState();
		switch (dataState) {
		case TelephonyManager.DATA_CONNECTED:
			buffer.append("数据连接可用：true" + "\r\n");
			break;

		default:
			buffer.append("数据连接可用：false" + "\r\n");
			break;
		}

		// 判断是否处于飞行模式
		boolean isAirplaneMode = getAirplaneMode(this);
		if (isAirplaneMode) {
			buffer.append("飞行模式开启：true" + "\r\n");
		} else {
			buffer.append("飞行模式开启：false" + "\r\n");
		}

		boolean isPhoneProcessUsing = getPhoneProcessState();
		if (isPhoneProcessUsing) {
			buffer.append("电话服务进程：true" + "\r\n");
		} else {
			buffer.append("电话服务进程：false" + "\r\n");
		}

		/*
		 * MCC+MNC(mobile country code + mobile network code) 注意：仅当用户已在网络注册时有效。
		 * 在CDMA网络中结果也许不可靠。
		 */
		buffer.append("仅在网络有效时显示SIM运营商代码："
				+ telephonyManager.getNetworkOperator() + "\r\n");
		/*
		 * 按照字母次序的current registered operator(当前已注册的用户)的名字 注意：仅当用户已在网络注册时有效。
		 * 在CDMA网络中结果也许不可靠。
		 */
		buffer.append("仅在网络有效时显示SIM运营商名称："
				+ telephonyManager.getNetworkOperatorName() + "\r\n");
		textView.setText(buffer);
	}

	// 判断是否处于飞行模式
	public static boolean getAirplaneMode(Context context) {
		int isAirplaneMode = Settings.System.getInt(
				context.getContentResolver(), Settings.System.AIRPLANE_MODE_ON,
				0);
		return (isAirplaneMode == 1) ? true : false;
	}

	private Boolean getPhoneProcessState() {
		ActivityManager activityManager = (ActivityManager) this
				.getSystemService(Context.ACTIVITY_SERVICE);
		List<RunningAppProcessInfo> appProcessInfos = activityManager
				.getRunningAppProcesses();
		for (RunningAppProcessInfo info : appProcessInfos) {
			String name = info.processName;
			// System.out.println(name);
			if (name.equals("com.android.phone")) {
				return true;
			}
		}
		return false;
	}

	private void getInstallSoftwareStatus() {
		boolean isInstall = isAvilible(selectPackage);
		if (isInstall) {
			textView.setText(selectPackage + "已经安装了");
		} else {
			textView.setText(selectPackage + "还没有安装");
		}
	}

	private boolean isAvilible(String packageName) {
		// 获取所有已安装程序的包信息
		List<PackageInfo> pinfo = manager.getInstalledPackages(0);
		for (int i = 0; i < pinfo.size(); i++) {
			if (pinfo.get(i).packageName.equalsIgnoreCase(packageName))
				return true;
		}
		return false;
	}

	private Boolean getSMSState() {
		ActivityManager activityManager = (ActivityManager) this
				.getSystemService(Context.ACTIVITY_SERVICE);
		List<RunningAppProcessInfo> appProcessInfos = activityManager
				.getRunningAppProcesses();
		for (RunningAppProcessInfo info : appProcessInfos) {
			String name = info.processName;
			System.out.println(name);
			if (name.equals("com.android.smspush")) {
				return true;
			}
		}
		return false;
	}

	private void getAPNVPDNState() {
		// 网络接入点//在WIFI和移动数据都开启的情况下，优先显示WIFI的信息
		ConnectivityManager conManager = (ConnectivityManager) this
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo ni = conManager.getActiveNetworkInfo();
		String apn = ni.getExtraInfo();// 获取网络接入点，这里一般为cmwap和cmnet
		buffer.append("网络接入点名称：" + apn + "\r\n");
		textView.setText(buffer);
	}

	// 一小时内的浏览器访问记录
	public void getRecords(ContentResolver contentResolver) {
		// ContentResolver contentResolver = getContentResolver();
		long selectTime = System.currentTimeMillis() - 3600000;
		Cursor cursor = contentResolver.query(
				Uri.parse("content://browser/bookmarks"), new String[] {
					"title", "url", "date" }, "date!=?",
					new String[] { "null" }, "date desc");
		while (cursor != null && cursor.moveToNext()) {
			String url = null;
			String title = null;
			String time = null;
			String date = null;

			title = cursor.getString(cursor.getColumnIndex("title"));
			url = cursor.getString(cursor.getColumnIndex("url"));

			date = cursor.getString(cursor.getColumnIndex("date"));
			System.err.println(date);
			SimpleDateFormat dateFormat = new SimpleDateFormat(
					"yyyy-MM-dd hh:mm;ss");
			Date d = new Date(Long.parseLong(date));
			time = dateFormat.format(d);

			if (Long.parseLong(date) > selectTime) {
				buffer.append(title + url + time + "\r\n");
			}
		}
		textView.setText(buffer);
	}

	public void showInternetStatus(){
		//1.获取一个包管理器。  
		PackageManager pm = getPackageManager();  
		//2.遍历手机操作系统 获取所有的应用程序的uid  
		List<ApplicationInfo> appliactaionInfos = pm.getInstalledApplications(0);  
		for(ApplicationInfo applicationInfo : appliactaionInfos){  
			int uid = applicationInfo.uid;    // 获得软件uid  
			//proc/uid_stat/10086  
			long tx = TrafficStats.getUidTxBytes(uid);//发送的 上传的流量byte  
			long rx = TrafficStats.getUidRxBytes(uid);//下载的流量 byte
			//方法返回值 -1 代表的是应用程序没有产生流量 或者操作系统不支持流量统计  
		}  
		TrafficStats.getMobileTxBytes();//获取手机3g/2g网络上传的总流量  
		TrafficStats.getMobileRxBytes();//手机2g/3g下载的总流量  

		TrafficStats.getTotalTxBytes();//手机全部网络接口 包括wifi，3g、2g上传的总流量  
		TrafficStats.getTotalRxBytes();//手机全部网络接口 包括wifi，3g、2g下载的总流量
	}
	public boolean WiFiEnable(Context context) {
		Object localObject = (WifiManager)context.getSystemService("wifi");
		if (localObject==null) {
			return false;
		}
		return true;
//		ConnectivityManager connectivity = (ConnectivityManager) context
//				.getSystemService(Context.CONNECTIVITY_SERVICE);
//		if (connectivity != null) {
//			NetworkInfo[] info = connectivity.getAllNetworkInfo();
//			if (info != null) {
//				for (int i = 0; i < info.length; i++) {
//					if (info[i].getTypeName().equals("WIFI")) {
//						return true;
//					}
//				}
//			}
//		}
//		return false;
	}

	public boolean WiFiActive(Context context) {
		ConnectivityManager connectivity = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		if (connectivity != null) {
			NetworkInfo[] info = connectivity.getAllNetworkInfo();
			if (info != null) {
				for (int i = 0; i < info.length; i++) {
					if (info[i].getTypeName().equals("WIFI")
							&& info[i].isConnected()) {
						return true;
					}
				}
			}
		}
		return false;
	}

	private boolean BluetoothOpen(Context context) {
		// TODO Auto-generated method stub
		// 得到BluetoothAdapter对象
		BluetoothAdapter bluetoothAdapter = BluetoothAdapter
				.getDefaultAdapter();// 判断BluetoothAdapter对象是否为空，如果为空，则表明本机没有蓝牙设备
		if (bluetoothAdapter != null) {
			System.out.println("本机拥有蓝牙设备");
			// 调用isEnabled()方法判断当前蓝牙设备是否可用
			if (bluetoothAdapter.isEnabled()) {
				return true;
			} else {
				return false;
			}
		} else {
			return false;
		}
	}

	private boolean BluetoothEnable(Context context) {
		BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
		if (adapter != null) {
			return true;
		}
		return false;
	}

	/**
	 * 检测手机是否root
	 * @return root返回true
	 */
	public static boolean isSu()
	{
		if(systemRootState==kSystemRootStateEnable)
		{
			return true;
		}
		else if(systemRootState==kSystemRootStateDisable)
		{

			return false;
		}
		File f=null;
		final String kSuSearchPaths[]={"/system/bin/","/system/xbin/","/system/sbin/","/sbin/","/vendor/bin/"};
		try{
			for(int i=0;i<kSuSearchPaths.length;i++)
			{
				f=new File(kSuSearchPaths[i]+"su");
				if(f!=null&&f.exists())
				{
					systemRootState=kSystemRootStateEnable;
					return true;
				}
			}
		}catch(Exception e)
		{
			e.printStackTrace();
		}
		systemRootState=kSystemRootStateDisable;
		return false;
	}
	public BroadcastReceiver BtStatusReceiver = new BroadcastReceiver() // receive broadcast that BT Adapter status change
	{
		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			String action = intent.getAction();
			if (action.equals(Intent.ACTION_BATTERY_CHANGED))
			{       
				plugged=intent.getIntExtra("plugged", 0);
			}
		}      
	};
}
