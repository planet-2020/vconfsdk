package com.kedacom.vconf.sdk.base.basement;

import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import com.kedacom.vconf.sdk.annotation.Consumer;
import com.kedacom.vconf.sdk.annotation.SerializeEnumAsInt;


@Consumer(SerializeEnumAsInt.class)
final class JsonProcessor {

    private static JsonProcessor instance;

    private GsonBuilder gsonBuilder;
    private Gson gson;  // gson instance is thread-safe.

    private JsonProcessor() {
        gsonBuilder = new GsonBuilder();

        Set<Class<? extends Enum<?>>> serializeEnumAsIntSet = SerializeEnumAsInt$$Generated.serializeEnumAsIntSet;

        for (Class<? extends Enum<?>> c : serializeEnumAsIntSet) {
            regEnumType(c);
        }

        gson = gsonBuilder.create();
    }

    synchronized static JsonProcessor instance() {
        if (null == instance) {
            instance = new JsonProcessor();
        }

        return instance;
    }


    private  <T extends Enum<?>> void regEnumType(Class<T> t) {
        gsonBuilder.registerTypeAdapter(t, (JsonSerializer<T>) (paramT, paramType, paramJsonSerializationContext) -> new JsonPrimitive(paramT.ordinal()));

        gsonBuilder.registerTypeAdapter(t, (JsonDeserializer<T>) (paramJsonElement, paramType, paramJsonDeserializationContext) -> {
            T[] enumConstants = t.getEnumConstants();
            int enumOrder = paramJsonElement.getAsInt();
            if (enumOrder < enumConstants.length)
                return enumConstants[enumOrder];

            return null;
        });
    }


    String toJson(Object obj){
        String json = gson.toJson(obj);
        return "null".equalsIgnoreCase(json) ? null : json;
    }

    <T> T fromJson(String json, Class<T> classOfT){
        return gson.fromJson(json, classOfT);
    }

    boolean isNeedToJson(Object obj){
        if (obj instanceof String
                || obj instanceof StringBuffer
                || obj instanceof Integer
                || obj instanceof Double
                || obj instanceof Float
                || obj instanceof String[]
                || obj instanceof StringBuffer[]
                || obj instanceof Integer[]
                || obj instanceof Double[]
                || obj instanceof Float[]){
            return false;
        }
        return true;
    }
}
