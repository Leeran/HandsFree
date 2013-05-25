package edu.washington.cs.touchfreelibrary.sensors;

import java.util.LinkedList;
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
 * <p><code>CameraGestureSensor</code> takes input data from the camera and uses that to sense
 * four gesture commands: up, down, left, and right.</p>
 * 
 * <p><strong>Important: The static function {@link #loadLibrary()} must be called after
 * OpenCV is initiated and before {@link #start()} is called!</strong></p>
 * 
 * @author Leeran Raphaely <leeran.raphaely@gmail.com>
 */
public class CameraGestureSensor extends ClickSensor {
	private static final String TAG = "CameraGestureSensor";
	
	/**
	 * To receive messages from CameraGestureSensor, classes must implement the <code>CameraGestureSensor.Listener</code>
	 * interface.
	 * 
	 * @author Leeran Raphaely <leeran.raphaely@gmail.com>
	 */
	public interface Listener {
		/**
		 * Called when an up gesture is triggered
		 * @param caller the CameraGestureSensor object that made the call
		 * @param gestureLength the amount of time the gesture took in milliseconds
		 */
		public void onGestureUp(CameraGestureSensor caller, long gestureLength);
		
		/**
		 * Called when a down gesture is triggered
		 * @param caller the CameraGestureSensor object that made the call
		 * @param gestureLength the amount of time the gesture took in milliseconds
		 */
		public void onGestureDown(CameraGestureSensor caller, long gestureLength);
		
		/**
		 * Called when a left gesture is triggered
		 * @param caller the CameraGestureSensor object that made the call
		 * @param gestureLength the amount of time the gesture took in milliseconds
		 */
		public void onGestureLeft(CameraGestureSensor caller, long gestureLength);
		
		/**
		 * Called when a right gesture is triggered
		 * @param caller the CameraGestureSensor object that made the call
		 * @param gestureLength the amount of time the gesture took in milliseconds
		 */
		public void onGestureRight(CameraGestureSensor caller, long gestureLength);
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
	
	private final static double DEFAULT_AVERAGE_COLOR_MAX_FOR_CLICK = 30.0;
	
	private List<Listener> mGestureListeners;
	
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
	
	private double mAverageColorMaxForClick;
	
	private long mStartGestureTime;
	
	/**
	 * To use a <code>CameraGestureSensor</code> object, this must be called some time after 
	 * OpenCV is initiated.
	 */
	static public void loadLibrary() {
		System.loadLibrary("touch_free_library");
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
	
	/**
	 * Creates a new instance of CameraGestureSensor. Remember to call {@link loadLibrary} 
	 * @param context A functional Context object needed to get the screen rotation.
	 */
	public CameraGestureSensor(Context context) {
		mIsHorizontalScrollEnabled = true;
		mIsVerticalScrollEnabled = true;
		mIsClickByColorEnabled = false;
		
		mIsRunning = false;
		
		mAverageColorMaxForClick = DEFAULT_AVERAGE_COLOR_MAX_FOR_CLICK;
		
		mGestureListeners = new LinkedList<Listener>();
		
		// find the front facing camera id
		mCameraId = getFrontCameraId();
		
		mContext = context;
	}
	
	/**
	 * Adds listener to the list of gesture listeners.
	 * @param listener This object will have its call-back methods called when a gesture is recognized
	 */
	public void addGestureListener(Listener listener) {
		mGestureListeners.add(listener);
	}
	
	/**
	 * Removes listener from the list of gesture listeners
	 * @param listener The object will no longer have its call-back mehtods called by this gesture sensor.
	 */
	public void removeGestureListener(Listener listener) {
		mGestureListeners.remove(listener);
	}
	
	/**
	 * Removes all gesture listeners.
	 */
	public void clearGestureListeners() {
		mGestureListeners.clear();
	}
	
	// these methods invoke gesture call backs on all listeners
	private void onGestureUp(long gestureLength) {
		for(Listener l : mGestureListeners) {
			l.onGestureUp(this, gestureLength);
		}
	}
	
	private void onGestureLeft(long gestureLength) {
		for(Listener l : mGestureListeners) {
			l.onGestureLeft(this, gestureLength);
		}
	}
	
	private void onGestureRight(long gestureLength) {
		for(Listener l : mGestureListeners) {
			l.onGestureRight(this, gestureLength);
		}
	}
	
	private void onGestureDown(long gestureLength) {
		for(Listener l : mGestureListeners) {
			l.onGestureDown(this, gestureLength);
		}
	}
	
	/**
	 * Enable/disable horizontal scroll.
	 * @param enabled When true, onGestureLeft/onGestureRight are called, when false, they are not.
	 */
	public void enableHorizontalScroll(boolean enabled) {
		mIsHorizontalScrollEnabled = enabled;
	}
	
	/**
	 * Test if horizontal scroll is enabled.
	 * @return true if horizontal scroll is enabled, false otherwise.
	 */
	public boolean isHorizontalScrollEnabled() {
		return mIsHorizontalScrollEnabled;
	}
	
	/**
	 * Enable/disable vertical scroll.
	 * @param enabled When true, onGestureUp/onGestureDown are called, when false, they are not.
	 */
	public void enableVerticalScroll(boolean enabled) {
		mIsVerticalScrollEnabled = enabled;
	}
	
	/**
	 * Test if vertical scroll is enabled.
	 * @return true if vertical scroll is enabled, false otherwise.
	 */
	public boolean isVerticalScrollEnabled() {
		return mIsVerticalScrollEnabled;
	}
	
	/**
	 * When enabled, an onSensorClick command is sent to any click listeners when a large enough
	 * percentage of the screen goes black.
	 * 
	 * @param enabled Set whether click-by-color is enabled
	 */
	public void enableClickByColor(boolean enabled) {
		mIsClickByColorEnabled = enabled;
	}
	
	/**
	 * Test if click by color is enabled.
	 * @return true if click by color is enabled, false otherwise.
	 */
	public boolean isClickByColorEnabled() {
		return mIsClickByColorEnabled;
	}
	
	/**
	 * <p>Causes this to start reading camera input and looking for gestures. The camera must be available
	 * for this method to be successful.</p>
	 * <p>Warning! CameraGestureSensor will seize control of the front facing camera, even if the activity loses focus.
	 * If you would like to let other applications use the camera, you must call stop() when the activity loses
	 * focus.</p>
	 */
	public void start() {
		if(mIsRunning)
			return;
		
		mPreviousPos = new Point(0, 0);
		mStartPos = null;
		
		if (mCamera != null) {
			VideoCapture camera = mCamera;
			mCamera = null; // Make it null before releasing...
			camera.release();
		}
		
		mCamera = new VideoCapture(mCameraId);
		
		if(!mCamera.isOpened()) {
			// the camera was not available
			VideoCapture camera = mCamera;
			mCamera = null; // Make it null before releasing...
			camera.release();
			
			return;
		}

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
  	    mFrameProcessor = new Thread(mProcessFramesRunnable);
  	    mFrameProcessor.start();
	}
	
	/**
	 * Stops this from looking at camera input for gestures, thus freeing the camera for other uses.
	 */
	public void stop() {
		if(!mIsRunning)
			return;
		
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
	
	private boolean isHorScrollAdjustForScreen() {
		Display display = ((WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
		
		switch(display.getRotation()) {
		case Surface.ROTATION_0:
			return mIsVerticalScrollEnabled;
		case Surface.ROTATION_90:
			return mIsHorizontalScrollEnabled;
		case Surface.ROTATION_180:
			return mIsVerticalScrollEnabled;
		default:
			return mIsHorizontalScrollEnabled;
		}
	}
	
	private boolean isVertScrollAdjustForScreen() {
		Display display = ((WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
		
		switch(display.getRotation()) {
		case Surface.ROTATION_0:
			return mIsHorizontalScrollEnabled;
		case Surface.ROTATION_90:
			return mIsVerticalScrollEnabled;
		case Surface.ROTATION_180:
			return mIsHorizontalScrollEnabled;
		default:
			return mIsVerticalScrollEnabled;
		}
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
						
						if(avgColor < mAverageColorMaxForClick)
							onSensorClick();
					}
					
					// detect the motion
					MotionDetectionReturnValue mdret = DetectMovementPosition(mCurrentFrame.getNativeObjAddr(), mPreviousFrame.getNativeObjAddr());
					
					// set the previous frame to the current frame
					mCurrentFrame.copyTo(mPreviousFrame);
					
					Direction movementDirection = Direction.None;
					
					if(mStartPos == null && mdret.fractionOfScreenInMotion > MIN_FRACTION_SCREEN_MOTION) {
						mStartPos = mdret.averagePosition;
						mStartGestureTime = System.currentTimeMillis();
					}
					else if(mStartPos != null && mdret.fractionOfScreenInMotion < MIN_FRACTION_SCREEN_MOTION) {
						// check if it's a vertical move or a horizontal move
						
						// for horizontal, assume screen is flipped
						if(isHorScrollAdjustForScreen()) {
							if(mPreviousPos.x - mStartPos.x > mMinDirectionalMotionX) {
								movementDirection = Direction.Left;
							}
							else if(mStartPos.x - mPreviousPos.x > mMinDirectionalMotionX) {
								movementDirection = Direction.Right;
							}
						}
						if(isVertScrollAdjustForScreen()) {
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
					if(mGestureListeners.size() != 0 && movementDirection != Direction.None) {
						long gestureLength = System.currentTimeMillis() - mStartGestureTime;
						
						int adjustedDirection = adjustDirectionForScreenRotation(movementDirection);
						
						if(adjustedDirection == Direction.Left.toInt())
							onGestureLeft(gestureLength);
						else if(adjustedDirection == Direction.Right.toInt())
							onGestureRight(gestureLength);
						else if(adjustedDirection == Direction.Up.toInt())
							onGestureUp(gestureLength);
						else if(adjustedDirection == Direction.Down.toInt())
							onGestureDown(gestureLength);
					}
					
					mPreviousPos.x = mdret.averagePosition.x;
					mPreviousPos.y = mdret.averagePosition.y;
				}
			}
		}
	};
	
	/**
	 * If ClickByColor is enabled, then when the mean color of the pixels is below c, register a click.
	 * @param c the maximum average color of the pixels received by the camera for a click to be registered
	 */
	public void setAverageColorMaxForClick(double c) {
		mAverageColorMaxForClick = c;
	}
	
	private native MotionDetectionReturnValue DetectMovementPosition(long currentFrame, long previousFrame);
}