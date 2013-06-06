package edu.washington.cs.touchfreelibraryusertesting;

import java.util.Timer;
import java.util.TimerTask;

import edu.washington.cs.touchfreelibrary.sensors.CameraGestureSensor;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Point;
import android.graphics.Rect;
import android.widget.Toast;

public class Demo1_GestureRight extends AbstractTestingView {
	// 0  - 30  - finger moving right
	// 30 - 100 
	private static final int FRAME_FINISH_FINGER_TO_RIGHT = 70;
	private static final int FRAME_FINISH_FINGER_WAIT1 = 50 + FRAME_FINISH_FINGER_TO_RIGHT;
	private static final int FRAME_FINISH_FINGER_CIRCLE = 100 + FRAME_FINISH_FINGER_WAIT1;
	private static final int FRAME_FINISH_FINGER_WAIT2 = 100 + FRAME_FINISH_FINGER_CIRCLE;
	
	protected String[] mMessages = {
		"Try swiping your finger across the camera in the same style as the info graphic.",
		"Very good! Remember to keep your finger out of the view of the camera when not trying to trigger a gesture. Swipe to the right one more time.",
		"Let's try to get the counter on the screen to get to 3. A gesture to the right will add a point, while a gesture to the left will subtract a point.",
		"Very good. Get the counter to 3, and we'll work on left gestures.",
		"Excellent work. One more swipe to the right and we'll begin the next section."
	};
	
	private int mAnimationCounter;
	private Timer mAnimationTimer;
	private TimerTask mAnimationTimerTask;
	protected TimerTask mMessageTimerTask;
	
	private Bitmap mPhone;
	protected Bitmap mFinger;
	
	protected Rect mPhoneDestRect;
	private Rect mFingerDestRect;
	
	protected Point mFingerDest1, mFingerDest2;
	protected Point mCircleCenter;
	protected int mRadius;
	
	protected double mAngle1;
	protected double mAngle2;
	
	protected int mCurrentMessageID;
	protected int mCurrentNumberSwipes;
	
	protected int mIntroduceCounterMessage;
	
	private Toast mCurrentToast;
	
	private Paint mTextPaint;
	
	private boolean mEnabled;

	public Demo1_GestureRight(TestingActivity activity) {
		super(activity);
		mPhone = BitmapFactory.decodeResource(getResources(), R.drawable.phone);
		mFinger  = BitmapFactory.decodeResource(getResources(), R.drawable.vert_finger);
		mPhoneDestRect = new Rect();
		mFingerDestRect = new Rect();
		mFingerDest1 = new Point();
		mFingerDest2 = new Point();
		mCircleCenter = new Point();
		mRadius = 0;
		
		mAngle1 = 0;
		mAngle2 = Math.PI;
		
		mTextPaint = new Paint();
		mTextPaint.setARGB(255, 255, 0, 0);
		mTextPaint.setTextSize(60);
		mTextPaint.setTextAlign(Align.CENTER);
		mCurrentToast = null;
		mIntroduceCounterMessage = 2;
	}
	
	@Override
	public synchronized void startView() {
		super.startView();
		
		mAnimationTimerTask = new TimerTask() {
			@Override
			public void run() {
				onAnimationTimer();
			}
	    };
	    
	    mMessageTimerTask = new TimerTask() {
			@Override
			public void run() {
				synchronized(Demo1_GestureRight.this) {
					mCurrentToast.show();
				}
			}
	    };
		
	    displayMessage();
	    
		mAnimationCounter = -100;
		mAnimationTimer = new Timer();
		mAnimationTimer.schedule(mAnimationTimerTask, 0, 10);
		mAnimationTimer.schedule(mMessageTimerTask, 0, 1000);
		
		mCurrentMessageID = 0;
		mCurrentNumberSwipes = 0;
		mEnabled = false;
	}
	
	@Override
	public synchronized void stopView() {
		super.stopView();
		mAnimationTimer.cancel();
		clearMessages();
	}
	
