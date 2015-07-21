package com.ryan.android.bvideoviewsample;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.ContactsContract.Directory;
import android.util.Log;

import com.baidu.cyberplayer.utils.VersionManager;
import com.baidu.cyberplayer.utils.VersionManager.CPU_TYPE;
import com.baidu.cyberplayer.utils.VersionManager.RequestCpuTypeAndFeatureCallback;
import com.baidu.cyberplayer.utils.VersionManager.RequestDownloadUrlForCurrentVersionCallback;

public class BVideoViewActivity extends Activity {

	private static final String TAG = BVideoViewActivity.class.getSimpleName();

	private String AK = "Q0jQpWTsQ3cWfdWYgTB2mePy";

	private String SK = "fIYdYAjUuKuR1QWi";

	private int timeout = 50000;

	private static final int GET_CPU_TYPE_SUCCESS = 0x100;

	private static final int GET_LIB_DOWNLOAD_URL_SUCCESS = 0x101;

	private CPU_TYPE current_cpu_type;

	private ProgressDialog mProgressDialog;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.bvideo_view_layout);

		mProgressDialog = new ProgressDialog(this);
		mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		mProgressDialog.setCancelable(false);

		Thread t = new Thread(mRunnable);
		t.start();

	}

	Runnable mRunnable = new Runnable() {

		@Override
		public void run() {

			VersionManager.getInstance().getCurrentSystemCpuTypeAndFeature(
					timeout, AK, SK, new RequestCpuTypeAndFeatureCallback() {

						@Override
						public void onComplete(CPU_TYPE arg0, int arg1) {

							if (null == arg0) {
								Log.e(TAG, "获取当前cpu型号失败");
								return;
							}

							Message msg = mHandler.obtainMessage();
							msg.what = GET_CPU_TYPE_SUCCESS;
							msg.obj = arg0;

							mHandler.sendMessage(msg);
						}
					});

		}
	};

	Runnable mRunnable4FindLibDownloadPath = new Runnable() {

		@Override
		public void run() {

			VersionManager.getInstance().getDownloadUrlForCurrentVersion(
					timeout, current_cpu_type, AK, SK,
					new RequestDownloadUrlForCurrentVersionCallback() {

						@Override
						public void onComplete(String arg0, int arg1) {

							if (null != arg0 && !"".equals(arg0)) {

								Message msg = mHandler.obtainMessage();
								msg.what = GET_LIB_DOWNLOAD_URL_SUCCESS;
								msg.obj = arg0;
								mHandler.sendMessage(msg);
							}

						}
					});

		}
	};

	@SuppressLint("HandlerLeak")
	Handler mHandler = new Handler() {

		public void handleMessage(android.os.Message msg) {
			super.handleMessage(msg);
			switch (msg.what) {
			case GET_CPU_TYPE_SUCCESS:

				Log.v(TAG, "当前cpu型号为：" + msg.obj);

				current_cpu_type = (CPU_TYPE) msg.obj;
				if (null != current_cpu_type) {
					Thread t = new Thread(mRunnable4FindLibDownloadPath);
					t.start();
				}
				break;

			case GET_LIB_DOWNLOAD_URL_SUCCESS:

				System.out.println("so下载地址：" + msg.obj);
				break;
			}
		};
	};

	private class FilowDownloadTask extends
			AsyncTask<String, Integer, InputStream> {

		private long fileSize = -1;

		@Override
		protected InputStream doInBackground(String... params) {

			if (null == params || 0 == params.length) {

				Log.e(TAG, "缺少参数，程序退出");
				return null;
			}
			String url = params[0];

			HttpClient client = null;
			HttpGet request = null;
			HttpResponse response = null;

			client = new DefaultHttpClient();
			request = new HttpGet(url);
			try {
				response = client.execute(request);

				if (HttpStatus.SC_OK != response.getStatusLine()
						.getStatusCode()) {
					Log.v(TAG, "程序响应失败");
					return null;
				} else {

					InputStream input = response.getEntity().getContent();
					fileSize = response.getEntity().getContentLength();
					return input;
				}
			} catch (ClientProtocolException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return null;
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			Log.v(TAG, "开始请求数据下载文件");
		}

		@Override
		protected void onProgressUpdate(Integer... values) {
			super.onProgressUpdate(values);
			mProgressDialog.setMax((int) fileSize);
			mProgressDialog.setProgress(values[0]);
		}

		@Override
		protected void onPostExecute(InputStream result) {
			super.onPostExecute(result);
			if (null != result) {
				File file = new File("/data/"
						+ getApplication().getPackageName() + "/files/");
				if (!file.exists())
					file.mkdirs();
				try {
					FileOutputStream fos = new FileOutputStream(file
							+ File.separator + "tmp.zip");
					int len = -1;
					byte[] b = new byte[1024];
					while ((len = result.read(b)) > 0) {
						fos.write(b, 0, len);
					}
					fos.flush();
					fos.close();
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException ex) {
					ex.printStackTrace();
				}

			}
		}
	}
}
