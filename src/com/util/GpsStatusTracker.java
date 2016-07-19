package com.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.content.Context;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.GpsStatus.Listener;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.util.Log;


public class GpsStatusTracker {
	public interface GpsInfoListener {
		void OnGpsInfoUpdate(String what);
	}
	String Tag = "gps tracker";
	Context context;
	Thread th;
	boolean running = false;
	LocationManager lm;
	LocationProvider gpsProvider;
	GpsInfoListener info_listener;
	int updateInterval_seconds = 5;
	 
	public GpsStatusTracker(Context ctx) {
		context = ctx.getApplicationContext();
		lm = (LocationManager)context.getSystemService(Context.LOCATION_SERVICE);
	}
	
	public void setGpsInfoListener(GpsInfoListener l)
	{
		info_listener = l;
	}
	
	public void setRequestUpdateInterval(int seconds)
	{
		if(seconds >=1 && seconds!=updateInterval_seconds)
		{
			lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 
									  seconds * 1000, 
					                  0, 
					                  locationListener);// 位置变化监听
			updateInterval_seconds = seconds;
		}
	}
	
	final LocationListener locationListener = new LocationListener() {
		@Override
		public void onStatusChanged(String provider, int status, Bundle data) {
			// TODO Auto-generated method stub
			if (status == LocationProvider.OUT_OF_SERVICE 
				|| status == LocationProvider.TEMPORARILY_UNAVAILABLE)
			{
			}
		}
		
		@Override
		public void onProviderEnabled(String arg0) {
			// TODO Auto-generated method stub
		}
		
		@Override
		public void onProviderDisabled(String arg0) {
			
		}
		
		@Override
		public void onLocationChanged(Location arg0) {
			
		}
	};
	
	public void startTracking()
	{
		if(!running)
		{
			lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 
	                  updateInterval_seconds *1000, 
	                  0, 
	                  locationListener);// 位置变化监听，1s一次
			lm.addGpsStatusListener(gpsListener); // gps 状态监听
			running = true;
		}
	}
	
	public void stopTracking()
	{
		if(running)
		{
			lm.removeUpdates(locationListener);
			lm.removeGpsStatusListener(gpsListener);
			running = false;
		}
	}
	
	GpsStatus.Listener gpsListener = new Listener() {
		@Override
		public void onGpsStatusChanged(int event) {
			GpsStatus status = lm.getGpsStatus(null);
			handleGpsStatusInfo(event,status);
		}
	};
	
	private void handleGpsStatusInfo(int event, GpsStatus status) 
	{
		if (event == GpsStatus.GPS_EVENT_SATELLITE_STATUS) 
	    {
			//获取最大的卫星数（这个只是一个预设值）
	        int maxSatellites = status.getMaxSatellites();
	        List<GpsSatellite> actualSatelliteList = new ArrayList<GpsSatellite>();
	        List<String> satInfo = new ArrayList<String>();
	        satInfo.clear();
	        
	        Iterator<GpsSatellite> it = status.getSatellites().iterator();
	        actualSatelliteList.clear();
	        //记录实际的卫星数目
	        int totalSat = 0;
	        int availableSat = 0;
	        while (it.hasNext() && totalSat <= maxSatellites) 
	        {
	        	//保存卫星的数据到一个队列，用于刷新界面
	            GpsSatellite s = it.next();
	            actualSatelliteList.add(s);
	            totalSat++;
	            if(s.getSnr() > 0.0f)
	            {
	            	++ availableSat;	            	
	            	satInfo.add(satelliteStatusInfo(totalSat, s));
	            }
	        }
	        satInfo.add("当前卫星总数：有信号" + availableSat + "/" +totalSat + "\r\n");
	        
	        if(info_listener!=null)
	        {
	        	info_listener.OnGpsInfoUpdate(satInfo.toString());
	        }
	     }
	     else if(event==GpsStatus.GPS_EVENT_STARTED)
	     {  
	         //定位启动   
	     }
	     else if(event==GpsStatus.GPS_EVENT_STOPPED)
	     {  
	         //定位结束   
	     }
	}
	
	String satelliteStatusInfo(int id,GpsSatellite s)
	{
		StringBuilder sb = new StringBuilder();
		sb.append("GPS_"+String.valueOf(id));
		sb.append("(" + String.valueOf(s.getSnr())+")");
		return sb.toString();
	}
	
	// 测试用接口
	public void sendEmulationGpsData(String data)
	{
        if(info_listener!=null)
        {
        	info_listener.OnGpsInfoUpdate(data);
        }
	}
}
