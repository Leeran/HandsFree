package edu.washington.cs.handsfreelibrary.touchemulation;

import java.util.Timer;

import android.app.Instrumentation;
import android.graphics.Point;
import android.os.SystemClock;
import android.view.MotionEvent;
import edu.washington.cs.handsfreelibrary.sensors.GestureSensor;

/**
 * Class <code>GestureScroller</code> emulates the swipe-to-scroll touch-screen
 * gesture with {@link GestureSensor}'s gestures. Users of this class must set
 * <code>this</code> as a Listener to an external <code>GestureSensor</code>
 * class.
 * @author Leeran Raphaely <leeran.raphaely@gmail.com>
 */
public class GestureScroller implements GestureSensor.Listener {
	// Points used for horizontal scroll:
	private Point mLeftPoint;
	private Point mRightPoint;
	
	// Points used for vertical scroll:
	private Point mTopPoint;
	private Point mBottomPoint;
	
	private boolean mVerticalScrollEnabled;
	private boolean mHorizontalScrollEnabled;
	
	private Instrumentation mInstrumentation;
	
	private boolean mIsRunning;
	
	/**
	 * Creates a new instance of <code>GestureScroller</code>. In order to function, this must be
	 * set as a listener for a {@link GestureSensor} object. Also, {@link start} must be called to
	 * activate this.
	 */
	public GestureScroller() {
		mLeftPoint = new Point(-1, -1);
		mRightPoint = new Point(-1, -1);
		mTopPoint = new Point(-1, -1);
		mBottomPoint = new Point(-1, -1);
		
		mVerticalScrollEnabled = true;
		mHorizontalScrollEnabled = true;
		
		mInstrumentation = new Instrumentation();
		mIsRunning = false;
	}
	
	/**
     * Activates this instance of <code>GestureScroller</code>.
     */
    public void start() {
    	mIsRunning = true;
    }

    /**
     * Stops the instance from acting as a cursor, which will no longer be drawn on the view.
     */
    public void stop() {
    	mIsRunning = false;
    }
    
    /**
     * Get whether or not this instance is currently activated
     * @return true if this is running, false otherwise.
     */
    public boolean isRunning() {
    	return mIsRunning;
    }
	
	/**
	 * Set whether this scroller responds to vertical scrolling commands.
	 * @param enabled true if vertical scrolling should be enabled, false otherwise.
	 */
	public void setVerticalScrollEnabled(boolean enabled) {
		mVerticalScrollEnabled = enabled;
	}
	
	/**
	 * Get whether or not this scroller responds to vertical scrolling commands.
	 * @return true if vertical scrolling is enabled, false otherwise.
	 */
	public boolean getVerticalScrollEnabled() {
		return mVerticalScrollEnabled;
	}
	
	/**
	 * Set whether this scroller responds to horizontal scrolling commands.
	 * @param enabled true if horizontal scrolling should be enabled, false otherwise.
	 */
	public void setHorizontalScrollEnabled(boolean enabled) {
		mHorizontalScrollEnabled = enabled;
	}
	
	/**
	 * Get whether or not this scroller responds to horizontal scrolling commands.
	 * @return true if horizontal scrolling is enabled, false otherwise.
	 */
	public boolean getHorizontalScrollEnabled() {
		return mHorizontalScrollEnabled;
	}
	
	/**
	 * Sets the position, in screen space, of the left point that is used in horizontal scrolling
	 * @param x the x coordinate, in screen space
	 * @param y the y coordinate, in screen space
	 */
	public void setLeftPosition(int x, int y) {
		mLeftPoint.x = x;
		mLeftPoint.y = y;
	}
	
	/**
	 * Sets the position, in screen space, of the right point that is used in horizontal scrolling
	 * @param x the x coordinate, in screen space
	 * @param y the y coordinate, in screen space
	 */
	public void setRightPosition(int x, int y) {
		mRightPoint.x = x;
		mRightPoint.y = y;
	}
	
	/**
	 * Sets the position, in screen space, of the top point that is used in vertical scrolling
	 * @param x the x coordinate, in screen space
	 * @param y the y coordinate, in screen space
	 */
	public void setTopPosition(int x, int y) {
		mTopPoint.x = x;
		mTopPoint.y = y;
	}
	
	/**
	 * Sets the position, in screen space, of the bottom point that is used in vertical scrolling
	 * @param x the x coordinate, in screen space
	 * @param y the y coordinate, in screen space
	 */
	public void setBottomPosition(int x, int y) {
		mBottomPoint.x = x;
		mBottomPoint.y = y;
	}

	@Override
	public void onGestureUp(GestureSensor caller, long gestureLength) {
		if(mVerticalScrollEnabled && mIsRunning && mTopPoint.x >= 0 && mBottomPoint.x >= 0)
			sendCursorDragEvent(mBottomPoint, mTopPoint, 20);
	}

	@Override
	public void onGestureDown(GestureSensor caller, long gestureLength) {
		if(mVerticalScrollEnabled && mIsRunning && mTopPoint.x >= 0 && mBottomPoint.x >= 0)
			sendCursorDragEvent(mTopPoint, mBottomPoint, 20);
	}

	@Override
	public void onGestureLeft(GestureSensor caller, long gestureLength) {
		if(mHorizontalScrollEnabled && mIsRunning && mLeftPoint.x >= 0 && mRightPoint.x >= 0)
			sendCursorDragEvent(mRightPoint, mLeftPoint, 20);
	}

	@Override
	public void onGestureRight(GestureSensor caller, long gestureLength) {
		if(mHorizontalScrollEnabled && mIsRunning && mLeftPoint.x >= 0 && mRightPoint.x >= 0)
			sendCursorDragEvent(mLeftPoint, mRightPoint, 20);
	}

	// invokes a fake drag using instrumentation. p1 and p2 are in screen space
	private void sendCursorDragEvent(final Point p1, final Point p2, final int stepCount) {
		// run the touch event
		new Thread() {
			@Override
			public void run() {
				float x = p1.x;
				float y = p1.y;
				
				long downTime = SystemClock.uptimeMillis();
				
				float xStep = (float)(p2.x - p1.x) / (float)stepCount;
				float yStep = (float)(p2.y - p1.y) / (float)stepCount;
				
				MotionEvent event = null;
				
				//synchronized(GestureScroller.this) {
					try {
						event = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, x, y, 0);
						mInstrumentation.sendPointerSync(event);
						mInstrumentation.waitForIdleSync();
						
						for (int i = 0; i < stepCount; i++) {
							x += xStep;
							y += yStep;
							
							event = MotionEvent.obtain(downTime, SystemClock.uptimeMillis(), MotionEvent.ACTION_MOVE, x, y, 0);
							mInstrumentation.sendPointerSync(event);
							mInstrumentation.waitForIdleSync();
						}
						
						event = MotionEvent.obtain(downTime, SystemClock.uptimeMillis(), MotionEvent.ACTION_UP, x, y, 0);
						mInstrumentation.sendPointerSync(event);
						mInstrumentation.waitForIdleSync();
					} catch (SecurityException e) {
						// security exception occurred, but we can pretty much ignore it.
					} finally {
						if(event != null) event.recycle();
					}
				//}
			}
		}.start();
	}
}
