package com.sissi.vconfsdk.base.engine;


/**
 * native配置管理器(CM)
 * Created by Sissi on 2/22/2017.
 */
final class CommandManager {
    private static CommandManager instance;
    private CommandManager(){
    }

    synchronized static CommandManager instance() {
        if (null == instance) {
            instance = new CommandManager();
        }

        return instance;
    }

    /**
     * 设置配置。
     * 该接口阻塞
     * */
    void set(String reqId, String config){
        NativeInteractor.invoke(reqId, config);
    }

    /**
     * 获取配置。
     * 该接口阻塞
     * */
    String get(String reqId){
        StringBuffer buffer = new StringBuffer();
        NativeInteractor.invoke(reqId, buffer);
        return buffer.toString();
    }

    /**
     * 获取配置。
     * 该接口阻塞
     * */
    String get(String reqId, String para){
        StringBuffer buffer = new StringBuffer();
        NativeInteractor.invoke(reqId, para, buffer);
        return buffer.toString();
    }
}
