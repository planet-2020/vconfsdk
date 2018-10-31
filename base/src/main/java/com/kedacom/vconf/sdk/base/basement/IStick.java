package com.kedacom.vconf.sdk.base.basement;

interface IStick {

    interface IRequestStick{
        int request(String reqId, String reqPara);
    }

    interface IResponseStick{
        void setResponseFairy(IFairy.IResponseFairy responseFairy);
    }

    interface ICommandStick{
        int set(String setId, String setPara);
        int get(String getId, StringBuffer output);
        int get(String getId, String para, StringBuffer output);
    }

    interface IEmitNotificationStick{
        boolean emitNotification(String ntfId);
    }

    interface INotificationStick{
        void setNotificationFairy(IFairy.INotificationFairy notificationFairy);
    }

    void setCrystalBall(ICrystalBall crystalBall);
}
