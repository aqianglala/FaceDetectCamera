package com.example.myfacedetectcamera.net;

public interface OnSocketConnectListener {

    void onSocketConnected();

    void onSocketDisconnected(String msg);
}
