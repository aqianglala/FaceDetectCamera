package com.example.myfacedetectcamera.model;

import android.graphics.Bitmap;

import com.example.myfacedetectcamera.net.OnSendImageListener;

public class ImageModel {
    private Bitmap mBitmap;
    private OnSendImageListener mListener;

    public ImageModel(Bitmap mBitmap, OnSendImageListener mListener) {
        this.mBitmap = mBitmap;
        this.mListener = mListener;
    }

    public Bitmap getmBitmap() {
        return mBitmap;
    }

    public void setmBitmap(Bitmap mBitmap) {
        this.mBitmap = mBitmap;
    }

    public OnSendImageListener getmListener() {
        return mListener;
    }

    public void setmListener(OnSendImageListener mListener) {
        this.mListener = mListener;
    }
}
