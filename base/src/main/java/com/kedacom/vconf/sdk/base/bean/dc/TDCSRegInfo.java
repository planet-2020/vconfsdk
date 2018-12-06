/*
 * Copyright (c) 2018 it.kedacom.com, Inc. All rights reserved.
 */

package com.kedacom.vconf.sdk.base.bean.dc;

public class TDCSRegInfo{
    public String achIp;
    public int dwPort;
    public EmDcsType emMtType;
    public TDCSRegInfo(String ip, int port, EmDcsType type){
        achIp = ip;
        dwPort = port;
        emMtType = type;
    }
}
