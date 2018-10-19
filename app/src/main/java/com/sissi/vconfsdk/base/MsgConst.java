package com.sissi.vconfsdk.base;


import android.support.annotation.RestrictTo;

import com.sissi.vconfsdk.annotation.SerializeEnumAsInt;

@RestrictTo(RestrictTo.Scope.LIBRARY)
@SerializeEnumAsInt
public final class MsgConst {

    public enum SetType {
        Phone,
        Pad,
        Tv,
    }

    public enum Color {
        Red,
        Green,
    }

    public enum EmDcsConfMode{
        /** 关闭数据协作 */
        emConfModeStop,
        /** 主席控制 */
        emConfModeManage,
        /** 自动协作 */
        emConfModeAuto
    }

    public enum EmDcsConfType{
        /** 点对点 */
        emConfTypeP2P,
        /** 多点 */
        emConfTypeMCC
    }

    public enum EmDcsWbMode {
        emWbModeWB,     // 空白白板模式（非文档模式）
        emWBModeDOC     // 文档
    }

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
        emWbPitchPicDel
    }

    public enum EmDcsType {
        // @formatter:off
        /**
         * 未知
         */
        emTypeUnknown,
        /**
         * 致玲
         */
        emTypeTrueLink,
        /**
         * 手机-IOS
         */
        emTypeTrueTouchPhoneIOS,
        /**
         * 平板-IOS
         */
        emTypeTrueTouchPadIOS,
        /**
         * 手机-android
         */
        emTypeTrueTouchPhoneAndroid,
        /**
         * 平板-android
         */
        emTypeTrueTouchPadAndroid,
        /**
         * 硬终端
         */
        emTypeTrueSens,
        /**
         * imix
         */
        emTypeIMIX,
        /**
         * 第三方终端
         */
        emTypeThirdPartyTer,
//        /**
//         * 无效的终端型号
//         */
//        emTypeButt(255);
    }


}
