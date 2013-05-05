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
	
	public AccelerometerClickSensor(Context context) {
		mListener = null;
		mIsStarted = false;
		
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
		
		// record them as a log
		Log.d(TAG, String.format("Accelerometer: (%f, %f, %f", ax, ay, az));
	}
}
