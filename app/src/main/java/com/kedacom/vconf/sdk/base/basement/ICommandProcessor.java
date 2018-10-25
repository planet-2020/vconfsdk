package com.kedacom.vconf.sdk.base.basement;

interface ICommandProcessor {
    void set(String setId, Object para);
    Object get(String getId);
    Object get(String getId, Object para);
}
