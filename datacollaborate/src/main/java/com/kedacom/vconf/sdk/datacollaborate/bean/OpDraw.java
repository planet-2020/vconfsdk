package com.kedacom.vconf.sdk.datacollaborate.bean;

import com.kedacom.vconf.sdk.base.MsgBeans;

public abstract class OpDraw extends OpPaint {
    private int strokeWidth;     // 线宽
    private int color;           // 颜色值

    protected MsgBeans.DCDrawOp toTransferObj(MsgBeans.DCDrawOp to) {
        super.toTransferObj(to);
        to.strokeWidth = strokeWidth;
        to.color = color;
        return to;
    }

    protected OpDraw fromTransferObj(MsgBeans.DCDrawOp to) {
        fromTransferObj(to);
        strokeWidth = to.strokeWidth;
        color = to.color;
        return this;
    }

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
}
