package com.util;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;

//===================================================
//                usage:
// new IssueAdapter(Context).StartReportLog();
//====================================================
@SuppressLint("NewApi") 
public class IssueAdapter {
	public interface UploadLogListener{
		void OnBeginUpload(String file);
		void OnEndUpload(String file);
		void OnUploadError(String errMsg,String file);
	}
	List<UploadLogListener> listeners = new ArrayList<IssueAdapter.UploadLogListener>();
	
	private static final String TAG = "IssueAdapter";

	public static final String X_REDMINE_API_KEY = "f4958c20a66cb9982d5decf0faa36e863b93cfaa";
	public static final String URL_UPLOAD_FILE = "http://112.74.132.86:8082/redmine/uploads.json";
	public static final String URL_NEW_ISSUE = "http://112.74.132.86:8082/redmine/issues.json";

	public String REPORT_DIR;
	private DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");

	public static String REPORT_TITLE = new String("HUD detect report".getBytes(Charset.defaultCharset()));

	private Context mContext = null;

	public IssueAdapter(Context context) {
		mContext = context;
		REPORT_DIR = mContext.getApplicationContext().getFilesDir().getAbsolutePath();
	}

	/** 设置长传到服务器的文件的名字，比如：服务器主题下 867251020030749-report */
	public void SetTitle(String title) {
		REPORT_TITLE = title;
	}
	
	public void addUploadLogListener(UploadLogListener o)
	{
		synchronized (listeners) {
			listeners.add(o);
		}
	}
	
	public void removeUploadLogListener(UploadLogListener o)
	{
		synchronized (listeners) {
			listeners.remove(o);
		}
	}
	
	void notifyBeginUpload(final String fileUrl)
	{
		synchronized (listeners) {
			for (int i = 0; i < listeners.size(); i++) {
				listeners.get(i).OnBeginUpload(fileUrl);
			}
		}
	}
	
	void notifyEndUpload(int OpCode,String fileUrl)
	{
		synchronized (listeners) {
			for (int i = 0; i < listeners.size(); i++) {
				listeners.get(i).OnEndUpload(fileUrl);
			}
		}
	}
	
	void notifyErrorInUpload(String errMsg,String fileUrl)
	{
		synchronized (listeners) {
			for (int i = 0; i < listeners.size(); i++) {
				listeners.get(i).OnUploadError(errMsg, fileUrl);
			}
		}
	}
	
	/** 开启记录进程日志和hud逻辑日志，功能不完全 */
	public void StartReportLog() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					long timestamp = System.currentTimeMillis();
					final String time = formatter.format(new Date());
					String fileName = "report-" + time + "-" + timestamp + ".log";
					String fileName0 = "report0-" + time + "-" + timestamp + ".log";
					// 保存hud逻辑日志
					{
						File dir = new File(REPORT_DIR);
						if (!dir.exists()) {
							dir.mkdirs();
						}

						FileOutputStream fos = new FileOutputStream(REPORT_DIR + File.separator + fileName0);

						for (int i = 0; i < 20; i++) {
							String line = "";
							String content = "";
							String logfileName = "HUD-" + i + ".log";
							File file = new File(REPORT_DIR + File.separator + logfileName);

							if (file.exists() == false) {
								continue;
							}

							InputStream instream = new FileInputStream(file);
							InputStreamReader inputreader = new InputStreamReader(instream);
							BufferedReader buffreader = new BufferedReader(inputreader);

							while ((line = buffreader.readLine()) != null) {
								content += line + "\r\n";
							}
							instream.close();

							fos.write(content.getBytes());
							fos.write("\r\n".getBytes());
						}

						fos.close();
					}

					// 保存进程日志
					{
						File dir = new File(REPORT_DIR);
						if (!dir.exists()) {
							dir.mkdirs();
						}

						FileOutputStream fos = new FileOutputStream(REPORT_DIR + File.separator + fileName);

						List<String> lists = getAllProcess();
						for (int i = 0; i < lists.size(); i++) {
							fos.write(lists.get(i).getBytes());
							fos.write("\r\n".getBytes());
						}

						fos.close();
					}

					String[] files = new String[2];

