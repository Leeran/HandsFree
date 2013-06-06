package edu.washington.cs.touchfreelibraryusertesting;

import android.graphics.Color;
import android.hardware.Camera.Size;
import edu.washington.cs.touchfreelibrary.sensors.CameraGestureSensor;
import edu.washington.cs.touchfreelibrary.touchemulation.GestureCursorController;

public class Demo7_CursorIntro extends MessageScreen {

	GestureCursorController mCursor;
	CameraGestureSensor mGestureSensor;
	
	public Demo7_CursorIntro(TestingActivity activity, CameraGestureSensor gestureSensor) {
		super(activity, activity.getString(R.string.cursor_intro), ContinueType.Clap);
		mCursor = new GestureCursorController(activity);
		mGestureSensor = gestureSensor;
		
		setTextSize(42.0f);
	}
	
	@Override
	public void startView() {
		super.startView();
		mGestureSensor.addGestureListener(mCursor);
		mCursor.attachToActivity(mActivity);
		mCursor.start();
	}

	public void stopView() {
		super.stopView();
		mCursor.removeFromParent();
		mGestureSensor.removeGestureListener(mCursor);
		mCursor.stop();
	}
}
