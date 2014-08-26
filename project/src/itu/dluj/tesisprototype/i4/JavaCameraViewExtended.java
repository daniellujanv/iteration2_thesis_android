package itu.dluj.tesisprototype.i4;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;

import android.content.Context;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.util.Log;

public class JavaCameraViewExtended extends JavaCameraView {

    private static final String TAG = "JavaCameraViewExtended";

    public JavaCameraViewExtended(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    
    public JavaCameraViewExtended(Context context, int cameraId) {
        super(context, cameraId);
    }

    public void setFpsRange(int min, int max) {
        Camera.Parameters params = mCamera.getParameters();
        params.setPreviewFpsRange(min, max);
        mCamera.setParameters(params);
    }
    
    public int getDefaultCameraIndex(){
    	Log.i(TAG, "no. cameras:: "+ Camera.getNumberOfCameras());
    	if(Camera.getNumberOfCameras() > 1){
    		return CameraBridgeViewBase.CAMERA_ID_FRONT;
    	}
    	return CameraBridgeViewBase.CAMERA_ID_ANY;
    }
    
    public String getFpsRange(){
    	Camera.Parameters params = mCamera.getParameters();
    	int[] range = new int[2];
        params.getPreviewFpsRange(range);
        String result = "PreviewFpsRange:: min: "+ range[0]+ " max:"+ range[1];
        return result;
    }
}
