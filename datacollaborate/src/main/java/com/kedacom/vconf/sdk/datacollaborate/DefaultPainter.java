package com.kedacom.vconf.sdk.datacollaborate;

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

import com.kedacom.vconf.sdk.base.KLog;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpDeletePic;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpDrawOval;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpDrawRect;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpErase;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpInsertPic;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpDrawLine;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpMatrix;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpPaint;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpDragPic;
import com.kedacom.vconf.sdk.datacollaborate.bean.PaintCfg;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpDrawPath;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.ConcurrentLinkedDeque;

public class DefaultPainter implements IPainter {

    private Map<String, DefaultPaintBoard> paintBoards = new HashMap<>();

    private String curBoardId;

    private Paint paint = new Paint();  // TODO 不需要

    private boolean needRender = false;


    public DefaultPainter() {
        renderThread.start();
    }


    @Override
    public int getMyId() {
        return 0;
    }


    @Override
    public void addPaintBoard(IPaintBoard paintBoard) {
        KLog.p(KLog.WARN,"add board %s", paintBoard.getBoardId());
        paintBoards.put(paintBoard.getBoardId(), (DefaultPaintBoard) paintBoard);  // TODO 只能强转吗。工厂模式怎样保证产品一致性的？
    }

    @Override
    public IPaintBoard deletePaintBoard(String boardId) {
        KLog.p(KLog.WARN,"delete board %s", boardId);
        return paintBoards.remove(boardId);
    }

    @Override
    public IPaintBoard switchPaintBoard(String boardId) {
        KLog.p(KLog.WARN, "switch board from %s to %s", curBoardId, boardId);
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
        KLog.p(KLog.WARN,"get board %s", boardId);
        return paintBoards.get(boardId);
    }

