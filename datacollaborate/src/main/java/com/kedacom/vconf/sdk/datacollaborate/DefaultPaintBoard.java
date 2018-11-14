package com.kedacom.vconf.sdk.datacollaborate;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class DefaultPaintBoard extends FrameLayout {
    private DefaultPaintView picPaintView;
    private DefaultPaintView shapePaintView;

    public static final int LAYER_NONE = 0;
    public static final int LAYER_PIC = 1;
    public static final int LAYER_SHAPE = 2;
    public static final int LAYER_ALL = 3;
    private int focusedLayer = LAYER_ALL;

    public DefaultPaintBoard(@NonNull Context context) {
        this(context, null);
    }

    public DefaultPaintBoard(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater layoutInflater = LayoutInflater.from(context);
        View whiteBoard = layoutInflater.inflate(R.layout.default_whiteboard_layout, this);
        picPaintView = whiteBoard.findViewById(R.id.wb_pic_paint_view);
        picPaintView.setOpaque(false);
        shapePaintView = whiteBoard.findViewById(R.id.wb_shape_paint_view);
        shapePaintView.setOpaque(false);
        setBackgroundColor(Color.DKGRAY);
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

    DefaultPaintView getPicPaintView(){
        return picPaintView;
    }

    DefaultPaintView getShapePaintView(){
        return shapePaintView;
    }

    void focusLayer(int layer){
        if (LAYER_NONE<=layer && layer<=LAYER_ALL){
            focusedLayer = layer;
        }
    }

}
