package itu.dluj.tesisprototype_iteration2.gesturerecognition;

import itu.dluj.tesisprototype_iteration2.GUIHandler;
import itu.dluj.tesisprototype_iteration2.StatesHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

public class RecordViewingGestures {

	public Mat mRgb;

	public List<Point[]> lConvexityDefects;

//	private int screenArea;
//	private int screenHeight;
	private int screenWidth;
	public long timeLastDetectedGest;
	private Point lastPointedLocation;
	private Point initSwipeLocation;

	public String currentState;
	public boolean nextState;
	public boolean previousState;

	private Context appContext;
	private Activity mainActivity;
	private Toast tToastMsg;
	
	private GUIHandler guiHandler;


	public RecordViewingGestures(int width, int height, Activity activity, GUIHandler handler){
		currentState = StatesHandler.sStateZero;
		previousState = false;
		nextState = false;
		//		currentState = sStateInit;

		mainActivity = activity;
		appContext = activity.getApplicationContext();
		
		guiHandler = handler;

//		screenHeight = height;
		screenWidth = width;
//		screenArea = width*height;
		//	    Log.i("device-info", "Width:"+width+" Height:"+height);
		//        mRgb = new Mat(screenHeight, screenWidth, CvType.CV_8UC4);
		//        mHsv = new Mat(screenHeight, screenWidth, CvType.CV_8UC4);
		//        mProcessed = new Mat(screenHeight, screenWidth, CvType.CV_8UC4);
		mRgb = new Mat();

//		pCentroid = new Point();
		lConvexityDefects = new ArrayList<Point[]>();
		lastPointedLocation = new Point();
	}

	public Mat processImage(Mat inputImage, Point centroid, List<Point[]> lDefects){
		mRgb = inputImage;
//		pCentroid = centroid;
		lConvexityDefects = lDefects;
		
		previousState = false;
		nextState = false;
		
		detectGesture(centroid, lConvexityDefects);	
//		Log.i("check", "imgIntGest - procressImage end");

		return mRgb;
	}

	public String getState(){
		return currentState;
	}

	/*************************** Gesture Methods *************************************/

