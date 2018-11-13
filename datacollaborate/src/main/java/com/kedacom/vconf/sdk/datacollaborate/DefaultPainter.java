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
import android.view.View;
import android.widget.FrameLayout;

import com.kedacom.vconf.sdk.base.KLog;
import com.kedacom.vconf.sdk.datacollaborate.bean.DCEraseOp;
import com.kedacom.vconf.sdk.datacollaborate.bean.DCInsertPicOp;
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

public class DefaultPainter implements IDCPainter{

    private static final int LOG_LEVEL = KLog.WARN;

    private FrameLayout layout;

    private DefaultWhiteBoard whiteBoard;
    private DefaultPaintView shapePaintView;
    private DefaultPaintView picPaintView;

    private Paint paint;

    private ConcurrentLinkedDeque<DCOp> shapeOps; // 图形操作，如画线、画圆、画路径等。NOTE: require API 21
    private ConcurrentLinkedDeque<DCOp> picOps; // 图片操作，如插入图片、删除图片等。
    private PriorityQueue<DCOp> batchOps; // 批量操作缓存。批量模式下操作到达的时序可能跟操作的序列号顺序不相符，此处我们使用PriorityQueue来为我们自动排序。
    private Stack<DCOp> repealedOps;  // 被撤销的操作，缓存以供恢复

    private final Matrix shapePaintViewMatrix = new Matrix();
    private final Matrix picPaintViewMatrix = new Matrix();

    private boolean isBatchDrawing = false;  // TODO batch drawing 放到manager中做掉，这里拿到的操作保证是时序正确的。去掉start/finBatchDrawing接口。
    private boolean needRender = false;

    private static int threadCount = 0;




    public DefaultPainter(Context context) {

        shapeOps = new ConcurrentLinkedDeque<>();  // XXX 限定容量？
        picOps = new ConcurrentLinkedDeque<>();  // XXX 限定容量？
        batchOps = new PriorityQueue<>();
        repealedOps = new Stack<>();

        paint = new Paint();

        whiteBoard = new DefaultWhiteBoard(context);
        picPaintView = whiteBoard.getPicPaintView();
        picPaintView.setOnMatrixChangedListener(this::onPicPaintViewMatrixChanged);
        shapePaintView = whiteBoard.getShapePaintView();
        shapePaintView.setOnMatrixChangedListener(this::onShapePaintViewMatrixChanged);

        renderThread.start();
    }



    public View getPaintView(){
//        return shapePaintView;
        return whiteBoard;
    }



    @Override
    public void startBatchDraw(){
        if (isBatchDrawing){
            return;
        }
        KLog.p(KLog.WARN, ">>>>>>>>>>>>>>>>>>>>>>");
        batchOps.clear();
        isBatchDrawing = true;
    }

