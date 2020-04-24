/*
 * Copyright (c) 2018 it.kedacom.com, Inc. All rights reserved.
 */

package com.kedacom.vconf.sdk.datacollaborate.bean.transfer;

import com.google.gson.annotations.JsonAdapter;
import com.kedacom.vconf.sdk.utils.json.Enum2IntJsonAdapter;
import com.kedacom.vconf.sdk.utils.json.Kson;

//@JsonAdapter(Enum2IntJsonAdapter.class)
public enum EmDcsOper {
    emWbLineOperInfo,       // TDCSWbLineOperInfo
    emWbCircleOperInfo,     // TDCSWbCircleOperInfo
    emWbRectangleOperInfo,  // TDCSWbRectangleOperInfo
    emWbPencilOperInfo,     // TDCSWbPencilOperInfo
    emWbColorPenOperInfo,   // TDCSWbColorPenOperInfo
    @Deprecated // 由emWbInsertPic、emWbPitchPicZoom、emWbPitchPicRotate、emWbPitchPicDrag、emWbPitchPicDel取代
    emWbImageOperInfo,      // TDCSWbImageOperInfo
    emWbAddSubPageInfo,     // TDCSWbAddSubPageInfo
    emWbEraseOperInfo,      // TDCSWbEraseOperInfo
    @Deprecated // 由emWbFullScreen取代
    emWbZoomInfo,           // TDCSWbZoomInfo
    emWbUndo,
    emWbRedo,
    emWbRotateLeft,
    emWbRotateRight,
    emWbClearScreen,
    @Deprecated // 由emWbFullScreen取代
    emWbScrollScreen,
    emWbFullScreen,
    @Deprecated // 由emWbFullScreen取代
    emWb100ProportionScreen,
    emWbReginErase,
    emWbInsertPic,
    emWbPitchPicZoom,
    emWbPitchPicRotate,
    emWbPitchPicDrag,
    emWbPitchPicDel;

    static {
        // 通过JsonAdapter注解的方式注册适配器更加便捷，但该注解是Gson2.3引入的，有的用户可能必须使用老版Gson，故回退使用老方式注册。
        Kson.registerAdapter(EmDcsOper.class, new Enum2IntJsonAdapter());
    }
}
