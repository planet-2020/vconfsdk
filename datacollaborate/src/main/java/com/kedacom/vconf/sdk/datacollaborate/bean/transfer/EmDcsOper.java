/*
 * Copyright (c) 2018 it.kedacom.com, Inc. All rights reserved.
 */

package com.kedacom.vconf.sdk.datacollaborate.bean.transfer;

import com.google.gson.annotations.JsonAdapter;
import com.kedacom.vconf.sdk.utils.json.EnumOrdinalStrategy;
import com.kedacom.vconf.sdk.utils.json.Kson;

//@JsonAdapter(Enum2IntJsonAdapter.class)
@EnumOrdinalStrategy
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

//    static {
//        Kson.registerAdapter(EmDcsOper.class, new Enum2IntJsonAdapter<EmDcsOper>(){});
//    }
}
