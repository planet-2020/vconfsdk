package com.kedacom.vconf.sdk.webrtc;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.media.projection.MediaProjection;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.annimon.stream.function.Consumer;
import com.annimon.stream.function.Predicate;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.kedacom.vconf.sdk.amulet.Caster;
import com.kedacom.vconf.sdk.amulet.INtfListener;
import com.kedacom.vconf.sdk.amulet.IResultListener;
import com.kedacom.vconf.sdk.common.type.BaseTypeBool;
import com.kedacom.vconf.sdk.common.type.BaseTypeInt;
import com.kedacom.vconf.sdk.common.type.Converter;
import com.kedacom.vconf.sdk.common.type.HangupConfReason;
import com.kedacom.vconf.sdk.common.type.transfer.EmAPIVersionType;
import com.kedacom.vconf.sdk.common.type.transfer.EmConfProtocol;
import com.kedacom.vconf.sdk.common.type.transfer.EmMtAliasType;
import com.kedacom.vconf.sdk.common.type.transfer.EmMtChanState;
import com.kedacom.vconf.sdk.common.type.transfer.EmMtModifyConfInfoType;
import com.kedacom.vconf.sdk.common.type.transfer.EmMtResolution;
import com.kedacom.vconf.sdk.common.type.transfer.EmMtVmpMode;
import com.kedacom.vconf.sdk.common.type.transfer.TAssVidStatus;
import com.kedacom.vconf.sdk.common.type.transfer.TMTEntityInfo;
import com.kedacom.vconf.sdk.common.type.transfer.TMTEntityInfoList;
import com.kedacom.vconf.sdk.common.type.transfer.TMtAlias;
import com.kedacom.vconf.sdk.common.type.transfer.TMtAssVidStatusList;
import com.kedacom.vconf.sdk.common.type.transfer.TMtCallLinkSate;
import com.kedacom.vconf.sdk.common.type.transfer.TMtCustomVmpParam;
import com.kedacom.vconf.sdk.common.type.transfer.TMtEntityStatus;
import com.kedacom.vconf.sdk.common.type.transfer.TMtId;
import com.kedacom.vconf.sdk.common.type.transfer.TMtIdList;
import com.kedacom.vconf.sdk.common.type.transfer.TMtSimpConfInfo;
import com.kedacom.vconf.sdk.common.type.transfer.TRegResultNtf;
import com.kedacom.vconf.sdk.common.type.transfer.TSelectedToWatch;
import com.kedacom.vconf.sdk.common.type.transfer.TShortMsg;
import com.kedacom.vconf.sdk.common.type.transfer.TSrvStartResult;
import com.kedacom.vconf.sdk.utils.collection.EvictingDeque;
import com.kedacom.vconf.sdk.utils.log.KLog;
import com.kedacom.vconf.sdk.utils.math.MatrixHelper;
import com.kedacom.vconf.sdk.webrtc.CommonDef.ConnType;
import com.kedacom.vconf.sdk.webrtc.CommonDef.MediaType;
import com.kedacom.vconf.sdk.webrtc.bean.ConfInfo;
import com.kedacom.vconf.sdk.webrtc.bean.ConfInvitationInfo;
import com.kedacom.vconf.sdk.webrtc.bean.ConfManSMS;
import com.kedacom.vconf.sdk.webrtc.bean.ConfPara;
import com.kedacom.vconf.sdk.webrtc.bean.ConfType;
import com.kedacom.vconf.sdk.webrtc.bean.CreateConfResult;
import com.kedacom.vconf.sdk.webrtc.bean.MakeCallResult;
import com.kedacom.vconf.sdk.webrtc.bean.Statistics;
import com.kedacom.vconf.sdk.webrtc.bean.trans.TAPIVersion;
import com.kedacom.vconf.sdk.webrtc.bean.trans.TConfSettingsModified;
import com.kedacom.vconf.sdk.webrtc.bean.trans.TCreateConfResult;
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
import org.webrtc.VideoFileRenderer;
import org.webrtc.VideoFrame;
import org.webrtc.VideoSink;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;
import org.webrtc.audio.AudioDeviceModule;
import org.webrtc.audio.JavaAudioDeviceModule;
import org.webrtc.voiceengine.WebRtcAudioUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import pattern.ConditionalConsumer;


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

    // 码流集合（没有己端的码流，业务组件没有上报）
    // 所有码流均能在与会方集合中找到owner
    private Set<KdStream> streams = new HashSet<>();

    private Set<Display> displays = new LinkedHashSet<>();

    // 平台的StreamId到WebRTC的TrackId之间的映射
    private BiMap<String, String> kdStreamId2RtcTrackIdMap = HashBiMap.create();

    // 当前用户的e164
    private String userE164;

    private RtcConnector.TCallInfo callInfo;

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
        // 启动服务过程中该模块其它请求禁止下发
        disableReq(true);

        String serviceName = "mtrtcservice";
        req(false, true, Msg.StartMtService, new SessionProcessor<Msg>() {
            int retriedCount = 0;

            @Override
            public void onRsp(Msg rsp, Object rspContent, IResultListener resultListener, boolean isFinal, Msg req, Object[] reqParas, boolean[] isConsumed) {
                // 取消禁令
                disableReq(false);

                TSrvStartResult result = (TSrvStartResult) rspContent;
                if (!result.AssParam.achSysalias.equals(serviceName)){
                    isConsumed[0] = false;
                    return;
                }
                boolean success = result.MainParam.basetype;
                if (success){
                    KLog.p("start service %s success!", serviceName);
                }
            }

            @Override
            public void onTimeout(IResultListener resultListener, Msg req, Object[] reqParas, boolean[] isConsumed) {
                if (retriedCount < 2) {
                    ++retriedCount;
                    isConsumed[0] = true;
                    req(false, true, req, this, resultListener, reqParas);
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
     * */
    public void login(@NonNull String e164, @NonNull IResultListener resultListener){
        req(Msg.GetSvrAddr, new SessionProcessor<Msg>() {
            @Override
            public void onReqSent(IResultListener resultListener, Msg req, Object[] reqParas, Object output) {
                TMtRtcSvrAddr rtcSvrAddr = (TMtRtcSvrAddr) output;
                if (null == rtcSvrAddr || (rtcSvrAddr.dwIp<= 0 && rtcSvrAddr.achIpv6.isEmpty())){
                    KLog.p(KLog.ERROR, "invalid rtcSvrAddr");
                    reportFailed(-1, resultListener);
                    return;
                }

                userE164 = e164;
                rtcSvrAddr.bUsedRtc = true;
                rtcSvrAddr.achNumber = e164;

                req(Msg.Login, new SessionProcessor<Msg>() {
                    @Override
                    public void onRsp(Msg rsp, Object rspContent, IResultListener resultListener, boolean isFinal, Msg req, Object[] reqParas, boolean[] isConsumed) {
                        TRegResultNtf loginResult = (TRegResultNtf) rspContent;
                        int resCode = RtcResultCode.trans(rsp, loginResult.AssParam.basetype);
                        if (RtcResultCode.LoggedIn == resCode) {
                            reportSuccess(null, resultListener);
                        } else if (RtcResultCode.LoggedOut == resCode){
                            isConsumed[0] = false; // 该条消息可能是上一次注销请求等待超时未消费，导致遗留至此的，此非我们期望的消息，继续等待后续的。
                        } else if (RtcResultCode.UnknownServerAddress == resCode){
                            isConsumed[0] = false; //组件在重连，我们静静等待吧
                        } else {
                            reportFailed(resCode, resultListener);
                        }
                    }
                }, resultListener, rtcSvrAddr);

            }
        }, resultListener);

    }


    /**
     * 注销rtc
     * @param resultListener onSuccess null
     *                       onFailed errorCode // TODO
     * */
    public void logout(IResultListener resultListener){
        userE164 = null;
        stopSession();
        req(Msg.Logout, new SessionProcessor<Msg>() {
            @Override
            public void onRsp(Msg rsp, Object rspContent, IResultListener resultListener, boolean isFinal, Msg req, Object[] reqParas, boolean[] isConsumed) {
                TRegResultNtf loginResult = (TRegResultNtf) rspContent;
                int resCode = RtcResultCode.trans(rsp, loginResult.AssParam.basetype);
                if (RtcResultCode.LoggedOut == resCode) {
                    reportSuccess(null, resultListener);
                } else if (RtcResultCode.LoggedIn == resCode){
                    isConsumed[0] = false;
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
     * */
    public void makeCall(String peerId, boolean bAudio, @NonNull IResultListener resultListener){
        if (!startSession()){
            reportFailed(-1, resultListener);
            return;
        }
        req(Msg.Call, new SessionProcessor<Msg>() {
                    @Override
                    public void onRsp(Msg rsp, Object rspContent, IResultListener resultListener, boolean isFinal, Msg req, Object[] reqParas, boolean[] isConsumed) {
                        switch (rsp){
                            case Calling:
                                TMtCallLinkSate sate = (TMtCallLinkSate) rspContent;
                                if (EmConfProtocol.emrtc != sate.emConfProtocol){
                                    isConsumed[0] = false;
                                }
                                break;
                            case MultipartyConfStarted:
                                sate = (TMtCallLinkSate) rspContent;
                                if (EmConfProtocol.emrtc == sate.emConfProtocol){
                                    reportSuccess(ToDoConverter.callLinkState2MakeCallResult(sate, bAudio), resultListener);
                                }else{
                                    isConsumed[0] = false;
                                }
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
     * */
    public void createConf(ConfPara confPara, @NonNull IResultListener resultListener){
        if (!startSession()){
            reportFailed(-1, resultListener);
            return;
        }
        if (confPara.bSelfAudioMannerJoin){
            // 音频入会则关闭己端主视频通道。底层上报onGetOfferCmd时带的媒体类型就为Audio了。
            req(Msg.CloseMyMainVideoChannel, null, null);
        }
        req(Msg.GetAPIVersion, new SessionProcessor<Msg>() {
            @Override
            public void onRsp(Msg rsp, Object rspContent, IResultListener resultListener, boolean isFinal, Msg req, Object[] reqParas, boolean[] isConsumed) {
                TAPIVersion version = (TAPIVersion) rspContent;
                if (version.MainParam.dwErrorID == 1000){
                    confPara.confType = version.AssParam.dwAPILevel <= 1 ? ConfType.RTC : ConfType.AUTO;
                }else{
                    confPara.confType = ConfType.AUTO;
                    KLog.p(KLog.ERROR, "GetAPIVersion failed, errorCode=%s", version.MainParam.dwErrorID);
                }

                req(Msg.CreateConf, new SessionProcessor<Msg>() {
                    @Override
                    public void onRsp(Msg rsp, Object rspContent, IResultListener resultListener, boolean isFinal, Msg req, Object[] reqParas, boolean[] isConsumed) {
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
                                reportSuccess(ToDoConverter.callLinkState2CreateConfResult( (TMtCallLinkSate) rspContent, confPara.bAudio, confPara.bSelfAudioMannerJoin), resultListener);
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
                }, resultListener, ToDoConverter.confPara2CreateConference(confPara));

            }

            @Override
            public void onTimeout(IResultListener resultListener, Msg req, Object[] reqParas, boolean[] isConsumed) {
                stopSession();
            }

        }, resultListener, EmAPIVersionType.emMcAPIVersion_Api);

    }


    /**
     * 退出会议。
     * @param reason 退会原因码
     * @param resultListener onSuccess null
     *                       onFailed
     * */
    public void quitConf(HangupConfReason reason, IResultListener resultListener){
        if (!stopSession()){
            reportFailed(-1, resultListener);
            return;
        }
        req(Msg.QuitConf, new SessionProcessor<Msg>() {
            @Override
            public void onRsp(Msg rsp, Object rspContent, IResultListener resultListener, boolean isFinal, Msg req, Object[] reqParas, boolean[] isConsumed) {
                BaseTypeInt reason = (BaseTypeInt) rspContent;
                int resCode = RtcResultCode.trans(req, reason.basetype);
                // TODO 判断resCode
                reportSuccess(null, resultListener);
            }
        }, resultListener, Converter.HangupConfReason2EmMtCallDisReason(reason));
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
            public void onRsp(Msg rsp, Object rspContent, IResultListener resultListener, boolean isFinal, Msg req, Object[] reqParas, boolean[] isConsumed) {
                // TODO 判断resCode
                reportSuccess(null, resultListener);
            }
        }, resultListener);
    }

    /**
     * 延长会议
     * @param duration 需要延长的时长。单位：分钟
     * @param resultListener onSuccess int // 延长后的会议时长。单位：分钟
     *      *                onTimeout
     * */
    public void prolongConf(int duration, IResultListener resultListener){
        req(Msg.ProlongConf, new SessionProcessor<Msg>() {
            @Override
            public void onRsp(Msg rsp, Object rspContent, IResultListener resultListener, boolean isFinal, Msg req, Object[] reqParas, boolean[] isConsumed) {
                BaseTypeInt res = (BaseTypeInt) rspContent;
                reportSuccess(res.basetype, resultListener);
            }
        }, resultListener, duration);
    }

    /**
     * 接受会议邀请
     * NOTE：目前只支持同时开一个会。如果呼叫中/创建中或会议中状态，则返回失败，需要先退出会议状态。
     * @param bAudio 是否音频方式入会
     * @param resultListener onSuccess {@link MakeCallResult}
     * */
    public void acceptInvitation(boolean bAudio, @NonNull IResultListener resultListener){
        if (!startSession()){
            reportFailed(-1, resultListener);
            return;
        }
        if (bAudio) {
            // 音频入会则关闭己端主视频通道。底层上报onGetOfferCmd时带的媒体类型就为Audio了。
            req(Msg.CloseMyMainVideoChannel, null, null);
        }
        req(Msg.AcceptInvitation, new SessionProcessor<Msg>() {
            @Override
            public void onRsp(Msg rsp, Object rspContent, IResultListener resultListener, boolean isFinal, Msg req, Object[] reqParas, boolean[] isConsumed) {
                reportSuccess(ToDoConverter.callLinkState2MakeCallResult((TMtCallLinkSate) rspContent, bAudio), resultListener);
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
            public void onRsp(Msg rsp, Object rspContent, IResultListener resultListener, boolean isFinal, Msg req, Object[] reqParas, boolean[] isConsumed) {
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
            public void onRsp(Msg rsp, Object rspContent, IResultListener resultListener, boolean isFinal, Msg req, Object[] reqParas, boolean[] isConsumed) {
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
            public void onRsp(Msg rsp, Object rspContent, IResultListener resultListener, boolean isFinal, Msg req, Object[] reqParas, boolean[] isConsumed) {
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
            public void onRsp(Msg rsp, Object rspContent, IResultListener resultListener, boolean isFinal, Msg req, Object[] reqParas, boolean[] isConsumed) {
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

        if (bSilence == config.isSilenced){
            reportSuccess(null, resultListener);
            return;
        }
        req(Msg.SetSilence, new SessionProcessor<Msg>() {
            @Override
            public void onRsp(Msg rsp, Object rspContent, IResultListener resultListener, boolean isFinal, Msg req, Object[] reqParas, boolean[] isConsumed) {
                BaseTypeBool result = (BaseTypeBool) rspContent;
                if (result.basetype == bSilence && doSetSilence(bSilence)){
                    config.isSilenced = bSilence;
                    reportSuccess(null, resultListener);
                }else{
                    reportFailed(RtcResultCode.Failed, resultListener);
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
        return true;
    }

    private boolean doSetMute(boolean mute){
        myself.setMuted(mute);
        PeerConnectionWrapper pcWrapper = getPcWrapper(ConnType.PUBLISHER);
        if (null == pcWrapper) {
            KLog.p(KLog.ERROR,"null == pcWrapper");
            return false;
        }
        pcWrapper.setLocalAudioEnable(!mute);
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
        if (bMute == config.isMuted){
            reportSuccess(null, resultListener);
            return;
        }
        req(Msg.SetMute, new SessionProcessor<Msg>() {
            @Override
            public void onRsp(Msg rsp, Object rspContent, IResultListener resultListener, boolean isFinal, Msg req, Object[] reqParas, boolean[] isConsumed) {
                BaseTypeBool result = (BaseTypeBool) rspContent;
                if (result.basetype == bMute){ // 设置成功
                    doSetMute(bMute);
                    reportSuccess(null, resultListener);
                }else{
                    reportFailed(RtcResultCode.Failed, resultListener);
                }
            }

        }, resultListener, bMute);
    }


    /**
     * 哑音其他与会方。
     * @param e164 哑音对象的e164
     * @param bMute true，哑音；false，取消哑音
     * */
    public void setMuteOther(String e164, boolean bMute, IResultListener resultListener){
        Conferee conferee = findConfereeByE164(e164, Conferee.ConfereeType.Normal);
        if (conferee == null){
            KLog.p(KLog.ERROR, "no such conferee %s", e164);
            reportFailed(RtcResultCode.Failed, resultListener);
            return;
        }
        req(Msg.SetMuteOther, new SessionProcessor<Msg>() {
            @Override
            public void onRsp(Msg rsp, Object rspContent, IResultListener resultListener, boolean isFinal, Msg req, Object[] reqParas, boolean[] isConsumed) {
                TMtEntityStatus status = (TMtEntityStatus) rspContent;
                if (status.dwMcuId==conferee.mcuId && status.dwTerId==conferee.terId && status.tStatus.bIsMute == bMute){
                    conferee.setMuted(bMute);
                    reportSuccess(null, resultListener);
                }else{
                    isConsumed[0] = false;
                }
            }
        }, resultListener, new TMtId(conferee.mcuId, conferee.terId), bMute);
    }


    /**
     * 设置哑音。
     * @param bMute 是否哑音。true哑音，false取消哑音
     * */
    public void setMuteMeeting(boolean bMute, IResultListener resultListener) {
        if (!bSessionStarted) {
            KLog.p(KLog.ERROR, "session not start");
            reportFailed(-1, resultListener);
            return;
        }
        req(Msg.SetMuteMeeting, new SessionProcessor<Msg>() {
            @Override
            public void onRsp(Msg rsp, Object rspContent, IResultListener resultListener, boolean isFinal, Msg req, Object[] reqParas, boolean[] isConsumed) {
                TConfSettingsModified res = (TConfSettingsModified) rspContent;
                if (res.MainParam.basetype == EmMtModifyConfInfoType.MT_MODIFY_CONF_DUMB.ordinal()){
                    reportSuccess(null, resultListener);
                }else{
                    isConsumed[0] = false;
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

    private boolean showStatistics;
    /**
     * 设置是否展示统计信息
     * */
    public void setShowStatistics(boolean show){
        if (showStatistics != show){
            showStatistics = show;
            Stream.of(conferees).forEach(Conferee::refreshDisplays);
            if (null != myself) {
                myself.refreshDisplays();
            }
        }
    }


    // 强制画面合成是否已开启
    private boolean forceScenesComposited;

    private final Runnable reloginFailedRunnable = () -> Stream.of(getNtfListeners(LoginStateChangedListener.class))
            .forEach(it -> it.onLoginStateChanged(RtcResultCode.UnknownServerAddress));
    @Override
    protected void onNtf(Msg ntf, Object ntfContent) {

        switch (ntf){
            case LoginStateChanged:
                handler.removeCallbacks(reloginFailedRunnable);
                TRegResultNtf regState = (TRegResultNtf) ntfContent;
                int resCode = RtcResultCode.trans(ntf, regState.AssParam.basetype);
                if (RtcResultCode.LoggedIn != resCode) {
                    if (RtcResultCode.UnknownServerAddress == resCode){
                        // 组件在重连，我们静静等待
                        handler.postDelayed(reloginFailedRunnable,
                                40*1000 // 业务组件每次重连的最大耗时
                        );
                    }else {
                        Stream.of(getNtfListeners(LoginStateChangedListener.class)).forEach(it -> it.onLoginStateChanged(resCode));
                    }
                }
                break;

            case CallIncoming:
                ConfInvitationInfo invitationInfo = ToDoConverter.callLinkSate2ConfInvitationInfo((TMtCallLinkSate) ntfContent);
                Stream.of(getNtfListeners(ConfInvitationListener.class)).forEach(it -> it.onConfInvitation(invitationInfo));
                break;

            case MultipartyConfEnded:
                Stream.of(getNtfListeners(ConfFinishedListener.class)).forEach(it -> it.onConfFinished(RtcResultCode.trans(ntf, ((BaseTypeInt) ntfContent).basetype)));
                stopSession();
                break;
            case ConfCanceled:
                Stream.of(getNtfListeners(ConfProcessCanceled.class)).forEach(it -> it.onConfProcessCanceled(RtcResultCode.trans(ntf, ((BaseTypeInt) ntfContent).basetype)));
                stopSession();
                break;

            case CurrentConfereeList: // NOTE: 入会后会收到一次该通知，创会者也会收到这条消息，列表中包含了自己。对于带密码的会议，下面会推两条这样的消息上来且内容重复。
                List<Conferee> presentConferees =
                        Stream.of(((TMTEntityInfoList) ntfContent).atMtEntitiy)
                        .distinctBy(it -> it.dwMcuId+"-"+it.dwTerId)
                        .map(this::tMTEntityInfo2Conferee)
                        .filter(it-> findConferee(it.mcuId, it.terId, it.type)==null)
                        .collect(Collectors.toList());

                boolean selfFilled = myself.mcuId != 0 && myself.terId != 0;
                Conferee self = Stream.of(presentConferees).filter(Conferee::isMyself).findFirst().orElse(null);
                if (self != null) {
                    presentConferees.remove(self);
                    if (!selfFilled){
                        myself.fill(self.mcuId, self.terId, self.alias, self.email);
                    }
                }

                conferees.addAll(presentConferees);

                Conferee assStreamConferee = tryCreateAssStreamConferee();
                if (null != assStreamConferee){
                    conferees.add(assStreamConferee);
                    presentConferees.add(assStreamConferee);
                }

                if (!selfFilled) {
                    presentConferees.add(myself); // 己端放最后
                }
                Set<ConfereesChangedListener> listeners = getNtfListeners(ConfereesChangedListener.class);
                Stream.of(presentConferees).forEach(conferee -> Stream.of(listeners).forEach(it -> it.onConfereeJoined(conferee)));
                break;

            case ConfereeJoined:
                Conferee joined = tMTEntityInfo2Conferee((TMTEntityInfo) ntfContent);
                if (findConferee(joined.mcuId, joined.terId, joined.type)==null){
                    conferees.add(joined);
                    Stream.of(getNtfListeners(ConfereesChangedListener.class)).forEach(it -> it.onConfereeJoined(joined));
                    assStreamConferee = tryCreateAssStreamConferee();
                    if (null != assStreamConferee){
                        conferees.add(assStreamConferee);
                        Stream.of(getNtfListeners(ConfereesChangedListener.class)).forEach(it -> it.onConfereeJoined(assStreamConferee));
                    }
                }
                break;

            case ConfereeLeft:
                TMtId tMtId = (TMtId) ntfContent;
                Conferee leftConferee = findConferee(tMtId.dwMcuId, tMtId.dwTerId, Conferee.ConfereeType.Normal);
                if (null != leftConferee){
                    conferees.remove(leftConferee);
                    Stream.of(getNtfListeners(ConfereesChangedListener.class)).forEach(it -> it.onConfereeLeft(leftConferee));
                }
                break;

            case CurrentStreamList: // NOTE: 创会者不会收到这条消息，并且CurrentStreamList和CurrentConfereeList的先后顺序不定
            case StreamJoined: // NOTE: 己端不会收到自己的流joined的消息
                List<KdStream> joinedStreams =
                        Stream.of(((TRtcStreamInfoList) ntfContent).atStramInfoList)
                        .distinctBy(it-> it.achStreamId)
                        .map(KdStream::new)
                        .filter(it-> findStream(it.getStreamId())==null)
                        .collect(Collectors.toList());

                if (!joinedStreams.isEmpty()) {
                    boolean hasAssStreamJoined = Stream.of(joinedStreams).anyMatch(KdStream::isAss);
                    if (hasAssStreamJoined) { // 有辅流加入
                        // 检查是否已存在辅流，若存在则先踢掉之前的辅流。（按说我们应该依赖下层消息驱动我们做这件事，
                        // 我们期望下层先推一个StreamLeft消息，表示前面的辅流被抢了，而后再推StreamJoined，表示新的辅流加入，但实际的时序刚好相反。
                        // 所以，我们自己调整时序——当抢发辅流的StreamJoined到达时我们主动踢掉前面的辅流，然后前面辅流的StreamLeft抵达时我们忽略。）
                        KdStream preAssStream = findAssStream();
                        if (null != preAssStream) {
                            streams.remove(preAssStream);
                            PeerConnectionWrapper pcWrapper = getPcWrapper(ConnType.ASS_SUBSCRIBER);
                            if (null != pcWrapper) {
                                pcWrapper.removeRemoteVideoTrack(preAssStream);
                            }
                            Conferee preAssStreamConferee = preAssStream.getOwner();
                            if (null != preAssStreamConferee) {
                                conferees.remove(preAssStreamConferee);
                                Stream.of(getNtfListeners(ConfereesChangedListener.class)).forEach(it -> it.onConfereeLeft(preAssStreamConferee));
                            }
                        }
                    }

                    streams.addAll(joinedStreams);

                    if (hasAssStreamJoined){
                        assStreamConferee = tryCreateAssStreamConferee();
                        if (null != assStreamConferee){
                            conferees.add(assStreamConferee);
                            Stream.of(getNtfListeners(ConfereesChangedListener.class)).forEach(it -> it.onConfereeJoined(assStreamConferee));
                        }
                    }

                    subscribeStream();
                }
                break;

            case StreamLeft: // NOTE 己端不会收到自己的流left的消息。码流退出不需要自己setplayitem重新订阅，业务组件已经重新订阅。
                Set<KdStream> leftStreams = Stream.of(streams)
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
                            Stream.of(getNtfListeners(ConfereesChangedListener.class)).forEach(it -> it.onConfereeLeft(assConferee));
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

            case AudioStreamOwnerChanged:
                Stream.of(streams).forEach(it -> {
                    if (it.streamInfo.bAudio && it.streamInfo.bMix){
                        it.setTerId(0); // 清空之前的映射关系，准备建立新的映射关系。0为无效terId。
                    }
                });

                Stream.of(((TRtcStreamInfoList) ntfContent).atStramInfoList)
                        .forEach(it -> Stream.of(streams).forEach(kdStream -> {
                            if (kdStream.getStreamId().equals(it.achStreamId)){
                                // 混音流映射到新的终端
                                kdStream.setMcuId(it.tMtId.dwMcuId);
                                kdStream.setTerId(it.tMtId.dwTerId);
                            }
                        }));

                break;

            case SelfSilenceStateChanged:
                BaseTypeBool cont = (BaseTypeBool) ntfContent;
                if (cont.basetype != config.isSilenced && doSetSilence(cont.basetype)) {
//                    if (sessionEventListener != null) sessionEventListener.onSelfSilenceStateChanged(config.isSilenced);
                }
                break;
            case SelfMuteStateChanged:
                cont = (BaseTypeBool) ntfContent;
                if (cont.basetype != myself.isMuted()) {
                    doSetMute(cont.basetype);
                    Stream.of(getNtfListeners(ConfereeStateChangedListener.class)).forEach(it -> it.onMuteStateChanged(myself));
                }
                break;
            case OtherConfereeStateChanged:
                TMtEntityStatus state = (TMtEntityStatus) ntfContent;
                final Conferee[] confereeWrapper = new Conferee[1];
                int maxTimesToTry = 3, interval = 1000;
                ConditionalConsumer.tryConsume(state, value -> {
                    confereeWrapper[0] = findConferee(value.dwMcuId, value.dwTerId, Conferee.ConfereeType.Normal);
                    return confereeWrapper[0] != null;
                }, value -> {
                    Conferee conferee = confereeWrapper[0];
                    if (!conferee.isMyself() && conferee.isMuted() != value.tStatus.bIsMute){
                        conferee.setMuted(value.tStatus.bIsMute);
                        KLog.p("onMuteStateChanged(conferee=%s)", conferee);
                        Stream.of(getNtfListeners(ConfereeStateChangedListener.class)).forEach(it -> it.onMuteStateChanged(conferee));
                    }
                },
                maxTimesToTry,
                interval,
                value -> KLog.p(KLog.ERROR, "conferee(mcu=%s, ter=%s) has still not joined yet after trying %s times in %s milliseconds.",
                        value.dwMcuId, value.dwTerId, maxTimesToTry, interval*maxTimesToTry));

                break;

            case ConfAboutToEnd:
                BaseTypeInt baseTypeInt = (BaseTypeInt) ntfContent;
                Stream.of(getNtfListeners(ConfAboutToEndListener.class)).forEach(it -> it.onConfAboutToEnd(baseTypeInt.basetype));
                break;

            case ConfProlonged:
                baseTypeInt = (BaseTypeInt) ntfContent;
                Stream.of(getNtfListeners(ConfProlongedListener.class)).forEach(it -> it.onConfProlonged(baseTypeInt.basetype));
                break;

//            case PresenterChanged:
//            case KeynoteSpeakerChanged:
            case BriefConfInfoArrived:
                final Conferee[] predecessorWrapper = new Conferee[1];
                final Conferee[] successorWrapper = new Conferee[1];
                TMtSimpConfInfo briefConfInfo = (TMtSimpConfInfo) ntfContent;

                // 处理主持人变更
                // 主持人变动通知可能在与会方入会/与会方列表通知之前抵达，而我们需要与会方信息来处理该通知，
                // 因此我们使用“条件消费者”处理该消息。
                maxTimesToTry = 3; interval = 1000;
                ConditionalConsumer.tryConsume(
                    briefConfInfo,

                    value -> {
                        TMtId mtId = value.tChairman;
                        if (mtId.isValid()) { // 此为指定主持人的场景
                            // 判断主持人是否已经入会
                            if (myself.getTerId() == mtId.dwTerId && myself.getMcuId() == mtId.dwMcuId) {
                                successorWrapper[0] = myself;
                            } else {
                                successorWrapper[0] = Stream.of(conferees)
                                        .filter(it -> it.getTerId() == mtId.dwTerId && it.getMcuId() == mtId.dwMcuId)
                                        .findFirst()
                                        .orElse(null);
                            }
                            return successorWrapper[0] != null;
                        }else { // 此为取消主持人的场景
                            successorWrapper[0] = null;
                            return true;
                        }
                    },

                    value -> {
                        predecessorWrapper[0] = findPresenter();
                        Conferee predecessor = predecessorWrapper[0];
                        Conferee successor = successorWrapper[0];
                        if (successor == predecessor) {
                            return;
                        }
                        if (predecessor != null) {
                            predecessor.setPresenter(false);
                        }
                        if (successor != null) {
                            successor.setPresenter(true);
                        }
                        KLog.p("onPresenterChanged(predecessor=%s, successor=%s)", predecessor, successor);
                        Stream.of(getNtfListeners(PresenterChangedListener.class))
                                .forEach(it -> it.onPresenterChanged(predecessor, successor));
                    },

                    maxTimesToTry,
                    interval,

                    value -> {
                        TMtId mtId = value.tChairman;
                        KLog.p(KLog.ERROR, "presenter(mcu=%s, ter=%s) has still not joined yet after trying %s times in %s milliseconds.",
                                mtId.dwMcuId, mtId.dwTerId, maxTimesToTry, interval*maxTimesToTry);
                    }
                );

                // 处理主讲人变更
                // 主讲人变动通知可能在与会方入会/与会方列表通知之前抵达，而我们需要与会方信息来处理该通知，
                // 因此我们使用“条件消费者”处理该消息。
                ConditionalConsumer.tryConsume(
                    briefConfInfo,

                    value -> {
                        TMtId mtId = value.tSpeaker;
                        if (mtId.isValid()){ // 指定主讲人
                            if (myself.getTerId() == mtId.dwTerId && myself.getMcuId() == mtId.dwMcuId){
                                successorWrapper[0] = myself;
                            }else{
                                successorWrapper[0] = Stream.of(conferees).filter(it -> it.getTerId() == mtId.dwTerId && it.getMcuId() == mtId.dwMcuId).findFirst().orElse(null);
                            }
                            return successorWrapper[0] != null;
                        }else{ // 取消主讲人
                            successorWrapper[0] = null;
                            return true;
                        }
                    },

                    value -> {
                        predecessorWrapper[0] = findKeynoteSpeaker();
                        Conferee predecessor = predecessorWrapper[0];
                        Conferee successor = successorWrapper[0];
                        if (successor == predecessor) {
                            return;
                        }
                        if (predecessor != null) {
                            predecessor.setKeynoteSpeaker(false);
                        }
                        if (successor != null) {
                            successor.setKeynoteSpeaker(true);
                        }
                        KLog.p("onKeynoteSpeakerChanged(predecessor=%s, successor=%s)", predecessor, successor);
                        Stream.of(getNtfListeners(KeynoteSpeakerChangedListener.class))
                                .forEach(it -> it.onKeynoteSpeakerChanged(predecessor, successor));
                    },

                    maxTimesToTry,
                    interval,

                    value -> {
                        TMtId mtId = value.tChairman;
                        KLog.p(KLog.ERROR, "keynoteSpeaker(mcu=%s, ter=%s) has still not joined yet after trying %s times in %s milliseconds.",
                                mtId.dwMcuId, mtId.dwTerId, maxTimesToTry, interval*maxTimesToTry);
                    }
                );

                break;

            case VIPsChanged:
                List<TMtId> tMtIds = ((TMtIdList) ntfContent).atList;
                Set<Conferee> newVips = new HashSet<>();
                maxTimesToTry = 3;
                interval = 1000;
                ConditionalConsumer.tryConsume(
                    tMtIds,

                    value -> {
                        Set<Conferee> vips = Stream.of(value)
                                .map(it -> {
                                    Conferee vip = findConferee(it.dwMcuId, it.dwTerId, Conferee.ConfereeType.Normal);
                                    if (vip==null){
                                        KLog.p(KLog.WARN, "vip(mcu=%s, ter=%s) has not joined yet", it.dwMcuId, it.dwTerId);
                                    }
                                    return vip;
                                })
                                .filter(it -> it != null)
                                .collect(Collectors.toSet());
                        newVips.clear();
                        newVips.addAll(vips);
                        return value.size() == vips.size();
                    },

                    tMtIds1 -> {
                        Set<Conferee> oldVips = findVIPs();
                        Set<Conferee> added = new HashSet<>(newVips);
                        added.removeAll(oldVips);
                        Set<Conferee> removed = new HashSet<>(oldVips);
                        removed.removeAll(newVips);
                        KLog.p("onVipChanged(added=%s, removed=%s)", added, removed);
                        Stream.of(getNtfListeners(VIPChangedListener.class))
                                .forEach(it -> it.onVipChanged(added, removed));
                    },

                    maxTimesToTry,
                    interval,

                    tMtIds12 -> KLog.p(KLog.ERROR, "some vip has still not joined yet after trying %s times in %s milliseconds.",
                    maxTimesToTry, interval*maxTimesToTry)
                );

                break;

//                case 全场哑音：
//            setMute();
//                    break;

            case ConfManSMSArrived:
                ConfManSMS sms = ToDoConverter.TShortMsg2ConfManSMS((TShortMsg) ntfContent);
                Stream.of(getNtfListeners(ConfManSMSListener.class)).forEach(it -> it.onConfManSMS(sms));
                break;

            case ConfPasswordNeeded:
                Stream.of(getNtfListeners(ConfPasswordNeededListener.class)).forEach(ConfPasswordNeededListener::onConfPasswordNeeded);
                break;

            case SelectedToWatch:
                TSelectedToWatch selectedToWatch = (TSelectedToWatch) ntfContent;
                TMtId mtId = selectedToWatch.AssParam.tTer;
                if (!mtId.isValid()){
                    KLog.p(KLog.ERROR, "invalid selected-to-watch conferee(mcu=%s, ter=%s)", mtId.dwMcuId, mtId.dwTerId);
                    return;
                }

                maxTimesToTry = 3;
                interval = 1000;
                final Conferee[] selectedStateChangedConfereeWrapper = new Conferee[1];
                final Conferee[] oldSelectedConfereeWrapper = new Conferee[1];
                ConditionalConsumer.tryConsume(
                    selectedToWatch,

                    value -> {
                        TMtId mtId1 = value.AssParam.tTer;
                        selectedStateChangedConfereeWrapper[0] = findConferee(mtId1.dwMcuId, mtId1.dwTerId, Conferee.ConfereeType.Normal);
                        return selectedStateChangedConfereeWrapper[0] != null;
                    },

                    value -> {
                        boolean isSelected = value.MainParam.basetype // 是否开启选看
                                && value.AssParam.bForce // 是否强制。对于rtc，仅在开启选看且强制的情况下是开启，其余情形均为关闭
                                ;
                        oldSelectedConfereeWrapper[0] = findSelectedToWatchConferee();
                        Conferee oldSelectedConferee = oldSelectedConfereeWrapper[0];
                        Conferee selectedStateChangedConferee = selectedStateChangedConfereeWrapper[0];
                        if (selectedStateChangedConferee.isSelectedToWatch() != isSelected) {
                            if (oldSelectedConferee != null && selectedStateChangedConferee != oldSelectedConferee && isSelected){
                                oldSelectedConferee.setSelectedToWatch(false);
                                KLog.p("onUnselected(%s)", oldSelectedConferee);
                                Stream.of(getNtfListeners(SelectedToWatchListener.class)).forEach(it -> it.onUnselected(oldSelectedConferee));
                            }
                            selectedStateChangedConferee.setSelectedToWatch(isSelected);
                            if (isSelected) {
                                KLog.p("onSelected(%s)", selectedStateChangedConferee);
                                Stream.of(getNtfListeners(SelectedToWatchListener.class)).forEach(it -> it.onSelected(selectedStateChangedConferee));
                            } else {
                                KLog.p("onUnselected(%s)", selectedStateChangedConferee);
                                Stream.of(getNtfListeners(SelectedToWatchListener.class)).forEach(it -> it.onUnselected(selectedStateChangedConferee));
                            }
                        }
                    },

                    maxTimesToTry,
                    interval,

                    value -> {
                        TMtId mtId12 = value.AssParam.tTer;
                        KLog.p(KLog.ERROR, "selected-to-watch conferee(mcu=%s, ter=%s) has still not joined yet after trying %s times in %s milliseconds.",
                                mtId12.dwMcuId, mtId12.dwTerId, maxTimesToTry, interval*maxTimesToTry);
                    }
                );

                break;

            case ScenesComposited:
                TMtCustomVmpParam vmpParam = (TMtCustomVmpParam) ntfContent;
                if (vmpParam.emVmpMode != EmMtVmpMode.emMt_VMP_MODE_CTRL){ // 对于rtc仅需处理自定义模式的画面合成
                    KLog.p(KLog.WARN, "ignore vmp mode %s", vmpParam.emVmpMode);
                    return;
                }
                if (vmpParam.atVmpItem.isEmpty()){
                    KLog.p(KLog.WARN, "vmp list empty");
                    return;
                }

                maxTimesToTry = 3;
                interval = 1000;
                List<Conferee> vmpConferees = new ArrayList<>();
                ConditionalConsumer.tryConsume(
                    vmpParam,

                    value -> {
                        List<Conferee> conferees = Stream.of(value.atVmpItem)
                                .filter(it -> { // 过滤掉无效的画面合成方。画面合成方数量不足合成风格的画面数时，平台会用无效的与会方填充列表。
                                    boolean valid = it.tMtid.isValid();
                                    if (!valid){
                                        KLog.p(KLog.WARN, "invalid ScenesComposited conferee(mcu=%s, ter=%s)", it.tMtid.dwMcuId, it.tMtid.dwTerId);
                                    }
                                    return valid;
                                })
                                .map(it -> {
                                    Conferee c = findConferee(it.tMtid.dwMcuId, it.tMtid.dwTerId, Conferee.ConfereeType.Normal);
                                    if (c != null) {
                                        c.setInCompositedScene(value.bForce);
                                        c.setOrderInCompositedScene(it.dwVmpItem_Idx);
                                    }
                                    return c;
                                })
                                .sorted((o1, o2) -> {
                                    int order1 = o1 != null ? o1.getOrderInCompositedScene() : 9999;
                                    int order2 = o2 != null ? o2.getOrderInCompositedScene() : 9999;
                                    return order1 - order2;
                                })
                                .collect(Collectors.toList());
                        vmpConferees.clear();
                        vmpConferees.addAll(conferees);
                        return !conferees.isEmpty() && !conferees.contains(null);
                    },

                    value -> {
                        if (value.bForce == forceScenesComposited){
                            // 开启普通画面合成和关闭强制画面合成是同一条消息且内容一致，我们需借助本地保存的状态区分这两种场景。
                            KLog.p(KLog.WARN, "ScenesComposited state not changed, open=%s", value.bForce);
                            return;
                        }
                        forceScenesComposited = value.bForce;

                        if (value.bForce){
                            KLog.p("onComposite(vmpConferees=%s)", vmpConferees);
                            Stream.of(getNtfListeners(ScenesCompositedListener.class)).forEach(it-> it.onComposite(vmpConferees));
                        }else {
                            KLog.p("onCancelComposite(vmpConferees=%s)", vmpConferees);
                            Stream.of(getNtfListeners(ScenesCompositedListener.class)).forEach(it-> it.onCancelComposite(vmpConferees));
                        }
                    },

                    maxTimesToTry,
                    interval,

                    value -> KLog.p(KLog.ERROR, "some ScenesComposited conferee has still not joined yet after trying %s times in %s milliseconds.",maxTimesToTry, interval*maxTimesToTry)
                );

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
    private synchronized boolean startSession(){
        if (bSessionStarted){
            KLog.p(KLog.ERROR, "session has started already!");
            return false;
        }

        KLog.p("starting session...");

        bSessionStarted = true;

        rtcConnector.setSignalingEventsCallback(new RtcConnectorEventListener());

        config.copy(RtcConfig.getInstance(context).dump());
        KLog.p("init rtc config: "+config);
        createPeerConnectionFactory();
        createPeerConnectionWrapper();

        myself = new Conferee(userE164);
        myself.setMuted(config.isMuted);

        callInfo = null;

        // 定时采集统计信息
        startCollectStats();
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

        rtcConnector.setSignalingEventsCallback(null);

        handler.removeCallbacksAndMessages(null);

//        myself = null;

        conferees.clear();

        streams.clear();

        for (Display display : displays){
            display.release();
        }
        displays.clear();


        kdStreamId2RtcTrackIdMap.clear();

        screenCapturePermissionData = null;

        destroyPeerConnectionWrapper();
        destroyPeerConnectionFactory();

        // destroy audiomanager
//        if (audioManager != null) {
//            audioManager.stop();
//            audioManager = null;
//        }

        cancelReq(null, null);

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

            WebRtcAudioUtils.setWebRtcBasedAcousticEchoCanceler(!config.isBuiltInAECPreferred);
//            WebRtcAudioUtils.setWebRtcBasedAutomaticGainControl(true);
            WebRtcAudioUtils.setWebRtcBasedNoiseSuppressor(!config.isBuiltInNSPreferred);

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

            if (config.isAECDumpEnabled) {
                try {
                    File dir = new File(context.getExternalFilesDir(null), "webrtc");
                    if (!dir.exists()) {
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
        if (config.isAECDumpEnabled) {
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
                .setAudioSource(config.audioSource)
                .createAudioDeviceModule();
    }

    public boolean isHWAECSupported(){
        return JavaAudioDeviceModule.isBuiltInAcousticEchoCancelerSupported();
    }

    public boolean isHWNSSupported(){
        return JavaAudioDeviceModule.isBuiltInNoiseSuppressorSupported();
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
        private final String id;

        private int mcuId;
        private int terId;
        private String   e164;
        private String   alias;
        private String   email;

        private final ConfereeType type;

        private int role;
        /** 主持人 */
        private static final int PRESENTER_MASK = 0x1;
        /** 主讲人 */
        private static final int KEYNOTE_SPEAKER_MASK = 0x2;
        /** VIP*/
        private static final int VIP_MASK = 0x4;

        private int state;
        /** 哑音*/
        private static final int MUTED_MASK = 0x1;
        /** 语音激励*/
        private static final int VOICE_ACTIVATED_MASK = 0x2;
        /** 被选看*/
        private static final int SELECTED_TO_WATCH_MASK = 0x4;
        /** 身处画面合成*/
        private static final int IN_COMPOSITED_SCENE_MASK = 0x8;

        /** 音量[0, 100]*/
        private int volume;
        /** 在画面合成中的次序。次序越靠前的优先级越高。0的优先级最高。
         界面可依据该优先级决定各与会方画面展示位置。
         仅当{@code isInCompositedScene()=true}时有用
         */
        private int orderInCompositedScene;
        /** 上一次Display刷新的时间戳*/
        private long lastRefreshTimestamp;

        /** 音频通道状态*/
        private AudioChannelState audioChannelState = AudioChannelState.Idle;
        /** 音频信号状态*/
        private AudioSignalState audioSignalState = AudioSignalState.Idle;

        /** 视频通道状态*/
        private VideoChannelState videoChannelState = VideoChannelState.Idle;
        /** 视频信号状态*/
        private VideoSignalState videoSignalState = VideoSignalState.Idle;

        /** 与会方画面显示器。
         一个与会方可以绑定多个Display。一个Display只能绑定到一个与会方 */
        private final Set<Display> displays = Collections.newSetFromMap(new ConcurrentHashMap<>());

        /** 文字图片装饰。如台标，静态图片等。
         与会方的画面内容由码流和装饰组成。 */
        private final Set<TextDecoration> textDecorations = new HashSet<>();
        private final Set<PicDecoration> picDecorations = new HashSet<>();

        // 麦克风装饰 // TODO 统一由用户设置 addWidgetDeco(WidgetDeco, onclickListener)
        private MicrophoneDecoration microphoneDeco = new MicrophoneDecoration();

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
        private StreamStateDecoration cameraDisabledDeco;
        /**
         * 视频信号弱状态下的装饰
         * */
        private StreamStateDecoration weakVideoSignalDeco;
        /**
         * 纯音频与会方的装饰
         * */
        private StreamStateDecoration audioConfereeDeco;
        /**
         * 正在接收辅流的装饰
         * */
        private StreamStateDecoration recvingAssStreamDeco;

        /**
         * 正在发送辅流的装饰
         * */
        private Bitmap sendingAssStreamDeco;

        // 统计信息paint
        private static final Paint statsPaint = new Paint();


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

        private TextDecoration getLabel(){
            return Stream.of(textDecorations).filter(value -> value.isLabel).findFirst().orElse(null);
        }

        public boolean isPresenter() {
            return (role & PRESENTER_MASK) != 0;
        }

        private void setPresenter(boolean presenter) {
            if (isPresenter() != presenter){
                role ^= PRESENTER_MASK;
            }
        }

        public boolean isKeynoteSpeaker() {
            return (role & KEYNOTE_SPEAKER_MASK) != 0;
        }

        private void setKeynoteSpeaker(boolean keynoteSpeaker) {
            if (isKeynoteSpeaker() != keynoteSpeaker){
                role ^= KEYNOTE_SPEAKER_MASK;
            }
        }

        public boolean isVIP() {
            return (role & VIP_MASK) != 0;
        }

        private void setVIP(boolean VIP) {
            if (isVIP() != VIP){
                role ^= VIP_MASK;
            }
        }

        public boolean isMuted() {
            return (state & MUTED_MASK) != 0;
        }

        private void setMuted(boolean muted) {
            if (isMyself()){
                instance.config.isMuted = muted;
            }
            if (isMuted() != muted){
                state ^= MUTED_MASK;
                refreshDisplays();
            }
        }

        /**
         * 是否处于语音激励状态
         * */
        public boolean isVoiceActivated(){
            return (state & VOICE_ACTIVATED_MASK) != 0;
        }

        private void setVoiceActivated(boolean voiceActivated){
            if (isVoiceActivated() != voiceActivated){
                state ^= VOICE_ACTIVATED_MASK;
                refreshDisplays();
            }
        }

        public int getVolume() {
            return volume;
        }

        private void setVolume(int volume) {
            if (Math.abs(volume - this.volume) > 5) {
                this.volume = volume;
                refreshDisplays();
            }
        }

        public boolean isSelectedToWatch() {
            return (state & SELECTED_TO_WATCH_MASK) != 0;
        }

        private void setSelectedToWatch(boolean selectedToWatch) {
            if (isSelectedToWatch() != selectedToWatch){
                state ^= SELECTED_TO_WATCH_MASK;
            }
        }

        public boolean isInCompositedScene() {
            return (state & IN_COMPOSITED_SCENE_MASK) != 0;
        }

        private void setInCompositedScene(boolean inCompositedScene) {
            if (isInCompositedScene() != inCompositedScene){
                state ^= IN_COMPOSITED_SCENE_MASK;
            }
        }

        public int getOrderInCompositedScene() {
            return orderInCompositedScene;
        }

        private void setOrderInCompositedScene(int orderInCompositedScene) {
            this.orderInCompositedScene = orderInCompositedScene;
        }

        /**
         * 获取与会方对应的所有显示器（包括被禁用的）
         * */
        public Set<Display> getDisplays() {
            return new HashSet<>(displays);
        }

        /**
         * 获取与会方对应的所有显示器（不包括被禁用的）
         * */
        private Set<Display> getWorkingDisplays(){
            return Stream.of(displays).filter(value -> value.enabled).collect(Collectors.toSet());
        }

        /**
         * 是否为己端
         * */
        public boolean isMyself(){
            // NOTE：目前的实现必须通过e164判断
            return e164.equals(instance.myself.e164);
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
                return instance.findAssStreamSenderExceptMyself() == this;
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
        public void setCameraDisabledDecoration(@NonNull Bitmap bitmap, int winW, int winH, int backgroundColor){
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
        public void setAudioConfereeDecoration(@NonNull Bitmap bitmap, int winW, int winH, int backgroundColor){
            audioConfereeDeco = StreamStateDecoration.createFromPicDecoration(
                    PicDecoration.createCenterPicDeco("audioConfereeDeco", bitmap, winW, winH),
                    backgroundColor
            );
        }

        /**
         * 设置视频信号弱的deco
         * @param winW UCD标注的deco所在窗口的宽
         * @param winH UCD标注的deco所在窗口的高
         * @param backgroundColor 背景色
         * */
        public void setWeakVideoSignalDecoration(@NonNull Bitmap bitmap, int winW, int winH, int backgroundColor){
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
        public void setRecvingAssStreamDecoration(@NonNull Bitmap bitmap, int winW, int winH, int backgroundColor){
            recvingAssStreamDeco = StreamStateDecoration.createFromPicDecoration(
                    PicDecoration.createCenterPicDeco("recvingAssStreamDeco", bitmap, winW, winH),
                    backgroundColor
            );
        }


        /**
         * 设置正在发送辅流时展示的deco
         * */
        public void setSendingAssStreamDecoration(@NonNull Bitmap bitmap){
            sendingAssStreamDeco = bitmap;
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
            setVideoSignalState(videoSignalState, true);
        }

        private void setVideoSignalState(VideoSignalState videoSignalState, boolean refreshDisplays) {
            if (videoSignalState != this.videoSignalState) {
                KLog.sp(String.format("%s change VIDEO SIGNAL state from %s to %s", getId(), this.videoSignalState, videoSignalState));
                this.videoSignalState = videoSignalState;
                if (refreshDisplays) {
                    refreshDisplays();
                }
            }
        }

        private VideoSignalState getVideoSignalState() {
            return videoSignalState;
        }

        private KdStream getVideoStream(){
            return instance.findStream(mcuId, terId, false, type==ConfereeType.AssStream);
        }

        private Set<KdStream> getAudioStreams(){
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
        private KdStream videoStream;
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
            KdStream curVS = getVideoStream();
            Display.Priority curPri = getPriority();
            int curPVQ = getPreferredVideoQuality();

            boolean needInvalidate = curVS != videoStream
                    || curPri != priority
                    || curPVQ != preferredVideoQuality && curVS.getResolution(preferredVideoQuality) != curVS.getResolution(curPVQ);

            if (needInvalidate){
                instance.subscribeStream();
            }
            return needInvalidate;
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
            instance.handler.removeCallbacks(refreshDisplaysRunnable);
            // 延迟刷新以防止短时间内大量重复刷新，亦可改善关开摄像头时帧残留的问题。
            if (System.currentTimeMillis()-lastRefreshTimestamp > 300) {
                doRefreshDisplays();
            }else {
                instance.handler.postDelayed(refreshDisplaysRunnable, 150);
            }
        }

        private Runnable refreshDisplaysRunnable = new Runnable() {
            @Override
            public void run() {
                doRefreshDisplays();
            }
        };

        private void doRefreshDisplays(){
            lastRefreshTimestamp = System.currentTimeMillis();
            for (Display display : displays){
                display.refresh();
            }
        }


        private void drawStatistics(Canvas canvas){
            Statistics.ConfereeRelated stats = instance.getStats(id);
            if (stats == null){
                return;
            }
            canvas.drawColor(0x80000000);
            Statistics.AudioInfo ai = stats.audioInfo;
            Statistics.VideoInfo vi = stats.videoInfo;
            statsPaint.reset();
            statsPaint.setColor(Color.GREEN);
            statsPaint.setTextSize(22);
            statsPaint.setAntiAlias(true);
            Paint.FontMetrics fm = statsPaint.getFontMetrics();
            float fontHeight = fm.descent - fm.ascent;
            int xPos = 5, yPos = Math.round(fontHeight);
            if (vi != null) {
                canvas.drawText("== video ==", xPos, yPos, statsPaint);
                yPos += fontHeight;
                canvas.drawText("width: "+vi.width, xPos, yPos, statsPaint);
                yPos += fontHeight;
                canvas.drawText("height: "+vi.height, xPos, yPos, statsPaint);
                yPos += fontHeight;
                if (vi.framerate < 5) {
                    statsPaint.setColor(Color.RED);}
                canvas.drawText("frame rate: " + vi.framerate, xPos, yPos, statsPaint);
                statsPaint.setColor(Color.GREEN);
                yPos += fontHeight;
                canvas.drawText("bitrate: "+vi.bitrate+"kbps", xPos, yPos, statsPaint);
                yPos += fontHeight;
                canvas.drawText("format: "+vi.encodeFormat, xPos, yPos, statsPaint);
                yPos += fontHeight;
                if (!isMyself()) {
                    canvas.drawText("packets received: " + vi.packetsReceived, xPos, yPos, statsPaint);
                    yPos += fontHeight;
                    canvas.drawText("packets lost: " + vi.packetsLost, xPos, yPos, statsPaint);
                    yPos += fontHeight;
                    if (vi.realtimeLostRate > 20) {
                        statsPaint.setColor(Color.RED);
                    }
                    canvas.drawText("realtime lost rate: " + vi.realtimeLostRate + "%", xPos, yPos, statsPaint);
                    statsPaint.setColor(Color.GREEN);
                    yPos += fontHeight;
//                canvas.drawText("encoder: "+vi.encoder, xPos, yPos, paint);
//                yPos += fontHeight;
                }
            }

            xPos = canvas.getWidth()/2;
            yPos = Math.round(fontHeight);

            Statistics statistics = instance.latestStats();
            if (ai != null || (statistics!=null && statistics.common != null)) {
                String title;
                if (ai != null){
                    title = "== audio ==";
                }else{
                    ai = statistics.common.mixedAudio;
                    title = "== mixed audio ==";
                }
                canvas.drawText(title, xPos, yPos, statsPaint);
                yPos += fontHeight;
                canvas.drawText("bitrate: "+ai.bitrate+"kbps", xPos, yPos, statsPaint);
                yPos += fontHeight;
                canvas.drawText("format: "+ai.encodeFormat, xPos, yPos, statsPaint);
                yPos += fontHeight;
                canvas.drawText("volume: "+ai.audioLevel, xPos, yPos, statsPaint);
                yPos += fontHeight;
                if (!isMyself()) {
                    canvas.drawText("packets received: " + ai.packetsReceived, xPos, yPos, statsPaint);
                    yPos += fontHeight;
                    canvas.drawText("packets lost: " + ai.packetsLost, xPos, yPos, statsPaint);
                    yPos += fontHeight;
                    if (ai.realtimeLostRate > 20) {
                        statsPaint.setColor(Color.RED);
                    }
                    canvas.drawText("realtime lost rate: " + ai.realtimeLostRate + "%", xPos, yPos, statsPaint);
                    statsPaint.setColor(Color.GREEN);
                    yPos += fontHeight;
                }
            }

        }


        // 与会方类型
        public enum ConfereeType{
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
    private class KdStream {
        private TRtcStreamInfo streamInfo;

        private KdStream(@NonNull TRtcStreamInfo streamInfo){
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

        private void setMcuId(int mcuId) {
            streamInfo.tMtId.dwMcuId = mcuId;
        }

        private void setTerId(int terId) {
            streamInfo.tMtId.dwTerId = terId;
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
            id = hashCode()+"";
            this.type = type;
            priority = type2Priority(type);
            preferredVideoQuality = type2VideoQuality(type);
            adjust();
            setEnableHardwareScaler(true);
            setWillNotDraw(false);
        }

        private void adjust(){
            if (type != Type.THUMBNAIL && conferee != null && conferee.isVirtualAssStreamConferee()) { // 大画面且为辅流画面
                // 辅流一般是共享文档或桌面的场景，内容完整性是首要的，共享方与接收方需要保证看到的内容一致。
                // 当发送的辅流的宽高比跟接收端展示的窗口的宽高比不一致时，接收端留黑边以保证画面不被拉伸。
                setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
                ViewGroup.LayoutParams lp = getLayoutParams();
                if (lp == null){
                    lp = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                }else {
                    lp.width = ViewGroup.LayoutParams.WRAP_CONTENT;
                    lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                }
                setLayoutParams(lp); // NOTE: 设置为WRAP_CONTENT，SCALE_ASPECT_FIT才能生效
            }else{
                // 让码流充满窗口，不留黑边，可能画面被拉伸以及部分画面被裁剪。
                setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_BALANCED);
            }
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
                adjust();
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

            adjust();

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

            adjust();

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
            enabled = false;
            if (conferee != null){
                conferee.removeDisplay(this);
            }
            super.release();
        }


        private void refresh(){
            invalidate();
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
                stateDeco = conferee.cameraDisabledDeco;
            }else if (Conferee.VideoChannelState.BindingFailed == videoChannelState
                    && conferee.type != Conferee.ConfereeType.AssStream
//                    && Conferee.AudioChannelState.BindingFailed != audioChannelState
            ){
                stateDeco = conferee.audioConfereeDeco;
            }else if (Conferee.VideoSignalState.Weak == videoSignalState){
                stateDeco = conferee.weakVideoSignalDeco;
            }else if (Conferee.VideoSignalState.Buffering == videoSignalState && conferee.isVirtualAssStreamConferee()){
                stateDeco = conferee.recvingAssStreamDeco;
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

            TextDecoration label = conferee.getLabel();
            if (label != null && label.enabled()
                    && !disabledDecos.contains(label.id)) {
                if (conferee.type == Conferee.ConfereeType.AssStream){
                    // 绘制发送辅流图标
                    canvas.drawRect(label.getMicroPhoneBackgroundRect(), label.bgPaint);
                    canvas.drawBitmap(conferee.sendingAssStreamDeco, null, label.getMicroPhoneBackgroundRect(), null);
                }else {
                    // 绘制麦克风
                    conferee.microphoneDeco.draw(label.getMicroPhoneRect(), label.getMicroPhoneBackgroundRect(), label.bgPaint.getColor(),
                            conferee.isMuted(), conferee.getVolume(), canvas);
                }
            }

            // 绘制语音激励deco
//            if (conferee.isVoiceActivated()){
//                conferee.voiceActivatedDeco.set(0, 0, displayWidth, displayHeight);
//                canvas.drawRect(conferee.voiceActivatedDeco, Conferee.voiceActivatedDecoPaint);
//            }

            // 绘制统计信息
            if (instance.showStatistics){
                conferee.drawStatistics(canvas);
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
            Conferee sender = findAssStreamSenderExceptMyself();
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
                .map(stream -> new TRtcPlayItem(stream.getStreamId(), stream.isAss(), stream.getResolution(), stream.getOwner().getPriority() == Display.Priority.HIGH))
                .collect(Collectors.toList());

        if (!playItems.isEmpty()) {
            // 因为是全量，所以可能既包含未订阅的的也包含已订阅过的。
            // 对于未订阅过的会触发PCObserver#onTrack，对于已订阅过的下层没有任何消息或者回调反馈
            req(Msg.SelectStream, null, null, new TRtcPlayParam(playItems));
        }
    }


    private Conferee findConferee(int mcuId, int terId, Conferee.ConfereeType type){
        if (mcuId == myself.getMcuId() && terId == myself.getTerId() && type == myself.type){
            return myself;
        }else {
            return Stream.of(conferees)
                    .filter(it -> it.mcuId == mcuId && it.terId == terId && it.type == type)
                    .findFirst()
                    .orElse(null);
        }
    }

    // NOTE: 查找范围不包含己端，己端没有对应的streamId，业务组件没报
    private Conferee findConfereeByStreamId(String streamId){
        KdStream stream = findStream(streamId);
        if (null != stream){
            return stream.getOwner();
        }
        return null;
    }


    /**
     * 根据与会方id查找与会方
     * */
    public Conferee findConfereeById(String id){
        if (myself.getId().equals(id)){
            return myself;
        }else {
            return Stream.of(conferees)
                    .filter(it -> it.id.equals(id))
                    .findFirst()
                    .orElse(null);
        }
    }


    /**
     * 根据e164号查找与会方
     * */
    public Conferee findConfereeByE164(String e164, Conferee.ConfereeType type){
        if (myself.getE164().equals(e164) && type == myself.type){
            return myself;
        }else {
            return Stream.of(conferees)
                    .filter(it-> it.e164.equals(e164) && it.type==type)
                    .findFirst()
                    .orElse(null);
        }
    }


    /**
     * 查找（虚拟的）辅流与会方。
     * 针对辅流我们虚构了一个“辅流与会方”。
     * NOTE: 查找范围不包含己端，己端发送辅流的场景没有对应的“辅流与会方”
     * */
    public Conferee findAssConferee(){
        KdStream assStream = findAssStream();
        if (null == assStream){
            return null;
        }
        return assStream.getOwner();
    }


    /**
     * 查找辅流发送者
     * */
    public Conferee findAssStreamSender(){
        if (myself.isSendingAssStream()){
            return myself;
        }else{
            return findAssStreamSenderExceptMyself();
        }
    }


    /**
     * 查找辅流发送者（不包含己端）
     * */
    private Conferee findAssStreamSenderExceptMyself(){
        KdStream assStream = findAssStream();
        if (null == assStream) {
            return null;
        }
        return findConferee(assStream.getMcuId(), assStream.getTerId(), Conferee.ConfereeType.Normal);
    }


    /**
     * 查找主持人
     * */
    public Conferee findPresenter(){
        if (myself.isPresenter()) {
            return myself;
        } else {
            return Stream.of(conferees).filter(Conferee::isPresenter).findFirst().orElse(null);
        }
    }

    /**
     * 查找主讲人
     * */
    public Conferee findKeynoteSpeaker(){
        if (myself.isKeynoteSpeaker()) {
            return myself;
        } else {
            return Stream.of(conferees).filter(Conferee::isKeynoteSpeaker).findFirst().orElse(null);
        }
    }

    /**
     * 查找VIP
     * */
    public Set<Conferee> findVIPs(){
        Set<Conferee> vips = new HashSet<>();
        if (myself.isVIP()) {
            vips.add(myself);
        }
        vips.addAll(Stream.of(conferees).filter(Conferee::isVIP).collect(Collectors.toSet()));

        return vips;
    }

    /**
     * 查找当前处于语音激励状态的与会方
     * */
    public Conferee findVoiceActivatedConferee(){
        if (myself.isVoiceActivated()){
            return myself;
        }else{
            return Stream.of(conferees).filter(Conferee::isVoiceActivated).findFirst().orElse(null);
        }
    }

    /**
     * 查找当前处于选看状态的与会方
     * */
    public Conferee findSelectedToWatchConferee(){
        if (myself.isSelectedToWatch()) {
            return myself;
        } else {
            return Stream.of(conferees).filter(Conferee::isSelectedToWatch).findFirst().orElse(null);
        }
    }


    private KdStream findStream(String streamId){
        return Stream.of(streams).filter(it-> it.getStreamId().equals(streamId)).findFirst().orElse(null);
    }


    private KdStream findStream(int mcuId, int terId, boolean isAudio, boolean isAss){
        return Stream.of(streams)
                .filter(it-> it.getMcuId()==mcuId && it.getTerId()==terId && it.isAudio()==isAudio && it.isAss()==isAss)
                .findFirst().orElse(null);
    }


    private KdStream findAssStream(){
        return Stream.of(streams).filter(KdStream::isAss).findFirst().orElse(null);
    }


    /**
     * 创建Display。
     * NOTE: Display不再使用时记得销毁{@link #releaseDisplay(Display)}
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
            KLog.p(KLog.ERROR, "display %s is not alive", display.id());
        }
    }


    public static class TextDecoration extends Decoration{
        public String text;     // 要展示的文字内容
        private int textSize;   // 文字大小（UCD标注的，实际展示的大小会依据Display的大小调整）
        private Paint bgPaint = new Paint();
        private RectF bgRect = new RectF();  // 文字背景区域
        private static final int minTextSizeLimit = 34;
        private Rect textBounds = new Rect();
        private boolean isLabel; // 是否为台标
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

        public boolean isLabel() {
            return isLabel;
        }

        public void setLabel(boolean label) {
            isLabel = label;
        }

        private RectF microPhoneRect = new RectF();
        private RectF microPhoneBackgroundRect = new RectF();
        RectF getMicroPhoneRect(){
            float left, top, right, bottom;
            top = actualY+textBounds.top
                    +(fm.ascent-textBounds.top)/2; // 麦克风高出文字一点以免太小看不清能量条变化。
            bottom = actualY+textBounds.bottom;//actualY;
            float phoneW = (bottom-top)*3/5;
            left = (bgRect.left-phoneW)/2;
            right = bgRect.left - left;
            microPhoneRect.set(left, top, right, bottom);
            return microPhoneRect;
        }

        RectF getMicroPhoneBackgroundRect(){
            float left, top, right, bottom;
            top = bgRect.top;
            bottom = bgRect.bottom;
            right = bgRect.left;
            left = 0;
            microPhoneBackgroundRect.set(left, top, right, bottom);
            return microPhoneBackgroundRect;
        }

        private Paint.FontMetrics fm = new Paint.FontMetrics();
        protected boolean adjust(int width, int height){
            if (!super.adjust(width, height)){
                return false;
            }

            // 根据实际窗体宽高计算适配后的字体大小
            float size = Math.max(minTextSizeLimit, textSize * MatrixHelper.getMinScale(matrix));
            paint.setTextSize(size);
            int xPadding = 12;
            int yPadding = 12;
            // 修正实际锚点坐标。
            // 为防止字体缩的过小我们限定了字体大小下限，我们需修正由此可能带来的偏差。
            // NOTE: 此修正目前仅针对文字横排的情形，若将来增加文字竖排的需求，需在此增加相应的处理逻辑。
            paint.getFontMetrics(fm);
            paint.getTextBounds(text, 0, text.length(), textBounds);
            if (POS_LEFTBOTTOM==refPos || POS_RIGHTBOTTOM==refPos) {
                actualY = Math.min(height-fm.bottom-yPadding, actualY);
            }else {
                actualY = Math.max(0-fm.top+yPadding, actualY);
            }

            actualX = x * size / textSize;

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


    private static class MicrophoneDecoration{
        Paint strokePaint = new Paint();
        Paint fillPaint = new Paint();
        private final int STROKE_WIDTH = 4;

        public MicrophoneDecoration() {
            strokePaint.setStyle(Paint.Style.STROKE);
            strokePaint.setStrokeWidth(STROKE_WIDTH);
            strokePaint.setColor(Color.WHITE);
            strokePaint.setAntiAlias(true);
            fillPaint.setStyle(Paint.Style.FILL);
        }

        /**
         * @param rect 麦克图标所在的矩形区域
         * @param background 图标背景区域
         * @param bgColor 背景区域填充色
         * */
        void draw(RectF rect, RectF background, int bgColor, boolean muted, int volume, Canvas canvas){
            volume = Math.max(volume, 0);
            volume = Math.min(volume, 100);

            fillPaint.setColor(bgColor);
            canvas.drawRect(background, fillPaint);
            float roundRectHorizontalMargin = (rect.right-rect.left)/4;
            float roundRectBottomMargin = (rect.bottom-rect.top)/3;
            fillPaint.setColor(Color.WHITE);
            RectF roundRect = new RectF(rect.left+roundRectHorizontalMargin, rect.top, rect.right-roundRectHorizontalMargin, rect.bottom-roundRectBottomMargin);
            canvas.drawRoundRect(roundRect, roundRect.width()/2, roundRect.width()/2, fillPaint);
            RectF halfRoundRect = new RectF(rect.left, rect.top-roundRectBottomMargin/2, rect.right, rect.bottom-roundRectBottomMargin/2);
            canvas.save();
            canvas.clipRect(halfRoundRect.left-STROKE_WIDTH, (halfRoundRect.top+halfRoundRect.bottom)/2, halfRoundRect.right+STROKE_WIDTH, halfRoundRect.bottom+STROKE_WIDTH);
            canvas.drawRoundRect(halfRoundRect, halfRoundRect.width()/2, halfRoundRect.width()/2, strokePaint);
            canvas.restore();
            canvas.drawLine(rect.left+roundRectHorizontalMargin, rect.bottom, rect.right-roundRectHorizontalMargin, rect.bottom, strokePaint);
            canvas.drawLine((rect.right+rect.left)/2, rect.bottom-roundRectBottomMargin/2, (rect.right+rect.left)/2, rect.bottom, strokePaint);
            if (muted){
                strokePaint.setColor(Color.RED);
                canvas.drawLine(rect.left, rect.top, rect.right, rect.bottom, strokePaint);
                strokePaint.setColor(Color.WHITE);
            }else{
                canvas.save();
                canvas.clipRect(roundRect.left-STROKE_WIDTH, roundRect.bottom-roundRect.height()*(volume/100f), roundRect.right+STROKE_WIDTH, roundRect.bottom+STROKE_WIDTH);
                fillPaint.setColor(0xFF1E94DA);
                canvas.drawRoundRect(roundRect, roundRect.width()/2, roundRect.width()/2, fillPaint);
                canvas.restore();
            }
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


    private class RtcConnectorEventListener implements RtcConnector.Listener {

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
        public void onSetAnswerCmd(int pcType, String answerSdp, List<RtcConnector.TRtcMedia> rtcMediaList) {
            ConnType connType = ConnType.getInstance(pcType);
            PeerConnectionWrapper pcWrapper = getPcWrapper(connType);
            if (null == pcWrapper || !pcWrapper.checkSdpState(SdpState.SENDING)) return;
            int mediaSize = rtcMediaList.size();
            if (mediaSize>0 && rtcMediaList.get(mediaSize-1).streamid==null){
                // 最后一个的streamId为null表示发布失败（业务组件如是说），发布失败要重置对应的pc。
                recreatePeerConnection(connType);
                return;
            }

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

        @Override
        public void onAgentRtcCodecStatisticReq() {
            rtcConnector.sendStatistics(latestStats());
        }

        @Override
        public void onCallConnectedNtf(RtcConnector.TCallInfo callInfo) {
            instance.callInfo = callInfo;
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
                        boolean isAssPublisher = connType==ConnType.ASS_PUBLISHER;
                        boolean bAudio = pcWrapper.isMediaType(MediaType.AUDIO);
                        RtcConnector.TRtcMedia rtcMedia = new RtcConnector.TRtcMedia(
                                SdpHelper.getMid(pc.getLocalDescription().description, bAudio),
                                isAssPublisher ? config.assVideoWidth : config.videoWidth,
                                isAssPublisher ? config.assVideoHeight : config.videoHeight,
                                bAudio ? null : createEncodingList(isAssPublisher) // 仅视频需要填encodings
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
                        boolean isAssPublisher = connType==ConnType.ASS_PUBLISHER;
                        RtcConnector.TRtcMedia rtcAudio = new RtcConnector.TRtcMedia(SdpHelper.getMid(pc.getLocalDescription().description, true));
                        RtcConnector.TRtcMedia rtcVideo = new RtcConnector.TRtcMedia(SdpHelper.getMid(pc.getLocalDescription().description, false),
                                isAssPublisher ? config.assVideoWidth : config.videoWidth,
                                isAssPublisher ? config.assVideoHeight : config.videoHeight,
                                createEncodingList(isAssPublisher));
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
            handler.post(() -> {
                PeerConnectionWrapper pcWrapper = getPcWrapper(connType);
                if (null == pcWrapper) return;
                if (newState == PeerConnection.IceConnectionState.FAILED) {
                    KLog.tp(TAG,KLog.INFO, "ICE failed, try restart ice...");
                    MediaConstraints mediaConstraints = new MediaConstraints();
                    mediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair("IceRestart", "true"));
                    pcWrapper.createOffer(mediaConstraints);
                }
            });

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


/*
*  针对webrtc sdk 30039版本的实现
* */

//    private List<RtpParameters.Encoding> createEncodingListForSendingOfferSdp(){
//        List<RtpParameters.Encoding> encodings = new ArrayList<>();
//        if (config.isSimulcastEnabled) {
//            // 从低到高，平台要求的。
//            RtpParameters.Encoding low = new RtpParameters.Encoding("l", true, 4.0);
//            low.maxFramerate = config.videoFps;
//            low.maxBitrateBps = config.videoMaxBitrate * 1024;
//            RtpParameters.Encoding medium = new RtpParameters.Encoding("m", true, 2.0);
//            medium.maxFramerate = config.videoFps;
//            medium.maxBitrateBps = config.videoMaxBitrate * 1024;
//            encodings.add(low);
//            encodings.add(medium);
//        }
//
//        RtpParameters.Encoding high = new RtpParameters.Encoding("h", true, 1.0);
//        high.maxFramerate = config.videoFps;
//        high.maxBitrateBps = config.videoMaxBitrate * 1024;
//        encodings.add(high);
//
//        return encodings;
//    }
//
//
//    private List<RtpParameters.Encoding> createEncodingList(List<RtpParameters.Encoding> encodings){
//        if (null != encodings) {
//            // NOTE：注意和sendOffer时传给业务组件的参数一致。
//            if (config.isSimulcastEnabled) {
//                for (RtpParameters.Encoding encoding : encodings) {
//                    if (encoding.rid.equals("l")) {
//                        encoding.scaleResolutionDownBy = 4.0;
//                        encoding.maxFramerate = config.videoFps;
//                        encoding.maxBitrateBps = config.videoMaxBitrate * 1024;
//                    } else if (encoding.rid.equals("m")) {
//                        encoding.scaleResolutionDownBy = 2.0;
//                        encoding.maxFramerate = config.videoFps;
//                        encoding.maxBitrateBps = config.videoMaxBitrate * 1024;
//                    } else if (encoding.rid.equals("h")) {
//                        encoding.scaleResolutionDownBy = 1.0;
//                        encoding.maxFramerate = config.videoFps;
//                        encoding.maxBitrateBps = config.videoMaxBitrate * 1024;
//                    }
//                    KLog.p("encoding: rid=%s, scaleResolutionDownBy=%s, maxFramerate=%s, maxBitrateBps=%s",
//                            encoding.rid, encoding.scaleResolutionDownBy, encoding.maxFramerate, encoding.maxBitrateBps);
//                }
//            }else {
//                RtpParameters.Encoding encoding = encodings.get(0);
//                encoding.scaleResolutionDownBy = 1.0;
//                encoding.maxFramerate = config.videoFps;
//                encoding.maxBitrateBps = config.videoMaxBitrate * 1024;
//                KLog.p("encoding.size=%s, encoding[0]: rid=%s, scaleResolutionDownBy=%s, maxFramerate=%s, maxBitrateBps=%s",
//                        encodings.size(), encoding.rid, encoding.scaleResolutionDownBy, encoding.maxFramerate, encoding.maxBitrateBps);
//            }
//
//        }else{
//            /* NOTE
//             track被添加之前scaleResolutionDownBy必须设置为null否则会崩溃，提示
//             "Fatal error: C++ addTransceiver failed"。
//             等到track被添加之后，sdp被创建之前，
//             通过sender.getParameters()获取encodings列表，然后给scaleResolutionDownBy赋予真实的值。
//             */
//            encodings = new ArrayList<>();
//            if (config.isSimulcastEnabled) {
//                encodings.add(new RtpParameters.Encoding("l", true, null));
//                encodings.add(new RtpParameters.Encoding("m", true, null));
//            }
//            encodings.add(new RtpParameters.Encoding("h", true, null));
//        }
//
//        return encodings;
//
//    }


    /*
     *  针对webrtc sdk m74版本的实现
     * */

    private RtpParameters.Encoding createEncoding(){
        Class encodingclz = null;
        try {
            encodingclz = Class.forName("org.webrtc.RtpParameters$Encoding");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        Constructor<?> ctor = null;
        try {
            ctor = encodingclz.getDeclaredConstructor(boolean.class, Integer.class, Integer.class, Integer.class, Integer.class, Double.class, Long.class);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        ctor.setAccessible(true);
        RtpParameters.Encoding encoding = null;
        try {
            encoding = (RtpParameters.Encoding) ctor.newInstance(true, 0, 0, 0, 0, 1.0, 0L);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return encoding;
    }

    private List<RtpParameters.Encoding> createEncodingList(boolean isForAssStream){
        List<RtpParameters.Encoding> encodings = new ArrayList<>();
        int confBitrate = callInfo.callBitrate;
        boolean isSelfSendingAssStream = myself.isSendingAssStream();
        int userConfigLimit = config.videoMaxBitrate;
        boolean simulcast = config.isSimulcastEnabled && !isForAssStream; // 双流不需要simulcast
        if (simulcast) {
            RtpParameters.Encoding low = createEncoding();
            low.maxFramerate = config.videoFps;
            low.maxBitrateBps = 1024 * calcEncodingBitrate(confBitrate, false, true, RID_L, isSelfSendingAssStream, userConfigLimit);
            RtpParameters.Encoding medium = createEncoding();
            medium.maxFramerate = config.videoFps;
            medium.maxBitrateBps = 1024 * calcEncodingBitrate(confBitrate, false, true, RID_M, isSelfSendingAssStream, userConfigLimit);
            RtpParameters.Encoding high = createEncoding();
            high.maxFramerate = config.videoFps;
            high.maxBitrateBps = 1024 * calcEncodingBitrate(confBitrate, false, true, RID_H, isSelfSendingAssStream, userConfigLimit);
            // 从低到高排列，平台要求的。
            encodings.add(low);
            encodings.add(medium);
            encodings.add(high);
        }else {
            RtpParameters.Encoding high = createEncoding();
            high.maxFramerate = config.videoFps;
            high.maxBitrateBps = 1024 * calcEncodingBitrate(confBitrate, isForAssStream, false, RID_H, isSelfSendingAssStream, userConfigLimit);
            encodings.add(high);
        }

        Stream.of(encodings).forEach(it -> KLog.p("encoding{frameRate=%s, bitrate=%s, scaleDownBy=%s}",
                it.maxFramerate, it.maxBitrateBps, it.scaleResolutionDownBy));

        return encodings;
    }


    private static final String RID_L = "l";
    private static final String RID_M = "m";
    private static final String RID_H = "h";

    /*
     *  针对平台给的实现了软编/硬编simulcast的库
     * */
//    private List<RtpParameters.Encoding> createEncodingList(boolean isForAssStream){
//        List<RtpParameters.Encoding> encodings = new ArrayList<>();
//        int confBitrate = callInfo.callBitrate;
//        boolean isSelfSendingAssStream = myself.isSendingAssStream();
//        int userConfigLimit = config.videoMaxBitrate;
//        boolean simulcast = config.isSimulcastEnabled && !isForAssStream; // 双流不需要simulcast
//        if (simulcast) {
//            RtpParameters.Encoding low = new RtpParameters.Encoding(RID_L, true, 4.0);
//            low.maxFramerate = config.videoFps;
//            low.maxBitrateBps = 1024 * calcEncodingBitrate(confBitrate, false, true, RID_L, isSelfSendingAssStream, userConfigLimit);
//            RtpParameters.Encoding medium = new RtpParameters.Encoding(RID_M, true, 2.0);
//            medium.maxFramerate = config.videoFps;
//            medium.maxBitrateBps = 1024 * calcEncodingBitrate(confBitrate, false, true, RID_M, isSelfSendingAssStream, userConfigLimit);
//            RtpParameters.Encoding high = new RtpParameters.Encoding(RID_H, true, 1.0);
//            high.maxFramerate = config.videoFps;
//            high.maxBitrateBps = 1024 * calcEncodingBitrate(confBitrate, false, true, RID_H, isSelfSendingAssStream, userConfigLimit);
//            // 从低到高排列，平台要求的。
//            encodings.add(low);
//            encodings.add(medium);
//            encodings.add(high);
//        }else {
//            RtpParameters.Encoding high = new RtpParameters.Encoding(RID_H, true, 1.0);
//            high.maxFramerate = config.videoFps;
//            high.maxBitrateBps = 1024 * calcEncodingBitrate(confBitrate, isForAssStream, false, RID_H, isSelfSendingAssStream, userConfigLimit);
//            encodings.add(high);
//        }
//
//        Stream.of(encodings).forEach(it -> KLog.p("encoding{rid=%s, frameRate=%s, bitrate=%s, scaleDownBy=%s}",
//                it.rid, it.maxFramerate, it.maxBitrateBps, it.scaleResolutionDownBy));
//
//        return encodings;
//    }


    private int calcEncodingBitrate(int confBitrate, boolean isAssStream, boolean isSimulcast, String rid,
                                    boolean isSelfSendingAssStream, int userConfigLimit){
        int bitrate = 0;
        if (isSimulcast) {
            // NOTE: simulcast的各路码率以会议码率为基准按照固定比例分配，不支持用户自定义！
            if (rid.equals(RID_L)){
                bitrate = (int)(confBitrate * 0.05);
            }else if (rid.equals(RID_M)){
                bitrate = (int)(confBitrate * 0.125);
            }else if (rid.equals(RID_H)){
                bitrate = (int)(confBitrate * (isSelfSendingAssStream ? 0.25 : 0.6));
            }
        }else {
            if (isAssStream){
                bitrate = (int) (confBitrate * 0.4);
            }else{
                bitrate = (int)(confBitrate * (isSelfSendingAssStream ? 0.25 : 0.6));
            }

            bitrate = Math.min(bitrate, userConfigLimit);
        }

        return bitrate;
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

        private Map<String, VideoFileRenderer> videoFileRendererMap = new HashMap<>();

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
            createOffer(new MediaConstraints());
        }

        void createOffer(MediaConstraints mediaConstraints){
            executor.execute(() -> {
                if (null == pc){
                    KLog.p(KLog.ERROR, "peerConnection destroyed");
                    return;
                }
                pc.createOffer(sdpObserver, mediaConstraints);
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
                    int frameW, frameH, frameRate;
                    if (LOCAL_WINDOW_TRACK_ID.equals(localVideoTrackId)){
                        frameW = config.assVideoWidth; frameH = config.assVideoHeight; frameRate = config.assVideoFps;
                    }else{
                        frameW = config.videoWidth; frameH = config.videoHeight; frameRate = config.videoFps;
                    }
                    KLog.p("localVideoTrackId=%s, capture videoWidth=%s, videoHeight=%s, videoFps=%s", localVideoTrackId, frameW, frameH, frameRate);
                    videoCapturer.startCapture(frameW, frameH, frameRate);
                }
                localVideoTrack = factory.createVideoTrack(localVideoTrackId, videoSource);
                localVideoTrack.setEnabled(bTrackEnable);

                boolean isAss = !localVideoTrackId.equals(LOCAL_VIDEO_TRACK_ID);
                boolean simulcast = config.isSimulcastEnabled && !isAss; // 辅流不发simulcast
                if (simulcast) {
                    RtpTransceiver.RtpTransceiverInit transceiverInit = new RtpTransceiver.RtpTransceiverInit(
                            RtpTransceiver.RtpTransceiverDirection.SEND_ONLY,
                            Collections.singletonList(STREAM_ID)
//                            createEncodingList(false)
                    );

                    RtpTransceiver transceiver = pc.addTransceiver(localVideoTrack, transceiverInit);
                    videoSender = transceiver.getSender();
//                    createEncodingList(videoSender.getParameters().encodings);
                }else {
                    videoSender = pc.addTrack(localVideoTrack);
                    int maxBitrate = 1024 * calcEncodingBitrate(callInfo.callBitrate, isAss, false, RID_H, myself.isSendingAssStream(), config.videoMaxBitrate);
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
                        if (config.saveSentMainVideo){
                            saveVideo(localVideoTrack, localVideoTrack.id());
                        }
                    }else if (localVideoTrackId.equals(LOCAL_WINDOW_TRACK_ID)){
                        if (config.saveSentAssVideo){
                            saveVideo(localVideoTrack, localVideoTrack.id());
                        }
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
                    stopSaveVideo(trackId);
                });

            });
        }


        void createAudioTrack(){
            executor.execute(() -> {
                if (null == factory){
                    KLog.p(KLog.ERROR, "factory destroyed");
                    return;
                }
                if (null == pc){
                    KLog.p(KLog.ERROR, "peerConnection destroyed");
                    return;
                }
                audioSource = factory.createAudioSource(new MediaConstraints());
                String localAudioTrackId = LOCAL_AUDIO_TRACK_ID+audioTrackCnt++;
                localAudioTrack = factory.createAudioTrack(localAudioTrackId, audioSource);
                localAudioTrack.setEnabled(!config.isMuted);
                localAudioTrack.setVolume(10 * config.inputAudioVolume/100f); // 设置音量增益，范围0-10
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
                if (null == pc){
                    KLog.p(KLog.ERROR, "peerConnection destroyed");
                    return;
                }
                String kdStreamId = mid2KdStreamIdMap.get(mid);
                if (null == kdStreamId) {
                    KLog.p(KLog.ERROR,"kdStreamId related to mid "+mid+" doesn't exist? i should have got it from onSetOfferCmd()");
                    return;
                }
                remoteVideoTracks.put(kdStreamId, track);
                track.setEnabled(config.isRemoteVideoEnabled);
                String trackId = track.id();
                KLog.p("create remote video track %s/%s", kdStreamId, trackId);

                handler.post(new Runnable() {
                    int retriedCount;
                    @Override
                    public void run() {
                        if (retriedCount >= 2){
                            return;
                        }
                        kdStreamId2RtcTrackIdMap.put(kdStreamId, trackId);
                        KdStream remoteStream = findStream(kdStreamId);
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
                            owner.setVideoSignalState(Conferee.VideoSignalState.Buffering, false);
                            handler.postDelayed(() -> owner.refreshDisplays(), 1000);
                            handler.postDelayed(() -> {
                                if (owner.getVideoSignalState() == Conferee.VideoSignalState.Buffering) {
                                    owner.setVideoSignalState(Conferee.VideoSignalState.Normal);
                                }
                            }, 3000);
                        }else {
                            owner.setVideoSignalState(Conferee.VideoSignalState.Normal);
                        }

                        if (owner.isVirtualAssStreamConferee() ? config.saveRecvedAssVideo : config.saveRecvedMainVideo) {
                            // 保存码流用于调试
                            saveVideo(track, owner.getId());
                        }

                        executor.execute(() -> {
                            if (null == pc){
                                KLog.p(KLog.ERROR, "peerConnection destroyed");
                                return;
                            }
                            KLog.p("bind track %s to conferee %s", trackId, owner.getId());
                            track.addSink(owner);
                        });
                    }
                });
            });
        }

        void removeRemoteVideoTrack(KdStream stream){
            String kdStreamId = stream.getStreamId();
            Conferee owner = stream.getOwner();
            executor.execute(() -> {
                if (null == pc){
                    KLog.p(KLog.ERROR, "peerConnection destroyed");
                    return;
                }
                for (String streamId : remoteVideoTracks.keySet()) {
                    if (streamId.equals(kdStreamId)) {
                        VideoTrack track = remoteVideoTracks.remove(streamId);
//                        track.dispose();
                        String trackId = track.id();
                        KLog.p("remote video track %s/%s removed", streamId, trackId);

                        handler.post(() -> {
                            kdStreamId2RtcTrackIdMap.remove(streamId);
                            if (null != owner) {
                                owner.setVideoChannelState(Conferee.VideoChannelState.Idle);
                                owner.setVideoSignalState(Conferee.VideoSignalState.Idle);
                                executor.execute(() -> {
                                    if (null == pc){
                                        KLog.p(KLog.ERROR, "peerConnection destroyed");
                                        return;
                                    }
                                    KLog.p("unbind track %s from conferee %s", trackId, owner.getId());
                                    track.removeSink(owner);
                                });

                                stopSaveVideo(owner.getId());
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
                if (null == pc){
                    KLog.p(KLog.ERROR, "peerConnection destroyed");
                    return;
                }
                String kdStreamId = mid2KdStreamIdMap.get(mid);
                if (null == kdStreamId) {
                    KLog.p(KLog.ERROR,"kdStreamId related to mid "+mid+" doesn't exist? i should have got it from onSetOfferCmd()");
                    return;
                }

                track.setEnabled(!config.isSilenced);
                track.setVolume(10 * config.outputAudioVolume/100f);
                remoteAudioTracks.put(kdStreamId, track);
                String trackId = track.id();

                KLog.p("remote audio track %s/%s created", kdStreamId, trackId);

                handler.post(new Runnable() {
                    int retriedCount;
                    @Override
                    public void run() {
                        if (retriedCount>=2){
                            return;
                        }
                        kdStreamId2RtcTrackIdMap.put(kdStreamId, trackId);
                        KdStream remoteStream = findStream(kdStreamId);
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

        void removeRemoteAudioTrack(KdStream stream){
            String kdStreamId = stream.getStreamId();
            Conferee owner = stream.getOwner();
            executor.execute(() -> {
                if (null == pc){
                    KLog.p(KLog.ERROR, "peerConnection destroyed");
                    return;
                }
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

        boolean isSdpProgressFinished(){
            return (sdpType==SdpType.OFFER && sdpState==SdpState.SET_REMOTE_SUCCESS)
                    || (sdpType==SdpType.ANSWER && sdpState==SdpState.SET_LOCAL_SUCCESS)
                    || (sdpType==SdpType.VIDEO_OFFER && sdpState==SdpState.SET_REMOTE_SUCCESS);
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

        private void saveVideo(VideoTrack track, String fileName){
            File dir = new File(context.getExternalFilesDir(null), "webrtc");
            if (!dir.exists()) {
                dir.mkdirs();
            }
            String savedVideo = dir.getAbsolutePath()+"/"+fileName+".video";
            try {
                VideoFileRenderer videoFileRenderer = new VideoFileRenderer(savedVideo, 640, 360, eglBase.getEglBaseContext());
                track.addSink(videoFileRenderer);
                videoFileRendererMap.put(fileName, videoFileRenderer);
            } catch (IOException e) {
                throw new RuntimeException(
                        "Failed to open video file for output: " + savedVideo, e);
            }
        }

        private void stopSaveVideo(String fileName){
            VideoFileRenderer renderer = videoFileRendererMap.remove(fileName);
            if (renderer != null){
                renderer.release();
            }
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
    private StatsHelper.Stats publisherStats;
    private StatsHelper.Stats subscriberStats;
    private StatsHelper.Stats assPublisherStats;
    private StatsHelper.Stats assSubscriberStats;

    // 上一个采集周期留存的统计信息。
    // 因为有些统计数据需要我们取区间差值自己计算，比如码率、帧率。
    private StatsHelper.Stats prePublisherStats;
    private StatsHelper.Stats preSubscriberStats;
    private StatsHelper.Stats preAssPublisherStats;
    private StatsHelper.Stats preAssSubscriberStats;

    // 统计信息采集周期。// 单位：毫秒
    private final int STATS_INTERVAL = 1000;
    // 已采集次数
    private int collectStatsCount;

    // 视频信号检测周期。单位：毫秒。
    // 注：选择一个合适的周期以提升用户体验，太短则在网络波动时会频繁切换视频信号标志，太长则标志变化显得迟钝。
    private final float videoSignalCheckPeriod = 3000f;
    private final int videoSignalCheckPerCount = (int) Math.max(videoSignalCheckPeriod /STATS_INTERVAL, 1);

    // 保存最近多久时间段的统计信息。单位：秒
    private final int keptDuration = 30;
    private final int keptSize = Math.max(keptDuration*1000/STATS_INTERVAL, 1);
    // 近期统计信息。按时间顺序从老到新排序，尾部最新。
    private final EvictingDeque<Statistics> recentStats = new EvictingDeque<>(keptSize);
    private Statistics latestStats(){
        return recentStats.peekLast();
    }

    private void startCollectStats(){
        publisherStats = null;
        subscriberStats = null;
        assPublisherStats = null;
        assSubscriberStats = null;
        prePublisherStats = null;
        preSubscriberStats = null;
        preAssPublisherStats = null;
        preAssSubscriberStats = null;
        collectStatsCount = 0;
        recentStats.clear();

        handler.postDelayed(new Runnable() {

            private int lastReportedRecvBitrate = 1;
            private final int calcRecvBitrateDuration = 30; // 单位秒。
            private final int calcRecvBitrateStartCount = calcRecvBitrateDuration*1000/STATS_INTERVAL;

            @Override
            public void run() {
                if (null != pubPcWrapper && null != pubPcWrapper.pc && pubPcWrapper.isSdpProgressFinished()) {
                    executor.execute(() -> pubPcWrapper.pc.getStats(rtcStatsReport -> handler.post(() -> {
                        prePublisherStats = publisherStats;
                        publisherStats = StatsHelper.resolveStats(rtcStatsReport);
                    })));
                }
                if (null != subPcWrapper && null != subPcWrapper.pc && subPcWrapper.isSdpProgressFinished()) {
                    executor.execute(() -> subPcWrapper.pc.getStats(rtcStatsReport -> handler.post(() -> {
                        preSubscriberStats = subscriberStats;
                        subscriberStats = StatsHelper.resolveStats(rtcStatsReport);
                    })));
                }

                if (null != assPubPcWrapper && null != assPubPcWrapper.pc && assPubPcWrapper.isSdpProgressFinished()) {
                    executor.execute(() -> assPubPcWrapper.pc.getStats(rtcStatsReport -> handler.post(() -> {
                        preAssPublisherStats = assPublisherStats;
                        assPublisherStats = StatsHelper.resolveStats(rtcStatsReport);
                    })));
                }
                if (null != assSubPcWrapper && null != assSubPcWrapper.pc && assSubPcWrapper.isSdpProgressFinished()) {
                    executor.execute(() -> assSubPcWrapper.pc.getStats(rtcStatsReport -> handler.post(() -> {
                        preAssSubscriberStats = assSubscriberStats;
                        assSubscriberStats = StatsHelper.resolveStats(rtcStatsReport);
                    })));
                }

                if (enableStatsLog){
                    KLog.p("/=== publisherStats: ");
                    printStats(publisherStats, false);
                    KLog.p("/=== subscriberStats: ");
                    printStats(subscriberStats, false);
                    KLog.p("/=== assPublisherStats: ");
                    printStats(assPublisherStats, false);
                    KLog.p("/=== assSubscriberStats: ");
                    printStats(assSubscriberStats, false);
                }

                aggregateStats();

                processStats(latestStats());

                Stream.of(getNtfListeners(StatsListener.class)).forEach(statsListener -> statsListener.onStats(latestStats()));

                if (collectStatsCount >= calcRecvBitrateStartCount) {
                    int bitrate = calcRecvBitrate(calcRecvBitrateDuration);
                    if (isMatchPeriod(3000)) {
                        KLog.p("calcRecvBitrate=%s, lastReportedRecvBitrate=%s", bitrate, lastReportedRecvBitrate);
                    }
                    boolean needReport = (bitrate == 0 && lastReportedRecvBitrate != 0) || (bitrate != 0 && lastReportedRecvBitrate == 0);
                    if (needReport) {
                        lastReportedRecvBitrate = bitrate;
                        rtcConnector.sendHasIncomingStreamOrNot(bitrate != 0);
                    }
                }

                if (showStatistics && isMatchPeriod(2000)){ // 2s刷新一次统计信息
                    Stream.of(conferees).forEach(Conferee::refreshDisplays);
                }

                ++collectStatsCount;

                handler.postDelayed(this, STATS_INTERVAL);
            }
        }, 2000);
    }


    private boolean isMatchPeriod(int millisecond){
        return collectStatsCount%(millisecond/STATS_INTERVAL) == 0 ;
    }

    private void aggregateStats(){
        Statistics statistics = new Statistics();
        aggregatePubStats(publisherStats, prePublisherStats, statistics, false);
        aggregateSubStats(subscriberStats, preSubscriberStats, statistics, false);
        aggregatePubStats(assPublisherStats, preAssPublisherStats, statistics, true);
        aggregateSubStats(assSubscriberStats, preAssSubscriberStats, statistics, true);
        recentStats.addLast(statistics);
        KLog.p("/=== aggregated Stats:\n"+statistics);
    }


    void aggregatePubStats(StatsHelper.Stats stats, StatsHelper.Stats preStats, Statistics statistics, boolean isAss){
        if (stats==null || preStats==null || statistics==null || (stats.audioOutboundRtp==null && stats.videoOutboundRtp==null)){
            return;
        }

        Statistics.AudioInfo audioInfo = null;
        int bitrate = 0;
        String codecMime = "";
        if (!isAss){
            if (!config.isMuted && !myself.isMuted()) {
                if (stats.audioOutboundRtp != null && preStats.audioOutboundRtp != null) {
                    bitrate = (int) ((stats.audioOutboundRtp.bytesSent - preStats.audioOutboundRtp.bytesSent) * 8 / (STATS_INTERVAL / 1000f) / 1024);
                }
                if (stats.audioOutboundRtp != null) {
                    codecMime = stats.getCodecMime(stats.audioOutboundRtp.trackId);
                }
                int audioLevel = stats.sendAudioTrack != null ? (int) (stats.sendAudioTrack.audioLevel * 100) : 0; // NOTE: 己端音量不受前面调用audioTrack#setVolume(int volume)的影响（但是远端会，不清楚为什么）
                audioInfo = new Statistics.AudioInfo(codecMime, bitrate, audioLevel);
                myself.setVolume(audioLevel);
            }
        }

        bitrate = 0;
        codecMime = "";
        String encoderImplementation = "";
        if (stats.videoOutboundRtp!=null && preStats.videoOutboundRtp!=null){
            bitrate = (int) ((stats.videoOutboundRtp.bytesSent - preStats.videoOutboundRtp.bytesSent)*8 / (STATS_INTERVAL/1000f) / 1024);
            encoderImplementation = stats.videoOutboundRtp.encoderImplementation;
        }
        if (stats.videoOutboundRtp!=null){
            codecMime = stats.getCodecMime(stats.videoOutboundRtp.trackId);
        }
        int frameRate = 0;
        int frameWidth = 0;
        int frameHeight = 0;
        if (stats.sendVideoTrack!=null && preStats.sendVideoTrack!=null){
            frameRate = (int) ((stats.sendVideoTrack.framesSent - preStats.sendVideoTrack.framesSent) / (STATS_INTERVAL/1000f));
            frameWidth = stats.sendVideoTrack.frameWidth;
            frameHeight = stats.sendVideoTrack.frameHeight;
        }
        Statistics.VideoInfo videoInfo = new Statistics.VideoInfo(codecMime, encoderImplementation, frameWidth, frameHeight, frameRate, bitrate);
        if (videoInfo.framerate<=0 || videoInfo.bitrate<=0){
            videoInfo.framerate = videoInfo.bitrate = 0;
        }

        String confereeId = isAss ? myself.mcuId+"-"+myself.terId+"-"+myself.e164+"-"+Conferee.ConfereeType.AssStream : myself.getId();
        statistics.confereeRelated.add(new Statistics.ConfereeRelated(confereeId, audioInfo, videoInfo));
    }


    void aggregateSubStats(StatsHelper.Stats stats, StatsHelper.Stats preStats, Statistics statistics, boolean isAss){
        if (stats==null || preStats==null || statistics==null || (stats.audioInboundRtpList==null && stats.videoInboundRtpList==null)){
            return;
        }

        Map<String, Statistics.AudioInfo> audioInfoMap = new HashMap<>();
        if (!isAss) {
            if (stats.audioInboundRtpList!=null && preStats.audioInboundRtpList!=null) {
                for (StatsHelper.AudioInboundRtp rtp : stats.audioInboundRtpList) {
                    for (StatsHelper.AudioInboundRtp preRtp : preStats.audioInboundRtpList) {
                        if (rtp.trackId==null || preRtp.trackId==null){
                            KLog.p(KLog.ERROR, "this inbound rtp has no track id !?");
                            continue;
                        }
                        if (rtp.trackId.equals(preRtp.trackId)) {
                            int bitrate = (int) ((rtp.bytesReceived - preRtp.bytesReceived) * 8 / (STATS_INTERVAL/1000f) / 1024);
                            String codecMime = stats.getCodecMime(rtp.trackId);
                            long rtTotalPack = (rtp.packetsReceived-preRtp.packetsReceived)+(rtp.packetsLost-preRtp.packetsLost);
                            int realtimeLostRate = rtTotalPack==0 ? 0 : (int) (100*(rtp.packetsLost-preRtp.packetsLost) / rtTotalPack);
                            StatsHelper.RecvAudioTrack recvAudioTrack = stats.getRecvAudioTrack(rtp.trackId);
                            if (recvAudioTrack == null){
                                KLog.p(KLog.ERROR, "no audio track of %s", rtp.trackId);
                                continue;
                            }
                            int audioLevel = (int) (recvAudioTrack.audioLevel
                                    / (10 * config.outputAudioVolume/100f) // 抵消增益拿到实际的音量。前面有调用audioTrack#setVolume(int volume)设置增益。
                                    * 100 // 原始值为[0, 1]，我们转为[1, 100]
                            );
                            Statistics.AudioInfo audioInfo = new Statistics.AudioInfo(codecMime, bitrate, audioLevel, rtp.packetsReceived, rtp.packetsLost, realtimeLostRate);
                            String kdStreamId = kdStreamId2RtcTrackIdMap.inverse().get(recvAudioTrack.trackIdentifier);
                            Conferee conferee = findConfereeByStreamId(kdStreamId);
                            if (conferee != null) {
                                if (!conferee.isMyself()) {
                                    audioInfoMap.put(conferee.getId(), audioInfo);
                                    conferee.setVolume(audioLevel);
                                }
                            } else {
                                KdStream kdStream = findStream(kdStreamId);
                                if (kdStream != null && kdStream.streamInfo.bMix) { // 混音
                                    statistics.common = new Statistics.Common(audioInfo);
                                } else {
                                    KLog.p(KLog.WARN, "track %s / %s does not belong to any conferee!", recvAudioTrack.trackIdentifier, kdStreamId);
                                }
                            }
                            break;
                        }
                    }
                }
            }
        }

        Map<String, Statistics.VideoInfo> videoInfoMap = new HashMap<>();
        if (stats.videoInboundRtpList!=null && preStats.videoInboundRtpList!=null) {
            for (StatsHelper.VideoInboundRtp rtp : stats.videoInboundRtpList) {
                for (StatsHelper.VideoInboundRtp preRtp : preStats.videoInboundRtpList) {
                    if (rtp.trackId==null || preRtp.trackId==null){
                        KLog.p(KLog.ERROR, "this inbound rtp has no track id !?");
                        continue;
                    }
                    if (rtp.trackId.equals(preRtp.trackId)) {
                        int bitrate = (int) ((rtp.bytesReceived - preRtp.bytesReceived) * 8 / (STATS_INTERVAL/1000f) / 1024);
                        String codecMime = stats.getCodecMime(rtp.trackId);
                        StatsHelper.RecvVideoTrack recvVideoTrack = stats.getRecvVideoTrack(rtp.trackId);
                        int frameRate = (int) ((recvVideoTrack.framesReceived - preStats.getRecvVideoTrack(preRtp.trackId).framesReceived) / (STATS_INTERVAL/1000f));
                        long rtTotalPack = (rtp.packetsReceived-preRtp.packetsReceived)+(rtp.packetsLost-preRtp.packetsLost);
                        int realtimeLostRate = rtTotalPack==0 ? 0 : (int) (100*(rtp.packetsLost-preRtp.packetsLost) / rtTotalPack);
                        Statistics.VideoInfo videoInfo = new Statistics.VideoInfo(codecMime, rtp.decoderImplementation, recvVideoTrack.frameWidth, recvVideoTrack.frameHeight, frameRate, bitrate, rtp.packetsReceived, rtp.packetsLost, realtimeLostRate);
                        if (videoInfo.framerate <= 0 || videoInfo.bitrate <= 0) {
                            videoInfo.framerate = videoInfo.bitrate = 0;
                        }
                        String kdStreamId = kdStreamId2RtcTrackIdMap.inverse().get(recvVideoTrack.trackIdentifier);
                        Conferee conferee = findConfereeByStreamId(kdStreamId);
                        if (conferee != null) {
                            videoInfoMap.put(conferee.getId(), videoInfo);
                        } else {
                            KLog.p(KLog.WARN, "track %s / %s does not belong to any conferee!", recvVideoTrack.trackIdentifier, kdStreamId);
                        }
                        break;
                    }
                }
            }
        }

        Set<String> confereeIds = new HashSet<>();
        confereeIds.addAll(audioInfoMap.keySet());
        confereeIds.addAll(videoInfoMap.keySet());
        for (String confereeId : confereeIds){
            Statistics.AudioInfo audioInfo = audioInfoMap.get(confereeId);
            Statistics.VideoInfo videoInfo = videoInfoMap.get(confereeId);
            statistics.confereeRelated.add(new Statistics.ConfereeRelated(confereeId, audioInfo, videoInfo));
        }

    }


    private void processStats(Statistics statistics){
        if (statistics == null){
            return;
        }
        // 语音激励
        Statistics.ConfereeRelated confereeRelated = statistics.findMaxAudioLevel();
        Conferee preMaxAudioConferee = findVoiceActivatedConferee();
        Conferee maxAudioConferee = null;
        if (confereeRelated != null){
            maxAudioConferee = findConfereeById(confereeRelated.confereeId);
            if (maxAudioConferee == null){
                KLog.p(KLog.ERROR, "max audio level does not belong to any conferee!");
            }
            Statistics.AudioInfo audioInfo = confereeRelated.audioInfo;
            int maxAudioLevel = audioInfo != null ? audioInfo.audioLevel : 0;
            int threshold = 10; // 小于该值我们认为不是人在说话，是环境噪音
            if (preMaxAudioConferee != maxAudioConferee){
                if (preMaxAudioConferee != null){
                    preMaxAudioConferee.setVoiceActivated(false);
                }
                if (maxAudioConferee != null && maxAudioLevel >= threshold){
                    maxAudioConferee.setVoiceActivated(true);
                }
            }else{
                if (maxAudioConferee != null && maxAudioLevel < threshold){
                    maxAudioConferee.setVoiceActivated(false);
                }
            }
        }else{
            KLog.p(KLog.ERROR, "no max audio level conferee!");
            if (preMaxAudioConferee != null){
                preMaxAudioConferee.setVoiceActivated(false);
            }
        }

        if (preMaxAudioConferee != maxAudioConferee) {
            Conferee finalMaxAudioConferee = maxAudioConferee;
            Stream.of(getNtfListeners(VoiceActivatedConfereeChangedListener.class))
                    .forEach(it -> it.onVoiceActivatedConfereeChanged(preMaxAudioConferee, finalMaxAudioConferee));
        }


        // 视频信号
        if (collectStatsCount % videoSignalCheckPerCount == 0) {
            Stream.of(statistics.confereeRelated).forEach(it -> {
                if (myself.getId().equals(it.confereeId)){
                    // 己端回显我们不根据统计信息展示视频信号，只受用户开关摄像头影响。
                    return;
                }
                Conferee conferee = findConfereeById(it.confereeId);
                if (conferee == null) {
                    KLog.p(KLog.ERROR, "no conferee with id %s", it.confereeId);
                    return;
                }
                Conferee.VideoSignalState vidSigState = conferee.getVideoSignalState();
                Statistics.VideoInfo videoInfo = it.videoInfo;
                float framerate = videoInfo != null ? videoInfo.framerate : 0;
                float bottomThreshold = 0.2f; // 可忍受的帧率下限，低于该下限则认为信号丢失
                float topThreshold = 1;   /* 尽管我们在帧率低于bottomThreshold时将状态设为WeakSignal但当帧率超过bottomThreshold时我们不立马设置状态为Normal
                                       而是等帧率回复到较高水平才切回Normal以使状态切换显得平滑而不是在临界值处频繁切换*/
                if (framerate < bottomThreshold && vidSigState == Conferee.VideoSignalState.Normal) {
                    conferee.setVideoSignalState(Conferee.VideoSignalState.Weak);
                } else if (framerate > topThreshold && Conferee.VideoSignalState.Weak == vidSigState) {
                    conferee.setVideoSignalState(Conferee.VideoSignalState.Normal);
                }
            });
        }

    }

    /**
     * 计算最近的指定时间内的接收码率
     * @param duration 最近多长时间段内。单位秒。
     * */
    int calcRecvBitrate(int duration){
        int periodNum = Math.max(duration*1000/STATS_INTERVAL, 1);
        int count = 0;
        int bitrateSum = 0;
        Iterator<Statistics> it = recentStats.descendingIterator();
        while (it.hasNext() && count < periodNum){
            Statistics stats = it.next();
            for (Statistics.ConfereeRelated confereeRelated : stats.confereeRelated){
                if (myself.getId().equals(confereeRelated.confereeId)){
                    // 我们只计算接收到的，自己发送的排除
                    continue;
                }
                bitrateSum += confereeRelated.audioInfo!=null ? confereeRelated.audioInfo.bitrate : 0;
                bitrateSum += confereeRelated.videoInfo!=null ? confereeRelated.videoInfo.bitrate : 0;
            }
            /*
            * 接收到的混音的码流纳入计算。
            * 直觉上，若会议中只有自己，不应该存在接收码流的，但是实测下来混音流始终会接收到。
            * 我们不做区别仍将其计算在内，因为这个值是给组件用的，我们如实记录就好。
            * */
            bitrateSum += (stats.common != null && stats.common.mixedAudio != null) ? stats.common.mixedAudio.bitrate : 0;

            ++count;
        }
        return bitrateSum/periodNum;
    }


    private boolean enableStatsLog = true;
    public void setStatsLogEnable(boolean enable){
        enableStatsLog = enable;
    }


    private void printStats(StatsHelper.Stats stats, boolean detail){
        if (null == stats){
            return;
        }
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
                KLog.p(conferee != null ? conferee.alias + " " + audioTrack : audioTrack.toString());
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

        if (detail) {
            KLog.p("---------- encoder");
            if (null != stats.encoderList) {
                for (StatsHelper.Codec codec : stats.encoderList) {
                    KLog.p(codec.toString());
                }
            }
            KLog.p("---------- decoder");
            if (null != stats.decoderList) {
                for (StatsHelper.Codec codec : stats.decoderList) {
                    KLog.p(codec.toString());
                }
            }
        }
    }

    
    private Statistics.ConfereeRelated getStats(String confereeId){
        Statistics statistics = latestStats();
        if (statistics == null){
            return null;
        }
        return Stream.of(statistics.confereeRelated)
                .filter(it-> it.confereeId.equals(confereeId))
                .findFirst()
                .orElse(null);
    }



    /**
     * 统计信息监听器
     * */
    public interface StatsListener extends INtfListener {
        void onStats(Statistics statistics);
    }


    /**
     * 登录状态变化监听器。
     * */
    public interface LoginStateChangedListener extends INtfListener {
        void onLoginStateChanged(int reason);
    }


    /**
     * 会议流程取消监听器。
     * 入会流程被取消，如对端取消呼叫。
     * */
    public interface ConfProcessCanceled extends INtfListener {
        /**
         * @param reason 取消原因{@link RtcResultCode}
         */
        void onConfProcessCanceled(int reason);
    }

    /**
     * 会议结束监听器
     * */
    public interface ConfFinishedListener extends  INtfListener {
        /**
         * @param reason 结束原因{@link RtcResultCode}
         */
        void onConfFinished(int reason);
    }

    /**
     * 会议邀请监听器
     * */
    public interface ConfInvitationListener extends  INtfListener{
        /**
         * @param confInvitationInfo 邀请信息
         */
        void onConfInvitation(ConfInvitationInfo confInvitationInfo);
    }

    /**
     * 与会方变更监听器
     * */
    public interface ConfereesChangedListener extends  INtfListener{
        /**
         * 与会方加入。
         * 一般情形下，用户收到该回调时调用{@link #createDisplay(Display.Type)}创建Display，
         * 然后调用{@link Display#setConferee(Conferee)} 将Display绑定到与会方以使与会方画面展示在Display上，
         * 如果还需要展示文字图标等deco，可调用{@link Conferee#addText(TextDecoration)}}, {@link Conferee#addPic(PicDecoration)}等deco设置相关接口。
         * */
        void onConfereeJoined(Conferee conferee);

        /**
         * 与会方离开。
         * 如果该Conferee对应的Display不需要了请调用{@link #releaseDisplay(Display)}销毁；
         * 如果后续要复用则可以不销毁，可以调用{@link Display#clear()} 清空内容；
         * NOTE: 会议结束时会销毁所有Display。用户不能跨会议复用Display。
         * */
        void onConfereeLeft(Conferee conferee);
    }


    /**
     * 与会方状态变更监听器
     * */
    public interface ConfereeStateChangedListener extends  INtfListener{
        /**
         * 哑音状态改变
         * {@link Conferee#isMuted()}
         * */
        void onMuteStateChanged(Conferee conferee);
    }


    /**
     * 会议即将结束监听器
     * */
    public interface ConfAboutToEndListener extends  INtfListener{
        /**
         * @param  remainingTime 剩余时长。单位：分钟
         * */
        void onConfAboutToEnd(int  remainingTime);
    }

    /**
     * 会议已被延长监听器
     * */
    public interface ConfProlongedListener extends  INtfListener{
        /**
         * @param  prolongedTime 延长的时长。单位：分钟
         * */
        void onConfProlonged(int  prolongedTime);
    }

    /**
     * 主持人变更监听器
     * */
    public interface PresenterChangedListener extends  INtfListener{
        /**
         * @param  predecessor 前任主持人。若为null表示没有前任，也即当前主持人为首位主持人。
         * @param  successor 继任的主持人。若为null表示没有继任的主持人，也即取消主持人。
         * */
        void onPresenterChanged(Conferee predecessor, Conferee successor);
    }

    /**
     * 主讲人变更监听器
     * */
    public interface KeynoteSpeakerChangedListener extends  INtfListener{
        /**
         * @param  predecessor 前任主讲人。若为null表示没有前任，也即当前主讲人为首位主讲人。
         * @param  successor 继任的主讲人。若为null表示没有继任的主讲人，也即取消主讲人。
         * */
        void onKeynoteSpeakerChanged(Conferee predecessor, Conferee successor);
    }

    /**
     * VIP变更监听器
     * */
    public interface VIPChangedListener extends  INtfListener{
        /**
         * @param added 新增的vip。若没有则为空列表
         * @param removed 移除的vip。若没有则为空列表
         * */
        void onVipChanged(@NonNull Set<Conferee> added, @NonNull Set<Conferee> removed);
    }

    /**
     * 会管短消息监听器
     * */
    public interface ConfManSMSListener extends INtfListener{
        void onConfManSMS(ConfManSMS sms);
    }

    /**
     * 需要会议密码监听器
     * */
    public interface ConfPasswordNeededListener extends INtfListener{
        void onConfPasswordNeeded();
    }

    /**
     * 与会方被选看监听器
     * */
    public interface SelectedToWatchListener extends INtfListener{
        /**
         * 被选看
         * */
        void onSelected(Conferee conferee);
        /**
         * 被取消选看
         * */
        void onUnselected(Conferee conferee);
    }

    /**
     * 画面合成监听器
     * */
    public interface ScenesCompositedListener extends INtfListener{
        /**
         * @param conferees 被纳入画面合成的与会方列表，按优先级从大到小排序
         * */
        void onComposite(List<Conferee> conferees);
        /**
         * @param conferees 被取消画面合成的与会方列表
         * */
        void onCancelComposite(List<Conferee> conferees);
    }

    /**
     * 语音激励方变更监听器。
     * 多方会议中某个与会方讲话声音最大，则认为该与会方处于语音激励状态，这往往暗示着该与会方需要被给予更多关注。
     * 界面可以据此做一些展示上的策略，比如让激励方展示在醒目位置。
     * 处于语音激励状态的与会方不同于会议中的发言人角色。发言人一般是主持人指定的，当前一段时间内会议内容的主要宣讲者，
     * 当发言人发言的过程中其他与会方可以发问讨论，发问方此时很有可能就成为了语音激励方。
     * */
    public interface VoiceActivatedConfereeChangedListener extends INtfListener {
        /**
         * @param  predecessor 前任语音激励方。若为null表示没有前任。
         * @param  successor 继任的语音激励方。若为null表示没有继任，即当前没有语音激励方。
         * */
        void onVoiceActivatedConfereeChanged(Conferee predecessor, Conferee successor);
    }

}