	@Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
    	if(changed) {
    		int phoneHeight = bottom - top;
    		int phoneWidth  = phoneHeight * mPhone.getWidth() / mPhone.getHeight();
    		
    		// get the ratio
    		double destToSrc = (double)phoneHeight / (double)mPhone.getHeight();
    		
	    	mPhoneDestRect.top = 0;
	    	mPhoneDestRect.bottom = phoneHeight;
	    	mPhoneDestRect.left = (right - left) / 2 - phoneWidth / 2;
	    	mPhoneDestRect.right = mPhoneDestRect.left + phoneWidth;
	    	
	    	// figure finger size
	    	int fingerWidth  = (int)(mFinger.getWidth()  * destToSrc * 0.7);
	    	int fingerHeight = (int)(mFinger.getHeight() * destToSrc * 0.7);
	    	mFingerDestRect.top = 0;
	    	mFingerDestRect.left = 0;
	    	mFingerDestRect.right = fingerWidth;
	    	mFingerDestRect.bottom = fingerHeight;
	    	
	    	mFingerDest1.x = (int)(70 * destToSrc);
	    	mFingerDest2.x = right - left - fingerWidth;
	    	
	    	mRadius = (mFingerDest2.x - mFingerDest1.x) / 2;
	    	mCircleCenter.x = mFingerDest1.x + mRadius;
    	}
    }

	@Override
    protected void onDraw(Canvas canvas) {
		canvas.drawBitmap(mPhone, null, mPhoneDestRect, null);
		canvas.drawBitmap(mFinger, null, mFingerDestRect, null);
		
		if(mCurrentMessageID >= mIntroduceCounterMessage) {
			canvas.drawText(
					"" + mCurrentNumberSwipes,
					canvas.getWidth()/2, canvas.getHeight()/2 + 160,
					mTextPaint);
		}
	}
	
	protected synchronized void displayMessage() {
		mActivity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if(mCurrentToast != null) {
					mCurrentToast.cancel();
				}
				mCurrentToast = Toast.makeText(
						getContext(),
						mMessages[mCurrentMessageID],
						Toast.LENGTH_LONG
					);
				mCurrentToast.show();
			}
		});
	}
	
	protected synchronized void clearMessages() {
		mActivity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if(mCurrentToast != null) {
					mCurrentToast.cancel();
					mCurrentToast = null;
				}
			}
		});
	}
	
	protected synchronized void onAnimationTimer() {
		if(mRadius == 0)
			return;
		int oldX = mFingerDestRect.left, oldY = mFingerDestRect.top;
		if(mAnimationCounter < 0) {
			mFingerDestRect.offsetTo(mFingerDest1.x, mFingerDest1.y);
		} else if(mAnimationCounter < FRAME_FINISH_FINGER_TO_RIGHT) {
			Point p = calculateLinearSplinePosition(
					mFingerDest1, mFingerDest2,
					mAnimationCounter,
					FRAME_FINISH_FINGER_TO_RIGHT);
			mFingerDestRect.offsetTo(p.x, p.y);
		} else if(mAnimationCounter < FRAME_FINISH_FINGER_WAIT1) {
			mEnabled = true;
			mFingerDestRect.offsetTo(mFingerDest2.x, mFingerDest2.y);
		} else if(mAnimationCounter < FRAME_FINISH_FINGER_CIRCLE) {
			Point p = calculateRadialSplinePosition(
					mCircleCenter, mRadius, mAngle1, mAngle2,
					mAnimationCounter - FRAME_FINISH_FINGER_WAIT1,
					FRAME_FINISH_FINGER_CIRCLE - FRAME_FINISH_FINGER_WAIT1);
			mFingerDestRect.offsetTo(p.x, p.y);
		} else if(mAnimationCounter < FRAME_FINISH_FINGER_WAIT2) {
			mFingerDestRect.offsetTo(mFingerDest1.x, mFingerDest1.y);
		} else mAnimationCounter = -1;
		mAnimationCounter++;
		
		if(oldX != mFingerDestRect.left || oldY != mFingerDestRect.top)
			Demo1_GestureRight.this.postInvalidate();
	}
	
	protected void goodGesture() {
		if(!mEnabled)
			return;
		
		if(mCurrentMessageID < mIntroduceCounterMessage) {
			mCurrentMessageID++;
			displayMessage();
		}
		else if(mCurrentMessageID == mIntroduceCounterMessage) {
			mCurrentMessageID++;
			displayMessage();
			mMessageTimerTask.cancel();
		}
		else if(mCurrentMessageID == mIntroduceCounterMessage + 1 && mCurrentNumberSwipes == 2) {
			mCurrentMessageID++;
			displayMessage();
			mMessageTimerTask.cancel();
		}
		else if(mCurrentMessageID == mIntroduceCounterMessage + 2) {
			goToNextScreen();
		}
		
		if(mCurrentMessageID > mIntroduceCounterMessage && mCurrentNumberSwipes < 3) {
			mCurrentNumberSwipes++;
			postInvalidate();
		}
	}
	
	protected void badGesture() {
		if(!mEnabled)
			return;
		
		if(mCurrentMessageID > mIntroduceCounterMessage && mCurrentNumberSwipes < 3) {
			mCurrentNumberSwipes--;
			postInvalidate();
		}
	}
    
    @Override
	public void onGestureLeft(CameraGestureSensor caller, long gestureLength) {
    	badGesture();
	}

	@Override
	public void onGestureRight(CameraGestureSensor caller, long gestureLength) {
		goodGesture();
	}
	    
}
