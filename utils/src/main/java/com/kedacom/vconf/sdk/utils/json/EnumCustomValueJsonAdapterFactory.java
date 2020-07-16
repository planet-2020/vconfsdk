package com.kedacom.vconf.sdk.utils.json;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.kedacom.vconf.sdk.utils.lang.PrimitiveTypeHelper;
import com.kedacom.vconf.sdk.utils.log.KLog;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * “枚举实例<-->自定义值”Json适配器工厂。
 * 配合{@link EnumCustomValueStrategy}使用。
 * */
public class EnumCustomValueJsonAdapterFactory implements TypeAdapterFactory {

    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        Class<T> rawType = (Class<T>) type.getRawType();
        if (!rawType.isEnum() || !rawType.isAnnotationPresent(EnumCustomValueStrategy.class)) {
            return null;
        }
        KLog.p("type %s use EnumCustomValueStrategy", rawType);

        Method getValue;
        Class<?> returnType;
        try {
            getValue = rawType.getMethod("getValue");
            getValue.setAccessible(true);
            returnType = getValue.getReturnType();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            return null;
        }
        if (!PrimitiveTypeHelper.isPrimitiveType(returnType)
                && returnType != String.class){
            throw new RuntimeException("EnumCustomValueStrategy: unsupported type " + returnType);
        }

        IWriter writer;
        IReader reader;
        if (PrimitiveTypeHelper.isNumericPrimitiveType(returnType)){
            writer = (jsonWriter, customValue) -> jsonWriter.value((Number) customValue);
            if (int.class == returnType
                    || short.class == returnType
                    || byte.class == returnType){
                reader = JsonReader::nextInt;
            }else if (long.class == returnType){
                reader = JsonReader::nextLong;
            }else {
                reader = JsonReader::nextDouble;
            }
        }else if (boolean.class == returnType){
            writer = (jsonWriter, customValue) -> jsonWriter.value((Boolean) customValue);
            reader = JsonReader::nextBoolean;
        }else{
            writer = (jsonWriter, customValue) -> jsonWriter.value((String) customValue);
            reader = JsonReader::nextString;
        }

        BiMap<T, Object> enumValueMap = HashBiMap.create(); //NOTE: 我们使用BiMap，意味着不同枚举实例的自定义值不能重复。
        T[] enumConstants = rawType.getEnumConstants();
        for (T enumConstant : enumConstants){
            Object customEnumVal = null;
            try {
                customEnumVal = getValue.invoke(enumConstant);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
            enumValueMap.put(enumConstant, customEnumVal);
        }


        return new TypeAdapter<T>(){
            @Override
            public void write(JsonWriter out, T value) throws IOException {
                if (value == null) {
                    out.nullValue();
                } else {
                    writer.write(out, enumValueMap.get(value));
                }
            }

            @Override
            public T read(JsonReader in) throws IOException {
                if (in.peek() == JsonToken.NULL) {
                    in.nextNull();
                    return null;
                } else {
                    Object customEnumVal = reader.read(in);
                    return enumValueMap.inverse().get(customEnumVal);
                }
            }

        };
    }

    private interface IWriter{
        void write(JsonWriter jsonWriter, Object customValue) throws IOException;
    }

    private interface IReader{
        Object read(JsonReader jsonReader) throws IOException;
    }

}
