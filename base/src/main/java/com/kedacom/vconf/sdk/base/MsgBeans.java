package com.kedacom.vconf.sdk.base;

import android.graphics.PointF;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

import static com.kedacom.vconf.sdk.base.MsgConst.*;

/**
 * Created by Sissi on 2018/9/6.
 *
 * 消息结构体定义。
 *
 * NOTE: 所有内部类请用“static final”修饰
 *
 */

@RestrictTo(RestrictTo.Scope.LIBRARY)
@SuppressWarnings({"WeakerAccess", "unused"})
public final class MsgBeans {

    private MsgBeans(){}




    //>>>>>>>>>>>>>>>>>>>>>>>>>>> 数据协作

//    /**数据协作服务器地址信息*/
//    public static final class DCServerAddr {
//        public String domain;        // 域名
//        public long ip;               // ip
//        public boolean bUseDefAddr;     // 是否使用默认地址
//        public String achCustomDomain;  // 用户自定义域名
//        public long dwCustomIp;         // 用户自定义ip
//        public int dwPort;
//    }


    /**数据协作登录参数*/
    public static final class DCLoginPara {
        public String ip;  // 服务器IP
        public int port;    // 服务器端口
        public EmDcsType mtType; // 终端类型
        public DCLoginPara(String ip, int port, EmDcsType type){
            this.ip = ip; this.port = port; mtType = type;
        }
    }

    /**通用结果*/
    public static class CommonResult {
        public boolean bSuccess;// 是否成功。true：成功
        public int errorCode;   // （如果失败的）错误码
        private CommonResult(){
            bSuccess = true;
        }
    }

    /**数据协作创会参数*/
    public static final class DCCreateConfPara {
        public String	    confE164;
        public String       confName;
        public EmDcsConfType confType;
        public EmDcsConfMode confMode;
        public DCMember[]   members;
        public String       adminE164;  // 管理员e164
        public EmDcsType   adminMtType; // 管理员终端类型
        public DCCreateConfPara(){

        }
    }

    /**数据协作创会结果*/
    public static final class DCCreateConfResult extends CommonResult{
        public String       confE164;
        public String       confName;
        public EmDcsConfMode confMode;
        public EmDcsConfType confType;
        public TDCSConfAddr confAddr; // TODO 这个是否有必要？
        public boolean      bCreator;   // 自己是否是这个数据协作的创建者。该消息既作为响应又作为通知，可用该字段加以区分这两种情形。
    }
    public static final class TDCSConfAddr { // TODO 这个是否有必要？
        public String achIp;
        public String achDomain;
        public int dwPort;
    }

    /**数据协作退出参数*/
    public static final class DCSQuitConf{
        public String e164;
        public boolean bForce; // true: 只退出数据协作；false：退出数据协作和会议
        public DCSQuitConf(String e164){
            this.e164=e164; bForce =true;
        }
    }

    /**数据协作成员标识*/
    public static final class DCMemberId {
        public String e164;
        public DCMemberId(String e164){
            this.e164 = e164;
        }
    }

    /**数据协作会议标识*/
    public static final class DCConfId { //TODO 就作为String传下去？
        public String e164;
        public DCConfId(String e164){
            this.e164 = e164;
        }
    }

    /**数据协作获取所有成员结果*/
    public static final class DCQueryAllMembersResult extends CommonResult{
        public DCMember[] members;
    }

    /**数据协作画板*/
    public  static final class DCBoard {
        public String id;           // 终端填写GUID（系统函数生成）
        public String name;
        public int sn;              // 序列号，递增，标记白板创建序号
        public String creatorE164;
        public int createTime;      // 平台成功响应后，平台填写
        public String confE164;         // 所属会议e164号
        public EmDcsWbMode mode;    // 模式（白板、文档）
        public int pageNum;         // 总页数（限文档）——以TDCSWbAddSubPageInfo中的dwSubPageCount为准。
        public int pageId;            // 文档页id，平台成功响应后，平台填写（限文档）
        public int width;
        public int height;
        public String elementUrl;    // 图元Url，*.json格式，由业务层负责解析，上层接收业务层推送的各图元通知即可（如：DcsOperLineOperInfo_Ntf）
        public String downloadUrl;   // 图片下载Url（限文档）
        public String uploadUrl;     // 图片上传Url（限文档）
        public int anonyId;         // 平台成功响应后，平台填写（限白板），白板1白板2后面的数字，平台裁决后分配的。
        DCBoard(){
            name = "paint board";
            id = "boardId";
        }
    }

