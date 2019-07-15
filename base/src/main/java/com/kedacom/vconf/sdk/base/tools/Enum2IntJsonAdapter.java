package com.kedacom.vconf.sdk.base.tools;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.kedacom.vconf.sdk.base.KLog;

import java.io.IOException;

public class Enum2IntJsonAdapter implements TypeAdapterFactory {

    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        KLog.p("create type=%s", type);
        Class<T> rawType = (Class<T>) type.getRawType();
        if (!rawType.isEnum()) {
            return null;
        }

        return new TypeAdapter<T>() {
            @Override
            public void write(JsonWriter out, T value) throws IOException {
                KLog.p("value=%s", value);
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
                    KLog.p("enumOrder=%s", enumOrder);
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
