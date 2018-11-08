package com.kedacom.vconf.sdk.datacollaborate.bean;

public abstract class DCOp implements Comparable<DCOp>{
    public static final int OP_DRAW_LINE = 1;
    public static final int OP_DRAW_RECT = 2;
    public static final int OP_DRAW_OVAL = 3;
    public static final int OP_DRAW_PATH = 4;
    public static final int OP_UNDO = 5;
    public static final int OP_REDO = 6;
    public int type; // 类型：划线、画圈等。
    public int sn; // 序号。操作的先后顺序，序号越小的操作越先发生。
    public DCPaintCfg paintCfg;

    @Override
    public int compareTo(DCOp o) {
        if (sn>o.sn){ //NOTE: 我们让越近的操作排在越前以便于一些集合操作。
            return -1;
        }else if (sn == o.sn){
            return 0;
        }else{
            return 1;
        }
    }
}
