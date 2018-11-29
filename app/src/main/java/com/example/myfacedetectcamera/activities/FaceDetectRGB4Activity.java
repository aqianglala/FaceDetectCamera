package com.example.myfacedetectcamera.activities;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.PointF;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicYuvToRGB;
import android.renderscript.Type;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.myfacedetectcamera.OnReceivedListener;
import com.example.myfacedetectcamera.R;
import com.example.myfacedetectcamera.TcpSocketHelper;
import com.example.myfacedetectcamera.activities.ui.FaceOverlayView;
import com.example.myfacedetectcamera.model.DataModel;
import com.example.myfacedetectcamera.model.FaceResult;
import com.example.myfacedetectcamera.net.Net;
import com.example.myfacedetectcamera.utils.CameraErrorCallback;
import com.example.myfacedetectcamera.utils.ImageUtils;
import com.example.myfacedetectcamera.utils.Util;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * Created by Nguyen on 5/20/2016.
 */

/**
 * FACE DETECT EVERY FRAME WIL CONVERT TO RGB BITMAP SO THIS HAS LOWER PERFORMANCE THAN GRAY BITMAP
 * COMPARE FPS (DETECT FRAME PER SECOND) OF 2 METHODs FOR MORE DETAIL
 */


public final class FaceDetectRGB4Activity extends AppCompatActivity implements SurfaceHolder.Callback,
        Camera.PreviewCallback, TextToSpeech.OnInitListener {

    // Number of Cameras in device.
    private int numberOfCameras;

    public static final String TAG = FaceDetectRGB4Activity.class.getSimpleName();

    private Camera mCamera;
    private int cameraId = 0;

    // Let's keep track of the display rotation and orientation also:
    private int mDisplayRotation;
    private int mDisplayOrientation;

    private int previewWidth;
    private int previewHeight;

    // The surface view for the camera data
    private SurfaceView mView;

    // Draw rectangles and other fancy stuff:
    private FaceOverlayView mFaceView;

    // Log all errors:
    private final CameraErrorCallback mErrorCallback = new CameraErrorCallback();


    private static final int MAX_FACE = 1;
    private boolean isThreadWorking = false;
    private Handler handler;
    private FaceDetectRunnable detectRunnable = null;
    private int prevSettingWidth;
    private int prevSettingHeight;
    private android.media.FaceDetector fdet;

    private FaceResult faces[];
    private FaceResult faces_previous[];
    private int Id = 0;

    private String BUNDLE_CAMERA_ID = "camera";

    private ImageView iv_face;

    private ExecutorService mExecutor = Executors.newCachedThreadPool();

    private Context mContext;
    // 图片格式转换
    private RenderScript rs;
    private ScriptIntrinsicYuvToRGB yuvToRgbIntrinsic;
    private Type.Builder yuvType, rgbaType;
    private Allocation in, out;
    // 权限
    private static final int RC_HANDLE_CAMERA_PERM_RGB = 1;
    private static final int RC_HANDLE_CAMERA_PERM_GRAY = 2;

    private Net mNet;
    private TcpSocketHelper mSocketHelper;

    private TextToSpeech textToSpeech; // TTS对象
    private boolean isTTSInit;
    private TextView tv_user_voice;
    private TextView tv_user_name;
    private TextView tv_log;
    private ScrollView scrollView;
    private boolean login;// 人证后才能发图片
    private MyReceiveListener mReceiveListener;
    //==============================================================================================
    // Activity Methods
    //==============================================================================================

    /**
     * Initializes the UI and initiates the creation of a face detector.
     */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        login = false;
        setContentView(R.layout.activity_camera_viewer4);
        mContext = this;
        // 图片格式转换
        rs = RenderScript.create(this);
        yuvToRgbIntrinsic = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs));

        mView = findViewById(R.id.surfaceview);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Now create the OverlayView:
        mFaceView = new FaceOverlayView(this);
        addContentView(mFaceView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        // Create and Start the OrientationListener:

        iv_face = findViewById(R.id.iv_face);

        handler = new Handler();
        faces = new FaceResult[MAX_FACE];
        faces_previous = new FaceResult[MAX_FACE];
        for (int i = 0; i < MAX_FACE; i++) {
            faces[i] = new FaceResult();
            faces_previous[i] = new FaceResult();
        }

        tv_user_voice = findViewById(R.id.tv_user_voice);
        tv_user_name = findViewById(R.id.tv_user_name);
        tv_log = findViewById(R.id.tv_log);
        scrollView = findViewById(R.id.scrollView);


        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Face Detect RGB");

        if (icicle != null)
            cameraId = icicle.getInt(BUNDLE_CAMERA_ID, 0);

//        mNet = new Net();
//        // 连接服务器
//        mExecutor.execute(new Runnable() {
//            @Override
//            public void run() {
//                if (mNet.init()) {
//                    mNet.bingAuth();
//                }
//            }
//        });
        textToSpeech = new TextToSpeech(this, this);
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                mSocketHelper = TcpSocketHelper.getImpl();
                mSocketHelper.startConnect();
                mReceiveListener = new MyReceiveListener(FaceDetectRGB4Activity.this);
                mSocketHelper.setReceivedListener(mReceiveListener);
                mSocketHelper.bingAuth();
            }
        });
    }

    private void requestCameraPermission(final int RC_HANDLE_CAMERA_PERM) {
        Log.w(TAG, "Camera permission is not granted. Requesting permission");

        final String[] permissions = new String[]{Manifest.permission.CAMERA};

        ActivityCompat.requestPermissions(this, permissions, RC_HANDLE_CAMERA_PERM);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {

        if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && requestCode == RC_HANDLE_CAMERA_PERM_RGB) {
            initialPreview();
            return;
        }

        if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && requestCode == RC_HANDLE_CAMERA_PERM_GRAY) {
            initialPreview();
            return;
        }

        Log.e(TAG, "Permission not granted: results len = " + grantResults.length +
                " Result code = " + (grantResults.length > 0 ? grantResults[0] : "(empty)"));
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Check for the camera permission before accessing the camera.  If the
        // permission is not granted yet, request permission.
        int rc = ActivityCompat.checkSelfPermission(mContext, Manifest.permission.CAMERA);
        if (rc == PackageManager.PERMISSION_GRANTED) {
            initialPreview();
        } else {
            requestCameraPermission(RC_HANDLE_CAMERA_PERM_RGB);
        }
    }

    private void initialPreview() {
        SurfaceHolder holder = mView.getHolder();
        holder.addCallback(this);
        holder.setFormat(ImageFormat.NV21);
    }


