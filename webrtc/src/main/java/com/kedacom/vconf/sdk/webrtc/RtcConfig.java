package com.kedacom.vconf.sdk.webrtc;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Rtc配置。
 * NOTE：会议中设置的配置不会立即生效于当次会议，将于下次会议生效。
 * Created by Sissi on 2019/12/30
 */
public final class RtcConfig {

    // 是否开启硬编解
    private static final String key_isVideoCodecHwAccelerationEnabled = "key_isVideoCodecHwAccelerationEnabled";
    // 是否开启simulcast
    private static final String key_isSimulcastEnabled = "key_isSimulcastEnabled";
    // 偏好的视频格式
    private static final String key_preferredVideoCodec = "key_preferredVideoCodec";
    // 偏好的编码视频宽度
    private static final String key_videoWidth = "key_videoWidth";
    // 偏好的编码视频高度
    private static final String key_videoHeight = "key_videoHeight";
    // 偏好的编码视频帧率
    private static final String key_videoFps = "key_videoFps";
    // 最大编码视频码流
    private static final String key_videoMaxBitrate = "key_videoMaxBitrate";
    // 偏好的编码音频格式
    private static final String key_preferredAudioCodec = "key_preferredAudioCodec";
    // 偏好的编码音频码率
    private static final String key_audioStartBitrate = "key_audioStartBitrate";
    // 是否哑音
    private static final String key_isMuted = "key_isMuted";
    // 是否静音
    private static final String key_isSilenced = "key_isSilenced";
    // 本地视频是否开启
    private static final String key_isLocalVideoEnabled = "key_isLocalVideoEnabled";
    // 远端视频是否开启
    private static final String key_isRemoteVideoEnabled = "key_isRemoteVideoEnabled";
    // 是否偏好前置摄像头
    private static final String key_isFrontCameraPreferred = "key_isFrontCameraPreferred";
    // 视频质量偏好
    private static final String key_preferredVideoQuality = "key_preferredVideoQuality";

    // 视频质量定义
    public static final int VideoQuality_Low = 1;
    public static final int VideoQuality_Medium = 2;
    public static final int VideoQuality_High = 3;

    // 媒体编码格式定义
    public static final String VIDEO_CODEC_VP8 = "VP8";
    public static final String VIDEO_CODEC_VP9 = "VP9";
    public static final String VIDEO_CODEC_H264 = "H264";
    public static final String VIDEO_CODEC_H264_BASELINE = "H264 Baseline";
    public static final String VIDEO_CODEC_H264_HIGH = "H264 High";
    public static final String AUDIO_CODEC_OPUS = "opus";
    public static final String AUDIO_CODEC_ISAC = "ISAC";


    private SharedPreferences rtcUserConfig;
    private SharedPreferences.Editor editor;

    private static RtcConfig instance;

    private RtcConfig(Context context){
        rtcUserConfig = context.getSharedPreferences("Config", Context.MODE_PRIVATE);
        editor = rtcUserConfig.edit();
    }

    public static RtcConfig getInstance(Context context){
        if (null == instance){
            instance = new RtcConfig(context);
        }
        return instance;
    }

    /**
     * 设置是否开启硬编解码
     * NOTE:
     * 开启硬编解码后会优先选择硬编解，但若设备本身不支持，则仍可能使用的是软编解；
     * */
    public RtcConfig setVideoCodecHwAccelerationEnable(boolean enable){
        editor.putBoolean(key_isVideoCodecHwAccelerationEnabled, enable).apply();
        return this;
    }
    
    public boolean isVideoCodecHwAccelerationEnabled(){
        return rtcUserConfig.getBoolean(key_isVideoCodecHwAccelerationEnabled, true);
    }


    /**
     * 设置是否开启simulcast
     * NOTE: 即便开启simulcast仍不能保证最终能编出多路码流，实际效果跟设备的编码能力有关。
     * 编码能力包括：支持的编码格式、CPU（软编）或硬件编码器（硬编）是否有能力同时编出指定参数的多路码流。
     * */
    public RtcConfig setSimulcastEnable(boolean enable){
        editor.putBoolean(key_isSimulcastEnabled, enable).apply();
        return this;
    }

    public boolean isSimulcastEnabled(){
        return rtcUserConfig.getBoolean(key_isSimulcastEnabled, true);
    }

    /**
     * 设置视频质量偏好。
     * @param quality {@link #VideoQuality_Low},{@link #VideoQuality_Medium},{@link #VideoQuality_High}
     * */
    public RtcConfig setPreferredVideoQuality(int quality){
        editor.putInt(key_preferredVideoQuality, quality).apply();
        return this;
    }

    public int getPreferredVideoQuality(){
        return rtcUserConfig.getInt(key_preferredVideoQuality, VideoQuality_High);
    }


