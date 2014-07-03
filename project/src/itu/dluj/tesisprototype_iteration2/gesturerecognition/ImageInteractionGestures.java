package itu.dluj.tesisprototype_iteration2.gesturerecognition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.opencv.core.Core;
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

public class ImageInteractionGestures {

	private Scalar red = new Scalar(255,0,0);
	private Scalar green = new Scalar(0,255,0);
	private Scalar blue = new Scalar(0,0,255);
	private Scalar magenta = new Scalar(255,0,255);

	private HashMap<String, Boolean> interactionStates;
	private String currentState;

	private Mat mRgb;
	private Context appContext;
	private Activity mainActivity;

	private long timeLastDetectedGest;
	private String sStateZero = "Zipou";
	private String sStateInit = "Init";
	private String sStateZoom = "Zoom";
	private String sStateRotate = "Rotate";
	@SuppressWarnings("unused")
	private String sStateEnd ="End";

	private MatOfPoint mHandContour;
	private List<MatOfPoint> lHandContour;
	private MatOfInt convexHull;
	private Point handContourCentroid;
	private Point rotateInitPos;
	private double zoomInitDistance;
	private MatOfInt4 convexityDefects;

	private int screenArea;
	private int screenHeight;
	private int screenWidth;
	private Toast tToastMsg;

	public ImageInteractionGestures(int width, int height, Activity activity){
		interactionStates = new HashMap<String, Boolean>();
		interactionStates.put("Init", false);
		interactionStates.put("End", false);
		interactionStates.put("Rotate_Init", false);
		interactionStates.put("Rotate_End", false);
		interactionStates.put("Zoom_Init", false);
		interactionStates.put("Zoom_End", false);
		currentState = sStateZero;
		mainActivity = activity;
		appContext = mainActivity.getApplicationContext();
		//		currentState = "Rotate";
		//		currentState = "Init";

		screenHeight = height;
		screenWidth = width;
		screenArea = width*height;
		//	    Log.i("device-info", "Width:"+width+" Height:"+height);
		//        mRgb = new Mat(screenHeight, screenWidth, CvType.CV_8UC4);
		//        mHsv = new Mat(screenHeight, screenWidth, CvType.CV_8UC4);
		//        mProcessed = new Mat(screenHeight, screenWidth, CvType.CV_8UC4);
		mRgb = new Mat();

		convexHull = new MatOfInt();
		mHandContour = new MatOfPoint();
		convexityDefects = new MatOfInt4();
		lHandContour = new ArrayList<MatOfPoint>();

		timeLastDetectedGest = System.currentTimeMillis();

	}

	/*
	 * Process image - applying operations to the image
	 * common to every gesture before detecting them
	 */
	public Mat processImage(Mat inputImage, MatOfPoint contour){
		//if 5 seconds passed with no change go back to "zipou"
		if(((System.currentTimeMillis() - timeLastDetectedGest) >= 10000) && (currentState != sStateZero)){
			//no gestures detected for 8.0 seconds... go back to zipou
			int x = (int)Math.round(screenWidth*0.05);
			int y = (int)Math.round(screenHeight*0.35);
			writeToImage( x, y, "back to zipou ", Toast.LENGTH_SHORT);
			interactionStates.put("Init", false);
			interactionStates.put("Rotate_Init", false);
			interactionStates.put("Rotate_End", false); 
			interactionStates.put("Zoom_Init", false);
			interactionStates.put("Zoom_End", false);
			currentState = sStateZero;
			timeLastDetectedGest = System.currentTimeMillis();
		}


		Log.i("check", "imgIntGest - procressImage init");
		mRgb = inputImage;
		mHandContour = contour;
		if(Imgproc.contourArea(mHandContour) < screenArea * 0.15){
			//no good contours found
			int x = (int)Math.round(screenWidth*0.05);
			int y = (int)Math.round(screenHeight*0.15);
			writeToImage( x, y, "Hand too far!", -1);
		}else if(Imgproc.contourArea(mHandContour) > screenArea * 0.35){
			//no good contours found
			int x = (int)Math.round(screenWidth*0.05);
			int y = (int)Math.round(screenHeight*0.15);
			writeToImage( x, y, "Hand too close!", -1);
		}else{
			//good contour found
			//approximate polygon to hand contour, makes the edges more stable
			MatOfPoint2f temp_contour = new MatOfPoint2f(mHandContour.toArray());
			double epsilon = Imgproc.arcLength(temp_contour, true)*0.0030;
			MatOfPoint2f result_temp_contour = new MatOfPoint2f();
			Imgproc.approxPolyDP(temp_contour, result_temp_contour, epsilon, true);
			mHandContour = new MatOfPoint(result_temp_contour.toArray());
			lHandContour.add(mHandContour);
			handContourCentroid = getCentroid(mHandContour);
			//draw circle in centroid of contour
			Core.circle(mRgb, handContourCentroid, 10, red, -1);
			//            Log.i("contours-info", "contours="+contours.size()+" size="+biggestArea);
			/* 
			 * handContour == biggestContour 
			 * but Imgproc.drawContours method takes only List<MapOfPoint> as parameter
			 */
			Imgproc.drawContours(mRgb, lHandContour, -1, green, 3);
			Imgproc.convexHull(mHandContour, convexHull, true);
			Imgproc.convexityDefects(mHandContour, convexHull, convexityDefects);
//			drawDefects(convexityDefects, mHandContour);
			detectGesture(mHandContour, convexityDefects);

			temp_contour.release();
			result_temp_contour.release();
			lHandContour.clear();
			mHandContour.release();
		}

		Log.i("check", "imgIntGest - procressImage end");

		return mRgb;
	}


