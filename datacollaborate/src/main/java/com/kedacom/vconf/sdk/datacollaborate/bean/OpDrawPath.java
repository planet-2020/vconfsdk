package com.kedacom.vconf.sdk.datacollaborate.bean;

import android.graphics.PointF;

public class OpDrawPath extends OpPaint {
    public PointF[] points;
    public OpDrawPath(PointF[] points, int sn, PaintCfg paintCfg, String boardId){
        this.points = points;
        this.sn = sn;
        this.paintCfg = paintCfg;
        this.boardId = boardId;
        type = OpPaint.OP_DRAW_PATH;
    }
}
