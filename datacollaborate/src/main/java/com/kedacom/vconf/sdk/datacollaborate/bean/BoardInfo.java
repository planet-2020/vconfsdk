/**
 * 画板信息
 * */

package com.kedacom.vconf.sdk.datacollaborate.bean;

import androidx.annotation.NonNull;

public class BoardInfo {
    public String id;           // 终端填写GUID（系统函数生成）
    public String name;
    public String confE164;         // 所属会议e164号
    public String creatorE164;
    public int createTime;      // 平台成功响应后，平台填写
    public EBoardMode mode;    // 模式（白板、文档）
    public int pageNum;         // 总页数（限文档）——以TDCSWbAddSubPageInfo中的dwSubPageCount为准。
    public int pageId;            // 文档页id，平台成功响应后，平台填写（限文档）
    public int anonyId;         // 平台成功响应后，平台填写（限白板），白板1白板2后面的数字，平台裁决后分配的。

    @NonNull
    @Override
    public String toString() {
        return "{"+String.format("id=%s, name=%s, confE164=%s, creatorE164=%s, createTime=%s, " +
                "mode=%s, pageNum=%s, pageId=%s, anonyId",
                id, name, confE164, creatorE164, createTime,
                mode, pageNum, pageId, anonyId)+"}";
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getConfE164() {
        return confE164;
    }

    public void setConfE164(String confE164) {
        this.confE164 = confE164;
    }

    public String getCreatorE164() {
        return creatorE164;
    }

    public void setCreatorE164(String creatorE164) {
        this.creatorE164 = creatorE164;
    }

    public int getCreateTime() {
        return createTime;
    }

    public void setCreateTime(int createTime) {
        this.createTime = createTime;
    }

    public EBoardMode getMode() {
        return mode;
    }

    public void setMode(EBoardMode mode) {
        this.mode = mode;
    }

    public int getPageNum() {
        return pageNum;
    }

    public void setPageNum(int pageNum) {
        this.pageNum = pageNum;
    }

    public int getPageId() {
        return pageId;
    }

    public void setPageId(int pageId) {
        this.pageId = pageId;
    }

    public int getAnonyId() {
        return anonyId;
    }

    public void setAnonyId(int anonyId) {
        this.anonyId = anonyId;
    }
}
