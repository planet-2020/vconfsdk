package com.kedacom.vconf.sdk.datacollaborate;

import android.content.Context;
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
import com.kedacom.vconf.sdk.datacollaborate.bean.OpDrawLine;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpDrawOval;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpDrawPath;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpDrawRect;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpMatrix;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpPaint;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpRedo;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpUndo;

import java.util.ArrayList;

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

    // 画笔粗细W（像素值）
    private int paintStrokeWidth = 5;

    // 画笔颜色
    private int paintColor = Color.GREEN;

    private static final int MIN_ZOOM = 50;
    private static final int MAX_ZOOM = 300;
    private int zoom = 100;

    private BoardInfo boardInfo;

    private IOnPaintOpGeneratedListener paintOpGeneratedListener;
    private IOnMatrixOpGeneratedListener matrixOpGeneratedListener;
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
        private static final int MODE_NORMAL = 1;
        private static final int MODE_SCALE = 2;
        private static final int MODE_DRAG = 3;
        private static final int MODE_SCALE_AND_DRAG = 4;
        private int mode = MODE_NORMAL;

        private static final float DOUBLE_CLICK_SCALE = 2;	// 双击时的缩放倍数
        private float maxScale = 3.0f;	// 缩放倍数上限
        private float minScale = 0.5f; // 缩放倍数下限

        private Matrix initMatrix = new Matrix();  	// 初始matrix
        private Matrix curMatrix = new Matrix();    // 当前matrix

        private PointF startPoint = new PointF();	// 起始点
        private float startDis = 0;	// 起始距离

        private boolean bMovingFarEnough = false;

        private float scaleFactor = 1.0f;
        private float lastScaleFactor = scaleFactor;
        private ScaleGestureDetector scaleGestureDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener(){
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                KLog.p("focusX = " + detector.getFocusX());
                KLog.p("focusY = " + detector.getFocusY());
                KLog.p("scale=%s, lastScale=%s, |scale-lastScale|=%s", scaleFactor, lastScaleFactor, Math.abs(scaleFactor-lastScaleFactor));
                scaleFactor *= detector.getScaleFactor();
//                if (scaleFactor == lastScaleFactor){
//                    return true;
//                }
//                if (Math.abs(scaleFactor-lastScaleFactor) > 0.01) {
//                    lastScaleFactor = scaleFactor;
//                }
                if (null != matrixOpGeneratedListener){
                    ((OpMatrix)opPaint).getMatrix().setScale(scaleFactor, scaleFactor, detector.getFocusX(), detector.getFocusY());
                    matrixOpGeneratedListener.onMatrix(opPaint);
                }
                return  true;
            }

            @Override
            public boolean onScaleBegin(ScaleGestureDetector detector) {
                KLog.p("focusX = " + detector.getFocusX());
                KLog.p("focusY = " + detector.getFocusY());
                mode = MODE_SCALE_AND_DRAG;
                if (null != paintOpGeneratedListener){
                    opPaint = new OpMatrix();
                    assignBasicInfo(opPaint);
                }
                return true;
            }

            @Override
            public void onScaleEnd(ScaleGestureDetector detector) {
                KLog.p("focusX = " + detector.getFocusX());
                KLog.p("focusY = " + detector.getFocusY());
                KLog.p("scale = " + scaleFactor);
//                mode = MODE_NORMAL;
                opPaint = null;
            }
        });

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            KLog.p("mode = %s", mode);
            scaleGestureDetector.onTouchEvent(event);
            if (MODE_SCALE == mode || MODE_SCALE_AND_DRAG == mode){
                return true;
            }

            switch (event.getActionMasked()) {

                case MotionEvent.ACTION_DOWN:
                    KLog.p("ACTION_DOWN{%s}", event);
//                    mode=MODE_ONE_FINGER;
                    startPoint.set(event.getX(), event.getY()); // 记录起始点
                    bMovingFarEnough = false;
                    if (null != paintOpGeneratedListener) {
                        createPaintOp(event);
                    }
                    break;

                case MotionEvent.ACTION_POINTER_DOWN:
                    KLog.p("ACTION_POINTER_DOWN{%s}", event);
//                    if (2 == event.getPointerCount()){
//                        mode=MODE_TWO_FINGER;
//                        if (null != paintOpGeneratedListener) {
//                            if (bMovingFarEnough){
//                                confirmPaintOp(event);
//                            }else{
//                                cancelPaintOp(event);
//                            }
//                        }
//                    }
//                    startDis = distance(event); // 记录起始距离
                    break;

                case MotionEvent.ACTION_POINTER_UP:
                    KLog.p("ACTION_POINTER_UP{%s}", event);
//                    if (2 == event.getPointerCount()){ // 二个手指其中一个抬起，只剩一个手指了，切换到单手指模式
//                        mode=MODE_ONE_FINGER;
//                        bMovingFarEnough = false;
////                        startPoint.set(event.getX(), event.getY()); // 记录起始点  // TODO 获取仅剩的手指坐标
//                        if (null != paintOpGeneratedListener) {
//                            confirmPaintOp(event);     // 确认之前的操作
//                            createPaintOp(event);       // 重新创建新的操作
//                        }
//                    }
                    break;

                case MotionEvent.ACTION_MOVE:
//                    if (mode == MODE_ZOOM) {
//                        if (setZoomMatrix(event)){
//                            needRefresh = true;
////                            KLog.p("ACTION_ZOOM{%s}", event);
//                        }
//                    }else if (mode==MODE_DRAG) {
//                        if (setDragMatrix(event)){
//                            needRefresh = true;
//                            KLog.p("ACTION_DRAG{%s}", event);
//                        }
//                    }
                    if (!bMovingFarEnough){  // TODO 包含多个点全部放进op里面
                        int dx = (int) (event.getX() - startPoint.x);
                        int dy = (int) (event.getY() - startPoint.y);
//                        KLog.p("cur distance=%s", Math.sqrt(dx*dx+dy*dy));
                        bMovingFarEnough =  Math.sqrt(dx*dx+dy*dy) > 15;
                    }
                    if (bMovingFarEnough && null != paintOpGeneratedListener) {
                        adjustPaintOp(event);
                    }
                    break;

                case MotionEvent.ACTION_UP:
//                    if (needRefresh) {
//                        refresh();
//                        needRefresh = false;
//                    }
                    KLog.p("ACTION_UP{%s}", event);
                    if (null != paintOpGeneratedListener) {
                        confirmPaintOp(event);
                    }
                    break;

                case MotionEvent.ACTION_CANCEL:
                    KLog.p("ACTION_CANCEL{%s}", event);
                    break;
                default:
//                    KLog.p("Discarded ACTION{%s}", event.getActionMasked());
                    break;
            }

            return true;
        }


