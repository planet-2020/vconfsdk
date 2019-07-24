package com.kedacom.vconf.sdk.utils.lang;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Sissi on 2019/7/23
 */
public class PrimitiveTypeHelper {
    private static BiMap<Class<?>, Class<?>> primitiveWrapperMap = HashBiMap.create(9);
    private static Map<Class<?>, Object> primitiveDefaultValueMap = new HashMap<>(8);

    static {
        primitiveWrapperMap.put(byte.class, Byte.class);
        primitiveWrapperMap.put(char.class, Character.class);
        primitiveWrapperMap.put(boolean.class, Boolean.class);
        primitiveWrapperMap.put(int.class, Integer.class);
        primitiveWrapperMap.put(short.class, Short.class);
        primitiveWrapperMap.put(long.class, Long.class);
        primitiveWrapperMap.put(float.class, Float.class);
        primitiveWrapperMap.put(double.class, Double.class);
        primitiveWrapperMap.put(void.class, Void.class);

        primitiveDefaultValueMap.put(byte.class, 0);
        primitiveDefaultValueMap.put(char.class, 0);
        primitiveDefaultValueMap.put(boolean.class, false);
        primitiveDefaultValueMap.put(int.class, 0);
        primitiveDefaultValueMap.put(short.class, 0);
        primitiveDefaultValueMap.put(long.class, 0);
        primitiveDefaultValueMap.put(float.class, 0);
        primitiveDefaultValueMap.put(double.class, 0);

    }

    public static Class<?> getWrapperClass(Class<?> primitiveClass){
        return primitiveWrapperMap.get(primitiveClass);
    }

    public static Class<?> getPrimitiveClass(Class<?> wrapperClass){
        return primitiveWrapperMap.inverse().get(wrapperClass);
    }

    public static Object getDefaultValue(Class<?> primitiveClass){
        return primitiveDefaultValueMap.get(primitiveClass);
    }

}
