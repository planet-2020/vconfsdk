package com.kedacom.vconf.sdk.datacollaborate.bean;

import android.graphics.Path;
import android.graphics.PointF;

import java.util.Iterator;
import java.util.List;

import androidx.annotation.NonNull;

public class OpErase extends OpDraw {
    private int width;
    private int height;
    private List<PointF> points;
    private Path path;

    public OpErase(int width, int height, List<PointF> points){
        this.width = width;
        this.height = height;
        if (null != points) {
            path = new Path();
            if (!points.isEmpty()) { // TODO smooth，参考path
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
        type = EOpType.ERASE;
    }

    @NonNull
    @Override
    public String toString() {
        return "{"+"path="+path+super.toString()+"}";
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public List<PointF> getPoints() {
        return points;
    }

    public Path getPath() {
        return path;
    }
}
