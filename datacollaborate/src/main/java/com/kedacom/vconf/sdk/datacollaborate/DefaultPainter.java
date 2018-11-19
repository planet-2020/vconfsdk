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

import com.kedacom.vconf.sdk.base.KLog;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpDrawOval;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpDrawRect;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpErase;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpInsertPic;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpDrawLine;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpMatrix;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpPaint;
import com.kedacom.vconf.sdk.datacollaborate.bean.PaintCfg;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpDrawPath;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.ConcurrentLinkedDeque;

public class DefaultPainter implements IPainter {

    private static final int LOG_LEVEL = KLog.WARN;

//    private Context context;

    private Map<String, DefaultPaintBoard> paintBoards = new HashMap<>();
    private String curBoardId;
    private Paint paint = new Paint();
    private boolean needRender = false;

    private final Matrix curShapePaintViewMatrix = new Matrix();
    private final Matrix curPicPaintViewMatrix = new Matrix();

    private static int threadCount = 0;

    public DefaultPainter() {
        renderThread.start();
    }

//    public DefaultPainter(Context context) {
//        this.context = context;
//        renderThread.start();
//    }



    @Override
    public int getMyId() {
        return 0;
    }


    @Override
    public void addPaintBoard(IPaintBoard paintBoard) {
        paintBoards.put(paintBoard.getBoardId(), (DefaultPaintBoard) paintBoard);  // TODO 只能强转吗。工厂模式怎样保证产品一致性的？
    }

    @Override
    public IPaintBoard deletePaintBoard(String boardId) {
        KLog.p(LOG_LEVEL,"delete board %s", boardId);
        return paintBoards.remove(boardId);
    }


    @Override
    public IPaintBoard switchPaintBoard(String boardId) {
        KLog.p(LOG_LEVEL, "switch board from %s to %s", curBoardId, boardId);
        DefaultPaintBoard paintBoard = paintBoards.get(boardId);
        if(null == paintBoard){
            KLog.p(KLog.ERROR,"no such board %s", boardId);
            return null;
        }
        curBoardId = boardId;

        return paintBoard;
    }

    @Override
    public IPaintBoard getPaintBoard(String boardId) {
        return paintBoards.get(boardId);
    }

    @Override
    public IPaintBoard getCurrentPaintBoard(){
        return paintBoards.get(curBoardId);
    }


    @Override
    public void paint(OpPaint op){
        KLog.p(KLog.WARN, "for board %s op %s", op.boardId, op);
        DefaultPaintBoard paintBoard = paintBoards.get(op.boardId);
        if(null == paintBoard){
            KLog.p(KLog.ERROR,"no board %s for op %s", op.boardId, op);
            return;
        }

        boolean refresh = op.boardId.equals(curBoardId); // 操作属于当前board则需立即刷新

        ConcurrentLinkedDeque<OpPaint> shapeOps = paintBoard.getShapePaintView().getOps();
        Stack<OpPaint> repealedShapeOps = paintBoard.getShapePaintView().getrepealedOps(); // 当前仅支持图形操作的撤销，不支持图片操作撤销。
        ConcurrentLinkedDeque<OpPaint> picOps = paintBoard.getPicPaintView().getOps();

        OpPaint tmpOp;
        switch (op.type){
            case OpPaint.OP_UNDO:
                tmpOp = shapeOps.pollLast(); // 撤销最近的操作
                if (null != tmpOp){
//                    KLog.p(KLog.WARN, "repeal %s",tmpOp);
                    repealedShapeOps.push(tmpOp); // 缓存撤销的操作以供恢复
                }else{
                    refresh = false;
                }
                break;
            case OpPaint.OP_REDO:
                if (!repealedShapeOps.empty()) {
                    tmpOp = repealedShapeOps.pop();
//                    KLog.p(KLog.WARN, "restore %s",tmpOp);
                    shapeOps.offer(tmpOp); // 恢复最近操作
                }else {
                    refresh = false;
                }
                break;
            default:

                /* 只要不是redo或undo操作，被撤销操作缓存就得清空，因为此时redo操作已失效（
                redo操作前面只能是redo操作或者undo操作），而撤销操作缓存仅供redo操作使用。*/
                repealedShapeOps.clear();

                if (OpPaint.OP_MATRIX == op.type){  // TODO matrix操作也当作普通操作对待。放到线程里面去遍历然后设置。
                /* matrix操作不同于普通绘制操作。从它生效开始，
                它作用于所有之前的以及之后的操作，不受时序的制约，所以需要特殊处理。*/
                    synchronized (curShapePaintViewMatrix) {
                        curShapePaintViewMatrix.setValues(((OpMatrix) op).matrixValue);
                    }
                }else
                if (OpPaint.OP_INSERT_PICTURE == op.type){
                    picOps.offer(op);
                }else {
                    shapeOps.offer(op);
                }
//                KLog.p(KLog.WARN, "need render op %s", op);
                break;

        }

        if (refresh) {
            synchronized (renderThread) {
                needRender = true;
                if (Thread.State.WAITING == renderThread.getState()) {
                    KLog.p(KLog.WARN, "notify");
                    renderThread.notify();
                }
            }
        }
    }


