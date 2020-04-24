package com.kedacom.vconf.sdk.datacollaborate.bean.transfer;

import androidx.annotation.NonNull;

import com.google.gson.annotations.JsonAdapter;
import com.kedacom.vconf.sdk.utils.json.Kson;
import com.kedacom.vconf.sdk.utils.json.LameJsonAdapter;

//@JsonAdapter(DcsGetConfAddrRsp.Adapter.class)
public class DcsGetConfAddrRsp {
    public TDCSResult MainParam;
    public TDCSConfAddr AssParam;

    @NonNull
    @Override
    public String toString() {
        return "DcsGetConfAddrRsp{" +
                "MainParam=" + MainParam +
                ", AssParam=" + AssParam +
                '}';
    }

    static {
        // 通过JsonAdapter注解的方式注册适配器更加便捷，但该注解是Gson2.3引入的，有的用户可能必须使用老版Gson，故回退使用老方式注册。
        Kson.registerAdapter(DcsGetConfAddrRsp.class, new LameJsonAdapter<DcsGetConfAddrRsp, TDCSResult, TDCSConfAddr>(){});
    }

//    static final class Adapter extends LameJsonAdapter<DcsGetConfAddrRsp, TDCSResult, TDCSConfAddr> { }
}
