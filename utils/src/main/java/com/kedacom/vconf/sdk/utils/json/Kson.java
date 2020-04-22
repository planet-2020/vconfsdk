package com.kedacom.vconf.sdk.utils.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.lang.reflect.Type;
import java.util.Map;

/**
 * Created by Sissi on 2019/7/31
 *
 * json 封装类
 */
public final class Kson {
    private static GsonBuilder gsonBuilder = new GsonBuilder();
    private static Gson gson = gsonBuilder.create();

    public static void registerAdapters(Map<Type, Object> adapters){
        for (Map.Entry<Type, Object> entry : adapters.entrySet()) {
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