    /**
     * 设置视频编码格式偏好。
     * @param codec 优先选用的视频编码格式。 取值如{@link #VIDEO_CODEC_H264}, ...
     * */
    public RtcConfig setPreferredVideoCodec(String codec){
        editor.putString(key_preferredVideoCodec, codec).apply();
        return this;
    }

    public String getPreferredVideoCodec(){
        return rtcUserConfig.getString(key_preferredVideoCodec, VIDEO_CODEC_H264);
    }

    /**
     * 设置视频宽度
     * */
    public RtcConfig setVideoWidth(int width){
        editor.putInt(key_videoWidth, width).apply();
        return this;
    }

    public int getVideoWidth(){
        return rtcUserConfig.getInt(key_videoWidth, 1920);
    }

    /**
     * 设置视频高度
     * */
    public RtcConfig setVideoHeight(int height){
        editor.putInt(key_videoHeight, height).apply();
        return this;
    }

    public int getVideoHeight(){
        return rtcUserConfig.getInt(key_videoHeight, 1080);
    }


    /**
     * 设置视频帧率
     * */
    public RtcConfig setVideoFps(int fps){
        editor.putInt(key_videoFps, fps).apply();
        return this;
    }

    public int getVideoFps(){
        return rtcUserConfig.getInt(key_videoFps, 20);
    }


    /**
     * 设置最大视频码率
     * @param bitrate 码率。单位：KB
     * */
    public RtcConfig setVideoMaxBitrate(int bitrate){
        editor.putInt(key_videoMaxBitrate, bitrate).apply();
        return this;
    }

    public int getVideoMaxBitrate(){
        return rtcUserConfig.getInt(key_videoMaxBitrate, 2048);
    }

    /**
     * 设置音频编码格式偏好
     * @param codec 优先选用的音频编码格式。 取值如{@link #AUDIO_CODEC_OPUS}, ...
     * */
    public RtcConfig setPreferredAudioCodec(String codec){
        editor.putString(key_preferredAudioCodec, codec).apply();
        return this;
    }

    public String getPreferredAudioCodec(){
        return rtcUserConfig.getString(key_preferredAudioCodec, AUDIO_CODEC_OPUS);
    }

    /**
     * 设置起始音频码率
     * @param audioStartBitrate 起始音频码率。单位KB
     * */
    public RtcConfig setAudioStartBitrate(int audioStartBitrate){
        editor.putInt(key_audioStartBitrate, audioStartBitrate).apply();
        return this;
    }

    public int getAudioStartBitrate(){
        return rtcUserConfig.getInt(key_audioStartBitrate, 32);
    }

    /**
     * 设置是否哑音
     * */
    public RtcConfig setMute(boolean bMute){
        editor.putBoolean(key_isMuted, bMute).apply();
        return this;
    }

    public boolean isMuted(){
        return rtcUserConfig.getBoolean(key_isMuted, true);
    }

    /**
     * 设置是否静音
     * */
    public RtcConfig setSilence(boolean bSilence){
        editor.putBoolean(key_isSilenced, bSilence).apply();
        return this;
    }

    public boolean isSilenced(){
        return rtcUserConfig.getBoolean(key_isSilenced, true);
    }


    /**
     * 设置是否开启摄像头
     * */
    public RtcConfig setCameraEnable(boolean enable){
        editor.putBoolean(key_isLocalVideoEnabled, enable).apply();
        return this;
    }

    public boolean isCameraEnabled(){
        return rtcUserConfig.getBoolean(key_isLocalVideoEnabled, true);
    }

    /**
     * 设置是否接收视频
     * */
    public RtcConfig setRemoteVideoEnable(boolean enable){
        editor.putBoolean(key_isRemoteVideoEnabled, enable).apply();
        return this;
    }

    public boolean isRemoteVideoEnabled(){
        return rtcUserConfig.getBoolean(key_isRemoteVideoEnabled, true);
    }

    /**
     * 设置是否偏好前置摄像头
     * @param isFrontCameraPreferred true优先使用前置，false优先使用后置
     * */
    public RtcConfig setFrontCameraPreferred(boolean isFrontCameraPreferred){
        editor.putBoolean(key_isFrontCameraPreferred, isFrontCameraPreferred).apply();
        return this;
    }

    public boolean isFrontCameraPreferred(){
        return rtcUserConfig.getBoolean(key_isFrontCameraPreferred, true);
    }


