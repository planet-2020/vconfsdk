package com.sissi.vconfsdk.base.amulet;

/**
 * 模拟器开关。
 * Created by Sissi on 2/9/2017.
 *
 * 当开启时，可本地模拟“请求——消息响应”流程，这样界面层不必等到底层Native接口开发完成即可走通整个业务流程。
 * 待底层Native接口开发完成后，关闭此开关即可。
 *
 * 该开关仅调试模式下开启！SVN上的版本应始终为关闭状态且该文件在首次提交后应设置为不可提交！
 * */
class NativeEmulatorOnOff {
    static final boolean on = true;
}
