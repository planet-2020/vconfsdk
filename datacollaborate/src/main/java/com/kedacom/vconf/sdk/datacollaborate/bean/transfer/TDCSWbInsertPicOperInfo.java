/*
 * Copyright (c) 2018 it.kedacom.com, Inc. All rights reserved.
 */

package com.kedacom.vconf.sdk.datacollaborate.bean.transfer;

public class TDCSWbInsertPicOperInfo {
    public String achTabId;         // 白板tab id（guid）
    public int dwSubPageId;         // 子页面id
    public String achImgId;         // 图元ID
    public int dwImgWidth;
    public int dwImgHeight;
    public TDCSWbPoint tPoint;
    public String achPicName;
    public String[] aachMatrixValue;
    public TDCSWbInsertPicOperInfo(String boardId, int pageId, String picId, int picWidth, int picHeight, TDCSWbPoint insertPos, String picName, String[] matrix){
        achTabId = boardId;
        dwSubPageId = pageId;
        achImgId = picId;
        dwImgWidth = picWidth;
        dwImgHeight = picHeight;
        tPoint = insertPos;
        achPicName = picName;
        aachMatrixValue = matrix;
    }
}
