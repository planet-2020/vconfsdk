
package com.kedacom.vconf.sdk.webrtc;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.google.protobuf.InvalidProtocolBufferException;
import com.kedacom.kdv.mt.ospconnector.Connector;
import com.kedacom.kdv.mt.ospconnector.IRcvMsgCallback;
import com.kedacom.mt.netmanage.protobuf.BasePB;
import com.kedacom.mt.netmanage.protobuf.EnumPB;
import com.kedacom.mt.netmanage.protobuf.StructCommonPB;
import com.kedacom.mt.netmanage.protobuf.StructConfPB;
import com.kedacom.osp.BodyItem;
import com.kedacom.osp.EmMtOspMsgSys;
import com.kedacom.osp.MtMsg;
import com.kedacom.vconf.sdk.utils.log.KLog;

import org.webrtc.RtpParameters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


class RtcConnector implements IRcvMsgCallback{

	private static final String TAG = "RtcConnector";
	private static final String WEBRTC_NAME = "WEBRTC_NAME";
	private static final short WEBRTC_ID = 142;
	private static final short MTDISPATCH_ID = 107;
	private final Map<String, ICbMsgHandler> cbMsgHandlerMap = new HashMap<>();

	private final long myId =Connector.MAKEIID(WEBRTC_ID, (short)1 );
    private final long myNode = 0;
    private final long dispatchId = Connector.MAKEIID(MTDISPATCH_ID, (short)1 );
	private final long dispatchNode = 0;
    private long rtcServiceId;
    private long rtcServiceNode;

	private Handler handler = new Handler(Looper.getMainLooper());


	private SignalingEvents signalingEvents;
	void setSignalingEventsCallback(SignalingEvents signalingEvents){
		this.signalingEvents = signalingEvents;
	}

	RtcConnector( ) {
		// 注册消息处理方法
		cbMsgHandlerMap.put("Ev_MT_GetOffer_Cmd", this::onGetOfferCmd);
//		cbMsgHandlerMap.put("Ev_MT_GetOffer_Ntf", this::onGetOfferNtf);
		cbMsgHandlerMap.put("Ev_MT_SetOffer_Cmd", this::onSetOfferCmd);
//		cbMsgHandlerMap.put("Ev_MT_GetAnswer_Ntf", this::onGetAnswerNtf);
		cbMsgHandlerMap.put("Ev_MT_SetAnswer_Cmd", this::onSetAnswerCmd);
//		cbMsgHandlerMap.put("Ev_MT_IceCandidate_Ntf", this::onIceCandidateNtf);
		cbMsgHandlerMap.put("Ev_MT_SetIceCandidate_Cmd", this::onSetIceCandidateCmd);
		cbMsgHandlerMap.put("Ev_MT_GetFingerPrint_Cmd", this::onGetFingerPrintCmd);

		if (!CreateOspObject()){
			throw new RuntimeException("CreateOspObject failed!");
		}

		// 向业务组件订阅消息
		MtMsg msg = new MtMsg();
		msg.SetMsgId("Ev_MT_Subscribe_Cmd");
		StructCommonPB.TSubsMsgID.Builder subsMsgBuilder = StructCommonPB.TSubsMsgID.newBuilder();
		subsMsgBuilder.addMsgid("Ev_MT_GetOffer_Cmd");
		subsMsgBuilder.addMsgid("Ev_MT_SetOffer_Cmd");
		subsMsgBuilder.addMsgid("Ev_MT_SetAnswer_Cmd");
		subsMsgBuilder.addMsgid("Ev_MT_SetIceCandidate_Cmd");
        subsMsgBuilder.addMsgid("Ev_MT_GetFingerPrint_Cmd");
		msg.addMsg(subsMsgBuilder.build());
		byte[] abyContent = msg.Encode();
		int ret = Connector.PostOspMsg( EmMtOspMsgSys.Ev_MtOsp_ProtoBufMsg.getnVal(), abyContent, abyContent.length,
				dispatchId, dispatchNode, myId, myNode, 5000 );
		if (0 != ret){
			throw new RuntimeException("Register msg failed!");
		}

	}

