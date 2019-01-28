package com.kedacom.vconf.sdk.datacollaborate;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.os.Process;

import com.kedacom.vconf.sdk.base.KLog;
import com.kedacom.vconf.sdk.datacollaborate.bean.EOpType;
import com.kedacom.vconf.sdk.datacollaborate.bean.IRepealable;
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

public class DefaultPainter implements IPainter {

    private Context context;

    private Map<String, DefaultPaintBoard> paintBoards = new LinkedHashMap<>();

    private String curBoardId;

    private Paint paint = new Paint();

    private boolean bNeedRender = false;

    private boolean bPaused = false;

    // 调整中的操作。比如画线时，从手指按下到手指拿起之间的绘制都是“调整中”的。
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

        this.context = context;

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
                    clean();
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


    private void clean(){
        deleteAllPaintBoards();
    }

    private void refresh(){
        synchronized (renderThread) {
            bNeedRender = true;
            if (Thread.State.WAITING == renderThread.getState()) {
//                KLog.p(KLog.WARN, "notify");
                renderThread.notify();
            }
        }
    }


    @Override
    public boolean addPaintBoard(IPaintBoard paintBoard) {
        String boardId = paintBoard.getBoardId();
        if (paintBoards.containsKey(boardId)){
            KLog.p(KLog.ERROR,"board %s already exist!", boardId);
            return false;
        }
        DefaultPaintBoard defaultPaintBoard = (DefaultPaintBoard) paintBoard;
        defaultPaintBoard.setOnPaintOpGeneratedListener(onPaintOpGeneratedListener);
        paintBoards.put(boardId, defaultPaintBoard);
        KLog.p(KLog.WARN,"board %s added", paintBoard.getBoardId());

        return true;
    }

    @Override
    public IPaintBoard deletePaintBoard(String boardId) {
        KLog.p(KLog.WARN,"delete board %s", boardId);
        if (boardId.equals(curBoardId)){
            curBoardId = null;
        }
        DefaultPaintBoard board =  paintBoards.remove(boardId);
        if (null != board){
            board.clean();
        }
        return board;
    }

