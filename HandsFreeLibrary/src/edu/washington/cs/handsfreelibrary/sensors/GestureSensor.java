package edu.washington.cs.handsfreelibrary.sensors;

import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.highgui.Highgui;
import org.opencv.highgui.VideoCapture;

import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

/**
 * 
 * @author Leeran Raphaely <leeran.raphaely@gmail.com>
 *
 */
public class GestureSensor extends ClickSensor {
	public interface Listener {
		public void onGestureUp();
		public void onGestureDown();
		public void onGestureLeft();
		public void onGestureRight();
	}
	
	private enum Direction {
		Left(0), Down(1), Right(2), Up(3), None(4);
		
		private int numVal;
		
		Direction(int numVal) {
			this.numVal = numVal;
		}
		
		public int toInt() {
			return numVal;
		}
	} ;
	
	private Listener mGestureListener;
	
	private VideoCapture mCamera;
	private int mCameraId;
	private Size mPreviewSize;
	private Thread mFrameProcessor;
	
	private boolean mIsRunning;
	
	private static final double EPSILON = 0.00001;
	private static final double MIN_FRACTION_SCREEN_MOTION   = 0.1;
	
	private double mMinDirectionalMotionX;
	private double mMinDirectionalMotionY;
	private double mWidthToHeight;
	
	private Mat mPreviousFrame;
	private Mat mCurrentFrame;
	
	private Point mStartPos;
	private Point mPreviousPos;
	
	private boolean mIsHorizontalScrollEnabled;
	private boolean mIsVerticalScrollEnabled;
	private boolean mIsClickByColorEnabled;
	
	private Context mContext;
	
	static public void loadLibrary() {
		try {
			System.loadLibrary("hands_free_library");
		} catch(UnsatisfiedLinkError e) {
			
		}
	}
	
	// a quick utility function to find the camera id
	int getFrontCameraId() {
		CameraInfo ci = new CameraInfo();
		for (int i = 0 ; i < Camera.getNumberOfCameras(); i++) {
	        Camera.getCameraInfo(i, ci);
	        if (ci.facing == CameraInfo.CAMERA_FACING_FRONT) return i;
	    }
	    return 0; // No front-facing camera found
	}
	
	public GestureSensor(Context context) {
		mIsHorizontalScrollEnabled = true;
		mIsVerticalScrollEnabled = true;
		mIsClickByColorEnabled = false;
		
		mIsRunning = false;
		
		mGestureListener = null;
		
		// find the front facing camera id
		mCameraId = getFrontCameraId();
		
		mContext = context;
		
		mFrameProcessor = new Thread(mProcessFramesRunnable);
	}
	
	public void setGestureListener(Listener listener) {
		mGestureListener = listener;
	}
	
	public void enableHorizontalScroll(boolean enabled) {
		mIsHorizontalScrollEnabled = enabled;
	}
	
	public boolean isHorizontalScrollEnabled() {
		return mIsHorizontalScrollEnabled;
	}
	
	public void enableVerticalScroll(boolean enabled) {
		mIsVerticalScrollEnabled = enabled;
	}
	
	public boolean isVerticalScrollEnabled() {
		return mIsVerticalScrollEnabled;
	}
	
	public void enableClickByColor(boolean enabled) {
		mIsClickByColorEnabled = enabled;
	}
	
	public boolean isClickByColorEnabled() {
		return mIsClickByColorEnabled;
	}
	
