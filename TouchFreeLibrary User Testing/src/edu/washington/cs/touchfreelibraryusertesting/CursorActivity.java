package edu.washington.cs.touchfreelibraryusertesting;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.view.View;
import edu.washington.cs.touchfreelibrary.sensors.CameraGestureSensor;
import edu.washington.cs.touchfreelibrary.sensors.ClickSensor;
import edu.washington.cs.touchfreelibrary.touchemulation.GestureCursorController;

public class CursorActivity extends AbstractTestingView
	implements View.OnClickListener
{
	static final private int RIGHT_COLOR = Color.GREEN;
	static final private int WRONG_COLOR = Color.BLACK;
	
	public class CursorResult {
		private long mTimeLength;
		private int mNumberOfMoves;
		private int mTimesMissed;
		
		public CursorResult(long timeLength, int numberOfMoves, int numberMisses) {
			mTimeLength = timeLength;
			mNumberOfMoves = numberOfMoves;
			mTimesMissed = numberMisses;
		}
		
		public void writeResult(PrintWriter writer) {
			writer.println("" + mTimeLength + "\t" + mNumberOfMoves + "\t" + mTimesMissed);
		}
	}
	
	private GestureCursorController mCursor;
	private CameraGestureSensor mGestureSensor;
	private ClickSensor mClickSensor;
	
	private Random mRandom;
	
	private Point mBoxSize;
	
	private Rect mCurrentBox;
	
	private Paint mBoxPaintFill;
	private Paint mBoxPaintStroke;
	private Paint mNumberRightPaint;
	private Paint mNumberWrongPaint;
	private Paint mNumberMovesPaint;
	
	protected int mRightScore;
	protected int mWrongScore;
	private int mNumberOfMoves;
	private int mTimesMissed;
	private long mTimeStarted;
	
	protected boolean mIsRunning;
	
	private List<CursorResult> mCursorHistory;
	
	public CursorActivity(
			TestingActivity activity,
			CameraGestureSensor gestureSensor,
			ClickSensor clickSensor) {
		super(activity);
		mCursor = new GestureCursorController(activity);
		mCursor.setDisableInjection(true);
		mGestureSensor = gestureSensor;
		mClickSensor = clickSensor;
		
		setOnClickListener(this);
		
		mRandom = new Random();
		
		mBoxSize = new Point(mCursor.getCursorRadius() * 12, mCursor.getCursorRadius() * 5);
		mCurrentBox = new Rect(0, 0, mBoxSize.x, mBoxSize.y);
		
		mBoxPaintFill = new Paint();
		mBoxPaintFill.setStyle(Style.FILL);
		
		mBoxPaintStroke = new Paint();
		mBoxPaintStroke.setColor(Color.BLACK);
		mBoxPaintStroke.setStrokeWidth(5.0f);
		mBoxPaintStroke.setStyle(Style.STROKE);
		
		mNumberRightPaint = new Paint();
		mNumberRightPaint.setARGB(255, 0, 0, 0);
		mNumberRightPaint.setTextSize(60);
		mNumberRightPaint.setTextAlign(Align.LEFT);
		
		mNumberWrongPaint = new Paint();
		mNumberWrongPaint.setARGB(255, 255, 0, 0);
		mNumberWrongPaint.setTextSize(60);
		mNumberWrongPaint.setTextAlign(Align.RIGHT);
		
		mNumberMovesPaint = new Paint();
		mNumberMovesPaint.setARGB(255, 0, 100, 0);
		mNumberMovesPaint.setTextSize(60);
		mNumberMovesPaint.setTextAlign(Align.CENTER);
		
		mCursorHistory = new LinkedList<CursorResult>();
	}
	
	protected void randomizeBox() {
		mCurrentBox.offsetTo(
				mRandom.nextInt(getWidth() - mBoxSize.x),
				mRandom.nextInt(getHeight() - mBoxSize.y - 150 - 280) + 150);
		mNumberOfMoves = 0;
		mTimesMissed = 0;
		mTimeStarted = System.currentTimeMillis();
		
		mBoxPaintFill.setColor(Color.argb(255, mRandom.nextInt(256), mRandom.nextInt(256), mRandom.nextInt(256)));
		postInvalidate();
	}
	
	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		if(changed) {
			randomizeBox();
		}
	}

	@Override
	public void startView() {
		super.startView();
		mGestureSensor.addGestureListener(mCursor);
		mClickSensor.addClickListener(mCursor);
		mCursor.attachToActivity(mActivity);
		
		mIsRunning = false;
		
		mRightScore = mWrongScore = 0;
		mCursorHistory.clear();
		
		mCursor.start();
	}

	public void stopView() {
		super.stopView();
		mCursorHistory.clear();
		mCursor.removeFromParent();
		mGestureSensor.removeGestureListener(mCursor);
		mClickSensor.removeClickListener(mCursor);
		mCursor.stop();
	}
	
	@Override
	public void onSensorClick(ClickSensor sender) {
		if(!mIsRunning)
			return;
		
		Point p = mCursor.getPositionInViewSpace();
		if(mCurrentBox.contains(p.x, p.y)) {
			long timeElapsed = System.currentTimeMillis() - mTimeStarted;
			mCursorHistory.add(new CursorResult(timeElapsed, mNumberOfMoves, mTimesMissed));
			mRightScore++;
		
			randomizeBox();
			
			mCursor.setClickColor(RIGHT_COLOR);
		}
		else {
			mWrongScore++;
			mTimesMissed++;
			mCursor.setClickColor(WRONG_COLOR);
			postInvalidate();
		}
	}
	
	private Rect boundsRect = new Rect();
	@Override
	protected void onDraw(Canvas canvas) {
		canvas.drawRect(mCurrentBox, mBoxPaintFill);
		canvas.drawRect(mCurrentBox, mBoxPaintStroke);
		
		String rightText = "Right: " + mRightScore;
		String wrongText = "Wrong: " + mWrongScore;
		String movesText = "" + mNumberOfMoves + (mNumberOfMoves == 1 ? " move" : " moves");
		
		mNumberRightPaint.getTextBounds(rightText, 0, rightText.length(), boundsRect);
		canvas.drawText(rightText, 0.0f, boundsRect.bottom - boundsRect.top, mNumberRightPaint);
		
		mNumberWrongPaint.getTextBounds(wrongText, 0, wrongText.length(), boundsRect);
		canvas.drawText(wrongText, canvas.getWidth(), boundsRect.bottom - boundsRect.top, mNumberWrongPaint);
		
		canvas.drawText(movesText, canvas.getWidth() / 2, 2*(boundsRect.bottom - boundsRect.top), mNumberMovesPaint);
	}
	
	@Override
	public void onGestureUp(CameraGestureSensor caller, long gestureLength) {
		if(!mIsRunning)
			return;
		mNumberOfMoves++;
		postInvalidate();
	}
	
	@Override
	public void onGestureDown(CameraGestureSensor caller, long gestureLength) {
		if(!mIsRunning)
			return;
		mNumberOfMoves++;
		postInvalidate();
	}
	
	@Override
	public void onGestureLeft(CameraGestureSensor caller, long gestureLength) {
		if(!mIsRunning)
			return;
		mNumberOfMoves++;
		postInvalidate();
	}
	
	@Override
	public void onGestureRight(CameraGestureSensor caller, long gestureLength) {
		if(!mIsRunning)
			return;
		mNumberOfMoves++;
		postInvalidate();
	}
	
	protected void writeResultsToFile(String fileName) {
		File root = android.os.Environment.getExternalStorageDirectory();

	    File dir = new File (root.getAbsolutePath() + "/TouchFreeLibraryTests/cursorTest");
	    dir.mkdirs();
	    File file = new File(dir, fileName);

	    try {
	        FileOutputStream fos = new FileOutputStream(file);
			PrintWriter writer = new PrintWriter(fos);
			writer.println("Right: " + mRightScore);
			writer.println("Wrong: " + mWrongScore);
			writer.println();
			writer.println("Length (in milliseconds)\tNumber of Moves\tNumber of Misses");
			
			for(CursorResult cr : mCursorHistory) {
				cr.writeResult(writer);
			}
			
			writer.close();
			fos.close();
	    } catch (FileNotFoundException e) {
	        e.printStackTrace();
	    } catch (IOException e) {
	        e.printStackTrace();
	    }
	}

	@Override
	public void onClick(View arg0) {
		goToNextScreen();
	}
}
