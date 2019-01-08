/*
 * Copyright (c) 2018 it.kedacom.com, Inc. All rights reserved.
 */

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

@JsonAdapter(DcsGetUserListRsp.class)
public class DcsGetUserListRsp  implements JsonDeserializer<DcsGetUserListRsp> {

    public TDCSResult MainParam;
    public TDCSGetUserList AssParam;

    @Override
    public DcsGetUserListRsp deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        DcsGetUserListRsp rsp = new DcsGetUserListRsp();
        JsonObject jsonObject = json.getAsJsonObject();
        Gson gson = JsonProcessor.instance().obtainGson();
        if (jsonObject.has("MainParam")){
            rsp.MainParam =  gson.fromJson(jsonObject.getAsJsonObject("MainParam"), TDCSResult.class);
            rsp.AssParam = gson.fromJson(jsonObject.getAsJsonObject("AssParam"), TDCSGetUserList.class);
        }else{
            KLog.p(KLog.WARN,"no AssParam");
            rsp.MainParam = gson.fromJson(json, TDCSResult.class);
        }
        return rsp;
    }

    @Override
    public String toString() {
        return "DcsGetUserListRsp{" +
                "MainParam=" + MainParam +
                ", AssParam=" + AssParam +
                '}';
    }
}
