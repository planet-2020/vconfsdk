/**
 * created by gaofan_kd7331 2018-11-14
 *
 * 数据协作绘制器接口。
 *
 * */

package com.kedacom.vconf.sdk.datacollaborate;

import com.kedacom.vconf.sdk.datacollaborate.bean.PaintOp;

public interface IPainter {
    default void startBatchDraw(){}  // TODO 这个拿掉。接口中只保留drawXXX之类操作，顺序由sdk排好后再回调各个draw接口。
//    void drawLine(DrawLineOp lineOpInfo);
//    void drawRect(DrawRectOp rectOpInfo);
//    void drawOval(DrawOvalOp ovalOpInfo);
//    void drawPath(DrawPathOp pathOpInfo);
//    void drawImage(DCImageOp imageOpInfo);
//    void erase(EraseOp eraseOpInfo);
    void paint(PaintOp op);  //TODO 还是拆分为具体子类比较好。
    default void finishBatchDraw(){}
}
