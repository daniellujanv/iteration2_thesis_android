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

public class RecordViewingGestures {

	public Mat mRgb;

	public List<Point[]> lConvexityDefects;

//	private int screenArea;
//	private int screenHeight;
	private int screenWidth;
	public long timeLastDetectedGest;
	private Point lastPointedLocation;
	private List<Point> lPointedLocations;
	private Point initSwipeLocation;

	public String currentState;
	public boolean nextState;
	public boolean previousState;

//	private Context appContext;
//	private Activity mainActivity;
//	private Toast tToastMsg;
	
	private GUIHandler guiHandler;


	public RecordViewingGestures(int width, int height, Activity activity, GUIHandler handler){
		currentState = StatesHandler.sStateZero;
		previousState = false;
		nextState = false;
		guiHandler = handler;

		screenWidth = width;
		mRgb = new Mat();

//		pCentroid = new Point();
		lConvexityDefects = new ArrayList<Point[]>();
		lastPointedLocation = new Point();
		lPointedLocations = new ArrayList<Point>();
	}

	public Mat processImage(Mat inputImage, Point centroid, List<Point[]> lDefects){
		mRgb = inputImage;
		lConvexityDefects = lDefects;
		
		previousState = false;
		nextState = false;
		
		detectGesture(centroid, lConvexityDefects);	

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
		 */
		
		/*
		 * Always detect end gesture
		 */
		if(Gestures.detectEndGesture(lDefects, centroid) == true ){
			if(currentState != StatesHandler.sStateZero){
//				postToast("End!");
				currentState = StatesHandler.sStateInit;				
				timeLastDetectedGest = System.currentTimeMillis() - 1500;	
			}
		}
		
		if(currentState == StatesHandler.sStateZero){
			//Init not detected no interaction has not started
			if(Gestures.detectInitGesture(lDefects, centroid) == true ){
//				postToast("Init!");
				//good contour found
				currentState = StatesHandler.sStateInit;
				//				drawDefects(convexityDefects, handContour);
				Log.i("ImageInteraction", "Gesture detected - INIT");
				timeLastDetectedGest = System.currentTimeMillis();
				return;
			}
		}else if(currentState == StatesHandler.sStateInit){
			//Interaction has started, nothing detected yet
			Point detectedPoint = Gestures.detectPointSelectGesture(lDefects, centroid, false);
			if( detectedPoint != null){
				addPointedLocation(detectedPoint);
				currentState = StatesHandler.sStatePointSelect;
				timeLastDetectedGest = System.currentTimeMillis() - 1000;
				return;
			}else{ 
				Point detectedPointSwipe = Gestures.detectSwipeGesture(lDefects, centroid, false); 
				if(detectedPointSwipe != null){
//					postToast("Swipe!");
					initSwipeLocation = detectedPointSwipe;
					currentState = StatesHandler.sStateSwipe;
					timeLastDetectedGest = System.currentTimeMillis() - 1000;
					return;
				}
			}


		}else if(currentState == StatesHandler.sStateSwipe){
			//Rotation initial gesture has been detected, look for rotation ending.
			//Or keep rotating until something happens
			Point detectedPoint = Gestures.detectSwipeGesture(lDefects, centroid, true);
			
			if( detectedPoint != null ){
				double traveledDistance = Tools.getDistanceBetweenPoints(initSwipeLocation, detectedPoint);
				if(traveledDistance > screenWidth*0.25){//more than 25% of the screen
					if(initSwipeLocation.x < detectedPoint.x){
						if(guiHandler.swipe("right") == true){
//							postToast("Swipe - Rigth");
							currentState = StatesHandler.sStateInit;
							timeLastDetectedGest = System.currentTimeMillis();
						}
						
					}else{
						if(guiHandler.swipe("left") == true){
							currentState = StatesHandler.sStateInit;
							timeLastDetectedGest = System.currentTimeMillis();
						}
					}
					return;
				}
			}

		}else if(currentState == StatesHandler.sStatePointSelect){
			//Zoom in initial gesture has been detected, look for zoom ending. 
			//Or keep zooming until something happens
			Core.circle(mRgb, getLastPointedLocation(), 5, Tools.magenta, -1);
			Point detectedPoint = Gestures.detectPointSelectGesture(lDefects, centroid, true);
			if(detectedPoint != null){
				addPointedLocation(detectedPoint);
				if(guiHandler.onClick(getLastPointedLocation()) == true){
					if(guiHandler.backBtnClicked == true){
						// if backBtn == false an image was clicked
						// click on imgBtn returns false
						previousState = true;
					}else if(guiHandler.bigImgShowing == true){
						nextState = true;
					}
				}
				//in this case it does not matter if we assure that click was true
				//because if the click is false the actions are handled by GUIHandler
				//thus next two lines can be outside the if(true)
				currentState = StatesHandler.sStateInit;
				timeLastDetectedGest = System.currentTimeMillis();
				return;
			}
			detectedPoint = Gestures.detectPointSelectGesture(lDefects, centroid, false); 
			if(detectedPoint != null){
				//			if(detectPointSelectGesture(convexityDefects, mHandContour, false) == true){
				addPointedLocation(detectedPoint);

				currentState = StatesHandler.sStatePointSelect;
				timeLastDetectedGest = System.currentTimeMillis() - 2000; // so it doesn't have to wait again 2 secs
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
