package com.example.myfacedetectcamera.net;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import com.example.myfacedetectcamera.BaseApplication;
import com.example.myfacedetectcamera.model.DataModel;
import com.example.myfacedetectcamera.model.ImageModel;
import com.example.myfacedetectcamera.test.NettyAuth;
import com.example.myfacedetectcamera.utils.NetworkUtil;
import com.example.myfacedetectcamera.utils.Util;
import com.google.gson.Gson;
import com.example.myfacedetectcamera.utils.NettyConstantUtils.NettyClientCmd;
import com.example.myfacedetectcamera.utils.NettyConstantUtils.NettyProtoType;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.LinkedList;

public class SocketManager {

    private String mTag = "SocketManager";
    private SocketManager mInstance;

    private Handler mWriterHandler;
    private int token;
    private Socket mSocket;
    private LinkedList<Bitmap> mBitmaps = new LinkedList<>();
    private boolean isSending;
    private boolean isConnecting;
    private boolean isReConnecting;
    private boolean isBinded;
    private boolean isActiveDisconnect;
    private boolean isThreadRunning;

    private OnSocketConnectListener onSocketConnectListener;
    private OnBindAuthListener onBindAuthListener;
    private OnSendImageListener onSendImageListener;

    public SocketManager() {}

    public void init(){
        isActiveDisconnect = false;
        HandlerThread mWriterThread = new HandlerThread("socket-writer");
        mWriterThread.start();
        mWriterHandler = new Handler(mWriterThread.getLooper());
    }

    public void quit(){
        isActiveDisconnect = true;
        this.onSocketConnectListener = null;
        this.onSendImageListener = null;
        this.onBindAuthListener = null;
        closeConnect();
    }

    public void startConnect(){
        if (isThreadRunning()) return;
        mWriterHandler.post(new Runnable() {
            @Override
            public void run() {
                setThreadRunning(true);
                doConnect();
                setThreadRunning(false);
            }
        });
    }

    public void setOnSocketConnectListener(OnSocketConnectListener listener){
        this.onSocketConnectListener = listener;
    }

//    public void unOnRegisterSocketConnectListener(){
//        this.onSocketConnectListener = null;
//    }

    public void setOnSendImageListener(OnSendImageListener listener){
        this.onSendImageListener = listener;
    }

//    public void unRegisterOnSendImageListener(){
//        this.onSendImageListener = null;
//    }

    public void setOnBindAuthListener(OnBindAuthListener listener){
        this.onBindAuthListener = listener;
    }

//    public void unRegisterOnBindAuthListener(){
//        this.onBindAuthListener = null;
//    }

    private synchronized void doConnect() {
        if (checkIsAlive()) return;
        if (isConnecting) return;
        isConnecting = true;
        try {
            String mIp = "192.168.3.181";
            int mPort = 10011;
            mSocket = new Socket(mIp, mPort);
            if (onSocketConnectListener != null) {
                onSocketConnectListener.onSocketConnected();
            }
            Log.e(mTag, "链接成功...");
        } catch (Exception e) {
            e.printStackTrace();
            if (onSocketConnectListener != null) {
                onSocketConnectListener.onSocketDisconnected(e.getMessage());
            }
            Log.e(mTag, "链接出错..." + e.getMessage());
        } finally {
            isConnecting = false;
            isReConnecting = false;
        }
    }

    private void reconnect(){
        if (isActiveDisconnect) return;
        if (isReConnecting) return;
        isReConnecting = true;
        closeConnect();
        startConnect();
    }

