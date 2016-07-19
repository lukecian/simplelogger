package com.util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.os.ConditionVariable;
import android.os.Environment;
import android.util.Log;

public  class LogThread implements Runnable{
	String TAG = "LogThread";
	public interface Action{
		void execute();
	}
	
	String issueTitle;  // 问题标题
	String issueDescription; // 问题描述
	LogUtil logger;
	Context context;
	String logHomeAbsolutePath; // 日志的顶层目录绝对路径
	Thread workThread;
	Object lock = new Object();
	List<Action> queue = new ArrayList<Action>();
	IssueAdapter logUploader; // 负责上传日志
	ConditionVariable condlock = new ConditionVariable(false);
	boolean open = true;
	
	/**
	 * 默认构造函数
	 */
	public LogThread(Context ctx)
	{
		this(ctx,"unkown","unkown");
	}
	/**
	 * 构造函数
	 * @param ctx
	 * @param moudleName
	 * @param logDirName
	 */
	public LogThread(Context ctx,String moudleName,String logDirName) 
	{
		context = ctx.getApplicationContext();
		/**
		 * log 日志的保存路径是 /data/data/com.app.path/files/logDirName/2001-01-02/log_ModuleName.txt
		 */
		{
			logHomeAbsolutePath = ctx.getFilesDir().getAbsolutePath() + File.separator 
					               + logDirName;
			Log.e(TAG, logHomeAbsolutePath);
		}
			
		logger = new LogUtil(moudleName, logHomeAbsolutePath);
		logUploader = new IssueAdapter(context);
	}
	/**
	 * 设置问题标题,通常是IMEI号+问题字符串组合
	 * @param title
	 */
	public void setIssueTitle(String title)
	{
		logUploader.SetTitle(title);
		issueTitle = title;
	}
	
	public void setIssueDescription(String desc)
	{
		issueDescription = desc;
	}
	
	public void addLogUploadListener(IssueAdapter.UploadLogListener o)
	{
		logUploader.addUploadLogListener(o);
	}
	
	public void removeLogUploadListener(IssueAdapter.UploadLogListener o)
	{
		logUploader.removeUploadLogListener(o);
	}
	
	boolean running = false;
	
	public final void startLog()
	{
		if(!running)
		{ 
			running = true;
			workThread = new Thread(this);
			workThread.setDaemon(true);
			workThread.start();
		}
	}
	
	public final void stopLog()
	{
		if(running)
		{
			running = false;
			workThread.interrupt();
			try {
				workThread.join();
				workThread = null;
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public void open()
	{
		open = true;
	}
	
	public void close()
	{
		open = false;
	}
	
	public final boolean isRunning()
	{
		return running;
	}
	
	public void printInfo(final String what)
	{
		enqueueLog(LOG_LEVEL_INFO,what);
	}
	
	final static int LOG_LEVEL_INFO = 0;
	final static int LOG_LEVEL_WARN = 1;
	final static int LOG_LEVEL_ERR = 2;
	
	private void enqueueLog(final int type,final String msg)
	{
		if(!open) 
			return;
		
		queue.add(new Action() {
			@Override
			public void execute() {
				switch(type){
					case LOG_LEVEL_INFO:
						logger.info( msg);
						break;
					case LOG_LEVEL_WARN:
						logger.warning(msg);
						break;
					case LOG_LEVEL_ERR:
						logger.error(msg);
						break;
				}
			}
		});
		condlock.open();
	}
	
	public void printError(final String what)
	{
		enqueueLog(LOG_LEVEL_ERR,what);
	}
	
	public void printWarning(final String what)
	{
		enqueueLog(LOG_LEVEL_WARN,what);
	}
	
	int count = 0;
	@Override
	public void run() {
		while(running && !Thread.interrupted())
		{
			if(queue.size() > 0)
			{
				while(queue.size() > 0)
				{
					// long start = System.currentTimeMillis();
					Action act = queue.get(0);
					act.execute();
					queue.remove(0);
					act = null;
					try {
						Thread.sleep(10);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					//Log.e("log", "write logs..." + count++ +" Time cost:"+ (System.currentTimeMillis() - start)+"ms");
				}
			}
			else
			{
				//Log.e("log", "blocking...");
				condlock.close();
				condlock.block();
			}
		}
	}

	// 上传当前日志到服务器
	public final void uploadLogFilesToServer() {
		File logFile = logger.getCurrentLogFile();// 当前的日志文件
		logUploader.CreateIssue(issueDescription, new String[]{logFile.getAbsolutePath()});
	}
	
	// 删除当前模块的所有日志文件
	public final void clearAllLogs() {
		logger.clearAllLogFiles();
	}
}
