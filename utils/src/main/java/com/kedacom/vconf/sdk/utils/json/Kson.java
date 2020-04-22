package com.kedacom.vconf.sdk.utils.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.Map;

/**
 * Created by Sissi on 2019/7/31
 *
 * json 封装类
 */
public final class Kson {
    private static GsonBuilder gsonBuilder = new GsonBuilder();
    private static Gson gson = gsonBuilder.create();

    public static <T> void registerAdapters(Map<Class<T>, Object> adapterMap){
        for (Map.Entry<Class<T>, Object> entry : adapterMap.entrySet()) {
            gsonBuilder.registerTypeAdapter(entry.getKey(), entry.getValue());
        }
        gson = gsonBuilder.create();
    }

    public static String toJson(Object obj){
        return gson.toJson(obj);
    }

    public static <T> T fromJson(String json, Class<T> classOfT) {
        return gson.fromJson(json, classOfT);
    }

}
