package edu.washington.cs.opencvtests;

import java.io.IOException;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.os.Bundle;
import android.app.Activity;
import android.text.Editable;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;

public class MovementDetection extends Activity implements CvCameraViewListener {
	protected static final String TAG = "MovementDetection";
	private static final double EPSILON = 0.00001;
	
	enum Direction {
		None, Left, Right, Up, Down
	} ;
	
	//private static final double START_MOTION_AT_FRACTION     = 0.03;
	private static final double MIN_FRACTION_SCREEN_MOTION   = 0.1;
	
	// unused for now
	//private static final double MAX_NON_DIRECTIONAL_MOTION_X = 40.0;
	//private static final double MAX_NON_DIRECTIONAL_MOTION_Y = 40.0;
	
	private double mMinDirectionalMotionX;
	private double mMinDirectionalMotionY;
	private double mWidthToHeight;
	
	private CameraBridgeViewBase mOpenCvCameraView;
	private EditText mTextEditor;
	
	private Mat mPreviousFrame;
	private Mat mCurrentFrame;
	private Mat mOutput;
	
	private Point mStartPos;
	private Point mPreviousPos;
	
	ClapDetector mClapDetector;
	AudioTest mAudioTest;
	
	// These are just diagnostics variables
	private int mLeft, mRight, mUp, mDown;
	private Point mLastStart, mLastStop;
	private int numFrames;
	
	private Point mMinPoint, mMaxPoint;
	
	private int mClickAnimationFrame;
	private static final int NUM_CLICK_ANIM_FRAMES = 10;
	
	// options menu:
	private MenuItem mEnableHorizontalScroll = null;
	private MenuItem mEnableVerticalScroll = null;
	private MenuItem mEnableClickByColor = null;
	private MenuItem mEnableClickBySound = null;
	
	private boolean mIsHorizontalScrollEnabled;
	private boolean mIsVerticalScrollEnabled;
	private boolean mIsClickByColorEnabled;
	private boolean mIsClickBySoundEnabled;
	
