/**
 * created by gaofan_kd7331 2018-11-14
 *
 * 数据协作绘制器接口。
 *
 * */

package com.kedacom.vconf.sdk.datacollaborate;

import android.graphics.Paint;

import com.kedacom.vconf.sdk.datacollaborate.bean.DCEraseOp;
import com.kedacom.vconf.sdk.datacollaborate.bean.DCImageOp;
import com.kedacom.vconf.sdk.datacollaborate.bean.DCLineOp;
import com.kedacom.vconf.sdk.datacollaborate.bean.DCOp;
import com.kedacom.vconf.sdk.datacollaborate.bean.DCOvalOp;
import com.kedacom.vconf.sdk.datacollaborate.bean.DCPathOp;
import com.kedacom.vconf.sdk.datacollaborate.bean.DCRectOp;

public interface IPainter {
    default void startBatchDraw(){}  // TODO 这个拿掉。接口中只保留drawXXX之类操作，顺序由sdk排好后再回调各个draw接口。
//    void drawLine(DCLineOp lineOpInfo);
//    void drawRect(DCRectOp rectOpInfo);
//    void drawOval(DCOvalOp ovalOpInfo);
//    void drawPath(DCPathOp pathOpInfo);
//    void drawImage(DCImageOp imageOpInfo);
//    void erase(DCEraseOp eraseOpInfo);
    void paint(DCOp op);  //TODO 还是拆分为具体子类比较好。
    default void finishBatchDraw(){}
}
