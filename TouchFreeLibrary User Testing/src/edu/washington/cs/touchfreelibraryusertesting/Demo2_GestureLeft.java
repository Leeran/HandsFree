package edu.washington.cs.touchfreelibraryusertesting;

import edu.washington.cs.touchfreelibrary.sensors.CameraGestureSensor;

public class Demo2_GestureLeft extends Demo1_GestureRight {
	public Demo2_GestureLeft(TestingActivity activity) {
		super(activity);
		mAngle1 = Math.PI;
		mAngle2 = 0;
		mMessages = new String[4];
		mMessages[0] = "Now repeat the same gesture, only this time to the left.";
		mMessages[1] = "Good. Let's do the counter to 3 again, only now a gesture to the left adds 1, while a gesture to the right subtracts";
		mMessages[2] = "Nice";
		mMessages[3] = "Very good. One more swipe to the left and we'll try some vertical maneuvers.";
		mIntroduceCounterMessage = 1;
	}
	
	@Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
    	super.onLayout(changed, left, top, right, bottom);
    	if(changed) {
	    	int temp = mFingerDest1.x;
	    	mFingerDest1.x = mFingerDest2.x;
	    	mFingerDest2.x = temp;
    	}
	}
	
	

	@Override
	public void onGestureLeft(CameraGestureSensor caller, long gestureLength) {
		goodGesture();
	}
	
	@Override
	public void onGestureRight(CameraGestureSensor caller, long gestureLength) {
    	badGesture();
	}
}
