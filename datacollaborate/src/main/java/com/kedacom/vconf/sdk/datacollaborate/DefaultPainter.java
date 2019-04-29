package com.kedacom.vconf.sdk.datacollaborate;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;

import com.kedacom.vconf.sdk.base.IResultListener;
import com.kedacom.vconf.sdk.base.KLog;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpPaint;
import com.kedacom.vconf.sdk.datacollaborate.bean.PainterInfo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

public class DefaultPainter implements IPainter {

    private int role = ROLE_COPIER;

    private Map<String, DefaultPaintBoard> paintBoards = new LinkedHashMap<>();

    private String curBoardId;

    private boolean bDirty = false;
    private boolean bPaused = false;
    private final Object renderLock = new Object();

    private HandlerThread handlerThread;
    private Handler handler;

    private PainterInfo painterInfo;
    private IPaintBoard.Config defaultBoardCfg;

    public DefaultPainter(@NonNull Context context, @NonNull PainterInfo painterInfo, LifecycleOwner lifecycleOwner) {

        this.painterInfo = painterInfo;

        if (null != lifecycleOwner){
            lifecycleOwner.getLifecycle().addObserver(new DefaultLifecycleObserver(){
                @Override
                public void onCreate(@NonNull LifecycleOwner owner) {
                    KLog.p("LifecycleOwner %s created", owner);
                    start();
                }

                @Override
                public void onResume(@NonNull LifecycleOwner owner) {
                    KLog.p("LifecycleOwner %s resumed", owner);
                    resume();
                }

                @Override
                public void onPause(@NonNull LifecycleOwner owner) {
                    KLog.p("LifecycleOwner %s to be paused", owner);
                    pause();
                }

                @Override
                public void onDestroy(@NonNull LifecycleOwner owner) {
                    KLog.p("LifecycleOwner %s to be destroyed", owner);
                    stop();
                }
            });
        }


        handlerThread = new HandlerThread("PainterAss", Process.THREAD_PRIORITY_BACKGROUND);
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
    }

    @Override
    public void start() {
        if (!renderThread.isAlive()){
            renderThread.start();
        }
    }

    /**
     * 调用该方法暂停绘制
     * */
    @Override
    public void pause() {
        synchronized (renderLock) {
            bPaused = true;
        }
    }

    /**
     * 调用{@link #pause()}后再调用该方法恢复正常运行状态。
     * */
    @Override
    public void resume() {
        synchronized (renderLock) {
            bPaused = false;
        }
        refresh();
    }

    /**
     * 调用该接口后painter释放了所有资源，已无用，不可再调用start恢复。
     * */
    @Override
    public void stop() {
        if (renderThread.isAlive()) {
            renderThread.interrupt();
        }
        handler.removeCallbacksAndMessages(null);
        handlerThread.quit();
        deleteAllPaintBoards();
    }

    /**
     * 刷新画板
     * */
    private void refresh(){
        synchronized (renderLock){
            bDirty = true;
            renderLock.notify();
        }
    }


    @Override
    public boolean addPaintBoard(@NonNull IPaintBoard paintBoard) {
        String boardId = paintBoard.getBoardId();
        if (paintBoards.containsKey(boardId)){
            KLog.p(KLog.ERROR,"board %s already exist!", boardId);
            return false;
        }
        DefaultPaintBoard defaultPaintBoard = (DefaultPaintBoard) paintBoard;
        if (null != defaultBoardCfg){
            defaultPaintBoard.getConfig().set(defaultBoardCfg);
        }
        defaultPaintBoard.setOnPaintOpGenerated(ROLE_AUTHOR==role ? onPaintOpGeneratedListener : null);
        defaultPaintBoard.setOnStateChangedListener(onStateChangedListener);
        paintBoards.put(boardId, defaultPaintBoard);
        KLog.p(KLog.WARN,"board %s added", paintBoard.getBoardId());

        return true;
    }

    @Nullable
    @Override
    public IPaintBoard deletePaintBoard(@NonNull String boardId) {
        KLog.p(KLog.WARN,"delete board %s", boardId);
        DefaultPaintBoard board =  paintBoards.remove(boardId);
        if (null != board){
            if (board.getBoardId().equals(curBoardId)){
                curBoardId = null;
            }
            board.setOnPaintOpGenerated(null);
            board.setOnStateChangedListener(null);
        }
        return board;
    }

    @Override
    public void deleteAllPaintBoards() {
        KLog.p(KLog.WARN,"delete all boards");
        for (DefaultPaintBoard board : paintBoards.values()){
            board.setOnPaintOpGenerated(null);
            board.setOnStateChangedListener(null);
        }
        paintBoards.clear();
        curBoardId = null;
    }


    @Nullable
    @Override
    public IPaintBoard switchPaintBoard(@NonNull String boardId) {
        DefaultPaintBoard paintBoard = paintBoards.get(boardId);
        if(null == paintBoard){
            KLog.p(KLog.ERROR,"no such board %s", boardId);
            return null;
        }
        KLog.p(KLog.WARN, "switched board from %s to %s", curBoardId, boardId);
        curBoardId = boardId;

        return paintBoard;
    }

    @Nullable
    @Override
    public IPaintBoard getPaintBoard(String boardId) {
        return paintBoards.get(boardId);
    }

