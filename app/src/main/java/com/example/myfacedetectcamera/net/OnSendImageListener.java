package com.example.myfacedetectcamera.net;

import com.example.myfacedetectcamera.model.DataModel;

public interface OnSendImageListener {

    void onRecognizeSuccess(DataModel data);

    void onRecognizeFail(String msg);
}
