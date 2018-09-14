package com.sissi.vconfsdk.base.engine;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * 会话处理器　
 * Created by Sissi on 1/9/2017.
 */

final class SessionManager {

    private static final String TAG = "SessionManager";

    private static SessionManager instance;

    private ArrayList<Session> sessions;  // 正常会话
    private ArrayList<Session> blockedSessions; // 被阻塞的会话

    private int sessionCnt = 0;
    private static final int MAX_SESSION_NUM = 2000; // 正常会话数上限
    private static final int MAX_BLOCKED_SESSION_NUM = 10000; // 被阻塞的会话数上限

    private static final int MSG_TIMEOUT = 999;
    private Thread timeoutThread; // 超时线程
    private Handler timeoutHandler;

    private MessageRegister messageRegister;

    private Handler sendreqHandler; // 用来发送请求的线程的handler，不能为null
    private Handler emulatedNativeHandler; // 模拟器handler，用于模拟模式

    private SessionManager(){
        sessions = new ArrayList<Session>();
        blockedSessions = new ArrayList<Session>();
        messageRegister = MessageRegister.instance();
        initTimeoutThread();
    }

    synchronized static SessionManager instance() {
        if (null == instance) {
            instance = new SessionManager();
        }

        return instance;
    }

    synchronized SessionManager setSendreqHandler(Handler sendreqHandler){
        this.sendreqHandler = sendreqHandler;
        return this;
    }
    synchronized SessionManager setEmulatedNativeHandler(Handler emulatedNativeHandler){
        this.emulatedNativeHandler = emulatedNativeHandler;
        return this;
    }


    /**
     * 发送请求。
     * @param requester 请求者.
     * @param reqId 请求ID
     * @param reqPara 请求参数.
     * @param reqSn 请求序列号。
     * @param emulatedRsps 模拟响应.若不为null则表示此为模拟请求，消息交互走模拟器而非真实native层。
     * @return 返回真若发送请求成功，假若发送请求失败。
     * */
    synchronized boolean request(Handler requester, String reqId, String reqPara, int reqSn, Object[] emulatedRsps){
        String[][] candidateRspIds = messageRegister.getRsps(reqId);
        String[][] rspIds = candidateRspIds;
        if (null != emulatedRsps) { // 此为模拟请求
            if (null==candidateRspIds || 0==candidateRspIds.length){ // 未为该请求注册对应响应
                return false; // 模拟模式下请求必须有对应的响应
            }
            // 检查模拟响应有效性
            String[] matchedRspIds = null;
            int i = 0;
            for (; i < candidateRspIds.length; ++i) {
                if (candidateRspIds[i].length != emulatedRsps.length) {
                    continue;
                }
                int j = 0;
                for (; j < emulatedRsps.length; ++j) {
                    if (!emulatedRsps[j].getClass().equals(messageRegister.getRspClazz(candidateRspIds[i][j]))) {
                        break;
                    }
                }
                if (j == emulatedRsps.length) {
                    matchedRspIds = candidateRspIds[i];
                    break;
                }
            }
            if (null == matchedRspIds) {
                return false;
            }
            rspIds = new String[][]{matchedRspIds};
        }

        // 检查是否存在未完成的同类请求
        boolean isReqExist = false;
        for (Session s : sessions){
            if (s.reqId.equals(reqId)) {
                isReqExist = true;
                break;
            }
        }

        // 创建会话
        Session s = new Session(++sessionCnt, requester, reqSn, reqId, reqPara, messageRegister.getTimeout(reqId)*1000, rspIds, emulatedRsps);
        // 尝试发送请求
        if (isReqExist){ // 存在未完成的同类请求（阻塞源）
            if (blockedSessions.size() >= MAX_BLOCKED_SESSION_NUM){
                return false;
            }
            blockedSessions.add(s);
//            KLog.p("created blocked session id=%s", sessionCnt); // TODO 启动超时？阻塞时间算在超时内？
            return true; // 返回真表示请求成功，不让外部感知请求被阻塞
        }else{
            if (sessions.size() >= MAX_SESSION_NUM){
                return false;
            }
            sessions.add(s);
            // 发送请求
            if (sendReq(s)){
                sessions.remove(s);
                driveBlockedSession(s.reqId);
            }

            return true;
        }
    }


