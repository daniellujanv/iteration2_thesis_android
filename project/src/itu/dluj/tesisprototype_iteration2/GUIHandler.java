package itu.dluj.tesisprototype_iteration2;

import itu.dluj.tesisprototype_iteration2.gesturerecognition.Tools;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;

import android.util.Log;

public class GUIHandler {

	private int screenWidth;
	private int screenHeight;
	public Point[] backButtonCoords;
	public Point[] imagesButtonCoord;
	public Point[] patientOneCoords;
	public Point[] patientTwoCoords;
	public Point[] patientInfoCoords;
	public String[] patientInfoText;
	
	
	public GUIHandler(int width, int height){
		screenWidth = width;
		screenHeight = height;
		//Coords [0] == upper left inner rectangle
		//Coords [1] == lower right inner rectangle
		//Coords [2] == upper left outer rectangle
		//Coords [3] == lower right outer rectangle
		//Coords [4] == text coords

		//BackButton coords
		backButtonCoords = new Point[5];
		backButtonCoords[0] = new Point(screenWidth*0.05, screenHeight*0.05);
		backButtonCoords[1] = new Point(screenWidth*0.35, screenHeight*0.20);
		backButtonCoords[2] = new Point(screenWidth*0.04, screenHeight*0.04);
		backButtonCoords[3] = new Point(screenWidth*0.35, screenHeight*0.20);
		backButtonCoords[4] = new Point(screenWidth*0.09, screenHeight*0.15);
		//ImagesButton coords
		imagesButtonCoord = new Point[5];
		imagesButtonCoord[0] = new Point(screenWidth*0.65, screenHeight*0.05);
		imagesButtonCoord[1] = new Point(screenWidth*0.95, screenHeight*0.20);
		imagesButtonCoord[2] = new Point(screenWidth*0.64, screenHeight*0.04);
		imagesButtonCoord[3] = new Point(screenWidth*0.95, screenHeight*0.20);
		imagesButtonCoord[4] = new Point(screenWidth*0.69, screenHeight*0.15);
		//DoctorOne coords
		patientOneCoords = new Point[5];
		patientOneCoords[0] = new Point(screenWidth*0.05, screenHeight*0.30);
		patientOneCoords[1] = new Point(screenWidth*0.48, screenHeight*0.90);
		patientOneCoords[2] = new Point(screenWidth*0.04, screenHeight*0.29);
		patientOneCoords[3] = new Point(screenWidth*0.48, screenHeight*0.90);
		patientOneCoords[4] = new Point(screenWidth*0.09, screenHeight*0.40);
		//DoctorTwo Coords
		patientTwoCoords = new Point[5];
		patientTwoCoords[0] = new Point(screenWidth*0.52, screenHeight*0.30);
		patientTwoCoords[1] = new Point(screenWidth*0.95, screenHeight*0.90);
		patientTwoCoords[2] = new Point(screenWidth*0.51, screenHeight*0.29);
		patientTwoCoords[3] = new Point(screenWidth*0.95, screenHeight*0.90);
		patientTwoCoords[4] = new Point(screenWidth*0.56, screenHeight*0.40);
		//PatientInfo Coords
		patientInfoCoords = new Point[5];
		patientInfoCoords[0] = new Point(screenWidth*0.05, screenHeight*0.30);
		patientInfoCoords[1] = new Point(screenWidth*0.95, screenHeight*0.90);
		patientInfoCoords[2] = new Point(screenWidth*0.04, screenHeight*0.29);
		patientInfoCoords[3] = new Point(screenWidth*0.95, screenHeight*0.90);
		patientInfoCoords[4] = new Point(screenWidth*0.09, screenHeight*0.40);
		//PatientInfo Text
		patientInfoText = new String[5];
		patientInfoText[0] = "Lorem ipsum dolor sit amet,";
	    patientInfoText[1] = "consectetur adipisicing elit,";
	    patientInfoText[2] = "sed do eiusmod tempor incididunt";
	    patientInfoText[3] = "ut labore et dolore magna aliqua.";
	    patientInfoText[4] = "Ut enim ad minim veniam...";
	}
	
