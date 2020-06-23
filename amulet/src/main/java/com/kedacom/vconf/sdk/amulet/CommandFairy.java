package com.kedacom.vconf.sdk.amulet;

import android.util.Log;

import com.kedacom.vconf.sdk.utils.json.Kson;
import com.kedacom.vconf.sdk.utils.log.KLog;

import java.util.Arrays;


final class CommandFairy implements IFairy.ICommandFairy{

    private IMagicBook magicBook;

    private ICrystalBall crystalBall;

    CommandFairy() {}

    @Override
    public void set(String reqName, Object... paras) {
        if (null == crystalBall){
            KLog.p(KLog.ERROR, "no crystalBall ");
            return;
        }
        if (null == magicBook){
            KLog.p(KLog.ERROR, "no magicBook ");
            return;
        }

        if (!Helper.checkUserPara(reqName, paras, magicBook)){
            KLog.p(KLog.ERROR,"checkUserPara not pass");
            return;
        }

        Class<?>[] nativeParaClasses = magicBook.getNativeParaClasses(reqName);
        Object[] methodParas = Helper.convertUserPara2NativePara(paras, nativeParaClasses);
        StringBuffer sb = new StringBuffer();
        for (int i=0; i<methodParas.length; ++i){
            sb.append(methodParas[i]).append(", ");
        }
        String methodName = magicBook.getReqId(reqName);
        Log.d(TAG, String.format(" -=->| %s(%s) \nparas={%s}", reqName, methodName, sb));
        crystalBall.spell(magicBook.getNativeMethodOwner(reqName), methodName, methodParas, nativeParaClasses);
    }

    @Override
    public Object get(String reqName, Object... paras) {
        if (null == crystalBall){
            KLog.p(KLog.ERROR, "no crystalBall ");
            return null;
        }
        if (null == magicBook){
            KLog.p(KLog.ERROR, "no magicBook ");
            return null;
        }

        if (!magicBook.isReqTypeGet(reqName)){
            KLog.p(KLog.ERROR, "Unknown GET req %s", reqName);
            return null;
        }

        if (!Helper.checkUserPara(reqName, paras, magicBook)){
            KLog.p(KLog.ERROR,"checkUserPara not pass");
            return null;
        }

        // 补齐用户参数
        // 传入的请求参数个数比注册的用户参数个数少1个，因为注册的最后一个用户参数是个“出参”用于接收native方法返回的结果，
        // 该参数用户无需传入，因为用户实际使用时是通过返回值获取结果而非传出参数。但是此处内部处理时需要补齐该参数。
        // NOTE：native方法的传出参数总是StringBuffer类型，并且总是参数列表的最后一个，这点跟业务组件约定好。
        paras = Arrays.copyOf(paras, paras.length+1);
        paras[paras.length-1] = new StringBuffer();
        Class<?>[] nativeParaClasses = magicBook.getNativeParaClasses(reqName);
        Object[] nativeParas = Helper.convertUserPara2NativePara(paras, nativeParaClasses);
        StringBuilder sb = new StringBuilder();
        for (int i=0; i<nativeParas.length; ++i){
            sb.append(nativeParas[i]).append(", ");
        }
        String methodName = magicBook.getReqId(reqName);
        Log.d(TAG, String.format(" -=-> %s(%s) \nparas={%s}", reqName, methodName, sb));
        crystalBall.spell(magicBook.getNativeMethodOwner(reqName), methodName, nativeParas, nativeParaClasses);
        Log.d(TAG, String.format(" <-=- %s \nresult=%s", reqName, nativeParas[nativeParas.length-1]));

        Class<?>[] userParaTypes = magicBook.getUserParaClasses(reqName);
        return Kson.fromJson(nativeParas[nativeParas.length-1].toString(), userParaTypes[userParaTypes.length-1]); // NOTE: 最后一个参数为出参！下层通过该参数反馈用户结果，必须遵守这个约定
    }

    @Override
    public void setCrystalBall(ICrystalBall crystalBall) {
        this.crystalBall = crystalBall;
    }

    @Override
    public void setMagicBook(IMagicBook magicBook) {
        this.magicBook = magicBook;
    }

}
