package itu.dluj.tesisprototype_iteration2;

import itu.dluj.tesisprototype_iteration2.gesturerecognition.Gestures;
import itu.dluj.tesisprototype_iteration2.gesturerecognition.ImageInteractionGestures;
import itu.dluj.tesisprototype_iteration2.gesturerecognition.PatientSelectionGestures;
import itu.dluj.tesisprototype_iteration2.gesturerecognition.RecordViewingGestures;
import itu.dluj.tesisprototype_iteration2.gesturerecognition.Tools;

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

import android.app.Activity;
import android.util.Log;
import android.widget.Toast;

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
	private String patientSelectionState = "PatientSelectionState";
	private RecordViewingGestures recViwRecognition;
	private String recordViewingState = "RecordViewingState";
	private ImageInteractionGestures imgIntRecognition;
	private String imageInteractionState = "ImageInteractionState";
	private GUIHandler guiHandler;

	private long timeLastDetectedGesture;
	private long timeLastDetectedContour;
	
	public static final String sStateZero = "Zipou";
	public static final String sStateInit = "Init";
	public static final String sStateZoom = "Zoom";
	public static final String sStateSwipe = "Swipe";
	public static final String sStateRotate = "Rotate";
	public static String sStatePointSelect = "PointSelect";
	public static final String sStateEnd ="End";
	
	private String currentState;

	//values for magenta globe 1
	private final Scalar minRange = new Scalar(120, 0, 0);
	private final Scalar maxRange = new Scalar(178, 255, 255);
	private Toast tToastMsg;

	//values for magenta globe 2
//	private final Scalar minRange = new Scalar(110, 50, 50);
//	private final Scalar maxRange = new Scalar(174, 230, 200);
	//values for blue globe
//	private final Scalar minRange = new Scalar(0, 0, 0);
//	private final Scalar maxRange = new Scalar(30, 255, 255);

	private Activity mainActivity;
	
