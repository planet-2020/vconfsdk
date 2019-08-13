package com.kedacom.vconf.sdk.main.conf.bean;

import com.google.gson.annotations.JsonAdapter;
import com.kedacom.vconf.sdk.utils.json.Enum2IntJsonAdapter;

/**
 * Created by Sissi on 2019/7/30
 */
@JsonAdapter(Enum2IntJsonAdapter.class)
public enum EmConfProtocol {
    emProtocolBegin_Api,  ///<起始值
    em323_Api,                ///<H323
    emsip_Api,                ///<SIP
    emsat_Api,                ///<SAT
    emtip_Api,                ///<TIP
}
