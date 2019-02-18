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


@JsonAdapter(DcsNewWhiteBoardRsp.class)
public class DcsNewWhiteBoardRsp  implements JsonDeserializer<DcsNewWhiteBoardRsp> {
    public TDCSBoardResult MainParam;
    public TDCSBoardInfo AssParam;

    public DcsNewWhiteBoardRsp() {
        MainParam = new TDCSBoardResult();
        AssParam = new TDCSBoardInfo();
    }

    @Override
    public DcsNewWhiteBoardRsp deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        DcsNewWhiteBoardRsp newWhiteBoardRsp = new DcsNewWhiteBoardRsp();
        JsonObject jsonObject = json.getAsJsonObject();
        Gson gson = JsonProcessor.instance().obtainGson();
        if (jsonObject.has("MainParam")){
            newWhiteBoardRsp.MainParam =  gson.fromJson(jsonObject.getAsJsonObject("MainParam"), TDCSBoardResult.class);
            newWhiteBoardRsp.AssParam = gson.fromJson(jsonObject.getAsJsonObject("AssParam"), TDCSBoardInfo.class);
        }else{
            KLog.p(KLog.WARN,"no AssParam");
            newWhiteBoardRsp.MainParam = gson.fromJson(json, TDCSBoardResult.class);
        }
        return newWhiteBoardRsp;
    }

    @Override
    public String toString() {
        return "DcsNewWhiteBoardRsp{" +
                "MainParam=" + MainParam +
                ", AssParam=" + AssParam +
                '}';
    }
}
