package com.kedacom.vconf.sdk.datacollaborate;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.os.Process;
import android.view.TextureView;

import com.kedacom.vconf.sdk.base.KLog;
import com.kedacom.vconf.sdk.datacollaborate.bean.EOpType;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpDeletePic;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpDraw;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpDrawOval;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpDrawRect;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpErase;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpRectErase;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpInsertPic;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpDrawLine;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpMatrix;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpPaint;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpDragPic;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpUpdatePic;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpDrawPath;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

public class DefaultPainter implements IPainter {

    private Map<String, Integer> roles = new HashMap<>();

    private Map<String, DefaultPaintBoard> paintBoards = new HashMap<>();

    private String curBoardId;

    private Paint paint = new Paint();

    private boolean bNeedRender = false;

    private boolean bPaused = false;

    private OpPaint adjustingOp;
    private final Object adjustingOpLock = new Object();
    private DefaultPaintBoard.IOnPaintOpGeneratedListener onPaintOpGeneratedListener = new DefaultPaintBoard.IOnPaintOpGeneratedListener() {
        @Override
        public void onOp(OpPaint opPaint) {
            synchronized (adjustingOpLock) {
                adjustingOp = opPaint;
            }
            refresh();
        }
    };


    public DefaultPainter(Context context) {
        if (context instanceof LifecycleOwner){
            ((LifecycleOwner)context).getLifecycle().addObserver(new DefaultLifecycleObserver(){
                @Override
                public void onCreate(@NonNull LifecycleOwner owner) { // TODO 为什么我们的sky没走这？难道要通过onResume？
                    KLog.p("LifecycleOwner %s created", owner);
                    start();
                }

                @Override
                public void onResume(@NonNull LifecycleOwner owner) {
                    KLog.p("LifecycleOwner %s resumed", owner);
                    resume();
                }

                @Override
                public void onPause(@NonNull LifecycleOwner owner) {
                    KLog.p("LifecycleOwner %s to be paused", owner);
                    pause();
                }

                @Override
                public void onDestroy(@NonNull LifecycleOwner owner) {
                    KLog.p("LifecycleOwner %s to be destroyed", owner);
                    stop();
                }
            });
        }

    }

    @Override
    public void start() {
        if (renderThread.isAlive()){
            return;
        }
        renderThread.start();
    }

    @Override
    public void pause() {
        bPaused = true;
    }

    @Override
    public void resume() {
        bPaused = false;
        refresh();
    }

    @Override
    public void stop() {
        if (renderThread.isAlive()) {
            renderThread.interrupt();
        }
    }



    private void refresh(){
        synchronized (renderThread) {
            bNeedRender = true;
            if (Thread.State.WAITING == renderThread.getState()) {
                KLog.p(KLog.WARN, "notify");
                renderThread.notify();
            }
        }
    }

