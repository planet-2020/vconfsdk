package com.sissi.vconfsdk.base.engine;

/**
 * Created by Sissi on 2018/9/17.
 */

public interface ICommandProcessor {
    void set(String setId, Object para);
    Object get(String getId);
    Object get(String getId, Object para);
}
