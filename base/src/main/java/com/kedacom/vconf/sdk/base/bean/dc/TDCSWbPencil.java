/*
 * Copyright (c) 2018 it.kedacom.com, Inc. All rights reserved.
 */

package com.kedacom.vconf.sdk.base.bean.dc;
public class TDCSWbPencil {
    public TDCSWbEntity tEntity;    // 基本信息
    public int dwPointNum;          // 曲线点数量
    public TDCSWbPoint[] atPList;   // 曲线点信息列表
    public int dwLineWidth;         // 线宽
    public long dwRgb;              // 颜色，强转成int类型就是颜色值了。比如long类型的4294967295强转成int就是0xFFFFFFFF（纯白色），即-1。

    public TDCSWbPencil(TDCSWbEntity tEntity, int dwPointNum, TDCSWbPoint[] atPList, int dwLineWidth, long dwRgb) {
        this.tEntity = tEntity;
        this.dwPointNum = dwPointNum;
        this.atPList = atPList;
        this.dwLineWidth = dwLineWidth;
        this.dwRgb = dwRgb;
    }
}
