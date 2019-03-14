package com.kedacom.vconf.sdk.datacollaborate;

import android.content.Context;
import android.content.res.AssetManager;
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
import android.graphics.SurfaceTexture;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Process;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;
import android.widget.FrameLayout;

import com.kedacom.vconf.sdk.base.KLog;
import com.kedacom.vconf.sdk.datacollaborate.bean.BoardInfo;
import com.kedacom.vconf.sdk.datacollaborate.bean.EOpType;
import com.kedacom.vconf.sdk.datacollaborate.bean.IBoundary;
import com.kedacom.vconf.sdk.datacollaborate.bean.IRepealable;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpClearScreen;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpDeletePic;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpDragPic;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpDraw;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpDrawLine;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpDrawOval;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpDrawPath;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpDrawRect;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpErase;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpInsertPic;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpMatrix;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpPaint;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpRectErase;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpRedo;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpUndo;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpUpdatePic;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class DefaultPaintBoard extends FrameLayout implements IPaintBoard{

    // 画板matrix
    private Matrix boardMatrix = new Matrix();

    /* 相对于xhdpi的屏幕密度。
    因为TL和网呈已经实现数据协作在先，他们传过来的是原始的像素值，
    为了使得展示效果尽量在各设备上保持一致并兼顾TL和网呈已有实现，我们以TL的屏幕密度为基准算出一个相对密度，
    以该相对密度作为缩放因子进行展示。TL的屏幕密度接近xhdpi，故以xhdpi作为基准*/
    private float relativeDensity=1;

    // 图形层。用于图形绘制如画线、画圈、擦除等等
    private TextureView shapePaintView;
    // 调整中的图形操作。比如画线时，从手指按下到手指拿起之间的绘制都是“调整中”的。
    private OpPaint adjustingShapeOp;
    private final Object adjustingOpLock = new Object();
    // 临时图形操作。手指拿起绘制完成，但并不表示此绘制已生效，需等到平台广播NTF后方能确认为生效的操作，在此之前的操作都作为临时操作保存在这里。
    private MyConcurrentLinkedDeque<OpPaint> tmpShapeOps = new MyConcurrentLinkedDeque<>();
    // 图形操作。已经平台NTF确认过的操作。
    private MyConcurrentLinkedDeque<OpPaint> shapeOps = new MyConcurrentLinkedDeque<>();
    // 被撤销的图形操作。撤销只针对已经平台NTF确认过的操作。
    private Stack<OpPaint> repealedShapeOps = new Stack<>();

    // 图片层。用于绘制图片。
    private TextureView picPaintView;
    // 图片操作。
    private MyConcurrentLinkedDeque<OpPaint> picOps = new MyConcurrentLinkedDeque<>();

    // 图片编辑层。
    private TextureView picEditPaintView;
    // 图片编辑层缩放及位移
    private Matrix picEditViewMatrix = new Matrix();
    // 图片编辑操作
    private MyConcurrentLinkedDeque<PicEditStuff> picEditStuffs = new MyConcurrentLinkedDeque<>();
    // 删除图片按钮
    private Bitmap del_pic_icon;
    private Bitmap del_pic_active_icon;

    // 图层
    static int LAYER_NONE = 100;
    static int LAYER_PIC =  101;
    static int LAYER_SHAPE =102;
    static int LAYER_PIC_TMP =103;
    static int LAYER_PIC_AND_SHAPE =104;
    static int LAYER_ALL =  109;
    private int focusedLayer = LAYER_ALL;

    // 工具
    private int tool = TOOL_PENCIL;

    // 画笔粗细。单位：pixel
    private int paintStrokeWidth = 5;

    // 画笔颜色
    private long paintColor = 0xFFFFFFFFL;

    // 橡皮擦尺寸。单位：pixel
    private int eraserSize = 25;

    // 放缩比例上下限
    private float minZoomRate = 0.1f;
    private float maxZoomRate = 10f;

    // 画板状态监听器
    private IOnStateChangedListener onStateChangedListener;

    // 画板信息
    private BoardInfo boardInfo;

    private DefaultTouchListener boardViewTouchListener;
    private DefaultTouchListener shapeViewTouchListener;
    private DefaultTouchListener picViewTouchListener;
    private DefaultTouchListener tmpPicViewTouchListener;


    private Handler handler = new Handler(Looper.getMainLooper());

    private final Runnable finishEditPicRunnable = () -> {
        KLog.p("edit picture timeout! picEditStuffs.isEmpty? %s", picEditStuffs.isEmpty());
        if (!picEditStuffs.isEmpty()) {
            finishEditPic(picEditStuffs.pollFirst());
        }
    };

    private static Handler assHandler;
    static {
        HandlerThread handlerThread = new HandlerThread("BoardAss", Process.THREAD_PRIORITY_BACKGROUND);
        handlerThread.start();
        assHandler = new Handler(handlerThread.getLooper());
    }

    public DefaultPaintBoard(@NonNull Context context, BoardInfo boardInfo) {
        super(context);

        relativeDensity = context.getResources().getDisplayMetrics().density/2;

        this.boardInfo = boardInfo;

        LayoutInflater layoutInflater = LayoutInflater.from(context);
        View whiteBoard = layoutInflater.inflate(R.layout.default_whiteboard_layout, this);
        picPaintView = whiteBoard.findViewById(R.id.pb_pic_paint_view);
        picPaintView.setOpaque(false);
        shapePaintView = whiteBoard.findViewById(R.id.pb_shape_paint_view);
        shapePaintView.setOpaque(false);
        picEditPaintView = whiteBoard.findViewById(R.id.pb_tmp_paint_view);
        picEditPaintView.setOpaque(false);

        shapePaintView.setSurfaceTextureListener(surfaceTextureListener);
        picPaintView.setSurfaceTextureListener(surfaceTextureListener);
        picEditPaintView.setSurfaceTextureListener(surfaceTextureListener);

        shapeViewTouchListener = new DefaultTouchListener(context, shapeViewEventListener);
        picViewTouchListener = new DefaultTouchListener(context, picViewEventListener);
        tmpPicViewTouchListener = new DefaultTouchListener(context, tmpPicViewEventListener);
        boardViewTouchListener = new DefaultTouchListener(context, boardViewEventListener);
        picPaintView.setOnTouchListener( picViewTouchListener);
        shapePaintView.setOnTouchListener(shapeViewTouchListener);
        picEditPaintView.setOnTouchListener(tmpPicViewTouchListener);

        try {
            AssetManager am = context.getAssets();
            InputStream is = am.open("del_pic.png");
            del_pic_icon = BitmapFactory.decodeStream(is);
            is.close();
            is = am.open("del_pic_active.png");
            del_pic_active_icon = BitmapFactory.decodeStream(is);
            is.close();
            Matrix matrix = new Matrix();
            float density = context.getResources().getDisplayMetrics().density;
            matrix.postScale(density/2, density/2);
            del_pic_icon = Bitmap.createBitmap(del_pic_icon, 0, 0,
                    del_pic_icon.getWidth(), del_pic_icon.getHeight(), matrix, true);
            del_pic_active_icon = Bitmap.createBitmap(del_pic_active_icon, 0, 0,
                    del_pic_active_icon.getWidth(), del_pic_active_icon.getHeight(), matrix, true);
        } catch (IOException e) {
            e.printStackTrace();
        }

        setBackgroundColor(Color.DKGRAY);

    }

    public DefaultPaintBoard(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }


    // 画板未加载时保存画板内容使用的画板宽高值
    private static int boardWidth = 1920;
    private static int boardHeight = 1080;
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (0!=w)  boardWidth = w;
        if (0!=h)  boardHeight = h;
    }


    private TextureView.SurfaceTextureListener surfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            KLog.p("surface available");
            // 刷新
            if (null != onStateChangedListener) onStateChangedListener.onPaintOpGenerated(getBoardId(), null, true);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            KLog.p("surface size changed");
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            KLog.p("surface destroyed");
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        }
    };


    private Matrix densityRelativeBoardMatrix = new Matrix();
    private Matrix getDensityRelativeBoardMatrix(){
        densityRelativeBoardMatrix.reset();
        densityRelativeBoardMatrix.postScale(relativeDensity, relativeDensity);
        densityRelativeBoardMatrix.postConcat(boardMatrix);
        return  densityRelativeBoardMatrix;
    }


    private boolean isExistEditingPic(String picId){
        for (PicEditStuff picEditStuff : picEditStuffs){
            OpInsertPic op = picEditStuff.pic;
            if (picId.equals(op.getPicId())) {
                return true;
            }
        }
        return false;
    }

    private void delEditingPic(String picId){
        handler.removeCallbacks(finishEditPicRunnable);
        Iterator<PicEditStuff> it = picEditStuffs.iterator();
        while (it.hasNext()) {
            PicEditStuff picEditStuff = it.next();
            OpInsertPic op = picEditStuff.pic;
            if (picId.equals(op.getPicId())) {
                it.remove();
                if (picEditStuffs.isEmpty()) {
                    // 如果最后一张正在编辑的图片被删除则重置画板到编辑前的状态
                    focusedLayer = savedLayerBeforeEditPic;
                    picEditViewMatrix.reset();
                }
                return;
            }
        }
    }


    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (null == onStateChangedListener){
            return true;
        }

        if (LAYER_ALL == focusedLayer){
            boardViewTouchListener.onTouch(this, ev);
            boolean ret2 = picPaintView.dispatchTouchEvent(ev);
            boolean ret1 = shapePaintView.dispatchTouchEvent(ev);
            boolean ret3 = picEditPaintView.dispatchTouchEvent(ev);
            return ret1||ret2||ret3;
        } else if (LAYER_PIC_TMP == focusedLayer){
            return picEditPaintView.dispatchTouchEvent(ev);
        } else if (LAYER_NONE == focusedLayer){
            return true;
        }else if (LAYER_PIC == focusedLayer){
            boardViewTouchListener.onTouch(this, ev);
            return picPaintView.dispatchTouchEvent(ev);
        }else if (LAYER_SHAPE == focusedLayer){
            boardViewTouchListener.onTouch(this, ev);
            return shapePaintView.dispatchTouchEvent(ev);
        }else if (LAYER_PIC_AND_SHAPE == focusedLayer){
            boardViewTouchListener.onTouch(this, ev);
            boolean ret2 = picPaintView.dispatchTouchEvent(ev);
            boolean ret1 = shapePaintView.dispatchTouchEvent(ev);
            return ret1||ret2;
        }

        return false;
    }

    DefaultTouchListener.IOnEventListener boardViewEventListener = new DefaultTouchListener.IOnEventListener(){
        private float scaleCenterX, scaleCenterY;

        @Override
        public void onMultiFingerDragBegin() {
        }

        @Override
        public void onMultiFingerDrag(float dx, float dy) {
            boardMatrix.postTranslate(dx, dy);
            if (null != onStateChangedListener) onStateChangedListener.onPaintOpGenerated(getBoardId(),null, true);
        }

        @Override
        public void onMultiFingerDragEnd() {
            OpMatrix opMatrix = new OpMatrix(boardMatrix);
            assignBasicInfo(opMatrix);
            if (null != onStateChangedListener) onStateChangedListener.onPaintOpGenerated(getBoardId(),opMatrix, true);
        }

        @Override
        public void onScaleBegin() {
            scaleCenterX = getWidth()/2;
            scaleCenterY = getHeight()/2;
        }

        @Override
        public void onScale(float factor) {
            float curZoomRate = MatrixHelper.getScale(boardMatrix);
            float zoomRate = curZoomRate * factor;
            if (zoomRate < minZoomRate) {
                boardMatrix.postScale(minZoomRate/curZoomRate, minZoomRate/curZoomRate, scaleCenterX, scaleCenterY);
            }else if (zoomRate > maxZoomRate){
                boardMatrix.postScale(maxZoomRate/curZoomRate, maxZoomRate/curZoomRate, scaleCenterX, scaleCenterY);
            }else {
                boardMatrix.postScale(factor, factor, scaleCenterX, scaleCenterY);
            }
            if (null != onStateChangedListener) onStateChangedListener.onPaintOpGenerated(getBoardId(),null, true);
            zoomRateChanged();
        }

        @Override
        public void onScaleEnd() {
            OpMatrix opMatrix = new OpMatrix(boardMatrix);
            assignBasicInfo(opMatrix);
            if (null != onStateChangedListener) onStateChangedListener.onPaintOpGenerated(getBoardId(),opMatrix, true);
        }
    };


    DefaultTouchListener.IOnEventListener shapeViewEventListener = new DefaultTouchListener.IOnEventListener(){

        @Override
        public void onDragBegin(float x, float y) {
            createShapeOp(x, y);
        }

        @Override
        public void onDrag(float x, float y) {
            adjustShapeOp(x, y);
            if (null != onStateChangedListener) onStateChangedListener.onPaintOpGenerated(getBoardId(), null, true);
        }

        @Override
        public void onDragEnd() {
            confirmShapeOp();
            tmpShapeOps.offerLast(adjustingShapeOp);
            if (null != onStateChangedListener) onStateChangedListener.onPaintOpGenerated(getBoardId(), adjustingShapeOp, true);
            adjustingShapeOp = null;
        }

    };


    DefaultTouchListener.IOnEventListener picViewEventListener = new DefaultTouchListener.IOnEventListener(){
        @Override
        public boolean onDown(float x, float y) {
            if (picOps.isEmpty()){
                return false; // 当前没有图片不用处理后续事件
            }
            return true;
        }


        @Override
        public void onLongPress(float x, float y) {
            OpInsertPic opInsertPic = selectPic(x, y);
            if (null == opInsertPic){
                KLog.p("no pic selected(x=%s, y=%s)", x, y);
                return;
            }
            picOps.remove(opInsertPic);
            // 选中图片是所见即所得的效果，所以需要把图片层的matrix拷贝到图片编辑层
            picEditViewMatrix.set(getDensityRelativeBoardMatrix());
            savedMatrixBeforeEditPic.set(picEditViewMatrix);
            editPic(opInsertPic);
        }
    };


    private float[] mapPoint= new float[2];
    private Matrix invertedDensityRelativeBoardMatrix;
    private void createShapeOp(float startX, float startY){
        invertedDensityRelativeBoardMatrix = MatrixHelper.invert(getDensityRelativeBoardMatrix());
//        KLog.p("invert success?=%s, orgX=%s, orgY=%s", suc, x, y);
        mapPoint[0] = startX;
        mapPoint[1] = startY;
        invertedDensityRelativeBoardMatrix.mapPoints(mapPoint);
        float x = mapPoint[0];
        float y = mapPoint[1];
//            KLog.p("startX=%s, startY=%s, shapeScaleX=%s, shapeScaleY=%s", startX, startY, shapeScaleX, shapeScaleY);
        switch (tool){
            case TOOL_PENCIL:
                OpDrawPath opDrawPath = new OpDrawPath(new ArrayList<>());
                opDrawPath.getPoints().add(new PointF(x, y));
                opDrawPath.getPath().moveTo(x, y);
                adjustingShapeOp = opDrawPath;
                break;
            case TOOL_LINE:
                OpDrawLine opDrawLine = new OpDrawLine();
                opDrawLine.setStartX(x);
                opDrawLine.setStartY(y);
                adjustingShapeOp = opDrawLine;
                break;
            case TOOL_RECT:
                OpDrawRect opDrawRect = new OpDrawRect();
                opDrawRect.setLeft(x);
                opDrawRect.setTop(y);
                adjustingShapeOp = opDrawRect;
                break;
            case TOOL_OVAL:
                OpDrawOval opDrawOval = new OpDrawOval();
                opDrawOval.setLeft(x);
                opDrawOval.setTop(y);
                adjustingShapeOp = opDrawOval;
                break;
            case TOOL_ERASER:
                OpErase opErase = new OpErase(eraserSize, eraserSize, new ArrayList<>());
                opErase.getPoints().add(new PointF(x, y));
                opErase.getPath().moveTo(x, y);
                adjustingShapeOp = opErase;
                break;
            case TOOL_RECT_ERASER:
                // 矩形擦除先绘制一个虚线矩形框选择擦除区域
                OpDrawRect opDrawRect1 = new OpDrawRect();
                opDrawRect1.setLeft(x);
                opDrawRect1.setTop(y);
                adjustingShapeOp = opDrawRect1;
                break;
            default:
                KLog.p(KLog.ERROR, "unknown TOOL %s", tool);
                return;
        }
        if (adjustingShapeOp instanceof OpDraw){
            OpDraw opDraw = (OpDraw) adjustingShapeOp;
            if (TOOL_ERASER == tool){
                opDraw.setStrokeWidth(eraserSize);
            }else if(TOOL_RECT_ERASER == tool){
                opDraw.setLineStyle(OpDraw.DASH);
                opDraw.setStrokeWidth(2);
                opDraw.setColor(0xFF08b1f2L);
            } else {
                opDraw.setStrokeWidth(paintStrokeWidth);
                opDraw.setColor(paintColor);
            }
        }
        assignBasicInfo(adjustingShapeOp);
    }

    private void adjustShapeOp(float adjustX, float adjustY){
        mapPoint[0] = adjustX;
        mapPoint[1] = adjustY;
        invertedDensityRelativeBoardMatrix.mapPoints(mapPoint);
        float x = mapPoint[0];
        float y = mapPoint[1];
        switch (tool){
            case TOOL_PENCIL:
                OpDrawPath opDrawPath = (OpDrawPath) adjustingShapeOp;
                List<PointF> pointFS = opDrawPath.getPoints();
                float preX, preY, midX, midY;
                preX = pointFS.get(pointFS.size()-1).x;
                preY = pointFS.get(pointFS.size()-1).y;
                midX = (preX + x) / 2;
                midY = (preY + y) / 2;
//                    KLog.p("=pathPreX=%s, pathPreY=%s, midX=%s, midY=%s", preX, preY, midX, midY);
                opDrawPath.getPath().quadTo(preX, preY, midX, midY);
                pointFS.add(new PointF(x, y));

                break;
            case TOOL_LINE:
                OpDrawLine opDrawLine = (OpDrawLine) adjustingShapeOp;
                opDrawLine.setStopX(x);
                opDrawLine.setStopY(y);
                break;
            case TOOL_RECT:
                OpDrawRect opDrawRect = (OpDrawRect) adjustingShapeOp;
                opDrawRect.setRight(x);
                opDrawRect.setBottom(y);
                break;
            case TOOL_OVAL:
                OpDrawOval opDrawOval = (OpDrawOval) adjustingShapeOp;
                opDrawOval.setRight(x);
                opDrawOval.setBottom(y);
                break;
            case TOOL_ERASER:
                OpErase opErase = (OpErase) adjustingShapeOp;
                pointFS = opErase.getPoints();
                preX = pointFS.get(pointFS.size()-1).x;
                preY = pointFS.get(pointFS.size()-1).y;
                midX = (preX + x) / 2;
                midY = (preY + y) / 2;
//                    KLog.p("=pathPreX=%s, pathPreY=%s, midX=%s, midY=%s", preX, preY, midX, midY);
                opErase.getPath().quadTo(preX, preY, midX, midY);
                pointFS.add(new PointF(x, y));

                break;
            case TOOL_RECT_ERASER:
                OpDrawRect opDrawRect1 = (OpDrawRect) adjustingShapeOp;
                opDrawRect1.setRight(x);
                opDrawRect1.setBottom(y);
                break;
            default:
                return;
        }


    }


    private void confirmShapeOp(){
        if (TOOL_RECT_ERASER == tool){
            OpDrawRect opDrawRect = (OpDrawRect) adjustingShapeOp;
            adjustingShapeOp = new OpRectErase(opDrawRect.getLeft(), opDrawRect.getTop(), opDrawRect.getRight(), opDrawRect.getBottom());
            assignBasicInfo(adjustingShapeOp);
        }else if (TOOL_PENCIL == tool){
            OpDrawPath opDrawPath = (OpDrawPath) adjustingShapeOp;
            List<PointF> points = opDrawPath.getPoints();
            PointF lastPoint = points.get(points.size()-1);
            opDrawPath.getPath().lineTo(lastPoint.x, lastPoint.y);
        }else if (TOOL_ERASER == tool){
            OpErase opErase = (OpErase) adjustingShapeOp;
            List<PointF> points = opErase.getPoints();
            PointF lastPoint = points.get(points.size()-1);
            opErase.getPath().lineTo(lastPoint.x, lastPoint.y);
        }
    }


    private void assignBasicInfo(OpPaint op){
        op.setConfE164(boardInfo.getConfE164());
        op.setBoardId(boardInfo.getId());
        op.setPageId(boardInfo.getPageId());
    }


    @Override
    public String getBoardId() {
        return null!=boardInfo ? boardInfo.getId() : null;
    }

    @Override
    public BoardInfo getBoardInfo(){
        return boardInfo;
    }

    @Override
    public View getBoardView() {
        return this;
    }


    @Override
    public void setTool(int style) {
        this.tool = style;
    }

    @Override
    public int getTool() {
        return tool;
    }

    @Override
    public void setPaintStrokeWidth(int width) {
        this.paintStrokeWidth = width;
    }

    @Override
    public int getPaintStrokeWidth() {
        return paintStrokeWidth;
    }

    @Override
    public void setPaintColor(long color) {
        this.paintColor = color;
    }

    @Override
    public long getPaintColor() {
        return paintColor;
    }

    @Override
    public void setEraserSize(int size) {
        eraserSize = size;
    }

    @Override
    public int getEraserSize() {
        return eraserSize;
    }


    private MyConcurrentLinkedDeque<OpPaint> getPicOpsBySnapshot(){
        MyConcurrentLinkedDeque<OpPaint> allOps = new MyConcurrentLinkedDeque<>();
        MyConcurrentLinkedDeque<OpPaint> refinedOps = new MyConcurrentLinkedDeque<>();
        // 筛选需要纳入快照的图片操作
        allOps.addAll(picOps);
        while (!allOps.isEmpty()){
            OpPaint op = allOps.pollFirst();
            if (op instanceof IBoundary){
                refinedOps.offerLast(op);
            }
        }
        return refinedOps;
    }

    private MyConcurrentLinkedDeque<OpPaint> getShapeOpsBySnapshot(boolean[] hasEraseOp){
        MyConcurrentLinkedDeque<OpPaint> allOps = new MyConcurrentLinkedDeque<>();
        MyConcurrentLinkedDeque<OpPaint> refinedOps = new MyConcurrentLinkedDeque<>();
        allOps.addAll(shapeOps);
        allOps.addAll(tmpShapeOps);
        while (!allOps.isEmpty()){
            OpPaint op = allOps.pollFirst();
            if (EOpType.CLEAR_SCREEN == op.getType()){
                refinedOps.clear(); // 剔除清屏及其之前的操作（清屏/擦除不影响图片）
                continue;
            }
            if (op instanceof IBoundary) {
                refinedOps.offerLast(op);
                if (op instanceof OpErase || op instanceof OpRectErase){
                    hasEraseOp[0] = true;
                }
            }
        }

        return refinedOps;
    }

    private MyConcurrentLinkedDeque<OpPaint> getEditingPicOpsBySnapshot(){
        MyConcurrentLinkedDeque<OpPaint> ops = new MyConcurrentLinkedDeque<>();
        Matrix increasedMatrix = new Matrix(picEditViewMatrix);
        increasedMatrix.postConcat(MatrixHelper.invert(savedMatrixBeforeEditPic));
        RectF bound = new RectF();
        // 正在编辑的图片也纳入快照
        for(PicEditStuff picEditStuff : picEditStuffs){
            OpInsertPic pic = new OpInsertPic();
            pic.setPic(picEditStuff.pic.getPic());
            pic.setMatrix(picEditStuff.pic.getMatrix());
            pic.getMatrix().postConcat(increasedMatrix);

            OpInsertPic delIcon = new OpInsertPic();
            delIcon.setPic(picEditStuff.delIcon.getPic());
            delIcon.setMatrix(picEditStuff.delIcon.getMatrix());
            delIcon.getMatrix().postConcat(increasedMatrix);

            OpDrawRect dashedRect = new OpDrawRect();
            bound.set(picEditStuff.dashedRect.boundary());
            increasedMatrix.mapRect(bound);
            dashedRect.setValues(bound);
            dashedRect.setLineStyle(picEditStuff.dashedRect.getLineStyle());
            dashedRect.setStrokeWidth(picEditStuff.dashedRect.getStrokeWidth());
            dashedRect.setColor(picEditStuff.dashedRect.getColor());

            ops.offerLast(pic);
            ops.offerLast(dashedRect);
            ops.offerLast(delIcon);
        }

        return ops;
    }

    /**计算操作集合的边界*/
    private RectF calcBoundary(MyConcurrentLinkedDeque<OpPaint> bounds){
        MyConcurrentLinkedDeque<OpPaint> ops = new MyConcurrentLinkedDeque<>();
        ops.addAll(bounds);
        IBoundary bound = (IBoundary) ops.peekFirst();
        RectF bd = bound.boundary();
        float left = bd.left;
        float top = bd.top;
        float right = bd.right;
        float bottom = bd.bottom;
        OpPaint op;
        while (!ops.isEmpty()){
            op = ops.pollFirst();
//            KLog.p("op =%s", op);
            bd = ((IBoundary) op).boundary();
            left = bd.left < left ? bd.left : left;
            top = bd.top < top ? bd.top : top;
            right = bd.right > right ? bd.right: right;
            bottom = bd.bottom > bottom ? bd.bottom : bottom;
//            KLog.p("bound[%s, %s, %s, %s]", left, top, right, bottom);
        }

        return new RectF(left, top, right, bottom);
    }

    /**
     * 快照。
     * @param area 区域{@link #AREA_ALL},{@link #AREA_WINDOW}。
     *             取值AREA_WINDOW时是直接获取的缓存图片，效率较高，但仅截取了画板窗口内的内容且若定制的尺寸较小输出的图片较模糊；
     *             取值AREA_ALL时是经过一系列计算调整内容的大小和位置，然后把所有的内容重新绘制一遍生成一个图片，
     *             效率较低，但输出图片包含了画板所有内容且无论定制尺寸多大内容均清晰；
     *             截取缩略图优先使用AREA_WINDOW，保存画板内容使用AREA_ALL。
     * @param outputWidth 生成的图片的宽，若大于画板宽或者小于等于0则取值画板的宽。
     * @param outputHeight 生成的图片的高，若大于画板高或者小于等于0则取值画板的高。
     * */
    @Override
    public void snapshot(int area, int outputWidth, int outputHeight, ISnapshotResultListener resultListener) {
        KLog.p("=>");
        if (null == resultListener){
            KLog.p(KLog.ERROR, "null == resultListener");
            return;
        }

        boolean bLoaded = getWidth()>0 && getHeight()>0;

        int boardW = bLoaded ? getWidth() : boardWidth;
        int boardH = bLoaded ? getHeight() : boardHeight;
        int outputW = (outputWidth <=0 || boardW< outputWidth) ? boardW : outputWidth;
        int outputH = (outputHeight <=0 || boardH< outputHeight) ? boardH : outputHeight;

        Bitmap bt = Bitmap.createBitmap(outputW, outputH, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bt);
        if (!(outputW==boardW && outputH==boardH)) {
            canvas.scale(outputW/(float)boardW, outputH/(float)boardH);
        }

        // 绘制背景
        if (bLoaded){
            draw(canvas);
        }else {
            Drawable.ConstantState constantState = getBackground().getConstantState();
            if (null != constantState) {
                Drawable background = constantState.newDrawable().mutate();
                background.setBounds(0, 0, boardW, boardH);
                background.draw(canvas);
            }
        }

        assHandler.post(() -> {
            if (AREA_WINDOW == area) {
                if (bLoaded && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    snapshotWindow(canvas);
                } else {
                    snapshotAll(canvas);
                }
            }else{
                snapshotAll(canvas);
            }
            handler.post(() -> resultListener.onResult(bt));  // XXX：可能发生resultListener被销毁后仍收到该回调的情形。
        });

    }


    private void snapshotAll(Canvas canvas){

        // 筛选需要保存的操作
        MyConcurrentLinkedDeque<OpPaint> ops = new MyConcurrentLinkedDeque<>();

        boolean[] hasEraseOp = new boolean[1];
        MyConcurrentLinkedDeque<OpPaint> picOps = getPicOpsBySnapshot();
        MyConcurrentLinkedDeque<OpPaint> shapeOps = getShapeOpsBySnapshot(hasEraseOp);
        MyConcurrentLinkedDeque<OpPaint> editingPicOps = getEditingPicOpsBySnapshot();
        ops.addAll(picOps);
        ops.addAll(shapeOps);
        ops.addAll(editingPicOps);

        if (ops.isEmpty()){
            KLog.p(KLog.WARN, "no content!");
            return;
        }

        float boardW = getWidth()>0 ? getWidth() : boardWidth;
        float boardH = getHeight()>0 ? getHeight() : boardHeight;

        // 计算操作的边界
        RectF bound = calcBoundary(ops);
//        KLog.p("calcBoundary=%s", bound);

        // 根据操作边界结合当前画板缩放计算绘制操作需要的缩放及位移
        Matrix curRelativeBoardMatrix = getDensityRelativeBoardMatrix();
        curRelativeBoardMatrix.mapRect(bound);
        float boundW = bound.width();
        float boundH = bound.height();
        float scale = 1;
        if (boundW/boundH > boardW/boardH){
            scale = boardW/boundW;
        }else {
            scale = boardH/boundH;
        }

//        KLog.p("bound=%s, curRelativeBoardMatrix=%s", bound, curRelativeBoardMatrix);

        Matrix matrix = new Matrix(curRelativeBoardMatrix);
        if (scale > 1){ // 画板尺寸大于操作边界尺寸
            if (0<bound.left&&boundW<boardW
                    && 0<bound.right&&boundH<boardH){
                // 操作已在画板中全景展示则无需做任何放缩或位移，直接原样展示
                // DO NOTHING
            }else { // 尽管操作边界尺寸较画板小，但操作目前展示在画板外，则移动操作以保证全景展示。
                // 操作居中展示
                matrix.postTranslate(-bound.left + (boardW - boundW) / 2, -bound.top + (boardH - boundH) / 2);
            }
        }else{
            // 画板尺寸小于操作边界尺寸则需将操作缩放以适应画板尺寸
            // 操作边界外围加塞padding使展示效果更友好
            float refinedBoundW = boundW + 2*40;
            float refinedBoundH = boundH + 2*40;
            float refinedScale;
            if (refinedBoundW/refinedBoundH > boardW/boardH){
                refinedScale = boardW/refinedBoundW;
            }else {
                refinedScale = boardH/refinedBoundH;
            }
            // 先让操作边界居中
            matrix.postTranslate(-bound.left+(boardW-boundW)/2, -bound.top+(boardH-boundH)/2);
            // 再以屏幕中心为缩放中心缩小操作边界
            matrix.postScale(refinedScale, refinedScale, boardW/2, boardH/2);
        }

        canvas.concat(matrix);

        render(picOps, canvas);

        if (hasEraseOp[0]) {
            // 保存已绘制的内容，避免被擦除
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
                canvas.saveLayer(null, null);
            }else {
                canvas.saveLayer(null, null, Canvas.ALL_SAVE_FLAG);
            }

            render(shapeOps, canvas);

            canvas.restore();

        }else{

            render(shapeOps, canvas);
        }

        render(editingPicOps, canvas);

    }


    private final Object snapshotLock = new Object();
    private Bitmap shapeLayerSnapshot;
    private Bitmap picLayerSnapshot;
    private Bitmap picEditingLayerSnapshot;
    /**
     * NOTE: 该方法需在API LEVEL >= 21时使用，21以下TextureView.getBitmap方法有bug，
     * @see <a href="https://github.com/mapbox/mapbox-gl-native/issues/4911">
     *     Android 4.4.x (KitKat) Hardware Acceleration Thread Bug #4911
     *     </a>
     * */
    private void snapshotWindow(Canvas canvas){
        synchronized (snapshotLock) {
//            KLog.p("picOps.isEmpty() = %s, shapeOps.isEmpty()=%s, isEmpty()=%s, picEditStuffs.isEmpty() = %s, " +
//                            "picLayerSnapshot=%s, shapeLayerSnapshot=%s, picEditingLayerSnapshot=%s",
//                    picOps.isEmpty(), shapeOps.isEmpty(), isEmpty(), picEditStuffs.isEmpty(),
//                    picLayerSnapshot, shapeLayerSnapshot, picEditingLayerSnapshot);

            if (null == picLayerSnapshot) {
                picLayerSnapshot = picPaintView.getBitmap();
            } else {
                picPaintView.getBitmap(picLayerSnapshot);
            }

            if (null == shapeLayerSnapshot) {
                shapeLayerSnapshot = shapePaintView.getBitmap();
            } else {
                shapePaintView.getBitmap(shapeLayerSnapshot);
            }

            if (null == picEditingLayerSnapshot) {
                picEditingLayerSnapshot = picEditPaintView.getBitmap();
            } else {
                picEditPaintView.getBitmap(picEditingLayerSnapshot);
            }

            Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
            if (null != picLayerSnapshot) {
                canvas.drawBitmap(picLayerSnapshot, 0, 0, paint);
            }
            if (null != shapeLayerSnapshot) {
                canvas.drawBitmap(shapeLayerSnapshot, 0, 0, paint);
            }
            if (null != picEditingLayerSnapshot) {
                canvas.drawBitmap(picEditingLayerSnapshot, 0, 0, paint);
            }
        }
    }


    private void dealSimpleOp(OpPaint op){
        if (null != onStateChangedListener) {
            assignBasicInfo(op);
            onStateChangedListener.onPaintOpGenerated(getBoardId(), op, false); // 等到平台广播ntf才刷新
        }
    }

    @Override
    public void undo() {
        dealSimpleOp(new OpUndo());
    }

    @Override
    public void redo() {
        dealSimpleOp(new OpRedo());
    }

    @Override
    public void clearScreen() {
        dealSimpleOp(new OpClearScreen());
    }

