package itu.dluj.tesisprototype_iteration2.gesturerecognition;


import itu.dluj.tesisprototype_iteration2.GUIHandler;
import itu.dluj.tesisprototype_iteration2.StatesHandler;

import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;

import android.app.Activity;
import android.util.Log;

public class ImageInteractionGestures {

	public String currentState;
	public boolean changeOfState;

	private Mat mRgb;
//	private Context appContext;
//	private Activity mainActivity;
	
	private GUIHandler guiHandler;


	private List<Point[]> lConvexityDefects;

	public long timeLastDetectedGest;
	private Point lastPointedLocation;


	private Point rotateInitPos;
	private double zoomInitDistance;

//	private int screenArea;
//	private int screenHeight;
	private int screenWidth;
//	private Toast tToastMsg;

	public ImageInteractionGestures(int width, int height, Activity activity, GUIHandler handler){
		currentState = StatesHandler.sStateZero;
		changeOfState = false;
		
//		mainActivity = activity;
//		appContext = mainActivity.getApplicationContext();
		
		guiHandler = handler;
		//		currentState = "Rotate";
		//		currentState = sStateInit;

//		mHandContour = new MatOfPoint();
		lConvexityDefects = new ArrayList<Point[]>();

//		screenHeight = height;
		screenWidth = width;
//		screenArea = width*height;
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
	public Mat processImage(Mat inputImage, Point centroid, List<Point[]> lDefects){
		mRgb = inputImage;
		lConvexityDefects = lDefects;
		
		changeOfState = false;
		detectGesture(centroid, lConvexityDefects);
//		Log.i("check", "imgIntGest - procressImage end");

		return mRgb;
	}


	public String getState(){
		return currentState;
	}

	/*******************gestures methods**************************/

	private void detectGesture(Point centroid, List<Point[]> lDefects) {
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
		if(Gestures.detectEndGesture(lDefects, centroid) == true ){
			if(currentState != StatesHandler.sStateZero){
//				postToast("END!");
				currentState = StatesHandler.sStateInit;				
				//			drawDefects(convexityDefects, handContour);
//				Log.i("ImageInteraction", "Gesture detected - End");
				//			int x = (int)Math.round(screenWidth*0.05);
				//			int y = (int)Math.round(screenHeight*0.15);
				//			mRgb = guiHandler.writeInfoToImage(mRgb, "END!");
				timeLastDetectedGest = System.currentTimeMillis() - 1500;	
			}
			//			SystemClock.sleep(500);
		}

		if(currentState == StatesHandler.sStateZero){
			//Init not detected no interaction has not started
			if(Gestures.detectInitGesture(lDefects, centroid) == true ){
//				postToast("Init!");
				currentState = StatesHandler.sStateInit;
				//				drawDefects(convexityDefects, handContour);
				Log.i("ImageInteraction", "Gesture detected - INIT");
//				int x = (int)Math.round(screenWidth*0.05);
//				int y = (int)Math.round(screenHeight*0.15);
				//				writeToImage( x, y, "INIT!", Toast.LENGTH_LONG);
//				mRgb = Tools.writeToImage(mRgb, x, y, "Init!");
				timeLastDetectedGest = System.currentTimeMillis();
				return;
			}
		}else if(currentState == StatesHandler.sStateInit){
			//Interaction has started, nothing detected yet
			Point detectedPoint = Gestures.detectRotateGesture(lDefects, centroid, false);
			if( detectedPoint != null ){
//				postToast("Rotate!");
				rotateInitPos = detectedPoint;
				currentState = StatesHandler.sStateRotate;
				//				drawDefects(convexityDefects, handContour);
				Log.i("ImageInteraction", "Gesture detected - Rotate_Init");
//				int x = (int)Math.round(screenWidth*0.05);
//				int y = (int)Math.round(screenHeight*0.15);
				//				writeToImage( x, y, "Rotate!", Toast.LENGTH_LONG);
//				mRgb = Tools.writeToImage(mRgb, x, y, "Rotate!");
				timeLastDetectedGest = System.currentTimeMillis() - 1000;
				return;
			}else {
				double detectedDistance = Gestures.detectZoomGesture(lDefects, centroid, false); 
				if(detectedDistance != -1){
//					postToast("Zoom!");		
					zoomInitDistance = detectedDistance;
					currentState = StatesHandler.sStateZoom;
					//				drawDefects(convexityDefects, handContour);
					Log.i("ImageInteraction", "Gesture detected - Zoom_Init");
//					int x = (int)Math.round(screenWidth*0.05);
//					int y = (int)Math.round(screenHeight*0.15);
//					mRgb = Tools.writeToImage(mRgb, x, y, "Zoom!");		
					timeLastDetectedGest = System.currentTimeMillis() - 1000;	
					return;
				}else{
					detectedPoint = Gestures.detectPointSelectGesture(lDefects, centroid, false); 
					if(detectedPoint != null){
						//			if(detectPointSelectGesture(convexityDefects, mHandContour, false) == true){
//						postToast("PointSelect!");					
						lastPointedLocation = detectedPoint;
						Core.circle(mRgb, lastPointedLocation, 5, Tools.magenta, -1);

						currentState = StatesHandler.sStatePointSelect;
						//				drawDefects(convexityDefects, handContour);
						//				Log.i("ImageInteraction", "Gesture detected - Zoom_End");
						//				int x = (int)Math.round(screenWidth*0.05);
						//				int y = (int)Math.round(screenHeight*0.15);
						//				mRgb = Tools.writeToImage(mRgb, x, y, "PointSelect!");
						//wait only 1 seconds instead of 2
						timeLastDetectedGest = System.currentTimeMillis() - 1000;

						return;
					}
				}
			}

		}else if(currentState == StatesHandler.sStateRotate){
			//Rotation initial gesture has been detected, look for rotation ending.
			//Or keep rotating until something happens
			Point detectedPoint = Gestures.detectRotateGesture(lDefects, centroid, true);
			if(detectedPoint != null ){
				Point rotateEndPos = detectedPoint;
				double traveledDistance = Tools.getDistanceBetweenPoints(rotateInitPos, rotateEndPos);
				if(traveledDistance > screenWidth*0.10){//more than 10% of the screen
					if((rotateInitPos.x >= rotateEndPos.x)){
						//rotate left
						Log.i("ImageInteraction", "Rotate gesture::LEFT ");
						if(guiHandler.rotate("left")){
//							postToast("Rotate Left!");
							currentState = StatesHandler.sStateInit;
							timeLastDetectedGest = System.currentTimeMillis();
						}
//						int x = (int)Math.round(screenWidth*0.05);
//						int y = (int)Math.round(screenHeight*0.15);
//						mRgb = Tools.writeToImage(mRgb, x, y, "Rotate Left!");		
					}else if((rotateInitPos.x < rotateEndPos.x) ){
						//rotate right
						Log.i("ImageInteraction", "Rotate gesture::RIGHT");
						if(guiHandler.rotate("right")){
							currentState = StatesHandler.sStateInit;
							timeLastDetectedGest = System.currentTimeMillis();
//							postToast("Rotate Right!");
						}
//						int x = (int)Math.round(screenWidth*0.05);
//						int y = (int)Math.round(screenHeight*0.15);
//						mRgb = Tools.writeToImage(mRgb, x, y, "Rotate Right!");		
					}
					//					rotateInitPos = new Point();
					return;
				}
			}
		}else if(currentState == StatesHandler.sStateZoom){
			//Zoom in initial gesture has been detected, look for zoom ending. 
			//Or keep zooming until something happens
			double detectedDistance = Gestures.detectZoomGesture(lDefects, centroid, true);
			if(detectedDistance != -1){
				double zoomEndDistance = detectedDistance;
				//				Log.i("ImageInteraction", "Zoom End distances::"
				//						+ " distance: " + (Math.abs(zoomEndDistance - zoomInitDistance))
				//						+ " width: " + screenWidth*0.10
				//						);

				if(Math.abs(zoomEndDistance - zoomInitDistance) > screenWidth*0.05 ){ //more than 05% of screen
					if(zoomEndDistance > zoomInitDistance){
						//Zoom-IN gesture
						if(guiHandler.zoom("in")){
							Log.i("ImageInteraction", "Zoom gesture::IN");
							currentState = StatesHandler.sStateInit;
							timeLastDetectedGest = System.currentTimeMillis();
//							postToast("Zoom IN");
						}
//						int x = (int)Math.round(screenWidth*0.05);
//						int y = (int)Math.round(screenHeight*0.15);
//						mRgb = Tools.writeToImage(mRgb, x, y, "Zoom in!");		

					}else if(zoomEndDistance < zoomInitDistance){
						//Zoom-OUT gesture
						if(guiHandler.zoom("out")){
							currentState = StatesHandler.sStateInit;
							timeLastDetectedGest = System.currentTimeMillis();
							Log.i("ImageInteraction", "Zoom gesture::OUT");
//							postToast("Zoom OUT");
						}
//						int x = (int)Math.round(screenWidth*0.05);
//						int y = (int)Math.round(screenHeight*0.15);
//						mRgb = Tools.writeToImage(mRgb, x, y, "Zoom out!");		

					}
					return;
				}

			}
		}else if(currentState == StatesHandler.sStatePointSelect){
			Point detectedPoint = Gestures.detectPointSelectGesture(lDefects, centroid, true);
			Core.circle(mRgb, lastPointedLocation, 5, Tools.magenta, -1);
			if(detectedPoint != null){
				//				Point defect_one = finalDefects.get(0);
				//				Point defect_two = finalDefects.get(1);
				//				Core.circle(mRgb, defect_one, 5, Tools.red, -1);
				//				Core.circle(mRgb, defect_two, 5, Tools.red, -1);
//				postToast("PointSelect_End!");
				if(guiHandler.onClick(lastPointedLocation) == true){
					currentState = StatesHandler.sStateInit;
					timeLastDetectedGest = System.currentTimeMillis();
					changeOfState = true;
				}
				//				drawDefects(convexityDefects, handContour);
				//				Log.i("ImageInteraction", "Gesture detected - Zoom_End");
				//				int x = (int)Math.round(screenWidth*0.05);
				//				int y = (int)Math.round(screenHeight*0.15);
				//				mRgb = Tools.writeToImage(mRgb, x, y, "PointSelect_End!");
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
//	private void postToast(final String string) {
//		mainActivity.runOnUiThread(new Runnable() {
//			@Override
//			public void run() {
//				//					testToast.cancel();
//				if(tToastMsg != null){
//					tToastMsg.cancel();
//				}
//				tToastMsg = Toast.makeText(appContext, string, Toast.LENGTH_LONG);
//				tToastMsg.show();
//				//		            Toast.makeText(appContext, string, length).show();
//			}
//		});
//	}
}
