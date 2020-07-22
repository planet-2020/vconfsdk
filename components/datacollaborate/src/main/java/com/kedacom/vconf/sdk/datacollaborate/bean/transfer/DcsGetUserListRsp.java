/*
 * Copyright (c) 2018 it.kedacom.com, Inc. All rights reserved.
 */

package com.kedacom.vconf.sdk.datacollaborate.bean.transfer;

import androidx.annotation.NonNull;

import com.kedacom.vconf.sdk.utils.json.LameStrategy;

//@JsonAdapter(DcsGetUserListRsp.Adapter.class)

@LameStrategy(mainField = TDCSResult.class)
public class DcsGetUserListRsp{

    public TDCSResult MainParam;
    public TDCSGetUserList AssParam;

    @NonNull
    @Override
    public String toString() {
        return "DcsGetUserListRsp{" +
                "MainParam=" + MainParam +
                ", AssParam=" + AssParam +
                '}';
    }

//    static {
//        // 通过JsonAdapter注解的方式注册适配器更加便捷，但该注解是Gson2.3引入的，有的用户可能必须使用老版Gson，故回退使用老方式注册。
//        Kson.registerAdapter(DcsGetUserListRsp.class, new LameJsonAdapter<DcsGetUserListRsp, TDCSResult, TDCSGetUserList>(){});
//    }

//    static final class Adapter extends LameJsonAdapter<DcsGetUserListRsp, TDCSResult, TDCSGetUserList> { }
}
