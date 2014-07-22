package itu.dluj.tesisprototype_iteration2.gesturerecognition;

import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt4;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;

import android.util.Log;

public class Gestures {

	public static int secondsToWait = 2;
	private static String TAG = "Gestures::";

	/*
	 * Detection of Init gesture
	 */
	public static boolean detectInitGesture(MatOfInt4 convexityDefects, MatOfPoint handContour) {
		int positiveDefects = 0;
		int defects[] = convexityDefects.toArray();
		List<Point> contour = handContour.toList();
		Point centroid = Tools.getCentroid(handContour);
		//		MatOfPoint2f m2fHandContour = new MatOfPoint2f(handContour.toArray());
		/*
		 * convexityDefects -> structure containing (by order) start, end, depth_point, depth.
		 * depth-> farthest point (depth_point) distance to convex hull
		 */
				Point start;
//		Point end;
		Point furthest;
		//distance between the furthest point and convex hull
		/*
		 * Removing defects
		 * we want to keep only defects above centroid of hand
		 */
		for(int i=0; i< defects.length; i=i+4){
			start = contour.get(defects[i]);
//			end = contour.get(defects[i+1]);
			furthest = contour.get(defects[i+2]);
			//			distanceFurthest = Math.round(defects[i+3]/256.0);
			//top left screen x=0,y=0. Otherwise would be end.x > centroid.x
			if((start.y < centroid.y && furthest.y < centroid.y) && (start.y < furthest.y)){
				//distance between the center of hand and defect
				double distanceCenterPoint = Tools.getDistanceBetweenPoints(centroid, start);
				//distance between the start of defect and furthest point
				double distanceFurthestPoint = Tools.getDistanceBetweenPoints(start, furthest);
				//relation of distances from 1) center of hand to defect and 2)start of defect to furthest point 
				double relationCenterPoint_FurthestPoint = (distanceCenterPoint/distanceFurthestPoint);
				Log.i(TAG+"Init", "relCenter_Furthest: "+relationCenterPoint_FurthestPoint
						);
				if((relationCenterPoint_FurthestPoint < 2.0)){
					//				        points
//					Core.circle(mRgb, end, 5, red, -1);
					positiveDefects = positiveDefects + 1;
				}
			}
		}
		//		Log.i("ImageInteraction", "Init gesture::defect number -> "+ positiveDefects);
		if(positiveDefects >= 3){
			return true;
		}else{
			return false;
		}
	}

	/*
	 * Detection of End gesture
	 */
	public static boolean detectEndGesture(MatOfInt4 convexityDefects, MatOfPoint handContour) {
		int positiveDefects = 0;
		int defects[] = convexityDefects.toArray();
		List<Point> contour = handContour.toList();
		Point centroid = Tools.getCentroid(handContour);
		//		MatOfPoint2f m2fHandContour = new MatOfPoint2f(handContour.toArray());

		/*
		 * convexityDefects -> structure containing (by order) start, end, depth_point, depth.
		 * depth-> farthest point (depth_point) distance to convex hull
		 */
		Point start;
//		Point end;
		Point furthest;
		//		float distanceFurthest;
		/*
		 * Removing defects
		 * we want to keep only defects above centroid of hand
		 */
		for(int i=0; i< defects.length; i=i+4){
			start = contour.get(defects[i]);
//			end = contour.get(defects[i+1]);
			furthest = contour.get(defects[i+2]);
			//			distanceFurthest = Math.round(defects[i+3]/256.0);
			//top left screen x=0,y=0. Otherwise would be end.x > centroid.x
//			if(start.y < centroid.y && furthest.y < centroid.y){
			if((start.y < centroid.y && furthest.y < centroid.y) && (start.y < furthest.y)){	
				//distance between the center of hand and defect
				double distanceCenterPoint = Tools.getDistanceBetweenPoints(centroid, start);
				//distance between the start of defect and furthest point
				double distanceFurthestPoint = Tools.getDistanceBetweenPoints(start, furthest);
				//relation of distances from 1) center of hand to defect and 2)start of defect to furthest point 
				double relationCenterPoint_FurthestPoint = (distanceCenterPoint/distanceFurthestPoint);
				Log.i(TAG+"End", "relCenter_furthest: "+relationCenterPoint_FurthestPoint
						);
				if((relationCenterPoint_FurthestPoint >= 3.0)){
					//				        points
//					Core.circle(mRgb, end, 5, red, -1);
					positiveDefects = positiveDefects + 1;
				}
			}
		}
//		Log.i("ImageInteraction", "End gesture::defect number -> "+ positiveDefects);
		if(positiveDefects >= 3){
			return true;
		}else{
			return false;
		}
	}

