package com.kedacom.vconf.sdk.datacollaborate.bean;


import androidx.annotation.NonNull;

public abstract class OpDraw extends OpPaint implements IRepealable{
    private int strokeWidth;     // 线宽
    private long color;           // 颜色值

    public static final int SOLID = 1; // 实线
    public static final int DASH = 2;  // 虚线
    private int lineStyle = SOLID;

    public int getStrokeWidth() {
        return strokeWidth;
    }

    public void setStrokeWidth(int strokeWidth) {
        this.strokeWidth = strokeWidth;
    }

    public long getColor() {
        return color;
    }

    public void setColor(long color) {
        this.color = color;
    }

    public int getLineStyle() {
        return lineStyle;
    }

    public void setLineStyle(int lineStyle) {
        this.lineStyle = lineStyle;
    }

    @NonNull
    @Override
    public String toString() {
        return String.format(" strokeWidth=%s, color=%s ", strokeWidth, color)+super.toString();
    }
}
