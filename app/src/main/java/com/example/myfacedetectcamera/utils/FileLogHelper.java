package com.example.myfacedetectcamera.utils;

/**
 * Created by 60129 on 2018/7/27.
 */

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by 60129 on 2018/7/6.
 */

public class FileLogHelper {

    private static final String cmdBegin = "logcat -f ";
    private static final boolean shouldLog = true; //TODO: set to false in final version of the app
    private static final String TAG = "FileLogHelper";

    private String logFileAbsolutePath;
    private String cmdEnd = "";//" *:F"
    private boolean isLogStarted = false;
    private static FileLogHelper mInstance;

    private FileLogHelper(){}

    public static FileLogHelper getInstance(){
        if(mInstance == null){
            mInstance = new FileLogHelper();
        }
        return mInstance;
    }

    public void initLog(){
        if(!isLogStarted && shouldLog){
            SimpleDateFormat dF = new SimpleDateFormat("yy-MM-dd_HH_mm''ss", Locale.getDefault());
            String fileName = "logcat_" + dF.format(new Date()) + ".txt";
            File outputFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/IIM/logcat/");
            if(outputFile.mkdirs() || outputFile.isDirectory()){
                logFileAbsolutePath = outputFile.getAbsolutePath() + "/" + fileName;
                startLog();
            }
        }
    }

    private void startLog(){
        if(shouldLog){
            try{
                File prevLogFile = new File(logFileAbsolutePath);
                prevLogFile.delete();
                Runtime.getRuntime().exec(cmdBegin + logFileAbsolutePath + cmdEnd);
                isLogStarted = true;
            }catch(IOException ignored){
                Log.e(TAG, "initLogCat: failed");
            }
        }
    }

    /**
     * Add a new tag to file log.
     *
     * @param tag      The android {@link Log} tag, which should be logged into the file.
     * @param priority The priority which should be logged into the file. Can be V, D, I, W, E, F
     *
     * @see <a href="http://developer.android.com/tools/debugging/debugging-log.html#filteringOutput">Filtering Log Output</a>
     */
    public void addLogTag(String tag, String priority) {
//        String newEntry = " " + tag + ":" + priority;
//        if(!cmdEnd.contains(newEntry)){
//        cmdEnd = newEntry + cmdEnd;
        if (isLogStarted) {
            startLog();
        } else {
            initLog();
//            }
//        }
        }
    }

    /**
     * Add a new tag to file log with default priority, which is Verbose.
     *
     * @param tag The android {@link Log} tag, which should be logged into the file.
     */
    public void addLogTag(String tag){
        addLogTag(tag, "V");
    }

}

