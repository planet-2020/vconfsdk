package com.kedacom.vconf.sdk.webrtc;

import com.kedacom.vconf.sdk.common.constant.EmMtAliasType;
import com.kedacom.vconf.sdk.common.type.vconf.TMtAlias;
import com.kedacom.vconf.sdk.common.type.vconf.TMtCallLinkSate;
import com.kedacom.vconf.sdk.webrtc.bean.MakeCallResult;

/**
 * Created by Sissi on 2019/11/14
 */
final class ToDoConverter {

    public static MakeCallResult fromTransferObj(TMtCallLinkSate tMtCallLinkSate) {
        String e164=null, alias=null, email=null;
        int callBitRate = tMtCallLinkSate.dwCallRate;
        for (TMtAlias tMtAlias : tMtCallLinkSate.tPeerAlias.arrAlias){
            if (EmMtAliasType.emAliasE164 == tMtAlias.emAliasType){
                e164 = tMtAlias.achAlias;
            }else if (EmMtAliasType.emAliasH323 == tMtAlias.emAliasType){
                alias = tMtAlias.achAlias;
            }else if (EmMtAliasType.emAliasEmail == tMtAlias.emAliasType){
                email = tMtAlias.achAlias;
            }
            if (null!=e164 && null!=alias && null!=email){
                break;
            }
        }

        return new MakeCallResult(e164, alias, email, callBitRate);
    }

}
