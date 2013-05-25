package edu.washington.cs.gesturecursortest;

import edu.washington.cs.touchfreelibrary.touchemulation.GestureCursorActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;

public class CursorActivity extends GestureCursorActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_cursor);
		initializeTouchFree(ClickSensorType.Microphone);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.cursor, menu);
		return true;
	}

	public void openScreen2(View view) {
		Intent intent = new Intent(this, Screen2.class);
	    startActivity(intent);
	}
}
