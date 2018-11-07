package com.kedacom.vconf.sdk.datacollaborate;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.View;

import com.kedacom.vconf.sdk.base.KLog;
import com.kedacom.vconf.sdk.datacollaborate.bean.DCPaintInfo;

import java.util.HashSet;
import java.util.Set;

public class RawPainter extends View implements IDCPainter {

    Set<Object> ops = new HashSet<>();
    Paint paint = new Paint();

    public RawPainter(Context context) {
        super(context);
        paint.setStyle(Paint.Style.STROKE);
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        KLog.p("canvas=%s", canvas);
        for (Object op : ops){
            ops.remove(op);
            if (op instanceof LineOp){
                KLog.p("drawLine");
                LineOp lineOp = (LineOp) op;
                paint.setStrokeWidth(lineOp.strokeWidth);
                paint.setColor(lineOp.color);
                canvas.drawLine(lineOp.x1, lineOp.y1, lineOp.x2, lineOp.y2, paint);
            }else if (op instanceof RectOp){
                KLog.p("drawRect");
                RectOp rectOp = (RectOp) op;
                paint.setStrokeWidth(rectOp.strokeWidth);
                paint.setColor(rectOp.color);
                canvas.drawRect(rectOp.x1, rectOp.y1, rectOp.x2, rectOp.y2, paint);
            }else if (op instanceof OvalOp){
                KLog.p("drawOval");
                OvalOp rectOp = (OvalOp) op;
                paint.setStrokeWidth(rectOp.strokeWidth);
                paint.setColor(rectOp.color);
                canvas.drawOval(new RectF(rectOp.x1, rectOp.y1, rectOp.x2, rectOp.y2), paint);
            }
        }
    }

    @Override
    public void onWhiteBoard() {

    }

    class LineOp{
        float x1;
        float y1;
        float x2;
        float y2;
        int strokeWidth;
        int color;
    }

    class RectOp{
        float x1;
        float y1;
        float x2;
        float y2;
        int strokeWidth;
        int color;
    }

    class OvalOp{
        float x1;
        float y1;
        float x2;
        float y2;
        int strokeWidth;
        int color;
    }

    @Override
    public void drawLine(float startX, float startY, float stopX, float stopY, DCPaintInfo paintInfo) {
        KLog.p("drawLine");
        LineOp op = new LineOp();
        op.x1 = startX; op.y1 = startY;
        op.x2 = stopX; op.y2 = stopY;
        op.strokeWidth = paintInfo.strokeWidth;
        op.color = paintInfo.color;
        ops.add(op);
        invalidate();
    }

    @Override
    public void drawRect(float left, float top, float right, float bottom, DCPaintInfo paintInfo) {
        KLog.p("drawRect");
        RectOp op = new RectOp();
        op.x1 = left; op.y1 = top;
        op.x2 = right; op.y2 = bottom;
        op.strokeWidth = paintInfo.strokeWidth;
        op.color = paintInfo.color;
        ops.add(op);
        invalidate();
    }

    @Override
    public void drawOval(float left, float top, float right, float bottom, DCPaintInfo paintInfo) {
        KLog.p("drawOval");
        OvalOp op = new OvalOp();
        op.x1 = left; op.y1 = top;
        op.x2 = right; op.y2 = bottom;
        op.strokeWidth = paintInfo.strokeWidth;
        op.color = paintInfo.color;
        ops.add(op);
        invalidate();
    }

    @Override
    public void drawPath(Path path, DCPaintInfo paintInfo) {

    }

    @Override
    public void drawBitmap(Bitmap bitmap, Rect src, Rect dst, DCPaintInfo paintInfo) {

    }

    @Override
    public void erase(float left, float top, float right, float bottom, DCPaintInfo paintInfo) {

    }
}
