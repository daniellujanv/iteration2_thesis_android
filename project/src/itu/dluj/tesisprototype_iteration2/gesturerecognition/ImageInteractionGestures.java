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

import android.util.Log;

public class ImageInteractionGestures {

	private Scalar red;
	private Scalar green;
	private Scalar blue;
	private HashMap<String, Boolean> interactionStates;
	private String currentState;
	private Mat mRgb;
	
	private List<MatOfPoint> contours;
	private MatOfPoint handContour;
	private MatOfInt convexHull;
	private Point handContourCentroid;
	private MatOfInt4 convexityDefects;
	
	private int screenArea;
	private int screenHeight;
	private int screenWidth;
	
	public ImageInteractionGestures(int width, int height){
		interactionStates = new HashMap<String, Boolean>();
		interactionStates.put("Init", false);
		interactionStates.put("End", false);
		interactionStates.put("Rotate_Init", false);
		interactionStates.put("Rotate_End", false);
		interactionStates.put("Zoom_Init", false);
		interactionStates.put("Zoom_End", false);
		currentState = "zipou";
		
	    screenHeight = height;
	    screenWidth = width;
	    screenArea = width*height;
//	    Log.i("device-info", "Width:"+width+" Height:"+height);
//        mRgb = new Mat(screenHeight, screenWidth, CvType.CV_8UC4);
//        mHsv = new Mat(screenHeight, screenWidth, CvType.CV_8UC4);
//        mProcessed = new Mat(screenHeight, screenWidth, CvType.CV_8UC4);
        mRgb = new Mat();

        contours =  new ArrayList<MatOfPoint>();
        convexHull = new MatOfInt();
        handContour = new MatOfPoint();
        convexityDefects = new MatOfInt4();
        
        red = new Scalar(255,0,0);
        green = new Scalar(0,255,0);
        blue = new Scalar(0,0,255);
	}
	
	public Mat processImage(Mat inputImage, List<MatOfPoint> contoursList, int indexHandContour){
    	Log.i("check", "imgIntGest - procressImage init");
		mRgb = inputImage;
		contours = contoursList;
		handContour = contours.get(indexHandContour);

		if((Imgproc.contourArea(handContour) > screenArea * 0.15) && (Imgproc.contourArea(handContour) < screenArea * 0.305)){
	    	//good contour found
            //approximate polygon to hand contour, makes the edges more stable
            MatOfPoint2f temp_contour = new MatOfPoint2f(handContour.toArray());
            double epsilon = Imgproc.arcLength(temp_contour, true)*0.0025;
            MatOfPoint2f result_temp_contour = new MatOfPoint2f();
            Imgproc.approxPolyDP(temp_contour, result_temp_contour, epsilon, true);
            handContour = new MatOfPoint(result_temp_contour.toArray());

            handContourCentroid = getCentroid(handContour);
            //draw circle in centroid of contour
            Core.circle(mRgb, handContourCentroid, 10, red, -1);
    		//We lower the centroid to try to get the center of the palm not the whole hand.
            handContourCentroid.y = handContourCentroid.y + handContourCentroid.y*0.10;
            Core.circle(mRgb, handContourCentroid, 10, green, -1);
            //            Log.i("contours-info", "contours="+contours.size()+" size="+biggestArea);
            /* 
             * handContour == biggestContour 
             * but Imgproc.drawContours method takes only List<MapOfPoint> as parameter
             */
            Imgproc.drawContours(mRgb, contours, indexHandContour, green, 3);
            Imgproc.convexHull(handContour, convexHull);
            Imgproc.convexityDefects(handContour, convexHull, convexityDefects);
            detectGesture(handContour, convexityDefects);
//            drawDefects(convexityDefects, handContour);
            
            temp_contour.release();
            result_temp_contour.release();
            contours.clear();
            handContour.release();
        }else{
        	//no good contours found
        	writeToImage( (int)Math.round(screenWidth*0.05), (int)Math.round(screenHeight*0.15), "Hand too far!");
        }
    	Log.i("check", "imgIntGest - procressImage end");

        return mRgb;
	}
	

