package com.kedacom.vconf.sdk.datacollaborate.bean;


import java.util.UUID;

import androidx.annotation.NonNull;

public abstract class OpPaint implements Comparable<OpPaint>{

    protected String uuid = UUID.randomUUID().toString();
    protected EOpType type; // 类型：划线、画圈等。

    protected String   confE164;   // 所属会议e164号
    protected String   boardId;    // 画板ID
    protected int      pageId;     // 文档页ID（仅文档模式下有效）
    protected int      sn;         // 操作序号，用来表示操作的先后顺序，越小越靠前。由平台填写。


    @Override
    public int compareTo(OpPaint o) {
        if (sn<o.sn){
            return -1;
        }else if (sn == o.sn){
            return 0;
        }else{
            return 1;
        }
    }

    @NonNull
    @Override
    public String toString() {
        return String.format("\n type=%s, confE164=%s, boardId=%s, pageId=%s", type, confE164, boardId, pageId);
    }

    public String getUuid() {
        return uuid;
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

    public int getSn() {
        return sn;
    }

    public void setSn(int sn) {
        this.sn = sn;
    }
}
