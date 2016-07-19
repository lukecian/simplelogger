package com.example.logutiltest;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
	TextView tvGpsStat;
	LogServiceApi logService;
	ServiceConnection connetionToLogService;
	ILogUploadListener logListener = new LogListener();
	Button btnUploadLog;
	Button btnWriteLog;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
                     
        btnUploadLog = (Button) findViewById(R.id.btn_upload);
        btnUploadLog.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				try {
					logService.upload_log();
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
		});
        
        btnWriteLog = (Button) findViewById(R.id.btn_write_log);
        btnWriteLog.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				try {
					logService.log_print(0, "------- write log test ------ + "+System.currentTimeMillis());
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
		});
        
        tvGpsStat = (TextView) findViewById(R.id.tvGpsStatus);
        
        connetionToLogService = new ServiceConnection() {
    		@Override
    		public void onServiceDisconnected(ComponentName arg0) {
    		}
    		
    		@Override
    		public void onServiceConnected(ComponentName arg0, IBinder binder) {
    			Toast.makeText(getApplicationContext(), "Log 服务连接成功!", Toast.LENGTH_LONG).show();
    			logService = LogServiceApi.Stub.asInterface(binder);
    			try {
    				logService.addLogListener(logListener);
    				logService.setGpsInfoUpdateListener(gpsListener);
    			} catch (RemoteException e) {
    				e.printStackTrace();
    			}
    		}
    	};
        Intent serviceIntent = new Intent(this,LogService.class);
        serviceIntent.setAction("com.logservice");
        bindService(serviceIntent, connetionToLogService, Context.BIND_AUTO_CREATE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    public void onDestroy() {
    	super.onDestroy();
    	unbindService(connetionToLogService);
    };
    
    IGpsInfoListener gpsListener = new GpsInfoListenerImpl();
    
    class GpsInfoListenerImpl extends IGpsInfoListener.Stub
    {
		@Override
		public void OnGpsInfoUpdate(String what) {
			tvGpsStat.setText(what);
		}
    }
    
    class LogListener extends ILogUploadListener.Stub
    {
    	Handler h = new Handler(Looper.getMainLooper());
		@Override
		public void OnUploadBegin() throws RemoteException {
			h.post(new Runnable() {
				@Override
				public void run() {
					btnUploadLog.setClickable(false);
					Toast.makeText(getApplicationContext(), "开始上传Log...", Toast.LENGTH_SHORT).show();
				}
			});
		}

		@Override
		public void OnUploadComplete() throws RemoteException {
			h.post(new Runnable() {
				@Override
				public void run() {
					btnUploadLog.setClickable(true);
					Toast.makeText(getApplicationContext(), "Log 上传成功!", Toast.LENGTH_LONG).show();
				}
			});
		}

		@Override
		public void OnUploadError() throws RemoteException {
			h.post(new Runnable() {
				@Override
				public void run() {
					btnUploadLog.setClickable(true);
					Toast.makeText(getApplicationContext(), "Log 上传失败!", Toast.LENGTH_LONG).show();
				}
			});
		}
    }
}
