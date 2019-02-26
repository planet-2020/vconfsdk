package com.kedacom.vconf.sdk.datacollaborate;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.os.Process;

import com.kedacom.vconf.sdk.base.KLog;
import com.kedacom.vconf.sdk.datacollaborate.bean.EOpType;
import com.kedacom.vconf.sdk.datacollaborate.bean.IRepealable;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpDeletePic;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpDraw;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpDrawOval;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpDrawRect;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpErase;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpRectErase;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpInsertPic;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpDrawLine;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpMatrix;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpPaint;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpDragPic;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpUpdatePic;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpDrawPath;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

public class DefaultPainter implements IPainter {

    private Context context;

    private Map<String, DefaultPaintBoard> paintBoards = new LinkedHashMap<>();

    private String curBoardId;

    private boolean bNeedRender = false;

    private boolean bPaused = false;

    // 调整中的操作。比如画线时，从手指按下到手指拿起之间的绘制都是“调整中”的。
    private DefaultPaintBoard.IOnPaintOpGeneratedListener onPaintOpGeneratedListener = new DefaultPaintBoard.IOnPaintOpGeneratedListener() {
        @Override
        public void onOp(OpPaint opPaint) {
            refresh();
        }
    };


    public DefaultPainter(Context context) {

        this.context = context;

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
        if (renderThread.isAlive()){
            return;
        }
        renderThread.start();
    }

    @Override
    public void pause() {
        bPaused = true;
    }

    @Override
    public void resume() {
        bPaused = false;
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
        synchronized (renderThread) {
            bNeedRender = true;
            if (Thread.State.WAITING == renderThread.getState()) {
//                KLog.p(KLog.WARN, "notify");
                renderThread.notify();
            }
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
        if (boardId.equals(curBoardId)){
            curBoardId = null;
        }
        DefaultPaintBoard board =  paintBoards.remove(boardId);
        if (null != board){
            board.clean();
        }
        return board;
    }

    @Override
    public void deleteAllPaintBoards() {
        KLog.p(KLog.WARN,"delete all boards");
        for (String boardId : paintBoards.keySet()){
            paintBoards.get(boardId).clean();
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

        @Override
        public void run() {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

            while (true){
//                KLog.p("start loop run");
                if (isInterrupted()){
                    KLog.p(KLog.WARN, "quit renderThread");
                    return;
                }

                // 判断当前是否有渲染任务
                synchronized (this) {
                    try {
                        if (!bNeedRender) {
//                            KLog.p("waiting...");
                            wait();
//                            KLog.p("resume run");
                        }
                        bNeedRender = false;
                        if (bPaused){
//                            KLog.p("paused");
                            continue;
                        }
                    } catch (InterruptedException e) {
                        KLog.p(KLog.WARN, "quit renderThread");
                        return;
                    }
                }

                // 获取当前画板
                DefaultPaintBoard paintBoard = paintBoards.get(curBoardId);
                if (null == paintBoard){
                    continue;
                }

                synchronized (this) {
                    bNeedRender = false;
                }

                paintBoard.paint();

            }
        }



    };





}
