package com.kedacom.vconf.sdk.common.type.transfer;

import com.kedacom.vconf.sdk.utils.json.EnumOrdinalStrategy;

@EnumOrdinalStrategy
public enum EmAudFormat {
	emAG711a,    //G711A
	emAG711u,       //G711U
	emAG722,        //G722
	emAG7231,       //G7231
	emAG728,    //G728
	emAG729,        //G729
	emAMP3,         //MP3
	emAG721,        //协议不支持， 协议还有一个g723， 编码可能不支持
	emAG7221,       //G7221
	emAG719,        //G719
	emAMpegAACLC,   //MpegAACLC
	emAMpegAACLD,   //MpegAACLD
	emAOpus,        //Opus
	emAudEnd;
}