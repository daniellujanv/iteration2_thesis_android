package itu.dluj.tesisprototype.i4;

import itu.dluj.tesisprototype_iteration2.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.WindowManager;

/**
 * 
 * 
 */
public class MainActivity extends Activity implements CvCameraViewListener2 {
	
//	private JavaCameraViewExtended mOpenCvCameraView;
	private JavaCameraView mOpenCvCameraView;
	private StatesHandler statesHandler;
	private MenuItem miFrontCamera;
	private MenuItem miBackCamera;
	final Handler mHandler = new Handler();
	private String sDeviceModel = android.os.Build.MODEL;
	private int cameraIndex;
	private int screenWidth;
	private int screenHeight;

//	private Mat mProcessed;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Log.i("opencv", "called onCreate");
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		setContentView(R.layout.activity_main);

		mOpenCvCameraView = (JavaCameraView) findViewById(R.id.cameraView);
//		mOpenCvCameraView = (JavaCameraViewExtended) findViewById(R.id.cameraView);
		mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);

		if(
//				sDeviceModel.equals("Nexus 5") ||
				sDeviceModel.equals("GT-S6810P")
				){
			mOpenCvCameraView.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_FRONT);
			cameraIndex = CameraBridgeViewBase.CAMERA_ID_FRONT;
		}else{
			mOpenCvCameraView.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_ANY);
			cameraIndex = CameraBridgeViewBase.CAMERA_ID_ANY;
		}
		mOpenCvCameraView.enableFpsMeter();
		mOpenCvCameraView.setCvCameraViewListener(this);
	
	}


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
//        mItemPreviewRGBA  = menu.add("Preview RGBA");
        miFrontCamera = menu.add("Front Camera");
        miBackCamera = menu.add("Back Camera");
    	return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	mOpenCvCameraView.disableView();
        if(item == miFrontCamera){
        	mOpenCvCameraView.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_FRONT);
        }else if(item == miBackCamera){
        	mOpenCvCameraView.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_BACK);
        }
        mOpenCvCameraView.enableView();
        return true;
    }
    
	/**
	 * *********************************************
	 * OpenCV
	 * *********************************************
	 */

	 @Override
	 public void onPause()
	 {
		 Log.i("pause", "app paused");
		 super.onPause();
	     if (mOpenCvCameraView != null)
	         mOpenCvCameraView.disableView();
	 }

	 public void onDestroy() {
		 Log.i("storagedirectory", Environment.DIRECTORY_DOWNLOADS.toString());
		 String state = Environment.getExternalStorageState();
		    if (Environment.MEDIA_MOUNTED.equals(state)) {
		    	Log.i("MainActivity", "Mdia mounted");
				 File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
				 if(!path.mkdirs()){
					 Log.i("MainActivity", "Error mkdirs");
				 }
				 //				 File path = this.getExternalFilesDir(null);
				 String fileName = "logcatParticipant6.txt";
				 File file = new File(path, fileName);
			     try {
			    	 OutputStream os = new FileOutputStream(file);
			    	 Log.i("bla", "writting");
			    	 os.write(("testng").getBytes());
			    	 os.close();
			    	 Log.i("bla", "closing");
			    	 Runtime.getRuntime().exec(new String[]{"logcat", "-f", file.getPath(), "itu.dluj.tesisprototype_iteration2:I"});
			    	 Log.i("bla", "logcat done :: "+ path.toString());
			    	 Intent mediaScanIntent = new Intent("android.intent.action.MEDIA_SCANNER_SCAN_FILE");
			    	    mediaScanIntent.setData(Uri.fromFile(file));
			    	    this.sendBroadcast(mediaScanIntent);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					 Log.i("storagedirectory", e.toString());
				}
		    }else{
		    	Log.i("MainActivity", "Mdia NOT mounted");
		    }

		 
		 Log.i("crash", "app crashed");
	     super.onDestroy();
	     if (mOpenCvCameraView != null)
	         mOpenCvCameraView.disableView();
	 }
	 
	private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
		@Override
		public void onManagerConnected(int status) {
			switch (status) {
			case LoaderCallbackInterface.SUCCESS:
			{
				Log.i("opencv", "OpenCV loaded successfully");
				mOpenCvCameraView.enableView();

			} break;
			default:
			{
				super.onManagerConnected(status);
			} break;
			}
		}
	};

	@Override
	public void onResume()
	{
		super.onResume();
		OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_6, this, mLoaderCallback);
	}

	@Override
	public void onCameraViewStarted(int width, int height) {				
//		mOpenCvCameraView.setFpsRange(30000, 30000);
		Log.i("MainActivity", "size:: w:"+ width+" h:"+height);
		statesHandler = new StatesHandler(width, height, MainActivity.this);
		screenHeight = height;
		screenWidth = width;
//		Log.i("MainActivity", "FPSRange::"+ mOpenCvCameraView.getFpsRange());
	}

	@Override
	public void onCameraViewStopped() {
		 Log.i("stop", "camera view stopped");
	}

	@Override
	public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
		Mat output = new Mat();
//		Log.i("DEVICE", sDeviceModel);
		if(cameraIndex == CameraBridgeViewBase.CAMERA_ID_BACK){  
			Core.flip(inputFrame.rgba(), output, 1);
		}else{
			output = inputFrame.rgba();
		}
		
//		Mat gray = new Mat();
//		Imgproc.cvtColor(output, gray, Imgproc.COLOR_RGB2GRAY);
//		Imgproc.equalizeHist(gray, gray);
//		Imgproc.cvtColor(gray, output, Imgproc.COLOR_GRAY2RGBA);
//		
		
		Mat outputScaled = new Mat();
//		Log.i("MainActivity", "dims output before pyrdown::"+ output.cols());
		Imgproc.pyrDown(output, outputScaled);
//		Log.i("MainActivity", "dims scaled after pyrdown::"+ outputScaled.cols());
		outputScaled = statesHandler.handleFrame(outputScaled);
		Imgproc.pyrUp(outputScaled, output);
//		Log.i("MainActivity", "dims output after pyrup::"+ output.cols());
		Point handCentroid = statesHandler.getHandCentroid();
		if(handCentroid != null){
//			Log.i("StatesHandler", "centroidNormal::"+temp.toString());
			mOpenCvCameraView.resetFMAreas(handCentroid, screenWidth, screenHeight);
		}
        return output;
	}

}
