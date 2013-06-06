package edu.washington.cs.touchfreelibraryusertesting;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import edu.washington.cs.touchfreelibrary.sensors.CameraGestureSensor;

import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.view.View;

public class GestureDemos extends AbstractTestingView implements View.OnClickListener {
	public class GestureResult {
		private Direction mExpectedGesture;
		private Direction mReceivedGesture;
		
		private long mTimeLength;
		
		public GestureResult(Direction expected, Direction received, long time) {
			mExpectedGesture = expected;
			mReceivedGesture = received;
			mTimeLength = time;
		}
		
		public void writeResult(PrintWriter writer) {
			String expected = directionToString(mExpectedGesture);
			String received = directionToString(mReceivedGesture);
			writer.println(expected + "\t" + received + "\t" + mTimeLength);
		}
	}
	
	private enum Direction {
		Up, Down, Left, Right
	}
	
	private String directionToString(Direction d) {
		switch(d) {
		case Up:
			return "Up";
		case Down:
			return "Down";
		case Left:
			return "Left";
		default:
			return "Right";
		}
	}
	
	private Rect mArrowRect;
	private Random mRandom;
	
	private Paint mBorderPaint;
	private Paint mFillPaint;
	private Paint mNumberRightPaint;
	private Paint mNumberWrongPaint;
	
	private Path mUpArrowPath;
	private Path mDownArrowPath;
	private Path mLeftArrowPath;
	private Path mRightArrowPath;
	
	protected int mRightScore;
	protected int mWrongScore;
	
	private Direction mCurrentDirection;
	private long mTimePresented;
	protected boolean mIsRunning;
	
	protected List<GestureResult> mGestureHistory;

	public GestureDemos(TestingActivity activity) {
		super(activity);
		mArrowRect = new Rect();
		mRandom = new Random();
		
		mGestureHistory = new LinkedList<GestureResult>();
		
		mBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		mFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		mNumberRightPaint = new Paint();
		mNumberWrongPaint = new Paint();
		
		mFillPaint.setARGB(255, 50, 168, 153);
		mFillPaint.setStyle(Style.FILL);
		
		mBorderPaint.setARGB(255, 0, 0, 0);
		mBorderPaint.setStrokeWidth(6.0f);
		mBorderPaint.setStyle(Style.STROKE);
		
		mNumberRightPaint.setARGB(255, 0, 0, 0);
		mNumberRightPaint.setTextSize(60);
		mNumberRightPaint.setTextAlign(Align.LEFT);
		
		mNumberWrongPaint.setARGB(255, 255, 0, 0);
		mNumberWrongPaint.setTextSize(60);
		mNumberWrongPaint.setTextAlign(Align.RIGHT);
		
		setOnClickListener(this);
		
		makeArrowPaths();
	}
	
	@Override
	public void startView() {
		super.startView();
		mRightScore = mWrongScore = 0;
		mGestureHistory.clear();
		setRandomDirection();
		mIsRunning = false;
	}
	
	@Override
	public void stopView() {
		super.stopView();
	}
	
	// make an arrow facing up
	private void makeArrowPaths() {
		mUpArrowPath = new Path();
		mDownArrowPath = new Path();
		mLeftArrowPath = new Path();
		mRightArrowPath = new Path();
		
		mDownArrowPath.moveTo(-40.0f, -80.0f); // bottom left corner
		mDownArrowPath.lineTo(40.0f, -80.0f);  // bottom right corner
		mDownArrowPath.lineTo(40.0f, 80.0f);   // top right indent
		mDownArrowPath.lineTo(88.0f, 80.0f);   // top right out-dent
		mDownArrowPath.lineTo(0.0f, 190.0f);    // vertex
		mDownArrowPath.lineTo(-88.0f, 80.0f);  // top left out-dent
		mDownArrowPath.lineTo(-40.0f, 80.0f);  // top left indent
		mDownArrowPath.close();
		
		Matrix scale = new Matrix();
		scale.postScale(1.7f, 1.7f);
		mDownArrowPath.transform(scale);
		
		Matrix rotate = new Matrix();
		rotate.postRotate(90.0f);
		
		mLeftArrowPath.addPath(mDownArrowPath, rotate);
		mUpArrowPath.addPath(mLeftArrowPath, rotate);
		mRightArrowPath.addPath(mUpArrowPath, rotate);
	}
	
	protected void setRandomDirection() {
		int dir = mRandom.nextInt(4);
		if(dir == 0) mCurrentDirection = Direction.Up;
		else if(dir == 1) mCurrentDirection = Direction.Down;
		else if(dir == 2) mCurrentDirection = Direction.Left;
		else if(dir == 3) mCurrentDirection = Direction.Right;
		
		mFillPaint.setARGB(255, mRandom.nextInt(256), mRandom.nextInt(256), mRandom.nextInt(256));
		
		postInvalidate();
		mTimePresented = System.currentTimeMillis();
	}
	
