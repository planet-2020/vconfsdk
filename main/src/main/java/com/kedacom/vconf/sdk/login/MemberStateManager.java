package com.kedacom.vconf.sdk.login;

import com.kedacom.vconf.sdk.base.INotificationListener;
import com.kedacom.vconf.sdk.base.Msg;
import com.kedacom.vconf.sdk.base.RequestAgent;
import com.kedacom.vconf.sdk.utils.KLog;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class MemberStateManager extends RequestAgent {

    private MemberStateManager(){}

    @Override
    protected Map<Msg, RspProcessor> rspProcessors() {
        return null;
    }

    @Override
    protected Map<Msg, NtfProcessor> ntfProcessors() {
        Map<Msg, NtfProcessor> ntfProcessorMap = new HashMap<>();

        ntfProcessorMap.put(Msg.MemberStateChanged, this::processMemberStateChanged);

        return ntfProcessorMap;
    }

    private void processMemberStateChanged(Msg ntfId, Object ntfContent, Set<INotificationListener> listeners){
        KLog.p("listener=%s, ntfId=%s, ntfContent=%s", listeners, ntfId, ntfContent);
        for (INotificationListener listener : listeners) {
            listener.onNotification(ntfContent);
        }
    }

    public void subscribeMemberState(/*Set<Member> members, */INotificationListener notificationListener){ // 不需要反注册方法，可以直接使用delListener()
        subscribe(Msg.MemberStateChanged, notificationListener);

        eject(Msg.MemberStateChanged);
    }

}
