package com.kedacom.vconf.sdk.base.basement;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.Process;
import android.util.Log;
import android.util.SparseIntArray;

import java.util.HashSet;
import java.util.Set;


@SuppressWarnings("UnusedReturnValue")
final class SessionFairy implements IRequestProcessor, IResponseProcessor {

    private static final String TAG = SessionFairy.class.getSimpleName();

    private static SessionFairy instance;

    private Set<Session> sessions;  // 进行中的会话
    private Set<Session> blockedSessions; // 被阻塞的会话

    private int sessionCnt = 0;
    private static final int MAX_SESSION_NUM = 2000; // 进行中的会话数上限
    private static final int MAX_BLOCKED_SESSION_NUM = 10000; // 被阻塞的会话数上限

    private Handler reqHandler;
    private Handler timeoutHandler;
    private static final int MSG_ID_START_SESSION = 100;
    private static final int MSG_ID_TIMEOUT = 999;

    private JsonProcessor jsonProcessor;
    private MagicBook magicBook;
    private MagicStick magicStick;

    private SessionFairy(){
        sessions = new HashSet<>();
        blockedSessions = new HashSet<>();

        jsonProcessor = JsonProcessor.instance();
        magicBook = MagicBook.instance();
        magicStick = MagicStick.instance();

        initRequestHandler();
        initTimeoutHandler();
    }

    synchronized static SessionFairy instance() {
        if (null == instance) {
            instance = new SessionFairy();
        }

        return instance;
    }



    @Override
    public synchronized boolean processRequest(Handler requester, String reqId, Object reqPara, int reqSn) {

        if (null == requester){
            Log.e(TAG, "requester is null");
            return false;
        }

        if (!magicBook.isRequest(reqId)){
            Log.e(TAG, "no such request: "+reqId);
            return false;
        }

        if (null != reqPara
                && reqPara.getClass() != magicBook.getReqParaClazz(reqId)){
            Log.e(TAG, String.format("invalid request para %s, expect %s", reqPara.getClass(), magicBook.getReqParaClazz(reqId)));
            return false;
        }

        // 检查是否存在未完成的同类请求
        boolean isReqExist = false;
        for (Session s : sessions){
            if (s.reqId.equals(reqId)) {
                isReqExist = true;
                break;
            }
        }

        if (isReqExist
                && magicBook.isMutualExclusive(reqId)){
            Log.w(TAG, String.format("request %s is mutual exclusive", reqId));
            return false;
        }

        // 创建会话
        Session s = new Session(++sessionCnt, requester, reqSn, reqId, reqPara,
                magicBook.getTimeout(reqId)*1000,
                magicBook.getRspSeqs(reqId));
        // 尝试发送请求
        if (!isReqExist){
            if (sessions.size() >= MAX_SESSION_NUM){
                Log.e(TAG, "requests reach the limit "+MAX_SESSION_NUM);
                return false;
            }
            s.state = Session.READY;
            sessions.add(s);
            Message msg = Message.obtain();
            msg.what = MSG_ID_START_SESSION;
            msg.obj = s.id;
            reqHandler.sendMessage(msg);

            return true;

        }else{
            /* 存在未完成的同类请求则阻塞当前请求直到同类请求完成。 此举的目的是为了尽量保证“请求-响应”是正确匹配的。
            在不采用此举的情形下，考虑如下场景：上层连续多次同类请求req1、req2，期望的响应序列为rsp1、rsp2，
            但因下层不保证消息的有序性，所以可能到达序列为rsp2、rsp1，进而错误的将req1-rsp2、req2-rsp1匹配了。
             注意：此举并不能完全保证“请求-响应”的正确匹配，极端情形下，req1等待rsp1超时然后req1被废弃接着发送req2，
             若此时刚好rsp1到来了，则req2-rsp1将被匹配。*/
            if (blockedSessions.size() >= MAX_BLOCKED_SESSION_NUM){
                Log.e(TAG, "blocked requests reach the limit "+MAX_BLOCKED_SESSION_NUM);
                return false;
            }
            s.state = Session.BLOCKING;
            blockedSessions.add(s);

            Log.w(TAG, String.format("-=->| %s (session %d BLOCKED)", s.reqId, s.id)); // XXX 启动超时？阻塞时间算在超时内？

            return true;
        }

    }

    @Override
    public synchronized boolean processCancelRequest(Handler requester, int reqSn) {
        if (null == requester){
            Log.e(TAG, "requester is null");
            return false;
        }

        for (Session s : sessions){
            if (reqSn == s.reqSn
                    && requester.equals(s.requester)) {
                s.state = Session.END;
                timeoutHandler.removeMessages(MSG_ID_TIMEOUT, s.id); // 移除定时器
                sessions.remove(s);
                Log.d(TAG, String.format("<-=- (session %d CANCELED)", s.id));
                driveBlockedSession(s.reqId);// 驱动被当前会话阻塞的会话
                return true;
            }
        }

        for (Session s : blockedSessions){
            if (reqSn == s.reqSn
                    && requester.equals(s.requester)) {
                blockedSessions.remove(s);
                return true;
            }
        }

        return false;
    }


