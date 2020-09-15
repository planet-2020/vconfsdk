package com.kedacom.vconf.sdk.webrtc.bean;

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

    public void clear(){
        confereeRelated.clear();
        common = null;
    }

    public static class ConfereeRelated{
        // 该统计信息所属与会方ID。
        public String confereeId;
        // 音频输出信息。
        public AudioOutput audioOutput;
        // 音频输入信息。
        public AudioInput audioInput;
        // 视频输出信息。
        public VideoOutput videoOutput;
        // 视频输入信息。
        public VideoInput videoInput;

        public ConfereeRelated(String confereeId, AudioOutput audioOutput, VideoOutput videoOutput, AudioInput audioInput, VideoInput videoInput) {
            this.confereeId = confereeId;
            this.audioOutput = audioOutput;
            this.audioInput = audioInput;
            this.videoOutput = videoOutput;
            this.videoInput = videoInput;
        }

        @Override
        public String toString() {
            return "ConfereeRelated{" +
                    "confereeId='" + confereeId + '\'' +
                    ", audioOutput=" + audioOutput +
                    ", audioInput=" + audioInput +
                    ", videoOutput=" + videoOutput +
                    ", videoInput=" + videoInput +
                    "}\n";
        }
    }

    public static class Common{
        // 混音
        public AudioInput mixedAudio;

        public Common(AudioInput mixedAudio) {
            this.mixedAudio = mixedAudio;
        }

        @Override
        public String toString() {
            return "Common{" +
                    "mixedAudio=" + mixedAudio +
                    "}\n";
        }
    }

    public static class AudioOutput{
        // 码率。kbit/s
        public int bitrate;
        // 编码格式。
        public String encodeFormat;

        public AudioOutput(int bitrate, String mime) {
            this.bitrate = bitrate;
            this.encodeFormat = mime2CodecName(mime);
        }

        @Override
        public String toString() {
            return "AudioOutput{" +
                    "bitrate=" + bitrate +
                    ", encodeFormat=" + encodeFormat +
                    "}\n";
        }
    }

    public static class AudioInput{
        // 收包总数
        public long packetsReceived;
        // 丢包总数
        public long packetsLost;
        // 码率。kbit/s
        public int bitrate;
        // 编码格式。
        public String encodeFormat;

        public AudioInput(long packetsReceived, long packetsLost, int bitrate, String mime) {
            this.packetsReceived = packetsReceived;
            this.packetsLost = packetsLost;
            this.bitrate = bitrate;
            this.encodeFormat = mime2CodecName(mime);
        }

        @Override
        public String toString() {
            return "AudioInput{" +
                    "packetsReceived=" + packetsReceived +
                    ", packetsLost=" + packetsLost +
                    ", bitrate=" + bitrate +
                    ", encodeFormat=" + encodeFormat +
                    "}\n";
        }
    }

    public static class VideoOutput{
        // 帧率。fps
        public int framerate;
        // 码率。kbit/s
        public int bitrate;
        // 编码格式。
        public String encodeFormat;
        // 帧宽
        public int width;
        // 帧高
        public int height;
        // 编码器名称
        public String encoder;

        public VideoOutput(int framerate, int width, int height, int bitrate, String mime, String encoder) {
            this.framerate = framerate;
            this.bitrate = bitrate;
            this.encodeFormat = mime2CodecName(mime);
            this.width = width;
            this.height = height;
            this.encoder = encoder;
        }

        @Override
        public String toString() {
            return "VideoOutput{" +
                    "framerate=" + framerate +
                    ", bitrate=" + bitrate +
                    ", encodeFormat=" + encodeFormat +
                    ", width=" + width +
                    ", height=" + height +
                    ", encoder='" + encoder + '\'' +
                    "}\n";
        }
    }

    public static class VideoInput{
        // 收包总数
        public long packetsReceived;
        // 丢包总数
        public long packetsLost;
        // 帧率。fps
        public int framerate;
        // 码率。kbit/s
        public int bitrate;
        // 编码格式。
        public String encodeFormat;
        // 帧宽
        public int width;
        // 帧高
        public int height;
        // 编码器名称。
        public String encoder;

        public VideoInput(int framerate, int width, int height, long packetsReceived, long packetsLost, int bitrate, String mime, String encoder) {
            this.packetsReceived = packetsReceived;
            this.packetsLost = packetsLost;
            this.framerate = framerate;
            this.bitrate = bitrate;
            this.encodeFormat = mime2CodecName(mime);
            this.width = width;
            this.height = height;
            this.encoder = encoder;
        }

        @Override
        public String toString() {
            return "VideoInput{" +
                    "packetsReceived=" + packetsReceived +
                    ", packetsLost=" + packetsLost +
                    ", framerate=" + framerate +
                    ", bitrate=" + bitrate +
                    ", encodeFormat=" + encodeFormat +
                    ", width=" + width +
                    ", height=" + height +
                    ", encoder='" + encoder + '\'' +
                    "}\n";
        }
    }

    @Override
    public String toString() {
        return "Statistics{" +
                "confereeRelated=" + confereeRelated +
                ", common=" + common +
                "}\n";
    }
}
