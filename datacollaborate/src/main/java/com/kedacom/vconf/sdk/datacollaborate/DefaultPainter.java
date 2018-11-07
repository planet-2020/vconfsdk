package com.kedacom.vconf.sdk.datacollaborate;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.os.Process;
import android.view.TextureView;
import android.view.View;

import com.kedacom.vconf.sdk.base.KLog;
import com.kedacom.vconf.sdk.datacollaborate.bean.DCLineOp;
import com.kedacom.vconf.sdk.datacollaborate.bean.DCOp;
import com.kedacom.vconf.sdk.datacollaborate.bean.DCOvalOp;
import com.kedacom.vconf.sdk.datacollaborate.bean.DCPaintCfg;
import com.kedacom.vconf.sdk.datacollaborate.bean.DCPathOp;
import com.kedacom.vconf.sdk.datacollaborate.bean.DCRectOp;

import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class DefaultPainter implements IDCPainter {

    private TextureView textureView;
    private Paint paint;

    private ConcurrentLinkedQueue<DCOp> dcOps;
    private PriorityQueue<DCOp> batchDcOps;

    private boolean isBatchDrawing = false;
    private boolean needRender = false;

    private static int threadCount = 0;
    private final Thread renderThread = new Thread("DCRenderThr"+threadCount++){
        @Override
        public void run() {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
            DCLineOp lineOp;
            DCRectOp rectOp;
            DCOvalOp ovalOp;
            DCPathOp pathOp;
            Path path = new Path();
            RectF rect = new RectF();

            while (true){
                KLog.p(KLog.WARN, "############==run");
                if (isInterrupted()){
                    KLog.p(KLog.WARN, "quit renderThread");
                    return;
                }

                synchronized (this) {
                    try {
                        if (!needRender) {
                            wait();
                        }
                        needRender = false;
                        KLog.p(KLog.WARN, "############wakeup");
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        KLog.p(KLog.WARN, "quit renderThread");
                        return;
                    }
                }

                Canvas canvas = textureView.lockCanvas();
                if (null == canvas){
                    KLog.p(KLog.WARN, "lockCanvas failed");
                    continue;
                }

                KLog.p(KLog.WARN, "############op.size=%s",dcOps.size());
                for (DCOp op : dcOps){
                    KLog.p(KLog.WARN, "############op=%s",op);
                    if (DCOp.DRAW_LINE == op.type){
                        lineOp = (DCLineOp) op;
                        canvas.drawLine(lineOp.startX, lineOp.startY, lineOp.stopX, lineOp.stopY, cfgPaint(lineOp.paintCfg));
                    }else if (DCOp.DRAW_RECT == op.type){
                        rectOp = (DCRectOp) op;
                        canvas.drawRect(rectOp.left, rectOp.top, rectOp.right, rectOp.bottom, cfgPaint(rectOp.paintCfg));
                    }else if (DCOp.DRAW_OVAL == op.type){
                        ovalOp = (DCOvalOp) op;
                        rect.set(ovalOp.left, ovalOp.top, ovalOp.right, ovalOp.bottom);
                        canvas.drawOval(rect, cfgPaint(ovalOp.paintCfg));
                    }else if (DCOp.DRAW_PATH == op.type){
                        pathOp = (DCPathOp) op;
                        path.reset();
                        path.moveTo(pathOp.points[0].x, pathOp.points[0].y);
                        for (PointF point : pathOp.points){
                            path.lineTo(point.x, point.y); // NOTE 起点多做了一次lineTo
                        }
                        canvas.drawPath(path, cfgPaint(pathOp.paintCfg));
                    }
//                    try {
//                        sleep(1500);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                        KLog.p(KLog.WARN, "quit renderThread");
//                    }
                }

                textureView.unlockCanvasAndPost(canvas);

            }
        }
    };



    public DefaultPainter(Context context) {

        dcOps = new ConcurrentLinkedQueue<>();  // XXX 限定容量？
        batchDcOps = new PriorityQueue<>();

        paint = new Paint();
        textureView = new TextureView(context);
        textureView.setOpaque(false);

        renderThread.start();
    }



    public View getPaintView(){
        return textureView;
    }



    @Override
    public void startBatchDraw(){
        KLog.p(KLog.WARN, "############startBatchDraw");
        isBatchDrawing = true;
    }

    @Override
    public void draw(DCOp op) {
        if (isBatchDrawing) {
            KLog.p(KLog.WARN, "############batch draw op=%s, size=%s",op, batchDcOps.size());
            batchDcOps.offer(op);
        }else{
            KLog.p(KLog.WARN, "############draw op=%s, size=%s",op, dcOps.size());
            dcOps.offer(op);
            synchronized (renderThread) {
                needRender = true;
                if (Thread.State.WAITING == renderThread.getState()) {
                    KLog.p(KLog.WARN, "############notify");
                    renderThread.notify();
                }
            }
        }
    }


    @Override
    public void finishBatchDraw(){
        KLog.p(KLog.WARN, "############finishBatchDraw");
        if (isBatchDrawing){
            while(!batchDcOps.isEmpty()){
                dcOps.offer(batchDcOps.poll());
            }
            synchronized (renderThread) {
                needRender = true;
                if (Thread.State.WAITING == renderThread.getState()) {
                    KLog.p(KLog.WARN, "############notify");
                    renderThread.notify();
                }
            }
            isBatchDrawing = false;
        }
    }

    private final PorterDuffXfermode DUFFMODE_SRCOVER = new PorterDuffXfermode(PorterDuff.Mode.SRC_OVER);
    private final PorterDuffXfermode DUFFMODE_DSTOVER = new PorterDuffXfermode(PorterDuff.Mode.DST_OVER);
    private Paint cfgPaint(DCPaintCfg paintInfo){
        paint.reset();
//        Paint paint = new Paint();
//        paint.setXfermode(DUFFMODE_DSTOVER);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(paintInfo.strokeWidth);
        paint.setColor(paintInfo.color);
        return paint;
    }


}