	/*
	 * Detection of PointSelect gesture
	 */
	public static Point detectPointSelectGesture(MatOfInt4 convexityDefects, MatOfPoint handContour, boolean initDetected) {
		int defects[] = convexityDefects.toArray();
		List<Point> contour = handContour.toList();
		List<Point> finalDefects = new ArrayList<Point>();
		Point centroid = Tools.getCentroid(handContour);
		//		MatOfPoint2f m2fHandContour = new MatOfPoint2f(handContour.toArray());

		/*
		 * convexityDefects -> structure containing (by order) start, end, depth_point, depth.
		 * depth-> farthest point (depth_point) distance to convex hull
		 */
		Point start;
//		Point end;
		Point furthest;
		//		double distancePointHull = 0;
		double avgDistanceCenterPoint_negative = 0;
		int positiveDefects = 0;
		int negativeDefects = 0;
		Log.i(TAG+"PointSelect", "PointSelect gesture::beginning");
		/*
		 * Look for PointSelect_Init gesture :: 1 finger lifted up
		 * - Removing defects
		 * -- we want to keep only defects above centroid of hand
		 */
		for(int i=0; i< defects.length; i=i+4){
			start = contour.get(defects[i]);
//			end = contour.get(defects[i+1]);
			furthest = contour.get(defects[i+2]);
			//			distancePointHull = defects[i+3]/256.0;
			//top left screen x=0,y=0. Otherwise would be end.x > centroid.x
			if( (start.y < centroid.y && furthest.y < centroid.y) && (start.y < furthest.y)){
				//distance between the center of hand and defect
				double distanceCenterPoint = Tools.getDistanceBetweenPoints(centroid, start);
				//					double relationCenterHull_EndHull = (distanceCenterPoint/distancePointHull);
				//					double distClosestPerimeter = Imgproc.pointPolygonTest(m2fHandContour, centroid, true);
				//					double relationCenterHull_distClosestPerimeter = distanceCenterPoint/distClosestPerimeter;
				//distance between the start of defect and furthest point
				double distanceFurthestPoint = Tools.getDistanceBetweenPoints(start, furthest);
				//relation of distances from 1) center of hand to defect and 2)start of defect to furthest point 
				double relationCenterPoint_FurthestPoint = distanceCenterPoint/distanceFurthestPoint;
				Log.i(TAG+"PointSelect", "center_endFurtherst:: "+relationCenterPoint_FurthestPoint);
				if(relationCenterPoint_FurthestPoint < 2.0){
					finalDefects.add(start);
					positiveDefects = positiveDefects + 1;
				}else{
					negativeDefects = negativeDefects + 1;
					avgDistanceCenterPoint_negative = avgDistanceCenterPoint_negative + distanceCenterPoint;  
				}
			}
			avgDistanceCenterPoint_negative = (negativeDefects != 0)? avgDistanceCenterPoint_negative/negativeDefects : 1;
		}
		if(!initDetected){
			//relation between length of positive defect and average length of negative defects
			if(finalDefects.size() == 1){
				Point defect_one = finalDefects.get(0);
				double relationPositive_AvgNegative = Tools.getDistanceBetweenPoints(centroid, defect_one)/avgDistanceCenterPoint_negative;
				Log.i(TAG+"PointSelect", "positive_avgNegative::"+ relationPositive_AvgNegative
						+ " negatives::"+negativeDefects);
				if(relationPositive_AvgNegative > 5.0){
//					Core.circle(mRgb, defect_one, 5, Tools.red, -1);
//					lastPointedLocation = defect_one;
//					return true;
//					
					return defect_one;				
				}
			}
		}else{
//			if(finalDefects.size() == 1){
//				//return false to stay in the pointselect_init state but keep the record of the detected finger
//				Point defect_one = finalDefects.get(0);
////				Core.circle(mRgb, defect_one, 5, Tools.red, -1);
////				lastPointedLocation = defect_one;
////				timeLastDetectedGest = System.currentTimeMillis();
////				return false;
//				return defect_one;
//			}else 
				if(finalDefects.size() == 2){
//				Core.circle(mRgb, lastPointedLocation, 5, Tools.magenta, -1);
				Point defect_one = finalDefects.get(0);
				Point defect_two = finalDefects.get(1);
				double distPointCenter_2 = Tools.getDistanceBetweenPoints(centroid, defect_two);
				double distPointCenter_1 = Tools.getDistanceBetweenPoints(centroid, defect_one);
				//relation of the distance between the points and the center
				//should be less than 1 because fingers are almost the same size
				double relationDistanceCenterPoints;
				if(distPointCenter_1 > distPointCenter_2){
					relationDistanceCenterPoints = distPointCenter_1 / distPointCenter_2;
				}else{
					relationDistanceCenterPoints = distPointCenter_2 / distPointCenter_1;
				}
				Log.i(TAG+"PointSelect_end", "relationDistCenter::"+ relationDistanceCenterPoints
						+ " point1::"+distPointCenter_1
						+ " point2::"+distPointCenter_2
						);
				if(relationDistanceCenterPoints < 1.5){
					//doesn't matter what i return 
					// what matters is the lastPointedLocation from the first detection
					return defect_one;
				}
//				Core.circle(mRgb, defect_one, 5, Tools.red, -1);
//				Core.circle(mRgb, defect_two, 5, Tools.red, -1);
//				return true;
			}	
		}
//		return false;
		return null;
	}