    private TextureView.SurfaceTextureListener surfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            KLog.p("surface available");
            refresh();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            KLog.p("surface size changed");
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        }
    };


    @Override
    public void setRoleForAllBoards(int role) {
        for (String boardId : roles.keySet()){
            if (ROLE_AUTHOR == role) {
                paintBoards.get(boardId).setOnPaintOpGeneratedListener(onPaintOpGeneratedListener);
            }else{
                paintBoards.get(boardId).setOnPaintOpGeneratedListener(null);
            }
            roles.put(boardId, role);
        }
    }

    @Override
    public void setRole(String boardId, int role) {
        DefaultPaintBoard board = paintBoards.get(boardId);
        if (null == board){
            KLog.p(KLog.ERROR, "no such board %s", boardId);
            return;
        }
        if (ROLE_AUTHOR == role) {
            board.setOnPaintOpGeneratedListener(onPaintOpGeneratedListener);
        }else{
            board.setOnPaintOpGeneratedListener(null);
        }
        roles.put(boardId, role);
    }

    @Override
    public int getRole(String boardId) {
        if (!paintBoards.keySet().contains(boardId)){
            KLog.p(KLog.ERROR, "no such board %s", boardId);
            return ROLE_UNKNOWN;
        }
        return roles.get(boardId);
    }

    @Override
    public boolean addPaintBoard(IPaintBoard paintBoard) {
        String boardId = paintBoard.getBoardId();
        if (paintBoards.containsKey(boardId)){
            KLog.p(KLog.ERROR,"board %s already exist!", boardId);
            return false;
        }
        DefaultPaintBoard defaultPaintBoard = (DefaultPaintBoard) paintBoard;
        paintBoards.put(boardId, defaultPaintBoard);
        roles.put(boardId, ROLE_COPYER);
        KLog.p(KLog.WARN,"board %s added", paintBoard.getBoardId());

        defaultPaintBoard.getShapePaintView().setSurfaceTextureListener(surfaceTextureListener);
        defaultPaintBoard.getPicPaintView().setSurfaceTextureListener(surfaceTextureListener);

        return true;
    }

    @Override
    public IPaintBoard deletePaintBoard(String boardId) {
        KLog.p(KLog.WARN,"delete board %s", boardId);
        if (boardId.equals(curBoardId)){
            curBoardId = null;
        }
        roles.remove(boardId);
        return paintBoards.remove(boardId);
    }

    @Override
    public void deleteAllPaintBoards() {
        KLog.p(KLog.WARN,"delete all boards");
        paintBoards.clear();
        roles.clear();
        curBoardId = null;
    }


    @Override
    public IPaintBoard switchPaintBoard(String boardId) {
        DefaultPaintBoard paintBoard = paintBoards.get(boardId);
        if(null == paintBoard){
            KLog.p(KLog.ERROR,"no such board %s", boardId);
            return null;
        }
        KLog.p(KLog.WARN, "switched board from %s to %s", curBoardId, boardId);
        curBoardId = boardId;

        return paintBoard;
    }

    @Override
    public IPaintBoard getPaintBoard(String boardId) {
        return paintBoards.get(boardId);
    }

    @Override
    public Set<IPaintBoard> getAllPaintBoards() {
        Set<IPaintBoard> boards = new HashSet<>();
        boards.addAll(paintBoards.values());
        return  boards;
    }

    @Override
    public IPaintBoard getCurrentPaintBoard(){
        if (null == curBoardId) {
            KLog.p(KLog.WARN, "current board is null");
            return null;
        }
        return paintBoards.get(curBoardId);
    }


    @Override
    public void paint(OpPaint op){
        String boardId = op.getBoardId();
        DefaultPaintBoard paintBoard = paintBoards.get(boardId);
        if(null == paintBoard){
            KLog.p(KLog.ERROR,"no board %s for op %s", boardId, op);
            return;
        }
        KLog.p(KLog.WARN, "for board %s op %s", boardId, op);

        DefaultPaintView shapePaintView = paintBoard.getShapePaintView();
        MyConcurrentLinkedDeque<OpPaint> shapeRenderOps = shapePaintView.getRenderOps();
        Matrix shapeMatrix = shapePaintView.getMyMatrix();
        Stack<OpPaint> shapeRepealedOps = shapePaintView.getRepealedOps();

        DefaultPaintView picPaintView = paintBoard.getPicPaintView();
        MyConcurrentLinkedDeque<OpPaint> picRenderOps = picPaintView.getRenderOps();
        Matrix picMatrix = picPaintView.getMyMatrix();
//        Stack<OpPaint> picRepealedOps = picPaintView.getRepealedOps(); // 当前仅支持图形操作的撤销，不支持图片操作撤销。

        // 检查是否为主动绘制触发的响应
        OpPaint shapeTmpOp = shapePaintView.getTmpOps().pollFirst();
        if (null != shapeTmpOp && shapeTmpOp.getUuid().equals(op.getUuid())) {
            KLog.p("tmp op %s confirmed", shapeTmpOp);
            shapeRenderOps.offerLast(shapeTmpOp); // 临时工转正
            return;
        }
        OpPaint picTmpOp = picPaintView.getTmpOps().pollFirst();
        if (null != picTmpOp && picTmpOp.getUuid().equals(op.getUuid())) {
            KLog.p("tmp op %s confirmed", picTmpOp);
            picRenderOps.offerLast(picTmpOp);
            return;
        }

        // 不是主动绘制的响应则清空临时绘制
        shapePaintView.getTmpOps().clear();
        picPaintView.getTmpOps().clear();

        boolean bRefresh = boardId.equals(curBoardId); // 操作属于当前board则尝试立即刷新
        OpPaint tmpOp;

        switch (op.getType()){
            case INSERT_PICTURE:
                picRenderOps.offerLast(op);
                OpInsertPic opInsertPic = (OpInsertPic) op;
                if (null == opInsertPic.getPic()) {
                    if (null != opInsertPic.getPicSavePath()) {
                        opInsertPic.setPic(BitmapFactory.decodeFile(opInsertPic.getPicSavePath())); // TODO 优化。比如大分辨率图片裁剪
                    } else {
                        bRefresh = false; // 图片为空不需刷新界面（图片可能正在下载）
                    }
                }
                paintBoard.picCountChanged();
                break;

            case DELETE_PICTURE:
                bRefresh = false;
                OpPaint picRenderOp;
                for (String picId : ((OpDeletePic)op).getPicIds()) {
                    Iterator it = picRenderOps.iterator();
                    while (it.hasNext()) {
                        picRenderOp = (OpPaint) it.next();
                        if (EOpType.INSERT_PICTURE == picRenderOp.getType()
                                && ((OpInsertPic) picRenderOp).getPicId().equals(picId)) {
                            it.remove();
                            bRefresh = true;
                            break;
                        }
                    }
                }
                if (bRefresh){
                    paintBoard.picCountChanged();
                }
                break;

            case DRAG_PICTURE:
                for (Map.Entry<String, float[]> dragOp : ((OpDragPic)op).getPicMatrices().entrySet()) {
                    for (OpPaint opPaint : picRenderOps) {
                        if (EOpType.INSERT_PICTURE == opPaint.getType()
                                && ((OpInsertPic) opPaint).getPicId().equals(dragOp.getKey())) {
                            ((OpInsertPic) opPaint).setMatrixValue(dragOp.getValue());
                            break;
                        }
                    }
                }
                break;
            case UPDATE_PICTURE:
                OpUpdatePic updatePic = (OpUpdatePic) op;
                for (OpPaint opPaint : picRenderOps) {
                    if (EOpType.INSERT_PICTURE == opPaint.getType()
                            && ((OpInsertPic) opPaint).getPicId().equals(updatePic.getPicId())) {
                        ((OpInsertPic) opPaint).setPicSavePath(updatePic.getPicSavePath());
                        ((OpInsertPic) opPaint).setPic(BitmapFactory.decodeFile(updatePic.getPicSavePath())); // TODO 优化。比如大分辨率图片裁剪
                        break;
                    }
                }
                break;
            case FULLSCREEN_MATRIX: // 全局放缩，包括图片和图形
                picMatrix.set(((OpMatrix)op).getMatrix());
                shapeMatrix.set(((OpMatrix)op).getMatrix());
                paintBoard.zoomRateChanged();
                break;

            default:  // 图形操作

                switch (op.getType()){
                    case UNDO:
                        tmpOp = shapeRenderOps.pollLast(); // 撤销最近的操作
                        if (null != tmpOp){
                            KLog.p(KLog.WARN, "repeal %s",tmpOp);
                            shapeRepealedOps.push(tmpOp); // 缓存撤销的操作以供恢复
                            paintBoard.repealedOpsCountChanged();
                        }else{
                            bRefresh = false;
                        }
                        break;
                    case REDO:
                        if (!shapeRepealedOps.empty()) {
                            tmpOp = shapeRepealedOps.pop();
                            KLog.p(KLog.WARN, "restore %s",tmpOp);
                            shapeRenderOps.offerLast(tmpOp); // 恢复最近操作
                            paintBoard.repealedOpsCountChanged();
                        }else {
                            bRefresh = false;
                        }
                        break;
                    default:

//                        /* 只要不是redo或undo操作，被撤销操作缓存就得清空，因为此时redo操作已失效（
//                        redo操作前面只能是redo操作或者undo操作），而撤销操作缓存仅供redo操作使用。*/
////                        KLog.p(KLog.WARN, "clean repealed ops");
//                        shapeRepealedOps.clear(); // NOTE: 这个留给用户决策。

                        shapeRenderOps.offerLast(op);

        //                KLog.p(KLog.WARN, "need render op %s", op);
                        break;

                }

            break;

        }

        if (bRefresh) {
            refresh();
        }
    }


    private final PorterDuffXfermode DUFFMODE_SRCIN = new PorterDuffXfermode(PorterDuff.Mode.SRC_IN);
