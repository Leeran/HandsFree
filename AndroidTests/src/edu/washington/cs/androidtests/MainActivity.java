package edu.washington.cs.androidtests;

import java.io.IOException;

import android.hardware.Camera;
import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;

public class MainActivity extends Activity {
	
	Camera mCamera;
	SurfaceView mCameraView;
	Button mCameraOnButton;
	Button mCameraOffButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        mCameraView = (SurfaceView) findViewById(R.id.cameraview);
        mCameraOnButton = (Button) findViewById(R.id.cameraonbutton);
        mCameraOffButton = (Button) findViewById(R.id.cameraoffbutton);
        
        mCameraOnButton.setEnabled(true);
        mCameraOffButton.setEnabled(false);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
    
    @Override
    public void onPause() {
    }
    
    public void onClickCameraOn(View view) {
    	// initialize the camera
        try {
        	mCamera = Camera.open(1);
        	if(mCamera == null)
        		throw new RuntimeException("error initializing camera");
        	mCamera.setPreviewDisplay(mCameraView.getHolder());
        	
        	mCamera.startPreview();
        	
        	mCameraOnButton.setEnabled(false);
        	mCameraOffButton.setEnabled(true);
        	
        } catch(RuntimeException e) {
        	// error opening camera
        } catch(IOException e) {
        	// issue with surface
        }
    }
    
    public void onClickCameraOff(View view) {
    	if(mCamera == null)
    		return;
    	
    	// destroy the camera
        try {
        	mCamera.stopPreview();
        	mCamera.release();
        	
        	mCamera = null;
        	
        	mCameraOnButton.setEnabled(true);
        	mCameraOffButton.setEnabled(false);
        	
        } catch(RuntimeException e) {
        	// error opening camera
        }
    }
}
