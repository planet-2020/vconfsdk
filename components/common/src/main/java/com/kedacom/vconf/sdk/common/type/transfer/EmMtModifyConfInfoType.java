
package com.kedacom.vconf.sdk.common.type.transfer;

import com.kedacom.vconf.sdk.utils.json.EnumOrdinalStrategy;

/**
 * 用户会议信息操作类型
 */
@EnumOrdinalStrategy
public enum EmMtModifyConfInfoType {
	MT_MODIFY_BEGIN,
	MT_MODIFY_CONF_NAMETYPE,
	MT_MODIFY_CONF_DURATIONTYPE,
	MT_MODIFY_CONF_PWDTYPE,
	MT_MODIFY_CONF_DUAL_MODETYPE,
	MT_MODIFY_CONF_SHOW_MEMBERALIAS,
	MT_MODIFY_CONF_OPENMODE ,	     //修改会议的openmode-暂不支持
	MT_MODIFY_CONF_DUMB ,	         //修改会议全体哑音
	MT_MODIFY_CONF_NoDisturb ,	     //修改会议是否终端免打扰（对应平台是否封闭）
	MT_MODIFY_CONF_WATERMARK ,	     //修改会议会议水印
}