    /**
     * 接收响应
     * @param rspId 响应Id
     * @param rspContent 响应内容
     * @return 若响应被消化返回真，否则返回假。
     * */
    synchronized boolean respond(String rspId, Object rspContent){
        HashMap<Integer, Integer> candidates = new HashMap<>();
        String[] rspIds = null;
        boolean gotLast = false; // 是否匹配到会话的最后一条响应
        for (final Session s : sessions) { // 查找期望该响应的会话
            if (Session.WAITING != s.state
                    && Session.RECVING != s.state) {
                continue;
            }

            for (int i : s.candidates.keySet()) { // 在候选序列中查找所有能处理该响应的序列。
                rspIds = s.rspIds[i];
                for (int j = s.candidates.get(i); j < rspIds.length; ++j) {
                    if (rspId.equals(rspIds[j])){ // 找到了一路匹配的序列
                        candidates.put(i, j); // 放入新的候选序列中
                        if (rspIds.length == j+1){
                            gotLast = true; // 该响应为该匹配序列中的最后一条响应
                        }
                        break; // 继续找下一路可匹配的序列
                    }
                }
                if (gotLast){ // 若该响应已匹配到某条响应序列的最后一条响应，则该序列即为最终响应序列，匹配过程结束，该会话结束。
                    break;
                }
            }

            if (candidates.isEmpty()){ // 候选响应序列中未找到该响应
                continue; // 该响应不是该会话所期望的，继续寻找期望该响应的会话
            }
            s.candidates = candidates; // 更新候选序列

            s.state = Session.RECVING; // 已收到响应，继续接收后续响应

            Message rsp = Message.obtain();

            if (gotLast){// 该会话已获取到最后一条期待的响应
                Log.i(TAG, String.format("<-=- (session %d FINISH) %s", s.id, rspId));
                timeoutHandler.removeMessages(MSG_TIMEOUT, s); // 移除定时器
                s.state = Session.END; // 已获取到所有期待的响应，该会话结束
                sessions.remove(s);
                rsp.obj = new ResponseBundle(rspId, rspContent, ResponseBundle.RSP_FIN, s.reqId, s.reqSn);
                s.requester.sendMessage(rsp); // 上报该响应
                // 驱动被当前会话阻塞的会话
                driveBlockedSession(s.reqId);
            } else {
                Log.i(TAG, String.format("<-=- (session %d) %s", s.id, rspId));
                rsp.obj = new ResponseBundle(rspId, rspContent, ResponseBundle.RSP, s.reqId, s.reqSn);
                s.requester.sendMessage(rsp); // 上报该响应
            }

            return true;
        }

        return false;
    }

    /**
     * 发送请求
     * @return 若会话结束返回真，否则返回假。
     * */
    private synchronized boolean sendReq(Session s){
        if (null!=s.emulatedRsps && null!=emulatedNativeHandler){ // 若模拟模式下则请求发给模拟器
            Message req = Message.obtain();
            req.obj = s;
            emulatedNativeHandler.sendMessage(req);
        }else{ // 否则直接调用native接口
            NativeMethods.invoke(s.reqId, s.reqPara);
        }

        Log.i(TAG, String.format("-=-> (session %d START) %s", s.id, s.reqId));
        if (null==s.rspIds || 0==s.rspIds.length){
            s.state = Session.END; // 请求没有响应，会话结束
            Log.i(TAG, String.format("<-=- (session %d FINISH) NO RESPONSE", s.id));
            return true;
        }

        s.state = Session.WAITING; // 请求已发出正在等待响应

        // 启动超时
        Message msg = Message.obtain();
        msg.what = MSG_TIMEOUT;
        msg.obj = s;
        timeoutHandler.sendMessageDelayed(msg, s.timeoutVal);

        return false;
    }


    /**
     * 处理超时
     * @param s 已超时的会话。
     * */
    private synchronized void timeout(final Session s){
        Log.i(TAG, String.format("<-=- (session %d TIMEOUT)", s.id));
        s.state = Session.END; // 会话结束

        // 通知用户请求超时
        Message rsp = Message.obtain();
        rsp.obj = new ResponseBundle("TIMEOUT", null, ResponseBundle.RSP_TIMEOUT, s.reqId, s.reqSn);
        s.requester.sendMessage(rsp);

        sessions.remove(s);

        // 驱动被当前会话阻塞的会话
        driveBlockedSession(s.reqId);
    }