    /**数据协作获取画板结果*/
    public static final class DCQueryBoardResult extends CommonResult{
        public DCBoard board;
    }

    /**数据协作获取所有画板结果*/
    public static final class DCQueryAllBoardsResult extends CommonResult{
        public DCBoard[] boards;
    }

    /**数据协作画板标识*/
    public static final class DCBoardId {
        public String confE164;
        public String boardId;
    }

    public static final class TDCSOperReq{ // TODO
        public String   achConfE164;
        public String   achTabId;
        public int  dwWbPageid;
    }

    public static final class DCSOperAddSubPageOper{ // TODO
        public TDCSOperReq pageInfo;
        public TDCSWbAddSubPageInfo lineInfo;
    }

    public static final class DCSTransferFile { // TODO
        public String url;
        public TDCSFileInfo fileInfo;
    }


    public static final class TDCSFileInfo { // TODO
        public String achFilePathName;
        public String achWbPicentityId;     // 如果是图片，则会有pic id，否则为空
        public String achTabid;
        public boolean bElementCacheFile;   // 是否为图元缓存文件，即，如果是图片图元，设置为false；非图片图元（线、圆、矩形等）为true
        public long dwFileSize;
    }

    public static final class TDCSWbAddSubPageInfo{ // TODO
        public String 	 achTabId;             // 白板tab id（guid）
        public int  dwSubPageCount;       // 子页总数，即打开的文档的总页数
    }


    public static final class DCOvalOp extends DCDrawOp{
        public float left;
        public float top;
        public float right;
        public float bottom;
        public DCOvalOp(){
            left = 100;
            top = 100;
            right = 600;
            bottom = 600;
            opType = EDcOpType.DRAW_OVAL;
        }
        @NonNull
        @Override
        public String toString() {
            return "{"+String.format("left=%s, top=%s, right=%s, bottom=%s", left, top, right, bottom)+super.toString()+"}";
        }
    }

    public static final class DCRectOp extends DCDrawOp{
        public float left;
        public float top;
        public float right;
        public float bottom;
        public DCRectOp(){
            left = 100;
            top = 100;
            right = 600;
            bottom = 600;
            opType = EDcOpType.DRAW_RECT;
        }
        @NonNull
        @Override
        public String toString() {
            return "{"+String.format("left=%s, top=%s, right=%s, bottom=%s", left, top, right, bottom)+super.toString()+"}";
        }
    }

    public static final class DCPathOp extends DCDrawOp{
        public PointF[] points;
        public DCPathOp(){
            points = new PointF[]{
                    new PointF(100, 600),
                    new PointF(350, 350),
                    new PointF(100, 350),
                    new PointF(350, 600),
            };
            opType = EDcOpType.DRAW_PATH;
        }
        @NonNull
        @Override
        public String toString() {
            StringBuffer stringBuffer = new StringBuffer();
            for (PointF pointF : points){
                stringBuffer.append("(").append(pointF.x).append(",").append(pointF.y).append(")");
            }
            return "{"+String.format("points=[%s] ", stringBuffer.toString())+super.toString()+"}";
        }

    }


    public static final class DCInertPicOp extends DCPaintOp{
        public String picId;
        public String picName;
        public int  width;  //TODO 图片原始宽？
        public int  height; //TODO 图片原始高？

        // 插入的目标位置（左上角坐标点）
        public float insertPosX;
        public float insertPosY;

        public String[] matrixValue; // TODO 放缩及位置信息？

        public DCInertPicOp(){
            picId = "picId";
            matrixValue = new String[]{
                    "1","0","0",
                    "0","1","0",
                    "0","0","1",
            };
            opType = EDcOpType.INSERT_PIC;
        }

        @NonNull
        @Override
        public String toString() {
            StringBuffer stringBuffer = new StringBuffer();
            stringBuffer.append("[");
            for (String val : matrixValue){
                stringBuffer.append(val).append(",");
            }
            stringBuffer.append("]");
            return "{"+String.format("picId=%s, picName=%s, picWidth=%s, picHeight=%s, insertPosX=%s, insertPosY, matrix=%s",
                    picId, picName, width, height, insertPosX, insertPosY, stringBuffer.toString())+super.toString()+"}";
        }
    }

