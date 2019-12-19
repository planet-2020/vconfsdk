package com.kedacom.vconf.sdk.webrtc;

import com.kedacom.vconf.sdk.utils.log.KLog;

import org.webrtc.RTCStats;
import org.webrtc.RTCStatsReport;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by Sissi on 2019/12/19
 */
final class StatsHelper {

    static Stats resolveStats(RTCStatsReport rtcStatsReport){
        System.out.println(String.format("rtcStatsReport=%s ", rtcStatsReport));
        Stats stats = new Stats();
        List<Codec> encoderList = new ArrayList<>();
        List<Codec> decoderList = new ArrayList<>();
        stats.encoderList = encoderList;
        stats.decoderList = decoderList;

        for (Map.Entry<String, RTCStats> rtcStatsEntry : rtcStatsReport.getStatsMap().entrySet()){
            RTCStats rtcStats = rtcStatsEntry.getValue();
            String type = rtcStats.getType();
            Map<String, Object> members = rtcStats.getMembers();

            System.out.println(String.format("statsEntry={key=%s, values={type=%s ",rtcStatsEntry.getKey(), type));
            for (Map.Entry<String, Object> member  : members.entrySet()){
                System.out.println(String.format("member={%s, %s}",member.getKey(), member.getValue().getClass()));
            }
            System.out.println("}");

            if (type.equals("media-source")) {
                boolean bAudio = "audio".equals(members.get("kind"));
                if (bAudio){
                    AudioSource audioSource = new AudioSource();
                    audioSource.trackIdentifier = (String) members.get("trackIdentifier");
                    audioSource.audioLevel = (double) members.get("audioLevel");
                    audioSource.totalAudioEnergy = (double) members.get("totalAudioEnergy");
                    audioSource.totalSamplesDuration = (double) members.get("totalSamplesDuration");
                    stats.audioSource = audioSource;
                }else{
                    VideoSource videoSource = new VideoSource();
                    videoSource.trackIdentifier = (String) members.get("trackIdentifier");
                    videoSource.width = (long) members.get("width");
                    videoSource.height = (long) members.get("height");
                    videoSource.framesPerSecond = (long) members.get("framesPerSecond");
                    stats.videoSource = videoSource;
                }

            }else if (type.equals("codec")){
                Codec codec = new Codec();
                codec.id = rtcStats.getId();
                codec.payloadType = (long) members.get("payloadType");
                codec.mimeType = (String) members.get("mimeType");
                codec.clockRate = (long) members.get("clockRate");
                if (codec.id.contains("Inbound")){
                    decoderList.add(codec);
                }else{
                    encoderList.add(codec);
                }

            }else if (type.equals("track")){
                boolean bRecv = (boolean) members.get("remoteSource");
                boolean bAudio = "audio".equals(members.get("kind"));
                if (bRecv){
                    if (bAudio){
                        RecvAudioTrack recvAudioTrack = new RecvAudioTrack();
                        recvAudioTrack.trackIdentifier = (String) members.get("trackIdentifier");
                        recvAudioTrack.ended = (boolean) members.get("ended");
                        recvAudioTrack.detached = (boolean) members.get("detached");
                        recvAudioTrack.jitterBufferDelay = (double) members.get("jitterBufferDelay");
                        recvAudioTrack.jitterBufferEmittedCount = ((BigInteger) members.get("jitterBufferEmittedCount")).longValue();
                        recvAudioTrack.audioLevel = (double) members.get("audioLevel");
                        recvAudioTrack.totalAudioEnergy = (double) members.get("totalAudioEnergy");
                        recvAudioTrack.totalSamplesReceived = ((BigInteger) members.get("totalSamplesReceived")).longValue();
                        recvAudioTrack.totalSamplesDuration = (double) members.get("totalSamplesDuration");
                        recvAudioTrack.concealedSamples = ((BigInteger) members.get("concealedSamples")).longValue();
                        recvAudioTrack.silentConcealedSamples = ((BigInteger) members.get("silentConcealedSamples")).longValue();
                        recvAudioTrack.concealmentEvents = ((BigInteger) members.get("concealmentEvents")).longValue();
                        recvAudioTrack.insertedSamplesForDeceleration = ((BigInteger) members.get("insertedSamplesForDeceleration")).longValue();
                        recvAudioTrack.removedSamplesForAcceleration = ((BigInteger) members.get("removedSamplesForAcceleration")).longValue();
                        recvAudioTrack.jitterBufferFlushes = ((BigInteger) members.get("jitterBufferFlushes")).longValue();
                        recvAudioTrack.delayedPacketOutageSamples = ((BigInteger) members.get("delayedPacketOutageSamples")).longValue();
                        recvAudioTrack.relativePacketArrivalDelay = (double) members.get("relativePacketArrivalDelay");
                        recvAudioTrack.interruptionCount = (long) members.get("interruptionCount");
                        recvAudioTrack.totalInterruptionDuration = (double) members.get("totalInterruptionDuration");
                        stats.recvAudioTrack = recvAudioTrack;
                    }else{
                        RecvVideoTrack recvVideoTrack = new RecvVideoTrack();
                        recvVideoTrack.trackIdentifier = (String) members.get("trackIdentifier");
                        recvVideoTrack.ended = (boolean) members.get("ended");
                        recvVideoTrack.detached = (boolean) members.get("detached");
                        recvVideoTrack.jitterBufferDelay = (double) members.get("jitterBufferDelay");
                        recvVideoTrack.jitterBufferEmittedCount = ((BigInteger) members.get("jitterBufferEmittedCount")).longValue();
                        recvVideoTrack.frameWidth = (long) members.get("frameWidth");
                        recvVideoTrack.frameHeight = (long) members.get("frameHeight");
                        recvVideoTrack.framesReceived = (long) members.get("framesReceived");
                        recvVideoTrack.framesDecoded = (long) members.get("framesDecoded");
                        recvVideoTrack.framesDropped = (long) members.get("framesDropped");
                        recvVideoTrack.freezeCount = (long) members.get("freezeCount");
                        recvVideoTrack.pauseCount = (long) members.get("pauseCount");
                        recvVideoTrack.totalFreezesDuration = (double) members.get("totalFreezesDuration");
                        recvVideoTrack.totalPausesDuration = (double) members.get("totalPausesDuration");
                        recvVideoTrack.totalFramesDuration = (double) members.get("totalFramesDuration");
                        recvVideoTrack.sumOfSquaredFramesDuration = (double) members.get("sumOfSquaredFramesDuration");
                        stats.recvVideoTrack = recvVideoTrack;
                    }
                }else{
                    if (bAudio){
                        SendAudioTrack sendAudioTrack = new SendAudioTrack();
                        sendAudioTrack.trackIdentifier = (String) members.get("trackIdentifier");
                        sendAudioTrack.mediaSourceId = (String) members.get("mediaSourceId");
                        sendAudioTrack.ended = (boolean) members.get("ended");
                        sendAudioTrack.detached = (boolean) members.get("detached");
                        stats.sendAudioTrack = sendAudioTrack;
                    }else{
                        SendVideoTrack sendVideoTrack = new SendVideoTrack();
                        sendVideoTrack.trackIdentifier = (String) members.get("trackIdentifier");
                        sendVideoTrack.mediaSourceId = (String) members.get("mediaSourceId");
                        sendVideoTrack.ended = (boolean) members.get("ended");
                        sendVideoTrack.detached = (boolean) members.get("detached");
                        sendVideoTrack.frameWidth = (long) members.get("frameWidth");
                        sendVideoTrack.frameHeight = (long) members.get("frameHeight");
                        sendVideoTrack.framesSent = (long) members.get("framesSent");
                        sendVideoTrack.hugeFramesSent = (long) members.get("hugeFramesSent");
                        stats.sendVideoTrack = sendVideoTrack;
                    }
                }

            }else if (type.equals("outbound-rtp")){
                boolean bAudio = "audio".equals(members.get("kind"));
                if (bAudio) {
                    AudioOutboundRtp audioOutboundRtp = new AudioOutboundRtp();
                    audioOutboundRtp.ssrc = (long) members.get("ssrc");
                    audioOutboundRtp.trackId = (String) members.get("trackId");
                    audioOutboundRtp.transportId = (String) members.get("transportId");
                    audioOutboundRtp.codecId = (String) members.get("codecId");
                    audioOutboundRtp.mediaSourceId = (String) members.get("mediaSourceId");
                    audioOutboundRtp.packetsSent = (long) members.get("packetsSent");
                    audioOutboundRtp.retransmittedPacketsSent = ((BigInteger) members.get("retransmittedPacketsSent")).longValue();
                    audioOutboundRtp.bytesSent = ((BigInteger) members.get("bytesSent")).longValue();
                    audioOutboundRtp.headerBytesSent = ((BigInteger) members.get("headerBytesSent")).longValue();
                    audioOutboundRtp.retransmittedBytesSent = ((BigInteger) members.get("retransmittedBytesSent")).longValue();
                    stats.audioOutboundRtp = audioOutboundRtp;
                }else{
                    VideoOutboundRtp videoOutboundRtp = new VideoOutboundRtp();
                    videoOutboundRtp.ssrc = (long) members.get("ssrc");
                    videoOutboundRtp.trackId = (String) members.get("trackId");
                    videoOutboundRtp.transportId = (String) members.get("transportId");
                    videoOutboundRtp.codecId = (String) members.get("codecId");
                    videoOutboundRtp.firCount = (long) members.get("firCount");
                    videoOutboundRtp.pliCount = (long) members.get("pliCount");
                    videoOutboundRtp.nackCount = (long) members.get("nackCount");
                    videoOutboundRtp.qpSum = ((BigInteger) members.get("qpSum")).longValue();
                    videoOutboundRtp.mediaSourceId = (String) members.get("mediaSourceId");
                    videoOutboundRtp.packetsSent = (long) members.get("packetsSent");
                    videoOutboundRtp.retransmittedPacketsSent = ((BigInteger) members.get("retransmittedPacketsSent")).longValue();
                    videoOutboundRtp.bytesSent = ((BigInteger) members.get("bytesSent")).longValue();
                    videoOutboundRtp.headerBytesSent = ((BigInteger) members.get("headerBytesSent")).longValue();
                    videoOutboundRtp.retransmittedBytesSent = ((BigInteger) members.get("retransmittedBytesSent")).longValue();
                    videoOutboundRtp.framesEncoded = (long) members.get("framesEncoded");
                    videoOutboundRtp.keyFramesEncoded = (long) members.get("keyFramesEncoded");
                    videoOutboundRtp.totalEncodeTime = (double) members.get("totalEncodeTime");
                    videoOutboundRtp.totalEncodedBytesTarget = ((BigInteger) members.get("totalEncodedBytesTarget")).longValue();
                    videoOutboundRtp.totalPacketSendDelay = (double) members.get("totalPacketSendDelay");
                    videoOutboundRtp.qualityLimitationReason = (String) members.get("qualityLimitationReason");
                    videoOutboundRtp.qualityLimitationResolutionChanges = (long) members.get("qualityLimitationResolutionChanges");
                    videoOutboundRtp.encoderImplementation = (String) members.get("encoderImplementation");
                    stats.videoOutboundRtp = videoOutboundRtp;
                }

            }else if (type.equals("inbound-rtp")){
                boolean bAudio = "audio".equals(members.get("kind"));
                if (bAudio) {
                    AudioInboundRtp audioInboundRtp = new AudioInboundRtp();
                    audioInboundRtp.ssrc = (long) members.get("ssrc");
                    audioInboundRtp.trackId = (String) members.get("trackId");
                    audioInboundRtp.transportId = (String) members.get("transportId");
                    audioInboundRtp.codecId = (String) members.get("codecId");
                    audioInboundRtp.packetsReceived = (long) members.get("packetsReceived");
                    audioInboundRtp.bytesReceived = ((BigInteger) members.get("bytesReceived")).longValue();
                    audioInboundRtp.headerBytesReceived = ((BigInteger) members.get("headerBytesReceived")).longValue();
                    audioInboundRtp.packetsLost = (int) members.get("packetsLost");
                    audioInboundRtp.lastPacketReceivedTimestamp = (double) members.get("lastPacketReceivedTimestamp");
                    audioInboundRtp.jitter = (double) members.get("jitter");
                    stats.audioInboundRtp = audioInboundRtp;
                }else{
                    VideoInboundRtp videoInboundRtp = new VideoInboundRtp();
                    videoInboundRtp.ssrc = (long) members.get("ssrc");
                    videoInboundRtp.trackId = (String) members.get("trackId");
                    videoInboundRtp.transportId = (String) members.get("transportId");
                    videoInboundRtp.codecId = (String) members.get("codecId");
                    videoInboundRtp.firCount = (long) members.get("firCount");
                    videoInboundRtp.pliCount = (long) members.get("pliCount");
                    videoInboundRtp.nackCount = (long) members.get("nackCount");
                    videoInboundRtp.qpSum = ((BigInteger) members.get("qpSum")).longValue();
                    videoInboundRtp.packetsReceived = (long) members.get("packetsReceived");
                    videoInboundRtp.bytesReceived = ((BigInteger) members.get("bytesReceived")).longValue();
                    videoInboundRtp.headerBytesReceived = ((BigInteger) members.get("headerBytesReceived")).longValue();
                    videoInboundRtp.packetsLost = (int) members.get("packetsLost");
                    videoInboundRtp.lastPacketReceivedTimestamp = (double) members.get("lastPacketReceivedTimestamp");
                    videoInboundRtp.framesDecoded = (long) members.get("framesDecoded");
                    videoInboundRtp.keyFramesDecoded = (long) members.get("keyFramesDecoded");
                    videoInboundRtp.totalDecodeTime = (double) members.get("totalDecodeTime");
                    videoInboundRtp.totalInterFrameDelay = (double) members.get("totalInterFrameDelay");
                    videoInboundRtp.totalSquaredInterFrameDelay = (double) members.get("totalSquaredInterFrameDelay");
                    videoInboundRtp.decoderImplementation = (String) members.get("decoderImplementation");
                    stats.videoInboundRtp = videoInboundRtp;
                }

            }

        }

        KLog.p(stats.toString());

        return stats;
    }



