package itu.dluj.tesisprototype;

import itu.dluj.tesisprototype.gesturerecognition.Tools;
import itu.dluj.tesisprototype_iteration2.R;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

public class GUIHandler {

	private int screenWidth;
	private int screenHeight;

	private Point[] backButtonCoords;
	private Point[] imagesButtonCoord;
	private Point[] patientOneCoords;
	private Point[] patientTwoCoords;
	private Point[] patientInfoCoords;
	private Point[] patientImgsCoords;
	private Point[] fullScreenImgCoords;

	private String hoverNone = "none";
	private String currentHover = hoverNone;
	private String hoverPatient1 = "hoverPatient1";
	private String hoverPatient2 = "hoverPatient2";
	private String hoverbackbutton = "hoverBackButton";
	private String hoverImgsButton = "hoverImgsButton";
	private String hoverImg1 = "hoverImg1";
	private String hoverImg2 = "hoverImg2";
	private String hoverBigImage = "hoverBigImage";

	//	private Point pBigImgCenter;
	//	private Size sBigImageOriginalSize;
	private int zoomLevel;
	private int maxZoom = 2;
	private Point pFullScreenImgCenter;

	private String[] patientInfoText;
	private Point infoCoords;
	private Point warningCoords;

	private Rect[] patientImgsRoi;
	private Rect[] profilePicRoi;
	private Rect fullScreenImgRoi;

	private Rect aidRoi;
	private Mat mAidRotate;
	private Mat mAidSwipe;
	private Mat mAidZoom;
	
	public boolean imagesBtnClicked;
	public boolean backBtnClicked;
	public boolean bigImgShowing;
	private int iCurrentPatient;
	private String sCurrentPatient;
	private int iCurrentImg;
	private int numberImgs = 3;

	private Mat[] mPatientImages;
	private Mat[] mFullScreenImages;
	private Mat[] mProfilePics;

	private Resources resources;

	private Context appContext;


