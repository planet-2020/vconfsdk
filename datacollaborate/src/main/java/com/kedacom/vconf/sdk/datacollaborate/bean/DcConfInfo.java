package com.kedacom.vconf.sdk.datacollaborate.bean;

import androidx.annotation.NonNull;

public class DcConfInfo {
    private String  confE164; // 会议e164
    private String  confName; // 会议名称
    private EDcMode confMode; // 数据协作模式
    private EConfType   confType; // 会议类型
    private boolean hasBoard; // 会议中是否存在画板
    private boolean bCreator; // 该数据协作是否是自己新建的（注：自己新建的返回true，退出后再次进入该字段会变为false）

    public DcConfInfo(String confE164, String confName, EDcMode confMode, EConfType confType, boolean bCreator) {
        this.confE164 = confE164;
        this.confName = confName;
        this.confMode = confMode;
        this.confType = confType;
        this.bCreator = bCreator;
    }

    @NonNull
    @Override
    public String toString() {
        return "{"+String.format("confE164=%s, confName=%s, mode=%s, confType=%s", confE164, confName, confMode, confType)+"}";
    }

    public String getConfE164() {
        return confE164;
    }

    public void setConfE164(String confE164) {
        this.confE164 = confE164;
    }

    public String getConfName() {
        return confName;
    }

    public void setConfName(String confName) {
        this.confName = confName;
    }

    public EDcMode getConfMode() {
        return confMode;
    }

    public void setConfMode(EDcMode confMode) {
        this.confMode = confMode;
    }

    public EConfType getConfType() {
        return confType;
    }

    public void setConfType(EConfType confType) {
        this.confType = confType;
    }

    public boolean hasBoard() {
        return hasBoard;
    }

    public void setHasBoard(boolean hasBoard) {
        this.hasBoard = hasBoard;
    }

    public boolean isCreator() {
        return bCreator;
    }

}