    @Override
    public IPaintBoard getCurrentPaintBoard(){
        KLog.p(KLog.WARN,"get current board %s", curBoardId);
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

        DefaultPaintView shapePaintView = paintBoard.getShapePaintView();
        ConcurrentLinkedDeque<OpPaint> shapeRenderOps = shapePaintView.getRenderOps();
        ConcurrentLinkedDeque<OpPaint> shapeMatrixOps = shapePaintView.getMatrixOps();
        Stack<OpPaint> shapeRepealedOps = shapePaintView.getRepealedOps();

        DefaultPaintView picPaintView = paintBoard.getPicPaintView();
        ConcurrentLinkedDeque<OpPaint> picRenderOps = picPaintView.getRenderOps();
        ConcurrentLinkedDeque<OpPaint> picMatrixOps = picPaintView.getMatrixOps();
//        Stack<OpPaint> picRepealedOps = picPaintView.getRepealedOps(); // 当前仅支持图形操作的撤销，不支持图片操作撤销。


        boolean refresh = op.boardId.equals(curBoardId); // 操作属于当前board则尝试立即刷新
        OpPaint tmpOp;

        switch (op.type){
            case OpPaint.OP_INSERT_PICTURE:
                picRenderOps.offer(op);
                break;

            case OpPaint.OP_DELETE_PICTURE:
                refresh = false;
                OpPaint picRenderOp;
                for (String picId : ((OpDeletePic)op).picIds) {
                    Iterator it = picRenderOps.iterator();
                    while (it.hasNext()) {
                        picRenderOp = (OpPaint) it.next();
                        if (OpPaint.OP_INSERT_PICTURE == picRenderOp.type
                                && ((OpInsertPic) picRenderOp).picId.equals(picId)) {
                            it.remove();
                            refresh = true;
                            break;
                        }
                    }
                }
                break;

            case OpPaint.OP_DRAG_PIC:
                for (Map.Entry<String, float[]> dragOp : ((OpDragPic)op).picsMatrix.entrySet()) {
                    for (OpPaint opPaint : picRenderOps) {
                        if (OpPaint.OP_INSERT_PICTURE == opPaint.type
                                && ((OpInsertPic) opPaint).picId.equals(dragOp.getKey())) {
                            ((OpInsertPic) opPaint).matrixValue = dragOp.getValue();
                            break;
                        }
                    }
                }
                break;

            case OpPaint.OP_MATRIX: // 全局放缩，包括图片和图形
                picMatrixOps.offer(op);
                shapeMatrixOps.offer(op);
                break;

            default:  // 图形操作

                switch (op.type){
                    case OpPaint.OP_UNDO:
                        tmpOp = shapeRenderOps.pollLast(); // 撤销最近的操作
                        if (null != tmpOp){
                            KLog.p(KLog.WARN, "repeal %s",tmpOp);
                            shapeRepealedOps.push(tmpOp); // 缓存撤销的操作以供恢复
                        }else{
                            refresh = false;
                        }
                        break;
                    case OpPaint.OP_REDO:
                        if (!shapeRepealedOps.empty()) {
                            tmpOp = shapeRepealedOps.pop();
                            KLog.p(KLog.WARN, "restore %s",tmpOp);
                            shapeRenderOps.offer(tmpOp); // 恢复最近操作
                        }else {
                            refresh = false;
                        }
                        break;
                    default:

                        /* 只要不是redo或undo操作，被撤销操作缓存就得清空，因为此时redo操作已失效（
                        redo操作前面只能是redo操作或者undo操作），而撤销操作缓存仅供redo操作使用。*/
                        KLog.p(KLog.WARN, "clean repealed ops");
                        shapeRepealedOps.clear();

                        shapeRenderOps.offer(op);

        //                KLog.p(KLog.WARN, "need render op %s", op);
                        break;

                }

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


    private final Thread renderThread = new Thread("DCRenderThr"){
        @Override
        public void run() {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
            Path path = new Path();
            RectF rect = new RectF();
            Matrix shapeMatrix = new Matrix();
            Matrix picMatrix = new Matrix();

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

                DefaultPaintBoard paintBoard = paintBoards.get(curBoardId);
                if (null == paintBoard){
                    continue;
                }
                DefaultPaintView shapePaintView = paintBoard.getShapePaintView();

                Canvas canvas = shapePaintView.lockCanvas();  // NOTE: TextureView.lockCanvas()获取的canvas没有硬件加速。
                if (null == canvas){
                    KLog.p(KLog.ERROR, "lockCanvas failed");
                    continue;
                }

                canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR); // 每次绘制前清空画布。

                OpPaint opMatrix = shapePaintView.getMatrixOps().peekLast();  // TODO 暂不考虑完整保存以供回放的功能。matrix就一个值就好。
                if (null != opMatrix) {
                    shapeMatrix.setValues( ((OpMatrix)opMatrix).matrixValue );
                    canvas.setMatrix(shapeMatrix);
                }

                synchronized (this){
                    /* 从被唤醒到运行至此可能有新的操作入队列（意味着needRender被重新置为true了），
                    接下来我们要开始遍历队列了，此处重新置needRender为false以避免下一轮无谓的重复刷新。*/
                    needRender = false;
                }

                // 图形绘制
                ConcurrentLinkedDeque<OpPaint> shapeOps = shapePaintView.getRenderOps();
                for (OpPaint op : shapeOps) {  //NOTE: Iterators are weakly consistent. 此遍历过程不感知并发的添加操作，但感知并发的删除操作。
                    KLog.p("to render %s", op);
                    switch (op.type){
                        case OpPaint.OP_DRAW_LINE:
                            OpDrawLine lineOp = (OpDrawLine) op;
                            canvas.drawLine(lineOp.startX, lineOp.startY, lineOp.stopX, lineOp.stopY, cfgPaint(lineOp.paintCfg));
                            break;
                        case OpPaint.OP_DRAW_RECT:
                            OpDrawRect rectOp = (OpDrawRect) op;
                            canvas.drawRect(rectOp.left, rectOp.top, rectOp.right, rectOp.bottom, cfgPaint(rectOp.paintCfg));
                            break;
                        case OpPaint.OP_DRAW_OVAL:
                            OpDrawOval ovalOp = (OpDrawOval) op;
                            rect.set(ovalOp.left, ovalOp.top, ovalOp.right, ovalOp.bottom);
                            canvas.drawOval(rect, cfgPaint(ovalOp.paintCfg));
                            break;
                        case OpPaint.OP_DRAW_PATH:
                            OpDrawPath pathOp = (OpDrawPath) op;
                            path.reset();
                            path.moveTo(pathOp.points[0].x, pathOp.points[0].y);
                            for (PointF point : pathOp.points) {
                                path.lineTo(point.x, point.y);
                            }
                            canvas.drawPath(path, cfgPaint(pathOp.paintCfg));
                            break;
                        case OpPaint.OP_ERASE:
                            OpErase eraseOp = (OpErase) op;
                            canvas.drawRect(eraseOp.left, eraseOp.top, eraseOp.right, eraseOp.bottom, cfgPaint(eraseOp.paintCfg));
                            break;
                        case OpPaint.OP_CLEAR_SCREEN:
                            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                            break;
                    }

                }

                DefaultPaintView picPaintView = paintBoard.getPicPaintView();
                Canvas picPaintViewCanvas = picPaintView.lockCanvas();
                if (null == picPaintViewCanvas){
                    KLog.p(KLog.ERROR, "lockCanvas failed");
                    shapePaintView.unlockCanvasAndPost(canvas);
                    continue;
                }

                picPaintViewCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

                opMatrix = picPaintView.getMatrixOps().peekLast();
                if (null != opMatrix) {
                    picMatrix.setValues( ((OpMatrix)opMatrix).matrixValue);
                    picPaintViewCanvas.setMatrix(picMatrix);
                }

                // 图片绘制
                ConcurrentLinkedDeque<OpPaint> picOps = picPaintView.getRenderOps();
                for (OpPaint op : picOps){
                    switch (op.type){
                        case OpPaint.OP_INSERT_PICTURE:
                            OpInsertPic insertPicOp = (OpInsertPic) op;
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