    static class Stats{
        // send
        AudioSource audioSource;
        VideoSource videoSource;
        List<Codec> encoderList;
        SendAudioTrack sendAudioTrack;
        SendVideoTrack sendVideoTrack;
        AudioOutboundRtp audioOutboundRtp;
        VideoOutboundRtp videoOutboundRtp;

        //receive
        List<Codec> decoderList;
        AudioInboundRtp audioInboundRtp;
        VideoInboundRtp videoInboundRtp;
        RecvAudioTrack recvAudioTrack;
        RecvVideoTrack recvVideoTrack;

        @Override
        public String toString() {
            return "Stats{" +
                    "\naudioSource=" + audioSource +
                    ", \nvideoSource=" + videoSource +
                    ", \nencoderList=" + encoderList +
                    ", \nsendAudioTrack=" + sendAudioTrack +
                    ", \nsendVideoTrack=" + sendVideoTrack +
                    ", \naudioOutboundRtp=" + audioOutboundRtp +
                    ", \nvideoOutboundRtp=" + videoOutboundRtp +
                    ", \ndecoderList=" + decoderList +
                    ", \naudioInboundRtp=" + audioInboundRtp +
                    ", \nvideoInboundRtp=" + videoInboundRtp +
                    ", \nrecvAudioTrack=" + recvAudioTrack +
                    ", \nrecvVideoTrack=" + recvVideoTrack +
                    '}';
        }
    }


