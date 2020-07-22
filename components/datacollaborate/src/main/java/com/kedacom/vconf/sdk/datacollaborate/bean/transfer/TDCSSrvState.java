package com.kedacom.vconf.sdk.datacollaborate.bean.transfer;

public class TDCSSrvState {
    public EmServerState emState;
    public boolean bInConference;
    public String achConfE164;

    public TDCSSrvState(EmServerState emState, boolean bInConference, String achConfE164) {
        this.emState = emState;
        this.bInConference = bInConference;
        this.achConfE164 = achConfE164;
    }
}