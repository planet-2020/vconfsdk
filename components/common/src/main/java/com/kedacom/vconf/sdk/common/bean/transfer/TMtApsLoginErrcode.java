package com.kedacom.vconf.sdk.common.bean.transfer;

public class TMtApsLoginErrcode {
	public boolean bSucess; // 是否成功 若失败 通过下面的错误码获取错误信息
	public int dwHttpErrcode; // http错误码 200表示链接服务器成功 其他表明连接服务器失败
	public int dwApsErroce; // bSucess为FALSE http错误码为200的情况下 服务器返回的错误码
}
