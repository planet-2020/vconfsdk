package com.kedacom.vconf.webrtc;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.kedacom.kdv.mt.ospconnector.Connector;
import com.kedacom.mt.netmanage.protobuf.BasePB;
import com.kedacom.osp.EmMtOspMsgSys;
import com.kedacom.osp.MtMsg;
import com.kedacom.vconf.sdk.amulet.Caster;
import com.kedacom.vconf.sdk.amulet.IResultListener;
import com.kedacom.vconf.sdk.utils.net.NetAddrHelper;
import com.kedacom.vconf.webrtc.been.StreamInfo;
import com.kedacom.vconf.webrtc.been.trans.TLoginResult;
import com.kedacom.vconf.webrtc.been.trans.TMtRtcSvrAddr;
import com.kedacom.vconf.webrtc.been.trans.TRtcPlayItem;
import com.kedacom.vconf.webrtc.been.trans.TRtcPlayParam;
import com.kedacom.vconf.webrtc.been.trans.TRtcStreamInfo;
import com.kedacom.vconf.webrtc.been.trans.TRtcStreamInfoList;

import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraEnumerator;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.Logging;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RendererCommon;
import org.webrtc.RtpTransceiver;
import org.webrtc.SessionDescription;
import org.webrtc.StatsReport;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoFrame;
import org.webrtc.VideoSink;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class WebRtcClient extends Caster<Msg>{

    private static String TAG = "WebRtcClient";

    private RtcConnector rtcConnector;
    private PeerConnectionClient pubConnClient;
    private PeerConnectionClient subConnClient;
    private PeerConnectionClient assPubConnClient;
    private PeerConnectionClient assSubConnClient;

    private Context context;
    private EglBase eglBase;
    private PeerConnectionClient.PeerConnectionParameters peerConnectionParameters;
//    private ProxyVideoSink localVideoSink = new ProxyVideoSink("localVideoSink");
//    private Map<RenderChannel, ProxyVideoSink> remoteVideoSinks = new HashMap<>();
    private Map<String, ProxyVideoSink> videoSinks = new HashMap<>();

    private Handler handler = new Handler(Looper.getMainLooper());

    public WebRtcClient(Context ctx){
        if (null == ctx){
            throw new IllegalArgumentException("null == ctx");
        }

        context = ctx;
        int videoWidth = context.getResources().getDisplayMetrics().widthPixels;
        int videoHeight = context.getResources().getDisplayMetrics().heightPixels;
        PeerConnectionClient.PeerConnectionParameters parameters =
                new PeerConnectionClient.PeerConnectionParameters(
                        RtpTransceiver.RtpTransceiverDirection.SEND_RECV,
                        true,
                        true,
                        videoWidth,
                        videoHeight,
                        20,
                        1700,
                        "VP8",
                        true,
                        true,
                        32,
                        "OPUS"
                        );

        PeerConnectionClient.PeerConnectionParameters parameters1 = new PeerConnectionClient.PeerConnectionParameters(parameters);
        PeerConnectionClient.PeerConnectionParameters parameters2 = new PeerConnectionClient.PeerConnectionParameters(parameters);
        parameters2.setTransDirection(RtpTransceiver.RtpTransceiverDirection.SEND_ONLY);
        PeerConnectionClient.PeerConnectionParameters parameters3 = new PeerConnectionClient.PeerConnectionParameters(parameters);
        parameters3.setTransDirection(RtpTransceiver.RtpTransceiverDirection.RECV_ONLY);
        /**
         * 创建peerconnectclient。
         * NOTE：一个peerconnect可以处理多路码流，收发均可。
         * 但业务要求主流发/收、辅流发/收4种情形分别用单独的peerconnect处理，故此处创建4个。
         * */
        eglBase = EglBase.create();
        pubConnClient = new PeerConnectionClient(ctx, eglBase, parameters2, new PCEvents(CommonDef.CONN_TYPE_PUBLISHER));
        pubConnClient.createPeerConnectionFactory(new PeerConnectionFactory.Options());
//        eglBase = EglBase.create();
        subConnClient = new PeerConnectionClient(ctx, eglBase, parameters3, new PCEvents(CommonDef.CONN_TYPE_SUBSCRIBER));
        subConnClient.createPeerConnectionFactory(new PeerConnectionFactory.Options());
//        eglBase = EglBase.create();
        assPubConnClient = new PeerConnectionClient(ctx, eglBase, new PeerConnectionClient.PeerConnectionParameters(parameters), new PCEvents(CommonDef.CONN_TYPE_ASS_PUBLISHER));
        assPubConnClient.createPeerConnectionFactory(new PeerConnectionFactory.Options());
//        eglBase = EglBase.create();
        assSubConnClient = new PeerConnectionClient(ctx, eglBase, new PeerConnectionClient.PeerConnectionParameters(parameters), new PCEvents(CommonDef.CONN_TYPE_ASS_SUBSCRIBER));
        assSubConnClient.createPeerConnectionFactory(new PeerConnectionFactory.Options());

        rtcConnector = new RtcConnector();
        rtcConnector.setSignalingEventsCallback(signalingEvents);

//        //XXX FORDEBUG
//        remoteVideoSinks.put(RenderChannel.REMOTE1, new ProxyVideoSink("remoteVideoSink1"));
    }

    public void destroy(){  // FIXME 该类承担过多任务，有一部分应该是单例。比如在aps中登录的部分也需要new一个对象，然后又没释放。

        // destroy rtcclient
        if (null != rtcConnector){
            rtcConnector.destroy();
            rtcConnector = null;
        }

        // destroy peerconnection
        if (pubConnClient != null) {
            pubConnClient.close();
            pubConnClient = null;
        }
        if (subConnClient != null) {
            subConnClient.close();
            subConnClient = null;
        }
        if (assPubConnClient != null) {
            assPubConnClient.close();
            assPubConnClient = null;
        }
        if (assSubConnClient != null) {
            assSubConnClient.close();
            assSubConnClient = null;
        }

        // destroy video sink
//        if (null != localVideoSink){
//            if (null != localVideoSink.target){
//                ((SurfaceViewRenderer)localVideoSink.target).release();
//            }
//            localVideoSink.setTarget(null);
//        }
//        for (ProxyVideoSink videoSink : remoteVideoSinks.values()){
//            if (null != videoSink.target){
//                ((SurfaceViewRenderer)videoSink.target).release();
//            }
//            videoSink.setTarget(null);
//        }
//        remoteVideoSinks.clear();

        for (ProxyVideoSink videoSink : videoSinks.values()){
            if (null != videoSink.target){
                ((SurfaceViewRenderer)videoSink.target).release();
            }
        }
        videoSinks.clear();

        if (null != eglBase) {
            eglBase.release();
            eglBase = null;
        }

        // destroy audiomanager
//        if (audioManager != null) {
//            audioManager.stop();
//            audioManager = null;
//        }

    }

    @Override
    protected Map<Msg[], RspProcessor<Msg>> rspsProcessors() {
        Map<Msg[], RspProcessor<Msg>> processorMap = new HashMap<>();
        processorMap.put(new Msg[]{
                Msg.Login,
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
        }, this::onNtfs);

        return processorMap;
    }


    /**
     * 登录rtc
     * 注意，需先登录aps成功。
     * */
    public void login(IResultListener resultListener){
        TMtRtcSvrAddr rtcSvrAddr = (TMtRtcSvrAddr) get(Msg.GetSvrAddr);
//        if (null == rtcSvrAddr){
//            KLog.p(KLog.ERROR, "null == rtcSvrAddr");
//            reportFailed(-1, resultListener);
//            return;
//        }
        if (null == rtcSvrAddr){
            KLog.p(KLog.ERROR, "null == rtcSvrAddr");
            try {
                int ip = NetAddrHelper.ipStr2Int("172.16.179.114"); //FIXME 写死方便调试
                rtcSvrAddr = new TMtRtcSvrAddr(ip, 7961,"0512110000004");
            } catch (NetAddrHelper.InvalidIpv4Exception e) {
                e.printStackTrace();
            }
        }

        req(Msg.Login, resultListener, rtcSvrAddr);
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
            default:
                return false;
        }

        return true;
    }


    private List<TRtcStreamInfo> streamInfos;

    private void onNtfs(Msg ntfId, Object ntfContent, Set<Object> listeners) {
        switch (ntfId){
            case StreamListReady:
                List<TRtcPlayItem> rtcPlayItems = new ArrayList<>();
                TRtcStreamInfoList streamInfoList = (TRtcStreamInfoList) ntfContent;
                streamInfos = streamInfoList.atStramInfoList;
                for (TRtcStreamInfo streamInfo : streamInfoList.atStramInfoList){
                    if (streamInfo.bAudio){
                        continue;
                    }
                    TRtcPlayItem rtcPlayItem = new TRtcPlayItem();
                    rtcPlayItem.emRes = streamInfo.aemSimcastRes.get(0); // FIXME 调试方便
                    rtcPlayItem.achStreamId = streamInfo.achStreamId;
                    rtcPlayItem.bLocal = false;
                    rtcPlayItem.bAss = streamInfo.bAss;
                    rtcPlayItems.add(rtcPlayItem);
                }
                set(Msg.SetPlayPara, new TRtcPlayParam(rtcPlayItems));
                break;
            case StreamJoined:  //TODO  这里是增量过来的，
                rtcPlayItems = new ArrayList<>();
                streamInfoList = (TRtcStreamInfoList) ntfContent;
                streamInfos = streamInfoList.atStramInfoList;
                for (TRtcStreamInfo streamInfo : streamInfoList.atStramInfoList){
                    if (streamInfo.bAudio){
                        continue;
                    }
                    TRtcPlayItem rtcPlayItem = new TRtcPlayItem();
                    rtcPlayItem.emRes = streamInfo.aemSimcastRes.get(0); // FIXME 调试方便
                    rtcPlayItem.achStreamId = streamInfo.achStreamId;
                    rtcPlayItem.bLocal = false;
                    rtcPlayItem.bAss = streamInfo.bAss;
                    rtcPlayItems.add(rtcPlayItem);
                }
                set(Msg.SetPlayPara, new TRtcPlayParam(rtcPlayItems)); // 这里设置是增量的，还是覆盖的？如果是覆盖的，我需要本地记录下原本的列表？
                break;
            case StreamLeft:
                rtcPlayItems = new ArrayList<>();
                streamInfoList = (TRtcStreamInfoList) ntfContent; // FIXME 这里过来的是增量，set的是全量
                streamInfos = streamInfoList.atStramInfoList;
                for (TRtcStreamInfo streamInfo : streamInfoList.atStramInfoList){
                    if (streamInfo.bAudio){
                        continue;
                    }
                    TRtcPlayItem rtcPlayItem = new TRtcPlayItem();
                    rtcPlayItem.emRes = streamInfo.aemSimcastRes.get(0); // FIXME 调试方便
                    rtcPlayItem.achStreamId = streamInfo.achStreamId;
                    rtcPlayItem.bLocal = false;
                    rtcPlayItem.bAss = streamInfo.bAss;
                    rtcPlayItems.add(rtcPlayItem);
                }
                set(Msg.SetPlayPara, new TRtcPlayParam(rtcPlayItems));
                break;
        }
    }


    public class RtcRender {
        private SurfaceViewRenderer surfaceViewRenderer;
        public RtcRender() {
            surfaceViewRenderer = new SurfaceViewRenderer(context);
            surfaceViewRenderer.init(eglBase.getEglBaseContext(), null);
            surfaceViewRenderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
            surfaceViewRenderer.setEnableHardwareScaler(true);
        }

        public void destroy(){
            surfaceViewRenderer.release();
            surfaceViewRenderer = null;
        }

        public View getView(){
            return surfaceViewRenderer;
        }

        public void setOnTop(boolean bOnTop){
            surfaceViewRenderer.setZOrderMediaOverlay(bOnTop);
        }

    }
    private static class ProxyVideoSink implements VideoSink {
        private String name;
        private VideoSink target;
        private long timestamp = System.currentTimeMillis();

        public ProxyVideoSink(String name) {
            this.name = name;
        }

        public ProxyVideoSink(String name, VideoSink target) {
            this.name = name;
            this.target = target;
        }

        @Override
        synchronized public void onFrame(VideoFrame frame) {
            long curts = System.currentTimeMillis();
            if (curts - timestamp > 5000){
                timestamp = curts;
                Logging.d(TAG, name+" onFrame, "+"target="+target);
            }
            if (target == null) {
                Logging.d(TAG, "Dropping frame in proxy because target is null.");
                return;
            }

            target.onFrame(frame);
        }

        synchronized void setTarget(VideoSink target) {
            this.target = target;
            Logging.d(TAG, name+" target="+target);
        }

    }


    public boolean bindRender(String trackId, RtcRender renderer){
        SurfaceViewRenderer surfaceViewRenderer = null==renderer ? null : renderer.surfaceViewRenderer;
        ProxyVideoSink videoSink = videoSinks.get(trackId);
        if (null == videoSink){
            KLog.p(KLog.ERROR, "null == videoSink");
            return false;
        }
        videoSink.setTarget(surfaceViewRenderer);
        return true;
    }

    public boolean swapTrack(String srcTrackId, String dstTrackId){
        ProxyVideoSink srcSink = videoSinks.get(srcTrackId);
        ProxyVideoSink dstSink = videoSinks.get(dstTrackId);
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

    private BiMap<String, String> midStreamIdMap = HashBiMap.create();

    private RtcConnector.SignalingEvents signalingEvents = new RtcConnector.SignalingEvents() {
        @Override
        public void onConnectedToRoom(int connType, SignalingParameters params, PeerConnectionClient.TRtcMedia rtcMedia) {
            KLog.tp("SIGNALINGEVENTS", KLog.WARN, "onConnectedToRoom connType=%s, mediaType=%s", connType, params.mediaType);
            handler.post(() -> {
                PeerConnectionClient pcClient = getPeerConnectionClient(connType);
                VideoCapturer videoCapturer = null;
                if (CommonDef.MEDIA_TYPE_AUDIO != params.mediaType && CommonDef.CONN_TYPE_PUBLISHER == connType){
                    videoCapturer = createVideoCapturer();
                }
                if (null != rtcMedia){
                    midStreamIdMap.put(rtcMedia.mid, rtcMedia.streamid);  // TODO streamId和流信息绑定起来，在上层只知道streamId（通过Ntf通知），bindRender时传下streamId
                }
                pcClient.createPeerConnection(
                        videoCapturer,
                        params, rtcMedia);
                if (params.initiator) {
                    // Create offer. Offer SDP will be sent to answering client in
                    // PeerConnectionEvents.onLocalDescription event.
                    KLog.p("Creating OFFER...");
                    pcClient.createOffer();
                }else{
                    if (params.offerSdp != null) {
                        pcClient.setRemoteDescription(params.offerSdp);
                        KLog.p("Creating ANSWER...");
                        // Create answer. Answer SDP will be sent to offering client in
                        // PeerConnectionEvents.onLocalDescription event.
                        pcClient.createAnswer();
                    }
                    if (params.iceCandidates != null) {
                        // Add remote ICE candidates from room.
                        for (IceCandidate iceCandidate : params.iceCandidates) {
                            pcClient.addRemoteIceCandidate(iceCandidate);
                        }
                    }
                }
            });

        }

        @Override
        public void onRemoteDescription(int connType, SessionDescription sdp) {
            KLog.tp("SIGNALINGEVENTS", KLog.WARN, "onRemoteDescription");
            handler.post(() -> {
                PeerConnectionClient pcClient = getPeerConnectionClient(connType);
                if (sdp.type == SessionDescription.Type.OFFER) {
                    pcClient.setRemoteDescription(sdp);
                    KLog.p("recv offer, Creating ANSWER...");
                    // Create answer. Answer SDP will be sent to offering client in
                    // PeerConnectionEvents.onLocalDescription event.
                    pcClient.createAnswer();
                }else{
                    KLog.p("recv ANSWER");
                    pcClient.setRemoteDescription(sdp);
                }
            });

        }

        @Override
        public void onRemoteIceCandidate(int connType, IceCandidate candidate) {
            KLog.tp("SIGNALINGEVENTS", KLog.WARN, "onRemoteIceCandidate");
            handler.post(() -> {
                PeerConnectionClient pcClient = getPeerConnectionClient(connType);
                pcClient.addRemoteIceCandidate(candidate);
            });
        }

        @Override
        public void onRemoteIceCandidatesRemoved(int connType, IceCandidate[] candidates) {
            KLog.tp("SIGNALINGEVENTS", KLog.WARN, "onRemoteIceCandidatesRemoved");
        }

        @Override
        public void onChannelClose(int connType) {
            KLog.tp("SIGNALINGEVENTS", KLog.WARN, "onChannelClose");
        }

        @Override
        public void onChannelError(int connType, String description) {
            KLog.tp("SIGNALINGEVENTS", KLog.WARN, "onChannelError");
        }
    };


    private class PCEvents implements PeerConnectionClient.PeerConnectionEvents {
        private int connType;

        public PCEvents(int connType) {
            this.connType = connType;
        }

        @Override
        public void onLocalDescription(SessionDescription sdp, PeerConnectionClient.TRtcMedia rtcMedia) {
            KLog.tp("PEERCONNECTIONEVENTS", KLog.WARN, "onLocalDescription");
            handler.post(() -> {
                if (sdp.type == SessionDescription.Type.OFFER) {
                    KLog.p("Creating OFFER success, sending offer...");
                    rtcConnector.sendOfferSdp(connType, sdp, rtcMedia);
                } else {
                    KLog.p("Creating ANSWER success, sending ANSWER...");
                    rtcConnector.sendAnswerSdp(connType, sdp);
                }
            });
        }

        @Override
        public void onIceCandidate(IceCandidate candidate) {
            KLog.tp("PEERCONNECTIONEVENTS", KLog.WARN, "onIceCandidate");
            handler.post(() -> {
                rtcConnector.sendIceCandidate(connType, candidate);
            });
        }

        @Override
        public void onIceCandidatesRemoved(IceCandidate[] candidates) {
            KLog.tp("PEERCONNECTIONEVENTS", KLog.WARN, "onIceCandidatesRemoved");
        }

        @Override
        public void onIceConnected() {
            KLog.tp("PEERCONNECTIONEVENTS", KLog.WARN, "onIceConnected");
        }

        @Override
        public void onIceDisconnected() {
            KLog.tp("PEERCONNECTIONEVENTS", KLog.WARN, "onIceDisconnected");
        }

        @Override
        public void onConnected() {
            KLog.tp("PEERCONNECTIONEVENTS", KLog.WARN, "onConnected");
            handler.post(() -> {
                logAndToast("<-onConnected, DTLS connected");
//                if (peerConnectionClient == null) {
//                    Log.w(TAG, "Call is connected in closed or error state");
//                    return;
//                }
//                // Enable statistics callback.
//                peerConnectionClient.enableStatsEvents(true, 1000);
//                setSwappedFeeds(false /* isSwappedFeeds */);
            });
        }

        @Override
        public void onDisconnected() {
            KLog.tp("PEERCONNECTIONEVENTS", KLog.WARN, "onDisconnected");
        }

        @Override
        public void onPeerConnectionClosed() {
            KLog.tp("PEERCONNECTIONEVENTS", KLog.WARN, "onPeerConnectionClosed");
        }

        @Override
        public void onPeerConnectionStatsReady(StatsReport[] reports) {
//            KLog.tp("PEERCONNECTIONEVENTS", KLog.WARN, "onPeerConnectionStatsReady");
        }

        @Override
        public void onPeerConnectionError(String description) {
            KLog.tp("PEERCONNECTIONEVENTS", KLog.WARN, "onPeerConnectionError");
        }


        @Override
        public void onLocalVideoTrack(String trackId) {
            handler.post(() -> {
                ProxyVideoSink videoSink = new ProxyVideoSink(trackId);  // FIXME 应该是StreamId，组件给的是mid和streamId的对应关系
                videoSinks.put(trackId, videoSink);
                getPeerConnectionClient(connType).bindLocalSink(trackId, videoSink);
                if (null != eventListner) {
                    String streamId = midStreamIdMap.get(trackId);
                    if (null == streamId){
                        KLog.p(KLog.ERROR, "not register stream %s in signaling progress", trackId);
                        return;
                    }
                    TRtcStreamInfo rtcStreamInfo = null;
                    for (TRtcStreamInfo streamInfo : streamInfos){
                        if (streamId.equals(streamInfo.achStreamId)){
                            rtcStreamInfo = streamInfo;
                            break;
                        }
                    }
                    if (null == rtcStreamInfo){
                        KLog.p(KLog.ERROR, "no such stream %s in stream list", trackId);
                        return;
                    }
                    eventListner.onLocalStream(new StreamInfo(rtcStreamInfo.tMtId.dwMcuId, rtcStreamInfo.tMtId.dwTerId, streamId));
                }
            });
        }

        @Override
        public void onRemoteVideoTrack(String trackId) {
            KLog.p("trackId=%s", trackId);
            handler.post(() -> {

                ProxyVideoSink videoSink = new ProxyVideoSink(trackId);  // FIXME 应该是StreamId，组件给的是mid和streamId的对应关系
                videoSinks.put(trackId, videoSink);
                getPeerConnectionClient(connType).bindRemoteSink(trackId, videoSink);
                if (null != eventListner) {
                    String streamId = midStreamIdMap.get(trackId);
                    if (null == streamId){
                        KLog.p(KLog.ERROR, "not register stream %s in signaling progress", trackId);
                        return;
                    }
                    TRtcStreamInfo rtcStreamInfo = null;
                    for (TRtcStreamInfo streamInfo : streamInfos){
                        if (streamId.equals(streamInfo.achStreamId)){
                            rtcStreamInfo = streamInfo;
                            break;
                        }
                    }
                    if (null == rtcStreamInfo){
                        KLog.p(KLog.ERROR, "no such stream %s in stream list", trackId);
                        return;
                    }
                    eventListner.onRemoteStream(new StreamInfo(rtcStreamInfo.tMtId.dwMcuId, rtcStreamInfo.tMtId.dwTerId, streamId));
                }

            });
        }

        @Override
        public void onRemoteVideoTrackRemoved(String trackId) {
            if (null != eventListner){
                // TODO
//                handler.post(() -> eventListner.onRemoteStreamRemoved(trackId));
            }
        }
    }

    private @Nullable
    VideoCapturer createVideoCapturer() {
        final VideoCapturer videoCapturer;

        Logging.d(TAG, "Creating capturer using camera2 API.");
        videoCapturer = createCameraCapturer(new Camera2Enumerator(context));

        if (videoCapturer == null) {
            KLog.p(KLog.ERROR, "Failed to open camera");
            return null;
        }
        return videoCapturer;
    }

    private @Nullable VideoCapturer createCameraCapturer(CameraEnumerator enumerator) {
        final String[] deviceNames = enumerator.getDeviceNames();

        // First, try to find front facing camera
        Logging.d(TAG, "Looking for front facing cameras.");
        for (String deviceName : deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                Logging.d(TAG, "Creating front facing camera capturer.");
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);

                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }

        // Front facing camera not found, try something else
        Logging.d(TAG, "Looking for other cameras.");
        for (String deviceName : deviceNames) {
            if (!enumerator.isFrontFacing(deviceName)) {
                Logging.d(TAG, "Creating other camera capturer.");
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);

                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }

        return null;
    }


    PeerConnectionClient getPeerConnectionClient(int type){
        if (CommonDef.CONN_TYPE_PUBLISHER == type){
            return pubConnClient;
        }else if (CommonDef.CONN_TYPE_SUBSCRIBER == type){
            return subConnClient;
        }else if (CommonDef.CONN_TYPE_ASS_PUBLISHER == type){
            return assPubConnClient;
        }else if (CommonDef.CONN_TYPE_ASS_SUBSCRIBER == type){
            return assSubConnClient;
        }else{
            return pubConnClient;
        }
    }

    // List of mandatory application permissions.
