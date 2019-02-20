/*
 * Copyright (c) 2018 it.kedacom.com, Inc. All rights reserved.
 */

package com.kedacom.vconf.sdk.base.bean.dc;


/**
 * 图元操作回调的MainParam
 * Created by chenjun on 2017/10/26.
 */
public class TDCSOperContent {
    public EmDcsOper emOper;
    public int dwMsgId;
    public String achTabId;
    public int dwWbPageId;
    public int dwMsgSequence;
    public String achConfE164;
    public String achFromE164;      // 谁画的
    public boolean bCacheElement;   // 是否是服务器缓存的图元

    public TDCSOperContent() {
        achTabId = "board";
        emOper = EmDcsOper.emWbClearScreen;
    }
}
