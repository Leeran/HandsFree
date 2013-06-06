package edu.washington.cs.touchfreelibraryusertesting;

import java.util.Timer;
import java.util.TimerTask;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.widget.Toast;
import edu.washington.cs.touchfreelibrary.sensors.CameraGestureSensor;
import edu.washington.cs.touchfreelibrary.sensors.ClickSensor;

public class Demo8_CursorPractice extends CursorActivity {
	private Paint mTextPaint1;
	private Paint mTextPaint2;
	
	private String mMessage1 = "Practice Round";
	private String mMessage2 = "Tap screen when finished practicing";
	
	private Timer mInstructionsTimer;
	private class InstructionsTimerTask extends TimerTask {
		@Override
		public void run() {
			mInstructions.show();
		}
	}
	
	private Toast mInstructions;

	public Demo8_CursorPractice(
			TestingActivity activity,
			CameraGestureSensor gestureSensor,
			ClickSensor clickSensor) {
		super(activity, gestureSensor, clickSensor);
		
		mTextPaint1 = new Paint();
		mTextPaint1.setARGB(255, 0, 0, 0);
		mTextPaint1.setTextSize(60);
		mTextPaint1.setTextAlign(Align.CENTER);
		
		mTextPaint2 = new Paint();
		mTextPaint2.setARGB(255, 255, 0, 0);
		mTextPaint2.setTextSize(40);
		mTextPaint2.setTextAlign(Align.CENTER);
	}
	
	@Override
	public void startView() {
		super.startView();
		mIsRunning = true;
		mInstructions = Toast.makeText(getContext()	, "Instructions: Move cursor into box, then clap.", Toast.LENGTH_LONG);
		
		mInstructionsTimer = new Timer();
		mInstructionsTimer.schedule(new InstructionsTimerTask(), 0, 1000);
		
	}
	
	@Override
	public void stopView() {
		super.stopView();
		mInstructionsTimer.cancel();
		mInstructions.cancel();
	}

	@Override
    protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		
		if(mRightScore == 1) {
			mInstructionsTimer.cancel();
			mInstructions.cancel();
		}
		
		canvas.drawText(mMessage1, canvas.getWidth() / 2, canvas.getHeight() - 200.0f, mTextPaint1);
		canvas.drawText(mMessage2, canvas.getWidth() / 2, canvas.getHeight() - 100.0f, mTextPaint2);
	}
	
}
