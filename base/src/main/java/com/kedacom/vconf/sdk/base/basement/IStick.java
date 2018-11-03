package com.kedacom.vconf.sdk.base.basement;

interface IStick {

    interface IRequestStick{
        int request(String reqName, String reqPara);
    }

    interface IResponseStick{
        void setResponseFairy(IFairy.IResponseFairy responseFairy);
    }

    interface ICommandStick{
        int set(String setName, String setPara);
        int get(String getName, StringBuffer output);
        int get(String getName, String para, StringBuffer output);
    }

    interface IEmitNotificationStick{
        boolean emitNotification(String ntfName);
    }

    interface INotificationStick{
        void setNotificationFairy(IFairy.INotificationFairy notificationFairy);
    }

    void setCrystalBall(ICrystalBall crystalBall);
}
