package com.kedacom.vconf.sdk.datacollaborate.bean;

import android.graphics.PointF;

public class DCPathOp extends DCOp {
    public PointF[] points;
    public DCPathOp(PointF[] points, int sn, DCPaintCfg paintCfg){
        this.points = points;
        this.sn = sn;
        this.paintCfg = paintCfg;
        type = DCOp.OP_DRAW_PATH;
    }
}
