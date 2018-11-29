package com.example.myfacedetectcamera;

import android.graphics.Bitmap;
import android.text.format.DateFormat;
import android.util.Log;

import com.example.myfacedetectcamera.model.DataModel;
import com.example.myfacedetectcamera.test.NettyAuth;
import com.example.myfacedetectcamera.utils.Util;
import com.google.gson.Gson;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.Random;

import com.example.myfacedetectcamera.utils.NettyConstantUtils.NettyClientCmd;
import com.example.myfacedetectcamera.utils.NettyConstantUtils.NettyProtoType;

import org.json.JSONException;
import org.json.JSONObject;

public class TcpSocketHelper {
    private String mTag = "TcpSocketHelper";
    private Socket socket;
    private ReceiveThread mReceiveThread;
    private Date lastKeepAliveOkTime;
    private String mIpAddr = "192.168.3.181";
    private int mPort = 10011;
    private boolean isConneting;

    // 每次识别接口返回后才可以继续发送图片
    private LinkedList<Bitmap> mBitmaps = new LinkedList<>();

    private static TcpSocketHelper mInstance = new TcpSocketHelper();

    private TcpSocketHelper() {
    }

    public static TcpSocketHelper getImpl(){
        return mInstance;
    }

    // 缓存区
    private void addBitmap(Bitmap bitmap) {
        if (bitmap != null) {
            if (mBitmaps.size() == 5) {
                mBitmaps.removeFirst();
            }
            mBitmaps.add(bitmap);
        }
    }

    private static boolean isSending;

    /**
     * 开启链接socket
     */
    public void startConnect() {
        if (isConneting) return;
        isConneting = true;
        closeConnect();
        try {
            socket = new Socket(mIpAddr, mPort);
            isConneting = false;
            mReceiveThread = new ReceiveThread(socket);
            mReceiveThread.start();
            Log.e(mTag, "链接成功...");
//            System.out.println("链接成功...");
        } catch (Exception e) {
            isConneting = false;
            Log.e(mTag, "链接出错..." + e.getMessage().toString());
//            System.out.println("链接出错..." + e.getMessage().toString());
            e.printStackTrace();
        }
    }

