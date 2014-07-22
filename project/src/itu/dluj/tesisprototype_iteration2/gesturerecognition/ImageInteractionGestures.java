package itu.dluj.tesisprototype_iteration2.gesturerecognition;


import itu.dluj.tesisprototype_iteration2.GUIHandler;
import itu.dluj.tesisprototype_iteration2.StatesHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt4;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

public class ImageInteractionGestures {

	private HashMap<String, Boolean> interactionStates;
	private String currentState;

	private Mat mRgb;
	private Context appContext;
	private Activity mainActivity;
	
	private GUIHandler guiHandler;


	private MatOfPoint mHandContour;
	private MatOfInt4 convexityDefects;

	private long timeLastDetectedGest;
	private final String sStateZero = "Zipou";
	private final String sStateInit = "Init";
	private final String sStateZoom = "Zoom";
	private final String sStateRotate = "Rotate";
	private final String sStateEnd ="End";

	private Point rotateInitPos;
	private double zoomInitDistance;

	private int screenArea;
	private int screenHeight;
	private int screenWidth;
	private Toast tToastMsg;

	public ImageInteractionGestures(int width, int height, Activity activity, GUIHandler handler){
		interactionStates = new HashMap<String, Boolean>();
		interactionStates.put(sStateInit, false);
		interactionStates.put(sStateEnd, false);
		interactionStates.put("Rotate_Init", false);
		interactionStates.put("Rotate_End", false);
		interactionStates.put("Zoom_Init", false);
		interactionStates.put("Zoom_End", false);
		currentState = sStateZero;
		
		mainActivity = activity;
		appContext = mainActivity.getApplicationContext();
		
		guiHandler = handler;
		//		currentState = "Rotate";
		//		currentState = sStateInit;

		mHandContour = new MatOfPoint();
		convexityDefects = new MatOfInt4();

		screenHeight = height;
		screenWidth = width;
		screenArea = width*height;
		//	    Log.i("device-info", "Width:"+width+" Height:"+height);
		//        mRgb = new Mat(screenHeight, screenWidth, CvType.CV_8UC4);
		//        mHsv = new Mat(screenHeight, screenWidth, CvType.CV_8UC4);
		//        mProcessed = new Mat(screenHeight, screenWidth, CvType.CV_8UC4);
		mRgb = new Mat();

		timeLastDetectedGest = System.currentTimeMillis();

	}