	public String getState(){
		return currentState;
	}

	/*******************gestures methods**************************/

	private void detectGesture(MatOfPoint handContour, MatOfInt4 convexityDefects) {
		/*
		 * iterate through states, already detected states are true so a rotation would look like:
		 * init = true, rotationInit = true, rotationEnd = false, end = false, everything else = false
		 * 
		 * 
		 * interactionState("Rotate_Init");
		 * interactionState("Rotate_End");
		 * interactionState("Zoom_Init");
		 * interactionState("Zoom_End");
		 */
		if(currentState == sStateZero){
			//Init not detected no interaction has not started
			if(detectInitGesture(convexityDefects, handContour) == true ){
				interactionStates.put("Init", true);
				currentState = "Init";
				//				drawDefects(convexityDefects, handContour);
				Log.i("ImageInteraction", "Gesture detected - INIT");
				writeToImage( 0, 0, "INIT found!", Toast.LENGTH_LONG);
				timeLastDetectedGest = System.currentTimeMillis();
				return;
			}
		}else if(currentState == sStateInit){
			//Interaction has started, nothing detected yet
			if(detectRotateGesture(convexityDefects, handContour, false) == true ){
				interactionStates.put("Rotate_Init", true);
				//making sure the others are false
				interactionStates.put("Zoom_Init", false);
				interactionStates.put("Zoom_Init", false);
				currentState = sStateRotate;
				//				drawDefects(convexityDefects, handContour);
				Log.i("ImageInteraction", "Gesture detected - Rotate_Init");
				writeToImage( 0, 0, "Rotate!", Toast.LENGTH_SHORT);
				timeLastDetectedGest = System.currentTimeMillis();
				return;
			}else if(detectZoomGesture(convexityDefects, handContour, false) == true){
				interactionStates.put("Zoom_Init", true);
				//making sure the others are false
				interactionStates.put("Rotate_Init", false);
				interactionStates.put("Rotate_Init", false);
				currentState = sStateZoom;
				//				drawDefects(convexityDefects, handContour);
				Log.i("ImageInteraction", "Gesture detected - Zoom_Init");
				writeToImage( 0, 0, "Zoom!", Toast.LENGTH_SHORT);		
				timeLastDetectedGest = System.currentTimeMillis();	
				return;
			}

		}else if(currentState == sStateRotate){
			//Rotation initial gesture has been detected, look for rotation ending.
			//Or keep rotating until something happens
			if(detectRotateGesture(convexityDefects, handContour, true) == true ){
				interactionStates.put("Rotate_Init", false);
				interactionStates.put("Init",true); 
				interactionStates.put("Zoom_Init", false);
				interactionStates.put("Zoom_End", false);
				currentState = sStateInit;
				timeLastDetectedGest = System.currentTimeMillis();
				return;
			}

		}else if(currentState == sStateZoom){
			//Zoom in initial gesture has been detected, look for zoom ending. 
			//Or keep zooming until something happens
			if(detectZoomGesture(convexityDefects, handContour, true) == true){
				interactionStates.put("Zoom_Init", false);				
				interactionStates.put("Init",true); 
				interactionStates.put("Rotate_Init", false);
				interactionStates.put("Rotate_End", false);
				currentState = sStateInit;
				timeLastDetectedGest = System.currentTimeMillis();
				return;
			}
		}

		/*
		 * Always detect end gesture
		 */
		if(detectEndGesture(convexityDefects, handContour) == true ){
			if(interactionStates.get("Init") == true){
				interactionStates.put("Init",true); 
				currentState = sStateInit;				
			}else{
				interactionStates.put("Init",false); 
				currentState = sStateZero;
			}
			interactionStates.put("Rotate_Init", false);
			interactionStates.put("Rotate_End", false); 
			interactionStates.put("Zoom_Init", false);
			interactionStates.put("Zoom_End", false); 
			//			drawDefects(convexityDefects, handContour);
			Log.i("ImageInteraction", "Gesture detected - End");
			writeToImage( 0, 0, "END!", Toast.LENGTH_LONG);
			timeLastDetectedGest = System.currentTimeMillis();		
			//			SystemClock.sleep(500);
		}
	}

