package com.kedacom.vconf.sdk.alirtc;

import android.app.Application;
import android.content.Context;

import androidx.annotation.NonNull;

import com.kedacom.vconf.sdk.alirtc.bean.transfer.TMtRegistCsvInfo;
import com.kedacom.vconf.sdk.amulet.Caster;
import com.kedacom.vconf.sdk.amulet.IResultListener;
import com.kedacom.vconf.sdk.common.bean.TerminalType;
import com.kedacom.vconf.sdk.common.bean.transfer.TRegResultNtf;
import com.kedacom.vconf.sdk.common.constant.EmConfProtocol;
import com.kedacom.vconf.sdk.common.constant.EmRegFailedReason;
import com.kedacom.vconf.sdk.common.type.TNetAddr;

import java.util.Map;

public class AlirtcManager extends Caster<Msg> {
    private static AlirtcManager instance = null;
    private Context context;

    private AlirtcManager(Context ctx) {
        context = ctx;
    }

    public synchronized static AlirtcManager getInstance(Application ctx) {
        if (instance == null) {
            instance = new AlirtcManager(ctx);
        }
        return instance;
    }


    @Override
    protected Map<Msg[], NtfProcessor<Msg>> subscribeNtfs() {
        return null;
    }

    /**
     * 登录
     * @param type 终端类型
     * @param version 终端软件版本
     * */
    public void login(@NonNull TerminalType type, @NonNull String version, IResultListener resultListener){
        TNetAddr addr = (TNetAddr) get(Msg.GetServerAddr);
        if (null == addr){
            reportFailed(-1, resultListener);
            return;
        }
        req(Msg.Login, new SessionProcessor<Msg>() {
            @Override
            public boolean onRsp(Msg rsp, Object rspContent, IResultListener resultListener, Msg req, Object[] reqParas) {
                TRegResultNtf result = (TRegResultNtf) rspContent;
                if (EmConfProtocol.emaliyun.ordinal() != result.MainParam.basetype){
                    return false;
                }
                if (Msg.Login == req) { // 登录
                    if (EmRegFailedReason.emRegSuccess.getValue() == result.AssParam.basetype) {
                        reportSuccess(null, resultListener);
                    } else {
                        reportFailed(-1, resultListener);
                    }
                }else{ // 注销
                    reportSuccess(null, resultListener);
                }
                return true;
            }
        }, resultListener, addr, new TMtRegistCsvInfo(type.getVal(), version, true));
    }

}
