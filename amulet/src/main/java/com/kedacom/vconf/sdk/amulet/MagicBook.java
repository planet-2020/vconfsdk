package com.kedacom.vconf.sdk.amulet;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;
import com.google.common.collect.Tables;
import com.kedacom.vconf.sdk.utils.json.Kson;
import com.kedacom.vconf.sdk.utils.lang.PrimitiveTypeHelper;
import com.kedacom.vconf.sdk.utils.lang.StringHelper;
import com.kedacom.vconf.sdk.utils.log.KLog;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;


@SuppressWarnings({"BooleanMethodIsAlwaysInverted", "FieldCanBeLocal", "unchecked"})
final class MagicBook {
    private static MagicBook instance;

    private Set<String> modules = Sets.newConcurrentHashSet();
    private BiMap<String, String> rspNameIdMap = Maps.synchronizedBiMap(HashBiMap.create());
    private Table<String, String, Object> reqMap = Tables.synchronizedTable(HashBasedTable.create());
    private Table<String, String, Object> rspMap = Tables.synchronizedTable(HashBasedTable.create());

    private static int REQ_TYPE_SESSION = 0; // “请求——响应”，异步。
    private static int REQ_TYPE_GET = 1; // 如获取配置。
    private static int REQ_TYPE_SET = 2; // 如设置配置。

    private static String COL_ID = "id";
    private static String COL_OWNER = "owner";
    private static String COL_PARAS = "paras";
    private static String COL_USERPARAS = "userParas";
    private static String COL_TYPE = "type";
    private static String COL_RSPSEQ = "rspSeq";
    private static String COL_TIMEOUT = "timeout";
    private static String COL_CLZ = "clz";
    private static String COL_DELAY = "delay";

    private MagicBook(){
    }

    synchronized static MagicBook instance() {
        if (null == instance) {
            instance = new MagicBook();
        }

        return instance;
    }

    synchronized void addChapter(Class<?> chapter){
        if (null == chapter) {
            KLog.p(KLog.WARN,"null == chapter");
            return;
        }

        try {
            Field field = chapter.getDeclaredField("module");
            field.setAccessible(true);
            String module = (String) field.get(null);
            if (modules.contains(module)){
                throw new IllegalArgumentException("duplicated chapter "+module);
            }
            modules.add(module);
            
            field = chapter.getDeclaredField("rspNameIdMap");
            field.setAccessible(true);
            this.rspNameIdMap.putAll((BiMap<String, String>) field.get(null));
            field = chapter.getDeclaredField("reqMap");
            field.setAccessible(true);
            reqMap.putAll((Table<String, String, Object>) field.get(null));
            field = chapter.getDeclaredField("rspMap");
            field.setAccessible(true);
            rspMap.putAll((Table<String, String, Object>) field.get(null));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }

    }


    String getReqId(String reqName){
        if (null == reqMap.row(reqName)) return null;
        return (String) reqMap.row(reqName).get(COL_ID);
    }

    List<String> getReqNames(String reqId){
        List<String> reqNameList = new ArrayList<>();
        Map<String, Object> map = reqMap.column(COL_ID);
        for (Map.Entry<String, Object> entry : map.entrySet()){
            if (entry.getValue().equals(reqId)){
                reqNameList.add(entry.getKey());
            }
        }
        return reqNameList;
    }

    String getRspId(String rspName){
        return rspNameIdMap.get(rspName);
    }

    String getRspName(String rspId){
        return rspNameIdMap.inverse().get(rspId);
    }

    boolean isRequest(String msgName){
        return reqMap.containsRow(msgName);
    }

    boolean isResponse(String msgName){
        return rspMap.containsRow(msgName);
    }

    boolean isNotification(String msgName){
        return isResponse(msgName);
    }

    boolean isSession(String reqName){
        if (null == reqMap.row(reqName)) return false;
        return REQ_TYPE_SESSION == (int)reqMap.row(reqName).get(COL_TYPE);
    }

    boolean isGet(String reqName){
        if (null == reqMap.row(reqName)) return false;
        return REQ_TYPE_GET == (int)reqMap.row(reqName).get(COL_TYPE);
    }

    boolean isSet(String reqName){
        if (null == reqMap.row(reqName)) return false;
        return REQ_TYPE_SET == (int)reqMap.row(reqName).get(COL_TYPE);
    }

    String getMethodOwner(String reqName){
        if (null == reqMap.row(reqName)) return null;
        return (String) reqMap.row(reqName).get(COL_OWNER);
    }
    String getMethod(String reqName){
        return getReqId(reqName);
    }

    Class<?>[] getParaClasses(String reqName) {
        if (null == reqMap.row(reqName)) return null;
        return (Class[]) reqMap.row(reqName).get(COL_PARAS);
    }
    Class<?>[] getUserParaClasses(String reqName){
        if (null == reqMap.row(reqName)) return null;
        return (Class[]) reqMap.row(reqName).get(COL_USERPARAS);
    }

