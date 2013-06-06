package edu.washington.cs.touchfreelibraryusertesting;

import edu.washington.cs.touchfreelibrary.sensors.CameraGestureSensor;

import android.graphics.Point;
import android.view.View;
import android.view.ViewGroup;

import edu.washington.cs.touchfreelibrary.sensors.ClickSensor;

public abstract class AbstractTestingView
		extends ViewGroup
		implements CameraGestureSensor.Listener, ClickSensor.Listener
{
	protected TestingActivity mActivity;
	
	public AbstractTestingView(TestingActivity activity) {
		super(activity);
		mActivity = activity;
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
	}

	public void startView() {
		mActivity.addContentView(this, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
		bringToFront();
		setVisibility(View.VISIBLE);
		setWillNotDraw(false);
	}
	
	public void stopView() {
		ViewGroup vg = (ViewGroup)(getParent());
		if(vg != null) {
			vg.removeView(this);
		}
	}
	
	protected Point calculateLinearSplinePosition(Point p1, Point p2, int time, int steps) {
		double percentDone = (double)time / (double)steps;
		
		double s = 3 * percentDone * percentDone - 2 * percentDone * percentDone * percentDone;
		return new Point((int)((1 - s) * p1.x + s * p2.x), (int)((1 - s) * p1.y + s * p2.y));
	}
	
	// calculates the point in a spline based on a radial move.
	// Angles in radians
	protected Point calculateRadialSplinePosition(Point center, int radius, double startAngle, double endAngle, int time, int steps) {
		double percentDone = (double)time / (double)steps;
		
		double s = 3 * percentDone * percentDone - 2 * percentDone * percentDone * percentDone;
		double angle = (1 - s) * startAngle + s * endAngle;
		return new Point((int)(center.x + radius * Math.cos(angle)), (int)(center.y + radius * Math.sin(angle)));
	}
	
	protected void goToNextScreen() {
		mActivity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				mActivity.goToNextScreen();
			}
		});
	}
	
	@Override
	public void onGestureUp(CameraGestureSensor caller, long gestureLength) {
	}

	@Override
	public void onGestureDown(CameraGestureSensor caller, long gestureLength) {
	}

	@Override
	public void onGestureLeft(CameraGestureSensor caller, long gestureLength) {
	}

	@Override
	public void onGestureRight(CameraGestureSensor caller, long gestureLength) {
	}
	
	@Override
	public void onSensorClick(ClickSensor caller) {
	}
}
