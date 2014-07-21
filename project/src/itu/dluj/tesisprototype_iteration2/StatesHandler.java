package itu.dluj.tesisprototype_iteration2;

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
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import android.app.Activity;
import android.util.Log;

public class StatesHandler {

	private HashMap<String, Boolean> overallState;
	private int screenWidth;
	private int screenHeight;
	private int screenArea;
	private Point infoScreen;
	private Point warningScreen;
	private double pctMinAreaGesture = 0.10;
	private double pctMaxAreaGesture = 0.30;
	
	private MatOfPoint mHandContour;
	private List<MatOfPoint> lHandContour;
	private Point handContourCentroid;
	private MatOfInt convexHull;
	private MatOfInt4 convexityDefects;


	private PatientSelectionGestures patSelRecognition;
	private RecordViewingGestures recViwRecognition;
	private ImageInteractionGestures imgIntRecognition;
	private GUIHandler guiHandler;

	private String currentState;

	//values for magenta globe 1
	private final Scalar minRange = new Scalar(120, 0, 0);
	private final Scalar maxRange = new Scalar(175, 255, 255);

	//values for magenta globe 2
//	private final Scalar minRange = new Scalar(110, 50, 50);
//	private final Scalar maxRange = new Scalar(174, 230, 200);
	//values for blue globe
//	private final Scalar minRange = new Scalar(0, 0, 0);
//	private final Scalar maxRange = new Scalar(30, 255, 255);

	
	private Size blurSize;
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

		mRgb = new Mat();
		mHsv = new Mat();
		mBin = new Mat();
		infoScreen = new Point(screenWidth*0.05, (screenHeight - screenHeight*0.10));
		warningScreen = new Point(screenWidth*0.40, screenHeight*0.15);
		
		
		convexHull = new MatOfInt();
		mHandContour = new MatOfPoint();
		convexityDefects = new MatOfInt4();
		lHandContour = new ArrayList<MatOfPoint>();

		int kernelSizeE = 15;
		int kernelSizeD = 12;
//		int kernelSize =(int)Math.round(screenWidth*0.035);
//		kernelSize = (kernelSize % 2 == 0)? kernelSize + 1: kernelSize;
		kernelErode = Mat.ones(kernelSizeE, kernelSizeE, CvType.CV_8U);
		kernelDilate = Mat.ones(kernelSizeD, kernelSizeD, CvType.CV_8U);
		int blur_size = 5;
//		int blur_size = (int)Math.round(screenWidth*0.008);
//		Log.i("StatesHandler", "kernelSize :: "+ kernelSize+ " blurSize::"+ blur_size);
//		blur_size = (blur_size % 2 == 0)? blur_size + 1: blur_size;
		blurSize = new Size(blur_size, blur_size);
		currentState = "Nothing done";

		Log.i("StatesHandler", " blur::"+blur_size+" kernel::"+kernelSizeE);
		
		overallState = new HashMap<String, Boolean>();
		overallState.put("PatientSelectionState", false);
		overallState.put("RecordViewingState", false);
		overallState.put("ImageInteractionState", true);	

		patSelRecognition = new PatientSelectionGestures(screenWidth, screenHeight, activity);
		recViwRecognition = new RecordViewingGestures(screenWidth, screenHeight, activity);
		imgIntRecognition = new ImageInteractionGestures(screenWidth, screenHeight, activity);
		guiHandler = new GUIHandler(screenWidth, screenHeight);
	}

	public Mat handleFrame(Mat mInputFrame){
		//    	Log.i("check", "handleFrame - init");
		List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
		Mat hierarchy = new Mat();
		Imgproc.cvtColor(mInputFrame, mRgb, Imgproc.COLOR_RGBA2RGB);
//		Imgproc.medianBlur(mRgb, mRgb, 3);
//		Imgproc.GaussianBlur(mRgb, mRgb, blurSize, 2.0);
		Imgproc.cvtColor(mRgb, mHsv, Imgproc.COLOR_RGB2HSV);

		//draw things after converting image to hsv so they don't interfere with gestures
		mRgb = guiHandler.drawBackButton(mRgb);
		mRgb = guiHandler.drawImagesButton(mRgb);
//		mRgb = guiHandler.drawPatientsToSelect(mRgb);
		mRgb = guiHandler.drawPatientsInfo(mRgb);
		
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
			Log.i("check", "imgIntGest - procressImage init");
			mHandContour = contours.get(indexBiggestArea);
//			Imgproc.drawContours(mRgb, contours, -1, Tools.red, 2);

			double contourArea = Imgproc.contourArea(mHandContour);
			//		Log.i("ImageInteraction", "contour area:: " + contourArea + " screen area min::"+ screenArea*0.15
			//				+ "screen area max::"+ screenArea * 0.45);
			if(contourArea < screenArea * pctMinAreaGesture){
				//no good contours found
				mRgb = Tools.writeToImage(mRgb, (int)warningScreen.x, (int)warningScreen.y, "Hand too far!");
			}else if(contourArea > screenArea * pctMaxAreaGesture){
				//no good contours found
				mRgb = Tools.writeToImage(mRgb, (int)warningScreen.x, (int)warningScreen.y, "Hand too close!");
			}else{
				//good contour found
				//approximate polygon to hand contour, makes the edges more stable
				MatOfPoint2f temp_contour = new MatOfPoint2f(mHandContour.toArray());
				double epsilon = Imgproc.arcLength(temp_contour, true)*0.0038;
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
				mRgb = Tools.drawDefects(mRgb, convexityDefects, mHandContour);

				//        	Log.i("check", "handleFrame - biggestArea found");
				if(overallState.get("PatientSelectionState") == true){
					//	        	Log.i("check", "NOPE 1");
					mRgb = patSelRecognition.processImage(mRgb, mHandContour, convexityDefects);
					currentState = "PatSel-"+patSelRecognition.getState();
				}else if(overallState.get("RecordViewingState") == true){
					//	        	Log.i("check", "NOPE 2");
					mRgb = recViwRecognition.processImage(mRgb, mHandContour, convexityDefects);
					currentState = "RecViw-"+recViwRecognition.getState();
				}else if(overallState.get("ImageInteractionState") == true){
					//	        	Log.i("check", "handleFrame - calling imgIntRecon");
					mRgb = imgIntRecognition.processImage(mRgb, mHandContour, convexityDefects);
					currentState = "ImgInt-"+imgIntRecognition.getState();
				}
				mRgb = Tools.writeToImage(mRgb, (int)infoScreen.x, (int)infoScreen.y, currentState);
			}
		}else{
			mRgb = Tools.writeToImage(mRgb, (int)warningScreen.x, (int)warningScreen.y, "No contour found");
			//        	Log.i("check", "writting to image - nothing found");
		}

		
		lHandContour.clear();
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
}