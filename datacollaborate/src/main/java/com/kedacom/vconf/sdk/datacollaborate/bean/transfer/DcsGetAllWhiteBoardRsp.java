/*
 * Copyright (c) 2018 it.kedacom.com, Inc. All rights reserved.
 */

package com.kedacom.vconf.sdk.datacollaborate.bean.transfer;


import androidx.annotation.NonNull;

import com.google.gson.annotations.JsonAdapter;
import com.kedacom.vconf.sdk.utils.json.Kson;
import com.kedacom.vconf.sdk.utils.json.LameJsonAdapter;

//@JsonAdapter(DcsGetAllWhiteBoardRsp.Adapter.class)  // 指定json反序列化的解析器
public class DcsGetAllWhiteBoardRsp{
    public TDCSResult MainParam;
    public TDCSGetAllBoard AssParam;

    @NonNull
    @Override
    public String toString() {
        return "DcsGetAllWhiteBoardRsp{" +
                "MainParam=" + MainParam +
                ", AssParam=" + AssParam +
                '}';
    }
    static {
        // 通过JsonAdapter注解的方式注册适配器更加便捷，但该注解是Gson2.3引入的，有的用户可能必须使用老版Gson，故回退使用老方式注册。
        Kson.registerAdapter(DcsGetAllWhiteBoardRsp.class, new LameJsonAdapter<DcsGetAllWhiteBoardRsp, TDCSResult, TDCSGetAllBoard>(){});
    }

//    static final class Adapter extends LameJsonAdapter<DcsGetAllWhiteBoardRsp, TDCSResult, TDCSGetAllBoard> { }

}
