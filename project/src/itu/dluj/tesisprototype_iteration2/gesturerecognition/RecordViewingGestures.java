package itu.dluj.tesisprototype_iteration2.gesturerecognition;

import itu.dluj.tesisprototype_iteration2.GUIHandler;

import java.util.HashMap;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt4;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

public class RecordViewingGestures {

	public HashMap<String, Boolean> navigationStates;
	public Mat mRgb;

	public MatOfPoint mHandContour;
	public MatOfInt4 convexityDefects;

	private int screenArea;
	private int screenHeight;
	private int screenWidth;
	private long timeLastDetectedGest;
	private Point lastPointedLocation;
	private Point initSwipeLocation;

	private String currentState;
	private String sStateInit = "Init";
	@SuppressWarnings("unused")
	private String sStateEnd = "End";
	private String sStateZero = "Zipou";
	private String sStateSwipe = "Swipe";
	private String sStatePointSelect = "PointSelect";

	private Context appContext;
	private Activity mainActivity;
	private Toast tToastMsg;
	
	private GUIHandler guiHandler;


	public RecordViewingGestures(int width, int height, Activity activity, GUIHandler handler){
		navigationStates = new HashMap<String, Boolean>();
		navigationStates.put("Init", false);
		navigationStates.put("End", false);
		navigationStates.put("Swipe_Init", false);
		navigationStates.put("Swipe_End", false);
		navigationStates.put("PointSelect_Init", false);
		navigationStates.put("PointSelect_End", false);
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
		//if 2 seconds have not passed since gesture detection, return
		if( (now - timeLastDetectedGest)/1000 < Gestures.secondsToWait){
//			int x = (int)Math.round(screenWidth*0.05);
//			int y = (int)Math.round(screenHeight*0.15);
			mRgb = guiHandler.writeInfoToImage(mRgb, "Wait " + (2 - (now - timeLastDetectedGest)/1000)+" sec." );		
			return mRgb;
		}
		
		//if 5 seconds passed with no change go back to "zipou"
		if(((now - timeLastDetectedGest) >= 10000) && (currentState != sStateZero)){
			//no gestures detected for 8.0 seconds... go back to zipou
//			int x = (int)Math.round(screenWidth*0.05);
//			int y = (int)Math.round(screenHeight*0.35);
//			mRgb = Tools.writeToImage(mRgb, x, y, "back to "+sStateZero);
			postToast("back to "+sStateZero);
			navigationStates.put(sStateInit, false);
			navigationStates.put("Rotate_Init", false);
			navigationStates.put("Rotate_End", false); 
			navigationStates.put("Zoom_Init", false);
			navigationStates.put("Zoom_End", false);
			currentState = sStateZero;
			timeLastDetectedGest = System.currentTimeMillis();
		}


		detectGesture(mHandContour, convexityDefects);	
		mHandContour.release();
		
		Log.i("check", "imgIntGest - procressImage end");

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
		if(currentState == sStateZero){
			//Init not detected no interaction has not started
			if(Gestures.detectInitGesture(convexityDefects, mHandContour) == true ){
				//good contour found
				navigationStates.put("Init", true);
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
			if( detectedPoint != null){
				lastPointedLocation = detectedPoint;
				Core.circle(mRgb, lastPointedLocation, 5, Tools.magenta, -1);

				navigationStates.put("PointSelect_Init", true);				
				navigationStates.put("Init",true); 
				navigationStates.put("Swipe_Init", false);				
				navigationStates.put("Swipe_End", false);				
				currentState = sStatePointSelect;
				timeLastDetectedGest = System.currentTimeMillis() - 1500;
//				int x = (int)Math.round(screenWidth*0.05);
//				int y = (int)Math.round(screenHeight*0.15);
//				//				drawDefects(convexityDefects, handContour);
//				//				Log.i("ImageInteraction", "Gesture detected - Zoom_End");
//				mRgb = Tools.writeToImage(mRgb, x, y, "PointSelect!");
				postToast("PointSelect!");
				return;
			}else{ 
				Point detectedPointSwipe = Gestures.detectSwipeGesture(convexityDefects, mHandContour, false); 
				if(detectedPointSwipe != null){
					initSwipeLocation = detectedPointSwipe;
					navigationStates.put("Swipe_Init", true);
					navigationStates.put("Init",true); 
					navigationStates.put("PointSelect_Init", false);				
					navigationStates.put("PointSelect_End", false);		
					currentState = sStateSwipe;
					timeLastDetectedGest = System.currentTimeMillis();
//					int x = (int)Math.round(screenWidth*0.05);
//					int y = (int)Math.round(screenHeight*0.15);
//					//				drawDefects(convexityDefects, handContour);
//					//				Log.i("ImageInteraction", "Gesture detected - Rotate_End");
//					mRgb = Tools.writeToImage(mRgb, x, y, "Swipe!");
					postToast("Swipe!");
					return;
				}
			}


		}else if(currentState == sStateSwipe){
			//Rotation initial gesture has been detected, look for rotation ending.
			//Or keep rotating until something happens
			Point detectedPoint = Gestures.detectSwipeGesture(convexityDefects, mHandContour, true);
			
			if( detectedPoint != null ){
				double traveledDistance = Tools.getDistanceBetweenPoints(initSwipeLocation, detectedPoint);
				if(traveledDistance > screenWidth*0.30){//more than 30% of the screen
//					int x = (int)Math.round(screenWidth*0.05);
//					int y = (int)Math.round(screenHeight*0.15);
					//				drawDefects(convexityDefects, handContour);
					//				Log.i("ImageInteraction", "Gesture detected - Rotate_End");

					if(initSwipeLocation.x < detectedPoint.x){
						postToast("Swipe - Rigth");
//						mRgb = Tools.writeToImage(mRgb, x, y, "Swipe Right!");
					}else{
						postToast("Swipe - Left");
//						mRgb = Tools.writeToImage(mRgb, x, y, "Swipe Left!");
					}

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
					return;
				}
			}

		}else if(currentState == sStatePointSelect){
			//Zoom in initial gesture has been detected, look for zoom ending. 
			//Or keep zooming until something happens
			Point detectedPoint = Gestures.detectPointSelectGesture(convexityDefects, mHandContour, true);
			if(detectedPoint != null){
				navigationStates.put("PointSelect_Init", false);				
				navigationStates.put("PointSelect_End", false);				
				navigationStates.put("Init",true); 
				navigationStates.put("Swipe_Init", false);				
				navigationStates.put("Swipe_End", false);	
				currentState = sStateInit;
				timeLastDetectedGest = System.currentTimeMillis();
				//				drawDefects(convexityDefects, handContour);
				//				Log.i("ImageInteraction", "Gesture detected - Zoom_End");
//				int x = (int)Math.round(screenWidth*0.05);
//				int y = (int)Math.round(screenHeight*0.15);
//				mRgb = Tools.writeToImage(mRgb, x, y, "PointSelect End!");
				postToast("PointSelect_END!");
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

		/*
		 * Always detect end gesture
		 */
		if(Gestures.detectEndGesture(convexityDefects, mHandContour) == true ){
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
//			int x = (int)Math.round(screenWidth*0.05);
//			int y = (int)Math.round(screenHeight*0.15);
//			mRgb = Tools.writeToImage(mRgb, x, y, "End!");
			postToast("End!");
			timeLastDetectedGest = System.currentTimeMillis();		
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