	public GUIHandler(int width, int height, Context context){
		screenWidth = width;
		screenHeight = height;
		imagesBtnClicked = false;
		backBtnClicked = false;

		zoomLevel = 0;
		pFullScreenImgCenter = new Point (width/2, height/2);
		//		pFullScreenImgCenter = new Point (0, 0);

		appContext = context;
		resources = appContext.getResources();

		//0 to 2
		iCurrentImg = 0;
		//0 or 1... -1 == null
		iCurrentPatient = -1;
		sCurrentPatient = "N/A";

		warningCoords = new Point(screenWidth*0.05, screenHeight*0.95);
		infoCoords = new Point(screenWidth*0.55, screenHeight*0.95);

		profilePicRoi = new Rect[2];// 0 == patient0, 1 == patient1
		mProfilePics = new Mat[2];

		//Coords [0] == upper left inner rectangle
		//Coords [1] == lower right inner rectangle
		//Coords [2] == upper left outer rectangle
		//Coords [3] == lower right outer rectangle
		//Coords [4] == text coords

		//BackButton coords
		backButtonCoords = new Point[10];
		backButtonCoords[0] = new Point(screenWidth*0.15, screenHeight*0.05);//normal coordinates
		backButtonCoords[1] = new Point(screenWidth*0.45, screenHeight*0.20);//n
		backButtonCoords[2] = new Point(screenWidth*0.14, screenHeight*0.04);//n
		backButtonCoords[3] = new Point(screenWidth*0.45, screenHeight*0.20);//n
		backButtonCoords[4] = new Point(screenWidth*0.19, screenHeight*0.15);//n
		backButtonCoords[5] = new Point(screenWidth*0.01, screenHeight*0.01);//fullscreenImg coordinates
		backButtonCoords[6] = new Point(screenWidth*0.20, screenHeight*0.20);//f
		backButtonCoords[7] = new Point(screenWidth*0.0, screenHeight*0.0);//f
		backButtonCoords[8] = new Point(screenWidth*0.20, screenHeight*0.20);//f
		backButtonCoords[9] = new Point(screenWidth*0.02, screenHeight*0.11);//f

		//ImagesButton coords
		imagesButtonCoord = new Point[5];
		imagesButtonCoord[0] = new Point(screenWidth*0.55, screenHeight*0.05);
		imagesButtonCoord[1] = new Point(screenWidth*0.85, screenHeight*0.20);
		imagesButtonCoord[2] = new Point(screenWidth*0.54, screenHeight*0.04);
		imagesButtonCoord[3] = new Point(screenWidth*0.85, screenHeight*0.20);
		imagesButtonCoord[4] = new Point(screenWidth*0.59, screenHeight*0.15);
		//PatientOne coords
		patientOneCoords = new Point[6];
		patientOneCoords[0] = new Point(screenWidth*0.05, screenHeight*0.05);
		patientOneCoords[1] = new Point(screenWidth*0.48, screenHeight*0.85);
		patientOneCoords[2] = new Point(screenWidth*0.04, screenHeight*0.04);
		patientOneCoords[3] = new Point(screenWidth*0.48, screenHeight*0.85);
		patientOneCoords[4] = new Point(screenWidth*0.09, screenHeight*0.15);
		patientOneCoords[5] = new Point(screenWidth*0.05, screenHeight*0.16); //profile pic
		int widthProfPic = (int) Math.abs(patientOneCoords[1].x - patientOneCoords[5].x);
		int heightProfPic = (int) Math.abs(patientOneCoords[1].y - patientOneCoords[5].y);
		profilePicRoi[0] = new Rect(patientOneCoords[5],new Size(widthProfPic, heightProfPic));
		Bitmap profilePic = BitmapFactory.decodeResource(resources, R.drawable.profilepic);
		Bitmap bitmap = Bitmap.createScaledBitmap( profilePic, widthProfPic, heightProfPic, true);
		mProfilePics[0] = new Mat();
		Utils.bitmapToMat(bitmap, mProfilePics[0], true);
		Imgproc.cvtColor(mProfilePics[0], mProfilePics[0], Imgproc.COLOR_RGBA2RGB);

		//PatientTwo Coords
		patientTwoCoords = new Point[6];
		patientTwoCoords[0] = new Point(screenWidth*0.52, screenHeight*0.05);
		patientTwoCoords[1] = new Point(screenWidth*0.95, screenHeight*0.85);
		patientTwoCoords[2] = new Point(screenWidth*0.51, screenHeight*0.04);
		patientTwoCoords[3] = new Point(screenWidth*0.95, screenHeight*0.85);
		patientTwoCoords[4] = new Point(screenWidth*0.56, screenHeight*0.15);
		patientTwoCoords[5] = new Point(screenWidth*0.52, screenHeight*0.16);//profile pic
		widthProfPic = (int) Math.abs(patientTwoCoords[1].x - patientTwoCoords[5].x);
		heightProfPic = (int) Math.abs(patientTwoCoords[1].y - patientTwoCoords[5].y);
		profilePicRoi[1] = new Rect(patientTwoCoords[5],new Size(widthProfPic, heightProfPic));
		//uncomment next two lines in case of different profile pic
		//		profilePic = BitmapFactory.decodeResource(resources, R.drawable.profilepic);
		//		bitmap = Bitmap.createScaledBitmap( profilePic, widthProfPic, heightProfPic, true);
		mProfilePics[1] = new Mat();
		Utils.bitmapToMat(bitmap, mProfilePics[1], true);
		Imgproc.cvtColor(mProfilePics[1], mProfilePics[1], Imgproc.COLOR_RGBA2RGB);

		//PatientInfo Coords
		patientInfoCoords = new Point[5];
		patientInfoCoords[0] = new Point(screenWidth*0.05, screenHeight*0.25);
		patientInfoCoords[1] = new Point(screenWidth*0.95, screenHeight*0.85);
		patientInfoCoords[2] = new Point(screenWidth*0.04, screenHeight*0.24);
		patientInfoCoords[3] = new Point(screenWidth*0.95, screenHeight*0.85);
		patientInfoCoords[4] = new Point(screenWidth*0.09, screenHeight*0.35);
		//PatientImages Coords
		patientImgsCoords = new Point[10];
		patientImgsCoords[0] = new Point(screenWidth*0.04, screenHeight*0.24);//big rectangle upper left
		patientImgsCoords[1] = new Point(screenWidth*0.95, screenHeight*0.85);//big rectanlge lower right
		patientImgsCoords[2] = new Point(screenWidth*0.15, screenHeight*0.35);//img1 upperleft
		patientImgsCoords[3] = new Point(screenWidth*0.45, screenHeight*0.85);//img1 lowerright
		patientImgsCoords[4] = new Point(screenWidth*0.55, screenHeight*0.35);//img2 upperleft
		patientImgsCoords[5] = new Point(screenWidth*0.85, screenHeight*0.85);//img2 lowerright
		patientImgsCoords[6] = new Point(screenWidth*0.09, screenHeight*0.31);//text above imgs
		patientImgsCoords[7] = new Point(screenWidth*0.20, screenHeight*0.40);//top left point where images will be drawn
		patientImgsCoords[8] = new Point(screenWidth*0.14, screenHeight*0.34);//img1 outer upperleft
		patientImgsCoords[9] = new Point(screenWidth*0.54, screenHeight*0.34);//img2 outer upperleft

		//fullScreenImg Coords
		fullScreenImgCoords = new Point[5];
		fullScreenImgCoords[0] = new Point(screenWidth*0.25, screenHeight*0.01);
		fullScreenImgCoords[1] = new Point(screenWidth*0.91, screenHeight*0.99);
		fullScreenImgCoords[2] = new Point(screenWidth*0.24, screenHeight*0.0);
		fullScreenImgCoords[3] = new Point(screenWidth*0.91, screenHeight*0.99);
		fullScreenImgCoords[4] = new Point(screenWidth*0.26, screenHeight*0.02);//top left point where images will be drawn
		//PatientInfo Text
		patientInfoText = new String[5];
//		patientInfoText[0] = "Lorem ipsum dolor sit amet,";
		patientInfoText[0] = "Say: Hello, I am playing a doctor,";
//		patientInfoText[1] = "consectetur adipisicing elit,";
		patientInfoText[1] = "this is an example of a record.";
//		patientInfoText[2] = "sed do eiusmod tempor incididunt";
		patientInfoText[2] = "Now, click the Images button above to";
//		patientInfoText[3] = "ut labore et dolore magna aliqua.";
		patientInfoText[3] = "see images!. Please select number 3.";
//		patientInfoText[4] = "Ut enim ad minim veniam...";
		patientInfoText[4] = "When clicked, it will appear bigger!";

		//load images just one time
		/*********Normal scale images**********/
		Bitmap bitmap_0 = BitmapFactory.decodeResource(resources, R.drawable.xr_0);
		Bitmap bitmap_1 = BitmapFactory.decodeResource(resources, R.drawable.xr_1);
		Bitmap bitmap_2 = BitmapFactory.decodeResource(resources, R.drawable.xr_2);
		Bitmap bitmap_3 = BitmapFactory.decodeResource(resources, R.drawable.rotate_aid);
		Bitmap bitmap_4 = BitmapFactory.decodeResource(resources, R.drawable.swipe_aid);
		Bitmap bitmap_5 = BitmapFactory.decodeResource(resources, R.drawable.zoom_aid);

		mPatientImages = new Mat[3];
		patientImgsRoi = new Rect[2];

		patientImgsRoi[0] = new Rect(patientImgsCoords[2], patientImgsCoords[3]);
		patientImgsRoi[1] = new Rect(patientImgsCoords[4], patientImgsCoords[5]);

		int bWidth = (int)(patientImgsCoords[3].x - patientImgsCoords[2].x);
		int bHeight = (int)(patientImgsCoords[3].y - patientImgsCoords[2].y);
		
		//image 0
		bitmap = Bitmap.createScaledBitmap( bitmap_0, bWidth, bHeight, true);
		mPatientImages[0] = new Mat();
		Utils.bitmapToMat(bitmap, mPatientImages[0], true);
		Imgproc.cvtColor(mPatientImages[0], mPatientImages[0], Imgproc.COLOR_RGBA2RGB);

		//image 1
		bitmap = Bitmap.createScaledBitmap(bitmap_1, bWidth, bHeight, true);
		mPatientImages[1] = new Mat();
		Utils.bitmapToMat(bitmap, mPatientImages[1], true);
		Imgproc.cvtColor(mPatientImages[1], mPatientImages[1], Imgproc.COLOR_RGBA2RGB);

		//image 2
		bitmap = Bitmap.createScaledBitmap(bitmap_2, bWidth, bHeight, true);
		mPatientImages[2] = new Mat();
		Utils.bitmapToMat(bitmap, mPatientImages[2], true);
		Imgproc.cvtColor(mPatientImages[2], mPatientImages[2], Imgproc.COLOR_RGBA2RGB);

		//ROI for Aid feedback
		aidRoi = new Rect(new Point(screenWidth*0.15, screenHeight*0.35), new Point(screenWidth*0.85, screenHeight*0.85));
		//aid rotate
		bitmap = Bitmap.createScaledBitmap( bitmap_3, aidRoi.width, aidRoi.height, true);
		mAidRotate = new Mat();
		Utils.bitmapToMat(bitmap, mAidRotate, true);
		Imgproc.cvtColor(mAidRotate, mAidRotate, Imgproc.COLOR_RGBA2RGB);
		//aid swipe
		bitmap = Bitmap.createScaledBitmap( bitmap_4, aidRoi.width, aidRoi.height, true);
		mAidSwipe = new Mat();
		Utils.bitmapToMat(bitmap, mAidSwipe, true);
		Imgproc.cvtColor(mAidSwipe, mAidSwipe, Imgproc.COLOR_RGBA2RGB);		
		//aid zoom
		bitmap = Bitmap.createScaledBitmap( bitmap_5, aidRoi.width, aidRoi.height, true);
		mAidZoom = new Mat();
		Utils.bitmapToMat(bitmap, mAidZoom, true);
		Imgproc.cvtColor(mAidZoom, mAidZoom, Imgproc.COLOR_RGBA2RGB);		
		
		/******* Full screen images*******/
		mFullScreenImages = new Mat[3];

		bHeight = (int)(fullScreenImgCoords[1].y - fullScreenImgCoords[4].y);
		bWidth = bHeight;

		fullScreenImgRoi = new Rect((int) fullScreenImgCoords[4].x, (int)fullScreenImgCoords[4].y, bWidth, bHeight);

		//image 0
		bitmap = Bitmap.createScaledBitmap( bitmap_0, bWidth, bHeight, true);
		mFullScreenImages[0] = new Mat();
		Utils.bitmapToMat(bitmap, mFullScreenImages[0], true);
		Imgproc.cvtColor(mFullScreenImages[0], mFullScreenImages[0], Imgproc.COLOR_RGBA2RGB);

		//image 1
		bitmap = Bitmap.createScaledBitmap(bitmap_1, bWidth, bHeight, true);
		mFullScreenImages[1] = new Mat();
		Utils.bitmapToMat(bitmap, mFullScreenImages[1], true);
		Imgproc.cvtColor(mFullScreenImages[1], mFullScreenImages[1], Imgproc.COLOR_RGBA2RGB);

		//image 2
		bitmap = Bitmap.createScaledBitmap(bitmap_2, bWidth, bHeight, true);
		mFullScreenImages[2] = new Mat();
		Utils.bitmapToMat(bitmap, mFullScreenImages[2], true);
		Imgproc.cvtColor(mFullScreenImages[2], mFullScreenImages[2], Imgproc.COLOR_RGBA2RGB);


		bitmap.recycle();
		bitmap_0.recycle();
		bitmap_1.recycle();
		bitmap_2.recycle();
		bitmap_3.recycle();
		bitmap_4.recycle();
		bitmap_5.recycle();
	}

