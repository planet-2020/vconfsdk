/*
 * Copyright (c) 2018 it.kedacom.com, Inc. All rights reserved.
 */

package com.kedacom.vconf.sdk.base.bean.dc;
public class TDCSWbEntity {
    public String achEntityId;  // 现在使用GUID来填写
    public boolean bLock;
    public TDCSWbEntity(String achEntityId) {
        this(achEntityId, false);
    }
    public TDCSWbEntity(String achEntityId, boolean bLock) {
        this.achEntityId = achEntityId;
        this.bLock = bLock;
    }
}
