package com.kedacom.vconf.sdk.base.amulet;

/**
 * Created by Sissi on 2018/9/14.
 */

interface IResponseProcessor {
    boolean processResponse(String rspName, String rspBody);
}
