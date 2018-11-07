package com.kedacom.vconf.sdk.datacollaborate;

import com.kedacom.vconf.sdk.datacollaborate.bean.DCEraseOp;
import com.kedacom.vconf.sdk.datacollaborate.bean.DCImageOp;
import com.kedacom.vconf.sdk.datacollaborate.bean.DCLineOp;
import com.kedacom.vconf.sdk.datacollaborate.bean.DCOp;
import com.kedacom.vconf.sdk.datacollaborate.bean.DCOvalOp;
import com.kedacom.vconf.sdk.datacollaborate.bean.DCPathOp;
import com.kedacom.vconf.sdk.datacollaborate.bean.DCRectOp;

public interface IDCPainter {
    default void startBatchDraw(){}
//    void drawLine(DCLineOp lineOpInfo);
//    void drawRect(DCRectOp rectOpInfo);
//    void drawOval(DCOvalOp ovalOpInfo);
//    void drawPath(DCPathOp pathOpInfo);
//    void drawImage(DCImageOp imageOpInfo);
//    void erase(DCEraseOp eraseOpInfo);
    void draw(DCOp op);
    // TODO undo(); 对于这两个方法都需要判断sn，根据sn决定undo的哪个redo的哪个。
    // TODO redo();
    default void finishBatchDraw(){}
}
