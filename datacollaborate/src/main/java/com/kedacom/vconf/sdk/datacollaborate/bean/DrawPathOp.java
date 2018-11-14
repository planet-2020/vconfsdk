package com.kedacom.vconf.sdk.datacollaborate.bean;

import android.graphics.PointF;

public class DrawPathOp extends PaintOp {
    public PointF[] points;
    public DrawPathOp(PointF[] points, int sn, PaintCfg paintCfg){
        this.points = points;
        this.sn = sn;
        this.paintCfg = paintCfg;
        type = PaintOp.OP_DRAW_PATH;
    }
}
