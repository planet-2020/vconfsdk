package com.kedacom.vconf.sdk.datacollaborate.bean;

import android.graphics.PointF;

import java.util.List;

public class OpErase extends OpDrawPath {
    private int width;
    private int height;

    public OpErase(int width, int height, List<PointF> points){

        super(points);

        this.width = width;
        this.height = height;
        type = EOpType.ERASE;
    }

    @Override
    public String toString() {
        return "OpErase{" +
                "width=" + width +
                ", height=" + height +
                 super.toString()+
                '}';
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

}
