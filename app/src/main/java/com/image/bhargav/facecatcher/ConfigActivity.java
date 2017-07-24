package com.image.bhargav.facecatcher;

import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.SeekBar;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

public class ConfigActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2, View.OnTouchListener, SeekBar.OnSeekBarChangeListener{

    private CameraBridgeViewBase mOpenCvCameraView;
    static int cameraID = 1;//frontcam - 1, back cam - 0

    SeekBar hMinSeekBar;
    SeekBar sMinSeekBar;
    SeekBar vMinSeekBar;

    SeekBar hMaxSeekBar;
    SeekBar sMaxSeekBar;
    SeekBar vMaxSeekBar;

    public static int h_min = 0;
    public static int s_min = 0;
    public static int v_min = 0;

    public static int h_max = 255;
    public static int s_max = 255;
    public static int v_max = 255;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i("--TAG--", "OpenCV loaded successfully");
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
    protected void onResume() {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.config_masked_view);
        mOpenCvCameraView.setCameraIndex(cameraID);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
        mOpenCvCameraView.setMaxFrameSize(320, 240);

        hMinSeekBar =(SeekBar)findViewById(R.id.h_min);
        sMinSeekBar =(SeekBar)findViewById(R.id.s_min);
        vMinSeekBar =(SeekBar)findViewById(R.id.v_min);

        hMaxSeekBar =(SeekBar)findViewById(R.id.h_max);
        sMaxSeekBar =(SeekBar)findViewById(R.id.s_max);
        vMaxSeekBar =(SeekBar)findViewById(R.id.v_max);

        hMaxSeekBar.setProgress(255);
        sMaxSeekBar.setProgress(255);
        vMaxSeekBar.setProgress(255);

        hMinSeekBar.setOnSeekBarChangeListener(this);
        sMinSeekBar.setOnSeekBarChangeListener(this);
        vMinSeekBar.setOnSeekBarChangeListener(this);

        hMaxSeekBar.setOnSeekBarChangeListener(this);
        sMaxSeekBar.setOnSeekBarChangeListener(this);
        vMaxSeekBar.setOnSeekBarChangeListener(this);
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        return false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onCameraViewStarted(int width, int height) {

    }

    @Override
    public void onCameraViewStopped() {

    }

    public Mat getHSVImage(Mat source){
        Mat destination = new Mat(source.rows(),source.cols(),source.type());
        Imgproc.cvtColor(source, destination, Imgproc.COLOR_RGB2HSV);
        return destination;
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat imageRGB = inputFrame.rgba();
        Mat flipped = new Mat();

        Core.flip(imageRGB, flipped, 1);
        imageRGB.release();

        Mat hsvImage = getHSVImage(flipped);

        //get a mask
        Mat masked = new Mat();
        Core.inRange(hsvImage, new Scalar(h_min, s_min, v_min), new Scalar(h_max, s_max, v_max), masked);//for yellow ball - re calibrated

        hsvImage.release();

        return masked;
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
        if(seekBar == hMinSeekBar){
            h_min = i;
        }
        else if(seekBar == sMinSeekBar){
            s_min = i;
        }
        else if(seekBar == vMinSeekBar){
            v_min = i;
        }
        else if(seekBar == hMaxSeekBar){
            h_max = i;
        }
        else if(seekBar == sMaxSeekBar){
            s_max = i;
        }
        else if(seekBar == vMaxSeekBar){
            v_max = i;
        }
        Toast.makeText(getApplicationContext(),"seekbar progress: "+i, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        //Toast.makeText(getApplicationContext(),"seekbar touch started!", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        //Toast.makeText(getApplicationContext(),"seekbar touch stopped!", Toast.LENGTH_SHORT).show();
    }
}
