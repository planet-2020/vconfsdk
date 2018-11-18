package com.kedacom.vconf.sdk.datacollaborate;

import com.kedacom.vconf.sdk.datacollaborate.bean.PaintBoardInfo;

public interface IPaintFactory {
    IPainter createPainter();
    IPaintBoard createPaintBoard(PaintBoardInfo boardInfo);
    IPaintView createPaintView(String boardId);
}
