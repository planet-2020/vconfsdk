package com.kedacom.vconf.sdk.datacollaborate;

import android.content.Context;

import com.kedacom.vconf.sdk.datacollaborate.bean.PaintBoardInfo;

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
    public IPaintBoard createPaintBoard(PaintBoardInfo boardInfo) {
        DefaultPaintBoard paintBoard = new DefaultPaintBoard(context);
        paintBoard.setBoardInfo(boardInfo);
        return paintBoard;
    }

//    @Override
//    public IPaintView createPaintView(String boardId) {
//        return new DefaultPaintView(context);
//    }

}
