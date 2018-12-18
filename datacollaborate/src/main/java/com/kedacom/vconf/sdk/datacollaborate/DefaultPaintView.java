package com.kedacom.vconf.sdk.datacollaborate;

import android.content.Context;
import android.graphics.Matrix;
import android.util.AttributeSet;
import android.view.TextureView;

import com.kedacom.vconf.sdk.datacollaborate.bean.OpMatrix;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpPaint;

import java.util.Stack;

public class DefaultPaintView extends TextureView{

    private MyConcurrentLinkedDeque<OpPaint> renderOps = new MyConcurrentLinkedDeque<>(); // 绘制操作
//    private OpMatrix matrixOp = new OpMatrix(); // 缩放及位移
    private Stack<OpPaint> repealedOps = new Stack<>(); // 被撤销的操作
    private Matrix matrix = new Matrix();  // 缩放及位移

    public DefaultPaintView(Context context) {
        this(context, null);
    }

    public DefaultPaintView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    MyConcurrentLinkedDeque<OpPaint> getRenderOps(){
        return renderOps;
    }

//    OpMatrix getMatrixOp() {
//        return matrixOp;
//    }

    Stack<OpPaint> getRepealedOps(){
        return repealedOps;
    }

    Matrix getMyMatrix() {
        return matrix;
    }
}
