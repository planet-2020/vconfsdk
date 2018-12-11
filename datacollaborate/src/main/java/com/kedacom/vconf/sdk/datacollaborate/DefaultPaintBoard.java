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
    private long paintColor = 0xFFFFFFFFL;

    private static final int MIN_ZOOM = 50;
    private static final int MAX_ZOOM = 300;
    private int zoom = 100;

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
        private static final int MODE_NORMAL = 1;
        private static final int MODE_SCALE = 2;
        private static final int MODE_DRAG = 3;
        private static final int MODE_SCALE_AND_DRAG = 4;
        private int mode = MODE_NORMAL;

        private static final int STATE_IDLE = 0;
        private static final int STATE_SHAKING = 1;
        private static final int STATE_DRAWING = 2;
        private static final int STATE_SCALING = 3;
        private static final int STATE_DRAGING = 4;
        private static final int STATE_SCALING_AND_DRAGING = 5;
        private int state = STATE_IDLE;

        private static final float DOUBLE_CLICK_SCALE = 2;	// 双击时的缩放倍数
        private float maxScale = 3.0f;	// 缩放倍数上限
        private float minScale = 0.5f; // 缩放倍数下限

        private Matrix initMatrix = new Matrix();  	// 初始matrix
        private Matrix curMatrix = new Matrix();    // 当前matrix

        private PointF startPoint = new PointF();	// 起始绘制点
        private PointF startDragPoint = new PointF();	// 起始拖拽点
        private PointF zoomCenter = new PointF();

        private boolean bMovingFarEnough = false;

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

                OpMatrix opMatrix = shapePaintView.getMatrixOp();
                KLog.p("zoomCenter={%s, %s} ",  zoomCenter.x, zoomCenter.y);
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
                return true;
            }

            @Override
            public void onScaleEnd(ScaleGestureDetector detector) {
                KLog.p("state=%s, focusX = %s, focusY =%s, scale = %s", state, detector.getFocusX(), detector.getFocusY(), scaleFactor);
                if (null != publisher){
                    publisher.publish(shapePaintView.getMatrixOp());
                }
                state = STATE_IDLE;  // TODO STATE_SHAKING ?
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
///                        event.getX(1) + event.getX(0)
//                        startDragPoint = distance(event); // 记录起始距离
//                        if (null != paintOpGeneratedListener) {
////                            createPaintOp(event); // TODO 拖动操作。matrix操作。
//                        }
                        startDragPoint.set((event.getX(1) + event.getX(0))/2,
                                (event.getY(1) + event.getY(0))/2);
                        state = STATE_DRAGING;
                    }
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
//                    startDragPoint = distance(event); // 记录起始距离
                    break;

                case MotionEvent.ACTION_POINTER_UP:
                    KLog.p("state=%s, ACTION_POINTER_UP{%s}", state, event);
                    if (2 == event.getPointerCount()){ // 二个手指其中一个抬起，只剩一个手指了，切换到单手指模式
//                        mode=MODE_ONE_FINGER;
//                        bMovingFarEnough = false;
////                        startPoint.set(event.getX(), event.getY()); // 记录起始点  // TODO 获取仅剩的手指坐标
//                        if (null != paintOpGeneratedListener) {
//                            confirmPaintOp(event);     // 确认之前的操作
//                            createPaintOp(event);       // 重新创建新的操作
//                        }
                        state = STATE_SHAKING;
                        int indx = 1==event.getActionIndex() ? 0 : 1;
                        createPaintOp(event.getX(indx), event.getY(indx));
                    }
                    break;

                case MotionEvent.ACTION_MOVE:
                    KLog.p("state=%s, ACTION_MOVE{%s}", state, event);
                    if (STATE_SHAKING == state) {
                        int dx = (int) (event.getX() - startPoint.x);
                        int dy = (int) (event.getY() - startPoint.y);
                        if (Math.sqrt(dx * dx + dy * dy) > 15){
                            state = STATE_DRAWING;
                            adjustPaintOp(event);
                        }
//                        if (!bMovingFarEnough) {  // TODO 包含多个点全部放进op里面
//                            int dx = (int) (event.getX() - startPoint.x);
//                            int dy = (int) (event.getY() - startPoint.y);
////                        KLog.p("cur distance=%s", Math.sqrt(dx*dx+dy*dy));
//                            bMovingFarEnough = Math.sqrt(dx * dx + dy * dy) > 15;
//                        }
//                        if (bMovingFarEnough && null != paintOpGeneratedListener) {
//                            adjustPaintOp(event);
//                        }
                    }else if (STATE_DRAWING == state){
                        adjustPaintOp(event);
                    }else if (STATE_DRAGING == state){
                        OpMatrix opMatrix = shapePaintView.getMatrixOp();
                        opMatrix.getMatrix().postTranslate((event.getX(1) + event.getX(0))/2 -startDragPoint.x,
                                (event.getY(1) + event.getY(0))/2-startDragPoint.y);
                        startDragPoint.set((event.getX(1) + event.getX(0))/2,
                                (event.getY(1) + event.getY(0))/2);
                        refreshPaintOp();
                    }
                    break;

                case MotionEvent.ACTION_UP:
//                    if (needRefresh) {
//                        refresh();
//                        needRefresh = false;
//                    }
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
        private void createPaintOp(float startX, float startY){
            switch (tool){
                case TOOL_PENCIL:
                    OpDrawPath opDrawPath = new OpDrawPath(new ArrayList<>());
                    opDrawPath.getPoints().add(new PointF(startX, startY));
                    opDrawPath.getPath().moveTo(startX, startY);
                    opPaint = opDrawPath;
                    break;
                case TOOL_LINE:
                    opPaint = new OpDrawLine();
                    break;
                case TOOL_RECT:
                    opPaint = new OpDrawRect();
                    break;
                case TOOL_OVAL:
                    opPaint = new OpDrawOval();
                    break;
                default:
                    KLog.p(KLog.ERROR, "unknown TOOL %s", tool);
                    return;
            }
            startPoint.set(startX, startY); // 记录起始点
            assignBasicInfo(opPaint);
        }

        private void adjustPaintOp(MotionEvent event){
            switch (tool){ // TODO 事件会打包多个进而造成图形轨迹不平滑，getHistorySize
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
            paintOpGeneratedListener.onConfirm(opPaint);
            if (null != publisher){
                publisher.publish(opPaint);
            }
            opPaint = null;
        }


        // 设置放缩
//        private boolean setZoomMatrix(MotionEvent event) {
//            if(event.getPointerCount()<2) {
//                return false;
//            }
//
//            float endDis = distance(event);// 结束距离
//            if (endDis < 10f){
//                return false;
//            }
//
//            float scale = endDis / startDragPoint;// 得到缩放倍数
//            startDragPoint =endDis;//重置距离
//            curMatrix.postScale(scale, scale, getWidth()/2,getHeight()/2);
//
////            onMatrixChangedListener.onMatrixChanged(curMatrix);
//
//            return true;
//        }


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
    public void setPaintColor(long color) {
        this.paintColor = color;
    }

    @Override
    public long getPaintColor() {
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
        void onAdjust(OpPaint opPaint);
        void onConfirm(OpPaint opPaint);
    }

}
