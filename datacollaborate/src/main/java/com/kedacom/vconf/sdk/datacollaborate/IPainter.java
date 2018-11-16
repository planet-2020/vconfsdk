/**
 * created by gaofan_kd7331 2018-11-14
 * 画作者。
 * 数据协作中创作画作并发布给协作中的其他参与者。
 *
 * */

package com.kedacom.vconf.sdk.datacollaborate;

import com.kedacom.vconf.sdk.datacollaborate.bean.PaintOp;

public interface IPainter {
    int getMyId();

    /**添加画板*/
    void addPaintBoard(IPaintBoard paintBoard);

    /**删除画板*/
    void deletePaintBoard(String boardId);

    /**切换画板。*/
    void switchPaintBoard(String boardId);

    /**绘制操作*/
    void paint(PaintOp op);

}
