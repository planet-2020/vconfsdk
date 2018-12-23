package com.kedacom.vconf.sdk.datacollaborate;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.PointF;
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

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

public class DefaultPaintBoard extends FrameLayout implements IPaintBoard{
    private Context context;

    // 图形绘制view。用于图形绘制如画线、画圈、擦除等等
    private DefaultPaintView shapePaintView;

    // 图片绘制view
    private DefaultPaintView picPaintView;

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
        picPaintView = whiteBoard.findViewById(R.id.wb_pic_paint_view);
        picPaintView.setOpaque(false);
        shapePaintView = whiteBoard.findViewById(R.id.wb_shape_paint_view);
        shapePaintView.setOpaque(false);

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


    DefaultPaintView.IOnEventListener shapeViewEventListener = new DefaultPaintView.IOnEventListener(){

        @Override
        public void onDragBegin(float x, float y) {
            KLog.p("~~> x=%s, y=%s", x, y);
            createShapeOp(x, y);
        }

        @Override
        public void onDrag(float x, float y) {
            KLog.p("~~> x=%s, y=%s", x, y);
            adjustShapeOp(x, y);
            paintOpGeneratedListener.onOp(opPaint);
        }

        @Override
        public void onDragEnd() {
            KLog.p("~~>");
            confirmShapeOp();
            KLog.p("new tmp op %s", opPaint);
            shapePaintView.getTmpOps().offerLast(opPaint);
            paintOpGeneratedListener.onOp(null);
            publisher.publish(opPaint);
            opPaint = null;
        }


        @Override
        public void onMultiFingerDrag(float dx, float dy) {
            KLog.p("~~> dx=%s, dy=%s", dx, dy);
            shapePaintView.getMyMatrix().postTranslate(dx, dy);
            paintOpGeneratedListener.onOp(null);
        }

        @Override
        public void onMultiFingerDragEnd() {
            KLog.p("~~>");
            OpMatrix opMatrix = new OpMatrix(shapePaintView.getMyMatrix());
            assignBasicInfo(opMatrix);
            publisher.publish(opMatrix); // TODO 图形和图片缩放会发布两次，只需一次
        }


        @Override
        public void onScale(float factor, float scaleCenterX, float scaleCenterY) {
            KLog.p("~~> factor=%s", factor);
            shapePaintView.getMyMatrix().postScale(factor, factor, scaleCenterX, scaleCenterY);
            paintOpGeneratedListener.onOp(null);
            zoomRateChanged(); // TODO 图形和图片缩放会发布两次，只需一次
        }

        @Override
        public void onScaleEnd() {
            KLog.p("~~>");
            OpMatrix opMatrix = new OpMatrix(shapePaintView.getMyMatrix());
            assignBasicInfo(opMatrix);
            publisher.publish(opMatrix); // TODO 图形和图片缩放会发布两次，只需一次
        }

        @Override
        public void onLongTouch() {
            KLog.p("~~>");
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
        public void onMultiFingerDrag(float x, float y) {
            KLog.p("~~> x=%s, y=%s", x, y);
        }

        @Override
        public void onMultiFingerDragEnd() {
            KLog.p("~~>");
        }

        @Override
        public void onScale(float factor, float scaleCenterX, float scaleCenterY) {
            KLog.p("~~> factor=%s", factor);
        }

        @Override
        public void onScaleEnd() {
            KLog.p("~~>");
        }

        @Override
        public void onLongTouch() {
            KLog.p("~~>");
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



//    private class MyTouchListener implements OnTouchListener{
//        private static final int STATE_IDLE = 0;
//        private static final int STATE_SHAKING = 1;
//        private static final int STATE_DRAWING = 2;
//        private static final int STATE_SCALING = 3;
//        private static final int STATE_DRAGING = 4;
//        private static final int STATE_SCALING_AND_DRAGING = 5;
//        private int state = STATE_IDLE;
//
//        private PointF startPoint = new PointF();	// 起始绘制点
//        private PointF startDragPoint = new PointF();	// 起始拖拽点
//        private PointF zoomCenter = new PointF();
//
//        private float scaleFactor = 1.0f;
//        private float lastScaleFactor = scaleFactor;
//        private ScaleGestureDetector scaleGestureDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener(){
//            @Override
//            public boolean onScale(ScaleGestureDetector detector) {
////                KLog.p("focusX= " + detector.getFocusX());
////                KLog.p("focusY= " + detector.getFocusY());
////                KLog.p("scale=%s, lastScale=%s, |scale-lastScale|=%s", scaleFactor, lastScaleFactor, Math.abs(scaleFactor-lastScaleFactor));
//                scaleFactor = detector.getScaleFactor();
//                if (Math.abs(scaleFactor-lastScaleFactor) < 0.00001){
//                    return true;
//                }
//                lastScaleFactor = scaleFactor;
//
////                KLog.p("zoomCenter={%s, %s} ",  zoomCenter.x, zoomCenter.y);
//                Matrix myMatrix = shapePaintView.getMyMatrix();
//                myMatrix.postScale(scaleFactor, scaleFactor, zoomCenter.x, zoomCenter.y);
//                myMatrix.postTranslate(detector.getFocusX()-startDragPoint.x, detector.getFocusY()-startDragPoint.y);
//                myMatrix = picPaintView.getMyMatrix();
//                myMatrix.postScale(scaleFactor, scaleFactor, zoomCenter.x, zoomCenter.y);
//                myMatrix.postTranslate(detector.getFocusX()-startDragPoint.x, detector.getFocusY()-startDragPoint.y);
//
//                startDragPoint.set(detector.getFocusX(), detector.getFocusY());
//
//                // TODO 放到tempOps中？不必，简化处理，再缩放下对方能恢复。
//                refreshPaintOp();
//
//                zoomRateChanged();
//
//                return true;
//            }
//
//            @Override
//            public boolean onScaleBegin(ScaleGestureDetector detector) {
//                KLog.p("state=%s, focusX = %s, focusY =%s", state, detector.getFocusX(), detector.getFocusY());
//                state = STATE_SCALING_AND_DRAGING;
//                startDragPoint.set(detector.getFocusX(), detector.getFocusY());
//                zoomCenter.set(getWidth()/2, getHeight()/2);
//                KLog.p("zoomCenter={%s, %s}", zoomCenter.x, zoomCenter.y);
//                return true;
//            }
//
//            @Override
//            public void onScaleEnd(ScaleGestureDetector detector) {
//                confirmMatrixOp();
//                state = STATE_IDLE;
//            }
//        });
//
//        @Override
//        public boolean onTouch(View v, MotionEvent event) {
//            if (null == paintOpGeneratedListener||null==publisher){
//                return false;
//            }
//            scaleGestureDetector.onTouchEvent(event);
//            if (STATE_SCALING == state
//                    || STATE_SCALING_AND_DRAGING == state){
//                return true;
//            }
//
//            switch (event.getActionMasked()) {
//
//                case MotionEvent.ACTION_DOWN:
////                    KLog.p("state=%s, ACTION_DOWN{%s}", state, event);
//                    state = STATE_SHAKING;
//                    startPoint.set(event.getX(), event.getY()); // 记录起始点
//                    createPaintOp(event.getX(), event.getY());
//                    break;
//
//                case MotionEvent.ACTION_POINTER_DOWN:
////                    KLog.p("state=%s, ACTION_POINTER_DOWN{%s}", state, event);
//                    if (2 == event.getPointerCount()) {
//                        if (STATE_DRAWING == state) {
//                            confirmPaintOp();
//                        }else if (STATE_SHAKING == state){
//                            cancelPaintOp();
//                        }
//                        startDragPoint.set((event.getX(1) + event.getX(0))/2,
//                                (event.getY(1) + event.getY(0))/2);
//                        state = STATE_DRAGING;
//                    }
//                    break;
//
//                case MotionEvent.ACTION_POINTER_UP:
////                    KLog.p("state=%s, ACTION_POINTER_UP{%s}", state, event);
//                    if (2 == event.getPointerCount()){ // 二个手指其中一个抬起，只剩一个手指了
//                        if (STATE_DRAGING == state){
//                            confirmMatrixOp();
//                        }
//                        state = STATE_SHAKING;
//                        int indx = 1==event.getActionIndex() ? 0 : 1;
//                        startPoint.set(event.getX(indx), event.getY(indx)); // 记录起始点
//                        createPaintOp(event.getX(indx), event.getY(indx));
//                    }
//                    break;
//
//                case MotionEvent.ACTION_MOVE:
////                    KLog.p("state=%s, ACTION_MOVE{%s}", state, event);
//                    if (STATE_SHAKING == state) {
//                        int dx = (int) (event.getX() - startPoint.x);
//                        int dy = (int) (event.getY() - startPoint.y);
//                        if (Math.sqrt(dx * dx + dy * dy) > 15){
//                            state = STATE_DRAWING;
//                            adjustPaintOp(event);
//                        }
//                    }else if (STATE_DRAWING == state){
//                        adjustPaintOp(event);
//                    }else if (STATE_DRAGING == state){
//                        Matrix myMatrix = shapePaintView.getMyMatrix();
//                        myMatrix.postTranslate((event.getX(1) + event.getX(0))/2 -startDragPoint.x,
//                                (event.getY(1) + event.getY(0))/2-startDragPoint.y);
//                        myMatrix = picPaintView.getMyMatrix();
//                        myMatrix.postTranslate((event.getX(1) + event.getX(0))/2 -startDragPoint.x,
//                                (event.getY(1) + event.getY(0))/2-startDragPoint.y);
//                        startDragPoint.set((event.getX(1) + event.getX(0))/2,
//                                (event.getY(1) + event.getY(0))/2);
//                        refreshPaintOp();
//                    }
//                    break;
//
//                case MotionEvent.ACTION_UP:
////                    KLog.p("state=%s, ACTION_UP{%s}", state, event);
//                    if (STATE_DRAWING == state) {
//                        confirmPaintOp();
//                    }else if (STATE_SHAKING == state){
//                        cancelPaintOp();
//                    }
//                    state = STATE_IDLE;
//                    break;
//
//                case MotionEvent.ACTION_CANCEL:
//                    KLog.p("ACTION_CANCEL{%s}", event);
//                    break;
//                default:
//                    KLog.p("Discarded ACTION{%s}", event.getActionMasked());
//                    break;
//            }
//
//            return true;
//        }
//
//
//        private OpPaint opPaint;
//        private Matrix shapeInvertMatrix = new Matrix();
//        private float[] mapPoint= new float[2];
//        private void createPaintOp(float startX, float startY){
//            boolean suc = shapePaintView.getMyMatrix().invert(shapeInvertMatrix);
////            KLog.p("invert success?=%s, orgX=%s, orgY=%s", suc, startX, startY);
//            mapPoint[0] = startX;
//            mapPoint[1] = startY;
//            shapeInvertMatrix.mapPoints(mapPoint);
//            startX = mapPoint[0];
//            startY = mapPoint[1];
////            KLog.p("startX=%s, startY=%s, shapeScaleX=%s, shapeScaleY=%s", startX, startY, shapeScaleX, shapeScaleY);
//            switch (tool){
//                case TOOL_PENCIL:
//                    OpDrawPath opDrawPath = new OpDrawPath(new ArrayList<>());
//                    opDrawPath.getPoints().add(new PointF(startX, startY));
//                    opDrawPath.getPath().moveTo(startX, startY);
//                    opPaint = opDrawPath;
//                    break;
//                case TOOL_LINE:
//                    OpDrawLine opDrawLine = new OpDrawLine();
//                    opDrawLine.setStartX(startX);
//                    opDrawLine.setStartY(startY);
//                    opPaint = opDrawLine;
//                    break;
//                case TOOL_RECT:
//                    OpDrawRect opDrawRect = new OpDrawRect();
//                    opDrawRect.setLeft(startX);
//                    opDrawRect.setTop(startY);
//                    opPaint = opDrawRect;
//                    break;
//                case TOOL_OVAL:
//                    OpDrawOval opDrawOval = new OpDrawOval();
//                    opDrawOval.setLeft(startX);
//                    opDrawOval.setTop(startY);
//                    opPaint = opDrawOval;
//                    break;
//                case TOOL_ERASER:
//                    OpErase opErase = new OpErase(eraserSize, eraserSize, new ArrayList<>());
//                    opErase.getPoints().add(new PointF(startX, startY));
//                    opErase.getPath().moveTo(startX, startY);
//                    opPaint = opErase;
//                    break;
//                case TOOL_RECT_ERASER:
//                    // 矩形擦除先绘制一个虚线矩形框选择擦除区域
//                    OpDrawRect opDrawRect1 = new OpDrawRect();
//                    opDrawRect1.setLeft(startX);
//                    opDrawRect1.setTop(startY);
//                    opPaint = opDrawRect1;
//                    break;
//                default:
//                    KLog.p(KLog.ERROR, "unknown TOOL %s", tool);
//                    return;
//            }
//            if (opPaint instanceof OpDraw){
//                OpDraw opDraw = (OpDraw) opPaint;
//                if (TOOL_ERASER == tool){
//                    opDraw.setStrokeWidth(eraserSize);
//                }else if(TOOL_RECT_ERASER == tool){
//                    opDraw.setLineStyle(OpDraw.DASH);
//                    opDraw.setStrokeWidth(2);
//                    opDraw.setColor(0xFF08b1f2L);
//                } else {
//                    opDraw.setStrokeWidth(paintStrokeWidth);
//                    opDraw.setColor(paintColor);
//                }
//            }
//            assignBasicInfo(opPaint);
//        }
//
//        private void adjustPaintOp(MotionEvent event){
//            mapPoint[0] = event.getX();
//            mapPoint[1] = event.getY();
//            shapeInvertMatrix.mapPoints(mapPoint);
//            float x = mapPoint[0];
//            float y = mapPoint[1];
//            switch (tool){
//                case TOOL_PENCIL:
//                    OpDrawPath opDrawPath = (OpDrawPath) opPaint;
////                    KLog.p("event.getX()=%s, event.getY()=%s, history size=%s", event.getX(), event.getY(), event.getHistorySize());
//                    float preX, preY, midX, midY;
//                    List<PointF> pointFS = opDrawPath.getPoints();
//                    for (int i = 0; i < event.getHistorySize(); ++i) {
//                        preX = pointFS.get(pointFS.size()-1).x;
//                        preY = pointFS.get(pointFS.size()-1).y;
//                        mapPoint[0] = event.getHistoricalX(i);
//                        mapPoint[1] = event.getHistoricalY(i);
////                        KLog.p("historicalX=%s, historicalY=%s", mapPoint[0], mapPoint[1]);
//                        shapeInvertMatrix.mapPoints(mapPoint);
//                        pointFS.add(new PointF(mapPoint[0], mapPoint[1]));
//                        midX = (preX + mapPoint[0]) / 2;
//                        midY = (preY + mapPoint[1]) / 2;
////                        KLog.p("pathPreX=%s, pathPreY=%s, midX=%s, midY=%s", preX, preY, midX, midY);
//                        opDrawPath.getPath().quadTo(preX, preY, midX, midY);
//                    }
//                    preX = pointFS.get(pointFS.size()-1).x;
//                    preY = pointFS.get(pointFS.size()-1).y;
//                    pointFS.add(new PointF(x, y));
//                    midX = (preX + x) / 2;
//                    midY = (preY + y) / 2;
////                    KLog.p("=pathPreX=%s, pathPreY=%s, midX=%s, midY=%s", preX, preY, midX, midY);
//                    opDrawPath.getPath().quadTo(preX, preY, midX, midY);
//
//                    break;
//                case TOOL_LINE:
//                    OpDrawLine opDrawLine = (OpDrawLine) opPaint;
//                    opDrawLine.setStopX(x);
//                    opDrawLine.setStopY(y);
//                    break;
//                case TOOL_RECT:
//                    OpDrawRect opDrawRect = (OpDrawRect) opPaint;
//                    opDrawRect.setRight(x);
//                    opDrawRect.setBottom(y);
//                    break;
//                case TOOL_OVAL:
//                    OpDrawOval opDrawOval = (OpDrawOval) opPaint;
//                    opDrawOval.setRight(x);
//                    opDrawOval.setBottom(y);
//                    break;
//                case TOOL_ERASER:
//                    OpErase opErase = (OpErase) opPaint;
//                    pointFS = opErase.getPoints();
//                    for (int i = 0; i < event.getHistorySize(); ++i) {
//                        preX = pointFS.get(pointFS.size()-1).x;
//                        preY = pointFS.get(pointFS.size()-1).y;
//                        mapPoint[0] = event.getHistoricalX(i);
//                        mapPoint[1] = event.getHistoricalY(i);
////                        KLog.p("historicalX=%s, historicalY=%s", mapPoint[0], mapPoint[1]);
//                        shapeInvertMatrix.mapPoints(mapPoint);
//                        pointFS.add(new PointF(mapPoint[0], mapPoint[1]));
//                        midX = (preX + mapPoint[0]) / 2;
//                        midY = (preY + mapPoint[1]) / 2;
////                        KLog.p("pathPreX=%s, pathPreY=%s, midX=%s, midY=%s", preX, preY, midX, midY);
//                        opErase.getPath().quadTo(preX, preY, midX, midY);
//                    }
//                    preX = pointFS.get(pointFS.size()-1).x;
//                    preY = pointFS.get(pointFS.size()-1).y;
//                    pointFS.add(new PointF(x, y));
//                    midX = (preX + x) / 2;
//                    midY = (preY + y) / 2;
////                    KLog.p("=pathPreX=%s, pathPreY=%s, midX=%s, midY=%s", preX, preY, midX, midY);
//                    opErase.getPath().quadTo(preX, preY, midX, midY);
//
//                    break;
//                case TOOL_RECT_ERASER:
//                    OpDrawRect opDrawRect1 = (OpDrawRect) opPaint;
//                    opDrawRect1.setRight(x);
//                    opDrawRect1.setBottom(y);
//                    break;
//                default:
//                    return;
//            }
//
//            paintOpGeneratedListener.onOp(opPaint);
//
//        }
//
//        private void refreshPaintOp(){
//            paintOpGeneratedListener.onOp(null);
//        }
//
//        private void cancelPaintOp(){
//            opPaint = null;
//        }
//
//        private void confirmPaintOp(){
//            if (TOOL_RECT_ERASER == tool){
//                OpDrawRect opDrawRect = (OpDrawRect) opPaint;
//                opPaint = new OpRectErase(opDrawRect.getLeft(), opDrawRect.getTop(), opDrawRect.getRight(), opDrawRect.getBottom());
//                assignBasicInfo(opPaint);
//            }else if (TOOL_PENCIL == tool){
//                OpDrawPath opDrawPath = (OpDrawPath) opPaint;
//                List<PointF> points = opDrawPath.getPoints();
//                PointF lastPoint = points.get(points.size()-1);
//                opDrawPath.getPath().lineTo(lastPoint.x, lastPoint.y);
//            }else if (TOOL_ERASER == tool){
//                OpErase opErase = (OpErase) opPaint;
//                List<PointF> points = opErase.getPoints();
//                PointF lastPoint = points.get(points.size()-1);
//                opErase.getPath().lineTo(lastPoint.x, lastPoint.y);
//            }
//            KLog.p("new tmp op %s", opPaint);
//            shapePaintView.getTmpOps().offerLast(opPaint);
//            refreshPaintOp();
//            publisher.publish(opPaint);
//            opPaint = null;
//        }
//
//        private void confirmMatrixOp(){
//            OpMatrix opMatrix = new OpMatrix(shapePaintView.getMyMatrix());
//            assignBasicInfo(opMatrix);
//            publisher.publish(opMatrix); // TODO 判断当前缩放图层
//        }
//
//    }

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
    public void insertPic(String path) {
        if (null == publisher){
            KLog.p(KLog.ERROR,"publisher is null");
            return;
        }
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
        KLog.p("new tmp op %s", op);
        picPaintView.getTmpOps().offerLast(op);
        if (null != paintOpGeneratedListener) {
            paintOpGeneratedListener.onOp(null);
        }
        publisher.publish(op);
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

        shapePaintView.setOnEventListener(null!=publisher ? shapeViewEventListener : null);
        picPaintView.setOnEventListener(null!=publisher ? picViewEventListener : null);

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
