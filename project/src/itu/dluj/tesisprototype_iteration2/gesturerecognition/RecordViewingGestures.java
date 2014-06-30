package itu.dluj.tesisprototype_iteration2.gesturerecognition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfInt4;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

public class RecordViewingGestures {

	public Scalar red;
	public Scalar green;
	public Scalar blue;
	public HashMap<String, Boolean> navigationStates;
	public Mat mRgb;
	public Mat mHsv;
	public Mat mProcessed;
	public Mat kernel;

	public List<MatOfPoint> contours;
	public MatOfPoint handContour;
	public Mat hierarchy;
	public MatOfInt convexHull;
	public Point handContourCentroid;
	public MatOfInt4 convexityDefects;

	private int screenArea;
	private int screenHeight;
	private int screenWidth;
	private long timeLastDetectedGest;

	private String currentState;
	private String sStateInit = "Init";
	private String sStateEnd = "End";
	private String sStateZero = "Zipou";
	private String sStateSwipe = "Swipe";
	private String sStatePointSelect = "PointSelect";

	private Context appContext;
	private Activity mainActivity;
	private Toast tToastMsg;


	public RecordViewingGestures(int width, int height, Activity activity){
		navigationStates = new HashMap<String, Boolean>();
		navigationStates.put("Init", false);
		navigationStates.put("End", false);
		navigationStates.put("Swipe_Init", false);
		navigationStates.put("Swipe_End", false);
		navigationStates.put("PointSelect_Init", false);
		navigationStates.put("PointSelect_End", false);
		currentState = sStateZero;

		mainActivity = activity;
		appContext = activity.getApplicationContext();

		screenHeight = height;
		screenWidth = width;
		screenArea = width*height;
		//	    Log.i("device-info", "Width:"+width+" Height:"+height);
		//        mRgb = new Mat(screenHeight, screenWidth, CvType.CV_8UC4);
		//        mHsv = new Mat(screenHeight, screenWidth, CvType.CV_8UC4);
		//        mProcessed = new Mat(screenHeight, screenWidth, CvType.CV_8UC4);
		mRgb = new Mat();
		mHsv = new Mat();
		mProcessed = new Mat();

		contours =  new ArrayList<MatOfPoint>();
		hierarchy = new Mat();
		kernel = Mat.ones(15, 15, CvType.CV_8U);
		convexHull = new MatOfInt();
		handContour = new MatOfPoint();
		convexityDefects = new MatOfInt4();

		red = new Scalar(255,0,0);
		green = new Scalar(0,255,0);
		blue = new Scalar(0,0,255);
	}

