package com.kedacom.vconf.sdk.base.login;

import com.kedacom.vconf.sdk.base.login.bean.DepartmentInfo;
import com.kedacom.vconf.sdk.base.login.bean.UserDetails;
import com.kedacom.vconf.sdk.common.bean.transfer.TMTWbParseKedaDept;
import com.kedacom.vconf.sdk.common.bean.transfer.TMTWbParseKedaEntUser;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Sissi on 2019/8/5
 */
final class ToDoConverter {

    static UserDetails fromTransferObj(TMTWbParseKedaEntUser entUser){
        UserDetails userDetails = new UserDetails();
        userDetails.account = entUser.achAccount;
        userDetails.moid = entUser.achMoid;
        userDetails.jid = entUser.achJid;
        userDetails.e164 = entUser.achE164;
        userDetails.email = entUser.achentMail;
        userDetails.name = entUser.achentName;
        userDetails.isMale = entUser.bMale;
        userDetails.jobNumber = entUser.achJobNum;
        userDetails.birthDate = entUser.achDateOfBirth;
        userDetails.brief = entUser.achBrief;
        userDetails.phoneNumber = entUser.achMobileNum;
        userDetails.extensionNumber = entUser.achextNum;
        userDetails.seat = entUser.achSeat;
        userDetails.officeLocation = entUser.achOfficeLocation;
        userDetails.portrait32 = entUser.achPortrait32;
        userDetails.portrait40 = entUser.achPortrait40;
        userDetails.portrait64 = entUser.achPortrait64;
        userDetails.portrait128 = entUser.achPortrait128;
        userDetails.portrait256 = entUser.achPortrait256;

        Map<DepartmentInfo, String> positions = new HashMap<>();
        for (TMTWbParseKedaDept dept : entUser.tMtWbParseKedaDepts.atMtWbParseKedaDept){
            positions.put(new DepartmentInfo(dept.dwDepartmentId, dept.achDepartmentName, dept.achFullPath), dept.achDeptPosition);
        }
        userDetails.positions = positions;

        return userDetails;
    }

}
