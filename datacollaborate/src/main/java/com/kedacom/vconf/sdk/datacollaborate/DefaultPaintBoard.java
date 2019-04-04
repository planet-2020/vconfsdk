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

import com.google.common.collect.Sets;
import com.kedacom.vconf.sdk.base.IResultListener;
import com.kedacom.vconf.sdk.base.KLog;
import com.kedacom.vconf.sdk.datacollaborate.bean.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class DefaultPaintBoard extends FrameLayout implements IPaintBoard{

    // 画板信息
    private BoardInfo boardInfo;

    // 图形层。用于图形绘制如画线、画圈、擦除等等
    private TextureView shapePaintView;

    // 图片层。用于绘制图片。
    private TextureView picPaintView;

    // 调整中的图形操作。比如画线时，从手指按下到手指拿起之间的绘制都是“调整中”的。
    private OpPaint adjustingShapeOp;
    private final Object adjustingShapeOpLock = new Object();

    // 临时图形操作。手指拿起绘制完成，但并不表示此绘制已生效，需等到平台广播NTF后方能确认为生效的操作，在此之前的操作都作为临时操作保存在这里。
    private MyConcurrentLinkedDeque<OpPaint> tmpShapeOps = new MyConcurrentLinkedDeque<>();

    // 编辑中的图片
    private PicEditStuff picEditStuff;
    private final Object picEditStuffLock = new Object();

    // 图片删除图标
    private Bitmap del_pic_icon;
    private Bitmap del_pic_active_icon;

    private OpWrapper opWrapper = new OpWrapper();

    // 图层
    static int LAYER_NONE = 100;
    static int LAYER_PIC =  101;
    static int LAYER_SHAPE =102;
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

    private DefaultTouchListener boardViewTouchListener;
    private DefaultTouchListener shapeViewTouchListener;
    private DefaultTouchListener picViewTouchListener;

    // 是否正在执行matrix操作
    private boolean bDoingMatrixOp = false;
    // 是否正在插入图片
    private boolean bInsertingPic = false;

    private Handler handler = new Handler(Looper.getMainLooper());

    private final Runnable finishEditPicRunnable = this::finishEditPic;

    private static Handler assHandler;
    static {
        HandlerThread handlerThread = new HandlerThread("BoardAss", Process.THREAD_PRIORITY_BACKGROUND);
        handlerThread.start();
        assHandler = new Handler(handlerThread.getLooper());
    }


    /* 若画板未加载（宽高为0）使用该值作为画板的默认宽高值*/
    private static int boardWidth = 1920;
    private static int boardHeight = 1080;


    public DefaultPaintBoard(@NonNull Context context, BoardInfo boardInfo) {
        super(context);

        this.boardInfo = boardInfo;

        relativeDensity = context.getResources().getDisplayMetrics().density/2;

        LayoutInflater layoutInflater = LayoutInflater.from(context);
        View whiteBoard = layoutInflater.inflate(R.layout.default_whiteboard_layout, this);
        picPaintView = whiteBoard.findViewById(R.id.pb_pic_paint_view);
        picPaintView.setOpaque(false);
        shapePaintView = whiteBoard.findViewById(R.id.pb_shape_paint_view);
        shapePaintView.setOpaque(false);

        shapePaintView.setSurfaceTextureListener(surfaceTextureListener);
        picPaintView.setSurfaceTextureListener(surfaceTextureListener);

        shapeViewTouchListener = new DefaultTouchListener(context, shapeViewEventListener);
        picViewTouchListener = new DefaultTouchListener(context, picViewEventListener);
        boardViewTouchListener = new DefaultTouchListener(context, boardViewEventListener);

        // 赋值图片删除图标
        try {
            AssetManager am = context.getAssets();
            InputStream is = am.open("del_pic.png");
            del_pic_icon = BitmapFactory.decodeStream(is);
            is.close();
            is = am.open("del_pic_active.png");
            del_pic_active_icon = BitmapFactory.decodeStream(is);
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        setBackgroundColor(Color.DKGRAY);

        // 获取画板宽高
        this.post(() -> {
            boardWidth = getWidth();
            boardHeight = getHeight();
        });

        // 初始化matrix
        OpMatrix opMatrix = new OpMatrix();
        assignBasicInfo(opMatrix);
        opWrapper.addMatrixOp(opMatrix);
    }

    public DefaultPaintBoard(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }


    private TextureView.SurfaceTextureListener surfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            KLog.p("surface available");
            // 刷新
            if (null != onStateChangedListener) onStateChangedListener.onPaintOpGenerated(getBoardId(), null, null, true);
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


    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (null == onStateChangedListener){
            return true;
        }

        if (LAYER_ALL == focusedLayer){
            boardViewTouchListener.onTouch(this, ev);
            boolean ret1 = picViewTouchListener.onTouch(picPaintView, ev);
            boolean ret2 = shapeViewTouchListener.onTouch(shapePaintView, ev);
            return ret1||ret2;
        }else if (LAYER_PIC == focusedLayer){
            return picViewTouchListener.onTouch(picPaintView, ev);
        }else if (LAYER_SHAPE == focusedLayer){
            return shapeViewTouchListener.onTouch(shapePaintView, ev);
        }else if (LAYER_NONE == focusedLayer){
            return true;
        }

        return false;
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

        if (AREA_WINDOW == area
                && bLoaded
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            snapshotWindow(canvas);
            resultListener.onResult(bt);
        }else{
            assHandler.post(() -> {
                snapshotAll(canvas);
                handler.post(() -> resultListener.onResult(bt));  // XXX：可能发生resultListener被销毁后用户仍收到该回调的情形。
            });
        }

    }


    @Override
    public void undo() {
        if (0 == opWrapper.getRepealableOpsCount()){
            KLog.p(KLog.ERROR,"no op to undo");
            return;
        }

        if (null != onStateChangedListener) {
            OpPaint opUndo = new OpUndo();
            assignBasicInfo(opUndo);
            onStateChangedListener.onPaintOpGenerated(getBoardId(), opUndo, new IResultListener() {
                @Override
                public void onSuccess(Object result) {
                    if (dealControlOp((OpUndo) result)){
                        onStateChangedListener.onPaintOpGenerated(getBoardId(), null, null,
                                true // 平台反馈撤销成功，此时刷新
                        );
                    }
                }
            },

            false // 暂不刷新，等平台反馈结果再刷新

            );
        }
    }

    @Override
    public void redo() {
        if (0==opWrapper.getRepealedOpsCount()){
            KLog.p(KLog.ERROR,"no op to repeal");
            return;
        }

        if (null != onStateChangedListener) {
            OpPaint opRedo = new OpRedo();
            assignBasicInfo(opRedo);
            onStateChangedListener.onPaintOpGenerated(getBoardId(), opRedo, new IResultListener() {
                        @Override
                        public void onSuccess(Object result) {
                            if (dealControlOp((OpRedo) result)){
                                onStateChangedListener.onPaintOpGenerated(getBoardId(), null, null, true);
                            }
                        }
                    },

                    false // 暂不刷新，等平台反馈结果再刷新

            );
        }
    }

    @Override
    public void clearScreen() {
        if (opWrapper.isClear()){
            KLog.p(KLog.ERROR, "already cleared");
            return;
        }
        if (null != onStateChangedListener) {
            OpPaint opCls = new OpClearScreen();
            assignBasicInfo(opCls);
            onStateChangedListener.onPaintOpGenerated(getBoardId(), opCls, new IResultListener() {
                        @Override
                        public void onSuccess(Object result) {
                            if (dealShapeOp((OpClearScreen) result)){
                                onStateChangedListener.onPaintOpGenerated(getBoardId(), null, null, true);
                            }
                        }
                    },

                    false // 暂不刷新，等平台反馈结果再刷新
            );
        }
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

    @Override
    public int getZoom() {
        float[] matrixValue = opWrapper.getLastMatrixOp().getMatrixValue();
        return (int) Math.ceil(matrixValue[Matrix.MSCALE_X]*100);
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

    /**
     * 画板是否是空。
     * NOTE: “空”的必要条件是视觉上画板没有内容，但是使用“擦除”操作清掉画板的内容并不会被判定为画板为空。
     * @return 若没有图片且 {@link #isClear()}为真则返回true，否则返回false。
     * */
    @Override
    public boolean isEmpty() {
        return opWrapper.isEmpty();
    }

    /**
     * 是否清屏状态。
     * 清屏状态不代表画板内容为空，目前清屏只针对图形操作，清屏状态只表示画板上所有图形操作已被清掉。
     * @return 若画板没有图形操作或者最后一个图形操作是清屏则返回true，否则返回false。
     * */
    @Override
    public boolean isClear(){
        return opWrapper.isClear();
    }

    @Override
    public int getRepealedOpsCount() {
        return opWrapper.getRepealedOpsCount();
    }

    @Override
    public int getShapeOpsCount() {
        return opWrapper.getShapeOpsCount();
    }

    @Override
    public int getPicCount() {
        return opWrapper.getInsertPicOpsCount();
    }


    @Override
    public void insertPic(String path) {
        if (null == onStateChangedListener){
            KLog.p(KLog.ERROR,"onStateChangedListener is null");
            return;
        }

        handler.removeCallbacks(finishEditPicRunnable);
        if (null != picEditStuff){
            finishEditPic();
        }

        bInsertingPic = true;

        Matrix matrix = new Matrix();
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);
        matrix.setTranslate((getWidth()-options.outWidth)/2f, (getHeight()-options.outHeight)/2f);
        matrix.postConcat(MatrixHelper.invert(getDensityRelativeBoardMatrix()));
        OpInsertPic op = new OpInsertPic(path, matrix);
        assignBasicInfo(op);

        startEditPic(Sets.newHashSet(op));
        handler.postDelayed(finishEditPicRunnable, 5000);
    }





    private boolean bLastStateIsEmpty =true;
    private void refreshEmptyState(){
        if (!bLastStateIsEmpty && isEmpty()){   // 之前是不为空的状态现在为空了
            if (null != onStateChangedListener) onStateChangedListener.onEmptyStateChanged(getBoardId(), bLastStateIsEmpty=true);
        }else if (bLastStateIsEmpty && !isEmpty()){ // 之前是为空的状态现在不为空了
            if (null != onStateChangedListener) onStateChangedListener.onEmptyStateChanged(getBoardId(), bLastStateIsEmpty=false);
        }
    }

    private void picCountChanged(){
        if (null != onStateChangedListener){
            onStateChangedListener.onChanged(getBoardId());
            onStateChangedListener.onPictureCountChanged(getBoardId(), getPicCount());
            refreshEmptyState();
        }
    }

    private void zoomRateChanged(){
        if (null != onStateChangedListener) {
            onStateChangedListener.onChanged(getBoardId());
            onStateChangedListener.onZoomRateChanged(getBoardId(), getZoom());
        }
    }




    private void snapshotAll(Canvas canvas){
        MyConcurrentLinkedDeque<OpPaint> ops = new MyConcurrentLinkedDeque<>();

        MyConcurrentLinkedDeque<OpInsertPic> picOps = opWrapper.getInsertPicOps();
        MyConcurrentLinkedDeque<OpPaint> shapeOps = opWrapper.getShapeOpsAfterCls();
        ops.addAll(picOps);
        ops.addAll(shapeOps);

        RectF bound = opWrapper.calcBoundary(ops);
        if (null != picEditStuff){
            if (null != bound) {
                bound.union(picEditStuff.bound());
            }else {
                bound = picEditStuff.bound();
            }
        }
        if (null == bound){
            KLog.p(KLog.ERROR,"no content!");
            return;
        }

//        KLog.p("calcBoundary=%s", bound);

        float boardW = getWidth()>0 ? getWidth() : boardWidth;
        float boardH = getHeight()>0 ? getHeight() : boardHeight;

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

        // 绘制图片
        render(picOps, canvas);

        boolean hasEraseOp =false;
        for (OpPaint op : shapeOps) {
            if (op instanceof OpErase || op instanceof OpRectErase) {
                hasEraseOp = true;
                break;
            }
        }
        // 绘制图形
        if (hasEraseOp) {
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

        // 绘制正在编辑的图片
        synchronized (picEditStuffLock) {
            if (null != picEditStuff) render(picEditStuff, canvas);
        }

    }


    private Bitmap shapeLayerSnapshot;
    private Bitmap picLayerSnapshot;
    /**
     * NOTE: 该方法需在API LEVEL >= 21时使用，21以下TextureView.getBitmap方法有bug，
     * @see <a href="https://github.com/mapbox/mapbox-gl-native/issues/4911">
     *     Android 4.4.x (KitKat) Hardware Acceleration Thread Bug #4911
     *     </a>
     * */
    private void snapshotWindow(Canvas canvas){
//            KLog.p("insertPicOps.isEmpty() = %s, shapeOps.isEmpty()=%s, isEmpty()=%s, picEditStuffs.isEmpty() = %s, " +
//                            "picLayerSnapshot=%s, shapeLayerSnapshot=%s, picEditingLayerSnapshot=%s",
//                    insertPicOps.isEmpty(), shapeOps.isEmpty(), isEmpty(), picEditStuffs.isEmpty(),
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

        Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
        if (null != picLayerSnapshot) {
            canvas.drawBitmap(picLayerSnapshot, 0, 0, paint);
        }
        if (null != shapeLayerSnapshot) {
            canvas.drawBitmap(shapeLayerSnapshot, 0, 0, paint);
        }

    }



    /* 相对于xhdpi的屏幕密度。
    因为TL和网呈已经实现数据协作在先，他们传过来的是原始的像素值，
    为了使得展示效果尽量在各设备上保持一致并兼顾TL和网呈已有实现，我们以TL的屏幕密度为基准算出一个相对密度，
    以该相对密度作为缩放因子进行展示。TL的屏幕密度接近xhdpi，故以xhdpi作为基准*/
    private float relativeDensity=1;
    private Matrix densityRelativeBoardMatrix = new Matrix();
    private Matrix getDensityRelativeBoardMatrix(){
        densityRelativeBoardMatrix.reset();
        densityRelativeBoardMatrix.postScale(relativeDensity, relativeDensity);
        densityRelativeBoardMatrix.postConcat(opWrapper.getLastMatrixOp().getMatrix());
        return  densityRelativeBoardMatrix;
    }

    /* 返回去掉放缩位移等matrix操作影响后的坐标*/
    private float[] mappedPoint= new float[2];
    private float[] getRidOfMatrix(float x, float y){
        mappedPoint[0] = x; mappedPoint[1] = y;
        MatrixHelper.invert(getDensityRelativeBoardMatrix()).mapPoints(mappedPoint);
        return mappedPoint;
    }


    DefaultTouchListener.IOnEventListener boardViewEventListener = new DefaultTouchListener.IOnEventListener(){
        private long timestamp = System.currentTimeMillis();
        private boolean bDragging = false;
        private boolean bScaling = false;
        private Matrix tmpMatrix = new Matrix();
        private Matrix confirmedMatrix = new Matrix();
        private IResultListener publishResultListener = new IResultListener() {
            @Override
            public void onSuccess(Object result) {
//                KLog.p("success to publish matrix op %s", result);
                confirmedMatrix.set(tmpMatrix);
            }
        };

        private OpMatrix update(){
            OpMatrix opMatrix = new OpMatrix(tmpMatrix);
            assignBasicInfo(opMatrix);
            opWrapper.addMatrixOp(opMatrix);
            return opMatrix;
        }

        private void publish(OpMatrix opMatrix){
            if (System.currentTimeMillis()-timestamp > 70) {
                timestamp = System.currentTimeMillis();
                // 每70ms发布一次
                if (null != onStateChangedListener) onStateChangedListener.onPaintOpGenerated(getBoardId(), opMatrix, publishResultListener, true);
            }else{
                if (null != onStateChangedListener) onStateChangedListener.onPaintOpGenerated(getBoardId(), null, null, true);
            }
        }

        private void rollback(){
            OpMatrix opMatrix = new OpMatrix(confirmedMatrix);
            assignBasicInfo(opMatrix);
            opWrapper.addMatrixOp(opMatrix);
            // 立即刷新
            if (null != onStateChangedListener) onStateChangedListener.onPaintOpGenerated(getBoardId(), null, null, true);
        }

        @Override
        public void onMultiFingerDragBegin() {
            bDragging = true;
            bDoingMatrixOp = true;
            tmpMatrix.set(opWrapper.getLastMatrixOp().getMatrix());
            confirmedMatrix.set(tmpMatrix);
        }

        @Override
        public void onMultiFingerDrag(float dx, float dy) {
            tmpMatrix.postTranslate(dx, dy);
            publish(update());
        }

        @Override
        public void onMultiFingerDragEnd() {
            OpMatrix opMatrix = update();
            if (null != onStateChangedListener) onStateChangedListener.onPaintOpGenerated(getBoardId(), opMatrix, new IResultListener() {
                @Override
                public void onArrive(boolean bSuccess) {
                    if (!bSuccess) {
                        KLog.p(KLog.ERROR,"failed to publish matrix op %s, rollback to %s", opMatrix, confirmedMatrix);
                        rollback(); // 发布失败则回退matrix
                    }
                }
            }, true);

            bDragging = false;
            if (!bScaling) bDoingMatrixOp = false;
        }

        @Override
        public void onScaleBegin() {
            bScaling = true;
            bDoingMatrixOp = true;
            tmpMatrix.set(opWrapper.getLastMatrixOp().getMatrix());
            confirmedMatrix.set(tmpMatrix);
        }

        @Override
        public void onScale(float factor, float focusX, float focusY) {
            float curZoomRate = MatrixHelper.getScale(tmpMatrix);
            float zoomRate = curZoomRate * factor;
            if (zoomRate < minZoomRate) {
                tmpMatrix.postScale(minZoomRate/curZoomRate, minZoomRate/curZoomRate, focusX, focusY);
            }else if (zoomRate > maxZoomRate){
                tmpMatrix.postScale(maxZoomRate/curZoomRate, maxZoomRate/curZoomRate, focusX, focusY);
            }else {
                tmpMatrix.postScale(factor, factor, focusX, focusY);
            }

            publish(update());

            zoomRateChanged();
        }

        @Override
        public void onScaleEnd() {
            OpMatrix opMatrix = update();
            if (null != onStateChangedListener) onStateChangedListener.onPaintOpGenerated(getBoardId(), opMatrix, new IResultListener() {
                @Override
                public void onArrive(boolean bSuccess) {
                    if (!bSuccess) {
                        KLog.p(KLog.ERROR,"failed to publish matrix op %s, rollback to %s", opMatrix, confirmedMatrix);
                        rollback(); // 发布失败则回退matrix
                    }
                }
            }, true);

            bScaling = false;
            if (!bDragging) bDoingMatrixOp = false;
        }
    };


    DefaultTouchListener.IOnEventListener shapeViewEventListener = new DefaultTouchListener.IOnEventListener(){

        @Override
        public void onDragBegin(float x, float y) {
            float[] pos = getRidOfMatrix(x, y);
            startShapeOp(pos[0], pos[1]);
        }

        @Override
        public void onDrag(float x, float y) {
            float[] pos = getRidOfMatrix(x, y);
            adjustShapeOp(pos[0], pos[1]);
            if (null != onStateChangedListener) onStateChangedListener.onPaintOpGenerated(getBoardId(), null, null,true);
        }

        @Override
        public void onDragEnd() {
            finishShapeOp();
            final OpPaint tmpOp = adjustingShapeOp;
            synchronized (adjustingShapeOpLock) {
                adjustingShapeOp = null;
            }
            tmpShapeOps.offerLast(tmpOp);
            if (null != onStateChangedListener) onStateChangedListener.onPaintOpGenerated(getBoardId(), tmpOp, new IResultListener() {

                        @Override
                        public void onArrive(boolean bSuccess) {
                            tmpShapeOps.remove(tmpOp); // 不论成功/失败/超时临时操作均已不需要，若成功该操作将被添加到“正式”的操作集，若失败则该操作被丢弃。
                            if (!bSuccess){
                                KLog.p(KLog.ERROR, "failed to publish shape op %s", tmpOp);
                                // 立即刷新
                                if (null != onStateChangedListener) onStateChangedListener.onPaintOpGenerated(getBoardId(), null, null, true);
                            }
                        }

                        @Override
                        public void onSuccess(Object result) {
                            dealShapeOp(tmpOp);
                        }

                    },
                    true /* 不同于undo/redo/clearscreen操作，绘制操作本端立即展示不等平台反馈结果以免展示效果出现迟滞。
                                       若平台反馈结果成功则保持现有展示的绘制不变，若平台没有反馈结果或者反馈失败则再清除该绘制。*/
            );

        }

    };


    private DefaultTouchListener.IOnEventListener picViewEventListener = new DefaultTouchListener.IOnEventListener(){
        private float preDragX, preDragY;

        private long timestamp = System.currentTimeMillis();
        private Matrix confirmedMatrix = new Matrix();
        private IResultListener publishResultListener = new IResultListener() {
            @Override
            public void onSuccess(Object result) {
                if (null != picEditStuff) {
                    confirmedMatrix.set(picEditStuff.matrix);
                }
            }
        };

        private void refreshDelIconState(float x, float y){
            Bitmap lastPic = picEditStuff.delIcon.getPic();
            if (picEditStuff.isInDelPicIcon(x, y)) {
                picEditStuff.delIcon.setPic(del_pic_active_icon);
            }else{
                picEditStuff.delIcon.setPic(del_pic_icon);
            }
            if (lastPic != picEditStuff.delIcon.getPic()) {
                // 刷新
                if (null != onStateChangedListener)
                    onStateChangedListener.onPaintOpGenerated(getBoardId(), null, null, true);
            }
        }


        private void publish(OpDragPic opDragPic){
            if (System.currentTimeMillis()-timestamp > 70) {
                timestamp = System.currentTimeMillis();
                // 每70ms发布一次
                if (null != onStateChangedListener) onStateChangedListener.onPaintOpGenerated(getBoardId(), opDragPic, publishResultListener, true);
            }else{
                if (null != onStateChangedListener) onStateChangedListener.onPaintOpGenerated(getBoardId(), null, null, true);
            }
        }

        private void rollback(){
            if (null != picEditStuff) {
                picEditStuff.matrix.set(confirmedMatrix);
                // 立即刷新
                if (null != onStateChangedListener)
                    onStateChangedListener.onPaintOpGenerated(getBoardId(), null, null, true);
            }
        }




        @Override
        public boolean onDown(float x, float y) {
            if (0 == opWrapper.getInsertPicOpsCount() && null == picEditStuff){
                return false; // 当前没有图片不用处理后续事件
            }
            if (null != picEditStuff){
                handler.removeCallbacks(finishEditPicRunnable);
                float[] pos = getRidOfMatrix(x, y);
                refreshDelIconState(pos[0], pos[1]);
            }
            return true;
        }

        @Override
        public void onUp(float x, float y) {
            if (null != picEditStuff) {
                float[] pos = getRidOfMatrix(x, y);
                if (picEditStuff.isInDelPicIcon(pos[0], pos[1])) {
                    delEditingPic();
                }else{
                    handler.postDelayed(finishEditPicRunnable, 5000);
                }
            }
        }


        @Override
        public void onLongPress(float x, float y) {
            float[] pos = getRidOfMatrix(x, y);
            if (null!=picEditStuff){
                if (picEditStuff.contains(pos[0], pos[1])){
                    return;
                }else {
                    finishEditPic();
                }
            }
            OpInsertPic opInsertPic = opWrapper.selectPic(pos[0], pos[1]);
            if (null != opInsertPic){
                OpDeletePic opDeletePic = new OpDeletePic(new String[]{opInsertPic.getPicId()});
                assignBasicInfo(opDeletePic);
                opWrapper.addPicOp(opDeletePic); // 编辑图片的操作我们认为是先删除图片（本地删除不走发布），然后编辑完成再插入图片。

                startEditPic(Sets.newHashSet(opInsertPic));
            }

            // SEALED 编辑多个图片
//            Collection<OpInsertPic> opInsertPics = opWrapper.getInsertPicOps();
//            startEditPic(opInsertPics);
//            opWrapper.addPicOp(createDelPicOp(opInsertPics));
        }

        @Override
        public void onSecondPointerDown(float x, float y) {
            if (null != picEditStuff) {
                if (picEditStuff.delIcon.getPic() != del_pic_icon) {
                    picEditStuff.delIcon.setPic(del_pic_icon);
                    if (null != onStateChangedListener)
                        onStateChangedListener.onPaintOpGenerated(getBoardId(), null, null, true);
                }
            }
        }

        @Override
        public void onLastPointerLeft(float x, float y) {
            if (null != picEditStuff) {
                float[] pos = getRidOfMatrix(x, y);
                refreshDelIconState(pos[0], pos[1]);
            }
        }

        @Override
        public void onSingleTap(float x, float y) {
            if (null != picEditStuff) {
                float[] pos = getRidOfMatrix(x, y);
                if (!picEditStuff.contains(pos[0], pos[1])){
                    finishEditPic();
                }
            }
        }


        @Override
        public void onDragBegin(float x, float y) {
            if (null != picEditStuff){
                confirmedMatrix.set(picEditStuff.matrix);
                preDragX = x; preDragY = y;
            }
        }

        @Override
        public void onDrag(float x, float y) {
//            KLog.p("onDrag tmp pic layer, x=%s. y=%s", x, y);
            if (null != picEditStuff){
                picEditStuff.matrix.postTranslate(x-preDragX, y-preDragY);
                preDragX = x; preDragY = y;
                publish(createDragPicOp(picEditStuff.pics, picEditStuff.matrix));
            }
        }

        @Override
        public void onDragEnd() {
            if (null != picEditStuff) {
                OpDragPic dragPicOp = createDragPicOp(picEditStuff.pics, picEditStuff.matrix);
                if (null != onStateChangedListener)
                    onStateChangedListener.onPaintOpGenerated(getBoardId(), dragPicOp, new IResultListener() {
                        @Override
                        public void onArrive(boolean bSuccess) {
                            if (!bSuccess) {
                                KLog.p(KLog.ERROR, "failed to publish pic matrix op %s, rollback to %s", dragPicOp, confirmedMatrix);
                                rollback(); // 发布失败则回退matrix
                            }
                        }
                    }, true);
            }
        }


        @Override
        public void onScaleBegin() {
            if (null != picEditStuff){
                confirmedMatrix.set(picEditStuff.matrix);
            }
        }

        @Override
        public void onScale(float factor, float focusX, float focusY) {
            if (null != picEditStuff){
                picEditStuff.matrix.postScale(factor, factor, focusX, focusY);
                publish(createDragPicOp(picEditStuff.pics, picEditStuff.matrix));
            }
        }

        @Override
        public void onScaleEnd() {
            if (null != picEditStuff) {
                OpDragPic dragPicOp = createDragPicOp(picEditStuff.pics, picEditStuff.matrix);
                if (null != onStateChangedListener)
                    onStateChangedListener.onPaintOpGenerated(getBoardId(), dragPicOp, new IResultListener() {
                        @Override
                        public void onArrive(boolean bSuccess) {
                            if (!bSuccess) {
                                KLog.p(KLog.ERROR, "failed to publish pic matrix op %s, rollback to %s", dragPicOp, confirmedMatrix);
                                rollback(); // 发布失败则回退matrix
                            }
                        }
                    }, true);
            }

        }

    };



    private void startShapeOp(float x, float y){
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

    private void adjustShapeOp(float x, float y){
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


    private void finishShapeOp(){
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



    private void startEditPic(Collection<OpInsertPic> opInsertPics){

        // 在图片外围绘制一个虚线矩形框
        OpDrawRect opDrawRect = new OpDrawRect();
        RectF dashRect = new RectF(opWrapper.calcBoundary(opInsertPics));
        dashRect.inset(-5, -5);
        opDrawRect.setValues(dashRect);
        opDrawRect.setLineStyle(OpDraw.DASH);
        opDrawRect.setStrokeWidth(2);
        opDrawRect.setColor(0xFF08b1f2L);

        // 在虚线矩形框正下方绘制删除图标
        OpInsertPic delPicIcon = new OpInsertPic();
        delPicIcon.setPic(del_pic_icon);
        Matrix matrix = new Matrix();
        matrix.postTranslate(dashRect.left+(dashRect.width()-del_pic_icon.getWidth())/2f,dashRect.bottom+8);
        Matrix boardMatrix = opWrapper.getLastMatrixOp().getMatrix();
        matrix.postScale(1/MatrixHelper.getScaleX(boardMatrix), 1/MatrixHelper.getScaleY(boardMatrix), dashRect.centerX(), dashRect.bottom); // 使图标以正常尺寸展示，不至于因画板缩小/放大而过小/过大
        delPicIcon.setMatrix(matrix);

        MyConcurrentLinkedDeque<OpInsertPic> editPics = new MyConcurrentLinkedDeque<>();
        for (OpInsertPic opInsertPic : opInsertPics) {
            KLog.p("edit pic %s", opInsertPic);
            editPics.offerLast(opInsertPic);
        }
        picEditStuff = new PicEditStuff(editPics, delPicIcon, opDrawRect);

        focusedLayer = LAYER_PIC;

        if (null != onStateChangedListener) onStateChangedListener.onPaintOpGenerated(getBoardId(), null, null,true);

    }



    private void delEditingPic(){
        if (null == picEditStuff){
            KLog.p(KLog.ERROR,"null == picEditStuff");
            return;
        }
        PicEditStuff editStuff = picEditStuff;
        synchronized (picEditStuffLock) {
            picEditStuff = null;
        }

        if (bInsertingPic) {
            // 如果是正在插入中就删除就不用走发布，仅刷新本端界面
            if (null != onStateChangedListener) onStateChangedListener.onPaintOpGenerated(getBoardId(), null, null,true);
            bInsertingPic = false;
        }else{
            if (null != onStateChangedListener) onStateChangedListener.onPaintOpGenerated(getBoardId(), createDelPicOp(editStuff.pics), null,true);

            picCountChanged();
        }

        focusedLayer = LAYER_ALL;

    }



    private void finishEditPic(){
        if (null == picEditStuff){
            KLog.p(KLog.ERROR,"null == picEditStuff");
            return;
        }

        PicEditStuff editStuff = picEditStuff;
        synchronized (picEditStuffLock) {
            picEditStuff = null;
        }
        MyConcurrentLinkedDeque<OpInsertPic> pics = new MyConcurrentLinkedDeque<>();
        while(!editStuff.pics.isEmpty()) {
            OpInsertPic opInsertPic = editStuff.pics.pollFirst();
            opInsertPic.getMatrix().postConcat(editStuff.matrix);
            opWrapper.addPicOp(opInsertPic);
            pics.offerLast(opInsertPic);
        }

        // 发布
        if (bInsertingPic) {
            // 正在插入图片
            if (null != onStateChangedListener) {
                while (!pics.isEmpty()) {
                    OpInsertPic opInsertPic = pics.pollFirst();

                    /*求取transMatrix
                     * transMatrix = 1/mixMatrix * (picMatrix * boardMatrix)
                     * NOTE: 己端的策略插入图片时是把图片的缩放位移信息全部放入transMatrix中，插入点恒为(0,0)，宽高恒为原始宽高。
                     * 所以mixMatrix为单位矩阵，所以 transMatrix = picMatrix * boardMatrix
                     * */
                    opInsertPic.setBoardMatrix(opWrapper.getLastMatrixOp().getMatrix());
                    Matrix transMatrix = new Matrix(opInsertPic.getMatrix());
                    transMatrix.postConcat(opInsertPic.getBoardMatrix());
                    opInsertPic.setTransMatrix(transMatrix);

                    onStateChangedListener.onPaintOpGenerated(getBoardId(), opInsertPic, null, true);
                }
            }

            // 通知用户图片数量变化
            picCountChanged();

            bInsertingPic = false;

        } else {
            // 正在拖动放缩图片
            if (null != onStateChangedListener)
                onStateChangedListener.onPaintOpGenerated(getBoardId(), createDragPicOp(pics), null, true);
        }

        focusedLayer = LAYER_ALL;

    }

    private OpDragPic createDragPicOp(Collection<OpInsertPic> opInsertPicSet){
        return createDragPicOp(opInsertPicSet, new Matrix());
    }

    private OpDragPic createDragPicOp(Collection<OpInsertPic> opInsertPicSet, Matrix matrix){
        if (opInsertPicSet.isEmpty()){
            return null;
        }
        Map<String, Matrix> picMatrices = new HashMap<>();
        for (OpInsertPic opInsertPic : opInsertPicSet) {
             /* 求取dragMatrix。
             mixMatrix*dragMatrix = picMatrix * boardMatrix
             * => dragMatrix = 1/mixMatrix * (picMatrix * boardMatrix)
             * */
            Matrix dragMatrix = new Matrix(opInsertPic.getMatrix());
            dragMatrix.postConcat(matrix);
            dragMatrix.postConcat(opWrapper.getLastMatrixOp().getMatrix());
            dragMatrix.preConcat(MatrixHelper.invert(opInsertPic.getMixMatrix()));
            picMatrices.put(opInsertPic.getPicId(), dragMatrix);
        }
        OpDragPic opDragPic = new OpDragPic(picMatrices);
        assignBasicInfo(opDragPic);

        return opDragPic;
    }


    private OpDeletePic createDelPicOp(Collection<OpInsertPic> opInsertPicSet){
        if (opInsertPicSet.isEmpty()){
            return null;
        }
        List<String> delPics = new ArrayList<>();
        for(OpInsertPic opInsertPic : opInsertPicSet) {
            delPics.add(opInsertPic.getPicId());
        }
        OpDeletePic opDeletePic = new OpDeletePic(delPics.toArray(new String[]{}));
        assignBasicInfo(opDeletePic);
        return opDeletePic;
    }


    private void assignBasicInfo(OpPaint op){
        op.setConfE164(boardInfo.getConfE164());
        op.setBoardId(boardInfo.getId());
        op.setPageId(boardInfo.getPageId());
    }



    static int editedPicCount=0;
    private class PicEditStuff{
        int id;
        MyConcurrentLinkedDeque<OpInsertPic> pics;
        OpInsertPic delIcon;
        OpDrawRect dashedRect;
        Matrix matrix;

        PicEditStuff(MyConcurrentLinkedDeque<OpInsertPic> pics, OpInsertPic delIcon, OpDrawRect dashedRect) {
            id = editedPicCount++;
            this.pics = pics;
            this.delIcon = delIcon;
            this.dashedRect = dashedRect;
            matrix = new Matrix();
        }

        boolean contains(float x, float y){
            return isInDashedRect(x, y) || isInDelPicIcon(x, y);
        }

        boolean isInDashedRect(float x, float y){
            RectF rectF = new RectF(dashedRect.boundary());
            matrix.mapRect(rectF);
            return rectF.contains(x, y);
        }

        boolean isInDelPicIcon(float x, float y){
            RectF rectF = new RectF(delIcon.boundary());
            matrix.mapRect(rectF);
            return rectF.contains(x, y);
        }

        RectF bound(){
            RectF bound = new RectF();
            bound.union(delIcon.boundary());
            bound.union(dashedRect.boundary());
            return bound;
        }

        private boolean existsPic(String picId){
            for (OpInsertPic pic : pics){
                if (pic.getPicId().equals(picId)) {
                    return true;
                }
            }
            return false;
        }

        private boolean delPic(String picId){
            Iterator<OpInsertPic> it = pics.iterator();
            while (it.hasNext()){
                OpInsertPic opInsertPic = it.next();
                if (opInsertPic.getPicId().equals(picId)){
                    it.remove();
                    return true;
                }
            }

            return false;
        }

    }



    private boolean dealShapeOp(OpPaint shapeOp){

        if (!opWrapper.addShapeOp(shapeOp)) return false;

        if (null != onStateChangedListener){
            onStateChangedListener.onChanged(getBoardId());
            onStateChangedListener.onRepealableStateChanged(getBoardId(), opWrapper.getRepealedOpsCount(),opWrapper.getRepealableOpsCount());
            refreshEmptyState();
        }

        return true;

    }


    private boolean dealControlOp(OpPaint op){

        if (!opWrapper.addControlOp(op)) return false;

        if (null != onStateChangedListener){
            onStateChangedListener.onChanged(getBoardId());
            onStateChangedListener.onRepealableStateChanged(getBoardId(), opWrapper.getRepealedOpsCount(),opWrapper.getRepealableOpsCount());
            refreshEmptyState();
        }

        return true;
    }


    private boolean dealMatrixOp(OpMatrix op){
        if (bDoingMatrixOp){
            // 本地正在执行matrix操作则屏蔽外来的matrix操作
            return false;
        }

        // 更新画板matrix
        if (!opWrapper.addMatrixOp(op)) return false;

        zoomRateChanged();

        return true;
    }

    private boolean dealPicOp(OpPaint picOp){
        if (!opWrapper.addPicOp(picOp)) return false;

        if (null != onStateChangedListener) {
            onStateChangedListener.onChanged(getBoardId());
            if (EOpType.INSERT_PICTURE == picOp.getType()
                    || EOpType.DELETE_PICTURE == picOp.getType()) {
                onStateChangedListener.onPictureCountChanged(getBoardId(), getPicCount());
                refreshEmptyState();
            }
        }

        return true;
    }



    /**
     * 接收绘制操作
     * @return true 需要刷新，false不需要。
     * */
    boolean onPaintOp(OpPaint op){
        String boardId = op.getBoardId();
        if(!boardId.equals(getBoardId())){
            KLog.p(KLog.ERROR,"op %s is not for %s", op, getBoardId());
            return false;
        }
        KLog.p("recv op %s", op);

        switch (op.getType()){
            case INSERT_PICTURE:
                OpInsertPic opInsertPic = (OpInsertPic) op;
                opInsertPic.setBoardMatrix(opWrapper.getLastMatrixOp().getMatrix());
                if (null == opInsertPic.getPic()
                        && null != opInsertPic.getPicPath()) {
                    opInsertPic.setPic(BitmapFactory.decodeFile(opInsertPic.getPicPath())); // XXX TODO 优化。比如大分辨率图片裁剪
                }
                Bitmap pic = opInsertPic.getPic();
                if (null != pic){
                    /*计算mixMatrix。
                    可将mixMatrix理解为：把左上角在原点处的未经缩放的图片变换为对端所描述的图片（插入图片时传过来的insertPos, picWidth, picHeight这些信息综合起来所描述的图片）
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

                return dealPicOp(opInsertPic);

            case DELETE_PICTURE:
                boolean bSuccess = dealPicOp(op);
                if (!bSuccess && null != picEditStuff){
                    for (String picId : ((OpDeletePic) op).getPicIds()) {
                        if (picEditStuff.delPic(picId)){
                            bSuccess = true;
                        }
                    }
                    if (picEditStuff.pics.isEmpty()){
                        synchronized (picEditStuffLock) {
                            picEditStuff = null;
                        }
                    }
                }
                return bSuccess;

            case DRAG_PICTURE:
            case UPDATE_PICTURE:
                return dealPicOp(op);

            case FULLSCREEN_MATRIX:
                return dealMatrixOp((OpMatrix) op);

            case UNDO:
            case REDO:
                return dealControlOp(op);

            default:
                return dealShapeOp(op);
        }

    }


    void paint(){
        KLog.p("=> board=%s", getBoardId());
        Matrix matrix = getDensityRelativeBoardMatrix();

        // 图形层绘制
        Canvas shapePaintViewCanvas = shapePaintView.lockCanvas();
        if (null != shapePaintViewCanvas) {
            // 每次绘制前先清空画布以避免残留
            shapePaintViewCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

            // 设置画布matrix
            shapePaintViewCanvas.setMatrix(matrix);

            // 图形绘制
            render(opWrapper.getShapeOps(), shapePaintViewCanvas);

            // 临时图形绘制
            render(tmpShapeOps, shapePaintViewCanvas);

            // 绘制正在调整中的操作
            synchronized (adjustingShapeOpLock) {
                if (null != adjustingShapeOp) render(adjustingShapeOp, shapePaintViewCanvas);
            }

            // 绘制正在编辑中的图片（编辑中的图片展示在图形上层）
            synchronized (picEditStuffLock) {
                if (null != picEditStuff) render(picEditStuff, shapePaintViewCanvas);
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
            render(opWrapper.getInsertPicOps(), picPaintViewCanvas);

        }

        // 提交绘制任务，执行绘制
//                KLog.p("go render!");
        if (null != shapePaintViewCanvas) shapePaintView.unlockCanvasAndPost(shapePaintViewCanvas);
        if (null != picPaintViewCanvas) picPaintView.unlockCanvasAndPost(picPaintViewCanvas);

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
//        KLog.p("to render %s", op);
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

    private void render(Collection<? extends OpPaint> ops, Canvas canvas){
        for (OpPaint op : ops) {  //NOTE: Iterators are weakly consistent. 此遍历过程不感知并发的添加操作，但感知并发的删除操作。
            render(op, canvas);
        }
    }

    private void render(PicEditStuff picEditStuff, Canvas canvas){
        canvas.save();

        canvas.concat(picEditStuff.matrix);
        render(picEditStuff.delIcon, canvas);
        render(picEditStuff.dashedRect, canvas);
        render(picEditStuff.pics, canvas);

        canvas.restore();
    }


    interface IOnStateChangedListener{
        /**
         * 生成了绘制操作。（主动绘制）
         * @param Op 绘制操作
         * @param publishResultListener 发布结果监听器。（生成的绘制操作可发布给其他与会方）
         * @param bNeedRefresh 是否需要刷新
         * */
        default void onPaintOpGenerated(String boardId, OpPaint Op, IResultListener publishResultListener, boolean bNeedRefresh){}

        /**
         * 画板状态发生了改变。
         * 此回调是下面所有回调的先驱，方便用户做一些公共处理。
         * 如：用户可根据该回调决定是否需要重新“快照”{@link #snapshot(int, int, int, ISnapshotResultListener)}。
         * */
        default void onChanged(String boardId){}

//        /**图形操作数量变化
//         * @param count  当前图形操作数量*/
//        default void onShapeOpCountChanged(String boardId, int count){}

        /**图片数量变化
         * @param count  当前图片数量*/
        default void onPictureCountChanged(String boardId, int count){}

        int PIC_STATE_IDLE = 0;
        /**
         * 图片被选中
         * */
        int PIC_STATE_SELECTED = 1;
        /**
         * 图片正在进行拖拽放缩等操作
         * */
        int PIC_STATE_MATRIXING = 2;
        /**
         * 图片状态发生变化。如图片被选中，图片被拖动，被放缩。
         * @param picName 图片名称
         * @param state 图片当前状态
         * @param zoomRate 图片放缩比率，如50为50%
         * */
        default void onPicStateChanged(String boardId, String picName, int state, int zoomRate){}

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




    private class OpWrapper{

        /**
         * 所有操作（忠实于现场，致力于保存所有细节，可用于“录制/回放”）。
         * NOTE: 所有保存于此的操作均是经平台确认过生效的操作，不包括临时操作，如本端正在进行的图形绘制，正在进行的图片编辑。
         * */
        private MyConcurrentLinkedDeque<OpPaint> ops = new MyConcurrentLinkedDeque<>();

        /**
         * 图形操作
         * */
        private MyConcurrentLinkedDeque<OpPaint> shapeOps = new MyConcurrentLinkedDeque<>();
        /**
         * 插入图片操作
         * */
        private MyConcurrentLinkedDeque<OpInsertPic> insertPicOps = new MyConcurrentLinkedDeque<>();
        /**
         * matrix操作
         * */
        private MyConcurrentLinkedDeque<OpMatrix> matrixOps = new MyConcurrentLinkedDeque<>();
        /**
         * 已撤销操作
         * */
        private Stack<OpPaint> repealedOps = new Stack<>();

        /**
         * 已撤销操作数量。
         * NOTE: 该值不一定等于{@link #repealedOps}的size，因为根据需求该值会在一些场景下被重置。
         * */
        private int repealedOpsCount;

        /**
         * 图形操作数量。
         * 因为MyConcurrentLinkedDeque.size需要遍历队列且shapeOps可能数目较大则较低效，所以自行维护计数。
         * */
        private int shapeOpsCount;


        /***********************************************************************************
         * NOTE: 以下getXXX系列方法返回的集合被设计为仅用于读操作，请勿对返回的对象执行修改操作。
         * ************************************************************************************/

        MyConcurrentLinkedDeque<OpPaint> getShapeOps(){
            return shapeOps;
        }

        /**
         * 获取最后一次清屏操作之后的图形操作。（目前清屏仅针对图形）
         * */
        MyConcurrentLinkedDeque<OpPaint> getShapeOpsAfterCls(){
            MyConcurrentLinkedDeque<OpPaint> allShapeOps = new MyConcurrentLinkedDeque<>();
            MyConcurrentLinkedDeque<OpPaint> shapeOpsAfterCls = new MyConcurrentLinkedDeque<>();
            allShapeOps.addAll(shapeOps);
            while (!allShapeOps.isEmpty()){
                OpPaint op = allShapeOps.pollLast();
                if (EOpType.CLEAR_SCREEN == op.getType()){
                    break;
                }
                shapeOpsAfterCls.offerFirst(op);
            }
            return shapeOpsAfterCls;
        }

        MyConcurrentLinkedDeque<OpInsertPic> getInsertPicOps(){
            return insertPicOps;
        }

        MyConcurrentLinkedDeque<OpMatrix> getMatrixOps(){
            return matrixOps;
        }

        OpMatrix getLastMatrixOp(){
            return matrixOps.peekLast();
        }

        MyConcurrentLinkedDeque<OpPaint> getAllOps(){
            return ops;
        }


        int getShapeOpsCount(){
            return shapeOpsCount;
        }

        /**
         * 获取可被撤销的操作的数量
         * */
        int getRepealableOpsCount(){
            return shapeOpsCount;
        }

        /**
         * 获取已被撤销的操作数量
         * */
        int getRepealedOpsCount(){
            return repealedOpsCount;
        }

        /**
         * 获取真实的历来已被撤销的操作的数量。
         * 不同于{@link #getRepealedOpsCount()}可能被重置（需求要求某些场景需重置可撤销操作计数），
         * 该方法获取的是历来所有已被撤销未恢复的操作。
         * */
        int getRealRepealedOpsCount(){
            return repealedOps.size();
        }

        int getInsertPicOpsCount(){
            return insertPicOps.size();
        }

        boolean isClear(){
            return shapeOps.isEmpty() || shapeOps.peekLast() instanceof OpClearScreen;
        }

        boolean isEmpty(){
            return insertPicOps.isEmpty() && isClear();
        }


        /********************************************************
         * 以下addXXX系列接口为添加各种操作，没有相应的delXXX接口
         * ****************************************************/

        /**
         * 添加图形操作。包括画线、画圆、擦除、清屏等。
         * */
        boolean addShapeOp(OpPaint op){

            ops.offerLast(op);

            if (EOpType.CLEAR_SCREEN==op.getType() && isClear()){
                KLog.p(KLog.ERROR, "already cleared");
                return false;
            }
            shapeOps.offerLast(op);
            ++shapeOpsCount;
            if (op instanceof IRepealable) {
                repealedOpsCount=0; // 有新的可撤销操作加入时重置已撤销操作数量（需求要求）
            }
            return true;
        }


        /**
         * 添加图片操作。包括插入/删除/拖动/放缩图片等。目前仅入队插入/删除图片。
         * // TODO 若将来需要录制/回放功能则所有中间效果的操作均需保存。
         * */
        boolean addPicOp(OpPaint op){

            if (EOpType.INSERT_PICTURE==op.getType()){

                ops.offerLast(op); // 保存插入图片操作

                OpInsertPic opInsertPic = (OpInsertPic) op;

                for (OpInsertPic insertPic : insertPicOps){
                    if (insertPic.getPicId().equals(opInsertPic.getPicId())){
                        KLog.p(KLog.ERROR, "pic op %s already exist!", opInsertPic);
                        return false;
                    }
                }

                insertPicOps.offerLast(opInsertPic);

                return true;

            }else if (EOpType.DELETE_PICTURE==op.getType()){

                ops.offerLast(op); // 保存删除图片操作

                OpDeletePic opDeletePic = (OpDeletePic) op;
                boolean bSuccess = false;
                for (String picId : opDeletePic.getPicIds()) {
                    boolean bDel = false;
                    Iterator it = insertPicOps.iterator();
                    while (it.hasNext()) {
                        OpInsertPic picOp = (OpInsertPic) it.next();
                        if (picOp.getPicId().equals(picId)) {
                            it.remove();
                            bDel = true;
                            bSuccess = true;
                            break;
                        }
                    }
                    if (!bDel){
                        KLog.p(KLog.ERROR, "del pic %s failed", picId);
                    }
                }
                if (!bSuccess){
                    KLog.p(KLog.ERROR, "del pics failed: %s ", opDeletePic);
                    return false;
                }

                return true;

            }else if (EOpType.DRAG_PICTURE==op.getType()){

//                ops.offerLast(op); // 目前不支持“录制/回放”功能，所以拖动图片这类中间效果操作暂不保存

                boolean bEffective = false;
                OpDragPic opDragPic = (OpDragPic) op;
                for (Map.Entry<String, Matrix> dragOp : opDragPic.getPicMatrices().entrySet()) {
                    for (OpPaint picOp : insertPicOps) {
                        OpInsertPic opInsertPic = (OpInsertPic) picOp;
                        if (opInsertPic.getPicId().equals(dragOp.getKey())) {
                            if (null != opInsertPic.getPic()) {
                                /*图片已经准备好了则直接求取picmatrix
                                 * mixMatrix * dragMatrix = picMatrix * boardMatrix
                                 * => picMatrix = mixMatrix * dragMatrix / boardMatrix
                                 * */
                                Matrix matrix = new Matrix(opInsertPic.getMixMatrix());
                                matrix.postConcat(dragOp.getValue());
                                matrix.postConcat(MatrixHelper.invert(getLastMatrixOp().getMatrix()));
                                if (!matrix.equals(opInsertPic.getMatrix())) {
                                    // 更新图片的matrix
                                    opInsertPic.setMatrix(matrix);
                                    bEffective = true;
                                }else {
                                    // 计算出的matrix跟当前matrix相等则不需拖动
                                    KLog.p(KLog.WARN, "drag pic %s no effect, matrix no change", opInsertPic.getPicId());
                                }
                            }else{
                                // 图片尚未准备好，我们先保存matrix，等图片准备好了再计算
                                opInsertPic.setDragMatrix(dragOp.getValue());
                                opInsertPic.setBoardMatrix(getLastMatrixOp().getMatrix());
                                KLog.p(KLog.WARN, "drag pic %s no effect, pic is null", opInsertPic.getPicId());
                            }
                            break;
                        }
                    }
                }

                return bEffective;

            }else if (EOpType.UPDATE_PICTURE==op.getType()){
                boolean bSuccess = false;
                OpUpdatePic opUpdatePic = (OpUpdatePic) op;
                for (OpPaint picOp : insertPicOps) {
                    OpInsertPic opInsertPic = (OpInsertPic) picOp;
                    if (opInsertPic.getPicId().equals(opUpdatePic.getPicId())) {
                        opInsertPic.setPicPath(opUpdatePic.getPicSavePath()); // 更新图片路径
                        Bitmap pic = BitmapFactory.decodeFile(opInsertPic.getPicPath());
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

                            opInsertPic.setMatrix(picMatrix); // 更新图片matrix

                            bSuccess = true;
                        }else{
                            KLog.p(KLog.ERROR, "pic is null!");
                        }

                        break;
                    }
                }

                if (!bSuccess){
                    KLog.p(KLog.ERROR, "update pic failed: %s ", opUpdatePic);
                    return false;
                }
                return true;

            } else{
                KLog.p(KLog.ERROR, "unknown pic op %s", op);
                return false;
            }

        }


        /**
         * 添加控制操作。包括撤销/恢复。
         * */
        boolean addControlOp(OpPaint op){
            ops.offerLast(op);

            if (EOpType.UNDO==op.getType()){
                OpPaint shapeOp = shapeOps.pollLast(); // 撤销最近的操作（目前仅图形操作支持撤销）
                if (null == shapeOp){
                    KLog.p(KLog.ERROR, "no op to repeal");
                    return false;
                }
                --shapeOpsCount;
                repealedOps.push(shapeOp); // 缓存撤销的操作以供恢复
                ++repealedOpsCount;
            }else if (EOpType.REDO==op.getType()){
                if (repealedOps.isEmpty()){
                    KLog.p(KLog.ERROR, "no op to restore");
                    return false;
                }
                OpPaint repealedOp = repealedOps.pop();
                if (--repealedOpsCount<0) repealedOpsCount = 0;
                shapeOps.offerLast(repealedOp); // 恢复最近被撤销的操作
                ++shapeOpsCount;
            }else{
                KLog.p(KLog.ERROR, "unknown control op %s", op);
                return false;
            }

            return true;
        }

        /**
         * 添加matrix操作
         * */
        boolean addMatrixOp(OpMatrix op){

//            ops.offerLast(op); // 目前不支持“录制/回放”功能，所以拖动放缩这类中间效果操作暂不保存

            if (EOpType.FULLSCREEN_MATRIX != op.getType()){
                return false;
            }

            matrixOps.offerLast(op);
            if (matrixOps.size()>1) {
                matrixOps.pollFirst(); //NOTE: 目前仅保存一个，后续如果要支持“录制/回放”功能则需保存matrix操作序列。
            }

            return true;
        }


        /**计算操作集合的边界
         * */
        private RectF calcBoundary(Collection<? extends OpPaint> ops){
            if (null == ops || ops.isEmpty()){
                return null;
            }
            RectF bound = null;
            for (OpPaint op : ops){
                if (!(op instanceof IBoundary)){
                    continue;
                }
//                KLog.p("op =%s", op);
                if (null == bound){
                    bound = new RectF();
                    bound.set(((IBoundary) op).boundary());
                    continue;
                }
//                KLog.p("1 bound=%s", bound);
                bound.union(((IBoundary) op).boundary());
//                KLog.p("2 bound=%s", bound);
            }

            return bound;
        }

        private OpInsertPic selectPic(float x, float y){
            Iterator<OpInsertPic> it = insertPicOps.descendingIterator();
            while (it.hasNext()){
                OpInsertPic opInsertPic = it.next();
                if (null == opInsertPic.getPic()){
                    continue; // 图片操作有但图片可能还未获取到（如，协作方上传图片尚未完成）
                }
                if (opInsertPic.boundary().contains(x, y)){
                    return opInsertPic;
                }
            }
            return null;
        }


    }

}
