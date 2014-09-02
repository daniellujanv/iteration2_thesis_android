package itu.dluj.tesisprototype.i4;

import itu.dluj.tesisprototype.i4.gesturerecognition.Gestures;
import itu.dluj.tesisprototype.i4.gesturerecognition.ImageInteractionGestures;
import itu.dluj.tesisprototype.i4.gesturerecognition.PatientSelectionGestures;
import itu.dluj.tesisprototype.i4.gesturerecognition.RecordViewingGestures;
import itu.dluj.tesisprototype.i4.gesturerecognition.Tools;

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

import android.content.Context;
import android.util.Log;

public class StatesHandler {

	private HashMap<String, Boolean> overallState;
	private int screenWidth;
	private int screenHeight;
	private int screenArea;
	private double pctMinAreaGesture = 0.05;
	private double pctMaxAreaGesture = 0.28;

	private MatOfPoint mHandContour;
	private List<MatOfPoint> lHandContour;
	private Point handContourCentroid;
	private MatOfInt convexHull;
	private MatOfInt4 convexityDefects;
	private List<Point[]> lFinalDefects; 


	private PatientSelectionGestures patSelRecognition;
	public static String sPatientSelectionState = "PatientSelectionState";
	private RecordViewingGestures recViwRecognition;
	public static String sRecordViewingState = "RecordViewingState";
	private ImageInteractionGestures imgIntRecognition;
	public static String sImageInteractionState = "ImageInteractionState";
	private GUIHandler guiHandler;

	private long timeLastDetectedGesture;
	private long timeLastDetectedContour;

	public static final String sStateZero = "Zipou";
	public static final String sStateInit = "Init";
	public static final String sStateZoom = "Zoom";
	public static final String sStateSwipe = "Swipe";
	public static final String sStateRotate = "Rotate";
	public static final String sStatePointSelect = "PointSelect";
	public static final String sStateEnd ="End";
	public static String currentOverallState;

	private String currentState = sStateInit;

	//values for magenta globe 1
	private final Scalar minRange = new Scalar(120, 0, 0);
	private final Scalar maxRange = new Scalar(178, 255, 255);

	//values for magenta globe 2
	//	private final Scalar minRange = new Scalar(110, 50, 50);
	//	private final Scalar maxRange = new Scalar(174, 230, 200);
	//values for blue globe
	//	private final Scalar minRange = new Scalar(0, 0, 0);
	//	private final Scalar maxRange = new Scalar(30, 255, 255);

	//	private Size blurSize;
	private Mat kernelErode;
	private Mat kernelDilate;

	private Mat mRgb;
	private Mat mHsv;
	private Mat mBin;

	public StatesHandler(int width, int height, Context appContext){

		/* 3 classes or overall states for the actions the user can perform, 
		 * therefore 3 classes for recognition of gestures
		 * - PoiSelState -> point and select actions -> point and select
		 * - NavState -> Navigation actions -> swipe, point and select
		 * - IntState -> Interaction actions -> rotate, zoom-in ( point for navigation inside zoom ), zoom-out, end 
		 */
		screenWidth = width/2;
		screenHeight = height/2;
		screenArea = screenWidth*screenHeight;

		timeLastDetectedContour = System.currentTimeMillis();

		mRgb = new Mat();
		mHsv = new Mat();
		mBin = new Mat();


		convexHull = new MatOfInt();
		mHandContour = new MatOfPoint();
		convexityDefects = new MatOfInt4();
		lHandContour = new ArrayList<MatOfPoint>();
		lFinalDefects = new ArrayList<Point[]>();

		int kernelSizeE = 15;
		int kernelSizeD = 12;
		//		int kernelSize =(int)Math.round(screenWidth*0.035);
		//		kernelSize = (kernelSize % 2 == 0)? kernelSize + 1: kernelSize;
		kernelErode = Mat.ones(kernelSizeE, kernelSizeE, CvType.CV_8U);
		kernelDilate = Mat.ones(kernelSizeD, kernelSizeD, CvType.CV_8U);
		//		int blur_size = 5;
		//		int blur_size = (int)Math.round(screenWidth*0.008);
		//		Log.i("StatesHandler", "kernelSize :: "+ kernelSize+ " blurSize::"+ blur_size);
		//		blur_size = (blur_size % 2 == 0)? blur_size + 1: blur_size;
		//		blurSize = new Size(blur_size, blur_size);
		currentState = sStateInit;
		currentOverallState = sPatientSelectionState;

		//		Log.i("StatesHandler", " blur::"+blur_size+" kernel::"+kernelSizeE);

		overallState = new HashMap<String, Boolean>();	
		setStateTrue(sPatientSelectionState);
		//		setStateTrue(sRecordViewingState);
		//		setStateTrue(sImageInteractionState);

		guiHandler = new GUIHandler(screenWidth, screenHeight, appContext);

		patSelRecognition = new PatientSelectionGestures(screenWidth, screenHeight, guiHandler);
		recViwRecognition = new RecordViewingGestures(screenWidth, screenHeight, guiHandler);
		imgIntRecognition = new ImageInteractionGestures(screenWidth, screenHeight, guiHandler);


	}

