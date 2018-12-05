package com.kedacom.vconf.sdk.base.basement;

interface IStick {

    interface IRequestStick{
        int request(String reqName, Object... reqPara);
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
        boolean emit(String ntfName);
        boolean emit(String[] ntfNames);
    }

    interface INotificationStick{
        void setNotificationFairy(IFairy.INotificationFairy notificationFairy);
    }

    void setCrystalBall(ICrystalBall crystalBall);
}