	/*
	 * Detection of PointSelect gesture
	 */
	public static Point detectSwipeGesture(MatOfInt4 convexityDefects, MatOfPoint handContour, boolean initDetected) {
		int positiveDefects = 0;
		int defects[] = convexityDefects.toArray();
		List<Point> contour = handContour.toList();
		List<Point> finalDefects = new ArrayList<Point>();
		Point centroid = Tools.getCentroid(handContour);
//		MatOfPoint2f m2fHandContour = new MatOfPoint2f(handContour.toArray());

		/*
		 * convexityDefects -> structure containing (by order) start, end, depth_point, depth.
		 * depth-> farthest point (depth_point) distance to convex hull
		 */
		Point start;
//		Point end;
		Point furthest;
//		double distancePointHull;
//		Log.i("Gestures::Swipe", "Swipe gesture::beginning");
		/*
		 * - Removing defects
		 * -- we want to keep only defects above centroid of hand
		 */
		for(int i=0; i< defects.length; i=i+4){
			start = contour.get(defects[i]);
//			end = contour.get(defects[i+1]);
			furthest = contour.get(defects[i+2]);
//			distancePointHull = defects[i+3]/256.0;
			//top left screen x=0,y=0. Otherwise would be end.x > centroid.x
			if( (start.y < centroid.y && furthest.y < centroid.y) && (start.y < furthest.y)){
				//distance between the center of hand and defect
				double distanceCenterPoint = Tools.getDistanceBetweenPoints(centroid, start);
				//					double relationCenterHull_EndHull = (distanceCenterPoint/distancePointHull);
				//					double distClosestPerimeter = Imgproc.pointPolygonTest(m2fHandContour, centroid, true);
				//					double relationCenterHull_distClosestPerimeter = distanceCenterPoint/distClosestPerimeter;
				//distance between the start of defect and furthest point
				double distanceEndFurthest = Tools.getDistanceBetweenPoints(start, furthest);
				//relation of distances from 1) center of hand to defect and 2)start of defect to furthest point 
				double relationCenter_EndFurtherst = distanceCenterPoint/distanceEndFurthest;
				if(relationCenter_EndFurtherst < 2.0){
					Log.i(TAG+"Swipe", "center_endFurtherst:: "+relationCenter_EndFurtherst);
					finalDefects.add(start);
					positiveDefects = positiveDefects + 1;
				}
			}
		}
		if(finalDefects.size() == 2){
			Point defect_one = finalDefects.get(0);
			Point defect_two = finalDefects.get(1);
			double distanceCenterPoint_1 = Tools.getDistanceBetweenPoints(defect_one, centroid);
			double distanceCenterPoint_2 = Tools.getDistanceBetweenPoints(defect_two, centroid);
			//relation of the distance between the points and the center
			//should be less than 1 because fingers are almost the same size
			// _2/_1 because defects are obtained clockwise. finger to the right is pointing finger and
			//should be smaller giving the correct result... otherwise _1/_2 > 1.0
			double relDistanceCenterPoint = distanceCenterPoint_2 / distanceCenterPoint_1;
			Log.i(TAG+"Swipe", "relationDistCenter::"+ relDistanceCenterPoint
					+ " point1::"+distanceCenterPoint_1
					+ " point2::"+distanceCenterPoint_2
					);
			if(relDistanceCenterPoint < 1.0){
				//			Core.circle(mRgb, defect_one, 5, Tools.red, -1);
				//			Core.circle(mRgb, defect_two, 5, Tools.red, -1);
				return defect_one;
			}
		}	

		return null;
	}

