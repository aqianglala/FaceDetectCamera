package com.example.myfacedetectcamera.utils;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Random;
import java.util.UUID;

public class FileUtils {

    public static File saveToLocal(Bitmap bitmap) {
        //通过UUID生成字符串文件名
        String fileName = UUID.randomUUID().toString() + ".jpg";
        File file = null;
        File dir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/IIM/image/");
        if(dir.mkdirs() || dir.isDirectory()){
            String absolutePath = dir.getAbsolutePath() + "/" + fileName;
            file = new File(absolutePath);
        }
        FileOutputStream out;
        try {
            out = new FileOutputStream(file);
            if (bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)) {
                out.flush();
                out.close();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return file;
    }
}
