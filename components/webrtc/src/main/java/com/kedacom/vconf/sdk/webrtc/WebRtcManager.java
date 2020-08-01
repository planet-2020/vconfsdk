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
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.view.SurfaceHolder;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.kedacom.vconf.sdk.amulet.Caster;
import com.kedacom.vconf.sdk.amulet.ILifecycleOwner;
import com.kedacom.vconf.sdk.amulet.IResultListener;
import com.kedacom.vconf.sdk.common.bean.transfer.TMtEntityStatus;
import com.kedacom.vconf.sdk.common.bean.transfer.TRegResultNtf;
import com.kedacom.vconf.sdk.common.bean.transfer.TSrvStartResult;
import com.kedacom.vconf.sdk.common.constant.EmConfProtocol;
import com.kedacom.vconf.sdk.common.constant.EmMtAliasType;
import com.kedacom.vconf.sdk.common.constant.EmMtCallDisReason;
import com.kedacom.vconf.sdk.common.constant.EmMtChanState;
import com.kedacom.vconf.sdk.common.constant.EmMtResolution;
import com.kedacom.vconf.sdk.common.type.BaseTypeBool;
import com.kedacom.vconf.sdk.common.type.BaseTypeInt;
import com.kedacom.vconf.sdk.common.type.vconf.TAssVidStatus;
import com.kedacom.vconf.sdk.common.type.vconf.TMtAlias;
import com.kedacom.vconf.sdk.common.type.vconf.TMtAssVidStatusList;
import com.kedacom.vconf.sdk.common.type.vconf.TMtCallLinkSate;
import com.kedacom.vconf.sdk.utils.log.KLog;
import com.kedacom.vconf.sdk.utils.math.MatrixHelper;
import com.kedacom.vconf.sdk.webrtc.CommonDef.ConnType;
import com.kedacom.vconf.sdk.webrtc.CommonDef.MediaType;
import com.kedacom.vconf.sdk.webrtc.bean.ConfInfo;
import com.kedacom.vconf.sdk.webrtc.bean.ConfInvitationInfo;
import com.kedacom.vconf.sdk.webrtc.bean.ConfPara;
import com.kedacom.vconf.sdk.webrtc.bean.CreateConfResult;
import com.kedacom.vconf.sdk.webrtc.bean.MakeCallResult;
import com.kedacom.vconf.sdk.webrtc.bean.trans.TCreateConfResult;
import com.kedacom.vconf.sdk.webrtc.bean.trans.TMTEntityInfo;
import com.kedacom.vconf.sdk.webrtc.bean.trans.TMTEntityInfoList;
import com.kedacom.vconf.sdk.webrtc.bean.trans.TMtId;
import com.kedacom.vconf.sdk.webrtc.bean.trans.TMtRtcSvrAddr;
import com.kedacom.vconf.sdk.webrtc.bean.trans.TQueryConfInfoResult;
import com.kedacom.vconf.sdk.webrtc.bean.trans.TRtcPlayItem;
import com.kedacom.vconf.sdk.webrtc.bean.trans.TRtcPlayParam;
import com.kedacom.vconf.sdk.webrtc.bean.trans.TRtcStreamInfo;
import com.kedacom.vconf.sdk.webrtc.bean.trans.TRtcStreamInfoList;

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
import org.webrtc.voiceengine.WebRtcAudioUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


@SuppressWarnings({"unused", "WeakerAccess"})
public class WebRtcManager extends Caster<Msg>{

    private static final String TAG = WebRtcManager.class.getSimpleName();

    private static WebRtcManager instance;

    private Application context;

    private RtcConnector rtcConnector = new RtcConnector();

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private EglBase eglBase;
    private PeerConnectionFactory factory;
    private PeerConnectionWrapper pubPcWrapper;
    private PeerConnectionWrapper subPcWrapper;
    private PeerConnectionWrapper assPubPcWrapper;
    private PeerConnectionWrapper assSubPcWrapper;

    // 己端与会方
    private Conferee myself;

    // 与会方集合（不包含己端）
    private Set<Conferee> conferees = new LinkedHashSet<>();

    // 码流集合（没有己端的码流）
    private Set<RtcStream> streams = new HashSet<>();

    private Set<Display> displays = new LinkedHashSet<>();

    // 平台的StreamId到WebRTC的TrackId之间的映射
    private BiMap<String, String> kdStreamId2RtcTrackIdMap = HashBiMap.create();

    // 当前用户的e164
    private String userE164;

    // RTC配置
    private RtcConfig.Config config = new RtcConfig.Config();

    private Handler handler = new Handler(Looper.getMainLooper()){
        @Override
        public void handleMessage(Message msg) {
        }
    };



    private WebRtcManager(Application context){
        this.context = context;
    }


    public synchronized static WebRtcManager getInstance(@NonNull Application context){
        if (null == instance){
            instance = new WebRtcManager(context);
            instance.startService();
        }
        return instance;
    }

    // 启动业务组件webrtc服务
    private void startService(){
        String serviceName = "mtrtcservice";
        req(Msg.StartMtService, new SessionProcessor<Msg>() {
            @Override
            public void onRsp(Msg rsp, Object rspContent, IResultListener resultListener, Msg req, Object[] reqParas, boolean[] isConsumed) {
                TSrvStartResult result = (TSrvStartResult) rspContent;
                if (!result.AssParam.achSysalias.equals(serviceName)){
                    isConsumed[0] = false;
                    return;
                }
                boolean success = result.MainParam.basetype;
                if (success){
                    KLog.p("start %s service success!", serviceName);
                }
            }
        }, null , serviceName);
    }


    /**
     * 登录rtc
     * 注意，需先登录aps成功。
     * @param e164 用户e164号
     * @param resultListener onSuccess null
     *                       onFailed {@link RtcResultCode#NetworkUnreachable}
     *                                {@link RtcResultCode#UnknownServerAddress}
     *                                {@link RtcResultCode#AlreadyLoggedIn}
     * @param confEventListener 会议事件监听器
     * */
    public void login(@NonNull String e164, @NonNull IResultListener resultListener, @NonNull ConfEventListener confEventListener){
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
        req(Msg.Login, new SessionProcessor<Msg>() {
            @Override
            public void onRsp(Msg rsp, Object rspContent, IResultListener resultListener, Msg req, Object[] reqParas, boolean[] isConsumed) {
                TRegResultNtf loginResult = (TRegResultNtf) rspContent;
                int resCode = RtcResultCode.trans(rsp, loginResult.AssParam.basetype);
                if (RtcResultCode.LoggedIn == resCode) {
                    reportSuccess(null, resultListener);
                } else {
                    reportFailed(resCode, resultListener);
                }
            }
        }, resultListener, rtcSvrAddr);
    }


    /**
     * 注销rtc
     * @param resultListener onSuccess null
     *                       onFailed errorCode // TODO
     * */
    public void logout(IResultListener resultListener){
        this.confEventListener = null;
        userE164 = null;
        stopSession();
        req(Msg.Logout, new SessionProcessor<Msg>() {
            @Override
            public void onRsp(Msg rsp, Object rspContent, IResultListener resultListener, Msg req, Object[] reqParas, boolean[] isConsumed) {
                TRegResultNtf loginResult = (TRegResultNtf) rspContent;
                int resCode = RtcResultCode.trans(rsp, loginResult.AssParam.basetype);
                if (RtcResultCode.LoggedOut == resCode) {
                    reportSuccess(null, resultListener);
                } else {
                    reportFailed(resCode, resultListener);
                }
            }
        }, resultListener, new TMtRtcSvrAddr(false));
    }


    /**
     * 呼叫
     * NOTE：目前只支持同时开一个会。如果呼叫中/创建中或会议中状态，则返回失败，需要先退出会议状态。
     * @param peerId 对于点对点而言是对端e164，对于多方会议而言是会议e164
     * @param bAudio 是否音频方式入会
     * @param resultListener onSuccess {@link MakeCallResult}
     *                       onFailed {@link RtcResultCode#NotLoggedInYet}
     *                                {@link RtcResultCode#ConfereeNumReachLimit}
     * @param sessionEventListener 会话事件监听器
     * */
    public void makeCall(String peerId, boolean bAudio, @NonNull IResultListener resultListener, @NonNull SessionEventListener sessionEventListener){
        if (!startSession(sessionEventListener)){
            reportFailed(-1, resultListener);
            return;
        }
        req(Msg.Call, new SessionProcessor<Msg>() {
                    @Override
                    public void onRsp(Msg rsp, Object rspContent, IResultListener resultListener, Msg req, Object[] reqParas, boolean[] isConsumed) {
                        switch (rsp){
                            case Calling:
                                if (EmConfProtocol.emrtc != ((TMtCallLinkSate) rspContent).emConfProtocol){
                                    isConsumed[0] = false;
                                }
                                break;
                            case MultipartyConfStarted:
                                reportSuccess(ToDoConverter.callLinkState2MakeCallResult((TMtCallLinkSate) rspContent), resultListener);
                                break;
                            case ConfCanceled:
                                stopSession();
                                reportFailed(RtcResultCode.trans(rsp, ((BaseTypeInt) rspContent).basetype), resultListener);
                                break;
                        }
                    }

                    @Override
                    public void onTimeout(IResultListener resultListener, Msg req, Object[] reqParas, boolean[] isConsumed) {
                        stopSession();
                    }
                },
                resultListener, peerId,
                bAudio ? 64 : 1024 * 4, // 音频入会64K码率，视频入会4M
                EmConfProtocol.emrtc
        );
    }


    /**
     * 创建会议
     * NOTE：目前只支持同时开一个会。如果呼叫中/创建中或会议中状态，则返回失败，需要先退出会议状态。
     * @param confPara 创会参数
     * @param resultListener onSuccess {@link CreateConfResult}
     *                       onFailed {@link RtcResultCode#InstantConfDenied}
     * @param sessionEventListener 会话事件监听器
     * */
    public void createConf(ConfPara confPara, @NonNull IResultListener resultListener, @NonNull SessionEventListener sessionEventListener){
        if (!startSession(sessionEventListener)){
            reportFailed(-1, resultListener);
            return;
        }
        if (confPara.bSelfAudioMannerJoin){
            // 音频入会则关闭己端主视频通道。底层上报onGetOfferCmd时带的媒体类型就为Audio了。
            req(Msg.CloseMyMainVideoChannel, null, null);
        }
        req(Msg.CreateConf, new SessionProcessor<Msg>() {
            @Override
            public void onRsp(Msg rsp, Object rspContent, IResultListener resultListener, Msg req, Object[] reqParas, boolean[] isConsumed) {
                switch (rsp){
                    case CreateConfRsp:
                        TCreateConfResult tCreateConfResult = (TCreateConfResult) rspContent;
                        int resCode = RtcResultCode.trans(rsp, tCreateConfResult.MainParam.dwErrorID);
                        if (RtcResultCode.Success != resCode){
                            stopSession();
                            cancelReq(req, resultListener);
                            reportFailed(resCode, resultListener);
                        }
                        break;
                    case MultipartyConfStarted:
                        reportSuccess(ToDoConverter.callLinkState2CreateConfResult( (TMtCallLinkSate) rspContent), resultListener);
                        break;
                    case ConfCanceled:
                        stopSession();
                        reportFailed(RtcResultCode.trans(rsp, ((BaseTypeInt) rspContent).basetype), resultListener);
                        break;
                }
            }

            @Override
            public void onTimeout(IResultListener resultListener, Msg req, Object[] reqParas, boolean[] isConsumed) {
                stopSession();
            }
        }, resultListener, ToDoConverter.confPara2CreateConference(confPara), EmConfProtocol.emrtc);
    }


    /**
     * 退出会议。
     * @param disReason 原因码
     * @param resultListener onSuccess null
     *                       onFailed
     * */
    public void quitConf(EmMtCallDisReason disReason, IResultListener resultListener){
        if (!stopSession()){
            reportFailed(-1, resultListener);
            return;
        }
        req(Msg.QuitConf, new SessionProcessor<Msg>() {
            @Override
            public void onRsp(Msg rsp, Object rspContent, IResultListener resultListener, Msg req, Object[] reqParas, boolean[] isConsumed) {
                BaseTypeInt reason = (BaseTypeInt) rspContent;
                int resCode = RtcResultCode.trans(req, reason.basetype);
                // TODO 判断resCode
                reportSuccess(null, resultListener);
            }
        }, resultListener, disReason);
    }


    /**
     * 结束会议。
     * @param resultListener onSuccess null
     *                       onFailed
     * */
    public void endConf(IResultListener resultListener){
        if (!stopSession()){
            reportFailed(-1, resultListener);
            return;
        }
        req(Msg.EndConf, new SessionProcessor<Msg>() {
            @Override
            public void onRsp(Msg rsp, Object rspContent, IResultListener resultListener, Msg req, Object[] reqParas, boolean[] isConsumed) {
                // TODO 判断resCode
                reportSuccess(null, resultListener);
            }
        }, resultListener);
    }


    /**
     * 接受会议邀请
     * NOTE：目前只支持同时开一个会。如果呼叫中/创建中或会议中状态，则返回失败，需要先退出会议状态。
     * @param bAudio 是否音频方式入会
     * @param resultListener onSuccess {@link MakeCallResult}
     * */
    public void acceptInvitation(boolean bAudio, @NonNull IResultListener resultListener, @NonNull SessionEventListener sessionEventListener){
        if (!startSession(sessionEventListener)){
            reportFailed(-1, resultListener);
            return;
        }
        if (bAudio) {
            // 音频入会则关闭己端主视频通道。底层上报onGetOfferCmd时带的媒体类型就为Audio了。
            req(Msg.CloseMyMainVideoChannel, null, null);
        }
        req(Msg.AcceptInvitation, new SessionProcessor<Msg>() {
            @Override
            public void onRsp(Msg rsp, Object rspContent, IResultListener resultListener, Msg req, Object[] reqParas, boolean[] isConsumed) {
                reportSuccess(ToDoConverter.callLinkState2MakeCallResult((TMtCallLinkSate) rspContent), resultListener);
            }

            @Override
            public void onTimeout(IResultListener resultListener, Msg req, Object[] reqParas, boolean[] isConsumed) {
                stopSession();
            }
        }, resultListener);
    }


    /**
     * 拒绝会议邀请
     * */
    public void declineInvitation(){
        req(Msg.DeclineInvitation, null, null);
    }


