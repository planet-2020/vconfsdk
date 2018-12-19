package com.kedacom.vconf.sdk.datacollaborate.bean;

import androidx.annotation.NonNull;

public class CreateConfResult {
    private String       confE164;
    private String       confName;
    private EDcMode confMode;
    private EConfType   confType;

    public CreateConfResult(String confE164, String confName, EDcMode confMode, EConfType confType) {
        this.confE164 = confE164;
        this.confName = confName;
        this.confMode = confMode;
        this.confType = confType;
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
}