	/*
	 * handles frame
	 */
	public Mat handleFrame(Mat mInputFrame){
		//    	Log.i("check", "handleFrame - init");
		Imgproc.cvtColor(mInputFrame, mRgb, Imgproc.COLOR_RGBA2RGB);
		long now = System.currentTimeMillis();
		if(overallState.get(sPatientSelectionState) == true){
			timeLastDetectedGesture = patSelRecognition.timeLastDetectedGest;
		}else if(overallState.get(sRecordViewingState) == true){
			timeLastDetectedGesture = recViwRecognition.timeLastDetectedGest;
		}else if(overallState.get(sImageInteractionState) == true){
			timeLastDetectedGesture = imgIntRecognition.timeLastDetectedGest;
		}
		//if 5 seconds passed with no change go back to "zipou"
		if(currentState != sStateZero){
			if(((now - timeLastDetectedContour) >= 40000)){
				//no gestures detected for 40.0 seconds... go back to zipou&patientSelection
				patSelRecognition.currentState = sStateZero;
				recViwRecognition.currentState = sStateZero;
				imgIntRecognition.currentState = sStateZero;
				currentState = sStateZero;
				setStateTrue(sPatientSelectionState);
				timeLastDetectedContour = System.currentTimeMillis();
				drawGUI(true);
				return mRgb;
			}
		}

		List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
		Mat hierarchy = new Mat();
		Imgproc.cvtColor(mRgb, mHsv, Imgproc.COLOR_RGB2HSV);

		//		mRgb = drawGUI();
		//		drawGUI();

		Core.inRange(mHsv, minRange, maxRange, mBin);
		Imgproc.erode(mBin, mBin, kernelErode);
		Imgproc.dilate(mBin, mBin, kernelDilate);

		Imgproc.findContours(mBin, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);
		double biggestArea = 0;
		int indexBiggestArea = -1;
		double tempArea = 0;
		for(int i=0; i< contours.size(); i++){
			tempArea = Imgproc.contourArea(contours.get(i));
			/******
			 * - the border of the frame is padded with 0's and 
			 * since the threshold is inverse it gets marked as a contour
			 * with an area equal to the screen area, therefore if tempArea == screenArea we don't count the contour
			 * - the contour has to cover over 30% of the screen area 
			 */
			if((tempArea >= biggestArea) && (tempArea != screenArea)){
				indexBiggestArea = i;
				biggestArea = tempArea;
			}
		}

		if(indexBiggestArea != -1){
			//			Log.i("check", "procressImage init");
			mHandContour = contours.get(indexBiggestArea);
			//			Imgproc.drawContours(mRgb, contours, -1, Tools.red, 2);

			double contourArea = Imgproc.contourArea(mHandContour);
			if(contourArea < screenArea * pctMinAreaGesture){
				//no good contours found
				drawGUI(false);
				mRgb = guiHandler.writeWarningToImage(mRgb, " Hand too far!");
			}else if(contourArea > screenArea * pctMaxAreaGesture){
				//no good contours found
				drawGUI(false);
				mRgb = guiHandler.writeWarningToImage(mRgb," Hand too close!");
			}else{
				timeLastDetectedContour = System.currentTimeMillis();
				//good contour found
				//approximate polygon to hand contour, makes the edges more stable
				MatOfPoint2f temp_contour = new MatOfPoint2f(mHandContour.toArray());
				double epsilon = Imgproc.arcLength(temp_contour, true)*0.0035;
				MatOfPoint2f result_temp_contour = new MatOfPoint2f();
				Imgproc.approxPolyDP(temp_contour, result_temp_contour, epsilon, true);
				mHandContour = new MatOfPoint(result_temp_contour.toArray());

				temp_contour.release();
				result_temp_contour.release();
				mHandContour = Gestures.shiftContour(mHandContour);
				lHandContour.add(mHandContour);
				handContourCentroid = Tools.getCentroid(mHandContour);
				//draw circle in centroid of contour
				Core.circle(mRgb, handContourCentroid, 5, Tools.red, -1);
				/************/
				if( ((now - timeLastDetectedGesture)/1000 < Gestures.secondsToWait)){
					//if 2 seconds have not passed since gesture detection, return	
					long sec = (long) (2 - (double)((now - timeLastDetectedGesture)/1000));
					drawGUIAid(sec);
					lHandContour.clear();
					//					mRgb = guiHandler.writeInfoToImage(mRgb, "Wait " + sec+"s" );	
					//					mRgb = guiHandler.writeWarningToImage(mRgb, currentState);
					return mRgb;
				}else{
					drawGUI(true);
				}
				/************/
				Imgproc.convexHull(mHandContour, convexHull, true);
				Imgproc.convexityDefects(mHandContour, convexHull, convexityDefects);
				lFinalDefects = Gestures.filterDefects(convexityDefects, mHandContour);
				/***********/
				//				mRgb = Tools.drawDefects(mRgb, lFinalDefects, handContourCentroid);
				/**********/


				if(overallState.get(sPatientSelectionState) == true){
					mRgb = patSelRecognition.processImage(mRgb, handContourCentroid, lFinalDefects);
					currentState = patSelRecognition.getState();
					if(patSelRecognition.changeOfState == true){
						recViwRecognition.timeLastDetectedGest = System.currentTimeMillis();
						recViwRecognition.currentState = sStateInit;
						setStateTrue(sRecordViewingState);
					}
				}else if(overallState.get(sRecordViewingState) == true){
					mRgb = recViwRecognition.processImage(mRgb, handContourCentroid, lFinalDefects);
					currentState = recViwRecognition.getState();
					if(recViwRecognition.nextState == true){
						imgIntRecognition.timeLastDetectedGest = System.currentTimeMillis();
						imgIntRecognition.currentState = sStateInit;
						setStateTrue(sImageInteractionState);
					}else if(recViwRecognition.previousState == true){
						patSelRecognition.timeLastDetectedGest = System.currentTimeMillis();
						patSelRecognition.currentState = sStateInit;
						setStateTrue(sPatientSelectionState);
					}
				}else if(overallState.get(sImageInteractionState) == true){
					mRgb = imgIntRecognition.processImage(mRgb, handContourCentroid, lFinalDefects);
					currentState = imgIntRecognition.getState();
					if(imgIntRecognition.changeOfState == true){
						recViwRecognition.timeLastDetectedGest = System.currentTimeMillis();
						recViwRecognition.currentState = sStateInit;
						setStateTrue(sRecordViewingState);
					}
				}
				//				mRgb = guiHandler.writeWarningToImage(mRgb, currentState);
			}
		}else{
			/*
			 * GIVE FEEDBACK THAT CONTOUR IS NOT FOUND
			 */
			drawGUI(false);
			mRgb = guiHandler.writeWarningToImage(mRgb, " No hand found!");
		}


		lHandContour.clear();
		lFinalDefects.clear();
		mHandContour.release();
		mHsv.release();
		mBin.release();
		hierarchy.release();
		contours.clear();

		//    	Log.i("check", "handleFrame - end");
		//		return mBin;
		//		return mHsv;
		return mRgb;
	}

