package com.kedacom.vconf.sdk.data;

import com.kedacom.vconf.sdk.base.IResponseListener;
import com.kedacom.vconf.sdk.base.Msg;
import com.kedacom.vconf.sdk.base.RequestAgent;

import java.util.Map;

public class MembersRemoteDataSource extends RequestAgent implements IMembersDataSource{
    @Override
    protected Map<Msg, RspProcessor> rspProcessors() {
        return null;
    }

    @Override
    protected Map<Msg, NtfProcessor> ntfProcessors() {
        return null;
    }


    @Override
    public void getMembers(IResponseListener resultListener) {

    }
}