	public String getState(){
		return currentState;
	}
	
	/*******************internal methods**************************/

	private void detectGesture(MatOfPoint handContour, MatOfInt4 convexityDefects) {
		/*
		 * iterate through states, already detected states are true so a rotation would look like:
		 * init = true, rotationInit = true, rotationEnd = false, end = false, everything else = false
		 * 
		 * 
		 * interactionState.put("Rotate_Init");
		 * interactionState.put("Rotate_End");
		 * interactionState.put("Zoom_Init");
		 * interactionState.put("Zoom_End");
		 */
		if(currentState == "zipou"){
			//Init not detected no interaction has not started
			if(detectInitGesture(convexityDefects, handContour) == true ){
//				interactionStates.put("Init", true);
//				currentState = "Init";
				drawDefects(convexityDefects, handContour);
				Log.i("ImageInteraction", "Gesture detected - INIT");
				int x = (int)Math.round(screenWidth*0.05);
				int y = (int)Math.round(screenHeight*0.15);
				writeToImage( x, y, "INIT found!");

			}
		}else if(currentState == "Init"){
			//Interaction has started, nothing detected yet
			
			
		}else if(currentState == "Rotate"){
			//Rotation initial gesture has been detected, look for rotation ending.
			//Or keep rotating until something happens

		}else if(currentState == "Zoom"){
			//Zoom in initial gesture has been detected, look for zoom ending. 
			//Or keep zooming until something happens
			
		}
		
		/*
		 * Always detect end gesture
		 * 
		 */
		if(detectEndGesture(convexityDefects, handContour) == true ){
//			interactionStates.put("Init", true);
//			currentState = "Init";
			drawDefects(convexityDefects, handContour);
			Log.i("ImageInteraction", "Gesture detected - End");
			int x = (int)Math.round(screenWidth*0.05);
			int y = (int)Math.round(screenHeight*0.15);
			writeToImage( x, y, "END found!");

		}
		
		/*
		 * if(gestures['Init'] == False):
        defects = removeDefects(defects, contour, contourProps, 'Init')
        positiveDefects = 0
        positiveDefects = detectInitGesture(defects, contour, contourProps)
        if(positiveDefects >= 4):
            gestures['Init'] = True
            print "Gesture Detected: INIT"
            drawDefects(imgColor, defects, contour, contourProps)
            return gestures #return at this point so no other gesture can start
            
    else:# gesture INIT already detected, find other gestures
        #once INIT = True, we can always stop/end interaction
        gestures['End'] = False
        defects = removeDefects(defects, contour, contourProps, 'End')
        positiveDefects = 0
        positiveDefects = detectEndGesture(defects, contour, contourProps)
        #if End is detected then we start again
        if(positiveDefects >= 4):
            print "Gesture Detected: END"
            gestures['Init'] = False
            gestures['Rotate_Init'] = False
            gestures['End'] = False
            drawDefects(imgColor, defects, contour, contourProps)    
            return gestures #return at this point so no other gesture can start
        
        if((gestures['Rotate_Init'] == False) and (gestures['Resize_Init'] == False)):
            positiveDefects = 0
            if(positiveDefects >= 4):
                print "Gesture Detected: Rotate_INIT"
                gestures['Rotate_Init'] = True
                drawDefects(imgColor, defects, contour, contourProps)    
                return gestures #return at this point so no other gesture can start
        elif((gestures['Rotate_Init'] == True) and (gestures['Rotate_End'] == False)):
            positiveDefects = 0
            if(positiveDefects >= 4):#second rotation gesture detected
                print "Gesture Detected: Rotate_END"
                gestures['Rotate_Init'] = False
                gestures['Rotate_End'] = False
                drawDefects(imgColor, defects, contour, contourProps)    
                return gestures #return at this point so no other gesture can start
            
        if((gestures['Resize_Init'] == False) and (gestures['Rotate_Init'] == False)):#initial rotate gesture detected, now look for second rotation gesture 
            positiveDefects = 0
            if(positiveDefects >= 4):
                print "Gesture Detected: Resize_INIT"
                gestures['Resize_Init'] = True
                drawDefects(imgColor, defects, contour, contourProps)    
                return gestures #return at this point so no other gesture can start
        elif((gestures['Resize_Init'] == True) and (gestures['Resize_End'] == False)):
            positiveDefects = 0
            if(positiveDefects >= 4):#second rotation gesture detected
                print "Gesture Detected: Resize_END"
                gestures['Resize_Init'] = False
                gestures['Resize_End'] = False
                drawDefects(imgColor, defects, contour, contourProps)    
                return gestures #return at this point so no other gesture can start
        
    drawDefects(imgColor, defects, contour, contourProps)    
    return gestures
		 */
	}