// SEALED
//    @Override
//    public void zoom(int percentage) {
//        float zoomRate = percentage/100f;
//        zoomRate = (minZoomRate <=zoomRate && zoomRate<= maxZoomRate) ? zoomRate : (zoomRate< minZoomRate ? minZoomRate : maxZoomRate);
//        KLog.p("zoomRate=%s, width=%s, height=%s", zoomRate, getWidth(), getHeight());
//        OpMatrix opMatrix = new OpMatrix();
//        opMatrix.getMatrix().setScale(zoomRate, zoomRate, getWidth()/2, getHeight()/2);
//        dealSimpleOp(opMatrix);
//    }

    float[] zoomVals = new float[9];
    @Override
    public int getZoom() {
        boardMatrix.getValues(zoomVals);
        return (int) Math.ceil(zoomVals[Matrix.MSCALE_X]*100);
    }

    @Override
    public void setMinZoomRate(int rate) {
        minZoomRate = rate/100f;
    }

    @Override
    public int getMinZoomRate() {
        return (int) (minZoomRate*100);
    }

    @Override
    public void setMaxZoomRate(int rate) {
        maxZoomRate = rate/100f;
    }

    @Override
    public int getMaxZoomRate() {
        return (int) (maxZoomRate * 100);
    }

    private boolean bLastStateIsEmpty =true;
    /**
     * 画板是否是空。
     * NOTE: “空”的必要条件是视觉上画板没有内容，但是使用“擦除”操作清掉画板的内容并不会被判定为画板为空。
     * */
    @Override
    public boolean isEmpty() {
        // XXX: 如果对端直接擦除一个空白画板会导致此接口返回false。TODO 在painter中过滤掉此种情形的擦除、清屏消息。
        return  (picOps.isEmpty() && (shapeOps.isEmpty() || shapeOps.peekLast() instanceof OpClearScreen));
    }


    @Override
    public int getRepealedOpsCount() {
        return repealedShapeOps.size();
    }

    @Override
    public int getShapeOpsCount() {
        return shapeOps.size();
    }

    @Override
    public int getPicCount() {
        int count = 0;
        for (OpPaint op : picOps){
            if (EOpType.INSERT_PICTURE == op.getType()){
                ++count;
            }
        }
        return count;
    }


    private void refreshEmptyState(){
        if (!bLastStateIsEmpty && isEmpty()){   // 之前是不为空的状态现在为空了
            if (null != onStateChangedListener) onStateChangedListener.onEmptyStateChanged(getBoardId(), bLastStateIsEmpty=true);
        }else if (bLastStateIsEmpty && !isEmpty()){ // 之前是为空的状态现在不为空了
            if (null != onStateChangedListener) onStateChangedListener.onEmptyStateChanged(getBoardId(), bLastStateIsEmpty=false);
        }
    }

    private void repealableStateChanged(){
        if (null != onStateChangedListener){
            onStateChangedListener.onRepealableStateChanged(getBoardId(), getRepealedOpsCount(), getShapeOpsCount());
            refreshEmptyState();
        }
    }

    private void screenCleared(){
        refreshEmptyState();
    }

    private void picCountChanged(){
        if (null != onStateChangedListener){
            onStateChangedListener.onPictureCountChanged(getBoardId(), getPicCount());
            refreshEmptyState();
        }
    }

    private void zoomRateChanged(){
        if (null != onStateChangedListener) onStateChangedListener.onZoomRateChanged(getBoardId(), getZoom());
    }


    DefaultTouchListener.IOnEventListener tmpPicViewEventListener = new DefaultTouchListener.IOnEventListener(){
        private float preDragX, preDragY;
        private float scaleCenterX, scaleCenterY;
        private final float scaleRateTopLimit = 3f;
        private final float scaleRateBottomLimit = 0.5f;

        @Override
        public boolean onDown(float x, float y) {
            if (picEditStuffs.isEmpty() && picOps.isEmpty()){
                return false; // 放弃处理后续事件
            }
            if (!picEditStuffs.isEmpty()){
                handler.removeCallbacks(finishEditPicRunnable);
                PicEditStuff picEditStuff = picEditStuffs.peekFirst(); // NOTE: 目前仅同时编辑一张图片
                if (picEditStuff.isInDelPicIcon(x, y)){
                    picEditStuff.delIcon.setPic(del_pic_active_icon);
                    if (null != onStateChangedListener) onStateChangedListener.onPaintOpGenerated(getBoardId(), null, true);
                }
            }
            return true;
        }


        @Override
        public void onUp(float x, float y) {
            if (!picEditStuffs.isEmpty()) {
                PicEditStuff picEditStuff = picEditStuffs.peekFirst();
                if (picEditStuff.isInDelPicIcon(x, y)){
                    delPic(picEditStuffs.pollFirst());
                }else {
                    handler.postDelayed(finishEditPicRunnable, 5000);
                }
            }
        }

        @Override
        public void onSecondPointerDown(float x, float y) {
            if (!picEditStuffs.isEmpty()) {
                PicEditStuff picEditStuff = picEditStuffs.peekFirst();
                picEditStuff.delIcon.setPic(del_pic_icon);
                if (null != onStateChangedListener) onStateChangedListener.onPaintOpGenerated(getBoardId(), null, true);
            }
        }

        @Override
        public void onLastPointerLeft(float x, float y) {
            if (!picEditStuffs.isEmpty()) {
                PicEditStuff picEditStuff = picEditStuffs.peekFirst();
                if (picEditStuff.isInDelPicIcon(x, y)){
                    picEditStuff.delIcon.setPic(del_pic_active_icon);
                    if (null != onStateChangedListener) onStateChangedListener.onPaintOpGenerated(getBoardId(), null, true);
                }
            }
        }

        @Override
        public void onSingleTap(float x, float y) {
            if (!picEditStuffs.isEmpty()) {
                PicEditStuff picEditStuff = picEditStuffs.peekFirst();
                if (!picEditStuff.isInDashedRect(x, y)&&!picEditStuff.isInDelPicIcon(x,y)){
                    finishEditPic(picEditStuffs.pollFirst());
                }
            }
        }

        @Override
        public void onDragBegin(float x, float y) {
            if (picEditStuffs.isEmpty()){
                return;
            }
            preDragX = x; preDragY = y;
        }

        @Override
        public void onDrag(float x, float y) {
//            KLog.p("onDrag tmp pic layer, x=%s. y=%s", x, y);
            if (picEditStuffs.isEmpty()){
                return;
            }
            picEditViewMatrix.postTranslate(x-preDragX, y-preDragY);
            if (null != onStateChangedListener) onStateChangedListener.onPaintOpGenerated(getBoardId(), null, true);
            preDragX = x; preDragY = y;
        }

        @Override
        public void onScaleBegin() {
            if (picEditStuffs.isEmpty()){
                return;
            }
            scaleCenterX = getWidth()/2;
            scaleCenterY = getHeight()/2;
        }

        @Override
        public void onScale(float factor) {
            if (picEditStuffs.isEmpty()){
                return;
            }
            picEditViewMatrix.postScale(factor, factor, scaleCenterX, scaleCenterY);
            if (null != onStateChangedListener) onStateChangedListener.onPaintOpGenerated(getBoardId(), null, true);
        }

    };


    private Matrix savedMatrixBeforeEditPic = new Matrix();
    private boolean bInsertingPic = false;
    @Override
    public void insertPic(String path) {
        if (null == onStateChangedListener){
            KLog.p(KLog.ERROR,"onStateChangedListener is null");
            return;
        }

        handler.removeCallbacks(finishEditPicRunnable);
        if (!picEditStuffs.isEmpty()){
            finishEditPic(picEditStuffs.pollFirst());
        }

        bInsertingPic = true;

        // 插入图片是所见即所得的效果
        picEditViewMatrix.reset();
        savedMatrixBeforeEditPic.set(picEditViewMatrix);

        Matrix matrix = new Matrix();
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);
        matrix.setTranslate((getWidth()-options.outWidth)/2f, (getHeight()-options.outHeight)/2f);
        OpInsertPic op = new OpInsertPic(path, matrix);
        assignBasicInfo(op);

        editPic(op);
        handler.postDelayed(finishEditPicRunnable, 5000);
    }




    private void finishEditPic(PicEditStuff picEditStuff){

        OpInsertPic opInsertPic = picEditStuff.pic;

        // 图片在编辑过程中生成的matrix计入图片“自身”的matrix
        Matrix increasedMatrix = new Matrix(picEditViewMatrix);
        increasedMatrix.postConcat(MatrixHelper.invert(savedMatrixBeforeEditPic));
        opInsertPic.getMatrix().postConcat(increasedMatrix);

        picOps.offerLast(opInsertPic);

        OpPaint op;

        // 发布
        if (bInsertingPic) {
            // 正在插入图片

            //插入时是所见即所得的效果，所以需要抵消掉画板的matrix
            opInsertPic.getMatrix().postConcat(MatrixHelper.invert(getDensityRelativeBoardMatrix()));

            opInsertPic.setBoardMatrix(boardMatrix);

            /*求取transMatrix
             * transMatrix = 1/mixMatrix * (picMatrix * boardMatrix)
             * NOTE: 己端的策略插入图片时是把图片的缩放位移信息全部放入transMatrix中，插入点恒为(0,0)，宽高恒为原始宽高。
             * 所以mixMatrix为单位矩阵，所以 transMatrix = picMatrix * boardMatrix
             * */
            Matrix transMatrix = new Matrix(opInsertPic.getMatrix());
            transMatrix.postConcat(opInsertPic.getBoardMatrix());
            opInsertPic.setTransMatrix(transMatrix);

            bInsertingPic = false;

            // 通知用户图片数量变化
            picCountChanged();

            op = opInsertPic;

        } else {
            // 正在拖动放缩图片

            /* 求取dragMatrix。
             mixMatrix*dragMatrix = picMatrix * boardMatrix
             * => dragMatrix = 1/mixMatrix * (picMatrix * boardMatrix)
             * */
            Matrix dragMatrix = new Matrix(opInsertPic.getMatrix());
            dragMatrix.postConcat(boardMatrix);
            dragMatrix.preConcat(MatrixHelper.invert(opInsertPic.getMixMatrix()));

            Map<String, Matrix> picMatrices = new HashMap<>();
            picMatrices.put(opInsertPic.getPicId(), dragMatrix);
            OpDragPic opDragPic = new OpDragPic(picMatrices);
            assignBasicInfo(opDragPic);

            op = opDragPic;
        }

        if (null != onStateChangedListener) onStateChangedListener.onPaintOpGenerated(getBoardId(), op, true);

        focusedLayer = savedLayerBeforeEditPic;

        // 清空tmpPaintView设置。
        picEditViewMatrix.reset();
    }


    private void delPic(PicEditStuff picEditStuff){
        OpInsertPic opInsertPic = picEditStuff.pic;

        focusedLayer = savedLayerBeforeEditPic;
        picEditViewMatrix.reset();
        if (bInsertingPic) {
            // 如果是正在插入中就删除就不用走发布
            if (null != onStateChangedListener) onStateChangedListener.onPaintOpGenerated(getBoardId(), null, true);
            bInsertingPic = false;
        }else{
            if (null == opInsertPic){
                KLog.p(KLog.ERROR,"null == opInsertPic");
                return;
            }
            OpDeletePic opDeletePic = new OpDeletePic(new String[]{opInsertPic.getPicId()});
            assignBasicInfo(opDeletePic);
            if (null != onStateChangedListener) onStateChangedListener.onPaintOpGenerated(getBoardId(), opDeletePic, true);

            picCountChanged();
        }
    }


    private OpInsertPic selectPic(float x, float y){
        RectF picBoundary = new RectF();
        Iterator<OpPaint> it = picOps.descendingIterator();
        while (it.hasNext()){
            OpPaint op = it.next();
            if (op instanceof OpInsertPic){
                OpInsertPic opInsertPic = (OpInsertPic) op;
                if (null == opInsertPic.getPic()){
                    continue; // 图片操作有但图片可能还未获取到（如，协作方上传图片尚未完成）
                }
                picBoundary.set(opInsertPic.boundary()); // 图片边界仅包含了图片自身的matrix未包含boardMatrix
                getDensityRelativeBoardMatrix().mapRect(picBoundary);
                if (picBoundary.contains(x, y)){
                    return opInsertPic;
                }
            }
        }
        return null;
    }

    private int savedLayerBeforeEditPic;
    private static final int DASH_RECT_PADDING = 5; // 图片编辑时的虚线矩形框和图片之间的间隙。单位：pixel
    private static final int DEL_ICON_TOP_PADDING = 8; // 图片编辑时的虚线矩形框和删除图标之间的间隙。单位：pixel
    private static final int DASH_RECT_STROKE_WIDTH = 2; // 图片编辑时的虚线矩形框粗细。单位：pixel
    private static final long DASH_RECT_COLOR = 0xFF08b1f2L; // 图片编辑时的虚线矩形框颜色。
    private void editPic(OpInsertPic opInsertPic){

        // 在图片外围绘制一个虚线矩形框
        OpDrawRect opDrawRect = new OpDrawRect();
        RectF dashRect = new RectF(opInsertPic.boundary());
        dashRect.inset(-DASH_RECT_PADDING, -DASH_RECT_PADDING);
        opDrawRect.setValues(dashRect);
        opDrawRect.setLineStyle(OpDraw.DASH);
        opDrawRect.setStrokeWidth(DASH_RECT_STROKE_WIDTH);
        opDrawRect.setColor(DASH_RECT_COLOR);

        // 在虚线矩形框正下方绘制删除图标
        OpInsertPic delPicIcon = new OpInsertPic();
        delPicIcon.setPic(del_pic_icon);
        Matrix matrix = new Matrix();
        matrix.postTranslate(dashRect.left+(dashRect.width()-del_pic_icon.getWidth())/2f,
                dashRect.bottom+DEL_ICON_TOP_PADDING);
        matrix.postScale(1/MatrixHelper.getScaleX(picEditViewMatrix), 1/MatrixHelper.getScaleY(picEditViewMatrix),
                dashRect.centerX(), dashRect.bottom); // 使图标以正常尺寸展示，不至于因画板缩小/放大而过小/过大
        delPicIcon.setMatrix(matrix);

        PicEditStuff picEditStuff = new PicEditStuff(opInsertPic, delPicIcon, opDrawRect);

        picEditStuffs.offerLast(picEditStuff);

        if (null != onStateChangedListener) onStateChangedListener.onPaintOpGenerated(getBoardId(), null, true);

        savedLayerBeforeEditPic = focusedLayer;
        focusedLayer = LAYER_PIC_TMP;
    }



    static int editedPicCount=0;
    class PicEditStuff{
        int id;
        OpInsertPic pic;
        OpInsertPic delIcon;
        OpDrawRect dashedRect;

        PicEditStuff(OpInsertPic pic, OpInsertPic delIcon, OpDrawRect dashedRect) {
            id = editedPicCount++;
            this.pic = pic;
            this.delIcon = delIcon;
            this.dashedRect = dashedRect;
        }

        boolean isInDashedRect(float x, float y){
            RectF rectF = new RectF(dashedRect.boundary());
            picEditViewMatrix.mapRect(rectF);
            return rectF.contains(x, y);
        }

        boolean isInDelPicIcon(float x, float y){
            RectF rectF = new RectF(delIcon.boundary());
            picEditViewMatrix.mapRect(rectF);
            return rectF.contains(x, y);
        }

    }




    /**
     * @return true 需要刷新，false不需要。
     * */
    boolean onPaintOp(OpPaint op){
        String boardId = op.getBoardId();
        if(!boardId.equals(getBoardId())){
            KLog.p(KLog.ERROR,"op %s is not for %s", op, getBoardId());
            return false;
        }
        KLog.p("recv op %s", op);

        MyConcurrentLinkedDeque<OpPaint> shapeRenderOps = shapeOps;
        Stack<OpPaint> shapeRepealedOps = repealedShapeOps;
        MyConcurrentLinkedDeque<OpPaint> picRenderOps = picOps;
        Matrix boardMatrix1 = boardMatrix;

        if (null != onStateChangedListener) onStateChangedListener.onChanged(getBoardId());

        // 检查是否为主动绘制触发的响应。若是则我们不再重复绘制，因为它已经展示在界面上。
        OpPaint shapeTmpOp = tmpShapeOps.pollFirst();
        if (null != shapeTmpOp && shapeTmpOp.getUuid().equals(op.getUuid())) {
            KLog.p("tmp op %s confirmed", shapeTmpOp);
            if (!shapeRepealedOps.isEmpty() && shapeTmpOp instanceof IRepealable) {
                //撤销/恢复操作流被“可撤销”操作中断，则重置撤销/恢复相关状态
                shapeRepealedOps.clear();
                repealableStateChanged();
            }
            boolean bEmpty = shapeRenderOps.isEmpty();
            shapeRenderOps.offerLast(shapeTmpOp); // 临时工转正
            if (bEmpty && shapeTmpOp instanceof IRepealable){ // 可撤销操作从无到有
                repealableStateChanged();
            }
            return false;
        }

        // 不是主动绘制的响应则清空临时绘制 // TODO 本端还是加时间戳吧3S超时可清空，清空在绘制线程中重绘时去做，不在这里做
        tmpShapeOps.clear();

        boolean bRefresh = true;
        OpPaint tmpOp;

        switch (op.getType()){
            case INSERT_PICTURE:
                for (OpPaint opPaint : picRenderOps){
                    if (opPaint instanceof OpInsertPic
                            && ((OpInsertPic)opPaint).getPicId().equals(((OpInsertPic) op).getPicId())
                            ){
                        KLog.p("pic op %s already exist!", opPaint);
                        return false;
                    }
                }
                picRenderOps.offerLast(op);
                OpInsertPic opInsertPic = (OpInsertPic) op;
                opInsertPic.setBoardMatrix(boardMatrix1);
                if (null == opInsertPic.getPic()) {
                    if (null != opInsertPic.getPicPath()) {
                        opInsertPic.setPic(BitmapFactory.decodeFile(opInsertPic.getPicPath())); // TODO 优化。比如大分辨率图片裁剪
                    } else {
                        bRefresh = false; // 图片为空不需刷新界面（图片可能正在下载）
                    }
                }

                Bitmap pic = opInsertPic.getPic();
                if (null != pic){
                    /*计算mixMatrix。
                    可将mixMatrix理解为：把左上角在原点处的未经缩放的图片变换为对端所描述的图片（插入图片时传过来的insertPos, picWidth, picHeight这些信息所描述的图片）
                    所需经历的矩形变换*/
                    PointF insertPos = opInsertPic.getInsertPos();
                    Matrix mixMatrix = MatrixHelper.calcTransMatrix(new RectF(0, 0, pic.getWidth(), pic.getHeight()),
                            new RectF(insertPos.x, insertPos.y, insertPos.x+opInsertPic.getPicWidth(), insertPos.y+opInsertPic.getPicHeight()));
                    opInsertPic.setMixMatrix(mixMatrix);

                    // 计算picMatrix= mixMatrix * transMatrix / boardMatrix
                    Matrix picMatrix = new Matrix(mixMatrix);
                    picMatrix.postConcat(opInsertPic.getTransMatrix());
                    picMatrix.postConcat(MatrixHelper.invert(opInsertPic.getBoardMatrix()));
                    opInsertPic.setMatrix(picMatrix);
                }

                picCountChanged();

                break;

            case DELETE_PICTURE:
                bRefresh = false;
                OpPaint picRenderOp;
                boolean got =false;
                for (String picId : ((OpDeletePic) op).getPicIds()) {
                    got = false;
                    Iterator it = picRenderOps.iterator();
                    while (it.hasNext()) {
                        picRenderOp = (OpPaint) it.next();
                        if (EOpType.INSERT_PICTURE == picRenderOp.getType()
                                && ((OpInsertPic) picRenderOp).getPicId().equals(picId)) {
                            it.remove();
                            got = true;
                            bRefresh = true;
                            break;
                        }
                    }
                    if (!got) {
                        // 可能图片正在被编辑
                        if (isExistEditingPic(picId)){
                            got = true;
                            bRefresh = true;
                            delEditingPic(picId);
                        }
                    }
                }
                if (bRefresh){
                    picCountChanged();
                }
                break;

            case DRAG_PICTURE: // TODO NOTE 己端拖动图片也会触发此通知，尚未过滤，需过滤。
                for (Map.Entry<String, Matrix> dragOp : ((OpDragPic) op).getPicMatrices().entrySet()) {
                    for (OpPaint opPaint : picRenderOps) {
                        if (EOpType.INSERT_PICTURE == opPaint.getType()
                                && ((OpInsertPic) opPaint).getPicId().equals(dragOp.getKey())) {
                            opInsertPic = (OpInsertPic) opPaint;
                            if (null != opInsertPic.getPic()) {
                                /*图片已经准备好了则直接求取picmatrix
                                 * mixMatrix * dragMatrix = picMatrix * boardMatrix
                                 * => picMatrix = mixMatrix * dragMatrix / boardMatrix
                                 * */
                                Matrix matrix = new Matrix(opInsertPic.getMixMatrix());
                                matrix.postConcat(dragOp.getValue());
                                matrix.postConcat(MatrixHelper.invert(boardMatrix1));
                                if (matrix.equals(opInsertPic.getMatrix())) {
                                    // 计算出的matrix跟当前matrix相等则不需拖动
                                    bRefresh = false;
                                }else{
                                    opInsertPic.setMatrix(matrix);
                                }
                            }else{
                                // 图片尚未准备好，我们先保存matrix，等图片准备好了再计算
                                opInsertPic.setDragMatrix(dragOp.getValue());
                                opInsertPic.setBoardMatrix(boardMatrix1);
                                bRefresh = false; // 图片尚未准备好不需刷新
                            }
                            break;
                        }
                    }
                }
                break;
            case UPDATE_PICTURE: // XXX 己端插入图片也会收到该通知，需过滤
                OpUpdatePic updatePic = (OpUpdatePic) op;
                for (OpPaint opPaint : picRenderOps) {
                    if (EOpType.INSERT_PICTURE == opPaint.getType()
                            && ((OpInsertPic) opPaint).getPicId().equals(updatePic.getPicId())) {
                        opInsertPic = (OpInsertPic) opPaint;
                        opInsertPic.setPicPath(updatePic.getPicSavePath());
                        pic = BitmapFactory.decodeFile(updatePic.getPicSavePath());
                        opInsertPic.setPic(pic); // TODO 优化。比如大分辨率图片裁剪
                        if (null != pic){
                            PointF insertPos = opInsertPic.getInsertPos();
                            Matrix mixMatrix = MatrixHelper.calcTransMatrix(new RectF(0, 0, pic.getWidth(), pic.getHeight()),
                                    new RectF(insertPos.x, insertPos.y, insertPos.x+opInsertPic.getPicWidth(), insertPos.y+opInsertPic.getPicHeight()));
                            opInsertPic.setMixMatrix(mixMatrix);

                            Matrix picMatrix = new Matrix(opInsertPic.getMixMatrix());
                            Matrix dragMatrix = opInsertPic.getDragMatrix();
                            if (null != dragMatrix) { // XXX 还有其它影响图片matrix的操作呢？
                                /* 在此之前有图片拖动操作则以图片拖动操作中的dragMatrix来计算picMatrix
                                 * picMatrix = mixMatrix * dragMatrix / boardMatrix*/
                                picMatrix.postConcat(opInsertPic.getDragMatrix());
                                opInsertPic.setDragMatrix(null);
                            }else{
                                // picMatrix = mixMatrix * transMatrix / boardMatrix
                                picMatrix.postConcat(opInsertPic.getTransMatrix());
                            }

                            picMatrix.postConcat(MatrixHelper.invert(opInsertPic.getBoardMatrix()));

                            opInsertPic.setMatrix(picMatrix);
                        }
                        break;
                    }
                }
                break;
            case FULLSCREEN_MATRIX: // 全局放缩、位移，包括图片和图形
                boardMatrix.set(((OpMatrix) op).getMatrix());
                zoomRateChanged();
                break;

            default:  // 图形操作

                switch (op.getType()){
                    case UNDO:
                        tmpOp = shapeRenderOps.pollLast(); // 撤销最近的操作
                        if (null != tmpOp){
                            KLog.p(KLog.WARN, "repeal %s",tmpOp);
                            shapeRepealedOps.push(tmpOp); // 缓存撤销的操作以供恢复
                            repealableStateChanged();
                        }else{
                            bRefresh = false;
                        }
                        break;
                    case REDO:
                        if (!shapeRepealedOps.empty()) {
                            tmpOp = shapeRepealedOps.pop();
                            KLog.p(KLog.WARN, "restore %s",tmpOp);
                            shapeRenderOps.offerLast(tmpOp); // 恢复最近操作
                            repealableStateChanged();
                        }else {
                            bRefresh = false;
                        }
                        break;
                    default:

                        if (!shapeRepealedOps.isEmpty() && op instanceof IRepealable) {
//                            KLog.p(KLog.WARN, "clean repealed ops");
                            //撤销/恢复操作流被“可撤销”操作中断，则重置撤销/恢复相关状态
                            shapeRepealedOps.clear();
                            repealableStateChanged();
                        }
                        boolean bEmpty = shapeRenderOps.isEmpty(); // XXX 如果shapeOps中将来也有不可撤销操作呢？也就是说将来可能不能简单根据是否空来判断是否该改变可撤销状态
                        shapeRenderOps.offerLast(op);
                        if (bEmpty && op instanceof IRepealable){ // 可撤销操作从无到有
                            repealableStateChanged();
                        }

                        if (EOpType.CLEAR_SCREEN == op.getType()){
                            screenCleared();
                        }
                        //                KLog.p(KLog.WARN, "need render op %s", op);
                        break;

                }

                break;

        }

        return bRefresh;
    }


    void paint(){
        KLog.p("=> board=%s", getBoardId());
        Matrix matrix = getDensityRelativeBoardMatrix();

        synchronized (snapshotLock) {
            // 图形层绘制
            Canvas shapePaintViewCanvas = shapePaintView.lockCanvas();
            if (null != shapePaintViewCanvas) {
                // 每次绘制前先清空画布以避免残留
                shapePaintViewCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

                // 设置画布matrix
                shapePaintViewCanvas.setMatrix(matrix);

                // 图形绘制
                render(shapeOps, shapePaintViewCanvas);

                // 临时图形绘制
                render(tmpShapeOps, shapePaintViewCanvas);

                // 绘制正在调整中的操作
                synchronized (adjustingOpLock) {
                    if (null != adjustingShapeOp) render(adjustingShapeOp, shapePaintViewCanvas);
                }
            }

            // 图片层绘制
            Canvas picPaintViewCanvas = picPaintView.lockCanvas();
            if (null != picPaintViewCanvas) {  // TODO 优化，尝试如果没有影响图片层的操作，如插入/删除/拖动/放缩图片，就不刷新图片层。
                // 清空画布
                picPaintViewCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

                // 设置画布matrix
                picPaintViewCanvas.setMatrix(matrix);

                // 图片绘制
                render(picOps, picPaintViewCanvas);
            }

            // 图片编辑层绘制
            Canvas tmpPaintViewCanvas = picEditPaintView.lockCanvas();
            if (null != tmpPaintViewCanvas) {
                // 清空画布
                tmpPaintViewCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

                // 设置缩放比例
                tmpPaintViewCanvas.setMatrix(picEditViewMatrix);

                // 绘制
                for (DefaultPaintBoard.PicEditStuff picEditStuff : picEditStuffs) {
                    render(picEditStuff.pic, tmpPaintViewCanvas);
                    render(picEditStuff.dashedRect, tmpPaintViewCanvas);
                    render(picEditStuff.delIcon, tmpPaintViewCanvas);
                }
            }

            // 提交绘制任务，执行绘制
//                KLog.p("go render!");
            shapePaintView.unlockCanvasAndPost(shapePaintViewCanvas);
            picPaintView.unlockCanvasAndPost(picPaintViewCanvas);
            picEditPaintView.unlockCanvasAndPost(tmpPaintViewCanvas);
        }
        KLog.p("<=");
    }


    private Paint paint = new Paint();
    private final PorterDuffXfermode DUFFMODE_SRCIN = new PorterDuffXfermode(PorterDuff.Mode.SRC_IN);
    //    private final PorterDuffXfermode DUFFMODE_DSTOVER = new PorterDuffXfermode(PorterDuff.Mode.DST_OVER);
    private final PorterDuffXfermode DUFFMODE_CLEAR = new PorterDuffXfermode(PorterDuff.Mode.CLEAR);
    private Paint cfgPaint(OpPaint opPaint){
        paint.reset();
        switch (opPaint.getType()){
            case INSERT_PICTURE:
                paint.setStyle(Paint.Style.STROKE);
                break;
            case RECT_ERASE:
            case CLEAR_SCREEN:
                paint.setStyle(Paint.Style.FILL);
                paint.setXfermode(DUFFMODE_CLEAR);
                break;
            default:
                if (opPaint instanceof OpDraw) {
                    OpDraw opDraw = (OpDraw) opPaint;
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setAntiAlias(true);
                    paint.setStrokeWidth(opDraw.getStrokeWidth());
                    paint.setColor((int) opDraw.getColor());
                    if (OpDraw.DASH == opDraw.getLineStyle()){
                        paint.setPathEffect(new DashPathEffect( new float[]{10, 4},0));
                    }
                    if (EOpType.DRAW_PATH == opPaint.getType()){
                        paint.setStrokeJoin(Paint.Join.ROUND);
                    } else if (EOpType.ERASE == opPaint.getType()){
                        int w = ((OpErase)opDraw).getWidth();
                        int h = ((OpErase)opDraw).getHeight();
                        paint.setStrokeWidth(w>h?w:h);
                        paint.setStrokeJoin(Paint.Join.ROUND);
                        paint.setAlpha(0);
                        paint.setXfermode(DUFFMODE_SRCIN);
                    }
                }
                break;
        }

        return paint;
    }



    private RectF rect = new RectF();
    private void render(OpPaint op, Canvas canvas){
        KLog.p("to render %s", op);
        switch (op.getType()) {
            case DRAW_LINE:
                OpDrawLine lineOp = (OpDrawLine) op;
                canvas.drawLine(lineOp.getStartX(), lineOp.getStartY(), lineOp.getStopX(), lineOp.getStopY(), cfgPaint(lineOp));
                break;
            case DRAW_RECT:
                OpDrawRect rectOp = (OpDrawRect) op;
                canvas.drawRect(rectOp.getLeft(), rectOp.getTop(), rectOp.getRight(), rectOp.getBottom(), cfgPaint(rectOp));
                break;
            case DRAW_OVAL:
                OpDrawOval ovalOp = (OpDrawOval) op;
                rect.set(ovalOp.getLeft(), ovalOp.getTop(), ovalOp.getRight(), ovalOp.getBottom());
                canvas.drawOval(rect, cfgPaint(ovalOp));
                break;
            case DRAW_PATH:
                OpDrawPath pathOp = (OpDrawPath) op;
                canvas.drawPath(pathOp.getPath(), cfgPaint(pathOp));
                break;
            case ERASE:
                OpErase opErase = (OpErase) op;
                canvas.drawPath(opErase.getPath(), cfgPaint(opErase));
                break;
            case RECT_ERASE:
                OpRectErase eraseOp = (OpRectErase) op;
                canvas.drawRect(eraseOp.getLeft(), eraseOp.getTop(), eraseOp.getRight(), eraseOp.getBottom(), cfgPaint(eraseOp));
                break;
            case CLEAR_SCREEN:
                canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                break;
            case INSERT_PICTURE:
                OpInsertPic insertPicOp = (OpInsertPic) op;
                if (null != insertPicOp.getPic()) {
                    canvas.drawBitmap(insertPicOp.getPic(), insertPicOp.getMatrix(), cfgPaint(insertPicOp));
                }
                break;
        }
    }

    private void render(MyConcurrentLinkedDeque<OpPaint> ops, Canvas canvas){
        for (OpPaint op : ops) {  //NOTE: Iterators are weakly consistent. 此遍历过程不感知并发的添加操作，但感知并发的删除操作。
            render(op, canvas);
        }
    }


    interface IOnStateChangedListener{
        /**
         * 生成了绘制操作。（主动绘制）
         * @param Op 绘制操作
         * @param bNeedRefresh 是否需要刷新
         * */
        default void onPaintOpGenerated(String boardId, OpPaint Op, boolean bNeedRefresh){}

        /**
         * 画板状态发生了改变。
         * 此回调是下面所有回调的先驱，方便用户做一些公共处理。
         * 如：用户可根据该回调决定是否需要重新“快照”{@link #snapshot(int, int, int, ISnapshotResultListener)}。
         * */
        default void onChanged(String boardId){}

        /**图片数量变化
         * @param count  当前图片数量*/
        default void onPictureCountChanged(String boardId, int count){}
        /**缩放比例变化
         * @param percentage  当前屏幕缩放比率百分数。如50代表50%。*/
        default void onZoomRateChanged(String boardId, int percentage){}
        /**
         * 可撤销状态变化。
         * 触发该方法的场景：
         * 1、新画板画了第一笔；
         * 2、执行了撤销操作；
         * 3、执行了恢复操作；
         * @param repealedOpsCount 已被撤销操作数量
         * @param remnantOpsCount 剩下的可撤销操作数量。如画了3条线撤销了1条则repealedOpsCount=1，remnantOpsCount=2。
         *                        NOTE: 此处的可撤销数量是具体需求无关的，“可撤销”指示的是操作类型，如画线画圆等操作是可撤销的而插入图片放缩等是不可撤销的。
         * */
        default void onRepealableStateChanged(String boardId, int repealedOpsCount, int remnantOpsCount){}
        /**
         * 画板内容为空状态变化（即画板内容从有到无或从无到有）。
         * 画板内容包括图形和图片。
         * 该方法触发的场景包括：
         * 1、最后一笔图形被撤销且没有图片，bEmptied=true；
         * 2、最后一张图片被删除且没有图形，bEmptied=true；
         * 3、清屏且没有图片，bEmptied=true；
         * 4、上述123或画板刚创建情形下，第一笔图形绘制或第一张图片插入，bEmptied=false；
         * NOTE:
         * 1、新建的画板为空（{@link IPaintBoard#isEmpty()}返回true），但不会触发该方法；
         * 2、使用“擦除”功能，包括黑板擦擦除矩形擦除，将画板内容清掉的情形不会触发此方法，且{@link IPaintBoard#isEmpty()}返回false；
         *
         * @param bEmptied 内容是否空了，true表示画板内容从有到无，false表示画板内容从无到有。
         * */
        default void onEmptyStateChanged(String boardId, boolean bEmptied){}

    }

    void setOnStateChangedListener(IOnStateChangedListener onStateChangedListener){
        this.onStateChangedListener = onStateChangedListener;
    }

}
