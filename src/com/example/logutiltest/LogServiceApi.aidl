package com.example.logutiltest;
import com.example.logutiltest.IGpsInfoListener;
import com.example.logutiltest.ILogUploadListener;

interface LogServiceApi{
	void start();
	void stop();
	void log_print(int level,String info);
	void upload_log();
	void setGpsInfoUpdateListener(IGpsInfoListener callback);
	void addLogListener(ILogUploadListener callback);
}