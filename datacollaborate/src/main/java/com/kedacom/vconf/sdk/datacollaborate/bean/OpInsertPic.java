package com.kedacom.vconf.sdk.datacollaborate.bean;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.RectF;

import androidx.annotation.NonNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


public class OpInsertPic extends OpPaint implements IBoundary{
    private String picId = UUID.randomUUID().toString();
    private String picName;
    private String picPath;
    private Bitmap pic;

    /*图片插入点。
    * NOTE: 网呈和TL传的该插入点并非原始插入点会进行一些倍数缩放后传过来；
    * 己端传的该插入点恒为(0,0)
    * */
    private PointF insertPos = new PointF();
    /*
    * 图片宽高。
    * NOTE: 网呈和TL传的该宽高并非图片的原始宽高会进行一些倍数缩放后传过来；
    * 己端传的该宽高恒为图片的原始宽高
    * */
    private int picWidth;
    private int picHeight;

    /*
    * 传输的matrix。
    * NOTE: 网呈和TL传的该matrix为一种混合体，含义混沌不明，跟网呈的具体实现强相关。
    * 通过分析网呈和TL代码发现满足关系：
    * 先canvas.setMatrix(transMatrix)，再canvas.drawBitmap(bitmap, dstRect, )，
     * 其中dstRect由insertPos和picWidth、picHeight得来，能正确展示图片。
    *
    * 己端传的该Matrix为包含了图片完整缩放位移信息的：picMatrix*boardMatrix
    * */
    private Matrix transMatrix = new Matrix();
    /*以上为网呈针对插入图片定义的数据结构。
      作为协议，它不通用（跟网呈具体实现强相关），相对于它需要传递的信息而言（图片的位置信息），难以置信的复杂晦涩，难以置信的草率。
     这大大增加了图片这块的复杂度，严重制约了其它终端在图片这块的可选实现方案，
     然而网呈实现在先不会改，只能向其对齐
     * */




    /*
     以下为方便己端处理而定义的一些成员。
    * NOTE: 针对图片展示，己端的策略是将图片最终位置信息分成两部分：picMatrix和boardMatrix，
    * 图片最终展示位置=picMatrix*boardMatrix，其中boardMatrix是画板的缩放位移信息，就是全屏缩放影响的那个matrix，
    * picMatrix是图片本身的缩放位移信息，在插入图片时以及编辑图片时（拖动旋转等）产生的matrix都计入picMatrix。
    * 按网呈和TL定的协议，picMatrix没有直接传过来而是混杂在"insertPos、picWidth、picHeight、transMatrix以及图片本身的宽高"这几个变量中，
    * 己端需通过这几个变量计算得到picMatrix。
    * */


    /*
    * 根据insertPos、picWidth、picHeight以及图片实际宽高计算出的matrix。
    *
    * 分析网呈和TL代码，图片位置 = mix(insertPos, picWidth, picHeight) * transMatrix，
    * 记为mixMatrix * transMatrix。
    * 其中transMatrix即为网呈插入图片时传来的matrix。
     * 因为己端策略为
    * 图片位置= picMatrix * boardMatrix
    * 所以得到等式：mixMatrix * transMatrix == picMatrix * boardMatrix
    * NOTE: 此等式是己端进行图片位置相关计算的基础。
    *
    * 可将mixMatrix理解为：把左上角在原点处的未经缩放的图片变换为对端所描述的图片（插入图片时传过来的
    * insertPos, picWidth, picHeight这些信息所描述的图片）所需经历的矩形变换
    *
    * mixMatrix是不变的，换句话说插入图片操作中的insertPos, picWidth, picHeight都是不变的。
    * */
    private Matrix mixMatrix = new Matrix();

    /* picMatrix。图片本身的matrix， 己端计算得来。
    picMatrix = mixMatrix * transMatrix / boardMatrix
    （图片最终位置=picMatrix * boardMatrix）*/
    private Matrix matrix = new Matrix();

    /*
    * 计算picMatrix时需要的boardMatrix。
    * 之所以需要保存该boardMatrix是因为计算picMatrix有可能是延后的，因为网呈传过来的数据所限，
    * 计算picMatrix我们需要图片的实际宽高，而图片实际宽高需等到图片下载完成。
    * */
    private Matrix boardMatrix = new Matrix();

