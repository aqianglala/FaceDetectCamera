package com.example.myfacedetectcamera.activities;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.PointF;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
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
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.myfacedetectcamera.BaseApplication;
import com.example.myfacedetectcamera.R;
import com.example.myfacedetectcamera.model.DataModel;
import com.example.myfacedetectcamera.model.FaceResult;
import com.example.myfacedetectcamera.net.OnBindAuthListener;
import com.example.myfacedetectcamera.net.OnSendImageListener;
import com.example.myfacedetectcamera.net.OnSocketConnectListener;
import com.example.myfacedetectcamera.net.SocketManagerNew;
import com.example.myfacedetectcamera.utils.CameraErrorCallback;
import com.example.myfacedetectcamera.utils.ImageUtils;
import com.example.myfacedetectcamera.utils.NetworkUtil;
import com.example.myfacedetectcamera.utils.Util;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public final class FaceDetectRGB6Activity extends AppCompatActivity implements SurfaceHolder.Callback,
        Camera.PreviewCallback, TextToSpeech.OnInitListener {

    public static final String TAG = FaceDetectRGB6Activity.class.getSimpleName();

    private Camera mCamera;
    private static int cameraId = 0;
    private static int mDisplayOrientation;

    private static int previewWidth;
    private static int previewHeight;

    // The surface view for the camera data
    private SurfaceView mView;

    // Log all errors:
    private final CameraErrorCallback mErrorCallback = new CameraErrorCallback();


    private static final int MAX_FACE = 1;
//    private boolean isThreadWorking = false;
    private MyHandler handler;
    private static int prevSettingWidth;
    private static int prevSettingHeight;
    private android.media.FaceDetector fdet;

    private FaceResult faces[];
    private int Id = 0;

    private String BUNDLE_CAMERA_ID = "camera";

    private ImageView iv_face;

    private ExecutorService mExecutor = Executors.newCachedThreadPool();

    private Context mContext;
    // 图片格式转换
    private RenderScript rs;
    private ScriptIntrinsicYuvToRGB yuvToRgbIntrinsic;
    private Type.Builder yuvType;
    private Allocation in, out;
    // 权限
    private static final int RC_HANDLE_CAMERA_PERM_RGB = 1;
    private static final int RC_HANDLE_CAMERA_PERM_GRAY = 2;

    private TextToSpeech textToSpeech; // TTS对象
    private boolean isTTSInit;
    private TextView tv_log;
    private ScrollView scrollView;
    private StringBuilder builder = new StringBuilder();
    private SocketManagerNew mSocketManager;
    private static int mDisplayRotation;
    //==============================================================================================
    // Activity Methods
    //==============================================================================================

    /**
     * Initializes the UI and initiates the creation of a face detector.
     */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        Log.e(TAG, "lifecycle: onCreate");
//        BaseApplication application = (BaseApplication)getApplication();
//        application.addActivity(this);

        setContentView(R.layout.activity_camera_viewer6);
        mContext = this;
        // 图片格式转换
        rs = RenderScript.create(this);
        yuvToRgbIntrinsic = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs));

        mView = findViewById(R.id.surfaceview);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        iv_face = findViewById(R.id.iv_face);

        handler = new MyHandler(this);
        faces = new FaceResult[MAX_FACE];
        for (int i = 0; i < MAX_FACE; i++) {
            faces[i] = new FaceResult();
        }

        tv_log = findViewById(R.id.tv_log);
        scrollView = findViewById(R.id.scrollView);

        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Face Detect RGB");

        if (icicle != null)
            cameraId = icicle.getInt(BUNDLE_CAMERA_ID, 0);

        // listener
        MyConnectListener onConnectListener = new MyConnectListener(this);
        MySendImageListener onSendImageListener = new MySendImageListener(this);
        MyOnBindAuthListener onBindAuthListener = new MyOnBindAuthListener(this);
        // 语音
        textToSpeech = new TextToSpeech(this, this);
        // socket
        mSocketManager = new SocketManagerNew();
        mSocketManager.init();
        mSocketManager.setOnSocketConnectListener(onConnectListener);
        mSocketManager.setOnBindAuthListener(onBindAuthListener);
        mSocketManager.setOnSendImageListener(onSendImageListener);
        mSocketManager.startConnect();

        findViewById(R.id.btn_throw).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCamera != null) {
                    mCamera.stopPreview();
                }
                Message message = handler.obtainMessage();
                message.what = 2;
                handler.sendMessageDelayed(message, 5000);
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
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        holder.addCallback(this);
        holder.setFormat(ImageFormat.NV21);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.e(TAG, "lifecycle: onStart");
    }

    /**
     * Restarts the camera.
     */
    @Override
    protected void onResume() {
        super.onResume();
        Log.e(TAG, "lifecycle: onResume");
        startPreview();
    }

    /**
     * Stops the camera.
     */
    @Override
    protected void onPause() {
        super.onPause();
        Log.e(TAG, "lifecycle: onPause");
        if (mCamera != null) {
            mCamera.stopPreview();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.e(TAG, "lifecycle: onStop");
    }

    /**
     * Releases the resources associated with the camera source, the associated detector, and the
     * rest of the processing pipeline.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.e(TAG, "lifecycle: onDestroy");
        if (textToSpeech != null) {
            textToSpeech.stop(); // 不管是否正在朗读TTS都被打断
            textToSpeech.shutdown(); // 关闭，释放资源
        }
        mSocketManager.quit();
//        BaseApplication application = (BaseApplication)getApplication();
//        application.removeActivity(this);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(BUNDLE_CAMERA_ID, cameraId);
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        //Find the total number of cameras available
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        for (int i = 0; i < Camera.getNumberOfCameras(); i++) {
            Camera.getCameraInfo(i, cameraInfo);
            // 优先打开前置摄像头
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                if (cameraId == 0) cameraId = i;
            }
        }

        mCamera = Camera.open(cameraId);

        Camera.getCameraInfo(cameraId, cameraInfo);
        try {
            mCamera.setPreviewDisplay(mView.getHolder());
        } catch (Exception e) {
            Log.e(TAG, "Could not preview the image.", e);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height) {
        Log.e(TAG, "lifecycle: surfaceChanged");
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
        // Let's keep track of the display rotation and orientation also:
        mDisplayRotation = Util.getDisplayRotation(FaceDetectRGB6Activity.this);
        mDisplayOrientation = Util.getDisplayOrientation(mDisplayRotation, cameraId);

        mCamera.setDisplayOrientation(mDisplayOrientation);
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
//        if (previewWidth / 4 > 360) {
//            prevSettingWidth = 360;
//            prevSettingHeight = 270;
//        } else if (previewWidth / 4 > 320) {
//            prevSettingWidth = 320;
//            prevSettingHeight = 240;
//        } else if (previewWidth / 4 > 240) {
//            prevSettingWidth = 240;
//            prevSettingHeight = 160;
//        } else {
//            prevSettingWidth = 160;
//            prevSettingHeight = 120;
//        }

        if (previewWidth / 2 > 480) {
            prevSettingWidth = 480;
        } else if (previewWidth / 2 > 320) {
            prevSettingWidth = 320;
        } else if (previewWidth / 2 > 240) {
            prevSettingWidth = 240;
        } else {
            prevSettingWidth = 160;
        }

        cameraParameters.setPreviewSize(previewSize.width, previewSize.height);
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
//            isThreadWorking = true;
        FaceDetectRunnable detectRunnable = new FaceDetectRunnable(handler, this);
        detectRunnable.setData(_data);
        mExecutor.execute(detectRunnable);

//        }
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = textToSpeech.setLanguage(Locale.CHINA);
            if (result == TextToSpeech.LANG_MISSING_DATA
                    || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(this, "语音数据丢失或不支持", Toast.LENGTH_SHORT).show();
            } else {
                isTTSInit = true;
            }
        }
    }

    /**
     * Do face detect in thread
     */
    private static class FaceDetectRunnable implements Runnable {
        private Handler handler;
        private byte[] data = null;

        private WeakReference<FaceDetectRGB6Activity> mThreadActivityRef;

        FaceDetectRunnable(Handler handler, FaceDetectRGB6Activity activity) {
            this.handler = handler;
            mThreadActivityRef = new WeakReference<>(activity);
        }

        public void setData(byte[] data) {
            this.data = data;
        }

        public void run() {
//            long start = System.currentTimeMillis();

            final FaceDetectRGB6Activity activity = mThreadActivityRef.get();
            if (activity == null) return;

            float aspect = (float) previewHeight / (float) previewWidth;
            int w = prevSettingWidth;
            int h = (int) (prevSettingWidth * aspect);

            // 使用更高效的图片转换方法
            Bitmap bitmap = activity.nv21ToBitmap(data, previewWidth, previewHeight);
            Bitmap bmp = Bitmap.createScaledBitmap(bitmap, w, h, false);
            float xScale = (float) previewWidth / (float) prevSettingWidth;
            float yScale = (float) previewHeight / (float) h;

            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(cameraId, info);
            int rotate = mDisplayOrientation;
            // 如果是手机需要翻转，如果是摄像头则不需要
//            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT && mDisplayRotation % 180 == 0) {
//                if (rotate + 180 > 360) {
//                    rotate = rotate - 180;
//                } else
//                    rotate = rotate + 180;
//            }

            switch (rotate) {
                case 90:
                    bmp = ImageUtils.rotate(bmp, 90);
                    xScale = (float) previewHeight / bmp.getWidth();
                    yScale = (float) previewWidth / bmp.getHeight();
                    break;
                case 180:
                    bmp = ImageUtils.rotate(bmp, 180);
                    break;
                case 270:
                    bmp = ImageUtils.rotate(bmp, 270);
                    xScale = (float) previewHeight / (float) h;
                    yScale = (float) previewWidth / (float) prevSettingWidth;
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

                    mid.x *= xScale;
                    mid.y *= yScale;

                    float eyesDis = fullResults[i].eyesDistance() * xScale;
                    float confidence = fullResults[i].confidence();
                    float pose = fullResults[i].pose(android.media.FaceDetector.Face.EULER_Y);
                    int idFace = activity.Id++;

                    Rect rect = new Rect(
                            (int) (mid.x - eyesDis * 1.20f),
                            (int) (mid.y - eyesDis * 1.40f),
                            (int) (mid.x + eyesDis * 1.20f),
                            (int) (mid.y + eyesDis * 1.85f));

                    // Only detect face size > 64x64
                    if (rect.height() * rect.width() > 64 * 64) {
                        activity.faces[i].setFace(idFace, mid, eyesDis, confidence, pose, System.currentTimeMillis());

                        final Bitmap faceCroped = ImageUtils.cropFaceNew(activity.faces[i], bitmap, rotate);
                        if (faceCroped != null) {
//                            long end = System.currentTimeMillis();
//                            Log.e(TAG, "检测人脸时间：" + (end - start) + "ms");
                            // 将图片发送到服务端
                            if (NetworkUtil.isNetworkAvailable(BaseApplication.getContext())) {
                                activity.mSocketManager.sendImage(faceCroped);
                            }
                            Message msg = Message.obtain(handler);
                            msg.what = 1;
                            msg.obj = faceCroped;
                            handler.sendMessage(msg);
                        }

                    }
                }
            }
        }
    }

    public Bitmap nv21ToBitmap(byte[] nv21, int width, int height) {
        if (yuvType == null) {
            yuvType = new Type.Builder(rs, Element.U8(rs)).setX(nv21.length);
            in = Allocation.createTyped(rs, yuvType.create(), Allocation.USAGE_SCRIPT);

            Type.Builder rgbaType = new Type.Builder(rs, Element.RGBA_8888(rs)).setX(width).setY(height);
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

    private static class MyConnectListener implements OnSocketConnectListener {

        private WeakReference<FaceDetectRGB6Activity> mActivityRef;

        MyConnectListener(FaceDetectRGB6Activity activity) {
            mActivityRef = new WeakReference<>(activity);
        }

        @Override
        public void onSocketConnected() {
        }

        @Override
        public void onSocketDisconnected(final String msg) {
            final FaceDetectRGB6Activity activity = mActivityRef.get();
            if (activity != null) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        activity.appendText("连接失败！" + msg);
                    }
                });
            }
        }
    }

    private static class MyOnBindAuthListener implements OnBindAuthListener {

        private WeakReference<FaceDetectRGB6Activity> mActivityRef;

        MyOnBindAuthListener(FaceDetectRGB6Activity activity) {
            this.mActivityRef = new WeakReference<>(activity);
        }

        @Override
        public void onBindSuccess() {
            final FaceDetectRGB6Activity activity = mActivityRef.get();
            if (activity != null) {
                Log.e(TAG, "认证成功");
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        activity.appendText("认证成功！");
                    }
                });
            }
        }

        @Override
        public void onBindFail(final String msg) {
            final FaceDetectRGB6Activity activity = mActivityRef.get();
            if (activity != null) {
                Log.e(TAG, "认证失败");
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        activity.appendText("认证失败！" + msg);
                    }
                });
            }
        }
    }

    private static class MySendImageListener implements OnSendImageListener {

        private WeakReference<FaceDetectRGB6Activity> mActivityRef;

        MySendImageListener(FaceDetectRGB6Activity activity) {
            mActivityRef = new WeakReference<>(activity);
        }

        @Override
        public void onRecognizeSuccess(final DataModel data) {
            final FaceDetectRGB6Activity activity = mActivityRef.get();
            if (activity != null) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        activity.recognizeSuccess(data);
                    }
                });
            }
        }

        @Override
        public void onRecognizeFail(String msg) {

        }
    }

    private static class MyHandler extends Handler {
        private WeakReference<FaceDetectRGB6Activity> activityWeakReference;

        MyHandler(FaceDetectRGB6Activity activity) {
            this.activityWeakReference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            FaceDetectRGB6Activity activity = activityWeakReference.get();
            if (activity == null) return;
            switch (msg.what) {
                case 1:
                    if (msg.obj != null) {
                        Bitmap bitmap = (Bitmap) msg.obj;
                        activity.iv_face.setImageBitmap(bitmap);
                    }
                    break;
                case 2:
                    activity.startPreview();
                    break;
            }
        }
    }

    private void recognizeSuccess(DataModel data) {
        String userVoice = data.getUserVoice();
        String userName = data.getUserName();
        if (isTTSInit) {
//            if (!TextUtils.isEmpty(userVoice)) {
//                textToSpeech.speak(userVoice, TextToSpeech.QUEUE_FLUSH, null);
//            } else if (!TextUtils.isEmpty(userName)) {
//                textToSpeech.speak(userName, TextToSpeech.QUEUE_FLUSH, null);
//            }
        }
        if (!TextUtils.isEmpty(userName)) {
            appendText(userName + " " + Util.formatCurrentTime());
        }
    }

    private void appendText(String text) {
        builder.append(text);
        builder.append(System.getProperty("line.separator"));
        tv_log.setText(builder.toString());
        scrollView.fullScroll(ScrollView.FOCUS_DOWN);
    }

}
