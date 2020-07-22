package com.kedacom.vconf.sdk.utils.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Created by Sissi on 2019/7/31
 *
 * json 封装类
 */
public final class Kson {
    private static Gson gson;

    static {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapterFactory(new EnumOrdinalJsonAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new EnumCustomValueJsonAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new LameJsonAdapterFactory());
        gson = gsonBuilder.create();
    }

    public static String toJson(Object obj){
        return gson.toJson(obj);
    }

    public static <T> T fromJson(String json, Class<T> classOfT) {
        return gson.fromJson(json, classOfT);
    }

}