	/******************************** Drawing methods ****************************************/

	public Mat drawBackButton(Mat mRgb, boolean fullScreenImage){
		Mat rec = mRgb;
		if(!fullScreenImage){
			if(currentHover == hoverbackbutton){
				Core.rectangle(rec, backButtonCoords[2], backButtonCoords[3], Tools.red, -1);
			}else{
				Core.rectangle(rec, backButtonCoords[2], backButtonCoords[3], Tools.green, -1);
			}
			Core.rectangle(rec, backButtonCoords[0], backButtonCoords[1], Tools.blue, -1);
			rec = writeToImage(rec, backButtonCoords[4], "Back");
		}else{
			if(currentHover == hoverbackbutton){
				Core.rectangle(rec, backButtonCoords[7], backButtonCoords[8], Tools.red, -1);
			}else{
				Core.rectangle(rec, backButtonCoords[7], backButtonCoords[8], Tools.green, -1);
			}
			Core.rectangle(rec, backButtonCoords[5], backButtonCoords[6], Tools.blue, -1);
			rec = writeToImage(rec, backButtonCoords[9], "Back", 0.7);
		}
		Mat output = new Mat();
		Core.addWeighted(mRgb, 0.0, rec, 1.0, 0, output);

		return output;
	}


	public Mat drawImagesButton(Mat mRgb){
		Mat rec = mRgb;
		if(currentHover == hoverImgsButton){
			Core.rectangle(rec, imagesButtonCoord[2], imagesButtonCoord[3], Tools.red, -1);
		}else{
			Core.rectangle(rec, imagesButtonCoord[2], imagesButtonCoord[3], Tools.green, -1);
		}
		Core.rectangle(rec, imagesButtonCoord[0], imagesButtonCoord[1], Tools.blue, -1);
		if(!imagesBtnClicked){
			rec = writeToImage(rec, imagesButtonCoord[4], "Images");
		}else{
			rec = writeToImage(rec, imagesButtonCoord[4], "Info");
		}
		Mat output = new Mat();
		Core.addWeighted(mRgb, 0.0, rec, 1.0, 0, output);

		return output;
	}

