package com.kedacom.vconf.sdk.datacollaborate.bean;


public abstract class OpDraw extends OpPaint {
    private int strokeWidth;     // 线宽
    private int color;           // 颜色值


    public int getStrokeWidth() {
        return strokeWidth;
    }

    public void setStrokeWidth(int strokeWidth) {
        this.strokeWidth = strokeWidth;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }
}
