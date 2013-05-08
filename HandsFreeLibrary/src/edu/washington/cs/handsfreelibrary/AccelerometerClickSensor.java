package edu.washington.cs.handsfreelibrary;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

public class AccelerometerClickSensor implements SensorEventListener {
	private static final String TAG = "AccelerometerClickSensor";
	
	private ClickListener mListener;
	private boolean mIsStarted;
	
	private SensorManager mSensorManager;
	private Sensor mAccelerometer;
	
	private static final double MAX_ACCELERATIONX_FOR_CLICK = 0.35;
	private static final double MIN_ACCELERATIONX_FOR_CLICK = -0.35;
	
	private static final double MAX_ACCELERATIONY_FOR_CLICK = 0.35;
	private static final double MIN_ACCELERATIONY_FOR_CLICK = -0.35;
	
	private static final double MAX_ACCELERATIONZ_FOR_CLICK = 8.5;
	private static final double MIN_ACCELERATIONZ_FOR_CLICK = 6.0;
	private static final int BREAK_TIME = 10;
	
	private int mBreakTimer;
	
	public AccelerometerClickSensor(Context context) {
		mListener = null;
		mIsStarted = false;
		mBreakTimer = 0;
		
		mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
		mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
	}
	
	public void setListener(ClickListener listener) {
		mListener = listener;
	}
	
	public void start() {
		if(!mIsStarted) {
			mIsStarted = true;
			mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
		}
	}
	
	public void stop() {
		if(mIsStarted) {
			mIsStarted = false;
			mSensorManager.unregisterListener(this);
		}
	}
	
	public boolean isStarted() {
		return mIsStarted;
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		// get the acceleration coordinates
		float ax = event.values[0];
		float ay = event.values[1];
		float az = event.values[2];
		
		if(mBreakTimer == 0) {
			if(ax < MAX_ACCELERATIONX_FOR_CLICK && ay < MAX_ACCELERATIONY_FOR_CLICK && az < MAX_ACCELERATIONZ_FOR_CLICK &&
			   ax > MIN_ACCELERATIONX_FOR_CLICK && ay > MIN_ACCELERATIONY_FOR_CLICK && az > MIN_ACCELERATIONZ_FOR_CLICK) {
				mListener.onSensorClick();
				mBreakTimer = BREAK_TIME;
			}
		} else mBreakTimer--;
		
		Log.d(TAG, String.format("Accelerometer: (%f, %f, %f", ax, ay, az));
	}
}