	public Mat drawPatientsToSelect(Mat mRgb){
		Mat rec = mRgb;
		//Patient One
		if(currentHover == hoverPatient1){
			Core.rectangle(rec, patientOneCoords[2], patientOneCoords[3], Tools.red, -1);
		}else{
			Core.rectangle(rec, patientOneCoords[2], patientOneCoords[3], Tools.green, -1);
		}
		Core.rectangle(rec, patientOneCoords[0], patientOneCoords[1], Tools.blue, -1);
		rec = writeToImage(rec, patientOneCoords[4], "Patient: Tom", 0.7);
		Log.i("GUIHandler", " profPicRoi::"+profilePicRoi[0].size().toString()
				+ " profPicSize::"+ mProfilePics[0].size().toString()
				);
		Core.addWeighted(rec.submat(profilePicRoi[0]), 0.0, mProfilePics[0], 1.0, 0, rec.submat(profilePicRoi[0]));

		//Patient Two
		//		Log.i("GUIHandler", " profPicRoi2::"+profilePicRoi[1].size().toString()
		//				+ " profPicSize2::"+ mProfilePics[1].size().toString()
		//				);

		if(currentHover == hoverPatient2){
			Core.rectangle(rec, patientTwoCoords[2], patientTwoCoords[3], Tools.red, -1);
		}else{
			Core.rectangle(rec, patientTwoCoords[2], patientTwoCoords[3], Tools.green, -1);
		}
		Core.rectangle(rec, patientTwoCoords[0], patientTwoCoords[1], Tools.blue, -1);
		rec = writeToImage(rec, patientTwoCoords[4], "Patient: Paul", 0.7);
		Core.addWeighted(rec.submat(profilePicRoi[1]), 0.0, mProfilePics[1], 1.0, 0, rec.submat(profilePicRoi[1]));

		Mat output = new Mat();
		Core.addWeighted(mRgb, 0.0, rec, 1.0, 0, output);

		return output;
	}