    /**
     * 驱动可能存在的被阻塞的会话。<p>
     * 相同请求ID的会话同一时间只允许一个处于工作状态，余下的处于阻塞状态。所以当会话结束时需调用此接口驱动可能存在的被阻塞的会话。
     * @param reqId 请求ID
     * */
    private void driveBlockedSession(String reqId){
        Session bs=null;
        for (final Session s : blockedSessions){
            if (reqId.equals(s.reqId)) {
                blockedSessions.remove(s);
                sessions.add(s);
                bs = s;
                break;
            }
        }

        if (null == bs){
            return;
        }

        // 使用请求线程的Handler发送请求以保证请求均通过请求线程发送
        final Session finalBs = bs;
        sendreqHandler.post(new Runnable() {
            @Override
            public void run() {
                if (sendReq(finalBs)){
                    sessions.remove(finalBs);
                    driveBlockedSession(finalBs.reqId);
                }
            }
        });
    }

    /**
     * 初始化超时线程
     * */
    private void initTimeoutThread(){
        final Object lock = new Object();
        timeoutThread = new Thread() {
            @SuppressLint("HandlerLeak")
            @Override
            public void run() {
                Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                Looper.prepare();
                timeoutHandler = new Handler(){
                    @Override
                    public void handleMessage(Message msg) {
                        Log.i("", "msg="+msg.what);
                        SessionManager.this.timeout((Session) msg.obj);
                    }
                };
                synchronized (lock){ lock.notify(); }
                Looper.loop();
            }
        };
        timeoutThread.setName("SM.timeout");
        timeoutThread.start();
        if (null == timeoutHandler){
            synchronized (lock){
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 会话 */
    final class Session{
        private final int id;   // 会话ID
        private final Handler requester;// 请求者
        private final int reqSn;        // 请求序列号。上层用来唯一标识一次请求，会话不使用不处理该字段，上报响应时带回给请求者。
        private final String reqId;      // 请求Id。标识请求类型。
        private final String reqPara;   // 请求参数（Json 格式）。可能为null，表示没有请求参数
        private final int timeoutVal;   // 超时时限。单位：毫秒
        private final String[][] rspIds;  // 响应Id序列组。一条请求可能对应多条响应序列，如{{rsp1, rsp2},{rsp1,rsp3}}，一次会话只能对应其中一条序列。
        private HashMap<Integer, Integer> candidates; // 候选的响应序列记录。记录当前可被用来匹配的响应序列及起始匹配位置。“键”对应响应Id序列组rspIds的行下标，“值”对应rspIds的列下标。每收到一条响应后该记录会更新。
        private final Object[] emulatedRsps;     // 模拟响应。仅用于模拟模式。

        private int state;  // 会话状态
        private static final int IDLE = 2;  // 空闲。初始状态
        private static final int WAITING = 3; // 等待。请求发送以后，收到响应序列中的第一条响应之前。
        private static final int RECVING = 4; // 接收。收到第一条响应后，收到最后一条响应之前。
        private static final int END = 5;   // 结束。最终状态。会话已成功结束（接收到最后一个响应）或者已失败（超时或其它失败原因）。

        private Session(int id, Handler requester, int reqSn, String reqId, String reqPara, int timeoutVal, String[][] rspIds, Object[] emulatedRsps){
            this.id = id;
            this.requester = requester;
            this.reqSn = reqSn;
            this.reqId = reqId;
            this.reqPara = reqPara;
            this.timeoutVal = timeoutVal;
            this.rspIds = rspIds;
            this.emulatedRsps = emulatedRsps;

            candidates = new HashMap<Integer, Integer>();
            if (null != rspIds) {
                for (int i = 0; i < rspIds.length; ++i) {
                    candidates.put(i, 0);
                }
            }
            state = IDLE;
        }

        synchronized String reqId(){
            return reqId;
        }
        synchronized String reqPara(){
            return reqPara;
        }
        synchronized String[] rspIds(){
            return null==rspIds ? null : rspIds[0];
        }
        synchronized Object[] rsps(){
            return emulatedRsps;
        }
    }

}
