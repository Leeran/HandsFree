package edu.washington.cs.overlaytest;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class MainActivity extends Activity implements OnClickListener {
	protected static final String TAG = "MainActivity";
	
	private Button startServiceButton;
	private Button stopServiceButton;
	
	private Intent serviceIntent;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		startServiceButton = (Button)findViewById(R.id.startService);
		startServiceButton.setOnClickListener(this);
		
		stopServiceButton = (Button)findViewById(R.id.stopService);
		stopServiceButton.setOnClickListener(this);
		
		serviceIntent = new Intent(this, SystemOverlay.class);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	@Override
	public void onClick(View v) {
		if(v == startServiceButton) {
			startService(serviceIntent);
		}
		else if(v == stopServiceButton) {
			stopService(serviceIntent);
		}
	}

}
