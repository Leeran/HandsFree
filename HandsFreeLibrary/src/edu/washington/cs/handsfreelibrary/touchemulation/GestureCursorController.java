package edu.washington.cs.handsfreelibrary.touchemulation;

import java.util.Timer;
import java.util.TimerTask;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
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
import edu.washington.cs.handsfreelibrary.sensors.AccelerometerClickSensor;
import edu.washington.cs.handsfreelibrary.sensors.ClickSensor;
import edu.washington.cs.handsfreelibrary.sensors.ClickSensorListener;
import edu.washington.cs.handsfreelibrary.sensors.GestureSensor;
import edu.washington.cs.handsfreelibrary.sensors.MicrophoneClickSensor;

public class GestureCursorController implements GestureSensor.Listener, ClickSensorListener {
	protected static final String TAG = "GestureCursorController";
	
	private static final int CURSOR_RADIUS = 20;
	
	private static final int MILLISECONDS_PER_FRAME = 7;
	private static final int NUM_CLICK_FRAMES = 50;
	
	private GestureCursorView mView;
	
	private Point mSize;
	private Point mPosition;
	private Point mVelocity;
	
	private boolean mIsRunning;
	
	private int mClickCounter;
	
	private Instrumentation mInstrumentation;
	
	
	// create the animation timer
	private TimerTask mAnimationTimerTask = new TimerTask() {
		@Override
		public void run() {
			synchronized(GestureCursorController.this) {
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
	
    public GestureCursorController(Context context) {
        mView = new GestureCursorView(context);
        
        mSize = new Point(0, 0);
        
        mPosition = new Point(0, 0);
        mVelocity = new Point(0, 0);
        
        mAnimationTimer = new Timer();
        mAnimationTimer.schedule(mAnimationTimerTask, 0, MILLISECONDS_PER_FRAME);
        
        mClickCounter = 0;
        
        mIsRunning = false;
        
        mInstrumentation = new Instrumentation();
    }
    
    public synchronized void start() {
    	mIsRunning = true;
    	mAnimationTimer = new Timer();
        mAnimationTimer.schedule(mAnimationTimerTask, 0, MILLISECONDS_PER_FRAME);
        
        mPosition = new Point(mSize.x / 2, mSize.y / 2);
        mVelocity = new Point(0, 0);
        mClickCounter = 0;
    }

    public synchronized void stop() {
    	if(mIsRunning) {
			mAnimationTimer.cancel();
			mIsRunning = false;
    	}
    }
    
	private class GestureCursorView extends ViewGroup {
    	private Paint mNormalPaint;
    	private Paint mClickPaint;
    	
    	public void setIdleColor(int c) {
    		mNormalPaint.setColor(c);
    	}
    	
    	public void setClickColor(int c) {
    		mClickPaint.setColor(c);
    	}

	    public GestureCursorView(Context context) {
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
}