    private final PorterDuffXfermode DUFFMODE_SRCOVER = new PorterDuffXfermode(PorterDuff.Mode.SRC_OVER);
    private final PorterDuffXfermode DUFFMODE_DSTOVER = new PorterDuffXfermode(PorterDuff.Mode.DST_OVER);
    private final PorterDuffXfermode DUFFMODE_CLEAR = new PorterDuffXfermode(PorterDuff.Mode.CLEAR);
    private Paint cfgPaint(PaintCfg paintInfo){
        paint.reset();
        if (PaintCfg.MODE_NORMAL == paintInfo.mode){
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(paintInfo.strokeWidth);
            paint.setColor(paintInfo.color);
        }else if (PaintCfg.MODE_PICTURE == paintInfo.mode){
            paint.setStyle(Paint.Style.STROKE);
        }else if (PaintCfg.MODE_ERASE == paintInfo.mode){
            paint.setStyle(Paint.Style.FILL);
            paint.setXfermode(DUFFMODE_CLEAR);
        }

        return paint;
    }

    public static final int TOOL_PENCIL = 100;
    public static final int TOOL_LINE = 101;
    public static final int TOOL_RECT = 102;
    public static final int TOOL_OVAL = 103;
    public static final int TOOL_ERASER = 104;
    public static final int TOOL_HAND = 105;

    private int tool = TOOL_PENCIL;
    void setTool(int tool){
        this.tool = tool;
    }

    private Paint authorPaint;
    void setPaint(Paint paint){
        authorPaint = paint;
    }

    public static final int ROLE_COPIER = 200;
    public static final int ROLE_AUTHOR = 201;
    private int role;
    void setRole(int role){
        this.role = role;
    }

    public void onShapePaintViewMatrixChanged(Matrix newMatrix) {
        synchronized (curShapePaintViewMatrix) {
            curShapePaintViewMatrix.set(newMatrix);
        }
        KLog.p("newMatrix=%s", curShapePaintViewMatrix);
        synchronized (renderThread) {
            needRender = true;
            if (Thread.State.WAITING == renderThread.getState()) {
                KLog.p(KLog.WARN, "notify");
                renderThread.notify();
            }
        }
    }

    public void onPicPaintViewMatrixChanged(Matrix newMatrix) {
        synchronized (curPicPaintViewMatrix) {
            curPicPaintViewMatrix.set(newMatrix);
        }
        KLog.p("newMatrix=%s", curPicPaintViewMatrix);
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
            OpDrawLine lineOp;
            OpDrawRect rectOp;
            OpDrawOval ovalOp;
            OpDrawPath pathOp;
            OpErase eraseOp;
            OpInsertPic insertPicOp;
            Path path = new Path();
            RectF rect = new RectF();
            Matrix picMatrix = new Matrix();

            DefaultPaintBoard paintBoard;
            DefaultPaintView shapePaintView;
            DefaultPaintView picPaintView;
            ConcurrentLinkedDeque<OpPaint> shapeOps;
            ConcurrentLinkedDeque<OpPaint> picOps;

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

                paintBoard = paintBoards.get(curBoardId);
                if (null == paintBoard){
                    continue;
                }
                shapePaintView = paintBoard.getShapePaintView();

                Canvas canvas = shapePaintView.lockCanvas();  // NOTE: TextureView.lockCanvas()获取的canvas没有硬件加速。
                if (null == canvas){
                    KLog.p(KLog.ERROR, "lockCanvas failed");
                    continue;
                }

                canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR); // 每次绘制前清空画布。

