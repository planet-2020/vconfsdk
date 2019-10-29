package com.kedacom.vconf.sdk.webrtc;

import org.webrtc.IceCandidate;
import org.webrtc.PeerConnection;
import org.webrtc.SessionDescription;

import java.util.List;

class SignalingParameters {
		final List<PeerConnection.IceServer> iceServers;
		final boolean initiator;
		final String clientId;
		final String wssUrl;
		final String wssPostUrl;
		final SessionDescription offerSdp;
		final List<IceCandidate> iceCandidates;

		final int connType;
		final int mediaType;

		SignalingParameters(List<PeerConnection.IceServer> iceServers, boolean initiator,
								   String clientId, String wssUrl, String wssPostUrl, SessionDescription offerSdp,
								   List<IceCandidate> iceCandidates, int connType, int mediaType) {
			this.iceServers = iceServers;
			this.initiator = initiator;
			this.clientId = clientId;
			this.wssUrl = wssUrl;
			this.wssPostUrl = wssPostUrl;
			this.offerSdp = offerSdp;
			this.iceCandidates = iceCandidates;

			this.connType = connType;
			this.mediaType = mediaType;
		}
	}