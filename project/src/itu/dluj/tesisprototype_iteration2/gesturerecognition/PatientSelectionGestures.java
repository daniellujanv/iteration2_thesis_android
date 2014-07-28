package itu.dluj.tesisprototype_iteration2.gesturerecognition;

import itu.dluj.tesisprototype_iteration2.GUIHandler;
import itu.dluj.tesisprototype_iteration2.StatesHandler;

import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;

import android.app.Activity;
import android.util.Log;

public class PatientSelectionGestures {

	public List<MatOfPoint> contours;

	public Mat mRgb;

	public Mat hierarchy;
	public MatOfInt convexHull;
	public List<Point[]> lConvexityDefects;

	//	private int screenArea;
	//	private int screenHeight;
	//	private int screenWidth;
	public long timeLastDetectedGest;
	private Point lastPointedLocation;

	public String currentState;
	public boolean changeOfState;

//	private Context appContext;
//	private Activity mainActivity;
//	private Toast tToastMsg;

	private GUIHandler guiHandler;

	public PatientSelectionGestures(int width, int height, Activity activity, GUIHandler handler){
		currentState = StatesHandler.sStateZero;
		changeOfState = false;
		//		currentState = sStateInit;
//
//		mainActivity = activity;
//		appContext = activity.getApplicationContext();

		guiHandler = handler;

		//		screenHeight = height;
		//		screenWidth = width;
		//		screenArea = width*height;
		//	    Log.i("device-info", "Width:"+width+" Height:"+height);
		//        mRgb = new Mat(screenHeight, screenWidth, CvType.CV_8UC4);
		//        mHsv = new Mat(screenHeight, screenWidth, CvType.CV_8UC4);
		//        mProcessed = new Mat(screenHeight, screenWidth, CvType.CV_8UC4);
		mRgb = new Mat();

		lConvexityDefects = new ArrayList<Point[]>();
		lastPointedLocation = new Point();
	}

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
//				postToast("END!");
				currentState = StatesHandler.sStateInit;				
				//			drawDefects(convexityDefects, handContour);
				//				Log.i("ImageInteraction", "Gesture detected - End");
				//			int x = (int)Math.round(screenWidth*0.05);
				//			int y = (int)Math.round(screenHeight*0.15);
				//			mRgb = Tools.writeToImage(mRgb, x, y, "END found!");
				timeLastDetectedGest = System.currentTimeMillis() - 1500;		
			}
		}

		if(currentState == StatesHandler.sStateZero){
			//Init not detected no interaction has not started
			if(Gestures.detectInitGesture(lDefects, centroid) == true ){
				//good contour found
//				postToast("Init!");
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
			if(detectedPoint != null){
				//			if(detectPointSelectGesture(convexityDefects, mHandContour, false) == true){
//				postToast("PointSelect!");					
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

		}else if(currentState == StatesHandler.sStatePointSelect){
			Point detectedPoint = Gestures.detectPointSelectGesture(lDefects, centroid, true);
			Core.circle(mRgb, lastPointedLocation, 5, Tools.magenta, -1);
			if(detectedPoint != null){
				//				Point defect_one = finalDefects.get(0);
				//				Point defect_two = finalDefects.get(1);
				//				Core.circle(mRgb, defect_one, 5, Tools.red, -1);
				//				Core.circle(mRgb, defect_two, 5, Tools.red, -1);
				
				if(guiHandler.onClick(lastPointedLocation) == false){
//					postToast("Nothing Clicked!");					
				}else{
					changeOfState = true;
					currentState = StatesHandler.sStateInit;
					timeLastDetectedGest = System.currentTimeMillis();
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
