/*
 * Copyright (c) 2018 it.kedacom.com, Inc. All rights reserved.
 */

package com.kedacom.vconf.sdk.base.bean.dc;

public class TDCSConfAddr {
    public String achIp;
    public String achDomain;
    public int dwPort;

    public TDCSConfAddr(String achIp, String achDomain, int dwPort) {
        this.achIp = achIp;
        this.achDomain = achDomain;
        this.dwPort = dwPort;
    }

    @Override
    public String toString() {
        return "TDCSConfAddr{" +
                "achIp='" + achIp + '\'' +
                ", achDomain='" + achDomain + '\'' +
                ", dwPort=" + dwPort +
                '}';
    }
}
