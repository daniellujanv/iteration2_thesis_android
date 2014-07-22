package itu.dluj.tesisprototype_iteration2.gesturerecognition;

import itu.dluj.tesisprototype_iteration2.GUIHandler;
import itu.dluj.tesisprototype_iteration2.StatesHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfInt4;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

public class PatientSelectionGestures {

	public HashMap<String, Boolean> pointSelectStates;

	public List<MatOfPoint> contours;

	public Mat mRgb;
	public Mat mHsv;
	public Mat mProcessed;
	public Mat kernel;

	public MatOfPoint mHandContour;
	public Mat hierarchy;
	public MatOfInt convexHull;
	public Point handContourCentroid;
	public MatOfInt4 convexityDefects;

	private int screenArea;
	private int screenHeight;
	private int screenWidth;
	private long timeLastDetectedGest;
	private Point lastPointedLocation;

	private String currentState;
	private String sStateInit = "Init";
	private String sStateEnd = "End";
	private String sStateZero = "Zipou";
	private String sStatePointSelect = "PointSelect";

	private Context appContext;
	private Activity mainActivity;
	private Toast tToastMsg;
	
	private GUIHandler guiHandler;

	public PatientSelectionGestures(int width, int height, Activity activity, GUIHandler handler){
		pointSelectStates = new HashMap<String, Boolean>();
		pointSelectStates.put("Init", false);
		pointSelectStates.put("End", false);
		pointSelectStates.put("PointSelect_Init", false);
		pointSelectStates.put("PointSelect_End", false);
		currentState = sStateZero;
		//		currentState = sStateInit;

		mainActivity = activity;
		appContext = activity.getApplicationContext();
		
		guiHandler = handler;

		screenHeight = height;
		screenWidth = width;
		screenArea = width*height;
		//	    Log.i("device-info", "Width:"+width+" Height:"+height);
		//        mRgb = new Mat(screenHeight, screenWidth, CvType.CV_8UC4);
		//        mHsv = new Mat(screenHeight, screenWidth, CvType.CV_8UC4);
		//        mProcessed = new Mat(screenHeight, screenWidth, CvType.CV_8UC4);
		mRgb = new Mat();

		mHandContour = new MatOfPoint();
		convexityDefects = new MatOfInt4();
		lastPointedLocation = new Point();
	}

	public Mat processImage(Mat inputImage, MatOfPoint contour, MatOfInt4 defects){
		mRgb = inputImage;
		mHandContour = contour;
		convexityDefects = defects;
		long now = System.currentTimeMillis();

		//if 5 seconds passed with no change go back to "zipou"
		if(((System.currentTimeMillis() - timeLastDetectedGest) >= 10000) && (currentState != sStateZero)){
			//no gestures detected for 8.0 seconds... go back to zipou
//			int x = (int)Math.round(screenWidth*0.05);
//			int y = (int)Math.round(screenHeight*0.35);
//			mRgb = Tools.writeToImage(mRgb, x, y, "back to "+sStateZero);
			postToast("back to "+ sStateZero);
			pointSelectStates.put(sStateInit, false);
			pointSelectStates.put("PointSelect_Init", false);
			pointSelectStates.put("PointSelect_End", false);
			pointSelectStates.put(sStateEnd, false);
			currentState = sStateZero;
			timeLastDetectedGest = System.currentTimeMillis();
		}else if( (now - timeLastDetectedGest)/1000 < Gestures.secondsToWait){
			//if 2 seconds have not passed since gesture detection, return				
			mRgb = guiHandler.writeInfoToImage(mRgb, "Wait " + (2 - (now - timeLastDetectedGest)/1000)+" sec." );		
			return mRgb;
		}

		detectGesture(mHandContour, convexityDefects);	
		Log.i("check", "imgIntGest - procressImage end");
		mHandContour.release();

		return mRgb;
	}

