package com.kedacom.vconf.sdk.datacollaborate.bean;


import androidx.annotation.NonNull;

public abstract class OpPaint /*implements Comparable<OpPaint>*/{
    public static final int OP_DRAW_LINE = 1;
    public static final int OP_DRAW_RECT = 2;
    public static final int OP_DRAW_OVAL = 3;
    public static final int OP_DRAW_PATH = 4;
    public static final int OP_UNDO = 5;
    public static final int OP_REDO = 6;
    public static final int OP_MATRIX = 7;
    public static final int OP_RECT_ERASE = 8;
    public static final int OP_CLEAR_SCREEN = 9;
    public static final int OP_INSERT_PICTURE = 10;
    public static final int OP_DELETE_PICTURE = 11;
    public static final int OP_DRAG_PICTURE = 12;
    public static final int OP_UPDATE_PICTURE = 13;

    protected int type; // 类型：划线、画圈等。

    protected String   confE164;   // 所属会议e164号
    protected String   boardId;    // 画板ID
    protected int      pageId;     // 文档页ID（仅文档模式下有效）
//    protected int      sn;             // 操作序列号，用来表示操作的先后顺序，越小越靠前。由平台填写。

//    @Override
//    public int compareTo(OpPaint o) {
//        if (sn<o.sn){
//            return -1;
//        }else if (sn == o.sn){
//            return 0;
//        }else{
//            return 1;
//        }
//    }


    @NonNull
    @Override
    public String toString() {
        return String.format(" type=%s, confE164=%s, boardId=%s, pageId=%s", type, confE164, boardId, pageId);
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
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

//    public int getSn() {
//        return sn;
//    }
//
//    public void setSn(int sn) {
//        this.sn = sn;
//    }
}
