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
	private List<Point> lPointedLocations;


	private Point rotateInitPos;
	private double zoomInitDistance;

//	private int screenArea;
//	private int screenHeight;
	private int screenWidth;
//	private Toast tToastMsg;

	public ImageInteractionGestures(int width, int height, Activity activity, GUIHandler handler){
		currentState = StatesHandler.sStateZero;
		changeOfState = false;
		guiHandler = handler;
		lConvexityDefects = new ArrayList<Point[]>();

		screenWidth = width;
		mRgb = new Mat();

		timeLastDetectedGest = System.currentTimeMillis();
		lPointedLocations = new ArrayList<Point>();
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

		return mRgb;
	}


	public String getState(){
		return currentState;
	}

	/*******************gestures methods**************************/

	private void detectGesture(Point centroid, List<Point[]> lDefects) {
		/*
		 * 
		 */

		/*
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
				currentState = StatesHandler.sStateInit;
				Log.i("ImageInteraction", "Gesture detected - INIT");
				timeLastDetectedGest = System.currentTimeMillis();
				return;
			}
		}else if(currentState == StatesHandler.sStateInit){
			//Interaction has started, nothing detected yet
			Point detectedPoint = Gestures.detectRotateGesture(lDefects, centroid, false);
			if( detectedPoint != null ){
				rotateInitPos = detectedPoint;
				currentState = StatesHandler.sStateRotate;
				Log.i("ImageInteraction", "Gesture detected - Rotate_Init");
				timeLastDetectedGest = System.currentTimeMillis() - 1000;
				return;
			}else {
				double detectedDistance = Gestures.detectZoomGesture(lDefects, centroid, false); 
				if(detectedDistance != -1){
					zoomInitDistance = detectedDistance;
					currentState = StatesHandler.sStateZoom;
					Log.i("ImageInteraction", "Gesture detected - Zoom_Init");
					timeLastDetectedGest = System.currentTimeMillis() - 1000;	
					return;
				}else{
					detectedPoint = Gestures.detectPointSelectGesture(lDefects, centroid, false); 
					if(detectedPoint != null){
						addPointedLocation(detectedPoint);
						currentState = StatesHandler.sStatePointSelect;
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
							currentState = StatesHandler.sStateInit;
							timeLastDetectedGest = System.currentTimeMillis();
						}
					}else if((rotateInitPos.x < rotateEndPos.x) ){
						//rotate right
						Log.i("ImageInteraction", "Rotate gesture::RIGHT");
						if(guiHandler.rotate("right")){
							currentState = StatesHandler.sStateInit;
							timeLastDetectedGest = System.currentTimeMillis();
						}
					}
					return;
				}
			}
		}else if(currentState == StatesHandler.sStateZoom){
			//Zoom in initial gesture has been detected, look for zoom ending. 
			//Or keep zooming until something happens
			double detectedDistance = Gestures.detectZoomGesture(lDefects, centroid, true);
			if(detectedDistance != -1){
				double zoomEndDistance = detectedDistance;
				if(Math.abs(zoomEndDistance - zoomInitDistance) > screenWidth*0.05 ){ //more than 05% of screen
					if(zoomEndDistance > zoomInitDistance){
						//Zoom-IN gesture
						if(guiHandler.zoom("in")){
							Log.i("ImageInteraction", "Zoom gesture::IN");
							currentState = StatesHandler.sStateInit;
							timeLastDetectedGest = System.currentTimeMillis();
						}
					}else if(zoomEndDistance < zoomInitDistance){
						//Zoom-OUT gesture
						if(guiHandler.zoom("out")){
							currentState = StatesHandler.sStateInit;
							timeLastDetectedGest = System.currentTimeMillis();
							Log.i("ImageInteraction", "Zoom gesture::OUT");
						}
					}
					return;
				}

			}
		}else if(currentState == StatesHandler.sStatePointSelect){
			Point detectedPoint = Gestures.detectPointSelectGesture(lDefects, centroid, true);
			Core.circle(mRgb, getLastPointedLocation(), 5, Tools.magenta, -1);
			if(detectedPoint != null){
				addPointedLocation(detectedPoint);
				
				if(guiHandler.onClick(getLastPointedLocation()) == true){
					changeOfState = true;
				}
				currentState = StatesHandler.sStateInit;
				timeLastDetectedGest = System.currentTimeMillis();
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
