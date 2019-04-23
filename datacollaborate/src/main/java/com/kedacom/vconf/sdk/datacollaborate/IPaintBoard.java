package com.kedacom.vconf.sdk.datacollaborate;

import android.graphics.Bitmap;
import android.view.View;

import com.kedacom.vconf.sdk.datacollaborate.bean.BoardInfo;

/**
 * 画板。
 * 负责：
 * 提供画板界面；
 * 提供工具栏相关接口如设置画直线画圆，设置颜色画笔粗细，插入图片等；
 * 处理触屏事件结合工具栏设置生成相应绘制操作；
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
    /**
     * 铅笔（任意曲线）
     * */
    int TOOL_PENCIL = 1;
    /**
     * 直线
     * */
    int TOOL_LINE = 2;
    /**
     * 矩形
     * */
    int TOOL_RECT = 3;
    /**
     * 椭圆及圆
     * */
    int TOOL_OVAL = 4;
    /**
     * 矩形擦除
     * */
    int TOOL_RECT_ERASER = 5;
    /**
     * 任意擦除
     * */
    int TOOL_ERASER = 6;

    /**
     * 设置工具。
     * 在进行相关绘制操作前需先设置好对应的工具。
     * 如画任意曲线需设置TOOL_PENCIL, 画直线需设置TOOL_LINE。
     * */
    void setTool(int style);

    /**
     * 获取当前使用的工具
     * */
    int getTool();

    /**
     * 设置画笔粗细。
     * @param width 粗细。单位：pixel
     * */
    void setPaintStrokeWidth(int width);

    /**
     * 获取当前画笔粗细
     * */
    int getPaintStrokeWidth();

    /**
     * 设置画笔颜色。
     * @param color 颜色值。NOTE:必须为正整数，如果是字面值注意加后缀"L"，如0xFFFFFFFFL。
     * */
    void setPaintColor(long color);

    /**
     * 获取当前画笔颜色
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
     * 插入图片
     * @param picPath 图片绝对路径
     * */
    void insertPic(String picPath);


    // 快照区域
    /**
     * 所有区域，包括画板窗口内和窗口外（可见部分和不可见部分）。
     * */
    int AREA_ALL = 0;
    /**
     * 仅画板窗口内
     * */
    int AREA_WINDOW = 1;
//    /**
//     * 所有区域的图形
//     * */
//    int AREA_ALL_SHAPE = 2;
//    /**
//     * 所有区域的图片
//     * */
//    int AREA_ALL_PIC = 3;

    /**
     * 快照。
     * @param area 区域{@link #AREA_ALL},{@link #AREA_WINDOW}。
     * @param outputWidth 生成的图片的宽
     * @param outputHeight 生成的图片的高
     * @param resultListener 结果监听器。
     * */
    void snapshot(int area, int outputWidth, int outputHeight, ISnapshotResultListener resultListener);
    interface ISnapshotResultListener{
        void onResult(Bitmap bitmap);
    }


    /**
     * 撤销。
     * */
    void undo();

    /**
     * 恢复被撤销的操作（与撤销互为正反操作）
     * */
    void redo();

    /**
     * 清屏
     * */
    void clearScreen();

//    /**
//     * 放缩
//     * @param percentage 百分数。如50代表50%。
    // SEALED 目前仅支持触摸交互方式，放缩都是画板内部通过感应用户触摸事件做掉了。
    // 该接口需界面存在放缩按钮的场景下才有用（鼠标操作模式下会有这样的按钮）
//     * */
//    void zoom(int percentage);

    /**
     * 获取放缩百分数
     * @return 百分数。如50代表50%。
     * */
    int getZoom();

    /**
     * 设置最小缩放率
     * @param rate 缩放率。如50代表50%。
     * */
    void setMinZoomRate(int rate);

    /**
     * 获取最小缩放率
     * @return  缩放率。如50代表50%。
     * */
    int getMinZoomRate();

    /**
     * 设置最大缩放率
     * @param rate 缩放率。如300代表300%。
     * */
    void setMaxZoomRate(int rate);

    /**
     * 获取最大缩放率
     * @@return  缩放率。如300代表300%。
     * */
    int getMaxZoomRate();

    /**
     * 设置可撤销步数上限（为了对齐网呈的实现）
     * */
    void setWcRevocableOpsCountLimit(int limit);
    /**
     * 获取可撤销步数上限（对齐网呈的实现）
     * */
    int getWcRevocableOpsCountLimit();


    /**
     * 获取被撤销操作数量。
     * 对于有限制撤销步数的情形可用来判断是否应该允许用户继续撤销。
     * NOTE: 撤销数量会在画板上新添可撤销操作时清零。
     * */
    int getRepealedOpsCount();

    /**
     * 获取图形操作数量
     * 包括：
     * 诸如画线、画圆之类的操作；
     * 清屏、擦除操作；
     * 不包括：
     * 拖动、放缩、旋转、撤销、恢复。
     * */
    int getShapeOpsCount();

    /**
     * 获取图片数量
     * */
    int getPicCount();

    /**
     * 画板内容是否为空。
     * NOTE: “空”的必要条件是视觉上画板没有内容，但是使用“擦除”而非清屏操作清掉画板的内容并不会被判定为画板为空。
     * */
    boolean isEmpty();

    /**
     * 是否清屏状态。
     * 清屏状态不代表画板内容为空，目前清屏只针对图形操作，清屏状态只表示画板上所有图形操作已被清掉。
     * */
    boolean isClear();

}
