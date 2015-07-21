package com.ryan.android.bvideoviewsample;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.Process;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.baidu.cyberplayer.core.BVideoView;
import com.baidu.cyberplayer.core.BVideoView.OnCompletionListener;
import com.baidu.cyberplayer.core.BVideoView.OnErrorListener;
import com.baidu.cyberplayer.core.BVideoView.OnInfoListener;
import com.baidu.cyberplayer.core.BVideoView.OnPlayingBufferCacheListener;
import com.baidu.cyberplayer.core.BVideoView.OnPreparedListener;

public class BaiduVideoViewActivity extends Activity implements
		OnPreparedListener, OnCompletionListener, OnErrorListener,
		OnInfoListener, OnPlayingBufferCacheListener {

	public static final String TAG = BaiduVideoViewActivity.class
			.getSimpleName();

	// 视频播放地址
	private String mVideoSource = null;

	// 播放按钮
	private ImageButton mPlaybtn = null;

	// 播放器控制
	private LinearLayout mController = null;

	// 播放器进度条
	private SeekBar mProgress = null;

	// 视频总长度
	private TextView mDuration = null;

	// 当前播放长度
	private TextView mCurrPosition = null;

	// 记录最后播放长度
	private int mLastPos = 0;

	private enum PLAYER_STATUS {

		PLAYER_IDLE, PLAYER_PREPARING, PLAYER_PREPARED,
	}

	private PLAYER_STATUS mPlayerStatus = PLAYER_STATUS.PLAYER_IDLE;

	private BVideoView mVV = null;

	private EventHandler mEventHandler;

	private HandlerThread mHandlerThread;

	private final Object SYNC_Playing = new Object();

	private WakeLock mWakeLock = null;

	private static final String POWER_LOCK = BaiduVideoViewActivity.class
			.getSimpleName();

	private boolean mIsHwDecode = false;

	private final int EVENT_PLAY = 0;

	private final int UI_EVENT_UPDATE_CURRPOSITION = 1;

	class EventHandler extends Handler {

		public EventHandler(Looper looper) {
			super(looper);
		}

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			switch (msg.what) {
			case EVENT_PLAY:
				// 如果已经播放了，等待上一次播放结束
				if (mPlayerStatus != PLAYER_STATUS.PLAYER_IDLE) {
					synchronized (SYNC_Playing) {

						try {
							SYNC_Playing.wait();
							Log.v(TAG, "wait player status to idle");
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}

				// 设置播放url
				mVV.setVideoPath(mVideoSource);

				// 续播
				if (mLastPos > 0) {
					mVV.seekTo(mLastPos);
					mLastPos = 0;
				}
				// 显示或者隐藏缓冲提示
				mVV.showCacheInfo(true);
				// 开放播放
				mVV.start();
				mPlayerStatus = PLAYER_STATUS.PLAYER_PREPARING;
				break;
			default:
				break;
			}
		}
	}

	Handler mUIHandler = new Handler() {

		public void handleMessage(Message msg) {
			switch (msg.what) {
			// 更新进度及时间
			case UI_EVENT_UPDATE_CURRPOSITION:
				int currPosition = mVV.getCurrentPosition();
				int duration = mVV.getDuration();
				updateTextViewWithTimeFormat(mCurrPosition, currPosition);
				updateTextViewWithTimeFormat(mDuration, duration);
				mProgress.setMax(duration);
				mProgress.setProgress(currPosition);

				mUIHandler.sendEmptyMessageDelayed(
						UI_EVENT_UPDATE_CURRPOSITION, 200);
				break;
			default:
				break;
			}
		};
	};

	private void updateTextViewWithTimeFormat(TextView view, int second) {
		int hh = second / 3600;
		int mm = second % 3600 / 60;
		int ss = second % 60;
		String strTemp = null;
		if (0 != hh) {
			strTemp = String.format("%02d:%02d:%02d", hh, mm, ss);
		} else {
			strTemp = String.format("%02d:%02d", mm, ss);
		}
		view.setText(strTemp);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.controllerplaying_view);
		
		mVideoSource = "http://pl.youku.com/playlist/m3u8?vid=321997568&type=mp4&ts=1437437596&keyframe=0&ep=dyaXHU2OUcsJ4ybaiz8bNCnqfHReXP0I9xaMhtdkCNQiS%2BC%2F&sid=543743759655312c5883a&token=6380&ctype=12&ev=1&oip=-1557362058" ;

		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		mWakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK
				| PowerManager.ON_AFTER_RELEASE, POWER_LOCK);

		// mIsHwDecode = getIntent().getBooleanExtra("isHW", false);
		// Uri uriPath = getIntent().getData();
		// if (null != uriPath) {
		// String scheme = uriPath.getScheme();
		// if (null != scheme) {
		// mVideoSource = uriPath.toString();
		// } else {
		// mVideoSource = uriPath.getPath();
		// }
		// }
		initUI();

		/**
		 * 开启后台事件处理线程
		 */
		mHandlerThread = new HandlerThread("event handler thread",
				Process.THREAD_PRIORITY_BACKGROUND);
		mHandlerThread.start();
		mEventHandler = new EventHandler(mHandlerThread.getLooper());
	}

	// 初始化界面
	private void initUI() {

		mPlaybtn = (ImageButton) findViewById(R.id.play_btn);
		mController = (LinearLayout) findViewById(R.id.controlbar);

		mProgress = (SeekBar) findViewById(R.id.media_progress);
		mDuration = (TextView) findViewById(R.id.time_total);
		mCurrPosition = (TextView) findViewById(R.id.time_current);

		registerCallbackForControl();

		// 设置AK及SK
		BVideoView.setAKSK(AppConfig.AK, AppConfig.SK);

		mVV = (BVideoView) findViewById(R.id.video_view);

		mVV.setOnPreparedListener(this);
		mVV.setOnCompletionListener(this);
		mVV.setOnErrorListener(this);
		mVV.setOnInfoListener(this);

		// 设置解码模式
		mVV.setDecodeMode(mIsHwDecode ? BVideoView.DECODE_HW
				: BVideoView.DECODE_SW);
	}

	/**
	 * 为控件注册回调处理函数
	 */
	private void registerCallbackForControl() {

		mPlaybtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {

				if (mVV.isPlaying()) {
					mPlaybtn.setImageResource(R.drawable.play_btn_style);
					// 暂停播放
					mVV.pause();
				} else {
					mPlaybtn.setImageResource(R.drawable.pause_btn_style);
					// 继续播放
					mVV.resume();
				}

			}
		});

		OnSeekBarChangeListener osbcl = new OnSeekBarChangeListener() {

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {

				int iseekPos = seekBar.getProgress();
				// SeekBar完成seek时执行seekTo操作并更新界面
				mVV.seekTo(iseekPos);
				Log.v(TAG, "seek to " + iseekPos);
				mUIHandler.sendEmptyMessage(UI_EVENT_UPDATE_CURRPOSITION);

			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {

				mUIHandler.removeMessages(UI_EVENT_UPDATE_CURRPOSITION);
			}

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				updateTextViewWithTimeFormat(mCurrPosition, progress);

			}
		};

		mProgress.setOnSeekBarChangeListener(osbcl);
	}

	@Override
	protected void onPause() {
		super.onPause();

		// 在停止播放前，记录当前播放的位置，以便以后可以续播
		if (mPlayerStatus == PLAYER_STATUS.PLAYER_PREPARED) {
			mLastPos = mVV.getCurrentPosition();
			mVV.stopPlayback();
		}
	}

	@Override
	protected void onResume() {
		super.onResume();

		Log.v(TAG, "onResume");

		if (null != mWakeLock && (!mWakeLock.isHeld()))
			mWakeLock.acquire();

		// 发起一次播放任务
		mEventHandler.sendEmptyMessage(EVENT_PLAY);
	}

	private long mTouchTime;

	private boolean barShow = true;

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (event.getAction() == MotionEvent.ACTION_DOWN)
			mTouchTime = System.currentTimeMillis();
		else if (event.getAction() == MotionEvent.ACTION_UP) {
			long time = System.currentTimeMillis() - mTouchTime;
			if (time < 400) {
				updateControlBar(!barShow);
			}
		}

		return true;
	}

	public void updateControlBar(boolean show) {

		if (show) {
			mController.setVisibility(View.VISIBLE);
		} else {
			mController.setVisibility(View.INVISIBLE);
		}
		barShow = show;
	}

	@Override
	protected void onStop() {
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		/**
		 * 退出后台事件处理线程
		 */
		mHandlerThread.quit();
	}

	/**
	 * 当前缓冲的百分比， 可以配合onInfo中的开始缓冲和结束缓冲来显示百分比到界面
	 */
	@Override
	public void onPlayingBufferCache(int percent) {

	}

	@Override
	public boolean onInfo(int what, int extra) {
		switch (what) {
		/**
		 * 开始缓冲
		 */
		case BVideoView.MEDIA_INFO_BUFFERING_START:
			break;
		/**
		 * 结束缓冲
		 */
		case BVideoView.MEDIA_INFO_BUFFERING_END:
			break;
		default:
			break;
		}
		return true;
	}

	/**
	 * 播放出错
	 */
	@Override
	public boolean onError(int what, int extra) {
		Log.v(TAG, "onError");
		synchronized (SYNC_Playing) {
			SYNC_Playing.notify();
		}
		mPlayerStatus = PLAYER_STATUS.PLAYER_IDLE;
		mUIHandler.removeMessages(UI_EVENT_UPDATE_CURRPOSITION);
		return true;
	}

	/**
	 * 播放完成
	 */
	@Override
	public void onCompletion() {
		Log.v(TAG, "onCompletion");
		synchronized (SYNC_Playing) {
			SYNC_Playing.notify();
		}
		mPlayerStatus = PLAYER_STATUS.PLAYER_IDLE;
		mUIHandler.removeMessages(UI_EVENT_UPDATE_CURRPOSITION);
	}

	/**
	 * 准备播放就绪
	 */
	@Override
	public void onPrepared() {
		Log.v(TAG, "onPrepared");
		mPlayerStatus = PLAYER_STATUS.PLAYER_PREPARED;
		mUIHandler.sendEmptyMessage(UI_EVENT_UPDATE_CURRPOSITION);
	}
}
