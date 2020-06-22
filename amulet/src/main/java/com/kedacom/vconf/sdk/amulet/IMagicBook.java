package com.kedacom.vconf.sdk.amulet;

import java.util.List;

public interface IMagicBook {

    String getChapter();

    boolean isReqTypeGet(String reqName);

    String getReqId(String reqName);

    String getRspId(String rspName);

    List<String> getRspNames(String rspId);

    String getNativeMethodOwner(String reqName);

    Class<?>[] getNativeParaClasses(String reqName);

    Class<?>[] getUserParaClasses(String reqName);

    String[][] getRspSeqs(String reqName);

    Class<?> getRspClazz(String rspName);

    int getTimeout(String reqName);

}
