package com.kedacom.vconf.sdk.datacollaborate;

import com.kedacom.vconf.sdk.datacollaborate.bean.BoardInfo;

public interface IPaintFactory {
    IPainter createPainter();
    IPaintBoard createPaintBoard(BoardInfo boardInfo);
//    IPaintView createPaintView(String boardId);
}
