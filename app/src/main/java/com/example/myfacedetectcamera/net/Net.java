package com.example.myfacedetectcamera.net;

import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.util.Log;

import com.example.myfacedetectcamera.activities.FaceDetectRGB4Activity;
import com.example.myfacedetectcamera.test.NettyAuth;
import com.example.myfacedetectcamera.test.RestoreTest;
import com.example.myfacedetectcamera.utils.Util;
import com.google.gson.Gson;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Random;

import com.example.myfacedetectcamera.utils.NettyConstantUtils.NettyClientCmd;
import com.example.myfacedetectcamera.utils.NettyConstantUtils.NettyProtoType;


public class Net {

    public static final String TAG = Net.class.getSimpleName();
    private static final String IP_ADDR = "192.168.3.181";//服务器地址
    private static final int PORT = 10011;//服务器端口号
    private Socket socket = null;
    private boolean binded;

    private Socket getSocket() {
        return socket;
    }

    public void deinit() {
        try {
            if(socket != null){
                socket.close();
            }
            setBinded(false);
        } catch (Exception e) {
            // TODO: handle exception
        }finally {
            socket = null;
        }
    }

    public boolean init(){
        if (socket != null && socket.isConnected()) return true;
        try {
            socket = new Socket(IP_ADDR, PORT);
            System.err.println("socket: isConnected: " + socket.isConnected());
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean isBinded() {
        return binded;
    }

    public void setBinded(boolean binded) {
        this.binded = binded;
    }

    public void bingAuth(){
        try {

            NettyAuth auth = new NettyAuth();
            auth.setClientType(3000);
            auth.setClientId(Util.getNewMac());

//			String ret = JSON.toJSONString(auth);
            String ret = new Gson().toJson(auth);
            int bodyLength = ret.getBytes().length;

            int totalLength = 2 + 4+ 4+4+4 +bodyLength;

            DataOutputStream out = new DataOutputStream(getSocket().getOutputStream());
            DataInputStream input = new DataInputStream(getSocket().getInputStream());

            out.writeInt(totalLength);
            out.writeShort(NettyProtoType.NETTY_PROTO_JSON_VALUE);
            out.writeInt(1);//协议
            out.writeInt(NettyClientCmd.COMMOM_AUTHENTICATION);
            out.writeInt(new Random().nextInt());
            out.writeInt(bodyLength);
            out.write(ret.getBytes());

            int bodyLenth = input.readInt();
            if(bodyLenth>0){
                byte[] body = new byte[bodyLenth];
                input.mark(4);
                input.read(body, 0, bodyLenth);

                String s = new String(body);
                System.err.println(s);

                setBinded(s.contains("200"));
            }

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void sendImage(Bitmap bitmap){
        if (!isBinded() || bitmap == null) return;
        try {

            NettyAuth auth = new NettyAuth();
            auth.setClientType(3000);
            auth.setClientId(Util.getNewMac());

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            //读取图片到ByteArrayOutputStream
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
            byte[] bytes = baos.toByteArray();

            int bodyLength = bytes.length;

            int totalLength = 2 + 4+ 4+4+4 +bodyLength;

            DataOutputStream out = new DataOutputStream(getSocket().getOutputStream());
            DataInputStream input = new DataInputStream(getSocket().getInputStream());

            out.writeInt(totalLength);
            out.writeShort(NettyProtoType.NETTY_PROTO_STREAM_VALUE);
            out.writeInt(1);//协议
            out.writeInt(NettyClientCmd.EGO_PUSH_IMAGES_STREAM);
            out.writeInt(new Random().nextInt());
            out.writeInt(bodyLength);
            out.write(bytes);

            int bodyLenth = input.readInt();
            if(bodyLenth>0){
                byte[] body = new byte[bodyLenth];
                input.mark(4);
                input.read(body, 0, bodyLenth);

                System.err.println(new String(body));
            }

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        Net test = new Net();
        test.init();

        test.bingAuth();
    }
}
