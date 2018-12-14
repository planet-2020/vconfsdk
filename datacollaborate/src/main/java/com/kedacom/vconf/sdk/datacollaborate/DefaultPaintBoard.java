package com.kedacom.vconf.sdk.datacollaborate;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.FrameLayout;

import com.kedacom.vconf.sdk.base.KLog;
import com.kedacom.vconf.sdk.datacollaborate.bean.BoardInfo;
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

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

public class DefaultPaintBoard extends FrameLayout implements IPaintBoard{
    private Context context;
    private DefaultPaintView picPaintView;
    private DefaultPaintView shapePaintView;

    // 图层
    private int focusedLayer = LAYER_SHAPE;

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

    private IOnPaintOpGeneratedListener paintOpGeneratedListener;
    private IPublisher publisher;

    public DefaultPaintBoard(@NonNull Context context, BoardInfo boardInfo) {
        super(context);
        this.context = context;
        this.boardInfo = boardInfo;
        LayoutInflater layoutInflater = LayoutInflater.from(context);
        View whiteBoard = layoutInflater.inflate(R.layout.default_whiteboard_layout, this);
        picPaintView = whiteBoard.findViewById(R.id.wb_pic_paint_view);
        picPaintView.setOpaque(false);
        shapePaintView = whiteBoard.findViewById(R.id.wb_shape_paint_view);
        shapePaintView.setOpaque(false);
        MyTouchListener myTouchListener = new MyTouchListener();
        picPaintView.setOnTouchListener(myTouchListener);  // XXX 默认情形下onClick事件将被屏蔽
        shapePaintView.setOnTouchListener(myTouchListener);
        picPaintView.getMatrixOp().setConfE164(boardInfo.getConfE164());
        picPaintView.getMatrixOp().setBoardId(boardInfo.getId());
        picPaintView.getMatrixOp().setPageId(boardInfo.getPageId());
        shapePaintView.getMatrixOp().setConfE164(boardInfo.getConfE164());
        shapePaintView.getMatrixOp().setBoardId(boardInfo.getId());
        shapePaintView.getMatrixOp().setPageId(boardInfo.getPageId());

        setBackgroundColor(Color.DKGRAY);
    }

    public DefaultPaintBoard(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (LAYER_NONE == focusedLayer){
            return true;
        }else if (LAYER_SHAPE == focusedLayer){
            return shapePaintView.dispatchTouchEvent(ev);
        }else if (LAYER_PIC == focusedLayer){
            return picPaintView.dispatchTouchEvent(ev);
        }else if (LAYER_ALL == focusedLayer){
            boolean ret1 = shapePaintView.dispatchTouchEvent(ev);
            boolean ret2 = picPaintView.dispatchTouchEvent(ev);
            return ret1||ret2;
        }

        return false;
    }


    private class MyTouchListener implements OnTouchListener{
        private static final int STATE_IDLE = 0;
        private static final int STATE_SHAKING = 1;
        private static final int STATE_DRAWING = 2;
        private static final int STATE_SCALING = 3;
        private static final int STATE_DRAGING = 4;
        private static final int STATE_SCALING_AND_DRAGING = 5;
        private int state = STATE_IDLE;

        private PointF startPoint = new PointF();	// 起始绘制点
        private PointF startDragPoint = new PointF();	// 起始拖拽点
        private PointF zoomCenter = new PointF();