	public Mat processImage(Mat inputImage, List<MatOfPoint> contoursList, int indexHandContour){
		//if 5 seconds passed with no change go back to "zipou"
//		if(((System.currentTimeMillis() - timeLastDetectedGest) >= 8000) && (currentState != sStateZero)){
//			//no gestures detected for 8.0 seconds... go back to zipou
//			int x = (int)Math.round(screenWidth*0.05);
//			int y = (int)Math.round(screenHeight*0.35);
//			writeToImage( x, y, "back to zipou ", Toast.LENGTH_SHORT);
//			navigationStates.put("Init", false);
//			navigationStates.put("Rotate_Init", false);
//			navigationStates.put("Rotate_End", false); 
//			navigationStates.put("Zoom_Init", false);
//			navigationStates.put("Zoom_End", false);
//			currentState = sStateZero;
//			timeLastDetectedGest = System.currentTimeMillis();
//		}

		Log.i("check", "imgIntGest - procressImage init");
		mRgb = inputImage;
		contours = contoursList;
		handContour = contours.get(indexHandContour);
		if(Imgproc.contourArea(handContour) < screenArea * 0.15){
			//no good contours found
			int x = (int)Math.round(screenWidth*0.05);
			int y = (int)Math.round(screenHeight*0.15);
			writeToImage( x, y, "Hand too far!", -1);
		}else if(Imgproc.contourArea(handContour) > screenArea * 0.305){
			//no good contours found
			int x = (int)Math.round(screenWidth*0.05);
			int y = (int)Math.round(screenHeight*0.15);
			writeToImage( x, y, "Hand too close!", -1);
		}else{
			//good contour found
			//        	writeToImage((int)Math.round(screenWidth*0.05), (int)Math.round(screenHeight*0.10), "Hand found!");
			//approximate polygon to hand contour, makes the edges more stable
			MatOfPoint2f temp_contour = new MatOfPoint2f(handContour.toArray());
			double epsilon = Imgproc.arcLength(temp_contour, true)*0.0028;
			MatOfPoint2f result_temp_contour = new MatOfPoint2f();
			Imgproc.approxPolyDP(temp_contour, result_temp_contour, epsilon, true);
			handContour = new MatOfPoint(result_temp_contour.toArray());


			handContourCentroid = getCentroid(handContour);
			//draw circle in centroid of contour
			Core.circle(mRgb, handContourCentroid, 10, red, -1);
			//            Log.i("contours-info", "contours="+contours.size()+" size="+biggestArea);

			/* 
			 * handContour == biggestContour 
			 * but Imgproc.drawContours method takes only List<MapOfPoint> as parameter
			 */
			Imgproc.drawContours(mRgb, contours, indexHandContour, green, 3);
			Imgproc.convexHull(handContour, convexHull);
			Imgproc.convexityDefects(handContour, convexHull, convexityDefects);
			detectGesture(handContour, convexityDefects);	
			//            drawDefects(convexityDefects, handContourCentroid, handContour);

			temp_contour.release();
			result_temp_contour.release();
			contours.clear();
			handContour.release();
		}
		Log.i("check", "imgIntGest - procressImage end");

		return mRgb;
	}

	/*************************** Gesture Methods *************************************/

