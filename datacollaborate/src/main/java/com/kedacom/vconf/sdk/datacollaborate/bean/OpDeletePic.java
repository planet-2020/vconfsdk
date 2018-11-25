package com.kedacom.vconf.sdk.datacollaborate.bean;


import androidx.annotation.NonNull;

public class OpDeletePic extends OpPaint {
    private String[] picIds;

    public OpDeletePic(){
        type = EOpType.DELETE_PICTURE;
    }

    public OpDeletePic(String[] picIds){
        this.picIds = picIds;
        type = EOpType.DELETE_PICTURE;
    }

    @NonNull
    @Override
    public String toString() {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("(");
        for (String picId : picIds){
            stringBuffer.append(picId).append(",");
        }
        stringBuffer.append(")");
        return "{"+String.format("picIds=%s", stringBuffer.toString())+super.toString()+"}";
    }

    public String[] getPicIds() {
        return picIds;
    }

    public void setPicIds(String[] picIds) {
        this.picIds = picIds;
    }
}
