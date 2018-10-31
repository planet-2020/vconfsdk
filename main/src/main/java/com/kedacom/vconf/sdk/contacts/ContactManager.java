package com.kedacom.vconf.sdk.contacts;

import com.kedacom.vconf.sdk.base.Msg;
import com.kedacom.vconf.sdk.base.RequestAgent;
import com.kedacom.vconf.sdk.data.MembersLocalDataSource;
import com.kedacom.vconf.sdk.data.MembersRemoteDataSource;
import com.kedacom.vconf.sdk.data.MembersRepository;

import java.util.Map;

public class ContactManager {

    private final MembersRepository membersRepository;

    private ContactManager(){
        membersRepository = MembersRepository.getInstance(new MembersRemoteDataSource(), new MembersLocalDataSource());
    }

}
