package com.kedacom.vconf.sdk.datacollaborate.bean;

import com.kedacom.vconf.sdk.base.MsgBeans;

public class OpClearScreen extends OpPaint {
    public OpClearScreen(){
        type = OP_CLEAR_SCREEN;
    }

    public OpClearScreen fromTransferObj(MsgBeans.DCPaintOp to) {
        super.fromTransferObj(to);
        return this;
    }

    public MsgBeans.DCPaintOp toTransferObj(MsgBeans.DCPaintOp to) {
        return super.toTransferObj(to);
    }

}
