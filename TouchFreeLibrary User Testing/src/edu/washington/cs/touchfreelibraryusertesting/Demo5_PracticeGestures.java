package edu.washington.cs.touchfreelibraryusertesting;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Align;

public class Demo5_PracticeGestures extends GestureDemos {

	private Paint mTextPaint1;
	private Paint mTextPaint2;
	
	private String mMessage1 = "Practice Round";
	private String mMessage2 = "Tap Anywhere to Continue";
	
	public Demo5_PracticeGestures(TestingActivity activity) {
		super(activity);
		
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
	}

	@Override
    protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		
		canvas.drawText(mMessage1, canvas.getWidth() / 2, canvas.getHeight() - 200.0f, mTextPaint1);
		canvas.drawText(mMessage2, canvas.getWidth() / 2, canvas.getHeight() - 100.0f, mTextPaint2);
	}
}