    static class AudioSource{
        String trackIdentifier;
        double audioLevel;
        double totalAudioEnergy;
        double totalSamplesDuration;

        @Override
        public String toString() {
            return "AudioSource{" +
                    "trackIdentifier='" + trackIdentifier + '\'' +
                    ", audioLevel=" + audioLevel +
                    ", totalAudioEnergy=" + totalAudioEnergy +
                    ", totalSamplesDuration=" + totalSamplesDuration +
                    '}';
        }
    }

    static class VideoSource{
        String trackIdentifier;
        long width;
        long height;
        long framesPerSecond;

        @Override
        public String toString() {
            return "VideoSource{" +
                    "trackIdentifier='" + trackIdentifier + '\'' +
                    ", width=" + width +
                    ", height=" + height +
                    ", framesPerSecond=" + framesPerSecond +
                    '}';
        }
    }

    static class Codec{
        String id;
        long payloadType;
        String mimeType;
        long clockRate;

        @Override
        public String toString() {
            return "Codec{" +
                    "id='" + id + '\'' +
                    ", payloadType=" + payloadType +
                    ", mimeType='" + mimeType + '\'' +
                    ", clockRate=" + clockRate +
                    '}';
        }
    }

    static class AudioInboundRtp{
        long ssrc;
        String trackId;
        String transportId;
        String codecId;
        long packetsReceived;
        long bytesReceived;
        long headerBytesReceived;
        int packetsLost;
        double lastPacketReceivedTimestamp;
        double jitter;

