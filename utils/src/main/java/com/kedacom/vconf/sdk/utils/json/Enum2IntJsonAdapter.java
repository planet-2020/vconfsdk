package com.kedacom.vconf.sdk.utils.json;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;

/**
 * “枚举——枚举ordinal”Json适配器。枚举序列化为整型(按ordinal)，整型(按ordinal)反序列化为枚举。
 *
 * 使用示例：
 * {@code
 * @JsonAdapter(COLOR.Adapter.class)
 * public enum COLOR {
 *      RED, GREEN, BLUE;
 *
 *     static final class Adapter extends Enum2IntJsonAdapter<COLOR> { }
 * }
 * }
 *
 * 这样，对于
 * class Cup{
 *      ....
 *      COLOR color = COLOR.RED;
 * }
 * gson.toJson(new Cup());将输出{..., "color": 1}，而非默认的{..., "color": RED}；
 * 反之gson.fromJson("{..., \"color\": 1}")将得到Cup对象其成员color为COLOR.RED。
 *
 *
 * NOTE: 若不同枚举转换逻辑都是一样的，则您可以使用{@link Enum2IntJsonAdapterFactory}更加便捷。
 * */
@SuppressWarnings("unchecked")
public abstract class Enum2IntJsonAdapter<T> extends TypeAdapter<T> {

    private Class<T> enumT;

    public Enum2IntJsonAdapter() {
        enumT = (Class<T>)((ParameterizedType)getClass().getGenericSuperclass()).getActualTypeArguments()[0];
        if (!enumT.isEnum()) {
            throw new RuntimeException("Enum2IntJsonAdapter: " + enumT+" is not enum type!");
        }
    }

    @Override
    public void write(JsonWriter out, T value) throws IOException {
        if (value == null) {
            out.nullValue();
        } else {
            out.value(((Enum)value).ordinal());
        }
    }

    @Override
    public T read(JsonReader in) throws IOException {
        if (in.peek() == JsonToken.NULL) {
            in.nextNull();
            return null;
        } else {
            T[] enumConstants = enumT.getEnumConstants();
            int enumOrder = in.nextInt();
            if (enumOrder < enumConstants.length) {
                return enumConstants[enumOrder];
            }else {
                return null;
            }
        }
    }

}