	/*
	 * Process image - applying operations to the image
	 * common to every gesture before detecting them
	 */
	public Mat processImage(Mat inputImage, MatOfPoint contour, MatOfInt4 defects){
		mRgb = inputImage;
		mHandContour = contour;
		convexityDefects = defects;
		long now = System.currentTimeMillis();
		//if 2 seconds have not passed since gesture detection, return
		if( (now - timeLastDetectedGest)/1000 < Gestures.secondsToWait){
			mRgb = guiHandler.writeInfoToImage(mRgb, "Wait " + (2 - (now - timeLastDetectedGest)/1000)+" sec." );		
			return mRgb;
		}

		//if 5 seconds passed with no change go back to "zipou"
		if(((now - timeLastDetectedGest) >= 10000) && (currentState != sStateZero)){
			//no gestures detected for 8.0 seconds... go back to zipou
			//			int x = (int)Math.round(screenWidth*0.05);
			//			int y = (int)Math.round(screenHeight*0.35);
			postToast("Back to "+sStateZero);
			interactionStates.put(sStateInit, false);
			interactionStates.put("Rotate_Init", false);
			interactionStates.put("Rotate_End", false); 
			interactionStates.put("Zoom_Init", false);
			interactionStates.put("Zoom_End", false);
			currentState = sStateZero;
			timeLastDetectedGest = System.currentTimeMillis();
		}

		detectGesture(mHandContour, convexityDefects);
		Log.i("check", "imgIntGest - procressImage end");
		mHandContour.release();

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

		/*
		 * Always detect end gesture
		 */
		if(Gestures.detectEndGesture(convexityDefects, handContour) == true ){
			if(interactionStates.get(sStateInit) == true){
				interactionStates.put(sStateInit,true); 
				currentState = sStateInit;				
			}else{
				interactionStates.put(sStateInit,false); 
				currentState = sStateZero;
			}
			interactionStates.put("Rotate_Init", false);
			interactionStates.put("Rotate_End", false); 
			interactionStates.put("Zoom_Init", false);
			interactionStates.put("Zoom_End", false); 
			//			drawDefects(convexityDefects, handContour);
			Log.i("ImageInteraction", "Gesture detected - End");
//			int x = (int)Math.round(screenWidth*0.05);
//			int y = (int)Math.round(screenHeight*0.15);
//			mRgb = guiHandler.writeInfoToImage(mRgb, "END!");
			postToast("END!");
			timeLastDetectedGest = System.currentTimeMillis();		
			//			SystemClock.sleep(500);
		}

		if(currentState == sStateZero){
			//Init not detected no interaction has not started
			if(Gestures.detectInitGesture(convexityDefects, handContour) == true ){
				interactionStates.put(sStateInit, true);
				currentState = sStateInit;
				//				drawDefects(convexityDefects, handContour);
				Log.i("ImageInteraction", "Gesture detected - INIT");
//				int x = (int)Math.round(screenWidth*0.05);
//				int y = (int)Math.round(screenHeight*0.15);
				//				writeToImage( x, y, "INIT!", Toast.LENGTH_LONG);
				postToast("Init!");
//				mRgb = Tools.writeToImage(mRgb, x, y, "Init!");
				timeLastDetectedGest = System.currentTimeMillis();
				return;
			}
		}else if(currentState == sStateInit){
			//Interaction has started, nothing detected yet
			Point detectedPoint = Gestures.detectRotateGesture(convexityDefects, handContour, false);
			if( detectedPoint != null ){
				rotateInitPos = detectedPoint;
				interactionStates.put("Rotate_Init", true);
				//making sure the others are false
				interactionStates.put("Zoom_Init", false);
				interactionStates.put("Zoom_Init", false);
				currentState = sStateRotate;
				//				drawDefects(convexityDefects, handContour);
				Log.i("ImageInteraction", "Gesture detected - Rotate_Init");
//				int x = (int)Math.round(screenWidth*0.05);
//				int y = (int)Math.round(screenHeight*0.15);
				//				writeToImage( x, y, "Rotate!", Toast.LENGTH_LONG);
				postToast("Rotate!");
//				mRgb = Tools.writeToImage(mRgb, x, y, "Rotate!");
				timeLastDetectedGest = System.currentTimeMillis();
				return;
			}else {
				double detectedDistance = Gestures.detectZoomGesture(convexityDefects, handContour, false); 
				if(detectedDistance != -1){
					zoomInitDistance = detectedDistance;
					interactionStates.put("Zoom_Init", true);
					//making sure the others are false
					interactionStates.put("Rotate_Init", false);
					interactionStates.put("Rotate_Init", false);
					currentState = sStateZoom;
					//				drawDefects(convexityDefects, handContour);
					Log.i("ImageInteraction", "Gesture detected - Zoom_Init");
//					int x = (int)Math.round(screenWidth*0.05);
//					int y = (int)Math.round(screenHeight*0.15);
//					mRgb = Tools.writeToImage(mRgb, x, y, "Zoom!");		
					postToast("Zoom!");		
					timeLastDetectedGest = System.currentTimeMillis();	
					return;
				}
			}
		}else if(currentState == sStateRotate){
			//Rotation initial gesture has been detected, look for rotation ending.
			//Or keep rotating until something happens
			Point detectedPoint = Gestures.detectRotateGesture(convexityDefects, handContour, true);
			if(detectedPoint != null ){
				Point rotateEndPos = detectedPoint;
				double traveledDistance = Tools.getDistanceBetweenPoints(rotateInitPos, rotateEndPos);
				if(traveledDistance > screenWidth*0.10){//more than 10% of the screen
					if((rotateInitPos.x >= rotateEndPos.x)){
						//rotate left
						Log.i("ImageInteraction", "Rotate gesture::LEFT ");
						postToast("Rotate Left!");
//						int x = (int)Math.round(screenWidth*0.05);
//						int y = (int)Math.round(screenHeight*0.15);
//						mRgb = Tools.writeToImage(mRgb, x, y, "Rotate Left!");		
					}else if((rotateInitPos.x < rotateEndPos.x) ){
						//rotate right
						Log.i("ImageInteraction", "Rotate gesture::RIGHT");
						postToast("Rotate Right!");
//						int x = (int)Math.round(screenWidth*0.05);
//						int y = (int)Math.round(screenHeight*0.15);
//						mRgb = Tools.writeToImage(mRgb, x, y, "Rotate Right!");		
					}
					//					rotateInitPos = new Point();

					interactionStates.put("Rotate_Init", false);
					interactionStates.put(sStateInit,true); 
					interactionStates.put("Zoom_Init", false);
					interactionStates.put("Zoom_End", false);
					currentState = sStateInit;
					timeLastDetectedGest = System.currentTimeMillis();
					return;
				}
			}

		}else if(currentState == sStateZoom){
			//Zoom in initial gesture has been detected, look for zoom ending. 
			//Or keep zooming until something happens
			double detectedDistance = Gestures.detectZoomGesture(convexityDefects, handContour, true);
			if(detectedDistance != -1){
				double zoomEndDistance = detectedDistance;
				//				Log.i("ImageInteraction", "Zoom End distances::"
				//						+ " distance: " + (Math.abs(zoomEndDistance - zoomInitDistance))
				//						+ " width: " + screenWidth*0.10
				//						);

				if(Math.abs(zoomEndDistance - zoomInitDistance) > screenWidth*0.05 ){ //more than 05% of screen
					if(zoomEndDistance > zoomInitDistance){
						//Zoom-IN gesture
						zoomInitDistance = zoomEndDistance;
						Log.i("ImageInteraction", "Zoom gesture::IN");
						postToast("Zoom IN");
//						int x = (int)Math.round(screenWidth*0.05);
//						int y = (int)Math.round(screenHeight*0.15);
//						mRgb = Tools.writeToImage(mRgb, x, y, "Zoom in!");		

					}else if(zoomEndDistance < zoomInitDistance){
						//Zoom-OUT gesture
						zoomInitDistance = zoomEndDistance;
						Log.i("ImageInteraction", "Zoom gesture::OUT");
						postToast("Zoom OUT");
//						int x = (int)Math.round(screenWidth*0.05);
//						int y = (int)Math.round(screenHeight*0.15);
//						mRgb = Tools.writeToImage(mRgb, x, y, "Zoom out!");		

					}

					interactionStates.put("Zoom_Init", false);				
					interactionStates.put(sStateInit,true); 
					interactionStates.put("Rotate_Init", false);
					interactionStates.put("Rotate_End", false);
					currentState = sStateInit;
					timeLastDetectedGest = System.currentTimeMillis();
					return;
				}

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
