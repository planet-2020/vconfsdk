package com.kedacom.vconf.sdk.datacollaborate.bean;

import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public class OpDrawPath extends OpDraw {
    private List<PointF> points = new ArrayList<>();
    private Path path = new Path();
    private boolean finished = false; // 是否已完成（曲线是增量同步的）

    // path 的边界
    private float left, top, right, bottom;
    // 上一次计算边界时点的数量，如果数量有变化则重新计算否则直接使用上一次计算的结果。
    private int lastPointCount;

    private RectF bound = new RectF();

    public OpDrawPath(List<PointF> points) {
        addPoints(points);
        type = EOpType.DRAW_PATH;
    }

    @NonNull
    @Override
    public String toString() {
        return "OpDrawPath{" +
                "path=" + path +
                ", left=" + left +
                ", top=" + top +
                ", right=" + right +
                ", bottom=" + bottom +
                ", lastPointCount=" + lastPointCount +
                ", bound=" + bound +
                ", finished=" + finished +
                '\n'+super.toString() +
                '}';
    }

    public List<PointF> getPoints() {
        return points;
    }

    public Path getPath() {
        return path;
    }

    public boolean isFinished() {
        return finished;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }

    private void calcBoundary(){
        if (points.isEmpty() || lastPointCount == points.size()){
            return;
        }

        lastPointCount = points.size();

        PointF p = points.get(0);
        left = right = p.x;
        top = bottom = p.y;
        for (PointF point : points){
            if (point.x < left){
                left = point.x;
            }else if (point.x > right){
                right = point.x;
            }

            if (point.y < top){
                top = point.y;
            }else if (point.y > bottom){
                bottom = point.y;
            }
        }
    }


    @Override
    public RectF boundary() {
        calcBoundary();
        bound.set(left, top, right, bottom);
        return bound;
    }

    public void addPoints(List<PointF> points){
        for (PointF point : points){
            addPoint(point);
        }
    }

    public void addPoint(PointF point){
        if (null==point){
            return;
        }
        points.add(point);
        if (1==points.size()){
            path.moveTo(point.x, point.y);
            return;
        }

        float preX, preY, midX, midY;
        preX = points.get(points.size()-2).x;
        preY = points.get(points.size()-2).y;
        midX = (preX + point.x) / 2;
        midY = (preY + point.y) / 2;
        path.quadTo(preX, preY, midX, midY);
    }

}
