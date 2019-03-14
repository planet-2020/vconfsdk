package com.kedacom.vconf.sdk.datacollaborate;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;

import com.kedacom.vconf.sdk.base.KLog;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpPaint;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
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

    public DefaultPainter(Context context) {
        if (context instanceof LifecycleOwner){
            ((LifecycleOwner)context).getLifecycle().addObserver(new DefaultLifecycleObserver(){
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
    public boolean addPaintBoard(IPaintBoard paintBoard) {
        String boardId = paintBoard.getBoardId();
        if (paintBoards.containsKey(boardId)){
            KLog.p(KLog.ERROR,"board %s already exist!", boardId);
            return false;
        }
        DefaultPaintBoard defaultPaintBoard = (DefaultPaintBoard) paintBoard;
        defaultPaintBoard.setOnStateChangedListener(ROLE_AUTHOR==role ? onStateChangedListener : null);
        paintBoards.put(boardId, defaultPaintBoard);
        KLog.p(KLog.WARN,"board %s added", paintBoard.getBoardId());

        return true;
    }

    @Override
    public IPaintBoard deletePaintBoard(String boardId) {
        KLog.p(KLog.WARN,"delete board %s", boardId);
        DefaultPaintBoard board =  paintBoards.remove(boardId);
        if (null != board){
            if (board.getBoardId().equals(curBoardId)){
                curBoardId = null;
            }
            board.setOnStateChangedListener(null);
        }
        return board;
    }

    @Override
    public void deleteAllPaintBoards() {
        KLog.p(KLog.WARN,"delete all boards");
        for (DefaultPaintBoard board : paintBoards.values()){
            board.setOnStateChangedListener(null);
        }
        paintBoards.clear();
        curBoardId = null;
    }


    @Override
    public IPaintBoard switchPaintBoard(String boardId) {
        DefaultPaintBoard paintBoard = paintBoards.get(boardId);
        if(null == paintBoard){
            KLog.p(KLog.ERROR,"no such board %s", boardId);
            return null;
        }
        KLog.p(KLog.WARN, "switched board from %s to %s", curBoardId, boardId);
        curBoardId = boardId;

        return paintBoard;
    }

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


    private Runnable refreshRunnable = this::refresh;
    @Override
    public void paint(OpPaint op) {
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


    @Override
    public void setRole(int role) {
        this.role = role;
        for (DefaultPaintBoard board : paintBoards.values()){
            board.setOnStateChangedListener(ROLE_AUTHOR==role ? onStateChangedListener : null);
        }
    }


    private IOnBoardStateChangedListener onBoardStateChangedListener;
    @Override
    public void setOnBoardStateChangedListener(IOnBoardStateChangedListener onBoardStateChangedListener) {
        this.onBoardStateChangedListener = onBoardStateChangedListener;
    }


    private DefaultPaintBoard.IOnStateChangedListener onStateChangedListener = new DefaultPaintBoard.IOnStateChangedListener() {
        @Override
        public void onPaintOpGenerated(String boardId, OpPaint opPaint, boolean bNeedRefresh) {
            if (boardId.equals(curBoardId) && bNeedRefresh) {
                refresh();
            }

            if (null != opPaint && null != onBoardStateChangedListener){
                onBoardStateChangedListener.onPaintOpGenerated(boardId, opPaint);
            }
        }

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
