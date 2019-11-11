package com.kedacom.vconf.sdk.webrtc;

import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjection;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.kedacom.kdv.mt.ospconnector.Connector;
import com.kedacom.mt.netmanage.protobuf.BasePB;
import com.kedacom.osp.EmMtOspMsgSys;
import com.kedacom.osp.MtMsg;
import com.kedacom.vconf.sdk.amulet.Caster;
import com.kedacom.vconf.sdk.amulet.IResultListener;
import com.kedacom.vconf.sdk.common.constant.EmConfProtocol;
import com.kedacom.vconf.sdk.common.type.BaseTypeInt;
import com.kedacom.vconf.sdk.common.type.vconf.TMtCallLinkSate;
import com.kedacom.vconf.sdk.utils.log.KLog;
import com.kedacom.vconf.sdk.utils.net.NetAddrHelper;
import com.kedacom.vconf.sdk.webrtc.bean.StreamInfo;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SuppressWarnings("SwitchStatementWithTooFewBranches")
public class WebRtcManager extends Caster<Msg>{

    private RtcConnector rtcConnector;

    private Context context;
    private Map<String, ProxyVideoSink> videoSinks = new HashMap<>();

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private EglBase eglBase;
    private PeerConnectionFactory factory;
    private PeerConnectionWrapper pubConnWrapper;
    private PeerConnectionWrapper subConnWrapper;
    private PeerConnectionWrapper assPubConnWrapper;
    private PeerConnectionWrapper assSubConnWrapper;
    private List<TRtcStreamInfo> streamInfos = new ArrayList<>();
    private BiMap<String, String> midStreamIdMap = HashBiMap.create();

    private Handler handler = new Handler(Looper.getMainLooper());

    private static WebRtcManager instance;

    private WebRtcManager(){
    }

    public synchronized static WebRtcManager getInstance(){
        if (null == instance){
            return instance = new WebRtcManager();
        }
        return instance;
    }


    @Override
    protected Map<Msg[], RspProcessor<Msg>> rspsProcessors() {
        Map<Msg[], RspProcessor<Msg>> processorMap = new HashMap<>();
        processorMap.put(new Msg[]{
                Msg.Login,
                Msg.Call
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
                Msg.CallIncoming
        }, this::onNtfs);

        return processorMap;
    }


    /**
     * 登录rtc
     * 注意，需先登录aps成功。
     * */
    public void login(String e164, IResultListener resultListener){
        TMtRtcSvrAddr rtcSvrAddr = (TMtRtcSvrAddr) get(Msg.GetSvrAddr);
//        if (null == rtcSvrAddr || rtcSvrAddr.dwIp<= 0){
//            KLog.p(KLog.ERROR, "invalid rtcSvrAddr, have you logined APS");
//            reportFailed(-1, resultListener);
//            return;
//        }

//        if (null == rtcSvrAddr){
            KLog.p(KLog.ERROR, "null == rtcSvrAddr");
            try {
                long ip = NetAddrHelper.ipStr2LongLittleEndian("172.16.179.114"); //FIXME 写死方便调试
//                rtcSvrAddr = new TMtRtcSvrAddr(ip, 7961,"0512110000004");
                rtcSvrAddr = new TMtRtcSvrAddr(ip, 7961, e164);
            } catch (NetAddrHelper.InvalidIpv4Exception e) {
                e.printStackTrace();
            }
//        }

        req(Msg.Login, resultListener, rtcSvrAddr);
    }


    /**
     * 呼叫
     * @param peerId 对于点对点而言是对端e164，对于多方会议而言是会议e164
     * */
    public void makeCall(String peerId, IResultListener resultListener){
        req(Msg.Call, resultListener, peerId, 1024, EmConfProtocol.emrtc.ordinal());
    }


    private boolean onRsp(Msg rsp, Object rspContent, IResultListener listener, Msg req, Object[] reqParas) {
        switch (rsp){
            case LoginRsp:
                TLoginResult loginResult = (TLoginResult) rspContent;
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
                reportSuccess(null, listener);
                break;

            case P2pConfEnded:
                BaseTypeInt reason = (BaseTypeInt) rspContent;
                KLog.p("P2pConfEnded: %s", reason.basetype);
                reportFailed(-1, listener);
                break;

            case MultipartyConfStarted:
                callLinkSate = (TMtCallLinkSate) rspContent;
                KLog.p("MultipartyConfStarted: %s", callLinkSate);
                reportSuccess(null, listener);
                break;

            case MultipartyConfEnded:
                reason = (BaseTypeInt) rspContent;
                KLog.p("MultipartyConfEnded: %s", reason.basetype);
                reportFailed(-1, listener);
                break;

            default:
                return false;
        }

        return true;
    }