	private void detectGesture(MatOfPoint contour, MatOfInt4 defects) {
		/*
		 * iterate through states, already detected states are true so a rotation would look like:
		 * init = true, rotationInit = true, rotationEnd = false, end = false, everything else = false
		 * 
		 * 
		 * interactionState("Swipe_Init");
		 * interactionState("Swipe_End");
		 * interactionState("PointSelect_Init");
		 * interactionState("PointSelect_End");
		 */
		if(currentState == sStateZero){
			//Init not detected no interaction has not started
			if(detectInitGesture(convexityDefects, handContour) == true ){
				navigationStates.put("Init", true);
				currentState = "Init";
				//				drawDefects(convexityDefects, handContour);
				Log.i("ImageInteraction", "Gesture detected - INIT");
				int x = (int)Math.round(screenWidth*0.05);
				int y = (int)Math.round(screenHeight*0.15);
				writeToImage( x, y, "Init found!", Toast.LENGTH_LONG);
				timeLastDetectedGest = System.currentTimeMillis();
			}
		}else if(currentState == sStateInit){
			//Interaction has started, nothing detected yet
			if(detectSwipeGesture(convexityDefects, handContour, false) == true ){
				navigationStates.put("Swipe_Init", true);
				navigationStates.put("Init",true); 
				navigationStates.put("PointSelect_Init", false);				
				navigationStates.put("PointSelect_End", false);		
				currentState = sStateSwipe;
				timeLastDetectedGest = System.currentTimeMillis();
				//				drawDefects(convexityDefects, handContour);
				//				Log.i("ImageInteraction", "Gesture detected - Rotate_End");
				int x = (int)Math.round(screenWidth*0.05);
				int y = (int)Math.round(screenHeight*0.15);
				writeToImage( x, y, "Swipe_Init found!", Toast.LENGTH_SHORT);
			}else if(detectPointSelectGesture(convexityDefects, handContour, false) == true){
				navigationStates.put("PointSelect_Init", true);				
				navigationStates.put("Init",true); 
				navigationStates.put("Swipe_Init", false);				
				navigationStates.put("Swipe_End", false);				
				currentState = sStatePointSelect;
				timeLastDetectedGest = System.currentTimeMillis();
				//				drawDefects(convexityDefects, handContour);
				//				Log.i("ImageInteraction", "Gesture detected - Zoom_End");
				int x = (int)Math.round(screenWidth*0.05);
				int y = (int)Math.round(screenHeight*0.15);
				writeToImage( x, y, "PointSelect_Init found!", Toast.LENGTH_SHORT);				
			}

		}else if(currentState == sStateSwipe){
			//Rotation initial gesture has been detected, look for rotation ending.
			//Or keep rotating until something happens
			if(detectSwipeGesture(convexityDefects, handContour, true) == true ){
				navigationStates.put("Swipe_Init", false);
				navigationStates.put("Swipe_End", false);
				navigationStates.put("Init",true); 
				navigationStates.put("PointSelect_Init", false);				
				navigationStates.put("PointSelect_End", false);	
				currentState = sStateInit;
				timeLastDetectedGest = System.currentTimeMillis();
				//				drawDefects(convexityDefects, handContour);
				//				Log.i("ImageInteraction", "Gesture detected - Rotate_End");
				//				int x = (int)Math.round(screenWidth*0.05);
				//				int y = (int)Math.round(screenHeight*0.15);
				//				writeToImage( x, y, "Rotate_End found!");
			}

		}else if(currentState == sStatePointSelect){
			//Zoom in initial gesture has been detected, look for zoom ending. 
			//Or keep zooming until something happens
			if(detectPointSelectGesture(convexityDefects, handContour, true) == true){
				navigationStates.put("PointSelect_Init", false);				
				navigationStates.put("PointSelect_End", false);				
				navigationStates.put("Init",true); 
				navigationStates.put("Swipe_Init", false);				
				navigationStates.put("Swipe_End", false);	
				currentState = sStateInit;
				timeLastDetectedGest = System.currentTimeMillis();
				//				drawDefects(convexityDefects, handContour);
				//				Log.i("ImageInteraction", "Gesture detected - Zoom_End");
				int x = (int)Math.round(screenWidth*0.05);
				int y = (int)Math.round(screenHeight*0.15);
				writeToImage( x, y, "PointSelect_END found!", Toast.LENGTH_SHORT);				
			}
		}

		/*
		 * Always detect end gesture
		 */
		if(detectEndGesture(convexityDefects, handContour) == true ){
			if(navigationStates.get("Init") == true){
				navigationStates.put("Init",true); 
				currentState = sStateInit;				
			}else{
				navigationStates.put("Init",false); 
				currentState = sStateZero;
			}
			navigationStates.put("Rotate_Init", false);
			navigationStates.put("Rotate_End", false); 
			navigationStates.put("Zoom_Init", false);
			navigationStates.put("Zoom_End", false); 
			//			drawDefects(convexityDefects, handContour);
			Log.i("ImageInteraction", "Gesture detected - End");
			int x = (int)Math.round(screenWidth*0.05);
			int y = (int)Math.round(screenHeight*0.15);
			writeToImage( x, y, "END found!", Toast.LENGTH_LONG);
			timeLastDetectedGest = System.currentTimeMillis();		
			//			SystemClock.sleep(500);
		}
	}