	void destroy(){
		Connector.RelaseOspConnector();
	}


	private boolean CreateOspObject() {
		boolean bOspInited = Connector.IsOspConnectorInitd();

		if ( !bOspInited )
		{
			boolean bInited = Connector.OspConnectorInit( true , (short) 25000, "webrtc" );
			if (!bInited){
				KLog.p(KLog.ERROR, "ospinit failed");
				return false;
			}
		}

		boolean bSuccess = Connector.CreateOspConnector(
				WEBRTC_NAME, // app名
				WEBRTC_ID, // appID
				(byte)80, // 优先级
				(short)300, // 消息队列大小
				2000, // 堆栈大小
				0 // 保留参数
		);
		if (!bSuccess){
			KLog.p(KLog.ERROR, "CreateOspConnector failed");
			return false;
		}

		Connector.Setcallback(this);

		return true;
	}


	@Override
	public void Callback( long nEvent, byte[] abyContent, short wLen, long nDstIId, long nDstNode, long nSrcIId, long nSrcNode ) {

		MtMsg mtMsg = new MtMsg();
		
		if ( nEvent == EmMtOspMsgSys.Ev_MtOsp_ProtoBufMsg.getnVal() )
		{
			// PB消息处理
			boolean bRet = mtMsg.Decode( abyContent );
			if ( !bRet ) {
				KLog.p( TAG, " mtmsg decode failed" );
				return;
			}
		}
		else if ( nEvent == EmMtOspMsgSys.EV_MtOsp_OSP_DISCONNECT.getnVal() )
		{
			// OSP断链检测消息处理
			KLog.p(TAG, " mtdispatch disconnected\n" );
			mtMsg.SetMsgId( EmMtOspMsgSys.EV_MtOsp_OSP_DISCONNECT.name() );
		}
		else
		{
			KLog.p(TAG, " dismiss osp message + eventid: " + nEvent);
			return;
		}

		ICbMsgHandler cbMsgHandler = cbMsgHandlerMap.get(mtMsg.GetMsgId());
		if (null == cbMsgHandler){
			KLog.p(KLog.ERROR, "no handler for msg %s", mtMsg.GetMsgId());
			return;
		}

		KLog.p("msg=%s, srcId=%s, srcNode=%s", mtMsg, nSrcIId, nSrcNode);
		cbMsgHandler.onMsg(mtMsg, nSrcIId, nSrcNode);
	}


	private interface ICbMsgHandler{
		void onMsg(MtMsg mtMsg, long nSrcId, long nSrcNode);
	}


	/**
	 * 收到平台“发起signaling”的指示，主动发起signaling流程
	 * */
	private void onGetOfferCmd(MtMsg mtMsg, long nSrcId, long nSrcNode){
		this.rtcServiceId = nSrcId;
		this.rtcServiceNode = nSrcNode;
		BodyItem item0 = mtMsg.GetMsgBody(0);
		BodyItem item1 = mtMsg.GetMsgBody(1);
		int connType;
		int mediaType;
		try {
			connType = BasePB.TU32.parseFrom(item0.getAbyContent()).getValue();
			mediaType = BasePB.TU32.parseFrom(item1.getAbyContent()).getValue();
		} catch (InvalidProtocolBufferException e) {
			e.printStackTrace();
			return;
		}

		KLog.p("<= connType=%s, mediaType=%s", connType, mediaType);

		if (null != signalingEvents) handler.post(() -> signalingEvents.onGetOfferCmd(connType, mediaType));

	}


