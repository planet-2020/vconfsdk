package com.kedacom.vconf.sdk.base.startup.bean.transfer;

import com.kedacom.vconf.sdk.utils.json.EnumCustomValueStrategy;

@EnumCustomValueStrategy
public enum EmAuthType
{
	emUserNamePwdAuth_Api(0),
	emInnerPwdAuth_Api(1),
	emDynamicAuth_Api(2),
	emAuthEnd_Api(3);
	
	private int value;

	EmAuthType( int value ) {
	    this.value = value;
	}

	public int getValue() {
		return value;
	}
}
