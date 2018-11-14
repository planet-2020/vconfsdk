package com.kedacom.vconf.sdk.datacollaborate.bean;

public class PaintCfg {
    public int strokeWidth;
    public int color;
    public int mode;
    public static final int MODE_NORMAL = 1;
    public static final int MODE_ERASE = 2;
    public static final int MODE_PICTURE = 3;

    public PaintCfg(int strokeWidth, int color){
        this.strokeWidth = strokeWidth;
        this.color = color;
        mode = MODE_NORMAL;
    }

    public PaintCfg(int mode){
        this.mode = mode;
    }
}
