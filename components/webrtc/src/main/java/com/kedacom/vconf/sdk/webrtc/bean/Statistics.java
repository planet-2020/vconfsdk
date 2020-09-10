package com.kedacom.vconf.sdk.webrtc.bean;

/**
 * 统计信息
 * */
public class Statistics {
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

    public static class AudioOutput{
        // 码率。kbit/s
        public int bitrate;
        // 编码格式。EmAudFormat.ordinal()
        public int encodeFormat;
    }

    public static class AudioInput{
        // 收包总数
        public int packetsReceived;
        // 丢包总数
        public int packetsLost;
        // 码率。kbit/s
        public int bitrate;
        // 编码格式。EmAudFormat.ordinal()
        public int encodeFormat;
    }

    public static class VideoOutput{
        // 帧率。fps
        public int framerate;
        // 码率。kbit/s
        public int bitrate;
        // 编码格式。EmVidFormat.ordinal()
        public int encodeFormat;
        // 分辨率。EmMtResolution.ordinal()
        public int resolution;
        // 硬编码器名称。若没有则为软编码
        public String hwencoder;
    }

    public static class VideoInput{
        // 收包总数
        public int packetsReceived;
        // 丢包总数
        public int packetsLost;
        // 帧率。fps
        public int framerate;
        // 码率。kbit/s
        public int bitrate;
        // 编码格式。EmVidFormat.ordinal()
        public int encodeFormat;
        // 分辨率。EmMtResolution.ordinal()
        public int resolution;
        // 硬编码器名称。若没有则为软编码
        public String hwencoder;
    }

}
