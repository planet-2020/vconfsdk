package com.kedacom.vconf.sdk.webrtc;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by Sissi on 2019/12/30
 */
final class UserConfig {

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

    private SharedPreferences rtcUserConfig;
    private SharedPreferences.Editor editor;
    private static final String RTC_SP_NAME = "rtcUserConfig";

    UserConfig(Application context){
        rtcUserConfig = context.getSharedPreferences(RTC_SP_NAME, Context.MODE_PRIVATE);
        editor = rtcUserConfig.edit();
    }


    UserConfig setEnableVideoCodecHwAcceleration(boolean enableVideoCodecHwAcceleration){
        editor.putBoolean(key_enableVideoCodecHwAcceleration, enableVideoCodecHwAcceleration).apply();
        return this;
    }

    boolean getEnableVideoCodecHwAcceleration(){
        return rtcUserConfig.getBoolean(key_enableVideoCodecHwAcceleration, true);
    }


    UserConfig setEnableSimulcast(boolean enableSimulcast){
        editor.putBoolean(key_enableSimulcast, enableSimulcast).apply();
        return this;
    }

    boolean getEnableSimulcast(){
        return rtcUserConfig.getBoolean(key_enableSimulcast, true);
    }

    UserConfig setVideoCodec(String videoCodec){
        editor.putString(key_videoCodec, videoCodec).apply();
        return this;
    }

    String getVideoCodec(){
        return rtcUserConfig.getString(key_videoCodec, "H264");
    }

    UserConfig setVideoWidth(int videoWidth){
        editor.putInt(key_videoWidth, videoWidth).apply();
        return this;
    }

    int getVideoWidth(){
        return rtcUserConfig.getInt(key_videoWidth, 1920);
    }

    UserConfig setVideoHeight(int videoHeight){
        editor.putInt(key_videoHeight, videoHeight).apply();
        return this;
    }

    int getVideoHeight(){
        return rtcUserConfig.getInt(key_videoHeight, 1080);
    }

    UserConfig setVideoFps(int videoFps){
        editor.putInt(key_videoFps, videoFps).apply();
        return this;
    }

    int getVideoFps(){
        return rtcUserConfig.getInt(key_videoFps, 20);
    }

    UserConfig setVideoMaxBitrate(int videoMaxBitrate){
        editor.putInt(key_videoMaxBitrate, videoMaxBitrate).apply();
        return this;
    }

    int getVideoMaxBitrate(){
        return rtcUserConfig.getInt(key_videoMaxBitrate, 2048);
    }

    UserConfig setAudioCodec(String audioCodec){
        editor.putString(key_audioCodec, audioCodec).apply();
        return this;
    }

    String getAudioCodec(){
        return rtcUserConfig.getString(key_audioCodec, "opus");
    }

    UserConfig setAudioStartBitrate(int audioStartBitrate){
        editor.putInt(key_audioStartBitrate, audioStartBitrate).apply();
        return this;
    }

    int getAudioStartBitrate(){
        return rtcUserConfig.getInt(key_audioStartBitrate, 32);
    }

    UserConfig setIsLocalAudioEnabled(boolean isLocalAudioEnabled){
        editor.putBoolean(key_isLocalAudioEnabled, isLocalAudioEnabled).apply();
        return this;
    }

    boolean getIsLocalAudioEnabled(){
        return rtcUserConfig.getBoolean(key_isLocalAudioEnabled, true);
    }

    UserConfig setIsRemoteAudioEnabled(boolean isRemoteAudioEnabled){
        editor.putBoolean(key_isRemoteAudioEnabled, isRemoteAudioEnabled).apply();
        return this;
    }

    boolean getIsRemoteAudioEnabled(){
        return rtcUserConfig.getBoolean(key_isRemoteAudioEnabled, true);
    }

    UserConfig setIsLocalVideoEnabled(boolean isLocalVideoEnabled){
        editor.putBoolean(key_isLocalVideoEnabled, isLocalVideoEnabled).apply();
        return this;
    }

    boolean getIsLocalVideoEnabled(){
        return rtcUserConfig.getBoolean(key_isLocalVideoEnabled, true);
    }

    UserConfig setIsRemoteVideoEnabled(boolean isRemoteVideoEnabled){
        editor.putBoolean(key_isRemoteVideoEnabled, isRemoteVideoEnabled).apply();
        return this;
    }

    boolean getIsRemoteVideoEnabled(){
        return rtcUserConfig.getBoolean(key_isRemoteVideoEnabled, true);
    }

    UserConfig setIsPreferFrontCamera(boolean isPreferFrontCamera){
        editor.putBoolean(key_isPreferFrontCamera, isPreferFrontCamera).apply();
        return this;
    }

    boolean getIsPreferFrontCamera(){
        return rtcUserConfig.getBoolean(key_isPreferFrontCamera, true);
    }


}
