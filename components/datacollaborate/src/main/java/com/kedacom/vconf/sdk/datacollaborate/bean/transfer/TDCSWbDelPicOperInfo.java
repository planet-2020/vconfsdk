/*
 * Copyright (c) 2018 it.kedacom.com, Inc. All rights reserved.
 */

package com.kedacom.vconf.sdk.datacollaborate.bean.transfer;
public class TDCSWbDelPicOperInfo {
    public String achTabId;         // 白板tab id（guid）
    public int dwSubPageId;         // 子页面id
    public int dwGraphsCount;
    public String[] achGraphsId;
    public TDCSWbDelPicOperInfo(String boardId, int pageId, String[] picIds){
        achTabId = boardId;
        dwSubPageId = pageId;
        achGraphsId = picIds;
        dwGraphsCount = achGraphsId.length;
    }
}
