/*
 * Copyright (c) 2018 it.kedacom.com, Inc. All rights reserved.
 */

package com.kedacom.vconf.sdk.datacollaborate.bean.transfer;

public class TDCSOperReq {
    public String achConfE164;
    public String achTabId;
    public int dwWbPageid;
    public TDCSOperReq(String confE164, String boardId){
        this(confE164, boardId, 0);
    }
    public TDCSOperReq(String confE164, String boardId, int pageId){
        achConfE164 = confE164;
        achTabId = boardId;
        dwWbPageid = pageId;
    }
}
