package edu.washington.cs.touchfreelibraryusertesting;

import java.util.Timer;
import java.util.TimerTask;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.view.View;

public class Demo6_TestGestures extends GestureDemos {
	private static final int NUMBER_OF_ROUNDS = 50;
	private long mUserId;
	
	private Paint mTextPaint;
	private Paint mSubTextPaint;
	
	private boolean mStarted;
	private String [] mMessages = {
			"Tap to begin",
			"3 ... ",
			"3 ... 2 ...",
			"3 ... 2 ... 1 ...",
			"Recording Results",
			"Done"
	};
	private Timer mMessageTimer;
	private TimerTask mMessageTimerTask;
	
	private int mCurrentMessage;
	
	public Demo6_TestGestures(TestingActivity activity) {
		super(activity);
		
		mTextPaint = new Paint();
		mTextPaint.setARGB(255, 0, 0, 0);
		mTextPaint.setTextSize(60);
		mTextPaint.setTextAlign(Align.CENTER);
		
		mSubTextPaint = new Paint();
		mSubTextPaint.setARGB(255, 0, 0, 0);
		mSubTextPaint.setTextSize(40.0f);
		mSubTextPaint.setTextAlign(Align.CENTER);
	}
	
	@Override
	public void startView() {
		super.startView();
		mUserId = mActivity.getUserId();
		mCurrentMessage = 0;
		
		mStarted = false;
	}
	
	@Override
	public void stopView() {
		super.stopView();
		if(mMessageTimer != null)
			mMessageTimer.cancel();
	}

	@Override
    protected void onDraw(Canvas canvas) {
		if(mCurrentMessage >= mMessages.length - 2)
			super.onDraw(canvas);
		
		canvas.drawText(mMessages[mCurrentMessage], canvas.getWidth() / 2, canvas.getHeight() - 200.0f, mTextPaint);
		String message2;
		if(mCurrentMessage < mMessages.length - 2)
			message2 = "Don't worry about speed.";
		else if(mCurrentMessage == mMessages.length - 2)
			message2 = "Rounds left: " + (NUMBER_OF_ROUNDS - mRightScore - mWrongScore);
		else
			message2 = getContext().getString(R.string.tap_anywhere);
		canvas.drawText(message2, canvas.getWidth() / 2, canvas.getHeight() - 100.0f, mSubTextPaint);
	}
	
	@Override
	public void onClick(View v) {
		if(!mStarted) {
			mStarted = true;
			mMessageTimer = new Timer();
			mMessageTimerTask = new TimerTask() {
				@Override
				public void run() {
					if(mCurrentMessage < mMessages.length - 2) {
						mCurrentMessage++;
						setRandomDirection();
					}
					if(mCurrentMessage == mMessages.length - 2) {
						mMessageTimer.cancel();
						mIsRunning = true;
					}
				}
			};
			mMessageTimer.schedule(mMessageTimerTask, 0, 1000);
		} else if(mCurrentMessage == mMessages.length - 1) {
			goToNextScreen();
		}
	}
	
	@Override
	protected void setRandomDirection() {
		if(mRightScore + mWrongScore >= NUMBER_OF_ROUNDS) {
			mCurrentMessage++;
			mIsRunning = false;
			writeResultsToFile("R" + mUserId + ".txt");
			postInvalidate();
		} else {
			super.setRandomDirection();
		}
	}
}
