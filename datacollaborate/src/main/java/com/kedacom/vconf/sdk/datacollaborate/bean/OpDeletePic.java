package com.kedacom.vconf.sdk.datacollaborate.bean;


public class OpDeletePic extends OpPaint {
    private String[] picIds;

    public OpDeletePic(){
        type = OP_DELETE_PICTURE;
    }

    public OpDeletePic(String[] picIds){
        this.picIds = picIds;
        type = OP_DELETE_PICTURE;
    }

    public String[] getPicIds() {
        return picIds;
    }

    public void setPicIds(String[] picIds) {
        this.picIds = picIds;
    }
}
