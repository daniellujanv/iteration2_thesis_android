package itu.dluj.tesisprototype_iteration2.gesturerecognition;

import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfInt4;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;

public class Tools {

	public static Scalar red = new Scalar(255,0,0);
	public static Scalar green = new Scalar(0,255,0);
	public static Scalar blue = new Scalar(0,0,255);
	public static Scalar magenta = new Scalar(255,0,255);
	public static Scalar gray = new Scalar(10, 10, 10);
	
	/*************************** Utility methods ************************************/

	/*
	 * Draws convexity Defects in color image
	 * params: convexity defects, center of hand contour, hand contour
	 * 
	 */
	public static Mat drawDefects(Mat mRgb, MatOfInt4 convexityDefects, MatOfPoint handContour) {
		int defects[] = convexityDefects.toArray();
		List<Point> contour = handContour.toList();
		Point centroid = getCentroid(handContour);
		/*
		 * convexityDefects -> structure containing (by order) start, end, depth_point, depth.
		 * depth-> farthest point (depth_point) distance to convex hull
		 */
		//		Point start;
		Point end;
		Point furthest;
		//		float depth;

		for(int i=0; i< defects.length; i=i+4){
			//				start = contour.get(defects[i]);
			end = contour.get(defects[i+1]);
			furthest = contour.get(defects[i+2]);
			//				depth = Math.round(defects[i+3]/256.0);

			//		        convexhull
			//				Core.line(mRgb, start, end, blue, 3);
			//		        line from farthest point to convexhull
			Core.line(mRgb, furthest, end, red, 1);
			//		        line from center of contour to convexhull point
			Core.line(mRgb, end, centroid, blue, 1);
			//		        points
			Core.circle(mRgb, end, 5, red, -1);
//			Core.circle(mRgb, furthest, 5, magenta, -1);
			//		        write distance between hull and farthest point
			//		        tools.setText(image, end, str(distance))
			//		        distanceCenterHull = tools.getDistanceBetweenPoints(center, end)
			//		        centerLine = tools.getMidPointInLine(center, end)
			//		        tools.setText(image, centerLine, str(distanceCenterHull))	
		}
		
		return mRgb;
	}

	/*
	 * Calculates the center of the contour
	 * Params: hand contour
	 * returns Point centroid -> centroid{x,y}
	 */
	public static Point getCentroid(MatOfPoint contour) {
		Moments moments = Imgproc.moments(contour);
		Point centroid = new Point();
		centroid.x = moments.get_m10() / moments.get_m00();
		centroid.y = moments.get_m01() / moments.get_m00();
		return centroid;
	}

	/*
	 * Utility method - writes to color image
	 */
	public static Mat writeToImage(Mat mRgb, int x, int y, final String string) {
		Core.putText(mRgb, string, new Point(x, y),Core.FONT_HERSHEY_SIMPLEX, 0.7, new Scalar(0,0,0), 5);
		Core.putText(mRgb, string, new Point(x, y),Core.FONT_HERSHEY_SIMPLEX, 0.7, new Scalar(255,255,255), 1);
		return mRgb;
	}

	/*
	 * Utility method - gets distance between two points
	 */
	public static double getDistanceBetweenPoints(Point one, Point two) {
		return Math.sqrt( Math.pow((two.x-one.x), 2) + Math.pow((two.y-one.y), 2));
	}

	/*
	 * Utility method - gets distance between two points
	 */
	public static Point getPointBetweenPoints(Point one, Point two) {
		Point result = new Point();
		result.x = (one.x + two.x)/2;
		result.y = (one.y + two.y)/2;
		return result;
	}
}
