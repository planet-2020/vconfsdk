package com.kedacom.vconf.sdk.base.amulet;

/**
 * Created by Sissi on 2018/9/17.
 */

interface ICommandProcessor {
    void set(String setId, Object para);
    Object get(String getId);
    Object get(String getId, Object para);
}
