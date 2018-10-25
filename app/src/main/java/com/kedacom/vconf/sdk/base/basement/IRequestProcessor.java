package com.kedacom.vconf.sdk.base.basement;

import android.os.Handler;

interface IRequestProcessor {
    boolean processRequest(Handler requester, String reqId, Object reqPara, int reqSn);
    boolean processCancelRequest(Handler requester, int reqSn);
}