    /* 最后一次图片拖动传过来的matrix。
     * 主要用来应付如下场景：
     * 图片拖动通知过来了，但是图片还没下载好，pic为null，无法求取mixMatrix，因为mixMatrix的求取需要等到图片准备好（由于插图片时传过来的结构体中信息不完备，
     * 部分信息只能从图片本身中获得）。mixMatrix不知道也就无法求取picMatrix，进而无法在当前picMatrix基础上更新图片位置。
     *
     * 故这种场景下需要先保存拖动传过来的matrix，用以将来计算图片的正确展示位置。
     * picMatrix和dragMatrix的关系是：
     *  mixMatrix * dragMatrix = picMatrix * boardMatrix
     * */
    private Matrix dragMatrix;

    /*图片的边界。
    图片的原始宽高组成的矩形相对于picMatrix映射后得到的矩形*/
    private RectF bound = new RectF();

    /*
    * 图片的注解
    * */
    private List<OpPaint> notes = new ArrayList<>();

    public OpInsertPic(){
        type = EOpType.INSERT_PICTURE;
    }

    public OpInsertPic(String picPath, Matrix matrix){

        File file = new File(picPath);
        this.picPath = picPath;
        this.picName = file.getName();
        this.pic = BitmapFactory.decodeFile(picPath);
        this.picWidth = pic.getWidth();
        this.picHeight = pic.getHeight();
        this.matrix.set(matrix);
        type = EOpType.INSERT_PICTURE;
    }

    public OpInsertPic(String picId, String picName, PointF insertPos, int picWidth, int picHeight, Matrix matrix){
        this.picId = picId;
        this.picName = picName;
        this.insertPos = insertPos;
        this.picWidth = picWidth;
        this.picHeight = picHeight;
        this.transMatrix.set(matrix);
        type = EOpType.INSERT_PICTURE;
    }

    @NonNull
    @Override
    public String toString() {
        return "OpInsertPic{" +
                "picId='" + picId + '\'' +
                ", picName='" + picName + '\'' +
                ", picPath='" + picPath + '\'' +
                ", pic=" + pic +
                ", insertPos=" + insertPos +
                ", picWidth=" + picWidth +
                ", picHeight=" + picHeight +
                ", transMatrix=" + transMatrix +
                ", mixMatrix=" + mixMatrix +
                ", matrix=" + matrix +
                ", boardMatrix=" + boardMatrix +
                ", dragMatrix=" + dragMatrix +
                ", bound=" + bound +'\n'+
                super.toString() +
                '}';
    }

    public String getPicId() {
        return picId;
    }

    public void setPicId(String picId) {
        this.picId = picId;
    }

    public String getPicName() {
        return picName;
    }

    public void setPicName(String picName) {
        this.picName = picName;
    }

    public String getPicPath() {
        return picPath;
    }

    public void setPicPath(String picPath) {
        this.picPath = picPath;
    }

    public Bitmap getPic() {
        return pic;
    }

    public void setPic(Bitmap pic) {
        this.pic = pic;
    }

    public PointF getInsertPos() {
        return insertPos;
    }

    public void setInsertPos(PointF insertPos) {
        this.insertPos = insertPos;
    }

    public int getPicWidth() {
        return picWidth;
    }

    public void setPicWidth(int picWidth) {
        this.picWidth = picWidth;
    }

    public int getPicHeight() {
        return picHeight;
    }

    public void setPicHeight(int picHeight) {
        this.picHeight = picHeight;
    }

    public Matrix getTransMatrix() {
        return transMatrix;
    }

    public void setTransMatrix(Matrix transMatrix) {
        this.transMatrix.set(transMatrix);
    }

    public Matrix getMixMatrix() {
        return mixMatrix;
    }

    public void setMixMatrix(Matrix mixMatrix) {
        this.mixMatrix.set(mixMatrix);
    }


    public Matrix getMatrix() {
        return matrix;
    }

    public void setMatrix(Matrix matrix) {
        this.matrix.set(matrix);
    }

    public Matrix getBoardMatrix() {
        return boardMatrix;
    }

    public void setBoardMatrix(Matrix boardMatrix) {
        this.boardMatrix.set(boardMatrix);
    }

    public Matrix getDragMatrix() {
        return dragMatrix;
    }

    public void setDragMatrix(Matrix dragMatrix) {
        this.dragMatrix = dragMatrix;
    }

    public List<OpPaint> getNotes() {
        return notes;
    }

    public void setNotes(List<OpPaint> notes) {
        this.notes = notes;
    }

    @Override
    public RectF boundary() {
        if (null == pic){
            return bound;
        }
        bound.set(0, 0, pic.getWidth(), pic.getHeight());
        matrix.mapRect(bound);
        return bound;
    }
}
