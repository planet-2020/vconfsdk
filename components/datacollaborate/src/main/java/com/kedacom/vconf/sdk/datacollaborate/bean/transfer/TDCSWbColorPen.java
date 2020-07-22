/*
 * Copyright (c) 2018 it.kedacom.com, Inc. All rights reserved.
 */

package com.kedacom.vconf.sdk.datacollaborate.bean.transfer;

public class TDCSWbColorPen {
    public TDCSWbEntity tEntity;    // 基本信息
    public int dwColorPenNum;
    public TDCSWbPoint[] atCPList;  // 曲线点信息列表
    public int dwLineWidth;         // 线宽
    public long dwRgb;              // 颜色，强转成int类型就是颜色值了。比如long类型的4294967295强转成int就是0xFFFFFFFF（纯白色），即-1。
}
