package com.kedacom.vconf.sdk.amulet;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.util.Log;
import android.util.SparseIntArray;

import com.kedacom.vconf.sdk.utils.json.Kson;
import com.kedacom.vconf.sdk.utils.log.KLog;

import java.util.LinkedHashSet;
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
        if (magicBook.isGet(reqId)){
            KLog.p(KLog.ERROR, "session fairy can not handle GET req %s", reqId);
            return false;
        }
        if (null == reqParas){
            KLog.p(KLog.ERROR, "reqParas is null");
            return false;
        }

        if (!Helper.checkUserPara(reqId, reqParas, magicBook)){
            return false;
        }

        Session s = new Session(listener, reqSn, reqId, reqParas, (int) (magicBook.timeout(reqId) * 1000), magicBook.rspSeqs(reqId));
        sessions.add(s);

        // 用户参数转换为底层方法需要的参数
        Class<?>[] nativeParaClasses = magicBook.nativeParaClasses(s.reqId);
        Object[] paras = Helper.convertUserPara2NativePara(s.reqPara, nativeParaClasses);
        StringBuilder sb = new StringBuilder();
        for (Object para : paras) {
            sb.append(para).append(", ");
        }
        String methodName = magicBook.reqName(s.reqId);
        boolean hasRsp = null != s.rspSeqs && 0 != s.rspSeqs.length;
        if (hasRsp) {
            // 启动超时
            Message msg = Message.obtain();
            msg.what = MsgId_Timeout;
            msg.obj = s;
            uiHandler.sendMessageDelayed(msg, s.timeoutVal);
        }
        s.setState(Session.READY);

        Log.d(TAG, String.format("%s -~-> %s(%s) \nparas={%s}", hasRsp?s.id:"", s.reqId, methodName, sb));

        reqHandler.post(() -> {
            if(!s.transState(Session.READY, Session.SENDING)){
                return;
            }

            // 调用native接口
            String nativeMethodOwner = magicBook.nativeMethodOwner(s.reqId);
            long nativeCallCostTime;
            long timestamp = System.currentTimeMillis();
            if (null != crystalBall) {
                crystalBall.spell(nativeMethodOwner, methodName, paras, nativeParaClasses);
            }
            nativeCallCostTime = System.currentTimeMillis() - timestamp;

            KLog.p(KLog.DEBUG,"native method %s cost time: %s", nativeMethodOwner+"#"+methodName, nativeCallCostTime);
            if(!s.transState(Session.SENDING, Session.SENT)){
                return;
            }

            uiHandler.post(() -> {
                if(!s.transState(Session.SENT, Session.WAITING)){
                    return;
                }
                if (!hasRsp){
                    s.setState(Session.END);
                    sessions.remove(s);
                }
                s.listener.onReqSent(hasRsp, s.reqId, s.reqSn, s.reqPara);
            });

        });

        return true;
    }


    @Override
    public boolean cancelReq(int reqSn) {
        for (Session s : sessions) {
            if (reqSn == s.reqSn) {
                s.setState(Session.CANCELED);
                uiHandler.removeMessages(MsgId_Timeout, s);

                // 用户很有可能在onMsg回调中调用cancelReq（onMsg回调到上层的onRsp然后用户在onRsp中cancelReq），
                // 然而onMsg中我们正在遍历sessions，所以我们延迟删除
                uiHandler.post(() -> {
                    Log.d(TAG, String.format("%s -~->x %s", s.id, s.reqId));
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
                    Log.d(TAG, String.format("%s <-~-%s %s(%s) \n%s", s.id, gotLast?"o":"", rspId, msgName, msgContent));

                    break tryConsume;
                }
            }

        }

        return bConsumed;
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
