/**
 * created by gaofan_kd7331 2018-11-14
 *
 * 数据协作绘制器接口。
 *
 * */

package com.kedacom.vconf.sdk.datacollaborate;

import com.kedacom.vconf.sdk.datacollaborate.bean.PaintOp;

public interface IPainter {
    void paint(PaintOp op);
}
