package com.kedacom.vconf.sdk.base;

import androidx.annotation.RestrictTo;

import static com.kedacom.vconf.sdk.base.MsgConst.*;

/**
 * Created by Sissi on 2018/9/6.
 *
 * 消息结构体定义。
 *
 * （TODO 最好结合对组件层消息体内容的理解重新定义一套适合UI层理解的，而非直接照搬组件层的，然后在jni层做这两套消息体之间的转换）
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

    /**数据协作服务器地址信息*/
    public static final class TMtDCSSvrAddr{
        public String achDomain;        // 域名
        public long dwIp;               // ip
        public boolean bUseDefAddr;     // 是否使用默认地址
        public String achCustomDomain;  // 用户自定义域名
        public long dwCustomIp;         // 用户自定义ip
        public int dwPort;
    }


    /**数据协作登录参数*/
    public static final class TDCSRegInfo {
        public String achIp;
        public int dwPort;
        public EmDcsType emMtType;
        public TDCSRegInfo(String ip, int port, EmDcsType type){
            achIp = ip; dwPort = port; emMtType = type;
        }
    }

    /**数据协作链路建立结果响应消息体*/
    public static final class DcsLinkCreationResult{
        public boolean bSuccess;
        public int emErrorCode;
        private DcsLinkCreationResult(){
            bSuccess = true;
        }
    }

    /**数据协作登录结果消息体*/
    public static final class DcsLoginResult{
        public String achConfE164;
        public boolean bSucces;
        public int dwErrorCode;
        private DcsLoginResult(){
            bSucces = true;
        }
    }

    /**创建数据协作会议参数*/
    public static final class  DCSCreateConf{
        public EmDcsConfType   emConfType;
        public String	    achConfE164;
        public String      achConfName;
        public EmDcsConfMode   emConfMode;
        public TDCSConfUserInfo[] atUserList;
        public int		    dwListNum;
        public String  achConfAdminE164;
        public EmDcsType   emAdminMtType;
        public DCSCreateConf(){

        }
        public DCSCreateConf(EmDcsConfType type, String confE164, String confName, EmDcsConfMode mode,
                             TDCSConfUserInfo[] memberList, int num, String adminE164, EmDcsType dcsType){
            emConfType=type; achConfE164=confE164; achConfName=confName; emConfMode=mode;
            atUserList=memberList; dwListNum=num; achConfAdminE164=adminE164; emAdminMtType=dcsType;
        }
    }

    /**数据协作会议链路建立结果响应消息体*/
    public static final class DcsConfResult{
        public boolean bSuccess;
        public int emErrorCode;
        private DcsConfResult(){
            bSuccess = true;
        }
    }

    /**数据协作会议创建结果消息体*/
    public static final class TDCSCreateConfResult {
        public String achConfE164;
        public String achConfName;
        public boolean bSuccess;
        public int dwErrorCode;
        public EmDcsConfMode emConfMode;
        public EmDcsConfType emConfType;
        public TDCSConfAddr tConfAddr;
        public boolean bCreator;            // 自己是否是这个数据协作的创建者
        private TDCSCreateConfResult(){
            bSuccess = true;
        }
    }

    /**退出数据协作参数*/
    public static final class DCSQuitConf{
        public String e164;
        public boolean  force;
        public DCSQuitConf(String e164, boolean force){
            this.e164=e164; this.force=force;
        }
    }

    public static final class DCSBriefMemberInfo {
        public String e164;
        public DCSBriefMemberInfo(String e164){
            this.e164 = e164;
        }
    }

    public static final class DCSBriefConfInfo {
        public String e164;
        public DCSBriefConfInfo(String e164){
            this.e164 = e164;
        }
    }

    public static final class DCSGetUserListRsp {
        public TDCSResult MainParam;
        public TDCSGetUserList AssParam;
    }

    public static final class TDCSGetUserList {
        public TDCSConfUserInfo[] atUserList;
        public int dwListNum;
    }

    public class TDCSBoardInfo {
        public String achWbName;
        public EmDcsWbMode emWbMode;    // 模式（白板、文档）
        public int dwWbPageNum;         // 总页数（限文档）——以TDCSWbAddSubPageInfo中的dwSubPageCount为准。
        public int dwWbCreateTime;      // 平台成功响应后，平台填写
        public String achTabId;         // 终端填写
        public int dwPageId;            // 文档页id，平台成功响应后，平台填写（限文档）
        public int dwWbSerialNumber;    // 白板序列号，递增，标记白板创建序号
        public String achWbCreatorE164;
        public int dwWbWidth;
        public int dwWbHeight;
        public String achElementUrl;    // 图元Url，*.json格式，由业务层负责解析，上层接收业务层推送的各图元通知即可（如：DcsOperLineOperInfo_Ntf）
        public String achDownloadUrl;   // 图片下载Url（限文档）
        public String achUploadUrl;     // 图片上传Url（限文档）
        public int dwWbAnonyId;         // 平台成功响应后，平台填写（限白板）
    }

