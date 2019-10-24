package com.kedacom.osp;

public enum EmMtOspMsgSys {
	//OSP原生消息
	EV_MtOsp_OSP_POWERON(0x100),
	EV_MtOsp_OSP_OVERFLOW(0x103),
	EV_MtOsp_OSP_DISCONNECT(0x106),
	EV_MtOsp_OSP_BROADCASTACK(0x107),
	EV_MtOsp_OSP_NETBRAECHO(0x109),
	EV_MtOsp_OSP_QUIT(0x120),
	//MT业务消息
	Ev_MtOsp_ProtoBufMsg(29302),
	EV_MtOsp_InstanceInit(29303),
	EV_MtOsp_InstanceExit(29304), 
	//遥控器消息
	Ev_MT_DeviceKeyCodeNtf(5418),
	//UI unittest消息
	Ev_UI_UnitTestNtf(50100);

	private int nVal;
 
	public int getnVal() {
		return nVal;
	}

	public void setnVal(int nVal) {
		this.nVal = nVal;
	}

	private EmMtOspMsgSys( int _nVal ) {	
	    this.nVal = _nVal;	
	}

	public String toString() {	
	    return String.valueOf( this.nVal );
	}
}
