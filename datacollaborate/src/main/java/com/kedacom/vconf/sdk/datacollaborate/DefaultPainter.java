package com.kedacom.vconf.sdk.datacollaborate;

import android.content.Context;
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

    private Map<String, DefaultPaintBoard> paintBoards = new LinkedHashMap<>();

    private String curBoardId;

    private boolean bDirty = false;
    private boolean bPaused = false;
    private final Object renderLock = new Object();


    private DefaultPaintBoard.IOnPaintOpGeneratedListener onPaintOpGeneratedListener = new DefaultPaintBoard.IOnPaintOpGeneratedListener() {
        @Override
        public void onOp(OpPaint opPaint) {
            refresh();
        }
    };


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
                    clean();
                }
            });
        }

    }

    @Override
    public void start() {
        if (!renderThread.isAlive()){
            renderThread.start();
        }
    }

    @Override
    public void pause() {
        synchronized (renderLock) {
            bPaused = true;
        }
    }

    @Override
    public void resume() {
        synchronized (renderLock) {
            bPaused = false;
        }
        refresh();
    }

    @Override
    public void stop() {
        if (renderThread.isAlive()) {
            renderThread.interrupt();
        }
    }


    private void clean(){
        deleteAllPaintBoards();
    }

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
        defaultPaintBoard.setOnPaintOpGeneratedListener(onPaintOpGeneratedListener);
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
            board.setOnPaintOpGeneratedListener(null);
            board.clean();  // XXX 按理说不应该在删除时清理board，删除后再加载应仍能使用
        }
        return board;
    }

    @Override
    public void deleteAllPaintBoards() {
        KLog.p(KLog.WARN,"delete all boards");
        for (DefaultPaintBoard board : paintBoards.values()){
            board.setOnPaintOpGeneratedListener(null);
            board.clean();  // XXX 按理说不应该在删除时清理board，删除后再加载应仍能使用
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

    @Override
    public void paint(OpPaint op) {
        String boardId = op.getBoardId();
        DefaultPaintBoard paintBoard = paintBoards.get(boardId);
        if(null == paintBoard){
            KLog.p(KLog.ERROR,"no board %s for op %s", boardId, op);
            return;
        }
        if (paintBoard.onPaintOp(op)){
            refresh();
        }
    }

    @Override
    public void batchPaint(List<OpPaint> ops){
        boolean bRefresh = false;
        for (OpPaint op : ops){
            String boardId = op.getBoardId();
            DefaultPaintBoard paintBoard = paintBoards.get(boardId);
            if(null == paintBoard){
                KLog.p(KLog.WARN,"no board %s for op %s", boardId, op);
                continue;
            }
            if (paintBoard.onPaintOp(op)
                    && boardId.equals(curBoardId)){
                bRefresh = true;
            }
        }
        if (bRefresh) {
            refresh();
        }
    }



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