    private void onNtfs(Msg ntfId, Object ntfContent, Set<Object> listeners) {
        switch (ntfId){

            case CallIncoming:
                TMtCallLinkSate callLinkSate = (TMtCallLinkSate) ntfContent;
                KLog.p("CallIncoming: %s", callLinkSate);
                break;

            case P2pConfEnded:
                BaseTypeInt reason = (BaseTypeInt) ntfContent;
                KLog.p("P2pConfEnded: %s", reason.basetype);
                break;

            case MultipartyConfEnded:
                reason = (BaseTypeInt) ntfContent;
                KLog.p("MultipartyConfEnded: %s", reason.basetype);
                break;

            case StreamListReady:
                List<TRtcPlayItem> rtcPlayItems = new ArrayList<>();
                TRtcStreamInfoList streamInfoList = (TRtcStreamInfoList) ntfContent;
                streamInfos.clear();
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
                set(Msg.SetPlayPara, new TRtcPlayParam(rtcPlayItems));
                break;
            case StreamJoined:  //NOTE: 这里是增量过来的
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







    public synchronized boolean startSession(@NonNull Context ctx, @NonNull SessionEventListener listener){
        if (null != factory){
            KLog.p(KLog.ERROR, "session has started already!");
            return false;
        }

        context = ctx;
        sessionEventListener = listener;

        rtcConnector = new RtcConnector();
        rtcConnector.setSignalingEventsCallback(signalingEvents);

        int videoWidth = context.getResources().getDisplayMetrics().widthPixels;
        int videoHeight = context.getResources().getDisplayMetrics().heightPixels;
        PeerConnectionConfig pubConnConfig = new PeerConnectionConfig(
                        true,
                        true,
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
            pubConnWrapper = createPeerConnectionWrapper(CommonDef.CONN_TYPE_PUBLISHER, pubConnConfig);
            subConnWrapper = createPeerConnectionWrapper(CommonDef.CONN_TYPE_SUBSCRIBER, new PeerConnectionConfig(pubConnConfig));
            assPubConnWrapper = createPeerConnectionWrapper(CommonDef.CONN_TYPE_ASS_PUBLISHER, new PeerConnectionConfig(pubConnConfig));
            assSubConnWrapper = createPeerConnectionWrapper(CommonDef.CONN_TYPE_ASS_SUBSCRIBER, new PeerConnectionConfig(pubConnConfig));
        });

        KLog.p("session started ");

        return true;
    }


    public synchronized void stopSession(){
        if (null == factory){
            KLog.p(KLog.ERROR, "session has stopped already!");
            return;
        }

        context = null;
        sessionEventListener = null;

        if (null != rtcConnector){
            rtcConnector.destroy();
            rtcConnector = null;
        }

        if (pubConnWrapper != null) {
            pubConnWrapper.close();
            pubConnWrapper = null;
        }
        if (subConnWrapper != null) {
            subConnWrapper.close();
            subConnWrapper = null;
        }
        if (assPubConnWrapper != null) {
            assPubConnWrapper.close();
            assPubConnWrapper = null;
        }
        if (assSubConnWrapper != null) {
            assSubConnWrapper.close();
            assSubConnWrapper = null;
        }

        if (factory != null) {
            factory.dispose();
            factory = null;
        }

        if (null != eglBase) {
            eglBase.release();
            eglBase = null;
        }

        for (ProxyVideoSink videoSink : videoSinks.values()){
            if (null != videoSink.target){
                ((SurfaceViewRenderer)videoSink.target).release();
            }
        }
        videoSinks.clear();

        midStreamIdMap.clear();
        streamInfos.clear();
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

        PeerConnection peerConnection = factory.createPeerConnection(rtcConfig, new PCObserver(connType));

        return new PeerConnectionWrapper(connType, peerConnection, config, new SDPObserver(connType));
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
        private VideoSink target;
        private long timestamp = System.currentTimeMillis();

        ProxyVideoSink(String name) {
            this.name = name;
        }

        ProxyVideoSink(String name, VideoSink target) {
            this.name = name;
            this.target = target;
        }

        @Override
        synchronized public void onFrame(VideoFrame frame) {
            long curts = System.currentTimeMillis();
            if (curts - timestamp > 5000){
                timestamp = curts;
                KLog.p("%s onFrame, target %s", name, target);
                if (target == null) {
                    KLog.p("Dropping frame in proxy because target is null.");
                    return;
                }
            }

            if (target == null) {
                return;
            }

            target.onFrame(frame);
        }

        synchronized void setTarget(VideoSink target) {
            this.target = target;
        }

    }


    /**
     * 交换流。
     * 若原本stream1展示在view1上，stream2展示在view2上，
     * 交换后stream1展示在view2上，stream2展示在view1上。
     * */
    public boolean swapStream(String streamId1, String streamId2){
        ProxyVideoSink srcSink = videoSinks.get(streamId1);
        ProxyVideoSink dstSink = videoSinks.get(streamId2);
        if (null == srcSink){
            KLog.p(KLog.ERROR, "null == srcSink");
            return false;
        }
        if (null == dstSink){
            KLog.p(KLog.ERROR, "null == dstSink");
            return false;
        }
        VideoSink srcTarget = srcSink.target;
        VideoSink dstTarget = dstSink.target;
        srcSink.setTarget(dstTarget);
        dstSink.setTarget(srcTarget);

        return true;
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
            KLog.p(KLog.ERROR, "API level < LOLLIPOP(21)");
        }
        return videoCapturer;
    }