    public static final class DCDelPicOp extends DCPaintOp{
        public String[] picIds;
        public DCDelPicOp(){
            picIds = new String[]{"picId"};
            opType = EDcOpType.DEL_PIC;
        }
        @NonNull
        @Override
        public String toString() {
            StringBuffer stringBuffer = new StringBuffer();
            stringBuffer.append("(");
            for (String picId : picIds){
                stringBuffer.append(picId).append(",");
            }
            stringBuffer.append(")");
            return "{"+String.format("picIds=%s", stringBuffer.toString())+super.toString()+"}";
        }
    }

    public static final class DCRectEraseOp extends DCPaintOp{
        public float left;
        public float top;
        public float right;
        public float bottom;
        public DCRectEraseOp(){
            left = 100;
            top = 400;
            right = 600;
            bottom = 500;
            opType = EDcOpType.RECT_ERASE;
        }
        @NonNull
        @Override
        public String toString() {
            return "{"+String.format("left=%s, top=%s, right=%s, bottom=%s", left, top, right, bottom)+super.toString()+"}";
        }
    }

    public static final class DCFullScreenMatrixOp extends DCPaintOp{
        public String[] matrixValue;
        public DCFullScreenMatrixOp(){
            matrixValue = new String[]{
                    "0.5","0","0",
                    "0","0.5","0",
                    "0","0","1",
            };
            opType = EDcOpType.FULLSCREEN;
        }
        @NonNull
        @Override
        public String toString() {
            StringBuffer stringBuffer = new StringBuffer();
            stringBuffer.append("[");
            for (String val : matrixValue){
                stringBuffer.append(val).append(",");
            }
            stringBuffer.append("]");
            return "{"+String.format("matrix=%s", stringBuffer.toString())+super.toString()+"}";
        }
    }

// 批量图元结束通知
//    public static final class TDcsCacheElementParseResult {
//        public String achTabId;         // 子页所在的白板id
//        public long dwMsgSequence;       // 最后一个图元的序号 NOTE：这个可以用来筛选出混入的操作
//        public boolean bParseSuccess;   // 解析成功
//    }


    public static final class DCQueryPicUrlResult extends CommonResult{
        public String picId;
        public String url;
        public String boardId;
    }


    public static final class DcsReleaseConf_Ntf{

    }


    /**
     * 绘制操作基类。
     * 包括线、圆等各种图形、图片、清屏、滚屏、撤销等涉及界面绘制的操作。
     * */
    public static class DCPaintOp implements Comparable<DCPaintOp>{
        public String   id;         // 操作ID，唯一标识该操作。由终端使用GUID来填写
        public String   confE164;   // 所属会议e164号
        public String   boardId;    // 画板ID
        public int      pageId;     // 文档页ID（仅文档模式下有效）
        public EDcOpType opType;

        public int      sn;             // 操作序列号，用来表示操作的先后顺序，越小越靠前。由平台填写。
        public String   authorE164;      // 操作发出者。由平台填写。
        public boolean  bCached;   // 是否是服务器缓存的图元。由平台填写。

        @Override
        public int compareTo(DCPaintOp o) {
            if (sn<o.sn){
                return -1;
            }else if (sn == o.sn){
                return 0;
            }else{
                return 1;
            }
        }

        public DCPaintOp(){
            boardId = "boardId";
            confE164 = "confE164";
            opType = EDcOpType.UNKNOWN;
        }

        @NonNull
        @Override
        public String toString() {
            return String.format(" id=%s, opType=%s, confE164=%s, boardId=%s, pageId=%s, sn=%s, authorE164=%s, bCached=%s",
                    id, opType, confE164, boardId, pageId, sn, authorE164, bCached);
        }
    }

    /**
     * 图形绘制操作基类。
     * 各种几何图形的绘制操作。
     * */
    public static class DCDrawOp extends DCPaintOp {
        public int strokeWidth;     // 线宽
        public int color;           // 颜色值
        DCDrawOp(){
            strokeWidth = 5;
            color = 0xFF00FF00;
        }

        @NonNull
        @Override
        public String toString() {
            return String.format(" strokeWidth=%s, color=%s ", strokeWidth, color)+super.toString();
        }
    }

    /**
     * 画线
     * */
    public static final class DCLineOp extends DCDrawOp{
        public float startX;
        public float stopX;
        public float startY;
        public float stopY;
        public DCLineOp(){
            startX = 100;
            startY = 100;
            stopX = 600;
            stopY = 600;
            opType = EDcOpType.DRAW_LINE;
        }