//    private static final String[] MANDATORY_PERMISSIONS = {"android.permission.MODIFY_AUDIO_SETTINGS",
//            "android.permission.RECORD_AUDIO", "android.permission.INTERNET"};



//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//
//
//        // Check for mandatory permissions.
//        for (String permission : MANDATORY_PERMISSIONS) {
//            if (ctx.checkCallingOrSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
//                logAndToast("Permission " + permission + " is not granted");
//                return;
//            }
//        }
//
//        requestPermissions();
//
//    }

//    boolean isSwappedFeeds;
//    private void setSwappedFeeds(boolean isSwappedFeeds) {
//        Logging.d(TAG, "setSwappedFeeds: " + isSwappedFeeds);
//        this.isSwappedFeeds = isSwappedFeeds;
//        localProxyVideoSink.setTarget(isSwappedFeeds ? fullscreenRenderer : pipRenderer);
//        remoteProxyRenderer.setTarget(isSwappedFeeds ? pipRenderer : fullscreenRenderer);
//        fullscreenRenderer.setMirror(isSwappedFeeds);
//        pipRenderer.setMirror(!isSwappedFeeds);
//    }


    private Toast logToast;
    private void logAndToast(String msg) {
        Log.d(TAG, msg);
        if (logToast != null) {
            logToast.cancel();
        }
        logToast = Toast.makeText(context, msg, Toast.LENGTH_SHORT);
        logToast.show();
    }



    // FORDEBUG
    public boolean startSignaling()
    {
        MtMsg msg = new MtMsg();
        msg.SetMsgId("Ev_MT_GetOffer_Cmd");
        msg.addMsg(BasePB.TU32.newBuilder().setValue(0).build());
        msg.addMsg(BasePB.TU32.newBuilder().setValue(2).build());

        long nSrcID = Connector.MAKEIID(RtcConnector.WEBRTC_ID, (short)1 );
        long nDstID = nSrcID;
        long nSrcNodeId=0;
        long nDstNodeId=0;

        byte[] abyContent = msg.Encode();
        int nRet = Connector.PostOspMsg( EmMtOspMsgSys.Ev_MtOsp_ProtoBufMsg.getnVal(), abyContent, abyContent.length,
                nDstID, nDstNodeId, nSrcID, nSrcNodeId, 5000 );
        if (nRet != 0){
            KLog.p(KLog.ERROR, "post msg %s failed, ret=%s", msg.GetMsgId(), nRet);
            return false;
        }

        return true;
    }


