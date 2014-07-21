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
		//		Point start;
		Point end;
		Point furthest;
		//distance between the furthest point and convex hull
		/*
		 * Removing defects
		 * we want to keep only defects above centroid of hand
		 */
		for(int i=0; i< defects.length; i=i+4){
			//				start = contour.get(defects[i]);
			end = contour.get(defects[i+1]);
			furthest = contour.get(defects[i+2]);
			//			distanceFurthest = Math.round(defects[i+3]/256.0);
			//top left screen x=0,y=0. Otherwise would be end.x > centroid.x
			if(end.y <= centroid.y){
				double distanceCenterHull = Tools.getDistanceBetweenPoints(centroid, end);
				double distanceFurthestPoint = Tools.getDistanceBetweenPoints(end, furthest);
				double relationCenterPoint_FurthestPoint = (distanceCenterHull/distanceFurthestPoint);
//				Log.i("ImageInteraction","Defects Init::"
//						+ " relCenter_Furthest: "+relationCenterPoint_FurthestPoint
//						);
				if((relationCenterPoint_FurthestPoint < 2.0)){
					//				        points
//					Core.circle(mRgb, end, 5, red, -1);
					positiveDefects = positiveDefects + 1;
				}
			}
		}
		//		Log.i("ImageInteraction", "Init gesture::defect number -> "+ positiveDefects);
		if(positiveDefects >= 4){
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
		//		Point start;
		Point end;
		Point furthest;
		//		float distanceFurthest;
		/*
		 * Removing defects
		 * we want to keep only defects above centroid of hand
		 */
		for(int i=0; i< defects.length; i=i+4){
			//				start = contour.get(defects[i]);
			end = contour.get(defects[i+1]);
			furthest = contour.get(defects[i+2]);
			//			distanceFurthest = Math.round(defects[i+3]/256.0);
			//top left screen x=0,y=0. Otherwise would be end.x > centroid.x
			if(end.y <= centroid.y && furthest.y <= centroid.y){
				double distanceCenterPoint = Tools.getDistanceBetweenPoints(centroid, end);
				double distanceFurthestPoint = Tools.getDistanceBetweenPoints(end, furthest);
				double relationCenterPoint_FurthestPoint = (distanceCenterPoint/distanceFurthestPoint);
//				Log.i("ImageInteraction","End Defects::"
//						+ " relCenter_furthest: "+relationCenterPoint_FurthestPoint
//						);
				if((relationCenterPoint_FurthestPoint >= 3.0)){
					//				        points
//					Core.circle(mRgb, end, 5, red, -1);
					positiveDefects = positiveDefects + 1;
				}
			}
		}
//		Log.i("ImageInteraction", "End gesture::defect number -> "+ positiveDefects);
		if(positiveDefects >= 4){
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
		//Point start;
		Point end;
		Point furthest;
		//		double distancePointHull = 0;
		double avgDistanceCenterPoint = 0;
		int positiveDefects = 0;
		int negativeDefects = 0;
		Log.i("Gestures::PointSelect", "PointSelect gesture::beginning");
		/*
		 * Look for PointSelect_Init gesture :: 1 finger lifted up
		 * - Removing defects
		 * -- we want to keep only defects above centroid of hand
		 */
		for(int i=0; i< defects.length; i=i+4){
			//						start = contour.get(defects[i]);
			end = contour.get(defects[i+1]);
			furthest = contour.get(defects[i+2]);
			//			distancePointHull = defects[i+3]/256.0;
			//top left screen x=0,y=0. Otherwise would be end.x > centroid.x
			if( (end.y < centroid.y && furthest.y < centroid.y) && (end.y < furthest.y)){
				double distanceCenterPoint = Tools.getDistanceBetweenPoints(centroid, end);
				//					double relationCenterHull_EndHull = (distanceCenterPoint/distancePointHull);
				//					double distClosestPerimeter = Imgproc.pointPolygonTest(m2fHandContour, centroid, true);
				//					double relationCenterHull_distClosestPerimeter = distanceCenterPoint/distClosestPerimeter;
				double distanceEndFurthest = Tools.getDistanceBetweenPoints(end, furthest);
				double relationCenter_EndFurtherst = distanceCenterPoint/distanceEndFurthest;
				if(relationCenter_EndFurtherst < 2.0){
					Log.i("Gestures::PointSelect", "center_endFurtherst:: "+relationCenter_EndFurtherst);
					finalDefects.add(end);
					positiveDefects = positiveDefects + 1;
				}else{
					negativeDefects = negativeDefects + 1;
					avgDistanceCenterPoint = avgDistanceCenterPoint + distanceCenterPoint;  
				}
			}
			avgDistanceCenterPoint = (negativeDefects != 0)? avgDistanceCenterPoint/negativeDefects : 1;
		}
		if(!initDetected){
			//relation between length of positive defect and average length of negative defects
			if(finalDefects.size() == 1){
				Point defect_one = finalDefects.get(0);
				double relationPositive_AvgNegative = Tools.getDistanceBetweenPoints(centroid, defect_one)/avgDistanceCenterPoint;
				Log.i("Gestures::PointSelect", "positive_avgNegative::"+ relationPositive_AvgNegative
						+ " negatives::"+negativeDefects);
				if(relationPositive_AvgNegative > 50.0){
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
				// _2/_1 because defects are obtained clockwise. finger to the right is pointing finger and
				//should be smaller giving the correct result... otherwise _1/_2 > 1.0
				double relationDistanceCenterPoints = distPointCenter_2 / distPointCenter_1;
				Log.i("Gestures::PointSelect_end", "relationDistCenter::"+ relationDistanceCenterPoints
						+ " point1::"+distPointCenter_1
						+ " point2::"+distPointCenter_2
						);
				if(relationDistanceCenterPoints < 1.0){
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
		//Point start;
		Point end;
		Point furthest;
//		double distancePointHull;

		Log.i("Gestures::Swipe", "Swipe gesture::beginning");
		/*
		 * Look for PointSelect_Init gesture :: 1 finger lifted up
		 * - Removing defects
		 * -- we want to keep only defects above centroid of hand
		 */
		for(int i=0; i< defects.length; i=i+4){
			//						start = contour.get(defects[i]);
			end = contour.get(defects[i+1]);
			furthest = contour.get(defects[i+2]);
//			distancePointHull = defects[i+3]/256.0;
			//top left screen x=0,y=0. Otherwise would be end.x > centroid.x
			if( (end.y < centroid.y && furthest.y < centroid.y) && (end.y < furthest.y)){
				double distanceCenterPoint = Tools.getDistanceBetweenPoints(centroid, end);
				//					double relationCenterHull_EndHull = (distanceCenterPoint/distancePointHull);
				//					double distClosestPerimeter = Imgproc.pointPolygonTest(m2fHandContour, centroid, true);
				//					double relationCenterHull_distClosestPerimeter = distanceCenterPoint/distClosestPerimeter;
				double distanceEndFurthest = Tools.getDistanceBetweenPoints(end, furthest);
				double relationCenter_EndFurtherst = distanceCenterPoint/distanceEndFurthest;
				if(relationCenter_EndFurtherst < 2.0){
					Log.i("Gestures::Swipe", "center_endFurtherst:: "+relationCenter_EndFurtherst);
					finalDefects.add(end);
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
			Log.i("Gestures::Swipe", "relationDistCenter::"+ relDistanceCenterPoint
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
		int negativeDefects = 0;
		int defects[] = convexityDefects.toArray();
		List<Point> contour = handContour.toList();
		List<Point> finalDefects = new ArrayList<Point>();
		Point centroid = Tools.getCentroid(handContour);
		/*
		 * convexityDefects -> structure containing (by order) start, end, depth_point, depth.
		 * depth-> farthest point (depth_point) distance to convex hull
		 */
		//		Point start;
		Point end;
		Point furthest;
		double distanceCenterHull;
		//		Log.i("ImageInteraction", "Rotate gesture::beginning");
		/*
		 * Look for Rotate_Init gesture
		 * - Removing defects
		 * -- we want to keep only defects above centroid of hand
		 */
		for(int i=0; i< defects.length; i=i+4){
			//					start = contour.get(defects[i]);
			end = contour.get(defects[i+1]);
			furthest = contour.get(defects[i+2]);
			distanceCenterHull = Tools.getDistanceBetweenPoints(centroid, end);
			double distanceFurthestPoint = Tools.getDistanceBetweenPoints(end, furthest);
			double relationCenterPoint_FurthestPoint = distanceCenterHull/distanceFurthestPoint; 
			//top left screen x=0,y=0. Otherwise would be end.x > centroid.x
			if( (end.y <= centroid.y) ){
				Log.i("Gestures::Rotate",
						"relCenterPoint_FurthestPoint: " + relationCenterPoint_FurthestPoint
						);
				if(relationCenterPoint_FurthestPoint < 3.0){
					finalDefects.add(end);
					positiveDefects = positiveDefects + 1;
				}
				else{
					negativeDefects = negativeDefects + 1;
				}
			}
		}
		Log.i("Gestures::Rotate","finalDefects: "+ finalDefects.size()
				);			

		if(positiveDefects == 2 && negativeDefects == 0){
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
		//		Point start;
		Point end;
		Point furthest;
		double distanceCenterHull;
		//		Log.i("ImageInteraction", "Zoom gesture::beginning");
		/*
		 * Look for Zoom_Init gesture
		 * - Removing defects
		 * -- we want to keep only defects above centroid of hand
		 */
		for(int i=0; i< defects.length; i=i+4){
			//					start = contour.get(defects[i]);
			end = contour.get(defects[i+1]);
			furthest = contour.get(defects[i+2]);
			distanceCenterHull = Tools.getDistanceBetweenPoints(centroid, end);
			//top left screen x=0,y=0. Otherwise would be end.x > centroid.x
			double distanceFurthestPoint = Tools.getDistanceBetweenPoints(end, furthest);
			double relationCenterPoint_FurthestPoint = distanceCenterHull/distanceFurthestPoint; 
			//			Log.i("ImageInteraction","Defects Zoom:: " 
			//					+ "relCenterHull_FurthestHull: "+relationCenterPoint_FurthestPoint
			//					+ " centerHull" + distanceCenterHull
			//					+ " furthest" + distanceFurthest
			//					);			//top left screen x=0,y=0. Otherwise would be end.x > centroid.x
			if( (end.y <= centroid.y) ){
				if((relationCenterPoint_FurthestPoint < 2.0)){
					finalDefects.add(end);
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
