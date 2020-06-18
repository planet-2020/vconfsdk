package com.kedacom.vconf.sdk.base.login.bean.transfer;

public class TMTAccountManagerSystem {
	public String achusername = ""; // 账号，可以是E164号码或邮箱
	public String achpassword = ""; // 密码
	public String achmobileNum = ""; // 联系电话，可选
	public String achextNum = ""; // 分机，可选
	public String achseat = ""; // 座位号，可选
	public String achbrief = ""; // 一句话介绍, 支持70个汉字，可选
	public String achposition = ""; // 职位，可选
	public String achName = ""; // 姓名，可选
	public String achAPIAddr = ""; // API地址
	public String achEmail = "";// 邮箱

	public boolean bMale; // 性别，可选
	public boolean bIsAdding;

	public int dwBitMask;
	public int dwContextId; // 用户自定义的参数

	public TMTAccountManagerSystem(String achusername) {
		this.achusername = achusername;
	}
}
