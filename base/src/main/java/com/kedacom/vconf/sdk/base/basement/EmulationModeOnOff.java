/**
 * Created by gaofan_kd7331, 2018-10-25
 *
 * 仿真模式开关。
 *
 * 当开启时，可本地模拟“请求——消息响应”流程，这样界面层不必等到底层接口开发完成即可走通整个业务流程。
 * 待底层接口开发完成后，关闭此开关即可。
 *
 * 该开关仅本地开发调试情况下可开启，SVN上的版本应始终为关闭状态且该文件在首次提交后应设置为禁止提交！
 * */

package com.kedacom.vconf.sdk.base.basement;

public final class EmulationModeOnOff {
    static boolean on = true;
    public static void enable(boolean bEnable){
        on = bEnable;
    }
}