	public Mat drawPatientInfo(Mat mRgb){
		Mat rec;
		if(!imagesBtnClicked){
			rec = mRgb.clone();
			Core.rectangle(rec, patientInfoCoords[2], patientInfoCoords[3], Tools.blue, -1);
			//			Core.rectangle(rec, patientInfoCoords[2], patientInfoCoords[3], Tools.green, -1);
			//			Core.rectangle(rec, patientInfoCoords[0], patientInfoCoords[1], Tools.blue, -1);
			rec = writeToImage(rec, patientInfoCoords[4], sCurrentPatient +"'s Info");

			Point temp = patientInfoCoords[4].clone();
			for(int i = 0; i<patientInfoText.length; i++){
				temp.y = temp.y + screenHeight*0.08;
				//			Log.i("GUIHandler","Point "+i+"::"+temp.toString());
				rec = writeToImage(rec, temp, patientInfoText[i], 0.5);
			}
		}else{
			rec = drawPatientImages(mRgb);
		}
		Mat output = new Mat();
		Core.addWeighted(mRgb, 0.0, rec, 1.0, 0, output);

		return output;
	}

	public Mat drawPatientImages(Mat mRgb){
		Mat rec = mRgb;
		//Doc One
		//		Core.rectangle(rec, patientImgsCoords[2], patientImgsCoords[3], Tools.green, -1);
		//		Core.rectangle(rec, patientImgsCoords[0], patientImgsCoords[1], Tools.blue, -1);
		Core.rectangle(rec, patientImgsCoords[0], patientImgsCoords[1], Tools.blue, -1);
		//green rectangles beside images
		if(currentHover == hoverImg1){
			Core.rectangle(rec, patientImgsCoords[8], patientImgsCoords[3], Tools.red, -1);
		}else{
			Core.rectangle(rec, patientImgsCoords[8], patientImgsCoords[3], Tools.green, -1);
		}

		if(currentHover == hoverImg2){
			Core.rectangle(rec, patientImgsCoords[9], patientImgsCoords[5], Tools.red, -1);
		}else{
			Core.rectangle(rec, patientImgsCoords[9], patientImgsCoords[5], Tools.green, -1);
		}
		int iNextImg = (( iCurrentImg == numberImgs - 1)? 0 : iCurrentImg + 1);
		String toWrite = sCurrentPatient+"::Images "+ (iCurrentImg + 1) + " & "+ (iNextImg + 1)+ " " +" out of "+ numberImgs;
		rec = writeToImage(rec, patientImgsCoords[6], toWrite, 0.5);


		//patInfoCoords[5] upper left point for image
		//patInfoCoords[1] lower righ point for image (same as both squares)

//		Log.i("GUIHandler","rec::"+rec.submat(patientImgsRoi[1]).size().toString()+" mBitmap::"+mPatientImages[iCurrentImg].size().toString());
		Core.addWeighted(rec.submat(patientImgsRoi[0]), 0.0, mPatientImages[iCurrentImg], 1.0, 0, rec.submat(patientImgsRoi[0]));
		Core.addWeighted(rec.submat(patientImgsRoi[1]), 0.0, mPatientImages[iNextImg], 1.0, 0, rec.submat(patientImgsRoi[1]));
		Mat output = new Mat();
		Core.addWeighted(mRgb, 0.0, rec, 1.0, 0, output);
		return output;
	}

