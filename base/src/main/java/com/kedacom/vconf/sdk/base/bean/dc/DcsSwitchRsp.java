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

import java.lang.reflect.Type;

@JsonAdapter(DcsSwitchRsp.class)
public class DcsSwitchRsp implements JsonDeserializer<DcsSwitchRsp> {
    public TDCSBoardResult MainParam;
    public TDCSBoardInfo AssParam;

    static Gson gson = new Gson();
    @Override
    public DcsSwitchRsp deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        DcsSwitchRsp boardRsp = new DcsSwitchRsp();
        JsonObject jsonObject = json.getAsJsonObject();
        if (jsonObject.has("MainParam")){
            boardRsp.MainParam =  gson.fromJson(jsonObject.getAsJsonObject("MainParam"), TDCSBoardResult.class);
            boardRsp.AssParam = gson.fromJson(jsonObject.getAsJsonObject("AssParam"), TDCSBoardInfo.class);
        }else{
            KLog.p(KLog.WARN,"no AssParam");
            boardRsp.MainParam = gson.fromJson(json, TDCSBoardResult.class);
        }
        return boardRsp;
    }

    @Override
    public String toString() {
        return "DcsSwitchRsp{" +
                "MainParam=" + MainParam +
                ", AssParam=" + AssParam +
                '}';
    }
}
