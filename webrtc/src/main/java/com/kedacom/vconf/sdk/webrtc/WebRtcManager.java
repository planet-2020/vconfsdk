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
import android.view.SurfaceHolder;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.common.base.Function;
import com.google.common.collect.BiMap;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Iterables;
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
import com.kedacom.vconf.sdk.webrtc.bean.trans.TCreateConfResult;
import com.kedacom.vconf.sdk.webrtc.bean.trans.TLoginResult;
import com.kedacom.vconf.sdk.webrtc.bean.trans.TMTEntityInfo;
import com.kedacom.vconf.sdk.webrtc.bean.trans.TMTEntityInfoList;
import com.kedacom.vconf.sdk.webrtc.bean.trans.TMtId;
import com.kedacom.vconf.sdk.webrtc.bean.trans.TMtRtcSvrAddr;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.kedacom.vconf.sdk.webrtc.bean.*;

@SuppressWarnings({"unused"})
public class WebRtcManager extends Caster<Msg>{

    private static final String TAG = WebRtcManager.class.getSimpleName();

    private Context context;

    private RtcConnector rtcConnector = new RtcConnector();

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private EglBase eglBase;
    private PeerConnectionFactory factory;
    private PeerConnectionWrapper pubPcWrapper;
    private PeerConnectionWrapper subPcWrapper;
    private PeerConnectionWrapper assPubPcWrapper;
    private PeerConnectionWrapper assSubPcWrapper;


    // 与会方列表。
    // 一个与会方只有一路视频流可以有多个音频流。
    // 辅流虽然是由某个与会方发出，但我们特殊处理，既把它看作是一路流也把它看作是一个虚拟的与会方。
    private Map<String, Conferee> conferees = new HashMap<>();
    // 流列表。
    // 这里的流不是WebRTC的Stream而是平台的流，概念上等同于WebRTC的Track。
    // 一路流总是是属于某一个与会方。
    // 辅流虽然是由某个与会方发出，但我们特殊处理，既把它看作是一路流也把它看作是一个虚拟的与会方。
    private Map<String, Stream> streams = new HashMap<>();
    // 平台的StreamId到视频槽的映射。
    // 视频槽输入口连接WebRTC的视频流，输出口连接Display。
    private Map<String, ProxyVideoSink> videoSinks = new HashMap<>();
    // 用来展示与会方画面的Display集合。
    // 一个Display对应一个与会方，需绑定到与会方以展示其内容。
    private Set<Display> displaySet = new HashSet<>();

    // WebRTC的mid到平台的StreamId之间的映射
    private BiMap<String, String> mid2KdStreamIdMap = HashBiMap.create();
    // 平台的StreamId到WebRTC的TrackId之间的映射
    private BiMap<String, String> kdStreamId2RtcTrackIdMap = HashBiMap.create();

    // 用于定时收集统计信息
    private StatsHelper.Stats publisherStats;
    private StatsHelper.Stats subscriberStats;
    private StatsHelper.Stats allStats;
    private Runnable statsRunnable = new Runnable() {
        @Override
        public void run() {
            collectStats();
            sessionHandler.postDelayed(this, 2000);
        }
    };


    // 当前用户的e164
    private String userE164;

    private Conferee myConferee;

