package com.kedacom.vconf.sdk.datacollaborate;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;
import android.view.SurfaceView;
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


    private SurfaceView surfaceView;
    private TextureView textureView;
    private Canvas canvas;
    private Paint paint;

    private HandlerThread handlerThread;
    private Handler handler;

    private ConcurrentLinkedQueue<DCOp> dcOps;
    private PriorityQueue<DCOp> batchDcOps;

    private boolean isBatchDrawing = false;

    private final Thread renderThread = new Thread("DCRenderThr"){
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
                if (isInterrupted()){
                    KLog.p(KLog.WARN, "quit renderThread");
                    return;
                }

                synchronized (this) {
                    try {
                        wait();
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

                for (DCOp op : dcOps){
                    if (op instanceof DCLineOp){
                        lineOp = (DCLineOp) op;
                        canvas.drawLine(lineOp.startX, lineOp.startY, lineOp.stopX, lineOp.stopY, cfgPaint(lineOp.paintCfg));
                    }else if (op instanceof DCRectOp){
                        rectOp = (DCRectOp) op;
                        canvas.drawRect(rectOp.left, rectOp.top, rectOp.right, rectOp.bottom, cfgPaint(rectOp.paintCfg));
                    }else if (op instanceof DCOvalOp){
                        ovalOp = (DCOvalOp) op;
                        rect.set(ovalOp.left, ovalOp.top, ovalOp.right, ovalOp.bottom);
                        canvas.drawOval(rect, cfgPaint(ovalOp.paintCfg));
                    }else if (op instanceof DCPathOp){
                        pathOp = (DCPathOp) op;
                        path.reset();
                        path.moveTo(pathOp.points[0].x, pathOp.points[0].y);
                        for (PointF point : pathOp.points){
                            path.lineTo(point.x, point.y); // NOTE 起点多做了一次lineTo
                        }
                        canvas.drawPath(path, cfgPaint(pathOp.paintCfg));
                    }
                }

                textureView.unlockCanvasAndPost(canvas);

            }
        }
    };



    public DefaultPainter(Context context) {

        dcOps = new ConcurrentLinkedQueue<>();  // XXX 限定容量？
        batchDcOps = new PriorityQueue<>(); // TODO 提供排序方法

        handlerThread = new HandlerThread("DC.OpThr", Process.THREAD_PRIORITY_BACKGROUND);
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());

        paint = new Paint();
        surfaceView = new SurfaceView(context);
        textureView = new TextureView(context);
        textureView.setOpaque(false);
        textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                KLog.p("######################### surface=%s", surface);
//                drawLine(0, 0, 10, 80, new DCPaintCfg(10, 0x7FFF0000));
//                drawLine(0, 0, 80, 10, new DCPaintCfg(10, 0x7FFF0000));
//                drawLine(0, 0, 80, 78, new DCPaintCfg(10, 0x7FFF0000));
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
                KLog.p("surface=%s", surface);
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                KLog.p("surface=%s", surface);
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {
                KLog.p("surface=%s", surface);
            }
        });

        renderThread.start();

    }



    public View getPaintView(){
        return textureView;
//        return surfaceView;
    }



    @Override
    public void startBatchDraw(){
        isBatchDrawing = true;
    }

    @Override
    public void draw(DCOp op) {
        if (isBatchDrawing) {
            batchDcOps.offer(op);
        }else{
            dcOps.offer(op);
            synchronized (renderThread) {
                renderThread.notify();
            }
        }
    }


    @Override
    public void finishBatchDraw(){
        if (isBatchDrawing){
            while(!batchDcOps.isEmpty()){
                dcOps.offer(batchDcOps.poll());
            }
            isBatchDrawing = false;
            synchronized (renderThread) {
                renderThread.notify();
            }
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
