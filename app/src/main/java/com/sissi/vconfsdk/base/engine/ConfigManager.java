package com.sissi.vconfsdk.base.engine;


/**
 * native配置管理器(CM)
 * Created by Sissi on 2/22/2017.
 */
final class ConfigManager {
    private static ConfigManager instance;
    private ConfigManager(){
    }

    synchronized static ConfigManager instance() {
        if (null == instance) {
            instance = new ConfigManager();
        }

        return instance;
    }

    /**
     * 设置配置。
     * 该接口阻塞
     * */
    void setConfig(String reqId, String config){
        NativeMethods.invoke(reqId, config);
    }

    /**
     * 获取配置。
     * 该接口阻塞
     * */
    String getConfig(String reqId){
        StringBuffer buffer = new StringBuffer();
        NativeMethods.invoke(reqId, buffer);
        return buffer.toString();
    }
}