    private Handler sessionHandler = new Handler(Looper.getMainLooper());

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
        }, this::onRsp);
        return processorMap;
    }

    @Override
    protected Map<Msg[], NtfProcessor<Msg>> ntfsProcessors() {
        Map<Msg[], NtfProcessor<Msg>> processorMap = new HashMap<>();
        processorMap.put(new Msg[]{
                Msg.CallIncoming,
                Msg.ConfCanceled,
                Msg.MultipartyConfEnded,
                Msg.CurrentConfereeList,
                Msg.ConfereeJoined,
                Msg.ConfereeLeft,
                Msg.CurrentStreamList,
                Msg.StreamJoined,
                Msg.StreamLeft,
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
                1024*4, // 按需求创会码率固定为4M，呼叫码率也是???
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
     *                             成功：{@link MakeCallResult}
     *                             失败：失败码 //TODO
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
        sharedWindow = window;
        req(Msg.ToggleScreenShare, resultListener, true);
    }

    /**
     * 结束窗口共享
     * */
    public void stopWindowShare(){
        sharedWindow = null;
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
        req(Msg.SetSilence, null, bSilence);
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
        req(Msg.SetMute, null, bMute);
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
     * 摄像头是否已设置为开启
     * */
    public boolean isCameraEnabled(){
        PeerConnectionWrapper pcWrapper = getPcWrapper(CommonDef.CONN_TYPE_PUBLISHER);
        if (null != pcWrapper) {
            return pcWrapper.isLocalVideoEnabled();
        }
        return false;
    }

    /**
     * 开启/关闭摄像头
     * */
    public void setCameraEnable(final boolean enable) {
        PeerConnectionWrapper pcWrapper = getPcWrapper(CommonDef.CONN_TYPE_PUBLISHER);
        if (null != pcWrapper) {
            pcWrapper.setLocalVideoEnable(enable);
        }
        if (null != myConferee){
            Display display = getDisplay(myConferee.id);
            if (null != display){
                display.showCameraDisabledDeco(!enable);
            }
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
            case RegisterRsp:
                TLoginResult loginResult = (TLoginResult) rspContent;
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
                stopSession();
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
                    stopSession();
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

            case CurrentConfereeList:
                TMTEntityInfoList entityInfoList = (TMTEntityInfoList) ntfContent;
                for (TMTEntityInfo entityInfo : entityInfoList.atMtEntitiy) {
                    Conferee conferee = ToDoConverter.tMTEntityInfo2ConfereeInfo(entityInfo);
                    conferees.put(conferee.id, conferee);
                    if (userE164.equals(conferee.e164)){ // 自己
                        myConferee = conferee;
                    }
                }
                if (null != sessionEventListener) {
                    for (Conferee conferee : conferees.values()) {
                        sessionEventListener.onConfereeJoined(conferee);
                    }
                }
                break;

            case ConfereeJoined:
                Conferee conferee = ToDoConverter.tMTEntityInfo2ConfereeInfo((TMTEntityInfo) ntfContent);
                conferees.put(conferee.id, conferee);
                if (null != sessionEventListener) {
                    sessionEventListener.onConfereeJoined(conferee);
                }
                break;

            case ConfereeLeft:
                TMtId tMtId = (TMtId) ntfContent;
                KLog.p("ConfereeLeft, %s %s", tMtId.dwMcuId, tMtId.dwTerId);
                Conferee leftConferee = Iterables.find(conferees.values(), input -> input.id.equals(Conferee.buildId(tMtId.dwMcuId, tMtId.dwTerId, false)));
                if (null != leftConferee){
                    Conferee removedConferee = conferees.remove(leftConferee.id);
                    if (null != removedConferee && null != sessionEventListener) {
                        sessionEventListener.onConfereeLeft(removedConferee);
                    }
                }
                break;

            case CurrentStreamList:
            case StreamJoined:
                KLog.p("CurrentStreamList");
                TRtcStreamInfo assStreamInfo = null;
                for (TRtcStreamInfo tRtcStreamInfo : ((TRtcStreamInfoList) ntfContent).atStramInfoList){
                    Stream stream = ToDoConverter.tRtcStreamInfo2Stream(tRtcStreamInfo);
                    streams.put(stream.streamId, stream);
                    if (tRtcStreamInfo.bAss && !tRtcStreamInfo.bAudio){
                        assStreamInfo = tRtcStreamInfo;
                    }
                }
                List<TRtcPlayItem> playItems = FluentIterable.from(streams.values()).filter(input -> !input.bAudio).transform(new Function<Stream, TRtcPlayItem>() {
                    @NullableDecl
                    @Override
                    public TRtcPlayItem apply(@NullableDecl Stream input) {
                        return new TRtcPlayItem(input.streamId, input.bAss, input.supportedResolutionList.get(0));
                    }
                }).toList();

                set(Msg.SelectStream, new TRtcPlayParam(playItems));

                if (null != assStreamInfo){
                    // 对于辅流我们当作特殊的与会方。所以此处我们构造一个虚拟的与会方上报用户
                    Conferee sender = getConferee(assStreamInfo.tMtId.dwMcuId, assStreamInfo.tMtId.dwTerId);
                    if (null != sender){
                        Conferee assStreamConferee = new Conferee(sender.mcuId, sender.terId, sender.e164, sender.alias, sender.email, true);
                        conferees.put(assStreamConferee.id, assStreamConferee);
                        if (null != sessionEventListener){
                            sessionEventListener.onConfereeJoined(assStreamConferee);
                        }
                    }
                }
                break;

            case StreamLeft:
                KLog.p("StreamLeft");
                Stream assStream = null;
                for (TRtcStreamInfo kdStream : ((TRtcStreamInfoList) ntfContent).atStramInfoList){
                    Stream leftStream = Iterables.find(streams.values(), input -> input.streamId.equals(kdStream.achStreamId), null);
                    if (null != leftStream){
                        PeerConnectionWrapper pcWrapper;
                        if (leftStream.bAss) {
                            assStream = leftStream;  // 我们认为只可能有一路辅流，所以此处在循环中赋值也没关系，只可能赋值一次。
                            pcWrapper = getPcWrapper(CommonDef.CONN_TYPE_ASS_SUBSCRIBER);
                        }else{
                            pcWrapper = getPcWrapper(CommonDef.CONN_TYPE_SUBSCRIBER);
                        }
                        // 删除对应的track
                        if (null != pcWrapper) {
                            if (leftStream.bAudio) {
                                pcWrapper.removeRemoteAudioTrack(leftStream.streamId);
                            } else {
                                pcWrapper.removeRemoteVideoTrack(leftStream.streamId);
                            }
                        }
                    }
                }
                playItems = FluentIterable.from(streams.values()).filter(input -> !input.bAudio).transform(new Function<Stream, TRtcPlayItem>() {
                    @NullableDecl
                    @Override
                    public TRtcPlayItem apply(@NullableDecl Stream input) {
                        return new TRtcPlayItem(input.streamId, input.bAss, input.supportedResolutionList.get(0));
                    }
                }).toList();

                set(Msg.SelectStream, new TRtcPlayParam(playItems));

                if (null != assStream){
                    leftConferee = getConferee(assStream.ownerId);
                    if (null != leftConferee && null != sessionEventListener){
                        sessionEventListener.onConfereeLeft(leftConferee);
                    }
                }

                break;

        }

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


    public static class Config{
        public boolean enableVideoCodecHwAcceleration;
        public boolean enableSimulcast;
        public String videoCodec;
        public int videoWidth;
        public int videoHeight;
        public int videoFps;
        public int videoMaxBitrate;
        public String audioCodec;
        public int audioStartBitrate;

        public Config(){

        }

        public Config(boolean enableVideoCodecHwAcceleration, boolean enableSimulcast,
                      String videoCodec, int videoWidth, int videoHeight, int videoFps, int videoMaxBitrate,
                      String audioCodec, int audioStartBitrate) {
            this.enableVideoCodecHwAcceleration = enableVideoCodecHwAcceleration;
            this.enableSimulcast = enableSimulcast;
            this.videoCodec = videoCodec;
            this.videoWidth = videoWidth;
            this.videoHeight = videoHeight;
            this.videoFps = videoFps;
            this.videoMaxBitrate = videoMaxBitrate;
            this.audioCodec = audioCodec;
            this.audioStartBitrate = audioStartBitrate;
        }

        private void set(Config config){
            this.enableVideoCodecHwAcceleration = config.enableVideoCodecHwAcceleration;
            this.enableSimulcast = config.enableSimulcast;
            this.videoCodec = config.videoCodec;
            this.videoWidth = config.videoWidth;
            this.videoHeight = config.videoHeight;
            this.videoFps = config.videoFps;
            this.videoMaxBitrate = config.videoMaxBitrate;
            this.audioCodec = config.audioCodec;
            this.audioStartBitrate = config.audioStartBitrate;
        }

        @Override
        public String toString() {
            return "Config{" +
                    "enableVideoCodecHwAcceleration=" + enableVideoCodecHwAcceleration +
                    ", videoCodec='" + videoCodec + '\'' +
                    ", videoWidth=" + videoWidth +
                    ", videoHeight=" + videoHeight +
                    ", videoFps=" + videoFps +
                    ", videoMaxBitrate=" + videoMaxBitrate +
                    ", audioCodec='" + audioCodec + '\'' +
                    ", audioStartBitrate=" + audioStartBitrate +
                    '}';
        }
    }
    private Config config = new Config(
            true,
            true,
            VIDEO_CODEC_H264_HIGH,
            1920,
            1080,
            20,
            1700,
            AUDIO_CODEC_OPUS,
            32
    );

    /**
     * 设置媒体偏好
     * */
    public void setConfig(@NonNull Config config){
        this.config.set(config);
    }

    public Config getConfig(){
        Config config = new Config();
        config.set(this.config);
        return config;
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

        eglBase = EglBase.create();

        createPeerConnectionFactory();
        createPeerConnectionWrapper();

        // 定时获取统计信息
        sessionHandler.postDelayed(statsRunnable, 3000);

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

        sessionHandler.removeCallbacksAndMessages(null);

        if (null != eglBase) {
            eglBase.release();
            eglBase = null;
        }

        for (Display display : displaySet){
            display.destroy();
        }
        displaySet.clear();

        for (ProxyVideoSink videoSink : videoSinks.values()){
            videoSink.release();
        }
        videoSinks.clear();

        conferees.clear();
        streams.clear();
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

    }


    private void collectStats(){
        if (null != pubPcWrapper && null != pubPcWrapper.pc) {
            pubPcWrapper.pc.getStats(rtcStatsReport -> {
                KLog.p("publisher rtcStatsReport=%s ", rtcStatsReport);
                publisherStats = StatsHelper.resolveStats(rtcStatsReport);
                if (null == subscriberStats) {
                    // got publisherStats firstly, append subscriberStats later.
                    allStats = publisherStats;
                } else {
                    // already got subscriberStats, append publisherStats.
                    allStats.audioSource = publisherStats.audioSource;
                    allStats.videoSource = publisherStats.videoSource;
                    allStats.sendAudioTrack = publisherStats.sendAudioTrack;
                    allStats.sendVideoTrack = publisherStats.sendVideoTrack;
                    allStats.audioOutboundRtp = publisherStats.audioOutboundRtp;
                    allStats.videoOutboundRtp = publisherStats.videoOutboundRtp;
                    allStats.audioInboundRtpList.addAll(publisherStats.audioInboundRtpList);
                    allStats.videoInboundRtpList.addAll(publisherStats.videoInboundRtpList);
                    allStats.recvAudioTrackList.addAll(publisherStats.recvAudioTrackList);
                    allStats.recvVideoTrackList.addAll(publisherStats.recvVideoTrackList);
                    allStats.encoderList.addAll(publisherStats.encoderList);
                    allStats.decoderList.addAll(publisherStats.decoderList);

                    // both publisherStats and subscriberStats got, complete.
                    publisherStats = null;
                    subscriberStats = null;
                    KLog.p(allStats.toString());

                    dealWithStats(allStats);
                }
            });
        }

        if (null != subPcWrapper && null != subPcWrapper.pc) {
            subPcWrapper.pc.getStats(rtcStatsReport -> {
                System.out.println(String.format("subscriber rtcStatsReport=%s ", rtcStatsReport));
                subscriberStats = StatsHelper.resolveStats(rtcStatsReport);
                if (null == publisherStats) {
                    // got subscriberStats firstly, append publisherStats later.
                    allStats = subscriberStats;
                } else {
                    // already got publisherStats, append subscriberStats.
                    allStats.audioInboundRtpList.addAll(subscriberStats.audioInboundRtpList);
                    allStats.videoInboundRtpList.addAll(subscriberStats.videoInboundRtpList);
                    allStats.recvAudioTrackList.addAll(subscriberStats.recvAudioTrackList);
                    allStats.recvVideoTrackList.addAll(subscriberStats.recvVideoTrackList);
                    allStats.encoderList.addAll(subscriberStats.encoderList);
                    allStats.decoderList.addAll(subscriberStats.decoderList);

                    // both publisherStats and subscriberStats got, complete.
                    publisherStats = null;
                    subscriberStats = null;
                    KLog.p(allStats.toString());

//                    dealWithStats(allStats);
                }
            });
        }

    }


    private void createPeerConnectionFactory() {

        executor.execute(() -> {
            if (null != factory){
                KLog.p(KLog.ERROR, "Factory exists!");
                return;
            }

            PeerConnectionFactory.initialize(PeerConnectionFactory.InitializationOptions.builder(context).createInitializationOptions());

            PeerConnectionFactoryConfig factoryConfig = new PeerConnectionFactoryConfig(Arrays.asList(VIDEO_CODEC_VP8, VIDEO_CODEC_H264_HIGH), config.enableVideoCodecHwAcceleration);

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

            adm.release();
        });

    }


    private void destroyPeerConnectionFactory(){
        executor.execute(() -> {
            if (null == factory) {
                KLog.p(KLog.ERROR, "Factory not exists!");
                return;
            }
            factory.dispose();
            factory = null;
        });
    }


    private void createPeerConnectionWrapper() {
        /* 创建4个PeerConnectionWrapper
         * NOTE：一个peerconnection可以处理多路码流，收发均可。
         * 但业务要求主流发/收、辅流发/收4种情形分别用单独的peerconnect处理，故此处创建4个。
         * */
        PeerConnectionConfig pcConfig = new PeerConnectionConfig(
                config.videoWidth,
                config.videoHeight,
                config.videoFps,
                config.videoMaxBitrate,
                config.videoCodec,
                config.audioStartBitrate,
                config.audioCodec
        );
        KLog.p(config.toString());

        pubPcWrapper = new PeerConnectionWrapper(CommonDef.CONN_TYPE_PUBLISHER, pcConfig, new SDPObserver(CommonDef.CONN_TYPE_PUBLISHER));
        subPcWrapper = new PeerConnectionWrapper(CommonDef.CONN_TYPE_SUBSCRIBER, pcConfig, new SDPObserver(CommonDef.CONN_TYPE_SUBSCRIBER));
        assPubPcWrapper = new PeerConnectionWrapper(CommonDef.CONN_TYPE_ASS_PUBLISHER, pcConfig, new SDPObserver(CommonDef.CONN_TYPE_ASS_PUBLISHER));
        assSubPcWrapper = new PeerConnectionWrapper(CommonDef.CONN_TYPE_ASS_SUBSCRIBER, pcConfig, new SDPObserver(CommonDef.CONN_TYPE_ASS_SUBSCRIBER));

        executor.execute(() -> {
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

            PeerConnection pubPc = Objects.requireNonNull(factory.createPeerConnection(rtcConfig, new PCObserver(CommonDef.CONN_TYPE_PUBLISHER)));
            PeerConnection subPc = Objects.requireNonNull(factory.createPeerConnection(rtcConfig, new PCObserver(CommonDef.CONN_TYPE_SUBSCRIBER)));
            PeerConnection assPubPc = Objects.requireNonNull(factory.createPeerConnection(rtcConfig, new PCObserver(CommonDef.CONN_TYPE_ASS_PUBLISHER)));
            PeerConnection assSubPc = Objects.requireNonNull(factory.createPeerConnection(rtcConfig, new PCObserver(CommonDef.CONN_TYPE_ASS_SUBSCRIBER)));

            synchronized (pcWrapperLock) {
                if (null != pubPcWrapper) pubPcWrapper.setPeerConnection(pubPc);
                if (null != subPcWrapper) subPcWrapper.setPeerConnection(subPc);
                if (null != assPubPcWrapper) assPubPcWrapper.setPeerConnection(assPubPc);
                if (null != assSubPcWrapper) assSubPcWrapper.setPeerConnection(assSubPc);
            }

        });

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
            if (!targets.contains(target)) {
                KLog.p("add Display %s to sink %s", target, name);
                targets.add(target);
            }
        }

        synchronized boolean delTarget(Display target) {
            KLog.p("delete Display %s from sink %s", target, name);
            return targets.remove(target);
        }

        synchronized void release() {
            // display会在外部被释放，不需要在此处释放。
//            for (Display display : targets){
//                display.destroy();
//            }
            targets.clear();
        }

    }

    public Conferee getConferee(String confereeId){
        return conferees.get(confereeId);
    }

    private Conferee getConferee(int mcuId, int terId){
        for (Conferee conferee : conferees.values()){
            if (conferee.mcuId==mcuId && conferee.terId==terId){
                return conferee;
            }
        }
        return null;
    }

    public Conferee getConfereeByStreamId(String streamId){
        Stream stream = streams.get(streamId);
        if (null == stream){
            return null;
        }
        return conferees.get(stream.ownerId);
    }

    public Stream getStream(String streamId){
        return streams.get(streamId);
    }

    public Stream getStream(String confereeId, boolean bAudio, boolean bAss){
        for (Stream stream : streams.values()){
            if (stream.ownerId.equals(confereeId) && bAudio == stream.bAudio && stream.bAss == bAss){
                return stream;
            }
        }
        return null;
    }

    public Display getDisplay(String confereeId){
        for (Display display : displaySet){
            if ((confereeId == display.confereeId) || (confereeId != null && confereeId.equals(display.confereeId))){
                return display;
            }
        }
        return null;
    }

    public Display getDisplayByStreamId(String streamId){
        Stream stream = streams.get(streamId);
        if (null == stream){
            return null;
        }
        return getDisplay(stream.ownerId);
    }


    /**
     * 创建Display。
     * */
    public Display createDisplay(){
        Display display =  new Display(context);
        displaySet.add(display);
        KLog.p("create display %s", display);
        return display;
    }

    /**
     * 销毁display
     * */
    public void releaseDisplay(Display display){
        KLog.p("release display %s", display);
        if (displaySet.remove(display)){
            display.destroy();
        }else{
            KLog.p(KLog.ERROR, "wired, this display is not created by me!");
        }
    }


    /**
     * 用于展示与会方画面的控件。
     * */
    public final static class Display extends SurfaceViewRenderer{
        private boolean enabled = true;
        private String confereeId;
        private List<TextDecoration> textDecorations = new ArrayList<>();
        private List<PicDecoration> picDecorations = new ArrayList<>();
        private boolean bShowVoiceActivatedDeco;
        private boolean bShowCameraDisabledDeco;
        private boolean bShowAudioTerminalDeco;
        private boolean bShowStreamLostDeco;

        //===== 上面的内容是swap/copy时需要拷贝的内容，下面的则不需要 ========

        public static final int POS_LEFTTOP = 1;
        public static final int POS_LEFTBOTTOM = 2;
        public static final int POS_RIGHTTOP = 3;
        public static final int POS_RIGHTBOTTOM = 4;
        private int displayWidth;
        private int displayHeight;
        private RectF voiceActivatedDeco = new RectF();
        private static Paint voiceActivatedDecoPaint = new Paint();
        private static Bitmap videoCaptureDisabledDeco;
        private static Bitmap audioConfereeDeco;
        private static Bitmap streamLostDeco;
        static{
            voiceActivatedDecoPaint.setStyle(Paint.Style.STROKE);
            voiceActivatedDecoPaint.setStrokeWidth(5);
            voiceActivatedDecoPaint.setColor(Color.GREEN);
        }

        private Handler handler = getHandler();

        private Display(Context context) {
            super(context);
            init(instance.eglBase.getEglBaseContext(), null);
            setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL);
            setEnableHardwareScaler(true);
            setWillNotDraw(false);
        }


        @Override
        public String toString() {
            return "Display{hash=" + hashCode()+
                    " enabled=" + enabled +
                    ", confereeId='" + confereeId + '\'' +
                    ", textDecorations=" + textDecorations +
                    ", picDecorations=" + picDecorations +
                    ", displayWidth=" + displayWidth +
                    ", displayHeight=" + displayHeight +
                    '}';
        }




        /**
         * 设置语音激励装饰。
         * 当该Display对应的与会成员讲话音量最大时，该装饰会展示。
         * 该装饰是一个围在画面周围的边框，用户通过该接口设置该边框线条的粗细以及颜色。
         * @param strokeWidth 线条粗细
         * @param color 线条颜色
         * */
        public static void setVoiceActivatedDecoration(int strokeWidth, int color){
            voiceActivatedDecoPaint.reset();
            voiceActivatedDecoPaint.setStrokeWidth(strokeWidth);
            voiceActivatedDecoPaint.setColor(color);
        }

        private void showVoiceActivatedDeco(boolean bShow){
            handler.removeCallbacks(this::hideVoiceActivatedDecoration);
            bShowVoiceActivatedDeco = bShow;
            invalidate();
        }
        private void hideVoiceActivatedDecoration(){
            bShowVoiceActivatedDeco = false;
            invalidate();
        }

        /**
         * 设置关闭摄像头采集时本地画面对应的Display展示的图片。
         * */
        public static void setCameraDisabledDecoration(Bitmap bitmap){
            videoCaptureDisabledDeco = bitmap;
        }
        private void showCameraDisabledDeco(boolean bShow){
            bShowCameraDisabledDeco = bShow;
            invalidate();
        }

        /**
         * 设置音频入会方对应的Display展示的图片。
         * */
        public static void setAudioConfereeDecoration(Bitmap bitmap){
            audioConfereeDeco = bitmap;
        }
        private void showAudioConfereeDeco(boolean bShow){
            bShowAudioTerminalDeco = bShow;
            invalidate();
        }

        /**
         * 设置Display无视频源时展示的图片
         * */
        public static void setStreamLostDecoration(Bitmap bitmap){
            streamLostDeco = bitmap;
        }
        private void showStreamLostDeco(boolean bShow){
            bShowStreamLostDeco = bShow;
            invalidate();
        }

        /**
         * 将Display绑定到与会方。
         * 一个Display只会绑定到一个与会方；
         * 多个Display可以绑定到同一与会方；
         * 若一个Display已经绑定到某个与会方，则该绑定会先被解除，然后建立新的绑定关系；
         * NOTE:
         * Display的内容包括码流和Decoration，此方法只影响码流展示不会改变Decoration；
         * 若绑定到null则该Display不会展示码流，相当于于解除绑定；
         *
         * 对于画面内容交换的使用场景可以使用便捷方法{@link #swapContent(Display)}；
         * 对于画面内容拷贝的使用场景可以使用便捷方法{@link #copyContentFrom(Display)}；
         * @param confereeId 与会方Id。
         * @return true，若confereeId对应的conferee存在或者confereeId为null；否则返回false。
         * */
        public boolean bind(String confereeId){
            Map<String, Conferee> conferees = instance.conferees;
            Map<String, Stream> streams = instance.streams;
            Map<String, ProxyVideoSink> videoSinks = instance.videoSinks;

            // 解绑原来的
            Conferee boundConferee = conferees.get(this.confereeId);
            if (null != boundConferee){
                Stream boundVideoStream = instance.getStream(boundConferee.id, false, boundConferee.bAssStream);
                if (null != boundVideoStream){
                    ProxyVideoSink boundSink = videoSinks.get(boundVideoStream.streamId);
                    if (null != boundSink){
                        boundSink.delTarget(this);
                        KLog.p("delete display %s from sink %s", this, boundSink);
                    }
                }
            }

            // 重新绑定
            Conferee toBindConferee = conferees.get(confereeId);
            if (null != toBindConferee){
                Stream toBindVideoStream = instance.getStream(toBindConferee.id, false, toBindConferee.bAssStream);
                if (null != toBindVideoStream){
                    ProxyVideoSink toBindSink = videoSinks.get(toBindVideoStream.streamId);
                    if (null != toBindSink) {
                        toBindSink.addTarget(this);
                        KLog.p("add display %s into sink %s", this, toBindSink);
                    }
                }
            }

            KLog.p("bind display %s to conferee %s", this, confereeId);

            this.confereeId = confereeId;

            return null != toBindConferee || null == confereeId;

        }


        /**
         * 交换两个display的内容。
         * NOTE: 内容包括码流、Decoration。
         * @param otherDisplay 要交换的display。
         * */
        public void swapContent(@NonNull Display otherDisplay){
            KLog.p("swap display %s and display %s", this, otherDisplay);

            // 切换使能状态
            boolean myEnable = enabled;
            enabled = otherDisplay.enabled;
            otherDisplay.enabled = myEnable;

            // 切换绑定的与会方
            String myConfereeId = confereeId;
            bind(otherDisplay.confereeId);
            otherDisplay.bind(myConfereeId);

            // 切换贴在display上面的decoration
            List<Display.TextDecoration> myTextDecorationList = textDecorations;
            textDecorations = otherDisplay.textDecorations;
            otherDisplay.textDecorations = myTextDecorationList;
            List<Display.PicDecoration> myPicDecorationList = picDecorations;
            picDecorations = otherDisplay.picDecorations;
            otherDisplay.picDecorations = myPicDecorationList;

            // 切换静态图片decoration状态
            boolean tmp = bShowCameraDisabledDeco;
            bShowCameraDisabledDeco = otherDisplay.bShowCameraDisabledDeco;
            otherDisplay.bShowCameraDisabledDeco = tmp;
            tmp = bShowVoiceActivatedDeco;
            bShowVoiceActivatedDeco = otherDisplay.bShowVoiceActivatedDeco;
            otherDisplay.bShowVoiceActivatedDeco = tmp;
            tmp = bShowAudioTerminalDeco;
            bShowAudioTerminalDeco = otherDisplay.bShowAudioTerminalDeco;
            otherDisplay.bShowAudioTerminalDeco = tmp;
            tmp = bShowStreamLostDeco;
            bShowStreamLostDeco = otherDisplay.bShowStreamLostDeco;
            otherDisplay.bShowStreamLostDeco = tmp;

            // 刷新display
            adjustDecoration();
            otherDisplay.adjustDecoration();
            KLog.p("after swap: this display %s, other display %s", this, otherDisplay);
        }

        /**
         * 从另一个display拷贝内容
         * NOTE: 内容包括码流、Decoration。
         * */
        public void copyContentFrom(@NonNull Display src){
            enabled = src.enabled;
            bind(src.confereeId);
            clearAllDeco();
            addText(src.getAllText());
            addPic(src.getAllPic());
            bShowCameraDisabledDeco = src.bShowCameraDisabledDeco;
            bShowVoiceActivatedDeco = src.bShowVoiceActivatedDeco;
            bShowAudioTerminalDeco = src.bShowAudioTerminalDeco;
            bShowStreamLostDeco = src.bShowStreamLostDeco;
            adjustDecoration();
        }

        /**
         * 清空display内容
         * NOTE: 内容包括码流、Decoration。
         * */
        public void clearContent(){
            bind(null);
            clearAllDeco();
        }

        /**
         * 销毁display
         * */
        private void destroy(){
            clearContent();
            super.release();
            KLog.p("display %s destroyed", this);
        }


        /**
         * 获取该display绑定的与会方ID
         * */
        public String getConfereeId(){
            return confereeId;
        }

        /**
         * 获取该display绑定的与会方信息
         * */
        public Conferee getConferee(){
            return instance.getConferee(confereeId);
        }



        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.surfaceChanged(holder, format, width, height);
            displayWidth = width;
            displayHeight = height;
            KLog.p("displayWidth = %s, displayHeight=%s", displayWidth, displayHeight);
            adjustDecoration();
        }

        long ts = System.currentTimeMillis();
        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            if (System.currentTimeMillis() - ts > 5000){
                KLog.p("onDraw, displayWidth = %s, displayHeight=%s", displayWidth, displayHeight);
            }

            if (bShowCameraDisabledDeco){
                if (null != videoCaptureDisabledDeco) {
                    canvas.drawBitmap(videoCaptureDisabledDeco, 0, 0, null);
                }else{
                    canvas.drawColor(Color.BLACK);
                }
            }else if (bShowAudioTerminalDeco){
                if (null != audioConfereeDeco) {
                    canvas.drawBitmap(audioConfereeDeco, 0, 0, null);
                }else{
                    canvas.drawColor(Color.BLACK);
                }
            }else if (bShowStreamLostDeco){
                if (null != streamLostDeco) {
                    canvas.drawBitmap(streamLostDeco, 0, 0, null);
                }else{
                    canvas.drawColor(Color.BLACK);
                }
            }

            if (bShowVoiceActivatedDeco){
                canvas.drawRect(voiceActivatedDeco, voiceActivatedDecoPaint);
//                sessionHandler.postDelayed(this::hideVoiceActivatedDecoration, 2000);
            }

            for (PicDecoration deco : picDecorations){
                canvas.drawBitmap(deco.pic, deco.matrix, deco.paint);
            }

            for (TextDecoration deco : textDecorations){
                if (System.currentTimeMillis() - ts > 5000){
                    KLog.p("drawText(%s, %s, %s, %s) for display %s", deco.text, deco.x, deco.y, deco.paint.getTextSize(), this);
                }
                canvas.drawText(deco.text, deco.x, deco.y, deco.paint);
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
         * */
        public void addText(TextDecoration decoration){
            KLog.p(decoration.toString());
            decoration.adjust(displayWidth, displayHeight);
            textDecorations.add(decoration);
            invalidate();
        }

        /**
         * 添加文字
         * */
        public void addText(List<TextDecoration> decoList){
            for (TextDecoration deco : decoList){
                deco.adjust(displayWidth, displayHeight);
            }
            textDecorations.addAll(decoList);
            invalidate();
        }

        /**
         * 清空文字
         * */
        public void clearText(){
            textDecorations.clear();
            invalidate();
        }

        /**
         * 获取所有文字
         * */
        public List<TextDecoration> getAllText(){
            return textDecorations;
        }

        /**
         * 添加图片
         * */
        public void addPic(PicDecoration decoration){
            KLog.p(decoration.toString());
            decoration.adjust(displayWidth, displayHeight);
            picDecorations.add(decoration);
            invalidate();
        }
        /**
         * 添加图片
         * */
        public void addPic(List<PicDecoration> decoList){
            for (PicDecoration deco : decoList){
                deco.adjust(displayWidth, displayHeight);
            }
            picDecorations.addAll(decoList);
            invalidate();
        }

        /**
         * 清空图片
         * */
        public void clearPic(){
            picDecorations.clear();
            invalidate();
        }

        /**
         * 获取所有图片
         * */
        public List<PicDecoration> getAllPic(){
            return picDecorations;
        }

        /**
         * 清空所有deco，如图片、文字等
         * */
        public void clearAllDeco(){
            clearText();
            clearPic();
            bShowCameraDisabledDeco = false;
            bShowVoiceActivatedDeco = false;
            bShowAudioTerminalDeco =  false;
            bShowStreamLostDeco =  false;
        }


        private void adjustDecoration(){
            for (TextDecoration deco : textDecorations){
                deco.adjust(displayWidth, displayHeight);
            }
            for (PicDecoration deco : picDecorations){
                deco.adjust(displayWidth, displayHeight);
            }
            voiceActivatedDeco.set(0, 0, displayWidth, displayHeight);
            invalidate();
        }

        public static final class TextDecoration extends Decoration{
            public String text;     // 要展示的文字
            private int textSize;
            public TextDecoration(@NonNull String text, int textSize, int color, int dx, int dy, int refPos, int w, int h) {
                super(dx, dy, refPos, w, h);
                this.text = text;
                this.textSize = textSize;
                paint.setStyle(Paint.Style.STROKE);
                paint.setColor(color);
            }

            protected void adjust(int width, int height){
                if (width<=0 || height<=0){
                    return;
                }
                super.adjust(width, height);
                float size = textSize *Math.min(ratioW, ratioH);
                paint.setTextSize(size);
                if (POS_LEFTBOTTOM == refPos){
                    y -= size;
                }else if (POS_RIGHTTOP == refPos){
                    x -= size * text.length();
                }else if (POS_RIGHTBOTTOM == refPos){
                    x -= size * text.length();
                    y -= size;
                }
                KLog.p(toString());
            }

            @Override
            public String toString() {
                return "TextDecoration{" +
                        "text='" + text + '\'' +
                        ", textSize=" + textSize +
                        ", dx=" + dx +
                        ", dy=" + dy +
                        ", refPos=" + refPos +
                        ", w=" + w +
                        ", h=" + h +
                        ", paint=" + paint +
                        ", x=" + x +
                        ", y=" + y +
                        ", originX=" + originX +
                        ", originY=" + originY +
                        ", ratioW=" + ratioW +
                        ", ratioH=" + ratioH +
                        '}';
            }
        }

        public static final class PicDecoration extends Decoration{
            public Bitmap pic;     // 要展示的图片
            private Matrix matrix = new Matrix();
            public PicDecoration(@NonNull Bitmap pic, int dx, int dy, int refPos, int w, int h) {
                super(dx, dy, refPos, w, h);
                this.pic = pic;
                paint.setStyle(Paint.Style.STROKE);
            }

            protected void adjust(int width, int height){
                if (width<=0 || height<=0){
                    return;
                }
                super.adjust(width, height);
                float scaleFactor = Math.min(ratioW, ratioH);
                matrix.reset();
                matrix.postScale(scaleFactor, scaleFactor, originX, originY);
                int picW = (int) (pic.getWidth()*scaleFactor);
                int picH = (int) (pic.getHeight()*scaleFactor);
                if (POS_LEFTBOTTOM == refPos){
                    y -= picH;
                }else if (POS_RIGHTTOP == refPos){
                    x -= picW;
                }else if (POS_RIGHTBOTTOM == refPos){
                    x -= picW;
                    y -= picH;
                }
                matrix.postTranslate(x, y);
                KLog.p(toString());
            }

            @Override
            public String toString() {
                return "PicDecoration{" +
                        "pic=" + pic +
                        ", matrix=" + matrix +
                        ", dx=" + dx +
                        ", dy=" + dy +
                        ", refPos=" + refPos +
                        ", w=" + w +
                        ", h=" + h +
                        ", paint=" + paint +
                        ", x=" + x +
                        ", y=" + y +
                        ", originX=" + originX +
                        ", originY=" + originY +
                        ", ratioW=" + ratioW +
                        ", ratioH=" + ratioH +
                        '}';
            }
        }

        public static class Decoration{
            public int dx;          // 参照pos的x方向距离（UCD标注的）
            public int dy;          // 参照pos的y方向距离（UCD标注的）
            public int refPos;      /** dx, dy参照的位置。如取值{@link #POS_LEFTBOTTOM}则dx表示距离左下角的x方向距离*/
            public int w;           // 该deco所在画面的宽（UCD标注的）
            public int h;           // 该deco所在画面的高（UCD标注的）
            protected Paint paint;

            protected int x;          // 锚点x坐标（根据UCD标注结合Display的状态计算得出）
            protected int y;          // 锚点y坐标（根据UCD标注结合Display的状态计算得出）
            protected int originX;
            protected int originY;
            protected float ratioW;
            protected float ratioH;

            protected Decoration(int dx, int dy, int refPos, int w, int h) {
                this.dx = dx;
                this.dy = dy;
                this.refPos = refPos;
                this.w = w;
                this.h = h;
                this.paint = new Paint();
            }

            protected void adjust(int width, int height){
                ratioW = width/(float)w;
                ratioH = height/(float)h;
                if (POS_LEFTTOP == refPos){
                    x = Math.round(ratioW * dx);
                    y = Math.round(ratioH * dy);
                    originX = originY = 0;
                }else if (POS_LEFTBOTTOM == refPos){
                    x = Math.round(ratioW * dx);
                    y = Math.round(height - ratioH * dy);
                    originX = 0;
                    originY = height;
                }else if (POS_RIGHTTOP == refPos){
                    x = Math.round(width - ratioW * dx);
                    y = Math.round(ratioH * dy);
                    originX = width;
                    originY = 0;
                }else{
                    x = Math.round(width - ratioW * dx);
                    y = Math.round(height - ratioH * dy);
                    originX = width;
                    originY = height;
                }
                KLog.p("displayW=%s, displayH=%s, ratioW=%s, ratioH=%s, x=%s, y=%s, originX=%s, originY=%s, paint.textSize=%s",
                        width, height, ratioW, ratioH, x, y, originX, originY, paint.getTextSize());
            }

        }

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
         * 然后调用{@link Display#bind(String)} 将Display绑定到与会方以使与会方画面展示在Display上，
         * 如果还需要展示文字图标等可调用{@link Display#addText(Display.TextDecoration)}, {@link Display#addPic(Display.PicDecoration)}
         * */
        void onConfereeJoined(Conferee conferee);
        /**
         * 与会方离会
         * 如果该Display不需要了请调用{@link #releaseDisplay(Display)}销毁；
         * 如果后续要复用则可以不销毁，可以调用{@link Display#clearContent()}清空内容；
         * NOTE: {@link #stopSession()} 会销毁所有Display。用户不能跨Session复用Display，也不需要在stopSession时手动销毁Display。
         * */
        void onConfereeLeft(Conferee conferee);
    }
    private SessionEventListener sessionEventListener;

    /**
     * 与会方信息
     */
    public static class Conferee {
        public String id;
        public int mcuId;
        public int terId;
        public String   e164;
        public String   alias;
        public String   email;
        // 是否为辅流与会方（辅流我们当作特殊的与会方）
        public boolean bAssStream;

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
    }

    public static class Stream {
        public String streamId; // 平台的StreamId，概念上对应的是WebRTC中的TrackId
        public String ownerId; // Conferee.id
        public boolean bAudio;
        public boolean bAss;
        public List<EmMtResolution> supportedResolutionList;       // 流支持的分辨率

        public Stream(String streamId, int mcuId, int terId, boolean bAudio, boolean bAss, List<EmMtResolution> supportedResolutionList) {
            this.streamId = streamId;
            ownerId = Conferee.buildId(mcuId, terId, bAss);
            this.bAudio = bAudio;
            this.bAss = bAss;
            this.supportedResolutionList = supportedResolutionList;
        }
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
                    ", isLocalAudioEnabled=" + isLocalAudioEnabled +
                    ", isRemoteAudioEnabled=" + isRemoteAudioEnabled +
                    ", isLocalVideoEnabled=" + isLocalVideoEnabled +
                    ", isRemoteVideoEnabled=" + isRemoteVideoEnabled +
                    ", bPreferFrontCamera=" + bPreferFrontCamera +
                    '}';
        }
    }



    private class RtcConnectorEventListener implements RtcConnector.SignalingEvents{

        @Override
        public void onGetOfferCmd(int connType, int mediaType) {
            KLog.p("onGetOfferCmd: connType=%s, mediaType=%s", connType, mediaType);
            PeerConnectionWrapper pcWrapper = getPcWrapper(connType);
            if (null == pcWrapper) return;

            pcWrapper.checkSdpState(pcWrapper.Idle);

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
            if (null == pcWrapper) return;

            pcWrapper.checkSdpState(pcWrapper.Idle);
            pcWrapper.setSdpType(pcWrapper.Answer);
            setRemoteDescription(pcWrapper, offerSdp, SessionDescription.Type.OFFER);
            pcWrapper.setSdpState(pcWrapper.SettingRemote);
        }

        @Override
        public void onSetAnswerCmd(int connType, String answerSdp) {
            KLog.p("connType=%s", connType);
            PeerConnectionWrapper pcWrapper = getPcWrapper(connType);
            if (null == pcWrapper) return;

            pcWrapper.checkSdpState(pcWrapper.Sending);
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
            if (null == pcWrapper) return;

            pcWrapper.checkSdpState(pcWrapper.Idle);
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
        }

        @Override
        public void onCodecMuteCmd(boolean bMute) {
            PeerConnectionWrapper pcWrapper = getPcWrapper(CommonDef.CONN_TYPE_PUBLISHER);
            if (null != pcWrapper) {
                pcWrapper.setLocalAudioEnable(!bMute);
            }
        }

        @Override
        public void onUnPubCmd(int connType, int mediaType) {
            PeerConnectionWrapper pcWrapper = getPcWrapper(connType);
            if (null == pcWrapper) return;

            // 删除取消发布的媒体轨道
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
                if (null == pcWrapper) return;

                KLog.p("create local sdp success: type=%s", pcWrapper.sdpType);
                pcWrapper.checkSdpState(pcWrapper.Creating);
                if (pcWrapper.isSdpType(pcWrapper.FingerPrintOffer)){
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
                if (null == pcWrapper) return;

                pcWrapper.checkSdpState(pcWrapper.SettingLocal, pcWrapper.SettingRemote);
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
    private static final String LOCAL_VIDEO_TRACK_ID = STREAM_ID+"-v0";
    private static final String LOCAL_ASS_TRACK_ID = STREAM_ID+"-ass";
    private static final String LOCAL_AUDIO_TRACK_ID = STREAM_ID+"-a0";

    /**
     * PeerConnection包装类
     * */
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
                videoCapturer.startCapture(config.videoWidth, config.videoHeight, config.videoFps);
                String localVideoTrackId = videoCapturer instanceof WindowCapturer ? LOCAL_ASS_TRACK_ID : LOCAL_VIDEO_TRACK_ID;
                localVideoTrack = factory.createVideoTrack(localVideoTrackId, videoSource);
                localVideoTrack.setEnabled(config.isLocalVideoEnabled);
                ProxyVideoSink localVideoSink = new ProxyVideoSink(localVideoTrackId);
                localVideoTrack.addSink(localVideoSink);

                sessionHandler.post(() -> {
                    String kdStreamId = localVideoTrackId;
                    kdStreamId2RtcTrackIdMap.put(kdStreamId, localVideoTrackId);
                    videoSinks.put(kdStreamId, localVideoSink);

                    // 将本地track添加到流集合
                    if (null != myConferee){
                        streams.put(localVideoTrackId,
                                new Stream(localVideoTrackId, myConferee.mcuId, myConferee.terId,false, localVideoTrackId.equals(LOCAL_ASS_TRACK_ID),
                                        Collections.singletonList(EmMtResolution.emMtHD1080p1920x1080_Api) // XXX 实际可能并非1080P，比如辅流是截取的window宽高
                                )
                        );
                    }else{
                        KLog.p(KLog.ERROR, "what's wrong? myself not join conferee yet !?");
                    }

                    // 重新绑定Display，因为之前绑定时Track还没上来。
                    Display myDisplay = getDisplay(myConferee.id);
                    if (null != myDisplay) {
                        myDisplay.bind(myConferee.id);
                        myDisplay.showAudioConfereeDeco(false);
                        myDisplay.showStreamLostDeco(false);
                    }else{
                        KLog.p(KLog.ERROR, "user not bind a display to myConferee");
                    }

                });

                if (instance.config.enableSimulcast){
                    RtpTransceiver.RtpTransceiverInit transceiverInit = new RtpTransceiver.RtpTransceiverInit(
                            RtpTransceiver.RtpTransceiverDirection.SEND_ONLY,
                            Collections.singletonList(STREAM_ID),
                            createEncodingList(false)
                    );
                    RtpTransceiver transceiver = pc.addTransceiver(localVideoTrack, transceiverInit);
                    videoSender = transceiver.getSender();
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
                }else{
                    videoSender = pc.addTrack(localVideoTrack, Collections.singletonList(STREAM_ID));
                }

            });
        }

        void removeVideoTrack(){
            executor.execute(() -> {
                pc.removeTrack(videoSender);

                sessionHandler.post(() -> {
                    String trackId = localVideoTrack.id();
                    String kdStreamId = kdStreamId2RtcTrackIdMap.inverse().get(trackId);
                    KLog.p("localVideoTrack %s, %s removed ", kdStreamId, trackId);
                    videoSinks.remove(kdStreamId);
                    kdStreamId2RtcTrackIdMap.remove(kdStreamId);

                    // 将本地track从流集合删除 // TODO 看是否能收到StreamLeft
//                    if (null != myConferee) {
//                        streams.put(localVideoTrackId,
//                                new Stream(localVideoTrackId, myConferee.mcuId, myConferee.terId, false, videoCapturer instanceof WindowCapturer,
//                                        Collections.singletonList(EmMtResolution.emMtHD1080p1920x1080_Api) // XXX 实际可能并非1080P，比如辅流是截取的window宽高
//                                )
//                        );
//                    } else {
//                        KLog.p(KLog.ERROR, "what's wrong? myself not join conferee yet !?");
//                    }
                });

            });
        }


        void createRemoteVideoTrack(String mid, VideoTrack track){
            String streamId = mid2KdStreamIdMap.get(mid);
            KLog.p("mid=%s, streamId=%s", mid, streamId);
            if (null == streamId) {
                KLog.p(KLog.ERROR, "no register stream for mid %s in signaling progress(see onSetOfferCmd)", mid);
                return;
            }
            kdStreamId2RtcTrackIdMap.put(track.id(), streamId);

            executor.execute(() -> {
                KLog.p("stream %s added", streamId);
                remoteVideoTracks.put(streamId, track);
                track.setEnabled(config.isRemoteVideoEnabled);
                ProxyVideoSink videoSink = new ProxyVideoSink(streamId);
                track.addSink(videoSink);

                KLog.p("rtcstreamId=%s, kdstreamId=%s", track.id(), streamId);
                sessionHandler.post(() -> {
                    videoSinks.put(streamId, videoSink);
                    Stream remoteStream = getStream(streamId);
                    if (null == remoteStream) {
                        KLog.p(KLog.ERROR, "no such stream %s in subscribed stream list( see Msg.StreamLeft/Msg.StreamJoined/Msg.CurrentStreamList branch in method onNtf)", streamId);
                        return;
                    }
                    // 重新绑定Display，因为之前绑定时Track还没上来。
                    Conferee conferee = getConfereeByStreamId(remoteStream.streamId);
                    if (null != conferee) {
                        Display display = getDisplay(conferee.id);
                        if (null != display) {
                            display.bind(conferee.id);
                            display.showAudioConfereeDeco(false);
                            display.showStreamLostDeco(false);
                        } else {
                            KLog.p(KLog.ERROR, "user not bind a display to myConferee");
                        }
                    } else {
                        KLog.p(KLog.ERROR, "what's wrong? owner of stream %s not join conferee yet !?", remoteStream.streamId);
                    }
                });

            });
        }

        void removeRemoteVideoTrack(String kdStreamId){
            executor.execute(() -> {
                for (String streamId : remoteVideoTracks.keySet()) {
                    if (streamId.equals(kdStreamId)) {
                        VideoTrack track = remoteVideoTracks.remove(streamId);
//                        track.dispose();
                        KLog.p("stream %s removed", streamId);

                        sessionHandler.post(() -> {
                            videoSinks.remove(streamId);
                            kdStreamId2RtcTrackIdMap.remove(streamId);

                            Conferee conferee = getConfereeByStreamId(streamId);
                            if (null != conferee) {
                                Display display = getDisplay(conferee.id);
                                if (null != display) {
                                    // VideoTrack被删除时显示音频图标
                                    display.showAudioConfereeDeco(true);
                                }
                            }
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
                String localAudioTrackId = LOCAL_AUDIO_TRACK_ID;
                localAudioTrack = factory.createAudioTrack(localAudioTrackId, audioSource);
                localAudioTrack.setEnabled(config.isLocalAudioEnabled);
                audioSender = pc.addTrack(localAudioTrack, Collections.singletonList(STREAM_ID));

                sessionHandler.post(() -> kdStreamId2RtcTrackIdMap.put(localAudioTrackId, localAudioTrackId));
            });
        }

        void removeAudioTrack(){
            executor.execute(() -> {
                pc.removeTrack(audioSender);

                sessionHandler.post(() -> {
                    String trackId = localAudioTrack.id();
                    String kdStreamId = kdStreamId2RtcTrackIdMap.inverse().get(trackId);
                    KLog.p("localAudioTrack %s, %s removed ", kdStreamId, trackId);
                    kdStreamId2RtcTrackIdMap.remove(kdStreamId);

                    // 将本地track从流集合删除 // TODO 看是否能收到StreamLeft
//                    if (null != myConferee) {
//                        streams.put(localVideoTrackId,
//                                new Stream(localVideoTrackId, myConferee.mcuId, myConferee.terId, false, videoCapturer instanceof WindowCapturer,
//                                        Collections.singletonList(EmMtResolution.emMtHD1080p1920x1080_Api) // XXX 实际可能并非1080P，比如辅流是截取的window宽高
//                                )
//                        );
//                    } else {
//                        KLog.p(KLog.ERROR, "what's wrong? myself not join conferee yet !?");
//                    }
                });

            });
        }



        void createRemoteAudioTrack(String mid, AudioTrack track){
            String streamId = mid2KdStreamIdMap.get(mid);
            KLog.p("mid=%s, streamId=%s", mid, streamId);
            if (null == streamId) {
                KLog.p(KLog.ERROR, "no register stream for mid %s in signaling progress(see onSetOfferCmd)", mid);
                return;
            }
            Stream remoteStream = getStream(streamId);
            if (null == remoteStream) {
                KLog.p(KLog.ERROR, "no such stream %s in subscribed stream list( see Msg.StreamLeft/Msg.StreamJoined/Msg.CurrentStreamList branch in method onNtf)", streamId);
                return;
            }
            kdStreamId2RtcTrackIdMap.put(track.id(), streamId);

            executor.execute(() -> {
                track.setEnabled(config.isRemoteAudioEnabled);
                remoteAudioTracks.put(streamId, track);
            });
        }

        void removeRemoteAudioTrack(String kdStreamId){
            executor.execute(() -> {
                for (String streamId : remoteAudioTracks.keySet()) {
                    if (streamId.equals(kdStreamId)) {
                        AudioTrack track = remoteAudioTracks.remove(streamId);
//                        track.dispose();
                        sessionHandler.post(() -> kdStreamId2RtcTrackIdMap.inverse().remove(streamId));
                        KLog.p("stream %s removed", streamId);
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
                String trackId = localAudioTrack.id();
                sessionHandler.post(() -> kdStreamId2RtcTrackIdMap.remove(trackId));

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

        void checkSdpState(int... sdpStates) {
            for (int state : sdpStates){
                if (state == this.sdpState){
                    return;
                }
            }
            throw new RuntimeException("invalid sdp sate, expect "+sdpStates+" but current is "+this.sdpState);
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


        boolean isRemoteAudioEnabled(){
            return config.isRemoteAudioEnabled;
        }

        void setRemoteAudioEnable(boolean bEnable){
            config.isRemoteAudioEnabled = bEnable;
            executor.execute(() -> {
                for (AudioTrack audioTrack : remoteAudioTracks.values()) {
                    audioTrack.setEnabled(bEnable);
                }
            });
        }

        boolean isLocalAudioEnabled(){
            return config.isLocalAudioEnabled;
        }

        void setLocalAudioEnable(boolean bEnable){
            config.isLocalAudioEnabled = bEnable;
            executor.execute(() -> {
                if (localAudioTrack != null) {
                    localAudioTrack.setEnabled(bEnable);
                }
            });
        }


        boolean isRemoteVideoEnabled(){
            return config.isRemoteVideoEnabled;
        }

        void setRemoteVideoEnable(boolean bEnable){
            config.isRemoteVideoEnabled = bEnable;
            executor.execute(() -> {
                for (VideoTrack videoTrack : remoteVideoTracks.values()) {
                    videoTrack.setEnabled(bEnable);
                }
            });
        }

        boolean isLocalVideoEnabled(){
            return config.isLocalVideoEnabled;
        }

        void setLocalVideoEnable(boolean bEnable){
            config.isLocalVideoEnabled = bEnable;
            executor.execute(() -> {
                if (localVideoTrack != null) {
                    localVideoTrack.setEnabled(bEnable);
                }
            });
        }

        boolean isPreferFrontCamera(){
            return config.bPreferFrontCamera;
        }

        void switchCamera(){
            config.bPreferFrontCamera = !config.bPreferFrontCamera;
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

    private void dealWithStats(StatsHelper.Stats stats){  // TODO 不在这里做。统计信息采集仅负责采集。需要获取数据的主动去获取，而不是在这里通过回调直接穿透。
        if (true){
            return;
        }
        String maxAudioLevelTrackId = null != stats.audioSource ? stats.audioSource.trackIdentifier : null;
        double maxAudioLevel = null != stats.audioSource ? stats.audioSource.audioLevel : 0;
        for (StatsHelper.RecvAudioTrack track : stats.recvAudioTrackList){
            if (track.audioLevel > maxAudioLevel){
                maxAudioLevel = track.audioLevel;
                maxAudioLevelTrackId = track.trackIdentifier;
            }
        }
        if (maxAudioLevel > 0.1){
            // 语音激励
            String maxAudioLevelStreamId = kdStreamId2RtcTrackIdMap.inverse().get(maxAudioLevelTrackId);
            Conferee conferee = getConfereeByStreamId(maxAudioLevelStreamId);
            if (null != conferee){
                Display display = getDisplay(conferee.id);
                if (null != display){
                    display.showVoiceActivatedDeco(true);
                }
            }

        }

        // 通知用户
        if (null != statsListener){
            statsListener.onRtcStats(null/*TODO*/);
        }
    }

}
