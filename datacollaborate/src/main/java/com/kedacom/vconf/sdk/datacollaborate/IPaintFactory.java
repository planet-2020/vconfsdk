package com.kedacom.vconf.sdk.datacollaborate;

public interface IPaintFactory {
    IPainter createPainter();
    IPaintBoard createPaintBoard();
    IPaintView createPaintView();
}
