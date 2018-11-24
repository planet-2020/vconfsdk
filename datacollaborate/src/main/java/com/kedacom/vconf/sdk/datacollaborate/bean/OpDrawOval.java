package com.kedacom.vconf.sdk.datacollaborate.bean;


public class OpDrawOval extends OpDraw {
    private float left;
    private float top;
    private float right;
    private float bottom;

    public OpDrawOval(){
        type = OP_DRAW_OVAL;
    }

    public OpDrawOval(float left, float top, float right, float bottom){
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
        type = OP_DRAW_OVAL;
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


}