        @Override
        public String toString() {
            return "AudioInboundRtp{" +
                    "ssrc=" + ssrc +
                    ", trackId='" + trackId + '\'' +
                    ", transportId='" + transportId + '\'' +
                    ", codecId='" + codecId + '\'' +
                    ", packetsReceived=" + packetsReceived +
                    ", bytesReceived=" + bytesReceived +
                    ", headerBytesReceived=" + headerBytesReceived +
                    ", packetsLost=" + packetsLost +
                    ", lastPacketReceivedTimestamp=" + lastPacketReceivedTimestamp +
                    ", jitter=" + jitter +
                    '}';
        }
    }

    static class VideoInboundRtp{
        long ssrc;
        String trackId;
        String transportId;
        String codecId;
        long firCount;
        long pliCount;
        long nackCount;
        long qpSum;
        long packetsReceived;
        long bytesReceived;
        long headerBytesReceived;
        int packetsLost;
        double lastPacketReceivedTimestamp;
        long framesDecoded;
        long keyFramesDecoded;
        double totalDecodeTime;
        double totalInterFrameDelay;
        double totalSquaredInterFrameDelay;
        String decoderImplementation;

        @Override
        public String toString() {
            return "VideoInboundRtp{" +
                    "ssrc=" + ssrc +
                    ", trackId='" + trackId + '\'' +
                    ", transportId='" + transportId + '\'' +
                    ", codecId='" + codecId + '\'' +
                    ", firCount=" + firCount +
                    ", pliCount=" + pliCount +
                    ", nackCount=" + nackCount +
                    ", qpSum=" + qpSum +
                    ", packetsReceived=" + packetsReceived +
                    ", bytesReceived=" + bytesReceived +
                    ", headerBytesReceived=" + headerBytesReceived +
                    ", packetsLost=" + packetsLost +
                    ", lastPacketReceivedTimestamp=" + lastPacketReceivedTimestamp +
                    ", framesDecoded=" + framesDecoded +
                    ", keyFramesDecoded=" + keyFramesDecoded +
                    ", totalDecodeTime=" + totalDecodeTime +
                    ", totalInterFrameDelay=" + totalInterFrameDelay +
                    ", totalSquaredInterFrameDelay=" + totalSquaredInterFrameDelay +
                    ", decoderImplementation='" + decoderImplementation + '\'' +
                    '}';
        }
    }