                synchronized (curShapePaintViewMatrix){
                    canvas.setMatrix(curShapePaintViewMatrix);
                }

                synchronized (this){
                    /* 从被唤醒到运行至此可能有新的操作入队列（意味着needRender被重新置为true了），
                    接下来我们要开始遍历队列了，此处重新置needRender为false以避免下一轮无谓的重复刷新。*/
                    needRender = false;
                }
                shapeOps = shapePaintView.getOps();
                for (OpPaint op : shapeOps) {  //NOTE: Iterators are weakly consistent. 此遍历过程不感知并发的添加操作，但感知并发的删除操作。
                    KLog.p("to render %s", op);
                    switch (op.type){
                        case OpPaint.OP_DRAW_LINE:
                            lineOp = (OpDrawLine) op;
                            canvas.drawLine(lineOp.startX, lineOp.startY, lineOp.stopX, lineOp.stopY, cfgPaint(lineOp.paintCfg));
                            break;
                        case OpPaint.OP_DRAW_RECT:
                            rectOp = (OpDrawRect) op;
                            canvas.drawRect(rectOp.left, rectOp.top, rectOp.right, rectOp.bottom, cfgPaint(rectOp.paintCfg));
                            break;
                        case OpPaint.OP_DRAW_OVAL:
                            ovalOp = (OpDrawOval) op;
                            rect.set(ovalOp.left, ovalOp.top, ovalOp.right, ovalOp.bottom);
                            canvas.drawOval(rect, cfgPaint(ovalOp.paintCfg));
                            break;
                        case OpPaint.OP_DRAW_PATH:
                            pathOp = (OpDrawPath) op;
                            path.reset();
                            path.moveTo(pathOp.points[0].x, pathOp.points[0].y);
                            for (PointF point : pathOp.points) {
                                path.lineTo(point.x, point.y);
                            }
                            canvas.drawPath(path, cfgPaint(pathOp.paintCfg));
                            break;
                        case OpPaint.OP_ERASE:
                            eraseOp = (OpErase) op;
                            canvas.drawRect(eraseOp.left, eraseOp.top, eraseOp.right, eraseOp.bottom, cfgPaint(eraseOp.paintCfg));
                            break;
                        case OpPaint.OP_CLEAR_SCREEN:
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

                picPaintView = paintBoard.getPicPaintView();
                Canvas picPaintViewCanvas = picPaintView.lockCanvas();
                if (null == picPaintViewCanvas){
                    KLog.p(KLog.ERROR, "lockCanvas failed");
                    shapePaintView.unlockCanvasAndPost(canvas);
                    continue;
                }

                picPaintViewCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

                synchronized (curPicPaintViewMatrix){
                    picPaintViewCanvas.setMatrix(curPicPaintViewMatrix);
                }
                picOps = picPaintView.getOps();
                for (OpPaint op : picOps){
                    switch (op.type){
                        case OpPaint.OP_INSERT_PICTURE:
                            insertPicOp = (OpInsertPic) op;
//                            int w = insertPicOp.pic.getWidth();
//                            int h = insertPicOp.pic.getHeight();
                            picMatrix.setValues(insertPicOp.matrixValue);
                            KLog.p("to render %s", op);
                            picPaintViewCanvas.drawBitmap(insertPicOp.pic, picMatrix, cfgPaint(insertPicOp.paintCfg));
                            break;
                    }
                }


                shapePaintView.unlockCanvasAndPost(canvas);

                picPaintView.unlockCanvasAndPost(picPaintViewCanvas);

                KLog.p("end of loop run, go render!");
            }
        }
    };

}
