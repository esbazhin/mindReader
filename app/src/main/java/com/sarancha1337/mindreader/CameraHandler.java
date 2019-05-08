package com.sarancha1337.mindreader;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.media.MediaRecorder;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;
import com.affectiva.android.affdex.sdk.Frame;
import com.affectiva.android.affdex.sdk.detector.Detector;
import com.affectiva.android.affdex.sdk.detector.Face;
import com.affectiva.android.affdex.sdk.detector.FrameDetector;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static java.lang.Math.abs;

class CameraHandler {
    private boolean recording = false;
    private static Camera mCamera;
    private MediaRecorder mediaRecorder;
    private CameraPreview mPreview;

    CameraHandler(Context act, SurfaceView surf, boolean detect) {
        if (!hasCamera(act)) {
            Toast toast = Toast.makeText(act, "Sorry, your phone does not have a camera!", Toast.LENGTH_LONG);
            toast.show();
            //act.finish();
        }

            releasePreview();
            releaseCamera();
            // if the front facing camera does not exist
            int cameraId = findFrontFacingCamera();
            if (cameraId == 1) {

                mCamera = Camera.open(cameraId);
                mPreview = new CameraPreview(act, surf.getHolder(), mCamera, detect);
                mPreview.refreshCamera(mCamera);

            } else {
                Toast toast = Toast.makeText(act, "Sorry, your phone has only one camera!", Toast.LENGTH_LONG);
                toast.show();
            }

    }

    private boolean hasCamera(Context context) {
        // check if the device has camera
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA);
    }

    private int findFrontFacingCamera() {
        int cameraId = -1;
        // Search for the front facing camera
        int numberOfCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                cameraId = i;
                break;
            }
        }
        return cameraId;
    }

    private void releaseMediaRecorder() {
        if (mediaRecorder != null) {
            mediaRecorder.reset(); // clear recorder configuration
            mediaRecorder.release(); // release the recorder object
            mediaRecorder = null;
            //mCamera.lock(); // lock camera for later use
        }
    }

    private boolean prepareMediaRecorder(Activity act) {

        mediaRecorder = new MediaRecorder();

        mCamera.unlock();
        mediaRecorder.setCamera(mCamera);

        // Step 2: Set sources
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

        // Step 3: Set all values contained in profile except audio settings
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.MPEG_4_SP);
        mediaRecorder.setVideoFrameRate(15);

        String path = act.getExternalFilesDir(null).getPath()+"/videos/";
        File f = new File(path);
        if (!f.exists())
            f.mkdir();

        path += new Date().toString() +".mp4";

        mediaRecorder.setOutputFile(path);
        //mediaRecorder.setMaxDuration(600000); //set maximum duration 60 sec.
        //mediaRecorder.setMaxFileSize(50000000); //set maximum file size 50M

        try {
            mediaRecorder.prepare();
        } catch (IllegalStateException | IOException e) {
            e.printStackTrace();
            releaseMediaRecorder();
            return false;
        }
        return true;

    }

    private void releaseCamera() {
        // stop and release camera
        if (mCamera != null) {
            //mCamera.release();
            mCamera = null;
        }
    }

    private void releasePreview() {
        // stop and release camera
        if (mPreview != null) {
            mPreview.release();
            mPreview = null;
        }
    }

    void release()
    {
        releaseCamera();
        //releaseMediaRecorder();
        //mPreview.release();
        //mPreview = null;
    }

    void stopRecording(Activity act) {
        if (recording)
        {
            // stop recording and release camera
            mediaRecorder.stop(); // stop the recording
            releaseMediaRecorder(); // release the MediaRecorder object
            Toast.makeText(act, "Video captured!", Toast.LENGTH_LONG).show();
            recording = false;
        }
    }

    void startRecording(Activity act)
    {
        if(!recording) {
            if (!prepareMediaRecorder(act)) {
                Toast.makeText(act, "Fail in prepareMediaRecorder()!\n - Ended -", Toast.LENGTH_LONG).show();
                act.finish();
            }
            // work on UiThread for better performance
            act.runOnUiThread(new Runnable() {
                public void run() {
                    // If there are stories, add them to the table

                    try {
                        mediaRecorder.start();
                        mPreview.start();
                    } catch (final Exception ex) {
                        ex.printStackTrace();
                        releaseMediaRecorder();
                        recording = false;
                        // Log.i("---","Exception in thread");
                    }
                }
            });

            recording = true;
        }
    }

    String getEmotion(){return mPreview.getEmotion();}
    float getValence(){return mPreview.getValence();}
    float getEng(){return mPreview.getEng();}
}


class CameraPreview extends SurfaceView implements SurfaceHolder.Callback, Detector.ImageListener {
    private SurfaceHolder mHolder;
    private static Camera mCamera;
    private FrameDetector detector;
    private String curEmotion;
    private float curValence;
    private float curEng;