        private float scaleFactor = 1.0f;
        private float lastScaleFactor = scaleFactor;
        private ScaleGestureDetector scaleGestureDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener(){
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
//                KLog.p("focusX= " + detector.getFocusX());
//                KLog.p("focusY= " + detector.getFocusY());
//                KLog.p("scale=%s, lastScale=%s, |scale-lastScale|=%s", scaleFactor, lastScaleFactor, Math.abs(scaleFactor-lastScaleFactor));
                scaleFactor = detector.getScaleFactor();
                if (Math.abs(scaleFactor-lastScaleFactor) < 0.00001){
                    return true;
                }
                lastScaleFactor = scaleFactor;

//                KLog.p("zoomCenter={%s, %s} ",  zoomCenter.x, zoomCenter.y);
                OpMatrix opMatrix = shapePaintView.getMatrixOp();
                opMatrix.getMatrix().postScale(scaleFactor, scaleFactor, zoomCenter.x, zoomCenter.y);
                opMatrix.getMatrix().postTranslate(detector.getFocusX()-startDragPoint.x, detector.getFocusY()-startDragPoint.y);
                opMatrix = picPaintView.getMatrixOp();
                opMatrix.getMatrix().postScale(scaleFactor, scaleFactor, zoomCenter.x, zoomCenter.y);
                opMatrix.getMatrix().postTranslate(detector.getFocusX()-startDragPoint.x, detector.getFocusY()-startDragPoint.y);
                startDragPoint.set(detector.getFocusX(), detector.getFocusY());
                refreshPaintOp();
                return  true;
            }

            @Override
            public boolean onScaleBegin(ScaleGestureDetector detector) {
                KLog.p("state=%s, focusX = %s, focusY =%s", state, detector.getFocusX(), detector.getFocusY());
                state = STATE_SCALING_AND_DRAGING;
                startDragPoint.set(detector.getFocusX(), detector.getFocusY());
                zoomCenter.set(getWidth()/2, getHeight()/2);
                KLog.p("zoomCenter={%s, %s}", zoomCenter.x, zoomCenter.y);
                return true;
            }

