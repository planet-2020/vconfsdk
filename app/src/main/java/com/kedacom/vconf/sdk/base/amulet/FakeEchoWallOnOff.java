/**
 * Created by gaofan_kd7331, 2018-10-25
 *
 * 模拟器开关。
 *
 * 当开启时，可本地模拟“请求——消息响应”流程，这样界面层不必等到底层接口开发完成即可走通整个业务流程。
 * 待底层接口开发完成后，关闭此开关即可。
 *
 * 该开关仅调试模式下开启！SVN上的版本应始终为关闭状态且该文件在首次提交后应设置为不可提交！
 * */

package com.kedacom.vconf.sdk.base.amulet;

class FakeEchoWallOnOff {
    static final boolean on = true;
}
