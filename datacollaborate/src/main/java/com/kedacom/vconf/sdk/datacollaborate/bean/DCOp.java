package com.kedacom.vconf.sdk.datacollaborate.bean;

public abstract class DCOp {
    public static final int DRAW_LINE = 1;
    public static final int DRAW_RECT = 2;
    public static final int DRAW_OVAL = 3;
    public static final int DRAW_PATH = 4;
    public int type; // 类型：划线、画圈等。
    public int sn; // 序号。操作的先后顺序，序号小的在前。
    public DCPaintCfg paintCfg;
}
