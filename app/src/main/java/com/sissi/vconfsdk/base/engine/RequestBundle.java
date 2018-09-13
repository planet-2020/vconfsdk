package com.sissi.vconfsdk.base.engine;

import android.os.Handler;

/**
 * Created by Sissi on 2018/9/13.
 */
class RequestBundle {
    Handler requester; // 请求者
    String reqName; // 请求消息名称
    String reqPara; // 请求参数(JSon格式)
    int reqSn; // 请求序列号
    Object[] rsps;  // 模拟响应. 仅模拟模式下有意义

    RequestBundle(Handler requester, String reqName, String reqPara, int reqSn, Object[] rsps) {
        this.requester = requester;
        this.reqName = reqName;
        this.reqPara = reqPara;
        this.reqSn = reqSn;
        this.rsps = rsps;
    }
}
