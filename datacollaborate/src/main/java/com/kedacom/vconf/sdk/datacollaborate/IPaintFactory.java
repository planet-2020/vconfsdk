package com.kedacom.vconf.sdk.datacollaborate;

import com.kedacom.vconf.sdk.datacollaborate.bean.BoardInfo;

import androidx.annotation.NonNull;

public interface IPaintFactory {
    IPainter createPainter();
    IPaintBoard createPaintBoard(@NonNull BoardInfo boardInfo);
}
