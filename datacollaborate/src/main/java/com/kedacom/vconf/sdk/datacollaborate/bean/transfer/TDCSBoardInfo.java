/*
 * Copyright (c) 2018 it.kedacom.com, Inc. All rights reserved.
 */

package com.kedacom.vconf.sdk.datacollaborate.bean.transfer;

import androidx.annotation.NonNull;

public class TDCSBoardInfo implements Comparable<TDCSBoardInfo>{
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

    public TDCSBoardInfo() {
        achWbName = "board";
        emWbMode = EmDcsWbMode.emWbModeWB;
        achTabId = "board";
        achWbCreatorE164 = "e164";
    }

    public TDCSBoardInfo(String achTabId, String achWbCreatorE164) {
        this.emWbMode = EmDcsWbMode.emWbModeWB;
        this.dwWbPageNum = 1;
        this.achTabId = achTabId;
        this.achWbCreatorE164 = achWbCreatorE164;
    }

    public TDCSBoardInfo(EmDcsWbMode emWbMode, String achWbName, int dwWbPageNum, String achTabId, int dwPageId, String achWbCreatorE164) {
        this.achWbName = achWbName;
        this.emWbMode = emWbMode;
        this.dwWbPageNum = dwWbPageNum;
        this.achTabId = achTabId;
        this.dwPageId = dwPageId;
        this.achWbCreatorE164 = achWbCreatorE164;
    }

    @NonNull
    @Override
    public String toString() {
        return "TDCSBoardInfo{" +
                "achWbName='" + achWbName + '\'' +
                ", emWbMode=" + emWbMode +
                ", dwWbPageNum=" + dwWbPageNum +
                ", dwWbCreateTime=" + dwWbCreateTime +
                ", achTabId='" + achTabId + '\'' +
                ", dwPageId=" + dwPageId +
                ", dwWbSerialNumber=" + dwWbSerialNumber +
                ", achWbCreatorE164='" + achWbCreatorE164 + '\'' +
                ", dwWbWidth=" + dwWbWidth +
                ", dwWbHeight=" + dwWbHeight +
                ", achElementUrl='" + achElementUrl + '\'' +
                ", achDownloadUrl='" + achDownloadUrl + '\'' +
                ", achUploadUrl='" + achUploadUrl + '\'' +
                ", dwWbAnonyId=" + dwWbAnonyId +
                '}';
    }

    @Override
    public int compareTo(@NonNull TDCSBoardInfo o) {
        if (null == o){
            return 1;
        }

        if (dwWbAnonyId<o.dwWbAnonyId){
            return -1;
        }else if (dwWbAnonyId == o.dwWbAnonyId){
            return 0;
        }else{
            return 1;
        }
    }

}
