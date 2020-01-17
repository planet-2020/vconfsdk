package com.kedacom.vconf.sdk.webrtc;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by Sissi on 2019/12/30
 */
public final class RtcConfig {

    // 是否开启硬编解
    private static final String key_enableVideoCodecHwAcceleration = "enableVideoCodecHwAcceleration";
    // 是否开启simulcast
    private static final String key_enableSimulcast = "enableSimulcast";
    // 偏好的视频格式
    private static final String key_videoCodec = "videoCodec";
    // 偏好的编码视频宽度
    private static final String key_videoWidth = "videoWidth";
    // 偏好的编码视频高度
    private static final String key_videoHeight = "videoHeight";
    // 偏好的编码视频帧率
    private static final String key_videoFps = "videoFps";
    // 最大编码视频码流
    private static final String key_videoMaxBitrate = "videoMaxBitrate";
    // 偏好的编码音频格式
    private static final String key_audioCodec = "audioCodec";
    // 偏好的编码音频码率
    private static final String key_audioStartBitrate = "audioStartBitrate";
    // 本地音频是否开启（false为哑音状态）
    private static final String key_isLocalAudioEnabled = "isLocalAudioEnabled";
    // 远端音频是否开启（false为静音状态）
    private static final String key_isRemoteAudioEnabled = "isRemoteAudioEnabled";
    // 本地视频是否开启（false为屏蔽摄像头状态）
    private static final String key_isLocalVideoEnabled = "isLocalVideoEnabled";
    // 远端视频是否开启（false则屏蔽所有远端视频）
    private static final String key_isRemoteVideoEnabled = "isRemoteVideoEnabled";
    // 是否偏好前置摄像头（true前置，false后置）
    private static final String key_isPreferFrontCamera = "isPreferFrontCamera";
    // 视频质量偏好
    private static final String key_preferredVideoQuality = "preferredVideoQuality";

    private SharedPreferences rtcUserConfig;
    private SharedPreferences.Editor editor;
    private static final String RTC_SP_NAME = "rtcUserConfig";

    RtcConfig(Application context){
        rtcUserConfig = context.getSharedPreferences(RTC_SP_NAME, Context.MODE_PRIVATE);
        editor = rtcUserConfig.edit();
    }


    public RtcConfig setEnableVideoCodecHwAcceleration(boolean enableVideoCodecHwAcceleration){
        editor.putBoolean(key_enableVideoCodecHwAcceleration, enableVideoCodecHwAcceleration).apply();
        return this;
    }

    public boolean getEnableVideoCodecHwAcceleration(){
        return rtcUserConfig.getBoolean(key_enableVideoCodecHwAcceleration, true);
    }


    public RtcConfig setEnableSimulcast(boolean enableSimulcast){
        editor.putBoolean(key_enableSimulcast, enableSimulcast).apply();
        return this;
    }

    public boolean getEnableSimulcast(){
        return rtcUserConfig.getBoolean(key_enableSimulcast, true);
    }

    public static final int VideoQuality_High = 10;
    public static final int VideoQuality_Middle = 9;
    public static final int VideoQuality_Low = 8;

    /**
     * 设置视频质量偏好。
     * */
    public RtcConfig setPreferredVideoQuality(int quality){
        editor.putInt(key_preferredVideoQuality, quality).apply();
        return this;
    }

    public int getPreferredVideoQuality(){
        return rtcUserConfig.getInt(key_preferredVideoQuality, VideoQuality_High);
    }


    public RtcConfig setVideoCodec(String videoCodec){
        editor.putString(key_videoCodec, videoCodec).apply();
        return this;
    }

    public String getVideoCodec(){
        return rtcUserConfig.getString(key_videoCodec, "H264");
    }

    public RtcConfig setVideoWidth(int videoWidth){
        editor.putInt(key_videoWidth, videoWidth).apply();
        return this;
    }

    public int getVideoWidth(){
        return rtcUserConfig.getInt(key_videoWidth, 1920);
    }

    public RtcConfig setVideoHeight(int videoHeight){
        editor.putInt(key_videoHeight, videoHeight).apply();
        return this;
    }

    public int getVideoHeight(){
        return rtcUserConfig.getInt(key_videoHeight, 1080);
    }

    public RtcConfig setVideoFps(int videoFps){
        editor.putInt(key_videoFps, videoFps).apply();
        return this;
    }

    public int getVideoFps(){
        return rtcUserConfig.getInt(key_videoFps, 20);
    }

    public RtcConfig setVideoMaxBitrate(int videoMaxBitrate){
        editor.putInt(key_videoMaxBitrate, videoMaxBitrate).apply();
        return this;
    }

    public int getVideoMaxBitrate(){
        return rtcUserConfig.getInt(key_videoMaxBitrate, 2048);
    }

    public RtcConfig setAudioCodec(String audioCodec){
        editor.putString(key_audioCodec, audioCodec).apply();
        return this;
    }

    public String getAudioCodec(){
        return rtcUserConfig.getString(key_audioCodec, "opus");
    }

    public RtcConfig setAudioStartBitrate(int audioStartBitrate){
        editor.putInt(key_audioStartBitrate, audioStartBitrate).apply();
        return this;
    }

    public int getAudioStartBitrate(){
        return rtcUserConfig.getInt(key_audioStartBitrate, 32);
    }

    public RtcConfig setIsLocalAudioEnabled(boolean isLocalAudioEnabled){
        editor.putBoolean(key_isLocalAudioEnabled, isLocalAudioEnabled).apply();
        return this;
    }

    public boolean getIsLocalAudioEnabled(){
        return rtcUserConfig.getBoolean(key_isLocalAudioEnabled, true);
    }

    public RtcConfig setIsRemoteAudioEnabled(boolean isRemoteAudioEnabled){
        editor.putBoolean(key_isRemoteAudioEnabled, isRemoteAudioEnabled).apply();
        return this;
    }

    public boolean getIsRemoteAudioEnabled(){
        return rtcUserConfig.getBoolean(key_isRemoteAudioEnabled, true);
    }

    public RtcConfig setIsLocalVideoEnabled(boolean isLocalVideoEnabled){
        editor.putBoolean(key_isLocalVideoEnabled, isLocalVideoEnabled).apply();
        return this;
    }

    public boolean getIsLocalVideoEnabled(){
        return rtcUserConfig.getBoolean(key_isLocalVideoEnabled, true);
    }

    public RtcConfig setIsRemoteVideoEnabled(boolean isRemoteVideoEnabled){
        editor.putBoolean(key_isRemoteVideoEnabled, isRemoteVideoEnabled).apply();
        return this;
    }

    public boolean getIsRemoteVideoEnabled(){
        return rtcUserConfig.getBoolean(key_isRemoteVideoEnabled, true);
    }

    public RtcConfig setIsPreferFrontCamera(boolean isPreferFrontCamera){
        editor.putBoolean(key_isPreferFrontCamera, isPreferFrontCamera).apply();
        return this;
    }

    public boolean getIsPreferFrontCamera(){
        return rtcUserConfig.getBoolean(key_isPreferFrontCamera, true);
    }


}
