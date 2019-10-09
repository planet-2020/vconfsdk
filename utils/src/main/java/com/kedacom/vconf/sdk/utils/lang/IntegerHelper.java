package com.kedacom.vconf.sdk.utils.lang;

import java.nio.ByteBuffer;

/**
 * Created by Sissi on 2019/9/25
 */
public final class IntegerHelper {
    public static byte[] toByteArray(int a){
        return ByteBuffer.allocate(4).putInt(a).array();
    }
}
