package com.kedacom.vconf.sdk.base;

import android.util.Log;

import com.google.gson.Gson;


final class CommandFairy implements IFairy.ICommandFairy{

    private static final String TAG = CommandFairy.class.getSimpleName();

    private static MagicBook magicBook = MagicBook.instance();

    private static Gson gson = new Gson();

    private ICrystalBall crystalBall;

    CommandFairy(){ }


    @Override
    public void set(String setName, Object... paras) {
        if (null == crystalBall){
            Log.e(TAG, "no crystalBall ");
            return;
        }

        if (!magicBook.isSet(setName)){
            Log.e(TAG, "Unknown set command"+setName);
            return;
        }

        // 检查参数合法性
        Class[] userParaTypes = magicBook.getUserParaClasses(setName);
        if (userParaTypes.length != paras.length){
            Log.e(TAG, String.format("invalid para nums for %s, expect #%s but got #%s", setName, userParaTypes.length, paras.length));
            return;
        }
        for(int i=0; i<userParaTypes.length; ++i){
            if (null != paras[i]
                    && userParaTypes[i] != paras[i].getClass()){
                Log.e(TAG, String.format("invalid para type for %s, expect %s but got %s", setName, userParaTypes[i], paras[i].getClass()));
                return;
            }
        }

        Object[] methodParas = magicBook.userPara2MethodPara(paras, magicBook.getParaClasses(setName));
        StringBuffer sb = new StringBuffer();
        for (int i=0; i<methodParas.length; ++i){
            sb.append(methodParas[i]).append(", ");
        }
        String methodName = magicBook.getMethod(setName);
        Log.d(TAG, String.format("-=->| %s(%s) paras={%s}", setName, methodName, sb));
        crystalBall.spell(magicBook.getMethodOwner(setName), methodName, methodParas, magicBook.getParaClasses(setName));
    }

    @Override
    public Object get(String getName, Object... paras) {

        if (null == crystalBall){
            Log.e(TAG, "no crystalBall");
            return null;
        }

        if (!magicBook.isGet(getName)){
            Log.e(TAG, "Unknown get command "+getName);
            return null;
        }

        // 检查参数合法性
        Class<?>[] userParaTypes = magicBook.getUserParaClasses(getName);
        if (userParaTypes.length-1 != paras.length){ // NOTE: 约定native方法的最后一个参数为出参不校验。为了方便使用用户无需传入出参以获得结果而是通过返回值获取结果。所以用户参数个数比底层方法所需参数个数少1（少了最后一个出参）
            Log.e(TAG, String.format("invalid para nums for %s, expect #%s but got #%s", getName, userParaTypes.length-1, paras.length));
            return null;
        }
        for(int i=0; i<userParaTypes.length-1; ++i){
            if (null != paras[i]
                    && userParaTypes[i] != paras[i].getClass()){
                Log.e(TAG, String.format("invalid para type for %s, expect %s but got %s", getName, userParaTypes[i], paras[i].getClass()));
                return null;
            }
        }
        Object[] paddedParas = new Object[paras.length+1];
        for (int i=0; i<paras.length; ++i){
            paddedParas[i] = paras[i];
        }
        paras = paddedParas;
        Object[] methodParas = magicBook.userPara2MethodPara(paras, magicBook.getParaClasses(getName));
        StringBuffer sb = new StringBuffer();
        for (int i=0; i<methodParas.length; ++i){
            sb.append(methodParas[i]).append(", ");
        }
        String methodName = magicBook.getMethod(getName);
        Log.d(TAG, String.format("-=-> %s(%s) paras={%s}", getName, methodName, sb));
        crystalBall.spell(magicBook.getMethodOwner(getName), methodName, methodParas, magicBook.getParaClasses(getName));
        Log.d(TAG, String.format("<-=- %s result=%s", getName, methodParas[methodParas.length-1]));

        return gson.fromJson(methodParas[methodParas.length-1].toString(), userParaTypes[userParaTypes.length-1]); // XXX NOTE: 最后一个参数为出参！下层必须遵守这个约定
    }

    @Override
    public void setCrystalBall(ICrystalBall crystalBall) {
        this.crystalBall = crystalBall;
    }

}
