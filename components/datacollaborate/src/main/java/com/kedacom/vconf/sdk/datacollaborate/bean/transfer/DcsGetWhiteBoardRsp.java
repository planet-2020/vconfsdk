/*
 * Copyright (c) 2018 it.kedacom.com, Inc. All rights reserved.
 */

package com.kedacom.vconf.sdk.datacollaborate.bean.transfer;

import androidx.annotation.NonNull;

import com.kedacom.vconf.sdk.utils.json.LameStrategy;

//@JsonAdapter(DcsGetWhiteBoardRsp.Adapter.class)

@LameStrategy(mainField = TDCSBoardResult.class)
public class DcsGetWhiteBoardRsp {
    public TDCSBoardResult MainParam;
    public TDCSBoardInfo AssParam;

    @NonNull
    @Override
    public String toString() {
        return "DcsGetWhiteBoardRsp{" +
                "MainParam=" + MainParam +
                ", AssParam=" + AssParam +
                '}';
    }

//    static {
//        // 通过JsonAdapter注解的方式注册适配器更加便捷，但该注解是Gson2.3引入的，有的用户可能必须使用老版Gson，故回退使用老方式注册。
//        Kson.registerAdapter(DcsGetWhiteBoardRsp.class, new LameJsonAdapter<DcsGetWhiteBoardRsp, TDCSBoardResult, TDCSBoardInfo>(){});
//    }
//    static final class Adapter extends LameJsonAdapter<DcsGetWhiteBoardRsp, TDCSBoardResult, TDCSBoardInfo> { }
}
