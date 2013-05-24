package edu.washington.cs.touchfreelibrary.sensors;

import org.opencv.core.Point;

/**
 * Only used within the package to get data from JNI.
 * @author Leeran Raphaely <leeran.raphaely@gmail.com>
 */
public class MotionDetectionReturnValue {
	public Point averagePosition;
	public double fractionOfScreenInMotion;
	
	public MotionDetectionReturnValue(double x, double y, double fraction) {
		averagePosition = new Point(x, y);
		fractionOfScreenInMotion = fraction;
	}
}
