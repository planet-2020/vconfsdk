package com.kedacom.vconf.sdk.main.conf;

import com.kedacom.vconf.sdk.amulet.Caster;

import java.util.Map;

/**
 * Created by Sissi on 2019/7/29
 */
public final class ConfManager extends Caster<Msg> {

    private static ConfManager instance = null;

    private ConfManager() {
    }

    public static ConfManager getInstance() {
        if (instance == null) {
            synchronized (ConfManager.class) {
                if (instance == null) {
                    instance = new ConfManager();
                }
            }
        }
        return instance;
    }


    @Override
    protected Map<Msg[], RspProcessor<Msg>> rspsProcessors() {
        return null;
    }

    @Override
    protected Map<Msg[], NtfProcessor<Msg>> ntfsProcessors() {
        return null;
    }

}