    /**
     * 获取所有画板
     * @return 所有画板列表，顺序同添加时的顺序。
     * */
    @Override
    public List<IPaintBoard> getAllPaintBoards() {
        List<IPaintBoard> boards = new ArrayList<>();
        boards.addAll(paintBoards.values());
        return  boards;
    }

    @Nullable
    @Override
    public IPaintBoard getCurrentPaintBoard(){
        if (null == curBoardId) {
            KLog.p(KLog.WARN, "current board is null");
            return null;
        }
        return paintBoards.get(curBoardId);
    }

    @Override
    public int getPaintBoardCount() {
        return paintBoards.size();
    }

    /**
     * @param config 针对所有下辖画板的配置
     * NOTE: sdk不持有该传入参数，后续用户修改该对象不会影响配置。
     * */
    @Override
    public void setBoardConfig(IPaintBoard.Config config) {
        defaultBoardCfg = config;
        if (null != config) {
            for (DefaultPaintBoard board : paintBoards.values()) {
                board.getConfig().set(config);
            }
        }
    }


    private Runnable refreshRunnable = this::refresh;
    @Override
    public void paint(@NonNull OpPaint op) {
        String boardId = op.getBoardId();
        DefaultPaintBoard paintBoard = paintBoards.get(boardId);
        if(null == paintBoard){
            KLog.p(KLog.ERROR,"no board %s for op %s", boardId, op);
            return;
        }
        if (paintBoard.onPaintOp(op) && boardId.equals(curBoardId)){
            handler.removeCallbacks(refreshRunnable);
            handler.postDelayed(refreshRunnable, 50);
        }
    }

    private DefaultPaintBoard.IOnPaintOpGeneratedListener onPaintOpGeneratedListener = new DefaultPaintBoard.IOnPaintOpGeneratedListener() {
        @Override
        public void onPaintOpGenerated(String boardId, OpPaint Op, IResultListener publishResultListener, boolean bNeedRefresh) {
            if (boardId.equals(curBoardId) && bNeedRefresh) {
                refresh();
            }

//            if (bNeedRefresh){
//                if (null != onBoardStateChangedListener) onBoardStateChangedListener.onChanged(boardId);
//            }

            if (null != Op){
                Op.setAuthorE164(painterInfo.getE164());
                if (null != onBoardStateChangedListener) onBoardStateChangedListener.onPaintOpGenerated(boardId, Op, publishResultListener);
            }
        }
    };
    @Override
    public void setRole(int role) {
        this.role = role;
        for (DefaultPaintBoard board : paintBoards.values()){
            board.setOnPaintOpGenerated(ROLE_AUTHOR==role ? onPaintOpGeneratedListener : null);
        }
    }


    private IOnBoardStateChangedListener onBoardStateChangedListener;
    @Override
    public void setOnBoardStateChangedListener(IOnBoardStateChangedListener onBoardStateChangedListener) {
        this.onBoardStateChangedListener = onBoardStateChangedListener;
    }


    private DefaultPaintBoard.IOnStateChangedListener onStateChangedListener = new DefaultPaintBoard.IOnStateChangedListener() {

        @Override
        public void onChanged(String boardId) {
            if (null != onBoardStateChangedListener) onBoardStateChangedListener.onChanged(boardId);
        }

        @Override
        public void onPictureCountChanged(String boardId, int count) {
            if (null != onBoardStateChangedListener) onBoardStateChangedListener.onPictureCountChanged(boardId, count);
        }

        @Override
        public void onZoomRateChanged(String boardId, int percentage) {
            if (null != onBoardStateChangedListener) onBoardStateChangedListener.onZoomRateChanged(boardId, percentage);
        }

        @Override
        public void onRepealableStateChanged(String boardId, int repealedOpsCount, int remnantOpsCount) {
            if (null != onBoardStateChangedListener) onBoardStateChangedListener.onRepealableStateChanged(boardId, repealedOpsCount, remnantOpsCount);
        }

        @Override
        public void onWcRevocableStateChanged(String boardId, int revocableOpsCount, int restorableOpsCount) {
            if (null != onBoardStateChangedListener) onBoardStateChangedListener.onWcRevocableStateChanged(boardId, revocableOpsCount, restorableOpsCount);
        }

        @Override
        public void onEmptyStateChanged(String boardId, boolean bEmptied) {
            if (null != onBoardStateChangedListener) onBoardStateChangedListener.onEmptyStateChanged(boardId, bEmptied);
        }
    };



    private final Thread renderThread = new Thread("DCRenderThr"){
        private boolean bNeedRender;

        @Override
        public void run() {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

            while (true){
                KLog.p("start loop run");
                if (isInterrupted()){
                    KLog.p(KLog.WARN, "quit renderThread");
                    return;
                }

                // 判断当前是否有渲染任务
                synchronized (renderLock) {
                    bNeedRender = bDirty && !bPaused;
                    try {
                        if (!bNeedRender) {
                            do{
                                KLog.p("waiting...");
                                renderLock.wait();
                                bNeedRender = bDirty && !bPaused;
                                KLog.p("awaken! bNeedRender=%s", bNeedRender);
                            }while (!bNeedRender);
                        }
                    } catch (InterruptedException e) {
                        KLog.p(KLog.WARN, "quit renderThread");
                        return;
                    }

                    bDirty = false;

                }

                // 获取当前画板
                DefaultPaintBoard paintBoard = paintBoards.get(curBoardId);
                if (null != paintBoard){
                    paintBoard.paint();
                }

            }
        }


    };


}
