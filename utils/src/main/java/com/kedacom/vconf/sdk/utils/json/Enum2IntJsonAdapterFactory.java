package com.kedacom.vconf.sdk.utils.json;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;

/**
 * “枚举——枚举ordinal”Json适配器工厂。枚举序列化为整型(按ordinal)，整型(按ordinal)反序列化为枚举。
 *
 * 使用示例：
 * {@code
 * @JsonAdapter(Enum2IntJsonAdapterFactory.class)
 * public enum COLOR {
 *  RED, GREEN, BLUE,
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
 * */
public final class Enum2IntJsonAdapterFactory implements TypeAdapterFactory {

    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        Class<T> rawType = (Class<T>) type.getRawType();
        if (!rawType.isEnum()) {
            return null;
        }
        return new Enum2IntJsonAdapter<T>(){};
    }

}
