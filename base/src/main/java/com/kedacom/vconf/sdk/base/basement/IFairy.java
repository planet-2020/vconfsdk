package com.kedacom.vconf.sdk.base.basement;

import android.os.Handler;

interface IFairy {

    interface ICommandFairy{
        void processSet(String setId, Object para);
        Object processGet(String getId);
        Object processGet(String getId, Object para);
        void setCommandStick(IStick.ICommandStick commandStick);
    }

    interface IRequestFairy{
        boolean processRequest(Handler requester, String reqId, int reqSn, Object... reqPara);
        void processCancelRequest(Handler requester, String reqId, int reqSn);
        void setRequestStick(IStick.IRequestStick requestStick);
    }

    interface IResponseFairy{
        boolean processResponse(String rspName, String rspBody);
    }

    interface ISubscribeFairy{
        boolean processSubscribe(Handler subscriber, String ntfId);
        void processUnsubscribe(Handler subscriber, String ntfId);
    }

    interface INotificationFairy{
        boolean processNotification(String ntfName, String ntfBody);
    }

    interface IEmitNotificationFairy{
        boolean processEmitNotification(String ntfId);
        boolean processEmitNotifications(String[] ntfIds);
        void setEmitNotificationStick(IStick.IEmitNotificationStick emitNotificationStick);
    }

}