    static class AudioOutboundRtp{
        long ssrc;
        String trackId;
        String transportId;
        String codecId;
        String mediaSourceId;
        long packetsSent;
        long retransmittedPacketsSent;
        long bytesSent;
        long headerBytesSent;
        long retransmittedBytesSent;

        @Override
        public String toString() {
            return "AudioOutboundRtp{" +
                    "ssrc=" + ssrc +
                    ", trackId='" + trackId + '\'' +
                    ", transportId='" + transportId + '\'' +
                    ", codecId='" + codecId + '\'' +
                    ", mediaSourceId='" + mediaSourceId + '\'' +
                    ", packetsSent=" + packetsSent +
                    ", retransmittedPacketsSent=" + retransmittedPacketsSent +
                    ", bytesSent=" + bytesSent +
                    ", headerBytesSent=" + headerBytesSent +
                    ", retransmittedBytesSent=" + retransmittedBytesSent +
                    '}';
        }
    }

    static class VideoOutboundRtp{
        long ssrc;
        String trackId;
        String transportId;
        String codecId;
        long firCount;
        long pliCount;
        long nackCount;
        long qpSum;
        String mediaSourceId;
        long packetsSent;
        long retransmittedPacketsSent;
        long bytesSent;
        long headerBytesSent;
        long retransmittedBytesSent;
        long framesEncoded;
        long keyFramesEncoded;
        double totalEncodeTime;
        long totalEncodedBytesTarget;
        double totalPacketSendDelay;
        String qualityLimitationReason;
        long qualityLimitationResolutionChanges;
        String encoderImplementation;