    @Override
    public void draw(DCOp op) {
        KLog.p(KLog.WARN, "op %s",op);
        if (isBatchDrawing) {
            batchOps.offer(op);
            return;
        }

        /*NOTE: 非batch-draw模式下我们没有检查操作的序列号，平台必须保证给的操作序列号跟实际时序相符，
        即序列号为1的最先到达，2的其次，以此类推，否则绘制会乱序。*/
        DCOp tmpOp;
        boolean dirty = true;
        switch (op.type){
            case DCOp.OP_REDO:
                if (!repealedOps.empty()) {
                    tmpOp = repealedOps.pop();
                    KLog.p(KLog.WARN, "restore %s",tmpOp);
                    shapeOps.offer(tmpOp); // 恢复最近操作
                }else {
                    dirty = false;
                }
                break;
            case DCOp.OP_UNDO:
                tmpOp = shapeOps.pollLast(); // 撤销最近的操作
                if (null != tmpOp){
                    KLog.p(KLog.WARN, "repeal %s",tmpOp);
                    repealedOps.push(tmpOp); // 缓存撤销的操作以供恢复
                }else{
                    dirty = false;
                }
                break;
            default:

                /* 只要不是redo或undo操作，被撤销操作缓存就得清空，因为此时redo操作已失效（
                redo操作前面只能是redo操作或者undo操作），而撤销操作缓存仅供redo操作使用。*/
                repealedOps.clear();

                if (DCOp.OP_MATRIX == op.type){
                /* matrix操作不同于普通绘制操作。从它生效开始，
                它作用于所有之前的以及之后的操作，不受时序的制约，所以需要特殊处理。*/
                    synchronized (shapePaintViewMatrix) {
                        shapePaintViewMatrix.setValues(((DCMatrixOp) op).matrixValue);
                    }
                }else if (DCOp.OP_INSERT_PICTURE == op.type){
                    picOps.offer(op);
                }else {
                    shapeOps.offer(op);
                }
                KLog.p(KLog.WARN, "need render op %s", op);
                break;

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


    @Override
    public void finishBatchDraw(){
        if (!isBatchDrawing) {
            return;
        }
        KLog.p(KLog.WARN, "-----------------------");

        DCOp op;
        int redoCnt = 0;
        ConcurrentLinkedDeque<DCOp> needRenderOps = new ConcurrentLinkedDeque<>();
        boolean dirty = true;
        while(!batchOps.isEmpty()){ // 整理批量操作。NOTE: 时序上越近的操作排在队列的越前端。时序上我们是从后往前遍历。
            op = batchOps.poll();
            switch (op.type){
                case DCOp.OP_REDO:
                    ++redoCnt; // redo的作用只有一个就是用来抵消undo，它之前只可能是连续的redo或与之匹配的undo。故此处只做计数，尝试下次遍历与前面的undo抵消。
                    dirty = false;
                    KLog.p(KLog.WARN, "redo, redoCount=%s", redoCnt);
                    break;
                case DCOp.OP_UNDO:
                    if (0 != redoCnt){
                        --redoCnt; // undo被redo抵消
                        dirty = false;
                        KLog.p(KLog.WARN, "undo, redoCount=%s", redoCnt);
                        continue;
                    }
                    KLog.p(KLog.WARN, "undo, repealed op %s", batchOps.peek());
                    batchOps.poll(); // 没有被redo抵消则撤销之前的操作
                    break;
                default:
                    /* redo之前只可能是连续的redo或与之匹配的undo。
                    如果出现了非undo非redo则正常情况下redo操作已和相应的undo抵消，或根本未出现redo，
                    再或者redo未被同等数量的undo抵消则出现异常（平台给的消息序列有问题），异常情况下我们丢弃冗余的redo。
                    以上情况下redo计数均需清空*/
                    redoCnt = 0;

                    if (DCOp.OP_MATRIX == op.type){
                        synchronized (shapePaintViewMatrix) {
                            shapePaintViewMatrix.setValues(((DCMatrixOp) op).matrixValue);
                        }
                    }else {
                        needRenderOps.offerFirst(op); // 倒着入队列，这样时序最早的排在队首，恢复了正常次序。
                    }
                    KLog.p(KLog.WARN, "need render op %s", op);
                    break;

            }

        }

        if (dirty) {
            shapeOps.addAll(needRenderOps);
            synchronized (renderThread) {
                needRender = true;
                if (Thread.State.WAITING == renderThread.getState()) {
                    KLog.p(KLog.WARN, "notify");
                    renderThread.notify();
                }
            }
        }

        isBatchDrawing = false;


        KLog.p(KLog.WARN, "<<<<<<<<<<<<<<<<<<<<<<<");
    }

    private final PorterDuffXfermode DUFFMODE_SRCOVER = new PorterDuffXfermode(PorterDuff.Mode.SRC_OVER);
    private final PorterDuffXfermode DUFFMODE_DSTOVER = new PorterDuffXfermode(PorterDuff.Mode.DST_OVER);
    private final PorterDuffXfermode DUFFMODE_CLEAR = new PorterDuffXfermode(PorterDuff.Mode.CLEAR);
    private Paint cfgPaint(DCPaintCfg paintInfo){
        paint.reset();
        if (DCPaintCfg.MODE_NORMAL == paintInfo.mode){
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(paintInfo.strokeWidth);
            paint.setColor(paintInfo.color);
        }else if (DCPaintCfg.MODE_PICTURE == paintInfo.mode){
            paint.setStyle(Paint.Style.STROKE);
        }else if (DCPaintCfg.MODE_ERASE == paintInfo.mode){
            paint.setStyle(Paint.Style.FILL);
            paint.setXfermode(DUFFMODE_CLEAR);
        }

        return paint;
    }


    public void onShapePaintViewMatrixChanged(Matrix newMatrix) {
        synchronized (shapePaintViewMatrix) {
            shapePaintViewMatrix.set(newMatrix);
        }
        KLog.p("newMatrix=%s", shapePaintViewMatrix);
        synchronized (renderThread) {
            needRender = true;
            if (Thread.State.WAITING == renderThread.getState()) {
                KLog.p(KLog.WARN, "notify");
                renderThread.notify();
            }
        }
    }

    public void onPicPaintViewMatrixChanged(Matrix newMatrix) {
        synchronized (picPaintViewMatrix) {
            picPaintViewMatrix.set(newMatrix);
        }
        KLog.p("newMatrix=%s", picPaintViewMatrix);
        synchronized (renderThread) {
            needRender = true;
            if (Thread.State.WAITING == renderThread.getState()) {
                KLog.p(KLog.WARN, "notify");
                renderThread.notify();
            }
        }
    }


    private final Thread renderThread = new Thread("DCRenderThr"+threadCount++){
        @Override
        public void run() {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
            DCLineOp lineOp;
            DCRectOp rectOp;
            DCOvalOp ovalOp;
            DCPathOp pathOp;
            DCEraseOp eraseOp;
            DCInsertPicOp insertPicOp;
            Path path = new Path();
            RectF rect = new RectF();
            Matrix picMatrix = new Matrix();

            int layer=0;

            while (true){
                KLog.p("start loop run");
                if (isInterrupted()){
                    KLog.p(KLog.WARN, "quit renderThread");
                    return;
                }

                synchronized (this) {
                    try {
                        if (!needRender) {
                            KLog.p("waiting...");
                            wait();
                            KLog.p("resume run");
                        }
                        needRender = false;
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        KLog.p(KLog.WARN, "quit renderThread");
                        return;
                    }
                }

                Canvas canvas = shapePaintView.lockCanvas();  // NOTE: TextureView.lockCanvas()获取的canvas没有硬件加速。
                if (null == canvas){
                    KLog.p(KLog.ERROR, "lockCanvas failed");
                    continue;
                }

                canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR); // 每次绘制前清空画布。

                synchronized (shapePaintViewMatrix){
                    canvas.setMatrix(shapePaintViewMatrix);
                }
//                layer = canvas.saveLayer(null, null);
//                KLog.p(KLog.WARN, "############shapePaintView.isHWA=%s, cache enabled=%s, canvas=%s, op.size=%s",
//                        shapePaintView.isHardwareAccelerated(), shapePaintView.isDrawingCacheEnabled(), canvas, shapeOps.size());

                for (DCOp op : shapeOps) {  //NOTE: Iterators are weakly consistent. 此遍历过程不感知并发的添加操作，但感知并发的删除操作。
                    KLog.p("to render %s", op);
                    switch (op.type){
                        case DCOp.OP_DRAW_LINE:
                            lineOp = (DCLineOp) op;
                            canvas.drawLine(lineOp.startX, lineOp.startY, lineOp.stopX, lineOp.stopY, cfgPaint(lineOp.paintCfg));
                            break;
                        case DCOp.OP_DRAW_RECT:
                            rectOp = (DCRectOp) op;
                            canvas.drawRect(rectOp.left, rectOp.top, rectOp.right, rectOp.bottom, cfgPaint(rectOp.paintCfg));
                            break;
                        case DCOp.OP_DRAW_OVAL:
                            ovalOp = (DCOvalOp) op;
                            rect.set(ovalOp.left, ovalOp.top, ovalOp.right, ovalOp.bottom);
                            canvas.drawOval(rect, cfgPaint(ovalOp.paintCfg));
                            break;
                        case DCOp.OP_DRAW_PATH:
                            pathOp = (DCPathOp) op;
                            path.reset();
                            path.moveTo(pathOp.points[0].x, pathOp.points[0].y);
                            for (PointF point : pathOp.points) {
                                path.lineTo(point.x, point.y);
                            }
                            canvas.drawPath(path, cfgPaint(pathOp.paintCfg));
                            break;
                        case DCOp.OP_ERASE:
                            eraseOp = (DCEraseOp) op;
                            canvas.drawRect(eraseOp.left, eraseOp.top, eraseOp.right, eraseOp.bottom, cfgPaint(eraseOp.paintCfg));
                            break;
                        case DCOp.OP_CLEAR_SCREEN:
                            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                            break;
                    }

//                    try {
//                        KLog.p("sleeping...");
//                        sleep(1000);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                        KLog.p(KLog.WARN, "quit renderThread");
//                    }

                }

//                canvas.restoreToCount(layer);

                KLog.p("end of loop run, go render!");
                shapePaintView.unlockCanvasAndPost(canvas);


                Canvas picPaintViewCanvas = picPaintView.lockCanvas();
                if (null == picPaintViewCanvas){
                    KLog.p(KLog.ERROR, "lockCanvas failed");
                    continue;
                }

                picPaintViewCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR); // 每次绘制前清空画布。

                synchronized (picPaintViewMatrix){
                    picPaintViewCanvas.setMatrix(picPaintViewMatrix);
                }
                for (DCOp op : picOps){
                    switch (op.type){
                        case DCOp.OP_INSERT_PICTURE:
                            insertPicOp = (DCInsertPicOp) op;
//                            int w = insertPicOp.pic.getWidth();
//                            int h = insertPicOp.pic.getHeight();
                            picMatrix.setValues(insertPicOp.matrixValue);
                            KLog.p("to render %s", op);
                            picPaintViewCanvas.drawBitmap(insertPicOp.pic, picMatrix, cfgPaint(insertPicOp.paintCfg));
                            break;
                    }
                }
                picPaintView.unlockCanvasAndPost(picPaintViewCanvas);

            }
        }
    };


}
