package com.kedacom.vconf.sdk.datacollaborate;

import android.content.Context;
import android.util.AttributeSet;
import android.view.TextureView;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpPaint;

import java.util.Stack;

public class DefaultPaintView extends TextureView{

    private MyConcurrentLinkedDeque<OpPaint> renderOps = new MyConcurrentLinkedDeque<>(); // 绘制操作
    private MyConcurrentLinkedDeque<OpPaint> matrixOps = new MyConcurrentLinkedDeque<>(); // 缩放及位变操作
    private Stack<OpPaint> repealedOps = new Stack<>(); // 被撤销的操作

    public DefaultPaintView(Context context) {
        this(context, null);
    }

    public DefaultPaintView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    MyConcurrentLinkedDeque<OpPaint> getRenderOps(){
        return renderOps;
    }

    MyConcurrentLinkedDeque<OpPaint> getMatrixOps() {
        return matrixOps;
    }

    Stack<OpPaint> getRepealedOps(){
        return repealedOps;
    }


}
