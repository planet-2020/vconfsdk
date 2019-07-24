package com.kedacom.vconf.sdk.amulet;

import android.util.Log;

import com.google.gson.Gson;
import com.kedacom.vconf.sdk.utils.log.KLog;

import java.util.Arrays;


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

        if (!magicBook.checkUserPara(setName, paras)){
            KLog.p("checkUserPara not pass");
            return;
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

        if (!magicBook.checkUserPara(getName, paras)){
            KLog.p("checkUserPara not pass");
            return null;
        }

        // 填充用户参数
        // （用户传入的参数个数比注册的少1个，因为注册的包含传出参数用来接收请求结果，
        // 而用户是通过返回值获取结果而非传出参数，所以少了1个传出参数，但是此处内部处理时需要参数对齐，个数要一致）
        paras = Arrays.copyOf(paras, paras.length+1); // 对于get方法最后一个参数为出参，用户未传入，此处我们补全。
        paras[paras.length-1] = new StringBuffer();
        Object[] methodParas = magicBook.userPara2MethodPara(paras, magicBook.getParaClasses(getName));
        StringBuffer sb = new StringBuffer();
        for (int i=0; i<methodParas.length; ++i){ // TODO 使用KLog的打印数组功能直接打印
            sb.append(methodParas[i]).append(", ");
        }
        String methodName = magicBook.getMethod(getName);
        Log.d(TAG, String.format("-=-> %s(%s) paras={%s}", getName, methodName, sb));
        crystalBall.spell(magicBook.getMethodOwner(getName), methodName, methodParas, magicBook.getParaClasses(getName));
        Log.d(TAG, String.format("<-=- %s result=%s", getName, methodParas[methodParas.length-1]));

        Class<?>[] userParaTypes = magicBook.getUserParaClasses(getName);
        return gson.fromJson(methodParas[methodParas.length-1].toString(), userParaTypes[userParaTypes.length-1]); // XXX NOTE: 最后一个参数为出参！下层通过该参数反馈用户结果，必须遵守这个约定
    }

    @Override
    public void setCrystalBall(ICrystalBall crystalBall) {
        this.crystalBall = crystalBall;
    }

}
