package com.kedacom.vconf.sdk.base;

import com.kedacom.vconf.sdk.utils.KLog;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AgentManager {

    private static Map<Class<?>, RequestAgent> agents = new ConcurrentHashMap<>();

    public static <T extends RequestAgent> T obtain(Class<T> clz){
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
