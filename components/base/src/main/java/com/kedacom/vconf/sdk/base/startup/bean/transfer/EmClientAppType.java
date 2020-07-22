package com.kedacom.vconf.sdk.base.startup.bean.transfer;

import com.kedacom.vconf.sdk.utils.json.EnumCustomValueStrategy;

//mtc应用类型
@EnumCustomValueStrategy
public enum EmClientAppType
{
	emClientAppOsd_Api(0),
	emClientAppWebService_Api(1),
	emClientAppSkyShare_Api(2),
	emClientAppSkyRemote_Api(3),
	emClientAppSkyMTC_Api(4),
	emClientAppSkyWindows_Api(5),
	emClientAppSkyAndroid_Api(6),
	emClientAppSkyIOS_Api(7),
	emClientAppThirdParty_Api(8),
	emClientAppNexVison_Api(9),
	emClientAppMTCEnd_Api(10);
	
	private int value;

	EmClientAppType( int value ) {
	    this.value = value;
	}

	public int getValue() {
		return value;
	}
}