	public Mat drawFullScreenImage(Mat mRgb){
		Mat rec = mRgb;
		//Doc One
		if(currentHover == hoverBigImage){
			Core.rectangle(rec, fullScreenImgCoords[2], fullScreenImgCoords[3], Tools.red, -1);
		}else if(zoomLevel != 0){
			Core.rectangle(rec, fullScreenImgCoords[2], fullScreenImgCoords[3], Tools.green, -1);
		}
		Core.rectangle(rec, fullScreenImgCoords[0], fullScreenImgCoords[1], Tools.blue, -1);

		//patInfoCoords[5] upper left point for image
		//patInfoCoords[1] lower righ point for image (same as both squares)

		if(zoomLevel != 0){

			Mat toDraw = new Mat();

			int finalWidth = mFullScreenImages[iCurrentImg].width();
			int finalHeight = finalWidth;
			int smallWidth = (int) (finalWidth/( Math.pow(2,zoomLevel))); //for pyrUp to work the sizes have to be mult of 2
			int smallHeight = smallWidth;

			//			int smallX = (int) ((finalWidth - smallWidth)/( Math.pow(2,zoomLevel)));
			//			int smallY = (int) ((finalHeight - smallHeight)/( Math.pow(2,zoomLevel))); //for zoomed image

			int smallX = (int) ((pFullScreenImgCenter.x - smallWidth/2)/( Math.pow(2,zoomLevel)));
			int smallY = (int) ((pFullScreenImgCenter.y - smallHeight/2)/( Math.pow(2,zoomLevel)));

			int bigWidth = smallWidth*2;
			int bigHeight = smallHeight*2;

			for(int i=zoomLevel; i>=1; i--){
				while(smallX < 0){
					pFullScreenImgCenter.x = pFullScreenImgCenter.x + 10;
					smallX = (int) ((pFullScreenImgCenter.x - smallWidth/2)/( Math.pow(2,zoomLevel)));
				}
				while(smallY < 0){
					pFullScreenImgCenter.y = pFullScreenImgCenter.y + 10;
					smallY = (int) ((pFullScreenImgCenter.y - smallHeight/2)/( Math.pow(2,zoomLevel)));
				}
				while((smallX + smallWidth) >= finalWidth){
					pFullScreenImgCenter.x = pFullScreenImgCenter.x - 10;
					smallX = (int) ((pFullScreenImgCenter.x - smallWidth/2)/( Math.pow(2,zoomLevel)));
				}
				while((smallY + smallHeight) >= finalHeight){
					pFullScreenImgCenter.y = pFullScreenImgCenter.y - 10;
					smallY = (int) ((pFullScreenImgCenter.y - smallHeight/2)/( Math.pow(2,zoomLevel)));

				}
				Rect zoomRoi = new Rect(smallX, smallY, smallWidth, smallHeight);
				Size bigSize = new Size(bigWidth, bigHeight);				
				Log.i("GUIHandler", " zoomLev::"+i
						+ " zoomRoi::"+ zoomRoi.toString()
						+ " newSize::"+ bigSize.toString()
						+ " fsImg::"+ mFullScreenImages[iCurrentImg].size().toString() 
						);
				toDraw = new Mat();
				Imgproc.pyrUp(mFullScreenImages[iCurrentImg].submat(zoomRoi), toDraw, bigSize);
				smallX = smallX*2;
				smallY = smallY*2;
				smallWidth = smallWidth*2;
				smallHeight = smallHeight*2;
				bigWidth = bigWidth*2;
				bigHeight = bigHeight*2;
			}

			Log.i("GUIHandler","rec::"+rec.submat(fullScreenImgRoi).size().toString()+" toDraw::"+toDraw.size().toString()
					+ " imgRoi::"+fullScreenImgRoi.toString());


			Core.addWeighted(rec.submat(fullScreenImgRoi), 0.0, 
					toDraw, 1.0, 0, rec.submat(fullScreenImgRoi));
		}else {

			Core.addWeighted(rec.submat(fullScreenImgRoi), 0.0, 
					mFullScreenImages[iCurrentImg], 1.0, 0, rec.submat(fullScreenImgRoi));

		}
		Mat output = new Mat();
		Core.addWeighted(mRgb, 0.0, rec, 1.0, 0, output);
		return output;
	}


	public Mat drawAid(Mat mRgb, String gesture){
		Mat rec = mRgb;
		if(gesture == StatesHandler.sStateRotate){
			Core.addWeighted(rec.submat(aidRoi), 0.5, mAidRotate, 1.0, 0, rec.submat(aidRoi));
		}else if(gesture == StatesHandler.sStateSwipe){
			Core.addWeighted(rec.submat(aidRoi), 0.5, mAidSwipe, 1.0, 0, rec.submat(aidRoi));
		}else if(gesture == StatesHandler.sStateZoom){
			Core.addWeighted(rec.submat(aidRoi), 0.5, mAidZoom, 1.0, 0, rec.submat(aidRoi));			
		}
		return mRgb;
	}

