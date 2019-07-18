package com.kedacom.vconf.sdk.base;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.util.Log;
import android.util.SparseIntArray;

import com.google.gson.Gson;

import java.util.LinkedHashSet;
import java.util.Set;


//@SuppressWarnings("UnusedReturnValue")
final class SessionFairy implements IFairy.ISessionFairy{

    private static final String TAG = SessionFairy.class.getSimpleName();

    private static SessionFairy instance;

    private Set<Session> sessions = new LinkedHashSet<>();

    private Handler reqHandler;
    private static final int MSG_ID_START_SESSION = 100;
    private static final int MSG_ID_FIN_DUE_TO_NO_RSP = 102;
    private static final int MSG_ID_TIMEOUT = 999;

    private MagicBook magicBook = MagicBook.instance();

    private Gson gson = new Gson();

    private ICrystalBall crystalBall;


    private Handler handler = new Handler(Looper.getMainLooper()){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case MSG_ID_TIMEOUT:
                    timeout((Session) msg.obj);
                    break;
                case MSG_ID_FIN_DUE_TO_NO_RSP:
                    Session s = (Session) msg.obj;
                    s.listener.onFinDueToNoRsp(s.reqName, s.reqSn, s.reqPara);
                    break;
            }
        }
    };


    private SessionFairy(){
        HandlerThread handlerThread = new HandlerThread("reqThr", Process.THREAD_PRIORITY_BACKGROUND);
        handlerThread.start();
        reqHandler = new Handler(handlerThread.getLooper()){
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case MSG_ID_START_SESSION:
                        startSession((Session) msg.obj);
                        break;
                }
            }
        };
    }

    synchronized static SessionFairy instance() {
        if (null == instance) {
            instance = new SessionFairy();
        }

        return instance;
    }



    @Override
    public void setCrystalBall(ICrystalBall crystalBall) {
        this.crystalBall = crystalBall;
    }


    @Override
    public boolean req(IListener listener, String reqName, int reqSn, Object... reqPara) {

        if (null == crystalBall){
            Log.e(TAG, "no crystalBall");
            return false;
        }
        if (null == listener){
            Log.e(TAG, "listener is null");
            return false;
        }
        if (null == reqPara){
            Log.e(TAG, "reqPara is null");
            return false;
        }

        if (!magicBook.isSession(reqName)){
            Log.e(TAG, "no such session request: "+reqName);
            return false;
        }

        // 检查参数合法性
        Class[] classes = magicBook.getUserParaClasses(reqName);
        if (reqPara.length < classes.length){ // 传入的请求参数个数不得小于期望的，但可以大于，多余的被弃掉。
            Log.e(TAG, String.format("invalid para nums for %s, expect #%s but got #%s", reqName, classes.length, reqPara.length));
            return false;
        }
        for(int i=0; i<classes.length; ++i){
            if (null != reqPara[i]
                    && classes[i] != reqPara[i].getClass()){
                Log.e(TAG, String.format("invalid para type for %s, expect %s but got %s", reqName, classes[i], reqPara[i].getClass()));
                return false;
            }
        }

        // 创建会话
        Session s = new Session(listener, reqSn, reqName, reqPara,magicBook.getTimeout(reqName) * 1000, magicBook.getRspSeqs(reqName));
        s.state = Session.READY;
        Message msg = Message.obtain();
        msg.what = MSG_ID_START_SESSION;
        msg.obj = s;
        reqHandler.sendMessage(msg);

        return true;
    }


    @Override
    public synchronized void cancelReq(int reqSn) {
        for (Session s : sessions){
            if (reqSn == s.reqSn) {
                if (Session.READY == s.state) {
                    reqHandler.removeMessages(MSG_ID_START_SESSION, s);
                }else {
                    handler.removeMessages(MSG_ID_TIMEOUT, s);
                }
                s.state = Session.END;
                sessions.remove(s);
                handler.post(() -> Log.d(TAG, String.format("%s<-~- (session %d CANCELED. req=%s)", s.id, s.id, s.reqName)));
                return;
            }
        }
    }

    @Override
    public synchronized boolean onMsg(String msgId, String msgContent) {
        String msgName = magicBook.getMsgName(msgId);

        if (!magicBook.isResponse(msgName)){
            Log.w(TAG, "Unknown response "+ msgName);
            return false;
        }

        for (final Session s : sessions) { // 查找期望该响应的会话
            if (Session.WAITING != s.state
                    && Session.RECVING != s.state) {
                continue;
            }

            SparseIntArray candidates = new SparseIntArray();
            boolean gotLast = false; // 是否匹配到会话的最后一条响应

            for (int i=0; i<s.candidates.size(); ++i) { // 在候选序列中查找所有能处理该响应的序列。
                int rspSeqIndx = s.candidates.keyAt(i);
                int rspIndx = s.candidates.get(rspSeqIndx);
                String[] candidateRspSeq = s.rspSeqs[rspSeqIndx];
                String candidateRsp = candidateRspSeq[rspIndx];
                if (msgName.equals(candidateRsp)){ // 找到了一路匹配的序列
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

            boolean bConsumed = s.listener.onRsp(gotLast, msgName,
                    gson.fromJson(msgContent, magicBook.getRspClazz(msgName)),
                    s.reqName, s.reqSn, s.reqPara);

            if (bConsumed){
                s.candidates = candidates; // 更新候选序列
                if (gotLast){
                    Log.d(TAG, String.format("%s<-~- %s(%s) (session %d FINISHED. req=%s) \n%s", s.id, msgName, msgId, s.id, s.reqName, msgContent));
                    handler.removeMessages(MSG_ID_TIMEOUT, s); // 移除定时器
                    s.state = Session.END; // 已获取到所有期待的响应，该会话结束
                    sessions.remove(s);
                }else{
                    Log.d(TAG, String.format("%s<-~- %s(%s) (session %d RECVING. req=%s) \n%s", s.id, msgName, msgId, s.id, s.reqName, msgContent));
                    s.state = Session.RECVING; // 已收到响应，继续接收后续响应
                }
            }

            return bConsumed;
        }

        return false;
    }


    /**
     * 启动会话
     * */
    private synchronized void startSession(Session s){

        // 用户参数转换为底层方法需要的参数
        Object[] paras = magicBook.userPara2MethodPara(s.reqPara, magicBook.getParaClasses(s.reqName));
        StringBuffer sb = new StringBuffer();
        for (int i=0; i<paras.length; ++i){
            sb.append(paras[i]).append(", ");
        }
        String methodName = magicBook.getMethod(s.reqName);
        Log.d(TAG, String.format("%s-~-> %s(%s) (session %d START) \nparas={%s}", s.id, s.reqName, methodName, s.id, sb));

        crystalBall.spell(magicBook.getMethodOwner(s.reqName),
                methodName,
                paras,
                magicBook.getParaClasses(s.reqName));

        if (null==s.rspSeqs || 0==s.rspSeqs.length){
            Log.d(TAG, String.format("%s<-~- (session %d NO RESPONSE. req=%s)", s.id, s.id, s.reqName));
            Message msg = Message.obtain();
            msg.what = MSG_ID_FIN_DUE_TO_NO_RSP;
            msg.obj = s;
            handler.sendMessage(msg);
            return;
        }

        s.state = Session.WAITING; // 请求已发出正在等待响应

        sessions.add(s);

        // 启动超时
        Message msg = Message.obtain();
        msg.what = MSG_ID_TIMEOUT;
        msg.obj = s;
        handler.sendMessageDelayed(msg, s.timeoutVal);
    }


    /**
     * 会话超时
     * */
    private synchronized void timeout(Session s){
        Log.d(TAG, String.format("%s<-~- (session %d TIMEOUT. req=%s)", s.id, s.id, s.reqName));
        s.state = Session.END; // 会话结束
        sessions.remove(s);
        // 通知用户请求超时
        s.listener.onTimeout(s.reqName, s.reqSn, s.reqPara);
    }


    /**
     * 会话 */
    private static final class Session{
        private static int count = 0;
        private final int id;   // 会话ID
        private final IListener listener;// 会话监听器
        private final int reqSn;        // 请求序列号。上层用来唯一标识一次请求，会话不使用不处理该字段，上报响应时带回给请求者。
        private final String reqName;   // 请求名称。
        private final Object[] reqPara;   // 请求参数。
        private final int timeoutVal;   // 超时时限。单位：毫秒
        private final String[][] rspSeqs;  // 响应名称序列组。一条请求可能对应多条响应序列，如reqXX——{{rsp1, rsp2},{rsp1,rsp3}}，一次会话只能匹配其中一条序列。
        private SparseIntArray candidates; // 候选的响应序列记录。记录当前可被用来匹配的响应序列组及各响应序列中的下一条待匹配响应（的位置）。“键”对应响应序列组的行下标，“值”对应列下标。每收到一条响应后该记录会更新。

        private int state;  // 会话状态
        private static final int IDLE = 0;  // 空闲。初始状态
        private static final int BLOCKING = 1;  // 阻塞。若存在未完成的同类会话（请求名称相同）则当前会话被阻塞直到同类会话结束。
        private static final int READY = 2;  // 就绪。请求待发送。
        private static final int WAITING = 3; // 等待。请求发送以后，收到响应序列中的第一条响应之前。
        private static final int RECVING = 4; // 接收。收到第一条响应后，收到最后一条响应之前。
        private static final int END = 5;   // 结束。最终状态。会话已成功结束（接收到最后一个响应）或者已失败（超时或其它失败原因）。

        private Session(IListener listener, int reqSn, String reqName, Object[] reqPara, int timeoutVal, String[][] rspSeqs){
            this.id = count++;
            this.listener = listener;
            this.reqSn = reqSn;
            this.reqName = reqName;
            this.reqPara = reqPara;
            this.timeoutVal = timeoutVal>0 ? timeoutVal : 5*1000;
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
