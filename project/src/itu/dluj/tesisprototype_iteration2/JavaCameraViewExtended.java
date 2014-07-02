package itu.dluj.tesisprototype_iteration2;

import java.util.List;

import org.opencv.android.JavaCameraView;

import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.util.AttributeSet;

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
}
