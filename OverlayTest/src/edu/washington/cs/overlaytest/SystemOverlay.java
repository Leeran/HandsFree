package edu.washington.cs.overlaytest;

import java.util.Timer;
import java.util.TimerTask;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import edu.washington.cs.handsfreelibrary.sensors.AccelerometerClickSensor;
import edu.washington.cs.handsfreelibrary.sensors.ClickSensor;
import edu.washington.cs.handsfreelibrary.sensors.MicrophoneClickSensor;
import edu.washington.cs.handsfreelibrary.sensors.ClickSensorListener;
import edu.washington.cs.handsfreelibrary.sensors.GestureSensor;

import android.app.Instrumentation;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.Binder;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.view.WindowManager;

public class SystemOverlay extends Service implements GestureSensor.Listener, ClickSensorListener {
	protected static final String TAG = "SystemOverlay";
	
	private static final int CURSOR_RADIUS = 20;
	
	private static final int MILLISECONDS_PER_FRAME = 7;
	private static final int NUM_CLICK_FRAMES = 50;
	
	private OverlayView mView;
	private GestureSensor mGestureSensor;
	private MicrophoneClickSensor mMicrophoneSensor;
	private AccelerometerClickSensor mAccelerometerSensor;
	
	private Point mSize;
	private Point mPosition;
	private Point mVelocity;
	
	private int mClickCounter;
	
	private Instrumentation mInstrumentation;
	
	public class SystemOverlayBinder extends Binder {
		SystemOverlay getService() {
			return SystemOverlay.this;
		}
	}
	
	private final IBinder mBinder = new SystemOverlayBinder();
	
	
	// create the animation timer
	private TimerTask mAnimationTimerTask = new TimerTask() {
		@Override
		public void run() {
			synchronized(SystemOverlay.this) {
				if(mVelocity.x != 0 || mVelocity.y != 0 || mClickCounter != 0) mView.postInvalidate();
				
				if(mPosition.x < 0) {
					mPosition.x = 0;
					mVelocity.x = 0;
				}
				else if(mPosition.x >= mSize.x) {
					mPosition.x = mSize.x-1;
					mVelocity.x = 0;
				}
				
				if(mPosition.y < 0) {
					mPosition.y = 0;
					mVelocity.y = 0;
				}
				else if(mPosition.y >= mSize.y) {
					mPosition.y = mSize.y - 1;
					mVelocity.y = 0;
				}
				
				mPosition.x += mVelocity.x;
				mPosition.y += mVelocity.y;
				
				if(mClickCounter > 0) {
					mClickCounter--;
				}
				
			}
		}
    };
    private Timer mAnimationTimer;
	
