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
import com.google.common.collect.Sets;
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
import com.kedacom.vconf.sdk.utils.math.MatrixHelper;
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
import org.webrtc.Logging;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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

    // 平台的StreamId到WebRTC的TrackId之间的映射
    private BiMap<String, String> kdStreamId2RtcTrackIdMap = HashBiMap.create();


    // 当前用户的e164
    private String userE164;

    // RTC配置
    private RtcConfig.Config config = new RtcConfig.Config();

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


    private static WebRtcManager instance;

    private WebRtcManager(Application context){
        this.context = context;
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
                Msg.Login,
                Msg.Logout,
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
                Msg.LoginStateChanged,
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
     * @param e164 用户e164号
     * @param resultListener 登陆结果监听器。
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
        req(Msg.Login, resultListener, rtcSvrAddr);
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
        req(Msg.Logout, resultListener, new TMtRtcSvrAddr(false));
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
     *          失败：{@link RtcResultCode#IncorrectConfPassword}
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

    //@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
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
     * 暂停窗口共享
     * */
    public void pauseWindowShare(){
        setWindowShareEnable(false);
    }

    /**
     * 继续窗口共享
     * */
    public void resumeWindowShare(){
        setWindowShareEnable(true);
    }

    private void setWindowShareEnable(boolean enable){
        if (null == sharedWindow){
            KLog.p(KLog.WARN, "window share not started yet!");
            return;
        }
        PeerConnectionWrapper pcWrapper = getPcWrapper(CommonDef.CONN_TYPE_ASS_PUBLISHER);
        if (null != pcWrapper) {
            pcWrapper.setLocalVideoEnable(enable);
        }
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
     * 是否处于静音状态
     * */
    public boolean isSilenced(){
        return config.isSilenced;
    }


    /**
     * 设置静音。
     * @param bSilence true，屏蔽对方语音；false，放开对方语音。
     * @return true成功，false失败。
     * */
    public boolean setSilence(boolean bSilence){
        if(!bSessionStarted){
            KLog.p(KLog.ERROR,"session not start");
            return false;
        }
        if (bSilence != config.isSilenced) {
            PeerConnectionWrapper pcWrapper = getPcWrapper(CommonDef.CONN_TYPE_SUBSCRIBER);
            if (null == pcWrapper) {
                KLog.p(KLog.ERROR,"null == pcWrapper");
                return false;
            }
            pcWrapper.setRemoteAudioEnable(!bSilence);
            config.isSilenced = bSilence;
        }
        req(Msg.SetSilence, null, bSilence);

        return true;
    }


    /**
     * 是否处于哑音状态
     * */
    public boolean isMuted(){
        return config.isMuted;
    }

    /**
     * 设置哑音。
     * @param bMute true，屏蔽自己语音；false，放开自己语音。
     * */
    public boolean setMute(boolean bMute){
        if(!bSessionStarted){
            KLog.p(KLog.ERROR,"session not start");
            return false;
        }
        if (bMute != config.isMuted) {
            PeerConnectionWrapper pcWrapper = getPcWrapper(CommonDef.CONN_TYPE_PUBLISHER);
            if (null == pcWrapper) {
                KLog.p(KLog.ERROR,"null == pcWrapper");
                return false;
            }
            pcWrapper.setLocalAudioEnable(!bMute);
            config.isMuted = bMute;
        }
        req(Msg.SetMute, null, bMute);
        return true;
    }

    /**
     * 是否正在使用前置摄像头
     * @return true前置，false后置
     * */
    public boolean isUsingFrontCamera(){
        return config.isFrontCameraPreferred;
    }

    /**
     * 切换摄像头
     * */
    public boolean switchCamera(){
        if(!bSessionStarted){
            KLog.p(KLog.ERROR,"session not start");
            return false;
        }
        PeerConnectionWrapper pcWrapper = getPcWrapper(CommonDef.CONN_TYPE_PUBLISHER);
        if (null == pcWrapper) {
            KLog.p(KLog.ERROR,"null == pcWrapper");
            return false;
        }
        pcWrapper.switchCamera();
        config.isFrontCameraPreferred = !config.isFrontCameraPreferred;
        Conferee conferee = findMyself();
        if (null != conferee) {
            for (Display display : conferee.displays){
                display.setMirror(config.isFrontCameraPreferred); // 前置摄像头情况下需镜像显示
            }
        }
        return true;
    }


    /**
     * 摄像头是否开启状态
     * */
    public boolean isCameraEnabled(){
        return config.isLocalVideoEnabled;
    }

    /**
     * 开启/关闭摄像头
     * */
    public boolean setCameraEnable(boolean enable) {
        if(!bSessionStarted){
            KLog.p(KLog.ERROR,"session not start");
            return false;
        }
        if (enable != config.isLocalVideoEnabled) {
            PeerConnectionWrapper pcWrapper = getPcWrapper(CommonDef.CONN_TYPE_PUBLISHER);
            if (null == pcWrapper) {
                KLog.p(KLog.ERROR,"null == pcWrapper");
                return false;
            }
            pcWrapper.setLocalVideoEnable(enable);
            config.isLocalVideoEnabled = enable;
            Conferee conferee = findMyself();
            if (null != conferee && Conferee.VideoState_NoStream != conferee.state) {
                conferee.setState(enable ? Conferee.VideoState_Normal : Conferee.VideoState_Disabled);
            }
        }
        return true;
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
            case LoginStateChanged:
                TRegState loginResult = (TRegState) rspContent;
                KLog.p("loginResult: %s", loginResult.AssParam.basetype);
                int resCode = RtcResultCode.fromTransfer(loginResult.AssParam.basetype);
                if (Msg.Login == req) { // 登录
                    if (RtcResultCode.OK == resCode) {
                        reportSuccess(null, listener);
                    } else {
                        reportFailed(resCode, listener);
                    }
                }else{ // 注销
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
                resCode = RtcResultCode.fromTransfer(reason.basetype);
                if (Msg.QuitConf == req || Msg.EndConf == req) {
                    reportSuccess(null, listener);
                }else{
                    reportFailed(resCode, listener);
                }
                break;

            case ConfCanceled:
                stopSession();
                reason = (BaseTypeInt) rspContent;
                KLog.p("ConfCanceled: %s", reason.basetype);
                resCode = RtcResultCode.fromTransfer(reason.basetype);
                reportFailed(resCode, listener);
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
                resCode = RtcResultCode.fromTransfer(tCreateConfResult.MainParam.dwErrorID);
                if (RtcResultCode.ConfOK != resCode){
                    stopSession();
                    cancelReq(req, listener);
                    reportFailed(resCode, listener);
                }
                break;

            case QueryConfInfoRsp:
                TQueryConfInfoResult queryConfInfoResult = (TQueryConfInfoResult) rspContent;
                resCode = RtcResultCode.fromTransfer(queryConfInfoResult.MainParam.dwErrorID);
                if (RtcResultCode.ConfOK != resCode){
                    reportFailed(resCode, listener);
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
                    reportFailed(RtcResultCode.IncorrectConfPassword, listener);
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
            case LoginStateChanged:
                TRegState regState = (TRegState) ntfContent;
                int resCode = RtcResultCode.fromTransfer(regState.AssParam.basetype);
                if (RtcResultCode.OK != resCode) {
                    if (null != confEventListener) confEventListener.onConfServerDisconnected(resCode);
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
                resCode = RtcResultCode.fromTransfer(reason.basetype);
                if (null != confEventListener) confEventListener.onConfFinished(resCode);
                break;

            case ConfCanceled:
                stopSession();
                reason = (BaseTypeInt) ntfContent;
                KLog.p("ConfCanceled: %s", reason.basetype);
                resCode = RtcResultCode.fromTransfer(reason.basetype);
                if (null != confEventListener) confEventListener.onConfFinished(resCode);
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
                    // 更新与会方辅流发送状态
                    Conferee owner = findConfereeByConfereeId(Conferee.buildId(assConferee.mcuId, assConferee.terId, false));
                    if (null!=owner){
                        owner.isSendingAssStream = false;
                    }
                    // 辅流退出意味着虚拟的辅流入会方退会
                     if (null != sessionEventListener) {
                         sessionEventListener.onConfereeLeft(assConferee);
                     }
                }

                // 重新订阅视频流
                subscribeStreams();
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
            Conferee existedAssStreamConferee = findAssStreamConferee();
            if (null != existedAssStreamConferee){
                // 如果正在接收辅流，则此为抢发辅流的场景
                // 我们先删除正在接收的辅流（目前会议中仅支持一路辅流）
                conferees.remove(existedAssStreamConferee);
                // 更新与会方辅流发送状态
                Conferee owner = findConfereeByConfereeId(Conferee.buildId(existedAssStreamConferee.mcuId, existedAssStreamConferee.terId, false));
                if (null!=owner){
                    owner.isSendingAssStream = false;
                }
                if (null != sessionEventListener){
                    // 通知用户之前的辅流已退出
                    // 按说抢双流的场景平台推送消息的时序应该是：“前面的辅流退出->后面的辅流加入”，实际却是“后面的辅流加入->前面的辅流退出”。
                    // 这容易引起用户的疑惑，我们内部调整时序为“前面的辅流退出->后面的辅流加入”
                    sessionEventListener.onConfereeLeft(existedAssStreamConferee);
                }
            }
            // 针对辅流我们构造一个虚拟的与会方
            assStreamSender.isSendingAssStream = true;
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
        subscribeStreams();
    }

    private EmMtResolution getSubResolution(Conferee conferee){
        if (conferee == null || conferee.videoStream == null){
            KLog.p(KLog.ERROR, "conferee == null || conferee.videoStream == null");
            return null;
        }

        // NOTE: 分辨率是按从小到大的顺序排列的，这点平台保证。
        List<EmMtResolution> resolutions = conferee.videoStream.supportedResolutionList;

        KLog.p("config.PreferredVideoQuality=%s, conferee.%s preferredVideoQuality=%s, resolutions=%s",
                config.preferredVideoQuality, conferee.e164, conferee.preferredVideoQuality, resolutions);
        int quality = Math.min(config.preferredVideoQuality, conferee.preferredVideoQuality);
        if (RtcConfig.VideoQuality_High == quality){
            return resolutions.get(resolutions.size()-1);
        }else if (RtcConfig.VideoQuality_Medium == quality ){
            if (resolutions.size()>1){
                return resolutions.get(1);
            }else{
                return resolutions.get(0);
            }
        }else if (RtcConfig.VideoQuality_Low == quality ){
            return resolutions.get(0);
        }

        return resolutions.get(0);
    }


    private void subscribeStreams(){
        Set<VideoStream> videoStreams = getAllVideoStreamsExceptMine();
        List<TRtcPlayItem> playItems = FluentIterable.from(videoStreams).transform(new Function<VideoStream, TRtcPlayItem>() {
            @NullableDecl
            @Override
            public TRtcPlayItem apply(@NullableDecl VideoStream input) {
                return new TRtcPlayItem(input.streamId, input.bAss, getSubResolution(findConfereeByStreamId(input.streamId)) );
            }
        }).toList();

        set(Msg.SelectStream, new TRtcPlayParam(playItems));
    }


    private static String getSdpVideoCodecName(String videoCodec) {
        switch (videoCodec) {
            case RtcConfig.VIDEO_CODEC_VP9:
                return RtcConfig.VIDEO_CODEC_VP9;
            case RtcConfig.VIDEO_CODEC_H264_HIGH:
            case RtcConfig.VIDEO_CODEC_H264_BASELINE:
                return RtcConfig.VIDEO_CODEC_H264;
            case RtcConfig.VIDEO_CODEC_VP8:
            default:
                return RtcConfig.VIDEO_CODEC_VP8;
        }
    }





    private boolean bSessionStarted;
    private synchronized boolean startSession(@NonNull SessionEventListener listener){
        if (bSessionStarted){
            KLog.p(KLog.ERROR, "session has started already!");
            return false;
        }

        KLog.p("starting session...");

        bSessionStarted = true;
        sessionEventListener = listener;

        rtcConnector.setSignalingEventsCallback(new RtcConnectorEventListener());

        config.copy(RtcConfig.getInstance(context).dump());
        KLog.p("init rtc config: "+config);
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
        KLog.p("stopping session...");

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

        kdStreamId2RtcTrackIdMap.clear();

        screenCapturePermissionData = null;

        destroyPeerConnectionWrapper();
        destroyPeerConnectionFactory();

        // destroy audiomanager
//        if (audioManager != null) {
//            audioManager.stop();
//            audioManager = null;
//        }

        // 取消可能正在进行中的验证密码请求
        // 验证密码请求有些特殊：
        // 首先该请求对应的响应ConfPasswordNeeded既是响应（密码验证失败时）也是通知（加入一个密码会议时会推上来）；
        // 其次当连续3次验证密码失败，第3次不会回响应，而是直接上报会议结束的通知。此时我们再次加入该会议，由于第三次验证请求仍可能存在，
        // 则当“需要输入密码（ConfPasswordNeeded）”的通知上来时，会被第三次的请求当作它期望的响应给消费掉（原本应该作为通知上报用户输入密码），
        // 所以我们在会议结束时取消掉可能进行中的密码验证请求以解决该问题。
        cancelReq(Sets.newHashSet(Msg.VerifyConfPassword));

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

            KLog.p("creating factory...");

            String fieldTrials = ""
//                    +"WebRTC-H264Simulcast/Enabled/"
                    +"WebRTC-SpsPpsIdrIsH264Keyframe/Enabled/";

            PeerConnectionFactory.initialize(PeerConnectionFactory.InitializationOptions.builder(context)
                    .setFieldTrials(fieldTrials)
                    .createInitializationOptions()
            );

            final AudioDeviceModule adm = createJavaAudioDevice();

            final VideoEncoderFactory encoderFactory;
            final VideoDecoderFactory decoderFactory;

            if (config.isHardwareVideoEncoderPreferred) {
                encoderFactory = new DefaultVideoEncoderFactory(eglBase.getEglBaseContext(),true,true);
//                encoderFactory = new HardwareVideoEncoderFactory(eglBase.getEglBaseContext(),true,true);
            } else {
                encoderFactory = new SoftwareVideoEncoderFactory();
            }

            if (config.isHardwareVideoDecoderPreferred){
                decoderFactory = new DefaultVideoDecoderFactory(eglBase.getEglBaseContext());
            }else{
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

//            String webRtcTraceFile = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "webrtc-trace.txt";

//            KLog.p("start tracing...");
//            PeerConnectionFactory.startInternalTracingCapture(webRtcTraceFile);

//            Logging.enableLogThreads();
//            Logging.enableLogTimeStamps();
//            Logging.enableTracing(webRtcTraceFile, EnumSet.of(Logging.TraceLevel.TRACE_DEFAULT));
            Logging.enableLogToDebugOutput(Logging.Severity.LS_INFO);

        });

    }


    private void destroyPeerConnectionFactory(){
        executor.execute(() -> {
            if (null == factory) {
                KLog.p(KLog.ERROR, "Factory not exists!");
                return;
            }
            KLog.p("destroying factory...");
            if (null != eglBase) {
                eglBase.release();
                eglBase = null;
            }

            factory.dispose();
            factory = null;

//            KLog.p("stop tracing");
//            PeerConnectionFactory.stopInternalTracingCapture();
            KLog.p(KLog.WARN, "factory destroyed!");
        });
    }


    private void createPeerConnectionWrapper() {
        /* 创建4个PeerConnectionWrapper
         * NOTE：一个peerconnection可以处理多路码流，收发均可。
         * 但业务要求主流发/收、辅流发/收4种情形分别用单独的peerconnect处理，故此处创建4个。
         * */

        KLog.p("creating pcWrappers...");

        pubPcWrapper = new PeerConnectionWrapper(CommonDef.CONN_TYPE_PUBLISHER, new SDPObserver(CommonDef.CONN_TYPE_PUBLISHER));
        subPcWrapper = new PeerConnectionWrapper(CommonDef.CONN_TYPE_SUBSCRIBER, new SDPObserver(CommonDef.CONN_TYPE_SUBSCRIBER));
        assPubPcWrapper = new PeerConnectionWrapper(CommonDef.CONN_TYPE_ASS_PUBLISHER, new SDPObserver(CommonDef.CONN_TYPE_ASS_PUBLISHER));
        assSubPcWrapper = new PeerConnectionWrapper(CommonDef.CONN_TYPE_ASS_SUBSCRIBER, new SDPObserver(CommonDef.CONN_TYPE_ASS_SUBSCRIBER));

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

            KLog.p("pcWrappers created");

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
        KLog.p("recreating pc %s...", connType);
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
                        KLog.p("pc %s recreated", connType);
                    }else{
                        pc.dispose();
                        KLog.p("pcWrapper not exists, recreating pc %s failed", connType);
                    }
                }

            });
        }
    }

    private final Object pcWrapperLock = new Object();

    private void destroyPeerConnectionWrapper(){
        KLog.p("destroying pcWrappers...");
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
        KLog.p("pcWrappers destroyed");
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
         * @param errCode 错误码{@link RtcResultCode}
         * */
        void onConfServerDisconnected(int errCode);

        /**
         * 会议邀请
         */
        void onConfInvitation(ConfInvitationInfo confInvitationInfo);

        /**
         * 会议结束
         * @param resultCode 错误码{@link RtcResultCode}
         */
        void onConfFinished(int resultCode);
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
     * 获取与会方列表
     * @param exceptVirtualAssStreamConferee 是否排除虚拟的辅流与会方，true排除
     * @param exceptSelf 是否排除自己，true排除
     * @return 与会方列表。已排序。排序规则：自然排序；优先比对昵称，昵称若不存在则使用其e164，e164不存在则使用其邮箱。
     * */
    public List<Conferee> getConferees(boolean exceptVirtualAssStreamConferee, boolean exceptSelf){
        List<Conferee> confereesList = new ArrayList<>();
        for(Conferee conferee : conferees){
            if (exceptVirtualAssStreamConferee && conferee.isVirtualAssStreamConferee
                    || exceptSelf && null!=conferee.e164 && conferee.e164.equals(userE164)){
                continue;
            }
            confereesList.add(conferee);
        }
        Collections.sort(confereesList);
        return confereesList;
    }


    /**
     * 获取己端与会方
     * */
    public Conferee getMySelf(){
        return findMyself();
    }


    /**
     * 与会方
     */
    public static final class Conferee implements VideoSink, Comparable<Conferee>{
        private String id;
        private int mcuId;
        private int terId;
        private String   e164;
        private String   alias;
        private String   email;

        // 是否为辅流与会方（针对辅流我们创建了一个虚拟的“辅流与会方”与之对应）
        private boolean isVirtualAssStreamConferee;

        // 是否正在发送辅流
        // NOTE: 该字段仅真实与会方有效，虚拟的辅流与会方该字段恒为false。
        private boolean isSendingAssStream;

        // 该与会方的视频流。一个与会方只有一路视频流
        private VideoStream videoStream;
        // 该与会方的音频流。一个与会方可以有多路音频流
        private Set<AudioStream> audioStreams = new HashSet<>();

        // 是否在等待与之相关的码流，与会人员入会的消息上来后
        // 可能还要等一会才上来与之对应的码流消息，该字段标记是否此种场景。
        private boolean bWaitingVideoStream;

        // 该与会方绑定的Display。与会方画面将展示在Display上。
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
        private static final int VideoState_WeakSignal = 2; // 视频信号弱（视频通道在但没有视频帧（如对端关闭了摄像头采集，停止了发送）或者视频帧率非常低（如网络状况很差导致的低帧率））
        private static final int VideoState_NoStream = 3; // 没有视频通道，如音频入会
        private static final int VideoState_RecvingAss = 4; // 正在接收辅流

        // 优选的视频质量
        private int preferredVideoQuality = RtcConfig.VideoQuality_Low;

        // 语音激励使能
        private boolean bVoiceActivated;

        Conferee(int mcuId, int terId, String e164, String alias, String email, boolean isVirtualAssStreamConferee) {
            this.mcuId = mcuId;
            this.terId = terId;
            this.id = buildId(mcuId, terId, isVirtualAssStreamConferee);
            this.e164 = e164;
            this.alias = alias;
            this.email = email;
            this.isVirtualAssStreamConferee = isVirtualAssStreamConferee;
        }

        static String buildId(int mcuId, int terId, boolean bAssStream){
            String postfix = bAssStream ? "_assStream" : "";
            return mcuId+"_"+terId+postfix;
        }

        void addDisplay(Display display) {
            if (null != display) {
                // 前置摄像头场景下本地回显需镜像显示
                display.setMirror(null != e164 && e164.equals(instance.userE164) && instance.config.isFrontCameraPreferred);

                KLog.p("add Display %s to conferee %s", display, id);
                displays.add(display);

                adjustVideoQuality();
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
            adjustVideoQuality();
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
            KLog.p("add text deco %s", decoration.id);
            textDecorations.add(decoration);
            refreshDisplays();
        }

        /**
         * 添加图片deco
         * */
        public void addPic(PicDecoration decoration){
            KLog.p("add pic deco %s", decoration.id);
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
         * NOTE: 不同于{@link Display#setDecoEnable(String, boolean)}只对单独的display起作用，该方法会影响到所有绑定到该Conferee的Display。
         * */
        public void setDecoEnable(String decoId, boolean enabled){
            KLog.p("decoId %s, enabled=%s", decoId, enabled);
            boolean success = false;
            for (TextDecoration deco : textDecorations){
                KLog.p("deco id=%s, enabled=%s, toSetDecoId=%s, toEnable=%s", deco.id, deco.enabled(), decoId, enabled);
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
         * 调整视频质量。
         * 一个与会方可能会发布多路不同质量的视频码流(simulcast)，其他与会方可以根据自己需要选择。
         * 不同的使用场景要求展示的视频质量可能不一样，比如小窗口展示时我们不用高清用cif就够了，全屏展示时又需要切到高清，
         * 又比如对于关联到多个Display的与会方而言若其中一个高清的Display关闭了我们可能需要重新计算需要订阅的视频质量，可能要回落到cif
         * */
        void adjustVideoQuality(){
            int preferQuality = RtcConfig.VideoQuality_Low;
            for (Display display : displays){
                KLog.p("display %s preferredVideoQuality=%s", display.hashCode(), display.preferredVideoQuality);
                if (display.preferredVideoQuality > preferQuality){
                    preferQuality = display.preferredVideoQuality;
                }
            }
            KLog.p("old preferredVideoQuality=%s, new preferredVideoQuality=%s", preferredVideoQuality, preferQuality);
            if (preferQuality != preferredVideoQuality) {
                preferredVideoQuality = preferQuality;
                // 重新订阅
                instance.subscribeStreams();
            }
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


        private long timestamp = System.currentTimeMillis();
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
                        KLog.p("frame of conferee %s(%s) rendered onto display %s ", id, e164, display.hashCode());
                    }else{
                        KLog.p("frame of conferee %s(%s) dropped off display %s because it is disabled ", id, e164, display.hashCode());
                    }
                }
                if (!display.enabled) {
                    continue;
                }
                display.onFrame(videoFrame);
            }
        }


        public String getId() {
            return id;
        }

        public int getMcuId() {
            return mcuId;
        }

        public int getTerId() {
            return terId;
        }

        public String getE164() {
            return e164;
        }

        public String getAlias() {
            return alias;
        }

        public String getEmail() {
            return email;
        }

        public boolean isVirtualAssStreamConferee() {
            return isVirtualAssStreamConferee;
        }

        public boolean isSendingAssStream() {
            return isSendingAssStream;
        }

        @Override
        public int compareTo(Conferee o) {
            if (null == o){
                return 1;
            }
            String self = null!=alias ? alias : null!=e164 ? e164 : null!=email ? email : "";
            String other = null!=o.alias ? o.alias : null!=o.e164 ? o.e164 : null!=o.email ? o.email : "";
            return self.compareTo(other);
        }

    }



    /**
     * 用于展示与会方的控件。
     * */
    public final static class Display extends SurfaceViewRenderer{
        /**
         * 是否使能。
         * 若使能则正常显示内容，否则内容不显示。
         * */
        private boolean enabled = true;

        /**
         * Display对应的与会方。
         * 一个Display只会绑定到一个与会方；
         * 多个Display可以绑定到同一与会方；
         * */
        private Conferee conferee;

        /**
         * 禁止显示的deco集合
         * */
        private Set<String> disabledDecos = new HashSet<>();

        /**
         * 视频质量偏好。
         * 一个与会方“可能”有高中低三种不同质量的视频流，该字段用于指定该Display“倾向于”展示哪种质量的视频流。
         * */
        private int preferredVideoQuality = RtcConfig.VideoQuality_Low;

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
         * @param conferee 与会方。若为null或一个不存在于会议中的Conferee，界面效果等同没绑定。
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
         * 设置视频质量偏好。
         * @param quality 视频质量{@link RtcConfig#VideoQuality_High}{@link RtcConfig#VideoQuality_Medium}{@link RtcConfig#VideoQuality_Low}
         * 越高展示的该与会方的图像质量越高。
         * */
        public void setPreferredVideoQuality(int quality){
            KLog.p("display %s change preferredVideoQuality from %s to %s", this.hashCode(), preferredVideoQuality, quality);
            if (preferredVideoQuality != quality){
                preferredVideoQuality = quality;
                if (null != conferee){
                    conferee.adjustVideoQuality();
                }
            }
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

//            KLog.p("onDraw, displayWidth=%s, displayHeight=%s", displayWidth, displayHeight);

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
                if (deco.bgPaint.getColor() != 0) {
                    // 绘制文字背景色
                    canvas.drawRect(deco.bgRect, deco.bgPaint);
                }
                KLog.p("draw text deco: id=%s", deco.id);
                canvas.drawText(deco.text, deco.actualX, deco.actualY, deco.paint);
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
        public String text;     // 要展示的文字内容
        private int textSize;   // 文字大小（UCD标注的，实际展示的大小会依据Display的大小调整）
        private Paint bgPaint = new Paint();
        private RectF bgRect = new RectF();  // 文字背景区域
        private static final int minTextSizeLimit = 35;
        /**
         * @param id deco的id，唯一标识该deco，用户自定义。
         * @param text deco文字内容
         * @param textSize 文字大小
         * @param color 文字颜色
         * @param w UCD标注的deco所在窗口的宽
         * @param h UCD标注的deco所在窗口的高
         * @param dx UCD标注的deco相对于refPos与窗口的x方向边距
         * @param dy UCD标注的deco相对于refPos与窗口的y方向边距
         * @param refPos 计算dx,dy的相对位置。取值{@link #POS_LEFTTOP},{@link #POS_LEFTBOTTOM},{@link #POS_RIGHTTOP},{@link #POS_RIGHTBOTTOM}
         * @param backgroundColor text背景色。
         * 示例：
         * 有UCD的标注图如下：
         * Window 1920*1080
         * -----------------------
         * |
         * |-- 80px -|caption| textSize=20px, color=0xFF00FF00
         * |           |
         * |         100px
         * |          |
         * |-------------------
         *  则调用该接口时各参数传入情况：
         *  new TextDecoration("decoId", "caption", 20, 0xFF00FF00, 1920, 1080, 80, 100, POS_LEFTBOTTOM);
         * */
        public TextDecoration(@NonNull String id, @NonNull String text, int textSize, int color, int w, int h, int dx, int dy, int refPos, int backgroundColor) {
            super(id, w, h, dx, dy, refPos);
            this.text = text;
            this.textSize = textSize;
            paint.setStyle(Paint.Style.FILL);
            paint.setAntiAlias(true);
            paint.setColor(color);
            bgPaint.setStyle(Paint.Style.FILL);
            bgPaint.setColor(backgroundColor);

            paint.setTextSize(textSize);
            Paint.FontMetrics fm = paint.getFontMetrics();
            float textLength = paint.measureText(text);
            if (POS_LEFTTOP == refPos){
                y -= fm.top; // y坐标对齐至text的baseline。(drawText以baseline为基准，而非左上角)
            }else if (POS_LEFTBOTTOM == refPos){
                y -= fm.bottom; // y坐标对齐至text的baseline。
            }else if (POS_RIGHTTOP == refPos){
                x -= textLength;
                y -= fm.top;
            }else {
                x -= textLength;
                y -= fm.bottom;
            }
        }

        public TextDecoration(@NonNull String id, @NonNull String text, int textSize, int color, int w, int h, int dx, int dy, int refPos) {
            this(id, text, textSize, color, w, h, dx, dy, refPos, 0);
        }



        protected boolean adjust(int width, int height){
            if (!super.adjust(width, height)){
                return false;
            }

            // 根据实际窗体宽高计算适配后的字体大小
            float size = Math.max(minTextSizeLimit, textSize * MatrixHelper.getMinScale(matrix));
            paint.setTextSize(size);
            int xPadding = 4;
            int yPadding = 6;
            // 修正实际锚点坐标。
            // 为防止字体缩的过小我们限定了字体大小下限，我们需修正由此可能带来的偏差。
            // NOTE: 此修正目前仅针对文字横排的情形，若将来增加文字竖排的需求，需在此增加相应的处理逻辑。
            Paint.FontMetrics fm = paint.getFontMetrics();
            if (POS_LEFTBOTTOM==refPos || POS_RIGHTBOTTOM==refPos) {
                actualY = Math.min(height-fm.bottom-yPadding, actualY);
            }else {
                actualY = Math.max(0-fm.top+yPadding, actualY);
            }

            // 计算文字背景区域
            float left = actualX-xPadding;
            float right = actualX+paint.measureText(text)+xPadding;
            float top = actualY+fm.top;
            float bottom = actualY+fm.bottom;
            bgRect.set(left, top, right, bottom);

//            KLog.p("finish adjust: width=%s, height=%s, x=%s, y=%s, text=%s, textSize=%s," +
//                            "actualX=%s, actualY=%s, actualTextSize=%s, backgroundRect=(%s,%s,%s,%s)",
//                    width, height, x,y,text, textSize, actualX, actualY, size, left, top, right, bottom);

            return true;
        }

    }

    public static class PicDecoration extends Decoration{
        public Bitmap pic;
        public PicDecoration(@NonNull String id, @NonNull Bitmap pic, int w, int h, int dx, int dy, int refPos) {
            super(id, w, h, dx, dy, refPos);
            this.pic = pic;
            paint.setStyle(Paint.Style.STROKE);

            float picW = pic.getWidth();
            float picH = pic.getHeight();
            if (POS_LEFTBOTTOM == refPos){
                y -= picH;
            }else if (POS_RIGHTTOP == refPos){
                x -= picW;
            }else if (POS_RIGHTBOTTOM == refPos){
                x -= picW;
                y -= picH;
            }
        }

        static PicDecoration createCenterPicDeco(String id, Bitmap bitmap, int winW, int winH){
            int bmW = bitmap.getWidth();
            int bmH = bitmap.getHeight();
            winW = Math.max(winW, bmW);
            winH = Math.max(winH, bmH);
            return new PicDecoration(id, bitmap, winW, winH, (int)((winW-bmW)/2f), (int)((winH-bmH)/2f), Decoration.POS_LEFTTOP);
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
        // 相对窗体的位置
        public static final int POS_LEFTTOP = 1;
        public static final int POS_LEFTBOTTOM = 2;
        public static final int POS_RIGHTTOP = 3;
        public static final int POS_RIGHTBOTTOM = 4;

        private boolean enabled = true; // 是否使能。使能则展示否则不展示

        public String id;
        public int w;           // deco所在窗体的宽（UCD标注的）
        public int h;           // deco所在窗体的高（UCD标注的）
        public int dx;          // deco到窗体垂直边界的距离（参照pos）（UCD标注的）
        public int dy;          // deco到窗体水平边界的距离（参照pos）（UCD标注的）
        public int refPos;      // dx, dy参照的位置。如取值{@link #POS_LEFTBOTTOM}则dx表示距离窗体左边界的距离，dy表示距离窗体底部边界的距离。
        protected Paint paint;
        // 根据以上ucd标注计算出的deco绘制的锚点。
        // NOTE: 此锚点仅为ucd标注窗体中的锚点，并非实际绘制的锚点，实际的锚点根据该锚点结合实际窗体大小计算得出。
        protected float x;
        protected float y;

        // deco实际的锚点
        protected float actualX;
        protected float actualY;

        protected Matrix matrix = new Matrix();

        protected float preWidth;
        protected float preHeight;


        protected Decoration(String id, int w, int h, int dx, int dy, int refPos) {
            this.id = id;
            this.w = w;
            this.h = h;
            this.dx = dx;
            this.dy = dy;
            this.refPos = refPos;
            this.paint = new Paint();

            if (POS_LEFTTOP == refPos){
                x = dx;
                y = dy;
            }else if (POS_LEFTBOTTOM == refPos){
                x = dx;
                y = h - dy;
            }else if (POS_RIGHTTOP == refPos){
                x = w - dx;
                y = dy;
            }else{
                x = w - dx;
                y = h - dy;
            }

        }

        protected void setEnabled(boolean enabled){
            this.enabled = enabled;
        }

        protected boolean enabled(){
            return enabled;
        }

        /**
         * 根据实际窗口的大小调整deco
         * @param width 实际窗口的宽
         * @param height 实际窗口的高
         * @return */
        protected boolean adjust(int width, int height){
            if (width<=0 || height<=0){
                return false;
            }
            if (preWidth == width && preHeight == height){
                return false;
            }
            preWidth = width;
            preHeight = height;

            matrix.reset();
            matrix.postTranslate(x, y);
            matrix.postScale(width/(float)w, height/(float)h, 0, 0);
            float[] cor = new float[2];
            matrix.mapPoints(cor);
            actualX = cor[0]; actualY = cor[1];

//            KLog.p("displayW=%s, displayH=%s, x=%s, y=%s, matrix=%s, actualX=%s, actualY=%s",
//                    width, height, x, y, matrix, actualX, actualY);

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

    private Set<VideoStream> getAllVideoStreamsExceptMine(){
        Set<VideoStream> videoStreams = new HashSet<>();
        for (Conferee conferee : conferees){
            if (null != conferee.e164 && conferee.e164.equals(userE164)){
                continue;
            }
            if (null != conferee.videoStream) videoStreams.add(conferee.videoStream);
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

    private Conferee findAssStreamConferee(){
        for (Conferee conferee : conferees){
            if (conferee.isVirtualAssStreamConferee){
                return conferee;
            }
        }
        return null;
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
            PeerConnectionWrapper pcWrapper = getPcWrapper(connType);
            if (null == pcWrapper || !pcWrapper.checkSdpState(pcWrapper.Idle)) return;
            for (RtcConnector.TRtcMedia rtcMedia : rtcMediaList) {
                KLog.p("mid=%s, kdstreamId=%s", rtcMedia.mid, rtcMedia.streamid);
                pcWrapper.mapMid2KdStreamId(rtcMedia.mid, rtcMedia.streamid);
            }

            pcWrapper.setSdpType(pcWrapper.Answer);
            String sdp = modifySdp(pcWrapper, offerSdp);
            pcWrapper.setRemoteDescription(new SessionDescription(SessionDescription.Type.OFFER, sdp));
            pcWrapper.setSdpState(pcWrapper.SettingRemote);
        }

        @Override
        public void onSetAnswerCmd(int connType, String answerSdp) {
            KLog.p("connType=%s", connType);
            PeerConnectionWrapper pcWrapper = getPcWrapper(connType);
            if (null == pcWrapper || !pcWrapper.checkSdpState(pcWrapper.Sending)) return;
            String sdp = modifySdp(pcWrapper, answerSdp);
            pcWrapper.setRemoteDescription(new SessionDescription(SessionDescription.Type.ANSWER, sdp));
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

    }


    private String modifySdp(PeerConnectionWrapper pcWrapper, String origSdp){
        // 根据音视频编码偏好修改sdp
        String sdpVideoCodecName = getSdpVideoCodecName(config.preferredVideoCodec);
        if (pcWrapper.isSdpType(pcWrapper.Offer) || pcWrapper.isSdpType(pcWrapper.Answer)){
            origSdp = SdpHelper.preferCodec(origSdp, config.preferredAudioCodec, true);
            origSdp = SdpHelper.preferCodec(origSdp, sdpVideoCodecName, false);
        }else if (pcWrapper.isSdpType(pcWrapper.AudioOffer)){
            origSdp = SdpHelper.preferCodec(origSdp, config.preferredAudioCodec, true);
        }else if (pcWrapper.isSdpType(pcWrapper.VideoOffer)){
            origSdp = SdpHelper.preferCodec(origSdp, sdpVideoCodecName, false);
        }
        return origSdp;
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
                    String sdp = modifySdp(pcWrapper, origSdp.description);
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
                        executor.execute(() -> {
                            for (String mid : mids) {
                                String streamId = pcWrapper.mid2KdStreamIdMap.get(mid);
                                KLog.p("mid=%s, streamId=%s", mid, streamId);
                                if (null == streamId) {
                                    KLog.p(KLog.ERROR, "no streamId for mid %s (see onSetOfferCmd)", mid);
                                }
                                rtcMediaList.add(new RtcConnector.TRtcMedia(streamId, mid)); // 仅answer需要填streamId，answer不需要填encodings
                            }
                            sessionHandler.post(() -> rtcConnector.sendAnswerSdp(pcWrapper.connType, pc.getLocalDescription().description, rtcMediaList));
                        });
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
                KLog.p("connType %s received remote track %s", connType, track != null ? track.id() : null);
                PeerConnectionWrapper pcWrapper = getPcWrapper(connType);
                if (null == pcWrapper){
                    KLog.p(KLog.ERROR, "null == pcWrapper(connType=%s)", connType);
                    return;
                }

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

        // WebRTC的mid到平台的StreamId之间的映射。mid仅在一个PeerConnection内唯一
        private final BiMap<String, String> mid2KdStreamIdMap = HashBiMap.create();

        // 是否正在取消发布。由于协议组当前实现所限取消发布后我们需要重建PeerConnection
        private boolean isUnpublishing;

        PeerConnectionWrapper(int connType, @NonNull SDPObserver sdpObserver) {
            this.connType = connType;
            this.sdpObserver = sdpObserver;
        }

        PeerConnectionWrapper(int connType, @NonNull PeerConnection pc, @NonNull SDPObserver sdpObserver) {
            this.connType = connType;
            this.pc = pc;
            this.sdpObserver = sdpObserver;
        }

        void setPeerConnection(@NonNull PeerConnection pc){
            this.pc = pc;
        }


        void createOffer(){
            executor.execute(() -> {
                if (null == pc){
                    KLog.p(KLog.ERROR, "peerConnection destroyed");
                    return;
                }
                pc.createOffer(sdpObserver, new MediaConstraints());
            });
        }
        void createAnswer(){
            executor.execute(() -> {
                if (null == pc){
                    KLog.p(KLog.ERROR, "peerConnection destroyed");
                    return;
                }
                pc.createAnswer(sdpObserver, new MediaConstraints());
            });
        }
        void setLocalDescription(SessionDescription sdp){
            executor.execute(() -> {
                if (null == pc){
                    KLog.p(KLog.ERROR, "peerConnection destroyed");
                    return;
                }
                pc.setLocalDescription(sdpObserver, sdp);
            });
        }
        void setRemoteDescription(SessionDescription sdp){
            executor.execute(() -> {
                if (null == pc){
                    KLog.p(KLog.ERROR, "peerConnection destroyed");
                    return;
                }
                pc.setRemoteDescription(sdpObserver, sdp);
            });
        }

        void mapMid2KdStreamId(String mid, String kdStreamId){
            executor.execute(() -> mid2KdStreamIdMap.put(mid, kdStreamId));
        }


        /**
         * 创建视频轨道。
         * */
        void createVideoTrack(@NonNull VideoCapturer videoCapturer){
            executor.execute(() -> {
                if (null == factory){
                    KLog.p(KLog.ERROR, "factory destroyed");
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
                    bTrackEnable = config.isLocalVideoEnabled;
                }
                if (bTrackEnable) {
                    // 仅本地摄像头开启状态下开启采集
                    videoCapturer.startCapture(config.videoWidth, config.videoHeight, config.videoFps);
                }
                localVideoTrack = factory.createVideoTrack(localVideoTrackId, videoSource);
                localVideoTrack.setEnabled(bTrackEnable);

                boolean bSimulcast = config.isSimulcastEnabled;
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
                            encoding.maxBitrateBps = config.videoMaxBitrate/2;
                        }else if (encoding.rid.equals("l")){
                            encoding.scaleResolutionDownBy = 0.25;
                            encoding.maxFramerate = config.videoFps;
                            encoding.maxBitrateBps = config.videoMaxBitrate/4;
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
                                if (null == localVideoTrack){
                                    KLog.p(KLog.ERROR, "what's wrong? pc destroyed?");
                                    return;
                                }
                                localVideoTrack.addSink(myself);  // 本地回显
                            });
                            // 检查摄像头是否屏蔽，若屏蔽则展示静态图片
                            myself.setState(config.isLocalVideoEnabled ? Conferee.VideoState_Normal : Conferee.VideoState_Disabled);
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
                if (null == pc){
                    KLog.p(KLog.ERROR, "peerConnection destroyed");
                    return;
                }
                pc.removeTrack(videoSender);
                String trackId = localVideoTrack.id();
                sessionHandler.post(() -> {
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
            executor.execute(() -> {
                String kdStreamId = mid2KdStreamIdMap.get(mid);
                if (null == kdStreamId) {
                    KLog.p(KLog.ERROR, "no register stream for mid %s in signaling progress(see onSetOfferCmd)", mid);
                    return;
                }
                remoteVideoTracks.put(kdStreamId, track);
                track.setEnabled(config.isRemoteVideoEnabled);

                sessionHandler.post(() -> {
                    kdStreamId2RtcTrackIdMap.put(kdStreamId, track.id());
                    Conferee conferee = findConfereeByStreamId(kdStreamId);
                    KLog.p("create remote video track(trackId=%s, kdStreamId=%s, mid=%s) for conferee %s", track.id(), kdStreamId, mid, conferee != null ? conferee.e164 : null);
                    if (null == conferee) {
                        KLog.p(KLog.ERROR, "something wrong? stream %s not belong to any conferee", kdStreamId);
                        return;
                    }
                    executor.execute(() -> track.addSink(conferee));
                });
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
                if (null == factory){
                    KLog.p(KLog.ERROR, "factory destroyed");
                    return;
                }
                audioSource = factory.createAudioSource(new MediaConstraints());
                String localAudioTrackId = LOCAL_AUDIO_TRACK_ID+audioTrackCnt++;
                localAudioTrack = factory.createAudioTrack(localAudioTrackId, audioSource);
                localAudioTrack.setEnabled(!config.isMuted);
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
                if (null == pc){
                    KLog.p(KLog.ERROR, "peerConnection destroyed");
                    return;
                }
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
            executor.execute(() -> {
                String kdStreamId = mid2KdStreamIdMap.get(mid);
                KLog.p("mid=%s, streamId=%s", mid, kdStreamId);
                if (null == kdStreamId) {
                    KLog.p(KLog.ERROR, "no register stream for mid %s in signaling progress(see onSetOfferCmd)", mid);
                    return;
                }
                track.setEnabled(!config.isSilenced);
                remoteAudioTracks.put(kdStreamId, track);
                sessionHandler.post(() -> {
                    kdStreamId2RtcTrackIdMap.put(kdStreamId, track.id());
                    Conferee conferee = findConfereeByStreamId(kdStreamId);
                    KLog.p("create remote audio track(trackId=%s, kdStreamId=%s, mid=%s) for conferee %s", track.id(), kdStreamId, mid, conferee != null ? conferee.e164 : null);
                    if (null == conferee) {
                        KLog.p(KLog.ERROR, "something wrong? stream %s not belong to any conferee", kdStreamId);
                    }
                });
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
                if (null == pc){
                    KLog.p(KLog.ERROR, "peerConnection destroyed");
                    return;
                }
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
                if (null == pc){
                    KLog.p(KLog.ERROR, "peerConnection destroyed");
                    return;
                }
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
                if (null == pc){
                    KLog.p(KLog.ERROR, "peerConnection destroyed");
                    return;
                }
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

                mid2KdStreamIdMap.clear();

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

                KLog.p("pcWrapper (connType=%s) closed", connType);
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
                        KLog.p("resume capture");
                        videoCapturer.startCapture(config.videoWidth, config.videoHeight, config.videoFps);
                    }else{
                        try {
                            KLog.p("pause capture");
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
                    synchronized (publisherStats) {
                        StatsHelper.resolveStats(rtcStatsReport, publisherStats);
                        KLog.p("####publisherStats: ");
                        printStats(publisherStats);
                    }
                });
            }
            if (null != subPcWrapper && null != subPcWrapper.pc) {
                subPcWrapper.pc.getStats(rtcStatsReport -> {
                    synchronized (subscriberStats) {
                        StatsHelper.resolveStats(rtcStatsReport, subscriberStats);
                        KLog.p("####subscriberStats: ");
                        printStats(subscriberStats);
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
            String maxAudioLevelTrackId = null;
            double maxAudioLevel = 0;
            if (!config.isMuted) { // 非哑音状态我们才将己端音量纳入统计范畴（按理说并不需要这个条件判断，我理解哑音状态下己端音量应该为0才对，但是实测并不是）
                // 己端的音量
                synchronized (publisherStats) {
                    maxAudioLevelTrackId = null != publisherStats.audioSource ? publisherStats.audioSource.trackIdentifier : null;
                    maxAudioLevel = null != publisherStats.audioSource ? publisherStats.audioSource.audioLevel : 0;
                    KLog.p("my audioLevel= %s", maxAudioLevel);
                }
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
            if (null != maxAudioLevelTrackId
                    && maxAudioLevel > 0.1 // 大于0.1才认为是人说话，否则认为是环境噪音
            ){
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
    private static final int WeakSignalCheckInterval = 5000; // 单位：毫秒
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
                    long interval = curTimestamp - videoStatsTimeStamp;
                    float fps = (curReceivedFrames - preReceivedFrames) / (interval/1000f);
                    if (!conferee.bWaitingVideoStream
                            && interval >= WeakSignalCheckInterval // 计算帧率的间隔时长应适度，太短则太敏感地滑入WeakSignal状态，太长则太迟钝
                            && fps < 0.2 // 可忍受的帧率下限，低于该下限则认为信号丢失
                    ){
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

            if (curTimestamp - videoStatsTimeStamp >= WeakSignalCheckInterval) {
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


    private void printStats(StatsHelper.Stats stats){
        KLog.p(">>>>>>>>>> stats begin");
        // 因为android log一次输出有最大字符限制，所以我们分段输出
        KLog.p("---------- audio");
        if (null != stats.audioSource) {
            KLog.p(stats.audioSource.toString());
        }
        if (null != stats.sendAudioTrack) {
            KLog.p(stats.sendAudioTrack.toString());
        }
        if (null != stats.audioOutboundRtp) {
            KLog.p(stats.audioOutboundRtp.toString());
        }
        if (null != stats.recvAudioTrackList) {
            for (StatsHelper.RecvAudioTrack audioTrack : stats.recvAudioTrackList) {
                String kdStreamId = kdStreamId2RtcTrackIdMap.inverse().get(audioTrack.trackIdentifier);
                Conferee conferee = findConfereeByStreamId(kdStreamId);
                KLog.p(conferee != null ? conferee.alias+" " + audioTrack : audioTrack.toString());
            }
        }
        if (null != stats.audioInboundRtpList) {
            for (StatsHelper.AudioInboundRtp audioInbound : stats.audioInboundRtpList) {
                KLog.p(audioInbound.toString());
            }
        }

        KLog.p("---------- video");
        if (null != stats.videoSource) {
            KLog.p(stats.videoSource.toString());
        }
        if (null != stats.sendVideoTrack) {
            KLog.p(stats.sendVideoTrack.toString());
        }
        if (null != stats.videoOutboundRtp) {
            KLog.p(stats.videoOutboundRtp.toString());
        }
        if (null != stats.recvVideoTrackList) {
            for (StatsHelper.RecvVideoTrack videoTrack : stats.recvVideoTrackList) {
                String kdStreamId = kdStreamId2RtcTrackIdMap.inverse().get(videoTrack.trackIdentifier);
                Conferee conferee = findConfereeByStreamId(kdStreamId);
                KLog.p(conferee != null ? conferee.alias+" " + videoTrack : videoTrack.toString());
            }
        }
        if (null != stats.videoInboundRtpList) {
            for (StatsHelper.VideoInboundRtp videoInbound : stats.videoInboundRtpList) {
                KLog.p(videoInbound.toString());
            }
        }

        KLog.p("---------- codec");
        if (null != stats.encoderList) {
            for (StatsHelper.Codec codec : stats.encoderList) {
                KLog.p(codec.toString());
            }
        }
        if (null != stats.decoderList) {
            for (StatsHelper.Codec codec : stats.decoderList) {
                KLog.p(codec.toString());
            }
        }

        KLog.p(">>>>>>>>>> stats end");
    }

}
