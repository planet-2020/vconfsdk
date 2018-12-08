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
    /** 临摹者。
     * 临摹者只能被动接收绘制操作并原样绘制到画板上，不能主动在画板上创作。*/
    int ROLE_COPYER = 0;
    /** 创作者。
     * 创作者既能接收绘制操作并原样绘制到画板上，也能主动在画板上创作。*/
    int ROLE_AUTHOR = 1;

    int ROLE_UNKNOWN = 9;


    /**设置角色*/
    void setRole(String boardId, int role);

    /**获取角色*/
    int getRole(String boardId);

    /**添加画板*/
    boolean addPaintBoard(IPaintBoard paintBoard, int role);

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