	/*
	 * Detection of PointSelect gesture
	 */
	private boolean detectPointSelectGesture(MatOfInt4 convexityDefects, MatOfPoint handContour, boolean initDetected) {
		// TODO Auto-generated method stub
		int positiveDefects = 0;
		int defects[] = convexityDefects.toArray();
		List<Point> contour = handContour.toList();
		List<Point> finalDefects = new ArrayList<Point>();
		Point centroid = getCentroid(handContour);
		MatOfPoint2f m2fHandContour = new MatOfPoint2f(handContour.toArray());

		/*
		 * convexityDefects -> structure containing (by order) start, end, depth_point, depth.
		 * depth-> farthest point (depth_point) distance to convex hull
		 */
		//Point start;
		Point end;
		Point furthest;
		double distancePointHull;
		double distanceCenterPoint_previous = 0;

		if(!initDetected){

			Log.i("RecordViewing", "PointSelect gesture::beginning");
			/*
			 * Look for PointSelect_Init gesture :: 1 finger lifted up
			 * - Removing defects
			 * -- we want to keep only defects above centroid of hand
			 */
			for(int i=0; i< defects.length; i=i+4){
				//						start = contour.get(defects[i]);
				end = contour.get(defects[i+1]);
				furthest = contour.get(defects[i+2]);
				distancePointHull = defects[i+3]/256.0;
				if(i==1){
					distanceCenterPoint_previous = getDistanceBetweenPoints(centroid, end);
				}
				//top left screen x=0,y=0. Otherwise would be end.x > centroid.x
				if(end.y <= centroid.y && furthest.y <= centroid.y){
					double distanceCenterPoint = getDistanceBetweenPoints(centroid, end);
					double relationCenterHull_EndHull = (distanceCenterPoint/distancePointHull);
					double distClosestPerimeter = Imgproc.pointPolygonTest(m2fHandContour, centroid, true);
					double relationCenterHull_distClosestPerimeter = distanceCenterPoint/distClosestPerimeter;
					double relationCenterPointCurrent_CenterPointPrevious = distanceCenterPoint/distanceCenterPoint_previous;

					Log.i("RecordViewing", "centerHull_endHull:: "+relationCenterHull_EndHull
							+ " pointHull:: "+ distancePointHull
							+ " current-previous:: "+ relationCenterPointCurrent_CenterPointPrevious);
					
					if((relationCenterHull_EndHull > 10.0) && (distancePointHull < 3.0) 
							&& (relationCenterHull_distClosestPerimeter >= 1.9)
							&& (relationCenterPointCurrent_CenterPointPrevious > 1)){
						Core.circle(mRgb, end, 10, red, -1);
						Core.circle(mRgb, furthest, 10, red, -1);
						finalDefects.add(end);
						positiveDefects = positiveDefects + 1;
					}
				}
				distanceCenterPoint_previous = getDistanceBetweenPoints(centroid, end);
			}
			if(finalDefects.size() == 1){
				Point defect_one = finalDefects.get(0);
				Core.circle(mRgb, defect_one, 10, red, -1);
				return true;
			}
		}else{
			/*
			 * Look for PointSelect_End gesture
			 * - Removing defects
			 * -- we want to keep only defects above centroid of hand
			 */
			for(int i=0; i< defects.length; i=i+4){
				//						start = contour.get(defects[i]);
				end = contour.get(defects[i+1]);
				furthest = contour.get(defects[i+2]);
				distancePointHull = defects[i+3]/256.0;
				if(i==1){
					distanceCenterPoint_previous = getDistanceBetweenPoints(centroid, end);
				}
				//top left screen x=0,y=0. Otherwise would be end.x > centroid.x
				if(end.y <= centroid.y && furthest.y <= centroid.y){
					double distanceCenterPoint = getDistanceBetweenPoints(centroid, end);
					double relationCenterHull_EndHull = (distanceCenterPoint/distancePointHull);
					double distClosestPerimeter = Imgproc.pointPolygonTest(m2fHandContour, centroid, true);
					double relationCenterHull_distClosestPerimeter = distanceCenterPoint/distClosestPerimeter;
					double relationCenterPointCurrent_CenterPointPrevious = distanceCenterPoint/distanceCenterPoint_previous;

					Log.i("RecordViewing", "centerHull_endHull:: "+relationCenterHull_EndHull
							+ " pointHull:: "+ distancePointHull);
					
					if((relationCenterHull_EndHull > 10.0) && (distancePointHull < 3.0) 
							&& (relationCenterHull_distClosestPerimeter >= 1.9)
							&& (relationCenterPointCurrent_CenterPointPrevious < 1)){
						Core.circle(mRgb, end, 10, red, -1);
						Core.circle(mRgb, furthest, 10, red, -1);
						finalDefects.add(end);
						positiveDefects = positiveDefects + 1;
					}
				}
				distanceCenterPoint_previous = getDistanceBetweenPoints(centroid, end);
			}
			if(finalDefects.size() == 2){
				Point defect_one = finalDefects.get(0);
				Point defect_two = finalDefects.get(1);
				Core.circle(mRgb, defect_one, 10, red, -1);
				Core.circle(mRgb, defect_two, 10, red, -1);
				return true;
			}
		}
		return false;
	}

