package com.sissi.vconfsdk.contacts;

import com.sissi.vconfsdk.base.Msg;
import com.sissi.vconfsdk.base.RequestAgent;
import com.sissi.vconfsdk.data.MembersLocalDataSource;
import com.sissi.vconfsdk.data.MembersRemoteDataSource;
import com.sissi.vconfsdk.data.MembersRepository;

import java.util.Map;

public class ContactManager extends RequestAgent {

    private final MembersRepository membersRepository;

    private ContactManager(){
        membersRepository = MembersRepository.getInstance(new MembersRemoteDataSource(), new MembersLocalDataSource());
    }

    @Override
    protected Map<Msg, RspProcessor> rspProcessors() {
        return null;
    }

    @Override
    protected Map<Msg, NtfProcessor> ntfProcessors() {
        return null;
    }
}
