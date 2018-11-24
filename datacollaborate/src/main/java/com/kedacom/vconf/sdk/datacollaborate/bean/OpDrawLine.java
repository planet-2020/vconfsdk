package com.kedacom.vconf.sdk.datacollaborate.bean;


public class OpDrawLine extends OpDraw {
    private float startX;
    private float startY;
    private float stopX;
    private float stopY;

    public OpDrawLine(){
        type = OP_DRAW_LINE;
    }

    public OpDrawLine(float startX, float startY, float stopX, float stopY){
        this.startX = startX;
        this.startY =startY;
        this.stopX = stopX;
        this.startY = stopY;
        type = OP_DRAW_LINE;
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
