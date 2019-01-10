package com.kedacom.vconf.sdk.datacollaborate;


import android.content.Context;
import android.graphics.PointF;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import com.kedacom.vconf.sdk.base.KLog;

class DefaultTouchListener implements View.OnTouchListener {
    private static final int STATE_IDLE = 0;
    private static final int STATE_SHAKING = 1;
    private static final int STATE_DRAGGING = 2;
    private static final int STATE_MULTIFINGERS_SHAKING = 3;
    private static final int STATE_MULTIFINGERS_DRAGGING = 4;
    private static final int STATE_SCALING = 6;
    private static final int STATE_SCALING_AND_MULTIFINGERS_DRAGGING = 7;
    private int state = STATE_IDLE;

    private PointF lastPoint = new PointF();	    // 上一个单指点
    private PointF lastMultiFingersFocusPoint = new PointF();   // 上一个双指聚焦点

    private GestureDetector gestureDetector;
    private ScaleGestureDetector scaleGestureDetector;
    private IOnEventListener onEventListener;

    public DefaultTouchListener(Context context, IOnEventListener onEventListener) {
        gestureDetector = new GestureDetector(context, new MyOnGestureListener());
        scaleGestureDetector = new ScaleGestureDetector(context, new MyScaleGestureListener());
        this.onEventListener = onEventListener;
    }


    private class MyOnGestureListener extends GestureDetector.SimpleOnGestureListener{
        @Override
        public boolean onDown(MotionEvent e) {
            return true; // 返回true表示想要处理后续手势
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            onEventListener.onSingleTap(e.getX(), e.getY());
            return true;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            onEventListener.onLongPress(e.getX(), e.getY());
        }
    }

    private class MyScaleGestureListener extends ScaleGestureDetector.SimpleOnScaleGestureListener{
        private float scaleFactor = 1.0f;
        private float lastScaleFactor = scaleFactor;


        @Override
        public boolean onScale(ScaleGestureDetector detector) {
//                KLog.p("focusX=%s, focusY=%s, lastMultiFingersFocusPoint.x=%s, lastMultiFingersFocusPoint.y=%s", detector.getFocusX(), detector.getFocusY(), lastMultiFingersFocusPoint.x, lastMultiFingersFocusPoint.y);
//                KLog.p("scale=%s, lastScale=%s, |scale-lastScale|=%s", detector.getScaleFactor(), lastScaleFactor, Math.abs(detector.getScaleFactor()-lastScaleFactor));
            if (!detector.isInProgress()){
                KLog.p(KLog.WARN, "!detector.isInProgress()");
                return false;
            }

            scaleFactor = detector.getScaleFactor();
            if (Math.abs(scaleFactor-lastScaleFactor) > 0.001){ // XXX 去掉限制看缩放效果会不会更顺滑
                onEventListener.onScale(scaleFactor);
                lastScaleFactor = scaleFactor;
            }

            float focusX = detector.getFocusX();
            float focusY = detector.getFocusY();
            if (!isTolerable(lastMultiFingersFocusPoint.x, lastMultiFingersFocusPoint.y, focusX, focusY)) {
                onEventListener.onMultiFingerDrag(focusX - lastMultiFingersFocusPoint.x, focusY - lastMultiFingersFocusPoint.y);
                lastMultiFingersFocusPoint.x = focusX; lastMultiFingersFocusPoint.y = focusY;
            }

            return true;
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            float focusX = detector.getFocusX();
            float focusY = detector.getFocusY();
            float scale = detector.getScaleFactor();
//                KLog.p("state=%s, focusX = %s, focusY =%s, scale=%s", state, focusX, focusY, scale);
            if (STATE_MULTIFINGERS_SHAKING == state){
                onEventListener.onMultiFingerDragBegin();
            }
            state = STATE_SCALING_AND_MULTIFINGERS_DRAGGING;
            lastMultiFingersFocusPoint.x = focusX; lastMultiFingersFocusPoint.y = focusY;
            lastScaleFactor = scaleFactor = scale;
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
    }



    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (null == onEventListener){
            return false;
        }

        switch (event.getActionMasked()) {

            case MotionEvent.ACTION_DOWN:
                KLog.p("state=%s, ACTION_DOWN{%s}", state, event);
                if (!onEventListener.onDown(event.getX(), event.getY())){
                    return false; // 放弃处理后续事件
                }
                state = STATE_SHAKING;
                lastPoint.set(event.getX(), event.getY()); // 记录起始点
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
                    onEventListener.onSecondPointerDown(event.getX(), event.getY());
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
                    onEventListener.onLastPointerLeft(event.getX(indx), event.getY(indx));
                }
                break;

            case MotionEvent.ACTION_MOVE:
//                    KLog.p("state=%s, ACTION_MOVE{%s}", state, event);
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

        scaleGestureDetector.onTouchEvent(event);

        gestureDetector.onTouchEvent(event);

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



    interface IOnEventListener {
        /**第一个手指落下
         * @return true——想要继续处理后续事件；false——放弃处理后续事件
         * */
        default boolean onDown(float x, float y){return true;}
        /** 第二个手指落下*/
        default void onSecondPointerDown(float x, float y){}
        /**第二个手指拿起，仅剩最后一个手指
         * @param x 最后一个手指的x坐标
         * @param y 最后一个手指的y坐标*/
        default void onLastPointerLeft(float x, float y){}
        /**最后一个手指拿起*/
        default void onUp(float x, float y){}
        /**单击*/
        default void onSingleTap(float x, float y){}
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
         * */
        default void onScale(float factor){}
        /**缩放结束*/
        default void onScaleEnd(){}
        /**长按*/
        default void onLongPress(float x, float y){}
    }

    void setOnEventListener(IOnEventListener onEventListener){
        this.onEventListener = onEventListener;
    }

}

