package com.sissi.vconfsdk.login;

import com.sissi.vconfsdk.base.Msg;
import com.sissi.vconfsdk.base.Requester;
import com.sissi.vconfsdk.utils.KLog;

public class MemberStateManager extends Requester {

    private MemberStateManager(){}

    @Override
    protected void onNtf(Object listener, Msg ntfId, Object ntfContent) {
        KLog.p("listener=%s, ntfId=%s, ntfContent=%s", listener, ntfId, ntfContent);
        if (Msg.MemberStateChangedNtf.equals(ntfId)){
            if (null != listener){
                ((OnMemberStateChangedListener)listener).onMemberStateChanged();
            }
        }
    }

    public void addOnMemberStateChangedListener(OnMemberStateChangedListener onMemberStateChangedListener){ //TODO 添加和删除监听器的工作能否自动完成，兼顾activity生命周期
        subscribeNtf(onMemberStateChangedListener, Msg.MemberStateChangedNtf);

        ejectNtf(Msg.MemberStateChangedNtf);
    }

    public void delOnMemberStateChangedListener(OnMemberStateChangedListener onMemberStateChangedListener){
        unsubscribeNtf(onMemberStateChangedListener, Msg.MemberStateChangedNtf);
    }

    public interface OnMemberStateChangedListener{
        void onMemberStateChanged();
    }
}
