package com.image.bhargav.facecatcher;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
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

import java.util.ArrayList;
import java.util.List;

public class Main2Activity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2, View.OnTouchListener {

    private CameraBridgeViewBase mOpenCvCameraView;
    static int cameraID = 1;//frontcam - 1, back cam - 0
    static boolean isFlipOn = true;

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
    public void onResume()
    {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main2);
        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.HelloOpenCvView);
        mOpenCvCameraView.setCameraIndex(cameraID);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
        mOpenCvCameraView.setMaxFrameSize(320, 240);
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {

    }

    public void onCameraViewStopped() {
    }

    public Mat getBlurredImage(Mat source){
        Mat destination = new Mat(source.rows(),source.cols(),source.type());
        Imgproc.GaussianBlur(source, destination,new Size(11,11), 0);
        return destination;
    }

    public Mat getHSVImage(Mat source){
        Mat destination = new Mat(source.rows(),source.cols(),source.type());
        Imgproc.cvtColor(source, destination, Imgproc.COLOR_RGB2HSV);
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
        Mat dilateElement = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new  Size(12, 12));

        Imgproc.dilate(masked, morphOutput, dilateElement);
        Imgproc.dilate(masked, morphOutput, dilateElement);

        dilateElement.release();

        //masked.release();

        return morphOutput;
    }

    public List<MatOfPoint> getContours(Mat frame){
        final List<MatOfPoint> contours  = new ArrayList<>();
        final Mat hierarchy = new Mat();
        Imgproc.findContours(frame, contours , hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);
        //frame.release();
        return contours;
    }

    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

        Mat imageRGB = inputFrame.rgba();
        Mat flipped = new Mat();

        if(isFlipOn){
            Core.flip(imageRGB, flipped, 1);
            imageRGB.release();
        }else{
            flipped = imageRGB;
        }

        Mat blurred = getBlurredImage(flipped);//optional. can use the hsv directly from the rgb without the blur too
        //flipped.release();
        Mat hsvImage = getHSVImage(blurred);
        blurred.release();

        //get a mask
        Mat masked1 = new Mat();
        Core.inRange(hsvImage, new Scalar(0, 176, 114), new Scalar(30, 255, 255), masked1);//for yellow ball - re calibrated

        Mat masked2 = new Mat();
        Core.inRange(hsvImage, new Scalar(37, 96, 143), new Scalar(65, 211, 232), masked2);//for green tape

        hsvImage.release();

        Mat morph1 = getMorphedMat(masked1);
        masked1.release();
        Mat morph2 = getMorphedMat(masked2);
        masked2.release();



        List<MatOfPoint> contours1 = getContours(morph1);
        List<MatOfPoint> contours2 = getContours(morph2);

        List<MatOfPoint> allContours = contours1;

        for(MatOfPoint contour : contours2){
            allContours.add(contour);
        }


        if(allContours.size()>0){
            //For each contour found
            double maxVal = 0;
            int biggestContourId = 0;
            int secondBiggestContourId = 0;
            for (int contourIdx = 0; contourIdx < allContours.size(); contourIdx++)
            {
                double contourArea = Imgproc.contourArea(allContours.get(contourIdx));
                if (maxVal < contourArea)
                {
                    maxVal = contourArea;
                    secondBiggestContourId = biggestContourId;
                    biggestContourId = contourIdx;
                }
            }

            MatOfPoint2f approxCurve = new MatOfPoint2f();
            //Convert contour from MatOfPoint to MatOfPoint2f
            MatOfPoint2f contour2f = new MatOfPoint2f( allContours.get(secondBiggestContourId).toArray() );
            //Processing on mMOP2f1 which is in type MatOfPoint2f
            double approxDistance = Imgproc.arcLength(contour2f, true)*0.02;
            Imgproc.approxPolyDP(contour2f, approxCurve, approxDistance, true);

            //Convert back to MatOfPoint
            MatOfPoint points = new MatOfPoint( approxCurve.toArray() );

            // Get bounding rect of contour
            Rect rect = Imgproc.boundingRect(points);

            // draw enclosing rectangle (all same color, but you could use variable i to make them unique)
            Imgproc.rectangle(flipped, rect.tl(), rect.br(), new Scalar(255, 0, 0),1, 8,0);

        }
        return getCircledFrame(flipped);
    }

    public Mat getCircledFrame(Mat maskedFrame){

        //circle the object in the frame



        return maskedFrame;
    }

    public void onSwitchCamTouch(View view) {
        Toast.makeText(getApplicationContext(),"changing camera", Toast.LENGTH_SHORT).show();
        cameraID = cameraID == 0?1:0;
        finish();
        startActivity(getIntent());
    }

    public void onFlipTouch(View view){
        isFlipOn = !isFlipOn;
        finish();
        startActivity(getIntent());
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        return false;
    }
}