    /**
     * 查询会议详情
     * @param confE164 会议e164号
     * @param resultListener onSuccess {@link ConfInfo}
     *                       onFailed
     * */
    public void queryConfInfo(String confE164, IResultListener resultListener){
        req(Msg.QueryConfInfo, new SessionProcessor<Msg>() {
            @Override
            public void onRsp(Msg rsp, Object rspContent, IResultListener resultListener, Msg req, Object[] reqParas, boolean[] isConsumed) {
                TQueryConfInfoResult queryConfInfoResult = (TQueryConfInfoResult) rspContent;
                int resCode = RtcResultCode.trans(rsp, queryConfInfoResult.MainParam.dwErrorID);
                if (RtcResultCode.Success == resCode){
                    reportSuccess(ToDoConverter.tMTInstanceConferenceInfo2ConfInfo(queryConfInfoResult.AssParam), resultListener);
                }else{
                    reportFailed(resCode, resultListener);
                }
            }
        }, resultListener, confE164);
    }


    /**
     * 验证会议密码
     * @param passwd 会议密码
     * @param resultListener onSuccess null
     *                       onFailed {@link RtcResultCode#IncorrectConfPassword}
     * */
    public void verifyConfPassword(String passwd, IResultListener resultListener){
        req(Msg.VerifyConfPassword, new SessionProcessor<Msg>() {
            @Override
            public void onRsp(Msg rsp, Object rspContent, IResultListener resultListener, Msg req, Object[] reqParas, boolean[] isConsumed) {
                switch (rsp){
                    case MyLabelAssigned:
                        reportSuccess(null, resultListener);
                        break;
                    case ConfPasswordNeeded:
                        reportFailed(RtcResultCode.IncorrectConfPassword, resultListener);
                        break;
                }
            }
        }, resultListener, passwd);
    }


    //========================================================================================

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
        req(Msg.ToggleScreenShare, new SessionProcessor<Msg>() {
            @Override
            public void onRsp(Msg rsp, Object rspContent, IResultListener resultListener, Msg req, Object[] reqParas, boolean[] isConsumed) {
                TMtAssVidStatusList assVidStatusList = (TMtAssVidStatusList) rspContent;
                if (assVidStatusList.arrTAssVidStatus.length == 0){
                    reportFailed(-1, resultListener);
                }else{
                    TAssVidStatus assVidStatus = assVidStatusList.arrTAssVidStatus[0]; // 目前仅支持一路
                    if (EmMtChanState.emChanConnected == assVidStatus.emChanState){
                        reportSuccess(null, resultListener);
                    }else{
                        screenCapturePermissionData = null;
                        reportFailed(-1, resultListener);
                    }
                }
            }

            @Override
            public void onTimeout(IResultListener resultListener, Msg req, Object[] reqParas, boolean[] isConsumed) {
                screenCapturePermissionData = null;
            }
        }, resultListener, true);
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

