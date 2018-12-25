package com.kedacom.vconf.sdk.datacollaborate;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

import com.kedacom.vconf.sdk.base.KLog;
import com.kedacom.vconf.sdk.datacollaborate.bean.BoardInfo;
import com.kedacom.vconf.sdk.datacollaborate.bean.EOpType;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpClearScreen;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpDraw;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpDrawLine;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpDrawOval;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpDrawPath;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpDrawRect;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpErase;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpInsertPic;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpMatrix;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpPaint;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpRectErase;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpRedo;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpUndo;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

public class DefaultPaintBoard extends FrameLayout implements IPaintBoard{
    private Context context;

    // 图形画板。用于图形绘制如画线、画圈、擦除等等
    private DefaultPaintView shapePaintView;

    // 图片画板
    private DefaultPaintView picPaintView;

    // 临时画板。用于临时展示一些操作的中间效果，如插入图片、选中图片
    private DefaultPaintView tmpPaintView;
    // 删除图片按钮
    private Bitmap del_pic_icon;
    private Bitmap del_pic_active_icon;

    // 图层
    private int focusedLayer = LAYER_PIC_AND_SHAPE;

    // 工具
    private int tool = TOOL_PENCIL;

    // 画笔粗细。单位：pixel
    private int paintStrokeWidth = 5;

    // 画笔颜色
    private long paintColor = 0xFFFFFFFFL;

    // 橡皮擦尺寸。单位：pixel
    private int eraserSize = 25;

    private static final int MIN_ZOOM = 25;
    private static final int MAX_ZOOM = 400;

    private BoardInfo boardInfo;

    private IOnPictureCountChanged onPictureCountChangedListener;
    private IOnRepealableStateChangedListener onRepealableStateChangedListener;
    private IOnZoomRateChangedListener onZoomRateChangedListener;
    private IOnPaintOpGeneratedListener paintOpGeneratedListener;
    private IPublisher publisher;

