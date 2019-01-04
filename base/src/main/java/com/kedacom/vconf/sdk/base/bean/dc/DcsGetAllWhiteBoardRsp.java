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

@JsonAdapter(DcsGetAllWhiteBoardRsp.class)
public class DcsGetAllWhiteBoardRsp implements JsonDeserializer<DcsGetAllWhiteBoardRsp> {
    public TDCSResult MainParam;
    public TDCSGetAllBoard AssParam;

    static Gson gson = new Gson();
    @Override
    public DcsGetAllWhiteBoardRsp deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        DcsGetAllWhiteBoardRsp boardRsp = new DcsGetAllWhiteBoardRsp();
        JsonObject jsonObject = json.getAsJsonObject();
        if (jsonObject.has("MainParam")){
            boardRsp.MainParam =  gson.fromJson(jsonObject.getAsJsonObject("MainParam"), TDCSResult.class);
            boardRsp.AssParam = gson.fromJson(jsonObject.getAsJsonObject("AssParam"), TDCSGetAllBoard.class);
        }else{
            KLog.p(KLog.WARN,"no AssParam");
            boardRsp.MainParam = gson.fromJson(json, TDCSResult.class);
        }
        return boardRsp;
    }

    @Override
    public String toString() {
        return "DcsGetAllWhiteBoardRsp{" +
                "MainParam=" + MainParam +
                ", AssParam=" + AssParam +
                '}';
    }
}
