package com.kedacom.vconf.sdk.webrtc.bean;

import com.annimon.stream.Stream;

import java.util.ArrayList;
import java.util.List;

/**
 * 统计信息
 * */
public class Statistics {

    // 媒体编码格式定义
    public static final String VP8 = "vp8";
    public static final String VP9 = "vp9";
    public static final String H264 = "h264";
    public static final String H264_BASELINE = "h264 baseline";
    public static final String H264_HIGH = "h264 high";
    public static final String OPUS = "opus";
    public static final String ISAC = "isac";
    public static final String G722 = "g722";
    public static final String UNKNOWN = "unknown";

    public List<ConfereeRelated> confereeRelated = new ArrayList<>();
    public Common common;

    private static String mime2CodecName(String mime){
        mime = mime.toLowerCase();
        switch (mime){
            case "audio/opus":
                return OPUS;
            case "audio/g722":
                return G722;
            case "video/h264":
                return H264;
            case "video/vp8":
                return VP8;
            default:
                return UNKNOWN;
        }

    }

    public Statistics.ConfereeRelated findMaxAudioLevel(){
        return Stream.of(confereeRelated).max((o1, o2) -> {
            if (o1 == o2){
                return 0;
            } else {
                if (o1==null){
                    return -1;
                }else if (o2 ==null){
                    return 1;
                }else{
                    AudioInfo audioInfo1 = o1.audioInfo;
                    AudioInfo audioInfo2 = o2.audioInfo;
                    if (audioInfo1 == audioInfo2){
                        return 0;
                    }else {
                        if (audioInfo1==null){
                            return -1;
                        }else if (audioInfo2==null){
                            return 1;
                        }else {
                            return audioInfo1.audioLevel - audioInfo2.audioLevel;
                        }
                    }
                }
            }
        }).orElse(null);
    }

    public void clear(){
        confereeRelated.clear();
        common = null;
    }

    public static class ConfereeRelated{
        // 该统计信息所属与会方ID。
        public String confereeId;
        // 音频信息。
        public AudioInfo audioInfo;
        // 视频信息。
        public VideoInfo videoInfo;

        public ConfereeRelated(String confereeId, AudioInfo audioInfo, VideoInfo videoInfo) {
            this.confereeId = confereeId;
            this.audioInfo = audioInfo;
            this.videoInfo = videoInfo;
        }

        @Override
        public String toString() {
            return "ConfereeRelated{" +
                    "confereeId='" + confereeId + '\'' +
                    ", audioInfo=" + audioInfo +
                    ", videoInfo=" + videoInfo +
                    '}';
        }

    }

    public static class Common{
        // 混音
        public AudioInfo mixedAudio;

        public Common(AudioInfo mixedAudio) {
            this.mixedAudio = mixedAudio;
        }

        @Override
        public String toString() {
            return "Common{" +
                    "\nmixedAudio=" + mixedAudio +
                    "\n}\n";
        }
    }



    public static class AudioInfo{
        /** 编码格式 */
        public String encodeFormat;
        /** 码率。单位kbit/s*/
        public int bitrate;
        /** 音量[0, 100] */
        public int audioLevel;

        /**
         * NOTE: 以下字段仅在接收通道有效
         */
        /** 收包总数 */
        public long packetsReceived;
        /** 丢包总数 */
        public long packetsLost;
        /** 实时丢包率[0, 100]
         * NOTE：此丢包率是实时的，并非packetsLost/(packetsReceived+packetsLost)，那是总的丢包率。
         * */
        public int realtimeLostRate;

        public AudioInfo(String mime, int bitrate, int audioLevel) {
            this(mime, bitrate, audioLevel, 0, 0, 0);
        }

        public AudioInfo(String mime, int bitrate, int audioLevel, long packetsReceived, long packetsLost, int realtimeLostRate) {
            this.encodeFormat = mime2CodecName(mime);
            this.bitrate = bitrate;
            this.audioLevel = audioLevel;
            this.packetsReceived = packetsReceived;
            this.packetsLost = packetsLost;
            this.realtimeLostRate = realtimeLostRate;
        }

        @Override
        public String toString() {
            return "AudioInfo{" +
                    "encodeFormat='" + encodeFormat + '\'' +
                    ", bitrate=" + bitrate +
                    ", audioLevel=" + audioLevel +
                    ", packetsReceived=" + packetsReceived +
                    ", packetsLost=" + packetsLost +
                    ", realtimeLostRate=" + realtimeLostRate +
                    '}';
        }

    }

    public static class VideoInfo{
        /** 编码格式*/
        public String encodeFormat;
        /** 编码器名称*/
        public String encoder;
        /** 帧宽*/
        public int width;
        /** 帧高*/
        public int height;
        /** 帧率。fps*/
        public int framerate;
        /** 码率。kbit/s*/
        public int bitrate;

        /**
         * NOTE: 以下字段仅在接收通道有效
         */
        /** 收包总数*/
        public long packetsReceived;
        /** 丢包总数*/
        public long packetsLost;
        /** 实时丢包率[0, 100]*/
        public int realtimeLostRate;

        public VideoInfo(String mime, String encoder, int width, int height, int framerate, int bitrate) {
            this(mime, encoder, width, height, framerate, bitrate, 0, 0, 0);
        }

        public VideoInfo(String mime, String encoder, int width, int height, int framerate, int bitrate, long packetsReceived, long packetsLost, int realtimeLostRate) {
            this.encodeFormat = mime2CodecName(mime);
            this.encoder = encoder;
            this.width = width;
            this.height = height;
            this.framerate = framerate;
            this.bitrate = bitrate;
            this.packetsReceived = packetsReceived;
            this.packetsLost = packetsLost;
            this.realtimeLostRate = realtimeLostRate;
        }

        @Override
        public String toString() {
            return "VideoInfo{" +
                    "encodeFormat='" + encodeFormat + '\'' +
                    ", encoder='" + encoder + '\'' +
                    ", width=" + width +
                    ", height=" + height +
                    ", framerate=" + framerate +
                    ", bitrate=" + bitrate +
                    ", packetsReceived=" + packetsReceived +
                    ", packetsLost=" + packetsLost +
                    ", realtimeLostRate=" + realtimeLostRate +
                    '}';
        }
    }


    @Override
    public String toString() {
        return "Statistics{" +
                "\nconfereeRelated=" + confereeRelated +
                ", \ncommon=" + common +
                "\n}\n";
    }

}