	private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
	    @Override
	    public void onManagerConnected(int status) {
	        switch (status) {
	            case LoaderCallbackInterface.SUCCESS:
	            {
	                Log.i(TAG, "OpenCV loaded successfully");
	                
	                System.loadLibrary("opencv_tests");
	                
	                mOpenCvCameraView.enableView();
	                
	            } break;
	            default:
	            {
	                super.onManagerConnected(status);
	            } break;
	        }
	    }
	};

	@Override
	public void onResume()
	{
	    super.onResume();
	    OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this, mLoaderCallback);
	}

	@Override
	 public void onCreate(Bundle savedInstanceState) {
	     Log.i(TAG, "called onCreate");
	     super.onCreate(savedInstanceState);
	     
	     getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	     setContentView(R.layout.activity_movement_detection);
	     
	     mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.java_surface_view);
	     mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
	     mOpenCvCameraView.setCvCameraViewListener(this);
	     //mOpenCvCameraView.enableFpsMeter();
	     mOpenCvCameraView.setMaxFrameSize(352, 288);
	     
	     mPreviousPos = new Point(0, 0);
	     mStartPos = null;
	     
	     // just for diagnostics
	     mLastStart = new Point(-1.0, -1.0);
	     mLastStop = new Point(-1.0, -1.0);
	     mLeft = mRight = mUp = mDown = 0;
	     numFrames = 0;
	     
	     mClickAnimationFrame = 0;
	     
	     mTextEditor = (EditText) findViewById(R.id.textBox);
	     
	     mTextEditor.setText(getString(R.string.default_text));
	     for(int i = 0; i < getResources().getInteger(R.integer.numLines) - 1; i++) 
	    	 mTextEditor.append("\n" + getString(R.string.default_text));
	 }

	 @Override
	 public void onPause()
	 {
	     super.onPause();
	     if (mOpenCvCameraView != null)
	         mOpenCvCameraView.disableView();
	 }

	 public void onDestroy() {
	     super.onDestroy();
	     if (mOpenCvCameraView != null)
	         mOpenCvCameraView.disableView();
	 }

	 public void onCameraViewStarted(int width, int height) {
		 
		// create our movement frames
		 mPreviousFrame = new Mat(height, width, CvType.CV_8U);
		 mCurrentFrame = new Mat(height, width, CvType.CV_8U);
   	     mOutput = new Mat(height, width, CvType.CV_8UC4);
   	     
   	     mMinDirectionalMotionX = width / 5;
   	     mMinDirectionalMotionY = height / 6;
   	     
   	     mWidthToHeight = (double)width / (double)height * 6.0 / 5.0;
   	     
   	     mTextEditor.setEnabled(false);
   	     mTextEditor.setSelection(0, 1);
   	     
   	     mAudioTest = new AudioTest();
   	     mAudioTest.start();
	 }

	 public void onCameraViewStopped() {
	 }

	 public Mat onCameraFrame(Mat inputFrame) {
		 // flip the input
		 Core.flip(inputFrame, mOutput, 1);
		 
		 // convert the input frame to gray-scale
		 Imgproc.cvtColor(mOutput, mCurrentFrame, Imgproc.COLOR_RGB2GRAY);
		 
		 // see if we need to detect clicks by color, and if so, let's quickly get that out of the way
		 if(mIsClickByColorEnabled) {
			 double avgColor = Core.mean(mCurrentFrame).val[0];
			 
			 if(avgColor < 30.0)
				 onClick();
		 }
		 
		 // detect the motion
		 MotionDetectionReturnValue mdret = DetectMovementPosition(mCurrentFrame.getNativeObjAddr(), mPreviousFrame.getNativeObjAddr());
		 
		// set the previous frame to the current frame
		mCurrentFrame.copyTo(mPreviousFrame);
		 
		Direction movementDirection = Direction.None;
		if(mStartPos == null && mdret.fractionOfScreenInMotion > MIN_FRACTION_SCREEN_MOTION) {
			mStartPos = mdret.averagePosition;
			numFrames = 0;
			mMinPoint = roundPoint(mdret.averagePosition);
			mMaxPoint = roundPoint(mdret.averagePosition);
		}

		else if(mStartPos != null && mdret.fractionOfScreenInMotion < MIN_FRACTION_SCREEN_MOTION) {
			// check if it's a vertical move or a horizontal move
			if(mIsHorizontalScrollEnabled) {
				if(mPreviousPos.x - mStartPos.x > mMinDirectionalMotionX) {
					movementDirection = Direction.Right;
				}
				else if(mStartPos.x - mPreviousPos.x > mMinDirectionalMotionX) {
					movementDirection = Direction.Left;
				}
			}
			if(mIsVerticalScrollEnabled) {
				double verticalMotion = Math.abs(mPreviousPos.y - mStartPos.y);
				if(verticalMotion > mMinDirectionalMotionY) {
					if(movementDirection == Direction.None || verticalMotion * mWidthToHeight > Math.abs(mPreviousPos.x - mStartPos.x) ) {
						if(mPreviousPos.y < mStartPos.y)
							movementDirection = Direction.Up;
						else
							movementDirection = Direction.Down;
					}
				}
			}
			
			mLastStart = roundPoint(mStartPos);
			mLastStop = roundPoint(mPreviousPos);
			
			mStartPos = null;
		}
		
		// set diagnostics
		if(mStartPos != null && mdret.fractionOfScreenInMotion > MIN_FRACTION_SCREEN_MOTION) {
			numFrames++;
			
			if(mdret.averagePosition.x > mMaxPoint.x) mMaxPoint.x = Math.round(mdret.averagePosition.x); 
			if(mdret.averagePosition.x < mMinPoint.x) mMinPoint.x = Math.round(mdret.averagePosition.x); 
			if(mdret.averagePosition.y > mMaxPoint.y) mMaxPoint.y = Math.round(mdret.averagePosition.y); 
			if(mdret.averagePosition.y < mMinPoint.y) mMinPoint.y = Math.round(mdret.averagePosition.y); 
		}
		
		// keep up to date stats
		updateStats(mdret, movementDirection);
 
		mPreviousPos.x = mdret.averagePosition.x;
		mPreviousPos.y = mdret.averagePosition.y;

		Mat displayFrame = mOutput;
		
		 
		/* Core.putText(
				 displayFrame,
				 "(" + mdret.averagePosition.x + ", " + mdret.averagePosition.y + ")",
				 new Point(5.0, 30.0),
				 Core.FONT_HERSHEY_SIMPLEX,
				 1.0,
				 new Scalar(255, 0, 0));
		 
		 Core.putText(
				 displayFrame,
				 mdret.fractionOfScreenInMotion * 100.0 + "%",
				 new Point(5.0, 60.0),
				 Core.FONT_HERSHEY_SIMPLEX,
				 1.0,
				 new Scalar(255, 0, 0));
		 
		 Core.putText(
				 displayFrame,
				 "{" + mLeft + ", " + mRight + ", " + mUp + ", " + mDown + "}",
				 new Point(5.0, 90.0),
				 Core.FONT_HERSHEY_SIMPLEX,
				 1.0,
				 new Scalar(255, 0, 0));
		 
		 if(mLastStart.x >= 0.0) {
			 Core.circle(displayFrame, mLastStart, 10, new Scalar(0, 255, 0));
			 Core.circle(displayFrame, mLastStop, 10, new Scalar(255, 255, 0));
			 
			 Core.circle(displayFrame, mMinPoint, 15, new Scalar(255, 165, 0));
			 Core.circle(displayFrame, mMaxPoint, 15, new Scalar(255, 0, 255));
			 
			 // change in x, change in y, number of frames
			 Core.putText(
					 displayFrame,
					 "(" + Math.abs(mLastStart.x - mLastStop.x) + ", " + Math.abs(mLastStart.y - mLastStop.y) + ", " + numFrames + ")",
					 new Point(5.0, 120.0),
					 Core.FONT_HERSHEY_SIMPLEX,
					 1.0,
					 new Scalar(255, 255, 255));
		 }
		 
		 if(mStartPos == null)
			 Core.circle(displayFrame, mdret.averagePosition, 10, new Scalar(0, 0, 255));
		 else
			 Core.circle(displayFrame, mdret.averagePosition, 10, new Scalar(255, 0, 0));*/
		
		if(mClickAnimationFrame != 0) {
			int color = mClickAnimationFrame * 255 / NUM_CLICK_ANIM_FRAMES;
			Core.rectangle(displayFrame, new Point(0, 0), new Point(displayFrame.size().width, displayFrame.size().height), new Scalar(color, color, color), -1);
			
			mClickAnimationFrame--;
		}
		 
		 return displayFrame;
	 }
	 
	 private void updateStats(MotionDetectionReturnValue mdret, Direction dir) {
		 switch(dir) {
			case Down:
				mDown++;
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						int newSelection = mTextEditor.getSelectionStart() + getString(R.string.default_text).length() + 1;
						if(newSelection < mTextEditor.getText().length())
							mTextEditor.setSelection(newSelection, newSelection + 1);
					}
				});
				break;
			case Left:
				mLeft++;
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						if(mTextEditor.getSelectionStart() > 0)
							mTextEditor.setSelection(mTextEditor.getSelectionStart() - 1, mTextEditor.getSelectionEnd() - 1);
					}
				});
				break;
			case Right:
				mRight++;
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						if(mTextEditor.getSelectionEnd() < mTextEditor.getText().length())
							mTextEditor.setSelection(mTextEditor.getSelectionStart() + 1, mTextEditor.getSelectionEnd() + 1);
					}
				});
				break;
			case Up:
				mUp++;
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						int newSelection = mTextEditor.getSelectionStart() - getString(R.string.default_text).length() - 1;
						if(newSelection >= 0)
							mTextEditor.setSelection(newSelection, newSelection + 1);
					}
				});
				break;
			default:
			break;
		 }

		 /*final StringBuilder statusText = new StringBuilder("(" + mdret.averagePosition.x + ", " + mdret.averagePosition.y + ")");
		 statusText.append("\nPercent of Screen In Motion: " + mdret.fractionOfScreenInMotion * 100.0 + "%");
		 statusText.append("\n{" + mLeft + ", " + mRight + ", " + mUp + ", " + mDown + "}");
		 if(mLastStart.x >= 0.0) {
			 statusText.append("(Xdiff, Ydiff, Frames) = (" + Math.abs(mLastStart.x - mLastStop.x) + ", " + Math.abs(mLastStart.y - mLastStop.y) + ", " + numFrames + ")");
		 }
		 runOnUiThread(new Runnable() {
				@Override
				public void run() {
					mStatusText.setText(statusText);
				}
		 });*/
	 }
	 
	 private void modulateSelectedText(Direction d) {
		 if(d == Direction.Up) {
			 runOnUiThread(new Runnable() {
					@Override
					public void run() {
						Editable text = mTextEditor.getText();
						int selectionIndex = mTextEditor.getSelectionStart();
						String newstr = Character.toString((char) (text.charAt(mTextEditor.getSelectionStart()) - 1));
						text.replace(selectionIndex, selectionIndex + 1, newstr);
					}
			 });
		 }
		 else {
			 runOnUiThread(new Runnable() {
					@Override
					public void run() {
						Editable text = mTextEditor.getText();
						int selectionIndex = mTextEditor.getSelectionStart();
						String newstr = Character.toString((char) (text.charAt(mTextEditor.getSelectionStart()) + 1));
						text.replace(selectionIndex, selectionIndex + 1, newstr);
					}
			 });
		 }
	 }
	 
	 private void onClick() {
		 mClickAnimationFrame = NUM_CLICK_ANIM_FRAMES;
	 }
	 
	 private Point roundPoint(Point p) {
		 return new Point(Math.round(p.x), Math.round(p.y));
	 }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		// Inflate the menu; this adds items to the action bar if it is present.
		mEnableHorizontalScroll = menu.add("Disable Horizontal Scroll");
		mEnableVerticalScroll = menu.add("Disable Vertical Scroll");
		mEnableClickByColor = menu.add("Enable Click By Color");
		mEnableClickBySound = menu.add("Enable Click By Sound");
		
		mIsHorizontalScrollEnabled = true;
		mIsVerticalScrollEnabled = true;
		mIsClickByColorEnabled = false;
		mIsClickBySoundEnabled = false;
		
		return true;
	}
	
	public boolean onOptionsItemSelected(MenuItem item) {
		// user clicked "Enable Horizontal Scroll"
        if(item == mEnableHorizontalScroll) {
        	mIsHorizontalScrollEnabled = !mIsHorizontalScrollEnabled;
        	 if(mIsHorizontalScrollEnabled)
        		 mEnableHorizontalScroll.setTitle("Disable Horizontal Scroll");
        	 else
        		 mEnableHorizontalScroll.setTitle("Enable Horizontal Scroll");
        }
        // user clicked "Enable Vertical Scroll"
        else if(item == mEnableVerticalScroll) {
        	mIsVerticalScrollEnabled = !mIsVerticalScrollEnabled;
		 if(mIsVerticalScrollEnabled)
			 mEnableVerticalScroll.setTitle("Disable Vertical Scroll");
		 else
			 mEnableVerticalScroll.setTitle("Enable Vertical Scroll");
        }
        // user clicked "Enable Click By Color"
        else if(item == mEnableClickByColor) {
        	mIsClickByColorEnabled = !mIsClickByColorEnabled;
		 if(mIsClickByColorEnabled)
			 mEnableClickByColor.setTitle("Disable Click By Color");
		 else
			 mEnableClickByColor.setTitle("Enable Click By Color");
        }
        
        // user clicked "Enable Click By Sound"
        else if(item == mEnableClickBySound) {
        	mIsClickBySoundEnabled = !mIsClickBySoundEnabled;
		 if(mIsClickBySoundEnabled) {
			 mEnableClickBySound.setTitle("Disable Click By Sound");
			 try {
				mClapDetector = new ClapDetector();
			} catch (IOException e) {
				// TODO do something
				mEnableClickBySound.setEnabled(false);
			}
			new Thread()
			{
			   @Override
			    public void run() {
			    	try {
			    		while(mIsClickBySoundEnabled) {
							if(mClapDetector.recordClap()) {
								onClick();
							}
			    		}
						
					} catch (Exception e) {
						// TODO do something
						mEnableClickBySound.setEnabled(false);
					}
			     }
			 }.start();
		 }
		 else {
			 mEnableClickBySound.setTitle("Enable Click By Sound");
			 mClapDetector.stopRecording();
			 mClapDetector = null;
		 }
        }
        
        return true;
    }
	
	public native MotionDetectionReturnValue DetectMovementPosition(long currentFrame, long previousFrame);

}