	private boolean detectSwipeGesture(MatOfInt4 defects, MatOfPoint contour, boolean initDetected) {
		// TODO Auto-generated method stub
		return false;
	}

	/*
	 * Detection of Init gesture
	 */
	private boolean detectInitGesture(MatOfInt4 convexityDefects, MatOfPoint handContour) {
		int positiveDefects = 0;
		int defects[] = convexityDefects.toArray();
		List<Point> contour = handContour.toList();
		Point centroid = getCentroid(handContour);
		MatOfPoint2f m2fHandContour = new MatOfPoint2f(handContour.toArray());
		/*
		 * convexityDefects -> structure containing (by order) start, end, depth_point, depth.
		 * depth-> farthest point (depth_point) distance to convex hull
		 */
		//		Point start;
		Point end;
		Point furthest;
		float distancePointHull;
		/*
		 * Removing defects
		 * we want to keep only defects above centroid of hand
		 */
		for(int i=0; i< defects.length; i=i+4){
			//				start = contour.get(defects[i]);
			end = contour.get(defects[i+1]);
			furthest = contour.get(defects[i+2]);
			distancePointHull = Math.round(defects[i+3]/256.0);
			//top left screen x=0,y=0. Otherwise would be end.x > centroid.x
			if(end.y <= centroid.y && furthest.y <= centroid.y){
				int distanceCenterHull = (int)Math.round(getDistanceBetweenPoints(centroid, end));
				double relationCenterHull_EndHull = (distanceCenterHull/distancePointHull);
				double distClosestPerimeter = Imgproc.pointPolygonTest(m2fHandContour, centroid, true);
				double relationCenterHull_distClosestPerimeter = distanceCenterHull/distClosestPerimeter;
				//					Log.i("ImageInteracion-defects-init","center-Hull: "+ distanceCenterHull
				//							+ " relCenterHull_EndHull: "+relationCenterHull_EndHull
				//							+ " relCenterHull_ClosestPerimeter: "+ relationCenterHull_distClosestPerimeter);
				if((relationCenterHull_EndHull > 10.0) && (relationCenterHull_distClosestPerimeter >= 1.9)){
					//						Core.line(mRgb, furthest, end, blue, 3);
					////				        line from center of contour to convexhull point
					//						Core.line(mRgb, end, centroid, blue, 3);
					//				        points
					Core.circle(mRgb, end, 10, red, -1);
					Core.circle(mRgb, furthest, 10, red, -1);

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
	private boolean detectEndGesture(MatOfInt4 convexityDefects, MatOfPoint handContour) {
		int positiveDefects = 0;
		int defects[] = convexityDefects.toArray();
		List<Point> contour = handContour.toList();
		Point centroid = getCentroid(handContour);
		MatOfPoint2f m2fHandContour = new MatOfPoint2f(handContour.toArray());

		/*
		 * convexityDefects -> structure containing (by order) start, end, depth_point, depth.
		 * depth-> farthest point (depth_point) distance to convex hull
		 */
		//		Point start;
		Point end;
		Point furthest;
		float distancePointHull;
		/*
		 * Removing defects
		 * we want to keep only defects above centroid of hand
		 */
		for(int i=0; i< defects.length; i=i+4){
			//				start = contour.get(defects[i]);
			end = contour.get(defects[i+1]);
			furthest = contour.get(defects[i+2]);
			distancePointHull = Math.round(defects[i+3]/256.0);
			//top left screen x=0,y=0. Otherwise would be end.x > centroid.x
			if(end.y <= centroid.y && furthest.y <= centroid.y){
				int distanceCenterHull = (int)Math.round(getDistanceBetweenPoints(centroid, end));
				double relationCenterHull_EndHull = (distanceCenterHull/distancePointHull);
				double distClosestPerimeter = Imgproc.pointPolygonTest(m2fHandContour, centroid, true);
				double relationCenterHull_distClosestPerimeter = distanceCenterHull/distClosestPerimeter;
				//					Log.i("ImageInteracion","End gesture::center-Hull: "+ distanceCenterHull
				//							+ " relCenterHull_EndHull: "+relationCenterHull_EndHull
				//							+ " relCenterHull_ClosestPerimeter: "+ relationCenterHull_distClosestPerimeter);
				if((relationCenterHull_EndHull > 10.0) && (relationCenterHull_distClosestPerimeter < 1.9)){
					//						Core.line(mRgb, furthest, end, blue, 3);
					////				        line from center of contour to convexhull point
					//						Core.line(mRgb, end, centroid, blue, 3);
					//				        points
					Core.circle(mRgb, end, 10, red, -1);
					Core.circle(mRgb, furthest, 10, red, -1);

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
	 * Draws convexity Defects in color image
	 * params: convexity defects, center of hand contour, hand contour
	 * 
	 */
	@SuppressWarnings("unused")
	private void drawDefects(MatOfInt4 convexityDefects, Point centroid, MatOfPoint handContour) {
		int defects[] = convexityDefects.toArray();
		List<Point> contour = handContour.toList();
		/*
		 * convexityDefects -> structure containing (by order) start, end, depth_point, depth.
		 * depth-> farthest point (depth_point) distance to convex hull
		 */
		Point start;
		Point end;
		Point furthest;
		float depth;

		for(int i=0; i< defects.length; i=i+4){
			start = contour.get(defects[i]);
			end = contour.get(defects[i+1]);
			furthest = contour.get(defects[i+2]);
			depth = Math.round(defects[i+3]/256.0);

			//		        convexhull
			//				Core.line(mRgb, start, end, blue, 3);
			//		        line from farthest point to convexhull
			Core.line(mRgb, furthest, end, blue, 3);
			//		        line from center of contour to convexhull point
			Core.line(mRgb, end, centroid, blue, 3);
			//		        points
			Core.circle(mRgb, end, 10, red, -1);
			Core.circle(mRgb, furthest, 10, red, -1);
			//		        write distance between hull and farthest point
			//		        tools.setText(image, end, str(distance))
			//		        distanceCenterHull = tools.getDistanceBetweenPoints(center, end)
			//		        centerLine = tools.getMidPointInLine(center, end)
			//		        tools.setText(image, centerLine, str(distanceCenterHull))	
		}
	}


	/*************************** Utility methods ************************************************/
	/*
	 * Utility method - writes to color image
	 */
	private void writeToImage(int x, int y, final String string, final int length) {
		Core.putText(mRgb, string, new Point(x, y),Core.FONT_HERSHEY_SIMPLEX, 1.5, new Scalar(0,0,0), 20);
		Core.putText(mRgb, string, new Point(x, y),Core.FONT_HERSHEY_SIMPLEX, 1.5, new Scalar(255,255,255), 10);
		if(length != -1){
			mainActivity.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					//					testToast.cancel();
					if(tToastMsg != null){
						tToastMsg.cancel();
					}
					tToastMsg = Toast.makeText(appContext, string, length);
					tToastMsg.show();
					//		            Toast.makeText(appContext, string, length).show();
				}
			});
		}
	}

	/*
	 * Calculates the center of the contour
	 * Params: hand contour
	 * returns Point centroid -> centroid{x,y}
	 */
	private Point getCentroid(MatOfPoint contour) {
		Moments moments = Imgproc.moments(contour);
		Point centroid = new Point();
		centroid.x = moments.get_m10() / moments.get_m00();
		centroid.y = moments.get_m01() / moments.get_m00();
		return centroid;
	}

	public String getState(){
		return currentState;
	}

	/*
	 * Utility method - gets distance between two points
	 */
	private double getDistanceBetweenPoints(Point one, Point two) {
		return Math.sqrt( Math.pow((two.x-one.x), 2) + Math.pow((two.y-one.y), 2));
	}


}
