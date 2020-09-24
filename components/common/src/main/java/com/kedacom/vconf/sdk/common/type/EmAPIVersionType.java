package com.kedacom.vconf.sdk.common.type;

import com.kedacom.vconf.sdk.utils.json.EnumOrdinalStrategy;

//获取API版本信息类型
@EnumOrdinalStrategy
public enum EmAPIVersionType
{
    emVcAPIVersion_Api    ,    //会控API
	emMcAPIVersion_Api    ,    //会管API
	emEqpAPIVersion_Api   ,    //电视墙API
	emSystemAPIVersion_Api,    //登录认证API
	emAmcAPIVersion_Api   ,    //获取账号API
	emVrsAPIVersion_Api   ,    //VRS API
}