    /**
     * 关闭链接
     */
    public void closeConnect() {
        if (socket != null) {
            try {
                socket.close();
                socket = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (mReceiveThread != null) {// 终止线程
            mReceiveThread.setExit(false);
        }
        mBitmaps.clear();
        isSending = false;
    }

    /**
     * 保持心跳
     */
    public void KeepAlive() {
        // 判断socket是否已断开,断开就重连
        if (lastKeepAliveOkTime != null) {
//            Log.e(mTag, "上次心跳成功时间:"+ DateFormat.format("yyyy-MM-dd HH:mm:ss", lastKeepAliveOkTime));
            System.out.println("上次心跳成功时间:" + DateFormat.format("yyyy-MM-dd HH:mm:ss", lastKeepAliveOkTime));
            Date now = Calendar.getInstance().getTime();
            long between = (now.getTime() - lastKeepAliveOkTime.getTime());// 得到两者的毫秒数
            if (between > 60 * 1000) {
//                Log.e(mTag, "心跳异常超过1分钟,重新连接:");
                System.out.println("心跳异常超过1分钟,重新连接:");
                lastKeepAliveOkTime = null;
                socket = null;
            }

        } else {
            lastKeepAliveOkTime = Calendar.getInstance().getTime();
        }

        checkConnect();
    }

    /**
     * 重连
     */
    private boolean checkConnect() {
        if (!checkIsAlive()) {
            Log.e(mTag, "链接已断开,重新连接");
            startConnect();
            return false;
        }
        return true;
    }

    /**
     * 此方法是检测是否连接
     */
    private boolean checkIsAlive() {
        return !(socket == null || !socket.isConnected());
    }

    public void sendmessage(String msg) {
        boolean isAlive = checkConnect();
        if (!isAlive)
            return;
        Log.e(mTag, "准备发送消息:" + msg);
//        System.out.println("准备发送消息:" + msg);
        try {
            if (socket != null && socket.isConnected()) {
                if (!socket.isOutputShutdown()) {

                    //2.得到socket读写流
                    OutputStream os = socket.getOutputStream();
                    //true:是否自动flush
                    PrintWriter outStream = new PrintWriter(os, true);
                    outStream.print(msg);
                    outStream.flush();
                }
            }
            Log.e(mTag, "发送成功!");
//            System.out.println("发送成功!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void bingAuth() {
        boolean isAlive = checkConnect();
        if (!isAlive)
            return;
        try {
            if (socket != null && socket.isConnected()) {
                if (!socket.isOutputShutdown()) {

                    NettyAuth auth = new NettyAuth();
                    auth.setClientType(3000);
                    auth.setClientId(Util.getNewMac());

                    String ret = new Gson().toJson(auth);
                    int bodyLength = ret.getBytes().length;

                    int totalLength = 2 + 4 + 4 + 4 + 4 + bodyLength;

                    DataOutputStream out = new DataOutputStream(socket.getOutputStream());
//                    DataInputStream input = new DataInputStream(socket.getInputStream());

                    out.writeInt(totalLength);
                    out.writeShort(NettyProtoType.NETTY_PROTO_JSON_VALUE);
                    out.writeInt(1);//协议
                    out.writeInt(NettyClientCmd.COMMOM_AUTHENTICATION);
                    out.writeInt(new Random().nextInt());
                    out.writeInt(bodyLength);
                    out.write(ret.getBytes());
                    out.flush();

//                    int bodyLenth = input.readInt();
//                    if(bodyLenth>0){
//                        byte[] body = new byte[bodyLenth];
//                        input.mark(4);
//                        input.read(body, 0, bodyLenth);
//
//                        String s = new String(body);
//                        System.err.println(s);
//
//                        setBinded(s.contains("200"));
//                    }
                }
            }
            Log.e(mTag, "发送成功!");
//            System.out.println("发送成功!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendImage(Bitmap bitmap) {
        boolean isAlive = checkConnect();
        if (!isAlive || bitmap == null)
            return;
        try {
            if (socket != null && socket.isConnected()) {
                if (!socket.isOutputShutdown()) {
                    if (isSending) {// 正在发送，加入缓存区
                        addBitmap(bitmap);
                        return;
                    } else {// 检查缓存区
                        if (checkQueue()) return;
                    }
                    isSending = true;

                    NettyAuth auth = new NettyAuth();
                    auth.setClientType(3000);
                    auth.setClientId(Util.getNewMac());

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    //读取图片到ByteArrayOutputStream
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                    byte[] bytes = baos.toByteArray();

                    int bodyLength = bytes.length;

                    int totalLength = 2 + 4 + 4 + 4 + 4 + bodyLength;

                    DataOutputStream out = new DataOutputStream(socket.getOutputStream());

                    out.writeInt(totalLength);
                    out.writeShort(NettyProtoType.NETTY_PROTO_STREAM_VALUE);
                    out.writeInt(1);//协议
                    out.writeInt(NettyClientCmd.EGO_PUSH_IMAGES_STREAM);
                    out.writeInt(new Random().nextInt());
                    out.writeInt(bodyLength);
                    out.write(bytes);
                    out.flush();
                }
            }
            Log.e(mTag, "发送成功!");
//            System.out.println("发送成功!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean checkQueue() {
        if (mBitmaps.size() > 0) {
            sendImage(mBitmaps.removeFirst());
            return true;
        }
        return false;
    }

    /**
     * 设置接收数据监听器
     */
    public void setReceivedListener(OnReceivedListener receivedListener) {
        if (mReceiveThread != null) {
            mReceiveThread.setReceivedListener(receivedListener);
        }
    }

    /**
     * 数据接收线程
     */
    private static class ReceiveThread extends Thread {

        private String mTag = "TcpSocketHelper";
        private DataInputStream isRead;
        private Socket socket;
        private WeakReference<OnReceivedListener> mReceivedListenerRef;
        private boolean isExit = true;

        ReceiveThread(Socket socket) {
            this.socket = socket;
        }

        void setReceivedListener(OnReceivedListener receivedListener) {
            mReceivedListenerRef = new WeakReference<>(receivedListener);
        }

        void setExit(boolean isExit) {
            this.isExit = isExit;
        }

        @Override
        public void run() {
            while (isExit) {
                try {
//                    sleep(500);
//                    Log.i(mTag, "监听中...:"+socket.isConnected());
                    // 判断 Socket 是否处于连接状态
                    if (socket != null && socket.isConnected()) {
                        // 客户端接收服务器端的响应，读取服务器端向客户端的输入流
//                        InputStream isRead = socket.getInputStream();
                        if (isRead == null) {
                            isRead = new DataInputStream(socket.getInputStream());
                        }
                        // 缓冲区
//                        byte[] buffer = new byte[isRead.available()];
//                        // 读取缓冲区
//                        isRead.read(buffer);
                        // available stream to be read
                        int totalLength;
                        short prototype = 0;
                        int version;
                        int command = 0;
                        int token;
                        int contentLength;
                        totalLength = isRead.readInt();
//                        if (isRead.available() > 0) {
//                        }
                        if (isRead.available() > 0) {
                            prototype = isRead.readShort();
                        }
                        if (isRead.available() > 0) {
                            version = isRead.readInt();
                        }
                        if (isRead.available() > 0) {
                            command = isRead.readInt();
                        }
                        if (isRead.available() > 0) {
                            token = isRead.readInt();
                        }
                        if (isRead.available() > 0) {
                            contentLength = isRead.readInt();
                        }
                        if (command == NettyClientCmd.COMMOM_AUTHENTICATION && prototype == NettyProtoType.NETTY_PROTO_JSON_VALUE) {// 认证
                            byte[] buffer = new byte[isRead.available()];
                            isRead.read(buffer);
                            String responseInfo = new String(buffer);
                            JSONObject object = new JSONObject(responseInfo);
                            int responseCode = object.optInt("responseCode");
                            Log.e(mTag, "认证 code: " + responseCode);
                            if (responseCode == 200) {// 认证成功
                                if (mReceivedListenerRef.get() != null) {
                                    mReceivedListenerRef.get().onLoginSuccess();
                                }
                            }
                        }

                        if (command == NettyClientCmd.EGO_PUSH_IMAGES_STREAM && prototype == NettyProtoType.NETTY_PROTO_JSON_VALUE) {// 图片
                            // 上一张图片返回结果后才可以继续传下一张
                            isSending = false;

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
                                    SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");
                                    Log.e(mTag, "识别成功 user: " + data.getUserName() + " time: " + format.format(new Date()));
                                    if (mReceivedListenerRef.get() != null) {
                                        mReceivedListenerRef.get().onSendImageSuccess(data);
                                    }
                                }
                            }
                        }

                        // 解析结果，协议的长度是22
//                        if (buffer.length > 22){
//                            byte[] totalLengthByte = new byte[4];
//                            System.arraycopy(buffer, 0, totalLengthByte, 0, 4);
//                            int totalLength = DataUtils.byteToInt(totalLengthByte);
//                            byte[] prototypeByte = new byte[2];
//                            System.arraycopy(buffer, 4, prototypeByte, 0, 2);
//                            short prototype = DataUtils.byteToShort(prototypeByte);
//                            byte[] versionByte = new byte[4];
//                            System.arraycopy(buffer, 6, versionByte, 0, 4);
//                            int version = DataUtils.byteToInt(versionByte);
//                            byte[] commandByte = new byte[4];
//                            System.arraycopy(buffer, 10, commandByte, 0, 4);
//                            int command = DataUtils.byteToInt(commandByte);
//                            byte[] tokenByte = new byte[4];
//                            System.arraycopy(buffer, 14, tokenByte, 0, 4);
//                            int token = DataUtils.byteToInt(tokenByte);
//                            byte[] contentLengthByte = new byte[4];
//                            System.arraycopy(buffer, 18, contentLengthByte, 0, 4);
//                            int contentLength = DataUtils.byteToInt(contentLengthByte);
//                            int l = buffer.length - 22;
//                            byte[] responseCodeByte = new byte[l];
//                            System.arraycopy(buffer, 22, responseCodeByte, 0, l);
//                            String s = new String(responseCodeByte);
//                            Log.e(mTag, "返回：totalLength: "+ totalLength + " prototype: " + prototype + " version: " + version + " command: " + command + " token: " + token + " response: " + s);
//                        }

                        // 转换为字符串
//                        String responseInfo = new String(buffer);
//                        // 日志中输出
//                        if (responseInfo != null && !responseInfo.equals("")) {
//                            Log.e(mTag, "返回：" + responseInfo);
////                            System.out.println("返回："+responseInfo);
//                            if (mRecivedListener != null) {
//                                mRecivedListener.onReceived(responseInfo);
//                            }
//                        }

//                        lastKeepAliveOkTime = Calendar.getInstance().getTime();
//                        KeepAlive();
                    } else {
                        if (socket != null)
                            Log.e(mTag, "链接状态:" + socket.isConnected());
//                            System.out.println("链接状态:" + socket.isConnected());
                    }

                } catch (IOException e) {
                    Log.e(mTag, "监听出错:ReceiveThread" + e.toString());
//                    System.out.println("监听出错:" + e.toString());
                    e.printStackTrace();
                } catch (JSONException e) {
                    Log.e(mTag, "监听出错:ReceiveThread" + e.toString());
                    e.printStackTrace();
                }
            }
        }
    }

    public static void main(String[] args) {
//        new Thread(new ServerRunnable()).start();
        TcpSocketHelper socketHelper = new TcpSocketHelper();
//        socketHelper.startConnect("192.168.1.162", 10011);
        socketHelper.startConnect();
        socketHelper.bingAuth();
    }
}