	/*
	 * Detection of Rotation gesture
	 */
	public static Point detectRotateGesture(MatOfInt4 convexityDefects,	MatOfPoint handContour, boolean initDetected) {
		int positiveDefects = 0;
//		int negativeDefects = 0;
		int defects[] = convexityDefects.toArray();
		List<Point> contour = handContour.toList();
		List<Point> finalDefects = new ArrayList<Point>();
		Point centroid = Tools.getCentroid(handContour);
		/*
		 * convexityDefects -> structure containing (by order) start, end, depth_point, depth.
		 * depth-> farthest point (depth_point) distance to convex hull
		 */
		Point start;
//		Point end;
		Point furthest;
		//		Log.i("ImageInteraction", "Rotate gesture::beginning");
		/*
		 * Look for Rotate_Init gesture
		 * - Removing defects
		 * -- we want to keep only defects above centroid of hand
		 */
		for(int i=0; i< defects.length; i=i+4){
			start = contour.get(defects[i]);
//			end = contour.get(defects[i+1]);
			furthest = contour.get(defects[i+2]);
			//distance between the center of hand and defect
			double distanceCenterPoint = Tools.getDistanceBetweenPoints(centroid, start);
			//distance between the start of defect and furthest point
			double distanceFurthestPoint = Tools.getDistanceBetweenPoints(start, furthest);
			//relation of distances from 1) center of hand to defect and 2)start of defect to furthest point 
			double relationCenterPoint_FurthestPoint = distanceCenterPoint/distanceFurthestPoint; 
			//top left screen x=0,y=0. Otherwise would be end.x > centroid.x
			if( (start.y < centroid.y && furthest.y < centroid.y) && (start.y < furthest.y)){
				Log.i(TAG+"Rotate",
						"relCenterPoint_FurthestPoint: " + relationCenterPoint_FurthestPoint
						);
				if(relationCenterPoint_FurthestPoint < 2.0){
					finalDefects.add(start);
					positiveDefects = positiveDefects + 1;
				}
//				else{
//					negativeDefects = negativeDefects + 1;
//				}
			}
		}
		Log.i(TAG+"Rotate","finalDefects: "+ finalDefects.size()
				);			

		if(positiveDefects == 2 
//				&& negativeDefects == 0
				){
			Point defect_one = finalDefects.get(0);
			Point defect_two = finalDefects.get(1);
			double distanceCenterHull_one = Tools.getDistanceBetweenPoints(centroid, defect_one);
			double distanceCenterHull_two = Tools.getDistanceBetweenPoints(centroid, defect_two);
			double distanceOneTwo = Tools.getDistanceBetweenPoints(defect_one, defect_two);
			//				Core.line(mRgb, defect_one, centroid, blue, 3);
			//				//			        line from center of contour to convexhull point
			//				Core.line(mRgb, defect_two, centroid, blue, 3);
			//				Core.line(mRgb, defect_two, defect_one, blue, 3);
			//			        points
//			Core.circle(mRgb, defect_one, 5, Tools.red, -1);
//			Core.circle(mRgb, defect_two, 5, Tools.red, -1);
			//distance between fingers has to be greather than 1.3 times the distance from the
			//center to both points
			if((distanceOneTwo >= distanceCenterHull_one ) && (distanceOneTwo >= distanceCenterHull_two)){
				if(!initDetected){

					Point detectedPoint = Tools.getPointBetweenPoints(defect_one, defect_two);
					//					Core.circle(mRgb, rotateInitPos, 5, Tools.magenta, -1);
					return detectedPoint;
				}else{
					Point detectedPoint = Tools.getPointBetweenPoints(defect_one, defect_two);
					//					Log.i("null-check", "init::"+ rotateInitPos.toString());
					//					Log.i("null-check", " end::"+rotateEndPos.toString());
					return detectedPoint;
				}
			}
			//			Log.i("ImageInteraction", "Rotate gesture::defect number -> "+ positiveDefects);
		}
		return null;
	}

