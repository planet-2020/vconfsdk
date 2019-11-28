package com.kedacom.vconf.sdk.webrtc;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.media.projection.MediaProjection;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.kedacom.vconf.sdk.amulet.Caster;
import com.kedacom.vconf.sdk.amulet.IResultListener;
import com.kedacom.vconf.sdk.common.constant.EmConfProtocol;
import com.kedacom.vconf.sdk.common.constant.EmMtCallDisReason;
import com.kedacom.vconf.sdk.common.constant.EmMtChanState;
import com.kedacom.vconf.sdk.common.type.BaseTypeInt;
import com.kedacom.vconf.sdk.common.type.vconf.TAssVidStatus;
import com.kedacom.vconf.sdk.common.type.vconf.TMtAssVidStatusList;
import com.kedacom.vconf.sdk.common.type.vconf.TMtCallLinkSate;
import com.kedacom.vconf.sdk.utils.log.KLog;
import com.kedacom.vconf.sdk.utils.net.NetAddrHelper;
import com.kedacom.vconf.sdk.webrtc.bean.StreamInfo;
import com.kedacom.vconf.sdk.webrtc.bean.trans.TCreateConfResult;
import com.kedacom.vconf.sdk.webrtc.bean.trans.TLoginResult;
import com.kedacom.vconf.sdk.webrtc.bean.trans.TMtRtcSvrAddr;
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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.kedacom.vconf.sdk.webrtc.bean.*;

@SuppressWarnings("SwitchStatementWithTooFewBranches")
public class WebRtcManager extends Caster<Msg>{

    private static final String TAG = WebRtcManager.class.getSimpleName();

    private RtcConnector rtcConnector = new RtcConnector();

    private Context context;
    private Map<String, ProxyVideoSink> videoSinks = new HashMap<>();

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private EglBase eglBase;
    private PeerConnectionFactory factory;
    private List<PeerConnectionWrapper> connWrapperList = new ArrayList<>();
    private List<TRtcStreamInfo> streamInfos = new ArrayList<>();
    private BiMap<String, String> midStreamIdMap = HashBiMap.create();
    private List<StreamInfo> localStreamInfos = new ArrayList<>();

    private Handler handler = new Handler(Looper.getMainLooper());

    private static WebRtcManager instance;

