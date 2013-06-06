package edu.washington.cs.touchfreelibraryusertesting;

import android.graphics.BitmapFactory;
import edu.washington.cs.touchfreelibrary.sensors.CameraGestureSensor;

public class Demo4_GestureDown extends Demo1_GestureRight {
	public Demo4_GestureDown(TestingActivity activity) {
		super(activity);
		
		mFinger  = BitmapFactory.decodeResource(getResources(), R.drawable.horz_finger);
		
		mAngle1 = Math.PI;
		mAngle2 = 0;
		mMessages = new String[4];
		mMessages[0] = "Try do the same thing in the opposite direction, gesturing downwards.";
		mMessages[1] = "You know the drill, counter to 3. Now a downward gesture adds 1, while an upwards gesture subtracts 1.";
		mMessages[2] = "A few more should do it!";
		mMessages[3] = "Finally! One more down gesture, and you should just about have it.";
		mIntroduceCounterMessage = 1;
	}
	
	@Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
    	super.onLayout(changed, left, top, right, bottom);
    	if(changed) {
    		mPhoneDestRect.offsetTo(-20, 100);
	    	mFingerDest1.x = right - mFinger.getWidth() + 160;
	    	mFingerDest2.x = mCircleCenter.x = mFingerDest1.x;
	    	mFingerDest1.y = 0;
	    	mFingerDest2.y = 250;
	    	
	    	mCircleCenter.y = (mFingerDest1.y + mFingerDest2.y) / 2;
	    	mRadius = (mFingerDest2.y - mFingerDest1.y) / 2;
	    	
	    	mAngle1 = Math.PI / 2;
	    	mAngle2 = -Math.PI / 2;
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
		badGesture();
	}
	
	@Override
	public void onGestureDown(CameraGestureSensor caller, long gestureLength) {
    	goodGesture();
	}
}
