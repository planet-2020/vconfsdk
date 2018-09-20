package com.sissi.vconfsdk.base.amulet;

import android.os.Handler;

interface IRequestProcessor {
    boolean processRequest(Handler requester, String reqId, Object reqPara, int reqSn);
}