	/**
	 * FORDEBUG（模拟mtrtcservice）收到mtrtcmp的Ev_MT_GetOffer_Ntf的处理
	 * */
	private void onGetOfferNtf(MtMsg mtMsg, long nSrcId, long nSrcNode){
		BodyItem item0 = mtMsg.GetMsgBody(0);
		BodyItem item1 = mtMsg.GetMsgBody(1);
		BodyItem item2 = mtMsg.GetMsgBody(2);
		int connType;
		String offer;
		StructConfPB.TRtcMedialist rtcMedialist;
		try {
			connType = BasePB.TU32.parseFrom(item0.getAbyContent()).getValue();
			offer = BasePB.TString.parseFrom(item1.getAbyContent()).getValue();
			rtcMedialist = StructConfPB.TRtcMedialist.parseFrom(item2.getAbyContent());
		} catch (InvalidProtocolBufferException e) {
			e.printStackTrace();
			return;
		}

		KLog.p("peerType=%s, rtcMedialist=%s", connType, rtcMedialist);

		// 转发offer给对端
		MtMsg msg = new MtMsg();
        msg.SetMsgId("Ev_MT_SetOffer_Cmd");
        msg.addMsg(BasePB.TU32.newBuilder().setValue(convertConnType(connType)).build());
        msg.addMsg(BasePB.TString.newBuilder().setValue(offer).build());
        msg.addMsg(StructConfPB.TRtcMedialist.newBuilder().mergeFrom(rtcMedialist).build());
		byte[] abyContent = msg.Encode();
		int ret = Connector.PostOspMsg( EmMtOspMsgSys.Ev_MtOsp_ProtoBufMsg.getnVal(), abyContent, abyContent.length,
				myId, myNode, rtcServiceId, rtcServiceNode, 5000 );
        KLog.p("-> trans offer, dstId=%s, dstNode=%s, srcId=%s, srcNode=%s", myId, myNode, rtcServiceId, rtcServiceNode);
		if (0 != ret){
			KLog.p(KLog.ERROR, "PostOspMsg %s failed", mtMsg.GetMsgId());
		}


	}

	private int convertConnType(int peerType){
	    switch (peerType){
            case CommonDef.CONN_TYPE_PUBLISHER:
                return CommonDef.CONN_TYPE_SUBSCRIBER;
            case CommonDef.CONN_TYPE_SUBSCRIBER:
                return CommonDef.CONN_TYPE_PUBLISHER;
            case CommonDef.CONN_TYPE_ASS_PUBLISHER:
                return CommonDef.CONN_TYPE_ASS_SUBSCRIBER;
            case CommonDef.CONN_TYPE_ASS_SUBSCRIBER:
                return CommonDef.CONN_TYPE_ASS_PUBLISHER;
            default:
                return CommonDef.CONN_TYPE_PUBLISHER;
        }
    }

	/**
	 * 收到对端的offer，被动开始signaling流程
	 * */
	private void onSetOfferCmd(MtMsg mtMsg, long nSrcId, long nSrcNode){
		this.rtcServiceId = nSrcId;
		this.rtcServiceNode = nSrcNode;
		BodyItem item0 = mtMsg.GetMsgBody(0);
		BodyItem item1 = mtMsg.GetMsgBody(1);
		BodyItem item2 = mtMsg.GetMsgBody(2);
		int connType;
		String offer;
		StructConfPB.TRtcMedialist rtcMedialist;
		try {
			connType = BasePB.TU32.parseFrom(item0.getAbyContent()).getValue();
			offer = BasePB.TString.parseFrom(item1.getAbyContent()).getValue();
			rtcMedialist = StructConfPB.TRtcMedialist.parseFrom(item2.getAbyContent());
		} catch (InvalidProtocolBufferException e) {
			e.printStackTrace();
			return;
		}

		KLog.p("<= connType=%s, offer=%s, rtcMedialist=%s", connType, offer, rtcMedialist);

		List<TRtcMedia> rtcMedias = new ArrayList<>();
		for (StructConfPB.TRtcMedia tRtcMedia : rtcMedialist.getMediaList()){
			rtcMedias.add(rtcMediaFromPB(tRtcMedia));
		}
		if (null != signalingEvents) handler.post(() -> signalingEvents.onSetOfferCmd(connType, offer, rtcMedias) );

	}

