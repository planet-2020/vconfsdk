package com.kedacom.vconf.sdk.base.basement;

import java.util.HashMap;
import java.util.Map;

class Utils {
    /**
     * 基本类型——包装类型映射
     * */
    static Map<Class, Class> primitiveWrapperMap = new HashMap<>();
    static {
        primitiveWrapperMap.put(int.class, Integer.class);
        primitiveWrapperMap.put(short.class, Short.class);
        primitiveWrapperMap.put(long.class, Long.class);
        primitiveWrapperMap.put(float.class, Float.class);
        primitiveWrapperMap.put(double.class, Double.class);
        primitiveWrapperMap.put(char.class, Character.class);
        primitiveWrapperMap.put(boolean.class, Boolean.class);
        primitiveWrapperMap.put(byte.class, Byte.class);
    }

    /**
     * 是否为基本类型
     * */
    static boolean isPrimitiveType(Class clz){
        return primitiveWrapperMap.keySet().contains(clz);
    }

    /**
     * 获取基本类型对应的包装类型
     * */
    static Class getPrimitiveWrapperType(Class primitiveClz){
        return primitiveWrapperMap.get(primitiveClz);
    }
}
