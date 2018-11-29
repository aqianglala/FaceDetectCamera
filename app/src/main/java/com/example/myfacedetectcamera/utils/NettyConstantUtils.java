package com.example.myfacedetectcamera.utils;

public class NettyConstantUtils {
	
	public static class NettyDebugCmd{
		public static final int DEBUG_RESTORE = 90001; //恢复出厂
		public static final int DEBUG_QUERY_VERSION = 90002;//查询版本
		public static final int DEBUG_OPEN_AP_MODE = 90003;//开启ap模式
		public static final int DEBUG_STOP_AP_MODE = 90004;//关不ap模式
		
		public static final int DEBUG_OPEN_ONE_JOB = 90005;//开启/创建一个job
		public static final int DEBUG_STOP_ONE_JOB = 90006;//停止/删除一个任务
		public static final int DEBUG_REFRESH_ONE_JOB = 90007;//刷新一个任务
	}
	
	public static class NettyClientType{
		public static final int APP = 1000;
		public static final int EGO = 2000;
		public static final int SMART_EYE = 3000;
		public static final int DEBUG = 90000;
	}
	
	public static class NettyClientCmd{
		
		public static final int COMMOM_AUTHENTICATION = 10;//通用的认证	
		public static final int APP_MONITOR_OPEN = 1001; //打开摄像头操作
		public static final int APP_PULL_MONITOR_STREAM = 1002;//推送视频流
		public static final int APP_WIFI_CONNECT = 1003;//连接WiFi
		public static final int APP_STOP_AP_MODE = 1004;
		
		public static final int EGO_PUSH_IMAGES_STREAM = 2001;//推送图片流
	}
	
	public static class NettyNetCheckTime{
		public static final int NETWORK_CHECK_TIME_OUT = 30;
	}
	
	public static class NettyProtoType{
		public static final int NETTY_PROTO_JSON_VALUE = 1; //json协议组包
		public static final int NETTY_PROTO_STREAM_VALUE = 2;//字符流组包
		public static final int NETTY_PROTO_PROTOBUF_VALUE = 3;//protobuf打包
	}
	
	public static class NettyProtoVersion{
		public static final int NETTY_PROTO_VERSION_1 = 1;
	}
	
	public static class NettyResponseCode{
		public static final int NETTY_SUCEESS = 200;
		public static final int NETTY_ERROR = 201;
		//无权限
		public static final int NETTY_AUTH_FORBIDDEN =202;
		
		public static final int NETTY_RECO_SUCCESS = 400;
		public static final int NETTY_RECO_FAILED = 401;
	}
}