	/*
	 * Detection of Rotation gesture
	 */
	private boolean detectRotateGesture(MatOfInt4 convexityDefects,	MatOfPoint handContour, boolean initDetected) {
		int positiveDefects = 0;
		int defects[] = convexityDefects.toArray();
		List<Point> contour = handContour.toList();
		List<Point> finalDefects = new ArrayList<Point>();
		Point centroid = getCentroid(handContour);
		/*
		 * convexityDefects -> structure containing (by order) start, end, depth_point, depth.
		 * depth-> farthest point (depth_point) distance to convex hull
		 */
		//		Point start;
		Point end;
		Point furthest;
		float distanceFurthest;
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
			distanceFurthest = Math.round(defects[i+3]/256.0);
			distanceCenterHull = getDistanceBetweenPoints(centroid, end);
			double distanceFurthestPoint = getDistanceBetweenPoints(end, furthest);
			double relationCenterPoint_FurthestPoint = distanceCenterHull/distanceFurthestPoint; 
//			Log.i("ImageInteraction","Defects Rotate :: "
//					+ " relCenterPoint_FurthestPoint" + relationCenterPoint_FurthestPoint
//					+ " centerHull" + distanceCenterHull
//					+ " furthest" + distanceFurthest					
//					);
			//top left screen x=0,y=0. Otherwise would be end.x > centroid.x
			if( (end.y <= centroid.y) && (relationCenterPoint_FurthestPoint < 2.0)){
				finalDefects.add(end);
				positiveDefects = positiveDefects + 1;
			}
		}
//		Log.i("ImageInteraction","Defects Rotate :: finalDefects: "+ finalDefects.size()
//				);			

