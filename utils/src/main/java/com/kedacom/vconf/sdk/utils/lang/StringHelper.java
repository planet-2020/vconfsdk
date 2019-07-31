package com.kedacom.vconf.sdk.utils.lang;

/**
 * Created by Sissi on 2019/7/23
 */
public final class StringHelper {

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean isStringCompatible(Class<?> clz){
        return clz==String.class || clz==StringBuffer.class || clz==StringBuilder.class;
    }

    @SuppressWarnings("unchecked")
    public static <T> T convert2CompatibleType(Class<T> dstType, Object srcObj){
        if (!isStringCompatible(dstType)
                || null==srcObj || !isStringCompatible(srcObj.getClass())){
            return null;
        }

        if (srcObj.getClass()==dstType){
            return (T) srcObj;
        }

        if (String.class == dstType){
            return (T) srcObj.toString();
        }

        if (StringBuffer.class == dstType){
            return (T) new StringBuffer(srcObj.toString());
        }

        if (StringBuilder.class == dstType){
            return (T) new StringBuilder(srcObj.toString());
        }

        return null;
    }

}
