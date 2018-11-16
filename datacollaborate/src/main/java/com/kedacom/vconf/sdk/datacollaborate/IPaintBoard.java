package com.kedacom.vconf.sdk.datacollaborate;

import android.view.View;

public interface IPaintBoard {
    int getMyId();
    void addPaintView(IPaintView paintView);
    void delPaintView(int viewId);
    IPaintView getPaintView(int viewId);
    View getBoardView();
    void focusPaintView(int viewId);
}
