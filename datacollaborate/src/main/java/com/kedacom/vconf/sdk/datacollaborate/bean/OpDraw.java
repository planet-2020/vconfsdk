package com.kedacom.vconf.sdk.datacollaborate.bean;


import androidx.annotation.NonNull;

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

    @NonNull
    @Override
    public String toString() {
        return String.format(" strokeWidth=%s, color=%s ", strokeWidth, color)+super.toString();
    }
}
