/**
 * created by gaofan_kd7331 2018-11-14
 *
 * 画者
 *
 * */

package com.kedacom.vconf.sdk.datacollaborate;

import com.kedacom.vconf.sdk.datacollaborate.bean.OpPaint;

import java.util.List;

/**
 * 画师。
 * 负责管理画板以及在画板上绘制。
 * */
public interface IPainter {

    /**添加画板*/
    boolean addPaintBoard(IPaintBoard paintBoard);

    /**删除画板
     * @param boardId 画板Id*/
    IPaintBoard deletePaintBoard(String boardId);

    /**删除所有画板*/
    void deleteAllPaintBoards();

    /**切换画板
     * @param boardId 画板Id*/
    IPaintBoard switchPaintBoard(String boardId);

    /**获取画板
     * @param boardId 画板Id*/
    IPaintBoard getPaintBoard(String boardId);

    /**获取所有画板*/
    List<IPaintBoard> getAllPaintBoards();

    /**获取当前画板*/
    IPaintBoard getCurrentPaintBoard();

    /**开始绘制*/
    default void start(){}

    /**暂停绘制*/
    default void pause(){}

    /**继续绘制*/
    default void resume(){}

    /**停止绘制*/
    default void stop(){}

    /**绘制
     * @param op 绘制操作信息*/
    void paint(OpPaint op);

}
