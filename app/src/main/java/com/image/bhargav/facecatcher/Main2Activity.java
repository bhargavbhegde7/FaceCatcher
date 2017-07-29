package com.image.bhargav.facecatcher;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
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
import org.opencv.core.Point;
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
    private Handler mHandler;

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

        mHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message message) {
                // This is where you do your work in the UI thread.
                // Your worker tells you in the message what to do.

                Toast.makeText(getApplicationContext(), "awesome!!", Toast.LENGTH_SHORT).show();
            }
        };
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
        Mat hsvImage = getHSVImage(blurred);
        blurred.release();

        //get a mask
        Mat masked = new Mat();
        //Core.inRange(hsvImage, new Scalar(0, 176, 114), new Scalar(30, 255, 255), masked);//for yellow balls - re calibrated
        //Core.inRange(hsvImage, new Scalar(17, 168, 112), new Scalar(255, 255, 255), masked);//for yellow ball - re calibrated
        //Core.inRange(hsvImage, new Scalar(100, 114, 77), new Scalar(166, 255, 255), masked);//for blue lids
        Core.inRange(hsvImage, new Scalar(14, 96, 124), new Scalar(98, 225, 224), masked);//for blue lids

        hsvImage.release();

        Mat morph = masked.clone();
        morph = getMorphedMat(morph);
        //masked.release();

        List<MatOfPoint> contours = getContours(morph);
        //morph.release();

        int count = 0;
        Point[] centers = new Point[2];
        for(MatOfPoint contour : contours){

            if(count++>2){
                break;
            }

            drawBorder(contour, flipped);

            float[] radius = new float[1];
            Point center = new Point();
            Imgproc.minEnclosingCircle(new MatOfPoint2f(contour.toArray()), center, radius);

            if(count-1<2) {
                centers[count - 1] = center;
            }
        }

        double distance = 0;
        if(centers[0] != null && centers[1] != null){
            Imgproc.line(flipped, centers[0], centers[1], new Scalar(0,255,0), 1);

            distance = euclideanDistance(centers[0], centers[1]);
            if(distance < 50.0){
                //Toast.makeText(getApplicationContext(), "", Toast.LENGTH_SHORT).show();

                // And this is how you call it from the worker thread:
                Message message = mHandler.obtainMessage(1, "bhargav");
                message.sendToTarget();

            }
        }

        return flipped;
    }

    public double euclideanDistance(Point a, Point b){
        double distance = 0.0;
        try{
            if(a != null && b != null){
                double xDiff = a.x - b.x;
                double yDiff = a.y - b.y;
                distance = Math.sqrt(Math.pow(xDiff,2) + Math.pow(yDiff, 2));
            }
        }catch(Exception e){
            System.err.println("Something went wrong in euclideanDistance function : "+e.getMessage());
        }
        return distance;
    }

    private void drawBorder(MatOfPoint contour, Mat frame) {
        MatOfPoint2f approxCurve = new MatOfPoint2f();
        //Convert contour from MatOfPoint to MatOfPoint2f
        MatOfPoint2f contour2f = new MatOfPoint2f( contour.toArray() );
        //Processing on mMOP2f1 which is in type MatOfPoint2f
        double approxDistance = Imgproc.arcLength(contour2f, true)*0.02;
        Imgproc.approxPolyDP(contour2f, approxCurve, approxDistance, true);

        //Convert back to MatOfPoint
        MatOfPoint points = new MatOfPoint( approxCurve.toArray() );

        // Get bounding rect of contour
        Rect rect = Imgproc.boundingRect(points);

        // draw enclosing rectangle (all same color, but you could use variable i to make them unique)
        Imgproc.rectangle(frame, rect.tl(), rect.br(), new Scalar(255, 0, 0),1, 8,0);
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

    public void onConfigTouch(View view){
        Toast.makeText(getApplicationContext(), "configure", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        return false;
    }
}
