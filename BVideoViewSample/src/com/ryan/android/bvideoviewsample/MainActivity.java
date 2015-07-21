package com.ryan.android.bvideoviewsample;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class MainActivity extends Activity {

	private static final String TAG = "MainActivity";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.main_activity);

		// File file = new File("/data/data/" +
		// getApplication().getPackageName()
		// + "/files/");
		// if (!file.exists())
		// file.mkdirs();
		//
		// System.out.println(file.getAbsolutePath());

		Intent mIntent = new Intent(MainActivity.this,
				BaiduVideoViewActivity.class);
		startActivity(mIntent);
	}
}