    CameraPreview(Context context, SurfaceHolder h, Camera camera, boolean detect) {
        super(context);
        mCamera = camera;
        mHolder = h;
        mHolder.addCallback(this);
        // deprecated setting, but required on Android versions prior to 3.0
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        if(detect)
        {
            detector = new FrameDetector(getContext());
            detector.setDetectAllEmotions(true);
            detector.setImageListener(this);
        }
        else
            detector = null;
    }

    public void surfaceCreated(SurfaceHolder holder) {
        try {
            // create the surface and start camera preview
            if (mCamera != null) {
                mCamera.setPreviewDisplay(holder);
                mCamera.startPreview();

                //detector.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void start()
    {
        if(detector != null && !detector.isRunning())
            detector.start();
    }

    void refreshCamera(Camera camera) {
        if (mHolder.getSurface() == null) {
            // preview surface does not exist
            return;
        }
        // stop preview before making changes
        try {
            if(detector != null && detector.isRunning())
                detector.stop();
            mCamera.stopPreview();
        } catch (Exception e) {
            // ignore: tried to stop a non-existent preview
        }
        // set preview size and make any resize, rotate or
        // reformatting changes here
        // start preview with new settings
        setCamera(camera);
        try {
            mCamera.setPreviewDisplay(mHolder);
            mCamera.startPreview();
            if(detector != null && !detector.isRunning())
                detector.start();
        } catch (Exception e) {
            Log.d(VIEW_LOG_TAG, "Error starting camera preview: " + e.getMessage());
        }
    }

    void release()
    {
        setCamera(null);
        if(detector != null && detector.isRunning())
        {
            detector.stop();
            detector = null;
        }
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        // If your preview can change or rotate, take care of those events here.
        // Make sure to stop the preview before resizing or reformatting it.
        //refreshCamera(mCamera);
    }

    long t = 0;
    private void setCamera(Camera camera) {
        //method to set a camera instance
        mCamera = camera;

        mCamera.setPreviewCallback(new Camera.PreviewCallback() {

            @Override
            public void onPreviewFrame(byte[] data, Camera camera) {

                // ***The parameter 'data' holds the frame information***

                 int width = 0; int height = 0;

                 Camera.Parameters parameters = camera.getParameters();

                 height = parameters.getPreviewSize().height;

                 width = parameters.getPreviewSize().width;

                 Frame curFrame = createFrameFromData(data, width, height);

                 if(detector != null && detector.isRunning())
                    detector.process(curFrame, ++t);
            }

        });

    }

    private Frame createFrameFromData(byte[] frameData, int width, int height) {
        Frame.ByteArrayFrame frame = new Frame.ByteArrayFrame(frameData, width, height, Frame.COLOR_FORMAT.YUV_NV21);
        // frame.setTargetRotation(rotation);
        return frame;
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // TODO Auto-generated method stub
        //release();

    }

    @Override
    public void onImageResults(List<Face> list, Frame frame, float v)
    {
        detectEmtn(list);
    }

    String getEmotion(){return curEmotion;}
    float getValence(){return curValence;}
    float getEng(){return curEng;}

    private void detectEmtn(List<Face> list)
    {
        String answ;
        if (list == null)
            return;
        if (list.size() == 0) {
            answ = "no face";
        }
        else
        {
            Face face = list.get(0);

            ArrayList<String> emotions = new ArrayList<>();
            ArrayList<Float> data = new ArrayList<>();
            emotions.add("Surprise"); data.add(face.emotions.getSurprise());
            //emotions.add("Valence"); data.add(face.emotions.getValence()); // окраска
            emotions.add("Anger"); data.add(face.emotions.getAnger());
            emotions.add("Contempt"); data.add(face.emotions.getContempt()); //презрение
            emotions.add("Disgust"); data.add(face.emotions.getDisgust());
            //emotions.add("Engagement"); data.add(face.emotions.getEngagement()); // сила
            emotions.add("Fear"); data.add(face.emotions.getFear());
            emotions.add("Joy"); data.add(face.emotions.getJoy());
            emotions.add("Sadness"); data.add(face.emotions.getSadness());

            float max = -0.1f;
            answ = "unknown";

            for(int i = 0; i < emotions.size(); ++i)
                if(abs(data.get(i)) > abs(max))
                {
                    max = data.get(i);
                    answ = emotions.get(i);// + ": " + data.get(i);
                }

            curValence = face.emotions.getValence();
            curEng = face.emotions.getEngagement();
            curEmotion = answ;

            //answ += "\n" + emotions.get(i)+ ": " + String.valueOf(data.get(i));

        }
    }
}