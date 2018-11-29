package com.example.myfacedetectcamera.utils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class DataUtils {

    public static byte[] shortToByteArray(short s) {
        byte[] targets = new byte[2];
        for (int i = 0; i < 2; i++) {
            int offset = (targets.length - 1 - i) * 8;
            targets[i] = (byte) ((s >>> offset) & 0xff);
        }
        return targets;
    }

    public static byte[] intToByteArray(int s) {
//        byte[] targets = new byte[2];
//        for (int i = 0; i < 4; i++) {
//            int offset = (targets.length - 1 - i) * 8;
//            targets[i] = (byte) ((s >>> offset) & 0xff);
//        }
//        return targets;
        return ByteBuffer.allocate(4).putInt(s).array();
    }

    public static int byteToInt(byte[] b) {
        int s = 0;
        int s0 = b[0] & 0xff;// 最低位
        int s1 = b[1] & 0xff;
        int s2 = b[2] & 0xff;
        int s3 = b[3] & 0xff;
        s3 <<= 24;
        s2 <<= 16;
        s1 <<= 8;
        s = s0 | s1 | s2 | s3;
        return s;
    }

    public static short byteToShort(byte[] b) {
        short s = 0;
        short s0 = (short) (b[0] & 0xff);// 最低位
        short s1 = (short) (b[1] & 0xff);
        s1 <<= 8;
        s = (short) (s0 | s1);
        return s;
    }

    public static short shortToBig(short s){
        return ByteBuffer.wrap(DataUtils.shortToByteArray(s)).order(ByteOrder.LITTLE_ENDIAN).getShort();
    }

    public static int intToBig(int i){
        return ByteBuffer.wrap(DataUtils.intToByteArray(i)).order(ByteOrder.LITTLE_ENDIAN).getInt();
    }

    public static byte[] shortToBigByteArray(short s){
        // 转成大端
        short big = shortToBig(s);
        return shortToByteArray(big);
    }

    public static byte[] intToBigByteArray(int i){
        int big = intToBig(i);
        return intToByteArray(big);
    }

    public static byte[] addBytes(byte[] data1, byte[] data2) {
        byte[] data3 = new byte[data1.length + data2.length];
        System.arraycopy(data1, 0, data3, 0, data1.length);
        System.arraycopy(data2, 0, data3, data1.length, data2.length);
        return data3;
    }

}
