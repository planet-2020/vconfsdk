package com.kedacom.vconf.sdk.datacollaborate.bean;

import android.graphics.PointF;

import com.kedacom.vconf.sdk.base.MsgBeans;

public class OpDrawPath extends OpDraw {
    private PointF[] points;

    public OpDrawPath(){
        type = OP_DRAW_PATH;
    }

    public OpDrawPath(PointF[] points){
        this.points = points;
        type = OP_DRAW_PATH;
    }


    protected MsgBeans.DCPathOp toTransferObj(MsgBeans.DCPathOp to) {
        super.toTransferObj(to);
        to.points = points;
        return to;
    }

    protected OpDrawPath fromTransferObj(MsgBeans.DCPathOp to) {
        super.fromTransferObj(to);
        points = to.points;
        return this;
    }

    public PointF[] getPoints() {
        return points;
    }

    public void setPoints(PointF[] points) {
        this.points = points;
    }
}
