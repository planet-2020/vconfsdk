/*
 * Copyright (c) 2018 it.kedacom.com, Inc. All rights reserved.
 */

package com.kedacom.vconf.sdk.base.bean.dc;
public class TDCSFileInfo {
    public String achFilePathName;
    public String achWbPicentityId;     // 如果是图片，则会有pic id，否则为空
    public String achTabid;
    public boolean bElementCacheFile;   // 是否为图元缓存文件，即，如果是图片图元，设置为false；非图片图元（线、圆、矩形等）为true
    public int dwFileSize;
    public TDCSFileInfo(String path, String picId, String boardId, boolean bCachedElement){
        achFilePathName = path;
        achWbPicentityId = picId;
        achTabid = boardId;
        bElementCacheFile = bCachedElement;
    }
}
