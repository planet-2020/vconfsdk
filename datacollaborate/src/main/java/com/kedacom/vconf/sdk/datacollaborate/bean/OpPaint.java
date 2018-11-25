package com.kedacom.vconf.sdk.datacollaborate.bean;


import androidx.annotation.NonNull;

public abstract class OpPaint {

    protected EOpType type; // 类型：划线、画圈等。

    protected String   confE164;   // 所属会议e164号
    protected String   boardId;    // 画板ID
    protected int      pageId;     // 文档页ID（仅文档模式下有效）


    @NonNull
    @Override
    public String toString() {
        return String.format(" type=%s, confE164=%s, boardId=%s, pageId=%s", type, confE164, boardId, pageId);
    }

    public EOpType getType() {
        return type;
    }

    public void setType(EOpType type) {
        this.type = type;
    }

    public String getConfE164() {
        return confE164;
    }

    public void setConfE164(String confE164) {
        this.confE164 = confE164;
    }

    public String getBoardId() {
        return boardId;
    }

    public void setBoardId(String boardId) {
        this.boardId = boardId;
    }

    public int getPageId() {
        return pageId;
    }

    public void setPageId(int pageId) {
        this.pageId = pageId;
    }

}
