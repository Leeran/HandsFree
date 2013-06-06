package edu.washington.cs.touchfreelibraryusertesting;

import android.graphics.BitmapFactory;
import edu.washington.cs.touchfreelibrary.sensors.CameraGestureSensor;

public class Demo3_GestureUp extends Demo1_GestureRight {
	public Demo3_GestureUp(TestingActivity activity) {
		super(activity);
		
		mFinger  = BitmapFactory.decodeResource(getResources(), R.drawable.horz_finger);
		
		mAngle1 = Math.PI;
		mAngle2 = 0;
		mMessages = new String[4];
		mMessages[0] = "Try making a gesture upwards, mimicking the on-screen hand.";
		mMessages[1] = "Good. Let's do the counter to 3 again. Now an upward gesture adds 1, while a gesture downwards subtracts 1.";
		mMessages[2] = "Cool";
		mMessages[3] = "Excellent. One more up gesture and we'll finish off with down.";
		mIntroduceCounterMessage = 1;
	}
	
	@Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
    	super.onLayout(changed, left, top, right, bottom);
    	if(changed) {
    		mPhoneDestRect.offsetTo(-20, 100);
	    	mFingerDest2.x = right - mFinger.getWidth() + 160;
	    	mFingerDest1.x = mCircleCenter.x = mFingerDest2.x;
	    	mFingerDest2.y = 0;
	    	mFingerDest1.y = 250;
	    	
	    	mCircleCenter.y = (mFingerDest1.y + mFingerDest2.y) / 2;
	    	mRadius = (mFingerDest2.y - mFingerDest1.y) / 2;
	    	
	    	mAngle1 = Math.PI / 2;
	    	mAngle2 = 3 * Math.PI / 2;
    	}
	}
	
	@Override
	public void onGestureLeft(CameraGestureSensor caller, long gestureLength) {
	}

	@Override
	public void onGestureRight(CameraGestureSensor caller, long gestureLength) {
	}
	

	@Override
	public void onGestureUp(CameraGestureSensor caller, long gestureLength) {
		goodGesture();
	}
	
	@Override
	public void onGestureDown(CameraGestureSensor caller, long gestureLength) {
    	badGesture();
	}
}
