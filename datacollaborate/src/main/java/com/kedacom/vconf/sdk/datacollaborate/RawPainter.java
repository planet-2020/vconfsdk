package com.kedacom.vconf.sdk.datacollaborate;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.view.View;

import com.kedacom.vconf.sdk.base.KLog;
import com.kedacom.vconf.sdk.datacollaborate.bean.DCEraseOp;
import com.kedacom.vconf.sdk.datacollaborate.bean.DCImageOp;
import com.kedacom.vconf.sdk.datacollaborate.bean.DCLineOp;
import com.kedacom.vconf.sdk.datacollaborate.bean.DCOp;
import com.kedacom.vconf.sdk.datacollaborate.bean.DCOvalOp;
import com.kedacom.vconf.sdk.datacollaborate.bean.DCPathOp;
import com.kedacom.vconf.sdk.datacollaborate.bean.DCRectOp;

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
//        for (Object op : ops){
//            ops.remove(op);
//            if (op instanceof LineOp){
//                KLog.p("drawLine");
//                LineOp lineOp = (LineOp) op;
//                paint.setStrokeWidth(lineOp.strokeWidth);
//                paint.setColor(lineOp.color);
//                canvas.drawLine(lineOp.x1, lineOp.y1, lineOp.x2, lineOp.y2, paint);
//            }else if (op instanceof RectOp){
//                KLog.p("drawRect");
//                RectOp rectOp = (RectOp) op;
//                paint.setStrokeWidth(rectOp.strokeWidth);
//                paint.setColor(rectOp.color);
//                canvas.drawRect(rectOp.x1, rectOp.y1, rectOp.x2, rectOp.y2, paint);
//            }else if (op instanceof OvalOp){
//                KLog.p("drawOval");
//                OvalOp rectOp = (OvalOp) op;
//                paint.setStrokeWidth(rectOp.strokeWidth);
//                paint.setColor(rectOp.color);
//                canvas.drawOval(new RectF(rectOp.x1, rectOp.y1, rectOp.x2, rectOp.y2), paint);
//            }
//        }
    }


    @Override
    public void draw(DCOp op) {

        Drawable drawable = Drawable.createFromPath("/data/local/tmp/wb.png");
        setBackground(drawable);
        KLog.p(KLog.WARN, "drawable=%s", drawable);
    }


//    @Override
//    public void drawLine(DCLineOp lineOpInfo) {
//        KLog.p("drawLine");
//        LineOp op = new LineOp();
//        op.x1 = startX; op.y1 = startY;
//        op.x2 = stopX; op.y2 = stopY;
//        op.strokeWidth = lineOpInfo.strokeWidth;
//        op.color = lineOpInfo.color;
//        ops.add(op);
//        invalidate();
//    }
//
//    @Override
//    public void drawRect(DCRectOp rectOpInfo) {
//        KLog.p("drawRect");
//        RectOp op = new RectOp();
//        op.x1 = left; op.y1 = top;
//        op.x2 = right; op.y2 = bottom;
//        op.strokeWidth = rectOpInfo.strokeWidth;
//        op.color = rectOpInfo.color;
//        ops.add(op);
//        invalidate();
//    }
//
//    public void drawOval(DCOvalOp ovalOpInfo) {
//        KLog.p("drawOval");
//        OvalOp op = new OvalOp();
//        op.x1 = left; op.y1 = top;
//        op.x2 = right; op.y2 = bottom;
//        op.strokeWidth = ovalOpInfo.strokeWidth;
//        op.color = ovalOpInfo.color;
//        ops.add(op);
//        invalidate();
//    }
//
//    public void drawPath(DCPathOp pathOpInfo) {
//
//    }
//
//    public void drawImage(DCImageOp imageOpInfo) {
//
//    }
//
//    public void erase(DCEraseOp eraseOpInfo) {
//
//    }
}
