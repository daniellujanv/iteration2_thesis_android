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

import android.util.Log;

public class PatientSelectionGestures {

	public Scalar red;
	public Scalar green;
	public Scalar blue;
	public HashMap<String, Boolean> pointSelectState;
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
	
	private Scalar minRange;
	private Scalar maxRange;
	private int screenArea;
	private int screenHeight;
	private int screenWidth;
	
	public PatientSelectionGestures(int width, int height){
		pointSelectState = new HashMap<String, Boolean>();
		pointSelectState.put("Init", false);
		pointSelectState.put("End", false);
		pointSelectState.put("Point_Init", false);
		pointSelectState.put("Point_End", false);
		pointSelectState.put("Select_Init", false);
		pointSelectState.put("Select_End", false);
		
        minRange = new Scalar(0, 0, 0);
	    maxRange = new Scalar(134, 48, 255);
	    screenHeight = height;
	    screenWidth = width;
	    screenArea = width*height;
	    Log.i("device-info", "Width:"+width+" Height:"+height);
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
    	Log.i("check", "PatSelGes - procressImage init");
		mRgb = inputImage;
		contours = contoursList;
		handContour = contours.get(indexHandContour);

		if(Imgproc.contourArea(handContour) > screenArea * 0.35){
	    	//good contour found
        	writeToImage((int)Math.round(screenWidth*0.05), (int)Math.round(screenHeight*0.10), "Hand found!");
            //approximate polygon to hand contour, makes the edges more stable
            MatOfPoint2f temp_contour = new MatOfPoint2f(handContour.toArray());
            double epsilon = Imgproc.arcLength(temp_contour, true)*0.0025;
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
            
            drawDefects(convexityDefects, handContourCentroid, handContour);
            
            temp_contour.release();
            result_temp_contour.release();
            contours.clear();
            handContour.release();
        }else{
        	//no good contours found
        	writeToImage( (int)Math.round(screenWidth*0.05), (int)Math.round(screenHeight*0.10), "Hand too far!");
        }
    	Log.i("check", "imgIntGest - procressImage end");

        return mRgb;
	}
	
	private void writeToImage(int x, int y, String string) {
		Core.putText(mRgb, string, new Point(x, y),Core.FONT_HERSHEY_SIMPLEX, 1.5, new Scalar(0,0,0), 20);
		Core.putText(mRgb, string, new Point(x, y),Core.FONT_HERSHEY_SIMPLEX, 1.5, new Scalar(255,255,255), 10);
	}

	/*
	 * Draws convexity Defects in color image
	 * params: convexity defects, center of hand contour, hand contour
	 * 
	 */
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
	
	public HashMap<String,Boolean> getState(){
		return pointSelectState;
	}
}
