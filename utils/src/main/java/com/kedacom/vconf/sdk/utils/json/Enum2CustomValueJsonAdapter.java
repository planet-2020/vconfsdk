package com.kedacom.vconf.sdk.utils.json;

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
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Sissi on 2019/7/30
 * “枚举——自定义value”之间Json转换适配器
 *
 * 使用示例：
 * {@code
 * @JsonAdapter(Enum2CustomValueJsonAdapter.class)
 * public enum COLOR {
 *     RED(2), GREEN(4), BLUE(6);
 *
 *     private final int value;
 *     COLOR(int value){
 *         this.value = value;
 *     }
 *
 *     public int getValue() { // NOTE: 使用该适配器枚举中必须定义原型为"${TYPE} getValue()"的方法。${TYPE}为返回值类型，目前支持基本类型和String。
 *         return value;
 *     }
 * }
 * }
 *
 * 这样，对于
 * class Cup{
 *      ....
 *      COLOR color = COLOR.RED;
 * }
 * gson.toJson(new Cup());将输出{..., "color": 2}，而非默认的{..., "color": RED}；
 * 反之gson.fromJson("{..., \"color\": 2}")将得到Cup对象其成员color为COLOR.RED。
 *
 */
public class Enum2CustomValueJsonAdapter implements TypeAdapterFactory {

    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        Class<T> rawType = (Class<T>) type.getRawType();
        if (!rawType.isEnum()) {
            return null;
        }

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
        KLog.p("returnType=%s", returnType);
        if (!PrimitiveTypeHelper.isPrimitiveType(returnType)
                && returnType != String.class){
            throw new RuntimeException("Enum2CustomValueJsonAdapter: unsupported type " + returnType);
        }

        /*自定义Writer/Reader并在创建Adapter前初始化好，这样避免每次序列化/反序列化时做分支比较以提高效率*/
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

        /*事先保存好枚举对象和自定义value的映射关系，避免每次序列化/反序列化时重复查询以提高效率*/
        Map<T, Object> enumValueMap = new HashMap<>();
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

        return new TypeAdapter<T>() {
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
                    for(Map.Entry<T, Object> entry : enumValueMap.entrySet()){
                        if(entry.getValue().equals(customEnumVal)){
                            return entry.getKey(); // 若一个value对应多个key我们返回的是第一个匹配到的
                        }
                    }
                    return null;
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
