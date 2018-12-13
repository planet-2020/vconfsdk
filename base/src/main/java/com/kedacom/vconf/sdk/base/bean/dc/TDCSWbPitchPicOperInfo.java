/*
 * Copyright (c) 2018 it.kedacom.com, Inc. All rights reserved.
 */

package com.kedacom.vconf.sdk.base.bean.dc;
public class TDCSWbPitchPicOperInfo {
    public String achTabId;         // 白板tab id（guid）
    public int dwSubPageId;         // 子页面id
    public int dwGraphsCount;
    public TDCSWbGraphsInfo[] atGraphsInfo;
    public TDCSWbPitchPicOperInfo(String boardId, int pageId, TDCSWbGraphsInfo[] picInfos){
        achTabId = boardId;
        dwSubPageId = pageId;
        atGraphsInfo = picInfos;
        dwGraphsCount = atGraphsInfo.length;
    }
}