	public void start() {
		mPreviousPos = new Point(0, 0);
		mStartPos = null;
		
		if (mCamera != null) {
			VideoCapture camera = mCamera;
			mCamera = null; // Make it null before releasing...
			camera.release();
		}
		
		mCamera = new VideoCapture(mCameraId);

		List<Size> previewSizes = mCamera.getSupportedPreviewSizes();
		
		double smallestPreviewSize = 640 * 480; // We should be smaller than this...
		
		double smallestWidth = 320; // Let's not get smaller than this...
		
		for (Size previewSize : previewSizes) {
			if (previewSize.area() < smallestPreviewSize && previewSize.width >= smallestWidth) {
				mPreviewSize = previewSize;
			}
		}
		mCamera.set(Highgui.CV_CAP_PROP_FRAME_WIDTH, mPreviewSize.width);
		mCamera.set(Highgui.CV_CAP_PROP_FRAME_HEIGHT, mPreviewSize.height);
		
		mPreviousFrame = new Mat((int)mPreviewSize.height, (int)mPreviewSize.width, CvType.CV_8U);
		mCurrentFrame = new Mat((int)mPreviewSize.height, (int)mPreviewSize.width, CvType.CV_8U);
  	     
		mMinDirectionalMotionX = mPreviewSize.width / 5;
  	    mMinDirectionalMotionY = mPreviewSize.height / 6;
  	     
  	    mWidthToHeight = mPreviewSize.width / mPreviewSize.height * 6.0 / 5.0;
  	    
  	    mIsRunning = true;
  	    
  	    // run the frame processor now
  	    mFrameProcessor.start();
	}
	
	public void stop() {
		mIsRunning = false;
		if (mCamera != null) {
			synchronized (mProcessFramesRunnable) {
				VideoCapture camera = mCamera;
				mCamera = null; // Make it null before releasing...
				camera.release();
			}
		}
	}
	
	private int adjustDirectionForScreenRotation(Direction d) {
		Display display = ((WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
		
		int dNum = d.toInt();
		
		switch(display.getRotation()) {
		case Surface.ROTATION_0:
			dNum += 3;
			break;
		case Surface.ROTATION_90:
			break;
		case Surface.ROTATION_180:
			dNum += 1;
			break;
		case Surface.ROTATION_270:
			dNum += 2;
			break;
		}
		
		return dNum % 4;
	}
	
	private Runnable mProcessFramesRunnable = new Runnable() {
		@Override
		public void run() {
			while (mCamera != null && mIsRunning) {
				synchronized (this) {
					boolean grabbed = mCamera.grab();
					if(!grabbed)
						continue;
					mCamera.retrieve(mCurrentFrame, Highgui.CV_CAP_ANDROID_GREY_FRAME);
					
					// see if we need to detect clicks by color, and if so, let's quickly get that out of the way
					if(mIsClickByColorEnabled) {
						double avgColor = Core.mean(mCurrentFrame).val[0];
						
						if(avgColor < 30.0)
							onSensorClick();
					}
					
					// detect the motion
					MotionDetectionReturnValue mdret = DetectMovementPosition(mCurrentFrame.getNativeObjAddr(), mPreviousFrame.getNativeObjAddr());
					
					// set the previous frame to the current frame
					mCurrentFrame.copyTo(mPreviousFrame);
					
					Direction movementDirection = Direction.None;
					
					if(mStartPos == null && mdret.fractionOfScreenInMotion > MIN_FRACTION_SCREEN_MOTION) {
						mStartPos = mdret.averagePosition;
					}
					else if(mStartPos != null && mdret.fractionOfScreenInMotion < MIN_FRACTION_SCREEN_MOTION) {
						// check if it's a vertical move or a horizontal move
						
						// for horizontal, assume screen is flipped
						if(mIsHorizontalScrollEnabled) {
							if(mPreviousPos.x - mStartPos.x > mMinDirectionalMotionX) {
								movementDirection = Direction.Left;
							}
							else if(mStartPos.x - mPreviousPos.x > mMinDirectionalMotionX) {
								movementDirection = Direction.Right;
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
						
						mStartPos = null;
					}
					
					// see if we should call a callback based on movementDirection
					if(mGestureListener != null && movementDirection != Direction.None) {
						int adjustedDirection = adjustDirectionForScreenRotation(movementDirection);
						if(adjustedDirection == Direction.Left.toInt())
							mGestureListener.onGestureLeft();
						else if(adjustedDirection == Direction.Right.toInt())
							mGestureListener.onGestureRight();
						else if(adjustedDirection == Direction.Up.toInt())
							mGestureListener.onGestureUp();
						else if(adjustedDirection == Direction.Down.toInt())
							mGestureListener.onGestureDown();
					}
					
					mPreviousPos.x = mdret.averagePosition.x;
					mPreviousPos.y = mdret.averagePosition.y;
				}
			}
		}
	};
	
	public native MotionDetectionReturnValue DetectMovementPosition(long currentFrame, long previousFrame);
}