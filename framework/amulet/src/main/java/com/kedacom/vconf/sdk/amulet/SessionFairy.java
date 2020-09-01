package com.kedacom.vconf.sdk.amulet;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.util.Log;
import android.util.SparseIntArray;

import androidx.annotation.NonNull;

import com.kedacom.vconf.sdk.utils.json.Kson;
import com.kedacom.vconf.sdk.utils.lang.PrimitiveTypeHelper;
import com.kedacom.vconf.sdk.utils.lang.StringHelper;
import com.kedacom.vconf.sdk.utils.log.KLog;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;


final class SessionFairy implements IFairy.ISessionFairy{

    private IMagicBook magicBook;

    private ICrystalBall crystalBall;

    // session集合。
    // 使用有序集合以保证会话的时序性。
    private Set<Session> sessions = new LinkedHashSet<>();

    private static HandlerThread reqThread = new HandlerThread("reqThr", Process.THREAD_PRIORITY_BACKGROUND);
    static {
        reqThread.start();
    }
    private Handler reqHandler = new Handler(reqThread.getLooper());

    private static final int MsgId_Timeout = 999;
    private Handler uiHandler = new Handler(Looper.getMainLooper()){
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MsgId_Timeout) {
                Session s = (Session) msg.obj;
                s.setState(Session.TIMEOUT);
                sessions.remove(s);
                Log.d(TAG, String.format("%s <-~-o TIMEOUT", s.id));
                s.listener.onTimeout(s.reqId, s.reqSn, s.reqPara);
            }
        }
    };


    @Override
    public void setCrystalBall(ICrystalBall crystalBall) {
        this.crystalBall = crystalBall;
    }

    @Override
    public void setMagicBook(IMagicBook magicBook) {
        this.magicBook = magicBook;
    }

    @Override
    public boolean req(IListener listener, String reqId, int reqSn, Object... reqParas) {
        if (null == crystalBall){
            KLog.p(KLog.ERROR, "no crystalBall");
            return false;
        }
        if (null == magicBook){
            KLog.p(KLog.ERROR, "no magicBook");
            return false;
        }
        if (null == listener){
            KLog.p(KLog.ERROR, "listener is null");
            return false;
        }
        if (null == reqParas){
            KLog.p(KLog.ERROR, "reqParas is null");
            return false;
        }

        Session s = new Session(listener, reqSn, reqId, reqParas, (int) (magicBook.timeout(reqId) * 1000), magicBook.rspSeqs(reqId));
        sessions.add(s);

        // 用户参数转为native参数
        Object[] paras = convertUserPara2NativePara(s.reqId, s.reqPara, magicBook);
        if (paras == null){
            KLog.p(KLog.ERROR, "convertUserPara2NativePara failed");
            return false;
        }
        StringBuilder sb = new StringBuilder();
        for (Object para : paras) {
            sb.append(para).append(", ");
        }

        String methodName = magicBook.reqName(s.reqId);
        s.setState(Session.READY);

        reqHandler.postDelayed(() -> {
            if(!s.transState(Session.READY, Session.SENDING)){
                return;
            }

            Log.d(TAG, String.format("%s -~-> %s(%s) \nparas={%s}", s.id, s.reqId, methodName, sb));

            // 启动超时
            Message msg = Message.obtain();
            msg.what = MsgId_Timeout;
            msg.obj = s;
            uiHandler.sendMessageDelayed(msg, s.timeoutVal);

            // 调用native接口
            String nativeMethodOwner = magicBook.nativeMethodOwner(s.reqId);
            long timestamp = System.currentTimeMillis();
            if (null != crystalBall) {
                crystalBall.spell(nativeMethodOwner, methodName, paras, magicBook.nativeParaClasses(s.reqId));
            }
            long nativeCallCostTime = System.currentTimeMillis() - timestamp;

            KLog.p(KLog.DEBUG,"native method %s cost time: %s", nativeMethodOwner+"#"+methodName, nativeCallCostTime);
            if(!s.transState(Session.SENDING, Session.SENT)){
                return;
            }

            uiHandler.post(() -> {
                if(!s.transState(Session.SENT, Session.WAITING)){
                    return;
                }
                boolean hasRsp = null != s.rspSeqs && 0 != s.rspSeqs.length;
                if (!hasRsp){
                    // 没有响应，结束会话并移除定时器
                    s.setState(Session.END);
                    sessions.remove(s);
                    uiHandler.removeMessages(MsgId_Timeout, s);
                }
                // 判断是否存在出参，若存在出参（json形式）则转为用户参数（类对象）
                Object output = null;
                int outputParaIndex = magicBook.outputParaIndex(reqId);
                Class<?>[] userParaClasses = magicBook.userParaClasses(reqId);
                boolean outputParaExists = 0 <= outputParaIndex && outputParaIndex < userParaClasses.length;
                if (outputParaExists){
                    output = Kson.fromJson(
                            paras[outputParaIndex].toString(), // 目前和native之间交换数据是通过json字符串
                            userParaClasses[outputParaIndex]
                    );
                }
                if (!hasRsp) {
                    Log.d(TAG, String.format("%s <-~-o NO RESPONSE%s", s.id, outputParaExists ? ", outputPara="+paras[outputParaIndex] : ""));
                }else if(outputParaExists){
                    KLog.p(KLog.DEBUG,"native method %s output para: %s", nativeMethodOwner+"#"+methodName, paras[outputParaIndex]);
                }

                // 上报用户请求已发送，并反馈出参（若存在）
                s.listener.onReqSent(hasRsp, s.reqId, s.reqSn, s.reqPara, output);
            });

        },
                10 /*做一个短暂的延时以保证消息交互打印的时序更易理解*/
        );

        return true;
    }


    @Override
    public boolean cancelReq(int reqSn) {
        for (Session s : sessions) {
            if (reqSn == s.reqSn) {
                s.setState(Session.CANCELED);
                uiHandler.removeMessages(MsgId_Timeout, s);

                reqHandler.post(() -> Log.d(TAG, String.format("%s -~->x %s", s.id, s.reqId)));

                // 用户很有可能在onMsg回调中调用cancelReq（onMsg回调到上层的onRsp然后用户在onRsp中cancelReq），
                // 然而onMsg中我们正在遍历sessions，所以我们延迟删除
                uiHandler.post(() -> {
                    if (s.isState(Session.CANCELED)) {
                        sessions.remove(s);
                    }else{
                        KLog.p(KLog.ERROR, "canceled session %s gone? something must be wrong!", s.id);
                    }
                });
                return true;
            }
        }

        return false;
    }


    @Override
    public boolean onMsg(String msgName, String msgContent) {
        if (null == magicBook){
            return false;
        }
        Set<String> rspIds = magicBook.rspIds(msgName);
        if (rspIds==null || rspIds.isEmpty()){
            return false;
        }

        boolean bConsumed = false;
        tryConsume:
        for (String rspId : rspIds) {
            Class<?> rspClass = magicBook.rspClass(rspId);
            if (rspClass == null){
                continue;
            }

            for (final Session s : sessions) { // 查找期望该响应的会话
                if (!s.isState(Session.WAITING, Session.RECVING)) {
                    continue;
                }

                SparseIntArray candidates = new SparseIntArray();
                boolean gotLast = false; // 是否匹配到会话的最后一条响应

                for (int i = 0; i < s.candidates.size(); ++i) { // 在候选响应序列中查找所有能处理该响应的序列。
                    int rspSeqIdx = s.candidates.keyAt(i); // 挑出第i路响应序列
                    int expectedRspIdx = s.candidates.get(rspSeqIdx); // 定位到第i路响应序列当前期望匹配的rsp的位置
                    String[] candidateRspSeq = s.rspSeqs[rspSeqIdx];
                    String expectedRspId = candidateRspSeq[expectedRspIdx]; // 找到第i路响应序列当前期望匹配的rspId
                    
                    if (rspId.equals(expectedRspId)) { // 成功匹配
                        ++expectedRspIdx; // 期待下一条响应

                    }else{ // 未匹配成功，我们还需判断当前期望的rspId是否为GreedyNote，针对GreedyNote的情形我们需特殊处理

                        if (!magicBook.isGreedyNote(expectedRspId)){
                            continue;
                        }
                        // 若当前期望的rspId是GreedyNote则尝试匹配其前后的rspId
                        //noinspection StatementWithEmptyBody
                        if(rspId.equals(candidateRspSeq[expectedRspIdx-1])){
                            // 若匹配到GreedyNote前面的rspId，则expectedRspIdx不变，表明我们依然可以接收GreedyNote前面或后面的rspId
                            // Nothing to do
                        }else if (expectedRspIdx < candidateRspSeq.length-1 && rspId.equals(candidateRspSeq[expectedRspIdx+1])){
                            // 若匹配到GreedyNote后面的rspId，则expectedRspIdx+2
                            expectedRspIdx += 2;
                        }else{
                            continue;
                        }
                    }

                    candidates.put(rspSeqIdx, expectedRspIdx); //添加该响应序列到新的候选序列，并记录该序列下一个期望被匹配的响应

                    if(expectedRspIdx == candidateRspSeq.length){ // 已匹配到该候选响应序列的最后一条响应
                        gotLast = true;
                        break; // 已匹配到该候选响应序列的最后一条响应。则该候选响应序列被认为最终匹配的响应序列，无需再尝试匹配其他候选序列。
                    }

                }

                if (0 == candidates.size()) { // 未匹配到任何响应序列
                    continue; // 该响应不是该会话所期望的，继续寻找期望该响应的会话
                }

                bConsumed = s.listener.onRsp(gotLast, rspId,
                        Kson.fromJson(msgContent, rspClass),
                        s.reqId, s.reqSn, s.reqPara);

                if (bConsumed) {

                    Log.d(TAG, String.format("%s <-~-%s %s(%s) \n%s", s.id, gotLast?"o":"", rspId, msgName, msgContent));

                    s.candidates = candidates; // 更新候选序列
                    if (gotLast) {  // 已集齐会话期望的所有响应，该会话结束。
                        if (!s.isState(Session.CANCELED)) {
                            uiHandler.removeMessages(MsgId_Timeout, s);
                            s.setState(Session.END);
                            sessions.remove(s);
                        }
                    } else {
                        if (!s.isState(Session.CANCELED)) {
                            s.setState(Session.RECVING);
                        }
                    }

                    break tryConsume;
                }
            }

        }

        return bConsumed;
    }


    /**
     * 将用户参数转为native方法参数
     * */
    private Object[] convertUserPara2NativePara(@NonNull String reqId, @NonNull Object[] userParas, @NonNull IMagicBook magicBook){
        /*
         * 验证参数合法性
         * */
        Class<?>[] userParaClasses = magicBook.userParaClasses(reqId);
        Class<?>[] nativeParaClasses = magicBook.nativeParaClasses(reqId);
        if (null==userParaClasses || nativeParaClasses == null){
            throw new RuntimeException(String.format("reqId %s not registered!", reqId));
        }

        if (userParaClasses.length != nativeParaClasses.length){
            throw new RuntimeException(String.format("reqId %s: userParaClasses.length(%s) != nativeParaClasses.length(%s)",
                    reqId, userParaClasses.length, nativeParaClasses.length));
        }

        int outputParaIndex = magicBook.outputParaIndex(reqId);
        boolean outputParaExists = 0 <= outputParaIndex && outputParaIndex < userParaClasses.length;
        int expectedArgumentLen = outputParaExists ? userParaClasses.length - 1 : userParaClasses.length;
        if (expectedArgumentLen != userParas.length){
            throw new RuntimeException(String.format("reqId %s: invalid user para num, expect %s but got %s", reqId, expectedArgumentLen, userParas.length));
        }

        if (outputParaExists){
            // 补齐出参。 出参比较特殊，用户并不传入出参，导致实参比形参少。
            Class<?> outputParaClass = userParaClasses[outputParaIndex];
            Object outputPara;
            try {
                Constructor<?> ctor = outputParaClass.getDeclaredConstructor();
                ctor.setAccessible(true);
                outputPara = ctor.newInstance();
            } catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
                e.printStackTrace();
                throw new RuntimeException(String.format("try construct output para %s failed!", outputParaClass));
            }

            List<Object> userParaList = new ArrayList<>();
            Collections.addAll(userParaList, userParas);
            userParaList.add(outputParaIndex, outputPara);
            userParas = userParaList.toArray();
        }

        // 校验用户实际传入的参数类型是否匹配注册的参数类型
        for(int i=0; i<userParaClasses.length; ++i){
            Class<?> userParaClz = userParaClasses[i];
            Class<?> reqParaClz = userParas[i].getClass();
            if (!(reqParaClz==userParaClz // 同类
                    || userParaClz.isAssignableFrom(reqParaClz) // 子类亦可接受
                    || userParaClz.isPrimitive() && reqParaClz== PrimitiveTypeHelper.getWrapperClass(userParaClz) // 注册用户参数类型为基本类型，请求参数为对应的包装类亦可接受
            )){
                KLog.p(KLog.ERROR, "invalid user para type for %s, expect %s but got %s", reqId, userParaClz, reqParaClz);
                return null;
            }
        }

        /*
         * 用户参数转为native参数
         * */
        Object[] nativeParas = new Object[nativeParaClasses.length];
        for (int i=0; i<nativeParas.length; ++i){
            Object userPara = userParas[i];
            Class<?> nativeParaType = nativeParaClasses[i];
            KLog.p(KLog.DEBUG,"userPara[%s].class=%s, nativeMethodPara[%s].class=%s", i, null==userPara? null : userPara.getClass(), i, nativeParaType);
            if (null == userPara){
                nativeParas[i] = nativeParaType.isPrimitive() ? PrimitiveTypeHelper.getDefaultValue(nativeParaType) :
                        StringHelper.isStringCompatible(nativeParaType) ? "" : null;
            }else if (userPara.getClass() == nativeParaType
                    || nativeParaType.isAssignableFrom(userPara.getClass())){
                nativeParas[i] = userPara;
            }else {
                if (StringHelper.isStringCompatible(nativeParaType)) {
                    if (StringHelper.isStringCompatible(userPara.getClass())) {
                        nativeParas[i] = StringHelper.convert2CompatibleType(nativeParaType, userPara);
                    }else {
                        nativeParas[i] = StringHelper.convert2CompatibleType(nativeParaType, Kson.toJson(userPara));
                    }
                } else if (nativeParaType.isPrimitive()) {
                    if (userPara.getClass() == PrimitiveTypeHelper.getWrapperClass(nativeParaType)){
                        nativeParas[i] = userPara;
                    }else if (userPara.getClass().isEnum() && nativeParaType==int.class) {
                        nativeParas[i] = Integer.valueOf(Kson.toJson(userPara)); //XXX 虽然当前枚举都是转为int型，但是后续需求也可能变更为其它类型如String。后续如果有这样的需求此处需相应更改。
                    }else{
                        throw new ClassCastException("trying to convert user para to native method para failed: "+userPara.getClass()+" can not cast to "+nativeParaType);
                    }
                } else {
                    throw new ClassCastException("trying to convert user para to native method para failed: "+userPara.getClass()+" can not cast to "+nativeParaType);
                }
            }

        }

        return nativeParas;
    }



    /**
     * 会话 */
    private static final class Session{
        private static int count = 0;
        private final int id;   // 会话ID
        private final IListener listener;// 会话监听器
        private final int reqSn;        // 请求序列号。上层用来唯一标识一次请求，会话不使用不处理该字段，上报响应时带回给请求者。
        private final String reqId;   // 请求ID。
        private final Object[] reqPara;   // 请求参数。
        private final int timeoutVal;   // 超时时限。单位：毫秒
        private final String[][] rspSeqs;  // 响应名称序列组。一条请求可能对应多条响应序列，如reqXX——{{rsp1, rsp2},{rsp1,rsp3}}，一次会话只能匹配其中一条序列。
        private SparseIntArray candidates; // 候选的响应序列记录。记录当前可被用来匹配的响应序列组及各响应序列中的下一条待匹配响应（的位置）。“键”对应响应序列组的行下标，“值”对应列下标。每收到一条响应后该记录会更新。

        private int state = IDLE;  // 会话状态
        private static final int IDLE = 0;  // 空闲。初始状态
        private static final int READY = 1;  // 已准备，待执行。
        private static final int SENDING = 2; // 正在发送请求。
        private static final int SENT = 3; // 请求已发出。
        private static final int WAITING = 4; // 正在等待响应。请求发送以后，收到第一条响应之前。
        private static final int RECVING = 5; // 正在接收响应序列。收到第一条响应后，收到最后一条响应之前。
        private static final int CANCELED = 6; // 已取消。会话结束前用户取消了该会话。最终状态。
        private static final int TIMEOUT = 7; // 已超时。最终状态。
        private static final int END = 8;   // 正常结束。最终状态。

        private Session(IListener listener, int reqSn, String reqId, Object[] reqPara, int timeoutVal, String[][] rspSeqs){
            this.id = count++;
            this.listener = listener;
            this.reqSn = reqSn;
            this.reqId = reqId;
            this.reqPara = reqPara;
            this.timeoutVal = timeoutVal>0 ? timeoutVal : 5*1000;
            this.rspSeqs = rspSeqs;
            candidates = new SparseIntArray(3);
            if (null != rspSeqs) {
                for (int i = 0; i < rspSeqs.length; ++i) {
                    candidates.put(i, 0);
                }
            }
        }

        private synchronized boolean isState(int... states) {
            for (int st : states){
                if (st == state){
                    return true;
                }
            }
            return false;
        }

        private synchronized void setState(int state) {
            this.state = state;
        }

        @SuppressWarnings("BooleanMethodIsAlwaysInverted")
        private synchronized boolean transState(int from, int to) {
            if (from != state){
                return false;
            }
            state = to;
            return true;
        }

    }

}