    public DefaultPaintBoard(@NonNull Context context, BoardInfo boardInfo) {
        super(context);
        this.context = context;
        this.boardInfo = boardInfo;
        LayoutInflater layoutInflater = LayoutInflater.from(context);
        View whiteBoard = layoutInflater.inflate(R.layout.default_whiteboard_layout, this);
        picPaintView = whiteBoard.findViewById(R.id.pb_pic_paint_view);
        picPaintView.setOpaque(false);
        shapePaintView = whiteBoard.findViewById(R.id.pb_shape_paint_view);
        shapePaintView.setOpaque(false);
        tmpPaintView = whiteBoard.findViewById(R.id.pb_tmp_paint_view);
        tmpPaintView.setOpaque(false);

        try {
            AssetManager am = context.getAssets();
            InputStream is = am.open("del_pic.png");
            del_pic_icon = BitmapFactory.decodeStream(is);
            is.close();
            is = am.open("del_pic_active.png");
            del_pic_active_icon = BitmapFactory.decodeStream(is);
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        setBackgroundColor(Color.DKGRAY);
    }

    public DefaultPaintBoard(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (LAYER_NONE == focusedLayer){
            return true;
        }else if (LAYER_PIC == focusedLayer){
            return picPaintView.dispatchTouchEvent(ev);
        }else if (LAYER_SHAPE == focusedLayer){
            return shapePaintView.dispatchTouchEvent(ev);
        }else if (LAYER_TMP == focusedLayer){
            return tmpPaintView.dispatchTouchEvent(ev);
        }else if (LAYER_PIC_AND_SHAPE == focusedLayer || LAYER_ALL == focusedLayer){
            boolean ret2 = picPaintView.dispatchTouchEvent(ev);
            boolean ret1 = shapePaintView.dispatchTouchEvent(ev); // 事件先给pic层再给shape层，所以公共的publish操作在shape层事件处理完后做。
            return ret1||ret2;
        }

        return false;
    }


    DefaultPaintView.IOnEventListener shapeViewEventListener = new DefaultPaintView.IOnEventListener(){

        @Override
        public void onDragBegin(float x, float y) {
//            KLog.p("~~> x=%s, y=%s", x, y);
            createShapeOp(x, y);
        }

        @Override
        public void onDrag(float x, float y) {
//            KLog.p("~~> x=%s, y=%s", x, y);
            adjustShapeOp(x, y);
            if (null != paintOpGeneratedListener) paintOpGeneratedListener.onOp(opPaint);
        }

        @Override
        public void onDragEnd() {
//            KLog.p("~~>");
            confirmShapeOp();
            KLog.p("new tmp op %s", opPaint);
            shapePaintView.getTmpOps().offerLast(opPaint);
            if (null != paintOpGeneratedListener) paintOpGeneratedListener.onOp(null);
            publisher.publish(opPaint);
            opPaint = null;
        }


        @Override
        public void onMultiFingerDrag(float dx, float dy) {
//            KLog.p("~~> dx=%s, dy=%s", dx, dy);
            shapePaintView.getMyMatrix().postTranslate(dx, dy);
            if (null != paintOpGeneratedListener) paintOpGeneratedListener.onOp(null);
        }

        @Override
        public void onMultiFingerDragEnd() {
//            KLog.p("~~>");
            OpMatrix opMatrix = new OpMatrix(shapePaintView.getMyMatrix());
            assignBasicInfo(opMatrix);
            publisher.publish(opMatrix);
        }

        @Override
        public void onScale(float factor, float scaleCenterX, float scaleCenterY) {
//            KLog.p("~~> factor=%s", factor);
            shapePaintView.getMyMatrix().postScale(factor, factor, scaleCenterX, scaleCenterY);
            if (null != paintOpGeneratedListener) paintOpGeneratedListener.onOp(null);
            zoomRateChanged();
        }

        @Override
        public void onScaleEnd() {
//            KLog.p("~~>");
            OpMatrix opMatrix = new OpMatrix(shapePaintView.getMyMatrix());
            assignBasicInfo(opMatrix);
            publisher.publish(opMatrix);
        }

    };


    DefaultPaintView.IOnEventListener picViewEventListener = new DefaultPaintView.IOnEventListener(){
        @Override
        public void onDragBegin(float x, float y) {
            KLog.p("~~> x=%s, y=%s", x, y);
        }

        @Override
        public void onDrag(float x, float y) {
            KLog.p("~~> x=%s, y=%s", x, y);
        }

        @Override
        public void onDragEnd() {
            KLog.p("~~>");
        }


        @Override
        public void onMultiFingerDrag(float dx, float dy) {
            KLog.p("~~> dx=%s, dy=%s", dx, dy);
            picPaintView.getMyMatrix().postTranslate(dx, dy);
//            paintOpGeneratedListener.onOp(null); // XXX 当前图形和图片层放缩倍数同步一致，如果将来不一致，则需单独处理
        }

        @Override
        public void onMultiFingerDragEnd() {
            KLog.p("~~>");
//            OpMatrix opMatrix = new OpMatrix(picPaintView.getMyMatrix());
//            assignBasicInfo(opMatrix);
//            publisher.publish(opMatrix); // XXX 当前图形和图片层放缩倍数同步一致，如果将来不一致，则需单独处理
        }

        @Override
        public void onScale(float factor, float scaleCenterX, float scaleCenterY) {
//            KLog.p("~~> factor=%s", factor);
            picPaintView.getMyMatrix().postScale(factor, factor, scaleCenterX, scaleCenterY);
//            paintOpGeneratedListener.onOp(null);
//            zoomRateChanged(); // XXX 当前图形和图片层放缩倍数同步一致，如果将来不一致，则需单独处理
        }

        @Override
        public void onScaleEnd() {
            KLog.p("~~>");
//            OpMatrix opMatrix = new OpMatrix(picPaintView.getMyMatrix());
//            assignBasicInfo(opMatrix);
//            publisher.publish(opMatrix); // XXX 当前图形和图片层放缩倍数同步一致，如果将来不一致，则需单独publish
        }

        @Override
        public void onLongPress(float x, float y) {
            KLog.p("~~> x=%s, y=%s", x, y);
            // TODO 获取点中的图片；从图片层删除；添加到tmp层
        }

    };


    DefaultPaintView.IOnEventListener tmpViewEventListener = new DefaultPaintView.IOnEventListener(){
        private boolean hasInsertPicMsg;
        @Override
        public void onDown(float x, float y) {
            // TODO 如果落在删除图标中则删除并置hasInsertPicMsg为false
            hasInsertPicMsg = handler.hasMessages(MSGID_INSERT_PIC);
            if (hasInsertPicMsg) {
                handler.removeMessages(MSGID_INSERT_PIC);
            }
        }

        @Override
        public void onUp(float x, float y) {
            if (hasInsertPicMsg) {
                handler.sendEmptyMessageDelayed(MSGID_INSERT_PIC, 3000);
            }
        }

        @Override
        public void onMultiFingerDrag(float dx, float dy) {
            KLog.p("~~> dx=%s, dy=%s", dx, dy);
            tmpPaintView.getMyMatrix().postTranslate(dx, dy);
            if (null != paintOpGeneratedListener) paintOpGeneratedListener.onOp(null);
        }

        @Override
        public void onMultiFingerDragEnd() {
        }

        @Override
        public void onScale(float factor, float scaleCenterX, float scaleCenterY) {
            KLog.p("~~> factor=%s", factor);
            tmpPaintView.getMyMatrix().postScale(factor, factor, scaleCenterX, scaleCenterY);
            if (null != paintOpGeneratedListener) paintOpGeneratedListener.onOp(null);
        }

    };


    private OpPaint opPaint;
    private Matrix shapeInvertMatrix = new Matrix();
    private float[] mapPoint= new float[2];
    private void createShapeOp(float startX, float startY){
        boolean suc = shapePaintView.getMyMatrix().invert(shapeInvertMatrix);
//        KLog.p("invert success?=%s, orgX=%s, orgY=%s", suc, x, y);
        mapPoint[0] = startX;
        mapPoint[1] = startY;
        shapeInvertMatrix.mapPoints(mapPoint);
        float x = mapPoint[0];
        float y = mapPoint[1];
//            KLog.p("startX=%s, startY=%s, shapeScaleX=%s, shapeScaleY=%s", startX, startY, shapeScaleX, shapeScaleY);
        switch (tool){
            case TOOL_PENCIL:
                OpDrawPath opDrawPath = new OpDrawPath(new ArrayList<>());
                opDrawPath.getPoints().add(new PointF(x, y));
                opDrawPath.getPath().moveTo(x, y);
                opPaint = opDrawPath;
                break;
            case TOOL_LINE:
                OpDrawLine opDrawLine = new OpDrawLine();
                opDrawLine.setStartX(x);
                opDrawLine.setStartY(y);
                opPaint = opDrawLine;
                break;
            case TOOL_RECT:
                OpDrawRect opDrawRect = new OpDrawRect();
                opDrawRect.setLeft(x);
                opDrawRect.setTop(y);
                opPaint = opDrawRect;
                break;
            case TOOL_OVAL:
                OpDrawOval opDrawOval = new OpDrawOval();
                opDrawOval.setLeft(x);
                opDrawOval.setTop(y);
                opPaint = opDrawOval;
                break;
            case TOOL_ERASER:
                OpErase opErase = new OpErase(eraserSize, eraserSize, new ArrayList<>());
                opErase.getPoints().add(new PointF(x, y));
                opErase.getPath().moveTo(x, y);
                opPaint = opErase;
                break;
            case TOOL_RECT_ERASER:
                // 矩形擦除先绘制一个虚线矩形框选择擦除区域
                OpDrawRect opDrawRect1 = new OpDrawRect();
                opDrawRect1.setLeft(x);
                opDrawRect1.setTop(y);
                opPaint = opDrawRect1;
                break;
            default:
                KLog.p(KLog.ERROR, "unknown TOOL %s", tool);
                return;
        }
        if (opPaint instanceof OpDraw){
            OpDraw opDraw = (OpDraw) opPaint;
            if (TOOL_ERASER == tool){
                opDraw.setStrokeWidth(eraserSize);
            }else if(TOOL_RECT_ERASER == tool){
                opDraw.setLineStyle(OpDraw.DASH);
                opDraw.setStrokeWidth(2);
                opDraw.setColor(0xFF08b1f2L);
            } else {
                opDraw.setStrokeWidth(paintStrokeWidth);
                opDraw.setColor(paintColor);
            }
        }
        assignBasicInfo(opPaint);
    }

    private void adjustShapeOp(float adjustX, float adjustY){
        mapPoint[0] = adjustX;
        mapPoint[1] = adjustY;
        shapeInvertMatrix.mapPoints(mapPoint);
        float x = mapPoint[0];
        float y = mapPoint[1];
        switch (tool){
            case TOOL_PENCIL:
                OpDrawPath opDrawPath = (OpDrawPath) opPaint;
                List<PointF> pointFS = opDrawPath.getPoints();
                float preX, preY, midX, midY;
                preX = pointFS.get(pointFS.size()-1).x;
                preY = pointFS.get(pointFS.size()-1).y;
                midX = (preX + x) / 2;
                midY = (preY + y) / 2;
//                    KLog.p("=pathPreX=%s, pathPreY=%s, midX=%s, midY=%s", preX, preY, midX, midY);
                opDrawPath.getPath().quadTo(preX, preY, midX, midY);
                pointFS.add(new PointF(x, y));

                break;
            case TOOL_LINE:
                OpDrawLine opDrawLine = (OpDrawLine) opPaint;
                opDrawLine.setStopX(x);
                opDrawLine.setStopY(y);
                break;
            case TOOL_RECT:
                OpDrawRect opDrawRect = (OpDrawRect) opPaint;
                opDrawRect.setRight(x);
                opDrawRect.setBottom(y);
                break;
            case TOOL_OVAL:
                OpDrawOval opDrawOval = (OpDrawOval) opPaint;
                opDrawOval.setRight(x);
                opDrawOval.setBottom(y);
                break;
            case TOOL_ERASER:
                OpErase opErase = (OpErase) opPaint;
                pointFS = opErase.getPoints();
                preX = pointFS.get(pointFS.size()-1).x;
                preY = pointFS.get(pointFS.size()-1).y;
                midX = (preX + x) / 2;
                midY = (preY + y) / 2;
//                    KLog.p("=pathPreX=%s, pathPreY=%s, midX=%s, midY=%s", preX, preY, midX, midY);
                opErase.getPath().quadTo(preX, preY, midX, midY);
                pointFS.add(new PointF(x, y));

                break;
            case TOOL_RECT_ERASER:
                OpDrawRect opDrawRect1 = (OpDrawRect) opPaint;
                opDrawRect1.setRight(x);
                opDrawRect1.setBottom(y);
                break;
            default:
                return;
        }


    }


    private void confirmShapeOp(){
        if (TOOL_RECT_ERASER == tool){
            OpDrawRect opDrawRect = (OpDrawRect) opPaint;
            opPaint = new OpRectErase(opDrawRect.getLeft(), opDrawRect.getTop(), opDrawRect.getRight(), opDrawRect.getBottom());
            assignBasicInfo(opPaint);
        }else if (TOOL_PENCIL == tool){
            OpDrawPath opDrawPath = (OpDrawPath) opPaint;
            List<PointF> points = opDrawPath.getPoints();
            PointF lastPoint = points.get(points.size()-1);
            opDrawPath.getPath().lineTo(lastPoint.x, lastPoint.y);
        }else if (TOOL_ERASER == tool){
            OpErase opErase = (OpErase) opPaint;
            List<PointF> points = opErase.getPoints();
            PointF lastPoint = points.get(points.size()-1);
            opErase.getPath().lineTo(lastPoint.x, lastPoint.y);
        }
    }


    private void assignBasicInfo(OpPaint op){
        op.setConfE164(boardInfo.getConfE164());
        op.setBoardId(boardInfo.getId());
        op.setPageId(boardInfo.getPageId());
    }

    DefaultPaintView getPicPaintView(){ // TODO 封住，外部不需感知 paintview中操作挪到board内？？
        return picPaintView;
    }

    DefaultPaintView getShapePaintView(){
        return shapePaintView;
    }

    DefaultPaintView getTmpPaintView(){
        return tmpPaintView;
    }

    void clean(){
        publisher = null;
        paintOpGeneratedListener = null;
        onZoomRateChangedListener = null;
        onRepealableStateChangedListener = null;
        onPictureCountChangedListener = null;
        handler.removeMessages(MSGID_INSERT_PIC); // XXX 没有removeAll的接口？
    }

    @Override
    public String getBoardId() {
        return null!=boardInfo ? boardInfo.getId() : null;
    }

    @Override
    public BoardInfo getBoardInfo(){
        return boardInfo;
    }

    @Override
    public View getBoardView() {
        return this;
    }


    @Override
    public void setTool(int style) {
        this.tool = style;
    }

    @Override
    public int getTool() {
        return tool;
    }

    @Override
    public void setPaintStrokeWidth(int width) {
        this.paintStrokeWidth = width;
    }

    @Override
    public int getPaintStrokeWidth() {
        return paintStrokeWidth;
    }

    @Override
    public void setPaintColor(long color) {
        this.paintColor = color;
    }

    @Override
    public long getPaintColor() {
        return paintColor;
    }

    @Override
    public void setEraserSize(int size) {
        eraserSize = size;
    }

    @Override
    public int getEraserSize() {
        return eraserSize;
    }

    @Override
    public void focusLayer(int layer) {
        focusedLayer = layer;
    }


    @Override
    public void insertPic(String path) {
        if (null == publisher){
            KLog.p(KLog.ERROR,"publisher is null");
            return;
        }

        handler.removeMessages(MSGID_INSERT_PIC);
        if (null != picInsertBundleStuff){
            doInsertPic();
            picInsertBundleStuff = null;
        }

        // 绘制图片
        Bitmap bt = BitmapFactory.decodeFile(path);
        int picW = bt.getWidth();
        int picH = bt.getHeight();
        float transX = (getWidth()-picW)/2f;
        float transY = (getHeight()-picH)/2f;
        Matrix matrix = new Matrix();
        matrix.setTranslate(transX, transY);
        OpInsertPic op = new OpInsertPic(path, matrix);
        op.setPic(bt);
        assignBasicInfo(op);
        tmpPaintView.getTmpOps().offerLast(op);

        // 在图片外围绘制一个虚线矩形框
        OpDrawRect opDrawRect = new OpDrawRect();
        opDrawRect.setLeft(transX - 5);
        opDrawRect.setTop(transY - 5);
        opDrawRect.setRight(transX + picW + 5);
        opDrawRect.setBottom(transY + picH + 5);
        opDrawRect.setLineStyle(OpDraw.DASH);
        opDrawRect.setStrokeWidth(2);
        opDrawRect.setColor(0xFF08b1f2L);
        tmpPaintView.getTmpOps().offerLast(opDrawRect);

        // 在虚线矩形框正下方绘制删除图标
        transX = (getWidth()-del_pic_icon.getWidth())/2f;
        transY = opDrawRect.getBottom()+8;
        Matrix matrix1 = new Matrix();
        matrix1.setTranslate(transX, transY);
        OpInsertPic insertDelPicIcon = new OpInsertPic();
        insertDelPicIcon.setPic(del_pic_icon);
        insertDelPicIcon.setMatrix(matrix1);
        assignBasicInfo(insertDelPicIcon);
        tmpPaintView.getTmpOps().offerLast(insertDelPicIcon);

        if (null != paintOpGeneratedListener) paintOpGeneratedListener.onOp(null);

        int savedLayer = focusedLayer;
        focusedLayer = LAYER_TMP;
        picInsertBundleStuff = new PicInsertBundleStuff(op, opDrawRect, insertDelPicIcon, savedLayer);
        // 3秒过后画到图片画板上并清除临时画板
        handler.sendEmptyMessageDelayed(MSGID_INSERT_PIC, 3000); // TODO 如果3秒过程中用户按了返回键； TODO 用户有操作需更新时间戳

    }

    private class PicInsertBundleStuff{
        OpInsertPic opInsertPic;
        OpDrawRect opDrawRect;
        OpInsertPic opInsertDelIcon;
        int savedLayer;

        PicInsertBundleStuff(OpInsertPic opInsertPic, OpDrawRect opDrawRect, OpInsertPic opInsertDelIcon, int savedLayer) {
            this.opInsertPic = opInsertPic;
            this.opDrawRect = opDrawRect;
            this.opInsertDelIcon = opInsertDelIcon;
            this.savedLayer = savedLayer;
        }
    }

    private static int MSGID_INSERT_PIC = 666;
    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            if (MSGID_INSERT_PIC == msg.what){
                if (null != picInsertBundleStuff){
                    doInsertPic();
                    picInsertBundleStuff = null;
                }
            }
        }
    };


    private PicInsertBundleStuff picInsertBundleStuff;
    private void doInsertPic(){
        tmpPaintView.getTmpOps().remove(picInsertBundleStuff.opInsertPic);
        tmpPaintView.getTmpOps().remove(picInsertBundleStuff.opDrawRect);
        tmpPaintView.getTmpOps().remove(picInsertBundleStuff.opInsertDelIcon);
        picInsertBundleStuff.opInsertPic.getMatrix().postConcat(tmpPaintView.getMyMatrix());
        KLog.p("new tmp op %s", picInsertBundleStuff.opInsertPic);
        picPaintView.getTmpOps().offerLast(picInsertBundleStuff.opInsertPic);
        if (null != paintOpGeneratedListener) {
            paintOpGeneratedListener.onOp(null);
        }
        focusedLayer = picInsertBundleStuff.savedLayer;
        publisher.publish(picInsertBundleStuff.opInsertPic);
    }


    private void delPic(String picId){
        // TODO 清掉tmpView.tempOps
        handler.removeMessages(MSGID_INSERT_PIC);
    }

    @Override
    public Bitmap snapshot(int layer) {
        KLog.p("layer=%s", layer);
        Bitmap shot = null;
        if (LAYER_ALL == layer) {
            Bitmap picBt = picPaintView.getBitmap();
            Bitmap shapeBt = shapePaintView.getBitmap();
            int picW = picBt.getWidth();
            int picH = picBt.getHeight();
            int shapeW = shapeBt.getWidth();
            int shapeH = shapeBt.getHeight();
            int maxW = picW>shapeW?picW:shapeW;
            int maxH = picH>shapeH?picH:shapeH;
            KLog.p("picW=%s, picH=%s, shapeW=%s, shapeH=%s", picW, picH, shapeW, shapeH);
            shot = Bitmap.createBitmap(maxW, maxH, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(shot);
            draw(canvas);
            canvas.drawBitmap(picBt, 0, 0, null);
            canvas.drawBitmap(shapeBt, 0, 0, null);
        }else if (LAYER_PIC_AND_SHAPE == layer){
            Bitmap picBt = picPaintView.getBitmap();
            Bitmap shapeBt = shapePaintView.getBitmap();
            int picW = picBt.getWidth();
            int picH = picBt.getHeight();
            int shapeW = shapeBt.getWidth();
            int shapeH = shapeBt.getHeight();
            int maxW = picW>shapeW?picW:shapeW;
            int maxH = picH>shapeH?picH:shapeH;
            KLog.p("picW=%s, picH=%s, shapeW=%s, shapeH=%s", picW, picH, shapeW, shapeH);
            shot = Bitmap.createBitmap(maxW, maxH, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(shot);
            canvas.drawBitmap(picBt, 0, 0, null);
            canvas.drawBitmap(shapeBt, 0, 0, null);
        } else if (LAYER_SHAPE == layer){
            shot = shapePaintView.getBitmap();
        }else if (LAYER_PIC == layer){
            shot = picPaintView.getBitmap();
        }

        return shot;
    }

    private void dealSimpleOp(OpPaint op){
        if (null == publisher){
            KLog.p(KLog.ERROR,"publisher is null");
            return;
        }
        assignBasicInfo(op);
        publisher.publish(op);
    }

    @Override
    public void undo() {
        dealSimpleOp(new OpUndo());
    }

    @Override
    public void redo() {
        dealSimpleOp(new OpRedo());
    }

    @Override
    public void clearScreen() {
        dealSimpleOp(new OpClearScreen());
    }

    @Override
    public void zoom(int percentage) {
        int zoom = (MIN_ZOOM<=percentage && percentage<=MAX_ZOOM) ? percentage : (percentage<MIN_ZOOM ? MIN_ZOOM : MAX_ZOOM);
        KLog.p("zoom=%s, width=%s, height=%s", zoom, getWidth(), getHeight());
        OpMatrix opMatrix = new OpMatrix();
        opMatrix.getMatrix().setScale(zoom/100f, zoom/100f, getWidth()/2, getHeight()/2);
        dealSimpleOp(opMatrix);
    }

    @Override
    public int getZoom() {
        float[] vals = new float[9];
        shapePaintView.getMyMatrix().getValues(vals); // TODO 考虑图片层缩放率不一致的情形？
        return (int) (vals[Matrix.MSCALE_X]*100);
    }


    @Override
    public IPaintBoard setPublisher(IPublisher publisher) {
        this.publisher = publisher;
        if (publisher instanceof LifecycleOwner){
            ((LifecycleOwner)publisher).getLifecycle().addObserver(new DefaultLifecycleObserver(){
                @Override
                public void onDestroy(@NonNull LifecycleOwner owner) {
                    DefaultPaintBoard.this.publisher = null;
                    KLog.p("publisher destroyed");
                }
            });
        }

        picPaintView.setOnEventListener(null!=publisher ? picViewEventListener : null);
        shapePaintView.setOnEventListener(null!=publisher ? shapeViewEventListener : null);
        tmpPaintView.setOnEventListener(null!=publisher ? tmpViewEventListener : null);

        return this;
    }

    @Override
    public IPaintBoard setOnRepealableStateChangedListener(IOnRepealableStateChangedListener onRepealedOpsCountChangedListener) {
        this.onRepealableStateChangedListener = onRepealedOpsCountChangedListener;
        if (onRepealedOpsCountChangedListener instanceof LifecycleOwner){
            ((LifecycleOwner)onRepealedOpsCountChangedListener).getLifecycle().addObserver(new DefaultLifecycleObserver(){
                @Override
                public void onDestroy(@NonNull LifecycleOwner owner) {
                    DefaultPaintBoard.this.onRepealableStateChangedListener = null;
                    KLog.p("onRepealableStateChangedListener destroyed");
                }
            });
        }
        return this;
    }

    @Override
    public int getRepealedOpsCount() {
        return shapePaintView.getRepealedOps().size();
    }

    @Override
    public int getShapeOpsCount() {
        return shapePaintView.getRenderOps().size();
    }

    @Override
    public int getPicCount() {
        MyConcurrentLinkedDeque<OpPaint> ops = picPaintView.getRenderOps();
        int count = 0;
        for (OpPaint op : ops){
            if (EOpType.INSERT_PICTURE == op.getType()){
                ++count;
            }
        }
        return count;
    }

    @Override
    public IPaintBoard setOnPictureCountChangedListener(IOnPictureCountChanged onPictureCountChangedListener) {
        this.onPictureCountChangedListener = onPictureCountChangedListener;
        if (onPictureCountChangedListener instanceof LifecycleOwner){
            ((LifecycleOwner)onPictureCountChangedListener).getLifecycle().addObserver(new DefaultLifecycleObserver(){
                @Override
                public void onDestroy(@NonNull LifecycleOwner owner) {
                    DefaultPaintBoard.this.onPictureCountChangedListener = null;
                    KLog.p("onPictureCountChangedListener destroyed");
                }
            });
        }
        return this;
    }

    @Override
    public IPaintBoard setOnZoomRateChangedListener(IOnZoomRateChangedListener onZoomRateChangedListener) {
        this.onZoomRateChangedListener = onZoomRateChangedListener;
        if (onZoomRateChangedListener instanceof LifecycleOwner){
            ((LifecycleOwner)onZoomRateChangedListener).getLifecycle().addObserver(new DefaultLifecycleObserver(){
                @Override
                public void onDestroy(@NonNull LifecycleOwner owner) {
                    DefaultPaintBoard.this.onZoomRateChangedListener = null;
                    KLog.p("onZoomRateChangedListener destroyed");
                }
            });
        }
        return this;
    }


    void repealableStateChanged(){
        if (null != onRepealableStateChangedListener){
            onRepealableStateChangedListener.onRepealableStateChanged(getRepealedOpsCount(), getShapeOpsCount());
        }
    }

    void picCountChanged(){
        if (null != onPictureCountChangedListener){
            onPictureCountChangedListener.onPictureCountChanged(getPicCount());
        }
    }

    void zoomRateChanged(){
        if (null != onZoomRateChangedListener){
            onZoomRateChangedListener.onZoomRateChanged(getZoom());
        }
    }

    void setOnPaintOpGeneratedListener(IOnPaintOpGeneratedListener paintOpGeneratedListener) {
        this.paintOpGeneratedListener = paintOpGeneratedListener;
    }
    interface IOnPaintOpGeneratedListener{
        void onOp(OpPaint opPaint);
    }

}