    private Intent screenCapturePermissionData;
    /**
     * 设置截屏权限。（发送辅流需要截屏）
     * */
    public void setScreenCapturePermission(Intent permissionData){
        screenCapturePermissionData = permissionData;
    }


    private PeerConnectionWrapper getPeerConnectionWrapper(int type){
        KLog.p("type=%s", type);
        if (CommonDef.CONN_TYPE_PUBLISHER == type){
            return pubConnWrapper;
        }else if (CommonDef.CONN_TYPE_SUBSCRIBER == type){
            return subConnWrapper;
        }else if (CommonDef.CONN_TYPE_ASS_PUBLISHER == type){
            return assPubConnWrapper;
        }else if (CommonDef.CONN_TYPE_ASS_SUBSCRIBER == type){
            return assSubConnWrapper;
        }else{
            return pubConnWrapper;
        }
    }



    // FORDEBUG
    public boolean startSignaling()
    {
        MtMsg msg = new MtMsg();
        msg.SetMsgId("Ev_MT_GetOffer_Cmd");
        msg.addMsg(BasePB.TU32.newBuilder().setValue(CommonDef.CONN_TYPE_ASS_PUBLISHER).build());
        msg.addMsg(BasePB.TU32.newBuilder().setValue(CommonDef.MEDIA_TYPE_AV).build());

        long nSrcID = Connector.MAKEIID(RtcConnector.WEBRTC_ID, (short)1 );
        long nSrcNodeId=0;
        long nDstID = nSrcID;
        long nDstNodeId=nSrcNodeId;

        byte[] abyContent = msg.Encode();
        int nRet = Connector.PostOspMsg( EmMtOspMsgSys.Ev_MtOsp_ProtoBufMsg.getnVal(), abyContent, abyContent.length,
                nDstID, nDstNodeId, nSrcID, nSrcNodeId, 5000 );
        if (nRet != 0){
            KLog.p(KLog.ERROR, "post msg %s failed, ret=%s", msg.GetMsgId(), nRet);
            return false;
        }

        return true;
    }


    /**
     * 事件监听器
     * */
    public interface SessionEventListener {
        /**
         * 本地流到达
         * @param streamId 本地流Id。
         * @param display 流默认的渲染目标 */
        void onLocalStream(String streamId, View display);
        /**
         * 远端流到达
         * @param stream 流信息
         * @param display 流默认的渲染目标
         * */
        void onRemoteStream(StreamInfo stream, View display);
        void onRemoteStreamRemoved(StreamInfo stream);
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
        boolean videoEnabled;
        boolean audioEnabled;
        int videoWidth;
        int videoHeight;
        int videoFps;
        int videoMaxBitrate;
        String videoCodec;
        int audioStartBitrate;
        String audioCodec;