	/*
	 * Detection of Zoom gesture
	 */
	public static double detectZoomGesture(MatOfInt4 convexityDefects,	MatOfPoint handContour, boolean initDetected) {
		int positiveDefects = 0;
		int negativeDefects = 0;
		int defects[] = convexityDefects.toArray();
		List<Point> contour = handContour.toList();
		List<Point> finalDefects = new ArrayList<Point>();
		Point centroid = Tools.getCentroid(handContour);
		/*
		 * convexityDefects -> structure containing (by order) start, end, depth_point, depth.
		 * depth-> farthest point (depth_point) distance to convex hull
		 */
		Point start;
//		Point end;
		Point furthest;
		//		Log.i("ImageInteraction", "Zoom gesture::beginning");
		/*
		 * Look for Zoom_Init gesture
		 * - Removing defects
		 * -- we want to keep only defects above centroid of hand
		 */
		for(int i=0; i< defects.length; i=i+4){
			start = contour.get(defects[i]);
//			end = contour.get(defects[i+1]);
			furthest = contour.get(defects[i+2]);
			//distance between the center of hand and defect
			double distanceCenterHull = Tools.getDistanceBetweenPoints(centroid, start);
			//top left screen x=0,y=0. Otherwise would be end.x > centroid.x
			//distance between the start of defect and furthest point
			double distanceFurthestPoint = Tools.getDistanceBetweenPoints(start, furthest);
			//relation of distances from 1) center of hand to defect and 2)start of defect to furthest point 
			double relationCenterPoint_FurthestPoint = distanceCenterHull/distanceFurthestPoint; 
			//			Log.i("ImageInteraction","Defects Zoom:: " 
			//					+ "relCenterHull_FurthestHull: "+relationCenterPoint_FurthestPoint
			//					+ " centerHull" + distanceCenterHull
			//					+ " furthest" + distanceFurthest
			//					);			//top left screen x=0,y=0. Otherwise would be end.x > centroid.x
			if((start.y < centroid.y && furthest.y < centroid.y) && (start.y < furthest.y)){
				if((relationCenterPoint_FurthestPoint < 2.0)){
					finalDefects.add(start);
					positiveDefects = positiveDefects + 1;
				}else{
					negativeDefects = negativeDefects + 1;
				}
			}
		}
		//		Log.i("ImageInteraction","Defects Zoom :: finalDefects: "+ finalDefects.size()
		//				);			
		if(positiveDefects == 2 && negativeDefects == 0){
			Point defect_one = finalDefects.get(0);
			Point defect_two = finalDefects.get(1);
			double distanceCenterHull_one = Tools.getDistanceBetweenPoints(centroid, defect_one);
			double distanceCenterHull_two = Tools.getDistanceBetweenPoints(centroid, defect_two);
			double distanceOneTwo = Tools.getDistanceBetweenPoints(defect_one, defect_two);
			//			        points
//			Core.circle(mRgb, defect_one, 5, Tools.red, -1);
//			Core.circle(mRgb, defect_two, 5, Tools.red, -1);
			//-distance between the fingers has to be less than 1.3 times the distance 
			//from the center to the end of both fingers
			if(!initDetected){
				if((distanceOneTwo < distanceCenterHull_one ) && (distanceOneTwo < distanceCenterHull_two)){
					double detectedDistance = Tools.getDistanceBetweenPoints(defect_one, defect_two);
					return detectedDistance;
				}
			}else{
				double zoomEndDistance = Tools.getDistanceBetweenPoints(defect_one, defect_two);
				return zoomEndDistance;
			}
		}
		return -1;
	}


}
