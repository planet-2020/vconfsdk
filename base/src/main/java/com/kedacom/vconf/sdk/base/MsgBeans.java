package com.kedacom.vconf.sdk.base;

import android.graphics.Point;

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

    public static final class BaseTypeInt{
        public int basetype;
    }
    public static final class BaseTypeBool{
        public boolean basetype;
    }
    public static final class BaseTypeString{
        public String basetype;
    }


    public static final class StartupPara {

    }

    public static final class StartupResult{

    }

    public static final class LoginPara {
        public String serverAddr;
        public String account;
        public String passwd;
        public SetType setType;
        public LoginPara(String serverAddr, String account, String passwd, SetType setType){ //TODO 此类模版代码能否统一生成
            this.serverAddr = serverAddr; this.account = account; this.passwd=passwd; this.setType=setType;
        }

        @Override
        public String toString() { //TODO 此类模版代码能否统一生成, 直接使用json格式好了
            return String.format(getClass().getSimpleName()
                    +"{serverAddr=%s, account=%s, passwd=%s, setType=%s}", serverAddr, account, passwd, setType);
        }
    }

    public static final class LoginResult {
        public String sessionId;
        public int result;
    }

    public static final class LogoutPara {
        public String sessionId;
        public LogoutPara(String sessionId){
            this.sessionId = sessionId;
        }
    }

    public static final class LogoutRsp {
        public int result;
    }

    public static final class MemberState {
        public int memberId;
        public int preState;
        public int curState;

        private MemberState(){ // 我们不需要手动创建该类对象，默认构造方法仅用于框架生成模拟对象。
            memberId = 1;
            preState = 2;
            curState = 3;
        }

        @Override
        public String toString() { //TODO 此类模版代码能否统一生成
            return String.format(getClass().getSimpleName()
                    +"{memberId=%s, preState=%s, curState=%s}", memberId, preState, curState);
        }
    }


    public static final class XmppServerInfo{
        public String domain;
        public long ip;
        private XmppServerInfo(){
            domain = "www.kedacom.com";
            ip = 123445555;
        }
    }

    public static final class NetConfig{
        long ip;
        int port;
        public NetConfig(long ip, int port){
            this.ip = ip; this.port = port;
        }
    }







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
        public EmDcsType terminalType; // 终端类型
        public DCLoginPara(String ip, int port, EmDcsType type){
            this.ip = ip; this.port = port; terminalType = type;
        }
    }

    /**通用结果*/
    public static final class CommonResult {
        public boolean success;// 是否成功。true：成功
        public int errorCode;   // （如果失败的）错误码
        private CommonResult(){
            success = true;
        }
    }

