/*
 * Copyright (c) 2018 it.kedacom.com, Inc. All rights reserved.
 */

package com.kedacom.vconf.sdk.base.bean.dc;
public class TDCSWbReginEraseOperInfo {
    public String achTabId;         // 白板tab id（guid）
    public int dwSubPageId;         // 子页id
    public String achGraphsId;      // 图元ID
    public int dwEraseWidth;
    public int dwEraseHeight;
    public int dwPointCount;
    public TDCSWbPoint[] atPoint;   // 矩形中心点坐标集合
}
