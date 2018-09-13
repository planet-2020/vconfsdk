package com.sissi.vconfsdk.frame;

import android.util.Log;

import com.sissi.annotation.Consumer;
import com.sissi.annotation.Message;

import java.util.Map;
import java.util.Set;

/**
 * 消息注册表
 *
 * Created by Sissi on 1/9/2017.
 */

@Consumer(Message.class)
final class MessageRegister {
    private static MessageRegister instance;

    private Set<String> reqSet;

    private Set<String> rspSet;

    private Map<String, Class> reqParaMap;

    private Map<String, String[][]> reqRspsMap; // 请求——响应序列

    private Map<String, Integer> reqTimeoutMap; // 请求——超时时限. 单位: 秒.

    private Map<String, Class> rspClazzMap; // 响应——响应类类型
    
//    private final EnumMap<EmReq, Class<?>> confReqClazzs;   // 获取配置请求——配置类类型
//
//    private final EnumSet<EmRsp> whiteList; // 白名单
//    private final EnumSet<EmRsp> blackList; // 黑名单

    private MessageRegister(){
        reqSet = Message$$Generated.reqSet;
        rspSet = Message$$Generated.rspSet;
        reqParaMap = Message$$Generated.reqParaMap;
        reqRspsMap = Message$$Generated.reqRspsMap;
        reqTimeoutMap = Message$$Generated.reqTimeoutMap;
        rspClazzMap = Message$$Generated.rspClazzMap;
    }

    synchronized static MessageRegister instance() {
        if (null == instance) {
            instance = new MessageRegister();
        }

        return instance;
    }

    int getTimeout(String req){
        Integer timeoutVal = reqTimeoutMap.get(req);
        if (null == timeoutVal) {
            return 0;
        }

        return timeoutVal;
    }

    String[][] getRsps(String req){
        return reqRspsMap.get(req);
    }

    Class<?> getRspClazz(String rsp){
        return rspClazzMap.get(rsp);
    }

//    EmRsp getRsp(String rspName){
//        try {
//            return EmRsp.valueOf(rspName);
//        }catch (IllegalArgumentException e){
//            PcTrace.p(PcTrace.WARN, "%s is not a constant in %s", rspName, EmRsp.class.getName());
//            return null;
//        }
//    }

//    Class<?> getConfClazz(EmReq req){
//        return confReqClazzs.get(req);
//    }

//    boolean isInWhiteList(EmRsp rsp){
//        return whiteList.contains(rsp);
//    }
//
//    boolean isInBlackList(EmRsp rsp){
//        return blackList.contains(rsp);
//    }


//    private void initWhiteList(){
//    }
//
//    private void initBlackList(){
//    }

//    /**注册映射关系：获取配置请求——配置对应的类*/
//    private void initConfMap(){
////        map(EmReq.GetCallCapPlusCmd, ReqRspBeans.SetCallCapPlusCmd.class);
//        map(EmReq.GetSkyShareLoginState, ReqRspBeans.SkyShareLoginState.class);
//    }
//

}

