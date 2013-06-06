package edu.washington.cs.touchfreelibraryusertesting;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import edu.washington.cs.touchfreelibrary.sensors.CameraGestureSensor;
import edu.washington.cs.touchfreelibrary.sensors.MicrophoneClickSensor;
import edu.washington.cs.touchfreelibrary.touchemulation.GestureScroller;
import edu.washington.cs.touchfreelibraryusertesting.MessageScreen.ContinueType;
import android.annotation.TargetApi;
import android.app.ActionBar;
import android.os.Bundle;
import android.content.Context;
import android.os.Build;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class TestingActivity extends FragmentActivity implements
		ActionBar.OnNavigationListener {
	private static final String TAG = "ScrollerActivity";
	private CameraGestureSensor mGestureSensor;
	private MicrophoneClickSensor mClickSensor;
	
	// set when the user leaves the intro page
	private long mUserId;
	
	private boolean mOpenCVInitiated = false;
	
	private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
	    @Override
	    public void onManagerConnected(int status) {
	        switch (status) {
	            case LoaderCallbackInterface.SUCCESS:
	            {
	                Log.i(TAG, "OpenCV loaded successfully");
	                
	                mOpenCVInitiated = true;
	                
	                CameraGestureSensor.loadLibrary();
	                
	                // more initialization steps go here
	                
	                mGestureSensor.start();
	                mClickSensor.start();
	            } break;
	            default:
	            {
	                super.onManagerConnected(status);
	            } break;
	        }
	    }
	};
	
	private AbstractTestingView[] mTestingViews;
	private int mCurrentViewID;

	/**
	 * The serialization (saved instance state) Bundle key representing the
	 * current dropdown position.
	 */
	private static final String STATE_SELECTED_NAVIGATION_ITEM = "selected_navigation_item";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mUserId = System.currentTimeMillis();
		
		mGestureSensor = new CameraGestureSensor(this);
		mClickSensor = new MicrophoneClickSensor();
		
		OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this, mLoaderCallback);
		
		setContentView(R.layout.activity_testing);

		// Set up the action bar to show a dropdown list.
		final ActionBar actionBar = getActionBar();
		actionBar.setDisplayShowTitleEnabled(false);
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);

		// Set up the dropdown list navigation in the action bar.
		actionBar.setListNavigationCallbacks(
		// Specify a SpinnerAdapter to populate the dropdown list.
				new ArrayAdapter<String>(getActionBarThemedContextCompat(),
						android.R.layout.simple_list_item_1,
						android.R.id.text1, new String[] {
								"Introduction",
								"Gesture Right Demo",
								"Gesture Left Demo", 
								"Gesture Up Demo", 
								"Gesture Down Demo", 
								"Gesture Practice Intro",
								"Practice All Gestures", 
								"Test Gestures" ,
								"Click Sensor Intro", 
								"Cursor Intro",
								"Cursor Activity Practice",
								"Cursor Activity Test",
								"Closing Statement"}), this);
		
		mTestingViews = new AbstractTestingView[13];
		mTestingViews[0] = new MessageScreen(this, getString(R.string.intro_text), ContinueType.Tap);
		mTestingViews[1] = new Demo1_GestureRight(this);
		mTestingViews[2] = new Demo2_GestureLeft(this);
		mTestingViews[3] = new Demo3_GestureUp(this);
		mTestingViews[4] = new Demo4_GestureDown(this);
		mTestingViews[5] = new MessageScreen(this, getString(R.string.gesture_test_intro), ContinueType.Tap);
		mTestingViews[6] = new Demo5_PracticeGestures(this);
		mTestingViews[7] = new Demo6_TestGestures(this);
		mTestingViews[8] = new MessageScreen(this, getString(R.string.click_sensor_intro), ContinueType.Clap);
		mTestingViews[9] = new Demo7_CursorIntro(this, mGestureSensor);
		mTestingViews[10] = new Demo8_CursorPractice(this, mGestureSensor, mClickSensor);
		mTestingViews[11] = new Demo9_CursorTest(this, mGestureSensor, mClickSensor);
		mTestingViews[12] = new MessageScreen(this, getString(R.string.closer), ContinueType.None);
		
		mCurrentViewID = 0;
		
		currentTestingView().startView();
		mGestureSensor.addGestureListener(currentTestingView());
		mClickSensor.addClickListener(currentTestingView());
	}
	
	private AbstractTestingView currentTestingView() {
		return mTestingViews[mCurrentViewID];
	}

	/**
	 * Backward-compatible version of {@link ActionBar#getThemedContext()} that
	 * simply returns the {@link android.app.Activity} if
	 * <code>getThemedContext</code> is unavailable.
	 */
	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	private Context getActionBarThemedContextCompat() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			return getActionBar().getThemedContext();
		} else {
			return this;
		}
	}

	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		// Restore the previously serialized current dropdown position.
		if (savedInstanceState.containsKey(STATE_SELECTED_NAVIGATION_ITEM)) {
			getActionBar().setSelectedNavigationItem(
					savedInstanceState.getInt(STATE_SELECTED_NAVIGATION_ITEM));
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		// Serialize the current dropdown position.
		outState.putInt(STATE_SELECTED_NAVIGATION_ITEM, getActionBar()
				.getSelectedNavigationIndex());
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.testing, menu);
		return true;
	}

	@Override
	public boolean onNavigationItemSelected(int position, long id) {
		currentTestingView().stopView();
		mGestureSensor.removeGestureListener(currentTestingView());
		mClickSensor.removeClickListener(currentTestingView());
		
		mCurrentViewID = position;
		
		mGestureSensor.addGestureListener(currentTestingView());
		mClickSensor.addClickListener(currentTestingView());
		currentTestingView().startView();
		return true;
	}
	
	public void goToNextScreen() {
		int oldPosition = getActionBar().getSelectedNavigationIndex();
		
		// set the User ID when they finish the intro screen.
		if(oldPosition == 0) {
			mUserId = System.currentTimeMillis();
		}
		
		if(oldPosition + 1 < getActionBar().getNavigationItemCount())
			getActionBar().setSelectedNavigationItem(oldPosition + 1);
	}
	
	public long getUserId() {
		return mUserId;
	}
	
	@Override
	public void onWindowFocusChanged (boolean hasFocus) {
		if(!mOpenCVInitiated)
			return;
		
		if(hasFocus) {
			mGestureSensor.start();
			mClickSensor.start();
		}
		else {
			mGestureSensor.stop();
			mClickSensor.stop();
		}
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
		if(!mOpenCVInitiated)
			return;
		
		mGestureSensor.start();
		mClickSensor.start();
	}
	
	
	@Override
	public void onPause() {
		super.onPause();
		
		if(!mOpenCVInitiated)
			return;
		
		mGestureSensor.stop();
		mClickSensor.stop();
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		if(!mOpenCVInitiated)
			return;
		
		mGestureSensor.stop();
		mClickSensor.stop();
	}
}
