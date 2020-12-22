package com.kedacom.vconf.sdk.webrtc;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaRecorder;

/**
 * Rtc配置。
 * NOTE：会议中设置的配置不会立即生效于当次会议，将于下次会议生效。
 * Created by Sissi on 2019/12/30
 */
public final class RtcConfig {

    // 是否优先使用视频硬编
    private static final String key_isHardwareVideoEncoderPreferred = "key_isHardwareVideoEncoderPreferred";
    // 是否优先使用视频硬解
    private static final String key_isHardwareVideoDecoderPreferred = "key_isHardwareVideoDecoderPreferred";
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
    // 偏好的辅视频流编码宽度
    private static final String key_assVideoWidth = "key_assVideoWidth";
    // 偏好的辅视频流编码高度
    private static final String key_assVideoHeight = "key_assVideoHeight";
    // 偏好的辅视频流帧率
    private static final String key_assVideoFps = "key_assVideoFps";
    // 最大编码视频码率
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
    // 是否优先尝试使用android平台内置的AEC（如果没有内置的再选用webrtc自带的）
    private static final String key_isBuiltInAECPreferred = "key_isBuiltInAECPreferred";
    // 是否优先尝试使用android平台内置的NS（如果没有内置的再选用webrtc自带的）
    private static final String key_isBuiltInNSPreferred = "key_isBuiltInNSPreferred";
    // 是否dump AEC数据
    private static final String key_isAECDumpEnabled = "key_isAECDumpEnabled";
    // 本端音轨音量
    private static final String key_localAudioVolume = "key_localAudioVolume";
    // 远端音轨音量
    private static final String key_remoteAudioVolume = "key_remoteAudioVolume";
    // 是否保存发送的辅视频流
    private static final String key_saveSentAssVideo = "key_saveSentAssVideo";
    // 是否保存接收的辅视频流
    private static final String key_saveRecvedAssVideo = "key_saveRecvedAssVideo";
    // 是否保存发送的主视频流
    private static final String key_saveSentMainVideo = "key_saveSentMainVideo";
    // 是否保存接收的主视频流
    private static final String key_saveRecvedMainVideo = "key_saveRecvedMainVideo";
    // 音频采集源
    private static final String key_audioSource = "key_audioSource";
    // AGC level
    private static final String key_agcLevel = "key_agcLevel";

