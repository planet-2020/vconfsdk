package com.kedacom.vconf.sdk.datacollaborate;

import android.content.Context;

public class DefaultPaintFactory implements IPaintFactory {

    Context context;

    public DefaultPaintFactory(Context context){
        this.context = context;
    }

    @Override
    public IPainter createPainter() {
        return new DefaultPainter();
    }

    @Override
    public IPaintBoard createPaintBoard() {
        return new DefaultPaintBoard(context);
    }

    @Override
    public IPaintView createPaintView() {
        return new DefaultPaintView(context);
    }
}
