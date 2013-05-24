package edu.washington.cs.gesturescrollertest;

import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import edu.washington.cs.handsfreelibrary.sensors.GestureSensor;
import edu.washington.cs.handsfreelibrary.touchemulation.GestureScroller;

import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.Menu;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.EditText;

public class ScrollerActivity extends Activity {
	private static final String TAG = "ScrollerActivity";
	private GestureSensor mGestureSensor;
	private GestureScroller mGestureScroller;
	
	private boolean gestureStarted = false;
	
	private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
	    @Override
	    public void onManagerConnected(int status) {
	        switch (status) {
	            case LoaderCallbackInterface.SUCCESS:
	            {
	                Log.i(TAG, "OpenCV loaded successfully");
	                
	                GestureSensor.loadLibrary();
	                
	                // more initialization steps go here
	                
	                gestureStarted = true;
	                mGestureSensor.start();
	                mGestureScroller.start();
	            } break;
	            default:
	            {
	                super.onManagerConnected(status);
	            } break;
	        }
	    }
	};
	
	private EditText mScrollingTextBox;
	private Thread mTextLoadThread = new Thread() {
		@Override
		public void run() {
			InputStream othelloInput = getResources().openRawResource(R.raw.othello);
			
			try {
				Scanner s = new java.util.Scanner(othelloInput).useDelimiter("\\A");
			    mScrollingTextBox.setText(s.hasNext() ? s.next() : "");
				othelloInput.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_scroller);
		
		mScrollingTextBox = (EditText) findViewById(R.id.scrollingTextBox);
		mTextLoadThread.start();
		
		mGestureSensor = new GestureSensor(ScrollerActivity.this);
		mGestureScroller = new GestureScroller();
		mGestureSensor.addGestureListener(mGestureScroller);
		mGestureScroller.setHorizontalScrollEnabled(false);
		mGestureSensor.enableHorizontalScroll(false);
		
		
		OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this, mLoaderCallback);
		
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		
		ViewTreeObserver viewTreeObserver = mScrollingTextBox.getViewTreeObserver();
		if (viewTreeObserver.isAlive()) {
		  viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
			  @Override
			  public void onGlobalLayout() {
				  int [] scrollingTextBoxLoc = new int[2];
				  mScrollingTextBox.getLocationOnScreen(scrollingTextBoxLoc);
				  int xPos = mScrollingTextBox.getWidth() / 2 + scrollingTextBoxLoc[0];

				  mGestureScroller.setTopPosition(xPos, scrollingTextBoxLoc[1] + 10);
				  mGestureScroller.setBottomPosition(xPos, scrollingTextBoxLoc[1] + mScrollingTextBox.getHeight() - 10);
			  }
		  });
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.scroller, menu);
		return true;
	}
	
	@Override
	public void onWindowFocusChanged (boolean hasFocus) {
		if(!gestureStarted)
			return;
		
		if(hasFocus)
			mGestureSensor.start();
		else
			mGestureSensor.stop();
	}
}