	/******************************* Action methods *******************************************/
	/*
	 * onClick 
	 * returns true if click is accepted (clicked on something)
	 */
	public boolean onClick(Point click){
		/*
		 * - if iCurrentPatient == -1 --> we are in PatientSelection
		 * 		- check in which patient the click is inside of
		 * Coords [2] == upper left outer rectangle
		 * Coords [3] == lower right outer rectangle
		 */
		if(iCurrentPatient == -1){//PatSelection
			Rect rect_one = new Rect(patientOneCoords[2], patientOneCoords[3]);
			Rect rect_two = new Rect(patientTwoCoords[2], patientTwoCoords[3]);
			if(click.inside(rect_one)){
				iCurrentPatient = 0;
				sCurrentPatient = "Tom";
				currentHover = hoverNone;
				return true;
			}else if(click.inside(rect_two)){
				iCurrentPatient = 1;
				sCurrentPatient = "Paul";
				currentHover = hoverNone;
				return true;
			}
		}else if(!bigImgShowing){//RecordViewing
			/*
			 * - if iCurrentPatient != -1 --> we are in RecordViewing
			 * 		- if backBtnClicked == true --> previousState
			 * 		- if imagesBtnClicked == true --> stay and show images
			 * 		- if back&&imagesBtn == false --> nextState
			 */
			Rect rect_backBtn = new Rect(backButtonCoords[2],backButtonCoords[3]);
			Rect rect_imagesBtn = new Rect(imagesButtonCoord[2], imagesButtonCoord[3]);
			//			Rect rect_img = new Rect(patientImgsCoords[2], patientImgsCoords[3]);

			if(click.inside(rect_backBtn)){
				//go back to state PatientSelect
				iCurrentPatient = -1;
				sCurrentPatient = "N/A";
				iCurrentImg = 0;
				backBtnClicked = true;
				currentHover = hoverNone;
				imagesBtnClicked = false;
				return true;
			}else if(click.inside(rect_imagesBtn)){
				backBtnClicked = false;
				imagesBtnClicked = !imagesBtnClicked;
				currentHover = hoverNone;
				return false;
			}else if( imagesBtnClicked == true && click.inside(patientImgsRoi[0])){
				backBtnClicked = false;
				//				imagesBtnClicked = false;
				bigImgShowing = true;
				currentHover = hoverNone;
				return true;
			}else if( imagesBtnClicked == true && click.inside(patientImgsRoi[1])){
				backBtnClicked = false;
				iCurrentImg = (iCurrentImg == (numberImgs - 1))? 0:iCurrentImg+1;
				//				imagesBtnClicked = false;
				bigImgShowing = true;
				currentHover = hoverNone;
				return true;
			}
		}else{//ImgInteraction
			/*
			 * - if bigImgShowin == true --> we are in ImageInteraction
			 * 		- if backBtnClicked == true --> previousState
			 */
			/*
			 * Coords [2] == upper left outer rectangle
			 * Coords [3] == lower right outer rectangle
			 */
			Rect rect_back = new Rect(backButtonCoords[7],backButtonCoords[8]);
			Rect rect_fullScreenImg = new Rect(fullScreenImgCoords[2], fullScreenImgCoords[3]);

			if(click.inside(rect_back)){
				//go back to state PatientSelect
				backBtnClicked = false;
				//					imagesBtnClicked = false;
				bigImgShowing = false;
				currentHover = hoverNone;
				return true;
			}else if(click.inside(rect_fullScreenImg)){
				if(zoomLevel != 0){
					pFullScreenImgCenter = click;
				}
				currentHover = hoverNone;
				return false;
			}
		}
		return false;
	}

	/*
	 * hover 
	 * sets Hover action over available clickable objects
	 */
	public void hover(Point click){
		/*
		 * - if iCurrentPatient == -1 --> we are in PatientSelection
		 * 		- check in which patient the click is inside of
		 * Coords [2] == upper left outer rectangle
		 * Coords [3] == lower right outer rectangle
		 */
		if(iCurrentPatient == -1){//PatSelection
			Rect rect_one = new Rect(patientOneCoords[2], patientOneCoords[3]);
			Rect rect_two = new Rect(patientTwoCoords[2], patientTwoCoords[3]);
			if(click.inside(rect_one)){
				currentHover = hoverPatient1;
				return;
			}else if(click.inside(rect_two)){
				currentHover = hoverPatient2;
				return;
			}
		}else if(!bigImgShowing){//RecordViewing
			/*
			 * - if iCurrentPatient != -1 --> we are in RecordViewing
			 * 		- if backBtnClicked == true --> previousState
			 * 		- if imagesBtnClicked == true --> stay and show images
			 * 		- if back&&imagesBtn == false --> nextState
			 */
			Rect rect_backBtn = new Rect(backButtonCoords[2],backButtonCoords[3]);
			Rect rect_imagesBtn = new Rect(imagesButtonCoord[2], imagesButtonCoord[3]);
			//			Rect rect_img = new Rect(patientImgsCoords[2], patientImgsCoords[3]);

			if(click.inside(rect_backBtn)){
				//hover back button
				currentHover = hoverbackbutton;
				return;
			}else if(click.inside(rect_imagesBtn)){
				//hover on images button
				currentHover = hoverImgsButton;
				return;
			}else if( imagesBtnClicked == true && click.inside(patientImgsRoi[0])){
				currentHover = hoverImg1;
				return;
			}else if( imagesBtnClicked == true && click.inside(patientImgsRoi[1])){
				currentHover = hoverImg2;
				return;
			}
		}else{//ImgInteraction
			/*
			 * - if bigImgShowin == true --> we are in ImageInteraction
			 * 		- if backBtnClicked == true --> previousState
			 */
			/*
			 * Coords [2] == upper left outer rectangle
			 * Coords [3] == lower right outer rectangle
			 */
			Rect rect_back = new Rect(backButtonCoords[7],backButtonCoords[8]);
			Rect rect_fullScreenImg = new Rect(fullScreenImgCoords[2], fullScreenImgCoords[3]);

			if(click.inside(rect_back)){
				currentHover = hoverbackbutton;
				return;
			}else if(click.inside(rect_fullScreenImg)){
				if(zoomLevel != 0){
					currentHover = hoverBigImage;
				}
				return;
			}
		}
		currentHover = hoverNone;
	}


