package com.kedacom.vconf.sdk.datacollaborate;

import android.graphics.Bitmap;
import android.view.View;

import com.kedacom.vconf.sdk.datacollaborate.bean.BoardInfo;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpPaint;

public interface IPaintBoard {
    // 工具
    int TOOL_NONE = 0;
    int TOOL_HAND = 7;
    int TOOL_PENCIL = 1;
    int TOOL_LINE = 2;
    int TOOL_RECT = 3;
    int TOOL_OVAL = 4;
    int TOOL_ERASER = 8;
    int TOOL_RECT_ERASER = 5;
    int TOOL_PIC_SELECTOR = 6;

    // 图层
    int LAYER_NONE = 100;
    int LAYER_PIC =  101;
    int LAYER_SHAPE =102;
    int LAYER_PIC_AND_SHAPE =103;
    int LAYER_ALL =  109;

    String getBoardId();
    BoardInfo getBoardInfo();
    View getBoardView();

    void setTool(int style);
    int getTool();

    /**
     * 设置画笔粗细。
     * @param width 粗细。单位：pixel
     * */
    void setPaintStrokeWidth(int width);
    int getPaintStrokeWidth();

    /**
     * 设置画笔颜色。
     * @param color 颜色值。NOTE:必须为正整数，如果是字面值注意加后缀"L"，如0xFFFFFFFFL。
     * */
    void setPaintColor(long color);
    long getPaintColor();

    /**
     * 设置橡皮擦尺寸（size*size）。
     * @param size 橡皮擦尺寸。单位：pixel
     * */
    void setEraserSize(int size);
    int getEraserSize();

    /**
     * 聚焦图层。
     * 聚焦图层后，放缩、位移、擦除、清屏、截屏等操作均针对该图层，并且触屏事件只被该图层处理。
     * 默认是LAYER_ALL。
     * */
    void focusLayer(int layer);

    void insertPic(Bitmap pic);

    /**
     * 截屏。
     * @param layer 图层。
     * */
    Bitmap snapshot(int layer);
    void undo();
    void redo();
    void clearScreen();
    void zoom(int percentage);
    int getZoom();

    void setPublisher(IPublisher publisher);
    interface IPublisher{
        void publish(OpPaint Op);
    }
}