    /**
     * 将RtcConfig对象保存为持久化配置
     * */
    void save(Config config){
        setVideoCodecHwAccelerationEnable(config.isVideoCodecHwAccelerationEnabled);
        setSimulcastEnable(config.isSimulcastEnabled);
        setPreferredVideoCodec(config.preferredVideoCodec);
        setVideoWidth(config.videoWidth);
        setVideoHeight(config.videoHeight);
        setVideoFps(config.videoFps);
        setVideoMaxBitrate(config.videoMaxBitrate);
        setPreferredAudioCodec(config.preferredAudioCodec);
        setAudioStartBitrate(config.audioStartBitrate);
        setMute(config.isMuted);
        setSilence(config.isSilenced);
        setCameraEnable(config.isLocalVideoEnabled);
        setRemoteVideoEnable(config.isRemoteVideoEnabled);
        setFrontCameraPreferred(config.isFrontCameraPreferred);
        setPreferredVideoQuality(config.preferredVideoQuality);
    }


    /**
     * 将持久化的配置导出为RtcConfig对象。
     * */
    Config dump(){
        Config config = new Config();
        config.isVideoCodecHwAccelerationEnabled = isVideoCodecHwAccelerationEnabled();
        config.isSimulcastEnabled = isSimulcastEnabled();
        config.preferredVideoCodec = getPreferredVideoCodec();
        config.videoWidth = getVideoWidth();
        config.videoHeight = getVideoHeight();
        config.videoFps = getVideoFps();
        config.videoMaxBitrate = getVideoMaxBitrate();
        config.preferredAudioCodec = getPreferredAudioCodec();
        config.audioStartBitrate = getAudioStartBitrate();
        config.isMuted = isMuted();
        config.isSilenced = isSilenced();
        config.isLocalVideoEnabled = isCameraEnabled();
        config.isRemoteVideoEnabled = isRemoteVideoEnabled();
        config.isFrontCameraPreferred = isFrontCameraPreferred();
        config.preferredVideoQuality = getPreferredVideoQuality();

        return config;
    }

    /**
     * RTC配置
     * */
    static class Config {

        // 是否开启硬编解
        boolean isVideoCodecHwAccelerationEnabled;
        // 是否开启simulcast
        boolean isSimulcastEnabled;
        // 偏好的视频格式
        String preferredVideoCodec;
        // 偏好的编码视频宽度
        int videoWidth;
        // 偏好的编码视频高度
        int videoHeight;
        // 偏好的编码视频帧率
        int videoFps;
        // 最大编码视频码流
        int videoMaxBitrate;
        // 偏好的编码音频格式
        String preferredAudioCodec;
        // 音频起始码率
        int audioStartBitrate;
        // 是否哑音
        boolean isMuted;
        // 是否静音
        boolean isSilenced;
        // 本地视频是否开启
        boolean isLocalVideoEnabled;
        // 远端视频是否开启
        boolean isRemoteVideoEnabled;
        // 是否偏好前置摄像头
        boolean isFrontCameraPreferred;
        // 视频质量偏好
        int preferredVideoQuality;


        void copy(Config src){
            isVideoCodecHwAccelerationEnabled = src.isVideoCodecHwAccelerationEnabled;
            isSimulcastEnabled = src.isSimulcastEnabled;
            preferredVideoCodec = src.preferredVideoCodec;
            videoWidth = src.videoWidth;
            videoHeight = src.videoHeight;
            videoFps = src.videoFps;
            videoMaxBitrate = src.videoMaxBitrate;
            preferredAudioCodec = src.preferredAudioCodec;
            audioStartBitrate = src.audioStartBitrate;
            isMuted = src.isMuted;
            isSilenced = src.isSilenced;
            isLocalVideoEnabled = src.isLocalVideoEnabled;
            isRemoteVideoEnabled = src.isRemoteVideoEnabled;
            isFrontCameraPreferred = src.isFrontCameraPreferred;
            preferredVideoQuality = src.preferredVideoQuality;
        }


        @Override
        public String toString() {
            return "Config{" +
                    "isVideoCodecHwAccelerationEnabled=" + isVideoCodecHwAccelerationEnabled +
                    ", isSimulcastEnabled=" + isSimulcastEnabled +
                    ", preferredVideoCodec='" + preferredVideoCodec + '\'' +
                    ", videoWidth=" + videoWidth +
                    ", videoHeight=" + videoHeight +
                    ", videoFps=" + videoFps +
                    ", videoMaxBitrate=" + videoMaxBitrate +
                    ", preferredAudioCodec='" + preferredAudioCodec + '\'' +
                    ", audioStartBitrate=" + audioStartBitrate +
                    ", isMuted=" + isMuted +
                    ", isSilenced=" + isSilenced +
                    ", isLocalVideoEnabled=" + isLocalVideoEnabled +
                    ", isRemoteVideoEnabled=" + isRemoteVideoEnabled +
                    ", isFrontCameraPreferred=" + isFrontCameraPreferred +
                    ", preferredVideoQuality=" + preferredVideoQuality +
                    '}';
        }


    }
}
