package com.sissi.vconfsdk.base;

import com.sissi.vconfsdk.utils.KLog;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;

public class AgentManager {

    private static HashMap<Class<?>, RequestAgent> agents = new HashMap<>();
    private static HashMap<Class<?>, Integer> referCnt = new HashMap<>();

    public synchronized static RequestAgent obtain(Class<?> clz){
        if (!RequestAgent.class.isAssignableFrom(clz)){
            KLog.p("Invalid para!");
            return null;
        }
        RequestAgent agent = agents.get(clz);
        if (null == agent){
            try {
                Constructor ctor = clz.getDeclaredConstructor((Class[])null);
                ctor.setAccessible(true);
                agent = (RequestAgent) ctor.newInstance((Object[])null);
                agents.put(clz, agent);
                referCnt.put(clz, 1);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
                e.printStackTrace();
            }
        } else {
            int cnt = referCnt.get(clz);
            referCnt.put(clz, ++cnt);
        }

        return agent;
    }


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
