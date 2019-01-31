package com.kedacom.vconf.sdk.base;

import com.kedacom.vconf.sdk.base.basement.ICrystalBall2;
import com.kedacom.vconf.sdk.base.basement.IFairy2;
import com.kedacom.vconf.sdk.base.basement.NativeInteractor2;
import com.kedacom.vconf.sdk.base.basement.NotificationFairy2;
import com.kedacom.vconf.sdk.base.basement.SessionFairy2;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CasterManager {

    private static Map<Class<?>, Caster> casters = new ConcurrentHashMap<>();

    public synchronized static <T extends Caster> T obtain(Class<T> clz){
        Caster caster = casters.get(clz);
        if (null == caster){
            try {
                Constructor<T> ctor = clz.getDeclaredConstructor(IFairy2.ISessionFairy.class, IFairy2.INotificationFairy.class);
                ctor.setAccessible(true);
//                if (ClassYouWannaCustomize == clz){ // 常见的场景是新模块使用模拟模式调试，则使用FakeCrystalBall
//                    // 定制Fairy，CrystalBall
//                    IFairy2.ISessionFairy sessionFairy = SessionFairy2.instance();
//                    IFairy2.INotificationFairy notificationFairy = NotificationFairy2.instance();
//                    ICrystalBall2 crystalBall = FakeCrystalBall.instance();
//                    sessionFairy.setCrystalBall(crystalBall);
//                    notificationFairy.setCrystalBall(crystalBall);
//                    caster = ctor.newInstance(sessionFairy, notificationFairy);
//                }else
                {
                    IFairy2.ISessionFairy sessionFairy = SessionFairy2.instance();
                    IFairy2.INotificationFairy notificationFairy = NotificationFairy2.instance();
                    ICrystalBall2 crystalBall = NativeInteractor2.instance();
                    sessionFairy.setCrystalBall(crystalBall);
                    notificationFairy.setCrystalBall(crystalBall);
                    caster = ctor.newInstance(sessionFairy, notificationFairy);
                }
                KLog.p("create caster %s", caster);
                casters.put(clz, caster);
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
                return null;
            } catch (IllegalAccessException e) {
                e.printStackTrace();
                return null;
            } catch (InvocationTargetException e) {
                e.printStackTrace();
                return null;
            } catch (InstantiationException e) {
                e.printStackTrace();
                return null;
            }
        }

        return clz.cast(caster);
    }

}
