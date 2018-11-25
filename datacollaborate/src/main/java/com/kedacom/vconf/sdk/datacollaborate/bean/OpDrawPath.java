package com.kedacom.vconf.sdk.datacollaborate.bean;

import android.graphics.PointF;

import androidx.annotation.NonNull;

public class OpDrawPath extends OpDraw {
    private PointF[] points;

    public OpDrawPath(){
        type = EOpType.DRAW_PATH;
    }

    public OpDrawPath(PointF[] points){
        this.points = points;
        type = EOpType.DRAW_PATH;
    }

    @NonNull
    @Override
    public String toString() {
        StringBuffer stringBuffer = new StringBuffer();
        for (PointF pointF : points){
            stringBuffer.append("(").append(pointF.x).append(",").append(pointF.y).append(")");
        }
        return "{"+String.format("points=[%s] ", stringBuffer.toString())+super.toString()+"}";
    }

    public PointF[] getPoints() {
        return points;
    }

    public void setPoints(PointF[] points) {
        this.points = points;
    }
}
