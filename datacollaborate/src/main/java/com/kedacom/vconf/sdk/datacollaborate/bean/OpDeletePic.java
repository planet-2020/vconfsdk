package com.kedacom.vconf.sdk.datacollaborate.bean;

import com.kedacom.vconf.sdk.base.MsgBeans;

public class OpDeletePic extends OpPaint {
    private String[] picIds;

    public OpDeletePic(){
        type = OP_DELETE_PICTURE;
    }

    public OpDeletePic(String[] picIds){
        this.picIds = picIds;
        type = OP_DELETE_PICTURE;
    }


    public OpDeletePic fromTransferObj(MsgBeans.DCDelPicOp to) {
        super.fromTransferObj(to);
        picIds = to.picIds;
        return this;
    }

    public MsgBeans.DCDelPicOp toTransferObj(MsgBeans.DCDelPicOp to) {
        super.toTransferObj(to);
        to.picIds = picIds;
        return to;
    }

    public String[] getPicIds() {
        return picIds;
    }

    public void setPicIds(String[] picIds) {
        this.picIds = picIds;
    }
}
