package com.sissi.vconfsdk.base.engine;

/**
 * Created by Sissi on 2018/9/14.
 */

interface IResponseProcessor {
    boolean processResponse(String rspName, Object rspContent);
}
