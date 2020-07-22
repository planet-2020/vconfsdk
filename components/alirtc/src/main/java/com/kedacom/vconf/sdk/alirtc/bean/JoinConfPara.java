package com.kedacom.vconf.sdk.alirtc.bean;

public class JoinConfPara {
    public String confNum; // 会议号
    public String password; // 会议密码（没有则不用填）
    public boolean closeCamera; // 关闭摄像头入会
    public boolean closeMic; // 关闭麦克入会

    public JoinConfPara(String confNum, boolean closeCamera, boolean closeMic) {
        this(confNum, "", closeCamera, closeMic);
    }

    public JoinConfPara(String confNum, String password, boolean closeCamera, boolean closeMic) {
        this.confNum = confNum;
        this.password = password;
        this.closeCamera = closeCamera;
        this.closeMic = closeMic;
    }
}
