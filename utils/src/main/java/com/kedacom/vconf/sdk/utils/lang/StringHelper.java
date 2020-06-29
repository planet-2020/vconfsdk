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

    /**
     * 判断字符串是否相等。
     * @param ignoreCase 是否忽略大小写。true忽略
     * @param ignoreBlank 是否忽略空白符。true忽略
     * */
    public static boolean equals(String str1, String str2, boolean ignoreCase, boolean ignoreBlank){
        if (str1==null && str2==null){
            return true;
        }
        if (str1==null || str2==null){
            return false;
        }
        if (ignoreBlank){
            str1 = str1.trim();
            str2 = str2.trim();
        }
        if (ignoreCase){
            return str1.equalsIgnoreCase(str2);
        }else{
            return str1.equals(str2);
        }
    }

    /**
     * 判断字符串是否为NULL或空。
     * NOTE: 空串仅包含空白字符，如空格、TAB、换行。
     * */
    public static boolean isNullOrBlank(String str){
        return str==null || str.trim().isEmpty();
    }

}
