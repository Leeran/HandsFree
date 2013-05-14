package edu.washington.cs.handsfreelibrary.sensors;

/**
 * Listener for classes that derive from ClickSensor.
 * @author Leeran Raphaely <leeran.raphaely@gmail.com>
 */
public interface ClickSensorListener {
	/**
	 * Called by a {@link ClickSensor} when a click is sensed.
	 * @param caller The <code>ClickSensor</code> that called the click.
	 */
	public void onSensorClick(ClickSensor caller);
}
