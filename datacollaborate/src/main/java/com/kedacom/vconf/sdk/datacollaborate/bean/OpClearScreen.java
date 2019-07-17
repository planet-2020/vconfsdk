package com.kedacom.vconf.sdk.datacollaborate.bean;


import androidx.annotation.NonNull;

public class OpClearScreen extends OpPaint implements IRepealable{
    public OpClearScreen(){
        type = EOpType.CLEAR_SCREEN;
    }

    @NonNull
    @Override
    public String toString() {
        return String.format("OpClearScreen{%s}", super.toString());
    }
}
