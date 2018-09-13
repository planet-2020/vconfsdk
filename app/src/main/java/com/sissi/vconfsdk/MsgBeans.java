package com.sissi.vconfsdk;

import com.sissi.annotation.SerializeEnumAsInt;

/**
 * Created by Sissi on 2018/9/6.
 */

@SerializeEnumAsInt
public final class MsgBeans {


    //=> enum definitions
    public enum SetType {
        Phone,
        Pad,
        Tv,
    }

    public enum Color {
        Red,
        Green,
    }

    //<= enum definitions

    public static final class LoginReq {
        public String serverAddr;
        public String account;
        public String passwd;
        public SetType setType;
        public LoginReq(String serverAddr, String account, String passwd, SetType setType){
            this.serverAddr = serverAddr; this.account = account; this.passwd=passwd; this.setType=setType;
        }
    }

    public static final class LoginRsp {
        public String sessionId;
        public int result;
    }

    public static final class LoginRspFin {
        public String sessionId;
        public int result;
    }

    public static final class LogoutReq {
        public String sessionId;
        public LogoutReq(String sessionId){
            this.sessionId = sessionId;
        }
    }

    public static final class LogoutRsp {
        public int result;
    }

}
