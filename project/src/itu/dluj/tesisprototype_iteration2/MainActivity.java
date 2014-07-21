package itu.dluj.tesisprototype_iteration2;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import android.app.Activity;
import android.os.Bundle;
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
	 * OPENCV
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
        return output;
	}

}
