package com.kedacom.vconf.sdk.datacollaborate.bean;

import com.kedacom.vconf.sdk.base.MsgBeans;

public class OpRedo extends OpPaint {
    public OpRedo(){
        type = OP_REDO;
    }


    public OpRedo fromTransferObj(MsgBeans.DCPaintOp to) {
        super.fromTransferObj(to);
        return this;
    }

    public MsgBeans.DCPaintOp toTransferObj(MsgBeans.DCPaintOp to) {
        return super.toTransferObj(to);
    }

}