//        private boolean isMovingFarEnough(MotionEvent event){
//            int dx = (int) (event.getX() - startPoint.x);
//            int dy = (int) (event.getY() - startPoint.y);
//            return Math.sqrt(dx*dx+dy*dy) > 10;
//        }


        // 设置拖拽
        public boolean setDragMatrix(MotionEvent event) {
            float dx = event.getX() - startPoint.x; // 得到x轴的移动距离
            float dy = event.getY() - startPoint.y; // 得到x轴的移动距离

            if (Math.sqrt(dx*dx+dy*dy) < 10f){//避免和双击冲突,大于10f才算是拖动
                return false;
            }

            startPoint.set(event.getX(), event.getY()); //重置起始位置

            curMatrix.postTranslate(dx, dy);


//            onMatrixChangedListener.onMatrixChanged(curMatrix);

            return true;
        }

        private void assignBasicInfo(OpPaint op){
            op.setConfE164(boardInfo.getConfE164());
            op.setBoardId(boardInfo.getId());
            op.setPageId(boardInfo.getPageId());
        }

        private OpPaint opPaint;
        private OpPaint createPaintOp(MotionEvent event){
            switch (tool){
                case TOOL_PENCIL:
                    OpDrawPath opDrawPath = new OpDrawPath(new ArrayList<>());
                    opDrawPath.getPoints().add(new PointF(event.getX(), event.getY()));
                    opDrawPath.getPath().moveTo(event.getX(), event.getY());
                    opPaint = opDrawPath;
                    break;
                case TOOL_LINE:
                    OpDrawLine opDrawLine = new OpDrawLine();
                    opPaint = opDrawLine;
                    break;
                case TOOL_RECT:
                    OpDrawRect opDrawRect = new OpDrawRect();
                    opPaint = opDrawRect;
                    break;
                case TOOL_OVAL:
                    OpDrawOval opDrawOval = new OpDrawOval();
                    opPaint = opDrawOval;
                    break;
                default:
                    return null;
            }
            assignBasicInfo(opPaint);
            paintOpGeneratedListener.onCreated(opPaint);
            return opPaint;
        }

        private void adjustPaintOp(MotionEvent event){
            switch (tool){
                case TOOL_PENCIL:
                    OpDrawPath opDrawPath = (OpDrawPath) opPaint;
                    opDrawPath.setStrokeWidth(paintStrokeWidth);
                    opDrawPath.setColor(paintColor);
                    opDrawPath.getPoints().add(new PointF(event.getX(), event.getY()));
                    opDrawPath.getPath().lineTo(event.getX(), event.getY());
                    break;
                case TOOL_LINE:
                    OpDrawLine opDrawLine = (OpDrawLine) opPaint;
                    opDrawLine.setStartX(startPoint.x);
                    opDrawLine.setStartY(startPoint.y);
                    opDrawLine.setStopX(event.getX());
                    opDrawLine.setStopY(event.getY());
                    opDrawLine.setStrokeWidth(paintStrokeWidth);
                    opDrawLine.setColor(paintColor);
                    break;
                case TOOL_RECT:
                    OpDrawRect opDrawRect = (OpDrawRect) opPaint;
                    opDrawRect.setLeft(startPoint.x);
                    opDrawRect.setTop(startPoint.y);
                    opDrawRect.setRight(event.getX());
                    opDrawRect.setBottom(event.getY());
                    opDrawRect.setStrokeWidth(paintStrokeWidth);
                    opDrawRect.setColor(paintColor);
                    break;
                case TOOL_OVAL:
                    OpDrawOval opDrawOval = (OpDrawOval) opPaint;
                    opDrawOval.setLeft(startPoint.x);
                    opDrawOval.setTop(startPoint.y);
                    opDrawOval.setRight(event.getX());
                    opDrawOval.setBottom(event.getY());
                    opDrawOval.setStrokeWidth(paintStrokeWidth);
                    opDrawOval.setColor(paintColor);
                    break;
                default:
                    return;
            }

            paintOpGeneratedListener.onAdjust(opPaint);

        }

        private void cancelPaintOp(MotionEvent event){
            paintOpGeneratedListener.onCancel(opPaint);
            opPaint = null;
        }

        private void confirmPaintOp(MotionEvent event){
            paintOpGeneratedListener.onConfirm(opPaint);
            if (null != publisher){
                publisher.publish(opPaint);
            }
            opPaint = null;
        }


        // 设置放缩
        private boolean setZoomMatrix(MotionEvent event) {
            if(event.getPointerCount()<2) {
                return false;
            }

            float endDis = distance(event);// 结束距离
            if (endDis < 10f){
                return false;
            }

            float scale = endDis / startDis;// 得到缩放倍数
            startDis=endDis;//重置距离
            curMatrix.postScale(scale, scale, getWidth()/2,getHeight()/2);

//            onMatrixChangedListener.onMatrixChanged(curMatrix);

            return true;
        }


        private void refresh(){

            rectifyOverZoom();	// 矫正放缩过度

//            onMatrixChangedListener.onMatrixChanged(curMatrix);
        }


        // 矫正放缩过度
        private void rectifyOverZoom(){
            float curScale = getScale(curMatrix);
            if (curScale > maxScale){  // 放大过头
                curMatrix.postScale(maxScale/curScale, maxScale/curScale, getWidth()/2, getHeight()/2);
            } else if (curScale < minScale){ // 缩小过头
                curMatrix.postScale(minScale/curScale, minScale/curScale, getWidth()/2,getHeight()/2);
            }
        }

        // 获取Matrix的缩放倍数
        private float getScale(Matrix matrix){
            float[] values = new float[9];
            matrix.getValues(values);
            return values[Matrix.MSCALE_X];
        }

        // 计算两个手指间的距离
        private float distance(MotionEvent event) {
            float dx = event.getX(1) - event.getX(0);
            float dy = event.getY(1) - event.getY(0);
            return (float) Math.sqrt(dx * dx + dy * dy);
        }

    }


    public DefaultPaintView getPicPaintView(){
        return picPaintView;
    }

    public DefaultPaintView getShapePaintView(){
        return shapePaintView;
    }


    private void feedback(OpPaint op){
        if (null != paintOpGeneratedListener){
            op.setBoardId(boardInfo.getId());
            paintOpGeneratedListener.onConfirm(op);
            if (null != publisher){
                publisher.publish(op);
            }
        }
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
    public void setPaintColor(int color) {
        this.paintColor = color;
    }

    @Override
    public int getPaintColor() {
        return paintColor;
    }

    @Override
    public View snapshot(int layer) {
        return null;
    }

    @Override
    public void undo() {
        if (null != paintOpGeneratedListener){
            OpPaint op = new OpUndo();
            op.setBoardId(boardInfo.getId());
            paintOpGeneratedListener.onConfirm(op);
            if (null != publisher){
                publisher.publish(op);
            }
        }
    }

    @Override
    public void redo() {
        if (null != paintOpGeneratedListener){
            OpPaint op = new OpRedo();
            op.setBoardId(boardInfo.getId());
            paintOpGeneratedListener.onConfirm(op);
            if (null != publisher){
                publisher.publish(op);
            }
        }
    }

    @Override
    public void clearScreen() {
        if (null != paintOpGeneratedListener){
            OpPaint op = new OpClearScreen();
            op.setBoardId(boardInfo.getId());
            paintOpGeneratedListener.onConfirm(op);
            if (null != publisher){
                publisher.publish(op);
            }
        }
    }

    @Override
    public void zoom(int percentage) {
        zoom = (MIN_ZOOM<=percentage && percentage<=MAX_ZOOM) ? percentage : (percentage<MIN_ZOOM ? MIN_ZOOM : MAX_ZOOM);
    }

    @Override
    public int getZoom() {
        return zoom;
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
        void onCreated(OpPaint opPaint);
        void onAdjust(OpPaint opPaint);
        void onCancel(OpPaint opPaint);
        void onConfirm(OpPaint opPaint);
    }

    void setOnMatrixOpGeneratedListener(IOnMatrixOpGeneratedListener onMatrixOpGeneratedListener) {
        matrixOpGeneratedListener = onMatrixOpGeneratedListener;
    }
    interface IOnMatrixOpGeneratedListener{
        void onMatrix(OpPaint opPaint);
    }

}