	/**
	 * FORDEBUG （模拟mtrtcservice）收到mtrtcmp的onGetAnswerNtf的处理
	 * */
	private void onGetAnswerNtf(MtMsg mtMsg, long nSrcId, long nSrcNode){
		BodyItem item0 = mtMsg.GetMsgBody(0);
		BodyItem item1 = mtMsg.GetMsgBody(1);
		int connType;
		String answer;
		try {
			connType = BasePB.TU32.parseFrom(item0.getAbyContent()).getValue();
			answer = BasePB.TString.parseFrom(item1.getAbyContent()).getValue();
		} catch (InvalidProtocolBufferException e) {
			e.printStackTrace();
			return;
		}

		KLog.p("peerType=%s", connType);

		// 转发answer到对端
        MtMsg msg = new MtMsg();
        msg.SetMsgId("Ev_MT_SetAnswer_Cmd");
        msg.addMsg(BasePB.TU32.newBuilder().setValue(convertConnType(connType)).build());
        msg.addMsg(BasePB.TString.newBuilder().setValue(answer).build());
		byte[] abyContent = msg.Encode();
		int ret = Connector.PostOspMsg( EmMtOspMsgSys.Ev_MtOsp_ProtoBufMsg.getnVal(), abyContent, abyContent.length,
				myId, myNode, rtcServiceId, rtcServiceNode, 5000 );
		if (0 != ret){
			KLog.p(KLog.ERROR, "PostOspMsg %s failed", mtMsg.GetMsgId());
		}

		KLog.p("-> trans answer");
	}

	/**
	 * 收到对端的answer
	 * */
	private void onSetAnswerCmd(MtMsg mtMsg, long nSrcId, long nSrcNode){
		this.rtcServiceId = nSrcId;
		this.rtcServiceNode = nSrcNode;
		BodyItem item0 = mtMsg.GetMsgBody(0);
		BodyItem item1 = mtMsg.GetMsgBody(1);
		int connType;
		String answer;
		try {
			connType = BasePB.TU32.parseFrom(item0.getAbyContent()).getValue();
			answer = BasePB.TString.parseFrom(item1.getAbyContent()).getValue();
		} catch (InvalidProtocolBufferException e) {
			e.printStackTrace();
			return;
		}

		KLog.p("<= connType=%s, answer=%s", connType, answer);
		if (null != signalingEvents) handler.post(() -> signalingEvents.onSetAnswerCmd(connType, answer) );

	}

	/**
	 * FORDEBUG （模拟mtrtcservice）收到mtrtcmp的Ev_MT_IceCandidate_Ntf的处理
	 * */
	private void onIceCandidateNtf(MtMsg mtMsg, long nSrcId, long nSrcNode){
		BodyItem item0 = mtMsg.GetMsgBody(0);
		BodyItem item1 = mtMsg.GetMsgBody(1);
		BodyItem item2 = mtMsg.GetMsgBody(2);
		BodyItem item3 = mtMsg.GetMsgBody(3);
		int connType;
		String mid;
		int index;
		String candidate;
		try {
			connType = BasePB.TU32.parseFrom(item0.getAbyContent()).getValue();
			mid = BasePB.TString.parseFrom(item1.getAbyContent()).getValue();
			index = BasePB.TU32.parseFrom(item2.getAbyContent()).getValue();
			candidate = BasePB.TString.parseFrom(item3.getAbyContent()).getValue();
		} catch (InvalidProtocolBufferException e) {
			e.printStackTrace();
			return;
		}

		// 转发answer到对端
        MtMsg msg = new MtMsg();
        msg.SetMsgId("Ev_MT_SetIceCandidate_Cmd");
        msg.addMsg(BasePB.TU32.newBuilder().setValue(convertConnType(connType)).build());
        msg.addMsg(BasePB.TString.newBuilder().setValue(mid).build());
        msg.addMsg(BasePB.TU32.newBuilder().setValue(index).build());
        msg.addMsg(BasePB.TString.newBuilder().setValue(candidate).build());
		byte[] abyContent = msg.Encode();
		int ret = Connector.PostOspMsg( EmMtOspMsgSys.Ev_MtOsp_ProtoBufMsg.getnVal(), abyContent, abyContent.length,
				myId, myNode, rtcServiceId, rtcServiceNode, 5000 );
		if (0 != ret){
			KLog.p(KLog.ERROR, "PostOspMsg %s failed", mtMsg.GetMsgId());
		}

		KLog.p("-> trans candidate");
	}


