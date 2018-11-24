package com.kedacom.vconf.sdk.datacollaborate.bean;

import android.graphics.PointF;

public class OpDrawPath extends OpDraw {
    private PointF[] points;

    public OpDrawPath(){
        type = OP_DRAW_PATH;
    }

    public OpDrawPath(PointF[] points){
        this.points = points;
        type = OP_DRAW_PATH;
    }


    public PointF[] getPoints() {
        return points;
    }

    public void setPoints(PointF[] points) {
        this.points = points;
    }
}
