package com.sissi.vconfsdk.login;

import com.sissi.vconfsdk.base.Msg;
import com.sissi.vconfsdk.base.Requester;
import com.sissi.vconfsdk.utils.KLog;

public class MemberStateManager extends Requester {

    private MemberStateManager(){}

    @Override
    protected void onNtf(Msg ntfId, Object ntfContent, Object listener) {
        KLog.p("listener=%s, ntfId=%s, ntfContent=%s", listener, ntfId, ntfContent);
        if (Msg.MemberStateChangedNtf.equals(ntfId)){
            if (null != listener){
                ((OnMemberStateChangedListener)listener).onMemberStateChanged();
            }
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
