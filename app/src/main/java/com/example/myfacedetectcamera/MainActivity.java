package com.example.myfacedetectcamera;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.example.myfacedetectcamera.activities.FaceDetectRGB4Activity;
import com.example.myfacedetectcamera.activities.FaceDetectRGB5Activity;
import com.example.myfacedetectcamera.activities.FaceDetectRGB6Activity;
import com.example.myfacedetectcamera.activities.FaceDetectRGB7Activity;
import com.example.myfacedetectcamera.activities.FaceDetectRGB8Activity;
import com.example.myfacedetectcamera.activities.FaceDetectRGB9Activity;
import com.example.myfacedetectcamera.activities.ManagerActivity;
import com.example.myfacedetectcamera.activities.RegisterActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.btn_1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, FaceDetectRGB9Activity.class);
                startActivity(intent);
            }
        });
//        findViewById(R.id.btn_3).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Intent intent = new Intent(MainActivity.this, FaceDetectRGB5Activity.class);
//                startActivity(intent);
//            }
//        });
//        findViewById(R.id.btn_4).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Intent intent = new Intent(MainActivity.this, FaceDetectRGB6Activity.class);
//                startActivity(intent);
//            }
//        });
//        findViewById(R.id.btn_5).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Intent intent = new Intent(MainActivity.this, FaceDetectRGB7Activity.class);
//                startActivity(intent);
//            }
//        });
        findViewById(R.id.btn_2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, RegisterActivity.class);
                startActivity(intent);
            }
        });
        findViewById(R.id.btn_manager).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ManagerActivity.class);
                startActivity(intent);
            }
        });
    }
}
