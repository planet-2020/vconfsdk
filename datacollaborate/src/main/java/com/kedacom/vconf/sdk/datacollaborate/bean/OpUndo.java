package com.kedacom.vconf.sdk.datacollaborate.bean;

import com.kedacom.vconf.sdk.base.MsgBeans;

public class OpUndo extends OpPaint {
    public OpUndo(){
        type = OP_UNDO;
    }


    public OpUndo fromTransferObj(MsgBeans.DCPaintOp to) {
        super.fromTransferObj(to);
        return this;
    }

    public MsgBeans.DCPaintOp toTransferObj(MsgBeans.DCPaintOp to) {
        return super.toTransferObj(to);
    }
}
