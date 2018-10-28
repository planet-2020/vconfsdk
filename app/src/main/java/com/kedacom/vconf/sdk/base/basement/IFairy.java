package com.kedacom.vconf.sdk.base.basement;

import android.os.Handler;

interface IFairy {

    interface ICommandFairy{
        void set(String setId, Object para);  // TODO processSet
        Object get(String getId);
        Object get(String getId, Object para);
        void setCommandStick(IStick.ICommandStick commandStick);
    }

    interface IRequestFairy{
        boolean processRequest(Handler requester, String reqId, Object reqPara, int reqSn);
        boolean processCancelRequest(Handler requester, int reqSn);
        void setRequestStick(IStick.IRequestStick requestStick);
    }

    interface IResponseFairy{
        boolean processResponse(String rspName, String rspBody);
    }

    interface ISubscribeFairy{
        boolean subscribe(Handler subscriber, String ntfId);
        void unsubscribe(Handler subscriber, String ntfId);
    }

    interface INotificationFairy{
        boolean processNotification(String ntfName, String ntfBody);
    }

    interface IEmitNotificationFairy{
        boolean emitNotification(String ntfName);
        void setEmitNotificationStick(IStick.IEmitNotificationStick emitNotificationStick);
    }

}