//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        MenuInflater inflater = getMenuInflater();
//        inflater.inflate(R.menu.menu_camera, menu);
//
//        return true;
//    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case android.R.id.home:
                super.onBackPressed();
                return true;

            case R.id.switchCam:

                if (numberOfCameras == 1) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Switch Camera").setMessage("Your device have one camera").setNeutralButton("Close", null);
                    AlertDialog alert = builder.create();
                    alert.show();
                    return true;
                }

                cameraId = (cameraId + 1) % numberOfCameras;
                recreate();

                return true;


            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Restarts the camera.
     */
    @Override
    protected void onResume() {
        super.onResume();

        Log.i(TAG, "onResume");
        startPreview();
    }

    /**
     * Stops the camera.
     */
    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "onPause");
        if (mCamera != null) {
            mCamera.stopPreview();
        }
    }

    /**
     * Releases the resources associated with the camera source, the associated detector, and the
     * rest of the processing pipeline.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 退出时关闭流并关闭socket
//        if (mNet != null) {
//            mNet.deinit();
//        }
        mSocketHelper.closeConnect();
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(BUNDLE_CAMERA_ID, cameraId);
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        //Find the total number of cameras available
        numberOfCameras = Camera.getNumberOfCameras();
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        for (int i = 0; i < Camera.getNumberOfCameras(); i++) {
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                if (cameraId == 0) cameraId = i;
            }
        }

        mCamera = Camera.open(cameraId);

        Camera.getCameraInfo(cameraId, cameraInfo);
        if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            mFaceView.setFront(true);
        }

        try {
            mCamera.setPreviewDisplay(mView.getHolder());
        } catch (Exception e) {
            Log.e(TAG, "Could not preview the image.", e);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height) {
        // We have no surface, return immediately:
        if (surfaceHolder.getSurface() == null) {
            return;
        }
        // Try to stop the current preview:
        try {
            mCamera.stopPreview();
        } catch (Exception e) {
            // Ignore...
        }

        configureCamera(width, height);
        setDisplayOrientation();
        setErrorCallback();

        // Create media.FaceDetector
        float aspect = (float) previewHeight / (float) previewWidth;
        fdet = new android.media.FaceDetector(prevSettingWidth, (int) (prevSettingWidth * aspect), MAX_FACE);


        // Everything is configured! Finally start the camera preview again:
        startPreview();
    }

    private void setErrorCallback() {
        mCamera.setErrorCallback(mErrorCallback);
    }

    private void setDisplayOrientation() {
        // Now set the display orientation:
        mDisplayRotation = Util.getDisplayRotation(FaceDetectRGB4Activity.this);
        mDisplayOrientation = Util.getDisplayOrientation(mDisplayRotation, cameraId);

        mCamera.setDisplayOrientation(mDisplayOrientation);

        if (mFaceView != null) {
            mFaceView.setDisplayOrientation(mDisplayOrientation);
        }
    }

    private void configureCamera(int width, int height) {
        Camera.Parameters parameters = mCamera.getParameters();
        // Set the PreviewSize and AutoFocus:
        setOptimalPreviewSize(parameters, width, height);
        setAutoFocus(parameters);
        // And set the parameters:
        mCamera.setParameters(parameters);
    }

    private void setOptimalPreviewSize(Camera.Parameters cameraParameters, int width, int height) {
        List<Camera.Size> previewSizes = cameraParameters.getSupportedPreviewSizes();
        float targetRatio = (float) width / height;
        Camera.Size previewSize = Util.getOptimalPreviewSize(this, previewSizes, targetRatio);
        previewWidth = previewSize.width;
        previewHeight = previewSize.height;

        Log.e(TAG, "previewWidth" + previewWidth);
        Log.e(TAG, "previewHeight" + previewHeight);

        /**
         * Calculate size to scale full frame bitmap to smaller bitmap
         * Detect face in scaled bitmap have high performance than full bitmap.
         * The smaller image size -> detect faster, but distance to detect face shorter,
         * so calculate the size follow your purpose
         */
        if (previewWidth / 4 > 360) {
            prevSettingWidth = 360;
            prevSettingHeight = 270;
        } else if (previewWidth / 4 > 320) {
            prevSettingWidth = 320;
            prevSettingHeight = 240;
        } else if (previewWidth / 4 > 240) {
            prevSettingWidth = 240;
            prevSettingHeight = 160;
        } else {
            prevSettingWidth = 160;
            prevSettingHeight = 120;
        }

        Log.e(TAG, "prevSettingWidth" + prevSettingWidth);
        Log.e(TAG, "prevSettingHeight" + prevSettingHeight);
        cameraParameters.setPreviewSize(previewSize.width, previewSize.height);

        mFaceView.setPreviewWidth(previewWidth);
        mFaceView.setPreviewHeight(previewHeight);
    }

    private void setAutoFocus(Camera.Parameters cameraParameters) {
        List<String> focusModes = cameraParameters.getSupportedFocusModes();
        if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE))
            cameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
    }

    private void startPreview() {
        if (mCamera != null) {
//            isThreadWorking = false;
            mCamera.startPreview();
            mCamera.setPreviewCallback(this);
            counter = 0;
        }
    }


    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        if (mCamera != null) {
            mCamera.setPreviewCallbackWithBuffer(null);
            mCamera.setErrorCallback(null);
            mCamera.release();
            mCamera = null;
        }
    }


    @Override
    public void onPreviewFrame(byte[] _data, Camera _camera) {
//        if (!isThreadWorking) {
        if (!login) return;
        if (counter == 0)
            start = System.currentTimeMillis();

//            isThreadWorking = true;
        detectRunnable = new FaceDetectRunnable(handler, this);
        detectRunnable.setData(_data);
        mExecutor.execute(detectRunnable);

//        }
    }

    // fps detect face (not FPS of camera)
    long start, end;
    int counter = 0;
    double fps;

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = textToSpeech.setLanguage(Locale.CHINA);
            if (result == TextToSpeech.LANG_MISSING_DATA
                    || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(this, "数据丢失或不支持", Toast.LENGTH_SHORT).show();
            } else {
                isTTSInit = true;
                textToSpeech.speak("可以说话吗", TextToSpeech.QUEUE_FLUSH, null);
            }
        }
    }

    /**
     * Do face detect in thread
     */
    private static class FaceDetectRunnable implements Runnable {
        private Handler handler;
        private byte[] data = null;
        private Bitmap faceCroped;

        private WeakReference<FaceDetectRGB4Activity> mThreadActivityRef;

        public FaceDetectRunnable(Handler handler, FaceDetectRGB4Activity activity) {
            this.handler = handler;
            mThreadActivityRef = new WeakReference<>(activity);
        }

        public void setData(byte[] data) {
            this.data = data;
        }

        public void run() {
            final FaceDetectRGB4Activity activity = mThreadActivityRef.get();
            if (activity == null) return;
//            Log.i("FaceDetectThread", "running");
            long start1 = System.currentTimeMillis();
            float aspect = (float) activity.previewHeight / (float) activity.previewWidth;
            int w = activity.prevSettingWidth;
            int h = (int) (activity.prevSettingWidth * aspect);

//            Bitmap bitmap = Bitmap.createBitmap(previewWidth, previewHeight, Bitmap.Config.RGB_565);
//            // face detection: first convert the image from NV21 to RGB_565
//            YuvImage yuv = new YuvImage(data, ImageFormat.NV21,
//                    bitmap.getWidth(), bitmap.getHeight(), null);
//            // TODO: make rect a member and use it for width and height values above
//            Rect rectImage = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
//
//            // TODO: use a threaded option or a circular buffer for converting streams?
//            //see http://ostermiller.org/convert_java_outputstream_inputstream.html
//            ByteArrayOutputStream baout = new ByteArrayOutputStream();
//            if (!yuv.compressToJpeg(rectImage, 100, baout)) {
//                Log.e("CreateBitmap", "compressToJpeg failed");
//            }
//
//            BitmapFactory.Options bfo = new BitmapFactory.Options();
//            bfo.inPreferredConfig = Bitmap.Config.RGB_565;
//            bitmap = BitmapFactory.decodeStream(
//                    new ByteArrayInputStream(baout.toByteArray()), null, bfo);
//
//            Bitmap bmp = Bitmap.createScaledBitmap(bitmap, w, h, false);
            // 使用更高效的图片转换方法
            Bitmap bitmap = activity.nv21ToBitmap(data, activity.previewWidth, activity.previewHeight);
            Bitmap bmp = Bitmap.createScaledBitmap(bitmap, w, h, false);

            long end1 = System.currentTimeMillis();
            long l = end1 - start1;
//            Log.e(TAG, "nv21 -> scale bitmap time" + l);

            float xScale = (float) activity.previewWidth / (float) activity.prevSettingWidth;
            float yScale = (float) activity.previewHeight / (float) h;

            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(activity.cameraId, info);
            int rotate = activity.mDisplayOrientation;
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT && activity.mDisplayRotation % 180 == 0) {
                if (rotate + 180 > 360) {
                    rotate = rotate - 180;
                } else
                    rotate = rotate + 180;
            }

            switch (rotate) {
                case 90:
                    bmp = ImageUtils.rotate(bmp, 90);
                    xScale = (float) activity.previewHeight / bmp.getWidth();
                    yScale = (float) activity.previewWidth / bmp.getHeight();
                    break;
                case 180:
                    bmp = ImageUtils.rotate(bmp, 180);
                    break;
                case 270:
                    bmp = ImageUtils.rotate(bmp, 270);
                    xScale = (float) activity.previewHeight / (float) h;
                    yScale = (float) activity.previewWidth / (float) activity.prevSettingWidth;
                    break;
            }

            activity.fdet = new android.media.FaceDetector(bmp.getWidth(), bmp.getHeight(), MAX_FACE);

            android.media.FaceDetector.Face[] fullResults = new android.media.FaceDetector.Face[MAX_FACE];
            activity.fdet.findFaces(bmp, fullResults);

            for (int i = 0; i < MAX_FACE; i++) {
                if (fullResults[i] == null) {
                    activity.faces[i].clear();
                } else {
                    PointF mid = new PointF();
                    fullResults[i].getMidPoint(mid);

//                    mid.x *= xScale;
//                    mid.y *= yScale;

//                    float eyesDis = fullResults[i].eyesDistance() * xScale;
                    float eyesDis = fullResults[i].eyesDistance();
                    float confidence = fullResults[i].confidence();
                    float pose = fullResults[i].pose(android.media.FaceDetector.Face.EULER_Y);
//                    int idFace = Id;
                    int idFace = activity.Id++;
                    activity.faces[i].setFace(idFace, mid, eyesDis, confidence, pose, System.currentTimeMillis());

                    faceCroped = ImageUtils.cropFace(activity.faces[i], bmp, rotate);
                    if (faceCroped != null) {
                        // 将图片发送到服务端
//                        mNet.sendImage(faceCroped);
                        activity.mSocketHelper.sendImage(faceCroped);
                        handler.post(new Runnable() {
                            public void run() {
                                activity.iv_face.setImageBitmap(faceCroped);
                            }
                        });
                    }
                }
            }

            handler.post(new Runnable() {
                public void run() {
                    //send face to FaceView to draw rect
                    activity.mFaceView.setFaces(activity.faces);

                    //calculate FPS
                    activity.end = System.currentTimeMillis();
                    activity.counter++;
                    double time = (double) (activity.end - activity.start) / 1000;
                    if (time != 0)
                        activity.fps = activity.counter / time;

                    activity.mFaceView.setFPS(activity.fps);

                    if (activity.counter == (Integer.MAX_VALUE - 1000))
                        activity.counter = 0;

//                    isThreadWorking = false;
                }
            });
        }
    }

    public Bitmap nv21ToBitmap(byte[] nv21, int width, int height) {
        if (yuvType == null) {
            yuvType = new Type.Builder(rs, Element.U8(rs)).setX(nv21.length);
            in = Allocation.createTyped(rs, yuvType.create(), Allocation.USAGE_SCRIPT);

            rgbaType = new Type.Builder(rs, Element.RGBA_8888(rs)).setX(width).setY(height);
            out = Allocation.createTyped(rs, rgbaType.create(), Allocation.USAGE_SCRIPT);
        }

        in.copyFrom(nv21);

        yuvToRgbIntrinsic.setInput(in);
        yuvToRgbIntrinsic.forEach(out);

        Bitmap bmpout = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        out.copyTo(bmpout);
        // argb8888 -> rgb565
        return bmpout.copy(Bitmap.Config.RGB_565, false);

    }

    private static class MyReceiveListener implements OnReceivedListener {

        private final StringBuilder builder;
        private WeakReference<FaceDetectRGB4Activity> mActivityRef;

        MyReceiveListener(FaceDetectRGB4Activity activity) {
            builder = new StringBuilder();
            mActivityRef = new WeakReference<>(activity);
        }

        @Override
        public void onReceived(String data) {

        }

        @Override
        public void onLoginSuccess() {
            final FaceDetectRGB4Activity activity = mActivityRef.get();
            if (activity != null){
                activity.login = true;
            }
        }

        @Override
        public void onSendImageSuccess(final DataModel data) {
            final FaceDetectRGB4Activity activity = mActivityRef.get();
            if (activity != null){
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        String userVoice = data.getUserVoice();
                        String userName = data.getUserName();
//                    if (isTTSInit) {
//                        if (!TextUtils.isEmpty(userVoice)) {
//                            textToSpeech.speak(userVoice, TextToSpeech.QUEUE_FLUSH, null);
//                        } else if (!TextUtils.isEmpty(userName)) {
//                            textToSpeech.speak(userName, TextToSpeech.QUEUE_FLUSH, null);
//                        }
//                    } else {
//                        tv_user_voice.setText(TextUtils.isEmpty(userVoice) ? "" : userVoice);
//                        tv_user_name.setText(TextUtils.isEmpty(userName) ? "" : userName);
//                    }
                        if (!TextUtils.isEmpty(userName)){
                            SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss:SSS");
                            builder.append(userName + " " + format.format(new Date()));
                            builder.append(System.getProperty("line.separator"));
                            activity.tv_log.setText(builder.toString());
                            activity.scrollView.fullScroll(ScrollView.FOCUS_DOWN);
                        }
                    }
                });
            }
        }
    }

}
