package com.kedacom.vconf.sdk.common.type.transfer;

import java.util.List;

/**
 *  终端别名列表
 */
public class TMultMtAlias{

	// 对端别名，可以多个alias, e164
	public List<TMtAlias> arrAlias;

	// 别名个数
	public int byCnt;
}