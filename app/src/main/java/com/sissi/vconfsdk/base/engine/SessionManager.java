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

final class SessionManager implements IRequestProcessor, IResponseProcessor {

    private static final String TAG = SessionManager.class.getSimpleName();

    private static SessionManager instance;

    private ArrayList<Session> sessions;  // 正常会话
    private ArrayList<Session> blockedSessions; // 被阻塞的会话

    private int sessionCnt = 0;
    private static final int MAX_SESSION_NUM = 2000; // 正常会话数上限
    private static final int MAX_BLOCKED_SESSION_NUM = 10000; // 被阻塞的会话数上限

    private static final int UI_REQ = -999;
    private Thread reqThread; // 发送请求线程
    private Handler reqHandler; // 请求handler
    private static final int MSG_TIMEOUT = 999;
    private Thread timeoutThread; // 超时线程
    private Handler timeoutHandler;

    private JsonProcessor jsonProcessor;

    private MessageRegister messageRegister;

    private NativeInteractor nativeInteractor;

    private SessionManager(){
        sessions = new ArrayList<Session>();
        blockedSessions = new ArrayList<Session>();

        jsonProcessor = JsonProcessor.instance();
        messageRegister = MessageRegister.instance();
        nativeInteractor = NativeInteractor.instance();

        initRequestThread();
        initTimeoutThread();
    }

    synchronized static SessionManager instance() {
        if (null == instance) {
            instance = new SessionManager();
        }

        return instance;
    }



    @Override
    public synchronized boolean processRequest(Handler requester, String reqId, Object reqPara, int reqSn) {

        if (null == requester){
            Log.e(TAG, "requester is null");
            return false;
        }

        if (!messageRegister.isRequest(reqId)){
            Log.e(TAG, "no such request: "+reqId);
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

        // 创建会话
        Session s = new Session(++sessionCnt, requester, reqSn, reqId, reqPara,
                messageRegister.getTimeout(reqId)*1000,
                messageRegister.getRsps(reqId), null);
        // 尝试发送请求
        if (!isReqExist){
            if (sessions.size() >= MAX_SESSION_NUM){
                Log.e(TAG, "requests reach the limit "+MAX_SESSION_NUM);
                return false;
            }
            sessions.add(s);
            Message msg = Message.obtain();
            msg.obj = s;
            reqHandler.sendMessage(msg);

            return true;

        }else{ // 存在未完成的同类请求（阻塞源）
            if (blockedSessions.size() >= MAX_BLOCKED_SESSION_NUM){
                Log.e(TAG, "blocked requests reach the limit "+MAX_BLOCKED_SESSION_NUM);
                return false;
            }
            blockedSessions.add(s);


            Log.w(TAG, String.format("-=-> (session %d BLOCKED) %s", s.id, s.reqId)); // XXX 启动超时？阻塞时间算在超时内？

            return true; // 返回真表示请求成功，不让外部感知请求被阻塞
        }

    }


    @Override
    public synchronized boolean processResponse(String rspName, String rspBody){

        if (!messageRegister.isResponse(rspName)){
            return false;
        }

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
                    if (rspName.equals(rspIds[j])){ // 找到了一路匹配的序列
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
                Log.i(TAG, String.format("<-=- (session %d FINISH) %s", s.id, rspName));
                timeoutHandler.removeMessages(MSG_TIMEOUT, s); // 移除定时器
                s.state = Session.END; // 已获取到所有期待的响应，该会话结束
                sessions.remove(s);
                rsp.obj = new ResponseBundle(rspName, jsonProcessor.fromJson(rspBody, messageRegister.getRspClazz(rspName)), ResponseBundle.RSP_FIN, s.reqId, s.reqSn);
                s.requester.sendMessage(rsp); // 上报该响应
                // 驱动被当前会话阻塞的会话
                driveBlockedSession(s.reqId);
            } else {
                Log.i(TAG, String.format("<-=- (session %d) %s", s.id, rspName));
                rsp.obj = new ResponseBundle(rspName, jsonProcessor.fromJson(rspBody, messageRegister.getRspClazz(rspName)), ResponseBundle.RSP, s.reqId, s.reqSn);
                s.requester.sendMessage(rsp); // 上报该响应
            }

            return true;
        }

        return false;
    }


    private void startSession(Session s){ // session自己有start方法, 通过session.start这种方式.

        nativeInteractor.request(s.reqId, jsonProcessor.toJson(s.reqPara)); // 真实的还是模拟的这层不感知

        Log.i(TAG, String.format("-=-> (session %d START) %s", s.id, s.reqId));
        if (null==s.rspIds || 0==s.rspIds.length){
            s.state = Session.END; // 请求没有响应，会话结束
            Log.i(TAG, String.format("<-=- (session %d FINISH) NO RESPONSE", s.id));
            sessions.remove(s);
            driveBlockedSession(s.reqId);

            return;
        }

        s.state = Session.WAITING; // 请求已发出正在等待响应

        // 启动超时
        Message msg = Message.obtain();
        msg.what = MSG_TIMEOUT;
        msg.obj = s;
        timeoutHandler.sendMessageDelayed(msg, s.timeoutVal);
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

        startSession(bs);
        if (Session.END == bs.state){
            sessions.remove(bs);
            driveBlockedSession(bs.reqId);
        }

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
     * 初始化发送请求线程
     * */
    private void initRequestThread(){
        final Object lock = new Object();
        reqThread = new Thread(){
            @SuppressLint("HandlerLeak") // 或者, 可以使用Handler.Callback替代在Thread中定义内部类Handler以避免"HandlerLeak"问题，尤其在主线程中建议如此。
            @Override
            public void run() {
                Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                Looper.prepare();

                reqHandler = new Handler(){
                    @Override
                    public void handleMessage(Message msg) {
                        startSession((Session) msg.obj);
                    }
                };
                synchronized (lock){lock.notify();}

                Looper.loop();
            }
        };

        reqThread.setName("SM.processRequest");

        reqThread.start();

        if (null == reqHandler){
            synchronized (lock) {
                try {
                    lock.wait(); // 保证thread初始化结束后handler立即可用。
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
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
        private final Object reqPara;   // 请求参数。
        private final int timeoutVal;   // 超时时限。单位：毫秒
        private final String[][] rspIds;  // 响应Id序列组。一条请求可能对应多条响应序列，如{{rsp1, rsp2},{rsp1,rsp3}}，一次会话只能对应其中一条序列。
        private HashMap<Integer, Integer> candidates; // 候选的响应序列记录。记录当前可被用来匹配的响应序列及起始匹配位置。“键”对应响应Id序列组rspIds的行下标，“值”对应rspIds的列下标。每收到一条响应后该记录会更新。
        private final Object[] emulatedRsps;     // 模拟响应。仅用于模拟模式。

        private int state;  // 会话状态
        private static final int IDLE = 2;  // 空闲。初始状态
        private static final int WAITING = 3; // 等待。请求发送以后，收到响应序列中的第一条响应之前。
        private static final int RECVING = 4; // 接收。收到第一条响应后，收到最后一条响应之前。
        private static final int END = 5;   // 结束。最终状态。会话已成功结束（接收到最后一个响应）或者已失败（超时或其它失败原因）。

        private Session(int id, Handler requester, int reqSn, String reqId, Object reqPara, int timeoutVal, String[][] rspIds, Object[] emulatedRsps){
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
        synchronized String[] rspIds(){
            return null==rspIds ? null : rspIds[0];
        }
        synchronized Object[] rsps(){
            return emulatedRsps;
        }
    }

}
