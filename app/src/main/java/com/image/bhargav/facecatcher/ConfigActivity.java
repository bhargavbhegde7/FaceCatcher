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
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.BackgroundSubtractorMOG2;
import org.opencv.video.Video;

import java.util.ArrayList;
import java.util.List;

import static org.opencv.imgproc.Imgproc.ADAPTIVE_THRESH_MEAN_C;
import static org.opencv.imgproc.Imgproc.THRESH_BINARY;
import static org.opencv.imgproc.Imgproc.blur;

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
    public static int s_min = 176;
    public static int v_min = 114;

    public static int h_max = 30;
    public static int s_max = 255;
    public static int v_max = 255;

    public static int blockSize = 15;
    public static double C = 40;

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

    public Mat getBlurredImage(Mat source){
        Mat destination = new Mat(source.rows(),source.cols(),source.type());
        Imgproc.GaussianBlur(source, destination,new Size(11,11), 0);
        return destination;
    }

    public Mat getMorphedMat(Mat masked){
        Mat morphOutput = new Mat();

        //erode
        Mat erosionElement = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new  Size(34, 34));

        Imgproc.erode(masked, morphOutput, erosionElement);//use blurred instead of masked1 here to display the image without mask
        Imgproc.erode(masked, morphOutput, erosionElement);

        erosionElement.release();

        //dilate
        Mat dilateElement = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new  Size(15, 15));

        Imgproc.dilate(masked, morphOutput, dilateElement);
        Imgproc.dilate(masked, morphOutput, dilateElement);

        dilateElement.release();

        return morphOutput;
    }

    public List<MatOfPoint> getContours(Mat frame){
        final List<MatOfPoint> contours  = new ArrayList<>();
        final Mat hierarchy = new Mat();
        Imgproc.findContours(frame, contours , hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);
        return contours;
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat imageRGB = inputFrame.rgba();
        Mat flipped = new Mat();

        Core.flip(imageRGB, flipped, 1);
        imageRGB.release();

        Mat blurred = getBlurredImage(flipped);//optional. can use the hsv directly from the rgb without the blur too
        flipped.release();

        Mat hsvImage = getHSVImage(blurred);
        blurred.release();

        Mat masked = new Mat();
        Core.inRange(hsvImage, new Scalar(h_min, s_min, v_min), new Scalar(h_max, s_max, v_max), masked);//for yellow ball - re calibrated
        hsvImage.release();

        Mat morphed = getMorphedMat(masked);
        masked.release();

        //List<MatOfPoint> contours = getContours(morphed);
        //morphed.release();

        //---------------------------------------------------------

        /*if(contours.size()>0){
            //For each contour found
            double maxVal = 0;
            int biggestContourId = 0;
            for (int contourIdx = 0; contourIdx < contours.size(); contourIdx++)
            {
                double contourArea = Imgproc.contourArea(contours.get(contourIdx));
                if (maxVal < contourArea)
                {
                    maxVal = contourArea;
                    biggestContourId = contourIdx;
                }
            }

            MatOfPoint2f approxCurve = new MatOfPoint2f();
            //Convert contour from MatOfPoint to MatOfPoint2f
            MatOfPoint2f contour2f = new MatOfPoint2f( contours.get(biggestContourId).toArray() );
            //Processing on mMOP2f1 which is in type MatOfPoint2f
            double approxDistance = Imgproc.arcLength(contour2f, true)*0.02;
            Imgproc.approxPolyDP(contour2f, approxCurve, approxDistance, true);

            //Convert back to MatOfPoint
            MatOfPoint points = new MatOfPoint( approxCurve.toArray() );

            // Get bounding rect of contour
            Rect rect = Imgproc.boundingRect(points);

            // draw enclosing rectangle (all same color, but you could use variable i to make them unique)
            Imgproc.rectangle(morphed, rect.tl(), rect.br(), new Scalar(255, 0, 0),1, 8,0);

        }*/

        //---------------------------------------------------------

        return morphed;
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
        if(seekBar == hMinSeekBar){
            h_min = progress;
        }
        else if(seekBar == sMinSeekBar){
            s_min = progress;
        }
        else if(seekBar == vMinSeekBar){
            v_min = progress;
        }
        else if(seekBar == hMaxSeekBar){
            h_max = progress;
        }
        else if(seekBar == sMaxSeekBar){
            s_max = progress;
        }
        else if(seekBar == vMaxSeekBar){
            v_max = progress;
        }
        Toast.makeText(getApplicationContext(),"seekbar progress: "+progress, Toast.LENGTH_SHORT).show();
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
