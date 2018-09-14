package com.sissi.vconfsdk.base;

/**
 * Created by Sissi on 2018/9/14.
 */

public class RequesterManager {

    private static RequesterManager instance;
    private RequesterManager(){

    }

    synchronized static RequesterManager instance() {
        if (null == instance) {
            instance = new RequesterManager();
        }

        return instance;
    }
}
