package com.sissi.vconfsdk.login;

import com.sissi.vconfsdk.base.IOnNotificationListener;
import com.sissi.vconfsdk.base.Msg;
import com.sissi.vconfsdk.base.RequestAgent;
import com.sissi.vconfsdk.utils.KLog;

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

    private void processMemberStateChanged(Msg ntfId, Object ntfContent, Set<IOnNotificationListener> listeners){
        KLog.p("listener=%s, ntfId=%s, ntfContent=%s", listeners, ntfId, ntfContent);
        for (IOnNotificationListener listener : listeners) {
            listener.onNotification(ntfContent);
        }
    }

    public void subscribeMemberState(/*Set<Member> members, */IOnNotificationListener notificationListener){ // 不需要反注册方法，可以直接使用delListener()
        subscribe(Msg.MemberStateChanged, notificationListener);

        eject(Msg.MemberStateChanged);
    }

}
