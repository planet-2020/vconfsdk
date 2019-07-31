package com.kedacom.vconf.sdk.utils.json;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

/**
 * “枚举——枚举ordinal”Json适配器。枚举序列化为整型(按ordinal)，整型(按ordinal)反序列化为枚举。
 *
 * 使用示例：
 * {@code
 * @JsonAdapter(Enum2IntJsonAdapter.class)
 * public enum COLOR { RED, GREEN, BLUE, }
 * }
 *
 * 这样，对于
 * class Cup{
 *      ....
 *      COLOR color = COLOR.RED;
 * }
 * gson.toJson(new Cup());将输出{..., "color": 1}，而非默认的{..., "color": RED}；
 * 反之gson.fromJson("{..., \"color\": 1}")将得到Cup对象其成员color为COLOR.RED。
 * */
@SuppressWarnings("unchecked")
public final class Enum2IntJsonAdapter implements TypeAdapterFactory {

    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        Class<T> rawType = (Class<T>) type.getRawType();
        if (!rawType.isEnum()) {
            return null;
        }

        return new TypeAdapter<T>() {
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
                    T[] enumConstants = rawType.getEnumConstants();
                    int enumOrder = in.nextInt();
                    if (enumOrder < enumConstants.length) {
                        return enumConstants[enumOrder];
                    }else {
                        return null;
                    }
                }
            }
        };
    }

}