	/**
	 * 收到对端的candidate
	 * */
	private void onSetIceCandidateCmd(MtMsg mtMsg, long nSrcId, long nSrcNode){
		this.rtcServiceId = nSrcId;
		this.rtcServiceNode = nSrcNode;
		BodyItem item0 = mtMsg.GetMsgBody(0);
		BodyItem item1 = mtMsg.GetMsgBody(1);
		BodyItem item2 = mtMsg.GetMsgBody(2);
		BodyItem item3 = mtMsg.GetMsgBody(3);
		int connType;
		String mid;
		int index;
		String candidate;
		try {
			connType = BasePB.TU32.parseFrom(item0.getAbyContent()).getValue();
			mid = BasePB.TString.parseFrom(item1.getAbyContent()).getValue();
			index = BasePB.TU32.parseFrom(item2.getAbyContent()).getValue();
			candidate = BasePB.TString.parseFrom(item3.getAbyContent()).getValue();
		} catch (InvalidProtocolBufferException e) {
			e.printStackTrace();
			return;
		}

		KLog.p("<= connType=%s, mid=%s, index=%s, candidate=%s", connType, mid, index, candidate);
		if (null != signalingEvents) handler.post(() -> signalingEvents.onSetIceCandidateCmd(connType, mid, index, candidate) );
	}


	/**
	 * 获取指纹。
	 * 业务组件定的流程：
	 * 组件先向界面获取FingerPrint，然后向界面推送StreamListNtf，
	 * 界面setPlayPara，然后组件推onSetOfferCmd给界面开始订阅
	 * */
	private void onGetFingerPrintCmd(MtMsg mtMsg, long nSrcId, long nSrcNode){
		this.rtcServiceId = nSrcId;
		this.rtcServiceNode = nSrcNode;
		BodyItem item0 = mtMsg.GetMsgBody(0);
		int connType;
		try {
			connType = BasePB.TU32.parseFrom(item0.getAbyContent()).getValue();
		} catch (InvalidProtocolBufferException e) {
			e.printStackTrace();
			return;
		}

		KLog.p("<= connType=%s", connType);
		if (null != signalingEvents) handler.post(() -> signalingEvents.onGetFingerPrintCmd(connType) );
	}


	private EnumPB.EmMtResolution convertRes(double scale){
		if (0< scale && scale <= 0.25){
			return EnumPB.EmMtResolution.emMt480x270;
		}else if (0.25< scale && scale <= 0.5){
			return EnumPB.EmMtResolution.emMt960x540;
		}else if (0.5< scale && scale <= 0.75){
			return EnumPB.EmMtResolution.emMtV1440x816;
		}else if (0.75< scale && scale < 1){
			return EnumPB.EmMtResolution.emMtHD720p1280x720;
		}else{
			return EnumPB.EmMtResolution.emMtHD1080p1920x1080;
		}
	}

	private TRtcMedia rtcMediaFromPB(StructConfPB.TRtcMedia pbRtcMedia){
		List<RtpParameters.Encoding> encodings = new ArrayList<>();
		for (StructConfPB.TRtcRid tRtcRid : pbRtcMedia.getRidlistList()){
			String rid = tRtcRid.getRid();
//			encodings.add(PeerConnectionClient.createEncoding(rid,
//					1 // encodings不用处理
//			));
		}
		return new TRtcMedia(pbRtcMedia.getStreamid(), pbRtcMedia.getMid());
	}