        @NonNull
        @Override
        public String toString() {
            return "{"+String.format("startX=%s, startY=%s, stopX=%s, stopY=%s", startX, startY, stopX, stopY)+super.toString()+"}";
        }
    }


    /**
     * 放缩
     * */
    public static class DCZoomOp extends DCPaintOp {
        public int percentage;   // 放缩百分比。100%为没有放缩，50%为缩小一半。
        public DCZoomOp(){
            percentage = 80;
            opType = EDcOpType.FULLSCREEN;
        }
        @NonNull
        @Override
        public String toString() {
            return "{"+String.format("percentage=%s", percentage)+super.toString()+"}";
        }
    }

    /**
     * 滚屏
     * */
    public static class DCScrollOp extends DCPaintOp {
        public float stopX;   //TODO 中心点？
        public float stopY;
        public DCScrollOp(){
            stopX = 300;
            stopY = 300;
            opType = EDcOpType.FULLSCREEN;
        }
        @NonNull
        @Override
        public String toString() {
            return "{"+String.format("stopX=%s, stopY=%s", stopX, stopY)+super.toString()+"}";
        }
    }


    /**拖拽图片*/
    public static final class DCDragPicOp extends DCPaintOp{
        public DCPicMatrix[] picMatrices;
        public DCDragPicOp(){
            picMatrices = new DCPicMatrix[]{
                    new DCPicMatrix("picId",
                            new String[]{"1","0","200",
                                        "0","1","200",
                                        "0","0","1"}
                            ),
                    new DCPicMatrix("picId2",
                            new String[]{"1","0","300",
                                    "0","1","300",
                                    "0","0","1"}
                    ),
            };
            opType = EDcOpType.DRAG_PIC;
        }

        @NonNull
        @Override
        public String toString() {
            StringBuffer stringBuffer = new StringBuffer();
            stringBuffer.append("[");
            for (DCPicMatrix picMatrix : picMatrices){
                stringBuffer.append(picMatrix).append(", ");
            }
            stringBuffer.append("], ");
            return "{"+String.format("picMatrices={%s}", stringBuffer.toString())+super.toString()+"}";
        }

    }

    public static final class DCPicMatrix {
        public String picId;        // 图元ID
        public String[] matrixValue;
        public DCPicMatrix(String picId, String[] matrixValue){
            this.picId = picId;
            this.matrixValue = matrixValue;
        }

        @NonNull
        @Override
        public String toString() {
            StringBuffer stringBuffer = new StringBuffer();
            stringBuffer.append(picId).append("(");
            for (String val : matrixValue){
                stringBuffer.append(val).append(",");
            }
            stringBuffer.append(")");
            return stringBuffer.toString();
        }
    }



    public static final class DownloadPara {
        public String boardId;
        public String picId;  // 图片ID。为null则表示下载图元操作（画线、画圆、插入图片等），不为null则下载图片本身。
        public String picSavePath;
        public String url; // 下载地址
        public DownloadPara(String boardId, String url){
            this.boardId = boardId;
            this.picId = null;
            this.picSavePath = null;
            this.url = url;
        }
        public DownloadPara(String boardId, String picId, String picSavePath, String url){
            this.boardId = boardId;
            this.picId = picId;
            this.picSavePath = picSavePath;
            this.url = url;
        }
    }

    public static final class DownloadResult extends CommonResult{
        public String boardId;
        public boolean bPic;  // 是否是图片
        public String picId;
        public String picSavePath;
        DownloadResult(){
            boardId = "boardId";
            picId = "picId";
            picSavePath = "/data/local/tmp/wb.png";
        }
    }

    public static final class DCQueryPicUrlPara {
        public String picId;     // 图片ID
        public String confE164;
        public String boardId;
        public int pageId;
        public DCQueryPicUrlPara(String picId, String confE164, String boardId, int pageId) {
            this.picId = picId;
            this.confE164 = confE164;
            this.boardId = boardId;
            this.pageId = pageId;
        }
    }

    public  static final class DCMember {
        public String e164;
        /**
         * 只有在添加与会方，删除与会方用到
         */
        public String name;
        public EmDcsType mtType;
        /**
         * 暂时只在获取与会人员列表中有效
         */
        public boolean bOnline;
        public boolean bOperator;
        public boolean bConfAdmin;
    }


    //<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<< 数据协作








}
