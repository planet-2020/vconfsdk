package com.sissi.vconfsdk.base;

import com.sissi.vconfsdk.utils.KLog;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;

public class AgentManager {

    private static HashMap<Class<?>, RequestAgent> agents = new HashMap<>();

    public synchronized static <T extends RequestAgent> T obtain(Class<T> clz){
        RequestAgent agent = agents.get(clz);
        if (null == agent){
            try {
                Constructor<T> ctor = clz.getDeclaredConstructor((Class[])null);
                ctor.setAccessible(true);
                agent = ctor.newInstance((Object[])null);
                KLog.p(KLog.INFO,"create agent %s", agent);
                agents.put(clz, agent);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
                e.printStackTrace();
                return null;
            }
        }

        return clz.cast(agent);
    }

}
