package com.kedacom.vconf.sdk.datacollaborate.bean;


import androidx.annotation.NonNull;

public class OpDrawRect extends OpDraw {
    private float left;
    private float top;
    private float right;
    private float bottom;


    public OpDrawRect(){
        type = EOpType.DRAW_RECT;
    }

    public OpDrawRect(float left, float top, float right, float bottom){
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
        type = EOpType.DRAW_RECT;
    }

    @NonNull
    @Override
    public String toString() {
        return "{"+String.format("left=%s, top=%s, right=%s, bottom=%s", left, top, right, bottom)+super.toString()+"}";
    }

    public float getLeft() {
        return left;
    }

    public void setLeft(float left) {
        this.left = left;
    }

    public float getTop() {
        return top;
    }

    public void setTop(float top) {
        this.top = top;
    }

    public float getRight() {
        return right;
    }

    public void setRight(float right) {
        this.right = right;
    }

    public float getBottom() {
        return bottom;
    }

    public void setBottom(float bottom) {
        this.bottom = bottom;
    }

    public void setValues(float[] vals){
        left = vals[0];
        top = vals[1];
        right = vals[2];
        bottom = vals[3];
    }

    @Override
    public float left() {
        return left;
    }

    @Override
    public float top() {
        return top;
    }

    @Override
    public float right() {
        return right;
    }

    @Override
    public float bottom() {
        return bottom;
    }
}
