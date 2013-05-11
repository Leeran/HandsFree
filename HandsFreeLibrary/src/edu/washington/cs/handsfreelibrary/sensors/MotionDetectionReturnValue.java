package edu.washington.cs.handsfreelibrary.sensors;

import org.opencv.core.Point;

public class MotionDetectionReturnValue {
	public Point averagePosition;
	public double fractionOfScreenInMotion;
	
	public MotionDetectionReturnValue(double x, double y, double fraction) {
		averagePosition = new Point(x, y);
		fractionOfScreenInMotion = fraction;
	}
}
