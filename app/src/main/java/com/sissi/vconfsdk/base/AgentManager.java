package com.sissi.vconfsdk.base;

import com.sissi.vconfsdk.utils.KLog;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;

public class AgentManager {

    private static HashMap<Class<?>, RequestAgent> agents = new HashMap<>();
    private static HashMap<Class<?>, Integer> referCnt = new HashMap<>();

    public synchronized static <T extends RequestAgent> T obtain(Class<T> clz){
        RequestAgent agent = agents.get(clz);
        if (null == agent){
            try {
                Constructor<T> ctor = clz.getDeclaredConstructor((Class[])null);
                ctor.setAccessible(true);
                agent = ctor.newInstance((Object[])null);
                KLog.p("create agent {%s, %s} ", clz, agent);
                agents.put(clz, agent);
                referCnt.put(clz, 1);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
                e.printStackTrace();
                return null;
            }
        } else {
            int cnt = referCnt.get(clz);
            referCnt.put(clz, ++cnt);
        }

        return clz.cast(agent);
    }

}