        PeerConnectionConfig(boolean videoEnabled,
                                    boolean audioEnabled,
                                    int videoWidth,
                                    int videoHeight,
                                    int videoFps,
                                    int videoMaxBitrate,
                                    String videoCodec,
                                    int audioStartBitrate,
                                    String audioCodec) {
            this.videoEnabled = videoEnabled;
            this.audioEnabled = audioEnabled;
            this.videoWidth = videoWidth;
            this.videoHeight = videoHeight;
            this.videoFps = videoFps;
            this.videoMaxBitrate = videoMaxBitrate;
            this.videoCodec = videoCodec;
            this.audioStartBitrate = audioStartBitrate;
            this.audioCodec = audioCodec;
        }

        PeerConnectionConfig(PeerConnectionConfig config) {
            this.videoEnabled = config.videoEnabled;
            this.audioEnabled = config.audioEnabled;
            this.videoWidth = config.videoWidth;
            this.videoHeight = config.videoHeight;
            this.videoFps = config.videoFps;
            this.videoMaxBitrate = config.videoMaxBitrate;
            this.videoCodec = config.videoCodec;
            this.audioStartBitrate = config.audioStartBitrate;
            this.audioCodec = config.audioCodec;
        }
    }





    private RtcConnector.SignalingEvents signalingEvents = new RtcConnector.SignalingEvents() {

        @Override
        public void onGetOfferCmd(int connType, int mediaType) {
            KLog.p("connType=%s, mediaType=%s", connType, mediaType);
            PeerConnectionWrapper pcWrapper = getPeerConnectionWrapper(connType);
            VideoCapturer videoCapturer = null;
            if ((CommonDef.MEDIA_TYPE_VIDEO == mediaType || CommonDef.MEDIA_TYPE_AV == mediaType)
                    && pcWrapper.config.videoEnabled){
                if (CommonDef.CONN_TYPE_PUBLISHER == connType) {
                    videoCapturer = createCameraCapturer(new Camera2Enumerator(context));
                }else if (CommonDef.CONN_TYPE_ASS_PUBLISHER == connType) {
                    videoCapturer = createScreenCapturer();
                }
            }

            VideoCapturer finalVideoCapturer = videoCapturer;
            executor.execute(()->{
                if (pcWrapper.config.videoEnabled && null != finalVideoCapturer) {
                    pcWrapper.createVideoTrack(finalVideoCapturer);
                }
                if (pcWrapper.config.audioEnabled) {
                    pcWrapper.createAudioTrack();
                }

                pcWrapper.pc.createOffer(pcWrapper.sdpObserver, new MediaConstraints());
            });

        }

        @Override
        public void onSetOfferCmd(int connType, String offerSdp, RtcConnector.TRtcMedia rtcMedia) {
            KLog.p("connType=%s, rtcMedia.mid=%s, rtcMedia.streamid=%s, offerSdp=%s",
                    connType, rtcMedia.mid, rtcMedia.streamid, offerSdp);
            PeerConnectionWrapper pcWrapper = getPeerConnectionWrapper(connType);
            PeerConnection pc = pcWrapper.pc;
            PeerConnectionConfig config = pcWrapper.config;
            SDPObserver observer = pcWrapper.sdpObserver;
            midStreamIdMap.put(rtcMedia.mid, rtcMedia.streamid);

            executor.execute(()-> {
                pc.setRemoteDescription(observer, new SessionDescription(SessionDescription.Type.OFFER, offerSdp));
                pc.createAnswer(observer, new MediaConstraints());
            });
        }

        @Override
        public void onSetAnswerCmd(int connType, String answerSdp) {
            KLog.p("connType=%s, answerSdp=%s", connType, answerSdp);
            PeerConnectionWrapper pcWrapper = getPeerConnectionWrapper(connType);
            PeerConnection pc = pcWrapper.pc;
            PeerConnectionConfig config = pcWrapper.config;
            SDPObserver observer = pcWrapper.sdpObserver;
            executor.execute(()-> pc.setRemoteDescription(observer, new SessionDescription(SessionDescription.Type.ANSWER, answerSdp)));
        }

        @Override
        public void onSetIceCandidateCmd(int connType, String sdpMid, int sdpMLineIndex, String sdp) {
            KLog.p("connType=%s, sdpMid=%s, sdpMLineIndex=%s, sdp=%s", connType, sdpMid, sdpMLineIndex, sdp);
            PeerConnectionWrapper pcWrapper = getPeerConnectionWrapper(connType);
            executor.execute(()-> pcWrapper.addCandidate(new IceCandidate(sdpMid, sdpMLineIndex, sdp)));
        }

    };




