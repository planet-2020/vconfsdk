
package com.kedacom.vconf.webrtc;

import com.google.protobuf.InvalidProtocolBufferException;
import com.kedacom.kdv.mt.ospconnector.Connector;
import com.kedacom.kdv.mt.ospconnector.IRcvMsgCallback;
import com.kedacom.mt.netmanage.protobuf.BasePB;
import com.kedacom.mt.netmanage.protobuf.EnumPB;
import com.kedacom.mt.netmanage.protobuf.StructConfPB;
import com.kedacom.osp.BodyItem;
import com.kedacom.osp.EmMtOspMsgSys;
import com.kedacom.osp.MtMsg;

import org.webrtc.IceCandidate;
import org.webrtc.RtpParameters;
import org.webrtc.SessionDescription;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


class RtcConnector implements IRcvMsgCallback{

	public static final String TAG = "RtcConnector";
	public static final String WEBRTC_NAME = "WEBRTC_NAME";
	public static final short WEBRTC_ID = 143;
	public static final short MTDISPATCH_ID = 107;
	public static boolean stopHanldeJni = false;
	private final Map<String, ICbMsgHandler> cbMsgHandlerMap = new HashMap<>();

	private final long myId =Connector.MAKEIID(WEBRTC_ID, (short)1 );
    private final long myNode = 0;
    private final long dispatchId = Connector.MAKEIID(MTDISPATCH_ID, (short)1 );
	private final long dispatchNode = 0;
    private long rtcServiceId;
    private long rtcServiceNode;


	private SignalingEvents signalingEvents;
	void setSignalingEventsCallback(SignalingEvents signalingEvents){
		this.signalingEvents = signalingEvents;
	}