    // 视频质量定义
    static final int VideoQuality_Unknown = 0;
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
        rtcUserConfig = context.getSharedPreferences("WebRTCConfig", Context.MODE_PRIVATE);
        editor = rtcUserConfig.edit();
    }

    public static RtcConfig getInstance(Context context){
        if (null == instance){
            instance = new RtcConfig(context);
        }
        return instance;
    }

    /**
     * 设置是否优先使用视频硬编
     * NOTE: 若设备本身不支持指定视频格式的硬编，则会尝试使用软编；
     * */
    public RtcConfig setHardwareVideoEncoderPreferred(boolean prefer){
        editor.putBoolean(key_isHardwareVideoEncoderPreferred, prefer).apply();
        return this;
    }
    
    public boolean isHardwareVideoEncoderPreferred(){
        return rtcUserConfig.getBoolean(key_isHardwareVideoEncoderPreferred, true);
    }

    /**
     * 设置是否优先使用视频硬解
     * NOTE: 若设备本身不支持指定视频格式的硬解，则会尝试使用软解；
     * */
    public RtcConfig setHardwareVideoDecoderPreferred(boolean prefer){
        editor.putBoolean(key_isHardwareVideoDecoderPreferred, prefer).apply();
        return this;
    }

    public boolean isHardwareVideoDecoderPreferred(){
        return rtcUserConfig.getBoolean(key_isHardwareVideoDecoderPreferred, true);
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
     * 设置视频采集宽
     * */
    public RtcConfig setVideoWidth(int width){
        editor.putInt(key_videoWidth, width).apply();
        return this;
    }

    public int getVideoWidth(){
        return rtcUserConfig.getInt(key_videoWidth, 1280);
    }

    /**
     * 设置视频采集高
     * */
    public RtcConfig setVideoHeight(int height){
        editor.putInt(key_videoHeight, height).apply();
        return this;
    }

    public int getVideoHeight(){
        return rtcUserConfig.getInt(key_videoHeight, 720);
    }


    /**
     * 设置视频采集帧率
     * */
    public RtcConfig setVideoFps(int fps){
        editor.putInt(key_videoFps, fps).apply();
        return this;
    }

    public int getVideoFps(){
        return rtcUserConfig.getInt(key_videoFps, 30);
    }


    /**
     * 设置辅视频采集宽
     * */
    public RtcConfig setAssVideoWidth(int width){
        editor.putInt(key_assVideoWidth, width).apply();
        return this;
    }

    public int getAssVideoWidth(){
        return rtcUserConfig.getInt(key_assVideoWidth, 1280);
    }

    /**
     * 设置辅视频采集高
     * */
    public RtcConfig setAssVideoHeight(int height){
        editor.putInt(key_assVideoHeight, height).apply();
        return this;
    }

    public int getAssVideoHeight(){
        return rtcUserConfig.getInt(key_assVideoHeight, 720);
    }


    /**
     * 设置辅视频采集帧率
     * */
    public RtcConfig setAssVideoFps(int fps){
        editor.putInt(key_assVideoFps, fps).apply();
        return this;
    }

    public int getAssVideoFps(){
        return rtcUserConfig.getInt(key_assVideoFps, 15);
    }


    /**
     * 设置最大视频发送码率
     * @param bitrate 码率。单位：kbps（b代表bit非byte）
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
        return rtcUserConfig.getBoolean(key_isMuted, false);
    }

    /**
     * 设置是否静音
     * */
    public RtcConfig setSilence(boolean bSilence){
        editor.putBoolean(key_isSilenced, bSilence).apply();
        return this;
    }

    public boolean isSilenced(){
        return rtcUserConfig.getBoolean(key_isSilenced, false);
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

    /** 是否优先尝试使用android平台内置的AEC——回声取消（如果没有内置的再选用webrtc自带的）*/
    public RtcConfig setBuiltInAECPreferred(boolean prefer){
        editor.putBoolean(key_isBuiltInAECPreferred, prefer).apply();
        return this;
    }

    public boolean isBuiltInAECPreferred(){
        return rtcUserConfig.getBoolean(key_isBuiltInAECPreferred, false);
    }

    /** 是否优先尝试使用android平台内置的NS——噪声抑制（如果没有内置的再选用webrtc自带的）*/
    public RtcConfig setBuiltInNSPreferred(boolean prefer){
        editor.putBoolean(key_isBuiltInNSPreferred, prefer).apply();
        return this;
    }

    public boolean isBuiltInNSPreferred(){
        return rtcUserConfig.getBoolean(key_isBuiltInNSPreferred, false);
    }

    /** 是否dump AEC数据*/
    public RtcConfig setAECDumpEnable(boolean enable){
        editor.putBoolean(key_isAECDumpEnabled, enable).apply();
        return this;
    }

    public boolean isAECDumpEnabled(){
        return rtcUserConfig.getBoolean(key_isAECDumpEnabled, false);
    }


    /**
     * 设置本端音轨音量
     * @param volume 音量，范围0-100。若小于0则取0，若大于100则取100.
     * */
    public RtcConfig setLocalAudioVolume(int volume){
        volume = Math.min(volume, 100);
        volume = Math.max(volume, 0);
        editor.putInt(key_localAudioVolume, volume).apply();
        return this;
    }

    public int getLocalAudioVolume(){
        return rtcUserConfig.getInt(key_localAudioVolume,
                5 // 媒控建议的数值
        );
    }

    /**
     * 设置远端音轨音量
     * @param volume 音量，范围0-100。若小于0则取0，若大于100则取100.
     * */
    public RtcConfig setRemoteAudioVolume(int volume){
        volume = Math.min(volume, 100);
        volume = Math.max(volume, 0);
        editor.putInt(key_remoteAudioVolume, volume).apply();
        return this;
    }

    public int getRemoteAudioVolume(){
        return rtcUserConfig.getInt(key_remoteAudioVolume,
                2 // 媒控建议的数值
        );
    }


    /**
     * 设置是否保存发送的主视频流
     * 生成的文件保存在sdcard/应用目录/files/webrtc
     * */
    public RtcConfig setSaveSentMainVideo(boolean save){
        editor.putBoolean(key_saveSentMainVideo, save).apply();
        return this;
    }

    public boolean getSaveSentMainVideo(){
        return rtcUserConfig.getBoolean(key_saveSentMainVideo, false);
    }

    /**
     * 设置是否保存接收的主视频流
     * 生成的文件保存在sdcard/应用目录/files/webrtc
     * */
    public RtcConfig setSaveRecvedMainVideo(boolean save){
        editor.putBoolean(key_saveRecvedMainVideo, save).apply();
        return this;
    }

    public boolean getSaveRecvedMainVideo(){
        return rtcUserConfig.getBoolean(key_saveRecvedMainVideo, false);
    }


    /**
     * 设置是否保存发送的辅视频流
     * 生成的文件保存在sdcard/应用目录/files/webrtc
     * */
    public RtcConfig setSaveSentAssVideo(boolean save){
        editor.putBoolean(key_saveSentAssVideo, save).apply();
        return this;
    }

    public boolean getSaveSentAssVideo(){
        return rtcUserConfig.getBoolean(key_saveSentAssVideo, false);
    }

    /**
     * 设置是否保存接收的辅视频流
     * 生成的文件保存在sdcard/应用目录/files/webrtc
     * */
    public RtcConfig setSaveRecvedAssVideo(boolean save){
        editor.putBoolean(key_saveRecvedAssVideo, save).apply();
        return this;
    }

    public boolean getSaveRecvedAssVideo(){
        return rtcUserConfig.getBoolean(key_saveRecvedAssVideo, false);
    }


    /**
     * 设置音频采集源
     * @param source {@link android.media.MediaRecorder.AudioSource}#VOICE_SOURCE_DEFINATION
     * */
    public RtcConfig setAudioSource(int source){
        editor.putInt(key_audioSource, source).apply();
        return this;
    }

    public int getAudioSource(){
        return rtcUserConfig.getInt(key_audioSource, MediaRecorder.AudioSource.VOICE_COMMUNICATION);
    }


    /**
     * 设置AGC level
     * @param level [0, 31]，值越大采集的麦克风音量越小，回声效果好。建议 12-30，老版本是12
     * */
    public RtcConfig setAgcLevel(int level){
        editor.putInt(key_agcLevel, level).apply();
        return this;
    }

    public int getAgcLevel(){
        return rtcUserConfig.getInt(key_agcLevel, 12);
    }


    /**
     * 将RtcConfig对象保存为持久化配置
     * */
    void save(Config config){
        setHardwareVideoEncoderPreferred(config.isHardwareVideoEncoderPreferred);
        setHardwareVideoDecoderPreferred(config.isHardwareVideoDecoderPreferred);
        setSimulcastEnable(config.isSimulcastEnabled);
        setPreferredVideoCodec(config.preferredVideoCodec);
        setVideoWidth(config.videoWidth);
        setVideoHeight(config.videoHeight);
        setVideoFps(config.videoFps);
        setAssVideoWidth(config.assVideoWidth);
        setAssVideoHeight(config.assVideoHeight);
        setAssVideoFps(config.assVideoFps);
        setVideoMaxBitrate(config.videoMaxBitrate);
        setPreferredAudioCodec(config.preferredAudioCodec);
        setAudioStartBitrate(config.audioStartBitrate);
        setMute(config.isMuted);
        setSilence(config.isSilenced);
        setCameraEnable(config.isLocalVideoEnabled);
        setRemoteVideoEnable(config.isRemoteVideoEnabled);
        setFrontCameraPreferred(config.isFrontCameraPreferred);
        setPreferredVideoQuality(config.preferredVideoQuality);
        setBuiltInAECPreferred(config.isBuiltInAECPreferred);
        setBuiltInNSPreferred(config.isBuiltInNSPreferred);
        setAECDumpEnable(config.isAECDumpEnabled);
        setLocalAudioVolume(config.inputAudioVolume);
        setRemoteAudioVolume(config.outputAudioVolume);
        setSaveSentMainVideo(config.saveSentMainVideo);
        setSaveRecvedMainVideo(config.saveRecvedMainVideo);
        setSaveSentAssVideo(config.saveSentAssVideo);
        setSaveRecvedAssVideo(config.saveRecvedAssVideo);
        setAudioSource(config.audioSource);
        setAgcLevel(config.agcLevel);
    }


    /**
     * 将持久化的配置导出为RtcConfig对象。
     * */
    Config dump(){
        Config config = new Config();
        config.isHardwareVideoEncoderPreferred = isHardwareVideoEncoderPreferred();
        config.isHardwareVideoDecoderPreferred = isHardwareVideoDecoderPreferred();
        config.isSimulcastEnabled = isSimulcastEnabled();
        config.preferredVideoCodec = getPreferredVideoCodec();
        config.videoWidth = getVideoWidth();
        config.videoHeight = getVideoHeight();
        config.videoFps = getVideoFps();
        config.assVideoWidth = getAssVideoWidth();
        config.assVideoHeight = getAssVideoHeight();
        config.assVideoFps = getAssVideoFps();
        config.videoMaxBitrate = getVideoMaxBitrate();
        config.preferredAudioCodec = getPreferredAudioCodec();
        config.audioStartBitrate = getAudioStartBitrate();
        config.isMuted = isMuted();
        config.isSilenced = isSilenced();
        config.isLocalVideoEnabled = isCameraEnabled();
        config.isRemoteVideoEnabled = isRemoteVideoEnabled();
        config.isFrontCameraPreferred = isFrontCameraPreferred();
        config.preferredVideoQuality = getPreferredVideoQuality();
        config.isBuiltInAECPreferred = isBuiltInAECPreferred();
        config.isBuiltInNSPreferred = isBuiltInNSPreferred();
        config.isAECDumpEnabled = isAECDumpEnabled();
        config.inputAudioVolume = getLocalAudioVolume();
        config.outputAudioVolume = getRemoteAudioVolume();
        config.saveSentMainVideo = getSaveSentMainVideo();
        config.saveRecvedMainVideo = getSaveRecvedMainVideo();
        config.saveSentAssVideo = getSaveSentAssVideo();
        config.saveRecvedAssVideo = getSaveRecvedAssVideo();
        config.audioSource = getAudioSource();
        config.agcLevel = getAgcLevel();

        return config;
    }

    /**
     * RTC配置
     * */
    static class Config {

        // 是否开启视频硬编
        boolean isHardwareVideoEncoderPreferred;
        // 是否开启视频硬解
        boolean isHardwareVideoDecoderPreferred;
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
        // 偏好的辅视频编码宽度
        int assVideoWidth;
        // 偏好的辅视频编码高度
        int assVideoHeight;
        // 偏好的辅视频帧率
        int assVideoFps;
        // 最大编码视频码流。单位：kbps（注意：b代表bit而非byte）
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

        // 是否优先尝试使用android平台内置的AEC——回声取消（如果没有内置的再选用webrtc自带的）
        boolean isBuiltInAECPreferred;
        // 是否优先尝试使用android平台内置的NS——噪声抑制（如果没有内置的再选用webrtc自带的）
        boolean isBuiltInNSPreferred;
        // 是否Dump AEC数据
        boolean isAECDumpEnabled;
        // 输入音量
        int inputAudioVolume;
        // 输出音量
        int outputAudioVolume;
        // 是否保存发送的主视频流
        boolean saveSentMainVideo;
        // 是否保存接收的主视频流
        boolean saveRecvedMainVideo;
        // 是否保存发送的辅视频流
        boolean saveSentAssVideo;
        // 是否保存接收的辅视频流
        boolean saveRecvedAssVideo;
        // 音频采集源
        int audioSource;
        // AGC level
        int agcLevel;


        void copy(Config src){
            isHardwareVideoEncoderPreferred = src.isHardwareVideoEncoderPreferred;
            isHardwareVideoDecoderPreferred = src.isHardwareVideoDecoderPreferred;
            isSimulcastEnabled = src.isSimulcastEnabled;
            preferredVideoCodec = src.preferredVideoCodec;
            videoWidth = src.videoWidth;
            videoHeight = src.videoHeight;
            videoFps = src.videoFps;
            assVideoWidth = src.assVideoWidth;
            assVideoHeight = src.assVideoHeight;
            assVideoFps = src.assVideoFps;
            videoMaxBitrate = src.videoMaxBitrate;
            preferredAudioCodec = src.preferredAudioCodec;
            audioStartBitrate = src.audioStartBitrate;
            isMuted = src.isMuted;
            isSilenced = src.isSilenced;
            isLocalVideoEnabled = src.isLocalVideoEnabled;
            isRemoteVideoEnabled = src.isRemoteVideoEnabled;
            isFrontCameraPreferred = src.isFrontCameraPreferred;
            preferredVideoQuality = src.preferredVideoQuality;
            isBuiltInAECPreferred = src.isBuiltInAECPreferred;
            isBuiltInNSPreferred = src.isBuiltInNSPreferred;
            isAECDumpEnabled = src.isAECDumpEnabled;
            inputAudioVolume = src.inputAudioVolume;
            outputAudioVolume = src.outputAudioVolume;
            saveSentMainVideo = src.saveSentMainVideo;
            saveRecvedMainVideo = src.saveRecvedMainVideo;
            saveSentAssVideo = src.saveSentAssVideo;
            saveRecvedAssVideo = src.saveRecvedAssVideo;
            audioSource = src.audioSource;
            agcLevel = src.agcLevel;
        }

        @Override
        public String toString() {
            return "Config{" +
                    "isHardwareVideoEncoderPreferred=" + isHardwareVideoEncoderPreferred +
                    ", isHardwareVideoDecoderPreferred=" + isHardwareVideoDecoderPreferred +
                    ", isSimulcastEnabled=" + isSimulcastEnabled +
                    ", preferredVideoCodec='" + preferredVideoCodec + '\'' +
                    ", videoWidth=" + videoWidth +
                    ", videoHeight=" + videoHeight +
                    ", videoFps=" + videoFps +
                    ", assVideoWidth=" + assVideoWidth +
                    ", assVideoHeight=" + assVideoHeight +
                    ", assVideoFps=" + assVideoFps +
                    ", videoMaxBitrate=" + videoMaxBitrate +
                    ", preferredAudioCodec='" + preferredAudioCodec + '\'' +
                    ", audioStartBitrate=" + audioStartBitrate +
                    ", isMuted=" + isMuted +
                    ", isSilenced=" + isSilenced +
                    ", isLocalVideoEnabled=" + isLocalVideoEnabled +
                    ", isRemoteVideoEnabled=" + isRemoteVideoEnabled +
                    ", isFrontCameraPreferred=" + isFrontCameraPreferred +
                    ", preferredVideoQuality=" + preferredVideoQuality +
                    ", isBuiltInAECPreferred=" + isBuiltInAECPreferred +
                    ", isBuiltInNSPreferred=" + isBuiltInNSPreferred +
                    ", isAECDumpEnabled=" + isAECDumpEnabled +
                    ", inputAudioVolume=" + inputAudioVolume +
                    ", outputAudioVolume=" + outputAudioVolume +
                    ", saveSentMainVideo=" + saveSentMainVideo +
                    ", saveRecvedMainVideo=" + saveRecvedMainVideo +
                    ", saveSentAssVideo=" + saveSentAssVideo +
                    ", saveRecvedAssVideo=" + saveRecvedAssVideo +
                    ", audioSource=" + audioSource +
                    ", agcLevel=" + agcLevel +
                    '}';
        }
    }

}
