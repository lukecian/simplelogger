package com.example.logutiltest;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.util.GpsStatusTracker;
import com.util.GpsStatusTracker.GpsInfoListener;
import com.util.IssueAdapter.UploadLogListener;
import com.util.LogThread;

public class LogService extends Service implements GpsInfoListener,UploadLogListener{
	
	class GpsLogger extends LogThread
	{
		public GpsLogger(Context ctx) {
			super(ctx,"gps","gps_log");
		}
	}
	TelephonyManager tm;
	
	GpsLogger loger;
	LogServiceImpl serviceImp = new LogServiceImpl();
	GpsStatusTracker gps_tracker;

	@Override
	public void onCreate() {
		super.onCreate();
		loger = new GpsLogger(this);
		loger.addLogUploadListener(this);
		loger.startLog();
		
		gps_tracker = new GpsStatusTracker(this);
	    gps_tracker.setGpsInfoListener(this);
	    gps_tracker.startTracking();
	    
	    tm = (TelephonyManager) this.getSystemService(TELEPHONY_SERVICE);  
	}
	
	@Override
	public IBinder onBind(Intent arg0) {
		return serviceImp;
	}
	
	com.example.logutiltest.IGpsInfoListener gps_listener;
	ILogUploadListener log_listener;
	
	class LogServiceImpl extends LogServiceApi.Stub
	{
		@Override
		public void start() throws RemoteException {
			loger.startLog();
		}
		
		@Override
		public void stop() throws RemoteException {
			loger.stopLog();
		}
		
		@Override
		public void log_print(int level, String info) throws RemoteException {
			loger.printInfo(info);
		}
		
		@Override
		public void upload_log() throws RemoteException {
			Log.w("Logger","上传日志到服务器-------->");
			String imei = tm.getDeviceId();
			loger.setIssueTitle("GPS 信号跟踪  用户"+imei);
			loger.setIssueDescription("GPS SNR log data");
			loger.uploadLogFilesToServer();
		}

		@Override
		public void setGpsInfoUpdateListener(com.example.logutiltest.IGpsInfoListener callback)
				throws RemoteException {
			gps_listener = callback;
		}

		@Override
		public void addLogListener(ILogUploadListener callback)
				throws RemoteException {
			log_listener = callback;
		}
	}

	// 记录日志，通知UI界面
	@Override
	public void OnGpsInfoUpdate(String info) {
		loger.printInfo(info);
		try {
			if(gps_listener!=null)
				gps_listener.OnGpsInfoUpdate(info);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void OnBeginUpload(String file) {
		if(log_listener!=null)
			try {
				log_listener.OnUploadBegin();
			} catch (RemoteException e) {
				e.printStackTrace();
			}
	}

	@Override
	public void OnEndUpload(String file) {
		if(log_listener!=null)
			try {
				log_listener.OnUploadComplete();
			} catch (RemoteException e) {
				e.printStackTrace();
			}
	}

	@Override
	public void OnUploadError(String errMsg, String file) {
		if(log_listener!=null)
			try {
				log_listener.OnUploadError();
			} catch (RemoteException e) {
				e.printStackTrace();
			}
	}
}
