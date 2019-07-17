package com.kedacom.vconf.sdk.datacollaborate.bean;

import androidx.annotation.NonNull;

public class DCMember {
    private String e164;
    private String name;
    private ETerminalType type; // 类型
    private boolean bOperator; // 是否协作方
    private boolean bChairman; // 是否主席
    private boolean bOnline;

    public DCMember(String e164, String name, ETerminalType type, boolean bOperator, boolean bChairman, boolean bOnline) {
        this.e164 = e164;
        this.name = name;
        this.type = type;
        this.bOperator = bOperator;
        this.bChairman = bChairman;
        this.bOnline = bOnline;
    }

    @NonNull
    @Override
    public String toString() {
        return "DCMember{" +
                "e164='" + e164 + '\'' +
                ", name='" + name + '\'' +
                ", type=" + type +
                ", bOperator=" + bOperator +
                ", bChairman=" + bChairman +
                ", bOnline=" + bOnline +
                '}';
    }

    public String getE164() {
        return e164;
    }

    public void setE164(String e164) {
        this.e164 = e164;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ETerminalType getType() {
        return type;
    }

    public void setType(ETerminalType type) {
        this.type = type;
    }

    public boolean isbOperator() {
        return bOperator;
    }

    public void setbOperator(boolean bOperator) {
        this.bOperator = bOperator;
    }

    public boolean isbChairman() {
        return bChairman;
    }

    public void setbChairman(boolean bChairman) {
        this.bChairman = bChairman;
    }

    public boolean isbOnline() {
        return bOnline;
    }

    public void setbOnline(boolean bOnline) {
        this.bOnline = bOnline;
    }
}
