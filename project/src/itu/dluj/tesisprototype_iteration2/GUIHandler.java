package itu.dluj.tesisprototype_iteration2;

import itu.dluj.tesisprototype_iteration2.gesturerecognition.Tools;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;

public class GUIHandler {

	private int screenWidth;
	private int screenHeight;
	public Point[] backButtonCoords;
	
	
	public GUIHandler(int width, int height){
		screenWidth = width;
		screenHeight = height;
		
		backButtonCoords = new Point[2];
		backButtonCoords[0] = new Point(screenWidth*0.05, screenHeight*0.05);
		backButtonCoords[1] = new Point(screenWidth*0.35, screenHeight*0.20);
	}
	
	public Mat drawBackButton(Mat mRgb){
		Core.rectangle(mRgb, backButtonCoords[0], backButtonCoords[1], Tools.gray, -1);
		return mRgb;
	}
	
	
}
