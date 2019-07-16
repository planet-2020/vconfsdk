package com.kedacom.vconf.sdk.base.tools;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.kedacom.vconf.sdk.base.KLog;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * “跛子”Json适配器。
 *
 * Class X{
 *     A MainParam;
 *     B AssParam;
 * }
 * 对于大多数X下层返回的json字符串是如下完整形式：
 * {MainParam: val1, AssParam: val2}
 * 如此可直接使用gson.fromJson解析得到X对象。
 * 但是对于有些X下层返回的json字符串除了上面那种完整形式外还有如下“跛子”形式——缺少AssParam：
 * {val1}
 *
 * 此定制的适配器专门用于后一种X。
 *
 * 泛型参数T对应X（反序列化的目标类），U对应A（json中必有的部分），V对应B（json中可能没有的部分）
 * */
public abstract class LameJsonAdapter<T, U, V> implements JsonDeserializer<T> {

    private static Gson gson = new GsonBuilder().create();

    private Class<T> classT;
    private Field mainField;
    private Field assField;
    private String mainKey;
    private String assKey;

    @Override
    public T deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        if (null == classT) {
            Type[] typeArgs = ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments();
            KLog.p("arg1=%s, arg2=%s, arg3=%s", typeArgs[0], typeArgs[1], typeArgs[2]);
            classT = (Class<T>)typeArgs[0];
            Class<U> classU = (Class<U>)typeArgs[1];
            Class<V> classV = (Class<V>)typeArgs[2];
            Field[] fields = classT.getDeclaredFields();
            for (Field field : fields){
                if (field.getType() == classU){
                    mainField = field;
                    mainField.setAccessible(true);
                    mainKey = field.getName();
                }else if (field.getType() == classV){
                    assField = field;
                    assField.setAccessible(true);
                    assKey = field.getName();
                }
            }
            KLog.p("classT=%s, mainField=%s, assField=%s, mainKey=%s, assKey=%s",
                    classT, mainField, assField, mainKey, assKey);
        }

        T instanceT = null;
        try {
            instanceT = classT.newInstance();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        }

        try {
            JsonObject jsonObject = json.getAsJsonObject();
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

}