    private WebRtcManager(Context context){
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
                Msg.Call,
                Msg.CreateConf,
                Msg.QuitConf,
                Msg.EndConf,
                Msg.AcceptInvitation,
                Msg.DeclineInvitation,
        }, this::onRsp);
        return processorMap;
    }

    @Override
    protected Map<Msg[], NtfProcessor<Msg>> ntfsProcessors() {
        Map<Msg[], NtfProcessor<Msg>> processorMap = new HashMap<>();
        processorMap.put(new Msg[]{
                Msg.StreamListReady,
                Msg.StreamJoined,
                Msg.StreamLeft,
                Msg.CallIncoming,
                Msg.P2pConfEnded,
                Msg.MultipartyConfEnded,
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
    public void login(String e164, IResultListener resultListener, ConfEventListener confEventListener){  // TODO 去掉e164，从aps获取有了
        TMtRtcSvrAddr rtcSvrAddr = (TMtRtcSvrAddr) get(Msg.GetSvrAddr);
//        if (null == rtcSvrAddr || rtcSvrAddr.dwIp<= 0){
//            KLog.p(KLog.ERROR, "invalid rtcSvrAddr, have you logined APS");
//            reportFailed(-1, resultListener);
//            return;
//        }
        if (null != rtcSvrAddr){
            KLog.p("rtcip=%s", NetAddrHelper.ipLong2Str(rtcSvrAddr.dwIp));
        }else{
            KLog.p(KLog.ERROR, "null == rtcSvrAddr");
        }

//        if (null == rtcSvrAddr){
            try {
                long ip = NetAddrHelper.ipStr2LongLittleEndian("172.16.179.114"); //FIXME 写死方便调试
//                rtcSvrAddr = new TMtRtcSvrAddr(ip, 7961,"0512110000004");
                rtcSvrAddr = new TMtRtcSvrAddr(ip, 7961, e164);
            } catch (NetAddrHelper.InvalidIpv4Exception e) {
                e.printStackTrace();
            }
//        }
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
        stopSession();
    }

    /**
     * 呼叫
     * NOTE：目前只支持同时开一个会。如果呼叫中/创建中或会议中状态，则返回失败，需要先退出会议状态。
     * @param peerId 对于点对点而言是对端e164，对于多方会议而言是会议e164
     **@param resultListener 结果监听器。
     *          成功: {@link MakeCallResult};
     *          失败：TODO
     * @param sessionEventListener 会话事件监听器
     * */
    public void makeCall(String peerId, IResultListener resultListener, SessionEventListener sessionEventListener){
        if (!startSession(sessionEventListener)){
            reportFailed(-1, resultListener);
            return;
        }
        req(Msg.Call, resultListener, peerId,
                1024, // XXX 上层传入 ???
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
        req(Msg.CreateConf, resultListener, ToDoConverter.confPara2CreateConference(confPara), EmConfProtocol.emrtc);
    }


    /**
     * 退出会议。
     * @param disReason 原因码
     * */
    public void quitConf(EmMtCallDisReason disReason, IResultListener resultListener){
        req(Msg.QuitConf, resultListener, disReason);
    }

    /**
     * 结束会议。
     * */
    public void endConf(IResultListener resultListener){
        req(Msg.EndConf, resultListener);
    }

    /**
     * 接受会议邀请
     * NOTE：目前只支持同时开一个会。如果呼叫中/创建中或会议中状态，则返回失败，需要先退出会议状态。
     * @param sessionEventListener 会话事件监听器
     * */
    public void acceptInvitation(IResultListener resultListener, SessionEventListener sessionEventListener){
        if (!startSession(sessionEventListener)){
            reportFailed(-1, resultListener);
            return;
        }
        req(Msg.AcceptInvitation, resultListener);
    }

    /**
     * 拒绝会议邀请
     * */
    public void declineInvitation(){
        req(Msg.DeclineInvitation, null);
    }


    private Intent screenCapturePermissionData;
    /**
     * 开始桌面共享
     * @param permissionData 截屏权限申请结果
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

    /**
     * 是否已静音。
     * */
    public boolean isSilenced(){
        PeerConnectionWrapper pcWrapper = getPcWrapper(CommonDef.CONN_TYPE_SUBSCRIBER);
        return null != pcWrapper && !pcWrapper.isRemoteAudioEnabled();
    }
    /**
     * 设置静音。（放开/屏蔽对方语音，默认放开）
     * @param bSilence true，屏蔽对方语音；false，放开对方语音。
     * */
    public void setSilence(boolean bSilence){
        PeerConnectionWrapper pcWrapper = getPcWrapper(CommonDef.CONN_TYPE_SUBSCRIBER);
        if (null != pcWrapper) {
            pcWrapper.setRemoteAudioEnable(!bSilence);
        }
    }

    /**
     * 是否已哑音。
     * */
    public boolean isMute(){
        PeerConnectionWrapper pcWrapper = getPcWrapper(CommonDef.CONN_TYPE_PUBLISHER);
        return null != pcWrapper && !pcWrapper.isLocalAudioEnabled();
    }

    /**
     * 设置哑音。（放开/屏蔽自己语音，默认放开）
     * @param bMute true，屏蔽自己语音；false，放开自己语音。
     * */
    public void setMute(boolean bMute){
        PeerConnectionWrapper pcWrapper = getPcWrapper(CommonDef.CONN_TYPE_PUBLISHER);
        if (null != pcWrapper) {
            pcWrapper.setLocalAudioEnable(!bMute);
        }
    }


    /**
     * 切换摄像头
     * */
    public void switchCamera(){
        PeerConnectionWrapper pcWrapper = getPcWrapper(CommonDef.CONN_TYPE_PUBLISHER);
        if (null != pcWrapper) {
            pcWrapper.switchCamera();
        }
    }

    /**
     * 当前摄像头是否为前置
     * @return true 前置；false 后置
     * */
    public boolean isCurrentFrontCamera(){
        PeerConnectionWrapper pcWrapper = getPcWrapper(CommonDef.CONN_TYPE_PUBLISHER);
        if (null != pcWrapper) {
            return pcWrapper.isPreferFrontCamera();
        }
        return true;
    }

    /**
     * 开启/关闭摄像头
     * */
    public void setCameraEnable(final boolean enable) {
        PeerConnectionWrapper pcWrapper = getPcWrapper(CommonDef.CONN_TYPE_PUBLISHER);
        if (null != pcWrapper) {
            pcWrapper.setLocalVideoEnable(enable);
        }
    }

    /**
     * 开启/关闭视频
     * */
    public void setVideoEnable(final boolean enable) {
        PeerConnectionWrapper pcWrapper = getPcWrapper(CommonDef.CONN_TYPE_PUBLISHER);
        if (null != pcWrapper) {
            pcWrapper.setLocalVideoEnable(enable);
        }
        pcWrapper = getPcWrapper(CommonDef.CONN_TYPE_SUBSCRIBER);
        if (null != pcWrapper) {
            pcWrapper.setRemoteVideoEnable(enable);
        }
        pcWrapper = getPcWrapper(CommonDef.CONN_TYPE_ASS_PUBLISHER);
        if (null != pcWrapper) {
            pcWrapper.setLocalVideoEnable(enable);
        }
        pcWrapper = getPcWrapper(CommonDef.CONN_TYPE_ASS_SUBSCRIBER);
        if (null != pcWrapper) {
            pcWrapper.setRemoteVideoEnable(enable);
        }
    }

    private boolean onRsp(Msg rsp, Object rspContent, IResultListener listener, Msg req, Object[] reqParas) {
        switch (rsp){
            case LoginRsp:
                TLoginResult loginResult = (TLoginResult) rspContent;
                KLog.p("loginResult: %s", loginResult.AssParam.basetype);
                if (100 == loginResult.AssParam.basetype){
                    reportSuccess(null, listener);
                }else{
                    reportFailed(-1, listener);
                }
                break;

            case Calling:
                TMtCallLinkSate callLinkSate = (TMtCallLinkSate) rspContent;
                KLog.p("Calling: %s", callLinkSate);
                if (EmConfProtocol.emrtc != callLinkSate.emConfProtocol){
                    return false;
                }
                break;

            case P2pConfStarted:
                callLinkSate = (TMtCallLinkSate) rspContent;
                KLog.p("P2pConfStarted: %s", callLinkSate);
                if (Msg.Call == req) {
                    reportSuccess(ToDoConverter.callLinkState2MakeCallResult(callLinkSate), listener);
                }else if (Msg.CreateConf == req){
                    reportSuccess(ToDoConverter.callLinkState2CreateConfResult(callLinkSate), listener);
                }
                break;

            case P2pConfEnded:
                stopSession();
                BaseTypeInt reason = (BaseTypeInt) rspContent;
                KLog.p("P2pConfEnded: %s", reason.basetype);
                if (Msg.QuitConf == req) {
                    reportSuccess(null, listener);
                }else{
                    reportFailed(-1, listener);
                }
                break;

            case MultipartyConfStarted:
                callLinkSate = (TMtCallLinkSate) rspContent;
                KLog.p("MultipartyConfStarted: %s", callLinkSate);
                reportSuccess(ToDoConverter.callLinkState2MakeCallResult(callLinkSate), listener);
                break;

            case MultipartyConfEnded:
                stopSession();
                reason = (BaseTypeInt) rspContent;
                KLog.p("MultipartyConfEnded: %s", reason.basetype);
                if (Msg.EndConf == req) {
                    reportSuccess(null, listener);
                }else{
                    reportFailed(-1, listener);
                }
                break;

            case ToggleScreenShareRsp:
                TMtAssVidStatusList assVidStatusList = (TMtAssVidStatusList) rspContent;
                if (assVidStatusList.arrTAssVidStatus.length == 0){
                    reportFailed(-1, listener);
                }else{
                    TAssVidStatus assVidStatus = assVidStatusList.arrTAssVidStatus[0]; // 目前仅支持一路
                    if (EmMtChanState.emChanConnected == assVidStatus.emChanState){
                        reportSuccess(null, listener);
                    }else{
                        reportFailed(-1, listener);
                    }
                }
                break;

            case CreateConfRsp:
                TCreateConfResult tCreateConfResult = (TCreateConfResult) rspContent;
                if (1000 != tCreateConfResult.MainParam.dwErrorID){
                    cancelReq(req, listener);
                    reportFailed(-1, listener);
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
            case QuitConf:
            case EndConf:
            case AcceptInvitation:
                stopSession();
                break;
        }
        return super.onTimeout(req, rspListener, reqPara);
    }



    private void onNtfs(Msg ntfId, Object ntfContent, Set<Object> listeners) {
        switch (ntfId){

            case CallIncoming:
                TMtCallLinkSate callLinkSate = (TMtCallLinkSate) ntfContent;
                KLog.p("CallIncoming: %s", callLinkSate);
                if (null != confEventListener) confEventListener.onConfInvitation(ToDoConverter.callLinkSate2ConfInvitationInfo(callLinkSate));
                break;

            case P2pConfEnded:
                stopSession();
                BaseTypeInt reason = (BaseTypeInt) ntfContent;
                KLog.p("P2pConfEnded: %s", reason.basetype);
                if (null != confEventListener) confEventListener.onConfFinished();
                break;

            case MultipartyConfEnded:
                stopSession();
                reason = (BaseTypeInt) ntfContent;
                KLog.p("MultipartyConfEnded: %s", reason.basetype);
                if (null != confEventListener) confEventListener.onConfFinished();
                break;

            case StreamListReady:
                KLog.p("StreamListReady");
                List<TRtcPlayItem> rtcPlayItems = new ArrayList<>();
                TRtcStreamInfoList streamInfoList = (TRtcStreamInfoList) ntfContent;
                streamInfos.clear();
                streamInfos.addAll(streamInfoList.atStramInfoList);
                for (TRtcStreamInfo streamInfo : streamInfos){
                    KLog.p("streamInfo: %s", streamInfo);
                    if (streamInfo.bAudio){
                        continue;
                    }
                    TRtcPlayItem rtcPlayItem = new TRtcPlayItem();
                    rtcPlayItem.emRes = streamInfo.aemSimcastRes.get(0); // FIXME 调试方便，实际要以需求为准
                    rtcPlayItem.achStreamId = streamInfo.achStreamId;
                    rtcPlayItem.bLocal = false;
                    rtcPlayItem.bAss = streamInfo.bAss;
                    rtcPlayItems.add(rtcPlayItem);
                }
                set(Msg.SetPlayPara, new TRtcPlayParam(rtcPlayItems));
                break;
            case StreamJoined:  //NOTE: 这里是增量过来的
                KLog.p("StreamJoined");
                rtcPlayItems = new ArrayList<>();
                streamInfoList = (TRtcStreamInfoList) ntfContent;
                streamInfos.addAll(streamInfoList.atStramInfoList);
                for (TRtcStreamInfo streamInfo : streamInfos){
                    if (streamInfo.bAudio){
                        continue;
                    }
                    TRtcPlayItem rtcPlayItem = new TRtcPlayItem();
                    rtcPlayItem.emRes = streamInfo.aemSimcastRes.get(0); // FIXME 调试方便，实际要以需求为准
                    rtcPlayItem.achStreamId = streamInfo.achStreamId;
                    rtcPlayItem.bLocal = false;
                    rtcPlayItem.bAss = streamInfo.bAss;
                    rtcPlayItems.add(rtcPlayItem);
                }
                set(Msg.SetPlayPara, new TRtcPlayParam(rtcPlayItems)); // 这里设置是增量的，还是覆盖的？如果是覆盖的，我需要本地记录下原本的列表？
                break;
            case StreamLeft: //NOTE: 这里是增量过来的
                KLog.p("StreamLeft");
                rtcPlayItems = new ArrayList<>();
                streamInfoList = (TRtcStreamInfoList) ntfContent;

                for (TRtcStreamInfo streamInfo : streamInfoList.atStramInfoList){
                    Iterator<TRtcStreamInfo> iterator = streamInfos.iterator();
                    while (iterator.hasNext()) {
                        TRtcStreamInfo localStreamInfo = iterator.next();
                        if (localStreamInfo.achStreamId.equals(streamInfo.achStreamId)){
                            iterator.remove();
                            break;
                        }
                    }
                }

                for (TRtcStreamInfo streamInfo : streamInfos){
                    if (streamInfo.bAudio){
                        continue;
                    }
                    TRtcPlayItem rtcPlayItem = new TRtcPlayItem();
                    rtcPlayItem.emRes = streamInfo.aemSimcastRes.get(0); // FIXME 调试方便，实际要以需求为准
                    rtcPlayItem.achStreamId = streamInfo.achStreamId;
                    rtcPlayItem.bLocal = false;
                    rtcPlayItem.bAss = streamInfo.bAss;
                    rtcPlayItems.add(rtcPlayItem);
                }
                set(Msg.SetPlayPara, new TRtcPlayParam(rtcPlayItems));
                break;
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

        int videoWidth = context.getResources().getDisplayMetrics().widthPixels;
        int videoHeight = context.getResources().getDisplayMetrics().heightPixels;
        PeerConnectionConfig pubConnConfig = new PeerConnectionConfig(
                        videoWidth,
                        videoHeight,
                        20,
                        1700,
                        "VP8",
                        32,
                        "OPUS"
                );


        eglBase = EglBase.create();
        executor.execute(() -> {
            createPeerConnectionFactory(
                    eglBase,
                    new PeerConnectionFactory.Options(),
                    new PeerConnectionFactoryConfig(Arrays.asList("VP8", "H264 High"),true)
            );

            /* 创建peerconnectclient。
             * NOTE：一个peerconnect可以处理多路码流，收发均可。
             * 但业务要求主流发/收、辅流发/收4种情形分别用单独的peerconnect处理，故此处创建4个。
             * */
            connWrapperList.add(createPeerConnectionWrapper(CommonDef.CONN_TYPE_PUBLISHER, pubConnConfig));
            connWrapperList.add(createPeerConnectionWrapper(CommonDef.CONN_TYPE_SUBSCRIBER, pubConnConfig));
            connWrapperList.add(createPeerConnectionWrapper(CommonDef.CONN_TYPE_ASS_PUBLISHER, pubConnConfig));
            connWrapperList.add(createPeerConnectionWrapper(CommonDef.CONN_TYPE_ASS_SUBSCRIBER, pubConnConfig));
        });

        KLog.p("session started ");

        return true;
    }


    /**
     * 停止会话
     * */
    private synchronized void stopSession(){
        if (!bSessionStarted){
            KLog.p(KLog.ERROR, "session has stopped already!");
            return;
        }
        bSessionStarted = false;
        sessionEventListener = null;

        rtcConnector.setSignalingEventsCallback(null);

        for (PeerConnectionWrapper peerConnectionWrapper : connWrapperList){
            peerConnectionWrapper.close();
        }
        connWrapperList.clear();

        if (factory != null) {
            factory.dispose();
            factory = null;
        }

        if (null != eglBase) {
            eglBase.release();
            eglBase = null;
        }

        for (ProxyVideoSink videoSink : videoSinks.values()){
            videoSink.release();
        }
        videoSinks.clear();

        midStreamIdMap.clear();
        streamInfos.clear();
        localStreamInfos.clear();
        screenCapturePermissionData = null;

        // destroy audiomanager
//        if (audioManager != null) {
//            audioManager.stop();
//            audioManager = null;
//        }

        KLog.p("session stopped ");

    }


    private void createPeerConnectionFactory(EglBase eglBase, PeerConnectionFactory.Options options, PeerConnectionFactoryConfig config) {
        if (null != factory){
            KLog.p(KLog.ERROR, "Factory exists!");
            return;
        }

        final AudioDeviceModule adm = createJavaAudioDevice();

        final VideoEncoderFactory encoderFactory;
        final VideoDecoderFactory decoderFactory;

        if (config.enableVideoCodecHwAcceleration) {
            encoderFactory = new DefaultVideoEncoderFactory(
                    eglBase.getEglBaseContext(),
                    config.videoCodecList.contains("VP8"),
                    config.videoCodecList.contains("H264 High"));
            decoderFactory = new DefaultVideoDecoderFactory(eglBase.getEglBaseContext());
        } else {
            encoderFactory = new SoftwareVideoEncoderFactory();
            decoderFactory = new SoftwareVideoDecoderFactory();
        }

        PeerConnectionFactory.initialize(
                PeerConnectionFactory.InitializationOptions.builder(context)
                        .createInitializationOptions());

        factory = PeerConnectionFactory.builder()
                .setOptions(options)
                .setAudioDeviceModule(adm)
                .setVideoEncoderFactory(encoderFactory)
                .setVideoDecoderFactory(decoderFactory)
                .createPeerConnectionFactory();

        adm.release();
    }



    private PeerConnectionWrapper createPeerConnectionWrapper(int connType, PeerConnectionConfig config) {
        if (null == factory){
            throw new RuntimeException("Factory not exists!");
        }

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

        PCObserver pcObserver = new PCObserver();
        PeerConnection peerConnection = factory.createPeerConnection(rtcConfig, pcObserver);
        if (null == peerConnection){
            throw new RuntimeException("createPeerConnection failed!");
        }

        return new PeerConnectionWrapper(connType, peerConnection, config, new SDPObserver(), pcObserver);
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


    private class ProxyVideoSink implements VideoSink {
        private String name;
        private List<Display> targets = new ArrayList<>();
        private long timestamp = System.currentTimeMillis();

        ProxyVideoSink(String name) {
            this.name = name;
        }

        @Override
        synchronized public void onFrame(VideoFrame frame) {
            long curts = System.currentTimeMillis();
            long ts = timestamp;
            if (curts - ts > 5000){
                timestamp = curts;
                KLog.p("%s onFrame ", name);
            }

            for (Display target : targets){
                if (curts - ts > 5000) {
                    if (target.enabled) {
                        KLog.p("sink %s render frame onto display %s ", name, target);
                    }else{
                        KLog.p("Dropping frame because display %s is disabled ", target);
                    }
                }
                if (!target.enabled) {
                    continue;
                }
                target.onFrame(frame);
            }

        }

        synchronized void addTarget(Display target) {
            KLog.p("add Display %s to sink %s", target, name);
            targets.add(target);
        }

        synchronized boolean delTarget(Display target) {
            KLog.p("delete Display %s from sink %s", target, name);
            return targets.remove(target);
        }

        synchronized void release() {
            for (Display display : targets){
                display.release();
            }
            targets.clear();
        }

    }


    /**
     * 用于显示码流的控件。
     * */
    public class Display extends SurfaceViewRenderer{
        private boolean enabled = true;
        private int decorateIdBase = 0;
        public final int INVALID_DECORATION_ID = decorateIdBase - 1;
        private CopyOnWriteArrayList<TextDecoration> onDisplayTextList = new CopyOnWriteArrayList<>();
        private CopyOnWriteArrayList<PicDecoration> onDisplayPicList = new CopyOnWriteArrayList<>();

        private Display(Context context) {
            super(context);
            init(eglBase.getEglBaseContext(), null);
            setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
            setEnableHardwareScaler(true);
            setWillNotDraw(false);
        }

        public Display() {
            this(context);
        }


        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            for (TextDecoration deco : onDisplayTextList){
                canvas.drawText(deco.text, deco.x, deco.y, deco.paint);
            }

            for (PicDecoration deco : onDisplayPicList){
                canvas.drawBitmap(deco.pic, deco.x, deco.y, deco.paint);
            }

        }

        /**
         * 设置是否在所有Display最前端展示。可用于多个Display层叠的场景
         * */
        public void setOnTopOverOtherDisplays(boolean bOnTop){
            setZOrderMediaOverlay(bOnTop);
        }

        public boolean isEnable() {
            return enabled;
        }
        /**
         * 设置是否使能。
         * @param enable true 使能；false 禁用
         * 若禁用则Display不会展示码流。默认是使能的。禁用后可通过该接口再设置使能以重新展示码流
         * */
        public void setEnable(boolean enable){
            KLog.p("Display %s enable=%s", this, enable);
            enabled = enable;
            setWillNotDraw(!enable);
        }

        /**
         * 添加文字
         * @return 文字对应的id
         * */
        public int addOnDisplayText(TextDecoration decoration){
            onDisplayTextList.add(decoration);
            return decoration.id;
        }

        /**
         * 删除文字
         * @param id 文字对应的id
         * @return 成功返回被删除的文字decoration，失败返回null
         * */
        public TextDecoration delOnDisplayText(int id){
            for (TextDecoration decoration : onDisplayTextList){
                if (id == decoration.id){
                    onDisplayTextList.remove(decoration);
                    return decoration;
                }
            }
            return null;
        }

        /**
         * 查找文字
         * @param id 文字对应的id
         * @return 若存在则返回该文字decoration，否则返回null
         * */
        public TextDecoration findOnDisplayText(int id){
            for (TextDecoration decoration : onDisplayTextList){
                if (id == decoration.id){
                    return decoration;
                }
            }
            return null;
        }

        /**
         * 清空文字
         * */
        public void clearOnDisplayText(){
            onDisplayTextList.clear();
        }

        /**
         * 添加图片
         * @return 图片对应的id
         * */
        public int addOnDisplayPic(PicDecoration decoration){
            onDisplayPicList.add(decoration);
            return decoration.id;
        }

        /**
         * 删除图片
         * @param id 图片对应的id
         * @return 成功返回被删除的图片decoration，失败返回null
         * */
        public PicDecoration delOnDisplayPic(int id){
            for (PicDecoration decoration : onDisplayPicList){
                if (id == decoration.id){
                    onDisplayPicList.remove(decoration);
                    return decoration;
                }
            }
            return null;
        }

        /**
         * 查找图片
         * @param id 图片对应的id
         * @return 若存在则返回该图片decoration，否则返回null
         * */
        public PicDecoration findOnDisplayPic(int id){
            for (PicDecoration decoration : onDisplayPicList){
                if (id == decoration.id){
                    return decoration;
                }
            }
            return null;
        }

        /**
         * 清空图片
         * */
        public void clearOnDisplayPic(){
            onDisplayPicList.clear();
        }

        public class TextDecoration{
            private int id;
            public String text;    // 要展示的文字
            public int x;          // 左上角x坐标
            public int y;          // 左上角y坐标
            public Paint paint;

            public TextDecoration(String text, int x, int y, Paint paint) {
                this.id = ++decorateIdBase;
                this.text = text;
                this.x = x;
                this.y = y;
                this.paint = paint;
            }
        }

        public class PicDecoration{
            private int id;
            public Bitmap pic;     // 要展示的文字
            public int x;          // 左上角x坐标
            public int y;          // 左上角y坐标
            public Paint paint;

            public PicDecoration(Bitmap pic, int x, int y, Paint paint) {
                this.id = ++decorateIdBase;
                this.pic = pic;
                this.x = x;
                this.y = y;
                this.paint = paint;
            }
        }

    }

    public static final String STREAMID_NULL = "NULL";
    /**
     * 将Display绑定到码流。
     * 一个Display只会绑定到一路码流；
     * 多个Display可以绑定到同一路码流；
     * 若一个Display已经绑定到某路码流，则该绑定会先被解除，然后建立新的绑定关系；
     * @param streamId 码流Id。
     *                 若绑定到{@link #STREAMID_NULL}则该Display不会展示码流，相当于于解除绑定。
     *                 NOTE：不能通过{@link #getDisplay(String)}找回绑定到{@link #STREAMID_NULL}的Display
     *                 对于画面交换的使用场景有个便利方法可以使用{@link #swapDisplay(Display, Display)}。
     *                 可以通过{@link #getStreamId(Display)}获取某个Display对应的streamId，这适用于“想要绑定到某个Display对应的流”的场景，比如多画面展示同一路码流。
     * */
    public boolean bindDisplay(Display display, String streamId){
        KLog.p("bind display %s to stream %s", display, streamId);
        ProxyVideoSink sink = videoSinks.get(streamId);
        if (null == sink && !STREAMID_NULL.equals(streamId)){
            KLog.p(KLog.ERROR, "no such stream %s", streamId);
            return false;
        }

        String boundStreamId = getStreamId(display);
        if (null != boundStreamId){
            ProxyVideoSink boundSink = videoSinks.get(boundStreamId);
            boundSink.delTarget(display);
        }
        if (null != sink) {
            sink.addTarget(display);
        }

        return true;
    }


    /**
     * 切换画面内容
     * */
    public void swapDisplayContent(Display display1, Display display2){
        KLog.p("swap display %s and display %s", display1, display2);
        // 切换绑定的流
        String streamId1 = getStreamId(display1);
        String streamId2 = getStreamId(display2);
        if (null == streamId1) streamId1 = STREAMID_NULL;
        if (null == streamId2) streamId2 = STREAMID_NULL;
        bindDisplay(display1, streamId2);
        bindDisplay(display2, streamId1);
        // 切换贴在display上面的文字图片
        CopyOnWriteArrayList<Display.TextDecoration> textDecorationList1 = display1.onDisplayTextList;
        display1.onDisplayTextList = display2.onDisplayTextList;
        display2.onDisplayTextList = textDecorationList1;
        CopyOnWriteArrayList<Display.PicDecoration> picDecorationList1 = display1.onDisplayPicList;
        display1.onDisplayPicList = display2.onDisplayPicList;
        display2.onDisplayPicList = picDecorationList1;
    }


    /**
     * 根据流的目标视图获取流ID
     * @param display 流的目标视图，流投射到该视图上展示。
     * @see #bindDisplay(Display, String)
     * */
    public String getStreamId(Display display){
        for (Map.Entry<String, ProxyVideoSink> entry : videoSinks.entrySet()){
            for (Display target : entry.getValue().targets){
                if (display == target){
                    return entry.getKey();
                }
            }
        }
        return null;
    }

    /**
     * 获取流信息
     * */
    public StreamInfo getStreamInfo(String streamId){
        KLog.p("streamId=%s", streamId);
        for (StreamInfo localStreamInfo : localStreamInfos){
            KLog.p("localStreamInfo.streamId=%s", localStreamInfo.streamId);
            if (localStreamInfo.streamId.equals(streamId)){
                return localStreamInfo;
            }
        }
        for (TRtcStreamInfo remoteStreamInfo : streamInfos){
            KLog.p("remoteStreamInfo.streamId=%s", remoteStreamInfo.achStreamId);
            if (remoteStreamInfo.achStreamId.equals(streamId)){
                return ToDoConverter.rtcStreamInfo2StreamInfo(remoteStreamInfo);
            }
        }
        return null;
    }

    /**
     * 根据流ID获取目标视图
     * @param streamId 流Id
     * @see  #bindDisplay(Display, String)
     * */
    public List<Display> getDisplay(String streamId){
        ProxyVideoSink videoSink = videoSinks.get(streamId);
        return null != videoSink ? videoSink.targets : null;
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


    private PeerConnectionWrapper getPcWrapper(int connType){
        for (PeerConnectionWrapper peerConnectionWrapper : connWrapperList){
            if (peerConnectionWrapper.connType == connType){
                return peerConnectionWrapper;
            }
        }
        return null;
    }

    private PeerConnectionWrapper getPcWrapper(PCObserver pcObserver){
        for (PeerConnectionWrapper peerConnectionWrapper : connWrapperList){
            if (peerConnectionWrapper.pcObserver == pcObserver){
                return peerConnectionWrapper;
            }
        }
        return null;
    }

    private PeerConnectionWrapper getPcWrapper(SDPObserver sdpObserver){
        for (PeerConnectionWrapper peerConnectionWrapper : connWrapperList){
            if (peerConnectionWrapper.sdpObserver == sdpObserver){
                return peerConnectionWrapper;
            }
        }
        return null;
    }



//    // FORDEBUG
//    public boolean startSignaling()
//    {
//        MtMsg msg = new MtMsg();
//        msg.SetMsgId("Ev_MT_GetOffer_Cmd");
//        msg.addMsg(BasePB.TU32.newBuilder().setValue(CommonDef.CONN_TYPE_ASS_PUBLISHER).build());
//        msg.addMsg(BasePB.TU32.newBuilder().setValue(CommonDef.MEDIA_TYPE_AV).build());
//
//        long nSrcID = Connector.MAKEIID(RtcConnector.WEBRTC_ID, (short)1 );
//        long nSrcNodeId=0;
//        long nDstID = nSrcID;
//        long nDstNodeId=nSrcNodeId;
//
//        byte[] abyContent = msg.Encode();
//        int nRet = Connector.PostOspMsg( EmMtOspMsgSys.Ev_MtOsp_ProtoBufMsg.getnVal(), abyContent, abyContent.length,
//                nDstID, nDstNodeId, nSrcID, nSrcNodeId, 5000 );
//        if (nRet != 0){
//            KLog.p(KLog.ERROR, "post msg %s failed, ret=%s", msg.GetMsgId(), nRet);
//            return false;
//        }
//
//        return true;
//    }

    /**
     * 会议事件监听器
     * */
    public interface ConfEventListener {
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
         * 流来了
         * 正常情形下，用户收到该回调应创建{@link Display}并调用{@link #bindDisplay(Display, String)}以使码流展示在Display上。
         * */
        void onStream(StreamInfo stream);
        /**
         * 流走了
         * 用户不需要处理stream和Display的绑定关系，只需处理界面相关控件的显隐。
         * */
        void onStreamRemoved(StreamInfo stream);
    }
    private SessionEventListener sessionEventListener;


    private class PeerConnectionFactoryConfig{
        private List<String> videoCodecList;
        private boolean enableVideoCodecHwAcceleration;
        PeerConnectionFactoryConfig(@NonNull List<String> videoCodecList, boolean enableVideoCodecHwAcceleration) {
            this.videoCodecList = videoCodecList;
            this.enableVideoCodecHwAcceleration = enableVideoCodecHwAcceleration;
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
        boolean isLocalAudioEnabled;
        boolean isRemoteAudioEnabled;
        boolean isLocalVideoEnabled;
        boolean isRemoteVideoEnabled;
        boolean bPreferFrontCamera;

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
            this.isLocalAudioEnabled = true;
            this.isRemoteAudioEnabled = true;
            this.isLocalVideoEnabled = true;
            this.isRemoteVideoEnabled = true;
            this.bPreferFrontCamera = true;
        }

        PeerConnectionConfig(PeerConnectionConfig config) {
            this.videoWidth = config.videoWidth;
            this.videoHeight = config.videoHeight;
            this.videoFps = config.videoFps;
            this.videoMaxBitrate = config.videoMaxBitrate;
            this.videoCodec = config.videoCodec;
            this.audioStartBitrate = config.audioStartBitrate;
            this.audioCodec = config.audioCodec;
            this.isLocalAudioEnabled = config.isLocalAudioEnabled;
            this.isRemoteAudioEnabled = config.isRemoteAudioEnabled;
            this.isLocalVideoEnabled = config.isLocalVideoEnabled;
            this.isRemoteVideoEnabled = config.isRemoteVideoEnabled;
            this.bPreferFrontCamera = config.bPreferFrontCamera;
        }
    }



    private class RtcConnectorEventListener implements RtcConnector.SignalingEvents{

        @Override
        public void onGetOfferCmd(int connType, int mediaType) {
            KLog.p("onGetOfferCmd: connType=%s, mediaType=%s", connType, mediaType);
            executor.execute(()->{
                PeerConnectionWrapper pcWrapper = getPcWrapper(connType);

                pcWrapper.checkSdpState(pcWrapper.Idle);

                pcWrapper.curMediaType = mediaType;
                VideoCapturer videoCapturer = null;
                if ((CommonDef.MEDIA_TYPE_VIDEO == mediaType
                        || CommonDef.MEDIA_TYPE_ASS_VIDEO == mediaType)){
                    if (CommonDef.CONN_TYPE_PUBLISHER == connType) {
                        videoCapturer = createCameraCapturer(new Camera2Enumerator(context));
                    }else if (CommonDef.CONN_TYPE_ASS_PUBLISHER == connType) {
                        videoCapturer = createScreenCapturer();
                    }
                }
                if (null != videoCapturer) {
                    pcWrapper.createVideoTrack(videoCapturer);
//                    pcWrapper.createVideoTrack2(videoCapturer);
                }

                if ((CommonDef.MEDIA_TYPE_AUDIO == mediaType
                        || CommonDef.MEDIA_TYPE_AV == mediaType)) {
                    pcWrapper.createAudioTrack();
                }

                pcWrapper.pc.createOffer(pcWrapper.sdpObserver, new MediaConstraints());
                if (CommonDef.MEDIA_TYPE_AV == mediaType){
                    // 针对多路码流的情形，我们需要一路一路地发布（平台的限制）
                    // 我们先发Audio，等到收到setAnswerCmd后再发Video
                    pcWrapper.setSdpType(pcWrapper.AudioOffer);
                }else{
                    pcWrapper.setSdpType(pcWrapper.Offer);
                }

                pcWrapper.setSdpState(pcWrapper.Creating);

            });

        }

        @Override
        public void onSetOfferCmd(int connType, String offerSdp, List<RtcConnector.TRtcMedia> rtcMediaList) {
            KLog.p("connType=%s", connType);
            for (RtcConnector.TRtcMedia rtcMedia : rtcMediaList) {
                KLog.p("mid=%s, streamid=%s", rtcMedia.mid, rtcMedia.streamid);
                midStreamIdMap.put(rtcMedia.mid, rtcMedia.streamid);
            }

            executor.execute(()-> {
                PeerConnectionWrapper pcWrapper = getPcWrapper(connType);
                pcWrapper.checkSdpState(pcWrapper.Idle);
                pcWrapper.setSdpType(pcWrapper.Answer);
                pcWrapper.pc.setRemoteDescription(pcWrapper.sdpObserver,
                        new SessionDescription(SessionDescription.Type.OFFER, offerSdp)
                );
                pcWrapper.setSdpState(pcWrapper.SettingRemote);
            });
        }

        @Override
        public void onSetAnswerCmd(int connType, String answerSdp) {
            KLog.p("connType=%s", connType);
            executor.execute(() -> {
                PeerConnectionWrapper pcWrapper = getPcWrapper(connType);
                pcWrapper.checkSdpState(pcWrapper.Sending);
                pcWrapper.pc.setRemoteDescription(pcWrapper.sdpObserver,
                        new SessionDescription(SessionDescription.Type.ANSWER, answerSdp)
                );
                pcWrapper.setSdpState(pcWrapper.SettingRemote);
            });

        }

        @Override
        public void onSetIceCandidateCmd(int connType, String sdpMid, int sdpMLineIndex, String sdp) {
            KLog.p("connType=%s, sdpMid=%s, sdpMLineIndex=%s", connType, sdpMid, sdpMLineIndex);
            executor.execute(()-> {
                PeerConnectionWrapper pcWrapper = getPcWrapper(connType);
                pcWrapper.addCandidate(new IceCandidate(sdpMid, sdpMLineIndex, sdp));
            });
        }

        @Override
        public void onGetFingerPrintCmd(int connType) {
            KLog.p("connType=%s", connType);
            executor.execute(()-> {
                PeerConnectionWrapper pcWrapper = getPcWrapper(connType);
                pcWrapper.checkSdpState(pcWrapper.Idle);
                pcWrapper.setSdpType(pcWrapper.FingerPrintOffer);
                pcWrapper.createAudioTrack();
                pcWrapper.pc.createOffer(pcWrapper.sdpObserver, new MediaConstraints());
                pcWrapper.setSdpState(pcWrapper.Creating);
            });
        }
    }




    private class SDPObserver implements SdpObserver {

        @Override
        public void onCreateSuccess(final SessionDescription origSdp) {
            executor.execute(() -> {
                PeerConnectionWrapper pcWrapper = getPcWrapper(this);
                KLog.p("create local sdp success: type=%s", pcWrapper.sdpType);
                pcWrapper.checkSdpState(pcWrapper.Creating);
                if (pcWrapper.isSdpType(pcWrapper.FingerPrintOffer)){
                    pcWrapper.destoryAudioTrack();
                    rtcConnector.sendFingerPrint(pcWrapper.connType, getFingerPrint(origSdp.description));
                    pcWrapper.setSdpState(pcWrapper.Idle);
                }else {
                    pcWrapper.pc.setLocalDescription(this, origSdp);
                    pcWrapper.setSdpState(pcWrapper.SettingLocal);
                }
            });
        }

        @Override
        public void onSetSuccess() {
            executor.execute(() -> {
                PeerConnectionWrapper pcWrapper = getPcWrapper(this);
                pcWrapper.checkSdpState(pcWrapper.SettingLocal, pcWrapper.SettingRemote);
                PeerConnection pc = pcWrapper.pc;
                if (pcWrapper.isSdpType(pcWrapper.Offer)){
                    if (pcWrapper.isSdpState(pcWrapper.SettingLocal)){
                        KLog.p("setLocalDescription for Offer success, sending offer...");
                        boolean bAudio = pcWrapper.curMediaType == CommonDef.MEDIA_TYPE_AUDIO;
                        RtcConnector.TRtcMedia rtcMedia = new RtcConnector.TRtcMedia(
                                getMid(pc.getLocalDescription().description, bAudio),
                                bAudio ? null : createEncodingList() // 仅视频需要填encodings
                        );
                        handler.post(() -> rtcConnector.sendOfferSdp(pcWrapper.connType, pc.getLocalDescription().description, rtcMedia));
                        pcWrapper.setSdpState(pcWrapper.Sending);
                    }else {
                        KLog.p("setRemoteDescription for Offer success, sdp progress FINISHED, drainCandidates");
                        pcWrapper.drainCandidates();
                        pcWrapper.setSdpState(pcWrapper.Idle);
                    }
                }
                else if (pcWrapper.isSdpType(pcWrapper.Answer)){
                    if (pcWrapper.isSdpState(pcWrapper.SettingRemote)){
                        KLog.p("setRemoteDescription for Answer success, create answer...");
                        pcWrapper.pc.createAnswer(pcWrapper.sdpObserver, new MediaConstraints());
                        pcWrapper.setSdpState(pcWrapper.Creating);
                    }else {
                        KLog.p("setLocalDescription for Answer success, sending answer...");
                        List<RtcConnector.TRtcMedia> rtcMediaList = new ArrayList<>();
                        List<String> mids = getAllMids(pc.getLocalDescription().description);
                        for (String mid : mids){
                            KLog.p("mid=%s", mid);
                            String streamId = midStreamIdMap.get(mid);
                            if (null == streamId){
                                KLog.p(KLog.ERROR, "no streamId for mid %s (see onSetOfferCmd)", mid);
                            }
                            rtcMediaList.add(new RtcConnector.TRtcMedia(streamId, mid)); // 仅answer需要填streamId，answer不需要填encodings
                        }
                        handler.post(() -> rtcConnector.sendAnswerSdp(pcWrapper.connType, pc.getLocalDescription().description, rtcMediaList));
                        KLog.p("answer sent, sdp progress FINISHED, drainCandidates");
                        pcWrapper.drainCandidates();
                        pcWrapper.setSdpState(pcWrapper.Idle);
                    }
                }
                else if (pcWrapper.isSdpType(pcWrapper.AudioOffer)){
                    if (pcWrapper.isSdpState(pcWrapper.SettingLocal)){
                        KLog.p("setLocalDescription for AudioOffer success, sending offer...");
                        RtcConnector.TRtcMedia rtcMedia = new RtcConnector.TRtcMedia(getMid(pc.getLocalDescription().description,true));
                        handler.post(() -> rtcConnector.sendOfferSdp(pcWrapper.connType, pc.getLocalDescription().description, rtcMedia));
                        pcWrapper.setSdpState(pcWrapper.Sending);
                    }else{
                        KLog.p("setRemoteDescription for AudioOffer success, we now need create video offer...");
                        VideoCapturer videoCapturer = createCameraCapturer(new Camera2Enumerator(context));
                        if (null != videoCapturer) {
                            pcWrapper.createVideoTrack(videoCapturer);
//                            pcWrapper.createVideoTrack2(videoCapturer);
                            pcWrapper.pc.createOffer(pcWrapper.sdpObserver, new MediaConstraints());
                        }
                        // 不同于正常sdp流程，此时还需要再发video的offer，所以切换sdptype为videoOffer
                        pcWrapper.setSdpType(pcWrapper.VideoOffer);
                        pcWrapper.setSdpState(pcWrapper.Creating);
                    }
                }
                else if (pcWrapper.isSdpType(pcWrapper.VideoOffer)){
                    if (pcWrapper.isSdpState(pcWrapper.SettingLocal)){
                        KLog.p("setLocalDescription for VideoOffer success, sending offer...");
                        RtcConnector.TRtcMedia rtcAudio = new RtcConnector.TRtcMedia(getMid(pc.getLocalDescription().description,true));
                        RtcConnector.TRtcMedia rtcVideo = new RtcConnector.TRtcMedia(
                                getMid(pc.getLocalDescription().description,false),
                                createEncodingList()
                        );
                        handler.post(() -> rtcConnector.sendOfferSdp(pcWrapper.connType, pc.getLocalDescription().description, rtcAudio, rtcVideo));
                        pcWrapper.setSdpState(pcWrapper.Sending);
                    }else{
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
            KLog.p(KLog.ERROR, "error: %s", error);
        }

        @Override
        public void onSetFailure(final String error) {
            KLog.p(KLog.ERROR, "error: %s", error);
        }

    }



    private class PCObserver implements PeerConnection.Observer{

        @Override
        public void onIceCandidate(final IceCandidate candidate) {
            KLog.p("onIceCandidate, sending candidate...");
            PeerConnectionWrapper pcWrapper = getPcWrapper(this);
            handler.post(() -> rtcConnector.sendIceCandidate(pcWrapper.connType, candidate.sdpMid, candidate.sdpMLineIndex, candidate.sdp));
        }

        @Override
        public void onIceCandidatesRemoved(final IceCandidate[] candidates) {
            KLog.p("onIceCandidatesRemoved");
        }

        @Override
        public void onAddStream(MediaStream mediaStream) {
        }

        @Override
        public void onSignalingChange(PeerConnection.SignalingState newState) {
        }

        @Override
        public void onIceConnectionChange(final PeerConnection.IceConnectionState newState) {
//                handler.post(() -> {
//                    if (newState == PeerConnection.IceConnectionState.CONNECTED) {
//                    } else if (newState == PeerConnection.IceConnectionState.DISCONNECTED) {
//                    } else if (newState == PeerConnection.IceConnectionState.FAILED) {
////                        reportError("ICE connection failed.");
//                    }
//                });

        }

        @Override
        public void onConnectionChange(final PeerConnection.PeerConnectionState newState) {

//                handler.post(() -> {
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
            KLog.p("stream %s removed", stream.getId());
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
            KLog.p("received remote track %s", track);
            PeerConnectionWrapper pcWrapper = getPcWrapper(this);
            if (track instanceof VideoTrack){
                pcWrapper.createRemoteVideoTrack(transceiver.getMid(), (VideoTrack) track);
            }else{
                pcWrapper.createRemoteAudioTrack(transceiver.getMid(), (AudioTrack) track);
            }

        }

    }



    private String getMid(String sdpDescription, boolean bAudio){
//        KLog.p("bAudio =%s, sdp=%s", bAudio, sdpDescription);
        final String[] lines = sdpDescription.split("\r\n");
        String mediaHead = bAudio ? "m=audio" : "m=video";
        boolean gotMediaHead = false;
        for (String line : lines) {
            if (line.startsWith(mediaHead)) {
                gotMediaHead = true;
                continue;
            }
            if (line.startsWith("a=mid:") && gotMediaHead) {
                return line.substring("a=mid:".length());
            }
        }

        KLog.p(KLog.WARN, "getMid null");
        return null;
    }

    private List<String> getAllMids(String sdpDescription){
//        KLog.p("sdp=%s", sdpDescription);
        final String[] lines = sdpDescription.split("\r\n");
        List<String> mids = new ArrayList<>();
        for (String line : lines) {
            if (line.startsWith("a=mid:")) {
                mids.add(line.substring("a=mid:".length()));
            }
        }
        return mids;
    }

    private String getFingerPrint(String sdpDescription){
//        KLog.p("sdp=%s", sdpDescription);
        final String[] lines = sdpDescription.split("\r\n");
        for (String line : lines) {
            if (line.startsWith("a=fingerprint:sha-256 ")) {
                return line.substring("a=fingerprint:sha-256 ".length());
            }
        }

        KLog.p(KLog.WARN, "getFingerPrint null");
        return null;
    }

    /**
     * 创建Encoding列表（具体内容由需求决定）
     * XXX：sdk有意屏蔽了创建Encoding的构造，此处使用了反射强行创建Encoding对象（很可能有问题）。
     * */
    private List<RtpParameters.Encoding> encodingList;
    private List<RtpParameters.Encoding> createEncodingList(){
        if (null != encodingList){
            return encodingList;
        }
        List<RtpParameters.Encoding> encodings = new ArrayList<>();
        try {
            Class<?> clz = Class.forName("org.webrtc.RtpParameters$Encoding");
            Constructor<?> ctor = clz.getDeclaredConstructor(String.class, boolean.class, Double.class);
            ctor.setAccessible(true);
            encodings.add((RtpParameters.Encoding) ctor.newInstance("h", true, 1.0));
            encodings.add((RtpParameters.Encoding) ctor.newInstance("m", true, 0.5));
            encodings.add((RtpParameters.Encoding) ctor.newInstance("l", true, 0.25));
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        encodingList = encodings;
        return encodings;
    }

    private RtpParameters.Encoding createEncoding(String rid, double scaleDwon){
        RtpParameters.Encoding encoding = null;
        try {
            Class<?> clz = Class.forName("org.webrtc.RtpParameters$Encoding");
            Constructor<?> ctor = clz.getDeclaredConstructor(String.class, boolean.class, Double.class);
            ctor.setAccessible(true);
            encoding = (RtpParameters.Encoding) ctor.newInstance(rid, true, scaleDwon);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return encoding;
    }






    /**
     * PeerConnection包装类
     * */
    private class PeerConnectionWrapper{

        final String STREAM_ID = "TT-Android-"+System.currentTimeMillis();
        final String LOCAL_VIDEO_ID = STREAM_ID+"-v0";
        final String LOCAL_AUDIO_ID = STREAM_ID+"-a0";

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
        PCObserver pcObserver;
        List<IceCandidate> queuedRemoteCandidates = new ArrayList<>();
        SurfaceTextureHelper surfaceTextureHelper;
        VideoCapturer videoCapturer;
        VideoSource videoSource;
        VideoTrack localVideoTrack;
        List<VideoTrack> remoteVideoTracks = new ArrayList<>();
        AudioSource audioSource;
        AudioTrack localAudioTrack;
        List<AudioTrack> remoteAudioTracks = new ArrayList<>();

        PeerConnectionWrapper(int connType, @NonNull PeerConnection pc, @NonNull PeerConnectionConfig config,
                              @NonNull SDPObserver sdpObserver, @NonNull PCObserver pcObserver) {
            this.connType = connType;
            this.pc = pc;
            this.config = config;
            this.sdpObserver = sdpObserver;
            this.pcObserver = pcObserver;
//            if (CommonDef.CONN_TYPE_PUBLISHER == connType
//                    || CommonDef.CONN_TYPE_ASS_PUBLISHER == connType){
//                RtpTransceiver.RtpTransceiverInit transceiverInit = new RtpTransceiver.RtpTransceiverInit(
//                        RtpTransceiver.RtpTransceiverDirection.SEND_ONLY,
//                        Collections.singletonList(STREAM_ID),
//                        createEncodingList()
//                );
//                pc.addTransceiver(MediaStreamTrack.MediaType.MEDIA_TYPE_VIDEO, transceiverInit);
//            }
        }


        void createVideoTrack(@NonNull VideoCapturer videoCapturer){
            if (null != localVideoTrack){
                KLog.p(KLog.ERROR, "localVideoTrack has created");
                return;
            }
            this.videoCapturer = videoCapturer;
            surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", eglBase.getEglBaseContext());
            videoSource = factory.createVideoSource(videoCapturer.isScreencast());
            videoCapturer.initialize(surfaceTextureHelper, context, videoSource.getCapturerObserver());
            videoCapturer.startCapture(config.videoWidth, config.videoHeight, config.videoFps);
            localVideoTrack = factory.createVideoTrack(LOCAL_VIDEO_ID, videoSource);
            localVideoTrack.setEnabled(config.isLocalVideoEnabled);
            pc.addTrack(localVideoTrack, Collections.singletonList(STREAM_ID));
            String localTrackId = LOCAL_VIDEO_ID;
            ProxyVideoSink localVideoSink = new ProxyVideoSink(localTrackId);
            videoSinks.put(localTrackId, localVideoSink);
            localVideoTrack.addSink(localVideoSink);
            handler.post(() -> {
                if (null != sessionEventListener) {
                    StreamInfo streamInfo = new StreamInfo(localTrackId, videoCapturer.isScreencast() ? StreamInfo.Type_LocalScreenShare : StreamInfo.Type_LocalCamera);
                    KLog.p("####onLocalStream stream info=%s", streamInfo);
                    localStreamInfos.add(streamInfo);
                    sessionEventListener.onStream(streamInfo);
                }
            });
        }

//        void createVideoTrack2(@NonNull VideoCapturer videoCapturer){
//            if (null != localVideoTrack){
//                KLog.p(KLog.ERROR, "localVideoTrack has created");
//                return;
//            }
//            this.videoCapturer = videoCapturer;
//            surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", eglBase.getEglBaseContext());
//            videoSource = factory.createVideoSource(videoCapturer.isScreencast());
//            videoCapturer.initialize(surfaceTextureHelper, context, videoSource.getCapturerObserver());
//            videoCapturer.startCapture(config.videoWidth, config.videoHeight, config.videoFps);
//            localVideoTrack = factory.createVideoTrack(LOCAL_VIDEO_ID, videoSource);
//            localVideoTrack.setEnabled(config.videoEnabled);
////            pc.addTrack(localVideoTrack, Collections.singletonList(STREAM_ID));
//            String localTrackId = LOCAL_VIDEO_ID;
//            ProxyVideoSink localVideoSink = new ProxyVideoSink(localTrackId);
//            videoSinks.put(localTrackId, localVideoSink); // XXX 保证trackId 不能和远端流的id冲突
//            localVideoTrack.addSink(localVideoSink);
//            RtpTransceiver.RtpTransceiverInit transceiverInit = new RtpTransceiver.RtpTransceiverInit(
//                            RtpTransceiver.RtpTransceiverDirection.SEND_ONLY,
//                            Collections.singletonList(STREAM_ID),
//                            createEncodingList()
//            );
//            RtpTransceiver transceiver = pc.addTransceiver(MediaStreamTrack.MediaType.MEDIA_TYPE_VIDEO, transceiverInit);
//            transceiver.getSender().setTrack(localVideoTrack, false);
//            handler.post(() -> {
//                SurfaceViewRenderer surfaceViewRenderer = new SurfaceViewRenderer(context);
//                surfaceViewRenderer.init(eglBase.getEglBaseContext(), null);
//                surfaceViewRenderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
//                surfaceViewRenderer.setEnableHardwareScaler(true);
//                localVideoSink.addTarget(surfaceViewRenderer);
//                if (null != sessionEventListener) {
//                    KLog.p("####onLocalStream localTrackId=%s, render=%s", localTrackId, surfaceViewRenderer);
//                    sessionEventListener.onLocalStream(localTrackId, surfaceViewRenderer);
//                }
//            });
//        }


        void createRemoteVideoTrack(String mid, VideoTrack track){
            String streamId = midStreamIdMap.get(mid);
            KLog.p("mid=%s, streamId=%s", mid, streamId);
            if (null == streamId){
                KLog.p(KLog.ERROR, "no register stream for mid %s in signaling progress(see onSetOfferCmd)", mid);
                return;
            }
            remoteVideoTracks.add(track);
            track.setEnabled(config.isRemoteVideoEnabled);
            ProxyVideoSink videoSink = new ProxyVideoSink(streamId);
            videoSinks.put(streamId, videoSink);
            track.addSink(videoSink);

            TRtcStreamInfo rtcStreamInfo = null;
            for (TRtcStreamInfo streamInfo : streamInfos){
                if (streamId.equals(streamInfo.achStreamId)){
                    rtcStreamInfo = streamInfo;
                    break;
                }
            }
            if (null == rtcStreamInfo){
                KLog.p(KLog.ERROR, "no such stream %s in stream list( see Msg.StreamLeft/Msg.StreamJoined/Msg.StreamListReady branch in method onNtf)", streamId);
                return;
            }

            if (null != sessionEventListener) {
                TRtcStreamInfo finalRtcStreamInfo = rtcStreamInfo;
                handler.post(() -> {
                    StreamInfo streamInfo = ToDoConverter.rtcStreamInfo2StreamInfo(finalRtcStreamInfo);
                    KLog.p("####onRemoteStream, streamInfo=%s", streamInfo);
                    sessionEventListener.onStream(streamInfo);
//                    // FORDEBUG 仅调试
//                    sessionEventListener.onRemoteStream(new StreamInfo(0, 0, streamId), surfaceViewRenderer);
                });
            }

        }


        private RtpSender audioSender;
        void createAudioTrack(){
            audioSource = factory.createAudioSource(new MediaConstraints());
            localAudioTrack = factory.createAudioTrack(LOCAL_AUDIO_ID, audioSource);
            localAudioTrack.setEnabled(config.isLocalAudioEnabled);
            audioSender = pc.addTrack(localAudioTrack, Collections.singletonList(STREAM_ID));
        }


        void createRemoteAudioTrack(String mid, AudioTrack track){
            track.setEnabled(config.isRemoteAudioEnabled);
            remoteAudioTracks.add(track);
        }

        void destoryAudioTrack(){
            localAudioTrack.setEnabled(false);
            if (audioSource != null) {
                audioSource.dispose();
                audioSource = null;
            }
            localAudioTrack = null;
            pc.removeTrack(audioSender);
            audioSender = null;
        }


        void setSdpType(int sdpType) {
            KLog.up("set sdp type from %s to %s", this.sdpType, sdpType);
            this.sdpType = sdpType;
        }

        boolean isSdpType(int sdpType){
            return sdpType == this.sdpType;
        }

        void setSdpState(int sdpState) {
            KLog.up("switch sdp state from %s to %s", this.sdpState, sdpState);
            this.sdpState = sdpState;
        }

        boolean isSdpState(int sdpState){
            return sdpState == this.sdpState;
        }

        void checkSdpState(int... sdpStates) {
            for (int state : sdpStates){
                if (state == this.sdpState){
                    return;
                }
            }
            throw new RuntimeException("invalid sdp sate, expect "+sdpStates+" but current is "+this.sdpState);
        }



        void drainCandidates() {
            if (queuedRemoteCandidates != null) {
                for (IceCandidate candidate : queuedRemoteCandidates) {
                    pc.addIceCandidate(candidate);
                }
                queuedRemoteCandidates = null;
            }
        }

        void addCandidate(IceCandidate candidate) {
            if (queuedRemoteCandidates != null) {
                queuedRemoteCandidates.add(candidate);
            }else {
                pc.addIceCandidate(candidate);
            }
        }

        void close(){
            if (pc != null) {
                pc.dispose();
                pc = null;
            }
            if (audioSource != null) {
                audioSource.dispose();
                audioSource = null;
            }
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
            if (surfaceTextureHelper != null) {
                surfaceTextureHelper.dispose();
                surfaceTextureHelper = null;
            }
        }


        boolean isRemoteAudioEnabled(){
            return config.isRemoteAudioEnabled;
        }

        void setRemoteAudioEnable(boolean bEnable){
            config.isRemoteAudioEnabled = bEnable;
            executor.execute(() -> {
                for (AudioTrack audioTrack : remoteAudioTracks) {
                    audioTrack.setEnabled(bEnable);
                }
            });
        }

        boolean isLocalAudioEnabled(){
            return config.isLocalAudioEnabled;
        }

        void setLocalAudioEnable(boolean bEnable){
            config.isLocalAudioEnabled = bEnable;
            if (localAudioTrack != null) {
                executor.execute(() -> localAudioTrack.setEnabled(bEnable));
            }
        }


        boolean isRemoteVideoEnabled(){
            return config.isRemoteVideoEnabled;
        }

        void setRemoteVideoEnable(boolean bEnable){
            config.isRemoteVideoEnabled = bEnable;
            executor.execute(() -> {
                for (VideoTrack videoTrack : remoteVideoTracks) {
                    videoTrack.setEnabled(bEnable);
                }
            });
        }

        boolean isLocalVideoEnabled(){
            return config.isLocalVideoEnabled;
        }

        void setLocalVideoEnable(boolean bEnable){
            config.isLocalVideoEnabled = bEnable;
            if (localVideoTrack != null) {
                executor.execute(() -> localVideoTrack.setEnabled(bEnable));
            }
        }

        boolean isPreferFrontCamera(){
            return config.bPreferFrontCamera;
        }

        void switchCamera(){
            config.bPreferFrontCamera = !config.bPreferFrontCamera;
            if (null != videoCapturer){
                executor.execute(() -> ((CameraVideoCapturer)videoCapturer).switchCamera(null));
            }
        }

    }





}
