/*
 * Copyright (c) 2018 it.kedacom.com, Inc. All rights reserved.
 */

package com.kedacom.vconf.sdk.datacollaborate.bean.transfer;
public class TDCSImageUrl {
    public String achConfE164;
    public String achTabId;
    public int dwPageId;
    public String achPicUrl;
    public String achWbPicentityId;     // 图片ID
    public TDCSImageUrl(String confE164, String boardId, int pageId, String picId){
        achConfE164 = confE164;
        achTabId = boardId;
        dwPageId = pageId;
        achWbPicentityId = picId;
    }
}
