package edu.washington.cs.touchfreelibraryusertesting;

import android.graphics.Canvas;
import android.graphics.Paint.Align;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.view.View;
import android.view.View.OnClickListener;

import edu.washington.cs.touchfreelibrary.sensors.ClickSensor;

public class MessageScreen extends AbstractTestingView implements OnClickListener {
	private static final long MINIMUM_READING_TIME = 1000;
	
	public enum ContinueType {
		Tap, Clap, None
	}
	
	private StaticLayout mTextBox;
	private TextPaint mTextPaint;
	private TextPaint mTextPaintRed;
	
	private static final int MARGINS = 40;
	
	private String mMessage;
	
	private ContinueType mContinueType;
	private String mContinueMessage;
	
	private long mStartingTime;
	
	public MessageScreen(TestingActivity activity, String message, ContinueType continueType) {
		super(activity);
		
		setOnClickListener(this);
		
		mMessage = message;
		
		mTextBox = null;
		
		mTextPaint = new TextPaint();
		mTextPaint.setARGB(255, 0, 0, 0);
		mTextPaint.setTextSize(50);
		
		mTextPaintRed = new TextPaint();
		mTextPaintRed.setARGB(255, 255, 0, 0);
		mTextPaintRed.setTextSize(50);
		mTextPaintRed.setTextAlign(Align.CENTER);
		
		mContinueType = continueType;
		if(mContinueType == ContinueType.Tap) {
			mContinueMessage = getContext().getString(R.string.tap_anywhere);
		} else if(mContinueType == ContinueType.Clap)
			mContinueMessage = getContext().getString(R.string.clap_continue);
		else
			mContinueMessage = "";
	}
	
	@Override
	public void startView() {
		super.startView();
		mStartingTime = System.currentTimeMillis();
	}
	
	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		mTextBox = new StaticLayout(
				mMessage,
				mTextPaint,
				right - left - MARGINS * 2,
				Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
	}

	@Override
    protected void onDraw(Canvas canvas) {
		canvas.translate(MARGINS, MARGINS);
		
		mTextBox.draw(canvas);
		
		canvas.translate(-MARGINS, mTextBox.getHeight() + 60);
		
		canvas.drawText(mContinueMessage, canvas.getWidth() / 2, 0.0f, mTextPaintRed);
    }
	
	public void setTextSize(float size) {
		mTextPaint.setTextSize(size);
		mTextPaintRed.setTextSize(size);
		postInvalidate();
	}

	@Override
	public void onClick(View v) {
		if(mContinueType == ContinueType.Tap)
			goToNextScreen();
	}

	@Override
	public void onSensorClick(ClickSensor caller) {
		if(mContinueType == ContinueType.Clap && System.currentTimeMillis() - mStartingTime > MINIMUM_READING_TIME)
			goToNextScreen();
	}
}
