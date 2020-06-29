package com.kedacom.vconf.sdk.base.upgrade;

import androidx.annotation.NonNull;

import com.kedacom.vconf.sdk.base.upgrade.bean.UpgradePkgInfo;
import com.kedacom.vconf.sdk.base.upgrade.bean.transfer.TMTUpgradeVersionInfo;

final class ToDoConverter {
    static UpgradePkgInfo TMTUpgradeVersionInfo2UpgradePkgInfo(@NonNull TMTUpgradeVersionInfo to){
        return new UpgradePkgInfo(to.achFileName, to.dwSize, to.dwVer_id, to.achCurSoftVer, to.achVerNotes);
    }
}