//    /**数据协作通用结果*/
//    public static final class DCCommonResult {
//        public boolean bSucces;
//        public int dwErrorCode;
//        public String achConfE164;
//        private DCCommonResult(){
//            bSucces = true;
//        }
//    }

    /**创建数据协作会议参数*/
    public static final class DCCreateConfPara {
        public EmDcsConfType   confType;
        public String	    confE164;
        public String       confName;
        public EmDcsConfMode confMode;
        public DCMember[]   members;
        public String       adminE164;  // 管理员e164
        public EmDcsType   adminMtType; // 管理员终端类型
        public DCCreateConfPara(){

        }
    }

    /**数据协作会议创建结果消息体*/
    public static final class DCCreateConfResult {
        public CommonResult commonResult;
        public String       confE164;
        public String       confName;
        public EmDcsConfMode confMode;
        public EmDcsConfType confType;
        public TDCSConfAddr confAddr;
        public boolean      bCreator;   // 自己是否是这个数据协作的创建者。该消息既作为响应又作为通知，可用该字段加以区分这两种情形。
    }

    /**退出数据协作参数*/
    public static final class DCSQuitConf{
        public String e164;
        public boolean  force; // true: 只退出数据协作；false：退出数据协作和会议
        public DCSQuitConf(String e164){
            this.e164=e164; force=true;
        }
    }

    public static final class DCMemberId {
        public String e164;
        public DCMemberId(String e164){
            this.e164 = e164;
        }
    }

    public static final class DCConfId { //XXX 就作为String传下去？
        public String e164;
        public DCConfId(String e164){
            this.e164 = e164;
        }
    }

    public static final class DCQueryAllMembersResult {
        public CommonResult commonResult;
        public DCMember[] AssParam;
    }



    public  static final class DCPaintBoard {
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
        DCPaintBoard(){
            name = "paint board";
            id = "boardId";
        }
    }



    public static final class TDCSBoardResult{ // XXX DCCommonResult
        public boolean     	bSucces;
        public int			dwErrorCode;
        public String		achConfE164;
        public String  		achTabId;
        public int			dwPageId;
    }
    public static final class DCSWhiteBoardResult {
        public TDCSBoardResult MainParam;
        public DCPaintBoard AssParam;
    }

    public static final class DCPaintBoardId {
        public String confE164;
        public String boardId;
    }


    public static final class TDCSGetAllBoard{ // XXX num不要，该结构体可简化为TDCSBoardInfo[].class（尝试下看这样行不行）， achConfE164作为para传下去，
        public String	achConfE164;
        public int	    dwBoardNum;
        public DCPaintBoard[] atBoardInfo;
    }
    public static final class DCSGetAllWhiteBoardRsp{
        public TDCSResult MainParam;
        public TDCSGetAllBoard AssParam;
    }


    public static final class DCSOperLineOper{
        public TDCSOperReq pageInfo;
        public DCLineOp lineInfo;
    }

    public static final class TDCSOperReq{ // XXX CommonOpInfo
        public String   achConfE164;
        public String   achTabId;
        public int  dwWbPageid;
    }

    public static final class DCSOperCircleOper{
        public TDCSOperReq pageInfo;
        public TDCSWbCircleOperInfo lineInfo;
    }

    public static final class DCSOperRectangleOper{
        public TDCSOperReq pageInfo;
        public TDCSWbRectangleOperInfo lineInfo;
    }

    public static final class DCSOperPencilOper{
        public TDCSOperReq pageInfo;
        public TDCSWbPencilOperInfo lineInfo;
    }

    public static final class DCSOperImageOper{
        public TDCSOperReq pageInfo;
        public TDCSWbImageOperInfo lineInfo;
    }

    public static final class DCSOperAddSubPageOper{
        public TDCSOperReq pageInfo;
        public TDCSWbAddSubPageInfo lineInfo;
    }

    public static final class DCSOperEraseOper{
        public TDCSOperReq pageInfo;
        public TDCSWbEraseOperInfo lineInfo;
    }

    public static final class DCSOperZoomOper{
        public TDCSOperReq pageInfo;
        public TDCSWbZoomInfo lineInfo;
    }

    public static final class DCSOperUndoOper{
        public TDCSOperReq pageInfo;
        public TDCSWbTabPageIdInfo lineInfo;
    }

    public static final class DCSOperRedoOper{
        public TDCSOperReq pageInfo;
        public TDCSWbTabPageIdInfo lineInfo;
    }

    public static final class DCSOperScrollOper{
        public TDCSOperReq pageInfo;
        public TDCSScrollScreenInfo lineInfo;
    }

    public static final class DCSTransferFile {
        public String url;
        public TDCSFileInfo fileInfo;
    }

//    public static final class DownloadResult {
//        public boolean bSuccess; //XXX 改为Common result
//        public boolean bElementFile;
//        public String achFilePathName;
//        public String achWbPicentityId;
//        public String achTabid;
//        DownloadResult(){
//            bSuccess = true;
//            achTabid = "boardId";
//            bElementFile = true;
//            achWbPicentityId = "picId";
//            achFilePathName = "/data/local/tmp/wb.png";
//        }
//    }



    public static final class TDCSFileInfo {
        public String achFilePathName;
        public String achWbPicentityId;     // 如果是图片，则会有pic id，否则为空
        public String achTabid;
        public boolean bElementCacheFile;   // 是否为图元缓存文件，即，如果是图片图元，设置为false；非图片图元（线、圆、矩形等）为true
        public int dwFileSize;
    }

    public static final class TDCSScrollScreenInfo {
        public String achTabId; 	    //tab白板页
        public int  dwSubPageId;        //子页ID
        public TDCSWbPoint  tPoint;     //滚动到的目标点坐标
    }

    public static final class TDCSWbZoomInfo {
        public String achTabId;     // 白板tab id（guid）
        public int dwZoom;          // 当前页缩放倍数，取百分制，例如100.0，对应100%
    }

    public static final class TDCSWbAddSubPageInfo{
        public String 	 achTabId;             // 白板tab id（guid）
        public int  dwSubPageCount;       // 子页总数，即打开的文档的总页数
    }




    public static final class TDCSWbImageOperInfo{
        public String  achTabId;
        public int     dwSubPageId; // ???
        public TDCSWbImage	 tImage;
    }

    public  static final class TDCSWbImage {
        TDCSWbEntity	tEntity;				// 基本信息
        TDCSWbPoint 	tBoardPt;				// 边界矩形左上角坐标
        int 	    dwWidth;				// 边界矩形宽度
        int 		    dwHeight;				// 边界矩形宽度
        EmDcsWbImageState   emNetworkstate;			// 网络状态信息
        String  achFileName;          // 文件名（utf8编码）
        boolean	    bBkImg;	    // 是否文档底图
    }


