package com.kedacom.vconf.sdk.amulet;

import androidx.annotation.NonNull;

import com.kedacom.vconf.sdk.utils.collection.CompatibleConcurrentLinkedDeque;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Sissi on 2019/8/2
 */
public class SimulatedDataRepository {
    private static final Map<Class<?>, CompatibleConcurrentLinkedDeque<Object>> datas = new ConcurrentHashMap<>();

    public static void put(@NonNull Object data){
        CompatibleConcurrentLinkedDeque<Object> concurrentLinkedDeque = datas.get(data.getClass());
        if (null == concurrentLinkedDeque){
            concurrentLinkedDeque = new CompatibleConcurrentLinkedDeque<>();
            datas.put(data.getClass(), concurrentLinkedDeque);
        }
        concurrentLinkedDeque.offerLast(data);
    }

    public static Object get(@NonNull Class<?> clz){
        CompatibleConcurrentLinkedDeque<Object> concurrentLinkedDeque = datas.get(clz);
        if (null == concurrentLinkedDeque){
            return null;
        }
        return concurrentLinkedDeque.pollFirst();
    }

}
