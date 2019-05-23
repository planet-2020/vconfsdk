/**
 * created by gaofan_kd7331 2018-11-14
 *
 * 画者
 *
 * */

package com.kedacom.vconf.sdk.datacollaborate;

import com.kedacom.vconf.sdk.base.IResultListener;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpPaint;

import java.util.List;

import androidx.annotation.NonNull;

/**
 * 画师。
 * 负责管理画板以及在画板上绘制。
 * */
public interface IPainter {

    /**添加画板*/
    boolean addPaintBoard(@NonNull IPaintBoard paintBoard);

    /**删除画板
     * @param boardId 画板Id*/
    IPaintBoard deletePaintBoard(@NonNull String boardId);

    /**删除所有画板*/
    void deleteAllPaintBoards();

    /**切换画板
     * @param boardId 画板Id*/
    IPaintBoard switchPaintBoard(@NonNull String boardId);

    /**获取画板
     * @param boardId 画板Id*/
    IPaintBoard getPaintBoard(@NonNull String boardId);

    /**获取所有画板*/
    List<IPaintBoard> getAllPaintBoards();

    /**获取当前画板*/
    IPaintBoard getCurrentPaintBoard();

    /**获取画板数量*/
    int getPaintBoardCount();


    /**
     * 临摹者角色。
     * 临摹者只能被动接收画板绘制操作，不能主动增/删/切画板及主动在画板上绘制。
     * */
    int ROLE_COPIER = 1;
    /**
     * 创作者角色。
     * 创作者既能临摹亦能主动增/删/切画板及主动在画板上绘制。
     * */
    int ROLE_AUTHOR = 2;
    /**
     * 设置角色
     * */
    void setRole(int role);


    /**开始绘制*/
    void start();

    /**暂停绘制*/
    void pause();

    /**继续绘制*/
    void resume();

    /**结束绘制并销毁painter*/
    void destroy();

    /**绘制
     * @param op 绘制操作*/
    void paint(@NonNull OpPaint op);



    /**
     * 画板状态变化监听器。
     * */
    interface IOnBoardStateChangedListener{

        /**
         * 生成了绘制操作。（主动绘制）
         * @param op 绘制操作
         * @param publishResultListener 发布结果监听器。（生成的绘制操作可发布给其他与会方）
         * NOTE: 若画师角色为临摹者{@link #ROLE_COPIER}则该回调不会被触发
         * */
        default void onPaintOpGenerated(String boardId, OpPaint op, IResultListener publishResultListener){}

        /**
         * 画板状态发生了改变。
         * 此回调是下面所有回调的先驱，方便用户做一些公共处理。
         * 如：用户可根据该回调决定是否需要重新“快照”{@link IPaintBoard#snapshot(int, int, int, IPaintBoard.ISnapshotResultListener)}。
         * */
        default void onChanged(String boardId){}

        /**图片数量变化
         * @param count  当前图片数量*/
        default void onPictureCountChanged(String boardId, int count){}
        /**缩放比例变化
         * @param percentage  当前屏幕缩放比率百分数。如50代表50%。*/
        default void onZoomRateChanged(String boardId, int percentage){}
        /**
         * 可撤销状态变化。
         * NOTE: 该接口和{@link #onWcRevocableStateChanged(String, int, int)} 二选一，不要同时实现。
         * @param repealedOpsCount 已被撤销操作数量
         * @param remnantOpsCount 剩下的可撤销操作数量。
         *                        如画了10条线撤销了2条则repealedOpsCount=2，remnantOpsCount=8。
         *                        然后又画了一条则repealedOpsCount=0（新的可撤销操作会导致已撤销操作清空），remnantOpsCount=9
         *                        NOTE: 此处的可撤销数量是具体需求无关的，“可撤销”指示的是操作类型，如画线画圆等操作是可撤销的而插入图片放缩等是不可撤销的。
         * */
        default void onRepealableStateChanged(String boardId, int repealedOpsCount, int remnantOpsCount){}
        /** 可撤销状态变化（对齐网呈实现）。
         * @param revocableOpsCount 可撤销操作数量
         * @param restorableOpsCount 可恢复操作数量
         * NOTE: 该接口和{@link #onRepealableStateChanged(String, int, int)}二选一，不要同时实现。
         * 该接口的行为模仿了某些软件如PS的“保存历史记录条数X”功能，
         * 设置撤销步数上限为5，然后绘制10笔，撤销2笔，再画一笔，则revocableOpsCount=4, restorableOpsCount=0，作为对比
         * {@link #onRepealableStateChanged(String, int, int)}将返回repealedOpsCount=0, remnantOpsCount=9
         * */
        default void onWcRevocableStateChanged(String boardId, int revocableOpsCount, int restorableOpsCount){}
        /**
         * 画板内容为空状态变化（即画板内容从有到无或从无到有）。
         * 画板内容包括图形和图片。
         * 该方法触发的场景包括：
         * 1、最后一笔图形被撤销且没有图片，bEmptied=true；
         * 2、最后一张图片被删除且没有图形，bEmptied=true；
         * 3、清屏且没有图片，bEmptied=true；
         * 4、上述123或画板刚创建情形下，第一笔图形绘制或第一张图片插入，bEmptied=false；
         * NOTE:
         * 1、新建的画板为空（{@link IPaintBoard#isEmpty()}返回true），但不会触发该方法；
         * 2、使用“擦除”功能，包括黑板擦擦除矩形擦除，将画板内容清掉的情形不会触发此方法，且{@link IPaintBoard#isEmpty()}返回false；
         *
         * @param bEmptied 内容是否空了，true表示画板内容从有到无，false表示画板内容从无到有。
         * */
        default void onEmptyStateChanged(String boardId, boolean bEmptied){}

    }
    void setOnBoardStateChangedListener(IOnBoardStateChangedListener onBoardStateChangedListener);

}