//    public static final class DCOvalOp{
//        public DCOpCommonInfo MainParam;
//        public TDCSWbCircleOperInfo AssParam;
//        DCOvalOp(){
//            MainParam = new DCOpCommonInfo();  //MainParam.dwMsgSequence = 2;
//            AssParam = new TDCSWbCircleOperInfo();
//        }
//    }

    public static final class DCOvalOp {
        public int left;
        public int top;
        public int right;
        public int bottom;
        public DCOpCommonInfo commonInfo;
        public DCPaintCfg paintCfg;
    }

    public static final class DCRectOp {
        public int left;
        public int top;
        public int right;
        public int bottom;
        public DCOpCommonInfo commonInfo;
        public DCPaintCfg paintCfg;
    }

    public static final class DCPathOp {
        public Point[] points;
        public DCOpCommonInfo commonInfo;
        public DCPaintCfg paintCfg;
    }


    public static final class DCInertPicOp {
        public String achImgId;         // 图元ID
        public String achPicName;
        public int dwImgWidth;
        public int dwImgHeight;
        public Point tPoint;
        public String[] aachMatrixValue;

        public DCOpCommonInfo MainParam;
        public DCInertPicOp(){
            MainParam = new DCOpCommonInfo(); //MainParam.dwMsgSequence = 4;
        }
    }

    public static final class DCDelPicOp {
        public String[] achGraphsId;
        public DCOpCommonInfo MainParam;
        DCDelPicOp(){
        }
    }

    public static final class DCRectEraseOp {
        public int left;
        public int top;
        public int right;
        public int bottom;
        public DCOpCommonInfo commonInfo;
    }

    public static final class DCFullScreenMatrixOp {
        public float[] matrixValue;
        public DCOpCommonInfo commonInfo;
    }


