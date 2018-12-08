package com.kedacom.vconf.sdk.datacollaborate;

import android.content.Context;

import com.kedacom.vconf.sdk.datacollaborate.bean.BoardInfo;

import androidx.annotation.NonNull;

public class DefaultPaintFactory implements IPaintFactory {

    Context context;

    public DefaultPaintFactory(Context context){
        this.context = context;
    }


    @Override
    public IPainter createPainter() {
        return new DefaultPainter(context);
    }

    @Override
    public IPaintBoard createPaintBoard(@NonNull BoardInfo boardInfo) {
        return new DefaultPaintBoard(context, boardInfo);
    }

}