//    public static final class DcsNewWhiteBoard_Ntf{
//
//    }

//    public static final class DcsSwitch_Ntf{
//
//    }

    public static final class TDCSNewWhiteBoard{
        public String		 achConfE164;
        public TDCSBoardInfo tBoardinfo;
        public TDCSNewWhiteBoard(String confE164, TDCSBoardInfo boardInfo){
            achConfE164 = confE164; tBoardinfo=boardInfo;
        }
    }

    public static final class DCSWhiteBoardResult {
        public TDCSBoardResult MainParam;
        public TDCSBoardInfo AssParam;
    }

    public static final class DCSWhiteBoardIndex {
        public String confE164;
        public String boardIndx;  // ??? 白板索引
    }

    public static final class TDCSBoardResult{
        public boolean     	bSucces;
        public int			dwErrorCode;
        public String		achConfE164;
        public String  		achTabId; // ???
        public int			dwPageId; // ???
    }

    public static final class DCSGetAllWhiteBoardRsp{
        public TDCSResult MainParam;
        public TDCSGetAllBoard AssParam;
    }

    public static final class TDCSGetAllBoard{
        public String	achConfE164;
        public int	    dwBoardNum;
        public TDCSBoardInfo[] atBoardInfo;
    }

    public static final class DCSOperLineOper{
        public TDCSOperReq pageInfo;
        public TDCSWbLineOperInfo lineInfo;
    }

    public static final class TDCSOperReq{
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

    public static final class DCSOperColorPenOper{
        public TDCSOperReq pageInfo;
        public TDCSWbColorPenOperInfo lineInfo;
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

    public static final class TDCSFileLoadResult {
        public boolean bSuccess;
        public boolean bElementFile;
        public String achFilePathName;
        public String achWbPicentityId;
        public String achTabid;
    }



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

    public class TDCSWbImage {
        TDCSWbEntity	tEntity;				// 基本信息
        TDCSWbPoint 	tBoardPt;				// 边界矩形左上角坐标
        int 	    dwWidth;				// 边界矩形宽度
        int 		    dwHeight;				// 边界矩形宽度
        EmDcsWbImageState   emNetworkstate;			// 网络状态信息
        String  achFileName;          // 文件名（utf8编码）
        boolean	    bBkImg;	    // 是否文档底图
    }

    public static final class DcsElementOperBegin_Ntf{
        public BaseTypeString MainParam;
        public BaseTypeInt AssParam;
    }

    public static final class DcsOperLineOperInfo_Ntf{
        public TDCSOperContent MainParam;
        public TDCSWbLineOperInfo AssParam;
        DcsOperLineOperInfo_Ntf(){
            MainParam = new TDCSOperContent(); //MainParam.dwMsgSequence = 1;
            AssParam = new TDCSWbLineOperInfo();
        }
    }

    public static final class DcsOperCircleOperInfo_Ntf{
        public TDCSOperContent MainParam;
        public TDCSWbCircleOperInfo AssParam;
        DcsOperCircleOperInfo_Ntf(){
            MainParam = new TDCSOperContent();  //MainParam.dwMsgSequence = 2;
            AssParam = new TDCSWbCircleOperInfo();
        }
    }

    public static final class DcsOperRectangleOperInfo_Ntf{
        public TDCSOperContent MainParam;
        public TDCSWbRectangleOperInfo AssParam;
        DcsOperRectangleOperInfo_Ntf(){
            MainParam = new TDCSOperContent(); //MainParam.dwMsgSequence = 3;
            AssParam = new TDCSWbRectangleOperInfo();
        }
    }


    public static final class DcsOperPencilOperInfo_Ntf{
        public TDCSOperContent MainParam;
        public TDCSWbPencilOperInfo AssParam;
        DcsOperPencilOperInfo_Ntf(){
            MainParam = new TDCSOperContent(); //MainParam.dwMsgSequence = 4;
            AssParam = new TDCSWbPencilOperInfo();
        }
    }

    public static final class DcsOperColorPenOperInfo_Ntf{
        public TDCSOperContent MainParam;
        public TDCSWbColorPenOperInfo AssParam;
    }

    public static final class DcsOperInsertPic_Ntf{
        public TDCSOperContent MainParam;
        public TDCSWbInsertPicOperInfo AssParam;
        public DcsOperInsertPic_Ntf(){
            MainParam = new TDCSOperContent(); //MainParam.dwMsgSequence = 4;
            AssParam = new TDCSWbInsertPicOperInfo();
        }
    }

    public static final class DcsOperPitchPicDrag_Ntf{
        public TDCSOperContent MainParam;
        public TDCSWbPitchPicOperInfo AssParam;
    }

    public static final class DcsOperPitchPicDel_Ntf{
        public TDCSOperContent MainParam;
        public TDCSWbDelPicOperInfo AssParam;
    }

    public static final class DcsOperEraseOperInfo_Ntf{
        public TDCSOperContent MainParam;
        public TDCSWbEraseOperInfo AssParam;
        public DcsOperEraseOperInfo_Ntf(){
            MainParam = new TDCSOperContent();  //MainParam.dwMsgSequence = 2;
            AssParam = new TDCSWbEraseOperInfo();
        }
    }

    public static final class DcsOperFullScreen_Ntf{
        public TDCSOperContent MainParam;
        public TDCSWbDisPlayInfo AssParam;
        public DcsOperFullScreen_Ntf(){
            MainParam = new TDCSOperContent(); MainParam.dwMsgSequence = 14;
            AssParam = new TDCSWbDisPlayInfo();
        }
    }

    public static final class DcsOperUndo_Ntf{
        public TDCSOperContent MainParam;
        public TDCSWbTabPageIdInfo AssParam;
        public DcsOperUndo_Ntf(){
            MainParam = new TDCSOperContent(); // MainParam.dwMsgSequence = 4;
            AssParam = new TDCSWbTabPageIdInfo();
        }
    }

    public static final class DcsOperRedo_Ntf{
        public TDCSOperContent MainParam;
        public TDCSWbTabPageIdInfo AssParam;
        public DcsOperRedo_Ntf(){
            MainParam = new TDCSOperContent(); // MainParam.dwMsgSequence = 5;
            AssParam = new TDCSWbTabPageIdInfo();
        }
    }

//    public static final class DcsOperClearScreen_Ntf{
//
//    }

//    public static final class DcsElementOperFinal_Ntf{
//
//    }

    public static final class TDcsCacheElementParseResult {
        public String achTabId;         // 子页所在的白板id
        public long dwMsgSequence;       // 最后一个图元的序号
        public boolean bParseSuccess;   // 解析成功
    }


    public static final class DcsTransferImage {
        public TDCSResult MainParam;
        public TDCSImageUrl AssParam;
    }

//    public static final class DownloadImage_Ntf{
//
//    }

//    public static final class DcsDownloadFile_Rsp{
//
//    }


    public static final class DcsDelWhiteBoard_Ntf{

    }


//    public static final class DcsQuitConf_Rsp{
//
//    }

    public static final class TDCSResult {
        public boolean bSucces;
        public int dwErrorCode;
        public String achConfE164;
    }

    public static final class DcsReleaseConf_Ntf{

    }

//    public static final class DcsUserApplyOper_Ntf{
//
//    }

    public static final class TDCSUserInfo {
        public String achConfE164;
        public String achConfName;
        public TDCSConfUserInfo tUserInfo;
    }

    public class TDCSOperator{
        public String achConfE164;
        public int dwListNum;
        public TDCSConfUserInfo[] atOperList;
    }


    public static final class TDCSConfAddr {
        public String achIp;
        public String achDomain;
        public int dwPort;
    }

    public static final class TDCSOperContent{
        public EmDcsOper emOper;
        public int dwMsgId;
        public String achTabId;
        public int dwWbPageId;
        public int dwMsgSequence;
        public String achConfE164;
        public String achFromE164;      // 谁画的
        public boolean bCacheElement;   // 是否是服务器缓存的图元
    }

    public static final class TDCSWbLineOperInfo {
        public String achTabId;     // 白板tab id（guid）
        public int dwSubPageId;     // 子页面id
        public TDCSWbLine tLine;    // 线操作信息
        TDCSWbLineOperInfo(){
            tLine = new TDCSWbLine();
        }
    }

    public static final class TDCSWbLine {
        public TDCSWbEntity tEntity;    // 基本信息
        public TDCSWbPoint tBeginPt;    // 起点坐标
        public TDCSWbPoint tEndPt;      // 终点坐标
        public int dwLineWidth;         // 线宽
        public long dwRgb;              // 颜色，强转成int类型就是颜色值了。比如long类型的4294967295强转成int就是0xFFFFFFFF（纯白色），即-1。
        TDCSWbLine(){
            tBeginPt = new TDCSWbPoint(0,0);
            tEndPt = new TDCSWbPoint(500, 500);
            dwLineWidth = 10;
            dwRgb = 0xFF00FF00;
        }
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

    public static final class TDCSWbColorPenOperInfo {
        public String achTabId;             // 白板tab id（guid）
        public int dwSubPageId;             // 子页面id
        public TDCSWbColorPen tColoePen;    // 彩笔操作信息
    }

    public static final class TDCSWbColorPen {
        public TDCSWbEntity tEntity;    // 基本信息
        public int dwColorPenNum;
        public TDCSWbPoint[] atCPList;  // 曲线点信息列表
        public int dwLineWidth;         // 线宽
        public long dwRgb;              // 颜色，强转成int类型就是颜色值了。比如long类型的4294967295强转成int就是0xFFFFFFFF（纯白色），即-1。
    }

    public static final class TDCSWbInsertPicOperInfo {
        public String achTabId;         // 白板tab id（guid）
        public int dwSubPageId;         // 子页面id
        public String achImgId;         // 图元ID
        public int dwImgWidth;
        public int dwImgHeight;
        public TDCSWbPoint tPoint;
        public String achPicName;
        public String[] aachMatrixValue;
        public TDCSWbInsertPicOperInfo(){
            tPoint = new TDCSWbPoint(10, 50);
            aachMatrixValue = new String[]{
                    "0.500000",
                    "0.000000",
                    "162.000000",
                    "0.000000",
                    "0.500000",
                    "155.250000",
                    "0.000000",
                    "0.000000",
                    "1.000000"
            };
        }
    }

    public static final class TDCSWbPitchPicOperInfo {
        public String achTabId;         // 白板tab id（guid）
        public int dwSubPageId;         // 子页面id
        public int dwGraphsCount;
        public TDCSWbGraphsInfo[] atGraphsInfo;
    }

    public static final class TDCSWbGraphsInfo {
        public String achGraphsId;        // 图元ID
        public String[] aachMatrixValue;
    }

    public static final class TDCSWbDelPicOperInfo {
        public String achTabId;         // 白板tab id（guid）
        public int dwSubPageId;         // 子页面id
        public int dwGraphsCount;
        public String[] achGraphsId;
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

    public  static final class TDCSWbDisPlayInfo {
        public String achTabId;        //tab白板页
        public int dwSubPageId;        //子页ID
        public String[] aachMatrixValue;     //滚动到的目标点坐标
        TDCSWbDisPlayInfo(){
            aachMatrixValue = new String[]{
                    "0.500000",
                    "0.000000",
                    "162.000000",
                    "0.000000",
                    "0.500000",
                    "155.250000",
                    "0.000000",
                    "0.000000",
                    "1.000000"
            };
        }
    }


    public static final class TDCSWbTabPageIdInfo {
        String 	 achTabId;
        int  dwSubPageId;
    }

    public static final class TDCSImageUrl {
        public String achConfE164;
        public String achTabId;
        public int dwPageId;
        public String achPicUrl;
        public String achWbPicentityId;     // 图片ID
    }

    public class TDCSConfUserInfo {
        // @formatter:off
        public String achE164;
        /**
         * 只有在添加与会方，删除与会方用到
         */
        public String achName;
        public EmDcsType emMttype;
        /**
         * 暂时只在获取与会人员列表中有效
         */
        public boolean bOnline;
        public boolean bIsOper;
        public boolean bIsConfAdmin;
    }


    //<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<< 数据协作








}
