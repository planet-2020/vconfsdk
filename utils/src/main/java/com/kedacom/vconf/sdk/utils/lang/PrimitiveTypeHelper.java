package com.kedacom.vconf.sdk.utils.lang;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Sissi on 2019/7/23
 */
public final class PrimitiveTypeHelper {
    private final static BiMap<Class<?>, Class<?>> primitiveWrapperMap = HashBiMap.create(9);
    private final static BiMap<Class<?>, Class<?>> numericPrimitiveWrapperMap = HashBiMap.create(6);
    private final static Map<Class<?>, Object> primitiveDefaultValueMap = new HashMap<>(8);

    static {
        numericPrimitiveWrapperMap.put(byte.class, Byte.class);
        numericPrimitiveWrapperMap.put(int.class, Integer.class);
        numericPrimitiveWrapperMap.put(short.class, Short.class);
        numericPrimitiveWrapperMap.put(long.class, Long.class);
        numericPrimitiveWrapperMap.put(float.class, Float.class);
        numericPrimitiveWrapperMap.put(double.class, Double.class);

        primitiveWrapperMap.putAll(numericPrimitiveWrapperMap);
        primitiveWrapperMap.put(char.class, Character.class);
        primitiveWrapperMap.put(boolean.class, Boolean.class);
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

    public static boolean isPrimitiveType(Class<?> clz){
        return primitiveWrapperMap.containsKey(clz);
    }

    public static boolean isPrimitiveWrapperType(Class<?> clz){
        return primitiveWrapperMap.containsValue(clz);
    }

    public static boolean isNumericPrimitiveType(Class<?> clz){
        return numericPrimitiveWrapperMap.containsKey(clz);
    }

    public static boolean isNumericPrimitiveWrapperType(Class<?> clz){
        return numericPrimitiveWrapperMap.containsValue(clz);
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
