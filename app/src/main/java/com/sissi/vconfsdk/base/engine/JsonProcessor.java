package com.sissi.vconfsdk.base.engine;

import java.lang.reflect.Type;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.sissi.annotation.Consumer;
import com.sissi.annotation.SerializeEnumAsInt;


/***
 *
 * Json处理器
 * */
@Consumer(SerializeEnumAsInt.class)
final class JsonProcessor {

    private static JsonProcessor instance;

    private GsonBuilder gsonBuilder;
    private Gson gson;

    private JsonProcessor() {
        gsonBuilder = new GsonBuilder();

        Set<Class> serializeEnumAsIntSet = SerializeEnumAsInt$$Generated.serializeEnumAsIntSet;

        for (Class c : serializeEnumAsIntSet) {
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


    private  <T extends Enum<T>> void regEnumType(final Class<T> t) {
        gsonBuilder.registerTypeAdapter(t, new JsonSerializer<T>() {

            @Override
            public JsonElement serialize(T paramT, Type paramType,
                    JsonSerializationContext paramJsonSerializationContext) {
                return new JsonPrimitive(paramT.ordinal());
            }
        });

        gsonBuilder.registerTypeAdapter(t, new JsonDeserializer<T>() {

            @Override
            public T deserialize(JsonElement paramJsonElement, Type paramType,
                    JsonDeserializationContext paramJsonDeserializationContext) throws JsonParseException {
                T[] enumConstants = t.getEnumConstants();
                int enumOrder = paramJsonElement.getAsInt();
                if (enumOrder < enumConstants.length)
                    return enumConstants[enumOrder];

                return null;
            }

        });
    }


    Gson obtainGson() {
        return gson;
    }

    String toJson(Object obj){
        String json = gson.toJson(obj);
        return "null".equalsIgnoreCase(json) ? null : json;
    }

    <T> T fromJson(String json, Class<T> classOfT){
        return gson.fromJson(json, classOfT);
    }

}
