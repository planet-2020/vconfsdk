package com.sissi.vconfsdk.frame;

import java.lang.reflect.Type;
import java.util.ArrayList;
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

import org.json.JSONException;
import org.json.JSONObject;

@Consumer(SerializeEnumAsInt.class)
final class JsonManager {

    private static JsonManager instance;

    private GsonBuilder gsonBuilder;
    private Gson gson;

    private static ArrayList<Class<? extends Enum>> enumClazzs ;

    private static final String KEY_MTAPI = "mtapi";
    private static final String KEY_HEAD = "head";
    private static final String KEY_BODY = "body";
    private static final String KEY_EVENTNAME = "eventname";
    private static final String KEY_BASE_TYPE = "basetype";

    private JsonManager() {
        gsonBuilder = new GsonBuilder();

        Set<Class> serializeEnumAsIntSet = SerializeEnumAsInt$$Generated.serializeEnumAsIntSet;

        for (Class c : serializeEnumAsIntSet) {
            regEnumType(c);
        }

        gson = gsonBuilder.create();
    }

    synchronized static JsonManager instance() {
        if (null == instance) {
            instance = new JsonManager();
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
        return gson.toJson(obj);
    }

    <T> T fromJson(String json, Class<T> classOfT){
        return gson.fromJson(json, classOfT);
    }

    Object getRootObj(String jsonRsp) throws JSONException{
        return new JSONObject(jsonRsp).getJSONObject(KEY_MTAPI);
    }

    String getRspName(Object rootObj) throws JSONException {
        return ((JSONObject)rootObj).getJSONObject(KEY_HEAD).getString(KEY_EVENTNAME);
    }

    String getRspBody(Object rootObj) throws JSONException {
        return ((JSONObject)rootObj).getString(KEY_BODY);
    }

}
