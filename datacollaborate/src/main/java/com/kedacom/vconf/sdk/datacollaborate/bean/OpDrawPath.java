package com.kedacom.vconf.sdk.datacollaborate.bean;

import android.graphics.PointF;

import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;

import androidx.annotation.NonNull;

public class OpDrawPath extends OpDraw {
    private ConcurrentLinkedQueue<PointF> points;

    public OpDrawPath(){
        type = EOpType.DRAW_PATH;
    }

    public OpDrawPath(ConcurrentLinkedQueue<PointF> points){
        this.points = points;
        type = EOpType.DRAW_PATH;
    }

    @NonNull
    @Override
    public String toString() {
        StringBuffer stringBuffer = new StringBuffer();
        Iterator it = points.iterator();
        PointF pointF;
        while (it.hasNext()){
            pointF = (PointF) it.next();
            stringBuffer.append("(").append(pointF.x).append(",").append(pointF.y).append(")");
        }
        return "{"+String.format("points=[%s] ", stringBuffer.toString())+super.toString()+"}";
    }

    public ConcurrentLinkedQueue<PointF> getPoints() {
        return points;
    }

    public void setPoints(ConcurrentLinkedQueue<PointF> points) {
        this.points = points;
    }
}
