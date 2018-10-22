package com.sissi.vconfsdk.datacollaborate;

import com.sissi.vconfsdk.base.Msg;
import com.sissi.vconfsdk.base.RequestAgent;

import java.util.Map;

public class DataCollaborateManager extends RequestAgent {
    @Override
    protected Map<Msg, RspProcessor> rspProcessors() {
        return null;
    }

    @Override
    protected Map<Msg, NtfProcessor> ntfProcessors() {
        return null;
    }
    
}
