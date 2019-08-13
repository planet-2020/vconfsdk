package com.kedacom.vconf.sdk.main.im;

import com.kedacom.vconf.sdk.amulet.Caster;

import java.util.Map;

/**
 * Created by Sissi on 2019/7/29
 */
public final class ImManager extends Caster<Msg> {

    private static ImManager instance = null;

    private ImManager() {
    }

    public static ImManager getInstance() {
        if (instance == null) {
            synchronized (ImManager.class) {
                if (instance == null) {
                    instance = new ImManager();
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
