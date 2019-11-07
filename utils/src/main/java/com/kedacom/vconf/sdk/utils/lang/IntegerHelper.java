package com.kedacom.vconf.sdk.utils.lang;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Created by Sissi on 2019/9/25
 */
public final class IntegerHelper {
    public static byte[] toByteArray(int a){
        return ByteBuffer.allocate(4).putInt(a).array();
    }

    public static byte[] toLittleEndianByteArray(int a){
        return ByteBuffer.allocate(4).putInt(a).order(ByteOrder.LITTLE_ENDIAN).array();
    }

    public static byte[] toBigEndianByteArray(int a){
        return ByteBuffer.allocate(4).putInt(a).order(ByteOrder.BIG_ENDIAN).array();
    }
}
