package com.kedacom.vconf.sdk.base;


import android.support.annotation.RestrictTo;

import com.kedacom.vconf.sdk.annotation.SerializeEnumAsInt;

/**
 * Created by Sissi on 2018/9/3.
 * 枚举及常量定义。
 * （TODO 最好结合对组件层消息的理解重新定义一套适合UI层使用及理解的，而非直接照搬组件层的，然后在jni层做这两套消息之间的转换）
 */

@SuppressWarnings("unused")
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

    public enum EmDcsWbImageState
    {
        emImageStateDownloading,       // 文件正在接收，请耐心等待...
        emImageStateDownLoadFail,		// 文件接收失败，非常抱歉！！！
        emImageStateOwnerAlreadyLeave,	// 文件同步失败，发起方可能已断开连接
        emImageStateDownLoadOk,			// 文件接收成功，即将显示
        emImageStateInit,		        // 初始状态
        emImageStateConvertFail,		// 文件转换失败，可能文件错误或已损坏
        emImageStateSelfAlreadyLeave,    // 文件接收未完成，已离开会议
    }

}
