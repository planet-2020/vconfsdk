package com.kedacom.vconf.sdk.datacollaborate.bean;

import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;

import java.util.List;

public class OpDrawPath extends OpDraw {
    private List<PointF> points;
    private Path path;

    // path 的边界
    private float left, top, right, bottom;
    // 上一次计算边界时点的数量，如果数量有变化则重新计算否则直接使用上一次计算的结果。
    private int lastPointCount;

    private RectF bound = new RectF();

    public OpDrawPath(List<PointF> points){
        if (null != points) {
            this.points = points;
            path = new Path();
            if (points.size()>=3){
                PointF pointF = points.get(0);
                path.moveTo(pointF.x, pointF.y);
                pointF = points.get(1);
//                path.lineTo(pointF.x, pointF.y);
                PointF prePoint = pointF;
                float midX, midY;
                int i=2;
                for (; i<points.size()-1; ++i){
                    pointF = points.get(i);
                    midX = (prePoint.x+pointF.x)/2;
                    midY = (prePoint.y+pointF.y)/2;
                    path.quadTo(prePoint.x, prePoint.y, midX, midY);
                    prePoint = pointF;
                }
                pointF = points.get(i);
                path.lineTo(pointF.x, pointF.y);
            }else if(!points.isEmpty()){
                path.moveTo(points.get(0).x, points.get(0).y);
                if (points.size()==2){
                    path.lineTo(points.get(1).x, points.get(1).y);
                }
            }
        }
        type = EOpType.DRAW_PATH;
    }

    @Override
    public String toString() {
        return "OpDrawPath{" +
                ", path=" + path +
                ", left=" + left +
                ", top=" + top +
                ", right=" + right +
                ", bottom=" + bottom +
                ", lastPointCount=" + lastPointCount +
                ", bound=" + bound +'\n'+
                super.toString() +
                '}';
    }

    public List<PointF> getPoints() {
        return points;
    }

    public Path getPath() {
        return path;
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
}
