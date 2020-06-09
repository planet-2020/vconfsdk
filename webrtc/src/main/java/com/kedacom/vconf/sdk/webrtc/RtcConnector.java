
package com.kedacom.vconf.sdk.webrtc;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


class RtcConnector implements IRcvMsgCallback{

	private static final String TAG = "RtcConnector";
	private static final String WEBRTC_NAME = "WEBRTC_NAME";
	private static final short WEBRTC_ID = 144;
	private static final short MTDISPATCH_ID = 107;
	private static final short MTRTCSERVICE_ID = 145;
	private final Map<String, ICbMsgHandler> cbMsgHandlerMap = new HashMap<>();

	private final long myId =Connector.MAKEIID(WEBRTC_ID, (short)1 );
    private final long myNode = 0;
    private final long dispatchId = Connector.MAKEIID(MTDISPATCH_ID, (short)1 );
	private final long dispatchNode = 0;

	private final long mtrtcserviceId = Connector.MAKEIID(MTRTCSERVICE_ID, (short)1 );
	private final long mtrtcserviceNode = 0;

	private Handler handler = new Handler(Looper.getMainLooper());

	// 因受osp单次可发送数据长度的限制，sdp可能需要分段发送，需要先缓存下来后拼接，拼接完成后decode。
	private ByteArrayOutputStream offerBuf;
	private ByteArrayOutputStream answerBuf;
	private static final int SegLengthLimit = 50000; // 每段的长度上限。

	private SignalingEvents signalingEvents;
	void setSignalingEventsCallback(SignalingEvents signalingEvents){
		this.signalingEvents = signalingEvents;
	}