        @Override
        public String toString() {
            return "VideoOutboundRtp{" +
                    "ssrc=" + ssrc +
                    ", trackId='" + trackId + '\'' +
                    ", transportId='" + transportId + '\'' +
                    ", codecId='" + codecId + '\'' +
                    ", firCount=" + firCount +
                    ", pliCount=" + pliCount +
                    ", nackCount=" + nackCount +
                    ", qpSum=" + qpSum +
                    ", mediaSourceId='" + mediaSourceId + '\'' +
                    ", packetsSent=" + packetsSent +
                    ", retransmittedPacketsSent=" + retransmittedPacketsSent +
                    ", bytesSent=" + bytesSent +
                    ", headerBytesSent=" + headerBytesSent +
                    ", retransmittedBytesSent=" + retransmittedBytesSent +
                    ", framesEncoded=" + framesEncoded +
                    ", keyFramesEncoded=" + keyFramesEncoded +
                    ", totalEncodeTime=" + totalEncodeTime +
                    ", totalEncodedBytesTarget=" + totalEncodedBytesTarget +
                    ", totalPacketSendDelay=" + totalPacketSendDelay +
                    ", qualityLimitationReason='" + qualityLimitationReason + '\'' +
                    ", qualityLimitationResolutionChanges=" + qualityLimitationResolutionChanges +
                    ", encoderImplementation='" + encoderImplementation + '\'' +
                    '}';
        }
    }

    static class SendAudioTrack{
        String trackIdentifier;
        String mediaSourceId;
        boolean ended;
        boolean detached;

        @Override
        public String toString() {
            return "SendAudioTrack{" +
                    "trackIdentifier='" + trackIdentifier + '\'' +
                    ", mediaSourceId='" + mediaSourceId + '\'' +
                    ", ended=" + ended +
                    ", detached=" + detached +
                    '}';
        }
    }

    static class SendVideoTrack{
        String trackIdentifier;
        String mediaSourceId;
        boolean ended;
        boolean detached;
        long frameWidth;
        long frameHeight;
        long framesSent;
        long hugeFramesSent;

