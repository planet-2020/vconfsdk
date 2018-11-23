package com.kedacom.vconf.sdk.datacollaborate.bean;

import com.kedacom.vconf.sdk.base.MsgBeans;

public class OpRectErase extends OpPaint {
    public float left;
    public float top;
    public float right;
    public float bottom;

    public OpRectErase(){
        type = OP_ERASE;
    }

    public OpRectErase(float left, float top, float right, float bottom){
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
        type = OP_ERASE;
    }

    public OpRectErase fromTransferObj(MsgBeans.DCRectEraseOp to) {
        super.fromTransferObj(to);
        left = to.left;
        top = to.top;
        right = to.right;
        bottom = to.bottom;
        return this;
    }

    public MsgBeans.DCRectEraseOp toTransferObj(MsgBeans.DCRectEraseOp to) {
        super.toTransferObj(to);
        to.left=  left;
        to.top=  top;
        to.right=  right;
        to.bottom= bottom;
        return to;
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