//	private Size blurSize;
	private Mat kernelErode;
	private Mat kernelDilate;

	private Mat mRgb;
	private Mat mHsv;
	private Mat mBin;

	public StatesHandler(int width, int height, Activity activity){

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
		currentState = sStateZero;

//		Log.i("StatesHandler", " blur::"+blur_size+" kernel::"+kernelSizeE);

		overallState = new HashMap<String, Boolean>();	
		setStateTrue(patientSelectionState);
//		setStateTrue(recordViewingState);
//		setStateTrue(imageInteractionState);
		
		mainActivity = activity;
		guiHandler = new GUIHandler(screenWidth, screenHeight, activity.getApplicationContext());

		patSelRecognition = new PatientSelectionGestures(screenWidth, screenHeight, activity, guiHandler);
		recViwRecognition = new RecordViewingGestures(screenWidth, screenHeight, activity, guiHandler);
		imgIntRecognition = new ImageInteractionGestures(screenWidth, screenHeight, activity, guiHandler);
		
		
	}

	public Mat handleFrame(Mat mInputFrame){
		//    	Log.i("check", "handleFrame - init");
		Imgproc.cvtColor(mInputFrame, mRgb, Imgproc.COLOR_RGBA2RGB);
		long now = System.currentTimeMillis();
		if(overallState.get(patientSelectionState) == true){
			timeLastDetectedGesture = patSelRecognition.timeLastDetectedGest;
		}else if(overallState.get(recordViewingState) == true){
			timeLastDetectedGesture = recViwRecognition.timeLastDetectedGest;
		}else if(overallState.get(imageInteractionState) == true){
			timeLastDetectedGesture = imgIntRecognition.timeLastDetectedGest;
		}
		//if 5 seconds passed with no change go back to "zipou"
		if(currentState != sStateZero){
			if(((now - timeLastDetectedContour) >= 15000)){
				//no gestures detected for 15.0 seconds... go back to PatSel
				//			int x = (int)Math.round(screenWidth*0.05);
				//			int y = (int)Math.round(screenHeight*0.35);
				//			mRgb = Tools.writeToImage(mRgb, x, y, "back to "+sStateZero);
				postToast("Back to "+ sStatePointSelect);
				patSelRecognition.currentState = sStateZero;
				recViwRecognition.currentState = sStateZero;
				imgIntRecognition.currentState = sStateZero;
				currentState = sStateZero;
				setStateTrue(patientSelectionState);
				timeLastDetectedContour = System.currentTimeMillis();
				mRgb = drawGUI(mRgb);
				return mRgb;
			}else if( ((now - timeLastDetectedGesture)/1000 < Gestures.secondsToWait)){
				//if 2 seconds have not passed since gesture detection, return				
				mRgb = guiHandler.writeInfoToImage(mRgb, "Wait " + (2 - (now - timeLastDetectedGesture)/1000)+"s" );	
				mRgb = guiHandler.writeWarningToImage(mRgb, currentState);
				mRgb = drawGUI(mRgb);
				return mRgb;
			}
		}
		
		
		List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
		Mat hierarchy = new Mat();
//		Imgproc.medianBlur(mRgb, mRgb, 3);
//		Imgproc.GaussianBlur(mRgb, mRgb, blurSize, 2.0);
		Imgproc.cvtColor(mRgb, mHsv, Imgproc.COLOR_RGB2HSV);

		mRgb = drawGUI(mRgb);
		
		//		Imgproc.medianBlur(mHsv, mHsv, 3);
		Core.inRange(mHsv, minRange, maxRange, mBin);
//		Imgproc.threshold(mHsv, mBin, 1, 255, Imgproc.THRESH_BINARY);
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
			Log.i("check", "procressImage init");
			mHandContour = contours.get(indexBiggestArea);
//			Imgproc.drawContours(mRgb, contours, -1, Tools.red, 2);

			double contourArea = Imgproc.contourArea(mHandContour);
			//		Log.i("ImageInteraction", "contour area:: " + contourArea + " screen area min::"+ screenArea*0.15
			//				+ "screen area max::"+ screenArea * 0.45);
			if(contourArea < screenArea * pctMinAreaGesture){
				//no good contours found
				mRgb = guiHandler.writeWarningToImage(mRgb, currentState + " - Hand too far!");
			}else if(contourArea > screenArea * pctMaxAreaGesture){
				//no good contours found
				mRgb = guiHandler.writeWarningToImage(mRgb, currentState + " - Hand too close!");
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
				
				lHandContour.add(mHandContour);
				handContourCentroid = Tools.getCentroid(mHandContour);
				//draw circle in centroid of contour
				Core.circle(mRgb, handContourCentroid, 5, Tools.red, -1);
				//            Log.i("contours-info", "contours="+contours.size()+" size="+biggestArea);
				/* 
				 * handContour == biggestContour 
				 * but Imgproc.drawContours method takes only List<MapOfPoint> as parameter
				 */
				Imgproc.drawContours(mRgb, lHandContour, -1, Tools.green, 1);
				Imgproc.convexHull(mHandContour, convexHull, true);
				Imgproc.convexityDefects(mHandContour, convexHull, convexityDefects);
				lFinalDefects = Gestures.filterDefects(convexityDefects, mHandContour);
				mRgb = Tools.drawDefects(mRgb, lFinalDefects, handContourCentroid);

				//        	Log.i("check", "handleFrame - biggestArea found");
				if(overallState.get(patientSelectionState) == true){
					//	        	Log.i("check", "NOPE 1");
					mRgb = patSelRecognition.processImage(mRgb, handContourCentroid, lFinalDefects);
					currentState = patSelRecognition.getState();
					if(patSelRecognition.changeOfState == true){
						recViwRecognition.timeLastDetectedGest = System.currentTimeMillis();
						recViwRecognition.currentState = sStateInit;
						setStateTrue(recordViewingState);
					}
				}else if(overallState.get(recordViewingState) == true){
					//	        	Log.i("check", "NOPE 2");
					mRgb = recViwRecognition.processImage(mRgb, handContourCentroid, lFinalDefects);
					currentState = recViwRecognition.getState();
					if(recViwRecognition.nextState == true){
						imgIntRecognition.timeLastDetectedGest = System.currentTimeMillis();
						imgIntRecognition.currentState = sStateInit;
						setStateTrue(imageInteractionState);
					}else if(recViwRecognition.previousState == true){
						patSelRecognition.timeLastDetectedGest = System.currentTimeMillis();
						patSelRecognition.currentState = sStateInit;
						setStateTrue(patientSelectionState);
					}
				}else if(overallState.get(imageInteractionState) == true){
					//	        	Log.i("check", "handleFrame - calling imgIntRecon");
					mRgb = imgIntRecognition.processImage(mRgb, handContourCentroid, lFinalDefects);
					currentState = imgIntRecognition.getState();
					if(imgIntRecognition.changeOfState == true){
						recViwRecognition.timeLastDetectedGest = System.currentTimeMillis();
						recViwRecognition.currentState = sStateInit;
						setStateTrue(recordViewingState);
					}
				}
				mRgb = guiHandler.writeWarningToImage(mRgb, currentState);
			}
		}else{
			mRgb = guiHandler.writeWarningToImage(mRgb, currentState + " - No contour found");
			//        	Log.i("check", "writting to image - nothing found");
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

	private Mat drawGUI(Mat mRgb) {
		//draw things after converting image to hsv so they don't interfere with gestures
		if(overallState.get(patientSelectionState) == true){
			mRgb = guiHandler.drawPatientsToSelect(mRgb);
			
		}else if(overallState.get(recordViewingState) == true){
			mRgb = guiHandler.drawBackButton(mRgb, false);
			mRgb = guiHandler.drawImagesButton(mRgb);
			mRgb = guiHandler.drawPatientInfo(mRgb);
			
		}else if(overallState.get(imageInteractionState) == true){
			mRgb = guiHandler.drawBackButton(mRgb, true);
			mRgb = guiHandler.drawFullScreenImage(mRgb);
		}

		return mRgb;
	}

	private void setStateTrue(String state){
		if(state.equals(patientSelectionState)){
			overallState.put(patientSelectionState, true);
			overallState.put(recordViewingState, false);
			overallState.put(imageInteractionState, false);
		}else if(state.equals(recordViewingState)){
			overallState.put(patientSelectionState, false);
			overallState.put(recordViewingState, true);
			overallState.put(imageInteractionState, false);
		}else if(state.equals(imageInteractionState)){
			overallState.put(patientSelectionState, false);
			overallState.put(recordViewingState, false);
			overallState.put(imageInteractionState, true);
		}
	}
	
	
	/*
	 * Util
	 */
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
				tToastMsg = Toast.makeText(mainActivity.getApplicationContext(), string, Toast.LENGTH_LONG);
				tToastMsg.show();
				//		            Toast.makeText(appContext, string, length).show();
			}
		});
	}

	public Point getHandCentroid(){
		if(handContourCentroid != null){
//			Log.i("StatesHandler", "centroidScaled::"+handContourCentroid.toString());
			return new Point(handContourCentroid.x*2, handContourCentroid.y*2);
		}
		return null;
	}
	
}