	RtcConnector( ) {
		// 注册消息处理方法
		cbMsgHandlerMap.put("Ev_MT_GetOffer_Cmd", this::onGetOfferCmd);
		cbMsgHandlerMap.put("Ev_MT_SetOffer_Cmd", this::onSetOfferCmd);
		cbMsgHandlerMap.put("Ev_MT_SetAnswer_Cmd", this::onSetAnswerCmd);
		cbMsgHandlerMap.put("Ev_MT_SetIceCandidate_Cmd", this::onSetIceCandidateCmd);
		cbMsgHandlerMap.put("Ev_MT_GetFingerPrint_Cmd", this::onGetFingerPrintCmd);
		cbMsgHandlerMap.put("Ev_MT_UnPub_Cmd", this::onUnPubCmd);

		cbMsgHandlerMap.put("Ev_MT_CodecQuiet_Cmd", this::onCodecQuietCmd);
		cbMsgHandlerMap.put("Ev_MT_CodecMute_Cmd", this::onCodecMuteCmd);

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
		subsMsgBuilder.addMsgid("Ev_MT_CodecQuiet_Cmd");
		subsMsgBuilder.addMsgid("Ev_MT_CodecMute_Cmd");
		subsMsgBuilder.addMsgid("Ev_MT_UnPub_Cmd");
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

		KLog.p("mtrtcCallback event=%s", nEvent);

		MtMsg mtMsg = new MtMsg();
		
		if ( nEvent == EmMtOspMsgSys.Ev_MtOsp_ProtoBufMsg.getnVal() )
		{
			// PB消息处理
			boolean bRet = mtMsg.Decode( abyContent );
			if ( !bRet ) {
				KLog.p(KLog.ERROR, " mtmsg decode failed" );
				return;
			}
		}
		else if ( nEvent == EmMtOspMsgSys.Ev_MtOsp_SubPackageOffer.getnVal())
		{
			if (null == offerBuf){
				offerBuf = new ByteArrayOutputStream();
			}
			try {
				offerBuf.write(abyContent);
			} catch (IOException e) {
				e.printStackTrace();
				offerBuf = null;
			}
			// 分段消息，等收齐后再处理
			return;
		}
		else if ( nEvent == EmMtOspMsgSys.Ev_MtOsp_SubPackageOfferEnd.getnVal())
		{
			if (null != offerBuf){
				byte[] offer = offerBuf.toByteArray();
				try {
					offerBuf.close();
				} catch (IOException e) {
					e.printStackTrace();
				}finally {
					offerBuf = null;
				}
				// 分段消息已收齐，可以decode了
				boolean bRet = mtMsg.Decode(offer);
				if ( !bRet ) {
					KLog.p(KLog.ERROR, " mtmsg decode failed" );
					return;
				}
			}
		}
		else if ( nEvent == EmMtOspMsgSys.Ev_MtOsp_SubPackageAnswer.getnVal() )
		{
			if (null == answerBuf){
				answerBuf = new ByteArrayOutputStream();
			}
			try {
				answerBuf.write(abyContent);
			} catch (IOException e) {
				e.printStackTrace();
				answerBuf = null;
			}
			// 分段消息，等收齐后再处理
			return;
		}
		else if (nEvent == EmMtOspMsgSys.Ev_MtOsp_SubPackageAnswerEnd.getnVal())
		{
			if (null != answerBuf){
				byte[] offer = answerBuf.toByteArray();
				try {
					answerBuf.close();
				} catch (IOException e) {
					e.printStackTrace();
				}finally {
					answerBuf = null;
				}
				// 分段消息已收齐，可以decode了
				boolean bRet = mtMsg.Decode(offer);
				if ( !bRet ) {
					KLog.p(KLog.ERROR, " mtmsg decode failed" );
					return;
				}
			}
		}

		else if ( nEvent == EmMtOspMsgSys.EV_MtOsp_OSP_DISCONNECT.getnVal() )
		{
			// OSP断链检测消息处理
			KLog.p(KLog.WARN, " mtdispatch disconnected" );
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

//		KLog.p("msg=%s, srcId=%s, srcNode=%s", mtMsg, nSrcIId, nSrcNode);
		cbMsgHandler.onMsg(mtMsg, nSrcIId, nSrcNode);
	}


	private interface ICbMsgHandler{
		void onMsg(MtMsg mtMsg, long nSrcId, long nSrcNode);
	}


	/**
	 * 收到平台“发起signaling”的指示，主动发起signaling流程
	 * */
	private void onGetOfferCmd(MtMsg mtMsg, long nSrcId, long nSrcNode){
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

		Log.i(TAG, String.format("[PUB]<=#= onGetOfferCmd, connType=%s, mediaType=%s", connType, mediaType));

		handler.post(() -> {
			if (null != signalingEvents) {
				signalingEvents.onGetOfferCmd(connType, mediaType);
			}
		});

	}


	/**
	 * 收到对端的offer，被动开始signaling流程
	 * */
	private void onSetOfferCmd(MtMsg mtMsg, long nSrcId, long nSrcNode){
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

		Log.i(TAG, String.format("[sub]<=#= onSetOfferCmd, connType=%s, offer=", connType));
		KLog.p(offer);

		List<TRtcMedia> rtcMedias = new ArrayList<>();
		for (StructConfPB.TRtcMedia tRtcMedia : rtcMedialist.getMediaList()){
			rtcMedias.add(rtcMediaFromPB(tRtcMedia));
		}
		handler.post(() -> {
			if (null != signalingEvents) {
				signalingEvents.onSetOfferCmd(connType, offer, rtcMedias);
			}
		});

	}


	/**
	 * 收到对端的answer
	 * */
	private void onSetAnswerCmd(MtMsg mtMsg, long nSrcId, long nSrcNode){
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

		Log.i(TAG, String.format("[PUB]<=#= onSetAnswerCmd, connType=%s, answer=", connType));
		KLog.p(answer);

		handler.post(() -> {
			if (null != signalingEvents) {
				signalingEvents.onSetAnswerCmd(connType, answer);
			}
		});

	}


	/**
	 * 收到对端的candidate
	 * */
	private void onSetIceCandidateCmd(MtMsg mtMsg, long nSrcId, long nSrcNode){
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

		Log.i(TAG, String.format("<=#= connType=%s, mid=%s, index=%s, candidate=", connType, mid, index));
		KLog.p(candidate);
		handler.post(() -> {
			if (null != signalingEvents) {
				signalingEvents.onSetIceCandidateCmd(connType, mid, index, candidate);
			}
		});
	}


	/**
	 * 获取指纹。
	 * 业务组件定的流程：
	 * 组件先向界面获取FingerPrint，然后向界面推送StreamListNtf，
	 * 界面setPlayPara，然后组件推onSetOfferCmd给界面开始订阅
	 * */
	private void onGetFingerPrintCmd(MtMsg mtMsg, long nSrcId, long nSrcNode){
		BodyItem item0 = mtMsg.GetMsgBody(0);
		int connType;
		try {
			connType = BasePB.TU32.parseFrom(item0.getAbyContent()).getValue();
		} catch (InvalidProtocolBufferException e) {
			e.printStackTrace();
			return;
		}

		KLog.p("[sub]<=#= connType=%s", connType);
		handler.post(() -> {
			if (null != signalingEvents) {
				signalingEvents.onGetFingerPrintCmd(connType);
			}
		});
	}


	/**
	 * 静音
	 * */
	private void onCodecQuietCmd(MtMsg mtMsg, long nSrcId, long nSrcNode){
		BodyItem item0 = mtMsg.GetMsgBody(0);
		boolean bQuiet;
		try {
			bQuiet = BasePB.TBOOL32.parseFrom(item0.getAbyContent()).getValue();
		} catch (InvalidProtocolBufferException e) {
			e.printStackTrace();
			return;
		}

		KLog.p("bQuiet=%s", bQuiet);
		handler.post(() -> {
			if (null != signalingEvents) {
				signalingEvents.onCodecQuietCmd(bQuiet);
			}
		});
	}


	/**
	 * 哑音
	 * */
	private void onCodecMuteCmd(MtMsg mtMsg, long nSrcId, long nSrcNode){
		BodyItem item0 = mtMsg.GetMsgBody(0);
		boolean bMute;
		try {
			bMute = BasePB.TBOOL32.parseFrom(item0.getAbyContent()).getValue();
		} catch (InvalidProtocolBufferException e) {
			e.printStackTrace();
			return;
		}

		KLog.p("bMute=%s", bMute);
		handler.post(() -> {
			if (null != signalingEvents) {
				signalingEvents.onCodecMuteCmd(bMute);
			}
		});
	}


	/**
	 * 取消发布
	 * */
	private void onUnPubCmd(MtMsg mtMsg, long nSrcId, long nSrcNode){
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

		Log.i(TAG, String.format("[UNPUB]<=#= onUnPubCmd, connType=%s, mediaType=%s", connType, mediaType));

		handler.post(() -> {
			if (null != signalingEvents) {
				signalingEvents.onUnPubCmd(connType, mediaType);
			}
		});
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
		bufSend(abyContent, EmMtOspMsgSys.Ev_MtOsp_SubPackageOffer, EmMtOspMsgSys.Ev_MtOsp_SubPackageOfferEnd, mtrtcserviceId, mtrtcserviceNode);

		Log.i(TAG, String.format("[PUB]=#=> sendOfferSdp, connType=%s, offerSdp=", connType));
		KLog.p(offerSdp);
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
		bufSend(abyContent, EmMtOspMsgSys.Ev_MtOsp_SubPackageAnswer, EmMtOspMsgSys.Ev_MtOsp_SubPackageAnswerEnd, mtrtcserviceId, mtrtcserviceNode);

		Log.i(TAG, String.format("[sub]=#=> sendAnswerSdp, connType=%s, answerSdp=", connType));
		KLog.p(answerSdp);
	}

	private void bufSend(byte[] buf, EmMtOspMsgSys segId, EmMtOspMsgSys finalId, long peerId, long peerNode){
		KLog.p("buf.len=%s, segId=%s, finalId=%s, peerId=%s, peerNode=%s", buf.length, segId, finalId, peerId, peerNode);
		int sentLen = 0;
		do{
			int segLen = Math.min(buf.length-sentLen, SegLengthLimit);
			byte[] seg = new byte[segLen];
			System.arraycopy(buf, sentLen, seg, 0, segLen);
			int ret = Connector.PostOspMsg(segId.getnVal(), seg, segLen, peerId, peerNode, myId, myNode, 5000 );
			if (0 != ret){
				KLog.p(KLog.ERROR, "PostOspMsg %s failed", segId);
				return;
			}
			sentLen += segLen;
		}while (sentLen < buf.length);

		int ret = Connector.PostOspMsg(finalId.getnVal(), new byte[]{}, 0, peerId, peerNode, myId, myNode, 5000 );
		if (0 != ret){
			KLog.p(KLog.ERROR, "PostOspMsg %s failed", segId);
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
		int ret = Connector.PostOspMsg( EmMtOspMsgSys.Ev_MtOsp_ProtoBufMsg.getnVal(), abyContent, abyContent.length,
				dispatchId, dispatchNode, myId, myNode, 5000 );
		if (0 != ret){
			KLog.p(KLog.ERROR, "PostOspMsg %s failed", msg.GetMsgId());
		}

		Log.i(TAG, String.format("=#=> sendIceCandidate, sdpmid=%s, sdpline=%s, candidate=", sdpMid, sdpMLineIndex));
		KLog.p(sdp);
	}


	void sendFingerPrint(int connType, @NonNull String fingerPrint){
        MtMsg msg = new MtMsg();
        msg.SetMsgId("Ev_MT_FingerPrint_Ntf");
        msg.addMsg(BasePB.TU32.newBuilder().setValue(connType).build());
        msg.addMsg(BasePB.TString.newBuilder().setValue(fingerPrint).build());
        byte[] abyContent = msg.Encode();
        int ret = Connector.PostOspMsg( EmMtOspMsgSys.Ev_MtOsp_ProtoBufMsg.getnVal(), abyContent, abyContent.length,
                dispatchId, dispatchNode, myId, myNode, 5000 );
        if (0 != ret){
            KLog.p(KLog.ERROR, "PostOspMsg %s failed", msg.GetMsgId());
        }

        KLog.p("[sub]=#=> sendFingerPrint connType=%s, fingerPrint=%s", connType, fingerPrint);
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

		default void onCodecQuietCmd(boolean bQuiet){}

		default void onCodecMuteCmd(boolean bMute){}

		void onUnPubCmd(int connType, int mediaType);
	}


}

 