    String[][] getRspSeqs(String reqName){
        if (null == reqMap.row(reqName)) return null;
        return (String[][]) reqMap.row(reqName).get(COL_RSPSEQ);
    }

    int getTimeout(String reqName){
        if (null == reqMap.row(reqName)) return 0;
        return (int) reqMap.row(reqName).get(COL_TIMEOUT);
    }


    Class<?> getRspClazz(String rspName){
        if (null == rspMap.row(rspName)) return null;
        return (Class<?>) rspMap.row(rspName).get(COL_CLZ);
    }

    int getRspDelay(String rspName){
        if (null == rspMap.row(rspName)) return 0;
        return (int) rspMap.row(rspName).get(COL_DELAY);
    }

    boolean checkUserPara(String reqName, Object[] userParas){
        Class[] classes = getUserParaClasses(reqName);
        if (null==classes || 0 == classes.length){
            classes = getParaClasses(reqName);// 如果没有指定用户参数类型，则用户参数类型同native方法参数类型
        }
        if (null==classes){
            KLog.p(KLog.ERROR, "reqName=%s, null==classes", reqName);
            return false;
        }

        boolean invalidParasNum =
                isSession(reqName) && userParas.length<classes.length   // 对于Session请求，参数个数不得少于注册的（可以多于）
                || isSet(reqName) && userParas.length != classes.length  // 对于Set请求，参数个数需等于注册的
                || isGet(reqName) && userParas.length != classes.length-1; // 对于Get请求，参数个数需比注册的少1（少一个传出参数，用户通过返回值获取结果而非传出参数）

        if (invalidParasNum) {
            KLog.p(KLog.ERROR, "invalid para nums for %s, expect #%s but got #%s", reqName, classes.length, userParas.length);
            return false;
        }

        int userParasNum = isGet(reqName) ? classes.length-1 : classes.length;

        for(int i=0; i<userParasNum; ++i){
            if (null == userParas[i]){
                continue;
            }
            Class reqParaClz = userParas[i].getClass();
            if (reqParaClz==classes[i] // 同类
                    || classes[i].isAssignableFrom(reqParaClz) // 实参是形参的子类
                    || classes[i].isPrimitive() && reqParaClz==PrimitiveTypeHelper.getWrapperClass(classes[i])){
                continue;
            }
            KLog.p(KLog.ERROR, "invalid para type for %s, expect %s but got %s", reqName, classes[i], reqParaClz);
            return false;
        }

        return true;

    }

    Object[] userPara2MethodPara(Object[] userParas, Class<?>[] methodParaTypes){
        if (null == methodParaTypes || userParas.length < methodParaTypes.length){
            KLog.p(KLog.ERROR, "null == methodParaTypes || userParas.length < methodParaTypes.length");
            return userParas;
        }
        Object[] methodParas = new Object[methodParaTypes.length];
        for (int i=0; i<methodParaTypes.length; ++i){
            Object userPara = userParas[i];
            Class<?> methodParaType = methodParaTypes[i];
            KLog.p(KLog.DEBUG,"userPara[%s].class=%s, methodPara[%s].class=%s", i, null==userPara? null : userPara.getClass(), i, methodParaType);
            if (null == userPara){
                methodParas[i] = methodParaType.isPrimitive() ? PrimitiveTypeHelper.getDefaultValue(methodParaType) : null;
            }else if (userPara.getClass() == methodParaType
                    || methodParaType.isAssignableFrom(userPara.getClass())){
                methodParas[i] = userPara;
            }else {
                if (StringHelper.isStringCompatible(methodParaType)) {
                    if (StringHelper.isStringCompatible(userPara.getClass())) {
                        methodParas[i] = StringHelper.convert2CompatibleType(methodParaType, userPara);
                    }else {
                        methodParas[i] = StringHelper.convert2CompatibleType(methodParaType, Kson.toJson(userPara));
                    }
                } else if (methodParaType.isPrimitive()) {
                    if (userPara.getClass() == PrimitiveTypeHelper.getWrapperClass(methodParaType)){
                        methodParas[i] = userPara;
                    }else if (userPara.getClass().isEnum() && methodParaType==int.class) {
                        methodParas[i] = Integer.valueOf(Kson.toJson(userPara));
                    }else{
                        throw new ClassCastException("trying to convert user para to native method para failed: "+userPara.getClass()+" can not cast to "+methodParaType);
                    }
                } else {
                    throw new ClassCastException("trying to convert user para to native method para failed: "+userPara.getClass()+" can not cast to "+methodParaType);
                }
            }

        }

        return methodParas;
    }

}

