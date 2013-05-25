package edu.washington.cs.gesturecursortest;

import edu.washington.cs.touchfreelibrary.touchemulation.GestureCursorActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;

public class Screen2 extends GestureCursorActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_screen2);
		initializeTouchFree(ClickSensorType.Microphone);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.screen2, menu);
		return true;
	}

	public void closeClicked(View view) {
		finish();
	}
}
