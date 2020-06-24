package com.kedacom.vconf.sdk.base.settings;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import com.kedacom.vconf.sdk.amulet.Caster;
import com.kedacom.vconf.sdk.common.type.BaseTypeBool;

public class SettingsManager extends Caster<Msg> {

    // 是否启动后立即检测升级
    private static final String key_checkUpgradeImmediatelyStartup = "key_checkUpgradeImmediatelyStartup";

    private SharedPreferences settings;
    private SharedPreferences.Editor editor;

    private static SettingsManager instance = null;
    private Context context;

    private SettingsManager(Context ctx) {
        context = ctx;
        settings = context.getSharedPreferences("BaseSettings", Context.MODE_PRIVATE);
        editor = settings.edit();
    }

    public synchronized static SettingsManager getInstance(Application ctx) {
        if (instance == null) {
            instance = new SettingsManager(ctx);
        }
        return instance;
    }


    /**
     * 设置是否开启交互式调试。
     * 若开启则可通过命令行输入命令交互式调试程序，具体操作方法请询业务组件开发。
     * */
    public SettingsManager setEnableInteractiveDebug(boolean enable){
        BaseTypeBool baseTypeBool = new BaseTypeBool(enable);
        set(Msg.SetEnableInteractiveDebug, baseTypeBool);
        return this;
    }

    /**
     * 交互式调试是否已开启
     * */
    public boolean hasEnabledInteractiveDebug(){
        BaseTypeBool baseTypeBool = (BaseTypeBool) get(Msg.HasEnabledInteractiveDebug);
        if (baseTypeBool==null) return false;
        return baseTypeBool.basetype;
    }

    /**
     * 设置是否启动后立即检测升级
     * */
    public SettingsManager setCheckUpgradeImmediatelyStartup(boolean enable){
        editor.putBoolean(key_checkUpgradeImmediatelyStartup, enable).apply();
        return this;
    }

    /**
     * 是否启动后立即检测升级
     * */
    public boolean needCheckUpgradeImmediatelyStartup(){
        return settings.getBoolean(key_checkUpgradeImmediatelyStartup, false);
    }

}
