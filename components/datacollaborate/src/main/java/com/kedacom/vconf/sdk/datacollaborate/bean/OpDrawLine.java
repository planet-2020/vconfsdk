package com.kedacom.vconf.sdk.datacollaborate.bean;


import android.graphics.RectF;

import androidx.annotation.NonNull;

public class OpDrawLine extends OpDraw {
    private float startX;
    private float startY;
    private float stopX;
    private float stopY;

    private RectF bound = new RectF();

    public OpDrawLine(){
        type = EOpType.DRAW_LINE;
    }

    public OpDrawLine(float startX, float startY, float stopX, float stopY){
        this.startX = startX;
        this.startY =startY;
        this.stopX = stopX;
        this.stopY = stopY;
        bound.set(startX<stopX ? startX : stopX, startY<stopY ? startY : stopY,
                startX>stopX ? startX : stopX, startY>stopY ? startY : stopY);
        type = EOpType.DRAW_LINE;
    }

    @NonNull
    @Override
    public String toString() {
        return "OpDrawLine{" +
                "startX=" + startX +
                ", startY=" + startY +
                ", stopX=" + stopX +
                ", stopY=" + stopY +
                ", bound=" + bound +'\n'+
                super.toString() +
                '}';
    }

    public float getStartX() {
        return startX;
    }

    public void setStartX(float startX) {
        this.startX = startX;
    }

    public float getStartY() {
        return startY;
    }

    public void setStartY(float startY) {
        this.startY = startY;
    }

    public float getStopX() {
        return stopX;
    }

    public void setStopX(float stopX) {
        this.stopX = stopX;
    }

    public float getStopY() {
        return stopY;
    }

    public void setStopY(float stopY) {
        this.stopY = stopY;
    }

    @Override
    public RectF boundary() {
        bound.set(startX<stopX ? startX : stopX, startY<stopY ? startY : stopY,
                startX>stopX ? startX : stopX, startY>stopY ? startY : stopY);
        return bound;
    }
}