//    public static final class TDcsCacheElementParseResult {
//        public String achTabId;         // 子页所在的白板id
//        public long dwMsgSequence;       // 最后一个图元的序号 NOTE：这个可以用来筛选出混入的操作
//        public boolean bParseSuccess;   // 解析成功
//    }


    public static final class DCQueryPicUrlResult {
        public String picId;
        public String url;
        public String boardId;
        public CommonResult commonResult;
    }


    public static final class TDCSResult { // XXX DCCommonResult
        public boolean bSucces;
        public int dwErrorCode;
        public String achConfE164;
        TDCSResult(){
            bSucces = true;
        }
    }

    public static final class DcsReleaseConf_Ntf{

    }


    public  static final class DCMembers {
        public String achConfE164; // XXX 会议e164有必要吗？为什么申请/取消协作方都不需要？
        public DCMember[] atOperList;
        public DCMembers(String confE164, DCMember[] members){
            achConfE164 = confE164;
            atOperList = members;
        }
    }


    public static final class TDCSConfAddr {
        public String achIp;
        public String achDomain;
        public int dwPort;
    }

    public static final class DCOpCommonInfo {
        public String   boardId;        // 画板ID
        public int      pageId;      // 文档页ID
        public int      sn;          // 操作序列号，用来表示操作的先后顺序，越小越靠前。
        public String   confE164;       // 所属会议e164号
        public String   authorE164;      // 操作发出者
        public boolean  bCacheElement;   // 是否是服务器缓存的图元
    }

    public static final class DCLineOp {
        public int startX;
        public int stopX;
        public int startY;
        public int stopY;
        public DCOpCommonInfo commonInfo;
        public DCPaintCfg paintCfg;
        DCLineOp(){
        }
    }

    public static final class DCPaintCfg {
        public int strokeWidth;     // 线宽
        public int color;           // 颜色值
    }


    public static final class TDCSWbCircleOperInfo {
        public String achTabId;         // 白板tab id（guid）
        public int dwSubPageId;         // 子页面id
        public TDCSWbCircle tCircle;    // 圆操作信息
        TDCSWbCircleOperInfo(){
            tCircle = new TDCSWbCircle();
        }
    }

    public static final class TDCSWbCircle {
        public TDCSWbEntity tEntity;    // 基本信息
        public TDCSWbPoint tBeginPt;    // 起点坐标
        public TDCSWbPoint tEndPt;      // 终点坐标
        public int dwLineWidth;         // 线宽
        public long dwRgb;              // 颜色，强转成int类型就是颜色值了。比如long类型的4294967295强转成int就是0xFFFFFFFF（纯白色），即-1。
        TDCSWbCircle(){
            tBeginPt = new TDCSWbPoint(500,500);
            tEndPt = new TDCSWbPoint(800, 800);
            dwLineWidth = 10;
            dwRgb = 0xFF00FF00;
        }
    }

    public static final class TDCSWbEntity {
        public String achEntityId;  // 现在使用GUID来填写
        public boolean bLock;
    }

    public static final class TDCSWbPoint {
        public int nPosx;
        public int nPosy;
        TDCSWbPoint(){}
        TDCSWbPoint(int x, int y){nPosx=x; nPosy=y;}
    }

    public static final class TDCSWbRectangleOperInfo {
        public String achTabId;             // 白板tab id（guid）
        public int dwSubPageId;             // 子页面id
        public TDCSWbRectangle tRectangle;  // 矩形操作信息
        TDCSWbRectangleOperInfo(){
            tRectangle = new TDCSWbRectangle();
        }
    }

    public static final class TDCSWbRectangle {
        public TDCSWbEntity tEntity;    // 基本信息
        public TDCSWbPoint tBeginPt;    // 起点坐标
        public TDCSWbPoint tEndPt;      // 终点坐标
        public int dwLineWidth;         // 线宽
        public long dwRgb;              // 颜色，强转成int类型就是颜色值了。比如long类型的4294967295强转成int就是0xFFFFFFFF（纯白色），即-1。
        TDCSWbRectangle(){
            tBeginPt = new TDCSWbPoint(0,0);
            tEndPt = new TDCSWbPoint(500, 500);
            dwLineWidth = 10;
            dwRgb = 0xFF00FF00;
        }
    }

    public static final class TDCSWbPencilOperInfo {
        public String achTabId;         // 白板tab id（guid）
        public int dwSubPageId;         // 子页面id
        public TDCSWbPencil tPencil;    // 铅笔操作信息
        TDCSWbPencilOperInfo(){tPencil = new TDCSWbPencil();}
    }

    public static final class TDCSWbPencil {
        public TDCSWbEntity tEntity;    // 基本信息
        public int dwPointNum;          // 曲线点数量
        public TDCSWbPoint[] atPList;   // 曲线点信息列表
        public int dwLineWidth;         // 线宽
        public long dwRgb;              // 颜色，强转成int类型就是颜色值了。比如long类型的4294967295强转成int就是0xFFFFFFFF（纯白色），即-1。
        TDCSWbPencil(){
            atPList = new TDCSWbPoint[]{
                    new TDCSWbPoint(0, 500),
                    new TDCSWbPoint(250, 250),
                    new TDCSWbPoint(0, 250),
                    new TDCSWbPoint(250, 500),
            };
            dwLineWidth = 10;
            dwRgb = 0xFF0000FF;
        }
    }


    public static final class TDCSWbInsertPicOperInfo {
        public String achImgId;         // 图元ID
        public int dwImgWidth;
        public int dwImgHeight;
        public TDCSWbPoint tPoint;
        public String achPicName;
        public String[] aachMatrixValue;
        public TDCSWbInsertPicOperInfo(){
            achImgId = "picId";
            tPoint = new TDCSWbPoint(10, 50);
            aachMatrixValue = new String[]{
                    "1.000000",
                    "0.000000",
                    "0.000000",
                    "0.000000",
                    "1.00000",
                    "0.250000",
                    "0.000000",
                    "0.000000",
                    "1.000000"
            };
        }
    }


    public static final class DCDragPicOp {
        public DCPicMatrix[] atGraphsInfo;
        public DCOpCommonInfo commonInfo;
    }

    public static final class DCPicMatrix {
        public String achGraphsId;        // 图元ID
        public String[] aachMatrixValue;
        DCPicMatrix(){
            achGraphsId = "picId";
            aachMatrixValue = new String[]{
                    "1.000000",
                    "0.000000",
                    "200.000000",
                    "0.000000",
                    "1.000000",
                    "200.250000",
                    "0.000000",
                    "0.000000",
                    "1.000000"
            };
        }
    }

    public static final class TDCSWbDelPicOperInfo {
        public String achTabId;         // 白板tab id（guid）
        public int dwSubPageId;         // 子页面id
        public int dwGraphsCount;
        public String[] achGraphsId;
        TDCSWbDelPicOperInfo(){
            achGraphsId = new String[]{"picId"};
        }
    }

    public  static final class TDCSWbEraseOperInfo {
        public String achTabId;         // 白板tab id（guid）
        public int dwSubPageId;
        public TDCSWbPoint tBeginPt;    // 矩形擦除区域的开始坐标（此参数矩形擦除使用）
        public TDCSWbPoint tEndPt;      // 矩形擦除区域的结束坐标（此参数矩形擦除使用）
        public TDCSWbEraseOperInfo(){
            tBeginPt = new TDCSWbPoint(0, 100);
            tEndPt = new TDCSWbPoint(800, 150);
        }
    }


    public static final class TDCSWbTabPageIdInfo {
        String 	 achTabId;
        int  dwSubPageId;
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

    public static final class DownloadResult {
        public String boardId;
        public boolean bElement;  // 是否普通图元（而非图片）
        public String picId;
        public String picSavePath;
        public CommonResult commonResult;
    }

    public static final class DCQueryPicUrlPara {
        public String picId;     // 图片ID
        public String confE164;
        public String boardId;
        public int pageId;
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
