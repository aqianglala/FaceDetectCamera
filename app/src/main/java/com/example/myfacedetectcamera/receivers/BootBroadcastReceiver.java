package com.example.myfacedetectcamera.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.myfacedetectcamera.MainActivity;
import com.example.myfacedetectcamera.activities.FaceDetectRGB9Activity;


public class BootBroadcastReceiver extends BroadcastReceiver {
    static final String ACTION = "android.intent.action.BOOT_COMPLETED";
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.e("BootBroadcastReceiver", "onReceive");
        if (intent.getAction().equals(ACTION)) {
//            Intent startIntent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
            Intent startIntent = new Intent(context, FaceDetectRGB9Activity.class);
            //下面这句话必须加上才能开机自动运行app的界面
            startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(startIntent);
        }
    }
}