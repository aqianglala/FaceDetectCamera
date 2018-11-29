package com.example.myfacedetectcamera;

import com.example.myfacedetectcamera.model.DataModel;

public interface OnReceivedListener {
    void onReceived(String data);
    void onLoginSuccess();
    void onSendImageSuccess(DataModel data);
}
