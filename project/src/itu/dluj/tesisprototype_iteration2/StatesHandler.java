package itu.dluj.tesisprototype_iteration2;

import itu.dluj.tesisprototype_iteration2.gesturerecognition.ImageInteractionGestures;
import itu.dluj.tesisprototype_iteration2.gesturerecognition.PatientSelectionGestures;
import itu.dluj.tesisprototype_iteration2.gesturerecognition.RecordViewingGestures;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import android.app.Activity;
import android.content.Context;

public class StatesHandler {

	private HashMap<String, Boolean> overallState;
	private int screenWidth;
	private int screenHeight;
	private int screenArea;
	private Context appContext;

	private PatientSelectionGestures patSelRecognition;
	private RecordViewingGestures recViwRecognition;
	private ImageInteractionGestures imgIntRecognition;
	
	private String currentState;

	private Scalar minRange;
	private Scalar maxRange;
	private Mat kernel;

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
		screenArea = width*height;
		
		appContext = activity.getApplicationContext();
		
		mRgb = new Mat();
		mHsv = new Mat();
		mBin = new Mat();

		minRange = new Scalar(0, 0, 0);
		maxRange = new Scalar(134, 48, 255);
		kernel = Mat.ones(15, 15, CvType.CV_8U);

		currentState = "Nothing done";
		
		overallState = new HashMap<String, Boolean>();
		overallState.put("PatientSelectionState", false);
		overallState.put("RecordViewingState", false);
		overallState.put("ImageInteractionState", true);	

		patSelRecognition = new PatientSelectionGestures(screenWidth, screenHeight, appContext);
		recViwRecognition = new RecordViewingGestures(screenWidth, screenHeight, activity);
		imgIntRecognition = new ImageInteractionGestures(screenWidth, screenHeight, activity);
	}

	public Mat handleFrame(Mat mInputFrame){
//    	Log.i("check", "handleFrame - init");
		List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
		Mat hierarchy = new Mat();

		Imgproc.cvtColor(mInputFrame, mRgb, Imgproc.COLOR_RGBA2RGB);
		Imgproc.GaussianBlur(mRgb, mRgb, new Size(9,9), 2.0);
		//output == input in case something does not go according to planned 
		Imgproc.cvtColor(mRgb, mHsv, Imgproc.COLOR_RGB2HSV);
		Core.inRange(mHsv, minRange, maxRange, mHsv);
		Imgproc.threshold(mHsv, mBin, 1, 255, Imgproc.THRESH_BINARY_INV);
		Imgproc.erode(mBin, mBin, kernel);
		Imgproc.dilate(mBin, mBin, kernel);

		Imgproc.findContours(mBin, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);
		double biggestArea = 0;
		int indexBiggestArea = -1;
		double tempArea = 0;
		for(int i=0; i< contours.size(); i++){
			tempArea = Imgproc.contourArea(contours.get(i));
			/* 
			 * - the border of the frame is padded with 0's and 
			 * since the threshold is inverse it gets marked as a contour
			 * with an area equal to the screen area, therefore if tempArea == screenArea we don't count the contour
			 * - the contour has to cover over 35% of the screen area 
			 */
			if((tempArea >= biggestArea) && (tempArea != screenArea)){
				indexBiggestArea = i;
				biggestArea = tempArea;
			}
		}

		if(indexBiggestArea != -1){
//        	Log.i("check", "handleFrame - biggestArea found");
			if(overallState.get("PatientSelectionState") == true){
//	        	Log.i("check", "NOPE 1");
				mRgb = patSelRecognition.processImage(mRgb, contours, indexBiggestArea);
			}else if(overallState.get("RecordViewingState") == true){
//	        	Log.i("check", "NOPE 2");
				mRgb = recViwRecognition.processImage(mRgb, contours.get(indexBiggestArea));
				currentState = "RecViw-"+recViwRecognition.getState();
			}else if(overallState.get("ImageInteractionState") == true){
//	        	Log.i("check", "handleFrame - calling imgIntRecon");
				mRgb = imgIntRecognition.processImage(mRgb, contours.get(indexBiggestArea));
				currentState = "ImgInt-"+imgIntRecognition.getState();
			}
			int x =((int)Math.round(screenWidth*0.05));
			int y = screenHeight - ((int)Math.round(screenHeight*0.10));
			writeToImage(x, y, currentState);
		}else{
			int x = (int)Math.round(screenWidth*0.05);
			int y = (int)Math.round(screenHeight*0.10);
        	writeToImage( x, y, "No contour found");
//        	Log.i("check", "writting to image - nothing found");
		}

		mHsv.release();
		mBin.release();
		hierarchy.release();
		contours.clear();

//    	Log.i("check", "handleFrame - end");
		return mRgb;
	}

	private void writeToImage(int x, int y, String string) {
		Core.putText(mRgb, string, new Point(x, y),Core.FONT_HERSHEY_SIMPLEX, 1.2, new Scalar(0,0,0), 20);
		Core.putText(mRgb, string, new Point(x, y),Core.FONT_HERSHEY_SIMPLEX, 1.2, new Scalar(255,255,255), 10);
	}
}
