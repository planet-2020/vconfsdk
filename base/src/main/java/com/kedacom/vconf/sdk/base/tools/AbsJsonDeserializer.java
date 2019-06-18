/**
 * 定制的Json反序列化适配器。
 *
 * 对于Class A{
 *     MainParam;
 *     AssParam;
 * }
 * 正常情况下下层返回的json字符串应是如下形式：
 * {MainParam: val1, AssParam: val2}
 * 如此可直接使用gson.fromJson解析得到A对象。
 * 但有些情况下(如请求失败)下层返回的json字符串可能如下：
 * {val1}
 *
 * 此定制的适配器专门用于此类场景。
 *
 * */

package com.kedacom.vconf.sdk.base.tools;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.kedacom.vconf.sdk.base.basement.JsonProcessor;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public abstract class AbsJsonDeserializer<T> implements JsonDeserializer<T> {

    private static final String MANKEY = "MainParam";
    private static final String ASSKEY = "AssParam";

    @Override
    public T deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        Type[] types = ((ParameterizedType)getClass().getGenericSuperclass()).getActualTypeArguments();

        T instanceT = null;
        Class<T> classT = (Class<T>)types[0];
        try {
            instanceT = classT.newInstance();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        }

        JsonObject jsonObject = json.getAsJsonObject();
        Gson gson = JsonProcessor.instance().obtainGson();
        try {
            if (jsonObject.has(MANKEY)){ // json 由两部分组成： "MainParam" : {} "AssParam" : {}
                Field fieldMain = classT.getDeclaredField(MANKEY);
                fieldMain.setAccessible(true);
                fieldMain.set(instanceT, gson.fromJson(jsonObject.getAsJsonObject(MANKEY), fieldMain.getType()));
                Field fieldAss = classT.getDeclaredField(ASSKEY);
                fieldAss.setAccessible(true);
                fieldAss.set(instanceT, gson.fromJson(jsonObject.getAsJsonObject(ASSKEY), fieldAss.getType()));
            }else{ // json 只包含MainParam部分（没有"MainParam" key）：{}
                Field fieldMain = classT.getDeclaredField(MANKEY);
                fieldMain.setAccessible(true);
                fieldMain.set(instanceT, gson.fromJson(jsonObject.getAsJsonObject(MANKEY), fieldMain.getType()));
            }
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        return instanceT;
    }

}
