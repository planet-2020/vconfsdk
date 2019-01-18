package com.kedacom.vconf.sdk.datacollaborate.bean;


import androidx.annotation.NonNull;

public class OpRectErase extends OpPaint implements IRepealable, IBoundary{
    public float left;
    public float top;
    public float right;
    public float bottom;

    public OpRectErase(float left, float top, float right, float bottom){
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
        type = EOpType.RECT_ERASE;
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
