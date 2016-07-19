package com.util;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.annotation.TargetApi;
import android.os.Build;
import android.util.Log;


@TargetApi(Build.VERSION_CODES.GINGERBREAD) 
public class LogUtil
{
	private final static String Tag = LogUtil.class.getSimpleName();
	SimpleDateFormat log_msg_formatter = new SimpleDateFormat ("yyyy-MM-dd HH:mm:ss",Locale.CHINA); 
	SimpleDateFormat current_log_dir_formatter = new SimpleDateFormat ("yyyy-MM-dd",Locale.CHINA); 
	RandomAccessFile raf;
	File logDir;
	File logFile;
	File logRootDir;
	String logRootPath;
	String moudleTag = "no-name";
	String createDateString;
	ILogAppender appender;
	/**
	 * 构造函数
	 * @param moduleTag
	 * @param rootPath
	 */
	public LogUtil(String module,String rootPath)
	{
		/**
		 * LogHome/log_module_tag_DateString.txt
		 */
		moudleTag = module;
		Calendar today = Calendar.getInstance();
		logRootPath = rootPath;
	    createDateString = current_log_dir_formatter.format(today.getTime());//当前创建的日期
		
		logDir = new File(logRootPath);
		if(!logDir.exists())
			logDir.mkdirs();
			
		logFile = new File(logDir,"log_"+moudleTag+createDateString+".txt");
		logFile.setReadable(true);
		logFile.setWritable(true);
		try {
			raf = new RandomAccessFile(logFile,"rws");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	private void check()
	{
		// 系统时间变化的时候及时切换日志写入路径
		Calendar now = Calendar.getInstance();
		String nowDateString = current_log_dir_formatter.format(now.getTime());
		Log.e(Tag, "Create Log at:"+createDateString+" now is:"+nowDateString);
		if(!createDateString.equalsIgnoreCase(nowDateString))
		{
			logDir = new File(logRootPath);
			if(!logDir.exists())
			{
				logDir.mkdirs();
			}
			
			logFile = new File(logDir,"log_"+moudleTag+nowDateString+".txt");
			logFile.setReadable(true);
			logFile.setWritable(true);
			createDateString = nowDateString;
			try {
				raf = new RandomAccessFile(logFile,"rws");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public synchronized void printf(String TAG,Object msg)
	{
		check();
		//long mem_total = Runtime.getRuntime().totalMemory();
		String timeStamp = log_msg_formatter.format(new Date()); 
		StringBuilder logContent = new StringBuilder();
		Log.w(Tag,"logUtl.print() ");
		if(TAG==null){
			logContent.append("[");
			logContent.append(timeStamp);
			logContent.append("] ");
			logContent.append(" ");
			logContent.append(msg.toString());
			logContent.append("\r\n");
		}
		else
		{
			logContent.append("[");
			logContent.append(timeStamp);
			logContent.append("] ");
			logContent.append(TAG);
			logContent.append(" ");
			logContent.append(msg.toString());
			logContent.append("\r\n");
		}
		// appending text string
		try {
			raf.seek(raf.length());
			raf.write(logContent.toString().getBytes());
			Log.w(Tag,"LogContent:" + logContent);
			Log.w(Tag,"log to file:" + logFile.getAbsolutePath() + " ok!");
		} catch (IOException e) {
			e.printStackTrace();
			Log.e(Tag,"log to file Failure:" + logFile.getAbsolutePath()+ e.getMessage());
		}
		//Log.e(Tag,"mem used:" + (mem_total - Runtime.getRuntime().freeMemory()) / 1024+" KBytes");
	}
	
	interface ILogAppender
	{
		void append(String content);
	}
	
	class RandomAccessFileAppender implements ILogAppender
	{
		RandomAccessFile file;
		public RandomAccessFileAppender(RandomAccessFile raf)
		{
			file = raf;
		}
		@Override
		public void append(String content) {
			if(raf!=null)
			{
				try {
					raf.seek(raf.length());
					raf.write(content.toString().getBytes());
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	class FileStreamAppender  implements ILogAppender
	{
		FileOutputStream file;
		int offset;
		public FileStreamAppender(int ofs,FileOutputStream fos)
		{
			file = fos;
			offset = ofs;
		}
		
		@Override
		public void append(String content) {
			if(file!=null)
			{
				try {
					file.write(content.getBytes(), offset, content.getBytes().length);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	/**
	 * 清除几天前的日志
	 * @param daysBefore
	 */
	public synchronized void deleteOldLogsFromNow(int daysBefore)
	{
		if(daysBefore >=1)
		{
			final SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd",Locale.CHINA);
			List<String> logFilesDontDelete = new ArrayList<String>();
			for(int i=0;i<daysBefore;i++)
			{
				Calendar now = Calendar.getInstance();
				now.add(Calendar.DAY_OF_YEAR, -i);
				String nowDateString = sf.format(now.getTime());
				logFilesDontDelete.add("log_" + moudleTag + nowDateString + ".txt");
			}
			// 扫描所有子目录
			File[] allFilesUnderLogDir = logRootDir.listFiles(new FileFilter() {
				public boolean accept(File pathname) {
					return pathname.isFile();
				}
			});
			
			if(allFilesUnderLogDir!=null)
			{
				for (int i = 0; i < allFilesUnderLogDir.length; i++) {
					if(!logFilesDontDelete.contains(allFilesUnderLogDir[i].getName()))
					{
						allFilesUnderLogDir[i].delete();
					}
				}
			}
		}
	}
	
	// 删除所有日志文件
	public void clearAllLogFiles()
	{
		deleteFileRecursively(logRootDir);
	}
	
	public File getCurrentLogFile()
	{
		Calendar today = Calendar.getInstance();
		String nowDateString = current_log_dir_formatter.format(today.getTime());
		logDir = new File(logRootPath);
		logFile = new File(logDir,"log_" + moudleTag + nowDateString + ".txt");
		Log.w(Tag, "get current log File:"+logFile.getAbsolutePath());
		return logFile;
	}
	
	public static void deleteFileRecursively(File dir)
	{
		if(dir.isDirectory())
		{
			File[] sub = dir.listFiles();
			if(sub.length > 0)
			{
				for (int i = 0; i < sub.length; i++) {
					if(sub[i].isDirectory()){
						deleteFileRecursively(sub[i]);
					}else{
						sub[i].delete();
					}
				}
			}
			dir.delete();
		}
	}
	
	public void error(Object msg)
	{
		printf("ERROR: ",msg);
	}
	
	public void warning(Object msg)
	{
		printf("WARNING: ",msg);
	}
	
	public void info(Object msg)
	{
		printf("INFO: ",msg);
	}
	
	public void close()
	{
		if(this.raf!=null)
			try {
				raf.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
	}
}
