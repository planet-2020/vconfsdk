package com.kedacom.vconf.sdk.utils.json;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.kedacom.vconf.sdk.utils.log.KLog;

import java.io.IOException;
import java.lang.reflect.Field;

/**
 * “跛子”Json适配器工厂。
 * 配合{@link LameStrategy}使用。
 * */
public class LameJsonAdapterFactory implements TypeAdapterFactory {

    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        Class<T> rawType = (Class<T>) type.getRawType();
        if (!rawType.isAnnotationPresent(LameStrategy.class)) {
            return null;
        }
        Field[] fields = rawType.getDeclaredFields();
        if (2 != fields.length){
            return null;
        }
        Field mainField;
        Field assField;
        Class<?> main = rawType.getAnnotation(LameStrategy.class).mainField();
        if (fields[0].getType() == main){
            mainField = fields[0];
            assField = fields[1];
        }else{
            mainField = fields[1];
            assField = fields[0];
        }

        KLog.p("type %s use LameStrategy", rawType);

        TypeAdapter<T> delegate = gson.getDelegateAdapter(this, type);
        TypeAdapter<JsonElement> jsonElementAdapter = gson.getAdapter(JsonElement.class);

        return new TypeAdapter<T>(){
            @Override
            public void write(JsonWriter out, T value) throws IOException {
                delegate.write(out, value);
            }

            @Override
            public T read(JsonReader in) throws IOException {
                T instanceT = null;
                try {
                    instanceT = rawType.newInstance();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InstantiationException e) {
                    e.printStackTrace();
                }

                try {
                    String mainKey = mainField.getName();
                    String assKey = assField.getName();
                    JsonElement jsonElement = jsonElementAdapter.read(in);
                    JsonObject jsonObject = jsonElement.getAsJsonObject();
                    if (jsonObject.has(mainKey)){ // json 由两部分组成： "MainParam" : {} "AssParam" : {}
                        mainField.set(instanceT, gson.fromJson(jsonObject.getAsJsonObject(mainKey), mainField.getType()));
                        assField.set(instanceT, gson.fromJson(jsonObject.getAsJsonObject(assKey), assField.getType()));
                    }else{ // json 只包含MainParam部分（没有"MainParam" key）：{}
                        mainField.set(instanceT, gson.fromJson(jsonObject, mainField.getType()));
                    }
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }

                return instanceT;
            }
        };
    }

}
