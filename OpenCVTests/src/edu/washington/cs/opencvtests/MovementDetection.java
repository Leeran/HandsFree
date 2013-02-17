package edu.washington.cs.opencvtests;

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
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.Menu;
import android.view.SurfaceView;
import android.view.WindowManager;

public class MovementDetection extends Activity implements CvCameraViewListener {
	protected static final String TAG = "MovementDetection";
	private static final double EPSILON = 0.00001;
	
	enum Direction {
		None, Left, Right, Up, Down
	} ;
	
	private static final double MIN_FRACTION_SCREEN_MOTION   = 0.1;
	
	// unused for now
	//private static final double MAX_NON_DIRECTIONAL_MOTION_X = 40.0;
	//private static final double MAX_NON_DIRECTIONAL_MOTION_Y = 40.0;
	
	private static final double MIN_DIRECTIONAL_MOTION_X     = 300.0;
	private static final double MIN_DIRECTIONAL_MOTION_Y     = 150.0;
	
	private CameraBridgeViewBase mOpenCvCameraView;
	
	private Mat mPreviousFrame;
	private Mat mCurrentFrame;
	private Mat mDifference;
	
	private Point mStartPos;
	private Point mPreviousPos;
	
	int mLeft, mRight, mUp, mDown;
	
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
	     mOpenCvCameraView.enableFpsMeter();
	     
	     mLeft = mRight = mUp = mDown = 0;
	     
	     mPreviousPos = new Point(0, 0);
	     mStartPos = null;
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
   	     mDifference = new Mat(height, width, CvType.CV_8U);
	 }

	 public void onCameraViewStopped() {
	 }

	 public Mat onCameraFrame(Mat inputFrame) {
		 // convert the input frame to gray-scale
		 Imgproc.cvtColor(inputFrame, mCurrentFrame, Imgproc.COLOR_RGB2GRAY);
		 Core.flip(mCurrentFrame, mCurrentFrame, 1);
		 
		 // get the difference between the current frame and the frame before it
		 Core.absdiff(mCurrentFrame, mPreviousFrame, mDifference);
		 
		 // process the resulting image to clean it up
		 Imgproc.blur(mDifference, mDifference, new Size(5, 5));
		 Imgproc.threshold(mDifference, mDifference, 30, 255, Imgproc.THRESH_BINARY_INV);
		 
		 // set the previous frame to the current frame
		 mCurrentFrame.copyTo(mPreviousFrame);
		 
		 MotionDetectionReturnValue mdret = DetectMovementPosition(mDifference.getNativeObjAddr());
		 
		 Direction movementDirection = Direction.None;
		 
		 if(mStartPos == null && mdret.fractionOfScreenInMotion > MIN_FRACTION_SCREEN_MOTION) {
			 mStartPos = mdret.averagePosition;
		 }
		 
		 else if(mStartPos != null && mdret.fractionOfScreenInMotion < MIN_FRACTION_SCREEN_MOTION) {
			 // check if it's a vertical move or a horizontal move
			 if(mPreviousPos.x - mStartPos.x > MIN_DIRECTIONAL_MOTION_X) {
				 movementDirection = Direction.Right;
			 }
			 
			 else if(mStartPos.x - mPreviousPos.x > MIN_DIRECTIONAL_MOTION_X) {
				 movementDirection = Direction.Left;
			 }
			 
			 double verticalMotion = Math.abs(mPreviousPos.y - mStartPos.y);
			 if(verticalMotion > MIN_DIRECTIONAL_MOTION_Y) {
				 if(movementDirection == Direction.None || verticalMotion > Math.abs(mPreviousPos.x - mStartPos.x) * 2) {
					 if(mPreviousPos.y < mStartPos.y)
						 movementDirection = Direction.Up;
					 else
						 movementDirection = Direction.Down;
				 }
			 }
			 
			 mStartPos = null;
		 }
		 
		 // keep up to date stats
		 updateStats(movementDirection);
		 
		 mPreviousPos.x = mdret.averagePosition.x;
		 mPreviousPos.y = mdret.averagePosition.y;
		 
		 /*Core.putText(
				 mDifference,
				 "(" + mdret.averagePosition.x + ", " + mdret.averagePosition.y + ")",
				 new Point(5.0, 30.0),
				 Core.FONT_HERSHEY_SIMPLEX,
				 1.0,
				 new Scalar(0, 0, 0));
		 
		 Core.putText(
				 mDifference,
				 "Percent of Screen In Motion: " + mdret.fractionOfScreenInMotion * 100.0 + "%",
				 new Point(5.0, 60.0),
				 Core.FONT_HERSHEY_SIMPLEX,
				 1.0,
				 new Scalar(0, 0, 0));*/
		 
		 Core.putText(
				 mDifference,
				 "{" + mLeft + ", " + mRight + ", " + mUp + ", " + mDown + "}",
				 new Point(5.0, 90.0),
				 Core.FONT_HERSHEY_SIMPLEX,
				 1.0,
				 new Scalar(0, 0, 0));
		 
		 
		 return mDifference;
	 }
	 
	 private void updateStats(Direction dir) {
		 switch(dir) {
			case Down:
				mDown++;
				break;
			case Left:
				mLeft++;
				break;
			case Right:
				mRight++;
				break;
			case Up:
				mUp++;
				break;
			default:
			break;
		 }
	 }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_movement_detection, menu);
		return true;
	}
	
	public native MotionDetectionReturnValue DetectMovementPosition(long mat);

}
