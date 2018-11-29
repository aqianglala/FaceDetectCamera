package com.example.myfacedetectcamera.test;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.format.DateUtils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Random;


import com.example.myfacedetectcamera.utils.NettyConstantUtils;
import com.example.myfacedetectcamera.utils.NettyConstantUtils.NettyClientCmd;
import com.example.myfacedetectcamera.utils.NettyConstantUtils.NettyClientType;
import com.example.myfacedetectcamera.utils.NettyConstantUtils.NettyDebugCmd;
import com.example.myfacedetectcamera.utils.NettyConstantUtils.NettyProtoType;
import com.google.gson.Gson;

public class RestoreTest {
	
	private static final String IP_ADDR = "192.168.3.181";//服务器地址   
	//private static final String IP_ADDR = "10.42.0.1";//服务器地址
	private static final int PORT = 10011;//服务器端口号    
	private Socket socket = null;   
	
	private Socket getSocket() {
		return socket;
	}
	
	private void deinit() {
		try {
			if(socket != null){
				socket.close();
			}
				
		} catch (Exception e) {
			// TODO: handle exception
		}finally {
			socket = null;
		}
	}
	
	private void init(){
		try {
			socket = new Socket(IP_ADDR, PORT);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void bingTest(){
		try {
			DataOutputStream out = new DataOutputStream(getSocket().getOutputStream());
			out.writeInt(12);
			out.writeInt(NettyClientCmd.COMMOM_AUTHENTICATION);
			out.writeInt(new Random().nextInt());
			out.writeInt(NettyClientType.DEBUG);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void bingEgo(){
		try {
			DataOutputStream out = new DataOutputStream(getSocket().getOutputStream());
			out.writeInt(12);
			out.writeInt(NettyClientCmd.COMMOM_AUTHENTICATION);
			out.writeInt(new Random().nextInt());
			out.writeInt(NettyClientType.EGO);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void bingApp(){
		try {
			DataOutputStream out = new DataOutputStream(getSocket().getOutputStream());
			out.writeInt(12);
			out.writeInt(NettyClientCmd.COMMOM_AUTHENTICATION);
			out.writeInt(new Random().nextInt());
			out.writeInt(NettyClientType.APP);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void restore(){
		try {
			DataOutputStream out = new DataOutputStream(getSocket().getOutputStream());
			out.writeInt(8);
			out.writeInt(NettyConstantUtils.NettyDebugCmd.DEBUG_RESTORE);
			out.writeInt(new Random().nextInt());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void openAPMode(){
		try {
			DataOutputStream out = new DataOutputStream(getSocket().getOutputStream());
			out.writeInt(8);
			out.writeInt(NettyDebugCmd.DEBUG_OPEN_AP_MODE);
			out.writeInt(new Random().nextInt());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void stopAPMode(){
		try {
			DataOutputStream out = new DataOutputStream(getSocket().getOutputStream());
			out.writeInt(8);
			out.writeInt(NettyClientCmd.APP_STOP_AP_MODE);
			out.writeInt(new Random().nextInt());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void getVersion(){
		try {
			DataOutputStream out = new DataOutputStream(getSocket().getOutputStream());
			DataInputStream input = new DataInputStream(getSocket().getInputStream());
			out.writeInt(8);
			out.writeInt(NettyDebugCmd.DEBUG_QUERY_VERSION);
			out.writeInt(new Random().nextInt());

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

	public void connectWifi(){
		String ssid = "iim_robot_test_5g";
		String passwd = "zxzq8888";

		try {
			DataOutputStream out = new DataOutputStream(getSocket().getOutputStream());
			DataInputStream input = new DataInputStream(getSocket().getInputStream());

			int ssidlength = ssid.getBytes().length;
			int pwdlength = passwd.getBytes().length;

			int length = 4+4 + 2 + ssidlength+2+pwdlength;
			out.writeInt(length);
			out.writeInt(NettyClientCmd.APP_WIFI_CONNECT);
			out.writeInt(new Random().nextInt());
			out.writeShort(ssidlength);
			out.write(ssid.getBytes());
			out.writeShort(pwdlength);
			out.write(passwd.getBytes());

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

	public  void openJobTest(){
		try {
			DataOutputStream out = new DataOutputStream(getSocket().getOutputStream());
			DataInputStream input = new DataInputStream(getSocket().getInputStream());

			String jobName = "upgrate_job";
			String jobGroup = "upgrate_job_group";
			String doAction = "xxxxxxxxxxxxxxxx";
//			String execTime = DateUtils.formatTime(System.currentTimeMillis() + 120*1000);
//			System.err.println(execTime);
//			int length = 4 + 4 + 2 +jobName.getBytes().length
//					+2+jobGroup.getBytes().length
//					+2+doAction.getBytes().length
//					+2+execTime.getBytes().length;

//			out.writeInt(length);
			out.writeInt(NettyDebugCmd.DEBUG_OPEN_ONE_JOB);
//			out.writeInt(RandomUtils.nextInt());
			out.writeShort(jobName.getBytes().length);
			out.write(jobName.getBytes());

			out.writeShort(jobGroup.getBytes().length);
			out.write(jobGroup.getBytes());

			out.writeShort(doAction.getBytes().length);
			out.write(doAction.getBytes());

//			out.writeShort(execTime.getBytes().length);
//			out.write(execTime.getBytes());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void bingAuth(){
		try {

			NettyAuth auth = new NettyAuth();
			auth.setClientType(3000);
			auth.setClientId("123456789");

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

				System.err.println(new String(body));
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void sendImage(){
		try {

			NettyAuth auth = new NettyAuth();
			auth.setClientType(3000);
			auth.setClientId("123456789");

//			String ret = JSON.toJSONString(auth);
//			String ret = new Gson().toJson(auth);
//			int bodyLength = ret.getBytes().length;

			byte[] bytes = new byte[]{1,2,3,4};
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
		RestoreTest test = new RestoreTest();
		test.init();
		
		int cmd =6 ;
		
		if(cmd == 1){
			test.bingTest();
			test.restore();
		}else if (cmd == 2) {
			test.bingApp();
			test.connectWifi();
		}else if (cmd == 3) {
			test.bingApp();
			test.stopAPMode();
		}else if (cmd == 4) {
			test.bingTest();
			test.getVersion();
		}else if (cmd == 5) {
			test.bingTest();
			test.openJobTest();
		}else if (cmd == 6) {
			test.bingAuth();
		} else if (cmd == 7){
			test.sendImage();
		}
	}
}
