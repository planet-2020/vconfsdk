package com.kedacom.vconf.sdk.utils.json;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;

/**
 * Created by Sissi on 2019/7/30
 * “枚举——自定义value”之间Json转换适配器
 *
 * 使用示例：
 * {@code
 * @JsonAdapter(Enum2CustomValueJsonAdapterFactory.class)
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
public final class Enum2CustomValueJsonAdapterFactory implements TypeAdapterFactory {

    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        Class<T> rawType = (Class<T>) type.getRawType();
        if (!rawType.isEnum()) {
            return null;
        }
        return new Enum2CustomValueJsonAdapter<T>(){};
    }

}