            @Override
            public void onScaleEnd(ScaleGestureDetector detector) {
                KLog.p("state=%s, focusX = %s, focusY =%s, scale = %s", state, detector.getFocusX(), detector.getFocusY(), scaleFactor);
                if (null != publisher){
                    publisher.publish(shapePaintView.getMatrixOp()); // TODO 判断当前缩放图层
                }
                state = STATE_IDLE;
            }
        });

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (null == paintOpGeneratedListener){
                return false;
            }
            scaleGestureDetector.onTouchEvent(event);
            if (STATE_SCALING == state
                    || STATE_SCALING_AND_DRAGING == state){
                return true;
            }

            switch (event.getActionMasked()) {

                case MotionEvent.ACTION_DOWN:
                    KLog.p("state=%s, ACTION_DOWN{%s}", state, event);
                    state = STATE_SHAKING;
                    startPoint.set(event.getX(), event.getY()); // 记录起始点
                    createPaintOp(event.getX(), event.getY());
                    break;

                case MotionEvent.ACTION_POINTER_DOWN:
                    KLog.p("state=%s, ACTION_POINTER_DOWN{%s}", state, event);
                    if (2 == event.getPointerCount()) {
                        if (STATE_DRAWING == state) {
                            confirmPaintOp();
                        }else if (STATE_SHAKING == state){
                            cancelPaintOp();
                        }
                        startDragPoint.set((event.getX(1) + event.getX(0))/2,
                                (event.getY(1) + event.getY(0))/2);
                        state = STATE_DRAGING;
                    }
                    break;

                case MotionEvent.ACTION_POINTER_UP:
                    KLog.p("state=%s, ACTION_POINTER_UP{%s}", state, event);
                    if (2 == event.getPointerCount()){ // 二个手指其中一个抬起，只剩一个手指了
                        state = STATE_SHAKING;
                        int indx = 1==event.getActionIndex() ? 0 : 1;
                        startPoint.set(event.getX(indx), event.getY(indx)); // 记录起始点
                        createPaintOp(event.getX(indx), event.getY(indx));
                    }
                    break;

                case MotionEvent.ACTION_MOVE:
//                    KLog.p("state=%s, ACTION_MOVE{%s}", state, event);
                    if (STATE_SHAKING == state) {
                        int dx = (int) (event.getX() - startPoint.x);
                        int dy = (int) (event.getY() - startPoint.y);
                        if (Math.sqrt(dx * dx + dy * dy) > 15){
                            state = STATE_DRAWING;
                            adjustPaintOp(event);
                        }
                    }else if (STATE_DRAWING == state){
                        adjustPaintOp(event);
                    }else if (STATE_DRAGING == state){
                        OpMatrix opMatrix = shapePaintView.getMatrixOp();
                        opMatrix.getMatrix().postTranslate((event.getX(1) + event.getX(0))/2 -startDragPoint.x,
                                (event.getY(1) + event.getY(0))/2-startDragPoint.y);
                        opMatrix = picPaintView.getMatrixOp();
                        opMatrix.getMatrix().postTranslate((event.getX(1) + event.getX(0))/2 -startDragPoint.x,
                                (event.getY(1) + event.getY(0))/2-startDragPoint.y);
                        startDragPoint.set((event.getX(1) + event.getX(0))/2,
                                (event.getY(1) + event.getY(0))/2);
                        refreshPaintOp();
                    }
                    break;

                case MotionEvent.ACTION_UP:
                    KLog.p("state=%s, ACTION_UP{%s}", state, event);
                    if (STATE_DRAWING == state) {
                        confirmPaintOp();
                    }else if (STATE_SHAKING == state){
                        cancelPaintOp();
                    }
                    state = STATE_IDLE;
                    break;

                case MotionEvent.ACTION_CANCEL:
                    KLog.p("ACTION_CANCEL{%s}", event);
                    break;
                default:
                    KLog.p("Discarded ACTION{%s}", event.getActionMasked());
                    break;
            }

            return true;
        }


        private OpPaint opPaint;
        private Matrix shapeInvertMatrix = new Matrix();
        private float[] mapPoint= new float[2];
        private void createPaintOp(float startX, float startY){
            boolean suc = shapePaintView.getMatrixOp().getMatrix().invert(shapeInvertMatrix);
            KLog.p("invert success?=%s, orgX=%s, orgY=%s", suc, startX, startY);
            mapPoint[0] = startX;
            mapPoint[1] = startY;
            shapeInvertMatrix.mapPoints(mapPoint);
            startX = mapPoint[0];
            startY = mapPoint[1];
//            KLog.p("startX=%s, startY=%s, shapeScaleX=%s, shapeScaleY=%s", startX, startY, shapeScaleX, shapeScaleY);
            switch (tool){
                case TOOL_PENCIL:
                    OpDrawPath opDrawPath = new OpDrawPath(new ArrayList<>());
                    opDrawPath.getPoints().add(new PointF(startX, startY));
                    opDrawPath.getPath().moveTo(startX, startY);
                    opPaint = opDrawPath;
                    break;
                case TOOL_LINE:
                    OpDrawLine opDrawLine = new OpDrawLine();
                    opDrawLine.setStartX(startX);
                    opDrawLine.setStartY(startY);
                    opPaint = opDrawLine;
                    break;
                case TOOL_RECT:
                    OpDrawRect opDrawRect = new OpDrawRect();
                    opDrawRect.setLeft(startX);
                    opDrawRect.setTop(startY);
                    opPaint = opDrawRect;
                    break;
                case TOOL_OVAL:
                    OpDrawOval opDrawOval = new OpDrawOval();
                    opDrawOval.setLeft(startX);
                    opDrawOval.setTop(startY);
                    opPaint = opDrawOval;
                    break;
                case TOOL_ERASER:
                    OpErase opErase = new OpErase(new ArrayList<>());
                    opErase.getPoints().add(new PointF(startX, startY));
                    opErase.getPath().moveTo(startX, startY);
                    opPaint = opErase;
                    break;
                case TOOL_RECT_ERASER:
                    // 矩形擦除先绘制一个虚线矩形框选择擦除区域
                    OpDrawRect opDrawRect1 = new OpDrawRect();
                    opDrawRect1.setLeft(startX);
                    opDrawRect1.setTop(startY);
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

        private void adjustPaintOp(MotionEvent event){
            mapPoint[0] = event.getX();
            mapPoint[1] = event.getY();
            shapeInvertMatrix.mapPoints(mapPoint);
            float x = mapPoint[0];
            float y = mapPoint[1];
            switch (tool){
                case TOOL_PENCIL:
                    OpDrawPath opDrawPath = (OpDrawPath) opPaint;
//                    KLog.p("event.getX()=%s, event.getY()=%s, history size=%s", event.getX(), event.getY(), event.getHistorySize());
                    float preX, preY, midX, midY;
                    List<PointF> pointFS = opDrawPath.getPoints();
                    for (int i = 0; i < event.getHistorySize(); ++i) {
                        preX = pointFS.get(pointFS.size()-1).x;
                        preY = pointFS.get(pointFS.size()-1).y;
                        mapPoint[0] = event.getHistoricalX(i);
                        mapPoint[1] = event.getHistoricalY(i);
//                        KLog.p("historicalX=%s, historicalY=%s", mapPoint[0], mapPoint[1]);
                        shapeInvertMatrix.mapPoints(mapPoint);
                        pointFS.add(new PointF(mapPoint[0], mapPoint[1]));
                        midX = (preX + mapPoint[0]) / 2;
                        midY = (preY + mapPoint[1]) / 2;
//                        KLog.p("pathPreX=%s, pathPreY=%s, midX=%s, midY=%s", preX, preY, midX, midY);
                        opDrawPath.getPath().quadTo(preX, preY, midX, midY);
                    }
                    preX = pointFS.get(pointFS.size()-1).x;
                    preY = pointFS.get(pointFS.size()-1).y;
                    pointFS.add(new PointF(x, y));
                    midX = (preX + x) / 2;
                    midY = (preY + y) / 2;
//                    KLog.p("=pathPreX=%s, pathPreY=%s, midX=%s, midY=%s", preX, preY, midX, midY);
                    opDrawPath.getPath().quadTo(preX, preY, midX, midY);

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
                    opErase.getPoints().add(new PointF(x, y)); // TODO historySize
                    opErase.getPath().lineTo(x, y);
                    break;
                case TOOL_RECT_ERASER:
                    OpDrawRect opDrawRect1 = (OpDrawRect) opPaint;
                    opDrawRect1.setRight(x);
                    opDrawRect1.setBottom(y);
                    break;
                default:
                    return;
            }

            paintOpGeneratedListener.onAdjust(opPaint);

        }

        private void refreshPaintOp(){
            if (null == paintOpGeneratedListener){
                return;
            }
            paintOpGeneratedListener.onAdjust(null);
        }

        private void cancelPaintOp(){
            opPaint = null;
        }

        private void confirmPaintOp(){
            if (TOOL_RECT_ERASER == tool){
                OpDrawRect opDrawRect = (OpDrawRect) opPaint;
                opPaint = new OpRectErase(opDrawRect.getLeft(), opDrawRect.getTop(), opDrawRect.getRight(), opDrawRect.getBottom());
                assignBasicInfo(opPaint);
            }
            paintOpGeneratedListener.onConfirm(opPaint);
            if (null != publisher){
                publisher.publish(opPaint);
            }
            opPaint = null;
        }

    }

    private void assignBasicInfo(OpPaint op){
        op.setConfE164(boardInfo.getConfE164());
        op.setBoardId(boardInfo.getId());
        op.setPageId(boardInfo.getPageId());
    }

    public DefaultPaintView getPicPaintView(){
        return picPaintView;
    }

    public DefaultPaintView getShapePaintView(){
        return shapePaintView;
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
    public void insertPic(Bitmap pic) {
        if (null == pic){
            KLog.p(KLog.ERROR, "null pic");
            return;
        }
        if (null != paintOpGeneratedListener){
            OpInsertPic op = new OpInsertPic();
            op.setPic(pic);
            float[] values = new float[9];
            new Matrix().getValues(values);
            op.setMatrixValue(values);
            assignBasicInfo(op); // TODO 更多赋值
            paintOpGeneratedListener.onConfirm(op);
            if (null != publisher){
                publisher.publish(op);
            }
        }
    }

    @Override
    public Bitmap snapshot(int layer) {
        KLog.p("layer=%s", layer);
        Bitmap shot = null;
        if (LAYER_ALL == layer) {
            shot = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(shot);
            draw(canvas);
            canvas.drawBitmap(picPaintView.getBitmap(), 0, 0, null);
            canvas.drawBitmap(shapePaintView.getBitmap(), 0, 0, null);
        }else if (LAYER_PIC_AND_SHAPE == layer){
            shot = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(shot);
            canvas.drawBitmap(picPaintView.getBitmap(), 0, 0, null);
            canvas.drawBitmap(shapePaintView.getBitmap(), 0, 0, null);
        } else if (LAYER_SHAPE == layer){
            shot = shapePaintView.getBitmap();
        }else if (LAYER_PIC == layer){
            shot = picPaintView.getBitmap();
        }

        return shot;
    }

    private void dealSimpleOp(OpPaint op){
        assignBasicInfo(op);
        paintOpGeneratedListener.onConfirm(op);
        if (null != publisher){
            publisher.publish(op);
        }
    }

    @Override
    public void undo() {
        if (null != paintOpGeneratedListener){
            dealSimpleOp(new OpUndo());
        }
    }

    @Override
    public void redo() {
        if (null != paintOpGeneratedListener){
            dealSimpleOp(new OpRedo());
        }
    }

    @Override
    public void clearScreen() {
        if (null != paintOpGeneratedListener){
            dealSimpleOp(new OpClearScreen());
        }
    }

    @Override
    public void zoom(int percentage) {
        if (null == paintOpGeneratedListener){
            return;
        }
        int zoom = (MIN_ZOOM<=percentage && percentage<=MAX_ZOOM) ? percentage : (percentage<MIN_ZOOM ? MIN_ZOOM : MAX_ZOOM);
        KLog.p("zoom=%s, width=%s, height=%s", zoom, getWidth(), getHeight());
        shapePaintView.getMatrixOp().getMatrix().setScale(zoom/100f, zoom/100f, getWidth()/2, getHeight()/2);
        paintOpGeneratedListener.onAdjust(null);
        if (null != publisher){
            publisher.publish(shapePaintView.getMatrixOp());
        }
    }

    @Override
    public int getZoom() {
        float[] vals = new float[9];
        shapePaintView.getMatrixOp().getMatrix().getValues(vals);
        return (int) (vals[Matrix.MSCALE_X]*100);
    }

    @Override
    public void setPublisher(IPublisher publisher) {
        this.publisher = publisher;
        if (publisher instanceof LifecycleOwner){
            ((LifecycleOwner)publisher).getLifecycle().addObserver(new DefaultLifecycleObserver(){
                @Override
                public void onDestroy(@NonNull LifecycleOwner owner) {
                    DefaultPaintBoard.this.publisher = null;
                }
            });
        }
    }

    void setOnPaintOpGeneratedListener(IOnPaintOpGeneratedListener paintOpGeneratedListener) {
        this.paintOpGeneratedListener = paintOpGeneratedListener;
    }
    interface IOnPaintOpGeneratedListener{
        void onAdjust(OpPaint opPaint);
        void onConfirm(OpPaint opPaint);
    }
    void setOnRepealedOpsCountChangedListener(IOnRepealedOpsCountChangedListener onRepealedOpsCountChangedListener) {
        this.paintOpGeneratedListener = paintOpGeneratedListener;
    }

}
