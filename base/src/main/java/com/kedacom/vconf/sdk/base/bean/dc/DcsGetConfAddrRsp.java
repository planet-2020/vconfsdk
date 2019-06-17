package com.kedacom.vconf.sdk.base.bean.dc;

import com.google.gson.Gson;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.JsonAdapter;
import com.kedacom.vconf.sdk.base.KLog;
import com.kedacom.vconf.sdk.base.basement.JsonProcessor;

import java.lang.reflect.Type;

@JsonAdapter(DcsGetConfAddrRsp.class)
public class DcsGetConfAddrRsp implements JsonDeserializer<DcsGetConfAddrRsp> { // TODO 用模板类提取公共逻辑，参数三个：目标类DcsGetConfAddrRsp，主参数TDCSResult，次参数TDCSConfAddr
    public TDCSResult MainParam;
    public TDCSConfAddr AssParam;

    @Override
    public DcsGetConfAddrRsp deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        DcsGetConfAddrRsp dcConfAddr = new DcsGetConfAddrRsp();
        JsonObject jsonObject = json.getAsJsonObject();
        Gson gson = JsonProcessor.instance().obtainGson();
        if (jsonObject.has("MainParam")){
            dcConfAddr.MainParam =  gson.fromJson(jsonObject.getAsJsonObject("MainParam"), TDCSResult.class);
            dcConfAddr.AssParam = gson.fromJson(jsonObject.getAsJsonObject("AssParam"), TDCSConfAddr.class);
        }else{
            KLog.p(KLog.WARN,"no AssParam");
            dcConfAddr.MainParam = gson.fromJson(json, TDCSResult.class);
        }
        return dcConfAddr;
    }
}
