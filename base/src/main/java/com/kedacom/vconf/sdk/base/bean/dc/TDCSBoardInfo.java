/*
 * Copyright (c) 2018 it.kedacom.com, Inc. All rights reserved.
 */

package com.kedacom.vconf.sdk.base.bean.dc;

public class TDCSBoardInfo {
    public String achWbName;
    public EmDcsWbMode emWbMode;    // 模式（白板、文档）
    public int dwWbPageNum;         // 总页数（限文档）——以TDCSWbAddSubPageInfo中的dwSubPageCount为准。
    public int dwWbCreateTime;      // 平台成功响应后，平台填写
    public String achTabId;         // 终端填写
    public int dwPageId;            // 文档页id，平台成功响应后，平台填写（限文档）
    public int dwWbSerialNumber;    // 白板序列号，递增，标记白板创建序号
    public String achWbCreatorE164;
    public int dwWbWidth;
    public int dwWbHeight;
    public String achElementUrl;    // 图元Url，*.json格式，由业务层负责解析，上层接收业务层推送的各图元通知即可（如：DcsOperLineOperInfo_Ntf）
    public String achDownloadUrl;   // 图片下载Url（限文档）
    public String achUploadUrl;     // 图片上传Url（限文档）
    public int dwWbAnonyId;         // 平台成功响应后，平台填写（限白板）

}