    @Override
    public synchronized boolean processResponse(String rspName, String rspBody){

        if (!magicBook.isResponse(rspName)){
            return false;
        }

        SparseIntArray candidates = new SparseIntArray();
        String[] candidateRspSeq;
        String candidateRsp;
        int rspSeqIndx;
        int rspIndx;
        boolean gotLast = false; // 是否匹配到会话的最后一条响应
        for (final Session s : sessions) { // 查找期望该响应的会话
            if (Session.WAITING != s.state
                    && Session.RECVING != s.state) {
                continue;
            }

            for (int i=0; i<s.candidates.size(); ++i) { // 在候选序列中查找所有能处理该响应的序列。
                rspSeqIndx = s.candidates.keyAt(i);
                rspIndx = s.candidates.get(rspSeqIndx);
                candidateRspSeq = s.rspSeqs[rspSeqIndx];
                candidateRsp = candidateRspSeq[rspIndx];
                if (rspName.equals(candidateRsp)){ // 找到了一路匹配的序列
                    candidates.put(rspSeqIndx, rspIndx+1); // 记录该序列下一个可匹配的响应
                    if (candidateRspSeq.length == rspIndx+1){ // 该响应为该匹配序列中的最后一条响应
                        gotLast = true;
                    }
                }

                if (gotLast){ // 若该响应已匹配到某条响应序列的最后一条响应，则该序列即为最终响应序列，匹配过程结束，该会话结束。
                    break;
                }
            }

            if (0 == candidates.size()){ // 候选响应序列中未找到该响应
                continue; // 该响应不是该会话所期望的，继续寻找期望该响应的会话
            }

            s.candidates = candidates; // 更新候选序列

            Message rsp = Message.obtain();

            if (gotLast){// 该会话已获取到最后一条期待的响应
                Log.d(TAG, String.format("<-=- %s (session %d FINISH) \n%s", rspName, s.id, rspBody));
                timeoutHandler.removeMessages(MSG_ID_TIMEOUT, s.id); // 移除定时器
                s.state = Session.END; // 已获取到所有期待的响应，该会话结束
                sessions.remove(s);
                rsp.obj = new FeedbackBundle(rspName, jsonProcessor.fromJson(rspBody, magicBook.getRspClazz(rspName)), FeedbackBundle.RSP_FIN, s.reqId, s.reqSn);
                s.requester.sendMessage(rsp); // 上报该响应

                driveBlockedSession(s.reqId); // 驱动被当前会话阻塞的会话

            } else {
                Log.d(TAG, String.format("<-=- %s (session %d) \n%s", rspName, s.id, rspBody));
                s.state = Session.RECVING; // 已收到响应，继续接收后续响应
                rsp.obj = new FeedbackBundle(rspName, jsonProcessor.fromJson(rspBody, magicBook.getRspClazz(rspName)), FeedbackBundle.RSP, s.reqId, s.reqSn);
                s.requester.sendMessage(rsp); // 上报该响应
            }

/*            // for debug
            Log.i(TAG, String.format("candidate rsp seqs for req %s{\n", s.reqId));
            for (int i=0; i<candidates.size(); ++i) {
                int key = candidates.keyAt(i);
                String[] rspSeq = s.rspSeqs[key];
                StringBuffer stringBuffer = new StringBuffer();
                stringBuffer.append("{");
                for (int j=0; j<rspSeq.length; ++j){
                    stringBuffer.append(rspSeq[j]+" ");
                }
                stringBuffer.append("}");
                int value = candidates.get(key);
                String next = value<rspSeq.length ? rspSeq[value] : "none";
                stringBuffer.append(" next "+next+"\n");
                Log.i(TAG, stringBuffer.toString());
            }
            Log.i(TAG, "}\n");*/

            return true;
        }

        return false;
    }


    private synchronized Session getActiveSession(int sid){
        for (Session s: sessions){
            if (sid == s.id){
                return s;
            }
        }

        return null;
    }

    private synchronized Session getBlockedSession(String reqId){
        for (Session s : blockedSessions){
            if (s.reqId.equals(reqId)) {
                return s;
            }
        }
        return null;
    }


    private synchronized int startSession(int sid){
        Session s = getActiveSession(sid);
        if (null == s){
            Log.e(TAG, "try to start session failed, no such session, sid="+sid);
            return -1;
        }

        if (Session.READY != s.state){
            Log.e(TAG, "try to start session failed, invalid session state "+s.state);
            return -1;
        }

        String jsonReqPara = jsonProcessor.toJson(s.reqPara);
        Log.d(TAG, String.format("-=-> %s (session %d START) \n%s", s.reqId, s.id, jsonReqPara));

        magicStick.request(s.reqId, jsonReqPara);

        if (null==s.rspSeqs || 0==s.rspSeqs.length){
            s.state = Session.END; // 请求没有响应，会话结束
            Log.d(TAG, String.format("<-=- (session %d FINISHED. NO RESPONSE)", s.id));
            sessions.remove(s);
            driveBlockedSession(s.reqId);
            return 0;
        }

        s.state = Session.WAITING; // 请求已发出正在等待响应

        // 启动超时
        Message msg = Message.obtain();
        msg.what = MSG_ID_TIMEOUT;
        msg.obj = s.id;
        timeoutHandler.sendMessageDelayed(msg, s.timeoutVal);

        return 0;
    }


