/*
 * Copyright (c) 2018 it.kedacom.com, Inc. All rights reserved.
 */

package com.kedacom.vconf.sdk.datacollaborate.bean.transfer;
public class TDCSWbGraphsInfo {
    public String achGraphsId;        // 图元ID
    public String[] aachMatrixValue;
    public TDCSWbGraphsInfo(String picId, String[] matrixVal){
        achGraphsId = picId;
        aachMatrixValue = matrixVal;
    }
}
