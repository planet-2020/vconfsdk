package com.kedacom.vconf.sdk.common.constant;


import com.kedacom.vconf.sdk.utils.json.EnumCustomValueStrategy;

/**
 * Created by Sissi on 2019/10/24
 */
@EnumCustomValueStrategy
public enum EmMtResolution {
    emMtResAuto_Api             (0),   ///<自适应
    emMtSQCIF_Api               (1),   ///<SQCIF 88x72
    emMtQCIF_Api                (2),   ///<QCIF 176x144
    emMtCIF_Api                 (3),   ///<CIF 352x288
    emMt2CIF_Api                (4),   ///<2CIF 352x576
    emMt4CIF_Api                (5),   ///<4CIF 704x576
    emMt16CIF_Api               (6),   ///<16CIF 1408x1152
    emMtVGA352x240_Api          (7),   ///<352x240  对应平台SIF
    emMt2SIF_Api                (8),   ///<对应平台2SIF，具体不知道多少*多少
    emMtVGA704x480_Api          (9),   ///<704x480  对应平台4SIF
    emMtVGA640x480_Api          (10),  ///<VGA 640x480
    emMtVGA800x600_Api          (11),  ///<SVGA 800x600
    emMtVGA1024x768_Api         (12),  ///<XGA 1024x768
    emMtVWCIF_Api               (13),  ///<WCIF 512*288
    /** 仅用于终端分辨率改变 */
    emMtVSQCIF112x96_Api        (14),  ///<SQCIF(112*96)
    emMtVSQCIF96x80_Api         (15),  ///<SQCIF(96*80)
    /** 高清分辨率 */
    emMtVW4CIF_Api              (16),  ///<Wide 4CIF(1024*576)
    emMtHD720p1280x720_Api      (17),  ///<720p 1280x720
    emMtVGA1280x1024_Api        (18),  ///<SXGA 1280x1024
    emMtVGA1600x1200_Api        (19),  ///<UXGA 1600x1200
    emMtHD1080i1920x1080_Api    (20),  ///<1080i 1920x1080
    emMtHD1080p1920x1080_Api    (21),  ///<1080p 1920x1080
    emMtVGA1280x800_Api         (22),  ///<WXGA 1280x800
    emMtVGA1440x900_Api         (23),  ///<WSXGA 1440x900
    emMtVGA1280x960_Api         (24),  ///<XVGA  1280x960
    /** 非标分辨率（1080p底图）－用于终端分辨率改*/
    emMtV1440x816_Api           (25),  ///<1440×816(3/4)
    emMt1280x720_Api            (26),  ///<1280×720(2/3)
    emMtV960x544_Api            (27),  ///<960×544(1/2)
    emMtV640x368_Api            (28),  ///<640×368(1/3)
    emMtV480x272_Api            (29),  ///<480×272(1/4)
    emMt384x272_Api             (30),  ///<384×272(1/5)
    emMt640x544_Api             (31),  ///<640x544
    emMt320x272_Api             (32),  ///<320x272
    /** 非标分辨率（720p底图） －用于终端分辨率改变*/
    emMt_720_960x544_Api        (33), ///<960×544(3/4)
    emMt_720_864x480_Api        (34), ///<864×480(2/3)
    emMt_720_640x368_Api        (35), ///<640×368(1/2)
    emMt_720_432x240_Api        (36), ///<432×240(1/3)
    emMt_720_320x192_Api        (37), ///<320×192(1/4)

    /** 非标分辨率 */
    emMtVGA480x352_Api          (38),     ///<480×352, iPad专用

    emMtHD480i720x480_Api       (39),   ///<480i720x480
    emMtHD480p720x480_Api       (40),   ///<480p720x480
    emMtHD576i720x576_Api       (41),   ///<576i720x576
    emMtHD576p720x576_Api       (42),   ///<576p720x576
    emMtVGA1280x768_Api         (43),   ///<WXGA1280x768
    emMtVGA1366x768_Api         (44),   ///<WXGA1366x768
    emMtVGA1280x854_Api         (45),   ///<WSXGA1280x854
    emMtVGA1680x1050_Api        (46),   ///<WSXGA+1680x1050
    emMtVGA1920x1200_Api        (47),   ///<WUXGA1920x1200
    emMtV3840x2160_Api          (48),   ///<4Kx2K3840x2160
    emMt1280x600_Api            (49),   //1280*600
    emMt1360x768_Api            (50),  //1360*768
    emMtVRes3840x2160_Api       (51),   //3840*2160
    emMtVRes4096x2048_Api       (52),   //4096*2048
    emMtVRes4096x2160_Api       (53),   //4096*2160
    emMtVRes4096x2304_Api       (54),   //4096*2304

    emMt960x540_Api             (55),
    emMt480x270_Api             (56),
    emMt640x360_Api             (57),
    emMt320x180_Api             (58),

    emMtVResEnd_Api             (100);	///<结束值

    private final int value;

    EmMtResolution(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