	private Path getCurrentArrow() {
		switch(mCurrentDirection) {
		case Up:
			return mUpArrowPath;
		case Down:
			return mDownArrowPath;
		case Left:
			return mLeftArrowPath;
		default:
			return mRightArrowPath;
		}
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		if(changed) {
			int arrowWidth = (int)((right - left)  * 0.75);
			int arrowHeight = arrowWidth;
			
			mArrowRect.left = (right - left) / 2 - arrowWidth / 2;
			mArrowRect.right = mArrowRect.left + arrowWidth;
			mArrowRect.top = (bottom - top) / 2 - arrowHeight / 2;
			mArrowRect.bottom = mArrowRect.top + arrowHeight;
		}
	}
	
	private Rect boundsRect = new Rect();
	@Override
    protected void onDraw(Canvas canvas) {
		canvas.save();
		String rightText = "Right: " + mRightScore;
		String wrongText = "Wrong: " + mWrongScore;
		
		mNumberRightPaint.getTextBounds(rightText, 0, rightText.length(), boundsRect);
		canvas.drawText(rightText, 0.0f, boundsRect.bottom - boundsRect.top, mNumberRightPaint);
		
		mNumberWrongPaint.getTextBounds(wrongText, 0, wrongText.length(), boundsRect);
		canvas.drawText(wrongText, canvas.getWidth(), boundsRect.bottom - boundsRect.top, mNumberWrongPaint);
		
		canvas.translate(canvas.getWidth() / 2.0f, canvas.getHeight() / 2.0f - 100.0f);
		canvas.drawPath(getCurrentArrow(), mFillPaint);
		canvas.drawPath(getCurrentArrow(), mBorderPaint);
		
		canvas.restore();
	}
	
	@Override
	public void onGestureUp(CameraGestureSensor caller, long gestureLength) {
		if(!mIsRunning)
			return;
		mGestureHistory.add(new GestureResult(
				mCurrentDirection,
				Direction.Up,
				System.currentTimeMillis() - mTimePresented));
		
		if(mCurrentDirection == Direction.Up) {
			mRightScore++;
		} else {
			mWrongScore++;
		}
		
		setRandomDirection();
	}

	@Override
	public void onGestureDown(CameraGestureSensor caller, long gestureLength) {
		if(!mIsRunning)
			return;
		
		mGestureHistory.add(new GestureResult(
				mCurrentDirection,
				Direction.Down,
				System.currentTimeMillis() - mTimePresented));
		
		if(mCurrentDirection == Direction.Down) {
			mRightScore++;
		} else {
			mWrongScore++;
		}
		
		setRandomDirection();
	}

	@Override
	public void onGestureLeft(CameraGestureSensor caller, long gestureLength) {
		if(!mIsRunning)
			return;
		
		mGestureHistory.add(new GestureResult(
				mCurrentDirection,
				Direction.Left,
				System.currentTimeMillis() - mTimePresented));
		
		if(mCurrentDirection == Direction.Left) {
			mRightScore++;
		} else {
			mWrongScore++;
		}
		
		setRandomDirection();
	}

	@Override
	public void onGestureRight(CameraGestureSensor caller, long gestureLength) {
		if(!mIsRunning)
			return;
		
		mGestureHistory.add(new GestureResult(
				mCurrentDirection,
				Direction.Right,
				System.currentTimeMillis() - mTimePresented));
		
		if(mCurrentDirection == Direction.Right) {
			mRightScore++;
		} else {
			mWrongScore++;
		}
		
		setRandomDirection();
	}
	
	@Override
	public void onClick(View v) {
		goToNextScreen();
	}
	
	protected void writeResultsToFile(String fileName) {
		File root = android.os.Environment.getExternalStorageDirectory();

	    File dir = new File (root.getAbsolutePath() + "/TouchFreeLibraryTests/gestureTest1");
	    dir.mkdirs();
	    File file = new File(dir, fileName);

	    try {
	        FileOutputStream fos = new FileOutputStream(file);
			PrintWriter writer = new PrintWriter(fos);
			writer.println("Right: " + mRightScore);
			writer.println("Wrong: " + mWrongScore);
			writer.println();
			writer.println("Expected\tReceived\tLength (in milliseconds)");
			
			for(GestureResult gr : mGestureHistory) {
				gr.writeResult(writer);
			}
			
			writer.close();
			fos.close();
	    } catch (FileNotFoundException e) {
	        e.printStackTrace();
	    } catch (IOException e) {
	        e.printStackTrace();
	    }
	}
}
