package com.kedacom.vconf.sdk.datacollaborate;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
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
import com.kedacom.vconf.sdk.datacollaborate.bean.DCMatrixOp;
import com.kedacom.vconf.sdk.datacollaborate.bean.DCOp;
import com.kedacom.vconf.sdk.datacollaborate.bean.DCOvalOp;
import com.kedacom.vconf.sdk.datacollaborate.bean.DCPaintCfg;
import com.kedacom.vconf.sdk.datacollaborate.bean.DCPathOp;
import com.kedacom.vconf.sdk.datacollaborate.bean.DCRectOp;

import java.util.PriorityQueue;
import java.util.Stack;
import java.util.concurrent.ConcurrentLinkedDeque;

public class DefaultPainter implements IDCPainter {

    private TextureView textureView;
    private Paint paint;

//    private ConcurrentLinkedQueue<DCOp> renderOps;
    private ConcurrentLinkedDeque<DCOp> renderOps; // 待渲染的操作，如画线、画图等。NOTE: require API 21
//    private DCOp lastOp;
    private PriorityQueue<DCOp> batchOps; // 批量操作缓存。批量模式下操作到达的时序可能跟操作的序列号顺序不相符，此处我们使用PriorityQueue来为我们自动排序。
    private Stack<DCOp> repealedOps;  // 被撤销的操作，缓存以供恢复
//    private DCOp batchLastOp;


    private final Matrix matrix = new Matrix(); // TODO 如果需要回放功能则所有matrix操作均需完整保存而非现在的只保存最终的。

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
            DCMatrixOp matrixOp;
            Path path = new Path();
            RectF rect = new RectF();

            int layer=0;

            while (true){
                KLog.p("go render!");
                if (isInterrupted()){
                    KLog.p(KLog.WARN, "quit renderThread");
                    return;
                }

                synchronized (this) {
                    try {
                        if (!needRender) {
                            KLog.p("wait...");
                            wait();
                            KLog.p("wakeup!");
                        }
                        needRender = false;
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        KLog.p(KLog.WARN, "quit renderThread");
                        return;
                    }
                }

                Canvas canvas = textureView.lockCanvas();
                if (null == canvas){
                    KLog.p(KLog.ERROR, "lockCanvas failed");
                    continue;
                }

                KLog.p("clear canvas");
                canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR); // 清空画布。

                synchronized (matrix){
                    canvas.setMatrix(matrix);
                }
//                layer = canvas.saveLayer(null, null);
//                KLog.p(KLog.WARN, "############textureView.isHWA=%s, cache enabled=%s, canvas=%s, op.size=%s",
//                        textureView.isHardwareAccelerated(), textureView.isDrawingCacheEnabled(), canvas, renderOps.size());

                if (renderOps.isEmpty()){
                    KLog.p("clear canvas");
                    canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR); // 清空画布。（触发场景一般是清屏操作或者多次撤销至画布空白）
                }else {

                    for (DCOp op : renderOps) {  //NOTE: Iterators are weakly consistent. 此遍历过程不感知并发的添加操作。
//                DCOp op;
//                while(null != (op = renderOps.poll())) {

                        KLog.p("render %s", op);
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
                        KLog.p("sleeping...");
                        sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        KLog.p(KLog.WARN, "quit renderThread");
                    }

//                }
                    }
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
        repealedOps = new Stack<>();

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
        KLog.p(KLog.WARN, "startBatchDraw");
        batchOps.clear();
        isBatchDrawing = true;
    }

    @Override
    public void draw(DCOp op) {
        if (isBatchDrawing) {
            KLog.p(KLog.WARN, "batch draw %s",op);
            batchOps.offer(op);
        }else{
            //NOTE: 非batch-draw模式下我们没有检查操作的序列号，平台必须保证给的操作序列号跟实际时序相符，即序列号为1的最先到达，2的其次，以此类推，否则会有问题。
            DCOp tmpOp = null;
            boolean dirty = true;
            KLog.p(KLog.WARN, "draw %s",op);
            if (DCOp.OP_REDO == op.type){
                if (!repealedOps.empty()) tmpOp = repealedOps.pop();
                if (null != tmpOp){
                    KLog.p(KLog.WARN, "restore %s",tmpOp);
                    renderOps.offer(tmpOp); // 恢复最近操作
                }else{
                    dirty = false;
                }
            }else if (DCOp.OP_UNDO == op.type){
                tmpOp = renderOps.pollLast(); // 撤销最近的操作
                if (null != tmpOp){
                    KLog.p(KLog.WARN, "repeal %s",tmpOp);
                    repealedOps.push(tmpOp); // 缓存撤销的操作以供恢复
                }else{
                    dirty = false;
                }
            }else if (DCOp.OP_MATRIX == op.type){
                synchronized (matrix) {
                    matrix.setValues(((DCMatrixOp) op).matrixValue);
                }
            }else {
                repealedOps.clear(); // 只要不是redo或undo操作，被撤销操作缓存就没有了意义得清空。因为撤销操作缓存仅供redo操作使用，而普通绘制操作后redo操作即刻失效（redo操作必须跟在redo操作或者undo操作后面）。
                renderOps.offer(op);
                KLog.p(KLog.WARN, "to render op %s", op);
            }

            if (dirty) {
                synchronized (renderThread) {
                    needRender = true;
                    if (Thread.State.WAITING == renderThread.getState()) {
                        KLog.p(KLog.WARN, "notify");
                        renderThread.notify();
                    }
                }
            }
        }
    }


    @Override
    public void finishBatchDraw(){
        KLog.p(KLog.WARN, "finishBatchDraw");
        if (isBatchDrawing){
            DCOp op;
            int redoCnt = 0;
            ConcurrentLinkedDeque<DCOp> needRenderOps = new ConcurrentLinkedDeque<>();
            boolean dirty = true;
            while(!batchOps.isEmpty()){ // 整理批量操作。NOTE: 时序上越近的操作排在队列的越前端。时序上我们是从后往前遍历。
                op = batchOps.poll();
                KLog.p(KLog.WARN, "op %s", op);
                if (DCOp.OP_REDO == op.type){
                    ++redoCnt; // redo的作用只有一个就是用来抵消undo，它之前只可能是连续的redo或与之匹配的undo。故此处只做计数，尝试下次遍历与前面的undo抵消。
                    dirty = false;
                }else if (DCOp.OP_UNDO == op.type){
                    if (0 != redoCnt){
                        --redoCnt; // undo被redo抵消
                        dirty = false;
                        continue;
                    }
                    batchOps.poll(); // 没有被redo抵消则撤销之前的操作
                }else { // 非redo非undo

                    /* redo之前只可能是连续的redo或与之匹配的undo。
                    如果出现了非undo非redo则正常情况下redo操作已和相应的undo抵消，或根本未出现redo，
                    再或者redo未被同等数量的undo抵消则出现异常（平台给的消息序列有问题），异常情况下我们丢弃冗余的redo。
                    以上情况下redo计数均需清空*/
                    redoCnt = 0;

                    if (DCOp.OP_MATRIX == op.type){
                        synchronized (matrix) {
                            matrix.setValues(((DCMatrixOp) op).matrixValue);
                        }
                    }else {
                        needRenderOps.offerFirst(op); // 倒着入队列，这样时序最早的排在队首，恢复了正常次序。
                    }
                    KLog.p(KLog.WARN, "to render op %s", op);
                }

            }

            if (dirty) {
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