//    private final PorterDuffXfermode DUFFMODE_DSTOVER = new PorterDuffXfermode(PorterDuff.Mode.DST_OVER);
    private final PorterDuffXfermode DUFFMODE_CLEAR = new PorterDuffXfermode(PorterDuff.Mode.CLEAR);
    private Paint cfgPaint(OpPaint opPaint){
        paint.reset();
        switch (opPaint.getType()){
            case INSERT_PICTURE:
                paint.setStyle(Paint.Style.STROKE);
                break;
            case RECT_ERASE:
            case CLEAR_SCREEN:
                paint.setStyle(Paint.Style.FILL);
                paint.setXfermode(DUFFMODE_CLEAR);
                break;
            default:
                if (opPaint instanceof OpDraw) {
                    OpDraw opDraw = (OpDraw) opPaint;
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setAntiAlias(true);
                    paint.setStrokeWidth(opDraw.getStrokeWidth());
                    paint.setColor((int) opDraw.getColor());
                    if (OpDraw.DASH == opDraw.getLineStyle()){
                        paint.setPathEffect(new DashPathEffect( new float[]{10, 4},0));
                    }
                    if (EOpType.DRAW_PATH == opPaint.getType()){
                        paint.setStrokeJoin(Paint.Join.ROUND);
                    } else if (EOpType.ERASE == opPaint.getType()){
                        paint.setStrokeJoin(Paint.Join.ROUND);
                        paint.setAlpha(0);
                        paint.setXfermode(DUFFMODE_SRCIN);
                    }
                }
                break;
        }

        return paint;
    }


    private final Thread renderThread = new Thread("DCRenderThr"){
        private Canvas shapePaintViewCanvas;
        private Canvas picPaintViewCanvas;
        private RectF rect = new RectF();
        private Matrix shapeMatrix = new Matrix();
        private Matrix picMatrix = new Matrix();


        @Override
        public void run() {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

            while (true){
                KLog.p("start loop run");
                if (isInterrupted()){
                    KLog.p(KLog.WARN, "quit renderThread");
                    return;
                }

                // 判断当前是否有渲染任务
                synchronized (this) {
                    try {
                        if (!bNeedRender) {
                            KLog.p("waiting...");
                            wait();
                            KLog.p("resume run");
                        }
                        bNeedRender = false;
                        if (bPaused){
                            KLog.p("paused");
                            continue;
                        }
                    } catch (InterruptedException e) {
                        KLog.p(KLog.WARN, "quit renderThread");
                        return;
                    }
                }

                // 获取当前画板
                DefaultPaintBoard paintBoard = paintBoards.get(curBoardId);
                if (null == paintBoard){
                    continue;
                }


                DefaultPaintView shapePaintView = paintBoard.getShapePaintView();

                // 获取图形层画布
                shapePaintViewCanvas = shapePaintView.lockCanvas();  // NOTE: TextureView.lockCanvas()获取的canvas没有硬件加速。
                if (null == shapePaintViewCanvas){
                    KLog.p(KLog.ERROR, "lockCanvas failed");
                    continue;
                }
                // 每次绘制前先清空画布以避免残留
                shapePaintViewCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

                // 设置图形层画布的缩放比例
                shapePaintViewCanvas.setMatrix(shapePaintView.getMyMatrix());

                /* 刷新渲染标志。
                从被唤醒到运行至此可能有新的操作入队列（意味着needRender被重新置为true了），
                接下来我们要开始遍历队列了，此处重新置needRender为false以避免下一轮无谓的重复刷新。*/
                synchronized (this){
                    bNeedRender = false;
                }

                // 图形绘制
                render(shapePaintView.getRenderOps());

                // 临时图形绘制
                render(shapePaintView.getTmpOps());

                // 获取图片层画布
                DefaultPaintView picPaintView = paintBoard.getPicPaintView();
                picPaintViewCanvas = picPaintView.lockCanvas();
                if (null == picPaintViewCanvas){
                    KLog.p(KLog.ERROR, "lockCanvas failed");
                    shapePaintView.unlockCanvasAndPost(shapePaintViewCanvas);
                    continue;
                }

                // 清空画布
                picPaintViewCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

                // 设置画布的缩放比例
                picPaintViewCanvas.setMatrix(picPaintView.getMyMatrix());

                // 图片绘制
                render(picPaintView.getRenderOps());

                // 临时图片绘制
                render(picPaintView.getTmpOps());

                // 绘制正在调整中的操作
                synchronized (adjustingOpLock) {
                    if (null != adjustingOp){
                        render(adjustingOp);
                    }
                }

                // 提交绘制任务，执行绘制
                KLog.p("go render!");
                shapePaintView.unlockCanvasAndPost(shapePaintViewCanvas);
                picPaintView.unlockCanvasAndPost(picPaintViewCanvas);

            }
        }



        private void render(OpPaint op){
            switch (op.getType()) {
                case DRAW_LINE:
                    OpDrawLine lineOp = (OpDrawLine) op;
                    shapePaintViewCanvas.drawLine(lineOp.getStartX(), lineOp.getStartY(), lineOp.getStopX(), lineOp.getStopY(), cfgPaint(lineOp));
                    break;
                case DRAW_RECT:
                    OpDrawRect rectOp = (OpDrawRect) op;
                    shapePaintViewCanvas.drawRect(rectOp.getLeft(), rectOp.getTop(), rectOp.getRight(), rectOp.getBottom(), cfgPaint(rectOp));
                    break;
                case DRAW_OVAL:
                    OpDrawOval ovalOp = (OpDrawOval) op;
                    rect.set(ovalOp.getLeft(), ovalOp.getTop(), ovalOp.getRight(), ovalOp.getBottom());
                    shapePaintViewCanvas.drawOval(rect, cfgPaint(ovalOp));
                    break;
                case DRAW_PATH:
                    OpDrawPath pathOp = (OpDrawPath) op;
                    shapePaintViewCanvas.drawPath(pathOp.getPath(), cfgPaint(pathOp));
                    break;
                case ERASE:
                    OpErase opErase = (OpErase) op;
                    shapePaintViewCanvas.drawPath(opErase.getPath(), cfgPaint(opErase));
                    break;
                case RECT_ERASE:
                    OpRectErase eraseOp = (OpRectErase) op;
                    shapePaintViewCanvas.drawRect(eraseOp.left, eraseOp.top, eraseOp.right, eraseOp.bottom, cfgPaint(eraseOp));
                    break;
                case CLEAR_SCREEN:
                    shapePaintViewCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                    break;
                case INSERT_PICTURE:
                    OpInsertPic insertPicOp = (OpInsertPic) op;
                    if (null != insertPicOp.getPic()) {
//                            int w = insertPicOp.pic.getWidth();
//                            int h = insertPicOp.pic.getHeight();
                        picMatrix.setValues(insertPicOp.getMatrixValue());
//                                KLog.p("to render %s", op);
                        picPaintViewCanvas.drawBitmap(insertPicOp.getPic(), picMatrix, cfgPaint(insertPicOp));
                    }
                    break;
            }
        }

        private void render(MyConcurrentLinkedDeque<OpPaint> ops){
            for (OpPaint op : ops) {  //NOTE: Iterators are weakly consistent. 此遍历过程不感知并发的添加操作，但感知并发的删除操作。
                KLog.p("to render %s", op);
                switch (op.getType()) {
                    case DRAW_LINE:
                        OpDrawLine lineOp = (OpDrawLine) op;
                        shapePaintViewCanvas.drawLine(lineOp.getStartX(), lineOp.getStartY(), lineOp.getStopX(), lineOp.getStopY(), cfgPaint(lineOp));
                        break;
                    case DRAW_RECT:
                        OpDrawRect rectOp = (OpDrawRect) op;
                        shapePaintViewCanvas.drawRect(rectOp.getLeft(), rectOp.getTop(), rectOp.getRight(), rectOp.getBottom(), cfgPaint(rectOp));
                        break;
                    case DRAW_OVAL:
                        OpDrawOval ovalOp = (OpDrawOval) op;
                        rect.set(ovalOp.getLeft(), ovalOp.getTop(), ovalOp.getRight(), ovalOp.getBottom());
                        shapePaintViewCanvas.drawOval(rect, cfgPaint(ovalOp));
                        break;
                    case DRAW_PATH: //TODO 绘制曲线时起毛？
                        OpDrawPath pathOp = (OpDrawPath) op;
                        shapePaintViewCanvas.drawPath(pathOp.getPath(), cfgPaint(pathOp));
                        break;
                    case ERASE:
                        OpErase opErase = (OpErase) op;
                        shapePaintViewCanvas.drawPath(opErase.getPath(), cfgPaint(opErase));
                        break;
                    case RECT_ERASE:
                        OpRectErase eraseOp = (OpRectErase) op;
                        shapePaintViewCanvas.drawRect(eraseOp.left, eraseOp.top, eraseOp.right, eraseOp.bottom, cfgPaint(eraseOp));
                        break;
                    case CLEAR_SCREEN:
                        shapePaintViewCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                        break;
                    case INSERT_PICTURE:
                        OpInsertPic insertPicOp = (OpInsertPic) op;
                        if (null != insertPicOp.getPic()) {
//                            int w = insertPicOp.pic.getWidth();
//                            int h = insertPicOp.pic.getHeight();
                            picMatrix.setValues(insertPicOp.getMatrixValue());
//                                KLog.p("to render %s", op);
                            picPaintViewCanvas.drawBitmap(insertPicOp.getPic(), picMatrix, cfgPaint(insertPicOp));
                        }
                        break;
                }
            }
        }


    };





}
