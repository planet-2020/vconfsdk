package com.kedacom.vconf.sdk.utils.json;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.kedacom.vconf.sdk.utils.log.KLog;

import java.io.IOException;

/**
 * “枚举实例<-->枚举序号”Json适配器工厂。
 * 配合{@link EnumOrdinalStrategy}使用。
 * */
class EnumOrdinalJsonAdapterFactory implements TypeAdapterFactory {

    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        Class<T> rawType = (Class<T>) type.getRawType();
        if (!rawType.isEnum() || !rawType.isAnnotationPresent(EnumOrdinalStrategy.class)) {
            return null;
        }

        KLog.p("type %s use EnumOrdinalStrategy", rawType);

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
