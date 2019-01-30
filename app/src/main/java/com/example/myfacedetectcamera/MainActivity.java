package com.example.myfacedetectcamera;

import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.example.myfacedetectcamera.activities.FaceDetectRGBActivity;
import com.example.myfacedetectcamera.activities.ManagerActivity;
import com.example.myfacedetectcamera.activities.RegisterActivity;
import com.example.myfacedetectcamera.activities.WebActivity;
import com.example.myfacedetectcamera.utils.SPUtils;


public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.btn_1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, FaceDetectRGBActivity.class);
                startActivity(intent);
            }
        });
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
        findViewById(R.id.btn_set_ip).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialog();
            }
        });

        findViewById(R.id.btn_user).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String ip = BaseApplication.getIp();
                if (TextUtils.isEmpty(ip)) {
                    Toast.makeText(MainActivity.this, "请先设置ip！", Toast.LENGTH_SHORT).show();
                    return;
                }
                WebActivity.start(MainActivity.this, "http://" + ip + ":7070/App/cache/user/show");
            }
        });

        findViewById(R.id.btn_robot).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String ip = BaseApplication.getIp();
                if (TextUtils.isEmpty(ip)) {
                    Toast.makeText(MainActivity.this, "请先设置ip！", Toast.LENGTH_SHORT).show();
                    return;
                }
                WebActivity.start(MainActivity.this, "http://" + ip + ":7070/App/cache/robot/show");
            }
        });
    }

    private void showDialog() {
        final EditText et = new EditText(this);
        et.setHint("设置ip");

        new AlertDialog.Builder(this).setTitle("设置ip")
                .setView(et)
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                        final String input = et.getText().toString().trim();
                        if (TextUtils.isEmpty(input)) {
                            Toast.makeText(MainActivity.this, "ip为空！", Toast.LENGTH_SHORT).show();
                        } else {
                            if (checkIp(input)) {
                                SPUtils.put(Constants.KEY_IP, input);
                                Toast.makeText(MainActivity.this, "设置成功", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(MainActivity.this, "非法ip！", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private boolean checkIp(String ip) {
        String regex = "([1-9]|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])(\\.(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])){3}";
        return ip.matches(regex);
    }
}
