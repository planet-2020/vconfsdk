package com.kedacom.vconf.sdk.base.security;

import android.app.Application;
import android.content.Context;

import com.kedacom.vconf.sdk.amulet.Caster;
import com.kedacom.vconf.sdk.amulet.IResultListener;
import com.kedacom.vconf.sdk.common.type.BaseTypeBool;

public class SecurityManager extends Caster<Msg> {

    private static SecurityManager instance = null;
    private Context context;

    private SecurityManager(Context ctx) {
        context = ctx;
    }

    public synchronized static SecurityManager getInstance(Application ctx) {
        if (instance == null) {
            instance = new SecurityManager(ctx);
        }
        return instance;
    }


    /**
     * 设置是否开启交互式调试。
     * 若开启则可通过命令行输入命令交互式调试程序，具体操作方法请询业务组件开发。
     * */
    public void setEnableInteractiveDebug(boolean enable, IResultListener resultListener){
        BaseTypeBool baseTypeBool = new BaseTypeBool(enable);
        req(Msg.SetEnableInteractiveDebug, new SessionProcessor<Msg>() {
            @Override
            public void onRsp(Msg rsp, Object rspContent, IResultListener resultListener, Msg req, Object[] reqParas, boolean[] isConsumed) {
                boolean enabled = ((BaseTypeBool) rspContent).basetype;
                if (enabled){
                    reportSuccess(null, resultListener);
                }else{
                    reportFailed(-1, resultListener);
                }
            }
        }, resultListener, baseTypeBool);
    }

    /**
     * 交互式调试是否已开启
     * */
    public boolean hasEnabledInteractiveDebug(){
        BaseTypeBool baseTypeBool = (BaseTypeBool) get(Msg.HasEnabledInteractiveDebug);
        if (baseTypeBool==null) return false;
        return baseTypeBool.basetype;
    }

}
