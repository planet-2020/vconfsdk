/*
 * Copyright (c) 2018 it.kedacom.com, Inc. All rights reserved.
 */

package com.kedacom.vconf.sdk.base.bean.dc;


import com.google.gson.annotations.JsonAdapter;
import com.kedacom.vconf.sdk.base.tools.MainAssParaJsonAdapter;

@JsonAdapter(DcsGetAllWhiteBoardRsp.Adapter.class)  // 指定json反序列化的解析器
public class DcsGetAllWhiteBoardRsp{
    public TDCSResult MainParam;
    public TDCSGetAllBoard AssParam;

    @Override
    public String toString() {
        return "DcsGetAllWhiteBoardRsp{" +
                "MainParam=" + MainParam +
                ", AssParam=" + AssParam +
                '}';
    }

    static final class Adapter extends MainAssParaJsonAdapter<DcsGetAllWhiteBoardRsp> { }

}
