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

import com.kedacom.vconf.sdk.webrtc.bean.*;

@SuppressWarnings("SwitchStatementWithTooFewBranches")
public class WebRtcManager extends Caster<Msg>{

    private RtcConnector rtcConnector = new RtcConnector();

    private Context context;
    private Map<String, ProxyVideoSink> videoSinks = new HashMap<>();

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private EglBase eglBase;
    private PeerConnectionFactory factory;
    private List<PeerConnectionWrapper> connWrapperList = new ArrayList<>();
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
     * @param e164
     * @param resultListener 结果监听器。
     *          成功: null;
     *          失败：错误码 TODO
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
     **@param resultListener 结果监听器。
     *          成功: {@link MakeCallResult};
     *          失败：TODO
     * */
    public void makeCall(String peerId, IResultListener resultListener){
        req(Msg.Call, resultListener, peerId, 1024, EmConfProtocol.emrtc.ordinal());
    }

    /**
     * 创建会议
     * @param peerId 对于点对点而言是对端e164，对于多方会议而言是会议e164
     **@param resultListener 结果监听器。
     *          成功: {@link MakeCallResult};
     *          失败：TODO
     * */
    public void createConf(String peerId, IResultListener resultListener){
        req(Msg.CreateConf, resultListener, peerId, 1024, EmConfProtocol.emrtc.ordinal());
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
                reportSuccess(ToDoConverter.fromTransferObj(callLinkSate), listener);
                break;

            case P2pConfEnded:
                BaseTypeInt reason = (BaseTypeInt) rspContent;
                KLog.p("P2pConfEnded: %s", reason.basetype);
                reportFailed(-1, listener);
                break;

            case MultipartyConfStarted:
                callLinkSate = (TMtCallLinkSate) rspContent;
                KLog.p("MultipartyConfStarted: %s", callLinkSate);
                reportSuccess(ToDoConverter.fromTransferObj(callLinkSate), listener);
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







    public synchronized boolean startSession(@NonNull Context ctx, @NonNull SessionEventListener listener){
        if (null != factory){
            KLog.p(KLog.ERROR, "session has started already!");
            return false;
        }

        context = ctx;
        sessionEventListener = listener;

        rtcConnector.setSignalingEventsCallback(new RtcConnectorEventListener());

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
            connWrapperList.add(createPeerConnectionWrapper(CommonDef.CONN_TYPE_PUBLISHER, pubConnConfig));
            connWrapperList.add(createPeerConnectionWrapper(CommonDef.CONN_TYPE_SUBSCRIBER, pubConnConfig));
            connWrapperList.add(createPeerConnectionWrapper(CommonDef.CONN_TYPE_ASS_PUBLISHER, pubConnConfig));
            connWrapperList.add(createPeerConnectionWrapper(CommonDef.CONN_TYPE_ASS_SUBSCRIBER, pubConnConfig));
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
            KLog.p(KLog.ERROR, "createScreenCapturer failed, API level < LOLLIPOP(21)");
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



    private class RtcConnectorEventListener implements RtcConnector.SignalingEvents{

        @Override
        public void onGetOfferCmd(int connType, int mediaType) {
            KLog.p("connType=%s, mediaType=%s", connType, mediaType);

            executor.execute(()->{
                PeerConnectionWrapper pcWrapper = getPcWrapper(connType);
                pcWrapper.curMediaType = mediaType;
                VideoCapturer videoCapturer = null;
                if (CommonDef.MEDIA_TYPE_AV == mediaType){
                    // 针对多路码流的情形，我们需要一路一路地发布（平台的限制）
                    // 我们先发AudioTrack，等到收到setAnswerCmd后再发VideoTrack
                    pcWrapper.setSegmentallyPublishState(pcWrapper.SegmentallyPublishState_PublishingAudio);
                }else{
                    if ((CommonDef.MEDIA_TYPE_VIDEO == mediaType
//                    || CommonDef.MEDIA_TYPE_AV == mediaType
                            || CommonDef.MEDIA_TYPE_ASS_VIDEO == mediaType)
                            && pcWrapper.config.videoEnabled){
                        if (CommonDef.CONN_TYPE_PUBLISHER == connType) {
                            videoCapturer = createCameraCapturer(new Camera2Enumerator(context));
                        }else if (CommonDef.CONN_TYPE_ASS_PUBLISHER == connType) {
                            videoCapturer = createScreenCapturer();
                        }
                    }
                }
                if (pcWrapper.config.videoEnabled && null != /*finalVideoCapturer*/videoCapturer) {
                    pcWrapper.createVideoTrack(/*finalVideoCapturer*/videoCapturer);
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

            midStreamIdMap.put(rtcMedia.mid, rtcMedia.streamid);

            executor.execute(()-> {
                PeerConnectionWrapper pcWrapper = getPcWrapper(connType);
                pcWrapper.pc.setRemoteDescription(pcWrapper.sdpObserver,
                        new SessionDescription(SessionDescription.Type.OFFER, offerSdp)
                );
                pcWrapper.pc.createAnswer(pcWrapper.sdpObserver, new MediaConstraints());
            });
        }

        @Override
        public void onSetAnswerCmd(int connType, String answerSdp) {
            KLog.p("connType=%s, answerSdp=%s", connType, answerSdp);

            executor.execute(() -> {
                PeerConnectionWrapper pcWrapper = getPcWrapper(connType);
                pcWrapper.pc.setRemoteDescription(pcWrapper.sdpObserver,
                        new SessionDescription(SessionDescription.Type.ANSWER, answerSdp)
                );

                if (pcWrapper.isSegmentallyPublishState(pcWrapper.SegmentallyPublishState_PublishingAudio)){
                    pcWrapper.setSegmentallyPublishState(pcWrapper.SegmentallyPublishState_PublishingVideo);
                    // 针对多路码流的情形，我们需要一路一路地发布（平台的限制）
                    // 我们先发AudioTrack，等到收到setAnswerCmd后再发VideoTrack。
                    // 此处我们收到了audiotrack对应的响应，接着我们发videotrack
                    VideoCapturer videoCapturer = createCameraCapturer(new Camera2Enumerator(context));
                    if (pcWrapper.config.videoEnabled && null != videoCapturer) {
                        pcWrapper.createVideoTrack(videoCapturer);
                        pcWrapper.pc.createOffer(pcWrapper.sdpObserver, new MediaConstraints());
                    }
                }else if(pcWrapper.isSegmentallyPublishState(pcWrapper.SegmentallyPublishState_PublishingVideo)){
                    pcWrapper.setSegmentallyPublishState(pcWrapper.SegmentallyPublishState_Finished);
                }

            });

        }

        @Override
        public void onSetIceCandidateCmd(int connType, String sdpMid, int sdpMLineIndex, String sdp) {
            KLog.p("connType=%s, sdpMid=%s, sdpMLineIndex=%s, sdp=%s", connType, sdpMid, sdpMLineIndex, sdp);
            executor.execute(()-> {
                PeerConnectionWrapper pcWrapper = getPcWrapper(connType);
                pcWrapper.addCandidate(new IceCandidate(sdpMid, sdpMLineIndex, sdp));
            });
        }
    }




    private class SDPObserver implements SdpObserver {

        boolean bCreateLocalSdpSuccess;

        @Override
        public void onCreateSuccess(final SessionDescription origSdp) {
            KLog.p("create local sdp success: type=%s, sdp=%s", origSdp.type, origSdp.description);
            executor.execute(() -> {
                bCreateLocalSdpSuccess = true;
                PeerConnectionWrapper pcWrapper = getPcWrapper(this);
                pcWrapper.pc.setLocalDescription(this, origSdp);
            });
        }

        @Override
        public void onSetSuccess() {
            executor.execute(() -> {
                PeerConnectionWrapper pcWrapper = getPcWrapper(this);
                PeerConnection pc = pcWrapper.pc;
                if (pcWrapper.isOffer()) {
                    if (pcWrapper.isSdpProgressFinished()){
                        KLog.p("setRemoteDescription success, sdp progress finished, drainCandidates");
                        pcWrapper.drainCandidates();
                    }else{
                        if (bCreateLocalSdpSuccess) {
                            bCreateLocalSdpSuccess = false;
//                        updateRtpPara(true);
                            KLog.p("setLocalDescription success, sending offer... \n%s", pc.getLocalDescription().description);
                            boolean bGetAudioMid = false;
                            if (pcWrapper.curMediaType == CommonDef.MEDIA_TYPE_AUDIO
                                    || pcWrapper.curMediaType == CommonDef.MEDIA_TYPE_AV && pcWrapper.isSegmentallyPublishState(pcWrapper.SegmentallyPublishState_PublishingAudio)) {
                                bGetAudioMid = true;
                            } else {
                                bGetAudioMid = false;
                            }
                            RtcConnector.TRtcMedia rtcMedia = new RtcConnector.TRtcMedia(pcWrapper.STREAM_ID, getMid(pc.getLocalDescription().description, bGetAudioMid), createEncodingList());
                            handler.post(() -> rtcConnector.sendOfferSdp(pcWrapper.connType, pc.getLocalDescription().description, rtcMedia));
                        }
                    }

                } else {

                    if (pcWrapper.isSdpProgressFinished()){
                        KLog.p("setLocalDescription success, sending answer..., \n%s", pc.getLocalDescription().description);
                        handler.post(() -> rtcConnector.sendAnswerSdp(pcWrapper.connType, pc.getLocalDescription().description));
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

        @Override
        public void onIceCandidate(final IceCandidate candidate) {
            KLog.p("onIceCandidate, sending candidate... %s", candidate.sdp);
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
            KLog.p("onTrack: %s", transceiver);
            MediaStreamTrack track = transceiver.getReceiver().track();
            if (!(track instanceof VideoTrack)){
                return;
            }

            PeerConnectionWrapper pcWrapper = getPcWrapper(this);
            pcWrapper.createRemoteVideoTrack(transceiver.getMid(), (VideoTrack) track);

        }

    }


    private class PeerConnectionWrapper{

        final String STREAM_ID = "TT-Android-"+System.currentTimeMillis();
        final String VIDEO_TRACK_ID = STREAM_ID+"-v0";
        final String AUDIO_TRACK_ID = STREAM_ID+"-a0";

        int connType;
        int curMediaType;

        /*
        * 分段发布状态。
        * 针对多路码流的情形，平台限制需要一路一路地发布，
        * 我们先发AudioTrack，等到收到setAnswerCmd后再发VideoTrack
        * */
        int segmentallyPublishState;
        final int SegmentallyPublishState_PublishingAudio = 1;
        final int SegmentallyPublishState_PublishingVideo = 2;
        final int SegmentallyPublishState_Finished = 3;

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

        PeerConnectionWrapper(int connType, @NonNull PeerConnection pc, @NonNull PeerConnectionConfig config,
                              @NonNull SDPObserver sdpObserver, @NonNull PCObserver pcObserver) {
            this.connType = connType;
            this.pc = pc;
            this.config = config;
            this.sdpObserver = sdpObserver;
            this.pcObserver = pcObserver;
        }

        boolean isSegmentallyPublishState(int state){
            return segmentallyPublishState == state;
        }

        public void setSegmentallyPublishState(int segmentallyPublishState) {
            KLog.p("switch segmentallyPublishState from %s to %s", this.segmentallyPublishState, segmentallyPublishState);
            this.segmentallyPublishState = segmentallyPublishState;
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
            if (SegmentallyPublishState_PublishingAudio == segmentallyPublishState
                    || SegmentallyPublishState_PublishingVideo == segmentallyPublishState){
                return false;
            }

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


    private String getMid(String sdpDescription, boolean bAudio){
        KLog.p("bAudio =%s, sdp=%s", bAudio, sdpDescription);
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