	private StructConfPB.TRtcMedia rtcMediaToPB(TRtcMedia rtcMedia){
		StructConfPB.TRtcMedia.Builder rtcMediaBuilder = StructConfPB.TRtcMedia.newBuilder();
		if (null != rtcMedia.encodings){
			for (RtpParameters.Encoding encoding : rtcMedia.encodings){
				StructConfPB.TRtcRid.Builder ridBuider = StructConfPB.TRtcRid.newBuilder();
				ridBuider.setRid(encoding.rid)
						.setEmres(convertRes(encoding.scaleResolutionDownBy));
				rtcMediaBuilder.addRidlist(ridBuider.build());
			}
		}
		rtcMediaBuilder.setMid(rtcMedia.mid);
		rtcMediaBuilder.setStreamid(rtcMedia.streamid);
		return rtcMediaBuilder.build();
	}

	void sendOfferSdp(int connType, @NonNull String offerSdp, TRtcMedia... rtcMediaList) {
        StructConfPB.TRtcMedialist.Builder builder = StructConfPB.TRtcMedialist.newBuilder();
        for (TRtcMedia rtcMedia : rtcMediaList){
            builder.addMedia(rtcMediaToPB(rtcMedia));
        }
		MtMsg msg = new MtMsg();
		msg.SetMsgId("Ev_MT_GetOffer_Ntf");
		msg.addMsg(BasePB.TU32.newBuilder().setValue(connType).build());
		msg.addMsg(BasePB.TString.newBuilder().setValue(offerSdp).build());
		msg.addMsg(builder.build());
		byte[] abyContent = msg.Encode();
//		int ret = Connector.PostOspMsg( EmMtOspMsgSys.Ev_MtOsp_ProtoBufMsg.getnVal(), abyContent, abyContent.length,
//                rtcServiceId, rtcServiceNode, myId, myNode, 5000 );
		int ret = Connector.PostOspMsg( EmMtOspMsgSys.Ev_MtOsp_ProtoBufMsg.getnVal(), abyContent, abyContent.length,
				dispatchId, dispatchNode, myId, myNode, 5000 );
		if (0 != ret){
			KLog.p(KLog.ERROR, "PostOspMsg %s failed", msg.GetMsgId());
		}

		KLog.p("=> send offer: connType=%s, sdp=%s", connType, offerSdp);
	}

	void sendAnswerSdp(int connType, @NonNull String answerSdp, List<RtcConnector.TRtcMedia> rtcMediaList) {
		StructConfPB.TRtcMedialist.Builder builder = StructConfPB.TRtcMedialist.newBuilder();
		for (TRtcMedia rtcMedia : rtcMediaList){
			builder.addMedia(rtcMediaToPB(rtcMedia));
		}
		MtMsg msg = new MtMsg();
		msg.SetMsgId("Ev_MT_GetAnswer_Ntf");
		msg.addMsg(BasePB.TU32.newBuilder().setValue(connType).build());
		msg.addMsg(BasePB.TString.newBuilder().setValue(answerSdp).build());
		msg.addMsg(builder.build());
		byte[] abyContent = msg.Encode();
//		int ret = Connector.PostOspMsg( EmMtOspMsgSys.Ev_MtOsp_ProtoBufMsg.getnVal(), abyContent, abyContent.length,
//                rtcServiceId, rtcServiceNode, myId, myNode, 5000 );
		int ret = Connector.PostOspMsg( EmMtOspMsgSys.Ev_MtOsp_ProtoBufMsg.getnVal(), abyContent, abyContent.length,
				dispatchId, dispatchNode, myId, myNode, 5000 );
        KLog.p("=> send answer, connType=%s, sdp=%s", connType, answerSdp);
        if (0 != ret){
            KLog.p(KLog.ERROR, "PostOspMsg %s failed", msg.GetMsgId());
        }

	}


