package edu.washington.cs.overlaytest;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import edu.washington.cs.touchfreelibrary.sensors.AccelerometerClickSensor;
import edu.washington.cs.touchfreelibrary.sensors.MicrophoneClickSensor;
import edu.washington.cs.touchfreelibrary.sensors.CameraGestureSensor;
import edu.washington.cs.touchfreelibrary.touchemulation.GestureCursorController;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;

public class SystemOverlay extends Service {
	protected static final String TAG = "SystemOverlay";
	
	private GestureCursorController mGestureCursorController;
	private CameraGestureSensor mGestureSensor;
	private MicrophoneClickSensor mMicrophoneSensor;
	private AccelerometerClickSensor mAccelerometerSensor;
	
	private View mView;
	
	public class SystemOverlayBinder extends Binder {
		SystemOverlay getService() {
			return SystemOverlay.this;
		}
	}
	
	private final IBinder mBinder = new SystemOverlayBinder();
	
	private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
	    @Override
	    public void onManagerConnected(int status) {
	        switch (status) {
	            case LoaderCallbackInterface.SUCCESS:
	            {
	                Log.i(TAG, "OpenCV loaded successfully");
	                
	                CameraGestureSensor.loadLibrary();
	                
	                // more initialization steps go here.
	                mGestureSensor.start();
	                mMicrophoneSensor.start();
	                mGestureCursorController.start();
	                
	            } break;
	            default:
	            {
	                super.onManagerConnected(status);
	            } break;
	        }
	    }
	};

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        
        mGestureSensor = new CameraGestureSensor(this);
        mMicrophoneSensor = new MicrophoneClickSensor();
        mAccelerometerSensor = new AccelerometerClickSensor(this);
        
        mGestureCursorController = new GestureCursorController(this);
        
        mGestureSensor.addGestureListener(mGestureCursorController);
        mGestureSensor.addClickListener(mGestureCursorController);
        mMicrophoneSensor.addClickListener(mGestureCursorController);
        mAccelerometerSensor.addClickListener(mGestureCursorController);
        
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this, mLoaderCallback);
        mView = mGestureCursorController.getView();
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
                0,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.FILL;
        params.setTitle("Load Average");
        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        wm.addView(mView, params);
    }

    @Override
    public synchronized void onDestroy() {
        super.onDestroy();
        
        ((WindowManager) getSystemService(WINDOW_SERVICE)).removeView(mView);
        mGestureSensor.stop();
        mMicrophoneSensor.stop();
        mGestureCursorController.stop();
    }
	
	public void enableClapToClick(boolean enabled) {
		if(enabled)
			mMicrophoneSensor.start();
		else
			mMicrophoneSensor.stop();
	}

	public boolean isClapToClickEnabled() {
		return mMicrophoneSensor.isStarted();
	}
	
	public void enableAccelerometerToClick(boolean enabled) {
		if(enabled)
			mAccelerometerSensor.start();
		else
			mAccelerometerSensor.stop();
	}

	public boolean isAccelerometerToClickEnabled() {
		return mAccelerometerSensor.isStarted();
	}
	
	public void enableColorToClick(boolean enabled) {
		mGestureSensor.enableClickByColor(enabled);
	}

	public boolean isColorToClickEnabled() {
		return mGestureSensor.isClickByColorEnabled();
	}
}
