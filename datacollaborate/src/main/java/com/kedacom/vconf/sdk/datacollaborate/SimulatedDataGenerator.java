package com.kedacom.vconf.sdk.datacollaborate;

import com.kedacom.vconf.sdk.common.type.SimulatedError;
import com.kedacom.vconf.sdk.common.type.SimulatedTimeout;
import com.kedacom.vconf.sdk.datacollaborate.bean.*;
import com.kedacom.vconf.sdk.datacollaborate.bean.transfer.*;

/**
 * Created by Sissi on 2019/8/2
 *
 * 模拟数据生成器，仅用于模拟模式。
 *
 * NOTE: 该类跟Manager的方法名及请求流程强相关，若有变化需联动。
 */
final class SimulatedDataGenerator {

    static Object[]  generate(String key, Object src){
        switch (key){
            case "login":
                TDCSSrvState srvState = new TDCSSrvState(EmServerState.emSrvIdle, false, "");
                TDCSSvrAddr svrAddr = new TDCSSvrAddr(10000, 5000);
                TDCSConnectResult connectResult = new TDCSConnectResult(true);
                TDCSResult result;
                if (src instanceof SimulatedError){
                    result = new TDCSResult(false, ((SimulatedError)src).errorCode, "");
                }else if (src instanceof SimulatedTimeout){
                    result = null;
                }else{
                    result = new TDCSResult(true, 0, "");
                }
                return new Object[]{srvState, svrAddr, connectResult, result};

            case "logout":
                if (src instanceof SimulatedError){
                    result = new TDCSResult(false, ((SimulatedError)src).errorCode, "");
                    return new Object[]{result};
                }else if (src instanceof SimulatedTimeout){
                    return null;
                }else{
                    result = new TDCSResult(true, 0, "");
                    connectResult = new TDCSConnectResult(false);
                    return new Object[]{result, connectResult};
                }

            case "startCollaborate":
                connectResult = new TDCSConnectResult(true);
                TDCSCreateConfResult createConfResult;
                if (src instanceof SimulatedError){
                    createConfResult = new TDCSCreateConfResult(false, ((SimulatedError)src).errorCode);
                    return new Object[]{connectResult, createConfResult};
                }else if (src instanceof SimulatedTimeout){
                    return null;
                }else{
                    createConfResult = ToDoConverter.toTDCSCreateConfResult((DcConfInfo)src);
                    return new Object[]{connectResult, createConfResult};
                }

            default:
                return null;
        }

    }

}
