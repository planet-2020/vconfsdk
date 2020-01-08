package com.kedacom.vconf.sdk.webrtc;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.media.projection.MediaProjection;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.SurfaceHolder;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.common.base.Function;
import com.google.common.collect.BiMap;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.HashBiMap;
import com.kedacom.vconf.sdk.amulet.Caster;
import com.kedacom.vconf.sdk.amulet.IResultListener;
import com.kedacom.vconf.sdk.common.constant.EmConfProtocol;
import com.kedacom.vconf.sdk.common.constant.EmMtCallDisReason;
import com.kedacom.vconf.sdk.common.constant.EmMtChanState;
import com.kedacom.vconf.sdk.common.constant.EmMtResolution;
import com.kedacom.vconf.sdk.common.type.BaseTypeInt;
import com.kedacom.vconf.sdk.common.type.vconf.TAssVidStatus;
import com.kedacom.vconf.sdk.common.type.vconf.TMtAssVidStatusList;
import com.kedacom.vconf.sdk.common.type.vconf.TMtCallLinkSate;
import com.kedacom.vconf.sdk.utils.log.KLog;
import com.kedacom.vconf.sdk.utils.thread.HandlerHelper;
import com.kedacom.vconf.sdk.webrtc.bean.trans.TCreateConfResult;
import com.kedacom.vconf.sdk.webrtc.bean.trans.TRegState;
import com.kedacom.vconf.sdk.webrtc.bean.trans.TMTEntityInfo;
import com.kedacom.vconf.sdk.webrtc.bean.trans.TMTEntityInfoList;
import com.kedacom.vconf.sdk.webrtc.bean.trans.TMtId;
import com.kedacom.vconf.sdk.webrtc.bean.trans.TMtRtcSvrAddr;
import com.kedacom.vconf.sdk.webrtc.bean.trans.TQueryConfInfoResult;
import com.kedacom.vconf.sdk.webrtc.bean.trans.TRtcPlayItem;
import com.kedacom.vconf.sdk.webrtc.bean.trans.TRtcPlayParam;
import com.kedacom.vconf.sdk.webrtc.bean.trans.TRtcStreamInfo;
import com.kedacom.vconf.sdk.webrtc.bean.trans.TRtcStreamInfoList;

import org.checkerframework.checker.nullness.compatqual.NullableDecl;
import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraEnumerator;
import org.webrtc.CameraVideoCapturer;
import org.webrtc.CryptoOptions;
import org.webrtc.DataChannel;
import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.MediaStreamTrack;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RendererCommon;
import org.webrtc.RtpParameters;
import org.webrtc.RtpReceiver;
import org.webrtc.RtpSender;
import org.webrtc.RtpTransceiver;
import org.webrtc.ScreenCapturerAndroid;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.SoftwareVideoDecoderFactory;
import org.webrtc.SoftwareVideoEncoderFactory;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoDecoderFactory;
import org.webrtc.VideoEncoderFactory;
import org.webrtc.VideoFrame;
import org.webrtc.VideoSink;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;
import org.webrtc.audio.AudioDeviceModule;
import org.webrtc.audio.JavaAudioDeviceModule;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.kedacom.vconf.sdk.webrtc.bean.*;

@SuppressWarnings({"unused", "WeakerAccess"})
public class WebRtcManager extends Caster<Msg>{

    private static final String TAG = WebRtcManager.class.getSimpleName();

    private Application context;

    private RtcConnector rtcConnector = new RtcConnector();

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private EglBase eglBase;
    private PeerConnectionFactory factory;
    private PeerConnectionWrapper pubPcWrapper;
    private PeerConnectionWrapper subPcWrapper;
    private PeerConnectionWrapper assPubPcWrapper;
    private PeerConnectionWrapper assSubPcWrapper;


    // 与会方集合。
    private Set<Conferee> conferees = new HashSet<>();

    // 用来展示与会方画面的Display集合。
    private Set<Display> displaySet = new HashSet<>();

    // WebRTC的mid到平台的StreamId之间的映射
    private BiMap<String, String> mid2KdStreamIdMap = HashBiMap.create();
    // 平台的StreamId到WebRTC的TrackId之间的映射
    private BiMap<String, String> kdStreamId2RtcTrackIdMap = HashBiMap.create();


    // 当前用户的e164
    private String userE164;

    private static WebRtcManager instance;

    private UserConfig userConfig;

    private static final int ConfereeWaitVideoStreamTimeout = 1;
    private static final int RecvingAssStreamTimeout = 2;
    private Handler sessionHandler = new Handler(Looper.getMainLooper()){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case ConfereeWaitVideoStreamTimeout:
                    Conferee conferee = (Conferee) msg.obj;
                    conferee.bWaitingVideoStream = false;
                    break;
                case RecvingAssStreamTimeout:
                    conferee = (Conferee) msg.obj;
                    conferee.setState(conferee.preState);
                    break;
            }
        }
    };


    private WebRtcManager(Application context){
        this.context = context;
        userConfig = new UserConfig(context);
    }

    public synchronized static WebRtcManager getInstance(@NonNull Application context){
        if (null == instance){
            return instance = new WebRtcManager(context);
        }
        return instance;
    }


    @Override
    protected Map<Msg[], RspProcessor<Msg>> rspsProcessors() {
        Map<Msg[], RspProcessor<Msg>> processorMap = new HashMap<>();
        processorMap.put(new Msg[]{
                Msg.Register,
                Msg.Call,
                Msg.CreateConf,
                Msg.QuitConf,
                Msg.EndConf,
                Msg.AcceptInvitation,
                Msg.DeclineInvitation,
                Msg.SetSilence,
                Msg.SetMute,
                Msg.ToggleScreenShare,
                Msg.QueryConfInfo,
                Msg.VerifyConfPassword,
                Msg.CloseMyMainVideoChannel,
        }, this::onRsp);
        return processorMap;
    }

    @Override
    protected Map<Msg[], NtfProcessor<Msg>> ntfsProcessors() {
        Map<Msg[], NtfProcessor<Msg>> processorMap = new HashMap<>();
        processorMap.put(new Msg[]{
                Msg.RegisteredStateChanged,
                Msg.CallIncoming,
                Msg.ConfCanceled,
                Msg.MultipartyConfEnded,
                Msg.CurrentConfereeList,
                Msg.ConfereeJoined,
                Msg.ConfereeLeft,
                Msg.CurrentStreamList,
                Msg.StreamJoined,
                Msg.StreamLeft,
                Msg.ConfPasswordNeeded,
        }, this::onNtfs);

        return processorMap;
    }


    /**
     * 登录rtc
     * 注意，需先登录aps成功。
     * @param e164
     * @param resultListener 结果监听器。
     *          成功: null;
     *          失败：错误码 TODO
     * @param confEventListener 会议事件监听器
     * */
    public void login(String e164, IResultListener resultListener, ConfEventListener confEventListener){
        TMtRtcSvrAddr rtcSvrAddr = (TMtRtcSvrAddr) get(Msg.GetSvrAddr);
        if (null == rtcSvrAddr || rtcSvrAddr.dwIp<= 0){
            KLog.p(KLog.ERROR, "invalid rtcSvrAddr");
            reportFailed(-1, resultListener);
            return;
        }

        userE164 = e164;
        rtcSvrAddr.bUsedRtc = true;
        rtcSvrAddr.achNumber = e164;

        this.confEventListener = confEventListener;
        req(Msg.Register, resultListener, rtcSvrAddr);
    }

    /**
     * 登出rtc
     * @param resultListener 结果监听器。
     *          成功: null;
     *          失败：错误码 TODO
     * */
    public void logout(IResultListener resultListener){
        this.confEventListener = null;
        userE164 = null;
        stopSession();
        req(Msg.Register, resultListener, new TMtRtcSvrAddr(false));
    }

    /**
     * 呼叫
     * NOTE：目前只支持同时开一个会。如果呼叫中/创建中或会议中状态，则返回失败，需要先退出会议状态。
     * @param peerId 对于点对点而言是对端e164，对于多方会议而言是会议e164
     * @param bAudio 是否音频方式入会
     **@param resultListener 结果监听器。
     *          成功: {@link MakeCallResult};
     *          失败：TODO
     * @param sessionEventListener 会话事件监听器
     * */
    public void makeCall(String peerId, boolean bAudio, IResultListener resultListener, SessionEventListener sessionEventListener){
        if (!startSession(sessionEventListener)){
            reportFailed(-1, resultListener);
            return;
        }
        req(Msg.Call, resultListener, peerId,
                bAudio ? 64 : 1024*4, // 音频入会64K码率，视频入会4M
                EmConfProtocol.emrtc);
    }

    /**
     * 创建会议
     * NOTE：目前只支持同时开一个会。如果呼叫中/创建中或会议中状态，则返回失败，需要先退出会议状态。
     * @param confPara 创会参数
     **@param resultListener 结果监听器。
     *          成功: {@link CreateConfResult};
     *          失败：TODO
     * @param sessionEventListener 会话事件监听器
     * */
    public void createConf(ConfPara confPara, IResultListener resultListener, SessionEventListener sessionEventListener){
        if (!startSession(sessionEventListener)){
            reportFailed(-1, resultListener);
            return;
        }
        if (confPara.bSelfAudioMannerJoin){
            // 音频入会则关闭己端主视频通道。底层上报onGetOfferCmd时带的媒体类型就为Audio了。
            req(Msg.CloseMyMainVideoChannel, resultListener);
        }
        req(Msg.CreateConf, resultListener, ToDoConverter.confPara2CreateConference(confPara), EmConfProtocol.emrtc);
    }


    /**
     * 退出会议。
     * @param disReason 原因码
     * */
    public void quitConf(EmMtCallDisReason disReason, IResultListener resultListener){
        if (!stopSession()){
            reportFailed(-1, resultListener);
            return;
        }
        req(Msg.QuitConf, resultListener, disReason);
    }

    /**
     * 结束会议。
     * */
    public void endConf(IResultListener resultListener){
        if (!stopSession()){
            reportFailed(-1, resultListener);
            return;
        }
        req(Msg.EndConf, resultListener);
    }

    /**
     * 接受会议邀请
     * NOTE：目前只支持同时开一个会。如果呼叫中/创建中或会议中状态，则返回失败，需要先退出会议状态。
     * @param bAudio 是否音频方式入会
     * @param sessionEventListener 会话事件监听器
     *                             成功：{@link MakeCallResult}
     *                             失败：失败码 //TODO
     * */
    public void acceptInvitation(boolean bAudio, IResultListener resultListener, SessionEventListener sessionEventListener){
        if (!startSession(sessionEventListener)){
            reportFailed(-1, resultListener);
            return;
        }
        if (bAudio) {
            // 音频入会则关闭己端主视频通道。底层上报onGetOfferCmd时带的媒体类型就为Audio了。
            req(Msg.CloseMyMainVideoChannel, resultListener);
        }
        req(Msg.AcceptInvitation, resultListener);
    }

    /**
     * 拒绝会议邀请
     * */
    public void declineInvitation(){
        req(Msg.DeclineInvitation, null);
    }


    /**
     * 查询会议详情
     * @param confE164 会议e164号
     * @param resultListener 结果监听器。
     *          成功: {@link ConfInfo};
     *          失败：TODO
     * */
    public void queryConfInfo(String confE164, IResultListener resultListener){
        req(Msg.QueryConfInfo, resultListener, confE164);
    }


    /**
     * 验证会议密码
     * @param passwd 会议密码
     * @param resultListener 结果监听器。
     *          成功: null;
     *          失败：{@link RtcErrorCode#IncorrectConfPassword}
     * */
    public void verifyConfPassword(String passwd, IResultListener resultListener){
        req(Msg.VerifyConfPassword, resultListener, passwd);
    }



    private Intent screenCapturePermissionData;
    /**
     * 开始桌面共享
     * @param permissionData 截屏权限申请结果。该参数可以通过如下方式取得：
     * {@code
     *     @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
     *     private void requestScreenCapturePermission() {
     *         MediaProjectionManager mediaProjectionManager = (MediaProjectionManager) getApplication().getSystemService(Context.MEDIA_PROJECTION_SERVICE);
     *         Intent intent = mediaProjectionManager.createScreenCaptureIntent();
     *         startActivityForResult(intent, YOUR_REQ_CODE);
     *     }
     *
     *     ...
     *
     *         @Override
     *     protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
     *         super.onActivityResult(requestCode, resultCode, data);
     *         if (requestCode == YOUR_REQ_CODE){
     *             if (RESULT_OK == resultCode){
     *                 webRtcClient.startScreenShare(data, resultListener);
     *             }
     *         }
     *     }
     * }
     * */
    public void startScreenShare(Intent permissionData, IResultListener resultListener){
        if (null == permissionData){
            KLog.p(KLog.ERROR, "null == permissionData");
            reportFailed(-1, resultListener);
            return;
        }
        if (null != screenCapturePermissionData){
            KLog.p(KLog.ERROR, "Screen Share started already");
            reportFailed(-1, resultListener);
            return;
        }
        screenCapturePermissionData = permissionData;
        req(Msg.ToggleScreenShare, resultListener, true);
    }

    /**
     * 结束桌面共享
     * */
    public void stopScreenShare(){
        if (null == screenCapturePermissionData){
            KLog.p(KLog.ERROR, "Screen Share not started yet!");
            return;
        }
        screenCapturePermissionData = null;

        req(Msg.ToggleScreenShare, null, false);
    }


    private View sharedWindow;
    /**
     * 开启窗口共享
     * */
    public void startWindowShare(@NonNull View window, IResultListener resultListener){
        if (null != sharedWindow){
            KLog.p(KLog.WARN, "window share started already!");
            return;
        }
        sharedWindow = window;
        req(Msg.ToggleScreenShare, resultListener, true);
    }

    /**
     * 结束窗口共享
     * */
    public void stopWindowShare(){
        if (null == sharedWindow){
            KLog.p(KLog.WARN, "window share stopped already!");
            return;
        }
        sharedWindow = null;
        req(Msg.ToggleScreenShare, null, false);
    }


    /**
     * 是否已静音。
     * */
    public boolean isSilenced(){
        return !userConfig.getIsRemoteAudioEnabled();
    }

    /**
     * 设置静音。（放开/屏蔽对方语音，默认放开）
     * @param bSilence true，屏蔽对方语音；false，放开对方语音。
     * */
    public void setSilence(boolean bSilence){
        if (!bSilence == userConfig.getIsRemoteAudioEnabled()){
            KLog.p(KLog.WARN, "!bSilence == userConfig.getIsRemoteAudioEnabled()");
            return;
        }
        req(Msg.SetSilence, null, bSilence);
    }

    /**
     * 是否已哑音。
     * */
    public boolean isMute(){
        return !userConfig.getIsLocalAudioEnabled();
    }

    /**
     * 设置哑音。（放开/屏蔽自己语音，默认放开）
     * @param bMute true，屏蔽自己语音；false，放开自己语音。
     * */
    public void setMute(boolean bMute){
        if (!bMute == userConfig.getIsLocalAudioEnabled()){
            KLog.p(KLog.WARN, "!bMute == userConfig.getIsLocalAudioEnabled()");
            return;
        }
        req(Msg.SetMute, null, bMute);
    }


    /**
     * 摄像头偏好是否为前置
     * @return true 前置；false 后置
     * */
    public boolean isPreferFrontCamera(){
        return userConfig.getIsPreferFrontCamera();
    }

    /**
     * 切换摄像头
     * */
    public void switchCamera(){
        PeerConnectionWrapper pcWrapper = getPcWrapper(CommonDef.CONN_TYPE_PUBLISHER);
        if (null != pcWrapper) {
            pcWrapper.switchCamera();
        }
        userConfig.setIsPreferFrontCamera(!userConfig.getIsPreferFrontCamera());
    }


    /**
     * 摄像头是否开启。
     * */
    public boolean isCameraEnabled(){
        return userConfig.getIsLocalVideoEnabled();
    }

    /**
     * 开启/关闭摄像头
     * */
    public void setCameraEnable(final boolean enable) {
        if (enable == userConfig.getIsLocalVideoEnabled()){
            KLog.p(KLog.WARN, "enable == userConfig.getIsLocalVideoEnabled()");
            return;
        }
        PeerConnectionWrapper pcWrapper = getPcWrapper(CommonDef.CONN_TYPE_PUBLISHER);
        if (null != pcWrapper) {
            pcWrapper.setLocalVideoEnable(enable);
        }
        userConfig.setIsLocalVideoEnabled(enable);

        Conferee conferee = findMyself();
        if (null != conferee && Conferee.VideoState_NoStream != conferee.state){
            conferee.setState(enable ? Conferee.VideoState_Normal : Conferee.VideoState_Disabled);
        }
    }

