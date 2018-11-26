/**
 * created by gaofan_kd7331 2018-11-14
 * 画作者。
 * 数据协作中创作画作并发布给协作中的其他参与者。
 *
 * */

package com.kedacom.vconf.sdk.datacollaborate;

import com.kedacom.vconf.sdk.datacollaborate.bean.OpPaint;

import java.util.Set;

public interface IPainter {
    default int getMyId() {
        return 0;
    }

    /**添加画板*/
    boolean addPaintBoard(IPaintBoard paintBoard);

    /**删除画板*/
    IPaintBoard deletePaintBoard(String boardId);

    /**删除所有画板*/
    void deleteAllPaintBoards();

    /**切换画板*/
    IPaintBoard switchPaintBoard(String boardId);

    /**获取画板*/
    IPaintBoard getPaintBoard(String boardId);

    /**获取所有画板*/
    Set<IPaintBoard> getAllPaintBoards();

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

    /**绘制*/
    void paint(OpPaint op);

}
