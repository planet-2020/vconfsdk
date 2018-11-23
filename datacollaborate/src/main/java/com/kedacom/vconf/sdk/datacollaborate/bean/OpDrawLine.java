package com.kedacom.vconf.sdk.datacollaborate.bean;

import com.kedacom.vconf.sdk.base.MsgBeans;

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

    public OpDrawLine fromTransferObj(MsgBeans.DCLineOp to) {
        super.fromTransferObj(to);
        startX = to.startX;
        startY = to.startY;
        stopX = to.stopX;
        stopY = to.stopY;
        return this;
    }

    public MsgBeans.DCLineOp toTransferObj(MsgBeans.DCLineOp to) {
        super.toTransferObj(to);
        to.startX = startX;
        to.startY = startY;
        to.stopX = stopX;
        to.stopY = stopY;
        return to;
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
