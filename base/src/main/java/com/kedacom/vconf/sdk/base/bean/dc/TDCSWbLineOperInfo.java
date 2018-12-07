/*
 * Copyright (c) 2018 it.kedacom.com, Inc. All rights reserved.
 */

package com.kedacom.vconf.sdk.base.bean.dc;
public class TDCSWbLineOperInfo {
    public String achTabId;     // 白板tab id（guid）
    public int dwSubPageId;     // 子页面id
    public TDCSWbLine tLine;    // 线操作信息
    public TDCSWbLineOperInfo(String boardId, TDCSWbLine line){
        this(boardId, 0, line);
    }
    public TDCSWbLineOperInfo(String boardId, int pageId, TDCSWbLine line){
        achTabId = boardId;
        dwSubPageId = pageId;
        tLine = line;
    }
}