	private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
	    @Override
	    public void onManagerConnected(int status) {
	        switch (status) {
	            case LoaderCallbackInterface.SUCCESS:
	            {
	                Log.i(TAG, "OpenCV loaded successfully");
	                
	                GestureSensor.loadLibrary();
	                
	                // more initialization steps go here.
	                mGestureSensor.start();
	                mMicrophoneSensor.start();
	                
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
        
        mGestureSensor = new GestureSensor(this);
        mMicrophoneSensor = new MicrophoneClickSensor();
        mAccelerometerSensor = new AccelerometerClickSensor(this);
        
        mGestureSensor.addGestureListener(this);
        mGestureSensor.addClickListener(this);
        mMicrophoneSensor.addClickListener(this);
        mAccelerometerSensor.addClickListener(this);
        
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this, mLoaderCallback);
        mView = new OverlayView(this);
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
        
        mSize = new Point();
        wm.getDefaultDisplay().getSize(mSize);
        
        mPosition = new Point(mSize.x / 2, mSize.y / 2);
        mVelocity = new Point(0, 0);
        
        mAnimationTimer = new Timer();
        mAnimationTimer.schedule(mAnimationTimerTask, 0, MILLISECONDS_PER_FRAME);
        
        mClickCounter = 0;
        
        mInstrumentation = new Instrumentation();
    }

    @Override
    public synchronized void onDestroy() {
        super.onDestroy();
        mAnimationTimer.cancel();
        if(mView != null)
        {
            ((WindowManager) getSystemService(WINDOW_SERVICE)).removeView(mView);
            mView = null;
        }
        mGestureSensor.stop();
        mMicrophoneSensor.stop();
    }
    
	class OverlayView extends ViewGroup {
    	private Paint mNormalPaint;
    	private Paint mClickPaint;

	    public OverlayView(Context context) {
	    	super(context);

	    	mNormalPaint = new Paint();
	    	mNormalPaint.setAntiAlias(true);
	    	mNormalPaint.setARGB(255, 255, 0, 0);
	    	
	    	mClickPaint = new Paint();
	    	mClickPaint.setAntiAlias(true);
	    	mClickPaint.setARGB(255, 0, 255, 0);
	    }

	    @Override
	    protected void onDraw(Canvas canvas) {
	    	synchronized(SystemOverlay.this) {
	    		super.onDraw(canvas);
	    		if(mClickCounter == 0)
	    			canvas.drawCircle(mPosition.x, mPosition.y, CURSOR_RADIUS, mNormalPaint);
	    		else 
	    			canvas.drawCircle(mPosition.x, mPosition.y, CURSOR_RADIUS, mClickPaint);
	    	}
	    }

	    @Override
	    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
	    	if(changed) {
		    	mSize.x = right - left; mSize.y = bottom - top;
		        mPosition.x = mSize.x / 2; mPosition.y = mSize.y / 2;
		        mVelocity.x = mVelocity.y = 0;
	    	}
	    }
    }

	@Override
	public synchronized void onGestureUp(GestureSensor caller) {
		if(mClickCounter == 0) {
			mVelocity.x = 0;
			if(mVelocity.y > 0)
				mVelocity.y = 0;
			else
				mVelocity.y -= 1;
		}
	}

	@Override
	public synchronized void onGestureDown(GestureSensor caller) {
		if(mClickCounter == 0) {
			mVelocity.x = 0;
			if(mVelocity.y < 0)
				mVelocity.y = 0;
			else
				mVelocity.y += 1;
		}
	}

	@Override
	public synchronized void onGestureLeft(GestureSensor caller) {
		if(mClickCounter == 0) {
			mVelocity.y = 0;
			if(mVelocity.x > 0)
				mVelocity.x = 0;
			else
				mVelocity.x -= 1;
		}
	}

	@Override
	public synchronized void onGestureRight(GestureSensor caller) {
		if(mClickCounter == 0) {
			mVelocity.y = 0;
			if(mVelocity.x < 0)
				mVelocity.x = 0;
			else
				mVelocity.x += 1;
		}
	}

	@Override
	public void onSensorClick(ClickSensor caller) {
		mClickCounter = NUM_CLICK_FRAMES;
		mVelocity.x = mVelocity.y = 0;
		
		final Point screenPos = new Point(mPosition.x, mPosition.y);
		int [] screenCoords = new int[2];
		mView.getLocationOnScreen(screenCoords);
		
		screenPos.x += screenCoords[0];
		screenPos.y += screenCoords[1];
		
		// run the touch event
		new Thread() {
			@Override
			public void run() {
				MotionEvent downAction = MotionEvent.obtain(
						SystemClock.uptimeMillis(), SystemClock.uptimeMillis(),
						MotionEvent.ACTION_DOWN, screenPos.x, screenPos.y, 0);
				MotionEvent upAction = MotionEvent.obtain(
						SystemClock.uptimeMillis(), SystemClock.uptimeMillis(),
						MotionEvent.ACTION_UP, screenPos.x, screenPos.y, 0);
				
				mInstrumentation.sendPointerSync(downAction);
				mInstrumentation.sendPointerSync(upAction);
				
				downAction.recycle();
				upAction.recycle();
			}
		}.start();
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