	RtcConnector( ) {
		cbMsgHandlerMap.put("Ev_MT_GetOffer_Cmd", this::onGetOfferCmd);
		cbMsgHandlerMap.put("Ev_MT_GetOffer_Ntf", this::onGetOfferNtf);
		cbMsgHandlerMap.put("Ev_MT_SetOffer_Cmd", this::onSetOfferCmd);
		cbMsgHandlerMap.put("Ev_MT_GetAnswer_Ntf", this::onGetAnswerNtf);
		cbMsgHandlerMap.put("Ev_MT_SetAnswer_Cmd", this::onSetAnswerCmd);
		cbMsgHandlerMap.put("Ev_MT_IceCandidate_Ntf", this::onIceCandidateNtf);
		cbMsgHandlerMap.put("Ev_MT_SetIceCandidate_Cmd", this::onSetIceCandidateCmd);

		if (!CreateOspObject()){
			throw new RuntimeException("CreateOspObject failed!");
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

		if ( stopHanldeJni )
		{
			//停止接收
			KLog.p( TAG, " stop rev connector msg, and will dismiss msg:" + nEvent );
			return;
		}

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

	private enum ConnectionState { NEW, CONNECTED, CLOSED, ERROR }
	private ConnectionState roomState;

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

		KLog.p("rtcServiceId=%s, rtcServiceNode=%s, peerType=%s, mediaType=%s", rtcServiceId, rtcServiceNode, connType, mediaType);

		roomState = ConnectionState.CONNECTED;

		SignalingParameters parameters = new SignalingParameters(
				// Ice servers are not needed for direct connections.
				new ArrayList<>(),
				true, // Server side acts as the initiator on direct connections.
				null, // clientId
				null, // wssUrl
				null, // wwsPostUrl
				null, // offerSdp
				null, // iceCandidates
                connType,
				mediaType
		);
		signalingEvents.onConnectedToRoom(connType, parameters, null);
	}


	/**
	 * （模拟mtrtcservice）收到mtrtcmp的Ev_MT_GetOffer_Ntf的处理
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

		KLog.p("<= recv offer");

		KLog.p("rtcServiceId=%s, rtcServiceNode=%s,  peerType=%s, offer=%s, rtcMedialist=%s", rtcServiceId, rtcServiceNode, connType, offer, rtcMedialist);
		SessionDescription sdp = new SessionDescription(SessionDescription.Type.OFFER, offer);
		SignalingParameters parameters = new SignalingParameters(
				// Ice servers are not needed for direct connections.
				new ArrayList<>(),
				false, // This code will only be run on the client side. So, we are not the initiator.
				null, // clientId
				null, // wssUrl
				null, // wssPostUrl
				sdp, // offerSdp
				null, // iceCandidates
                connType,
                CommonDef.MEDIA_TYPE_UNKNOWN
		);
		roomState = ConnectionState.CONNECTED;
		signalingEvents.onConnectedToRoom(connType, parameters, rtcMediaFromPB(rtcMedialist.getMedia(0)));

	}

	/**
	 * （模拟mtrtcservice）收到mtrtcmp的onGetAnswerNtf的处理
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

		KLog.p("<= recv answer");

		KLog.p("peerType=%s, answer=%s", connType, answer);
		SessionDescription sdp = new SessionDescription(SessionDescription.Type.ANSWER, answer);
		signalingEvents.onRemoteDescription(connType, sdp);

	}

	/**
	 * （模拟mtrtcservice）收到mtrtcmp的Ev_MT_IceCandidate_Ntf的处理
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

		KLog.p("peerType=%s, mid=%s, index=%s, candidate", connType, mid, index, candidate);
		IceCandidate candidate1 = new IceCandidate(mid, index, candidate);
		signalingEvents.onRemoteIceCandidate(connType, candidate1);
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

	private PeerConnectionClient.TRtcMedia rtcMediaFromPB(StructConfPB.TRtcMedia pbRtcMedia){
		List<RtpParameters.Encoding> encodings = new ArrayList<>();
		for (StructConfPB.TRtcRid tRtcRid : pbRtcMedia.getRidlistList()){
			String rid = tRtcRid.getRid();
			encodings.add(PeerConnectionClient.createEncoding(rid,
					1 // FIXME 这里暂时写死为了联调
			));
		}
		return new PeerConnectionClient.TRtcMedia(pbRtcMedia.getStreamid(), pbRtcMedia.getMid(), encodings);
	}

	private StructConfPB.TRtcMedia rtcMediaToPB(PeerConnectionClient.TRtcMedia rtcMedia){
		StructConfPB.TRtcMedia.Builder rtcMediaBuilder = StructConfPB.TRtcMedia.newBuilder();
		for (RtpParameters.Encoding encoding : rtcMedia.encodings){
			StructConfPB.TRtcRid.Builder ridBuider = StructConfPB.TRtcRid.newBuilder();
			ridBuider.setRid(encoding.rid)
					.setEmres(convertRes(encoding.scaleResolutionDownBy));
			rtcMediaBuilder.addRidlist(ridBuider.build());
		}
		rtcMediaBuilder.setMid(rtcMedia.mid);
		rtcMediaBuilder.setStreamid(rtcMedia.streamid);
		return rtcMediaBuilder.build();
	}

	public void sendOfferSdp(int connType, SessionDescription sdp, PeerConnectionClient.TRtcMedia rtcMedia) {
		// 发送offer
		StructConfPB.TRtcMedia.Builder rtcMediaBuilder = StructConfPB.TRtcMedia.newBuilder();
		for (RtpParameters.Encoding encoding : rtcMedia.encodings){
			StructConfPB.TRtcRid.Builder ridBuider = StructConfPB.TRtcRid.newBuilder();
			ridBuider.setRid(encoding.rid)
					.setEmres(convertRes(encoding.scaleResolutionDownBy));
			rtcMediaBuilder.addRidlist(ridBuider.build());
		}
		rtcMediaBuilder.setMid(rtcMedia.mid);
		rtcMediaBuilder.setStreamid(rtcMedia.streamid);

		MtMsg msg = new MtMsg();
		msg.SetMsgId("Ev_MT_GetOffer_Ntf");
		msg.addMsg(BasePB.TU32.newBuilder().setValue(connType).build());
		msg.addMsg(BasePB.TString.newBuilder().setValue(sdp.description).build());
		msg.addMsg(StructConfPB.TRtcMedialist.newBuilder()
				.addMedia(rtcMediaToPB(rtcMedia))
				.build()
		);
		byte[] abyContent = msg.Encode();
//		int ret = Connector.PostOspMsg( EmMtOspMsgSys.Ev_MtOsp_ProtoBufMsg.getnVal(), abyContent, abyContent.length,
//                rtcServiceId, rtcServiceNode, myId, myNode, 5000 );
		int ret = Connector.PostOspMsg( EmMtOspMsgSys.Ev_MtOsp_ProtoBufMsg.getnVal(), abyContent, abyContent.length,
				dispatchId, dispatchNode, myId, myNode, 5000 );
		if (0 != ret){
			KLog.p(KLog.ERROR, "PostOspMsg %s failed", msg.GetMsgId());
		}

		KLog.p("=> send offer: rtcServiceId=%s, rtcServiceNode=%s, sdp=%s, %s", rtcServiceId, rtcServiceNode, sdp.type, sdp.description);
	}

	public void sendAnswerSdp(int connType, final SessionDescription sdp) {
		// 发送answer给对端
		MtMsg msg = new MtMsg();
		msg.SetMsgId("Ev_MT_GetAnswer_Ntf");
		msg.addMsg(BasePB.TU32.newBuilder().setValue(connType).build());
		msg.addMsg(BasePB.TString.newBuilder().setValue(sdp.description).build());
		byte[] abyContent = msg.Encode();
//		int ret = Connector.PostOspMsg( EmMtOspMsgSys.Ev_MtOsp_ProtoBufMsg.getnVal(), abyContent, abyContent.length,
//                rtcServiceId, rtcServiceNode, myId, myNode, 5000 );
		int ret = Connector.PostOspMsg( EmMtOspMsgSys.Ev_MtOsp_ProtoBufMsg.getnVal(), abyContent, abyContent.length,
				dispatchId, dispatchNode, myId, myNode, 5000 );
        KLog.p("=> send answer, rtcServiceId=%s, rtcServiceNode=%s, sdp=%s, %s", rtcServiceId, rtcServiceNode, sdp.type, sdp.description);
        if (0 != ret){
            KLog.p(KLog.ERROR, "PostOspMsg %s failed", msg.GetMsgId());
        }

	}


	// 发送IceCandidate给对端
	public void sendIceCandidate(int connType, IceCandidate candidate) {
		MtMsg msg = new MtMsg();
		msg.SetMsgId("Ev_MT_IceCandidate_Ntf");
		msg.addMsg(BasePB.TU32.newBuilder().setValue(connType).build());
		msg.addMsg(BasePB.TString.newBuilder().setValue(candidate.sdpMid).build());
		msg.addMsg(BasePB.TU32.newBuilder().setValue(candidate.sdpMLineIndex).build());
		msg.addMsg(BasePB.TString.newBuilder().setValue(candidate.sdp).build());
		byte[] abyContent = msg.Encode();
//		int ret = Connector.PostOspMsg( EmMtOspMsgSys.Ev_MtOsp_ProtoBufMsg.getnVal(), abyContent, abyContent.length,
//                rtcServiceId, rtcServiceNode, myId, myNode, 5000 );
		int ret = Connector.PostOspMsg( EmMtOspMsgSys.Ev_MtOsp_ProtoBufMsg.getnVal(), abyContent, abyContent.length,
				dispatchId, dispatchNode, myId, myNode, 5000 );
		if (0 != ret){
			KLog.p(KLog.ERROR, "PostOspMsg %s failed", msg.GetMsgId());
		}

		KLog.p("=> send candidate sdpmid=%s, sdpline=%s, candidate=%s", candidate.sdpMid, candidate.sdpMLineIndex, candidate.sdp);
	}

	interface SignalingEvents {
		/**
		 * Callback fired once the room's signaling parameters
		 * SignalingParameters are extracted.
		 */
		void onConnectedToRoom(int connType, final SignalingParameters params, PeerConnectionClient.TRtcMedia rtcMedia);

		/**
		 * Callback fired once remote SDP is received.
		 */
		void onRemoteDescription(int connType, final SessionDescription sdp);

		/**
		 * Callback fired once remote Ice candidate is received.
		 */
		void onRemoteIceCandidate(int connType, final IceCandidate candidate);

		/**
		 * Callback fired once remote Ice candidate removals are received.
		 */
		void onRemoteIceCandidatesRemoved(int connType, final IceCandidate[] candidates);

		/**
		 * Callback fired once channel is closed.
		 */
		void onChannelClose(int connType);

		/**
		 * Callback fired once channel error happened.
		 */
		void onChannelError(int connType, final String description);
	}


}

 