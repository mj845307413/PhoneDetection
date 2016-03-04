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
			// ��װ���״̬���
			case INSTALL_SOFTWARE_STATUS:
				buffer.delete(0, buffer.length());
				getInstallSoftwareStatus();
				break;
				// �绰���ܼ��
			case PHONE_STATUS:
				buffer.delete(0, buffer.length());
				getPhoneState();
				break;
				// ���Ź��ܼ��
			case SMS_STATUS:
				buffer.delete(0, buffer.length());
				boolean isSMSUsed = getSMSState();
				if (isSMSUsed) {
					textView.setText("���Ź��ܽ��̿���");
				} else {
					textView.setText("���Ź��ܽ��̿���");
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
				buffer.append("wifi�Ƿ���ã�" + isWIFIEnable + "\r\n");
				buffer.append("wifi�Ƿ�����" + isWIFIopen + "\r\n");
				boolean isBluetoothEnable = BluetoothEnable(this);
				boolean isBluetoothOpen = BluetoothOpen(this);
				buffer.append("Bluetooth�Ƿ���ã�" + isBluetoothEnable + "\r\n");
				buffer.append("Bluetooth�Ƿ�����" + isBluetoothOpen + "\r\n");
				buffer.append("���⹦���Ƿ���ã�" + false + "\r\n");
				buffer.append("���⹦���Ƿ�����" + false + "\r\n");
				textView.setText(buffer);
				break;
			case ISROOT:
				buffer.delete(0, buffer.length());
				boolean isRoot=isSu();
				buffer.append("�Ƿ�Root��" +isRoot + "\r\n");
				textView.setText(buffer);
				break;
			case USB_DEVICE:
				buffer.delete(0, buffer.length());
				String acString = ""; 
				switch (plugged) { 
				case 0:
					acString = "û�������豸"; 
					break; 
				case BatteryManager.BATTERY_PLUGGED_AC: 
					acString = "���ӵ��豸Ϊ��Դ"; 
					break; 
				case BatteryManager.BATTERY_PLUGGED_USB: 
					acString = "���ӵ��豸Ϊusb"; 
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
		// ��ȡӲ��֧������
		System.out.println("phone_state");
		boolean telephoneSupport = manager
				.hasSystemFeature(PackageManager.FEATURE_TELEPHONY);// ���绰Ӳ���Ƿ���á�
		boolean gsmSupport = manager
				.hasSystemFeature(PackageManager.FEATURE_TELEPHONY_GSM);
		boolean cdmaSupport = manager
				.hasSystemFeature(PackageManager.FEATURE_TELEPHONY_CDMA);
		buffer.append("Ӳ��֧�֣�" + telephoneSupport + "\r\n");
		buffer.append("gsm֧�֣�" + gsmSupport + "\r\n");
		buffer.append("cdma֧�֣�" + cdmaSupport + "\r\n");

		// ��ȡ�绰����
		TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		int phoneType = telephonyManager.getPhoneType();
		switch (phoneType) {
		case TelephonyManager.PHONE_TYPE_GSM:
			buffer.append("�绰���ͣ�GSM" + "\r\n");
			break;
		case TelephonyManager.PHONE_TYPE_CDMA:
			buffer.append("�绰���ͣ�CDMA" + "\r\n");
			break;
		case TelephonyManager.PHONE_TYPE_SIP:
			buffer.append("�绰���ͣ�SIP" + "\r\n");
			break;
		case TelephonyManager.PHONE_TYPE_NONE:
			buffer.append("�绰���ͣ�none" + "\r\n");
			break;
		default:
			break;
		}

		// ��ȡSIM����Ϣ
		int aimState = telephonyManager.getSimState();
		switch (aimState) {
		case TelephonyManager.SIM_STATE_READY:
			String simCountry = telephonyManager.getSimCountryIso();// ���iso���Һ�
			String simOperatorCode = telephonyManager.getSimOperator();// ���SIM��Ӫ�̴���
			String simOperatorName = telephonyManager.getSimOperatorName();// �����Ӫ������
			String simSerial = telephonyManager.getSimSerialNumber();// SIM���к�
			buffer.append("ISO���Һţ�" + simCountry + "\r\n");
			buffer.append("SIM��Ӫ�̴��룺" + simOperatorCode + "\r\n");
			buffer.append("��Ӫ�����ƣ�" + simOperatorName + "\r\n");
			buffer.append("SIM���кţ�" + simSerial + "\r\n");
			break;

		default:
			buffer.append("�ֻ�SIMû�д��ھ���״̬" + "\r\n");
			break;
		}

		// ��ȡ��������״̬
		int dataState = telephonyManager.getDataState();
		switch (dataState) {
		case TelephonyManager.DATA_CONNECTED:
			buffer.append("�������ӿ��ã�true" + "\r\n");
			break;

		default:
			buffer.append("�������ӿ��ã�false" + "\r\n");
			break;
		}

		// �ж��Ƿ��ڷ���ģʽ
		boolean isAirplaneMode = getAirplaneMode(this);
		if (isAirplaneMode) {
			buffer.append("����ģʽ������true" + "\r\n");
		} else {
			buffer.append("����ģʽ������false" + "\r\n");
		}

		boolean isPhoneProcessUsing = getPhoneProcessState();
		if (isPhoneProcessUsing) {
			buffer.append("�绰������̣�true" + "\r\n");
		} else {
			buffer.append("�绰������̣�false" + "\r\n");
		}

		/*
		 * MCC+MNC(mobile country code + mobile network code) ע�⣺�����û���������ע��ʱ��Ч��
		 * ��CDMA�����н��Ҳ���ɿ���
		 */
		buffer.append("����������Чʱ��ʾSIM��Ӫ�̴��룺"
				+ telephonyManager.getNetworkOperator() + "\r\n");
		/*
		 * ������ĸ�����current registered operator(��ǰ��ע����û�)������ ע�⣺�����û���������ע��ʱ��Ч��
		 * ��CDMA�����н��Ҳ���ɿ���
		 */
		buffer.append("����������Чʱ��ʾSIM��Ӫ�����ƣ�"
				+ telephonyManager.getNetworkOperatorName() + "\r\n");
		textView.setText(buffer);
	}

	// �ж��Ƿ��ڷ���ģʽ
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
			textView.setText(selectPackage + "�Ѿ���װ��");
		} else {
			textView.setText(selectPackage + "��û�а�װ");
		}
	}

	private boolean isAvilible(String packageName) {
		// ��ȡ�����Ѱ�װ����İ���Ϣ
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
		// ��������//��WIFI���ƶ����ݶ�����������£�������ʾWIFI����Ϣ
		ConnectivityManager conManager = (ConnectivityManager) this
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo ni = conManager.getActiveNetworkInfo();
		String apn = ni.getExtraInfo();// ��ȡ�������㣬����һ��Ϊcmwap��cmnet
		buffer.append("�����������ƣ�" + apn + "\r\n");
		textView.setText(buffer);
	}

	// һСʱ�ڵ���������ʼ�¼
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
		//1.��ȡһ������������  
		PackageManager pm = getPackageManager();  
		//2.�����ֻ�����ϵͳ ��ȡ���е�Ӧ�ó����uid  
		List<ApplicationInfo> appliactaionInfos = pm.getInstalledApplications(0);  
		for(ApplicationInfo applicationInfo : appliactaionInfos){  
			int uid = applicationInfo.uid;    // ������uid  
			//proc/uid_stat/10086  
			long tx = TrafficStats.getUidTxBytes(uid);//���͵� �ϴ�������byte  
			long rx = TrafficStats.getUidRxBytes(uid);//���ص����� byte
			//��������ֵ -1 �������Ӧ�ó���û�в������� ���߲���ϵͳ��֧������ͳ��  
		}  
		TrafficStats.getMobileTxBytes();//��ȡ�ֻ�3g/2g�����ϴ���������  
		TrafficStats.getMobileRxBytes();//�ֻ�2g/3g���ص�������  

		TrafficStats.getTotalTxBytes();//�ֻ�ȫ������ӿ� ����wifi��3g��2g�ϴ���������  
		TrafficStats.getTotalRxBytes();//�ֻ�ȫ������ӿ� ����wifi��3g��2g���ص�������
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
		// �õ�BluetoothAdapter����
		BluetoothAdapter bluetoothAdapter = BluetoothAdapter
				.getDefaultAdapter();// �ж�BluetoothAdapter�����Ƿ�Ϊ�գ����Ϊ�գ����������û�������豸
		if (bluetoothAdapter != null) {
			System.out.println("����ӵ�������豸");
			// ����isEnabled()�����жϵ�ǰ�����豸�Ƿ����
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
	 * ����ֻ��Ƿ�root
	 * @return root����true
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
