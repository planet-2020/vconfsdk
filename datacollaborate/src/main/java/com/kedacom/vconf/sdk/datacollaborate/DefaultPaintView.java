package com.kedacom.vconf.sdk.datacollaborate;

import android.content.Context;
import android.graphics.Matrix;
import android.util.AttributeSet;
import android.view.TextureView;

import com.kedacom.vconf.sdk.datacollaborate.bean.OpPaint;

import java.util.Stack;

public class DefaultPaintView extends TextureView{

    private MyConcurrentLinkedDeque<OpPaint> renderOps = new MyConcurrentLinkedDeque<>(); // 绘制操作
    private Stack<OpPaint> repealedOps = new Stack<>(); // 被撤销的操作
    private MyConcurrentLinkedDeque<OpPaint> tmpOps = new MyConcurrentLinkedDeque<>(); // 临时绘制操作。所有的绘制操作需等到平台广播NTF后方能确认为生效的操作，在此之前的操作都作为临时操作保存在这里。
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

    Stack<OpPaint> getRepealedOps(){
        return repealedOps;
    }

    MyConcurrentLinkedDeque<OpPaint> getTmpOps() {
        return tmpOps;
    }

    Matrix getMyMatrix() {
        return matrix;
    }
}
