package com.kedacom.vconf.sdk.base.settings;

import android.app.Application;
import android.content.Context;

import com.kedacom.vconf.sdk.amulet.Caster;
import com.kedacom.vconf.sdk.amulet.IResultListener;
import com.kedacom.vconf.sdk.common.type.BaseTypeBool;

public class SettingsManager extends Caster<Msg> {
    private static SettingsManager instance = null;
    private Context context;

    private SettingsManager(Context ctx) {
        context = ctx;
    }

    public synchronized static SettingsManager getInstance(Application ctx) {
        if (instance == null) {
            instance = new SettingsManager(ctx);
        }
        return instance;
    }


    /**
     * 设置是否启用telnet调试
     * */
    public void enableTelnet(boolean enable, IResultListener resultListener){
        BaseTypeBool baseTypeBool = new BaseTypeBool(enable);
        req(Msg.EnableTelnet, new SessionProcessor<Msg>() {
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
     * telnet调试是否已开启
     * */
    public boolean isTelnetEnabled(){
        BaseTypeBool baseTypeBool = (BaseTypeBool) get(Msg.IsTelnetEnabled);
        if (baseTypeBool==null) return false;
        return baseTypeBool.basetype;
    }

}
