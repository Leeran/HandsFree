package edu.washington.cs.handsfreelibrary.touchemulation;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Instrumentation;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import edu.washington.cs.handsfreelibrary.sensors.ClickSensor;
import edu.washington.cs.handsfreelibrary.sensors.ClickSensorListener;
import edu.washington.cs.handsfreelibrary.sensors.GestureSensor;

/**
 * <p>Class <code>GestureCursorController</code> can be used to create the visual and functionality of a
 * cursor that is controlled via gestures. To use this class, one must create a {@link GestureSensor}
 * object and a {@link ClickSensor} object and set <code>this</code> as a listener to both.</p>
 * 
 * <p>For an object of this class to be functional, the view returned by {@link getView} must attached to
 * a parent object and drawn on the screen somewhere. Clicks will be registered to whatever is below the
 * view.</p>
 * 
 * @author Leeran Raphaely <leeran.raphaely@gmail.com>
 *
 */
public class GestureCursorController implements GestureSensor.Listener, ClickSensorListener {
	protected static final String TAG = "GestureCursorController";
	
	private static final int DEFAULT_CURSOR_RADIUS = 20;
	
	private static final int MILLISECONDS_PER_FRAME = 7;
	private static final int NUM_CLICK_FRAMES = 50;
	
	private GestureCursorView mView;
	
	private Point mSize;
	private Point mPosition;
	private Point mVelocity;
	
	private int mCursorRadius;
	
	private boolean mIsRunning;
	
	private int mClickCounter;
	
	private Instrumentation mInstrumentation;
	
	// create the animation timer
	private TimerTask mAnimationTimerTask = new TimerTask() {
		@Override
		public void run() {
			synchronized(GestureCursorController.this) {
				if(mVelocity.x != 0 || mVelocity.y != 0 || mClickCounter != 0) mView.postInvalidate();
				
				if(mSize.x == 0 || mSize.y == 0) {
					mPosition.x = mPosition.y = 0;
					mVelocity.x = mVelocity.y = 0;
				} else {
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
				}
				
				if(mClickCounter > 0) {
					mClickCounter--;
				}
				
			}
		}
    };
    private Timer mAnimationTimer;
	
    /**
     * Creates a new <code>GestureCursorController</code> object.
     * @param context A context from the application.
     */
    public GestureCursorController(Context context) {
        mView = new GestureCursorView(context);
        
        mSize = new Point(0, 0);
        
        mPosition = new Point(0, 0);
        mVelocity = new Point(0, 0);
        
        mClickCounter = 0;
        
        mCursorRadius = DEFAULT_CURSOR_RADIUS;
        
        mIsRunning = false;
        
        mInstrumentation = new Instrumentation();
    }
    
    /**
     * Starts this instance of <code>GestureCursorController</code>, meaning the cursor will be drawn
     * onto the view, and clicks will be sent to the application below.
     */
    public synchronized void start() {
    	if(!mIsRunning) {
	    	mIsRunning = true;
	    	mAnimationTimer = new Timer();
	        mAnimationTimer.schedule(mAnimationTimerTask, 0, MILLISECONDS_PER_FRAME);
	        
	        mPosition = new Point(mSize.x / 2, mSize.y / 2);
	        mVelocity = new Point(0, 0);
	        mClickCounter = 0;
    	}
    }

    /**
     * Stops the instance from acting as a cursor, which will no longer be drawn on the view.
     */
    public synchronized void stop() {
    	if(mIsRunning) {
			mAnimationTimer.cancel();
			mIsRunning = false;
    	}
    }
    
    /**
     * Set the color of the cursor when not clicking.
     * @param c the color-int that the cursor will be set to
     */
    public void setIdleColor(int c) {
		mView.mNormalPaint.setColor(c);
	}
	
    /**
     * Set the color of the cursor when clicking.
     * @param c the color-int that the cursor will be set to when clicking
     */
	public void setClickColor(int c) {
		mView.mClickPaint.setColor(c);
	}
	
	/**
	 * Set the radius of the cursor
	 * @param r the radius of the cursor
	 */
	public void setCursorRadius(int r) {
		mCursorRadius = r;
	}
	
	/**
	 * @return the view that contains the cursor (clicking won't work unless the view is attached to something)
	 */
	public View getView() {
		return mView;
	}
    
	private class GestureCursorView extends ViewGroup {
    	public Paint mNormalPaint;
    	public Paint mClickPaint;

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
	    	synchronized(GestureCursorController.this) {
	    		super.onDraw(canvas);
	    		if(mClickCounter == 0)
	    			canvas.drawCircle(mPosition.x, mPosition.y, mCursorRadius, mNormalPaint);
	    		else 
	    			canvas.drawCircle(mPosition.x, mPosition.y, mCursorRadius, mClickPaint);
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
	    
	    @Override
	    public boolean onInterceptTouchEvent (MotionEvent ev) {
	    	onTouchEvent(ev);
	    	return false;
	    }
    }

	@Override
	public synchronized void onGestureUp(GestureSensor caller, long gestureLength) {
		if(mClickCounter == 0) {
			mVelocity.x = 0;
			if(mVelocity.y > 0)
				mVelocity.y = 0;
			else
				mVelocity.y -= 1;
		}
	}

	@Override
	public synchronized void onGestureDown(GestureSensor caller, long gestureLength) {
		if(mClickCounter == 0) {
			mVelocity.x = 0;
			if(mVelocity.y < 0)
				mVelocity.y = 0;
			else
				mVelocity.y += 1;
		}
	}

	@Override
	public synchronized void onGestureLeft(GestureSensor caller, long gestureLength) {
		if(mClickCounter == 0) {
			mVelocity.y = 0;
			if(mVelocity.x > 0)
				mVelocity.x = 0;
			else
				mVelocity.x -= 1;
		}
	}

	@Override
	public synchronized void onGestureRight(GestureSensor caller, long gestureLength) {
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
				
				try {
					mInstrumentation.sendPointerSync(downAction);
					mInstrumentation.sendPointerSync(upAction);
					mClickCounter = NUM_CLICK_FRAMES;
				} catch (SecurityException e) {
					// security exception occurred, but we can pretty much ignore it.
				}
				
				downAction.recycle();
				upAction.recycle();
			}
		}.start();
	}
}