	/*
	 * swipe
	 * returns true if swipe is accepted (if images are showing)
	 */
	public boolean swipe(String side){
		if(imagesBtnClicked == true){
			if(side == "right"){
				iCurrentImg = (iCurrentImg == 0)? (numberImgs - 1): iCurrentImg - 1;
				return true;
			}else if(side == "left"){
				iCurrentImg = (iCurrentImg == (numberImgs - 1))? 0: iCurrentImg + 1;
				return true;
			}
		}
		return false;
	}

	/*
	 * rotate
	 * returns true if rotate is accepted
	 */
	public boolean rotate(String side){
		if(bigImgShowing == true){
			if(side == "left"){
				Core.transpose(mFullScreenImages[iCurrentImg], mFullScreenImages[iCurrentImg]);
				Core.flip(mFullScreenImages[iCurrentImg], mFullScreenImages[iCurrentImg], 0);
				//				mFullScreenImages[iCurrentImg] = mFullScreenImages[iCurrentImg].t();
				return true;
			}else if(side == "right"){
				//				mFullScreenImages[iCurrentImg] = mFullScreenImages[iCurrentImg].t();
				Core.transpose(mFullScreenImages[iCurrentImg], mFullScreenImages[iCurrentImg]);
				Core.flip(mFullScreenImages[iCurrentImg], mFullScreenImages[iCurrentImg], 1);
				return true;
			}
		}
		return false;
	}

	/*
	 * zoom
	 * returns true if zoom is accepted
	 * zoomLevels -> 0 (no zoom), 1, 2
	 */
	public boolean zoom(String zoom){
		if(bigImgShowing == true){
			if(zoom == "in"){
				zoomLevel = (zoomLevel < maxZoom)? zoomLevel + 1 : 0;
				return true;
			}else if(zoom == "out"){

				zoomLevel = (zoomLevel > 0)? zoomLevel - 1 : 0;
				return true;
			}
		}
		return false;
	}

	/******************************* Utility methods ******************************************/

	/*
	 * Utility method - writes to color image
	 */
	public Mat writeInfoToImage(Mat mRgb, final String string) {
		Core.putText(mRgb, string, infoCoords, Core.FONT_HERSHEY_SIMPLEX, 0.7, new Scalar(0,0,0), 5);
		Core.putText(mRgb, string, infoCoords, Core.FONT_HERSHEY_SIMPLEX, 0.7, new Scalar(255,255,255), 1);
		return mRgb;
	}

	/*
	 * Utility method - writes to color image
	 */
	public Mat writeWarningToImage(Mat mRgb, final String string) {
		Core.putText(mRgb, string, warningCoords, Core.FONT_HERSHEY_SIMPLEX, 0.7, new Scalar(0,0,0), 5);
		Core.putText(mRgb, string, warningCoords, Core.FONT_HERSHEY_SIMPLEX, 0.7, new Scalar(255,255,255), 1);
		return mRgb;
	}

	/*
	 * Utility method - writes to GUI image
	 */
	public Mat writeToImage(Mat mRgb, Point point, final String string) {
		Core.putText(mRgb, string, point, Core.FONT_HERSHEY_COMPLEX_SMALL, 1.0, new Scalar(200,200,200), 2);
		return mRgb;
	}

	/*
	 * Utility method - writes to GUI image
	 */
	public Mat writeToImage(Mat mRgb, Point point, final String string, double scale) {
		Core.putText(mRgb, string, point, Core.FONT_HERSHEY_SIMPLEX, scale, new Scalar(200,200,200), 2);
		return mRgb;
	}
}
