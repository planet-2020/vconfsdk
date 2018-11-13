package com.kedacom.vconf.sdk.datacollaborate;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;

import com.kedacom.vconf.sdk.base.KLog;

public class DefaultPaintView extends TextureView {
    private MyTouchListener myTouchListener;
    private GestureDetector gestureDetector;
    private OnMatrixChangedListener onMatrixChangedListener;

    public DefaultPaintView(Context context) {
        super(context);
    }

    public DefaultPaintView(Context context, OnMatrixChangedListener onMatrixChangedListener) {
        super(context);
        if (null != onMatrixChangedListener) {
            myTouchListener = new MyTouchListener();
            setOnTouchListener(myTouchListener);
            gestureDetector = new GestureDetector(getContext(), new GestureListener(myTouchListener));
            this.onMatrixChangedListener = onMatrixChangedListener;
        }
    }

    public class MyTouchListener implements OnTouchListener{
        private static final int MODE_DRAG = 1;		// 拖拽模式
        private static final int MODE_ZOOM = 2;		// 缩放模式
        private int mode = MODE_DRAG;

        private static final float DOUBLE_CLICK_SCALE = 2;	// 双击时的缩放倍数
        private float maxScale = 3.0f;	// 缩放倍数上限
        private float minScale = 0.5f; // 缩放倍数下限

        private Matrix initMatrix = new Matrix();  	// 初始matrix
        private Matrix curMatrix = new Matrix();    // 当前matrix

        private PointF startPoint = new PointF();	// 起始点
        private float startDis = 0;	// 起始距离

        private boolean needRefresh = false; // 是否需要刷新

        @Override
        public boolean onTouch(View v, MotionEvent event) {

            switch (event.getActionMasked()) {

                case MotionEvent.ACTION_DOWN:
                    mode=MODE_DRAG;
                    startPoint.set(event.getX(), event.getY()); // 记录起始点
                    KLog.p("ACTION_DOWN{%s}", event);
                    break;

                case MotionEvent.ACTION_POINTER_DOWN:
                    mode=MODE_ZOOM;
                    startDis = distance(event); // 记录起始距离
                    KLog.p("ACTION_POINTER_DOWN{%s}", event);
                    break;

                case MotionEvent.ACTION_MOVE:
                    if (mode == MODE_ZOOM) {
                        if (setZoomMatrix(event)){
                            needRefresh = true;
                            KLog.p("ACTION_ZOOM{%s}", event);
                        }
                    }else if (mode==MODE_DRAG) {
                        if (setDragMatrix(event)){
                            needRefresh = true;
                            KLog.p("ACTION_DRAG{%s}", event);
                        }
                    }
                    break;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    if (needRefresh) {
                        refresh();
                        needRefresh = false;
                    }
                    KLog.p("ACTION_UP{%s}", event);
                    break;

                case MotionEvent.ACTION_POINTER_UP:
                default:
//                    KLog.p("Discarded ACTION{%s}", event.getActionMasked());
                    break;
            }

            return gestureDetector.onTouchEvent(event);
        }

        // 设置拖拽
        public boolean setDragMatrix(MotionEvent event) {

//            if (!isZoomIn()){ // 仅放大状态下支持拖拽
//                return;
//            }
            float dx = event.getX() - startPoint.x; // 得到x轴的移动距离
            float dy = event.getY() - startPoint.y; // 得到x轴的移动距离

            if (Math.sqrt(dx*dx+dy*dy) < 10f){//避免和双击冲突,大于10f才算是拖动
                return false;
            }

            startPoint.set(event.getX(), event.getY()); //重置起始位置

            curMatrix.postTranslate(dx, dy);

            onMatrixChangedListener.OnMatrixChanged(curMatrix);

            return true;
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

            onMatrixChangedListener.OnMatrixChanged(curMatrix);

            return true;
        }


        // 调整显示效果
        private void refresh(){

            rectifyOverZoom();	// 矫正放缩过度

//            alignEdge();	// 对齐边界
//
//            tryFullView();  // 尝试全景显示

            onMatrixChangedListener.OnMatrixChanged(curMatrix);
        }


        // 是否处于放大状态
        private boolean isZoomIn() {
            return getScale(curMatrix) > getScale(initMatrix);
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

        // 处理双击
        public void onDoubleClick(){
            if (isZoomIn()){  // 已放大情况下双击，则缩小至初始状态
                curMatrix.set(initMatrix);
                KLog.p("zoom out");
            }else{ // 未放大情况下双击，则放大
//                curMatrix.set(getMatrix());
                curMatrix.postScale(DOUBLE_CLICK_SCALE, DOUBLE_CLICK_SCALE, getWidth()/2,getHeight()/2);
                KLog.p("zoom in");
            }

            onMatrixChangedListener.OnMatrixChanged(curMatrix);
        }

    }


    private class  GestureListener extends GestureDetector.SimpleOnGestureListener {
        private final MyTouchListener listener;
        public GestureListener(MyTouchListener listener) {
            this.listener=listener;
        }
        @Override
        public boolean onDown(MotionEvent e) {
            return true; //捕获Down事件
        }
        @Override
        public boolean onDoubleTap(MotionEvent e) {
            listener.onDoubleClick();
            return true;
        }
        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            return super.onSingleTapUp(e);
        }

        @Override
        public void onLongPress(MotionEvent e) {
            super.onLongPress(e);
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2,
                                float distanceX, float distanceY) {
            return super.onScroll(e1, e2, distanceX, distanceY);
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
                               float velocityY) {
            return super.onFling(e1, e2, velocityX, velocityY);
        }

        @Override
        public void onShowPress(MotionEvent e) {
            super.onShowPress(e);
        }

        @Override
        public boolean onDoubleTapEvent(MotionEvent e) {
            return super.onDoubleTapEvent(e);
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            return super.onSingleTapConfirmed(e);
        }

    }

    interface OnMatrixChangedListener{
        void OnMatrixChanged(Matrix matrix); //NOTE: 不要在该回调接口中做耗时操作
    }

    void setMatrix(Matrix matrix){
        myTouchListener.curMatrix.postConcat(matrix);
        onMatrixChangedListener.OnMatrixChanged(myTouchListener.curMatrix);
    }

}
