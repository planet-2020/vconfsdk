package com.kedacom.vconf.sdk.base.basement;

import com.kedacom.vconf.sdk.annotation.Consumer;
import com.kedacom.vconf.sdk.annotation.Message;

import java.util.Map;
import java.util.Set;


@SuppressWarnings("BooleanMethodIsAlwaysInverted")
@Consumer(Message.class)
final class MagicBook {
    private static MagicBook instance;

    private Set<String> reqSet;

    private Set<String> rspSet;

    private Set<String> ntfSet;

    private Set<String> gets;

    private Set<String> sets;

    private Map<String, String> idNameMap; // 消息ID——消息名称

    private Map<String, String> nameIdMap; // 消息名称——消息ID

    private Map<String, Class> reqMethodOwner; // 请求——请求对应的方法所在类

    private Map<String, Class[]> reqParasMap; // 请求——请求参数对应的类

    private Map<String, String[][]> reqRspSeqsMap; // 请求——响应序列

    private Map<String, Integer> reqTimeoutMap; // 请求——超时时限. 单位: 秒.

    private Map<String, Boolean> reqExclusiveMap; // 请求——是否互斥

    private Map<String, Class> rspClazzMap; // 响应——响应对应的类

    private Map<String, Integer> rspDelayMap; // 响应——响应延时。仅用于模拟模式

    private Map<String, Class> ntfClazzMap; // 通知——通知对应的类

    private Map<String, Integer> ntfDelayMap; // 通知——通知延时。仅用于模拟模式

    private Map<String, Class> getParaClazzMap; // 获取参数——参数对应的类

    private Map<String, Class> getResultClazzMap; // 获取结果——结果对应的类

    private Map<String, Class> setParaClazzMap; // 设置参数——参数对应的类
    
//
//    private final EnumSet<EmRsp> whiteList; // 白名单
//    private final EnumSet<EmRsp> blackList; // 黑名单

    private MagicBook(){
        idNameMap = Message$$Generated.idNameMap;
        nameIdMap = Message$$Generated.nameIdMap;
        reqMethodOwner = Message$$Generated.reqMethodOwner;
        reqParasMap = Message$$Generated.reqParasMap;
        reqRspSeqsMap = Message$$Generated.reqRspsMap;
        reqTimeoutMap = Message$$Generated.reqTimeoutMap;
        reqExclusiveMap = Message$$Generated.reqExclusiveMap;
        rspClazzMap = Message$$Generated.rspClazzMap;
        rspDelayMap = Message$$Generated.rspDelayMap;
        ntfClazzMap = Message$$Generated.ntfClazzMap;
        ntfDelayMap = Message$$Generated.ntfDelayMap;
        getParaClazzMap = Message$$Generated.getParaClazzMap;
        getResultClazzMap = Message$$Generated.getResultClazzMap;
        setParaClazzMap = Message$$Generated.setParaClazzMap;

        reqSet = reqTimeoutMap.keySet();
        rspSet = rspClazzMap.keySet();
        ntfSet = ntfClazzMap.keySet();
        gets = getParaClazzMap.keySet();
        sets = setParaClazzMap.keySet();
    }

    synchronized static MagicBook instance() {
        if (null == instance) {
            instance = new MagicBook();
        }

        return instance;
    }

    String getMsgName(String msgId){
        return idNameMap.get(msgId);
    }

    String getMsgId(String msgName){
        return nameIdMap.get(msgName);
    }

    boolean isRequest(String msg){
        return reqSet.contains(msg);
    }

    boolean isResponse(String msg){
        return rspSet.contains(msg);
    }

    boolean isNotification(String msg){
        return ntfSet.contains(msg);
    }

    boolean isGet(String msg){
        return gets.contains(msg);
    }

    boolean isSet(String msg){
        return sets.contains(msg);
    }

    Class<?> getReqMethodOwner(String req){
        return reqMethodOwner.get(req);
    }

    Class[] getReqParaClasses(String req){
        return reqParasMap.get(req);
    }

    String[][] getRspSeqs(String req){
        return reqRspSeqsMap.get(req);
    }

    int getTimeout(String req){
        Integer timeoutVal = reqTimeoutMap.get(req);
        if (null == timeoutVal) {
            return 0;
        }

        return timeoutVal;
    }

    boolean isMutualExclusiveReq(String req){
        return reqExclusiveMap.get(req);
    }

    Class<?> getRspClazz(String rsp){
        return rspClazzMap.get(rsp);
    }

    Class<?> getNtfClazz(String ntf){
        return ntfClazzMap.get(ntf);
    }

    int getRspDelay(String rsp){
        return rspDelayMap.get(rsp);
    }

    int getNtfDelay(String ntf){
        return ntfDelayMap.get(ntf);
    }

    Class<?> getGetParaClazz(String get){
        return getParaClazzMap.get(get);
    }

    Class<?> getGetResultClazz(String get){
        return getResultClazzMap.get(get);
    }

    Class<?> getSetParaClazz(String set){
        return setParaClazzMap.get(set);
    }

}