    private class SDPObserver implements SdpObserver {
        private int connType;

        SDPObserver(int connType) {
            this.connType = connType;
        }

        boolean isOffer(){
            return connType == CommonDef.CONN_TYPE_PUBLISHER || connType == CommonDef.CONN_TYPE_ASS_PUBLISHER;
        }

        @Override
        public void onCreateSuccess(final SessionDescription origSdp) {
            KLog.p("create local sdp success: %s", origSdp);
            PeerConnectionWrapper peerConnectionWrapper = getPeerConnectionWrapper(connType);
            executor.execute(() -> peerConnectionWrapper.pc.setLocalDescription(this, origSdp));
        }

        @Override
        public void onSetSuccess() {
            PeerConnectionWrapper pcWrapper = getPeerConnectionWrapper(connType);
            executor.execute(() -> {
                PeerConnection pc = pcWrapper.pc;
                if (isOffer()) {
                    if (pcWrapper.isSdpProgressFinished()){
                        KLog.p("setRemoteDescription success, sdp progress finished, drainCandidates");
                        pcWrapper.drainCandidates();
                    }else{
//                        updateRtpPara(true);
                        KLog.p("setLocalDescription success, sending offer...");
                        RtcConnector.TRtcMedia rtcMedia = new RtcConnector.TRtcMedia(pcWrapper.STREAM_ID, getMid(pc.getLocalDescription().description), createEncodingList());
                        handler.post(() -> rtcConnector.sendOfferSdp(connType, pc.getLocalDescription().description, rtcMedia));
                    }

                } else {

                    if (pcWrapper.isSdpProgressFinished()){
                        KLog.p("setLocalDescription success, sending answer...,");
                        handler.post(() -> rtcConnector.sendAnswerSdp(connType, pc.getLocalDescription().description));
                        KLog.p("answer sent, sdp progress finished, drainCandidates");
                        pcWrapper.drainCandidates();
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
        private int connType;

        PCObserver(int connType) {
            this.connType = connType;
        }

        @Override
        public void onIceCandidate(final IceCandidate candidate) {
            KLog.p("onIceCandidate, sending candidate... %s", candidate.sdp);
            handler.post(() -> rtcConnector.sendIceCandidate(connType, candidate.sdpMid, candidate.sdpMLineIndex, candidate.sdp));
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
            KLog.p("onTrack: %s", transceiver);
            MediaStreamTrack track = transceiver.getReceiver().track();
            if (!(track instanceof VideoTrack)){
                return;
            }

            PeerConnectionWrapper pcWrapper = getPeerConnectionWrapper(connType);
            pcWrapper.createRemoteVideoTrack(transceiver.getMid(), (VideoTrack) track);

        }

    }


    private class PeerConnectionWrapper{

        final String STREAM_ID = "TT-Android-"+System.currentTimeMillis();
        final String VIDEO_TRACK_ID = STREAM_ID+"-v0";
        final String AUDIO_TRACK_ID = STREAM_ID+"-a0";

        int connType;
        PeerConnection pc;
        PeerConnectionConfig config;
        SDPObserver sdpObserver;
        List<IceCandidate> queuedRemoteCandidates = new ArrayList<>();
        SurfaceTextureHelper surfaceTextureHelper;
        VideoCapturer videoCapturer;
        VideoSource videoSource;
        VideoTrack localVideoTrack;
        List<VideoTrack> remoteVideoTracks = new ArrayList<>();
        AudioSource audioSource;
        AudioTrack localAudioTrack;

        PeerConnectionWrapper(int connType, PeerConnection pc, PeerConnectionConfig config, SDPObserver sdpObserver) {
            this.connType = connType;
            this.pc = pc;
            this.config = config;
            this.sdpObserver = sdpObserver;
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
            localVideoTrack = factory.createVideoTrack(VIDEO_TRACK_ID, videoSource);
            localVideoTrack.setEnabled(config.videoEnabled);
            pc.addTrack(localVideoTrack, Collections.singletonList(STREAM_ID));
            String localTrackId = VIDEO_TRACK_ID;
            ProxyVideoSink localVideoSink = new ProxyVideoSink(localTrackId);
            videoSinks.put(localTrackId, localVideoSink); // XXX 保证trackId 不能和远端流的id冲突
            localVideoTrack.addSink(localVideoSink);
            handler.post(() -> {
                SurfaceViewRenderer surfaceViewRenderer = new SurfaceViewRenderer(context);
                surfaceViewRenderer.init(eglBase.getEglBaseContext(), null);
                surfaceViewRenderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
                surfaceViewRenderer.setEnableHardwareScaler(true);
                localVideoSink.setTarget(surfaceViewRenderer);
                if (null != sessionEventListener) {
                    sessionEventListener.onLocalStream(localTrackId, surfaceViewRenderer);
                }
            });
        }


        void createRemoteVideoTrack(String mid, VideoTrack track){
            remoteVideoTracks.add(track);
            track.setEnabled(config.videoEnabled);
            ProxyVideoSink videoSink = new ProxyVideoSink(mid);
            videoSinks.put(mid, videoSink);
            track.addSink(videoSink);

            handler.post(() -> {
                String streamId = midStreamIdMap.get(mid);
                KLog.p("mid=%s, streamId=%s", mid, streamId);
                if (null == streamId){
                    KLog.p(KLog.ERROR, "no register stream for mid %s in signaling progress", mid);
                    return;
                }

                // SEALED TODO 暂时无法联调
//                    TRtcStreamInfo rtcStreamInfo = null;
//                    for (TRtcStreamInfo streamInfo : streamInfos){
//                        if (streamId.equals(streamInfo.achStreamId)){
//                            rtcStreamInfo = streamInfo;
//                            break;
//                        }
//                    }
//                    if (null == rtcStreamInfo){
//                        KLog.p(KLog.ERROR, "no such stream %s in stream list", streamId);
//                        return;
//                    }

                SurfaceViewRenderer surfaceViewRenderer = new SurfaceViewRenderer(context);
                surfaceViewRenderer.init(eglBase.getEglBaseContext(), null);
                surfaceViewRenderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
                surfaceViewRenderer.setEnableHardwareScaler(true);
                videoSink.setTarget(surfaceViewRenderer);

                if (null != sessionEventListener) {
//                    sessionEventListener.onRemoteStream(new StreamInfo(rtcStreamInfo.tMtId.dwMcuId, rtcStreamInfo.tMtId.dwTerId, streamId), surfaceViewRenderer);
                    // XXX 仅调试，用上面行
                    sessionEventListener.onRemoteStream(new StreamInfo(0, 0, streamId), surfaceViewRenderer);
                }

            });

        }


        void createAudioTrack(){
            audioSource = factory.createAudioSource(new MediaConstraints());
            localAudioTrack = factory.createAudioTrack(AUDIO_TRACK_ID, audioSource);
            localAudioTrack.setEnabled(config.audioEnabled);
            pc.addTrack(localAudioTrack, Collections.singletonList(STREAM_ID));
        }



        boolean isOffer(){
            return connType == CommonDef.CONN_TYPE_PUBLISHER || connType == CommonDef.CONN_TYPE_ASS_PUBLISHER;
        }

        /**
         * sdp协商阶段是否已完成。
         * （设置candidate需要等sdp协商完成）
         * */
        boolean isSdpProgressFinished(){
            return (isOffer() && pc.getRemoteDescription() != null)
                    || (!isOffer() && pc.getLocalDescription() != null);
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
    }


    private String getMid(String sdpDescription){
        final String[] lines = sdpDescription.split("\r\n");
        boolean isVideoSeg = false;
        for (String line : lines) {
            if (line.startsWith("m=video")) {
                isVideoSeg = true;
                continue;
            } else if (line.startsWith("m=audio")) {
                isVideoSeg = false;
                continue;
            }

            if (line.startsWith("a=mid:") && isVideoSeg) {
                return line.substring("a=mid:".length());
            }
        }

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

    static RtpParameters.Encoding createEncoding(String rid, double scaleDwon){
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



}
