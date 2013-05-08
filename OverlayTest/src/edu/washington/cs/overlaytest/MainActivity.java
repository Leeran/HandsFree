package edu.washington.cs.overlaytest;

import android.os.Bundle;
import android.os.IBinder;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.view.Menu;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Toast;

public class MainActivity extends Activity implements OnClickListener {
	protected static final String TAG = "MainActivity";
	 
	private Button mStartServiceButton;
	private Button mStopServiceButton;
	
	private CheckBox mClapToClick;
	private CheckBox mAccelerometerToClick;
	private CheckBox mColorToClick;
	
	private Intent mServiceIntent;
	private SystemOverlay mSystemOverlay = null;
	
	private Toast mOldMessage = null;
	
	private ServiceConnection mSystemOverlayConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mSystemOverlay = ((SystemOverlay.SystemOverlayBinder)service).getService();
			
			mStartServiceButton.setEnabled(false);
			mStopServiceButton.setEnabled(true);
			
			mClapToClick.setEnabled(true);
			mClapToClick.setChecked(mSystemOverlay.isClapToClickEnabled());
			
			mAccelerometerToClick.setEnabled(true);
			mAccelerometerToClick.setChecked(mSystemOverlay.isAccelerometerToClickEnabled());
			
			mColorToClick.setEnabled(true);
			mColorToClick.setChecked(mSystemOverlay.isColorToClickEnabled());
		}
		@Override
		public void onServiceDisconnected(ComponentName name) {
			mSystemOverlay = null;
			
			mStartServiceButton.setEnabled(true);
			mStopServiceButton.setEnabled(false);
			mClapToClick.setEnabled(false);
			mAccelerometerToClick.setEnabled(false);
			mColorToClick.setEnabled(false);
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		mStartServiceButton = (Button)findViewById(R.id.startService);
		mStartServiceButton.setOnClickListener(this);
		
		mStopServiceButton = (Button)findViewById(R.id.stopService);
		mStopServiceButton.setOnClickListener(this);
		
		mClapToClick = (CheckBox)findViewById(R.id.clap_to_click);
		mClapToClick.setOnClickListener(this);
		
		mAccelerometerToClick = (CheckBox)findViewById(R.id.accelerometer_to_click);
		mAccelerometerToClick.setOnClickListener(this);
		
		mColorToClick = (CheckBox)findViewById(R.id.color_to_click);
		mColorToClick.setOnClickListener(this);
		
		// assign the test buttons to listen to this
		findViewById(R.id.test_button1).setOnClickListener(this);
		findViewById(R.id.test_button2).setOnClickListener(this);
		findViewById(R.id.test_button3).setOnClickListener(this);
		findViewById(R.id.test_button4).setOnClickListener(this);
		findViewById(R.id.test_button5).setOnClickListener(this);
		findViewById(R.id.test_button6).setOnClickListener(this);
		findViewById(R.id.test_button7).setOnClickListener(this);
		findViewById(R.id.test_button8).setOnClickListener(this);
		findViewById(R.id.test_button9).setOnClickListener(this);
		
		mServiceIntent = new Intent(this, SystemOverlay.class);
		bindService(mServiceIntent, mSystemOverlayConnection, 0);
		
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	@Override
	public void onClick(View v) {
		if(v == mStartServiceButton) {
			mStartServiceButton.setEnabled(false);
			startService(mServiceIntent);
			bindService(mServiceIntent, mSystemOverlayConnection, 0);
		}
		else if(v == mStopServiceButton) {
			mStopServiceButton.setEnabled(false);
			stopService(mServiceIntent);
		}
		else if(v == mClapToClick) {
			if(mSystemOverlay != null)
				mSystemOverlay.enableClapToClick(mClapToClick.isChecked());
		}
		else if(v == mAccelerometerToClick) { 
			if(mSystemOverlay != null)
				mSystemOverlay.enableAccelerometerToClick(mAccelerometerToClick.isChecked());
		}
		else if(v == mColorToClick) { 
			if(mSystemOverlay != null)
				mSystemOverlay.enableColorToClick(mColorToClick.isChecked());
		} else {
			Button b = (Button)v;
			// a test button has been hit
			if(mOldMessage != null)
				mOldMessage.cancel();
			mOldMessage = Toast.makeText(this, "Button \"" + b.getText() + "\" clicked.", Toast.LENGTH_SHORT);
			mOldMessage.show();
		}
	}

}
