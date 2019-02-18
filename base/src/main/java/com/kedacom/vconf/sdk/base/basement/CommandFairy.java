package com.kedacom.vconf.sdk.base.basement;

import android.util.Log;

import com.google.common.collect.Lists;

import java.util.List;

final class CommandFairy implements IFairy.ICommandFairy{

    private static final String TAG = CommandFairy.class.getSimpleName();

    private static CommandFairy instance;

    private JsonProcessor jsonProcessor = JsonProcessor.instance();

    private MagicBook magicBook = MagicBook.instance();

    private ICrystalBall crystalBall;

    private CommandFairy(){
    }

    synchronized static CommandFairy instance() {
        if (null == instance) {
            instance = new CommandFairy();
        }

        return instance;
    }


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

        crystalBall.spell(magicBook.getMethodOwner(setName), magicBook.getMethod(setName), methodParas, magicBook.getParaClasses(setName));
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
        if (userParaTypes.length != paras.length){
            Log.e(TAG, String.format("invalid para nums for %s, expect #%s but got #%s", getName, userParaTypes.length, paras.length));
            return null;
        }
        for(int i=0; i<userParaTypes.length-1; ++i){ // 最后一个参数为出参不校验。为了方便约定用户无需传入出参以获得结果而是通过返回值获取结果。所以用户参数个数比底层方法所需参数个数少1（少了最后一个出参）
            if (null != paras[i]
                    && userParaTypes[i] != paras[i].getClass()){
                Log.e(TAG, String.format("invalid para type for %s, expect %s but got %s", getName, userParaTypes[i], paras[i].getClass()));
                return null;
            }
        }

        Object[] methodParas = magicBook.userPara2MethodPara(paras, magicBook.getParaClasses(getName));
        List<Object> methodParas1 = Lists.newArrayList(methodParas);
        StringBuffer outPara = new StringBuffer();
        methodParas1.add(outPara);
        methodParas = methodParas1.toArray();
        crystalBall.spell(magicBook.getMethodOwner(getName), magicBook.getMethod(getName), methodParas, magicBook.getParaClasses(getName));

        return jsonProcessor.fromJson(methodParas[methodParas.length-1].toString(), userParaTypes[userParaTypes.length-1]);
    }

    @Override
    public void setCrystalBall(ICrystalBall crystalBall) {
        this.crystalBall = crystalBall;
    }

    @Override
    public ICrystalBall getCrystalBall() {
        return crystalBall;
    }
}
