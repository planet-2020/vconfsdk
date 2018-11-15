/**
 * created by gaofan_kd7331 2018-11-14
 *
 * 数据协作绘制器接口。
 *
 * */

package com.kedacom.vconf.sdk.datacollaborate;

import com.kedacom.vconf.sdk.datacollaborate.bean.PaintOp;

public interface IPainter {
    /**切换画板。
     * NOTE: 新建画板视为切换画板的特殊情形。
     * */
    void switchPaintBoard(String boardId);  // XXX 当使用DefaultPainter时，client怎么感知画板切换以及删除？client需要及时获取当前画板以切换界面。

    /**删除画板*/
    void deletePaintBoard(String boardId);

    /**绘制操作*/
    void paint(PaintOp op);
}
