package com.sissi.vconfsdk.login;

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

        ntfProcessorMap.put(Msg.MemberStateChangedNtf, this::processMemberStateChangedNtf);

        return ntfProcessorMap;
    }

    private void processMemberStateChangedNtf(Msg ntfId, Object ntfContent, Set<Object> listeners){
        KLog.p("listener=%s, ntfId=%s, ntfContent=%s", listeners, ntfId, ntfContent);
        for (Object listener : listeners) {
            ((OnMemberStateChangedListener) listener).onMemberStateChanged();
        }
    }


    public void addOnMemberStateChangedListener(OnMemberStateChangedListener onMemberStateChangedListener){ //TODO 添加和删除监听器的工作能否自动完成，兼顾activity生命周期
        subscribe(Msg.MemberStateChangedNtf, onMemberStateChangedListener);

        eject(Msg.MemberStateChangedNtf);
    }

    public void delOnMemberStateChangedListener(OnMemberStateChangedListener onMemberStateChangedListener){
        unsubscribe(Msg.MemberStateChangedNtf, onMemberStateChangedListener);
    }

    public interface OnMemberStateChangedListener{
        void onMemberStateChanged();
    }
}