	// 发送IceCandidate给对端
	void sendIceCandidate(int connType, @NonNull String sdpMid, int sdpMLineIndex, @NonNull String sdp) {
		MtMsg msg = new MtMsg();
		msg.SetMsgId("Ev_MT_IceCandidate_Ntf");
		msg.addMsg(BasePB.TU32.newBuilder().setValue(connType).build());
		msg.addMsg(BasePB.TString.newBuilder().setValue(sdpMid).build());
		msg.addMsg(BasePB.TU32.newBuilder().setValue(sdpMLineIndex).build());
		msg.addMsg(BasePB.TString.newBuilder().setValue(sdp).build());
		byte[] abyContent = msg.Encode();
//		int ret = Connector.PostOspMsg( EmMtOspMsgSys.Ev_MtOsp_ProtoBufMsg.getnVal(), abyContent, abyContent.length,
//                rtcServiceId, rtcServiceNode, myId, myNode, 5000 );
		int ret = Connector.PostOspMsg( EmMtOspMsgSys.Ev_MtOsp_ProtoBufMsg.getnVal(), abyContent, abyContent.length,
				dispatchId, dispatchNode, myId, myNode, 5000 );
		if (0 != ret){
			KLog.p(KLog.ERROR, "PostOspMsg %s failed", msg.GetMsgId());
		}

		KLog.p("=> send candidate sdpmid=%s, sdpline=%s, candidate=%s", sdpMid, sdpMLineIndex, sdp);
	}


	void sendFingerPrint(int connType, @NonNull String fingerPrint){
        MtMsg msg = new MtMsg();
        msg.SetMsgId("Ev_MT_FingerPrint_Ntf");
        msg.addMsg(BasePB.TU32.newBuilder().setValue(connType).build());
        msg.addMsg(BasePB.TString.newBuilder().setValue(fingerPrint).build());
        byte[] abyContent = msg.Encode();
//		int ret = Connector.PostOspMsg( EmMtOspMsgSys.Ev_MtOsp_ProtoBufMsg.getnVal(), abyContent, abyContent.length,
//                rtcServiceId, rtcServiceNode, myId, myNode, 5000 );
        int ret = Connector.PostOspMsg( EmMtOspMsgSys.Ev_MtOsp_ProtoBufMsg.getnVal(), abyContent, abyContent.length,
                dispatchId, dispatchNode, myId, myNode, 5000 );
        if (0 != ret){
            KLog.p(KLog.ERROR, "PostOspMsg %s failed", msg.GetMsgId());
        }

        KLog.p("=> sendFingerPrint connType=%s, fingerPrint=%s", connType, fingerPrint);
	}

	static class TRtcMedia {
		String streamid="";
		String mid;
		List<RtpParameters.Encoding> encodings;

		TRtcMedia(String streamid, String mid, List<RtpParameters.Encoding> encodings) {
			this.streamid = streamid;
			this.mid = mid;
			this.encodings = encodings;
		}
		TRtcMedia(String streamid, String mid) {
			this.streamid = streamid;
			this.mid = mid;
		}
		TRtcMedia(String mid) {
			this.mid = mid;
		}
		TRtcMedia(String mid, List<RtpParameters.Encoding> encodings) {
			this.mid = mid;
			this.encodings = encodings;
		}
	}

	/**
	 * 所有回调均从主线程
	 * */
	interface SignalingEvents {

		void onGetOfferCmd(int connType, int mediaType);

		void onSetOfferCmd(int connType, String offerSdp, List<TRtcMedia> rtcMediaList);

		void onSetAnswerCmd(int connType, String answerSdp);

		void onSetIceCandidateCmd(int connType, String sdpMid, int sdpMLineIndex, String sdp);

		void onGetFingerPrintCmd(int connType);

	}


}

 