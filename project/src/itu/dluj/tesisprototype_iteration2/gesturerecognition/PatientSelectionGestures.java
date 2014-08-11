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

	public long timeLastDetectedGest;
	private Point lastPointedLocation;
	private List<Point> lPointedLocations;

	public String currentState;
	public boolean changeOfState;


	private GUIHandler guiHandler;

	public PatientSelectionGestures(int width, int height, Activity activity, GUIHandler handler){
		currentState = StatesHandler.sStateZero;
		changeOfState = false;

		guiHandler = handler;
		mRgb = new Mat();

		lConvexityDefects = new ArrayList<Point[]>();
		lastPointedLocation = new Point();
		lPointedLocations = new ArrayList<Point>();
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
		 * Always detect end gesture
		 */
		if(Gestures.detectEndGesture(lDefects, centroid) == true ){
			if(currentState != StatesHandler.sStateZero){
				currentState = StatesHandler.sStateInit;				
				timeLastDetectedGest = System.currentTimeMillis() - 1500;		
			}
		}

		if(currentState == StatesHandler.sStateZero){
			//Init not detected no interaction has not started
			if(Gestures.detectInitGesture(lDefects, centroid) == true ){
				//good contour found
				currentState = StatesHandler.sStateInit;
				Log.i("ImageInteraction", "Gesture detected - INIT");
				timeLastDetectedGest = System.currentTimeMillis();
				return;
			}
		}else if(currentState == StatesHandler.sStateInit){
			//Interaction has started, nothing detected yet
			Point detectedPoint = Gestures.detectPointSelectGesture(lDefects, centroid, false); 
			if(detectedPoint != null){
				addPointedLocation(detectedPoint);
				currentState = StatesHandler.sStatePointSelect;
				//wait only 1 seconds instead of 2
				timeLastDetectedGest = System.currentTimeMillis() - 1000;

				return;
			}

		}else if(currentState == StatesHandler.sStatePointSelect){
			Point detectedPoint = Gestures.detectPointSelectGesture(lDefects, centroid, true);
			Core.circle(mRgb, getLastPointedLocation(), 5, Tools.magenta, -1);
			if(detectedPoint != null){
				addPointedLocation(detectedPoint);
				
				if(guiHandler.onClick(getLastPointedLocation()) == false){
//					postToast("Nothing Clicked!");					
				}else{
					changeOfState = true;
					currentState = StatesHandler.sStateInit;
					timeLastDetectedGest = System.currentTimeMillis();
				}
				return;
			}
			detectedPoint = Gestures.detectPointSelectGesture(lDefects, centroid, false); 
			if(detectedPoint != null){
				//			if(detectPointSelectGesture(convexityDefects, mHandContour, false) == true){
				addPointedLocation(detectedPoint);

				currentState = StatesHandler.sStatePointSelect;
				timeLastDetectedGest = System.currentTimeMillis() - 2000;
				return;
			}
		}

	}


	private void addPointedLocation(Point pointedLoc){
		if(lPointedLocations.size() > 10){
			lPointedLocations.remove(0);
		}

		lPointedLocations.add(pointedLoc);
	}

	private Point getLastPointedLocation(){
		int x = 0;
		int y = 0;
		int weights = 0;
		for(int i = 0; i< lPointedLocations.size(); i++){
			x += lPointedLocations.get(i).x* (i/4);
			y += lPointedLocations.get(i).y* (i/4);
			weights += (i/4);
		}
		x = (x == 0)? 0: x/weights;
		y = (y == 0)? 0: y/weights;
		Point result = new Point(x, y);
		return result;
	}
	
}