	private void detectGesture(Point centroid, List<Point[]> lDefects) {
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
		if(Gestures.detectEndGesture(lDefects, centroid) == true ){
			if(currentState != StatesHandler.sStateZero){
				postToast("End!");
				currentState = StatesHandler.sStateInit;				
				//			drawDefects(convexityDefects, handContour);
//				Log.i("ImageInteraction", "Gesture detected - End");
				//			int x = (int)Math.round(screenWidth*0.05);
				//			int y = (int)Math.round(screenHeight*0.15);
				//			mRgb = Tools.writeToImage(mRgb, x, y, "End!");
				timeLastDetectedGest = System.currentTimeMillis() - 1500;	
			}
		}
		
		if(currentState == StatesHandler.sStateZero){
			//Init not detected no interaction has not started
			if(Gestures.detectInitGesture(lDefects, centroid) == true ){
				postToast("Init!");
				//good contour found
				currentState = StatesHandler.sStateInit;
				//				drawDefects(convexityDefects, handContour);
				Log.i("ImageInteraction", "Gesture detected - INIT");
//				int x = (int)Math.round(screenWidth*0.05);
//				int y = (int)Math.round(screenHeight*0.15);
//				mRgb = Tools.writeToImage(mRgb, x, y, "Init!");
				timeLastDetectedGest = System.currentTimeMillis();
				return;
			}
		}else if(currentState == StatesHandler.sStateInit){
			//Interaction has started, nothing detected yet
			Point detectedPoint = Gestures.detectPointSelectGesture(lDefects, centroid, false);
			if( detectedPoint != null){
				postToast("PointSelect!");
				lastPointedLocation = detectedPoint;
				Core.circle(mRgb, lastPointedLocation, 5, Tools.magenta, -1);

				currentState = StatesHandler.sStatePointSelect;
				timeLastDetectedGest = System.currentTimeMillis() - 1500;
//				int x = (int)Math.round(screenWidth*0.05);
//				int y = (int)Math.round(screenHeight*0.15);
//				//				drawDefects(convexityDefects, handContour);
//				//				Log.i("ImageInteraction", "Gesture detected - Zoom_End");
//				mRgb = Tools.writeToImage(mRgb, x, y, "PointSelect!");
				return;
			}else{ 
				Point detectedPointSwipe = Gestures.detectSwipeGesture(lDefects, centroid, false); 
				if(detectedPointSwipe != null){
					postToast("Swipe!");
					initSwipeLocation = detectedPointSwipe;
					currentState = StatesHandler.sStateSwipe;
					timeLastDetectedGest = System.currentTimeMillis() - 1500;
//					int x = (int)Math.round(screenWidth*0.05);
//					int y = (int)Math.round(screenHeight*0.15);
//					//				drawDefects(convexityDefects, handContour);
//					//				Log.i("ImageInteraction", "Gesture detected - Rotate_End");
//					mRgb = Tools.writeToImage(mRgb, x, y, "Swipe!");
					return;
				}
			}


		}else if(currentState == StatesHandler.sStateSwipe){
			//Rotation initial gesture has been detected, look for rotation ending.
			//Or keep rotating until something happens
			Point detectedPoint = Gestures.detectSwipeGesture(lDefects, centroid, true);
			
			if( detectedPoint != null ){
				double traveledDistance = Tools.getDistanceBetweenPoints(initSwipeLocation, detectedPoint);
				if(traveledDistance > screenWidth*0.10){//more than 10% of the screen
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
					currentState = StatesHandler.sStateInit;
					timeLastDetectedGest = System.currentTimeMillis();
					//				drawDefects(convexityDefects, handContour);
					//				Log.i("ImageInteraction", "Gesture detected - Rotate_End");
					//				int x = (int)Math.round(screenWidth*0.05);
					//				int y = (int)Math.round(screenHeight*0.15);
					//				writeToImage( x, y, "Rotate_End found!");
					return;
				}
			}

		}else if(currentState == StatesHandler.sStatePointSelect){
			//Zoom in initial gesture has been detected, look for zoom ending. 
			//Or keep zooming until something happens
			Point detectedPoint = Gestures.detectPointSelectGesture(lDefects, centroid, true);
			if(detectedPoint != null){
//				postToast("PointSelect_END!");
				
				if(guiHandler.onClick(lastPointedLocation) == true){
					if(guiHandler.backBtnClicked == true){
						// if backBtn == false an image was clicked
						// click on imgBtn returns false
						previousState = true;
					}else if(guiHandler.bigImgShowing == true){
						nextState = true;
					}
				}
				currentState = StatesHandler.sStateInit;
				timeLastDetectedGest = System.currentTimeMillis();
				
				//				drawDefects(convexityDefects, handContour);
				//				Log.i("ImageInteraction", "Gesture detected - Zoom_End");
//				int x = (int)Math.round(screenWidth*0.05);
//				int y = (int)Math.round(screenHeight*0.15);
//				mRgb = Tools.writeToImage(mRgb, x, y, "PointSelect End!");
				return;
			}
			detectedPoint = Gestures.detectPointSelectGesture(lDefects, centroid, false); 
			if(detectedPoint != null){
				//			if(detectPointSelectGesture(convexityDefects, mHandContour, false) == true){
				lastPointedLocation = detectedPoint;
				Core.circle(mRgb, lastPointedLocation, 5, Tools.blue, -1);

				currentState = StatesHandler.sStatePointSelect;
				//				drawDefects(convexityDefects, handContour);
				//				Log.i("ImageInteraction", "Gesture detected - Zoom_End");
//				int x = (int)Math.round(screenWidth*0.05);
//				int y = (int)Math.round(screenHeight*0.15);
//				mRgb = Tools.writeToImage(mRgb, x, y, "PointSelect!");
				timeLastDetectedGest = System.currentTimeMillis() - 2000;
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