    private void closeConnect() {
//        mWriterHandler.removeCallbacksAndMessages(null);
        if (mSocket != null) {
            try {
                mSocket.close();
                mSocket = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        mBitmaps.clear();
    }

    private synchronized boolean checkIsAlive() {
        if (mSocket == null || !mSocket.isConnected()) return false;
        return true;
    }

    public boolean isThreadRunning() {
        return isThreadRunning;
    }

    public void setThreadRunning(boolean threadRunning) {
        isThreadRunning = threadRunning;
        if (threadRunning){
            Log.e(mTag, "任务开始");
        } else{
            Log.e(mTag, "任务结束");
        }
    }

    /**
     * 业务方法：进行设备认证
     */
    public synchronized void bindAuth() {
        if (isThreadRunning()) return;
        if (!NetworkUtil.isNetworkAvailable(BaseApplication.getContext())) return;
        if (!checkIsAlive()) {
            reconnect();
        }
        mWriterHandler.post(new Runnable() {
            @Override
            public void run() {
                setThreadRunning(true);
                doBindAuth();
                setThreadRunning(false);
            }
        });
    }

    /**
     * 业务方法：发送截取的人脸到服务器，服务器会对图片进行人脸识别
     *
     * @param bitmap   截取的人脸
     */
    public synchronized void sendImage(final Bitmap bitmap) {
        if (isSending){// 更新缓存
            addBitmap(bitmap);
        } else{
            if (!NetworkUtil.isNetworkAvailable(BaseApplication.getContext())) return;

            if (!checkIsAlive()) {
                reconnect();
                return;
            }
            if (!isBinded){
                bindAuth();
                return;
            }
            if (isThreadRunning()) return;
            mWriterHandler.post(new Runnable() {
                @Override
                public void run() {
                    setThreadRunning(true);
                    Bitmap b = checkQueue();
                    if (b != null){
                        addBitmap(bitmap);
                        doSendImage(b);
                    } else{
                        doSendImage(bitmap);
                    }
                    setThreadRunning(false);
                }
            });
        }


    }

    private void doBindAuth() {
        try {
            if (mSocket != null && mSocket.isConnected()) {
                if (!mSocket.isOutputShutdown()) {
                    NettyAuth auth = new NettyAuth();
                    auth.setClientType(3000);
                    auth.setClientId(Util.getNewMac());

                    String ret = new Gson().toJson(auth);
                    int bodyLength = ret.getBytes().length;

                    int totalWriteLength = 2 + 4 + 4 + 4 + 4 + bodyLength;

                    DataOutputStream out = new DataOutputStream(mSocket.getOutputStream());
                    DataInputStream isRead = new DataInputStream(mSocket.getInputStream());

                    out.writeInt(totalWriteLength);
                    out.writeShort(NettyProtoType.NETTY_PROTO_JSON_VALUE);
                    out.writeInt(1);//协议
                    out.writeInt(NettyClientCmd.COMMOM_AUTHENTICATION);
                    if (token >= Integer.MAX_VALUE) {
                        token = 0;
                    }
                    out.writeInt(token++);
                    out.writeInt(bodyLength);
                    out.write(ret.getBytes());

                    int totalLength = isRead.readInt();
                    short prototype = isRead.readShort();
                    int version = isRead.readInt();
                    int command = isRead.readInt();
                    int token = isRead.readInt();
                    int contentLength = isRead.readInt();
                    if (command == NettyClientCmd.COMMOM_AUTHENTICATION && prototype == NettyProtoType.NETTY_PROTO_JSON_VALUE) {// 认证
                        byte[] buffer = new byte[isRead.available()];
                        isRead.read(buffer);
                        String responseInfo = new String(buffer);
                        JSONObject object = new JSONObject(responseInfo);
                        int responseCode = object.optInt("responseCode");
                        Log.e(mTag, "认证 code: " + responseCode);
                        if (responseCode == 200) {// 认证成功
                            isBinded = true;
                            if (onBindAuthListener != null) {
                                onBindAuthListener.onBindSuccess();
                            }
                        } else{
                            isBinded = false;
                            if (onBindAuthListener != null) {
                                onBindAuthListener.onBindFail("code: " + responseCode);
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            closeConnect();
            isBinded = false;
            if (onBindAuthListener != null) {
                onBindAuthListener.onBindFail(e.getMessage());
            }
        } catch (JSONException e) {
            e.printStackTrace();
            isBinded = false;
            if (onBindAuthListener != null) {
                onBindAuthListener.onBindFail(e.getMessage());
            }
        }
    }

    private void doSendImage(Bitmap bitmap) {
        try {
            if (mSocket != null && mSocket.isConnected()) {
                if (!mSocket.isOutputShutdown()) {
                    isSending = true;
                    NettyAuth auth = new NettyAuth();
                    auth.setClientType(3000);
                    auth.setClientId(Util.getNewMac());

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    //读取图片到ByteArrayOutputStream
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                    byte[] bytes = baos.toByteArray();

                    int bodyLength = bytes.length;

                    int totalWriteLength = 2 + 4 + 4 + 4 + 4 + bodyLength;

                    DataOutputStream out = new DataOutputStream(mSocket.getOutputStream());
                    DataInputStream isRead = new DataInputStream(mSocket.getInputStream());

                    out.writeInt(totalWriteLength);
                    out.writeShort(NettyProtoType.NETTY_PROTO_STREAM_VALUE);
                    out.writeInt(1);//协议
                    out.writeInt(NettyClientCmd.EGO_PUSH_IMAGES_STREAM);
                    if (token >= Integer.MAX_VALUE) {
                        token = 0;
                    }
                    out.writeInt(token++);
                    out.writeInt(bodyLength);
                    out.write(bytes);

                    int totalLength = isRead.readInt();
                    short prototype = isRead.readShort();
                    int version = isRead.readInt();
                    int command = isRead.readInt();
                    int token = isRead.readInt();
                    int contentLength = isRead.readInt();

                    if (command == NettyClientCmd.EGO_PUSH_IMAGES_STREAM && prototype == NettyProtoType.NETTY_PROTO_JSON_VALUE) {// 图片
                        // 上一张图片返回结果后才可以继续传下一张

                        byte[] buffer = new byte[isRead.available()];
                        isRead.read(buffer);
                        String responseInfo = new String(buffer);
                        JSONObject object = new JSONObject(responseInfo);
                        int responseCode = object.optInt("responseCode");
                        Log.e(mTag, "识别 code: " + responseCode);
                        if (responseCode == 400) {// 识别成功
                            JSONObject dataObject = object.getJSONObject("data");
                            if (dataObject != null) {
                                DataModel data = new DataModel();
                                String userName = dataObject.optString("userName");
                                String userVoice = dataObject.optString("userVoice");
                                data.setUserName(userName);
                                data.setUserVoice(userVoice);
                                Log.e(mTag, "识别成功 user: " + data.getUserName() + " time: " + Util.formatCurrentTime());
                                if (onSendImageListener != null) {
                                    onSendImageListener.onRecognizeSuccess(data);
                                }
                            }
                        } else {
                            if (onSendImageListener != null) {
                                onSendImageListener.onRecognizeFail("code: " + responseCode);
                            }
                        }
                    }
                    Log.e(mTag, "发送成功!");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            closeConnect();
            if (onSendImageListener != null) {
                onSendImageListener.onRecognizeFail(e.getMessage());
            }
        } catch (JSONException e) {
            e.printStackTrace();
            if (onSendImageListener != null) {
                onSendImageListener.onRecognizeFail(e.getMessage());
            }
        }finally {
            isSending = false;
        }
    }

    /**
     * 运行在当前线程中
     *
     * @return
     */
    private Bitmap checkQueue() {
        if (mBitmaps.size() > 0) {
            return mBitmaps.removeFirst();
        }
        return null;
    }

    private void addBitmap(Bitmap bitmap) {
        if (bitmap != null) {
            if (mBitmaps.size() == 5) {
                mBitmaps.removeFirst();
            }
            mBitmaps.add(bitmap);
        }
    }

}