	public String getState(){
		return currentState;
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
		/*
		 * Always detect end gesture
		 */
		if(Gestures.detectEndGesture(convexityDefects, mHandContour) == true ){
			if(pointSelectStates.get("Init") == true){
				pointSelectStates.put("Init",true); 
				currentState = sStateInit;				
			}else{
				pointSelectStates.put("Init",false); 
				currentState = sStateZero;
			}
			pointSelectStates.put("Rotate_Init", false);
			pointSelectStates.put("Rotate_End", false); 
			pointSelectStates.put("Zoom_Init", false);
			pointSelectStates.put("Zoom_End", false); 
			//			drawDefects(convexityDefects, handContour);
			Log.i("ImageInteraction", "Gesture detected - End");
//			int x = (int)Math.round(screenWidth*0.05);
//			int y = (int)Math.round(screenHeight*0.15);
//			mRgb = Tools.writeToImage(mRgb, x, y, "END found!");
			postToast("END found!");
			timeLastDetectedGest = System.currentTimeMillis();		
		}
		
		if(currentState == sStateZero){
			//Init not detected no interaction has not started
			if(Gestures.detectInitGesture(convexityDefects, mHandContour) == true ){
				//good contour found
				pointSelectStates.put("Init", true);
				currentState = sStateInit;
				//				drawDefects(convexityDefects, handContour);
				Log.i("ImageInteraction", "Gesture detected - INIT");
//				int x = (int)Math.round(screenWidth*0.05);
//				int y = (int)Math.round(screenHeight*0.15);
//				mRgb = Tools.writeToImage(mRgb, x, y, "Init!");
				postToast("Init!");
				timeLastDetectedGest = System.currentTimeMillis();
				return;
			}
		}else if(currentState == sStateInit){
			//Interaction has started, nothing detected yet
			Point detectedPoint = Gestures.detectPointSelectGesture(convexityDefects, mHandContour, false); 
			if(detectedPoint != null){
				//			if(detectPointSelectGesture(convexityDefects, mHandContour, false) == true){
				lastPointedLocation = detectedPoint;
				Core.circle(mRgb, lastPointedLocation, 5, Tools.magenta, -1);

				currentState = sStatePointSelect;
				//				drawDefects(convexityDefects, handContour);
				//				Log.i("ImageInteraction", "Gesture detected - Zoom_End");
//				int x = (int)Math.round(screenWidth*0.05);
//				int y = (int)Math.round(screenHeight*0.15);
//				mRgb = Tools.writeToImage(mRgb, x, y, "PointSelect!");
				//wait only 1 seconds instead of 2
				timeLastDetectedGest = System.currentTimeMillis() - 1500;
				pointSelectStates.put("PointSelect_Init", true);				
				pointSelectStates.put("Init",true); 
				pointSelectStates.put("Swipe_Init", false);				
				pointSelectStates.put("Swipe_End", false);				
				postToast("PointSelect!");					

				return;
			}

		}else if(currentState == sStatePointSelect){
			Point detectedPoint = Gestures.detectPointSelectGesture(convexityDefects, mHandContour, true);
			Core.circle(mRgb, lastPointedLocation, 5, Tools.magenta, -1);
			if(detectedPoint != null){
//				Point defect_one = finalDefects.get(0);
//				Point defect_two = finalDefects.get(1);
//				Core.circle(mRgb, defect_one, 5, Tools.red, -1);
//				Core.circle(mRgb, defect_two, 5, Tools.red, -1);
				
				pointSelectStates.put("PointSelect_Init", false);				
				pointSelectStates.put("PointSelect_End", false);				
				pointSelectStates.put("Init",true); 
				pointSelectStates.put("Swipe_Init", false);				
				pointSelectStates.put("Swipe_End", false);	
				currentState = sStateInit;
				timeLastDetectedGest = System.currentTimeMillis();
				//				drawDefects(convexityDefects, handContour);
				//				Log.i("ImageInteraction", "Gesture detected - Zoom_End");
//				int x = (int)Math.round(screenWidth*0.05);
//				int y = (int)Math.round(screenHeight*0.15);
//				mRgb = Tools.writeToImage(mRgb, x, y, "PointSelect_End!");
				postToast("PointSelect_End!");
				return;
			}
			detectedPoint = Gestures.detectPointSelectGesture(convexityDefects, mHandContour, false); 
			if(detectedPoint != null){
				//			if(detectPointSelectGesture(convexityDefects, mHandContour, false) == true){
				lastPointedLocation = detectedPoint;
				Core.circle(mRgb, lastPointedLocation, 5, Tools.blue, -1);

				currentState = sStatePointSelect;
				//				drawDefects(convexityDefects, handContour);
				//				Log.i("ImageInteraction", "Gesture detected - Zoom_End");
//				int x = (int)Math.round(screenWidth*0.05);
//				int y = (int)Math.round(screenHeight*0.15);
//				mRgb = Tools.writeToImage(mRgb, x, y, "PointSelect!");
//				timeLastDetectedGest = System.currentTimeMillis() - 1000;
//				pointSelectStates.put("PointSelect_Init", true);				
//				pointSelectStates.put("Init",true); 
//				pointSelectStates.put("Swipe_Init", false);				
//				pointSelectStates.put("Swipe_End", false);				
//				postToast("PointSelect!");					
				return;
			}
		}

	}

	/*
	 * Utility method - writes to color image
	 */
	private void postToast(final String string) {
		mainActivity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				//					testToast.cancel();
				if(tToastMsg != null){
					tToastMsg.cancel();
				}
				tToastMsg = Toast.makeText(appContext, string, Toast.LENGTH_LONG);
				tToastMsg.show();
				//		            Toast.makeText(appContext, string, length).show();
			}
		});
	}

}
