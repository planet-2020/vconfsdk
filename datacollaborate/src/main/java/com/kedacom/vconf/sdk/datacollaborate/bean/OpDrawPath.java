package com.kedacom.vconf.sdk.datacollaborate.bean;

import android.graphics.Path;
import android.graphics.PointF;

import java.util.Iterator;
import java.util.List;

import androidx.annotation.NonNull;

public class OpDrawPath extends OpDraw {
    private List<PointF> points;
    private Path path;

    public OpDrawPath(List<PointF> points){
        if (null != points) {
            path = new Path();
            if (!points.isEmpty()) {
                Iterator it = points.iterator();
                PointF pointF = (PointF) it.next();
                path.moveTo(pointF.x, pointF.y);
                while (it.hasNext()) {
                    pointF = (PointF) it.next();
                    path.lineTo(pointF.x, pointF.y);
                }
            }
        }
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

    public List<PointF> getPoints() {
        return points;
    }

    public Path getPath() {
        return path;
    }
}