//    /**
//     * 开启/关闭视频
//     * */
//    public void setVideoEnable(final boolean enable) {
//        PeerConnectionWrapper pcWrapper = getPcWrapper(CommonDef.CONN_TYPE_PUBLISHER);
//        if (null != pcWrapper) {
//            pcWrapper.setLocalVideoEnable(enable);
//        }
//        pcWrapper = getPcWrapper(CommonDef.CONN_TYPE_SUBSCRIBER);
//        if (null != pcWrapper) {
//            pcWrapper.setRemoteVideoEnable(enable);
//        }
//        pcWrapper = getPcWrapper(CommonDef.CONN_TYPE_ASS_PUBLISHER);
//        if (null != pcWrapper) {
//            pcWrapper.setLocalVideoEnable(enable);
//        }
//        pcWrapper = getPcWrapper(CommonDef.CONN_TYPE_ASS_SUBSCRIBER);
//        if (null != pcWrapper) {
//            pcWrapper.setRemoteVideoEnable(enable);
//        }
//    }

    private boolean onRsp(Msg rsp, Object rspContent, IResultListener listener, Msg req, Object[] reqParas) {
        switch (rsp){
            case RegisteredStateChanged:
                TRegState loginResult = (TRegState) rspContent;
                KLog.p("loginResult: %s", loginResult.AssParam.basetype);
                TMtRtcSvrAddr rtcSvrAddr = (TMtRtcSvrAddr) reqParas[0];
                if (rtcSvrAddr.bUsedRtc) {
                    if (100 == loginResult.AssParam.basetype) {
                        reportSuccess(null, listener);
                    } else {
                        reportFailed(-1, listener);
                    }
                }else{
                    reportSuccess(null, listener);
                }
                break;

            case Calling:
                TMtCallLinkSate callLinkSate = (TMtCallLinkSate) rspContent;
                KLog.p("Calling: %s", callLinkSate);
                if (EmConfProtocol.emrtc != callLinkSate.emConfProtocol){
                    return false;
                }
                break;

            case MultipartyConfStarted:
                callLinkSate = (TMtCallLinkSate) rspContent;
                KLog.p("P2pConfStarted: %s", callLinkSate);
                if (Msg.Call == req || Msg.AcceptInvitation == req) {
                    reportSuccess(ToDoConverter.callLinkState2MakeCallResult(callLinkSate), listener);
                }else if (Msg.CreateConf == req){
                    reportSuccess(ToDoConverter.callLinkState2CreateConfResult(callLinkSate), listener);
                }
                break;

            case MultipartyConfEnded:
                BaseTypeInt reason = (BaseTypeInt) rspContent;
                KLog.p("MultipartyConfEnded: %s", reason.basetype);
                if (Msg.QuitConf == req || Msg.EndConf == req) {
                    reportSuccess(null, listener);
                }else{
                    reportFailed(-1, listener);
                }
                break;

            case ConfCanceled:
                stopSession();
                reason = (BaseTypeInt) rspContent;
                KLog.p("ConfCanceled: %s", reason.basetype);
                reportFailed(-1, listener);
                break;

            case ToggleScreenShareRsp:
                boolean bOpen = (boolean) reqParas[0];
                TMtAssVidStatusList assVidStatusList = (TMtAssVidStatusList) rspContent;
                if (assVidStatusList.arrTAssVidStatus.length == 0){
                    reportFailed(-1, listener);
                }else{
                    TAssVidStatus assVidStatus = assVidStatusList.arrTAssVidStatus[0]; // 目前仅支持一路
                    if (bOpen){
                        // 开启双流对应的响应
                        if (EmMtChanState.emChanConnected == assVidStatus.emChanState){
                            reportSuccess(null, listener);
                        }else{
                            sharedWindow = null;
                            reportFailed(-1, listener);
                        }
                    }else{
                        // 关闭双流对应的响应
                        if (EmMtChanState.emChanConnected != assVidStatus.emChanState){
                            reportSuccess(null, listener);
                        }else{
                            reportFailed(-1, listener);
                        }
                    }
                }
                break;

            case CreateConfRsp:
                TCreateConfResult tCreateConfResult = (TCreateConfResult) rspContent;
                if (1000 != tCreateConfResult.MainParam.dwErrorID){
                    stopSession();
                    cancelReq(req, listener);
                    reportFailed(-1, listener);
                }
                break;

            case QueryConfInfoRsp:
                TQueryConfInfoResult queryConfInfoResult = (TQueryConfInfoResult) rspContent;
                if (1000 != queryConfInfoResult.MainParam.dwErrorID){
                    reportFailed(-1, listener);
                }else{
                    reportSuccess(ToDoConverter.tMTInstanceConferenceInfo2ConfInfo(queryConfInfoResult.AssParam), listener);
                }
                break;

            case MyLabelAssigned:
                if (Msg.VerifyConfPassword == req) {
                    reportSuccess(null, listener);
                }
                break;

            case ConfPasswordNeeded:
                if (Msg.VerifyConfPassword == req) {
                    reportFailed(RtcErrorCode.IncorrectConfPassword, listener);
                }
                break;

            default:
                return false;
        }

        return true;
    }

    @Override
    protected boolean onTimeout(Msg req, IResultListener rspListener, Object[] reqPara) {
        switch (req){
            case Call:
            case CreateConf:
            case AcceptInvitation:
                stopSession();
                break;
            case ToggleScreenShare:
                sharedWindow = null;
                break;
        }
        return super.onTimeout(req, rspListener, reqPara);
    }



    private void onNtfs(Msg ntfId, Object ntfContent, Set<Object> listeners) {
        switch (ntfId){
            case RegisteredStateChanged:
                TRegState regState = (TRegState) ntfContent;
                if (100 != regState.AssParam.basetype) {
                    if (null != confEventListener) confEventListener.onConfServerDisconnected();
                }
                break;

            case CallIncoming:
                TMtCallLinkSate callLinkSate = (TMtCallLinkSate) ntfContent;
                KLog.p("CallIncoming: %s", callLinkSate);
                if (null != confEventListener) confEventListener.onConfInvitation(ToDoConverter.callLinkSate2ConfInvitationInfo(callLinkSate));
                break;

            case MultipartyConfEnded:
                stopSession();
                BaseTypeInt reason = (BaseTypeInt) ntfContent;
                KLog.p("MultipartyConfEnded: %s", reason.basetype);
                if (null != confEventListener) confEventListener.onConfFinished();
                break;

            case ConfCanceled:
                stopSession();
                reason = (BaseTypeInt) ntfContent;
                KLog.p("ConfCanceled: %s", reason.basetype);
                if (null != confEventListener) confEventListener.onConfFinished();
                break;

            case CurrentConfereeList: // NOTE: 入会后会收到一次该通知，创会者也会收到这条消息
                for (TMTEntityInfo entityInfo : ((TMTEntityInfoList) ntfContent).atMtEntitiy) {
                    if (null != findConfereeByConfereeId(Conferee.buildId(entityInfo.dwMcuId, entityInfo.dwTerId, false))) {
                        // 去重
                        continue;
                    }
                    Conferee conferee = ToDoConverter.tMTEntityInfo2ConfereeInfo(entityInfo);
                    conferees.add(conferee);
                    conferee.bWaitingVideoStream = true; // 正在等待与之相关的视频码流
                    HandlerHelper.sendMessageDelayed(sessionHandler, ConfereeWaitVideoStreamTimeout, conferee, 3000);
                    if (null != sessionEventListener) {
                        sessionEventListener.onConfereeJoined(conferee);
                    }
                }
                if (!tmpStreamInfos.isEmpty()){ // 码流先于与会方上来了
                    dealStreamAdded(tmpStreamInfos);
                    tmpStreamInfos.clear();
                }
                break;

            case ConfereeJoined:
                TMTEntityInfo entityInfo = (TMTEntityInfo) ntfContent;
                if(null == findConfereeByConfereeId(Conferee.buildId(entityInfo.dwMcuId, entityInfo.dwTerId, false))) {
                    Conferee joinedConferee = ToDoConverter.tMTEntityInfo2ConfereeInfo((TMTEntityInfo) ntfContent);
                    conferees.add(joinedConferee);
                    joinedConferee.bWaitingVideoStream = true; // 正在等待与之相关的码流
                    HandlerHelper.sendMessageDelayed(sessionHandler, ConfereeWaitVideoStreamTimeout, joinedConferee, 3000);
                    if (null != sessionEventListener) {
                        sessionEventListener.onConfereeJoined(joinedConferee);
                    }
                }
                break;

            case ConfereeLeft:
                TMtId tMtId = (TMtId) ntfContent;
                Conferee leftConferee = findConfereeByConfereeId(Conferee.buildId(tMtId.dwMcuId, tMtId.dwTerId, false));
                if (null != leftConferee){
                    conferees.remove(leftConferee);
                    if (null != sessionEventListener) {
                        sessionEventListener.onConfereeLeft(leftConferee);
                    }
                }
                break;

            case CurrentStreamList: // NOTE: 创会者不会收到这条消息；CurrentStreamList和CurrentConfereeList的先后顺序不定
            case StreamJoined: // NOTE: 己端不会收到自己的流joined的消息
                if (conferees.isEmpty()){
                    // CurrentConfereeList 消息尚未上报，我们暂存码流信息稍后处理
                    tmpStreamInfos.addAll(((TRtcStreamInfoList) ntfContent).atStramInfoList);
                }else{
                    dealStreamAdded(((TRtcStreamInfoList) ntfContent).atStramInfoList);
                }
                break;

            case StreamLeft: // NOTE 己端不会收到自己的流left的消息
                Conferee assConferee = null;
                for (TRtcStreamInfo kdStream : ((TRtcStreamInfoList) ntfContent).atStramInfoList){
                    Conferee owner = findConfereeByStreamId(kdStream.achStreamId);
                    if (null == owner){
                        KLog.p(KLog.ERROR, "this stream not belong to any conferee");
                        continue;
                    }

                    // 删除track
                    // 我们在onTrack回调中createRemoteVideoTrack/createRemoteAudioTrack，
                    // 相应的我们原本期望在onRemoveStream（没有onRemoveTrack）中removeRemoteVideoTrack/removeRemoteAudioTrack，
                    // 然而实测下来发现onRemoveStream回调上来时MediaStream的track列表为空，不得已我们只得在StreamLeft消息上来时
                    // removeRemoteVideoTrack/removeRemoteAudioTrack，由此造成了这种不对称的现象
                    PeerConnectionWrapper pcWrapper;
                    VideoStream stream = findVideoStream(kdStream.achStreamId); // StreamLeft消息中只有流id有效，所以我们需本地查找到对应的流再做进一步判断
                    if (null != stream){
                        if (stream.bAss) {
                            assConferee = owner; // 对于辅流我们稍后特殊处理。我们认为只可能有一路辅流，所以此处在循环中赋值也没关系，只可能赋值一次。
                            pcWrapper = getPcWrapper(CommonDef.CONN_TYPE_ASS_SUBSCRIBER);
                        }else{
                            pcWrapper = getPcWrapper(CommonDef.CONN_TYPE_SUBSCRIBER);
                        }
                        if (null != pcWrapper) pcWrapper.removeRemoteVideoTrack(stream.streamId);
                    }else{
                        AudioStream audioStream = findAudioStream(kdStream.achStreamId);
                        if (null != audioStream){
                            pcWrapper = getPcWrapper(CommonDef.CONN_TYPE_SUBSCRIBER);
                            if (null != pcWrapper) pcWrapper.removeRemoteAudioTrack(audioStream.streamId);
                        }
                    }

                    owner.removeStream(kdStream.achStreamId);
                }

                // 对于辅流特殊处理
                if (null != assConferee){
                    conferees.remove(assConferee);
                    // 辅流退出意味着虚拟的辅流入会方退会
                     if (null != sessionEventListener) {
                         sessionEventListener.onConfereeLeft(assConferee);
                     }
                }

                // 重新订阅视频流
                Set<VideoStream> videoStreams = getAllVideoStreams();
                List<TRtcPlayItem> playItems = FluentIterable.from(videoStreams).transform(new Function<VideoStream, TRtcPlayItem>() {
                    @NullableDecl
                    @Override
                    public TRtcPlayItem apply(@NullableDecl VideoStream input) {
                        return new TRtcPlayItem(input.streamId, input.bAss, input.supportedResolutionList.get(0));
                    }
                }).toList();
                set(Msg.SelectStream, new TRtcPlayParam(playItems));

                break;

            case ConfPasswordNeeded:
                if (null != sessionEventListener){
                    sessionEventListener.confPasswordNeeded();
                }
                break;
        }

    }


    /**
     * 临时的码流信息。
     * 码流我们目前是绑定在Conferee里的，但是下层上报“新增码流”消息时可能尚未上报“新增Conferee消息”，
     * 就导致码流上来了却没有Conferee可以绑定，针对这种情形我们临时保存码流信息，等Conferee上报了再做绑定。
     * */
    private List<TRtcStreamInfo> tmpStreamInfos = new ArrayList<>();
    void dealStreamAdded(List<TRtcStreamInfo> rtcStreamInfos){
        TRtcStreamInfo assStream = null;
        Conferee assStreamSender = null;
        for (TRtcStreamInfo streamInfo : rtcStreamInfos){
            // 查找该流所属的与会方
            Conferee owner = findConfereeByConfereeId(Conferee.buildId(streamInfo.tMtId.dwMcuId, streamInfo.tMtId.dwTerId, false));
            if (null == owner){
                KLog.p(KLog.ERROR, "this stream not belong to any conferee");
                continue;
            }
            if (streamInfo.bAss){
                assStream = streamInfo;  // 对于辅流我们稍后特殊处理
                assStreamSender = owner;
                continue;
            }

            // 将流关联到与会方
            if (streamInfo.bAudio){
                owner.addAudioStream(new AudioStream(streamInfo.achStreamId));
            }else{
                owner.setVideoStream(new VideoStream(streamInfo.achStreamId, false, streamInfo.aemSimcastRes));
                owner.bWaitingVideoStream = false;
                sessionHandler.removeMessages(ConfereeWaitVideoStreamTimeout, owner);
            }
        }

        // 对辅流特殊处理
        if (null != assStream){
            // 针对辅流我们构造一个虚拟的与会方
            Conferee assStreamConferee = new Conferee(assStreamSender.mcuId, assStreamSender.terId, assStreamSender.e164, assStreamSender.alias, assStreamSender.email, true);
            assStreamConferee.setVideoStream(new VideoStream(assStream.achStreamId, true, assStream.aemSimcastRes));
            conferees.add(assStreamConferee);
            if (null != sessionEventListener){
                sessionEventListener.onConfereeJoined(assStreamConferee);
            }
            assStreamConferee.setState(Conferee.VideoState_RecvingAss);
            HandlerHelper.sendMessageDelayed(sessionHandler, RecvingAssStreamTimeout, assStreamConferee, 3000);
        }

        // 订阅视频流
        Set<VideoStream> videoStreams = getAllVideoStreams();
        List<TRtcPlayItem> playItems = FluentIterable.from(videoStreams).transform(new Function<VideoStream, TRtcPlayItem>() {
            @NullableDecl
            @Override
            public TRtcPlayItem apply(@NullableDecl VideoStream input) {
                return new TRtcPlayItem(input.streamId, input.bAss, input.supportedResolutionList.get(0));
            }
        }).toList();

        set(Msg.SelectStream, new TRtcPlayParam(playItems));

    }


    public static final String VIDEO_CODEC_VP8 = "VP8";
    public static final String VIDEO_CODEC_VP9 = "VP9";
    public static final String VIDEO_CODEC_H264 = "H264";
    public static final String VIDEO_CODEC_H264_BASELINE = "H264 Baseline";
    public static final String VIDEO_CODEC_H264_HIGH = "H264 High";
    public static final String AUDIO_CODEC_OPUS = "opus";
    public static final String AUDIO_CODEC_ISAC = "ISAC";
    private static String getSdpVideoCodecName(String videoCodec) {
        switch (videoCodec) {
            case VIDEO_CODEC_VP9:
                return VIDEO_CODEC_VP9;
            case VIDEO_CODEC_H264_HIGH:
            case VIDEO_CODEC_H264_BASELINE:
                return VIDEO_CODEC_H264;
            case VIDEO_CODEC_VP8:
            default:
                return VIDEO_CODEC_VP8;
        }
    }





    private boolean bSessionStarted;
    private synchronized boolean startSession(@NonNull SessionEventListener listener){
        if (bSessionStarted){
            KLog.p(KLog.ERROR, "session has started already!");
            return false;
        }
        bSessionStarted = true;
        sessionEventListener = listener;

        rtcConnector.setSignalingEventsCallback(new RtcConnectorEventListener());

        createPeerConnectionFactory();
        createPeerConnectionWrapper();

        // 定时获取统计信息
        sessionHandler.postDelayed(statsCollector, 3000);
        // 定时处理音频统计信息
        sessionHandler.postDelayed(audioStatsProcesser, 3000);
        // 定时处理视频统计信息
        sessionHandler.postDelayed(videoStatsProcesser, 3000);
        // 定时检测音频与会方
        sessionHandler.postDelayed(kdStreamStatusMonitor, 3000);

        KLog.p("session started ");

        return true;
    }


    /**
     * 停止会话
     * */
    private synchronized boolean stopSession(){
        if (!bSessionStarted){
            KLog.p(KLog.ERROR, "session has not started yet!");
            return false;
        }
        bSessionStarted = false;
        sessionEventListener = null;

        rtcConnector.setSignalingEventsCallback(null);

        sessionHandler.removeCallbacksAndMessages(null);

        for (Display display : displaySet){
            display.destroy();
        }
        displaySet.clear();

        conferees.clear();
        tmpStreamInfos.clear();

        mid2KdStreamIdMap.clear();
        kdStreamId2RtcTrackIdMap.clear();

        screenCapturePermissionData = null;

        destroyPeerConnectionWrapper();
        destroyPeerConnectionFactory();

        // destroy audiomanager
//        if (audioManager != null) {
//            audioManager.stop();
//            audioManager = null;
//        }

        KLog.p("session stopped ");

        return true;

    }




    private void createPeerConnectionFactory() {
        eglBase = EglBase.create();

        executor.execute(() -> {
            if (null != factory){
                KLog.p(KLog.ERROR, "Factory exists!");
                return;
            }

//            PeerConnectionFactory.startInternalTracingCapture(
//                    Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator
//                            + "webrtc-trace.txt");

            PeerConnectionFactory.initialize(PeerConnectionFactory.InitializationOptions.builder(context).createInitializationOptions());

            PeerConnectionFactoryConfig factoryConfig = new PeerConnectionFactoryConfig(Arrays.asList(VIDEO_CODEC_VP8, VIDEO_CODEC_H264_HIGH), userConfig.getEnableVideoCodecHwAcceleration());

            KLog.p(factoryConfig.toString());

            final AudioDeviceModule adm = createJavaAudioDevice();

            final VideoEncoderFactory encoderFactory;
            final VideoDecoderFactory decoderFactory;

            if (factoryConfig.enableVideoCodecHwAcceleration) {
                encoderFactory = new DefaultVideoEncoderFactory(
                        eglBase.getEglBaseContext(),
                        factoryConfig.videoCodecList.contains(VIDEO_CODEC_VP8),
                        factoryConfig.videoCodecList.contains(VIDEO_CODEC_H264_HIGH));
                decoderFactory = new DefaultVideoDecoderFactory(eglBase.getEglBaseContext());

//            encoderFactory = new HardwareVideoEncoderFactory(
//                    eglBase.getEglBaseContext(),
//                    config.videoCodecList.contains(VIDEO_CODEC_VP8),
//                    config.videoCodecList.contains(VIDEO_CODEC_H264_HIGH));
//            decoderFactory = new DefaultVideoDecoderFactory(eglBase.getEglBaseContext());
            } else {
                encoderFactory = new SoftwareVideoEncoderFactory();
                decoderFactory = new SoftwareVideoDecoderFactory();
            }

            factory = PeerConnectionFactory.builder()
                    .setOptions(new PeerConnectionFactory.Options())
                    .setAudioDeviceModule(adm)
                    .setVideoEncoderFactory(encoderFactory)
                    .setVideoDecoderFactory(decoderFactory)
                    .createPeerConnectionFactory();

            KLog.p("factory created!");

            adm.release();

//            Logging.enableLogToDebugOutput(Logging.Severity.LS_VERBOSE);

        });

    }


    private void destroyPeerConnectionFactory(){
        executor.execute(() -> {
            if (null == factory) {
                KLog.p(KLog.ERROR, "Factory not exists!");
                return;
            }

            if (null != eglBase) {
                eglBase.release();
                eglBase = null;
            }

            factory.dispose();
            factory = null;

            KLog.p(KLog.WARN, "factory destroyed!");
        });
    }


    private void createPeerConnectionWrapper() {
        /* 创建4个PeerConnectionWrapper
         * NOTE：一个peerconnection可以处理多路码流，收发均可。
         * 但业务要求主流发/收、辅流发/收4种情形分别用单独的peerconnect处理，故此处创建4个。
         * */
        PeerConnectionConfig pcConfig = new PeerConnectionConfig(
                userConfig.getVideoWidth(),
                userConfig.getVideoHeight(),
                userConfig.getVideoFps(),
                userConfig.getVideoMaxBitrate(),
                userConfig.getVideoCodec(),
                userConfig.getAudioStartBitrate(),
                userConfig.getAudioCodec()
        );
        KLog.p(pcConfig.toString());

        pubPcWrapper = new PeerConnectionWrapper(CommonDef.CONN_TYPE_PUBLISHER, pcConfig, new SDPObserver(CommonDef.CONN_TYPE_PUBLISHER));
        subPcWrapper = new PeerConnectionWrapper(CommonDef.CONN_TYPE_SUBSCRIBER, pcConfig, new SDPObserver(CommonDef.CONN_TYPE_SUBSCRIBER));
        assPubPcWrapper = new PeerConnectionWrapper(CommonDef.CONN_TYPE_ASS_PUBLISHER, pcConfig, new SDPObserver(CommonDef.CONN_TYPE_ASS_PUBLISHER));
        assSubPcWrapper = new PeerConnectionWrapper(CommonDef.CONN_TYPE_ASS_SUBSCRIBER, pcConfig, new SDPObserver(CommonDef.CONN_TYPE_ASS_SUBSCRIBER));

        executor.execute(() -> {
            if (null == factory){
                throw new RuntimeException("Factory not exists!");
            }

            PeerConnection pubPc = createPeerConnection(CommonDef.CONN_TYPE_PUBLISHER);
            PeerConnection subPc = createPeerConnection(CommonDef.CONN_TYPE_SUBSCRIBER);
            PeerConnection assPubPc = createPeerConnection(CommonDef.CONN_TYPE_ASS_PUBLISHER);
            PeerConnection assSubPc = createPeerConnection(CommonDef.CONN_TYPE_ASS_SUBSCRIBER);

            synchronized (pcWrapperLock) {
                if (null != pubPcWrapper) pubPcWrapper.setPeerConnection(pubPc);
                if (null != subPcWrapper) subPcWrapper.setPeerConnection(subPc);
                if (null != assPubPcWrapper) assPubPcWrapper.setPeerConnection(assPubPc);
                if (null != assSubPcWrapper) assSubPcWrapper.setPeerConnection(assSubPc);
            }

        });

    }


    private PeerConnection createPeerConnection(int connType){
        PeerConnection.RTCConfiguration rtcConfig = new PeerConnection.RTCConfiguration(new ArrayList<>());
        // TCP candidates are only useful when connecting to a server that supports
        // ICE-TCP.
        rtcConfig.tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.DISABLED;
        rtcConfig.bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE;
        rtcConfig.rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE;
        rtcConfig.continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY;
        // Use ECDSA encryption.
        rtcConfig.keyType = PeerConnection.KeyType.ECDSA;
        // Enable DTLS for normal calls and disable for loopback calls.
        rtcConfig.enableDtlsSrtp = true;
        rtcConfig.sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN;

        // 设置srtp（Secure rtp）加密算法
        CryptoOptions.Builder builder = CryptoOptions.builder();
        builder.setEnableGcmCryptoSuites(true);
        rtcConfig.cryptoOptions = builder.createCryptoOptions();

        return factory.createPeerConnection(rtcConfig, new PCObserver(connType));
    }


    private void recreatePeerConnection(int connType){
        synchronized (pcWrapperLock) {
            PeerConnectionWrapper pcWrapper = getPcWrapper(connType);
            if (null != pcWrapper) pcWrapper.close();
            executor.execute(() -> {
                if (null == factory){
                    KLog.p(KLog.ERROR, "Factory not exists!");
                    return;
                }

                PeerConnection pc = createPeerConnection(connType);

                synchronized (pcWrapperLock) {
                    PeerConnectionWrapper pcw = getPcWrapper(connType);
                    if (null != pcw){
                        pcw.setPeerConnection(pc);
                    }else{
                        pc.dispose();
                    }
                }

            });
        }
    }

    private final Object pcWrapperLock = new Object();

    private void destroyPeerConnectionWrapper(){
        synchronized (pcWrapperLock) {
            if (null != pubPcWrapper) {
                pubPcWrapper.close();
                pubPcWrapper = null;
            }
            if (null != subPcWrapper) {
                subPcWrapper.close();
                subPcWrapper = null;
            }
            if (null != assPubPcWrapper) {
                assPubPcWrapper.close();
                assPubPcWrapper = null;
            }
            if (null != assSubPcWrapper) {
                assSubPcWrapper.close();
                assSubPcWrapper = null;
            }
        }
    }


    private AudioDeviceModule createJavaAudioDevice() {

        // Set audio record error callbacks.
        JavaAudioDeviceModule.AudioRecordErrorCallback audioRecordErrorCallback = new JavaAudioDeviceModule.AudioRecordErrorCallback() {
            @Override
            public void onWebRtcAudioRecordInitError(String errorMessage) {
                KLog.p(KLog.ERROR,"onWebRtcAudioRecordInitError: " + errorMessage);
//                reportError(errorMessage);
            }

            @Override
            public void onWebRtcAudioRecordStartError(
                    JavaAudioDeviceModule.AudioRecordStartErrorCode errorCode, String errorMessage) {
                KLog.p(KLog.ERROR,"onWebRtcAudioRecordStartError: " + errorCode + ". " + errorMessage);
//                reportError(errorMessage);
            }

            @Override
            public void onWebRtcAudioRecordError(String errorMessage) {
                KLog.p(KLog.ERROR,"onWebRtcAudioRecordError: " + errorMessage);
//                reportError(errorMessage);
            }
        };

        JavaAudioDeviceModule.AudioTrackErrorCallback audioTrackErrorCallback = new JavaAudioDeviceModule.AudioTrackErrorCallback() {
            @Override
            public void onWebRtcAudioTrackInitError(String errorMessage) {
                KLog.p(KLog.ERROR, "onWebRtcAudioTrackInitError: " + errorMessage);
//                reportError(errorMessage);
            }

            @Override
            public void onWebRtcAudioTrackStartError(
                    JavaAudioDeviceModule.AudioTrackStartErrorCode errorCode, String errorMessage) {
                KLog.p(KLog.ERROR, "onWebRtcAudioTrackStartError: " + errorCode + ". " + errorMessage);
//                reportError(errorMessage);
            }

            @Override
            public void onWebRtcAudioTrackError(String errorMessage) {
                KLog.p(KLog.ERROR,"onWebRtcAudioTrackError: " + errorMessage);
//                reportError(errorMessage);
            }
        };

        return JavaAudioDeviceModule.builder(context)
//        .setSamplesReadyCallback(saveRecordedAudioToFile)
//                .setUseHardwareAcousticEchoCanceler(!peerConnectionParameters.disableBuiltInAEC)
//                .setUseHardwareNoiseSuppressor(!peerConnectionParameters.disableBuiltInNS)
                .setAudioRecordErrorCallback(audioRecordErrorCallback)
                .setAudioTrackErrorCallback(audioTrackErrorCallback)
                .createAudioDeviceModule();
    }




    private @Nullable VideoCapturer createCameraCapturer(CameraEnumerator enumerator) {
        final String[] deviceNames = enumerator.getDeviceNames();

        // First, try to find front facing camera
        for (String deviceName : deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);

                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }

        // Front facing camera not found, try something else
        for (String deviceName : deviceNames) {
            if (!enumerator.isFrontFacing(deviceName)) {
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);

                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }

        KLog.p(KLog.ERROR, "failed to createCameraCapturer");

        return null;
    }

    private VideoCapturer createScreenCapturer() {
        if (null == screenCapturePermissionData){
            KLog.p(KLog.ERROR, "null == screenCapturePermissionData");
            return null;
        }
        VideoCapturer videoCapturer = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            videoCapturer = new ScreenCapturerAndroid(screenCapturePermissionData, new MediaProjection.Callback() {
                @Override
                public void onStop() {
                    KLog.p("user has revoked permissions");
                }
            });
        }else{
            KLog.p(KLog.ERROR, "createScreenCapturer failed, API level < LOLLIPOP(21)");
        }
        return videoCapturer;
    }


    private VideoCapturer createWindowCapturer(){
        if (null == sharedWindow){
            KLog.p(KLog.ERROR, "null == sharedWindow");
            return null;
        }
        return new WindowCapturer(sharedWindow);
    }


    private PeerConnectionWrapper getPcWrapper(int connType){
        if (CommonDef.CONN_TYPE_PUBLISHER == connType){
            return pubPcWrapper;
        }else if (CommonDef.CONN_TYPE_SUBSCRIBER == connType){
            return subPcWrapper;
        }else if (CommonDef.CONN_TYPE_ASS_PUBLISHER == connType){
            return assPubPcWrapper;
        }else if (CommonDef.CONN_TYPE_ASS_SUBSCRIBER == connType){
            return assSubPcWrapper;
        }
        KLog.p(KLog.ERROR, "no peerconnection to conntype %s", connType);
        return null;
    }



    /**
     * 会议事件监听器
     * */
    public interface ConfEventListener {

        /**
         * 会议服务器连接断开
         * */
        void onConfServerDisconnected();

        /**
         * 会议邀请
         */
        void onConfInvitation(ConfInvitationInfo confInvitationInfo);

        /**
         * 会议结束
         */
        void onConfFinished();
    }
    private ConfEventListener confEventListener;

    /**
     * 会话监听器
     * */
    public interface SessionEventListener {
        /**
         * 与会方入会了。
         * 一般情形下，用户收到该回调时调用{@link #createDisplay()}创建Display，
         * 然后调用{@link Display#setConferee(Conferee)} 将Display绑定到与会方以使与会方画面展示在Display上，
         * 如果还需要展示文字图标等deco，可调用{@link Conferee#addText(TextDecoration)}}, {@link Conferee#addPic(PicDecoration)}
         * NOTE: 文字、图片等deco是属于Conferee的而非Display，所以调用{@link Display#swapContent(Display)}等方法时，deco也会跟着迁移。
         * */
        void onConfereeJoined(Conferee conferee);
        /**
         * 与会方离会
         * 如果该Display不需要了请调用{@link #releaseDisplay(Display)}销毁；
         * 如果后续要复用则可以不销毁，可以调用{@link Display#setConferee)}参数传null清空内容；
         * NOTE: {@link #stopSession()} 会销毁所有Display。用户不能跨Session复用Display，也不需要在stopSession时手动销毁Display。
         * */
        void onConfereeLeft(Conferee conferee);

        /**
         * 此会议需要输入密码。
         * 收到此通知后请调用{@link #verifyConfPassword(String, IResultListener)}传入密码验证
         * */
        void confPasswordNeeded();
    }
    private SessionEventListener sessionEventListener;



    /**
     * 获取与会方集合
     * NOTE：该集合不可修改
     * */
    public Set<Conferee> getConferees(){
        return Collections.unmodifiableSet(conferees);
    }


    /**
     * 与会方
     */
    public static final class Conferee implements VideoSink {
        public String id;
        public int mcuId;
        public int terId;
        public String   e164;
        public String   alias;
        public String   email;

        // 是否为辅流与会方（针对辅流我们创建了一个虚拟的“辅流与会方”与之对应）
        public boolean bAssStream;

        // 该与会方的视频流。一个与会方只有一路视频流
        private VideoStream videoStream;
        // 该与会方的音频流。一个与会方可以有多路音频流
        private Set<AudioStream> audioStreams = new HashSet<>();

        // 是否在等待与之相关的码流，与会人员入会的消息上来后
        // 可能还要等一会才上来码流消息，该字段标记是否此种场景。
        private boolean bWaitingVideoStream;

        // 该与会方绑定的Display。与会方内容将展示在Display上。
        // 一个与会方可以绑定多个Display，一个Display只能绑定一个与会方。
        private Set<Display> displays = Collections.newSetFromMap(new ConcurrentHashMap<>());

        // 文字图片装饰。如台标，静态图片等。
        // 与会方的画面内容由码流和装饰组成，展示在Display上。
        private Set<TextDecoration> textDecorations = new HashSet<>();
        private Set<PicDecoration> picDecorations = new HashSet<>();

        // 视频码流状态
        private int state = VideoState_Normal;
        private int preState = state; // 上一个状态
        private static final int VideoState_Normal = 0;
        private static final int VideoState_Disabled = 1; // 视频源被屏蔽，如摄像头关闭
        private static final int VideoState_WeakSignal = 2; // 视频信号弱（视频通道在但几乎没有视频帧）
        private static final int VideoState_NoStream = 3; // 没有视频通道，如音频入会
        private static final int VideoState_RecvingAss = 4; // 正在接收辅流

        // 语音激励使能
        private boolean bVoiceActivated;

        private long timestamp = System.currentTimeMillis();

        Conferee(int mcuId, int terId, String e164, String alias, String email, boolean bAssStream) {
            this.mcuId = mcuId;
            this.terId = terId;
            this.id = buildId(mcuId, terId, bAssStream);
            this.e164 = e164;
            this.alias = alias;
            this.email = email;
            this.bAssStream = bAssStream;
        }

        static String buildId(int mcuId, int terId, boolean bAssStream){
            String postfix = bAssStream ? "_assStream" : "";
            return mcuId+"_"+terId+postfix;
        }

        void addDisplay(Display display) {
            if (null != display) {
                KLog.p("add Display %s to conferee %s", display, id);
                displays.add(display);
            }
        }

        boolean removeDisplay(Display display) {
            KLog.p("delete Display %s from conferee %s", display, id);
            boolean success = displays.remove(display);
            if (success) {
//                onFrameHandler.post(new Runnable() {
//                    @Override
//                    public void run() {
//                        display.onFrame(new VideoFrame()); // TODO 构造最后一帧空帧避免画面残留
//                    }
//                });
            }
            return success;
        }

        void addAudioStream(AudioStream audioStream){
            if (null != audioStream){
                audioStreams.add(audioStream);
            }
        }

        void setVideoStream(VideoStream videoStream){
            this.videoStream = videoStream;
        }

        boolean removeStream(String kdStreamId){
            boolean success = false;
            if (null != videoStream && videoStream.streamId.equals(kdStreamId)){
                videoStream = null;
                success = true;
            }else {
                for (AudioStream audioStream : audioStreams) {
                    if (audioStream.streamId.equals(kdStreamId)) {
                        success = true;
                        break;
                    }
                }
            }

            return success;
        }



        /**
         * 添加文字Deco
         * */
        public void addText(TextDecoration decoration){
            KLog.p(decoration.toString());
            textDecorations.add(decoration);
            refreshDisplays();
        }

        /**
         * 添加图片deco
         * */
        public void addPic(PicDecoration decoration){
            KLog.p(decoration.toString());
            picDecorations.add(decoration);
            refreshDisplays();
        }

        /**
         * 移除deco。
         * NOTE: 移除deco后若要再展示该deco需要重新添加该deco。如果只是屏蔽展示稍后还要开启请使用{@link #setDecoEnable(String, boolean)}
         * */
        public boolean removeDeco(String decoId){
            if (null == decoId){
                return false;
            }
            boolean success = false;
            for (TextDecoration deco : textDecorations){
                if (deco.id.equals(decoId)){
                    success = textDecorations.remove(deco);
                    break;
                }
            }
            for (PicDecoration deco : picDecorations){
                if (deco.id.equals(decoId)){
                    success = picDecorations.remove(deco);
                    break;
                }
            }

            if (success) {
                refreshDisplays();
            }

            return success;
        }


        /**
         * 设置是否启用某个deco
         * 默认是启用。
         * @param decoId {@link Decoration#id}
         * @param enabled true，启用；false，禁用
         * NOTE: 不同于{@link Display#setDecoEnable(String, boolean)}，该方法会影响到所有绑定到该Conferee的Display。
         * */
        public void setDecoEnable(String decoId, boolean enabled){
            boolean success = false;
            for (TextDecoration deco : textDecorations){
                if (deco.id.equals(decoId) && deco.enabled() != enabled){
                    deco.setEnabled(enabled);
                    success = true;
                    break;
                }
            }
            for (PicDecoration deco : picDecorations){
                if (deco.id.equals(decoId) && deco.enabled() != enabled){
                    deco.setEnabled(enabled);
                    success = true;
                    break;
                }
            }

            if (success) {
                refreshDisplays();
            }

        }


        /**
         * 语音激励状态下的装饰
         * */
        private RectF voiceActivatedDeco = new RectF();
        private static Paint voiceActivatedDecoPaint = new Paint();
        static{
            voiceActivatedDecoPaint.setStyle(Paint.Style.STROKE);
            voiceActivatedDecoPaint.setStrokeWidth(5);
            voiceActivatedDecoPaint.setColor(Color.GREEN);
        }

        /**
         * 摄像头关闭状态下的装饰
         * */
        private static StreamStateDecoration cameraDisabledDeco;
        /**
         * 视频信号弱状态下的装饰
         * */
        private static StreamStateDecoration weakVideoSignalDeco;
        /**
         * 纯音频与会方的装饰
         * */
        private static StreamStateDecoration audioConfereeDeco;
        /**
         * 正在接收辅流的装饰
         * */
        private static StreamStateDecoration recvingAssStreamDeco;


        /**
         * 设置语音激励装饰。
         * 当某个与会方讲话音量最大时，该装饰会展示在该与会方对应的Display。
         * 该装饰是一个围在画面周围的边框，用户通过该接口设置该边框线条的粗细以及颜色。
         * @param strokeWidth 线条粗细
         * @param color 线条颜色
         * */
        public static void setVoiceActivatedDecoration(int strokeWidth, int color){
            voiceActivatedDecoPaint.setStrokeWidth(strokeWidth);
            voiceActivatedDecoPaint.setColor(color);
        }


        /**
         * 设置关闭摄像头采集时己端与会方展示的图片。
         * @param winW UCD标注的deco所在窗口的宽
         * @param winH UCD标注的deco所在窗口的高
         * @param backgroundColor 背景色
         * */
        public static void setCameraDisabledDecoration(@NonNull Bitmap bitmap, int winW, int winH, int backgroundColor){
            cameraDisabledDeco = StreamStateDecoration.createFromPicDecoration(
                    PicDecoration.createCenterPicDeco("cameraDisabledDeco", bitmap, winW, winH),
                    backgroundColor
            );
        }

        /**
         * 设置音频入会方展示的图片。
         * @param winW UCD标注的deco所在窗口的宽
         * @param winH UCD标注的deco所在窗口的高
         * @param backgroundColor 背景色
         * */
        public static void setAudioConfereeDecoration(@NonNull Bitmap bitmap, int winW, int winH, int backgroundColor){
            audioConfereeDeco = StreamStateDecoration.createFromPicDecoration(
                    PicDecoration.createCenterPicDeco("audioConfereeDeco", bitmap, winW, winH),
                    backgroundColor
            );
        }

        /**
         * 设置视频信号丢失的与会方展示的图片
         * @param winW UCD标注的deco所在窗口的宽
         * @param winH UCD标注的deco所在窗口的高
         * @param backgroundColor 背景色
         * */
        public static void setWeakVideoSignalDecoration(@NonNull Bitmap bitmap, int winW, int winH, int backgroundColor){
            weakVideoSignalDeco = StreamStateDecoration.createFromPicDecoration(
                    PicDecoration.createCenterPicDeco("weakVideoSignalDeco", bitmap, winW, winH),
                    backgroundColor
            );
        }

        /**
         * 设置辅流与会方正在接收辅流时展示的图片
         * @param winW UCD标注的deco所在窗口的宽
         * @param winH UCD标注的deco所在窗口的高
         * @param backgroundColor 背景色
         * */
        public static void setRecvingAssStreamDecoration(@NonNull Bitmap bitmap, int winW, int winH, int backgroundColor){
            recvingAssStreamDeco = StreamStateDecoration.createFromPicDecoration(
                    PicDecoration.createCenterPicDeco("recvingAssStreamDeco", bitmap, winW, winH),
                    backgroundColor
            );
        }


        /**
         * 设置码流状态
         * */
        void setState(int state){
            if (this.state != state) {
                preState = this.state;
                this.state = state;
                refreshDisplays();
            }
        }

        /**
         * 设置语音激励状态
         * */
        void setVoiceActivated(boolean bVoiceActivated){
            if (this.bVoiceActivated != bVoiceActivated){
                this.bVoiceActivated = bVoiceActivated;
                refreshDisplays();
            }
        }

        private void refreshDisplays(){
            for (Display display : displays){
                display.refresh();
            }
        }


        @Override
        public void onFrame(VideoFrame videoFrame) {
            long curts = System.currentTimeMillis();
            long ts = timestamp;
            if (curts - ts > 5000){
                timestamp = curts;
                KLog.p("%s onFrame ", id);
            }

            for (Display display : displays){
                if (curts - ts > 5000) {
                    if (display.enabled) {
                        KLog.p("frame of conferee %s rendered onto display %s ", id, display.hashCode());
                    }else{
                        KLog.p("frame of conferee %s dropped off display %s because it is disabled ", id, display.hashCode());
                    }
                }
                if (!display.enabled) {
                    continue;
                }
                display.onFrame(videoFrame);
            }
        }

    }



    /**
     * 用于展示与会方的控件。
     * */
    public final static class Display extends SurfaceViewRenderer{
        /**
         * 是否使能。
         * 若使能则正常显示内容，否则内容不显示。
         * 使能状态是固属于Display的，不会随着如下方法的执行而迁移：
         * {@link #copyContentFrom(Display)}
         * {@link #moveContentTo(Display)}
         * {@link #swapContent(Display)}
         * */
        private boolean enabled = true;

        /**
         * Display对应的与会方。
         * Display用于展示与会方相关内容（码流、台标、状态图标等）
         * 一个Display只会绑定到一个与会方；
         * 多个Display可以绑定到同一与会方；
         * */
        private Conferee conferee;

        /**
         * 禁止显示的deco集合
         * */
        private Set<String> disabledDecos = new HashSet<>();


        private Handler handler = getHandler();


        private Display(Context context) {
            super(context);
            init(instance.eglBase.getEglBaseContext(), null);
            setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
            setEnableHardwareScaler(true);
            setWillNotDraw(false);
        }


        public boolean isEnable() {
            return enabled;
        }

        /**
         * 设置是否使能该Display
         * @param enable false 禁用该Display，屏蔽内容展示；true正常展示内容。默认true
         * */
        public void setEnable(boolean enable){
            KLog.p("set enable=%s for display %s", enable, this);
            this.enabled = enable;
            refresh();
        }

        /**
         * 设置是否在所有Display最前端展示。可用于多个Display层叠的场景
         * */
        public void setOnTopOverOtherDisplays(boolean bOnTop){
            setZOrderMediaOverlay(bOnTop);
        }

        /**
         * 设置Display绑定的与会方。
         * 一个Display只会绑定到一个与会方；
         * 多个Display可以绑定到同一与会方；
         * 若一个Display已经绑定到某个与会方，则该绑定会先被解除，然后建立新的绑定关系；
         *
         * 对于内容交换的使用场景建议使用便捷方法{@link #swapContent(Display)}；
         * 对于内容拷贝的使用场景建议使用便捷方法{@link #copyContentFrom(Display)}；
         * 对于内容移动的使用场景建议使用便捷方法{@link #moveContentTo(Display)}；
         * @param conferee 与会方。若为null则display不展示任何内容。
         *                 NOTE: 若用户设置了一个不存在于会议中的Conferee，接口不会报错，但Display不会展示出任何内容，形同设置了null；
         * */
        public void setConferee(@Nullable Conferee conferee){
            KLog.p("set content %s for display %s", null != conferee ? conferee.id : null, this.hashCode());
            if (null != this.conferee){
                this.conferee.removeDisplay(this);
            }
            if (null != conferee) {
                conferee.addDisplay(this);
            }
            this.conferee = conferee;

            refresh();
        }

        /**
         * 获取Display内容
         * @return  返回setContent设置的Conferee
         * */
        public Conferee getConferee(){
            return conferee;
        }

        /**
         * 设置是否展示某个deco
         * 默认是展示。
         * @param decoId {@link Decoration#id}
         * @param enabled true，展示；false，屏蔽
         * */
        public void setDecoEnable(String decoId, boolean enabled){
            boolean bSuccess;
            if (enabled){
                bSuccess = disabledDecos.remove(decoId);
            }else{
                bSuccess = disabledDecos.add(decoId);
            }
            if (bSuccess) {
                refresh();
            }
        }


        /**
         * 拷贝源Display的内容到本Display，覆盖原有内容。
         * @param src 源display。
         * */
        public void copyContentFrom(@NonNull Display src){
            KLog.p("copy content from display %s to display %s", src.hashCode(), this.hashCode());
            if (null != this.conferee){
                this.conferee.removeDisplay(this);
            }
            disabledDecos.clear();

            if (null != src.conferee) {
                src.conferee.addDisplay(this);
            }
            conferee = src.conferee;
            disabledDecos.addAll(src.disabledDecos);
            enabled = src.enabled;

            refresh();
        }

        /**
         * 将本Display的内容移动到目标Display，目标原有内容将被覆盖，本Display的内容将被清空。
         * @param dst 目标display。
         * */
        public void moveContentTo(@NonNull Display dst){
            KLog.p("move content from display %s to display %s", this.hashCode(), dst.hashCode());
            dst.setConferee(conferee);
            dst.disabledDecos.clear();
            dst.disabledDecos.addAll(disabledDecos);
            dst.enabled = enabled;

            if (null != conferee){
                conferee.removeDisplay(this);
            }
            conferee = null;
            disabledDecos.clear();
            enabled = true;

            refresh();
        }


        /**
         * 交换两个display的内容。
         * @param otherDisplay 要交换的display。
         * */
        public void swapContent(@NonNull Display otherDisplay){
            KLog.p("swap display %s with display %s", this.hashCode(), otherDisplay.hashCode());
            Conferee myConferee = conferee;
            setConferee(otherDisplay.conferee);
            otherDisplay.setConferee(myConferee);

            Set<String> myDisabledDecos = new HashSet<>(disabledDecos);
            disabledDecos.clear();
            disabledDecos.addAll(otherDisplay.disabledDecos);
            otherDisplay.disabledDecos.clear();
            otherDisplay.disabledDecos.addAll(myDisabledDecos);

            boolean myEnabled = enabled;
            enabled = otherDisplay.enabled;
            otherDisplay.enabled = myEnabled;
        }

        /**
         * 清空Display。
         * Conferee以及所有状态均被清空。
         * */
        public void clear(){
            setConferee(null);
            disabledDecos.clear();
            enabled = true;
        }

        /**
         * 销毁display
         * */
        private void destroy(){
            KLog.p("destroy display %s ", this.hashCode());
            if (null != conferee){
                conferee.removeDisplay(this);
                conferee = null;
            }
            super.release();
        }


        private void refresh(){
            invalidate();
        }



        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.surfaceChanged(holder, format, width, height);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            if (!enabled || null == conferee){
                KLog.p("enabled=%s, conferee=%s", enabled, conferee);
                return;
            }

            int displayWidth = getWidth();
            int displayHeight = getHeight();

            KLog.p("onDraw, displayWidth=%s, displayHeight=%s", displayWidth, displayHeight);

            // 绘制码流状态deco
            StreamStateDecoration stateDeco = null;
            if (Conferee.VideoState_Disabled == conferee.state){
                stateDeco = Conferee.cameraDisabledDeco;
            }else if (Conferee.VideoState_NoStream == conferee.state){
                stateDeco = Conferee.audioConfereeDeco;
            }else if (Conferee.VideoState_WeakSignal == conferee.state){
                stateDeco = Conferee.weakVideoSignalDeco;
            }else if (Conferee.VideoState_RecvingAss == conferee.state){
                stateDeco = Conferee.recvingAssStreamDeco;
            }
            if (null != stateDeco && stateDeco.enabled() && !disabledDecos.contains(stateDeco.id)) {
                stateDeco.adjust(displayWidth, displayHeight);
                canvas.drawColor(stateDeco.backgroundColor);
                canvas.drawBitmap(stateDeco.pic, stateDeco.matrix, stateDeco.paint);
            }else{
                canvas.drawColor(Color.BLACK);
            }

            // 绘制图片deco
            for (PicDecoration deco : conferee.picDecorations){
                if (!deco.enabled() || disabledDecos.contains(deco.id)){
                    continue;
                }
                deco.adjust(displayWidth, displayHeight);
                canvas.drawBitmap(deco.pic, deco.matrix, deco.paint);
            }

            // 绘制文字deco
            for (TextDecoration deco : conferee.textDecorations){
                if (!deco.enabled() || disabledDecos.contains(deco.id)){
                    continue;
                }
                deco.adjust(displayWidth, displayHeight);
                canvas.drawText(deco.text, deco.x, deco.y, deco.paint);
            }

            // 绘制语音激励deco
            if (conferee.bVoiceActivated){
                conferee.voiceActivatedDeco.set(0, 0, displayWidth, displayHeight);
                canvas.drawRect(conferee.voiceActivatedDeco, Conferee.voiceActivatedDecoPaint);
            }

        }


    }



    /**
     * 创建Display。
     * */
    public Display createDisplay(){
        Display display =  new Display(context);
        displaySet.add(display);
        KLog.p("create display %s", display.hashCode());
        return display;
    }

    /**
     * 销毁display
     * */
    public void releaseDisplay(Display display){
        KLog.p("release display %s", display.hashCode());
        if (displaySet.remove(display)){
            display.destroy();
        }else{
            KLog.p(KLog.ERROR, "wired, display %s is not created by me!", display.hashCode());
        }
    }

    /**
     * 获取Display集合
     * NOTE：该集合不可修改
     * */
    public Set<Display> getDisplays(){
        return Collections.unmodifiableSet(displaySet);
    }


    public static class TextDecoration extends Decoration{
        public String text;     // 要展示的文字
        private int textSize;
        private static final int minTextSizeLimit = 20;
        protected Matrix matrix = new Matrix();
        /**
         * @param id deco的id，用户自定义，方便用户找回。
         * @param text deco文字内容
         * @param textSize 文字大小
         * @param color 文字颜色
         * @param w UCD标注的deco所在窗口的宽
         * @param h UCD标注的deco所在窗口的高
         * @param dx UCD标注的deco相对于refPos与窗口的x方向边距
         * @param dy UCD标注的deco相对于refPos与窗口的y方向边距
         * @param refPos 计算dx,dy的相对位置。取值{@link #POS_LEFTTOP},{@link #POS_LEFTBOTTOM},{@link #POS_RIGHTTOP},{@link #POS_RIGHTBOTTOM}
         * 示例：
         * 有UCD的标注图如下：
         * Window 1920*1080
         * -----------------------
         * |
         * |-dx=80px-|caption| textSize=20px, color=0xFF00FF00
         * |           |
         * |         100px
         * |          |
         * |-------------------
         *  则调用该接口时各参数传入情况：
         *  new TextDecoration("decoId", "caption", 20, 0xFF00FF00, 1920, 1080, 80, 100, POS_LEFTBOTTOM);
         * */
        public TextDecoration(@NonNull String id, @NonNull String text, int textSize, int color, int w, int h, int dx, int dy, int refPos) {
            super(id, w, h, dx, dy, refPos);
            this.text = text;
            this.textSize = textSize;
            paint.setStyle(Paint.Style.FILL);
            paint.setAntiAlias(true);
            paint.setColor(color);
        }

        /**
         * 根据实际窗口的大小调整deco的位置及大小
         * @param width 实际窗口的宽
         * @param height 实际窗口的高
         * @return */
        protected boolean adjust(int width, int height){
            if (!super.adjust(width, height)){
                return false;
            }
            float size = textSize;
            float scaleCenterX=0, scaleCenterY=0;
            if (POS_LEFTTOP == refPos){
                y += size; // 此处需对y坐标添加字符大小的修正。不同于drawBitmap，drawText默认是Baseline对齐的，也就是y坐标以字符底部为基准
            }else if (POS_LEFTBOTTOM == refPos){
//                y -= size; //不需要对y坐标做如此修正。 不同于drawBitmap，drawText默认是Baseline对齐的，也就是y坐标以字符底部为基准
                scaleCenterY = height;
            }else if (POS_RIGHTTOP == refPos){
                x -= size * text.length();
                y += size;
                scaleCenterX = width;
            }else {
                x -= size * text.length();
                scaleCenterX = width;
                scaleCenterY = height;
            }
            float cor[] = new float[2];
            matrix.reset();
            matrix.postTranslate(x, y);
            float scaleFactor = Math.min(ratioW, ratioH);
            matrix.postScale(scaleFactor, scaleFactor, scaleCenterX, scaleCenterY);
            matrix.mapPoints(cor);
            x = cor[0]; y = cor[1];
            size = textSize*scaleFactor;
            size = size>minTextSizeLimit ? size : minTextSizeLimit;
            paint.setTextSize(size);
            KLog.p(toString());
            return true;
        }

        @Override
        public String toString() {
            return "TextDecoration{" +
                    "text='" + text + '\'' +
                    ", textSize=" + textSize +
                    ", id='" + id + '\'' +
                    ", w=" + w +
                    ", h=" + h +
                    ", dx=" + dx +
                    ", dy=" + dy +
                    ", refPos=" + refPos +
                    ", paint=" + paint +
                    ", x=" + x +
                    ", y=" + y +
                    ", ratioW=" + ratioW +
                    ", ratioH=" + ratioH +
                    '}';
        }
    }

    public static class PicDecoration extends Decoration{
        public Bitmap pic;     // 要展示的图片
        protected Matrix matrix = new Matrix();
        public PicDecoration(@NonNull String id, @NonNull Bitmap pic, int w, int h, int dx, int dy, int refPos) {
            super(id, w, h, dx, dy, refPos);
            this.pic = pic;
            paint.setStyle(Paint.Style.STROKE);
        }

        protected boolean adjust(int width, int height){
            if (!super.adjust(width, height)){
                return false;
            }
            float picW = pic.getWidth();
            float picH = pic.getHeight();
            float scaleCenterX=0, scaleCenterY=0;
            if (POS_LEFTBOTTOM == refPos){
                y -= picH;
                scaleCenterY = height;
            }else if (POS_RIGHTTOP == refPos){
                x -= picW;
                scaleCenterX = width;
            }else if (POS_RIGHTBOTTOM == refPos){
                x -= picW;
                y -= picH;
                scaleCenterX = width;
                scaleCenterY = height;
            }
            matrix.reset();
            matrix.postTranslate(x, y);
            matrix.postScale(ratioW, ratioH, scaleCenterX, scaleCenterY);
            KLog.p(toString());
            return true;
        }

        static PicDecoration createCenterPicDeco(String id, Bitmap bitmap, int winW, int winH){
            int bmW = bitmap.getWidth();
            int bmH = bitmap.getHeight();
            winW = winW > bmW ? winW : bmW;
            winH = winH > bmH ? winH : bmH;
            return new PicDecoration(id, bitmap, winW, winH, (int)((winW-bmW)/2f), (int)((winH-bmH)/2f), Decoration.POS_LEFTTOP);
        }

        @Override
        public String toString() {
            return "PicDecoration{" +
                    "pic=" + pic +
                    ", matrix=" + matrix +
                    ", id='" + id + '\'' +
                    ", w=" + w +
                    ", h=" + h +
                    ", dx=" + dx +
                    ", dy=" + dy +
                    ", refPos=" + refPos +
                    ", paint=" + paint +
                    ", x=" + x +
                    ", y=" + y +
                    ", ratioW=" + ratioW +
                    ", ratioH=" + ratioH +
                    '}';
        }
    }

    private static class StreamStateDecoration extends PicDecoration{
        int backgroundColor;
        StreamStateDecoration(@NonNull String id, @NonNull Bitmap pic, int w, int h, int dx, int dy, int refPos, int backgroundColor) {
            super(id, pic, w, h, dx, dy, refPos);
            this.backgroundColor = backgroundColor;
        }

        static StreamStateDecoration createFromPicDecoration(@NonNull PicDecoration picDeco, int backgroundColor){
            return new StreamStateDecoration(picDeco.id, picDeco.pic, picDeco.w, picDeco.h, picDeco.dx, picDeco.dy, picDeco.refPos, backgroundColor);
        }
    }

    public static class Decoration{
        public String id;
        private boolean enabled = true; // 是否使能。使能则展示否则不展示
        public int w;           // 该deco所在画面的宽（UCD标注的）
        public int h;           // 该deco所在画面的高（UCD标注的）
        public int dx;          // 参照pos的x方向距离（UCD标注的）
        public int dy;          // 参照pos的y方向距离（UCD标注的）
        public int refPos;      /** dx, dy参照的位置。如取值{@link #POS_LEFTBOTTOM}则dx表示距离左下角的x方向距离*/
        protected Paint paint;

        protected float x;          // 锚点x坐标（根据UCD标注结合Display的实际尺寸计算得出）
        protected float y;          // 锚点y坐标（根据UCD标注结合Display的实际尺寸计算得出）
        protected float ratioW;
        protected float ratioH;
        protected float preAdjustedWidth;
        protected float preAdjustedHeight;

        public static final int POS_LEFTTOP = 1;
        public static final int POS_LEFTBOTTOM = 2;
        public static final int POS_RIGHTTOP = 3;
        public static final int POS_RIGHTBOTTOM = 4;

        protected Decoration(String id, int w, int h, int dx, int dy, int refPos) {
            this.id = id;
            this.w = w;
            this.h = h;
            this.dx = dx;
            this.dy = dy;
            this.refPos = refPos;
            this.paint = new Paint();
        }

        protected void setEnabled(boolean enabled){
            this.enabled = enabled;
        }

        protected boolean enabled(){
            return enabled;
        }

        protected boolean adjust(int width, int height){
            if (width<=0 || height<=0){
                return false;
            }
            if (preAdjustedWidth == width && preAdjustedHeight == height){
                return false;
            }
            preAdjustedWidth = width;
            preAdjustedHeight = height;

            ratioW = width/(float)w;
            ratioH = height/(float)h;

            if (POS_LEFTTOP == refPos){
                x = dx;
                y = dy;
            }else if (POS_LEFTBOTTOM == refPos){
                x = dx;
                y = height - dy;
            }else if (POS_RIGHTTOP == refPos){
                x = width - dx;
                y = dy;
            }else{
                x = width - dx;
                y = height - dy;
            }

            KLog.p("displayW=%s, displayH=%s, ratioW=%s, ratioH=%s, x=%s, y=%s, paint.textSize=%s",
                    width, height, ratioW, ratioH, x, y, paint.getTextSize());

            return true;
        }

    }




    @SuppressWarnings("WeakerAccess")
    public static class Stream {
        public String streamId; // 平台的StreamId，概念上对应的是WebRTC中的TrackId

        public Stream(String streamId) {
            this.streamId = streamId;
        }
    }

    public static class AudioStream extends Stream{
        public AudioStream(String streamId) {
            super(streamId);
        }
    }

    public static class VideoStream extends Stream {
        public boolean bAss;
        public List<EmMtResolution> supportedResolutionList;       // 流支持的分辨率。仅视频有效

        public VideoStream(String streamId, boolean bAss, List<EmMtResolution> supportedResolutionList) {
            super(streamId);
            this.bAss = bAss;
            this.supportedResolutionList = supportedResolutionList;
        }
    }

    private VideoStream findVideoStream(String kdStreamId){
        for (Conferee conferee1 : conferees){
            if (null != conferee1.videoStream && conferee1.videoStream.streamId.equals(kdStreamId)){
                return conferee1.videoStream;
            }
        }
        return null;
    }

    private AudioStream findAudioStream(String kdStreamId){
        for (Conferee conferee1 : conferees){
            for (AudioStream audioStream : conferee1.audioStreams){
                if (audioStream.streamId.equals(kdStreamId)){
                    return audioStream;
                }
            }
        }
        return null;
    }

    private Set<VideoStream> getAllVideoStreams(){
        Set<VideoStream> videoStreams = new HashSet<>();
        for (Conferee conferee1 : conferees){
            if (null != conferee1.videoStream) videoStreams.add(conferee1.videoStream);
        }
        return videoStreams;
    }

    private Conferee findConfereeByConfereeId(String confereeId){
        for (Conferee conferee : conferees){
            if (conferee.id.equals(confereeId)){
                return conferee;
            }
        }
        return null;
    }

    private Conferee findConfereeByStreamId(String kdStreamId){
        for (Conferee conferee1 : conferees){
            if (null != conferee1.videoStream && conferee1.videoStream.streamId.equals(kdStreamId)){
                return conferee1;
            }
            for (AudioStream audioStream : conferee1.audioStreams){
                if (audioStream.streamId.equals(kdStreamId)){
                    return conferee1;
                }
            }
        }
        return null;
    }


    private Conferee findMyself(){
        for (Conferee conferee : conferees){
            if (null != conferee.e164 && conferee.e164.equals(userE164)){
                return conferee;
            }
        }
        return null;
    }








    private class PeerConnectionFactoryConfig{
        private List<String> videoCodecList;
        private boolean enableVideoCodecHwAcceleration;
        PeerConnectionFactoryConfig(@NonNull List<String> videoCodecList, boolean enableVideoCodecHwAcceleration) {
            this.videoCodecList = videoCodecList;
            this.enableVideoCodecHwAcceleration = enableVideoCodecHwAcceleration;
        }

        @Override
        public String toString() {
            return "PeerConnectionFactoryConfig{" +
                    "videoCodecList=" + videoCodecList +
                    ", enableVideoCodecHwAcceleration=" + enableVideoCodecHwAcceleration +
                    '}';
        }
    }

    private class PeerConnectionConfig{
        int videoWidth;
        int videoHeight;
        int videoFps;
        int videoMaxBitrate;
        String videoCodec;
        int audioStartBitrate;
        String audioCodec;

        PeerConnectionConfig(int videoWidth,
                            int videoHeight,
                            int videoFps,
                            int videoMaxBitrate,
                            String videoCodec,
                            int audioStartBitrate,
                            String audioCodec) {
            this.videoWidth = videoWidth;
            this.videoHeight = videoHeight;
            this.videoFps = videoFps;
            this.videoMaxBitrate = videoMaxBitrate;
            this.videoCodec = videoCodec;
            this.audioStartBitrate = audioStartBitrate;
            this.audioCodec = audioCodec;
        }

        PeerConnectionConfig(PeerConnectionConfig config) {
            this.videoWidth = config.videoWidth;
            this.videoHeight = config.videoHeight;
            this.videoFps = config.videoFps;
            this.videoMaxBitrate = config.videoMaxBitrate;
            this.videoCodec = config.videoCodec;
            this.audioStartBitrate = config.audioStartBitrate;
            this.audioCodec = config.audioCodec;
        }

        @Override
        public String toString() {
            return "PeerConnectionConfig{" +
                    "videoWidth=" + videoWidth +
                    ", videoHeight=" + videoHeight +
                    ", videoFps=" + videoFps +
                    ", videoMaxBitrate=" + videoMaxBitrate +
                    ", videoCodec='" + videoCodec + '\'' +
                    ", audioStartBitrate=" + audioStartBitrate +
                    ", audioCodec='" + audioCodec + '\'' +
                    '}';
        }
    }



    private class RtcConnectorEventListener implements RtcConnector.SignalingEvents{

        @Override
        public void onGetOfferCmd(int connType, int mediaType) {
            KLog.p("onGetOfferCmd: connType=%s, mediaType=%s", connType, mediaType);
            PeerConnectionWrapper pcWrapper = getPcWrapper(connType);
            if (null == pcWrapper || !pcWrapper.checkSdpState(pcWrapper.Idle)) return;

            pcWrapper.curMediaType = mediaType;
            VideoCapturer videoCapturer = null;
            if ((CommonDef.MEDIA_TYPE_VIDEO == mediaType
                    || CommonDef.MEDIA_TYPE_ASS_VIDEO == mediaType)){
                if (CommonDef.CONN_TYPE_PUBLISHER == connType) {
                    videoCapturer = createCameraCapturer(new Camera2Enumerator(context));
                }else if (CommonDef.CONN_TYPE_ASS_PUBLISHER == connType) {
//                        videoCapturer = createScreenCapturer();
                    videoCapturer = createWindowCapturer();
                }
            }
            if (null != videoCapturer) {
                pcWrapper.createVideoTrack(videoCapturer);
            }

            if ((CommonDef.MEDIA_TYPE_AUDIO == mediaType
                    || CommonDef.MEDIA_TYPE_AV == mediaType)) {
                pcWrapper.createAudioTrack();
            }

            pcWrapper.createOffer();

            if (CommonDef.MEDIA_TYPE_AV == mediaType){
                // 针对多路码流的情形，我们需要一路一路地发布（平台的限制）
                // 我们先发Audio，等到收到setAnswerCmd后再发Video
                pcWrapper.setSdpType(pcWrapper.AudioOffer);
            }else{
                pcWrapper.setSdpType(pcWrapper.Offer);
            }

            pcWrapper.setSdpState(pcWrapper.Creating);

        }

        @Override
        public void onSetOfferCmd(int connType, String offerSdp, List<RtcConnector.TRtcMedia> rtcMediaList) {
            KLog.p("connType=%s", connType);
            for (RtcConnector.TRtcMedia rtcMedia : rtcMediaList) {
                KLog.p("mid=%s, kdstreamId=%s", rtcMedia.mid, rtcMedia.streamid);
                mid2KdStreamIdMap.put(rtcMedia.mid, rtcMedia.streamid);
            }

            PeerConnectionWrapper pcWrapper = getPcWrapper(connType);
            if (null == pcWrapper || !pcWrapper.checkSdpState(pcWrapper.Idle)) return;

            pcWrapper.setSdpType(pcWrapper.Answer);
            setRemoteDescription(pcWrapper, offerSdp, SessionDescription.Type.OFFER);
            pcWrapper.setSdpState(pcWrapper.SettingRemote);
        }

        @Override
        public void onSetAnswerCmd(int connType, String answerSdp) {
            KLog.p("connType=%s", connType);
            PeerConnectionWrapper pcWrapper = getPcWrapper(connType);
            if (null == pcWrapper || !pcWrapper.checkSdpState(pcWrapper.Sending)) return;

            setRemoteDescription(pcWrapper, answerSdp, SessionDescription.Type.ANSWER);
            pcWrapper.setSdpState(pcWrapper.SettingRemote);
        }

        @Override
        public void onSetIceCandidateCmd(int connType, String sdpMid, int sdpMLineIndex, String sdp) {
            KLog.p("connType=%s, sdpMid=%s, sdpMLineIndex=%s", connType, sdpMid, sdpMLineIndex);
            PeerConnectionWrapper pcWrapper = getPcWrapper(connType);
            if (null == pcWrapper) return;

            pcWrapper.addCandidate(new IceCandidate(sdpMid, sdpMLineIndex, sdp));
        }

        @Override
        public void onGetFingerPrintCmd(int connType) {
            KLog.p("connType=%s", connType);
            PeerConnectionWrapper pcWrapper = getPcWrapper(connType);
            if (null == pcWrapper || !pcWrapper.checkSdpState(pcWrapper.Idle)) return;
;
            pcWrapper.setSdpType(pcWrapper.FingerPrintOffer);
            pcWrapper.createAudioTrack();
            pcWrapper.createOffer();
            pcWrapper.setSdpState(pcWrapper.Creating);
        }

        @Override
        public void onCodecQuietCmd(boolean bQuiet) {
            PeerConnectionWrapper pcWrapper = getPcWrapper(CommonDef.CONN_TYPE_SUBSCRIBER);
            if (null != pcWrapper) {
                pcWrapper.setRemoteAudioEnable(!bQuiet);
            }
            userConfig.setIsRemoteAudioEnabled(!bQuiet);
        }

        @Override
        public void onCodecMuteCmd(boolean bMute) {
            PeerConnectionWrapper pcWrapper = getPcWrapper(CommonDef.CONN_TYPE_PUBLISHER);
            if (null != pcWrapper) {
                pcWrapper.setLocalAudioEnable(!bMute);
            }
            userConfig.setIsLocalAudioEnabled(!bMute);
        }

        @Override
        public void onUnPubCmd(int connType, int mediaType) {
            PeerConnectionWrapper pcWrapper = getPcWrapper(connType);
            if (null == pcWrapper) return;

            pcWrapper.isUnpublishing = true;

            // 删除取消发布的流。
            // 取消发布己端不会收到StreamLeft消息（其他与会方会收到），
            // 所以我们需在此删除流而非依赖收到StreamLeft后的处理逻辑
            if (CommonDef.MEDIA_TYPE_AUDIO == mediaType
                    || CommonDef.MEDIA_TYPE_AV == mediaType){
                pcWrapper.removeAudioTrack();
            }
            if(CommonDef.MEDIA_TYPE_AUDIO != mediaType){
                pcWrapper.removeVideoTrack();
            }
            // 重新走发布
            pcWrapper.createOffer();
            pcWrapper.setSdpType(pcWrapper.Offer);
            pcWrapper.setSdpState(pcWrapper.Creating);
        }

        private void setRemoteDescription(PeerConnectionWrapper pcWrapper, String sdp, SessionDescription.Type type){
            // 根据音视频编码偏好修改sdp
            String sdpVideoCodecName = getSdpVideoCodecName(pcWrapper.config.videoCodec);
            if (pcWrapper.isSdpType(pcWrapper.Offer) || pcWrapper.isSdpType(pcWrapper.Answer)){
                sdp = SdpHelper.preferCodec(sdp, pcWrapper.config.audioCodec, true);
                sdp = SdpHelper.preferCodec(sdp, sdpVideoCodecName, false);
            }else if (pcWrapper.isSdpType(pcWrapper.AudioOffer)){
                sdp = SdpHelper.preferCodec(sdp, pcWrapper.config.audioCodec, true);
            }else if (pcWrapper.isSdpType(pcWrapper.VideoOffer)){
                sdp = SdpHelper.preferCodec(sdp, sdpVideoCodecName, false);
            }

            pcWrapper.setRemoteDescription(new SessionDescription(type, sdp));
        }

    }




    private class SDPObserver implements SdpObserver {
        private int connType;

        SDPObserver(int connType) {
            this.connType = connType;
        }

        @Override
        public void onCreateSuccess(final SessionDescription origSdp) {
            sessionHandler.post(() -> {
                PeerConnectionWrapper pcWrapper = getPcWrapper(connType);
                // 由于是异步，此时可能会议已经结束（如对端挂会），PeerConnectionWrapper已经销毁，所以我们此处需做非空判断
                if (null == pcWrapper || !pcWrapper.checkSdpState(pcWrapper.Creating)) return;

                KLog.p("create local sdp success: type=%s", pcWrapper.sdpType);
                if (pcWrapper.isSdpType(pcWrapper.FingerPrintOffer)){
                    // 之前创建的音频流仅用于和平台交互FingerPrint没实际用处，此处交互已完成，销毁
                    pcWrapper.destoryAudioTrack();

                    rtcConnector.sendFingerPrint(pcWrapper.connType, SdpHelper.getFingerPrint(origSdp.description));
                    pcWrapper.setSdpState(pcWrapper.Idle);
                }else {
                    // 根据音视频编码偏好修改sdp
                    String sdp = origSdp.description;
                    String sdpVideoCodecName = getSdpVideoCodecName(pcWrapper.config.videoCodec);
                    if (pcWrapper.isSdpType(pcWrapper.Offer) || pcWrapper.isSdpType(pcWrapper.Answer)){
                        sdp = SdpHelper.preferCodec(sdp, pcWrapper.config.audioCodec, true);
                        sdp = SdpHelper.preferCodec(sdp, sdpVideoCodecName, false);
                    }else if (pcWrapper.isSdpType(pcWrapper.AudioOffer)){
                        sdp = SdpHelper.preferCodec(sdp, pcWrapper.config.audioCodec, true);
                    }else if (pcWrapper.isSdpType(pcWrapper.VideoOffer)){
                        sdp = SdpHelper.preferCodec(sdp, sdpVideoCodecName, false);
                    }

                    pcWrapper.setLocalDescription(new SessionDescription(origSdp.type, sdp));
                    pcWrapper.setSdpState(pcWrapper.SettingLocal);
                }
            });

        }

        @Override
        public void onSetSuccess() {
            sessionHandler.post(() -> {
                PeerConnectionWrapper pcWrapper = getPcWrapper(connType);
                if (null == pcWrapper || !pcWrapper.checkSdpState(pcWrapper.SettingLocal, pcWrapper.SettingRemote)) return;

                PeerConnection pc = pcWrapper.pc;
                if (pcWrapper.isSdpType(pcWrapper.Offer)) {
                    if (pcWrapper.isSdpState(pcWrapper.SettingLocal)) {
                        KLog.p("setLocalDescription for Offer success, sending offer...");
                        boolean bAudio = pcWrapper.curMediaType == CommonDef.MEDIA_TYPE_AUDIO;
                        RtcConnector.TRtcMedia rtcMedia = new RtcConnector.TRtcMedia(
                                SdpHelper.getMid(pc.getLocalDescription().description, bAudio),
                                bAudio ? null : createEncodingList(true) // 仅视频需要填encodings
                        );
                        rtcConnector.sendOfferSdp(pcWrapper.connType, pc.getLocalDescription().description, rtcMedia);
                        pcWrapper.setSdpState(pcWrapper.Sending);
                    } else {
                        KLog.p("setRemoteDescription for Offer success, sdp progress FINISHED, drainCandidates");
                        pcWrapper.drainCandidates();
                        pcWrapper.setSdpState(pcWrapper.Idle);
                        if (pcWrapper.isUnpublishing) {
                            // 取消发布结束，因协议组目前实现所限我们需重建PeerConnection。解决第二次发双流失败的问题
                            pcWrapper.isUnpublishing = false;
                            recreatePeerConnection(pcWrapper.connType);
                        }
                    }
                } else if (pcWrapper.isSdpType(pcWrapper.Answer)) {
                    if (pcWrapper.isSdpState(pcWrapper.SettingRemote)) {
                        KLog.p("setRemoteDescription for Answer success, create answer...");
                        pcWrapper.createAnswer();
                        pcWrapper.setSdpState(pcWrapper.Creating);
                    } else {
                        KLog.p("setLocalDescription for Answer success, sending answer...");
                        List<RtcConnector.TRtcMedia> rtcMediaList = new ArrayList<>();
                        List<String> mids = SdpHelper.getAllMids(pc.getLocalDescription().description);
                        for (String mid : mids) {
                            KLog.p("mid=%s", mid);
                            String streamId = mid2KdStreamIdMap.get(mid);
                            if (null == streamId) {
                                KLog.p(KLog.ERROR, "no streamId for mid %s (see onSetOfferCmd)", mid);
                            }
                            rtcMediaList.add(new RtcConnector.TRtcMedia(streamId, mid)); // 仅answer需要填streamId，answer不需要填encodings
                        }
                        rtcConnector.sendAnswerSdp(pcWrapper.connType, pc.getLocalDescription().description, rtcMediaList);
                        KLog.p("answer sent, sdp progress FINISHED, drainCandidates");
                        pcWrapper.drainCandidates();
                        pcWrapper.setSdpState(pcWrapper.Idle);
                    }
                } else if (pcWrapper.isSdpType(pcWrapper.AudioOffer)) {
                    if (pcWrapper.isSdpState(pcWrapper.SettingLocal)) {
                        KLog.p("setLocalDescription for AudioOffer success, sending offer...");
                        RtcConnector.TRtcMedia rtcMedia = new RtcConnector.TRtcMedia(SdpHelper.getMid(pc.getLocalDescription().description, true));
                        rtcConnector.sendOfferSdp(pcWrapper.connType, pc.getLocalDescription().description, rtcMedia);
                        pcWrapper.setSdpState(pcWrapper.Sending);
                    } else {
                        KLog.p("setRemoteDescription for AudioOffer success, we now need create video offer...");
                        VideoCapturer videoCapturer = createCameraCapturer(new Camera2Enumerator(context));
                        if (null != videoCapturer) {
                            pcWrapper.createVideoTrack(videoCapturer);
                            pcWrapper.createOffer();
                        }
                        // 不同于正常sdp流程，此时还需要再发video的offer，所以切换sdptype为videoOffer
                        pcWrapper.setSdpType(pcWrapper.VideoOffer);
                        pcWrapper.setSdpState(pcWrapper.Creating);
                    }
                } else if (pcWrapper.isSdpType(pcWrapper.VideoOffer)) {
                    if (pcWrapper.isSdpState(pcWrapper.SettingLocal)) {
                        KLog.p("setLocalDescription for VideoOffer success, sending offer...");
                        RtcConnector.TRtcMedia rtcAudio = new RtcConnector.TRtcMedia(SdpHelper.getMid(pc.getLocalDescription().description, true));
                        RtcConnector.TRtcMedia rtcVideo = new RtcConnector.TRtcMedia(SdpHelper.getMid(pc.getLocalDescription().description, false), createEncodingList(true));
                        rtcConnector.sendOfferSdp(pcWrapper.connType, pc.getLocalDescription().description, rtcAudio, rtcVideo);
                        pcWrapper.setSdpState(pcWrapper.Sending);
                    } else {
                        KLog.p("setRemoteDescription for VideoOffer success, sdp progress FINISHED, drainCandidates");
                        pcWrapper.drainCandidates();
                        // videooffer发布完毕，整个发布结束
                        pcWrapper.setSdpState(pcWrapper.Idle);
                    }
                }

            });

        }

        @Override
        public void onCreateFailure(final String error) {
            sessionHandler.post(() -> {
                KLog.p(KLog.ERROR, "error: %s", error);
            });
        }

        @Override
        public void onSetFailure(final String error) {
            sessionHandler.post(() -> {
                KLog.p(KLog.ERROR, "error: %s", error);
            });
        }

    }



    private class PCObserver implements PeerConnection.Observer{
        private int connType;

        PCObserver(int connType) {
            this.connType = connType;
        }

        @Override
        public void onIceCandidate(final IceCandidate candidate) {
            sessionHandler.post(() -> {
                KLog.p("onIceCandidate, sending candidate...");
                PeerConnectionWrapper pcWrapper = getPcWrapper(connType);
                if (null == pcWrapper) return;

                rtcConnector.sendIceCandidate(pcWrapper.connType, candidate.sdpMid, candidate.sdpMLineIndex, candidate.sdp);
            });
        }

        @Override
        public void onIceCandidatesRemoved(final IceCandidate[] candidates) {
        }

        @Override
        public void onAddStream(MediaStream mediaStream) {
        }

        @Override
        public void onSignalingChange(PeerConnection.SignalingState newState) {
        }

        @Override
        public void onIceConnectionChange(final PeerConnection.IceConnectionState newState) {
//                sessionHandler.post(() -> {
//                    if (newState == PeerConnection.IceConnectionState.CONNECTED) {
//                    } else if (newState == PeerConnection.IceConnectionState.DISCONNECTED) {
//                    } else if (newState == PeerConnection.IceConnectionState.FAILED) {
////                        reportError("ICE connection failed.");
//                    }
//                });

        }

        @Override
        public void onConnectionChange(final PeerConnection.PeerConnectionState newState) {

//                sessionHandler.post(() -> {
//                    if (newState == PeerConnection.PeerConnectionState.CONNECTED) {
//                    } else if (newState == PeerConnection.PeerConnectionState.DISCONNECTED) {
//                    } else if (newState == PeerConnection.PeerConnectionState.FAILED) {
//                        reportError("DTLS connection failed.");
//                    }
//                });

        }

        @Override
        public void onIceGatheringChange(PeerConnection.IceGatheringState newState) {
        }

        @Override
        public void onIceConnectionReceivingChange(boolean receiving) {
        }


        @Override
        public void onRemoveStream(final MediaStream stream) {
            // 我们在onTrack回调中createRemoteVideoTrack/createRemoteAudioTrack，
            // 相应的我们原本期望在onRemoveStream（没有onRemoveTrack）中removeRemoteVideoTrack/removeRemoteAudioTrack，
            // 然而实测下来发现此回调上来时MediaStream的track列表为空，不得已我们只得在StreamLeft消息上来时
            // removeRemoteVideoTrack/removeRemoteAudioTrack，由此造成了这种不对称的现象
        }

        @Override
        public void onDataChannel(final DataChannel dc) {
        }

        @Override
        public void onRenegotiationNeeded() {
            // No need to do anything; AppRTC follows a pre-agreed-upon
            // signaling/negotiation protocol.
        }

        @Override
        public void onAddTrack(RtpReceiver rtpReceiver, MediaStream[] mediaStreams) {
        }


        @Override
        public void onTrack(RtpTransceiver transceiver) {
            MediaStreamTrack track = transceiver.getReceiver().track();
            sessionHandler.post(() -> {
                KLog.p("received remote track %s", track);
                PeerConnectionWrapper pcWrapper = getPcWrapper(connType);
                if (null == pcWrapper) return;

                if (track instanceof VideoTrack) {
                    pcWrapper.createRemoteVideoTrack(transceiver.getMid(), (VideoTrack) track);
                } else {
                    pcWrapper.createRemoteAudioTrack(transceiver.getMid(), (AudioTrack) track);
                }
            });
        }

    }



    /**
     * 创建Encoding列表
     * */
    private List<RtpParameters.Encoding> createEncodingList(boolean bTrackAdded){
        List<RtpParameters.Encoding> encodings = new ArrayList<>();
        if (!bTrackAdded) {
           /* NOTE
            track被添加之前scaleResolutionDownBy必须设置为null否则会崩溃，提示
            "Fatal error: C++ addTransceiver failed"。
            等到track被添加之后，sdp被创建之前，
            通过sender.getParameters()获取encodings列表，然后给scaleResolutionDownBy赋予真实的值。
            */
            encodings.add(new RtpParameters.Encoding("l", true,null));
            encodings.add(new RtpParameters.Encoding("m", true, null));
            encodings.add(new RtpParameters.Encoding("h", true, null));
        }else{
            //XXX 暂时写死，具体根据需求来
            encodings.add(new RtpParameters.Encoding("l", true,0.25));
            encodings.add(new RtpParameters.Encoding("m", true, 0.5));
            encodings.add(new RtpParameters.Encoding("h", true, 1.0));
        }
        return encodings;
    }




    private static final String STREAM_ID = "TT-Android-"+System.currentTimeMillis();
    private static final String LOCAL_VIDEO_TRACK_ID = STREAM_ID+"-v";
    private static final String LOCAL_WINDOW_TRACK_ID = STREAM_ID+"-window";
    private static final String LOCAL_SCREEN_TRACK_ID = STREAM_ID+"-screen";
    private static final String LOCAL_AUDIO_TRACK_ID = STREAM_ID+"-a";
    private static int audioTrackCnt = 0;

    /**
     * PeerConnection包装类
     * */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private class PeerConnectionWrapper{

        int connType;
        int curMediaType;

        int sdpType;
        final int Unknown = 0;
        final int Offer = 1;
        final int Answer = 2;
        final int AudioOffer = 3;
        final int VideoOffer = 4;
        final int FingerPrintOffer = 5;

        int sdpState;
        final int Idle = 0;
        final int Creating = 11;
        final int SettingLocal = 12;
        final int Sending = 13;
        final int SettingRemote = 14;


        PeerConnection pc;
        PeerConnectionConfig config;
        SDPObserver sdpObserver;
        List<IceCandidate> queuedRemoteCandidates = new ArrayList<>();
        SurfaceTextureHelper surfaceTextureHelper;
        VideoCapturer videoCapturer;
        VideoSource videoSource;
        VideoTrack localVideoTrack;
        Map<String, VideoTrack> remoteVideoTracks = new HashMap<>();
        AudioSource audioSource;
        AudioTrack localAudioTrack;
        Map<String, AudioTrack> remoteAudioTracks = new HashMap<>();
        private RtpSender videoSender;
        private RtpSender audioSender;

        // 是否正在取消发布。由于协议组当前实现所限取消发布后我们需要重建PeerConnection
        private boolean isUnpublishing;

        PeerConnectionWrapper(int connType, @NonNull PeerConnectionConfig config, @NonNull SDPObserver sdpObserver) {
            this.connType = connType;
            this.config = config;
            this.sdpObserver = sdpObserver;
        }

        PeerConnectionWrapper(int connType, @NonNull PeerConnection pc, @NonNull PeerConnectionConfig config,
                              @NonNull SDPObserver sdpObserver) {
            this.connType = connType;
            this.pc = pc;
            this.config = config;
            this.sdpObserver = sdpObserver;
        }

        void setPeerConnection(@NonNull PeerConnection pc){
            this.pc = pc;
        }


        void createOffer(){
            executor.execute(() -> pc.createOffer(sdpObserver, new MediaConstraints()));
        }
        void createAnswer(){
            executor.execute(() -> pc.createAnswer(sdpObserver, new MediaConstraints()));
        }
        void setLocalDescription(SessionDescription sdp){
            executor.execute(() -> pc.setLocalDescription(sdpObserver, sdp));
        }
        void setRemoteDescription(SessionDescription sdp){
            executor.execute(() -> pc.setRemoteDescription(sdpObserver, sdp));
        }


        /**
         * 创建视频轨道。
         * */
        void createVideoTrack(@NonNull VideoCapturer videoCapturer){
            executor.execute(() -> {
                if (null != localVideoTrack){
                    KLog.p(KLog.ERROR, "localVideoTrack has created");
                    return;
                }
                this.videoCapturer = videoCapturer;
                surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", eglBase.getEglBaseContext());
                videoSource = factory.createVideoSource(videoCapturer.isScreencast());
                videoCapturer.initialize(surfaceTextureHelper, context, videoSource.getCapturerObserver());
                boolean bTrackEnable = true;
                String localVideoTrackId;
                if (videoCapturer instanceof WindowCapturer){
                    localVideoTrackId = LOCAL_WINDOW_TRACK_ID;
                }else if (videoCapturer.isScreencast()){
                    localVideoTrackId = LOCAL_SCREEN_TRACK_ID;
                }else{
                    localVideoTrackId = LOCAL_VIDEO_TRACK_ID;
                    bTrackEnable = userConfig.getIsLocalVideoEnabled();
                }
                if (bTrackEnable) {
                    // 仅本地摄像头开启状态下开启采集
                    videoCapturer.startCapture(config.videoWidth, config.videoHeight, config.videoFps);
                }
                localVideoTrack = factory.createVideoTrack(localVideoTrackId, videoSource);
                localVideoTrack.setEnabled(bTrackEnable);

                boolean bSimulcast = userConfig.getEnableSimulcast();
                List<RtpParameters.Encoding> encodingList;
                if (bSimulcast){
                    encodingList = createEncodingList(false);
                }else{
                    encodingList = new ArrayList<>();
                }

                RtpTransceiver.RtpTransceiverInit transceiverInit = new RtpTransceiver.RtpTransceiverInit(
                        RtpTransceiver.RtpTransceiverDirection.SEND_ONLY,
                        Collections.singletonList(STREAM_ID),
                        encodingList
                );

                RtpTransceiver transceiver = pc.addTransceiver(localVideoTrack, transceiverInit);
                videoSender = transceiver.getSender();

                if (bSimulcast){
                    // 在添加track之后才去设置encoding参数。XXX  暂时写死，具体根据需求来。
                    // NOTE：注意和sendOffer时传给业务组件的参数一致。
                    for (RtpParameters.Encoding encoding : videoSender.getParameters().encodings){
                        if (encoding.rid.equals("h")){
                            encoding.scaleResolutionDownBy = 1.0;
                            encoding.maxFramerate = config.videoFps;
                            encoding.maxBitrateBps = config.videoMaxBitrate;
                        }else if (encoding.rid.equals("m")){
                            encoding.scaleResolutionDownBy = 0.5;
                            encoding.maxFramerate = config.videoFps;
                            encoding.maxBitrateBps = config.videoMaxBitrate;
                        }else if (encoding.rid.equals("l")){
                            encoding.scaleResolutionDownBy = 0.25;
                            encoding.maxFramerate = config.videoFps;
                            encoding.maxBitrateBps = config.videoMaxBitrate;
                        }
                    }
                }

                String kdStreamId = localVideoTrackId;
                KLog.p("create local video track %s/%s", kdStreamId, localVideoTrackId);
                sessionHandler.post(() -> kdStreamId2RtcTrackIdMap.put(kdStreamId, localVideoTrackId));

                if (localVideoTrackId.equals(LOCAL_VIDEO_TRACK_ID)) {
                    sessionHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            // 对于窗口共享、屏幕共享不需要回显。
                            // 所以此处我们仅针对摄像头采集的情形处理。
                            Conferee myself = findMyself();
                            if (null == myself) {
                                KLog.p(KLog.ERROR, "what's wrong? myself not join yet !?");
                                sessionHandler.postDelayed(this, 2000);
                                return;
                            }
                            executor.execute(() -> {
                                localVideoTrack.addSink(myself);  // 本地回显
                            });
                            // 检查摄像头是否屏蔽，若屏蔽则展示静态图片
                            myself.setState(userConfig.getIsLocalVideoEnabled() ? Conferee.VideoState_Normal : Conferee.VideoState_Disabled);
                            // 本地流不会通过StreamJoined消息报上来，所以我们需要在此处创建并设置给己端与会方，而非依赖StreamJoined消息的处理逻辑
                            myself.videoStream = new VideoStream(kdStreamId, false,
                                    Collections.singletonList(EmMtResolution.emMtHD1080p1920x1080_Api) // XXX 暂时写死了
                            );
                        }
                    });
                }


            });
        }

        void removeVideoTrack(){
            executor.execute(() -> {
                pc.removeTrack(videoSender);

                sessionHandler.post(() -> {
                    String trackId = localVideoTrack.id();
                    String kdStreamId = kdStreamId2RtcTrackIdMap.inverse().get(trackId);
                    kdStreamId2RtcTrackIdMap.remove(kdStreamId);
                    // 删除流
                    // 己端取消发布流对端会收到StreamLeft但己端收不到，所以我们需要在此处删除流，而非依赖StreamLeft消息的处理逻辑
                    Conferee myself = findMyself();
                    if (null != myself) {
                        myself.removeStream(kdStreamId);
                        KLog.p("localVideoTrack %s removed ", kdStreamId);
                    }else{
                        KLog.p(KLog.ERROR, "something wrong? stream %s is not mine", kdStreamId);
                    }
                });

            });
        }


        void createRemoteVideoTrack(String mid, VideoTrack track){
            String kdStreamId = mid2KdStreamIdMap.get(mid);
            if (null == kdStreamId) {
                KLog.p(KLog.ERROR, "no register stream for mid %s in signaling progress(see onSetOfferCmd)", mid);
                return;
            }
            KLog.p("create remote video track %s/%s", kdStreamId, track.id());
            kdStreamId2RtcTrackIdMap.put(kdStreamId, track.id());
            Conferee conferee = findConfereeByStreamId(kdStreamId);
            if (null == conferee) {
                KLog.p(KLog.ERROR, "something wrong? stream %s not belong to any conferee", kdStreamId);
                return;
            }

            executor.execute(() -> {
                remoteVideoTracks.put(kdStreamId, track);
                track.setEnabled(userConfig.getIsRemoteVideoEnabled());
                track.addSink(conferee);
            });
        }

        void removeRemoteVideoTrack(String kdStreamId){
            executor.execute(() -> {
                for (String streamId : remoteVideoTracks.keySet()) {
                    if (streamId.equals(kdStreamId)) {
                        VideoTrack track = remoteVideoTracks.remove(kdStreamId);
//                        track.dispose();
                        KLog.p("stream %s removed", kdStreamId);

                        sessionHandler.post(() -> {
                            kdStreamId2RtcTrackIdMap.remove(kdStreamId);
                        });
                        return;
                    }
                }

                KLog.p(KLog.ERROR, "failed to removeRemoteVideoTrack, no such track %s", kdStreamId);
            });
        }


        void createAudioTrack(){
            executor.execute(() -> {
                audioSource = factory.createAudioSource(new MediaConstraints());
                String localAudioTrackId = LOCAL_AUDIO_TRACK_ID+audioTrackCnt++;
                localAudioTrack = factory.createAudioTrack(localAudioTrackId, audioSource);
                localAudioTrack.setEnabled(userConfig.getIsLocalAudioEnabled());
                RtpTransceiver.RtpTransceiverInit transceiverInit = new RtpTransceiver.RtpTransceiverInit(
                        RtpTransceiver.RtpTransceiverDirection.SEND_ONLY,
                        Collections.singletonList(STREAM_ID)
                );
                RtpTransceiver transceiver = pc.addTransceiver(localAudioTrack, transceiverInit);
                audioSender = transceiver.getSender();

                String kdStreamId = localAudioTrackId;
                KLog.p("create local audio track %s/%s", kdStreamId, localAudioTrackId);
                sessionHandler.post(() -> kdStreamId2RtcTrackIdMap.put(kdStreamId, localAudioTrackId));
                sessionHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Conferee myself = findMyself();
                        if (null != myself){
                            // 本地流不会通过StreamJoined消息报上来，所以我们需要在此处创建并设置给己端与会方，而非依赖StreamJoined消息的处理逻辑
                            myself.addAudioStream(new AudioStream(kdStreamId));
                        }else{
                            KLog.p(KLog.ERROR, "what's wrong? myself not join yet !?");
                            sessionHandler.postDelayed(this, 2000);
                        }
                    }
                });
            });
        }

        void removeAudioTrack(){
            executor.execute(() -> {
                pc.removeTrack(audioSender);

                sessionHandler.post(() -> {
                    String trackId = localAudioTrack.id();
                    String kdStreamId = kdStreamId2RtcTrackIdMap.inverse().get(trackId);
                    kdStreamId2RtcTrackIdMap.remove(kdStreamId);

                    Conferee myself = findMyself();
                    if (null != myself) {
                        // 删除流
                        // 己端取消发布流对端会收到StreamLeft但己端收不到，所以我们需要在此处删除流，而非依赖StreamLeft消息的处理逻辑
                        myself.removeStream(kdStreamId);
                        KLog.p("localVideoTrack %s removed ", kdStreamId);
                    }else{
                        KLog.p(KLog.ERROR, "something wrong? stream %s is not mine", kdStreamId);
                    }
                });

            });
        }



        void createRemoteAudioTrack(String mid, AudioTrack track){
            String kdStreamId = mid2KdStreamIdMap.get(mid);
            KLog.p("mid=%s, streamId=%s", mid, kdStreamId);
            if (null == kdStreamId) {
                KLog.p(KLog.ERROR, "no register stream for mid %s in signaling progress(see onSetOfferCmd)", mid);
                return;
            }
            kdStreamId2RtcTrackIdMap.put(kdStreamId, track.id());
            KLog.p("create remote audio track %s/%s", kdStreamId, track.id());

            executor.execute(() -> {
                track.setEnabled(userConfig.getIsRemoteAudioEnabled());
                remoteAudioTracks.put(kdStreamId, track);
            });
        }

        void removeRemoteAudioTrack(String kdStreamId){
            executor.execute(() -> {
                for (String streamId : remoteAudioTracks.keySet()) {
                    if (streamId.equals(kdStreamId)) {
                        AudioTrack track = remoteAudioTracks.remove(kdStreamId);
//                        track.dispose();
                        sessionHandler.post(() -> kdStreamId2RtcTrackIdMap.remove(kdStreamId));
                        KLog.p("stream %s removed", kdStreamId);
                        return;
                    }
                }

                KLog.p(KLog.ERROR, "failed to removeRemoteAudioTrack, no such track %s", kdStreamId);
            });
        }


        void destoryAudioTrack(){
            executor.execute(() -> {
                localAudioTrack.setEnabled(false);
                if (audioSource != null) {
                    audioSource.dispose();
                    audioSource = null;
                }

                localAudioTrack = null;
                pc.removeTrack(audioSender);
                audioSender = null;
            });
        }


        void setSdpType(int sdpType) {
//            KLog.up("set sdp type from %s to %s", this.sdpType, sdpType);
            this.sdpType = sdpType;
        }

        boolean isSdpType(int sdpType){
            return sdpType == this.sdpType;
        }

        void setSdpState(int sdpState) {
//            KLog.up("switch sdp state from %s to %s", this.sdpState, sdpState);
            this.sdpState = sdpState;
        }

        boolean isSdpState(int sdpState){
            return sdpState == this.sdpState;
        }

        boolean checkSdpState(int... sdpStates) {
            for (int state : sdpStates){
                if (state == this.sdpState){
                    return true;
                }
            }
            KLog.p(KLog.ERROR, "invalid sdp sate, expect "+sdpStates+" but current is "+this.sdpState);
            return false;
        }



        void drainCandidates() {
            executor.execute(() -> {
                if (queuedRemoteCandidates != null) {
                    for (IceCandidate candidate : queuedRemoteCandidates) {
                        pc.addIceCandidate(candidate);
                    }
                    queuedRemoteCandidates = null;
                }
            });
        }

        void addCandidate(IceCandidate candidate) {
            executor.execute(() -> {
                if (queuedRemoteCandidates != null) {
                    queuedRemoteCandidates.add(candidate);
                } else {
                    pc.addIceCandidate(candidate);
                }
            });
        }

        void close(){
            executor.execute(() -> {
                if (pc != null) {
                    pc.dispose();
                    pc = null;
                }
                if (audioSource != null) {
                    audioSource.dispose();
                    audioSource = null;
                }

                localAudioTrack = null;

                remoteAudioTracks.clear();

                if (videoCapturer != null) {
                    try {
                        videoCapturer.stopCapture();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    videoCapturer.dispose();
                    videoCapturer = null;
                }
                if (videoSource != null) {
                    videoSource.dispose();
                    videoSource = null;
                }

                localVideoTrack = null;

                remoteVideoTracks.clear();

                if (surfaceTextureHelper != null) {
                    surfaceTextureHelper.dispose();
                    surfaceTextureHelper = null;
                }
            });
        }


        void setRemoteAudioEnable(boolean bEnable){
            executor.execute(() -> {
                for (AudioTrack audioTrack : remoteAudioTracks.values()) {
                    audioTrack.setEnabled(bEnable);
                }
            });
        }

        void setLocalAudioEnable(boolean bEnable){
            executor.execute(() -> {
                if (localAudioTrack != null) {
                    localAudioTrack.setEnabled(bEnable);
                }
            });
        }

        void setRemoteVideoEnable(boolean bEnable){
            executor.execute(() -> {
                for (VideoTrack videoTrack : remoteVideoTracks.values()) {
                    videoTrack.setEnabled(bEnable);
                }
            });
        }


        void setLocalVideoEnable(boolean bEnable){
            executor.execute(() -> {
                if (localVideoTrack != null && localVideoTrack.enabled() != bEnable) {
                    localVideoTrack.setEnabled(bEnable);
                    if (bEnable){
                        videoCapturer.startCapture(config.videoWidth, config.videoHeight, config.videoFps);
                    }else{
                        try {
                            videoCapturer.stopCapture();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
        }

        void switchCamera(){
            executor.execute(() -> {
                if (null != videoCapturer) {
                    ((CameraVideoCapturer) videoCapturer).switchCamera(null);
                }
            });
        }

    }


    /**
     * RTC 统计信息
     * */
    public static class Stats {
        public List<MediaStats> mediaStatsList = new ArrayList<>();

        @Override
        public String toString() {
            return "Stats{" +
                    "mediaStatsList=" + mediaStatsList +
                    '}';
        }
    }
    private Stats stats = new Stats();
    public static class MediaStats {
        public boolean bAudio; // 是否为音频
        public boolean bLocal; // 是否本地
        public String trackId; // 音/视轨ID
        public String fmt;     // 音/视频格式

        // 仅音频相关
        public float audioLevel;

        // 仅视频相关
        public int width;
        public int height;
        public int fps; // 帧率
        public boolean bHwa; // 是否硬编解（对于发送是编，接收是解）

        @Override
        public String toString() {
            return "MediaStats{" +
                    "bAudio=" + bAudio +
                    ", bLocal=" + bLocal +
                    ", trackId='" + trackId + '\'' +
                    ", fmt='" + fmt + '\'' +
                    ", audioLevel=" + audioLevel +
                    ", width=" + width +
                    ", height=" + height +
                    ", fps=" + fps +
                    ", bHwa=" + bHwa +
                    '}';
        }
    }

    public interface StatsListener {
        void onRtcStats(Stats stats);
    }


    private StatsListener statsListener;
    /**
     * 设置RTC统计信息监听器
     * */
    public void setStatsListener(StatsListener statsListener){
        this.statsListener = statsListener;
    }


    // 用于定时收集统计信息
    private final StatsHelper.Stats publisherStats = new StatsHelper.Stats();
    private final StatsHelper.Stats subscriberStats = new StatsHelper.Stats();
    private final StatsHelper.Stats assPublisherStats = new StatsHelper.Stats();
    private final StatsHelper.Stats assSubscriberStats = new StatsHelper.Stats();
    // RTC统计信息收集器
    private Runnable statsCollector = new Runnable() {
        @Override
        public void run() {
            if (null != pubPcWrapper && null != pubPcWrapper.pc) {
                pubPcWrapper.pc.getStats(rtcStatsReport -> {
//                    KLog.p("publisherStats=%s ", rtcStatsReport);
                    synchronized (publisherStats) {
                        StatsHelper.resolveStats(rtcStatsReport, publisherStats);
                    }
                });
            }
            if (null != subPcWrapper && null != subPcWrapper.pc) {
                subPcWrapper.pc.getStats(rtcStatsReport -> {
//                    KLog.p("subscriberStats=%s ", rtcStatsReport);
                    synchronized (subscriberStats) {
                        StatsHelper.resolveStats(rtcStatsReport, subscriberStats);
                    }
                });
            }
//            if (null != assPubPcWrapper && null != assPubPcWrapper.pc) {
//                assPubPcWrapper.pc.getStats(rtcStatsReport -> {
//                    KLog.p("assPublisherStats=%s ", rtcStatsReport);
//                    synchronized (assPublisherStats) {
//                        StatsHelper.resolveStats(rtcStatsReport, assPublisherStats);
//                    }
//                });
//            }
//            if (null != assSubPcWrapper && null != assSubPcWrapper.pc) {
//                assSubPcWrapper.pc.getStats(rtcStatsReport -> {
//                    KLog.p("assSubscriberStats=%s ", rtcStatsReport);
//                    synchronized (assSubscriberStats) {
//                        StatsHelper.resolveStats(rtcStatsReport, assSubscriberStats);
//                    }
//                });
//            }

            sessionHandler.postDelayed(this, 2000);
        }
    };


    private String preMaxAudioLevelKdStreamId;
    // 音频统计信息处理器
    private Runnable audioStatsProcesser = new Runnable() {
        @Override
        public void run() {
            // 比较各与会方的音量以选出最大者用以语音激励
            String maxAudioLevelTrackId;
            double maxAudioLevel;
            // 己端的音量
            synchronized (publisherStats) {
                maxAudioLevelTrackId = null != publisherStats.audioSource ? publisherStats.audioSource.trackIdentifier : null;
                maxAudioLevel = null != publisherStats.audioSource ? publisherStats.audioSource.audioLevel : 0;
                KLog.p("my audioLevel= %s", maxAudioLevel);
            }
            // 其他与会方的音量
            synchronized (subscriberStats) {
                for (StatsHelper.RecvAudioTrack track : subscriberStats.recvAudioTrackList) {
                    String kdStreamId = kdStreamId2RtcTrackIdMap.inverse().get(track.trackIdentifier);
                    Conferee conferee = findConfereeByStreamId(kdStreamId);
                    if (null == conferee) {
                        KLog.p(KLog.ERROR, "track %s(kdstreamId=%s) not belong to any conferee?", track.trackIdentifier, kdStreamId);
                        continue;
                    }
                    KLog.p("track %s(kdstreamId=%s), audioLevel %s, maxAudioLevel=%s", track.trackIdentifier, kdStreamId, track.audioLevel, maxAudioLevel);
                    if (track.audioLevel > maxAudioLevel) {
                        maxAudioLevel = track.audioLevel;
                        maxAudioLevelTrackId = track.trackIdentifier;
                    }
                }
            }
            KLog.p("maxAudioLevel=%s", maxAudioLevel);
            if (maxAudioLevel > 0.1){
                // 大于0.1才认为是人说话，否则认为是环境噪音
                String maxAudioLevelKdStreamId = kdStreamId2RtcTrackIdMap.inverse().get(maxAudioLevelTrackId);
                KLog.p("preMaxAudioLevelKdStreamId=%s, maxAudioLevelKdStreamId=%s", preMaxAudioLevelKdStreamId, maxAudioLevelKdStreamId);
                if (null != maxAudioLevelKdStreamId) {
                    if (!maxAudioLevelKdStreamId.equals(preMaxAudioLevelKdStreamId)) {
                        // 说话人变化了才需要刷新语音激励状态
                        Conferee conferee = findConfereeByStreamId(preMaxAudioLevelKdStreamId);
                        if (null != conferee) {
                            conferee.setVoiceActivated(false);
                        }
                        conferee = findConfereeByStreamId(maxAudioLevelKdStreamId);
                        if (null != conferee) {
                            conferee.setVoiceActivated(true);
                        }
                        preMaxAudioLevelKdStreamId = maxAudioLevelKdStreamId;
                    }
                }else{
                    KLog.p(KLog.ERROR, "something wrong! cannot find TrackId %s in kdStreamId2RtcTrackIdMap", maxAudioLevelTrackId);
                }
            }else {
                // 当前没有人说话，原来设置的语音激励清掉
                KLog.p("preMaxAudioLevelKdStreamId=%s", preMaxAudioLevelKdStreamId);
                Conferee conferee = findConfereeByStreamId(preMaxAudioLevelKdStreamId);
                if (null != conferee){
                    conferee.setVoiceActivated(false);
                }
                preMaxAudioLevelKdStreamId = null;
            }


            sessionHandler.postDelayed(this, 2000);
        }
    };


    private Map<String, Long> preReceivedFramesMap = new HashMap<>();
    private long videoStatsTimeStamp = System.currentTimeMillis();
    // 视频统计信息处理器
    private Runnable videoStatsProcesser = new Runnable() {
        @Override
        public void run() {
            // 其他与会方的帧率
            Map<String, Long> framesReceivedMap = new HashMap<>();
            synchronized (subscriberStats) {
                for (StatsHelper.RecvVideoTrack track : subscriberStats.recvVideoTrackList) {
                    framesReceivedMap.put(track.trackIdentifier, track.framesReceived);
                }
            }
            long curTimestamp = System.currentTimeMillis();
            for (Map.Entry<String, Long> entry : framesReceivedMap.entrySet()){
                String trackIdentifier = entry.getKey();
                long preReceivedFrames = null != preReceivedFramesMap.get(trackIdentifier) ? preReceivedFramesMap.get(trackIdentifier) : 0;
                long curReceivedFrames = entry.getValue();
                KLog.p("trackIdentifier=%s, preReceivedFrames=%s, curReceivedFrames=%s", trackIdentifier, preReceivedFrames, curReceivedFrames);
                String lostSignalKdStreamId = kdStreamId2RtcTrackIdMap.inverse().get(trackIdentifier);
                Conferee conferee = findConfereeByStreamId(lostSignalKdStreamId);
                if (null != conferee){
                    float fps = (curReceivedFrames - preReceivedFrames) / ((curTimestamp - videoStatsTimeStamp)/1000f);
                    if (!conferee.bWaitingVideoStream && fps < 0.2){ // 可忍受的帧率下限，低于该下限则认为信号丢失
                        KLog.p("conferee %s setState %s", conferee.id, Conferee.VideoState_WeakSignal);
                        conferee.setState(Conferee.VideoState_WeakSignal);
                    }else if (fps > 1){
                        // 尽管我们在帧率低于0.2时将状态设为WeakSignal但当帧率超过0.2时我们不立马设置状态为Normal
                        // 而是等帧率回复到较高水平才切回Normal以使状态切换显得平滑而不是在临界值处频繁切换
                        KLog.p("conferee %s setState %s", conferee.id, Conferee.VideoState_Normal);
                        conferee.setState(Conferee.VideoState_Normal);
                    }
                }else{
                    KLog.p(KLog.ERROR, "track %s(kdstreamId=%s) not belong to any conferee?", trackIdentifier, lostSignalKdStreamId);
                }
            }

            if (curTimestamp - videoStatsTimeStamp >= 2000) {
                preReceivedFramesMap = framesReceivedMap;
                videoStatsTimeStamp = curTimestamp;
            }

            sessionHandler.postDelayed(this, 2000);
        }
    };


    // 码流状态监视器
    private Runnable kdStreamStatusMonitor = new Runnable() {

        @Override
        public void run() {
            for (Conferee conferee : conferees){
                if (null == conferee.videoStream){
                    if (!conferee.bWaitingVideoStream) {
                        conferee.setState(Conferee.VideoState_NoStream);
                    }
                }else{
                    if (conferee.state == Conferee.VideoState_NoStream){
                        conferee.setState(Conferee.VideoState_Normal);
                    }
                }
            }
            sessionHandler.postDelayed(this, 2000);
        }
    };

}
