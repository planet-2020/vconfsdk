package com.sissi.vconfsdk.data;

import com.sissi.vconfsdk.base.IResultListener;
import com.sissi.vconfsdk.base.Msg;
import com.sissi.vconfsdk.base.RequestAgent;

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
    public void getMembers(IResultListener resultListener) {

    }
}