    /**
     * 驱动可能存在的被阻塞的会话。<p>
     * 相同请求ID的会话同一时间只允许一个处于工作状态，余下的处于阻塞状态。
     * @param reqId 请求ID
     * */
    private void driveBlockedSession(String reqId){
        Session blockedSession = getBlockedSession(reqId);
        if (null != blockedSession){
            blockedSessions.remove(blockedSession);
            blockedSession.state = Session.READY;
            sessions.add(blockedSession);
            Message msg = Message.obtain();
            msg.what = MSG_ID_START_SESSION;
            msg.obj = blockedSession.id;
            reqHandler.sendMessage(msg);
        }
    }


    /**
     * 处理超时
     * */
    private synchronized void timeout(int sid){
        Session s = getActiveSession(sid);
        if (null == s){
            Log.e(TAG, "timeout but no such session. sid="+sid);
            return;
        }
        if (Session.WAITING != s.state
                && Session.RECVING != s.state){
            Log.e(TAG, "timeout but invalid session state "+s.state);
            return;
        }

        Log.d(TAG, String.format("<-=- (session %d TIMEOUT)", s.id));
        s.state = Session.END; // 会话结束

        // 通知用户请求超时
        Message rsp = Message.obtain();
        rsp.obj = new FeedbackBundle("Timeout", null, FeedbackBundle.RSP_TIMEOUT, s.reqId, s.reqSn);
        s.requester.sendMessage(rsp);

        sessions.remove(s);

        // 驱动被当前会话阻塞的会话
        driveBlockedSession(s.reqId);
    }


    private void initRequestHandler(){
        HandlerThread handlerThread = new HandlerThread("SM.request", Process.THREAD_PRIORITY_BACKGROUND);
        handlerThread.start();
        reqHandler = new Handler(handlerThread.getLooper()){
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case MSG_ID_START_SESSION:
                        startSession((int) msg.obj);
                        break;
                }
            }
        };
    }


    private void initTimeoutHandler(){
        HandlerThread handlerThread = new HandlerThread("SM.timeout", Process.THREAD_PRIORITY_BACKGROUND);
        handlerThread.start();
        timeoutHandler = new Handler(handlerThread.getLooper()){
            @Override
            public void handleMessage(Message msg) {
                timeout((int) msg.obj);
            }
        };
    }



    /**
     * 会话 */
    private final class Session{
        private final int id;   // 会话ID
        private final Handler requester;// 请求者
        private final int reqSn;        // 请求序列号。上层用来唯一标识一次请求，会话不使用不处理该字段，上报响应时带回给请求者。
        private final String reqId;      // 请求Id。标识请求类型。
        private final Object reqPara;   // 请求参数。
        private final int timeoutVal;   // 超时时限。单位：毫秒
        private final String[][] rspSeqs;  // 响应Id序列组。一条请求可能对应多条响应序列，如{{rsp1, rsp2},{rsp1,rsp3}}，一次会话只能对应其中一条序列。
        private SparseIntArray candidates; // 候选的响应序列记录。记录当前可被用来匹配的响应序列组及各响应序列中的下一条待匹配响应（的位置）。“键”对应响应序列组的行下标，“值”对应列下标。每收到一条响应后该记录会更新。

        private int state;  // 会话状态
        private static final int IDLE = 0;  // 空闲。初始状态
        private static final int BLOCKING = 1;  // 阻塞。若存在未完成的同类会话（请求id相同）则当前会话被阻塞直到同类会话结束。
        private static final int READY = 2;  // 就绪。
        private static final int WAITING = 3; // 等待。请求发送以后，收到响应序列中的第一条响应之前。
        private static final int RECVING = 4; // 接收。收到第一条响应后，收到最后一条响应之前。
        private static final int END = 5;   // 结束。最终状态。会话已成功结束（接收到最后一个响应）或者已失败（超时或其它失败原因）。

        private Session(int id, Handler requester, int reqSn, String reqId, Object reqPara, int timeoutVal, String[][] rspSeqs){
            this.id = id;
            this.requester = requester;
            this.reqSn = reqSn;
            this.reqId = reqId;
            this.reqPara = reqPara;
            this.timeoutVal = timeoutVal;
            this.rspSeqs = rspSeqs;

            candidates = new SparseIntArray(3);
            if (null != rspSeqs) {
                for (int i = 0; i < rspSeqs.length; ++i) {
                    candidates.put(i, 0);
                }
            }
            state = IDLE;
        }

    }

}
