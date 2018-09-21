package com.sissi.vconfsdk.base;

import com.sissi.vconfsdk.base.amulet.Visitor;

public abstract class Requester extends Visitor{



    @Override
    protected void onRsp(Object listener, String rspId, Object rspContent) {

    }

    @Override
    protected void onNtf(Object listener, String ntfId, Object ntfContent) {

    }

    @Override
    protected void onTimeout(Object listener, String reqId) {

    }
}
