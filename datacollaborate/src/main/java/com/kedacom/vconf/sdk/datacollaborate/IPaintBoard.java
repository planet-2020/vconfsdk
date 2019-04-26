package com.kedacom.vconf.sdk.datacollaborate;

import android.graphics.Bitmap;
import android.view.View;

import com.kedacom.vconf.sdk.datacollaborate.bean.BoardInfo;

import androidx.annotation.NonNull;

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

    /**
     * 插入图片
     * @param picPath 图片绝对路径
     * */
    void insertPic(@NonNull String picPath);


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
    void snapshot(int area, int outputWidth, int outputHeight, @NonNull ISnapshotResultListener resultListener);
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


    /**
     * 获取放缩百分数
     * @return 百分数。如50代表50%。
     * */
    int getZoom();


    /**
     * 获取被撤销操作数量。
     * 对于有限制撤销步数的情形可用来判断是否应该允许用户继续撤销。
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



    /**
     * 设置画板配置
     * */
    void setConfig(@NonNull Config config);

    /**
     * 获取画板配置
     * */
    Config getConfig();

    /**
     * 画板配置
     * */
    class Config {

        public Config() {
            tool = Tool.PENCIL;
            strokeWidth = 5;
            paintColor = 0xFFFFFFFFL;
            eraserSize = 20;
            minZoomRate = 50;
            maxZoomRate = 300;
            wcRevocableOpsCountLimit = 5;
        }

        public Config(@NonNull Tool tool, int strokeWidth, long paintColor, int eraserSize, int minZoomRate, int maxZoomRate, int wcRevocableOpsCountLimit) {
            this.tool = tool;
            this.strokeWidth = strokeWidth>0?strokeWidth:5;
            this.paintColor = paintColor>0?paintColor:0xFFFFFFFFL;
            this.eraserSize = eraserSize>0?eraserSize:20;
            this.minZoomRate = minZoomRate>0?minZoomRate:50;
            this.maxZoomRate = maxZoomRate>100?maxZoomRate:100;
            this.wcRevocableOpsCountLimit = wcRevocableOpsCountLimit>0?wcRevocableOpsCountLimit:5;
        }


        public void set(@NonNull Config config){
            tool = config.tool;
            strokeWidth = config.strokeWidth;
            paintColor = config.paintColor;
            eraserSize = config.eraserSize;
            minZoomRate = config.minZoomRate;
            maxZoomRate = config.maxZoomRate;
            wcRevocableOpsCountLimit = config.wcRevocableOpsCountLimit;
        }

        /**
         * 绘制工具。
         * 在进行相关绘制操作前需先设置好对应的工具。
         * 如画任意曲线需设置PENCIL, 画直线需设置LINE。
         * */
        public enum Tool{
            PENCIL,     // 铅笔（任意曲线）
            LINE,       // 直线
            RECT,       // 矩形
            OVAL,       // 椭圆/圆
            ERASER,     // 擦除器
            RECT_ERASER, // 矩形擦除器
        }

        Tool tool;
        public void setTool(@NonNull Tool tool){
            this.tool = tool;
        }
        public Tool getTool(){
            return tool;
        }

        /**
         * 画笔粗细，单位：pixel
         * */
        int strokeWidth;
        public void setPaintStrokeWidth(int width){
            this.strokeWidth = width>0?width:5;
        }
        public int getPaintStrokeWidth(){
            return strokeWidth;
        }

        /**
         * 画笔颜色。
         * NOTE:必须为正整数，如果是字面值注意加后缀"L"，如0xFFFFFFFFL。
         * */
        long paintColor;
        public void setPaintColor(long color){
            paintColor = color>0?color:0xFFFFFFFFL;
        }
        public long getPaintColor(){
            return paintColor;
        }

        /**
         * 橡皮擦尺寸， 单位：pixel
         * */
        int eraserSize;
        public void setEraserSize(int size){
            eraserSize = size>0?size:20;
        }
        public int getEraserSize(){
            return eraserSize;
        }

        /**
         * 最小缩放率, 如50代表50%。
         * */
        int minZoomRate;
        public void setMinZoomRate(int rate){
            minZoomRate = rate>0?rate:50;
        }
        public int getMinZoomRate(){
            return minZoomRate;
        }

        /**
         * 最大缩放率,如300代表300%。
         * */
        int maxZoomRate;
        public void setMaxZoomRate(int rate){
            maxZoomRate = rate>100?rate:100;
        }
        public int getMaxZoomRate(){
            return maxZoomRate;
        }

        /**
         * 可撤销步数上限（为了对齐网呈的实现）
         * */
        int wcRevocableOpsCountLimit;
        public void setWcRevocableOpsCountLimit(int limit){
            wcRevocableOpsCountLimit = limit>0?limit:5;
        }
        public int getWcRevocableOpsCountLimit(){
            return wcRevocableOpsCountLimit;
        }

    }


}