		if(finalDefects.size() == 2){
			Point defect_one = finalDefects.get(0);
			Point defect_two = finalDefects.get(1);
			int distanceCenterHull_one = (int)Math.round(getDistanceBetweenPoints(centroid, defect_one));
			int distanceCenterHull_two = (int)Math.round(getDistanceBetweenPoints(centroid, defect_two));
			int distanceOneTwo = (int)Math.round(getDistanceBetweenPoints(defect_one, defect_two));
			//				Core.line(mRgb, defect_one, centroid, blue, 3);
			//				//			        line from center of contour to convexhull point
			//				Core.line(mRgb, defect_two, centroid, blue, 3);
			//				Core.line(mRgb, defect_two, defect_one, blue, 3);
			//			        points
			Core.circle(mRgb, defect_one, 10, red, -1);
			Core.circle(mRgb, defect_two, 10, red, -1);
			//distance between fingers has to be greather than 1.3 times the distance from the
			//center to both points
			if((distanceOneTwo > distanceCenterHull_one*1.3 ) && (distanceOneTwo > distanceCenterHull_two*1.3)){
				if(!initDetected){
					rotateInitPos = getPointBetweenPoints(defect_one, defect_two);
					Core.circle(mRgb, rotateInitPos, 10, magenta, -1);
					return true;
				}else{
					Point rotateEndPos = getPointBetweenPoints(defect_one, defect_two);
//					Log.i("null-check", "init::"+ rotateInitPos.toString());
//					Log.i("null-check", " end::"+rotateEndPos.toString());
					double traveledDistance = getDistanceBetweenPoints(rotateInitPos, rotateEndPos);
					if(traveledDistance > screenWidth*0.10){//more than 10% of the screen
						if((rotateInitPos.x >= rotateEndPos.x) && (rotateInitPos.y >= rotateEndPos.y)){
							//rotate left
							Log.i("ImageInteraction", "Rotate gesture::LEFT ");
							writeToImage( 0, 0, "Rotate LEFT!", Toast.LENGTH_SHORT);
							return true;
						}else if((rotateInitPos.x <= rotateEndPos.x) && (rotateInitPos.y <= rotateEndPos.y)){
							//rotate right
							Log.i("ImageInteraction", "Rotate gesture::RIGHT");
							writeToImage( 0, 0, "Rotate RIGHT!", Toast.LENGTH_SHORT);
							return true;
						}
						rotateInitPos = new Point();
					}
				}
			}
			//			Log.i("ImageInteraction", "Rotate gesture::defect number -> "+ positiveDefects);
		}
		return false;
	}

	/*
	 * Detection of Zoom gesture
	 */
	private boolean detectZoomGesture(MatOfInt4 convexityDefects,	MatOfPoint handContour, boolean initDetected) {
		int positiveDefects = 0;
		int defects[] = convexityDefects.toArray();
		List<Point> contour = handContour.toList();
		List<Point> finalDefects = new ArrayList<Point>();
		Point centroid = getCentroid(handContour);
		/*
		 * convexityDefects -> structure containing (by order) start, end, depth_point, depth.
		 * depth-> farthest point (depth_point) distance to convex hull
		 */
		//		Point start;
		Point end;
		Point furthest;
		double distanceFurthest;
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
			distanceFurthest = Math.round(defects[i+3]/256.0);
			distanceCenterHull = getDistanceBetweenPoints(centroid, end);
			//top left screen x=0,y=0. Otherwise would be end.x > centroid.x
			double distanceFurthestPoint = getDistanceBetweenPoints(end, furthest);
			double relationCenterPoint_FurthestPoint = distanceCenterHull/distanceFurthestPoint; 
//			Log.i("ImageInteraction","Defects Zoom:: " 
//					+ "relCenterHull_FurthestHull: "+relationCenterPoint_FurthestPoint
//					+ " centerHull" + distanceCenterHull
//					+ " furthest" + distanceFurthest
//					);			//top left screen x=0,y=0. Otherwise would be end.x > centroid.x
			if( (end.y <= centroid.y) && (relationCenterPoint_FurthestPoint < 2.0)){
				finalDefects.add(end);
				positiveDefects = positiveDefects + 1;
			}
		}
