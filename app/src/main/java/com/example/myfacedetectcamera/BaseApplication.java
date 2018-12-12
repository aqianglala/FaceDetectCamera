package com.example.myfacedetectcamera;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.text.TextUtils;
import android.widget.Toast;

import com.example.myfacedetectcamera.utils.FileLogHelper;
import com.example.myfacedetectcamera.utils.SPUtils;
import com.zhy.http.okhttp.OkHttpUtils;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

public class BaseApplication extends Application {

    private static Context context;

    public static Context getContext() {
        return context;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        context = this;
//        if (LeakCanary.isInAnalyzerProcess(this)) {
//            // This process is dedicated to LeakCanary for heap analysis.
//            // You should not init your app in this process.
//            return;
//        }
//        LeakCanary.install(this);

        OkHttpClient okHttpClient = new OkHttpClient.Builder()
//                .addInterceptor(new LoggerInterceptor("TAG"))
                .connectTimeout(10000L, TimeUnit.MILLISECONDS)
                .readTimeout(10000L, TimeUnit.MILLISECONDS)
                //其他配置
                .build();

        OkHttpUtils.initClient(okHttpClient);

//        init();
        // log工具类
        FileLogHelper.getInstance().addLogTag("");
    }


    ArrayList<Activity> list = new ArrayList<Activity>();

    public void init() {
        //设置该CrashHandler为程序的默认处理器
        UnCeHandler catchExcep = new UnCeHandler(this);
        Thread.setDefaultUncaughtExceptionHandler(catchExcep);
    }

    public void removeActivity(Activity a) {
        list.remove(a);
    }

    public void addActivity(Activity a) {
        list.add(a);
    }

    public void finishActivity() {
        for (Activity activity : list) {
            if (null != activity) {
                activity.finish();
            }
        }
        //杀死该应用进程
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    public static String getIp() {
        return (String) SPUtils.get(Constants.KEY_IP, "");
    }
}