	private boolean detectInitGesture(MatOfInt4 convexityDefects, MatOfPoint handContour) {
		int positiveDefects = 0;
		int defects[] = convexityDefects.toArray();
		List<Point> contour = handContour.toList();
		Point centroid = getCentroid(handContour);
		//We lower the centroid to try to get the center of the palm not the whole hand.
//		centroid.y = centroid.y + centroid.y*0.10;
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
					Log.i("ImageInteracion-defects-init","center-Hull: "+ distanceCenterHull
							+ " relCenterHull_EndHull: "+relationCenterHull_EndHull
							+ " relCenterHull_ClosestPerimeter: "+ relationCenterHull_distClosestPerimeter);

					if((relationCenterHull_EndHull > 10.0) && (relationCenterHull_distClosestPerimeter >= 1.9)){
						Core.line(mRgb, furthest, end, blue, 3);
//				        line from center of contour to convexhull point
						Core.line(mRgb, end, centroid, blue, 3);
//				        points
						Core.circle(mRgb, end, 10, red, -1);
						Core.circle(mRgb, furthest, 10, red, -1);
						
						positiveDefects = positiveDefects + 1;
					}
				}
		}
		Log.i("ImageInteraction", "defect number -> "+ positiveDefects);
		if(positiveDefects >= 4){
			return true;
		}else{
			return false;
		}
	}

	private boolean detectEndGesture(MatOfInt4 convexityDefects, MatOfPoint handContour) {
		int positiveDefects = 0;
		int defects[] = convexityDefects.toArray();
		List<Point> contour = handContour.toList();
		Point centroid = getCentroid(handContour);
		//We lower the centroid to try to get the center of the palm not the whole hand.
//		centroid.y = centroid.y + centroid.y*0.10;
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
					Log.i("ImageInteracion-defects-end","center-Hull: "+ distanceCenterHull
							+ " relCenterHull_EndHull: "+relationCenterHull_EndHull
							+ " relCenterHull_ClosestPerimeter: "+ relationCenterHull_distClosestPerimeter);
					if((relationCenterHull_EndHull > 10.0) && (relationCenterHull_distClosestPerimeter < 1.9)){
						Core.line(mRgb, furthest, end, blue, 3);
//				        line from center of contour to convexhull point
						Core.line(mRgb, end, centroid, blue, 3);
//				        points
						Core.circle(mRgb, end, 10, red, -1);
						Core.circle(mRgb, furthest, 10, red, -1);
						
						positiveDefects = positiveDefects + 1;
					}
				}
		}
		Log.i("ImageInteraction", "defect number -> "+ positiveDefects);
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
	
	private void writeToImage(int x, int y, String string) {
		Core.putText(mRgb, string, new Point(x, y),Core.FONT_HERSHEY_SIMPLEX, 1.5, new Scalar(0,0,0), 20);
		Core.putText(mRgb, string, new Point(x, y),Core.FONT_HERSHEY_SIMPLEX, 1.5, new Scalar(255,255,255), 10);
	}

	private double getDistanceBetweenPoints(Point one, Point two) {
		return Math.sqrt( Math.pow((two.x-one.x), 2) + Math.pow((two.y-one.y), 2));
	}

	
}