	/******************************** Drawing methods ****************************************/
	
	public Mat drawBackButton(Mat mRgb){
		Mat rec = mRgb.clone();
		Core.rectangle(rec, backButtonCoords[2], backButtonCoords[3], Tools.green, -1);
		Core.rectangle(rec, backButtonCoords[0], backButtonCoords[1], Tools.blue, -1);
		rec = writeToImage(rec, backButtonCoords[4], "Back");
		Mat output = new Mat();
		Core.addWeighted(mRgb, 0.5, rec, 0.5, 0, output);

		return output;
	}
	
	public Mat drawImagesButton(Mat mRgb){
		Mat rec = mRgb.clone();
		Core.rectangle(rec, imagesButtonCoord[2], imagesButtonCoord[3], Tools.green, -1);
		Core.rectangle(rec, imagesButtonCoord[0], imagesButtonCoord[1], Tools.blue, -1);
		rec = writeToImage(rec, imagesButtonCoord[4], "Images");
		Mat output = new Mat();
		Core.addWeighted(mRgb, 0.5, rec, 0.5, 0, output);

		return output;
	}
	
	public Mat drawPatientsToSelect(Mat mRgb){
		Mat rec = mRgb.clone();
		//Doc One
		Core.rectangle(rec, patientOneCoords[2], patientOneCoords[3], Tools.green, -1);
		Core.rectangle(rec, patientOneCoords[0], patientOneCoords[1], Tools.blue, -1);
		rec = writeToImage(rec, patientOneCoords[4], "Doctor One");
		//Doc Two
		Core.rectangle(rec, patientTwoCoords[2], patientTwoCoords[3], Tools.green, -1);
		Core.rectangle(rec, patientTwoCoords[0], patientTwoCoords[1], Tools.blue, -1);
		rec = writeToImage(rec, patientTwoCoords[4], "Doctor Two");

		Mat output = new Mat();
		Core.addWeighted(mRgb, 0.5, rec, 0.5, 0, output);
		
		return output;
	}
	
	public Mat drawPatientsInfo(Mat mRgb){
		Mat rec = mRgb.clone();
		//Doc One
		Core.rectangle(rec, patientInfoCoords[2], patientInfoCoords[3], Tools.green, -1);
		Core.rectangle(rec, patientInfoCoords[0], patientInfoCoords[1], Tools.blue, -1);
		rec = writeToImage(rec, patientInfoCoords[4], "Patient Info");
		Point temp = patientInfoCoords[4].clone();
		for(int i = 0; i<patientInfoText.length; i++){
			temp.y = temp.y + screenHeight*0.08;
//			Log.i("GUIHandler","Point "+i+"::"+temp.toString());
			rec = writeToImage(rec, temp, patientInfoText[i], 0.5);
		}
		
		Mat output = new Mat();
		Core.addWeighted(mRgb, 0.5, rec, 0.5, 0, output);
		
		return output;
	}
	
	/******************************* Utility methods ******************************************/
	/*
	 * Utility method - writes to GUI image
	 */
	public static Mat writeToImage(Mat mRgb, int x, int y, final String string) {
		Core.putText(mRgb, string, new Point(x, y), Core.FONT_HERSHEY_COMPLEX_SMALL, 1.0, new Scalar(200,200,200), 2);
		return mRgb;
	}

	/*
	 * Utility method - writes to GUI image
	 */
	public static Mat writeToImage(Mat mRgb, Point point, final String string) {
		Core.putText(mRgb, string, point, Core.FONT_HERSHEY_COMPLEX_SMALL, 1.0, new Scalar(200,200,200), 2);
		return mRgb;
	}
	
	/*
	 * Utility method - writes to GUI image
	 */
	public static Mat writeToImage(Mat mRgb, Point point, final String string, double scale) {
		Core.putText(mRgb, string, point, Core.FONT_HERSHEY_SIMPLEX, scale, new Scalar(200,200,200), 2);
		return mRgb;
	}
}