//		Log.i("ImageInteraction","Defects Zoom :: finalDefects: "+ finalDefects.size()
//				);			
		if(finalDefects.size() == 2){
			Point defect_one = finalDefects.get(0);
			Point defect_two = finalDefects.get(1);
			double distanceCenterHull_one = getDistanceBetweenPoints(centroid, defect_one);
			double distanceCenterHull_two = getDistanceBetweenPoints(centroid, defect_two);
			double distanceOneTwo = getDistanceBetweenPoints(defect_one, defect_two);
			//			        points
			Core.circle(mRgb, defect_one, 10, red, -1);
			Core.circle(mRgb, defect_two, 10, red, -1);
			//-distance between the fingers has to be less than 1.3 times the distance 
			//from the center to the end of both fingers
			if(!initDetected){
				if((distanceOneTwo < distanceCenterHull_one*1.3 ) && (distanceOneTwo < distanceCenterHull_two*1.3)){
					zoomInitDistance = getDistanceBetweenPoints(defect_one, defect_two);
					return true;
				}
			}else{
				double zoomEndDistance = getDistanceBetweenPoints(defect_one, defect_two);
//				Log.i("ImageInteraction", "Zoom End distances::"
//						+ " distance: " + (Math.abs(zoomEndDistance - zoomInitDistance))
//						+ " width: " + screenWidth*0.10
//						);
				
				if(Math.abs(zoomEndDistance - zoomInitDistance) > screenWidth*0.10 ){ //more than 10% of screen
					if(zoomEndDistance > zoomInitDistance){
						//Zoom-IN gesture
						zoomInitDistance = zoomEndDistance;
						Log.i("ImageInteraction", "Zoom gesture::IN");
						writeToImage( 0, 0, "Zoom IN", Toast.LENGTH_SHORT);
						return true;
					}else if(zoomEndDistance < zoomInitDistance){
						//Zoom-IN gesture
						zoomInitDistance = zoomEndDistance;
						Log.i("ImageInteraction", "Zoom gesture::OUT");
						writeToImage( 0, 0, "Zoom OUT", Toast.LENGTH_SHORT);
						return true;
					}
				}
			}
		}
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
		//distance between the furthest point and convex hull
		float distanceFurthest;
		/*
		 * Removing defects
		 * we want to keep only defects above centroid of hand
		 */
		for(int i=0; i< defects.length; i=i+4){
			//				start = contour.get(defects[i]);
			end = contour.get(defects[i+1]);
			furthest = contour.get(defects[i+2]);
			distanceFurthest = Math.round(defects[i+3]/256.0);
			//top left screen x=0,y=0. Otherwise would be end.x > centroid.x
			if(end.y <= centroid.y){
				double distanceCenterHull = getDistanceBetweenPoints(centroid, end);
				double distanceFurthestPoint = getDistanceBetweenPoints(end, furthest);
				double relationCenterPoint_FurthestPoint = (distanceCenterHull/distanceFurthestPoint);
				Log.i("ImageInteraction","Defects Init::"
						+ " relCenter_Furthest: "+relationCenterPoint_FurthestPoint
						);
				if((relationCenterPoint_FurthestPoint < 2.0)){
					//				        points
					Core.circle(mRgb, end, 10, red, -1);
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
		float distanceFurthest;
		/*
		 * Removing defects
		 * we want to keep only defects above centroid of hand
		 */
		for(int i=0; i< defects.length; i=i+4){
			//				start = contour.get(defects[i]);
			end = contour.get(defects[i+1]);
			furthest = contour.get(defects[i+2]);
			distanceFurthest = Math.round(defects[i+3]/256.0);
			//top left screen x=0,y=0. Otherwise would be end.x > centroid.x
			if(end.y <= centroid.y && furthest.y <= centroid.y){
				double distanceCenterPoint = getDistanceBetweenPoints(centroid, end);
				double distanceFurthestPoint = getDistanceBetweenPoints(end, furthest);
				double relationCenterPoint_FurthestPoint = (distanceCenterPoint/distanceFurthestPoint);
				Log.i("ImageInteraction","End Defects::"
						+ " relCenter_furthest: "+relationCenterPoint_FurthestPoint
						+ " center: "+ distanceCenterPoint
						+ " furthest: "+ distanceFurthestPoint
						);
				if((relationCenterPoint_FurthestPoint >= 4.0)){
					//				        points
					Core.circle(mRgb, end, 10, red, -1);
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


	/*************************** Utility methods ************************************/

	/*
	 * Draws convexity Defects in color image
	 * params: convexity defects, center of hand contour, hand contour
	 * 
	 */
	@SuppressWarnings("unused")
	private void drawDefects(MatOfInt4 convexityDefects, MatOfPoint handContour) {
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
			Core.line(mRgb, furthest, end, red, 3);
			//		        line from center of contour to convexhull point
			Core.line(mRgb, end, centroid, blue, 3);
			//		        points
			Core.circle(mRgb, end, 10, red, -1);
//			Core.circle(mRgb, furthest, 10, magenta, -1);
			//		        write distance between hull and farthest point
			//		        tools.setText(image, end, str(distance))
			//		        distanceCenterHull = tools.getDistanceBetweenPoints(center, end)
			//		        centerLine = tools.getMidPointInLine(center, end)
			//		        tools.setText(image, centerLine, str(distanceCenterHull))	
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

	/*
	 * Utility method - writes to color image
	 */
	private void writeToImage(int x, int y, final String string, final int length) {
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
		}else{
			Core.putText(mRgb, string, new Point(x, y),Core.FONT_HERSHEY_SIMPLEX, 1.5, new Scalar(0,0,0), 20);
			Core.putText(mRgb, string, new Point(x, y),Core.FONT_HERSHEY_SIMPLEX, 1.5, new Scalar(255,255,255), 10);
		}
	}

	/*
	 * Utility method - gets distance between two points
	 */
	private double getDistanceBetweenPoints(Point one, Point two) {
		return Math.sqrt( Math.pow((two.x-one.x), 2) + Math.pow((two.y-one.y), 2));
	}

	/*
	 * Utility method - gets distance between two points
	 */
	private Point getPointBetweenPoints(Point one, Point two) {
		Point result = new Point();
		result.x = (one.x + two.x)/2;
		result.y = (one.y + two.y)/2;
		return result;
	}

}
