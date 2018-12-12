package com.example.myfacedetectcamera.activities;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.myfacedetectcamera.BaseApplication;
import com.example.myfacedetectcamera.R;
import com.zhy.http.okhttp.OkHttpUtils;
import com.zhy.http.okhttp.callback.StringCallback;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Request;

public class RegisterActivity extends AppCompatActivity implements OnClickListener {

    public static final String TAG = RegisterActivity.class.getSimpleName();
    private Button bt_take_phone;
    private Button bt_register;
    private ImageView iv_picture;
    private EditText et_name;


    //回调标示
    private static final int TAKE_PHONE = 1;
    private static final int CHOOSE_FORM_ALBUM = 2;
    //权限标示
    private static final int PERMISSION_WRITE_EXTERNAL_STORAGE = 3;
    private Uri imageUri;
    private File outputImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        initView();
        initEvent();
    }

    private void initView() {
        bt_take_phone = findViewById(R.id.bt_take_phone);
        bt_register = findViewById(R.id.bt_register);
        iv_picture = findViewById(R.id.iv_picture);
        et_name = findViewById(R.id.et_name);
    }

    private void initEvent() {
        bt_take_phone.setOnClickListener(this);
        bt_register.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.bt_take_phone:
                takePhone();
                break;
            case R.id.bt_register:
                postImage();
                break;
        }
    }


    //调用相机拍照
    private void takePhone() {
        //创建一个File对象用于存储拍照后的照片
        outputImage = new File(getExternalCacheDir(), "output_image.jpg");
        try {
            if (outputImage.exists()) {
                outputImage.delete();
            }
            outputImage.createNewFile();
        } catch (Exception e) {
            e.printStackTrace();
        }

        //判断Android版本是否是Android7.0以上
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            imageUri = FileProvider.getUriForFile(RegisterActivity.this, "com.lidiwo.cameraalbum.fileprovider", outputImage);
        } else {
            imageUri = Uri.fromFile(outputImage);
        }

        //启动相机程序
        Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        startActivityForResult(intent, TAKE_PHONE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSION_WRITE_EXTERNAL_STORAGE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //用户给予授权，打开相册
                    openAlbum();
                } else {
                    Toast.makeText(RegisterActivity.this, "您没有给予授权", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    private void openAlbum() {
        Intent intent = new Intent("android.intent.action.GET_CONTENT");
        intent.setType("image/*");
        startActivityForResult(intent, CHOOSE_FORM_ALBUM);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case TAKE_PHONE:
                //相机拍照回调
                if (resultCode == RESULT_OK) {
                    try {
                        //将拍摄在照片显示出来
                        Bitmap bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(imageUri));
                        iv_picture.setImageBitmap(bitmap);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                break;
        }
    }

    private void postImage() {
        String userName = et_name.getText().toString().trim();
        if (TextUtils.isEmpty(userName)) {
            Toast.makeText(this, "用户名不能为空！", Toast.LENGTH_SHORT).show();
            return;
        }
        String ip = BaseApplication.getIp();
        if (TextUtils.isEmpty(ip)) {
            Toast.makeText(this, "请先设置ip！", Toast.LENGTH_SHORT).show();
            return;
        }
        if (outputImage != null && outputImage.exists()) {
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
                    .build()
                    .execute(new MyStringCallback());
        } else {
            Toast.makeText(this, "图片为空！", Toast.LENGTH_SHORT).show();
        }
    }

    public class MyStringCallback extends StringCallback {
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
            e.printStackTrace();
            Log.e(TAG, "onError:" + e.getMessage());
        }

        @Override
        public void onResponse(String response, int id) {

            Toast.makeText(RegisterActivity.this, "true".equals(response) ? "注册成功" : "注册失败", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "response:" + response);
        }

        @Override
        public void inProgress(float progress, long total, int id) {
            Log.e(TAG, "inProgress:" + progress);
        }
    }

    //获取照片路径
    private String getImagePath(Uri uri, String selection) {
        String path = null;
        //通过Uri和 selection获取真实图片路径
        Cursor cursor = getContentResolver().query(uri, null, selection, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
            }
            cursor.close();
        }
        return path;
    }

}