	/*
	 * 
	 */
	private void drawGUI(boolean goodContour) {
		//draw things after converting image to hsv so they don't interfere with gestures
		mRgb = guiHandler.drawGui(goodContour, lHandContour, currentOverallState, mRgb.size(), mRgb.type(), currentState, false, -1);
		//		return mRgb;
	}

	/*
	 * draws GUI plus Aid feedback for the gestures
	 */
	private void drawGUIAid(long second) {
		//draw things after converting image to hsv so they don't interfere with gestures
		mRgb = guiHandler.drawGui(true, lHandContour, currentOverallState, mRgb.size(), mRgb.type(), currentState, true, second);
	}

	/*
	 * 
	 */
	private void setStateTrue(String state){
		if(state.equals(sPatientSelectionState)){
			//patientSelectionState = true
			currentOverallState = sPatientSelectionState;
			overallState.put(sPatientSelectionState, true);
			overallState.put(sRecordViewingState, false);
			overallState.put(sImageInteractionState, false);
		}else if(state.equals(sRecordViewingState)){
			//recordViewingState = true
			currentOverallState = sRecordViewingState;
			overallState.put(sPatientSelectionState, false);
			overallState.put(sRecordViewingState, true);
			overallState.put(sImageInteractionState, false);
		}else if(state.equals(sImageInteractionState)){
			//imageInteractionstate = true
			currentOverallState = sImageInteractionState;
			overallState.put(sPatientSelectionState, false);
			overallState.put(sRecordViewingState, false);
			overallState.put(sImageInteractionState, true);
		}
	}

	/*
	 * 
	 */
	public Point getHandCentroid(){
		if(handContourCentroid != null){
			//			Log.i("StatesHandler", "centroidScaled::"+handContourCentroid.toString());
			return new Point(handContourCentroid.x*2, handContourCentroid.y*2);
		}
		return null;
	}

}