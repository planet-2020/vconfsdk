package com.kedacom.vconf.sdk.main.im.bean;

public class TImUserLogin {

	public String achNO; // 登录口号(格式为"用户名@域名")
	public String achUserPwd; // 密码
	public String achStunAddr; // stun server ip address
	public String achDefaultSaveDir; // default save directory
	public String achEnterpriseName; // 企业名称(用于创建组织架构聊天室)
	public String achPicSaveDir; // 保存截图的路径(绝对路径)
	public String achResource = "tp";

	public String achSock5Username; // sock5用户名
	public String achSock5Password; // sock5密码
	public String achConfigPath; // 保存用户配置的路径
	public String achSock5Addr; // sock5地址

	public long dwImIP; // 服务器ip地址
	public int wPort; // 服务器端口
	public int wStunPort; // stun port
	public int wSock5Port; // sock5端口
	public short byPwdLen; // 密码长度

	public boolean bFileShareEnable = true; // 是否启用xmpp文件传输
	public boolean bAudioCapability = true; // 是否有音频能力(硬件)
	public boolean bVideoCapability = true; // 是否有视频能力(硬件)
	public boolean bInstant = true; // 是否立即登录
	public boolean bUseSock5; // 是否启用sock5代理
	public boolean bCompression = true; // 是否使用zlib压缩流
}