        @Override
        public String toString() {
            return "SendVideoTrack{" +
                    "trackIdentifier='" + trackIdentifier + '\'' +
                    ", mediaSourceId='" + mediaSourceId + '\'' +
                    ", ended=" + ended +
                    ", detached=" + detached +
                    ", frameWidth=" + frameWidth +
                    ", frameHeight=" + frameHeight +
                    ", framesSent=" + framesSent +
                    ", hugeFramesSent=" + hugeFramesSent +
                    '}';
        }
    }

    static class RecvAudioTrack{
        String trackIdentifier;
        boolean ended;
        boolean detached;
        double jitterBufferDelay;
        long jitterBufferEmittedCount;
        double audioLevel;
        double totalAudioEnergy;
        long totalSamplesReceived;
        double totalSamplesDuration;
        long concealedSamples;
        long silentConcealedSamples;
        long concealmentEvents;
        long insertedSamplesForDeceleration;
        long removedSamplesForAcceleration;
        long jitterBufferFlushes;
        long delayedPacketOutageSamples;
        double relativePacketArrivalDelay;
        long interruptionCount;
        double totalInterruptionDuration;

        @Override
        public String toString() {
            return "RecvAudioTrack{" +
                    "trackIdentifier='" + trackIdentifier + '\'' +
                    ", ended=" + ended +
                    ", detached=" + detached +
                    ", jitterBufferDelay=" + jitterBufferDelay +
                    ", jitterBufferEmittedCount=" + jitterBufferEmittedCount +
                    ", audioLevel=" + audioLevel +
                    ", totalAudioEnergy=" + totalAudioEnergy +
                    ", totalSamplesReceived=" + totalSamplesReceived +
                    ", totalSamplesDuration=" + totalSamplesDuration +
                    ", concealedSamples=" + concealedSamples +
                    ", silentConcealedSamples=" + silentConcealedSamples +
                    ", concealmentEvents=" + concealmentEvents +
                    ", insertedSamplesForDeceleration=" + insertedSamplesForDeceleration +
                    ", removedSamplesForAcceleration=" + removedSamplesForAcceleration +
                    ", jitterBufferFlushes=" + jitterBufferFlushes +
                    ", delayedPacketOutageSamples=" + delayedPacketOutageSamples +
                    ", relativePacketArrivalDelay=" + relativePacketArrivalDelay +
                    ", interruptionCount=" + interruptionCount +
                    ", totalInterruptionDuration=" + totalInterruptionDuration +
                    '}';
        }
    }

    static class RecvVideoTrack{
        String trackIdentifier;
        boolean ended;
        boolean detached;
        double jitterBufferDelay;
        long jitterBufferEmittedCount;
        long frameWidth;
        long frameHeight;
        long framesReceived;
        long framesDecoded;
        long framesDropped;
        long freezeCount;
        long pauseCount;
        double totalFreezesDuration;
        double totalPausesDuration;
        double totalFramesDuration;
        double sumOfSquaredFramesDuration;

        @Override
        public String toString() {
            return "RecvVideoTrack{" +
                    "trackIdentifier='" + trackIdentifier + '\'' +
                    ", ended=" + ended +
                    ", detached=" + detached +
                    ", jitterBufferDelay=" + jitterBufferDelay +
                    ", jitterBufferEmittedCount=" + jitterBufferEmittedCount +
                    ", frameWidth=" + frameWidth +
                    ", frameHeight=" + frameHeight +
                    ", framesReceived=" + framesReceived +
                    ", framesDecoded=" + framesDecoded +
                    ", framesDropped=" + framesDropped +
                    ", freezeCount=" + freezeCount +
                    ", pauseCount=" + pauseCount +
                    ", totalFreezesDuration=" + totalFreezesDuration +
                    ", totalPausesDuration=" + totalPausesDuration +
                    ", totalFramesDuration=" + totalFramesDuration +
                    ", sumOfSquaredFramesDuration=" + sumOfSquaredFramesDuration +
                    '}';
        }
    }

}
