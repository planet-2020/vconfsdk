package com.kedacom.vconf.sdk.datacollaborate.bean;


import androidx.annotation.NonNull;

public class OpDrawLine extends OpDraw {
    private float startX;
    private float startY;
    private float stopX;
    private float stopY;

    public OpDrawLine(){
        type = EOpType.DRAW_LINE;
    }

    public OpDrawLine(float startX, float startY, float stopX, float stopY){
        this.startX = startX;
        this.startY =startY;
        this.stopX = stopX;
        this.startY = stopY;
        type = EOpType.DRAW_LINE;
    }

    @NonNull
    @Override
    public String toString() {
        return "{"+String.format("startX=%s, startY=%s, stopX=%s, stopY=%s", startX, startY, stopX, stopY)+super.toString()+"}";
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


}
