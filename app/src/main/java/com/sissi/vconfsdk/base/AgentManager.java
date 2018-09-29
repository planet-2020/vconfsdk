package com.sissi.vconfsdk.base;

import com.sissi.vconfsdk.utils.KLog;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;

public class AgentManager {

    private static HashMap<Class<?>, RequestAgent> agents = new HashMap<>();
    private static HashMap<Class<?>, Integer> referCnt = new HashMap<>();

    /**获取Jni请求者。
     * @param clz 请求者类型*/
    public synchronized static RequestAgent create(Class<?> clz){
        if (!RequestAgent.class.isAssignableFrom(clz)){
            KLog.p("Invalid para!");
            return null;
        }
        RequestAgent requester = agents.get(clz);
        if (null == requester){
            try {
                Constructor ctor = clz.getDeclaredConstructor((Class[])null);
                ctor.setAccessible(true);
                requester = (RequestAgent) ctor.newInstance();
                agents.put(clz, requester);
                referCnt.put(clz, 1);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
                e.printStackTrace();
            }
        } else {
            int cnt = referCnt.get(clz);
            referCnt.put(clz, ++cnt);
        }

        return requester;
    }

    /**释放Jni请求者。
     * @param clz 请求者类型*/
    public synchronized static void free(Class<?> clz){
        int cnt = referCnt.get(clz);
        referCnt.put(clz, --cnt);
        if (cnt > 0){
            return;
        }

        KLog.p("free presenter: "+clz);
        agents.remove(clz);
    }
}