        req(Msg.ToggleScreenShare, null, null, false);
    }


    private View sharedWindow;
    /**
     * 开启窗口共享
     * @param window 需要共享的窗口
     * @param resultListener onSuccess null
     *                       onFailed // TODO
     * */
    public void startWindowShare(@NonNull View window, IResultListener resultListener){
        if (null != sharedWindow){
            KLog.p(KLog.WARN, "window share started already!");
            return;
        }
        sharedWindow = window;
        req(Msg.ToggleScreenShare, new SessionProcessor<Msg>() {
            @Override
            public void onRsp(Msg rsp, Object rspContent, IResultListener resultListener, Msg req, Object[] reqParas, boolean[] isConsumed) {
                TMtAssVidStatusList assVidStatusList = (TMtAssVidStatusList) rspContent;
                if (assVidStatusList.arrTAssVidStatus.length == 0){
                    reportFailed(-1, resultListener);
                }else{
                    TAssVidStatus assVidStatus = assVidStatusList.arrTAssVidStatus[0]; // 目前仅支持一路
                    if (EmMtChanState.emChanConnected == assVidStatus.emChanState){
                        reportSuccess(null, resultListener);
                    }else{
                        sharedWindow = null;
                        reportFailed(-1, resultListener);
                    }
                }
            }

            @Override
            public void onTimeout(IResultListener resultListener, Msg req, Object[] reqParas, boolean[] isConsumed) {
                sharedWindow = null;
            }
        }, resultListener, true);
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
        PeerConnectionWrapper pcWrapper = getPcWrapper(ConnType.ASS_PUBLISHER);
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
        req(Msg.ToggleScreenShare, null, null, false);
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
     * */
    public void setSilence(boolean bSilence, IResultListener resultListener){
        if(!bSessionStarted){
            KLog.p(KLog.ERROR,"session not start");
            reportFailed(-1, resultListener);
            return;
        }
        boolean hasChange = bSilence != config.isSilenced;
        if (hasChange && !doSetSilence(bSilence)) { // 立即设置而非等到响应返回，因为我们希望用户的操作能立即展现出效果，若最终设置失败我们会回滚该设置。
            reportFailed(-1, resultListener);
            return;
        }
        req(Msg.SetSilence, new SessionProcessor<Msg>() {
            @Override
            public void onRsp(Msg rsp, Object rspContent, IResultListener resultListener, Msg req, Object[] reqParas, boolean[] isConsumed) {
                BaseTypeBool result = (BaseTypeBool) rspContent;
                if (result.basetype == bSilence){ // 设置成功
                    reportSuccess(null, resultListener);
                }else{
                    if (hasChange) {
                        doSetSilence(!bSilence); // 设置失败，回滚
                    }
                    reportFailed(-1, resultListener);
                }
            }

            @Override
            public void onTimeout(IResultListener resultListener, Msg req, Object[] reqParas, boolean[] isConsumed) {
                if (hasChange) {
                    doSetSilence(!bSilence); // 设置失败，回滚
                }
            }
        }, resultListener, bSilence);
    }

    private boolean doSetSilence(boolean silence){
        PeerConnectionWrapper pcWrapper = getPcWrapper(ConnType.SUBSCRIBER);
        if (null == pcWrapper) {
            KLog.p(KLog.ERROR,"null == pcWrapper");
            return false;
        }
        pcWrapper.setRemoteAudioEnable(!silence);
        config.isSilenced = silence;
        return true;
    }

    private boolean doSetMute(boolean mute){
        PeerConnectionWrapper pcWrapper = getPcWrapper(ConnType.PUBLISHER);
        if (null == pcWrapper) {
            KLog.p(KLog.ERROR,"null == pcWrapper");
            return false;
        }
        pcWrapper.setLocalAudioEnable(!mute);
        config.isMuted = mute;
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
    public void setMute(boolean bMute, IResultListener resultListener){
        if(!bSessionStarted){
            KLog.p(KLog.ERROR,"session not start");
            reportFailed(-1, resultListener);
            return;
        }
        boolean hasChange = bMute != config.isMuted;
        if (hasChange && !doSetMute(bMute)) {
            reportFailed(-1, resultListener);
            return;
        }
        req(Msg.SetMute, new SessionProcessor<Msg>() {
            @Override
            public void onRsp(Msg rsp, Object rspContent, IResultListener resultListener, Msg req, Object[] reqParas, boolean[] isConsumed) {
                BaseTypeBool result = (BaseTypeBool) rspContent;
                if (result.basetype == bMute){ // 设置成功
                    reportSuccess(null, resultListener);
                }else{
                    if (hasChange) {
                        doSetSilence(!bMute); // 设置失败，回滚
                    }
                    reportFailed(-1, resultListener);
                }
            }

            @Override
            public void onTimeout(IResultListener resultListener, Msg req, Object[] reqParas, boolean[] isConsumed) {
                if (hasChange) {
                    doSetSilence(!bMute); // 设置失败，回滚
                }
            }
        }, resultListener, bMute);
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
        PeerConnectionWrapper pcWrapper = getPcWrapper(ConnType.PUBLISHER);
        if (null == pcWrapper) {
            KLog.p(KLog.ERROR,"null == pcWrapper");
            return false;
        }
        pcWrapper.switchCamera();  // FIXME 攝像頭可能不是在前後切換。下面的鏡像設置可能有誤
        config.isFrontCameraPreferred = !config.isFrontCameraPreferred;
        for (Display display : myself.displays){
            display.setMirror(config.isFrontCameraPreferred); // 前置摄像头情况下需镜像显示
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
            PeerConnectionWrapper pcWrapper = getPcWrapper(ConnType.PUBLISHER);
            if (null == pcWrapper) {
                KLog.p(KLog.ERROR,"null == pcWrapper");
                return false;
            }
            pcWrapper.setLocalVideoEnable(enable);
            config.isLocalVideoEnabled = enable;
            myself.refreshDisplays();
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


    @Override
    protected void onNotification(Msg ntf, java.lang.Object ntfContent, Set<ILifecycleOwner> ntfListeners) {

        switch (ntf){
            case LoginStateChanged:
                TRegResultNtf regState = (TRegResultNtf) ntfContent;
                int resCode = RtcResultCode.trans(ntf, regState.AssParam.basetype);
                if (RtcResultCode.LoggedIn != resCode) {
                    confEventListener.onConfServerDisconnected(resCode);
                }
                break;

            case CallIncoming:
                confEventListener.onConfInvitation(ToDoConverter.callLinkSate2ConfInvitationInfo((TMtCallLinkSate) ntfContent));
                break;

            case ConfPasswordNeeded:
                sessionEventListener.confPasswordNeeded();
                break;

            case MultipartyConfEnded:

            case ConfCanceled:
                stopSession();
                confEventListener.onConfFinished(RtcResultCode.trans(ntf, ((BaseTypeInt) ntfContent).basetype));
                break;

            case CurrentConfereeList: // NOTE: 入会后会收到一次该通知，创会者也会收到这条消息。列表中包含了自己。
                List<Conferee> presentConferees =
                        Stream.of(((TMTEntityInfoList) ntfContent).atMtEntitiy)
                        .distinctBy(it -> it.dwMcuId+"-"+it.dwTerId)
                        .map(this::tMTEntityInfo2Conferee)
                        .filter(it-> findConferee(it.mcuId, it.terId, it.type)==null)
                        .collect(Collectors.toList());

                Conferee self = Stream.of(presentConferees).filter(Conferee::isMyself).findFirst().orElse(null);
                if (self != null){
                    // 己端我们拎出来特殊对待
                    myself.fill(self.mcuId, self.terId, self.alias,self.email);
                    presentConferees.remove(self);
                }

                conferees.addAll(presentConferees);

                Conferee assStreamConferee = tryCreateAssStreamConferee();
                if (null != assStreamConferee){
                    conferees.add(assStreamConferee);
                    presentConferees.add(assStreamConferee);
                }

                presentConferees.add(myself); // 己端放最后
                sessionEventListener.onConfereesAppeared(presentConferees);
                break;

            case ConfereeJoined:
                Conferee joined = tMTEntityInfo2Conferee((TMTEntityInfo) ntfContent);
                if (findConferee(joined.mcuId, joined.terId, joined.type)==null){
                    conferees.add(joined);
                    sessionEventListener.onConfereeJoined(joined);
                    assStreamConferee = tryCreateAssStreamConferee();
                    if (null != assStreamConferee){
                        conferees.add(assStreamConferee);
                        sessionEventListener.onConfereeJoined(assStreamConferee);
                    }
                }
                break;

            case ConfereeLeft:
                TMtId tMtId = (TMtId) ntfContent;
                Conferee leftConferee = findConferee(tMtId.dwMcuId, tMtId.dwTerId, Conferee.ConfereeType.Normal);
                if (null != leftConferee){
                    conferees.remove(leftConferee);
                    sessionEventListener.onConfereeLeft(leftConferee);
                }
                break;

            case CurrentStreamList: // NOTE: 创会者不会收到这条消息，并且CurrentStreamList和CurrentConfereeList的先后顺序不定
            case StreamJoined: // NOTE: 己端不会收到自己的流joined的消息
                List<RtcStream> joinedStreams =
                        Stream.of(((TRtcStreamInfoList) ntfContent).atStramInfoList)
                        .distinctBy(it-> it.achStreamId)
                        .map(RtcStream::new)
                        .filter(it-> findStream(it.getStreamId())==null)
                        .collect(Collectors.toList());

                if (!joinedStreams.isEmpty()) {
                    boolean hasAssStreamJoined = Stream.of(joinedStreams).anyMatch(RtcStream::isAss);
                    if (hasAssStreamJoined) { // 有辅流加入
                        // 检查是否已存在辅流，若存在则先踢掉之前的辅流。（按说我们应该依赖下层消息驱动我们做这件事，
                        // 我们期望下层先推一个StreamLeft消息，表示前面的辅流被抢了，而后再推StreamJoined，表示新的辅流加入，但实际的时序刚好相反。
                        // 所以，我们自己调整时序——当抢发辅流的StreamJoined到达时我们主动踢掉前面的辅流，然后前面辅流的StreamLeft抵达时我们忽略。）
                        RtcStream preAssStream = findAssStream();
                        if (null != preAssStream) {
                            streams.remove(preAssStream);
                            PeerConnectionWrapper pcWrapper = getPcWrapper(ConnType.ASS_SUBSCRIBER);
                            if (null != pcWrapper) {
                                pcWrapper.removeRemoteVideoTrack(preAssStream);
                            }
                            Conferee preAssStreamConferee = preAssStream.getOwner();
                            if (null != preAssStreamConferee) {
                                conferees.remove(preAssStreamConferee);
                                sessionEventListener.onConfereeLeft(preAssStreamConferee);
                            }
                        }
                    }

                    streams.addAll(joinedStreams);

                    if (hasAssStreamJoined){
                        assStreamConferee = tryCreateAssStreamConferee();
                        if (null != assStreamConferee){
                            conferees.add(assStreamConferee);
                            sessionEventListener.onConfereeJoined(assStreamConferee);
                        }
                    }

                    subscribeStream();
                }
                break;

            case StreamLeft: // NOTE 己端不会收到自己的流left的消息。码流退出不需要自己setplayitem重新订阅，业务组件已经重新订阅。
                Set<RtcStream> leftStreams = Stream.of(streams)
                        .filter(it-> Stream.of(((TRtcStreamInfoList) ntfContent).atStramInfoList).anyMatch(s-> s.achStreamId.equals(it.getStreamId())))
                        .collect(Collectors.toSet());

                streams.removeAll(leftStreams);

                Stream.of(leftStreams).forEach(stream -> {
                    // 删除track
                    // 我们在onTrack回调中createRemoteVideoTrack/createRemoteAudioTrack，
                    // 相应的我们原本期望在onRemoveStream（没有onRemoveTrack）中removeRemoteVideoTrack/removeRemoteAudioTrack，
                    // 然而实测下来发现onRemoveStream回调上来时MediaStream的track列表为空，不得已我们只得在StreamLeft消息上来时removeRemoteVideoTrack/removeRemoteAudioTrack
                    if (stream.isAss()){
                        PeerConnectionWrapper pcWrapper = getPcWrapper(ConnType.ASS_SUBSCRIBER);
                        if (null != pcWrapper) {
                            pcWrapper.removeRemoteVideoTrack(stream);
                        }
                        Conferee assConferee = stream.getOwner();
                        if (null != assConferee){
                            conferees.remove(assConferee);
                            sessionEventListener.onConfereeLeft(assConferee);
                        }
                    }else {
                        PeerConnectionWrapper pcWrapper = getPcWrapper(ConnType.SUBSCRIBER);
                        if (null != pcWrapper) {
                            if (stream.isAudio()) {
                                pcWrapper.removeRemoteAudioTrack(stream);
                            } else {
                                pcWrapper.removeRemoteVideoTrack(stream);
                            }
                        }
                    }
                });

//                bindStream(); // 码流离开不需要重新订阅，业务组件会处理
                break;

            case SelfSilenceStateChanged:
                BaseTypeBool cont = (BaseTypeBool) ntfContent;
                if (cont.basetype != config.isSilenced && doSetSilence(cont.basetype)) {
                    sessionEventListener.onSelfSilenceStateChanged(config.isSilenced);
                }
                break;
            case SelfMuteStateChanged:
                cont = (BaseTypeBool) ntfContent;
                if (cont.basetype != config.isMuted && doSetMute(cont.basetype)) {
                    sessionEventListener.onSelfSilenceStateChanged(config.isMuted);
                }
                break;
            case OtherConfereeStateChanged:
                TMtEntityStatus state = (TMtEntityStatus) ntfContent;
                Conferee conferee = findConferee(state.dwMcuId, state.dwTerId, Conferee.ConfereeType.Normal);
                if (conferee != null){
                    sessionEventListener.onOtherConfereeAudioStateChanged(conferee, state.tStatus.bIsMute, state.tStatus.bIsQuiet);
                }
                break;
        }

    }


    private Conferee tMTEntityInfo2Conferee(@NonNull TMTEntityInfo entityInfo){
        String e164="", alias="", email="";
        for (TMtAlias tMtAlias : entityInfo.tMtAlias.arrAlias) {
            if (EmMtAliasType.emAliasE164 == tMtAlias.emAliasType) {
                e164 = tMtAlias.achAlias;
            } else if (EmMtAliasType.emAliasH323 == tMtAlias.emAliasType) {
                alias = tMtAlias.achAlias;
            } else if (EmMtAliasType.emAliasEmail == tMtAlias.emAliasType) {
                email = tMtAlias.achAlias;
            }
            if (!e164.isEmpty() && !alias.isEmpty() && !email.isEmpty()) {
                break;
            }
        }

        return new Conferee(entityInfo.dwMcuId, entityInfo.dwTerId, e164, alias, email);
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

        myself = new Conferee(userE164);

        rtcConnector.setSignalingEventsCallback(new RtcConnectorEventListener());

        config.copy(RtcConfig.getInstance(context).dump());
        KLog.p("init rtc config: "+config);
        createPeerConnectionFactory();
        createPeerConnectionWrapper();

        // 定时获取统计信息
        handler.postDelayed(statsCollector, 3000);
        // 定时处理音频统计信息
        handler.postDelayed(audioStatsProcesser, 3000);
        // 定时处理视频统计信息
        handler.postDelayed(videoStatsProcesser, 3000);

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

        handler.removeCallbacksAndMessages(null);

        for (Display display : displays){
            display.destroy();
        }
        displays.clear();

//        myself = null;

        conferees.clear();

        streams.clear();


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
        cancelReq(Msg.VerifyConfPassword, null);

        KLog.p("session stopped ");

        return true;

    }


    private void createPeerConnectionFactory() {
        KLog.p("creating factory...");

        eglBase = EglBase.create();

        executor.execute(() -> {
            if (null != factory){
                KLog.p(KLog.ERROR, "Factory exists!");
                return;
            }

            String fieldTrials = ""
//                    +"WebRTC-H264Simulcast/Enabled/"
//                    +"WebRTC-SpsPpsIdrIsH264Keyframe/Enabled/"
                    ;

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

            WebRtcAudioUtils.setWebRtcBasedAcousticEchoCanceler(true);
            WebRtcAudioUtils.setWebRtcBasedAutomaticGainControl(true);
            WebRtcAudioUtils.setWebRtcBasedNoiseSuppressor(true);

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
        KLog.p("destroying factory...");
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

        pubPcWrapper = new PeerConnectionWrapper(ConnType.PUBLISHER, new SDPObserver(ConnType.PUBLISHER));
        subPcWrapper = new PeerConnectionWrapper(ConnType.SUBSCRIBER, new SDPObserver(ConnType.SUBSCRIBER));
        assPubPcWrapper = new PeerConnectionWrapper(ConnType.ASS_PUBLISHER, new SDPObserver(ConnType.ASS_PUBLISHER));
        assSubPcWrapper = new PeerConnectionWrapper(ConnType.ASS_SUBSCRIBER, new SDPObserver(ConnType.ASS_SUBSCRIBER));

        executor.execute(() -> {
            if (null == factory){
                throw new RuntimeException("Factory not exists!");
            }

            PeerConnection pubPc = createPeerConnection(ConnType.PUBLISHER);
            PeerConnection subPc = createPeerConnection(ConnType.SUBSCRIBER);
            PeerConnection assPubPc = createPeerConnection(ConnType.ASS_PUBLISHER);
            PeerConnection assSubPc = createPeerConnection(ConnType.ASS_SUBSCRIBER);

            synchronized (pcWrapperLock) {
                if (null != pubPcWrapper) pubPcWrapper.setPeerConnection(pubPc);
                if (null != subPcWrapper) subPcWrapper.setPeerConnection(subPc);
                if (null != assPubPcWrapper) assPubPcWrapper.setPeerConnection(assPubPc);
                if (null != assSubPcWrapper) assSubPcWrapper.setPeerConnection(assSubPc);
            }

            KLog.p("pcWrappers created");

            if (/*config.aecDump*/false) {
                try {
                    File dir = new File(Environment.getExternalStorageDirectory().getPath() + File.separator + "kedacom/webrtc");
                    if (!dir.exists()){
                        dir.mkdirs();
                    }
                    ParcelFileDescriptor aecDumpFileDescriptor =
                            ParcelFileDescriptor.open(new File(dir, "aec.dump"),
                                    ParcelFileDescriptor.MODE_READ_WRITE | ParcelFileDescriptor.MODE_CREATE | ParcelFileDescriptor.MODE_TRUNCATE);
                    factory.startAecDump(aecDumpFileDescriptor.detachFd(), -1);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }


        });

    }


    private PeerConnection createPeerConnection(ConnType connType){
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


    private void recreatePeerConnection(ConnType connType){
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
        if (/*config.aecDump*/false) {
            executor.execute(() -> {
                factory.stopAecDump();
            });
        }
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
                .setUseHardwareAcousticEchoCanceler(config.isBuiltInAECPreferred)
                .setUseHardwareNoiseSuppressor(config.isBuiltInNSPreferred)
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


    private PeerConnectionWrapper getPcWrapper(ConnType connType){
        if (ConnType.PUBLISHER == connType){
            return pubPcWrapper;
        }else if (ConnType.SUBSCRIBER == connType){
            return subPcWrapper;
        }else if (ConnType.ASS_PUBLISHER == connType){
            return assPubPcWrapper;
        }else if (ConnType.ASS_SUBSCRIBER == connType){
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
         * 己端加入会议时，会议中已有与会方出席（包括自己）。
         * 作为对比，{@link #onConfereeJoined(Conferee)}、{@link #onConfereeLeft(Conferee)}分别表示己端入会后与会方加入、离开。
         *
         * 一般情形下，用户收到该回调时调用{@link #createDisplay(Display.Type)}创建Display，
         * 然后调用{@link Display#setConferee(Conferee)} 将Display绑定到与会方以使与会方画面展示在Display上，
         * 如果还需要展示文字图标等deco，可调用{@link Conferee#addText(TextDecoration)}}, {@link Conferee#addPic(PicDecoration)}
         * NOTE: 文字、图片等deco是属于Conferee的而非Display，所以调用{@link Display#swapContent(Display)}等方法时，deco也会跟着迁移。
         *
         * @param conferees 已有与会方列表（包括自己）。
         * */
        void onConfereesAppeared(List<Conferee> conferees);
        /**
         * 与会方加入。
         * 一般情形下，用户收到该回调时调用{@link #createDisplay(Display.Type)}创建Display，
         * 然后调用{@link Display#setConferee(Conferee)} 将Display绑定到与会方以使与会方画面展示在Display上，
         * 如果还需要展示文字图标等deco，可调用{@link Conferee#addText(TextDecoration)}}, {@link Conferee#addPic(PicDecoration)}
         * NOTE: 文字、图片等deco是属于Conferee的而非Display，所以调用{@link Display#swapContent(Display)}等方法时，deco也会跟着迁移。
         * */
        void onConfereeJoined(Conferee conferee);
        /**
         * 与会方离开。
         * 如果该Conferee对应的Display不需要了请调用{@link #releaseDisplay(Display)}销毁；
         * 如果后续要复用则可以不销毁，可以调用{@link Display#setConferee)}参数传null清空内容；
         * NOTE: 会议结束时会销毁所有Display。用户不能跨会议复用Display。
         * */
        void onConfereeLeft(Conferee conferee);

        /**
         * 与会方正在发言
         * */
        default void onConfereeSpeaking(Conferee conferee){}

        /**
         * 此会议需要输入密码。
         * 收到此通知后请调用{@link #verifyConfPassword(String, IResultListener)}传入密码验证
         * */
        void confPasswordNeeded();

        /**
         * 己端哑音状态被改变
         * NOTE: 主动哑音{@link #setMute(boolean, IResultListener)}不会触发该事件
         */
        default void onSelfMuteStateChanged(boolean muted) {

        }

        /**
         * 己端静音状态被改变
         * NOTE: 主动静音{@link #setSilence(boolean, IResultListener)}不会触发该事件
         */
        default void onSelfSilenceStateChanged(boolean silenced) {

        }

        /**
         * 其他与会方声音状态变更
         */
        default void onOtherConfereeAudioStateChanged(Conferee conferee, boolean muted, boolean silenced) {

        }
    }
    private SessionEventListener sessionEventListener;


    /**
     * 获取与会方列表
     * @param excludeAssStreamConferee 是否排除辅流与会方，true排除
     * @param excludeAssStreamConferee 是否排除己端，true排除
     * @return 与会方列表。已排序。排序规则：
     *                          自然排序；
     *                          优先比对昵称，昵称若不存在则使用其e164，e164不存在则使用其邮箱；
     *                          若包含己端则己端排最后。
     * */
    public List<Conferee> getConferees(boolean excludeAssStreamConferee, boolean excludeSelf){
        List<Conferee> confereesList = new ArrayList<>();
        Conferee self =null;
        for(Conferee conferee : conferees){
            if (excludeAssStreamConferee && conferee.isVirtualAssStreamConferee()){
                continue;
            }
            confereesList.add(conferee);
        }
        Collections.sort(confereesList);
        if (!excludeSelf){
            confereesList.add(myself);
        }

        return confereesList;
    }


    /**
     * 与会方。
     * 该与会方相较于需求中的与会方是更抽象的概念，包含了普通的与会方以及虚拟的辅流与会方（以及将来可能扩展的其他类型与会方）。
     */
    public static final class Conferee implements VideoSink, Comparable<Conferee>{
        // 唯一标志
        private final String id;

        private int mcuId;
        private int terId;
        private String   e164;
        private String   alias;
        private String   email;

        private final ConfereeType type;

        // 音频通道状态
        private AudioChannelState audioChannelState = AudioChannelState.Idle;
        // 音频信号状态
        private AudioSignalState audioSignalState = AudioSignalState.Idle;

        // 视频通道状态
        private VideoChannelState videoChannelState = VideoChannelState.Idle;
        // 视频信号状态
        private VideoSignalState videoSignalState = VideoSignalState.Idle;

        // 与会方画面显示器。
        // 一个与会方可以绑定多个Display。一个Display只能绑定到一个与会方
        private Set<Display> displays = Collections.newSetFromMap(new ConcurrentHashMap<>());

        // 文字图片装饰。如台标，静态图片等。
        // 与会方的画面内容由码流和装饰组成。
        private Set<TextDecoration> textDecorations = new HashSet<>();
        private Set<PicDecoration> picDecorations = new HashSet<>();

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


        private Conferee(String e164) {
            this.e164 = e164;
            type = ConfereeType.Normal;
            id = e164+"-"+type;
        }

        private Conferee(int mcuId, int terId, String e164, String alias, String email) {
            this(mcuId, terId, e164, alias, email, ConfereeType.Normal);
        }

        private Conferee(int mcuId, int terId, String e164, String alias, String email, ConfereeType type) {
            id = mcuId+"-"+terId+"-"+e164+"-"+type;
            this.mcuId = mcuId;
            this.terId = terId;
            this.e164 = e164;
            this.alias = alias;
            this.email = email;
            this.type = type;
        }


        private void fill(int mcuId, int terId, String alias, String email){
            this.mcuId = mcuId;
            this.terId = terId;
            this.alias = alias;
            this.email = email;
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

        /**
         * @return 与会方对应的显示器集合。不可修改！
         * */
        public Set<Display> getDisplays() {
            return Collections.unmodifiableSet(displays);
        }


        private Set<Display> getWorkingDisplays(){
            return Stream.of(displays).filter(value -> value.enabled).collect(Collectors.toSet());
        }

        /**
         * 是否为己端
         * */
        public boolean isMyself(){
            return instance.userE164.equals(e164); // XXX 可以考虑增加一个与会方类型——己端，则可以直接判断类型是否为己端。
        }

        /**
         * 是否为（虚拟的）辅流与会方
         * */
        public boolean isVirtualAssStreamConferee() {
            return type == ConfereeType.AssStream;
        }

        /**
         * 是否正在发送辅流
         * */
        public boolean isSendingAssStream() {
            if (isMyself()){
                return null != instance.sharedWindow;
            }else {
                Conferee conferee = instance.findAssStreamSender();
                return null != conferee && conferee.getId().equals(id);
            }
        }

        /**
         * 设置语音激励deco
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
         * 设置关闭摄像头采集时己端与会方展示的deco
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
         * 设置音频入会的deco
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
         * 设置视频信号丢失的deco
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
         * 设置正在接收辅流时展示的deco
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
         * 获取文字deco
         * */
        public TextDecoration getTextDeco(String decoId){
            return Stream.of(textDecorations)
                    .filter(it-> it.id.equals(decoId))
                    .findFirst()
                    .orElse(null);
        }

        /**
         * 获取图片deco
         * */
        public PicDecoration getPicDeco(String decoId){
            return Stream.of(picDecorations)
                    .filter(it-> it.id.equals(decoId))
                    .findFirst()
                    .orElse(null);
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


        private long timestamp = System.currentTimeMillis();
        @Override
        public void onFrame(VideoFrame videoFrame) {
            long curts = System.currentTimeMillis();
            long ts = timestamp;
            if (curts - ts > 5000){
                timestamp = curts;
                KLog.p(KLog.DEBUG, "%s onFrame ", getId());
            }

            for (Display display : displays){
                if (curts - ts > 5000) {
                    if (display.enabled) {
                        KLog.p(KLog.DEBUG, "frame of conferee %s rendered onto display %s ", getId(), display.id());
                    }else{
                        KLog.p(KLog.DEBUG, "frame of conferee %s dropped off display %s because it is disabled ", getId(), display.id());
                    }
                }
                if (!display.enabled) {
                    continue;
                }
                display.onFrame(videoFrame);
            }
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


        private void setAudioChannelState(AudioChannelState state) {
            if (state != audioChannelState) {
                KLog.sp(String.format("%s change AUDIO CHANNEL state(from %s to %s)", getId(), audioChannelState, state));
                audioChannelState = state;
                refreshDisplays();
            }
        }

        private AudioChannelState getAudioChannelState() {
            return audioChannelState;
        }

        private void setAudioSignalState(AudioSignalState state) {
            if (state != audioSignalState) {
                KLog.sp(String.format("%s change AUDIO SIGNAL state(from %s to %s)", getId(), audioSignalState, state));
                audioSignalState = state;
                refreshDisplays();
            }
        }

        private AudioSignalState getAudioSignalState() {
            return audioSignalState;
        }


        private void setVideoChannelState(VideoChannelState videoChannelState) {
            if (videoChannelState != this.videoChannelState) {
                KLog.sp(String.format("%s change VIDEO CHANNEL state from %s to %s", getId(), this.videoChannelState, videoChannelState));
                this.videoChannelState = videoChannelState;
                refreshDisplays();
            }
        }

        private VideoChannelState getVideoChannelState() {
            return videoChannelState;
        }

        private void setVideoSignalState(VideoSignalState videoSignalState) {
            if (videoSignalState != this.videoSignalState) {
                KLog.sp(String.format("%s change VIDEO SIGNAL state from %s to %s", getId(), this.videoSignalState, videoSignalState));
                this.videoSignalState = videoSignalState;
                refreshDisplays();
            }
        }

        private VideoSignalState getVideoSignalState() {
            return videoSignalState;
        }

        private RtcStream getVideoStream(){
            return instance.findStream(mcuId, terId, false, type==ConfereeType.AssStream);
        }

        private Set<RtcStream> getAudioStreams(){
            return Stream.of(instance.streams)
                    .filter(it-> it.isAudio() && it.getMcuId()==mcuId && it.getTerId()==terId)
                    .collect(Collectors.toSet());
        }

        private int getPreferredVideoQuality(){
            return Stream.of(displays)
                    .filter(display -> display.enabled)
                    .map(display -> display.preferredVideoQuality)
                    .max(Integer::compareTo)
                    .orElse(RtcConfig.VideoQuality_Unknown);
        }

        private Display.Priority getPriority(){
            return Stream.of(displays)
                    .filter(display -> display.enabled)
                    .map(display -> display.priority)
                    .max(Display.Priority::compareTo)
                    .orElse(null);
        }

        private Display.Priority priority;
        private int preferredVideoQuality = -1;
        private RtcStream videoStream;
        private void mark(){
            if (isMyself()) {
                return;
            }
            priority = getPriority();
            preferredVideoQuality = getPreferredVideoQuality();
            videoStream = getVideoStream();
        }

        /**
         * 尝试刷新conferee。
         * @return true刷新，false无需刷新。
         * NOTE: 先{@link #mark()}再invalidate，成对。
         * */
        private boolean tryInvalidate(){
            if (isMyself()) {
                return false;
            }
            RtcStream curVS = getVideoStream();
            Display.Priority curPri = getPriority();
            int curPVQ = getPreferredVideoQuality();

            boolean needInvalidate = curVS != videoStream
                    || ( curVS != null && (curPri != priority || (curPVQ != preferredVideoQuality && curVS.getResolution(preferredVideoQuality) != curVS.getResolution(curPVQ)) ) );

            if (needInvalidate){
                instance.subscribeStream();
            }
            return needInvalidate;
        }

        private boolean onStage(){
            return getPriority() == Display.Priority.HIGH;
        }

        private void addDisplay(@NonNull Display display) {
            KLog.p("add Display %s to conferee %s", display.id(), getId());
            mark();
            displays.add(display);
            boolean invalidate = tryInvalidate();
            if (invalidate && videoChannelState == VideoChannelState.Idle){
                instance.handler.postDelayed(() -> {
                    if (VideoChannelState.Idle == videoChannelState){
                        // Conferee没有视频码流，此种情形下我们置其VideoChannelState为BindingFailed
                        setVideoChannelState(VideoChannelState.BindingFailed);
                    }
                }, 500);
            }
        }

        private boolean removeDisplay(@NonNull Display display) {
            KLog.p("try to remove Display %s from conferee %s", display.id(), getId());
            mark();
            boolean success = displays.remove(display);
            if (success) {
                KLog.p("display %s removed from conferee %s", display.id(), getId());
                tryInvalidate();
            }
            return success;
        }


        private void refreshDisplays(){
            // 延迟刷新以防止短时间内大量重复刷新
            instance.handler.removeCallbacks(refreshDisplaysRunnable);
            instance.handler.postDelayed(refreshDisplaysRunnable, 100); // TODO 增大延迟看能否改善关开摄像头时残留最后一帧的情况
        }

        private Runnable refreshDisplaysRunnable = new Runnable() {
            @Override
            public void run() {
                doRefreshDisplays();
            }
        };

        private void doRefreshDisplays(){
            for (Display display : displays){
                display.refresh();
            }
        }


        // 与会方类型
        private enum ConfereeType{
            Normal,
            AssStream, // 辅流与会方
        }

        // 音频通道状态
        private enum AudioChannelState {
            Idle,
            Binding,
            BindingFailed,
            Bound,
        }

        // 音频信号状态
        private enum AudioSignalState {
            Idle,
            Normal,
            Activated, // 语音激励
        }

        // 视频通道状态
        private enum VideoChannelState{
            Idle,
            Binding,
            BindingFailed,
            Bound,
        }

        // 视频信号状态
        private enum VideoSignalState{
            Idle,
            Buffering,  // 正在缓冲中。用于在展示与会方画面前展示一些过渡效果（如辅流与会方展示辅流缓冲图标）
            Normal,     // 正常画面
            Weak,       // 视频信号弱（视频已经订阅了，但没有视频帧（如对端关闭了摄像头采集，停止了发送）或者视频帧率非常低（如网络状况很差导致的低帧率））
        }

    }


    /**
     * 码流。
     * */
    private class RtcStream{
        private TRtcStreamInfo streamInfo;

        private RtcStream(@NonNull TRtcStreamInfo streamInfo){
            this.streamInfo = streamInfo;
        }


        private String getStreamId() {
            return streamInfo.achStreamId;
        }

        private int getMcuId() {
            return streamInfo.tMtId.dwMcuId;
        }

        private int getTerId() {
            return streamInfo.tMtId.dwTerId;
        }

        private boolean isAudio() {
            return streamInfo.bAudio;
        }

        private boolean isAss() {
            return streamInfo.bAss;
        }

        private EmMtResolution getResolution(){
            int ownerPreferredQuality = RtcConfig.VideoQuality_Unknown;
            Conferee owner = getOwner();
            if (null != owner){
                ownerPreferredQuality = owner.getPreferredVideoQuality();
            }
            KLog.p("streamid=%s, config.PreferredVideoQuality=%s, owner preferredVideoQuality=%s",
                    streamInfo.achStreamId, config.preferredVideoQuality, ownerPreferredQuality);
            int quality = RtcConfig.VideoQuality_Unknown==ownerPreferredQuality ?
                    config.preferredVideoQuality :
                    Math.min(config.preferredVideoQuality, ownerPreferredQuality);

            return getResolution(quality);
        }

        private EmMtResolution getResolution(int quality){
            List<EmMtResolution> resolutions = streamInfo.aemSimcastRes;
            if (resolutions.isEmpty()){
                // 若发布方不是simulcast发布的则resolution会是空，此时随便添一个即可，业务组件会处理。
                return EmMtResolution.emMtResAuto_Api;
            }
            // NOTE: 分辨率是按从小到大的顺序排列的，这点平台保证。
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

        private Conferee getOwner(){
            return findConferee(streamInfo.tMtId.dwMcuId, streamInfo.tMtId.dwTerId,
                    streamInfo.bAss ? Conferee.ConfereeType.AssStream : Conferee.ConfereeType.Normal);
        }

    }


    /**
     * 显示器。
     * 一个显示器只能绑定到一个与会方，一个与会方可以绑定到多个显示器。
     * */
    public final static class Display extends SurfaceViewRenderer{
        /**
         * Display唯一标识
         * */
        private String id;

        /**
         * 绑定的与会方。
         * */
        private Conferee conferee;

        /**
         * 禁止显示的deco集合
         * */
        private Set<String> disabledDecos = new HashSet<>();

        /**
         * 是否使能。
         * 使能则显示内容，否则不显示。
         * NOTE： 该属性乃Display固有，不会因内容相关操作，如{@link #swapContent(Display)}等，而变更。
         * */
        private boolean enabled = true;

        /**
         * Display类型。
         * Display类型影响{@link #priority}和{@link #preferredVideoQuality}的默认值。
         * NOTE： 该属性乃Display固有，不会因内容相关操作，如{@link #swapContent(Display)}等，而变更。
         * */
        private Type type;

        /**
         * 视频质量偏好。
         * 一个与会方“可能”有高中低三种不同质量的视频流，该字段用于指定该Display“倾向于”展示哪种质量的视频流。
         * NOTE： 该属性乃Display固有，不会因内容相关操作，如{@link #swapContent(Display)}等，而变更。
         * */
        private int preferredVideoQuality;

        /**
         * 优先级
         * 多个display场景下，高优先级的优先享用带宽，以保证画面的流畅。
         * NOTE： 该属性乃Display固有，不会因内容相关操作，如{@link #swapContent(Display)}等，而变更。
         * */
        private Priority priority;


        private Display(Context context, Type type) {
            super(context);
            init(instance.eglBase.getEglBaseContext(), null);
            setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
            setEnableHardwareScaler(true);
            setWillNotDraw(false);
            id = hashCode()+"";
            this.type = type;
            priority = type2Priority(type);
            preferredVideoQuality = type2VideoQuality(type);
        }

        /**
         * 设置是否在所有Display最前端展示。可用于多个Display层叠的场景
         * */
        public void setOnTopOverOtherDisplays(boolean bOnTop){
            setZOrderMediaOverlay(bOnTop);
        }

        public String id() {
            return id;
        }

        public boolean isEnable() {
            return enabled;
        }

        /**
         * 设置是否使能该Display
         * @param enable false 禁用该Display，屏蔽内容展示；true正常展示内容。默认true。
         * */
        public void setEnable(boolean enable){
            if (this.enabled != enable) {
                KLog.p("set enable=%s for display %s", enable, id());
                if (conferee != null){ conferee.mark(); }

                this.enabled = enable;

                if (conferee != null){
                    conferee.tryInvalidate();
                    refresh();
                }
            }
        }

        /**
         * 设置Display类型。
         * NOTE：设置Display类型会根据默认规则重置{@link #preferredVideoQuality}和{@link #priority}，参见{@link #createDisplay(Type)}
         * */
        public void setType(Type type){
            if (type != this.type){
                this.type = type;
                setPreferredVideoQuality(type2VideoQuality(type));
                setPriority(type2Priority(type));
            }
        }


        /**
         * 设置视频质量偏好。
         * @param quality 视频质量{@link RtcConfig#VideoQuality_High}{@link RtcConfig#VideoQuality_Medium}{@link RtcConfig#VideoQuality_Low}
         * 与会方“可能”发布了多种分辨率的视频流(simulcast)，此种情形下，可以依据使用场景选择对应的分辨率，如大画面以高分辨率展示，小画面以低分辨率展示。
         *
         * NOTE：大多数情况下您不需要调用该接口，默认会根据Display的type自动设置，参见{@link #createDisplay(Type)}及{@link #setType(Type)}
         * */
        public void setPreferredVideoQuality(int quality){
            if (preferredVideoQuality != quality){
                KLog.p("display %s change preferredVideoQuality from %s to %s", id(), preferredVideoQuality, quality);
                if (conferee != null){ conferee.mark(); }

                preferredVideoQuality = quality;

                if (conferee != null){ conferee.tryInvalidate(); }
            }
        }

        /**
         * 设置优先级。
         * 高优先级的优先享用网络带宽以保证画面质量。
         *
         * NOTE：大多数情况下您不需要调用该接口，默认会根据Display的type自动设置，参见{@link #createDisplay(Type)}及{@link #setType(Type)}
         * */
        public void setPriority(Priority priority) {
            if (this.priority != priority) {
                KLog.p("display %s change priority from %s to %s", id(), this.priority, priority);
                if (conferee != null){ conferee.mark(); }

                this.priority = priority;

                if (conferee != null){ conferee.tryInvalidate(); }
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
         * 清空禁掉的deco
         * */
        public void clearDisabledDecos(){
            disabledDecos.clear();
        }


        /**
         * 绑定与会方以展示其画面。
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
            if (conferee == this.conferee){
                return;
            }
            KLog.p("set content %s for display %s", null != conferee ? conferee.getId() : null, id());
            if (null != this.conferee){
                this.conferee.removeDisplay(this);
            }
            if (null != conferee) {
                // 前置摄像头，己端回显镜像
                setMirror(conferee.isMyself() && instance.config.isFrontCameraPreferred);

                conferee.addDisplay(this);
            }
            this.conferee = conferee;

            refresh();
        }

        public Conferee getConferee(){
            return conferee;
        }


        /**
         * 拷贝源Display的内容到本Display，覆盖原有内容。
         * @param src 源display。
         * */
        public void copyContentFrom(@NonNull Display src){
            KLog.p("copy content from display %s to display %s", src.id(), id());
            if (null != this.conferee){
                this.conferee.removeDisplay(this);
            }

            if (null != src.conferee) {
                src.conferee.addDisplay(this);
            }
            conferee = src.conferee;

            disabledDecos.clear();
            disabledDecos.addAll(src.disabledDecos);

            refresh();
        }

        /**
         * 将本Display的内容移动到目标Display，目标原有内容将被覆盖，本Display的内容将被清空。
         * @param dst 目标display。
         * */
        public void moveContentTo(@NonNull Display dst){
            KLog.p("move content from display %s to display %s", id(), dst.id());
            dst.setConferee(conferee);
            dst.disabledDecos.clear();
            dst.disabledDecos.addAll(disabledDecos);

            if (null != conferee){
                conferee.removeDisplay(this);
            }
            conferee = null;
            disabledDecos.clear();

            refresh();
        }


        /**
         * 交换两个display的内容。
         * @param otherDisplay 要交换的display。
         * */
        public void swapContent(@NonNull Display otherDisplay){
            KLog.p("swap display %s with display %s", id(), otherDisplay.id());
            Conferee myConferee = conferee;
            setConferee(otherDisplay.conferee);
            otherDisplay.setConferee(myConferee);

            Set<String> myDisabledDecos = new HashSet<>(disabledDecos);
            disabledDecos.clear();
            disabledDecos.addAll(otherDisplay.disabledDecos);
            otherDisplay.disabledDecos.clear();
            otherDisplay.disabledDecos.addAll(myDisabledDecos);
        }

        /**
         * 清空Display內容。（Display仍可複用）
         * */
        public void clear(){
            setConferee(null);
            disabledDecos.clear();
        }


        /**
         * 销毁display
         * */
        private void destroy(){
            KLog.p("destroy display %s ", id());
            clear();
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

            boolean isLocalVideoEnabled = instance.config.isLocalVideoEnabled;
            Conferee.VideoChannelState videoChannelState = conferee.getVideoChannelState();
            Conferee.VideoSignalState videoSignalState = conferee.getVideoSignalState();
            Conferee.AudioChannelState audioChannelState = conferee.getAudioChannelState();
            Conferee.AudioSignalState audioSignalState = conferee.getAudioSignalState();

            // 绘制码流状态deco
            StreamStateDecoration stateDeco = null;
            if (conferee.isMyself() && !isLocalVideoEnabled){
                stateDeco = Conferee.cameraDisabledDeco;
            }else if (Conferee.VideoChannelState.BindingFailed == videoChannelState
//                    && Conferee.AudioChannelState.BindingFailed != audioChannelState
            ){
                stateDeco = Conferee.audioConfereeDeco;
            }else if (Conferee.VideoSignalState.Weak == videoSignalState){
                stateDeco = Conferee.weakVideoSignalDeco;
            }else if (Conferee.VideoSignalState.Buffering == videoSignalState && conferee.isVirtualAssStreamConferee()){
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
            if (Conferee.AudioSignalState.Activated == audioSignalState){
                conferee.voiceActivatedDeco.set(0, 0, displayWidth, displayHeight);
                canvas.drawRect(conferee.voiceActivatedDeco, Conferee.voiceActivatedDecoPaint);
            }

        }


        /**
         * Display类型。
         * 对于一大多小的布局形式，大的是MAIN，小的是THUMBNAIL。
         * 對於等大佈局的情況，所有都是MAIN。
         * */
        public enum Type{
            THUMBNAIL, // 缩略图。一般以小画面形式展示，通常多个小画面形成一个列表。
            MAIN,  // 主Display。一般以大画面形式展示。
            FULLSCREEN // 全屏
        }

        /**
         * Display优先级。
         * 优先级影响一些内部策略，如带宽优享。
         * */
        public enum Priority{ // NOTE: 优先级按从小到大排序
            LOW,
//            MEDIUM, // 目前仅支持中高两种
            HIGH
        }

        private Priority type2Priority(Type type){
            switch (type){
                case THUMBNAIL:
                    return Priority.LOW;
                case MAIN:
//                    return Priority.MEDIUM;
                case FULLSCREEN:
                    return Priority.HIGH;
                default:
                    return Priority.LOW;
            }
        }

        private int type2VideoQuality(Type type){
            switch (type){
                case THUMBNAIL:
                    return RtcConfig.VideoQuality_Low;
                case MAIN:
                    return RtcConfig.VideoQuality_Medium;
                case FULLSCREEN:
//                    return RtcConfig.VideoQuality_High;
                    return RtcConfig.VideoQuality_Medium;
                default:
                    return RtcConfig.VideoQuality_Low;
            }
        }

    }


    private Conferee tryCreateAssStreamConferee(){
        if (null == findAssConferee()){
            Conferee sender = findAssStreamSender();
            if (null != sender){
                return new Conferee(sender.mcuId, sender.terId, sender.e164, sender.alias, sender.email, Conferee.ConfereeType.AssStream);
            }
        }
        return null;
    }


    /**
     * 订阅码流
     * */
    private void subscribeStream(){
        // 延迟处理以防止短时间内大量订阅
        handler.removeCallbacks(subscribeStreamRunnable);
        handler.postDelayed(subscribeStreamRunnable, 200);
    }


    private Runnable subscribeStreamRunnable = new Runnable() {
        @Override
        public void run() {
            doSubscribeStream();
        }
    };


    /**
     * 向平台订阅码流
     * NOTE：每次订阅都是全量，这意味着即使只新增一路码流也需要把之前已订阅过的码流全部重新订阅一遍，
     * 未包含在此次订阅码流集合中的则视为取消订阅（若之前已订阅）；音频码流无需订阅，业务组件已代劳。
     * */
    private void doSubscribeStream(){
        List<TRtcPlayItem> playItems = Stream.of(streams)
                .filter(it -> {
                    Conferee owner = it.getOwner();
                    if (null == owner){ // 不订阅/取消订阅没有归属的码流
                        return false;
                    }
                    if (owner.getWorkingDisplays().isEmpty()){ // 不订阅/取消订阅未绑定Display的Conferee的码流
                        return false;
                    }

                    if (it.isAudio()){
                        Conferee.AudioChannelState audioChannelState = owner.getAudioChannelState();
                        if (Conferee.AudioChannelState.Idle == audioChannelState
                                || Conferee.AudioChannelState.BindingFailed == audioChannelState) {
                            owner.setAudioChannelState(Conferee.AudioChannelState.Binding);
                            handler.postDelayed(() -> {
                                if (Conferee.AudioChannelState.Binding == owner.getAudioChannelState()){
                                    owner.setAudioChannelState(Conferee.AudioChannelState.BindingFailed);
                                }
                            }, 2000);
                        }
                        // 音频码流业务组件已代劳，无需订阅
                        return false;
                    }else {
                        Conferee.VideoChannelState videoChannelState = owner.getVideoChannelState();
                        if (Conferee.VideoChannelState.Idle == videoChannelState
                                || Conferee.VideoChannelState.BindingFailed == videoChannelState) {
                            owner.setVideoChannelState(Conferee.VideoChannelState.Binding);
                            handler.postDelayed(() -> {
                                if (Conferee.VideoChannelState.Binding == owner.getVideoChannelState()) {
                                    owner.setVideoChannelState(Conferee.VideoChannelState.BindingFailed);
                                }
                            }, 2000);
                        }
                        return true;
                    }

                })
                .map(stream -> new TRtcPlayItem(stream.getStreamId(), stream.isAss(), stream.getResolution(), stream.getOwner().onStage()))
                .collect(Collectors.toList());

        if (!playItems.isEmpty()) {
            // 因为是全量，所以可能既包含未订阅的的也包含已订阅过的。
            // 对于未订阅过的会触发PCObserver#onTrack，对于已订阅过的下层没有任何消息或者回调反馈
            set(Msg.SelectStream, new TRtcPlayParam(playItems));
        }
    }


    // 直接查找与会方。
    // 所有其他查找与会方的方法都是先查找RtcStream，然后通过RtcStream调用本方法。（较低效率换取简化的逻辑）
    // NOTE： 如下一系列方法均未将己端纳入考量！！
    private Conferee findConferee(int mcuId, int terId, Conferee.ConfereeType type){
        return Stream.of(conferees)
                .filter(it-> it.mcuId==mcuId && it.terId==terId && it.type==type)
                .findFirst()
                .orElse(null);
    }


    private Conferee findConfereeByStreamId(String streamId){
        RtcStream stream = findStream(streamId);
        if (null != stream){
            return stream.getOwner();
        }
        return null;
    }


    private Conferee findAssConferee(){
        RtcStream assStream = findAssStream();
        if (null == assStream){
            return null;
        }
        return assStream.getOwner();
    }


    private Conferee findAssStreamSender(){
        RtcStream assStream = findAssStream();
        if (null == assStream){
            return null;
        }
        return findConferee(assStream.getMcuId(), assStream.getTerId(), Conferee.ConfereeType.Normal);
    }


    private RtcStream findStream(String streamId){
        return Stream.of(streams).filter(it-> it.getStreamId().equals(streamId)).findFirst().orElse(null);
    }


    private RtcStream findStream(int mcuId, int terId, boolean isAudio, boolean isAss){
        return Stream.of(streams)
                .filter(it-> it.getMcuId()==mcuId && it.getTerId()==terId && it.isAudio()==isAudio && it.isAss()==isAss)
                .findFirst().orElse(null);
    }


    private RtcStream findAssStream(){
        return Stream.of(streams).filter(RtcStream::isAss).findFirst().orElse(null);
    }


    /**
     * 创建Display。
     *
     * @param type display類型。
     * 根据Display类型默认有如下策略：
     * 1、分辨率优选
     * 假设Conferee发布了3种不同分辨率的视频码流低、中、高，則Conferee分別展示在{@link Display.Type#THUMBNAIL}、{@link Display.Type#MAIN}、
     * {@link Display.Type#FULLSCREEN}類型的Display中時訂閱的嗎流分別爲低、中、高，Conferee在不同类型的Display中切换展示时码流选择会动态变化。
     * NOTE：您可以通过{@link Display#setPreferredVideoQuality(int)}覆盖默认的分辨率选择策略。
     *       若多个Display绑定到了同一个Conferee，则它们实际展示的分辨率一致——对齐其中最高的。
     * 2、带宽享用优先级
     * 默认情况下带宽享用的优先级{@link Display.Type#FULLSCREEN}>{@link Display.Type#MAIN}>{@link Display.Type#THUMBNAIL}，
     * 当带宽受限时会牺牲低优先级的Display画面效果以保证高优先级的流畅性。
     * NOTE：您可以通过{@link Display#setPriority(Display.Priority)} 覆盖默认的带宽享用优先级。
     *       若多个Display绑定到了同一个Conferee，则它们实际享用的带宽优先级是一致的——对齐其中最高的。
     *
     * NOTE：如果您后续想复用该Display，则您可能需要调用{@link Display#setType(Display.Type)}改换其类型。
     *      例如一开始以全屏展示某个画面{@link Display.Type#FULLSCREEN}，然后切换到大画面展示该画面，使用的是同一个Display，
     *      则该Display的类型需要更改为{@link Display.Type#MAIN}
     * */
    public Display createDisplay(Display.Type type){
        Display display =  new Display(context, type);
        displays.add(display);
        KLog.p("display %s created, type=%s", display.id(), type);
        return display;
    }


    /**
     * 销毁display
     * */
    public void releaseDisplay(Display display){
        if (displays.remove(display)){
            display.destroy();
            KLog.p("display %s released", display.id());
        }else{
            KLog.p(KLog.ERROR, "wired! display %s is not created by me?", display.id());
        }
    }


    public static class TextDecoration extends Decoration{
        public String text;     // 要展示的文字内容
        private int textSize;   // 文字大小（UCD标注的，实际展示的大小会依据Display的大小调整）
        private Paint bgPaint = new Paint();
        private RectF bgRect = new RectF();  // 文字背景区域
        private static final int minTextSizeLimit = 32;
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

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
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
        Bitmap pic;
        public PicDecoration(@NonNull String id, @NonNull Bitmap pic, int w, int h, int dx, int dy, int refPos) {
            super(id, w, h, dx, dy, refPos);
            this.pic = pic;
            paint.setStyle(Paint.Style.STROKE);
            paint.setAntiAlias(true);

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

        public Bitmap getPic() {
            return pic;
        }

        public void setPic(Bitmap pic) {
            this.pic = pic;
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


    public abstract static class Decoration{
        // 相对窗体的位置
        public static final int POS_LEFTTOP = 1;
        public static final int POS_LEFTBOTTOM = 2;
        public static final int POS_RIGHTTOP = 3;
        public static final int POS_RIGHTBOTTOM = 4;

        protected boolean enabled = true; // 是否使能。使能则展示否则不展示

        protected String id;
        protected int w;           // deco所在窗体的宽（UCD标注的）
        protected int h;           // deco所在窗体的高（UCD标注的）
        protected int dx;          // deco到窗体垂直边界的距离（参照pos）（UCD标注的）
        protected int dy;          // deco到窗体水平边界的距离（参照pos）（UCD标注的）
        protected int refPos;      // dx, dy参照的位置。如取值{@link #POS_LEFTBOTTOM}则dx表示距离窗体左边界的距离，dy表示距离窗体底部边界的距离。
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

        public String getId() {
            return id;
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

            // 计算实际锚点
            matrix.reset();
            matrix.postTranslate(x, y);
            matrix.postScale(width/(float)w, height/(float)h, 0, 0);
            float[] cor = new float[2];
            matrix.mapPoints(cor);
            actualX = cor[0]; actualY = cor[1];

            // 放缩尽量维持宽高比
            float averageScale = MatrixHelper.getAverageScale(matrix);
            matrix.reset();
            matrix.postTranslate(x, y);
            matrix.postScale(averageScale, averageScale, 0, 0);

//            KLog.p("displayW=%s, displayH=%s, x=%s, y=%s, matrix=%s, actualX=%s, actualY=%s",
//                    width, height, x, y, matrix, actualX, actualY);

            return true;
        }

    }


    private class RtcConnectorEventListener implements RtcConnector.SignalingEvents{

        @Override
        public void onGetOfferCmd(int pcType, int mType) {
            ConnType connType = ConnType.getInstance(pcType);
            MediaType mediaType = MediaType.getInstance(mType);
            PeerConnectionWrapper pcWrapper = getPcWrapper(connType);
            if (null == pcWrapper
                    || !pcWrapper.checkSdpState(SdpState.IDLE) // 只在空闲状态下才能发布。若已经处于发布状态需先取消发布方可再次发布
            ) {
                return;
            }

            pcWrapper.setMediaType(mediaType);
            VideoCapturer videoCapturer = null;
            if ((MediaType.VIDEO == mediaType
                    || MediaType.ASS_VIDEO == mediaType)){
                if (ConnType.PUBLISHER == connType) {
                    videoCapturer = createCameraCapturer(new Camera2Enumerator(context));
                }else if (ConnType.ASS_PUBLISHER == connType) {
//                        videoCapturer = createScreenCapturer();
                    videoCapturer = createWindowCapturer();
                }
            }
            if (null != videoCapturer) {
                pcWrapper.createVideoTrack(videoCapturer);
            }

            if ((MediaType.AUDIO == mediaType
                    || MediaType.AV == mediaType)) {
                pcWrapper.createAudioTrack();
            }

            pcWrapper.createOffer();

            if (MediaType.AV == mediaType){
                // 针对多路码流的情形，我们需要一路一路地发布（平台的限制）
                // 我们先发Audio，等到收到setAnswerCmd后再发Video
                pcWrapper.setSdpType(SdpType.AUDIO_OFFER);
            }else{
                pcWrapper.setSdpType(SdpType.OFFER);
            }

            pcWrapper.setSdpState(SdpState.CREATING);

            KLog.p(pcWrapper.toString());

        }

        @Override
        public void onSetOfferCmd(int pcType, String offerSdp, List<RtcConnector.TRtcMedia> rtcMediaList) {
            ConnType connType = ConnType.getInstance(pcType);
            PeerConnectionWrapper pcWrapper = getPcWrapper(connType);
            if (null == pcWrapper
                    || !pcWrapper.checkSdpState(SdpState.IDLE, SdpState.SET_LOCAL_SUCCESS) // 订阅可在空闲状态下（订阅第一路码流时）或者前面的订阅已完成状态下发起（订阅多路）
            ) {
                return;
            }
            for (RtcConnector.TRtcMedia rtcMedia : rtcMediaList) {
                KLog.p("mid=%s, kdstreamId=%s", rtcMedia.mid, rtcMedia.streamid);
                pcWrapper.mapMid2KdStreamId(rtcMedia.mid, rtcMedia.streamid);
            }

            pcWrapper.setSdpType(SdpType.ANSWER);
            String sdp = modifySdp(pcWrapper, offerSdp);
            pcWrapper.setRemoteDescription(new SessionDescription(SessionDescription.Type.OFFER, sdp));
            pcWrapper.setSdpState(SdpState.SETTING_REMOTE);
            KLog.p(pcWrapper.toString());
        }

        @Override
        public void onSetAnswerCmd(int pcType, String answerSdp) {
            ConnType connType = ConnType.getInstance(pcType);
            PeerConnectionWrapper pcWrapper = getPcWrapper(connType);
            if (null == pcWrapper || !pcWrapper.checkSdpState(SdpState.SENDING)) return;
            String sdp = modifySdp(pcWrapper, answerSdp);
            pcWrapper.setRemoteDescription(new SessionDescription(SessionDescription.Type.ANSWER, sdp));
            pcWrapper.setSdpState(SdpState.SETTING_REMOTE);
            KLog.p(pcWrapper.toString());
        }

        @Override
        public void onSetIceCandidateCmd(int pcType, String sdpMid, int sdpMLineIndex, String sdp) {
            ConnType connType = ConnType.getInstance(pcType);
            KLog.p("connType=%s, sdpMid=%s, sdpMLineIndex=%s", connType, sdpMid, sdpMLineIndex);
            PeerConnectionWrapper pcWrapper = getPcWrapper(connType);
            if (null == pcWrapper) return;

            pcWrapper.addCandidate(new IceCandidate(sdpMid, sdpMLineIndex, sdp));
        }

        @Override
        public void onGetFingerPrintCmd(int pcType) {
            ConnType connType = ConnType.getInstance(pcType);
            KLog.p("connType=%s", connType);
            PeerConnectionWrapper pcWrapper = getPcWrapper(connType);
            if (null == pcWrapper
                    || !pcWrapper.checkSdpState(SdpState.IDLE, SdpState.SET_LOCAL_SUCCESS)
            ) {
                return;
            }

            pcWrapper.setSdpType(SdpType.FINGERPRINT_OFFER);
            pcWrapper.createAudioTrack();
            pcWrapper.createOffer();
            pcWrapper.setSdpState(SdpState.CREATING);
        }

        @Override
        public void onUnPubCmd(int pcType, int mType) {
            ConnType connType = ConnType.getInstance(pcType);
            MediaType mediaType = MediaType.getInstance(mType);
            PeerConnectionWrapper pcWrapper = getPcWrapper(connType);
            if (null == pcWrapper
//                    || !pcWrapper.checkSdpState(pcWrapper.SetRemoteSuccess) // 只有在当前已处于发布状态的情形下才可以取消发布
            ) {
                return;
            }
            if (!pcWrapper.checkSdpState(SdpState.IDLE, SdpState.SET_REMOTE_SUCCESS)){
                KLog.p(KLog.ERROR, "invalid state, try again later...");
                // 多个终端几乎同时发双流时会出现发布双流流程尚未完成即收到取消发布双流的情况（因为接收到双流而取消正在发送的双流）
                // 这种情况下sdp process会失败，进而导致业务那边状态紊乱，下次发双流不成功。所以此处先等一会等前面的发布流程走完，再继续往下走取消发布。
                handler.postDelayed(() -> doUnpublish(connType, mediaType), 1000);
                return;
            }
            doUnpublish(connType, mediaType);
        }

    }


    private void doUnpublish(ConnType connType, MediaType mediaType){
        PeerConnectionWrapper pcWrapper = getPcWrapper(connType);
        if (null == pcWrapper) {
            return;
        }

        pcWrapper.isUnpublishing = true;

        // 删除取消发布的流。
        // 取消发布己端不会收到StreamLeft消息（其他与会方会收到），
        // 所以我们需在此删除流而非依赖收到StreamLeft后的处理逻辑
        if (MediaType.AUDIO == mediaType
                || MediaType.AV == mediaType){
            pcWrapper.removeAudioTrack();
        }
        if(MediaType.AUDIO != mediaType){
            pcWrapper.removeVideoTrack();
        }
        // 重新走发布
        pcWrapper.createOffer();
        pcWrapper.setSdpType(SdpType.OFFER);
        pcWrapper.setSdpState(SdpState.CREATING);
        KLog.p(pcWrapper.toString());
    }


    private String modifySdp(PeerConnectionWrapper pcWrapper, String origSdp){
        // 根据音视频编码偏好修改sdp
        String sdpVideoCodecName = getSdpVideoCodecName(config.preferredVideoCodec);
        if (pcWrapper.isSdpType(SdpType.OFFER) || pcWrapper.isSdpType(SdpType.ANSWER)){
            origSdp = SdpHelper.preferCodec(origSdp, config.preferredAudioCodec, true);
            origSdp = SdpHelper.preferCodec(origSdp, sdpVideoCodecName, false);
        }else if (pcWrapper.isSdpType(SdpType.AUDIO_OFFER)){
            origSdp = SdpHelper.preferCodec(origSdp, config.preferredAudioCodec, true);
        }else if (pcWrapper.isSdpType(SdpType.VIDEO_OFFER)){
            origSdp = SdpHelper.preferCodec(origSdp, sdpVideoCodecName, false);
        }
        return origSdp;
    }


    private class SDPObserver implements SdpObserver {
        private ConnType connType;

        SDPObserver(ConnType connType) {
            this.connType = connType;
        }

        @Override
        public void onCreateSuccess(final SessionDescription origSdp) {
            handler.post(() -> {
                PeerConnectionWrapper pcWrapper = getPcWrapper(connType);
                // 由于是异步，此时可能会议已经结束（如对端挂会），PeerConnectionWrapper已经销毁，所以我们此处需做非空判断
                if (null == pcWrapper || !pcWrapper.checkSdpState(SdpState.CREATING)) return;

                if (pcWrapper.isSdpType(SdpType.FINGERPRINT_OFFER)){
                    // 之前创建的音频流仅用于和平台交互FingerPrint没实际用处，此处交互已完成，销毁
                    pcWrapper.destroyAudioTrack();

                    rtcConnector.sendFingerPrint(pcWrapper.connType.ordinal(), SdpHelper.getFingerPrint(origSdp.description));
                    pcWrapper.setSdpState(SdpState.IDLE);
                }else {
                    String sdp = modifySdp(pcWrapper, origSdp.description);
                    pcWrapper.setLocalDescription(new SessionDescription(origSdp.type, sdp));
                    pcWrapper.setSdpState(SdpState.SETTING_LOCAL);
                }
                KLog.p(pcWrapper.toString());
            });

        }

        @Override
        public void onSetSuccess() {
            handler.post(() -> {
                PeerConnectionWrapper pcWrapper = getPcWrapper(connType);
                if (null == pcWrapper || !pcWrapper.checkSdpState(SdpState.SETTING_LOCAL, SdpState.SETTING_REMOTE)) return;

                PeerConnection pc = pcWrapper.pc;
                if (pcWrapper.isSdpType(SdpType.OFFER)) {
                    if (pcWrapper.isSdpState(SdpState.SETTING_LOCAL)) {
                        boolean bAudio = pcWrapper.isMediaType(MediaType.AUDIO);
                        RtcConnector.TRtcMedia rtcMedia = new RtcConnector.TRtcMedia(
                                SdpHelper.getMid(pc.getLocalDescription().description, bAudio),
                                bAudio ? null : createEncodingListForSendingOfferSdp() // 仅视频需要填encodings
                        );
                        rtcConnector.sendOfferSdp(pcWrapper.connType.ordinal(), pc.getLocalDescription().description, rtcMedia);
                        pcWrapper.setSdpState(SdpState.SENDING);
                    } else {
                        pcWrapper.drainCandidates();
                        pcWrapper.setSdpState(SdpState.SET_REMOTE_SUCCESS);
                        if (pcWrapper.isUnpublishing) {
                            // 取消发布结束，因协议组目前实现所限我们需重建PeerConnection。解决第二次发双流失败的问题
                            pcWrapper.isUnpublishing = false;
                            recreatePeerConnection(pcWrapper.connType);
                        }
                    }
                } else if (pcWrapper.isSdpType(SdpType.ANSWER)) {
                    if (pcWrapper.isSdpState(SdpState.SETTING_REMOTE)) {
                        pcWrapper.createAnswer();
                        pcWrapper.setSdpState(SdpState.CREATING);
                    } else {
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
                            handler.post(() -> rtcConnector.sendAnswerSdp(pcWrapper.connType.ordinal(), pc.getLocalDescription().description, rtcMediaList));
                        });
                        pcWrapper.drainCandidates();
                        pcWrapper.setSdpState(SdpState.SET_LOCAL_SUCCESS);
                    }
                } else if (pcWrapper.isSdpType(SdpType.AUDIO_OFFER)) {
                    if (pcWrapper.isSdpState(SdpState.SETTING_LOCAL)) {
                        RtcConnector.TRtcMedia rtcMedia = new RtcConnector.TRtcMedia(SdpHelper.getMid(pc.getLocalDescription().description, true));
                        rtcConnector.sendOfferSdp(pcWrapper.connType.ordinal(), pc.getLocalDescription().description, rtcMedia);
                        pcWrapper.setSdpState(SdpState.SENDING);
                    } else {
                        VideoCapturer videoCapturer = createCameraCapturer(new Camera2Enumerator(context));
                        if (null != videoCapturer) {
                            pcWrapper.createVideoTrack(videoCapturer);
                            pcWrapper.createOffer();
                        }
                        // 不同于正常sdp流程，此时还需要再发video的offer，所以切换sdptype为videoOffer
                        pcWrapper.setSdpType(SdpType.VIDEO_OFFER);
                        pcWrapper.setSdpState(SdpState.CREATING);
                    }
                } else if (pcWrapper.isSdpType(SdpType.VIDEO_OFFER)) {
                    if (pcWrapper.isSdpState(SdpState.SETTING_LOCAL)) {
                        RtcConnector.TRtcMedia rtcAudio = new RtcConnector.TRtcMedia(SdpHelper.getMid(pc.getLocalDescription().description, true));
                        RtcConnector.TRtcMedia rtcVideo = new RtcConnector.TRtcMedia(SdpHelper.getMid(pc.getLocalDescription().description, false), createEncodingListForSendingOfferSdp());
                        rtcConnector.sendOfferSdp(pcWrapper.connType.ordinal(), pc.getLocalDescription().description, rtcAudio, rtcVideo);
                        pcWrapper.setSdpState(SdpState.SENDING);
                    } else {
                        pcWrapper.drainCandidates();
                        // videooffer发布完毕，整个发布结束
                        pcWrapper.setSdpState(SdpState.SET_REMOTE_SUCCESS);
                    }
                }

                KLog.p(pcWrapper.toString());

            });

        }

        @Override
        public void onCreateFailure(final String error) {
            handler.post(() -> {
                PeerConnectionWrapper pcWrapper = getPcWrapper(connType);
                if (null == pcWrapper) return;
                KLog.p(KLog.ERROR, "create sdp failed, error info:%s. %s", error, pcWrapper);
                // 业务要求失败就重新创建peerconnection
                recreatePeerConnection(pcWrapper.connType);
            });
        }

        @Override
        public void onSetFailure(final String error) {
            handler.post(() -> {
                PeerConnectionWrapper pcWrapper = getPcWrapper(connType);
                if (null == pcWrapper) return;
                KLog.p(KLog.ERROR, "set sdp failed, error info:%s. %s", error, pcWrapper);
                // 业务要求失败就重新创建peerconnection
                recreatePeerConnection(pcWrapper.connType);
            });
        }

    }


    private class PCObserver implements PeerConnection.Observer{
        private static final String TAG = "PCObserver";
        private ConnType connType;

        PCObserver(ConnType connType) {
            this.connType = connType;
        }

        @Override
        public void onIceCandidate(final IceCandidate candidate) {
            KLog.tp(TAG, KLog.INFO, "onIceCandidate, connType=%s, sending candidate...", connType);
            handler.post(() -> {
                PeerConnectionWrapper pcWrapper = getPcWrapper(connType);
                if (null == pcWrapper) return;

                rtcConnector.sendIceCandidate(pcWrapper.connType.ordinal(), candidate.sdpMid, candidate.sdpMLineIndex, candidate.sdp);
            });
        }

        @Override
        public void onIceCandidatesRemoved(final IceCandidate[] candidates) {
            KLog.tp(TAG,KLog.INFO, "onIceCandidatesRemoved connType=%s", connType);
        }


        @Override
        public void onSignalingChange(PeerConnection.SignalingState newState) {
            KLog.tp(TAG,KLog.INFO, "onSignalingChange connType=%s, newState=%s", connType, newState);
        }

        @Override
        public void onIceConnectionChange(final PeerConnection.IceConnectionState newState) {
            KLog.tp(TAG,KLog.INFO, "onIceConnectionChange connType=%s, newState=%s", connType, newState);
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
            KLog.tp(TAG,KLog.INFO, "onConnectionChange connType=%s, newState=%s", connType, newState);

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
            KLog.tp(TAG,KLog.INFO, "onIceGatheringChange connType=%s, newState=%s", connType, newState);
        }

        @Override
        public void onIceConnectionReceivingChange(boolean receiving) {
            KLog.tp(TAG,KLog.INFO, "onIceConnectionReceivingChange connType=%s receiving=%s", connType, receiving);
        }

        @Override
        public void onAddStream(MediaStream mediaStream) {
            KLog.tp(TAG,KLog.INFO, "onAddStream connType=%s, mediaStream.hashcode=%s, mediaStream.id=%s", connType, mediaStream.hashCode(), mediaStream.getId());
            Stream.of(mediaStream.videoTracks).forEach(it-> {
                KLog.tp(TAG,KLog.INFO, "onAddStream videoTrack: id=%s, type=%s, enabled=%s, state=%s", it.id(), it.kind(), it.enabled(), it.state());
            });
            Stream.of(mediaStream.audioTracks).forEach(it-> {
                KLog.tp(TAG,KLog.INFO, "onAddStream audioTrack: id=%s, type=%s, enabled=%s, state=%s", it.id(), it.kind(), it.enabled(), it.state());
            });
            Stream.of(mediaStream.preservedVideoTracks).forEach(it-> {
                KLog.tp(TAG,KLog.INFO, "onAddStream preservedVideoTrack: id=%s, type=%s, enabled=%s, state=%s", it.id(), it.kind(), it.enabled(), it.state());
            });
//            handler.post(() -> {
//            });
        }

        @Override
        public void onRemoveStream(final MediaStream stream) {
            // 我们在onTrack回调中createRemoteVideoTrack/createRemoteAudioTrack，
            // 相应的我们原本期望在onRemoveStream（没有onRemoveTrack）中removeRemoteVideoTrack/removeRemoteAudioTrack，
            // 然而实测下来发现此回调上来时MediaStream的track列表为空，不得已我们只得在StreamLeft消息上来时
            // removeRemoteVideoTrack/removeRemoteAudioTrack，由此造成了这种不对称的现象
            KLog.tp(TAG,KLog.INFO, "onRemoveStream connType=%s, mediaStream.hashcode=%s", connType, stream.hashCode());
            Stream.of(stream.videoTracks).forEach(it -> {
                KLog.tp(TAG,KLog.INFO, "onRemoveStream videoTrack: id=%s, type=%s, enabled=%s, state=%s", it.id(), it.kind(), it.enabled(), it.state());
            });
            Stream.of(stream.audioTracks).forEach(it -> {
                KLog.tp(TAG,KLog.INFO, "onRemoveStream audioTrack: id=%s, type=%s, enabled=%s, state=%s", it.id(), it.kind(), it.enabled(), it.state());
            });
            Stream.of(stream.preservedVideoTracks).forEach(it -> {
                KLog.tp(TAG,KLog.INFO, "onRemoveStream preservedVideoTrack: id=%s, type=%s, enabled=%s, state=%s", it.id(), it.kind(), it.enabled(), it.state());
            });

//            handler.post(() -> {
//            });
        }

        @Override
        public void onDataChannel(final DataChannel dc) {
            KLog.tp(TAG, KLog.INFO, "onDataChannel, connType=%s", connType);
        }

        @Override
        public void onRenegotiationNeeded() {
            KLog.tp(TAG, KLog.INFO, "onRenegotiationNeeded, connType=%s", connType);
            // No need to do anything; AppRTC follows a pre-agreed-upon
            // signaling/negotiation protocol.
        }

        @Override
        public void onAddTrack(RtpReceiver rtpReceiver, MediaStream[] mediaStreams) {
            Stream.of(mediaStreams).forEach(stream -> {
                KLog.tp(TAG,KLog.INFO, "onAddTrack connType=%s, mediaStream.hashcode=%s, stream.id=%s", connType, stream.hashCode(), stream.getId());
                Stream.of(stream.videoTracks).forEach(it -> {
                    KLog.tp(TAG,KLog.INFO, "onAddTrack: id=%s, type=%s, enabled=%s, state=%s", it.id(), it.kind(), it.enabled(), it.state());
                });
                Stream.of(stream.audioTracks).forEach(it -> {
                    KLog.tp(TAG,KLog.INFO, "onAddTrack: id=%s, type=%s, enabled=%s, state=%s", it.id(), it.kind(), it.enabled(), it.state());
                });
                Stream.of(stream.preservedVideoTracks).forEach(it -> {
                    KLog.tp(TAG,KLog.INFO, "onAddTrack: id=%s, type=%s, enabled=%s, state=%s", it.id(), it.kind(), it.enabled(), it.state());
                });
            });

//            handler.post(() -> {
//            });
        }


        /*
        * 远端码流上来时会相继触发: onTrack, onAddTrack, onAddStream
        * */
        @Override
        public void onTrack(RtpTransceiver transceiver) {
            MediaStreamTrack track = transceiver.getReceiver().track();
            KLog.tp(TAG, KLog.INFO, "onTrack connType %s ,mid=%s, received remote track %s", connType, transceiver.getMid(), track != null ? track.id() : null);
            handler.post(() -> {
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



    private List<RtpParameters.Encoding> createEncodingListForSendingOfferSdp(){
        List<RtpParameters.Encoding> encodings = new ArrayList<>();
        if (config.isSimulcastEnabled) {
            // 从低到高，平台要求的。
            RtpParameters.Encoding low = new RtpParameters.Encoding("l", true, 4.0);
            low.maxFramerate = config.videoFps;
            low.maxBitrateBps = config.videoMaxBitrate * 1024;
            RtpParameters.Encoding medium = new RtpParameters.Encoding("m", true, 2.0);
            medium.maxFramerate = config.videoFps;
            medium.maxBitrateBps = config.videoMaxBitrate * 1024;
            encodings.add(low);
            encodings.add(medium);
        }

        RtpParameters.Encoding high = new RtpParameters.Encoding("h", true, 1.0);
        high.maxFramerate = config.videoFps;
        high.maxBitrateBps = config.videoMaxBitrate * 1024;
        encodings.add(high);

        return encodings;
    }


    private List<RtpParameters.Encoding> createEncodingList(List<RtpParameters.Encoding> encodings){
        if (null != encodings) {
            // NOTE：注意和sendOffer时传给业务组件的参数一致。
            if (config.isSimulcastEnabled) {
                for (RtpParameters.Encoding encoding : encodings) {
                    if (encoding.rid.equals("l")) {
                        encoding.scaleResolutionDownBy = 4.0;
                        encoding.maxFramerate = config.videoFps;
                        encoding.maxBitrateBps = config.videoMaxBitrate * 1024;
                    } else if (encoding.rid.equals("m")) {
                        encoding.scaleResolutionDownBy = 2.0;
                        encoding.maxFramerate = config.videoFps;
                        encoding.maxBitrateBps = config.videoMaxBitrate * 1024;
                    } else if (encoding.rid.equals("h")) {
                        encoding.scaleResolutionDownBy = 1.0;
                        encoding.maxFramerate = config.videoFps;
                        encoding.maxBitrateBps = config.videoMaxBitrate * 1024;
                    }
                    KLog.p("encoding: rid=%s, scaleResolutionDownBy=%s, maxFramerate=%s, maxBitrateBps=%s",
                            encoding.rid, encoding.scaleResolutionDownBy, encoding.maxFramerate, encoding.maxBitrateBps);
                }
            }else {
                RtpParameters.Encoding encoding = encodings.get(0);
                encoding.scaleResolutionDownBy = 1.0;
                encoding.maxFramerate = config.videoFps;
                encoding.maxBitrateBps = config.videoMaxBitrate * 1024;
                KLog.p("encoding.size=%s, encoding[0]: rid=%s, scaleResolutionDownBy=%s, maxFramerate=%s, maxBitrateBps=%s",
                        encodings.size(), encoding.rid, encoding.scaleResolutionDownBy, encoding.maxFramerate, encoding.maxBitrateBps);
            }

        }else{
            /* NOTE
             track被添加之前scaleResolutionDownBy必须设置为null否则会崩溃，提示
             "Fatal error: C++ addTransceiver failed"。
             等到track被添加之后，sdp被创建之前，
             通过sender.getParameters()获取encodings列表，然后给scaleResolutionDownBy赋予真实的值。
             */
            encodings = new ArrayList<>();
            if (config.isSimulcastEnabled) {
                encodings.add(new RtpParameters.Encoding("l", true, null));
                encodings.add(new RtpParameters.Encoding("m", true, null));
            }
            encodings.add(new RtpParameters.Encoding("h", true, null));
        }

        return encodings;

    }



    private static final String STREAM_ID = "TT-Android-"+System.currentTimeMillis();
    private static final String LOCAL_VIDEO_TRACK_ID = STREAM_ID+"-v";
    private static final String LOCAL_WINDOW_TRACK_ID = STREAM_ID+"-window";
    private static final String LOCAL_SCREEN_TRACK_ID = STREAM_ID+"-screen";
    private static final String LOCAL_AUDIO_TRACK_ID = STREAM_ID+"-a";
    private static int audioTrackCnt = 0;


    private enum SdpType{
        UNKNOWN,
        OFFER,
        ANSWER,
        AUDIO_OFFER,
        VIDEO_OFFER,
        FINGERPRINT_OFFER,
    }


    private enum SdpState{
        IDLE,
        CREATING,
        SETTING_LOCAL,
        SENDING,
        SETTING_REMOTE,
        SET_LOCAL_SUCCESS,
        SET_REMOTE_SUCCESS,
    }


    /**
     * PeerConnection包装类
     * */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private class PeerConnectionWrapper{

        ConnType connType = ConnType.UNKNOWN;
        MediaType mediaType = MediaType.UNKNOWN;

        SdpType sdpType = SdpType.UNKNOWN;
        SdpState sdpState = SdpState.IDLE;

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
        // NOTE: 映射按理说该是1对1，但实际是多对1——多个mid对应同一个streamId，其中有些streamId是“死”的（但下面仍抛上来了）
        private final Map<String, String> mid2KdStreamIdMap = new HashMap<>();

        // 是否正在取消发布。由于协议组当前实现所限取消发布后我们需要重建PeerConnection
        private boolean isUnpublishing;

        PeerConnectionWrapper(ConnType connType, @NonNull SDPObserver sdpObserver) {
            this.connType = connType;
            this.sdpObserver = sdpObserver;
        }

        PeerConnectionWrapper(ConnType connType, @NonNull PeerConnection pc, @NonNull SDPObserver sdpObserver) {
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
         * 创建本地视频轨道。
         * */
        void createVideoTrack(@NonNull VideoCapturer videoCapturer){
            executor.execute(() -> {
                if (null == factory){
                    KLog.p(KLog.ERROR, "factory destroyed");
                    return;
                }
                if (null == pc){
                    KLog.p(KLog.ERROR, "peerConnection destroyed");
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
                    KLog.p("capture videoWidth=%s, videoHeight=%s, videoFps=%s", config.videoWidth, config.videoHeight, config.videoFps);
                    videoCapturer.startCapture(config.videoWidth, config.videoHeight, config.videoFps);
                }
                localVideoTrack = factory.createVideoTrack(localVideoTrackId, videoSource);
                localVideoTrack.setEnabled(bTrackEnable);

                if (config.isSimulcastEnabled) {
                    RtpTransceiver.RtpTransceiverInit transceiverInit = new RtpTransceiver.RtpTransceiverInit(
                            RtpTransceiver.RtpTransceiverDirection.SEND_ONLY,
                            Collections.singletonList(STREAM_ID),
                            createEncodingList(null)
                    );

                    RtpTransceiver transceiver = pc.addTransceiver(localVideoTrack, transceiverInit);
                    videoSender = transceiver.getSender();
                    createEncodingList(videoSender.getParameters().encodings);
                }else {
                    videoSender = pc.addTrack(localVideoTrack);
                    int maxBitrate = config.videoMaxBitrate * 1024;
                    int curBitrate = maxBitrate/2;
                    int minBitrate = Math.min(100 * 1024, curBitrate);
                    pc.setBitrate(minBitrate, curBitrate, maxBitrate);

                    KLog.p("pc.setBitrate min=%s, cur=%s, max=%s", minBitrate, curBitrate, maxBitrate);
                }

                String kdStreamId = localVideoTrackId;
                KLog.p("create local video track %s/%s", kdStreamId, localVideoTrackId);
                handler.post(() -> {
                    kdStreamId2RtcTrackIdMap.put(kdStreamId, localVideoTrackId);
                    if (localVideoTrackId.equals(LOCAL_VIDEO_TRACK_ID)) {
                        // 对于己端发送窗口共享、发送屏幕共享不需要回显，也没有对应的stream以及Conferee
                        myself.setVideoChannelState(Conferee.VideoChannelState.Bound);
                        myself.setVideoSignalState(Conferee.VideoSignalState.Normal);
                        executor.execute(() -> {
                            KLog.p("bind local video track %s to conferee %s", localVideoTrack.id(), myself.getId());
                            localVideoTrack.addSink(myself);  // 本地回显
                        });
                    }
                });

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
                handler.post(() -> {
                    String kdStreamId = kdStreamId2RtcTrackIdMap.inverse().get(trackId);
                    kdStreamId2RtcTrackIdMap.remove(kdStreamId);
                    if(trackId.equals(LOCAL_VIDEO_TRACK_ID)){ // 仅处理主流的情形，参见createVideoTrack
                        myself.setVideoChannelState(Conferee.VideoChannelState.Idle);
                        myself.setVideoSignalState(Conferee.VideoSignalState.Idle);
                        executor.execute(() -> {
                                KLog.p("unbind track %s from conferee %s", localVideoTrack.id(), myself.getId());
                                localVideoTrack.removeSink(myself);
                        });
                    }
                });

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
                handler.post(() -> {
                    kdStreamId2RtcTrackIdMap.put(kdStreamId, localAudioTrackId);
                    myself.setAudioChannelState(Conferee.AudioChannelState.Bound);
                    myself.setAudioSignalState(Conferee.AudioSignalState.Normal); // XXX 一个与会方可能有多个音轨
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
                String trackId = localAudioTrack.id();

                KLog.p("localAudioTrack %s removed ", trackId);

                handler.post(() -> {
                    String kdStreamId = kdStreamId2RtcTrackIdMap.inverse().get(trackId);
                    kdStreamId2RtcTrackIdMap.remove(kdStreamId);
                    myself.setAudioChannelState(Conferee.AudioChannelState.Idle);
                    myself.setAudioSignalState(Conferee.AudioSignalState.Idle); // XXX 一个与会方可能有多个音轨
                });

            });
        }


        void createRemoteVideoTrack(String mid, VideoTrack track){
            executor.execute(() -> {
                String kdStreamId = mid2KdStreamIdMap.get(mid);
                if (null == kdStreamId) {
                    KLog.p(KLog.ERROR,"kdStreamId related to mid "+mid+" doesn't exist? i should have got it from onSetOfferCmd()");
                    return;
                }
                remoteVideoTracks.put(kdStreamId, track);
                track.setEnabled(config.isRemoteVideoEnabled);
                KLog.p("create remote video track %s/%s", kdStreamId, track.id());

                handler.post(new Runnable() {
                    int retriedCount;
                    @Override
                    public void run() {
                        if (retriedCount >= 2){
                            return;
                        }
                        kdStreamId2RtcTrackIdMap.put(kdStreamId, track.id());
                        RtcStream remoteStream = findStream(kdStreamId);
                        if (null == remoteStream) {
                            KLog.p(KLog.ERROR, "stream related to kdStreamId "+kdStreamId+" doesn't exist? \n" +
                                    "please check StreamJoined/StreamList and onSetOfferCmd to make sure they both contain kdStreamId "+kdStreamId+"\n and also " +
                                    "make sure the stream corresponding to this kdStreamId have not left yet!");
                            return;
                        }
                        Conferee owner = remoteStream.getOwner();
                        if (null == owner){
                            KLog.p(KLog.ERROR, "owner of stream %s has not joined yet? wait then try again...");
                            handler.postDelayed(this, 2000);
                            ++retriedCount;
                            return;
                        }

                        owner.setVideoChannelState(Conferee.VideoChannelState.Bound);

                        if(owner.isVirtualAssStreamConferee()){
                            // 接收双流先展示缓冲图标
                            owner.setVideoSignalState(Conferee.VideoSignalState.Buffering);
                            handler.postDelayed(() -> {
                                if (owner.getVideoSignalState() == Conferee.VideoSignalState.Buffering) {
                                    owner.setVideoSignalState(Conferee.VideoSignalState.Normal);
                                }
                            }, 3000);
                        }else {
                            owner.setVideoSignalState(Conferee.VideoSignalState.Normal);
                        }

                        executor.execute(() -> {
                            KLog.p("bind track %s to conferee %s", track.id(), owner.getId());
                            track.addSink(owner);
                        });
                    }
                });
            });
        }

        void removeRemoteVideoTrack(RtcStream stream){
            String kdStreamId = stream.getStreamId();
            Conferee owner = stream.getOwner();
            executor.execute(() -> {
                for (String streamId : remoteVideoTracks.keySet()) {
                    if (streamId.equals(kdStreamId)) {
                        VideoTrack track = remoteVideoTracks.remove(streamId);
//                        track.dispose();
                        KLog.p("remote video track %s/%s removed", streamId, track.id());

                        handler.post(() -> {
                            kdStreamId2RtcTrackIdMap.remove(streamId);
                            if (null != owner) {
                                owner.setVideoChannelState(Conferee.VideoChannelState.Idle);
                                owner.setVideoSignalState(Conferee.VideoSignalState.Idle);
                                executor.execute(() -> {
                                    KLog.p("unbind track %s from conferee %s", track.id(), owner.getId());
                                    track.removeSink(owner);
                                });
                            }
                        });

                        return;
                    }
                }

                KLog.p(KLog.ERROR, "failed to removeRemoteVideoTrack, no such stream %s", kdStreamId);
            });
        }


        void createRemoteAudioTrack(String mid, AudioTrack track){
            executor.execute(() -> {
                String kdStreamId = mid2KdStreamIdMap.get(mid);
                if (null == kdStreamId) {
                    KLog.p(KLog.ERROR,"kdStreamId related to mid "+mid+" doesn't exist? i should have got it from onSetOfferCmd()");
                    return;
                }
                track.setEnabled(!config.isSilenced);
                remoteAudioTracks.put(kdStreamId, track);

                KLog.p("remote audio track %s/%s created", kdStreamId, track.id());

                handler.post(new Runnable() {
                    int retriedCount;
                    @Override
                    public void run() {
                        if (retriedCount>=2){
                            return;
                        }
                        kdStreamId2RtcTrackIdMap.put(kdStreamId, track.id());
                        RtcStream remoteStream = findStream(kdStreamId);
                        if (null == remoteStream) {
                            KLog.p(KLog.ERROR, "stream related to kdStreamId "+kdStreamId+" doesn't exist? \n" +
                                    "please check StreamJoined/StreamList and onSetOfferCmd to make sure they both contain kdStreamId "+kdStreamId+"\n and also " +
                                    "make sure the stream corresponding to this kdStreamId have not left yet!");
                            return;
                        }
                        Conferee owner = remoteStream.getOwner();
                        if (null == owner){
                            KLog.p(KLog.ERROR, "owner of stream %s has not joined yet? wait then try again...");
                            handler.postDelayed(this, 2000);
                            ++retriedCount;
                            return;
                        }

                        myself.setAudioChannelState(Conferee.AudioChannelState.Bound);
                        myself.setAudioSignalState(Conferee.AudioSignalState.Normal); // XXX 一个与会方可能有多个音轨
                    }
                });
            });
        }

        void removeRemoteAudioTrack(RtcStream stream){
            String kdStreamId = stream.getStreamId();
            Conferee owner = stream.getOwner();
            executor.execute(() -> {
                for (String streamId : remoteAudioTracks.keySet()) {
                    if (streamId.equals(kdStreamId)) {
                        AudioTrack track = remoteAudioTracks.remove(kdStreamId);
//                        track.dispose();
                        handler.post(() -> {
                            kdStreamId2RtcTrackIdMap.remove(kdStreamId);
                            if (owner != null && owner.getAudioStreams().isEmpty()){
                                myself.setAudioChannelState(Conferee.AudioChannelState.Idle);
                                myself.setAudioSignalState(Conferee.AudioSignalState.Idle); // XXX 一个与会方可能有多个音轨
                            }
                        });
                        KLog.p("stream %s removed", kdStreamId);
                        return;
                    }
                }

                KLog.p(KLog.ERROR, "failed to removeRemoteAudioTrack, no such track %s", kdStreamId);
            });
        }


        void destroyAudioTrack(){
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


        boolean isMediaType(MediaType mediaType) {
            return this.mediaType == mediaType;
        }

        void setMediaType(MediaType mediaType) {
            this.mediaType = mediaType;
        }

        void setSdpType(SdpType sdpType) {
//            KLog.up("set sdp type from %s to %s", this.sdpType, sdpType);
            this.sdpType = sdpType;
        }

        boolean isSdpType(SdpType sdpType){
            return sdpType == this.sdpType;
        }

        void setSdpState(SdpState sdpState) {
//            KLog.up("switch sdp state from %s to %s", this.sdpState, sdpState);
            this.sdpState = sdpState;
        }

        boolean isSdpState(SdpState sdpState){
            return sdpState == this.sdpState;
        }

        boolean checkSdpState(SdpState... sdpStates) {
            for (SdpState state : sdpStates){
                if (state == this.sdpState){
                    return true;
                }
            }
            StringBuilder sb=new StringBuilder();
            for (SdpState state:sdpStates){
                sb.append(state).append(",");
            }
            KLog.p(KLog.ERROR, "invalid sdp sate, expect "+sb+" but current is "+this.sdpState);
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

                sdpState = SdpState.IDLE;

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

        @Override
        public String toString() {
            return "PeerConnectionWrapper{" +
                    "connType=" + connType +
                    ", mediaType=" + mediaType +
                    ", sdpType=" + sdpType +
                    ", sdpState=" + sdpState +
                    ", isUnpublishing=" + isUnpublishing +
                    '}';
        }
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
                        printStats(publisherStats, false);
                    }
                });
            }
            if (null != subPcWrapper && null != subPcWrapper.pc) {
                subPcWrapper.pc.getStats(rtcStatsReport -> {
                    synchronized (subscriberStats) {
                        StatsHelper.resolveStats(rtcStatsReport, subscriberStats);
                        KLog.p("####subscriberStats: ");
                        printStats(subscriberStats, false);
                    }
                });
            }

//            if (null != assPubPcWrapper && null != assPubPcWrapper.pc) {
//                assPubPcWrapper.pc.getStats(rtcStatsReport -> {
//                    KLog.p("####assPublisherStats: ");
//                    synchronized (assPublisherStats) {
//                        StatsHelper.resolveStats(rtcStatsReport, assPublisherStats);
//                        printStats(assPublisherStats, false);
//                    }
//                });
//            }
//            if (null != assSubPcWrapper && null != assSubPcWrapper.pc) {
//                assSubPcWrapper.pc.getStats(rtcStatsReport -> {
//                    KLog.p("####assSubscriberStats: ");
//                    synchronized (assSubscriberStats) {
//                        StatsHelper.resolveStats(rtcStatsReport, assPublisherStats);
//                        printStats(assPublisherStats, false);
//                    }
//                });
//            }

            handler.postDelayed(this, 2000);
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
                    KLog.p("local audioLevel= %s", maxAudioLevel);
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
                    if (!maxAudioLevelKdStreamId.equals(preMaxAudioLevelKdStreamId)) { // 说话人变化了才需要刷新语音激励状态
                        Conferee conferee = findConfereeByStreamId(preMaxAudioLevelKdStreamId);
                        if (null != conferee && Conferee.AudioSignalState.Activated == conferee.getAudioSignalState()) {
                            conferee.setAudioSignalState(Conferee.AudioSignalState.Normal);
                        }
                        conferee = findConfereeByStreamId(maxAudioLevelKdStreamId);
                        if (null != conferee && Conferee.AudioSignalState.Normal == conferee.getAudioSignalState()) {
                            conferee.setAudioSignalState(Conferee.AudioSignalState.Activated);
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
                if (null != conferee && Conferee.AudioSignalState.Activated == conferee.getAudioSignalState()){
                    conferee.setAudioSignalState(Conferee.AudioSignalState.Normal);
                }
                preMaxAudioLevelKdStreamId = null;
            }

            handler.postDelayed(this, 2000);
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
                    Conferee.VideoSignalState vidSigState = conferee.getVideoSignalState();
                    if (Conferee.VideoSignalState.Normal == vidSigState
                            && interval >= WeakSignalCheckInterval // 计算帧率的间隔时长应适度，太短则太敏感地滑入WeakSignal状态，太长则太迟钝
                            && fps < 0.2 // 可忍受的帧率下限，低于该下限则认为信号丢失
                    ){
                        conferee.setVideoSignalState(Conferee.VideoSignalState.Weak);
                    }else if (Conferee.VideoSignalState.Weak == vidSigState && fps > 1){
                        // 尽管我们在帧率低于0.2时将状态设为WeakSignal但当帧率超过0.2时我们不立马设置状态为Normal
                        // 而是等帧率回复到较高水平才切回Normal以使状态切换显得平滑而不是在临界值处频繁切换
                        conferee.setVideoSignalState(Conferee.VideoSignalState.Normal);
                    }
                }else{
                    KLog.p(KLog.ERROR, "track %s(kdstreamId=%s) not belong to any conferee?", trackIdentifier, lostSignalKdStreamId);
                }
            }

            if (curTimestamp - videoStatsTimeStamp >= WeakSignalCheckInterval) {
                preReceivedFramesMap = framesReceivedMap;
                videoStatsTimeStamp = curTimestamp;
            }

            handler.postDelayed(this, 2000);
        }
    };


    private boolean enableStatsLog = true;
    public void setStatsLogEnable(boolean enable){
        enableStatsLog = enable;
    }


    private void printStats(StatsHelper.Stats stats, boolean detail){
        if (!enableStatsLog){
            return;
        }
        KLog.p(">>>>>>>>>> stats begin");
        // 因为android log一次输出有最大字符限制，所以我们分段输出
        if (detail) {
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
                    KLog.p(conferee != null ? conferee.alias + " " + audioTrack : audioTrack.toString());
                }
            }
            if (null != stats.audioInboundRtpList) {
                for (StatsHelper.AudioInboundRtp audioInbound : stats.audioInboundRtpList) {
                    KLog.p(audioInbound.toString());
                }
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

        if (detail) {
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
        }

        KLog.p(">>>>>>>>>> stats end");
    }

}
