package edu.washington.cs.overlaytest;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import edu.washington.cs.handsfreelibrary.GestureSensor;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Toast;

public class SystemOverlay extends Service implements GestureSensor.Listener {
	protected static final String TAG = "SystemOverlay";
	
	private OverlayView mView;
	private GestureSensor mGestureSensor;
	
	private Point mPosition;
	
	private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
	    @Override
	    public void onManagerConnected(int status) {
	        switch (status) {
	            case LoaderCallbackInterface.SUCCESS:
	            {
	                Log.i(TAG, "OpenCV loaded successfully");
	                
	                GestureSensor.loadLibrary();
	                
	                // more initialization steps go here.
	                mGestureSensor.startSensor();
	                
	            } break;
	            default:
	            {
	                super.onManagerConnected(status);
	            } break;
	        }
	    }
	};

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        
        mGestureSensor = new GestureSensor();
        mGestureSensor.setGestureListener(this);
        
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this, mLoaderCallback);
        mView = new OverlayView(this);
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
                0,
//		              WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
//		                      | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.RIGHT | Gravity.TOP;
        params.setTitle("Load Average");
        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        wm.addView(mView, params);
        
        mPosition = new Point(mView.getWidth() / 2, mView.getHeight() / 2);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(mView != null)
        {
            ((WindowManager) getSystemService(WINDOW_SERVICE)).removeView(mView);
            mView = null;
        }
        mGestureSensor.stopSensor();
    }
    
    class OverlayView extends ViewGroup {
	    private Paint mLoadPaint;

	    public OverlayView(Context context) {
	        super(context);

	        mLoadPaint = new Paint();
	        mLoadPaint.setAntiAlias(true);
	        mLoadPaint.setTextSize(10);
	        mLoadPaint.setARGB(255, 255, 0, 0);
	    }

	    @Override
	    protected void onDraw(Canvas canvas) {
	        super.onDraw(canvas);
	        //canvas.drawText("Hello World", 5, 15, mLoadPaint);
	        canvas.drawCircle(mPosition.x, mPosition.y, 20, mLoadPaint);
	    }

	    @Override
	    protected void onLayout(boolean arg0, int arg1, int arg2, int arg3, int arg4) {
	    }

	    @Override
	    public boolean onTouchEvent(MotionEvent event) {
	        //return super.onTouchEvent(event);
	        Toast.makeText(getContext(),"onTouchEvent", Toast.LENGTH_LONG).show();
	        return true;
	    }
	}

	@Override
	public void onGestureUp() {
		mPosition.y -= 40;
		mView.postInvalidate();
	}

	@Override
	public void onGestureDown() {
		mPosition.y += 40;
		mView.postInvalidate();
	}

	@Override
	public void onGestureLeft() {
		mPosition.x -= 40;
		mView.postInvalidate();
	}

	@Override
	public void onGestureRight() {
		mPosition.x += 40;
		mView.postInvalidate();
	}

}
