/**
 * created by gaofan_kd7331 2018-11-14
 * 画作者。
 * 数据协作中创作画作并发布给协作中的其他参与者。
 *
 * */

package com.kedacom.vconf.sdk.datacollaborate;

import com.kedacom.vconf.sdk.datacollaborate.bean.OpPaint;

public interface IPainter {
    default int getMyId() {
        return 0;
    }

    /**添加画板*/
    void addPaintBoard(IPaintBoard paintBoard);

    /**删除画板*/
    IPaintBoard deletePaintBoard(String boardId);

    /**切换画板*/
    IPaintBoard switchPaintBoard(String boardId);

    /**获取画板*/
    IPaintBoard getPaintBoard(String boardId);

    /**获取当前画板*/
    IPaintBoard getCurrentPaintBoard();

    /**绘制*/
    void paint(OpPaint op);

}
