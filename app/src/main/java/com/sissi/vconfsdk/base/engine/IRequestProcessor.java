package com.sissi.vconfsdk.base.engine;

import android.os.Handler;

public interface IRequestProcessor {
    boolean processRequest(Handler requester, String reqId, Object reqPara, int reqSn);
}