//    private static final int PERMISSION_REQUEST = 2;
//    @TargetApi(Build.VERSION_CODES.M)
//    private void requestPermissions() {
//        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
//            // Dynamic permissions are not required before Android M.
//            return;
//        }
//
//        String[] missingPermissions = getMissingPermissions();
//        if (missingPermissions.length != 0) {
//            context.requestPermissions(missingPermissions, PERMISSION_REQUEST);
//        }
//    }


//    @TargetApi(Build.VERSION_CODES.M)
//    private String[] getMissingPermissions() {
//        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
//            return new String[0];
//        }
//
//        PackageInfo info;
//        try {
//            info = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_PERMISSIONS);
//        } catch (PackageManager.NameNotFoundException e) {
//            Log.w(TAG, "Failed to retrieve permissions.");
//            return new String[0];
//        }
//
//        if (info.requestedPermissions == null) {
//            Log.w(TAG, "No requested permissions.");
//            return new String[0];
//        }
//
//        ArrayList<String> missingPermissions = new ArrayList<>();
//        for (int i = 0; i < info.requestedPermissions.length; i++) {
//            if ((info.requestedPermissionsFlags[i] & PackageInfo.REQUESTED_PERMISSION_GRANTED) == 0) {
//                missingPermissions.add(info.requestedPermissions[i]);
//            }
//        }
//        Log.d(TAG, "Missing permissions: " + missingPermissions);
//
//        return missingPermissions.toArray(new String[missingPermissions.size()]);
//    }


//    @Override
//    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
//        if (requestCode == PERMISSION_REQUEST) {
//            String[] missingPermissions = getMissingPermissions();
//            if (missingPermissions.length != 0) {
//                // User didn't grant all the permissions. Warn that the application might not work
//                // correctly.
//                new AlertDialog.Builder(this)
//                        .setMessage("The application is missing permissions. It might not work correctly. Do you want to try again?")
//                        .setPositiveButton("yes",
//                                (dialog, id) -> {
//                                    // User wants to try giving the permissions again.
//                                    dialog.cancel();
//                                    requestPermissions();
//                                })
//                        .setNegativeButton("no",
//                                (dialog, id) -> {
//                                    // User doesn't want to give the permissions.
//                                    dialog.cancel();
//                                })
//                        .show();
//            }
//        }
//    }

    public interface EventListner{
        void onLocalStream(StreamInfo stream);
        void onRemoteStream(StreamInfo stream);
        void onRemoteStreamRemoved(StreamInfo stream);
    }
    private EventListner eventListner;
    public void setEventListner(EventListner eventListner){
        this.eventListner = eventListner;
    }

}
