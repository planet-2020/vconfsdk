package com.kedacom.vconf.sdk.datacollaborate;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.TextureView;
import android.view.View;

import com.kedacom.vconf.sdk.base.KLog;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpPaint;
import java.util.Stack;

public class DefaultPaintView extends TextureView{

    private MyConcurrentLinkedDeque<OpPaint> renderOps = new MyConcurrentLinkedDeque<>(); // 绘制操作
    private Stack<OpPaint> repealedOps = new Stack<>(); // 被撤销的操作
    private MyConcurrentLinkedDeque<OpPaint> tmpOps = new MyConcurrentLinkedDeque<>(); // 临时绘制操作。所有的绘制操作需等到平台广播NTF后方能确认为生效的操作，在此之前的操作都作为临时操作保存在这里。
    private Matrix matrix = new Matrix();  // 缩放及位移
    private Context context;

    public DefaultPaintView(Context context) {
        this(context, null);
    }

    public DefaultPaintView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        KLog.p("context=%s", context);
        setOnTouchListener(new MyTouchListener());
    }

    MyConcurrentLinkedDeque<OpPaint> getRenderOps(){
        return renderOps;
    }

    Stack<OpPaint> getRepealedOps(){
        return repealedOps;
    }

    MyConcurrentLinkedDeque<OpPaint> getTmpOps() {
        return tmpOps;
    }

    Matrix getMyMatrix() {
        return matrix;
    }



    private class MyTouchListener implements OnTouchListener{
        private static final int STATE_IDLE = 0;
        private static final int STATE_SHAKING = 1;
        private static final int STATE_DRAGGING = 2;
        private static final int STATE_MULTIFINGERS_SHAKING = 3;
        private static final int STATE_MULTIFINGERS_DRAGGING = 4;
        private static final int STATE_SCALING = 5;
        private static final int STATE_SCALING_AND_MULTIFINGERS_DRAGGING = 6;
        private int state = STATE_IDLE;

        private PointF lastPoint = new PointF();	    // 上一个单指点
        private PointF lastMultiFingersFocusPoint = new PointF();   // 上一个双指聚焦点

        private ScaleGestureDetector scaleGestureDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener(){
            private float scaleFactor = 1.0f;
            private float lastScaleFactor = scaleFactor;
            private float scaleCenterX = 0;
            private float scaleCenterY = 0;

            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                KLog.p("focusX=%s, focusY=%s, isInProgress=%s, currentSpan=%s", detector.getFocusX(), detector.getFocusY(), detector.isInProgress(), detector.getCurrentSpan());
                KLog.p("scale=%s, lastScale=%s, |scale-lastScale|=%s", detector.getScaleFactor(), lastScaleFactor, Math.abs(detector.getScaleFactor()-lastScaleFactor));
                if (!detector.isInProgress()){
                    return false;
                }

                scaleFactor = detector.getScaleFactor();
                if (Math.abs(scaleFactor-lastScaleFactor) < 0.001){
                    return false;
                }
                lastScaleFactor = scaleFactor;

                float focusX = detector.getFocusX();
                float focusY = detector.getFocusY();
                if (Math.abs(focusX-lastMultiFingersFocusPoint.x)>10
                        || Math.abs(focusY-lastMultiFingersFocusPoint.y)>10) {
                    // 两次拖拽之间距离不可能过大，过大是手指数量有变化，这种情况需过滤掉。
                    lastMultiFingersFocusPoint.x = focusX; lastMultiFingersFocusPoint.y = focusY;
                    return false;
                }
                if (!isTolerable(lastMultiFingersFocusPoint.x, lastMultiFingersFocusPoint.y, focusX, focusY)) {
                    onEventListener.onMultiFingerDrag(focusX-lastMultiFingersFocusPoint.x, focusY-lastMultiFingersFocusPoint.y);
                    lastMultiFingersFocusPoint.x = focusX; lastMultiFingersFocusPoint.y = focusY;
                }

                onEventListener.onScale(scaleFactor, scaleCenterX, scaleCenterY);
                return true;
            }

            @Override
            public boolean onScaleBegin(ScaleGestureDetector detector) {
                KLog.p("state=%s, focusX = %s, focusY =%s", state, detector.getFocusX(), detector.getFocusY());
                if (STATE_MULTIFINGERS_SHAKING == state){
                    onEventListener.onMultiFingerDragBegin();
                }
                state = STATE_SCALING_AND_MULTIFINGERS_DRAGGING;
                scaleCenterX = getWidth()/2;
                scaleCenterY = getHeight()/2;
                onEventListener.onScaleBegin();
                return true;
            }

            @Override
            public void onScaleEnd(ScaleGestureDetector detector) {
//                KLog.p("state=%s, focusX = %s, focusY =%s", state, detector.getFocusX(), detector.getFocusY());
                onEventListener.onMultiFingerDragEnd();
                onEventListener.onScaleEnd();
                state = STATE_IDLE;
            }
        });

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (null == onEventListener){
                return false;
            }
            scaleGestureDetector.onTouchEvent(event);
            if (STATE_SCALING == state
                    || STATE_SCALING_AND_MULTIFINGERS_DRAGGING == state){
                return true;
            }

            switch (event.getActionMasked()) {

                case MotionEvent.ACTION_DOWN:
                    KLog.p("state=%s, ACTION_DOWN{%s}", state, event);
                    state = STATE_SHAKING;
                    lastPoint.set(event.getX(), event.getY()); // 记录起始点
                    onEventListener.onDown(event.getX(), event.getY());
                    break;

                case MotionEvent.ACTION_POINTER_DOWN:
                    KLog.p("state=%s, ACTION_POINTER_DOWN{%s}", state, event);
                    if (2 == event.getPointerCount()) {
                        if (STATE_DRAGGING == state) {
                            onEventListener.onDragEnd();
                        }
                        float focusX = (event.getX(1) + event.getX(0))/2;
                        float focusY = (event.getY(1) + event.getY(0))/2;
                        lastMultiFingersFocusPoint.set(focusX, focusY);
                        state = STATE_MULTIFINGERS_SHAKING;
                    }
                    break;

                case MotionEvent.ACTION_POINTER_UP:
                    KLog.p("state=%s, ACTION_POINTER_UP{%s}", state, event);
                    if (2 == event.getPointerCount()){ // 二个手指其中一个抬起，只剩一个手指了
                        if (STATE_MULTIFINGERS_DRAGGING == state){
                            onEventListener.onMultiFingerDragEnd();
                        }
                        int indx = 1==event.getActionIndex() ? 0 : 1;
                        lastPoint.set(event.getX(indx), event.getY(indx)); // 记录起始点
                        state = STATE_SHAKING;
                    }
                    break;

                case MotionEvent.ACTION_MOVE:
                    KLog.p("state=%s, ACTION_MOVE{%s}", state, event);
                    if (STATE_SHAKING == state) {
                        if (!isShakingTolerable(lastPoint.x, lastPoint.y, event.getX(), event.getY())){
                            onEventListener.onDragBegin(lastPoint.x, lastPoint.y);
                            lastPoint.x = event.getX(); lastPoint.y = event.getY();
                            state = STATE_DRAGGING;
                        }
                    }else if (STATE_DRAGGING == state){
                        if (!isTolerable(lastPoint.x, lastPoint.y, event.getX(), event.getY())) {
                            onEventListener.onDrag(event.getX(), event.getY());
                            lastPoint.x = event.getX(); lastPoint.y = event.getY();
                        }
                    }else if (STATE_MULTIFINGERS_SHAKING == state){
                        float focusX = (event.getX(1) + event.getX(0))/2;
                        float focusY = (event.getY(1) + event.getY(0))/2;
                        if (!isShakingTolerable(lastMultiFingersFocusPoint.x, lastMultiFingersFocusPoint.y, focusX, focusY)){
                            onEventListener.onMultiFingerDragBegin();
                            lastMultiFingersFocusPoint.x = focusX; lastMultiFingersFocusPoint.y = focusY;
                            state = STATE_MULTIFINGERS_DRAGGING;
                        }
                    }else if (STATE_MULTIFINGERS_DRAGGING == state){
                        float focusX = (event.getX(1) + event.getX(0))/2;
                        float focusY = (event.getY(1) + event.getY(0))/2;
                        if (!isTolerable(lastMultiFingersFocusPoint.x, lastMultiFingersFocusPoint.y, focusX, focusY)) {
                            onEventListener.onMultiFingerDrag(focusX-lastMultiFingersFocusPoint.x, focusY-lastMultiFingersFocusPoint.y);
                            lastMultiFingersFocusPoint.x = focusX; lastMultiFingersFocusPoint.y = focusY;
                        }
                    }
                    break;

                case MotionEvent.ACTION_UP:
                    KLog.p("state=%s, ACTION_UP{%s}", state, event);
                    if (STATE_DRAGGING == state) {
                        onEventListener.onDragEnd();
                    }
                    onEventListener.onUp(event.getX(), event.getY());
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

        private float distance(float startX, float startY, float stopX, float stopY){
            float dx = stopX - startX;
            float dy = stopY - startY;
            return (float) Math.sqrt(dx*dx + dy*dy);
        }

        private boolean isShakingTolerable(float startX, float startY, float stopX, float stopY){
            return Math.abs(stopX-startX)<15f && Math.abs(stopY-startY)<15f;
        }

        private boolean isTolerable(float startX, float startY, float stopX, float stopY){
            return Math.abs(stopX-startX)<5f && Math.abs(stopY-startY)<5f;
        }

    }


    interface IOnEventListener {
        /**第一个手指落下*/
        default void onDown(float x, float y){}
        /**最后一个手指拿起*/
        default void onUp(float x, float y){}
        /**单指拖动开始*/
        default void onDragBegin(float x, float y){}
        /**单指拖动*/
        default void onDrag(float x, float y){}
        /**单指拖动结束*/
        default void onDragEnd(){}
        /**多指拖动开始*/
        default void onMultiFingerDragBegin(){}
        /**多指拖动
         * @param dx x方向的位移
         * @param dy y方向的位移*/
        default void onMultiFingerDrag(float dx, float dy){}
        /**多指拖动结束*/
        default void onMultiFingerDragEnd(){}
        /**缩放开始*/
        default void onScaleBegin(){}
        /**缩放
         * @param factor 缩放因子。如1.0为没有缩放，2.0为放大至200%
         * @param scaleCenterX 缩放中心点X坐标
         * @param scaleCenterY 缩放中心点Y坐标*/
        default void onScale(float factor, float scaleCenterX, float scaleCenterY){}
        /**缩放结束*/
        default void onScaleEnd(){}
        /**长按*/
        default void onLongTouch(){}
    }

    private IOnEventListener onEventListener;
    void setOnEventListener(IOnEventListener onEventListener){
        this.onEventListener = onEventListener;
    }

}
