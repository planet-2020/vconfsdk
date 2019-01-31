/**
 * Created by gaofan_kd7331, 2018-10-25
 */


package com.kedacom.vconf.sdk.base.basement;

import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class NativeInteractor implements ICrystalBall, INativeCallback{
    private static final String TAG = NativeInteractor.class.getSimpleName();
    private static NativeInteractor instance;
    private IYellback yb;

    private final Map<String, Method> cachedMethods = new HashMap<>();

    private NativeInteractor(){
//        setCallback(this);
    }

    public synchronized static NativeInteractor instance() {
        if (null == instance) {
            instance = new NativeInteractor();
        }
        return instance;
    }

    @Override
    public void setYellback(IYellback yb) {
        this.yb = yb;
    }

    @Override
    public int yell(String methodOwner, String methodName, Object[] para, Class[] paraType) {
        Log.d(TAG, "####methodOwner="+methodOwner+" methodName="+methodName);
        Method method = cachedMethods.get(methodName);
        if (null != method){
            try {
                method.invoke(null, para);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
            Log.d(TAG, "####call cached method: "+method);
            return 0;
        }

        try {
            Class<?> clz = Class.forName(methodOwner);
            method = clz.getDeclaredMethod(methodName, paraType);
            method.invoke(null, para);
            cachedMethods.put(methodName, method);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        Log.d(TAG, "####call method: "+method);

        return 0;
    }

//    @Override
//    public int request(String msgName, String para) {
//        return call(msgName, para);
//    }
//
//    @Override
//    public int get(String msgName, StringBuffer output) {
//        return call(msgName, output);
//    }
//
//    @Override
//    public int get(String msgName, String para, StringBuffer output) {
//        return call(msgName, para, output);
//    }


//    private native int call(String msgName, String para);  // request/set
//    private native int call(String msgName, StringBuffer output); // get
//    private native int call(String msgName, String para, StringBuffer output); // get
//
//    private native int setCallback(INativeCallback callback);

    @Override
    public void callback(String msgName, String msgBody) {
        if (null != yb){
            yb.yellback(msgName, msgBody);
        }
    }
}
