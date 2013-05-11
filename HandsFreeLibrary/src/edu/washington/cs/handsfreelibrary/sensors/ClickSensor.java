package edu.washington.cs.handsfreelibrary.sensors;

import java.util.LinkedList;
import java.util.List;

/**
 * Class <code>ClickSensor</code> is an abstract base class to all other click
 * sensors. It defines a list of click listeners, as well as the basic methods
 * used by outsiders to add and remove listeners. Classes derived from <code>
 * ClickSensor</code> may use the protected {@link onSensorClick() onSensorClick}
 * to call all the listeners at once.
 * 
 * @author Leeran Raphaely <leeran.raphaely@gmail.com>
 *
 */
public abstract class ClickSensor {
	private List<ClickSensorListener> mListeners;
	
	/**
	 * <code>ClickSensor</code>'s constructor. All derived classes are required to
	 * call this.
	 */
	protected ClickSensor() {
		mListeners = new LinkedList<ClickSensorListener>();
	}
	
	/**
	 * Adds a listener whose onSensorClick method will be called when a click is
	 * perceived.
	 * @param listener the <code>ClickSensorListener</code> to be added
	 */
	public void addListener(ClickSensorListener listener) {
		mListeners.add(listener);
	}
	
	/**
	 * Removes a listener so it will no longer be called when a click is perceived.
	 * @param listener the <code>ClickSensorListener</code> to be removed
	 */
	public void removeListener(ClickSensorListener listener) {
		mListeners.remove(listener);
	}
	
	/**
	 * Returns true if <code>listener</code> is currently listening for clicks
	 * from <code>this</code>.
	 * @param listener the <code>ClickSensorListener</code> who we are checking for local listener status
	 * @return
	 */
	public boolean isListener(ClickSensorListener listener) {
		return mListeners.contains(listener);
	}
	
	/**
	 * Removes all listeners from <code>this</code>.
	 */
	public void clearListeners() {
		mListeners.clear();
	}
	
	/**
	 * To be called by a derived class all listeners should have their <code>onSensorClick</code>
	 * methods called.
	 */
	protected void onSensorClick() {
		for(ClickSensorListener listener : mListeners) {
			listener.onSensorClick();
		}
	}
}
