package com.example.myfacedetectcamera.activities;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.PointF;
import android.graphics.Rect;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.provider.Settings;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicYuvToRGB;
import android.renderscript.Type;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
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
import com.example.myfacedetectcamera.net.SocketManager;
import com.example.myfacedetectcamera.utils.CameraErrorCallback;
import com.example.myfacedetectcamera.utils.FileUtils;
import com.example.myfacedetectcamera.utils.ImageUtils;
import com.example.myfacedetectcamera.utils.NetworkUtil;
import com.example.myfacedetectcamera.utils.Util;
import com.zhy.http.okhttp.OkHttpUtils;
import com.zhy.http.okhttp.callback.StringCallback;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.Call;
import okhttp3.Request;


public final class FaceDetectRGBActivity extends AppCompatActivity implements SurfaceHolder.Callback,
        Camera.PreviewCallback, TextToSpeech.OnInitListener {

    public static final String TAG = FaceDetectRGBActivity.class.getSimpleName();
    private Context mContext;
    private long mainThreadId;
    // camera
    private Camera mCamera;
    private static final int MAX_FACE = 1;
    private static int cameraId = 0;
    private static int mDisplayOrientation;
    private boolean isCameraUsable;

    private static int previewWidth;
    private static int previewHeight;
    private static int prevSettingWidth;

    private android.media.FaceDetector fdet;

    private String BUNDLE_CAMERA_ID = "camera";
    private final CameraErrorCallback mErrorCallback = new CameraErrorCallback();

    private FaceResult faces[];
    private int Id = 0;
    // The surface view for the camera data
    private SurfaceView mView;

    //    private boolean isThreadWorking = false;
    private MyHandler handler;
    private ExecutorService mExecutor = Executors.newCachedThreadPool();

    // 图片格式转换
    private RenderScript rs;
    private ScriptIntrinsicYuvToRGB yuvToRgbIntrinsic;
    private Type.Builder yuvType;
    private Allocation in, out;
    // 权限
    private static final int RC_HANDLE_CAMERA_PERM_RGB = 1;
    private static final int REQUEST_EXTERNAL_STORAGE = 2;
    private static String[] PERMISSIONS_STORAGE = {
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE"};
    // 语音
    private TextToSpeech textToSpeech;
    private boolean isTTSInit;
    // view
    private TextView tv_log;
    private ImageView iv_face;
    private ScrollView scrollView;

    private StringBuilder builder = new StringBuilder();
    private SocketManager mSocketManager;
    private static int mDisplayRotation;
    // 注册
    private CountDownTimer mCountDownTimer;
    private boolean isCounting;
    private boolean isRegistering;
    private Bitmap faceBitmap;
    private Object mTag = new Object();
    private int mRotate;
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

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_camera_viewer);

        initView();
        initListener();
        initData(icicle);
    }

    private void initData(Bundle icicle) {
        mContext = this;
        mainThreadId = android.os.Process.myTid();
        handler = new MyHandler(this);
        faces = new FaceResult[MAX_FACE];
        for (int i = 0; i < MAX_FACE; i++) {
            faces[i] = new FaceResult();
        }
        // 图片格式转换
        rs = RenderScript.create(this);
        yuvToRgbIntrinsic = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs));
        // 语音
        textToSpeech = new TextToSpeech(this, this);
        // camera
        if (icicle != null) {
            cameraId = icicle.getInt(BUNDLE_CAMERA_ID, 0);
        }
        // socket
        mSocketManager = new SocketManager();
        mSocketManager.init();
        mSocketManager.setOnSocketConnectListener(new MyConnectListener(this));
        mSocketManager.setOnBindAuthListener(new MyOnBindAuthListener(this));
        mSocketManager.setOnSendImageListener(new MySendImageListener(this));
        mSocketManager.startConnect();
    }

    private void initListener() {
        // 注册
        findViewById(R.id.btn_register).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //检测是否有写的权限
                int permission = ActivityCompat.checkSelfPermission(FaceDetectRGBActivity.this,
                        "android.permission.WRITE_EXTERNAL_STORAGE");
                if (permission != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(FaceDetectRGBActivity.this, PERMISSIONS_STORAGE, REQUEST_EXTERNAL_STORAGE);
                } else {
                    // 开启倒计时
                    countDown();
                }
            }
        });
    }

    private void initView() {
        mView = findViewById(R.id.surfaceview);
        iv_face = findViewById(R.id.iv_face);
        tv_log = findViewById(R.id.tv_log);
        scrollView = findViewById(R.id.scrollView);
    }

    private void countDown() {
        if (!isCounting) {
            if (mCountDownTimer == null) {
                // 弹框
                mCountDownTimer = new CountDownTimer(5000, 1000) {
                    @Override
                    public void onTick(long millisUntilFinished) {
                        isCounting = true;
                        String value = String.valueOf((int) (millisUntilFinished / 1000));
                        showToast(value);
                    }

                    @Override
                    public void onFinish() {
                        // 弹框
                        isCounting = false;
                        showDialog();
                    }
                };
            }
            mCountDownTimer.start();
        } else {
            if (mCountDownTimer != null) {
                mCountDownTimer.cancel();
                isCounting = false;
            }
        }
    }

    private void showDialog() {
        final EditText et = new EditText(this);
        et.setHint("请输入用户名");

        new AlertDialog.Builder(this).setTitle("使用当前人脸注册？")
                .setView(et)
                .setPositiveButton("上传", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        toggleSoftInput();

                        final String input = et.getText().toString();
                        if (input.equals("")) {
                            showToast("用户名不能为空！");
                            isRegistering = false;
                        } else {
                            if (faceBitmap != null) {
                                mExecutor.execute(new Runnable() {
                                    @Override
                                    public void run() {
                                        faceBitmap = ImageUtils.rotate(faceBitmap, mRotate);
                                        File file = FileUtils.saveToLocal(faceBitmap);
                                        if (!faceBitmap.isRecycled()) {
                                            faceBitmap.recycle();
                                            faceBitmap = null;
                                        }
                                        if (file != null && file.exists()) {
                                            postImage(input, file);
                                        } else {
                                            isRegistering = false;
                                            showToast("文件不存在！");
                                        }
                                    }
                                });
                            } else {
                                isRegistering = false;
                                showToast("没有检测到人脸！");
                            }
                        }
                    }
                })
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        isRegistering = false;
                        toggleSoftInput();
                    }
                })
                .setCancelable(false)
                .show();
        isRegistering = true;
    }

    private void toggleSoftInput() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS);
    }

    private void showToast(final String text) {
        long currentThreadId = android.os.Process.myTid();
        if (currentThreadId != mainThreadId) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(FaceDetectRGBActivity.this, text, Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Toast.makeText(FaceDetectRGBActivity.this, text, Toast.LENGTH_SHORT).show();
        }
    }

    private void postImage(String userName, File outputImage) {
        String ip = BaseApplication.getIp();
        if (TextUtils.isEmpty(ip)) {
            showToast("请先设置ip！");
            return;
        }
        Map<String, String> params = new HashMap<>();
        params.put("userName", userName);
        params.put("effectiveDate", "2018-11-27 00:00:00");
        params.put("threshold", "4.5");
        params.put("authorize", "1");
        params.put("userType", "5");
        OkHttpUtils
                .post()
                .url("http://" + ip + ":7070/App/user/register")
                .params(params)
                .addFile("image", "output_image.jpg", outputImage)
                .tag(mTag)
                .build()
                .execute(new MyStringCallback(outputImage));
    }

    public class MyStringCallback extends StringCallback {
        private File fileImage;

        MyStringCallback(File fileImage) {
            this.fileImage = fileImage;
        }

        @Override
        public void onBefore(Request request, int id) {
            Log.e(TAG, "onBefore:");
        }

        @Override
        public void onAfter(int id) {
            Log.e(TAG, "onAfter:");
        }

        @Override
        public void onError(Call call, Exception e, int id) {
            Log.e(TAG, "onError:" + e.getMessage());
            e.printStackTrace();
            isRegistering = false;
            showToast("注册出错" + e.getMessage());
            deleteFile();
        }

        @Override
        public void onResponse(String response, int id) {
            Log.e(TAG, "response:" + response);
            isRegistering = false;
            showToast("true".equals(response) ? "注册成功" : "注册失败");
            deleteFile();
        }

        @Override
        public void inProgress(float progress, long total, int id) {
            Log.e(TAG, "inProgress:" + progress);
        }

        private void deleteFile() {
            mExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    if (fileImage != null && fileImage.exists()) {
                        fileImage.delete();
                    }
                }
            });
        }
    }

    private void requestCameraPermission() {
        Log.w(TAG, "Camera permission is not granted. Requesting permission");

        final String[] permissions = new String[]{Manifest.permission.CAMERA};

        ActivityCompat.requestPermissions(this, permissions, RC_HANDLE_CAMERA_PERM_RGB);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (requestCode == RC_HANDLE_CAMERA_PERM_RGB) {
                initialPreview();
            } else if (requestCode == REQUEST_EXTERNAL_STORAGE) {
                countDown();
            }
        }
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
            requestCameraPermission();
        }
        // 加入后台白名单，否则activity可能会被回收
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent();
            String packageName = getPackageName();
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + packageName));
                startActivity(intent);
            }
        }
    }

    private void initialPreview() {
        SurfaceHolder holder = mView.getHolder();
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
    }

    /**
     * Stops the camera.
     */
    @Override
    protected void onPause() {
        super.onPause();
        Log.e(TAG, "lifecycle: onPause");
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
            textToSpeech = null;
        }
        mSocketManager.quit();
        if (mCountDownTimer != null) {
            mCountDownTimer.cancel();
        }
        // 取消请求
        OkHttpUtils.getInstance().cancelTag(mTag);
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
        try {
            mCamera = Camera.open(cameraId);
            isCameraUsable = true;
        } catch (Exception e) {
            mCamera = null;
            showToast("相机不可用！");
            return;
        }

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
        if (!isCameraUsable || mCamera == null) return;
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
        mDisplayRotation = Util.getDisplayRotation(FaceDetectRGBActivity.this);
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
        // 识别距离取决于人脸检测图片的大小
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
            // 将onPause中的stopPreview方法移到这里，onPause和onResume中不需要stop和start
            mCamera.stopPreview();
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
        if (_data == null) return;
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
                showToast("语音数据丢失或不支持");
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

        private WeakReference<FaceDetectRGBActivity> mThreadActivityRef;

        FaceDetectRunnable(Handler handler, FaceDetectRGBActivity activity) {
            this.handler = handler;
            mThreadActivityRef = new WeakReference<>(activity);
        }

        public void setData(byte[] data) {
            this.data = data;
        }

        public void run() {
//            long start = System.currentTimeMillis();

            final FaceDetectRGBActivity activity = mThreadActivityRef.get();
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
            if (!isTablet(activity) && info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT && mDisplayRotation % 180 == 0) {
                if (rotate + 180 > 360) {
                    rotate = rotate - 180;
                } else {
                    rotate = rotate + 180;
                }
            }
            activity.mRotate = rotate;
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
                            // 注册
                            if (activity.isCounting) {
                                activity.faceBitmap = bitmap;
                            }

                            // 将图片发送到服务端
                            if (NetworkUtil.isNetworkAvailable(BaseApplication.getContext())) {
                                activity.mSocketManager.sendImage(faceCroped);
                            }
                            if (!activity.isRegistering) {
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
    }

    public synchronized Bitmap nv21ToBitmap(byte[] nv21, int width, int height) {
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

        private WeakReference<FaceDetectRGBActivity> mActivityRef;

        MyConnectListener(FaceDetectRGBActivity activity) {
            mActivityRef = new WeakReference<>(activity);
        }

        @Override
        public void onSocketConnected() {
        }

        @Override
        public void onSocketDisconnected(final String msg) {
            final FaceDetectRGBActivity activity = mActivityRef.get();
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

        private WeakReference<FaceDetectRGBActivity> mActivityRef;

        MyOnBindAuthListener(FaceDetectRGBActivity activity) {
            this.mActivityRef = new WeakReference<>(activity);
        }

        @Override
        public void onBindSuccess() {
            final FaceDetectRGBActivity activity = mActivityRef.get();
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
            final FaceDetectRGBActivity activity = mActivityRef.get();
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

        private WeakReference<FaceDetectRGBActivity> mActivityRef;

        MySendImageListener(FaceDetectRGBActivity activity) {
            mActivityRef = new WeakReference<>(activity);
        }

        @Override
        public void onRecognizeSuccess(final DataModel data) {
            final FaceDetectRGBActivity activity = mActivityRef.get();
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
        private WeakReference<FaceDetectRGBActivity> activityWeakReference;

        MyHandler(FaceDetectRGBActivity activity) {
            this.activityWeakReference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            FaceDetectRGBActivity activity = activityWeakReference.get();
            if (activity == null) return;
            switch (msg.what) {
                case 1:
                    if (msg.obj != null) {
                        Bitmap bitmap = (Bitmap) msg.obj;
                        activity.iv_face.setImageBitmap(bitmap);
                    }
                    break;
            }
        }
    }

    private void recognizeSuccess(DataModel data) {
        String userVoice = data.getUserVoice();
        String userName = data.getUserName();
        if (isTTSInit && textToSpeech != null) {
            if (!TextUtils.isEmpty(userVoice)) {
                textToSpeech.speak(userVoice, TextToSpeech.QUEUE_FLUSH, null);
            } else if (!TextUtils.isEmpty(userName)) {
                textToSpeech.speak(userName, TextToSpeech.QUEUE_FLUSH, null);
            }
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

    /**
     * 程序运行在A64板上，是一个平板，因此可以使用此办法区分是手机还是平板
     */
    public static boolean isTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK)
                >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }
}
