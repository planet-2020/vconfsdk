package com.kedacom.vconf.sdk.datacollaborate;

import com.kedacom.vconf.sdk.datacollaborate.bean.BoardInfo;
import com.kedacom.vconf.sdk.datacollaborate.bean.PainterInfo;

import androidx.annotation.NonNull;

/**
 * 绘制工厂类。用于创建画师和画板。
 * */
public interface IPaintFactory {
    /**
     * 创建画师
     * @param painterInfo 画师信息
     * */
    IPainter createPainter(@NonNull PainterInfo painterInfo);
    /**
     * 创建画板
     * @param boardInfo 画板信息*/
    IPaintBoard createPaintBoard(@NonNull BoardInfo boardInfo);
}
