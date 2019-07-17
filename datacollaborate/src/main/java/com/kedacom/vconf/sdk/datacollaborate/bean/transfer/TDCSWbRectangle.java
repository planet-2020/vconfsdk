/*
 * Copyright (c) 2018 it.kedacom.com, Inc. All rights reserved.
 */

package com.kedacom.vconf.sdk.datacollaborate.bean.transfer;

public class TDCSWbRectangle {
    public TDCSWbEntity tEntity;    // 基本信息
    public TDCSWbPoint tBeginPt;    // 起点坐标
    public TDCSWbPoint tEndPt;      // 终点坐标
    public int dwLineWidth;         // 线宽
    public long dwRgb;              // 颜色，强转成int类型就是颜色值了。比如long类型的4294967295强转成int就是0xFFFFFFFF（纯白色），即-1。

    public TDCSWbRectangle(TDCSWbEntity tEntity, TDCSWbPoint tBeginPt, TDCSWbPoint tEndPt, int dwLineWidth, long dwRgb) {
        this.tEntity = tEntity;
        this.tBeginPt = tBeginPt;
        this.tEndPt = tEndPt;
        this.dwLineWidth = dwLineWidth;
        this.dwRgb = dwRgb;
    }
}
