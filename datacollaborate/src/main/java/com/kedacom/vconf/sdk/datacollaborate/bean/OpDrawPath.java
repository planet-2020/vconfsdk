package com.kedacom.vconf.sdk.datacollaborate.bean;

import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;

import java.util.ArrayList;
import java.util.List;

public class OpDrawPath extends OpDraw {
    private List<PointF> points;
    private Path path;
    private boolean finished; // 是否已完成（曲线是增量同步的）

    // path 的边界
    private float left, top, right, bottom;
    // 上一次计算边界时点的数量，如果数量有变化则重新计算否则直接使用上一次计算的结果。
    private int lastPointCount;

    private RectF bound = new RectF();

    public OpDrawPath(List<PointF> points){
        this(points, true);
    }

    public OpDrawPath(List<PointF> points, boolean bCreatePath) {
        this.points = points;
        if (null != points) path = new Path();

        if (null != points && bCreatePath) {
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

        finished = false;
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

    public void addPoints(List<PointF> appendPoints){
        if (null == appendPoints|| appendPoints.isEmpty()){
            return;
        }
        if (null == points){
            points = new ArrayList<>();
            path = new Path();
        }

        PointF prePoint;
        if (!points.isEmpty()){
            prePoint = points.get(points.size()-1);
        }else{
            prePoint = appendPoints.get(0);
            path.reset();
            path.moveTo(appendPoints.get(0).x, appendPoints.get(0).y);
        }

        points.addAll(appendPoints);

        for (int i=0; i<appendPoints.size()-1; ++i){
            PointF pointF = appendPoints.get(i);
            float midX = (prePoint.x+pointF.x)/2;
            float midY = (prePoint.y+pointF.y)/2;
            path.quadTo(prePoint.x, prePoint.y, midX, midY);
            prePoint = pointF;
        }
    }


}
