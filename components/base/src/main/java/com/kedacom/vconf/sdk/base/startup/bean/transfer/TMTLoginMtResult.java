package com.kedacom.vconf.sdk.base.startup.bean.transfer;

public class TMTLoginMtResult {
    public boolean             bLogin;
    public TMtcBaseInfo         tLoginParam;
    public boolean             bKickOther;
    public TMtcBaseInfo         tKickee;
    public boolean             bNameExist;
    public int                dwLoginErrCnt;
    public int                dwLoginTime;
    public int				   dwMaxLoginCnt;
    public int				   dwMaxLockTime;
    public EmMtcLoginFailReason emFailReason;
}