    @Override
    public void deleteAllPaintBoards() {
        KLog.p(KLog.WARN,"delete all boards");
        for (String boardId : paintBoards.keySet()){
            paintBoards.get(boardId).clean();
        }
        paintBoards.clear();
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

    /**
     * 获取所有画板
     * @return 所有画板列表，顺序同添加时的顺序。
     * */
    @Override
    public List<IPaintBoard> getAllPaintBoards() {
        List<IPaintBoard> boards = new ArrayList<>();
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
    public int getPaintBoardCount() {
        return paintBoards.size();
    }


    @Override
    public void paint(OpPaint op){
        String boardId = op.getBoardId();
        DefaultPaintBoard paintBoard = paintBoards.get(boardId);
        if(null == paintBoard){
            KLog.p(KLog.ERROR,"no board %s for op %s", boardId, op);
            return;
        }
        KLog.p(KLog.WARN, "#for board %s op %s", boardId, op);

        MyConcurrentLinkedDeque<OpPaint> shapeRenderOps = paintBoard.getShapeOps();
        Stack<OpPaint> shapeRepealedOps = paintBoard.getRepealedShapeOps();

        MyConcurrentLinkedDeque<OpPaint> picRenderOps = paintBoard.getPicOps();

        // 检查是否为主动绘制触发的响应。若是则我们不再重复绘制，因为它已经展示在界面上。
        OpPaint shapeTmpOp = paintBoard.getTmpShapeOps().pollFirst();
        if (null != shapeTmpOp && shapeTmpOp.getUuid().equals(op.getUuid())) {
            KLog.p("tmp op %s confirmed", shapeTmpOp);
            if (!shapeRepealedOps.isEmpty() && shapeTmpOp instanceof IRepealable) {
                //撤销/恢复操作流被“可撤销”操作中断，则重置撤销/恢复相关状态
                shapeRepealedOps.clear();
                paintBoard.repealableStateChanged();
            }
            boolean bEmpty = shapeRenderOps.isEmpty();
            shapeRenderOps.offerLast(shapeTmpOp); // 临时工转正
            if (bEmpty && shapeTmpOp instanceof IRepealable){ // 可撤销操作从无到有
                paintBoard.repealableStateChanged();
            }
            return;
        }

        // 不是主动绘制的响应则清空临时绘制 // TODO 本端还是加时间戳吧3S超时可清空，清空在绘制线程中重绘时去做，不在这里做
        paintBoard.getTmpShapeOps().clear();

        boolean bRefresh = boardId.equals(curBoardId); // 操作属于当前board则尝试立即刷新
        OpPaint tmpOp;

        switch (op.getType()){
            case INSERT_PICTURE:
                for (OpPaint opPaint : paintBoard.getPicOps()){
                    if (opPaint instanceof OpInsertPic
                            && ((OpInsertPic)opPaint).getPicId().equals(((OpInsertPic)op).getPicId())
                            ){
                        KLog.p("pic op %s already exist!", opPaint);
                        return;
                    }
                }
                picRenderOps.offerLast(op);
                OpInsertPic opInsertPic = (OpInsertPic) op;
                opInsertPic.setBoardMatrix(paintBoard.getBoardMatrix());
                if (null == opInsertPic.getPic()) {
                    if (null != opInsertPic.getPicPath()) {
                        opInsertPic.setPic(BitmapFactory.decodeFile(opInsertPic.getPicPath())); // TODO 优化。比如大分辨率图片裁剪
                    } else {
                        bRefresh = false; // 图片为空不需刷新界面（图片可能正在下载）
                    }
                }

                Bitmap pic = opInsertPic.getPic();
                if (null != pic){
                    /*计算mixMatrix。
                    可将mixMatrix理解为：把左上角在原点处的未经缩放的图片变换为对端所描述的图片（插入图片时传过来的insertPos, picWidth, picHeight这些信息所描述的图片）
                    所需经历的矩形变换*/
                    PointF insertPos = opInsertPic.getInsertPos();
                    Matrix mixMatrix = MatrixHelper.calcTransMatrix(new RectF(0, 0, pic.getWidth(), pic.getHeight()),
                            new RectF(insertPos.x, insertPos.y, insertPos.x+opInsertPic.getPicWidth(), insertPos.y+opInsertPic.getPicHeight()));
                    opInsertPic.setMixMatrix(mixMatrix);

                    // 计算picMatrix= mixMatrix * transMatrix / boardMatrix
                    Matrix picMatrix = new Matrix(mixMatrix);
                    picMatrix.postConcat(opInsertPic.getTransMatrix());
                    picMatrix.postConcat(MatrixHelper.invert(opInsertPic.getBoardMatrix()));
                    opInsertPic.setMatrix(picMatrix);
                }

                paintBoard.picCountChanged();

                break;

            case DELETE_PICTURE:
                bRefresh = false;
                OpPaint picRenderOp;
                boolean got =false;
                for (String picId : ((OpDeletePic)op).getPicIds()) {
                    got = false;
                    Iterator it = picRenderOps.iterator();
                    while (it.hasNext()) {
                        picRenderOp = (OpPaint) it.next();
                        if (EOpType.INSERT_PICTURE == picRenderOp.getType()
                                && ((OpInsertPic) picRenderOp).getPicId().equals(picId)) {
                            it.remove();
                            got = true;
                            bRefresh = true;
                            break;
                        }
                    }
                    if (!got) {
                        // 可能图片正在被编辑
                        if (paintBoard.isExistEditingPic(picId)){
                            got = true;
                            bRefresh = true;
                            paintBoard.delEditingPic(picId);
                        }
                    }
                }
                if (bRefresh){
                    paintBoard.picCountChanged();
                }
                break;

            case DRAG_PICTURE: // TODO NOTE 己端拖动图片也会触发此通知，尚未过滤，需过滤。
                for (Map.Entry<String, Matrix> dragOp : ((OpDragPic)op).getPicMatrices().entrySet()) {
                    for (OpPaint opPaint : picRenderOps) {
                        if (EOpType.INSERT_PICTURE == opPaint.getType()
                                && ((OpInsertPic) opPaint).getPicId().equals(dragOp.getKey())) {
                            opInsertPic = (OpInsertPic) opPaint;
                            if (null != opInsertPic.getPic()) {
                                /*图片已经准备好了则直接求取picmatrix
                                 * mixMatrix * dragMatrix = picMatrix * boardMatrix
                                 * => picMatrix = mixMatrix * dragMatrix / boardMatrix
                                 * */
                                Matrix matrix = new Matrix(opInsertPic.getMixMatrix());
                                matrix.postConcat(dragOp.getValue());
                                matrix.postConcat(MatrixHelper.invert(paintBoard.getBoardMatrix()));
                                if (matrix.equals(opInsertPic.getMatrix())) {
                                    // 计算出的matrix跟当前matrix相等则不需拖动
                                    bRefresh = false;
                                }else{
                                    opInsertPic.setMatrix(matrix);
                                }
                            }else{
                                // 图片尚未准备好，我们先保存matrix，等图片准备好了再计算
                                opInsertPic.setDragMatrix(dragOp.getValue());
                                bRefresh = false; // 图片尚未准备好不需刷新
                            }
                            break;
                        }
                    }
                }
                break;
            case UPDATE_PICTURE: // XXX 己端插入图片也会收到该通知，需过滤
                OpUpdatePic updatePic = (OpUpdatePic) op;
                for (OpPaint opPaint : picRenderOps) {
                    if (EOpType.INSERT_PICTURE == opPaint.getType()
                            && ((OpInsertPic) opPaint).getPicId().equals(updatePic.getPicId())) {
                        opInsertPic = (OpInsertPic) opPaint;
                        opInsertPic.setPicPath(updatePic.getPicSavePath());
                        pic = BitmapFactory.decodeFile(updatePic.getPicSavePath());
                        opInsertPic.setPic(pic); // TODO 优化。比如大分辨率图片裁剪
                        if (null != pic){
                            PointF insertPos = opInsertPic.getInsertPos();
                            Matrix mixMatrix = MatrixHelper.calcTransMatrix(new RectF(0, 0, pic.getWidth(), pic.getHeight()),
                                    new RectF(insertPos.x, insertPos.y, insertPos.x+opInsertPic.getPicWidth(), insertPos.y+opInsertPic.getPicHeight()));
                            opInsertPic.setMixMatrix(mixMatrix);

                            Matrix picMatrix = new Matrix(opInsertPic.getMixMatrix());
                            Matrix dragMatrix = opInsertPic.getDragMatrix();
                            if (null != dragMatrix) { // XXX 还有其它影响图片matrix的操作呢？
                                /* 在此之前有图片拖动操作则以图片拖动操作中的dragMatrix来计算picMatrix
                                 * picMatrix = mixMatrix * dragMatrix / boardMatrix*/
                                picMatrix.postConcat(opInsertPic.getDragMatrix());
                                opInsertPic.setDragMatrix(null);
                            }else{
                                // picMatrix = mixMatrix * transMatrix / boardMatrix
                                picMatrix.postConcat(opInsertPic.getTransMatrix());
                            }

                            picMatrix.postConcat(MatrixHelper.invert(opInsertPic.getBoardMatrix()));

                            opInsertPic.setMatrix(picMatrix);
                        }
                        break;
                    }
                }
                break;
            case FULLSCREEN_MATRIX: // 全局放缩、位移，包括图片和图形
                paintBoard.setBoardMatrix(((OpMatrix)op).getMatrix());
                paintBoard.zoomRateChanged();
                break;

            default:  // 图形操作

                switch (op.getType()){
                    case UNDO:
                        tmpOp = shapeRenderOps.pollLast(); // 撤销最近的操作
                        if (null != tmpOp){
                            KLog.p(KLog.WARN, "repeal %s",tmpOp);
                            shapeRepealedOps.push(tmpOp); // 缓存撤销的操作以供恢复
                            paintBoard.repealableStateChanged();
                        }else{
                            bRefresh = false;
                        }
                        break;
                    case REDO:
                        if (!shapeRepealedOps.empty()) {
                            tmpOp = shapeRepealedOps.pop();
                            KLog.p(KLog.WARN, "restore %s",tmpOp);
                            shapeRenderOps.offerLast(tmpOp); // 恢复最近操作
                            paintBoard.repealableStateChanged();
                        }else {
                            bRefresh = false;
                        }
                        break;
                    default:

                        if (!shapeRepealedOps.isEmpty() && op instanceof IRepealable) {
//                            KLog.p(KLog.WARN, "clean repealed ops");
                            //撤销/恢复操作流被“可撤销”操作中断，则重置撤销/恢复相关状态
                            shapeRepealedOps.clear();
                            paintBoard.repealableStateChanged();
                        }
                        boolean bEmpty = shapeRenderOps.isEmpty(); // XXX 如果shapeOps中将来也有不可撤销操作呢？也就是说将来可能不能简单根据是否空来判断是否该改变可撤销状态
                        shapeRenderOps.offerLast(op);
                        if (bEmpty && op instanceof IRepealable){ // 可撤销操作从无到有
                            paintBoard.repealableStateChanged();
                        }

                        if (EOpType.CLEAR_SCREEN == op.getType()){
                            paintBoard.screenCleared();
                        }
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
                        int w = ((OpErase)opDraw).getWidth();
                        int h = ((OpErase)opDraw).getHeight();
                        paint.setStrokeWidth(w>h?w:h);
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
        private RectF rect = new RectF();
        private Matrix matrix = new Matrix();

        @Override
        public void run() {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

            while (true){
//                KLog.p("start loop run");
                if (isInterrupted()){
                    KLog.p(KLog.WARN, "quit renderThread");
                    return;
                }

                // 判断当前是否有渲染任务
                synchronized (this) {
                    try {
                        if (!bNeedRender) {
//                            KLog.p("waiting...");
                            wait();
//                            KLog.p("resume run");
                        }
                        bNeedRender = false;
                        if (bPaused){
//                            KLog.p("paused");
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

                synchronized (this) {
                    bNeedRender = false;
                }

                matrix.set(paintBoard.getDensityRelativeBoardMatrix());

                // 图形层绘制
                Canvas shapePaintViewCanvas = paintBoard.lockCanvas(IPaintBoard.LAYER_SHAPE);
                if (null != shapePaintViewCanvas) {
                    // 每次绘制前先清空画布以避免残留
                    shapePaintViewCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

                    // 设置画布matrix
                    shapePaintViewCanvas.setMatrix(matrix);

                    // 图形绘制
                    render(paintBoard.getShapeOps(), shapePaintViewCanvas);

                    // 临时图形绘制
                    render(paintBoard.getTmpShapeOps(), shapePaintViewCanvas);

                    // 绘制正在调整中的操作
                    synchronized (adjustingOpLock) {
                        if (null != adjustingOp) {
                            render(adjustingOp, shapePaintViewCanvas);
                        }
                    }
                }

                // 图片层绘制
                Canvas picPaintViewCanvas = paintBoard.lockCanvas(IPaintBoard.LAYER_PIC);
                if (null != picPaintViewCanvas) {  // TODO 优化，尝试如果没有影响图片层的操作，如插入/删除/拖动/放缩图片，就不刷新图片层。
                    // 清空画布
                    picPaintViewCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

                    // 设置画布matrix
                    picPaintViewCanvas.setMatrix(matrix);

                    // 图片绘制
                    render(paintBoard.getPicOps(), picPaintViewCanvas);
                }

                // 临时图片层绘制
                Canvas tmpPaintViewCanvas = paintBoard.lockCanvas(IPaintBoard.LAYER_PIC_TMP);
                if (null != tmpPaintViewCanvas) {
                    // 清空画布
                    tmpPaintViewCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

                    // 设置缩放比例
                    tmpPaintViewCanvas.setMatrix(paintBoard.getTmpPicViewMatrix());

                    // 绘制
                    MyConcurrentLinkedDeque<DefaultPaintBoard.PicEditStuff> picEditStuffs = paintBoard.getPicEditStuffs();
                    for (DefaultPaintBoard.PicEditStuff picEditStuff : picEditStuffs) {
                        render(picEditStuff.pic, tmpPaintViewCanvas);
                        render(picEditStuff.dashedRect, tmpPaintViewCanvas);
                        render(picEditStuff.delIcon, tmpPaintViewCanvas);
                    }
                }

                // 提交绘制任务，执行绘制
//                KLog.p("go render!");
                paintBoard.unlockCanvasAndPost(IPaintBoard.LAYER_SHAPE, shapePaintViewCanvas);
                paintBoard.unlockCanvasAndPost(IPaintBoard.LAYER_PIC, picPaintViewCanvas);
                paintBoard.unlockCanvasAndPost(IPaintBoard.LAYER_PIC_TMP, tmpPaintViewCanvas);

            }
        }



        private void render(OpPaint op, Canvas canvas){
            KLog.p("to render %s", op);
            switch (op.getType()) {
                case DRAW_LINE:
                    OpDrawLine lineOp = (OpDrawLine) op;
                    canvas.drawLine(lineOp.getStartX(), lineOp.getStartY(), lineOp.getStopX(), lineOp.getStopY(), cfgPaint(lineOp));
                    break;
                case DRAW_RECT:
                    OpDrawRect rectOp = (OpDrawRect) op;
                    canvas.drawRect(rectOp.getLeft(), rectOp.getTop(), rectOp.getRight(), rectOp.getBottom(), cfgPaint(rectOp));
                    break;
                case DRAW_OVAL:
                    OpDrawOval ovalOp = (OpDrawOval) op;
                    rect.set(ovalOp.getLeft(), ovalOp.getTop(), ovalOp.getRight(), ovalOp.getBottom());
                    canvas.drawOval(rect, cfgPaint(ovalOp));
                    break;
                case DRAW_PATH:
                    OpDrawPath pathOp = (OpDrawPath) op;
                    canvas.drawPath(pathOp.getPath(), cfgPaint(pathOp));
                    break;
                case ERASE:
                    OpErase opErase = (OpErase) op;
                    canvas.drawPath(opErase.getPath(), cfgPaint(opErase));
                    break;
                case RECT_ERASE:
                    OpRectErase eraseOp = (OpRectErase) op;
                    canvas.drawRect(eraseOp.getLeft(), eraseOp.getTop(), eraseOp.getRight(), eraseOp.getBottom(), cfgPaint(eraseOp));
                    break;
                case CLEAR_SCREEN:
                    canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                    break;
                case INSERT_PICTURE:
                    OpInsertPic insertPicOp = (OpInsertPic) op;
                    if (null != insertPicOp.getPic()) {
                        canvas.drawBitmap(insertPicOp.getPic(), insertPicOp.getMatrix(), cfgPaint(insertPicOp));
                    }
                    break;
            }
        }

        private void render(MyConcurrentLinkedDeque<OpPaint> ops, Canvas canvas){
            for (OpPaint op : ops) {  //NOTE: Iterators are weakly consistent. 此遍历过程不感知并发的添加操作，但感知并发的删除操作。
                render(op, canvas);
            }
        }


    };





}
