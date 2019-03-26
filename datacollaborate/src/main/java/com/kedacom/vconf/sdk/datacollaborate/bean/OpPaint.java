package com.kedacom.vconf.sdk.datacollaborate.bean;

import java.util.Arrays;
import java.util.UUID;


public abstract class OpPaint implements Comparable<OpPaint>{

    protected String uuid = UUID.randomUUID().toString();
    protected EOpType type; // 类型：划线、画圈等。

    protected String   confE164;   // 所属会议e164号
    protected String   boardId;    // 画板ID
    protected String   authorE164; // 绘制者e164
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

    @Override
    public String toString() {
        return "OpPaint{" +
                "uuid='" + uuid + '\'' +
                ", type=" + type +
                ", confE164='" + confE164 + '\'' +
                ", boardId='" + boardId + '\'' +
                ", authorE164='" + authorE164 + '\'' +
                ", pageId=" + pageId +
                ", sn=" + sn +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this){
            return true;
        }
        if (null == obj){
            return false;
        }
        if (obj.getClass()!=getClass()){
            return false;
        }

        OpPaint otherOp = (OpPaint) obj;
        return confE164.equals(otherOp.confE164)
                && boardId.equals(otherOp.boardId)
                && pageId == otherOp.pageId
                && sn == otherOp.sn;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(new Object[]{confE164, boardId, pageId, sn});
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
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

    public String getAuthorE164() {
        return authorE164;
    }

    public void setAuthorE164(String authorE164) {
        this.authorE164 = authorE164;
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
