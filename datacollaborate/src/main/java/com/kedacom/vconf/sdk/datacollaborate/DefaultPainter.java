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
import java.util.concurrent.ConcurrentLinkedDeque;

public class DefaultPainter implements IDCPainter {

    private TextureView textureView;
    private Paint paint;

//    private ConcurrentLinkedQueue<DCOp> renderOps;
    private ConcurrentLinkedDeque<DCOp> renderOps; // 待渲染的操作，如画线、画图等。XXX require API 21
//    private DCOp lastOp;
    private PriorityQueue<DCOp> batchOps; // 批量操作的缓存
    private PriorityQueue<DCOp> repealedOps;  // 被撤销的操作
//    private DCOp batchLastOp;

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

            int layer=0;

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
//                layer = canvas.saveLayer(null, null);
                KLog.p(KLog.WARN, "############textureView.isHWA=%s, cache enabled=%s, canvas=%s, op.size=%s",
                        textureView.isHardwareAccelerated(), textureView.isDrawingCacheEnabled(), canvas, renderOps.size());
                for (DCOp op : renderOps){  //NOTE: Iterators are weakly consistent. 此遍历过程不感知并发的添加操作。
//                DCOp op;
//                while(null != (op = renderOps.poll())) {

                    KLog.p(KLog.WARN, "############op=%s", op);
                    if (DCOp.OP_DRAW_LINE == op.type) {
                        lineOp = (DCLineOp) op;
                        canvas.drawLine(lineOp.startX, lineOp.startY, lineOp.stopX, lineOp.stopY, cfgPaint(lineOp.paintCfg));
                    } else if (DCOp.OP_DRAW_RECT == op.type) {
                        rectOp = (DCRectOp) op;
                        canvas.drawRect(rectOp.left, rectOp.top, rectOp.right, rectOp.bottom, cfgPaint(rectOp.paintCfg));
                    } else if (DCOp.OP_DRAW_OVAL == op.type) {
                        ovalOp = (DCOvalOp) op;
                        rect.set(ovalOp.left, ovalOp.top, ovalOp.right, ovalOp.bottom);
                        canvas.drawOval(rect, cfgPaint(ovalOp.paintCfg));
                    } else if (DCOp.OP_DRAW_PATH == op.type) {
                        pathOp = (DCPathOp) op;
                        path.reset();
                        path.moveTo(pathOp.points[0].x, pathOp.points[0].y);
                        for (PointF point : pathOp.points) {
                            path.lineTo(point.x, point.y); // NOTE 起点多做了一次lineTo
                        }
                        canvas.drawPath(path, cfgPaint(pathOp.paintCfg));
                    }
                    try {
                        sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        KLog.p(KLog.WARN, "quit renderThread");
                    }
//                }
                }

//                canvas.restoreToCount(layer);

                textureView.unlockCanvasAndPost(canvas);

            }
        }
    };



    public DefaultPainter(Context context) {

//        renderOps = new ConcurrentLinkedQueue<>();  // XXX 限定容量？
        renderOps = new ConcurrentLinkedDeque<>();  // XXX 限定容量？
        batchOps = new PriorityQueue<>();
        repealedOps = new PriorityQueue<>();

        paint = new Paint();
        textureView = new TextureView(context);
//        textureView.setOpaque(false);

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
            KLog.p(KLog.WARN, "############batch draw op=%s, size=%s",op, batchOps.size());
            batchOps.offer(op);
        }else{
            KLog.p(KLog.WARN, "############draw op=%s, size=%s",op, renderOps.size());
            if (DCOp.OP_REDO == op.type){
                // TODO
            }else if (DCOp.OP_UNDO == op.type){
                renderOps.pollLast(); // 删掉最后一个操作
            }else {
                renderOps.offer(op);
            }
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
//            PriorityQueue<DCOp> ops = new PriorityQueue<>();
            DCOp op;
            int redoCnt = 0;
            ConcurrentLinkedDeque<DCOp> needRenderOps = new ConcurrentLinkedDeque<>();
//            boolean isDirty = false;
            while(!batchOps.isEmpty()){ // 整理批量操作。NOTE: 时序上越近的操作排在队列的越前端。时序上我们是从后往前遍历。// NOTE: 这样做会丢失部分操作，尽管当前使用场景下没问题，但如果新增类似“步骤回放”的需求，则此方案需改进。
                op = batchOps.poll();
                KLog.p(KLog.WARN, "############op=%s", op);
                if (DCOp.OP_REDO == op.type){
                    ++redoCnt; // redo的作用只有一个就是用来抵消undo，它之前只可能是连续的redo或与之匹配的undo。故此处只做计数，尝试下次遍历与前面的undo抵消。
                }else if (DCOp.OP_UNDO == op.type){  // undo之前可以是普通的图元绘制操作也可以是redo操作
                    if (0 != redoCnt){
                        --redoCnt; // undo被redo抵消
                        continue;
                    }
                    batchOps.poll(); // 没有被redo抵消则撤销之前的操作
//                    if (null!=discardOp) repealedOps.offer(discardOp); // 保存被撤销的操作以供“恢复”
                }else { // 非redo非undo
                    /* redo之前只可能是连续的redo或与之匹配的undo。
                    如果出现了非undo非redo则正常情况下redo操作已和相应的undo抵消，或根本未出现redo，
                    再或者redo未被同等数量的undo抵消则出现异常（平台给的消息序列有问题），异常情况下我们丢弃冗余的redo。
                    以上情况下我们均清空redo计数*/
                    redoCnt = 0;
                    needRenderOps.offerFirst(op); // 倒着入队列，这样时序最早的排在队首，恢复了正常次序。
//                    renderOps.offerFirst(op); // 倒着入队列，这样时序最早的排在队首，方便我们待会的绘制。
                    KLog.p(KLog.WARN, "############offer op=%s", op);
//                    isDirty = true;
                }


//                ops.offer(op);


//                renderOps.offer(op);
            }

            if (!needRenderOps.isEmpty()) {
                renderOps.addAll(needRenderOps);
                synchronized (renderThread) {
                    needRender = true;
                    if (Thread.State.WAITING == renderThread.getState()) {
                        renderThread.notify();
                    }
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
//        paint.setXfermode(null);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(paintInfo.strokeWidth);
        paint.setColor(paintInfo.color);
        return paint;
    }


}
