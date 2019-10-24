package com.kedacom.vconf.sdk.common.constant;

/**
 * Created by Sissi on 2019/10/24
 */
public enum EmMtResolution {
    emMtResAuto_Api             ,   ///<自适应
    emMtSQCIF_Api               ,   ///<SQCIF 88x72
    emMtQCIF_Api                ,   ///<QCIF 176x144
    emMtCIF_Api                 ,   ///<CIF 352x288
    emMt2CIF_Api                ,   ///<2CIF 352x576
    emMt4CIF_Api                ,   ///<4CIF 704x576
    emMt16CIF_Api               ,   ///<16CIF 1408x1152


    emMtVGA352x240_Api          ,   ///<352x240  对应平台SIF
    emMt2SIF_Api                ,   ///<对应平台2SIF，具体不知道多少*多少
    emMtVGA704x480_Api          ,   ///<704x480  对应平台4SIF
    emMtVGA640x480_Api          ,  ///<VGA 640x480
    emMtVGA800x600_Api          ,  ///<SVGA 800x600
    emMtVGA1024x768_Api         ,  ///<XGA 1024x768
    emMtVWCIF_Api               ,  ///<WCIF 512*288
    /** 仅用于终端分辨率改变 */
    emMtVSQCIF112x96_Api        ,  ///<SQCIF(112*96)
    emMtVSQCIF96x80_Api         ,  ///<SQCIF(96*80)
    /** 高清分辨率 */
    emMtVW4CIF_Api              ,  ///<Wide 4CIF(1024*576)
    emMtHD720p1280x720_Api      ,  ///<720p 1280x720
    emMtVGA1280x1024_Api        ,  ///<SXGA 1280x1024
    emMtVGA1600x1200_Api        ,  ///<UXGA 1600x1200
    emMtHD1080i1920x1080_Api    ,  ///<1080i 1920x1080
    emMtHD1080p1920x1080_Api    ,  ///<1080p 1920x1080
    emMtVGA1280x800_Api         ,  ///<WXGA 1280x800
    emMtVGA1440x900_Api         ,  ///<WSXGA 1440x900
    emMtVGA1280x960_Api         ,  ///<XVGA  1280x960
    /** 非标分辨率（1080p底图）－用于终端分辨率改*/
    emMtV1440x816_Api           ,  ///<1440×816(3/4)
    emMt1280x720_Api            ,  ///<1280×720(2/3)
    emMtV960x544_Api            ,  ///<960×544(1/2)
    emMtV640x368_Api            ,  ///<640×368(1/3)
    emMtV480x272_Api            ,  ///<480×272(1/4)
    emMt384x272_Api             ,  ///<384×272(1/5)
    emMt640x544_Api             ,  ///<640x544
    emMt320x272_Api             ,  ///<320x272
    /** 非标分辨率（720p底图） －用于终端分辨率改变*/
    emMt_720_960x544_Api        , ///<960×544(3/4)
    emMt_720_864x480_Api        , ///<864×480(2/3)
    emMt_720_640x368_Api        , ///<640×368(1/2)
    emMt_720_432x240_Api        , ///<432×240(1/3)
    emMt_720_320x192_Api        , ///<320×192(1/4)

    /** 非标分辨率 */
    emMtVGA480x352_Api          ,     ///<480×352, iPad专用


    emMtHD480i720x480_Api       ,   ///<480i720x480
    emMtHD480p720x480_Api       ,   ///<480p720x480
    emMtHD576i720x576_Api       ,   ///<576i720x576
    emMtHD576p720x576_Api       ,   ///<576p720x576
    emMtVGA1280x768_Api         ,   ///<WXGA1280x768
    emMtVGA1366x768_Api         ,   ///<WXGA1366x768
    emMtVGA1280x854_Api         ,   ///<WSXGA1280x854
    emMtVGA1680x1050_Api        ,   ///<WSXGA+1680x1050
    emMtVGA1920x1200_Api        ,   ///<WUXGA1920x1200
    emMtV3840x2160_Api          ,   ///<4Kx2K3840x2160
    emMt1280x600_Api            ,   //1280*600
    emMt1360x768_Api            ,  //1360*768
    emMtVRes3840x2160_Api       ,   //3840*2160
    emMtVRes4096x2048_Api       ,   //4096*2048
    emMtVRes4096x2160_Api       ,   //4096*2160
    emMtVRes4096x2304_Api       ,   //4096*2304

    emMtVResEnd_Api             ,	///<结束值
}
