package com.kedacom.vconf.sdk.datacollaborate.bean;


public class OpClearScreen extends OpPaint implements IRepealable{
    public OpClearScreen(){
        type = EOpType.CLEAR_SCREEN;
    }

    @Override
    public String toString() {
        return String.format("OpClearScreen{%s}", super.toString());
    }
}
