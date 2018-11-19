package com.kedacom.vconf.sdk.datacollaborate.bean;

public abstract class OpPaint implements Comparable<OpPaint>{
    public static final int OP_DRAW_LINE = 1;
    public static final int OP_DRAW_RECT = 2;
    public static final int OP_DRAW_OVAL = 3;
    public static final int OP_DRAW_PATH = 4;
    public static final int OP_UNDO = 5;
    public static final int OP_REDO = 6;
    public static final int OP_MATRIX = 7;
    public static final int OP_ERASE = 8;
    public static final int OP_CLEAR_SCREEN = 9;
    public static final int OP_INSERT_PICTURE = 10;
    public static final int OP_DELETE_PICTURE = 11;
    public static final int OP_PIC_MATRIX = 12;
    public int type; // 类型：划线、画圈等。
    int sn; // 序号。操作的先后顺序，序号越小的操作越先发生。
    public PaintCfg paintCfg;

    public String boardId;  // 画板ID
    public int pageIndex; // 当画板装载文档时表示文档页码


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
}
