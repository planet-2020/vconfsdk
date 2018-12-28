package com.kedacom.vconf.sdk.datacollaborate;

import android.graphics.Bitmap;
import android.view.View;

import com.kedacom.vconf.sdk.datacollaborate.bean.BoardInfo;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpPaint;

/**
 * 画板。
 * 负责：
 * 提供画板界面；
 * 提供工具栏相关接口如设置画直线画圆，设置颜色画笔粗细，插入图片等；
 * 处理用户触屏事件结合工具栏设置生成相应绘制操作并上报给用户；
 * */
public interface IPaintBoard {

    /**
     * 获取画板ID。
     * 画板信息中也包含该ID，此方法为便捷方法。
     * */
    String getBoardId();

    /**
     * 获取画板信息
     * */
    BoardInfo getBoardInfo();

    /**
     * 获取画板View。
     * 用户需要将该view添加到自己的view树中后方可正常展示画板及在画板上绘制。
     * 用户亦可通过为此view设置背景来给画板设置背景。
     * @return 画板对应的View
     * */
    View getBoardView();

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
    /**
     * 设置画板工具。
     * 在进行相关绘制操作前需先设置好对应的工具。
     * 如画任意曲线需设置TOOL_PENCIL, 画直线需设置TOOL_LINE。
     * */
    void setTool(int style);

    /**
     * 获取当前画板工具
     * */
    int getTool();

    /**
     * 设置画笔粗细。
     * @param width 粗细。单位：pixel
     * */
    void setPaintStrokeWidth(int width);

    /**
     * 获取画笔粗细
     * */
    int getPaintStrokeWidth();

    /**
     * 设置画笔颜色。
     * @param color 颜色值。NOTE:必须为正整数，如果是字面值注意加后缀"L"，如0xFFFFFFFFL。
     * */
    void setPaintColor(long color);

    /**
     * 获取画笔颜色
     * */
    long getPaintColor();

    /**
     * 设置橡皮擦尺寸（size*size）。
     * @param size 橡皮擦尺寸。单位：pixel
     * */
    void setEraserSize(int size);

    /**
     * 获取橡皮擦尺寸
     * */
    int getEraserSize();

    /**
     * 聚焦图层。
     * 聚焦图层后，放缩、位移、擦除、清屏、截屏等操作均针对该图层，并且触屏事件只被该图层处理。
     * 默认是LAYER_ALL。
     * */
    void focusLayer(int layer);  // TODO 删除，改为每个接口添加layer参数

    /**
     * 插入图片
     * @param picPath 图片绝对路径
     * */
    void insertPic(String picPath);

    // 图层
    int LAYER_NONE = 100;
    int LAYER_PIC =  101;
    int LAYER_SHAPE =102;
    int LAYER_PIC_TMP =103;
    int LAYER_PIC_AND_SHAPE =104;
    int LAYER_ALL =  109;
    /**
     * 截屏。
     * @param layer 图层。
     * */
    Bitmap snapshot(int layer);

    /**
     * 撤销。
     * */
    void undo();

    /**
     * 恢复被撤销的操作。
     * */
    void redo();

    /**
     * 清屏
     * */
    void clearScreen();

    /**
     * 放缩
     * @param percentage 百分数。如50代表50%。
     * */
    void zoom(int percentage);

    /**
     * 获取放缩百分数
     * @return 百分数。如50代表50%。
     * */
    int getZoom();

    /**
     * 发布者。
     * 己端作为“创作者”角色完成创作后通过发布者将内容（绘制操作）发布出去。
     * */
    interface IPublisher{
        /**
         * 产生了绘制操作可供发布
         * @param Op 绘制操作*/
        void publish(OpPaint Op);
    }
    IPaintBoard setPublisher(IPublisher publisher);

    /**
     * 可撤销状态变化监听器。
     * */
    interface IOnRepealableStateChangedListener {
        /**@param repealedOpsCount 已被撤销操作数量
         * @param remnantOpsCount 剩下的可撤销操作数量。如画了3条线撤销了1条则repealedOpsCount=1，remnantOpsCount=2。
         *                        NOTE: 此处的可撤销数量是具体业务无关的，“可撤销”指示的是操作类型，如画线画圆等操作是可撤销的而插入图片放缩等是不可撤销的。
         * */
        void onRepealableStateChanged(int repealedOpsCount, int remnantOpsCount);
    }
    IPaintBoard setOnRepealableStateChangedListener(IOnRepealableStateChangedListener onRepealableStateChangedListener);

    /**
     * 获取被撤销操作数量
     * */
    int getRepealedOpsCount();

    /**
     * 获取图形操作数量
     * */
    int getShapeOpsCount();

    /**
     * 获取图片数量
     * */
    int getPicCount();


    /**
     * 图片数量变化监听器
     * */
    interface IOnPictureCountChanged{
        /**图片数量变化
         * @param count  当前图片数量*/
        void onPictureCountChanged(int count);
    }
    IPaintBoard setOnPictureCountChangedListener(IOnPictureCountChanged onPictureCountChangedListener);

    /**
     * 缩放比例变化监听器
     * */
    interface IOnZoomRateChangedListener{
        /**缩放比例变化
         * @param percentage  当前屏幕缩放比率百分数。如50代表50%。*/
        void onZoomRateChanged(int percentage);
    }
    IPaintBoard setOnZoomRateChangedListener(IOnZoomRateChangedListener onZoomRateChangedListener);

}
