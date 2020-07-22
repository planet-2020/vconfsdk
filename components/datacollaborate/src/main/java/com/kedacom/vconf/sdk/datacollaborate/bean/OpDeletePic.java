package com.kedacom.vconf.sdk.datacollaborate.bean;


import androidx.annotation.NonNull;

import java.util.Arrays;

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
        return "OpDeletePic{" +
                "picIds=" + Arrays.toString(picIds) +'\n'+
                super.toString() +
                '}';
    }

    public String[] getPicIds() {
        return picIds;
    }

    public void setPicIds(String[] picIds) {
        this.picIds = picIds;
    }
}