					files[0] = REPORT_DIR + File.separator + fileName;
					files[1] = REPORT_DIR + File.separator + fileName0;

					CreateIssue("hud report.", files);
				} catch (Exception e) {
					e.printStackTrace();
				}

			}
		}).start();
	}

	public void CreateIssue(String IssueContext, String[] files) {
		if (IssueContext == null) {
			CreateIssue(REPORT_TITLE, "", files);
		} else {
			CreateIssue(REPORT_TITLE, IssueContext, files);
		}
	}

	/** 获取进程日志 */
	private List<String> getAllProcess() {
		List<String> orgProcList = new ArrayList<String>();
		Process proc = null;
		try {
			proc = Runtime.getRuntime().exec("ps");
			StreamConsumer errorConsumer = new StreamConsumer(proc.getErrorStream());

			StreamConsumer outputConsumer = new StreamConsumer(proc.getInputStream(), orgProcList);

			errorConsumer.start();
			outputConsumer.start();
			if (proc.waitFor() != 0) {
				Log.e(TAG, "getAllProcess proc.waitFor() != 0");

			}
		} catch (Exception e) {
			Log.e(TAG, "getAllProcess failed", e);

		} finally {
			try {
				proc.destroy();
			} catch (Exception e) {
				Log.e(TAG, "getAllProcess failed", e);
			}
		}
		return orgProcList;
	}

	class StreamConsumer extends Thread {
		InputStream is;
		List<String> list;

		StreamConsumer(InputStream is) {
			this.is = is;
		}

		StreamConsumer(InputStream is, List<String> list) {
			this.is = is;
			this.list = list;
		}

		public void run() {
			try {
				InputStreamReader isr = new InputStreamReader(is);
				BufferedReader br = new BufferedReader(isr);
				String line = null;
				while ((line = br.readLine()) != null) {
					if (list != null) {
						list.add(line);
					}
				}
			} catch (Exception ioe) {
				ioe.printStackTrace();
			}
		}
	}

	/** 1上传日志到服务器 */
	public void CreateIssue(final String IssueTitle, final String IssueContext, final String[] files) {
		Log.i(TAG, "CreateIssue() called ");
		try {
			final String[] tokens;
			if (files != null) {
				// 创建需要上传附件的日志
				// 支持多个文件
				tokens = new String[files.length];
				new Thread(new Runnable() {
					private FileInputStream fi;
					@Override
					public void run() {
						try {
							List<FileUploadInfo> fileUploadInfo = new ArrayList<IssueAdapter.FileUploadInfo>();
							// 上传文件
							for (int i = 0; i < files.length; i++) {
								// 先压缩文件
								String zipFileAbsolutePath = ZipUtil.getInstatnce().zip(files[i], files[i]+".zip");
								File zipCompressedFile = new File(zipFileAbsolutePath);
								long fileSize = zipCompressedFile.length();
								if (fileSize > Integer.MAX_VALUE) {
									Log.i(TAG, "file too big...");
									return;
								}
								notifyBeginUpload(files[i]);
								fi = new FileInputStream(zipCompressedFile);
								byte[] buffer = new byte[(int) fileSize];
								int offset = 0;
								int numRead = 0;
								while (offset < buffer.length && (numRead = fi.read(buffer, offset, buffer.length - offset)) >= 0) {
									offset += numRead;
								}

								if (offset != buffer.length) {
									Log.i(TAG, "Could not completely read file " + zipCompressedFile.getName());
									notifyErrorInUpload("", zipCompressedFile.getAbsolutePath());
									continue;
								}

								String uri = URL_UPLOAD_FILE;
								URL url = new URL(uri);
								HttpURLConnection conn = (HttpURLConnection) url.openConnection();
								conn.setReadTimeout(5 * 1000);
								conn.setDoInput(true);
								conn.setDoOutput(true);
								conn.setUseCaches(false);
								conn.setRequestMethod("POST");
								conn.setRequestProperty("connection", "keep-alive");
								conn.setRequestProperty("Charsert", "UTF-8");
								conn.setRequestProperty("Content-Type", "application/octet-stream");
								conn.setRequestProperty("X-Redmine-API-Key", X_REDMINE_API_KEY);

								DataOutputStream outStream = new DataOutputStream(conn.getOutputStream());
								outStream.write(buffer);
								outStream.flush();

								String token = "";
								int res = conn.getResponseCode();
								if (res == 201) {
									InputStream in = conn.getInputStream();
									byte[] ret = readStream(in);
									String strRet = new String(ret, "UTF-8");
									JSONObject obj = new JSONObject(strRet);
									JSONObject upload = obj.getJSONObject("upload");
									token = upload.getString("token");
									tokens[i] = token;
								}
								Log.e(TAG,i+" 正在上传日志文件  "+zipFileAbsolutePath+" response Code="+res);
								outStream.close();
								conn.disconnect();
								zipCompressedFile.delete();// 删除此压缩文件
								fileUploadInfo.add(new FileUploadInfo(zipFileAbsolutePath, token));
								notifyEndUpload(0, zipCompressedFile.getAbsolutePath());
							}//end of for()
						    createIssue(IssueTitle, IssueContext, fileUploadInfo);
						} catch (Exception e) {
							e.printStackTrace();
							notifyErrorInUpload(e.getMessage(), "");// notify error 
						}
					}
				}).start();

			} else {
				// 创建没有附件的问题
				new Thread(new Runnable() {
					@Override
					public void run() {
						createIssue(IssueTitle, IssueContext, null);
					}
				}).start();
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	
	/** 读取流 */
	public static byte[] readStream(InputStream inStream) throws Exception {
		ByteArrayOutputStream outSteam = new ByteArrayOutputStream();
		byte[] buffer = new byte[1024];
		int len = -1;
		while ((len = inStream.read(buffer)) != -1) {
			outSteam.write(buffer, 0, len);
		}
		outSteam.close();
		inStream.close();
		return outSteam.toByteArray();
	}
	
	// 上传文件的相关信息
	class FileUploadInfo
	{
		String filename;
		String filestoken;
		
		public FileUploadInfo(String filename, String filestoken) {
			this.filename = filename;
			this.filestoken = filestoken;
		}
		
		public String getFilename() {
			return filename;
		}
		
		public void setFilename(String filename) {
			this.filename = filename;
		}
		
		public String getFilestoken() {
			return filestoken;
		}
		
		public void setFilestoken(String filestoken) {
			this.filestoken = filestoken;
		}
	}
	
	/** 2上传日志到服务器 */
	public void createIssue(final String title, final String context,final List<FileUploadInfo> fileUploadInfoList) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					HttpClient httpclient = new DefaultHttpClient();
					String uri = URL_NEW_ISSUE;
					HttpPost httppost = new HttpPost(uri);
					httppost.addHeader("Content-Type", "application/json");
					httppost.addHeader("X-Redmine-API-Key", X_REDMINE_API_KEY);

					JSONObject obj = new JSONObject();
					obj.put("project_id", "1"); 
					obj.put("subject", title);
					obj.put("description", context);

					if (fileUploadInfoList != null) {
						JSONArray jArray = new JSONArray();

						for (int i = 0; i < fileUploadInfoList.size(); i++) {
							JSONObject upload = new JSONObject();
							upload.put("token", fileUploadInfoList.get(i).getFilestoken());
							upload.put("filename", fileUploadInfoList.get(i).getFilename());
							upload.put("content_type", "plain/text");
							jArray.put(upload);
						}
						obj.put("uploads", jArray);
					}

					JSONObject all = new JSONObject();
					all.put("issue", obj);

					String AllPost = all.toString();
					AllPost = AllPost.replace("\\", "");
					// httppost.setEntity(new StringEntity(AllPost));
					httppost.setEntity(new StringEntity(AllPost.toString(), "UTF-8"));
					
					HttpResponse response;
					response = httpclient.execute(httppost);
					int code = response.getStatusLine().getStatusCode();

					if (code == 201) {
						String rev = EntityUtils.toString(response.getEntity());
						Log.e(TAG, rev);
					} else {
						String rev = EntityUtils.toString(response.getEntity());
						Log.e(TAG, rev);
					}

				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}).start();
	}

}
