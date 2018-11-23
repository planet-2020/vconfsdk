package com.kedacom.vconf.sdk.base;

import android.graphics.PointF;

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




//    public static final class TDCSWbImageOperInfo{
//        public String  achTabId;
//        public int     dwSubPageId; // ???
//        public TDCSWbImage	 tImage;
//    }

//    public  static final class TDCSWbImage {
//        TDCSWbEntity	tEntity;				// 基本信息
//        TDCSWbPoint 	tBoardPt;				// 边界矩形左上角坐标
//        int 	    dwWidth;				// 边界矩形宽度
//        int 		    dwHeight;				// 边界矩形宽度
//        EmDcsWbImageState   emNetworkstate;			// 网络状态信息
//        String  achFileName;          // 文件名（utf8编码）
//        boolean	    bBkImg;	    // 是否文档底图
//    }


    public static final class DCOvalOp extends DCDrawOp{
        public float left;
        public float top;
        public float right;
        public float bottom;
    }

    public static final class DCRectOp extends DCDrawOp{
        public float left;
        public float top;
        public float right;
        public float bottom;
    }

    public static final class DCPathOp extends DCDrawOp{
        public PointF[] points;
    }


    public static final class DCInertPicOp extends DCPaintOp{
        public String picId;
        public String picName;
        public int  width;  //TODO 图片原始宽？
        public int  height; //TODO 图片原始高？
        public PointF dstPos; // 目标位置（左上角坐标点）
        public String[] matrixValue; // TODO 放缩及位置信息？
    }

    public static final class DCDelPicOp extends DCPaintOp{
        public String[] picIds;
        DCDelPicOp(){
        }
    }

    public static final class DCRectEraseOp extends DCPaintOp{
        public float left;
        public float top;
        public float right;
        public float bottom;
    }

    public static final class DCFullScreenMatrixOp extends DCPaintOp{
        public String[] matrixValue;
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
    public static class DCPaintOp {
        public String   id;         // 操作ID，唯一标识该操作。由终端使用GUID来填写
        public String   confE164;   // 所属会议e164号
        public String   boardId;    // 画板ID
        public int      pageId;     // 文档页ID（仅文档模式下有效）

        public int      sn;             // 操作序列号，用来表示操作的先后顺序，越小越靠前。由平台填写。
        public String   authorE164;      // 操作发出者。由平台填写。
        public boolean  bCached;   // 是否是服务器缓存的图元。由平台填写。
    }

    /**
     * 图形绘制操作基类。
     * 各种几何图形的绘制操作。
     * */
    public static class DCDrawOp extends DCPaintOp {
        public int strokeWidth;     // 线宽
        public int color;           // 颜色值
    }

    /**
     * 画线
     * */
    public static final class DCLineOp extends DCDrawOp{
        public float startX;
        public float stopX;
        public float startY;
        public float stopY;
    }


    /**
     * 放缩
     * */
    public static class DCZoomOp extends DCPaintOp {
        public int percentage;   // 放缩百分比。100%为没有放缩，50%为缩小一半。
    }

    /**
     * 滚屏
     * */
    public static class DCScrollOp extends DCPaintOp {
        public float stopX;   //TODO 中心点？
        public float stopY;
    }


    /**拖拽图片*/
    public static final class DCDragPicOp extends DCPaintOp{
        public DCPicMatrix[] picMatrices;
    }

    public static final class DCPicMatrix {
        public String picId;        // 图元ID
        public String[] matrixValue;
        public DCPicMatrix(String picId, String[] matrixValue){
            this.picId = picId;
            this.matrixValue = matrixValue;
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
